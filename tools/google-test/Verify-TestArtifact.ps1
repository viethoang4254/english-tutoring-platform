#Requires -Version 7.2
param([Parameter(Mandatory)]$Config)
$ErrorActionPreference='Stop'
$receipt=Get-Content -LiteralPath $Config.artifactReceipt -Raw | ConvertFrom-Json
if ($Config.approvedCommit -notmatch '^[a-f0-9]{40}$' -or $receipt.commit -cne $Config.approvedCommit -or
    $receipt.repository -cne 'viethoang4254/english-tutoring-platform' -or $receipt.platform -cne 'win32-x64' -or
    $receipt.apiOrigin -cne 'http://localhost:8081' -or [string]$receipt.runId -cne [string]$Config.approvedPackageRunId) { throw 'Artifact provenance mismatch.' }
# User verifies this receipt/run against GitHub before accepting the local files.
$root=[IO.Path]::GetFullPath($Config.runtimeDirectory).TrimEnd('\')+'\'
foreach ($property in $receipt.files.PSObject.Properties) {
    $path=[IO.Path]::GetFullPath((Join-Path $root $property.Name))
    if (-not $path.StartsWith($root,[StringComparison]::OrdinalIgnoreCase) -or $property.Value -notmatch '^[a-f0-9]{64}$') { throw 'Invalid artifact manifest path/hash.' }
    if (-not (Test-Path -LiteralPath $path -PathType Leaf) -or (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -ine $property.Value) { throw 'Runtime artifact checksum mismatch.' }
}
foreach ($required in @('backend/application.jar','frontend/package.json','frontend/.next/BUILD_ID','frontend/node_modules/next/dist/bin/next')) {
    if (-not $receipt.files.PSObject.Properties[$required]) { throw 'Incomplete runtime artifact.' }
}
$actual = @(Get-ChildItem -LiteralPath $root -Recurse -Force -File)
if ($actual.Count -ne @($receipt.files.PSObject.Properties).Count) { throw 'Unexpected runtime files. Extract to a fresh directory.' }
if (Get-ChildItem -LiteralPath $root -Recurse -Force -File | Where-Object { $_.Name -eq '.env' -or $_.Name.StartsWith('.env.') }) { throw 'Environment files are not permitted in the runtime bundle.' }
return $receipt
