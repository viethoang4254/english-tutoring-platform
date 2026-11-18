#Requires -Version 5.1
[CmdletBinding()]
param([Parameter(Mandatory)][string]$Manifest, [switch]$DryRun)
$ErrorActionPreference = 'Stop'
# This entry point deliberately cannot mutate until the fixture authority is resolved.
& (Join-Path $PSScriptRoot 'Start-TestEnvironment.ps1') -Manifest $Manifest -DryRun
Write-Output 'Demo plan: four courses from backend/src/test/resources/demo-courses.json; five 90-minute sessions each; Asia/Ho_Chi_Minh. Use an existing approved TEST Teacher.'
Write-Output 'Publication must use an approved TEST receiving account and a Teacher-provided real Meet URL. No Meet URL is generated.'
Write-Output 'Enrollment/payment creation and Teacher confirmation must use the existing authenticated APIs. No SQL payment activation.'
if (-not $DryRun) { throw 'BLOCKED: use the reviewed authenticated Teacher API procedure only after separate TEST write authorization, identity/TLS verification and hosted acceptance. This script does not create records.' }
