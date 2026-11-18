#Requires -Version 5.1
param([Parameter(Mandatory)]$Receipt, [Parameter(Mandatory)][AllowEmptyCollection()][object[]]$History)
$ErrorActionPreference = 'Stop'
$expected = @($Receipt.migrations.PSObject.Properties)
if ($expected.Count -ne 15 -or $History.Count -ne 15) { throw 'Complete release V1-V15 metadata and history required; no automatic migrations.' }
for ($i=1; $i -le 15; $i++) {
    $files = @($expected | Where-Object { $_.Name -cmatch "^V${i}__[A-Za-z0-9_]+\.sql$" })
    $rows = @($History | Where-Object { [string]$_.version -ceq [string]$i })
    if ($files.Count -ne 1 -or $rows.Count -ne 1) { throw 'Unexpected or duplicate migration version.' }
    $row=$rows[0]; $file=$files[0]
    if ($null -eq $file.Value -or $null -eq $row.checksum -or
        [string]$file.Value -notmatch '^-?\d+$' -or [string]$row.checksum -notmatch '^-?\d+$' -or
        $row.success -isnot [bool] -or -not $row.success -or $row.script -cne $file.Name -or
        [long]$row.checksum -ne [long]$file.Value) { throw 'Migration history/checksum mismatch.' }
}
