#Requires -Version 5.1
$ErrorActionPreference='Stop'
$verify=Join-Path $PSScriptRoot 'Verify-TestMigrations.ps1'
function Fixture {
    $m=@{}; $history=@()
    foreach ($i in 1..15) {
        $name="V${i}__synthetic.sql"; $m[$name]=$i
        $history+= [pscustomobject]@{version=[string]$i;script=$name;checksum=$i;success=$true}
    }
    return @{Receipt=[pscustomobject]@{migrations=[pscustomobject]$m};History=$history}
}
$valid=Fixture
& $verify @valid
foreach ($case in @('missing','altered','unexpected','failed','duplicate','metadata')) {
    $fixture=Fixture
    switch ($case) {
        missing { $fixture.History=$fixture.History[0..13] }
        altered { $fixture.History[14].checksum=999 }
        unexpected { $fixture.History[14].version='16' }
        failed { $fixture.History[14].success=$false }
        duplicate { $fixture.History[14].version='14' }
        metadata { $fixture.Receipt.migrations.PSObject.Properties.Remove('V15__synthetic.sql') }
    }
    $rejected=$false
    try { & $verify @fixture } catch { $rejected=$true }
    if (-not $rejected) { throw "Invalid migration fixture accepted: $case" }
}
$launcher=Get-Content -Raw (Join-Path $PSScriptRoot 'Start-TestEnvironment.ps1')
foreach ($name in @('SPRING_FLYWAY_ENABLED','APP_GOOGLE_CALENDAR_SYNC_ENABLED','APP_GOOGLE_CALENDAR_INVITATIONS_ENABLED','APP_SESSION_REMINDERS_ENABLED','APP_CONTACT_ENABLED')) {
    if ($launcher -notmatch ($name+"\s*=\s*'false'")) { throw 'Required disabled flag missing.' }
}
