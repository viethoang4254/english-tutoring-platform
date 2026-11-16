#Requires -Version 7.2
# Run manually in PowerShell 7.2+ on Windows. Never dot-source this script.
[CmdletBinding(SupportsShouldProcess, ConfirmImpact='High')]
param([string]$Directory = 'E:\SupabaseSecrets\google-calendar-test')
$ErrorActionPreference = 'Stop'
if (-not $IsWindows) { throw 'Windows DPAPI is required.' }
$Directory = [IO.Path]::GetFullPath($Directory)
if (-not $Directory.StartsWith('E:\SupabaseSecrets\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Use the external TEST secret directory.' }
if (Test-Path -LiteralPath $Directory) { throw 'Directory exists. Refusing to overwrite credentials or encryption keys.' }
if (-not $PSCmdlet.ShouldProcess($Directory, 'Create private TEST secrets')) { return }
New-Item -ItemType Directory -Path $Directory | Out-Null
$sid = [Security.Principal.WindowsIdentity]::GetCurrent().User
$acl = [Security.AccessControl.DirectorySecurity]::new()
$acl.SetOwner($sid)
$acl.SetAccessRuleProtection($true, $false)
$rule = [Security.AccessControl.FileSystemAccessRule]::new($sid, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')
$acl.AddAccessRule($rule)
Set-Acl -LiteralPath $Directory -AclObject $acl
foreach ($name in @('client-id', 'client-secret', 'database-password')) {
    Read-Host "Enter TEST $name locally (hidden)" -AsSecureString | Export-Clixml -LiteralPath (Join-Path $Directory "$name.dpapi.xml")
}
$bytes = [byte[]]::new(32)
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
try { [IO.File]::WriteAllText((Join-Path $Directory 'token-key.base64'), [Convert]::ToBase64String($bytes), [Text.UTF8Encoding]::new($false)) }
finally { [Array]::Clear($bytes, 0, $bytes.Length) }

# Independent application authentication material for this isolated instance.
$rsa = [Security.Cryptography.RSA]::Create(2048)
try {
    [IO.File]::WriteAllText((Join-Path $Directory 'jwt-private.pem'), ("-----BEGIN PRIVATE KEY-----`n" + [Convert]::ToBase64String($rsa.ExportPkcs8PrivateKey(), [Base64FormattingOptions]::InsertLineBreaks) + "`n-----END PRIVATE KEY-----`n"), [Text.UTF8Encoding]::new($false))
    [IO.File]::WriteAllText((Join-Path $Directory 'jwt-public.pem'), ("-----BEGIN PUBLIC KEY-----`n" + [Convert]::ToBase64String($rsa.ExportSubjectPublicKeyInfo(), [Base64FormattingOptions]::InsertLineBreaks) + "`n-----END PUBLIC KEY-----`n"), [Text.UTF8Encoding]::new($false))
} finally { $rsa.Dispose() }
$hmac = [byte[]]::new(32)
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($hmac) } finally { $rng.Dispose() }
try {
    ConvertTo-SecureString ([Convert]::ToBase64String($hmac)) -AsPlainText -Force |
        Export-Clixml -LiteralPath (Join-Path $Directory 'reset-hmac.dpapi.xml')
} finally { [Array]::Clear($hmac, 0, $hmac.Length) }
Write-Output 'TEST secrets saved. No secret values were printed. Preserve this encryption key.'
