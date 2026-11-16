#Requires -Version 5.1
[CmdletBinding()]
param([Parameter(Mandatory)][string]$Manifest, [switch]$DryRun)
$ErrorActionPreference = 'Stop'
# This entry point deliberately cannot mutate until the fixture authority is resolved.
& (Join-Path $PSScriptRoot 'Start-TestEnvironment.ps1') -Manifest $Manifest -DryRun
Write-Output 'Demo plan: synthetic approved Teacher and Student; Tiếng Anh giao tiếp thực tế; 1000000 VND; Asia/Ho_Chi_Minh; three future sessions.'
Write-Output 'Publication must use an approved TEST receiving account and a Teacher-provided real Meet URL. No Meet URL is generated.'
Write-Output 'Enrollment/payment creation and Teacher confirmation must use the existing authenticated APIs. No SQL payment activation.'
if (-not $DryRun) { throw 'BLOCKED: initial TEST Admin/account provisioning, synthetic receiving-account fixture and real Teacher Meet URL must be resolved before an executable demo mutator is supplied.' }
