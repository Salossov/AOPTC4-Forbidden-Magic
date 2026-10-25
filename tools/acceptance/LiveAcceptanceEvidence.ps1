function Assert-FmLiveEvidence {
    [CmdletBinding()]
    param([object]$Report, [int]$ExitCode, [string]$RunId, [string]$Profile, [string]$Phase)
    if ($ExitCode -ne 0) { throw "Launcher exited $ExitCode" }
    if ($Report.runId -cne $RunId -or $Report.profile -cne $Profile -or $Report.phase -cne $Phase) { throw 'Stale or unrelated acceptance report.' }
    if ($Report.passed -ne $true -or $Report.serverStopped -ne $true) { throw "Incomplete/failed server acceptance: $($Report.error)" }
    if (@($Report.checks).Count -eq 0 -or @($Report.checks | Where-Object { $_.passed -ne $true }).Count -gt 0) { throw 'Acceptance assertions are absent or failed.' }
    if ([long]$Report.processId -le 0 -or [int]$Report.ticks -lt 1) { throw 'No live server execution evidence.' }
    if ($Report.gameplayOrigin -notmatch 'ForbiddenMagic-1\.21\.1-.*\.jar') { throw 'Gameplay code did not originate from the packaged JAR.' }
}
