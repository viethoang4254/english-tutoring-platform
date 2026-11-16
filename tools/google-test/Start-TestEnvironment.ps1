#Requires -Version 5.1
# Preparation only: no production launcher is called. Requires Windows PowerShell 5.1.
[CmdletBinding(SupportsShouldProcess, ConfirmImpact='High')]
param(
    [Parameter(Mandatory)][string]$Manifest,
    [switch]$DryRun,
    [switch]$StartBackend,
    [switch]$VerifyDatabase
)
$ErrorActionPreference = 'Stop'
$project = 'zobxcixrxsazmngcthvg'
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
function Require($ok, [string]$message) { if (-not $ok) { throw $message } }
function Secret([string]$path) {
    $value = Import-Clixml -LiteralPath $path
    Require ($value -is [Security.SecureString]) 'Expected Windows DPAPI SecureString.'
    return [Net.NetworkCredential]::new('', $value).Password
}
function PrivateFile([string]$path) {
    $resolved = [IO.Path]::GetFullPath($path)
    Require ($resolved.StartsWith('E:\SupabaseSecrets\', [StringComparison]::OrdinalIgnoreCase)) 'Secrets must be outside Git in SupabaseSecrets.'
    Require (Test-Path -LiteralPath $resolved -PathType Leaf) 'Required external secret file is missing.'
    $owner = [Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    foreach ($entry in (Get-Acl -LiteralPath $resolved).Access) {
        $identity = $entry.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value
        Require ($entry.AccessControlType -ne 'Allow' -or $identity -in @($owner, 'S-1-5-18', 'S-1-5-32-544')) 'Secret ACL grants access to another identity.'
    }
}
Require ([Environment]::OSVersion.Platform -eq 'Win32NT') 'Use Windows PowerShell 5.1.'
$config = Get-Content -LiteralPath $Manifest -Raw -Encoding UTF8 | ConvertFrom-Json
Require ($config.projectRef -ceq $project) 'TEST project identity mismatch.'
Require ($config.databaseUser -ceq "postgres.$project") 'Only the pinned TEST pooler user is permitted.'
Require ($config.databaseHost -match '^aws-[0-9]+-[a-z0-9-]+\.pooler\.supabase\.com$') 'Expected Supabase session pooler host.'
Require ($config.databasePort -eq 5432 -and $config.databaseName -ceq 'postgres') 'Unexpected TEST database target.'
$listeners = @(Get-NetTCPConnection -State Listen -ErrorAction Stop)
foreach ($port in @(3000, 8081)) {
    Require (-not ($listeners | Where-Object LocalPort -eq $port)) "Port $port is occupied; existing services will not be stopped."
}
foreach ($field in @('clientIdFile', 'clientSecretFile', 'databasePasswordFile', 'tokenKeyFile', 'jwtPrivateKeyFile', 'jwtPublicKeyFile', 'resetHmacFile')) { PrivateFile $config.$field }
$key = [Convert]::FromBase64String(([IO.File]::ReadAllText($config.tokenKeyFile)).Trim())
Require ($key.Length -eq 32) 'Token key must decode to 32 bytes.'
[Array]::Clear($key, 0, $key.Length)
Require ($config.keyId -match '^[A-Za-z0-9_-]{1,40}$') 'Invalid token key identifier.'
$receipt = & (Join-Path $PSScriptRoot 'Verify-TestArtifact.ps1') -Config $config
$config | Add-Member -NotePropertyName backendJar -NotePropertyValue (Join-Path $config.runtimeDirectory 'backend/application.jar') -Force
Require (Test-Path -LiteralPath $config.databaseCaFile -PathType Leaf) 'Trusted TEST database CA file is required; TLS verification will not be disabled.'
Get-Command psql, java -ErrorAction Stop | Out-Null
Write-Output 'Local preflight passed. Database identity and V12 readiness must still be checked. Sync remains disabled.'
if ($DryRun -or (-not $StartBackend -and -not $VerifyDatabase)) {
    Write-Output 'DRY RUN: no database access, processes, migrations or demo records. Execution requires -StartBackend and confirmation.'
    return
}
# Compare the candidate against separately retained TEST metadata before any database operation.
$trusted = Get-Content -LiteralPath 'E:\SupabaseSecrets\english-learning-test.json' -Raw -Encoding UTF8 | ConvertFrom-Json
Require ($trusted.ProjectRef -ceq $project -and $trusted.User -ceq $config.databaseUser -and $trusted.Host -ceq $config.databaseHost -and $trusted.Port -eq 5432 -and $trusted.Database -ceq 'postgres') 'Independent TEST metadata mismatch.'
if (-not $PSCmdlet.ShouldProcess('Pinned Supabase TEST and localhost:8081', 'Read TEST database readiness; start backend only if -StartBackend was supplied')) { return }
$dbPassword = Secret $config.databasePasswordFile
$psi = [Diagnostics.ProcessStartInfo]::new('psql')
$psi.UseShellExecute = $false; $psi.CreateNoWindow = $true
$psi.RedirectStandardOutput = $true; $psi.RedirectStandardError = $true
$psi.EnvironmentVariables.Clear()
foreach ($name in @('PATH','SystemRoot','TEMP','TMP','USERPROFILE')) { $psi.EnvironmentVariables[$name] = [Environment]::GetEnvironmentVariable($name) }
$psi.EnvironmentVariables['PGPASSWORD'] = $dbPassword
$psi.EnvironmentVariables['PGSSLMODE'] = 'verify-full'
$psi.EnvironmentVariables['PGSSLROOTCERT'] = $config.databaseCaFile
$psi.EnvironmentVariables['PGCONNECT_TIMEOUT'] = '10'
$psi.EnvironmentVariables['PGOPTIONS'] = '-c default_transaction_read_only=on -c statement_timeout=10000'
$query = @"
SELECT json_build_object('database',current_database(),'user',current_user,
 'history',(SELECT json_agg(json_build_object('version',version,'script',script,'checksum',checksum,'success',success) ORDER BY installed_rank) FROM public.flyway_schema_history),
 'students',(SELECT count(*) FROM public.users WHERE role='STUDENT' AND NOT locked AND (email_verified OR student_login_without_verification)),
 'teachers',(SELECT count(*) FROM public.users u WHERE role='TEACHER' AND NOT locked AND EXISTS(SELECT FROM public.teacher_applications a WHERE a.teacher_id=u.id AND a.status='APPROVED')));
"@
# The validated host and project-qualified user contain no command-line metacharacters.
$psi.Arguments = "-X -w -h $($config.databaseHost) -p 5432 -U $($config.databaseUser) -d postgres -At -v ON_ERROR_STOP=1 -f -"
$psi.RedirectStandardInput = $true
$check = [Diagnostics.Process]::Start($psi)
$check.StandardInput.WriteLine($query)
$check.StandardInput.Close()
$read = $check.StandardOutput.ReadToEndAsync(); $discard = $check.StandardError.ReadToEndAsync()
if (-not $check.WaitForExit(30000)) { $check.Kill(); throw 'TEST database preflight timed out.' }
Require ($check.ExitCode -eq 0) 'TEST database read-only preflight failed; private diagnostics withheld.'
$result = $read.GetAwaiter().GetResult() | ConvertFrom-Json
Require ($result.database -ceq 'postgres' -and $result.user -ceq 'postgres') 'Unexpected active database or database role.'
$history = @($result.history)
Require ($history.Count -in @(12,13)) 'Complete V12 or V13 history required; no automatic migrations.'
for ($i=0; $i -lt $history.Count; $i++) {
    $row=$history[$i]
    $expected=$receipt.migrations.PSObject.Properties[$row.script]
    Require ($row.success -eq $true -and [string]$row.version -ceq [string]($i+1) -and $null -ne $expected -and [long]$expected.Value -eq [long]$row.checksum) 'Migration history/checksum mismatch.'
}
Write-Output "TEST identity verified; V12 present; V13 present: $($history.Count -eq 13); eligible Students: $($result.students); approved Teachers: $($result.teachers)."
if (-not $StartBackend) { return }
$start = [Diagnostics.ProcessStartInfo]::new('java')
$start.UseShellExecute = $false; $start.CreateNoWindow = $true
$start.WorkingDirectory = [IO.Path]::GetDirectoryName([IO.Path]::GetFullPath($config.backendJar))
$start.EnvironmentVariables.Clear()
foreach ($name in @('PATH','SystemRoot','TEMP','TMP','USERPROFILE','JAVA_HOME')) {
    $v = [Environment]::GetEnvironmentVariable($name); if ($v) { $start.EnvironmentVariables[$name] = $v }
}
$values = @{
    DATABASE_URL = "jdbc:postgresql://$($config.databaseHost):5432/postgres?sslmode=verify-full&sslrootcert=$([Uri]::EscapeDataString($config.databaseCaFile))"
    DATABASE_USERNAME = $config.databaseUser; DATABASE_PASSWORD = $dbPassword
    SPRING_FLYWAY_ENABLED = 'false'; SPRING_JPA_HIBERNATE_DDLAUTO = 'none'; SPRING_SQL_INIT_MODE = 'never'
    SERVER_ADDRESS = '127.0.0.1'; SERVER_PORT = '8081'
    APP_SECURITY_BROWSER_ALLOWEDORIGINS = 'http://localhost:3000'; APP_SECURITY_BROWSER_LOCALHTTP = 'true'
    APP_SECURITY_JWT_PRIVATEKEY = ([Uri]([IO.Path]::GetFullPath($config.jwtPrivateKeyFile))).AbsoluteUri
    APP_SECURITY_JWT_PUBLICKEY = ([Uri]([IO.Path]::GetFullPath($config.jwtPublicKeyFile))).AbsoluteUri
    APP_SECURITY_PASSWORDRESET_HMACSECRET = (Secret $config.resetHmacFile)
    APP_GOOGLE_CALENDAR_ENABLED = 'true'; APP_GOOGLE_CALENDAR_SYNC_ENABLED = 'false'
    APP_GOOGLE_CALENDAR_CLIENT_ID = (Secret $config.clientIdFile); APP_GOOGLE_CALENDAR_CLIENT_SECRET = (Secret $config.clientSecretFile)
    APP_GOOGLE_CALENDAR_REDIRECT_URI = 'http://localhost:8081/api/v1/integrations/google-calendar/callback'
    APP_GOOGLE_CALENDAR_SCOPES = 'openid email https://www.googleapis.com/auth/calendar.app.created'
    APP_GOOGLE_CALENDAR_TOKEN_KEY_ID = $config.keyId; APP_GOOGLE_CALENDAR_TOKEN_KEY_FILE = $config.tokenKeyFile
}
Require ($values.APP_GOOGLE_CALENDAR_CLIENT_ID -and $values.APP_GOOGLE_CALENDAR_CLIENT_SECRET) 'Missing OAuth configuration.'
foreach ($entry in $values.GetEnumerator()) { $start.EnvironmentVariables[$entry.Key] = [string]$entry.Value }
# WorkingDirectory is the verified backend directory, including paths with spaces.
$start.Arguments = '-Xmx512m -jar application.jar --spring.config.location=classpath:/application.yml'
$process = [Diagnostics.Process]::Start($start)
Write-Output "TEST backend process started: PID $($process.Id). This is not a health or OAuth success assertion."
