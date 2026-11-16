#Requires -Version 5.1
# Run manually in PowerShell 5.1+ on Windows. Never dot-source this script.
[CmdletBinding(SupportsShouldProcess, ConfirmImpact='High')]
param(
    [string]$Directory = 'E:\SupabaseSecrets\google-calendar-test',
    [ValidateSet('Efs','AclOnly')][string]$KeyProtection = 'Efs'
)
$ErrorActionPreference = 'Stop'
if (-not ([Environment]::OSVersion.Platform -eq 'Win32NT')) { throw 'Windows DPAPI is required.' }
$Directory = [IO.Path]::GetFullPath($Directory)
$ciDirectory = if ($env:GITHUB_ACTIONS -ceq 'true' -and $env:TEST_RUNNER_KIND -ceq 'github-hosted' -and $env:RUNNER_TEMP) {
    [IO.Path]::GetFullPath((Join-Path $env:RUNNER_TEMP 'google-private-ci'))
}
if ($Directory -ine $ciDirectory -and -not $Directory.StartsWith('E:\SupabaseSecrets\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Use the external TEST secret directory.' }
if (Test-Path -LiteralPath $Directory) { throw 'Directory exists. Refusing to overwrite credentials or encryption keys.' }
for ($parent = [IO.DirectoryInfo]::new($Directory).Parent; $parent; $parent = $parent.Parent) {
    if ($parent.Exists -and ($parent.Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'Secret directory ancestors must not be reparse points.' }
}
if ($KeyProtection -eq 'AclOnly') { Write-Warning 'ACL-only mode: offline disk access or a copied key file can expose the AES and JWT private keys.' }
if (-not $PSCmdlet.ShouldProcess($Directory, "Create private TEST secrets ($KeyProtection)")) { return }
New-Item -ItemType Directory -Path $Directory | Out-Null
$sid = [Security.Principal.WindowsIdentity]::GetCurrent().User
$acl = [Security.AccessControl.DirectorySecurity]::new()
$acl.SetOwner($sid)
$acl.SetAccessRuleProtection($true, $false)
$rule = [Security.AccessControl.FileSystemAccessRule]::new($sid, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')
$acl.AddAccessRule($rule)
Set-Acl -LiteralPath $Directory -AclObject $acl
$effective = Get-Acl -LiteralPath $Directory
$rules = @($effective.GetAccessRules($true, $true, [Security.Principal.SecurityIdentifier]))
if (-not $effective.AreAccessRulesProtected -or $effective.GetOwner([Security.Principal.SecurityIdentifier]) -ne $sid -or
    $rules.Count -ne 1 -or $rules[0].IdentityReference -ne $sid -or $rules[0].AccessControlType -ne 'Allow' -or
    $rules[0].InheritanceFlags -ne $rule.InheritanceFlags) { throw 'Owner-only inheritable NTFS ACL required.' }
# Java reads these files directly. EFS failure never changes the selected mode.
foreach ($name in @('token-key.base64', 'jwt-private.pem')) {
    $file = Join-Path $Directory $name
    [IO.File]::Open($file, [IO.FileMode]::CreateNew).Dispose()
    if ($KeyProtection -eq 'AclOnly') { continue }
    [IO.File]::Encrypt($file)
    if (-not ([IO.File]::GetAttributes($file) -band [IO.FileAttributes]::Encrypted)) { throw 'TEST key storage requires NTFS EFS.' }
}
foreach ($name in @('client-id', 'client-secret', 'database-password')) {
    Read-Host "Enter TEST $name locally (hidden)" -AsSecureString | Export-Clixml -LiteralPath (Join-Path $Directory "$name.dpapi.xml")
}
$bytes = [byte[]]::new(32)
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
try { [IO.File]::WriteAllText((Join-Path $Directory 'token-key.base64'), [Convert]::ToBase64String($bytes), [Text.UTF8Encoding]::new($false)) }
finally { [Array]::Clear($bytes, 0, $bytes.Length) }

# Independent application authentication material for this isolated instance.
$node = (Get-Command node.exe -CommandType Application -ErrorAction Stop | Select-Object -First 1).Path
$exeExists = [IO.File]::Exists($node)
$cwdExists = [IO.Directory]::Exists($Directory)
if (-not ($exeExists -and $cwdExists)) { throw "Node launch preflight: executableExists=$exeExists workingDirectoryExists=$cwdExists" }
$info = [Diagnostics.ProcessStartInfo]::new($node, '-')
$info.WorkingDirectory = $Directory
$info.UseShellExecute = $false
$info.RedirectStandardInput = $true
$info.EnvironmentVariables.Clear()
$info.EnvironmentVariables['SystemRoot'] = $env:SystemRoot
try { $keygen = [Diagnostics.Process]::Start($info) }
catch {
    $cause = $_.Exception.GetBaseException()
    $code = if ($cause -is [ComponentModel.Win32Exception]) { $cause.NativeErrorCode } else { 'unavailable' }
    throw "Node launch failed: executableExists=$exeExists workingDirectoryExists=$cwdExists nativeErrorCode=$code"
}
$keygen.StandardInput.WriteLine(@'
const fs = require('node:fs');
const keys = require('node:crypto').generateKeyPairSync('rsa', {modulusLength: 2048});
fs.writeFileSync('jwt-private.pem', keys.privateKey.export({type:'pkcs8',format:'pem'}));
fs.writeFileSync('jwt-public.pem', keys.publicKey.export({type:'spki',format:'pem'}), {flag:'wx'});
'@
)
$keygen.StandardInput.Close()
$keygen.WaitForExit()
if ($keygen.ExitCode -ne 0) { throw 'Independent TEST JWT key generation failed.' }
$hmac = [byte[]]::new(32)
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($hmac) } finally { $rng.Dispose() }
try {
    ConvertTo-SecureString ([Convert]::ToBase64String($hmac)) -AsPlainText -Force |
        Export-Clixml -LiteralPath (Join-Path $Directory 'reset-hmac.dpapi.xml')
} finally { [Array]::Clear($hmac, 0, $hmac.Length) }
Write-Output 'TEST secrets saved. No secret values were printed. Preserve this encryption key.'
