$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'LiveAcceptanceEvidence.ps1')
$good = [ordered]@{runId='fresh';profile='base';phase='write';passed=$true;serverStopped=$true;processId=123;ticks=100;gameplayOrigin='file:/ForbiddenMagic-1.21.1-test.jar';checks=@(@{id='real';passed=$true})}
$cases = @(
    @{name='valid';change=@{};exit=0;reject=$false},
    @{name='exit-code';change=@{};exit=1;reject=$true},
    @{name='stale-nonce';change=@{runId='old'};exit=0;reject=$true},
    @{name='wrong-profile';change=@{profile='other'};exit=0;reject=$true},
    @{name='wrong-phase';change=@{phase='read'};exit=0;reject=$true},
    @{name='not-stopped';change=@{serverStopped=$false};exit=0;reject=$true},
    @{name='not-passed';change=@{passed=$false};exit=0;reject=$true},
    @{name='no-assertions';change=@{checks=@()};exit=0;reject=$true},
    @{name='false-assertion';change=@{checks=@(@{passed=$false})};exit=0;reject=$true},
    @{name='no-pid';change=@{processId=0};exit=0;reject=$true},
    @{name='no-ticks';change=@{ticks=0};exit=0;reject=$true},
    @{name='dev-classes';change=@{gameplayOrigin='file:/build/classes/java/main'};exit=0;reject=$true}
)
foreach ($case in $cases) {
    $report = @{};foreach ($key in $good.Keys) { $report[$key]=$good[$key] };foreach ($key in $case.change.Keys) { $report[$key]=$case.change[$key] }
    $rejected=$false
    try { Assert-FmLiveEvidence -Report ([pscustomobject]$report) -ExitCode $case.exit -RunId fresh -Profile base -Phase write } catch { $rejected=$true }
    if ($rejected -ne $case.reject) { throw "Live evidence regression: $($case.name)" }
}
Write-Output "FM_LIVE_EVIDENCE_TESTS_PASS checks=$($cases.Count)"
