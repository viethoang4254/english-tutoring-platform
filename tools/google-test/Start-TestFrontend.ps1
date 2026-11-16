#Requires -Version 5.1
[CmdletBinding(SupportsShouldProcess,ConfirmImpact='High')]
param([Parameter(Mandatory)][string]$Manifest,[switch]$StartFrontend)
$ErrorActionPreference='Stop'
$config=Get-Content -LiteralPath $Manifest -Raw -Encoding UTF8 | ConvertFrom-Json
$receipt=& (Join-Path $PSScriptRoot 'Verify-TestArtifact.ps1') -Config $config
if (@(Get-NetTCPConnection -State Listen -ErrorAction Stop | Where-Object LocalPort -eq 3000).Count) { throw 'Port 3000 is occupied.' }
$node=(Get-Command node -ErrorAction Stop).Source
if ((& $node --version) -notmatch '^v24[.]') { throw 'Install Windows x64 Node 24.' }
if (-not $StartFrontend) { Write-Output 'Frontend dry-run passed; no process started.'; return }
if (-not $PSCmdlet.ShouldProcess('localhost:3000','Start matching TEST frontend')) { return }
$psi=[Diagnostics.ProcessStartInfo]::new($node)
$psi.UseShellExecute=$false; $psi.CreateNoWindow=$true
$psi.WorkingDirectory=Join-Path $config.runtimeDirectory 'frontend'
$psi.EnvironmentVariables.Clear()
foreach ($name in @('PATH','SystemRoot','TEMP','TMP','USERPROFILE')) { $psi.EnvironmentVariables[$name]=[Environment]::GetEnvironmentVariable($name) }
$psi.EnvironmentVariables['NODE_ENV']='production'
$psi.Arguments = 'node_modules/next/dist/bin/next start --hostname 127.0.0.1 --port 3000'
$process=[Diagnostics.Process]::Start($psi)
Write-Output "TEST frontend PID $($process.Id); health and OAuth are not yet verified."
