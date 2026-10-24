<#
.SYNOPSIS
    Core parsing and evaluation logic for Forbidden Magic verification suites.
#>

function Parse-FmGradleOutput {
    param(
        [AllowEmptyCollection()]
        [string[]]$OutputLines = @(),

        [Parameter(Mandatory = $true)]
        [int]$GradleExitCode,

        [Parameter(Mandatory = $true)]
        [array]$TestDefinitions,

        [string]$Phase = 'Offline',

        [bool]$IncludeAcceptance = $false,

        [string]$ReportRoot = ''
    )

    # 1. Slice output lines strictly by task
    $taskOutputs = @{}
    $taskHeaders = @{}
    $currentTask = $null

    foreach ($line in $OutputLines) {
        if ($line -match '^>\s*Task\s+:([a-zA-Z0-9_-]+)(?:\s+([A-Z_-]+))?') {
            $currentTask = $matches[1]
            $headerModifier = if ($matches[2]) { $matches[2].Trim() } else { $null }

            if (-not $taskOutputs.ContainsKey($currentTask)) {
                $taskOutputs[$currentTask] = [System.Collections.Generic.List[string]]::new()
            }
            if ($headerModifier) {
                $taskHeaders[$currentTask] = $headerModifier
            }
            $taskOutputs[$currentTask].Add($line)
        } elseif ($line -match '^(?:BUILD SUCCESSFUL|BUILD FAILED|FAILURE:)') {
            $currentTask = $null
        } elseif ($null -ne $currentTask) {
            $taskOutputs[$currentTask].Add($line)
        }
    }

    $results = [System.Collections.Generic.List[PSObject]]::new()

    foreach ($def in $TestDefinitions) {
        $testId = $def.Id
        $task = $def.Task
        $testName = $def.Name
        $targetPhase = $def.Phase
        $expectedMarker = $def.Marker

        $taskLogFile = if (-not [string]::IsNullOrWhiteSpace($ReportRoot)) {
            Join-Path $ReportRoot "$task.log"
        } else {
            $null
        }

        # Handling Acceptance phase
        if ($targetPhase -eq 'Acceptance') {
            # Acceptance suites are strictly gated
            $results.Add([PSCustomObject]@{
                Id = $testId
                Name = $testName
                Task = $task
                Phase = $targetPhase
                Status = 'blocked'
                ExitCode = -1
                DurationSec = $null
                Checks = 0
                Reason = 'Gated: Client/Server acceptance requires completed P04 and explicit user acceptance approval.'
                LogFile = $null
            })
            continue
        }

        # If launcher was executed for Acceptance phase only, offline tests are not_run
        if ($Phase -eq 'Acceptance') {
            $results.Add([PSCustomObject]@{
                Id = $testId
                Name = $testName
                Task = $task
                Phase = $targetPhase
                Status = 'not_run'
                ExitCode = -1
                DurationSec = $null
                Checks = 0
                Reason = 'Offline suites not scheduled during Acceptance-only phase run.'
                LogFile = $null
            })
            continue
        }

        # Check if task was seen in Gradle execution
        if (-not $taskOutputs.ContainsKey($task)) {
            $results.Add([PSCustomObject]@{
                Id = $testId
                Name = $testName
                Task = $task
                Phase = $targetPhase
                Status = 'not_run'
                ExitCode = -1
                DurationSec = $null
                Checks = 0
                Reason = "Task :$task was not invoked or did not produce output during Gradle run."
                LogFile = $null
            })
            continue
        }

        $lines = $taskOutputs[$task]
        if ($taskLogFile) {
            [System.IO.File]::WriteAllLines($taskLogFile, $lines, [System.Text.Encoding]::UTF8)
        }

        $headerMod = if ($taskHeaders.ContainsKey($task)) { $taskHeaders[$task] } else { '' }

        # Check if task was skipped or marked UP-TO-DATE
        if ($headerMod -eq 'UP-TO-DATE' -or $headerMod -eq 'SKIPPED') {
            $results.Add([PSCustomObject]@{
                Id = $testId
                Name = $testName
                Task = $task
                Phase = $targetPhase
                Status = 'failed'
                ExitCode = 1
                DurationSec = $null
                Checks = 0
                Reason = "Task was $headerMod; verification requires a fresh execution of all assertions."
                LogFile = $taskLogFile
            })
            continue
        }

        # Check if task header or output reports FAILED
        $hasFailedHeader = ($headerMod -eq 'FAILED')
        $hasFailedLine = ($lines | Where-Object { $_ -match '(?i)\bFAILED\b|AssertionError|Exception in thread' }) -ne $null

        if ($hasFailedHeader -or $hasFailedLine) {
            $results.Add([PSCustomObject]@{
                Id = $testId
                Name = $testName
                Task = $task
                Phase = $targetPhase
                Status = 'failed'
                ExitCode = 1
                DurationSec = $null
                Checks = 0
                Reason = "Task :$task encountered failure or assertion error during execution."
                LogFile = $taskLogFile
            })
            continue
        }

        # Check for the suite-specific expected marker
        $markerFound = $false
        $checksCount = 0

        if (-not [string]::IsNullOrWhiteSpace($expectedMarker)) {
            $escapedMarker = [regex]::Escape($expectedMarker)
            foreach ($l in $lines) {
                if ($l -match "^${escapedMarker}") {
                    $markerFound = $true
                    if ($l -match 'checks=(\d+)') {
                        $checksCount = [int]$matches[1]
                    }
                    break
                }
            }
        } else {
            # No specific marker required, assume pass if not failed
            $markerFound = $true
        }

        if ($markerFound) {
            $results.Add([PSCustomObject]@{
                Id = $testId
                Name = $testName
                Task = $task
                Phase = $targetPhase
                Status = 'passed'
                ExitCode = 0
                DurationSec = $null
                Checks = $checksCount
                Reason = if ($checksCount -gt 0) { "Passed all $checksCount assertions" } else { "Passed assertions (marker $expectedMarker verified)" }
                LogFile = $taskLogFile
            })
        } else {
            $results.Add([PSCustomObject]@{
                Id = $testId
                Name = $testName
                Task = $task
                Phase = $targetPhase
                Status = 'failed'
                ExitCode = 1
                DurationSec = $null
                Checks = 0
                Reason = "Required verification marker '$expectedMarker' was not found in task output."
                LogFile = $taskLogFile
            })
        }
    }

    # Evaluate Overall Status
    $passedCount = @($results | Where-Object { $_.Status -eq 'passed' }).Count
    $failedCount = @($results | Where-Object { $_.Status -eq 'failed' }).Count
    $blockedCount = @($results | Where-Object { $_.Status -eq 'blocked' }).Count
    $notRunCount = @($results | Where-Object { $_.Status -eq 'not_run' }).Count

    $offlineSuites = @($results | Where-Object { $_.Phase -eq 'Offline' })
    $offlineFailed = @($offlineSuites | Where-Object { $_.Status -eq 'failed' }).Count
    $offlineNotRun = @($offlineSuites | Where-Object { $_.Status -eq 'not_run' }).Count

    # If any offline suite actively failed, offlineStatus is FAILURE
    # If all offline suites passed, offlineStatus is SUCCESS
    # If some suites were not run (partial run) and none failed, PARTIAL_SUCCESS
    $offlineStatus = if ($offlineFailed -gt 0 -or $GradleExitCode -ne 0) {
        'FAILURE'
    } elseif ($offlineNotRun -eq 0 -and $passedCount -gt 0) {
        'SUCCESS'
    } else {
        'PARTIAL_SUCCESS'
    }

    $acceptanceStatus = if ($blockedCount -gt 0) { 'BLOCKED' } else { 'NOT_RUN' }

    $overallStatus = if ($offlineStatus -eq 'SUCCESS' -and $failedCount -eq 0) {
        if ($Phase -eq 'Offline') {
            'OFFLINE_SUCCESS_ACCEPTANCE_BLOCKED'
        } else {
            'SUCCESS'
        }
    } elseif ($offlineStatus -eq 'PARTIAL_SUCCESS' -and $failedCount -eq 0) {
        'PARTIAL_OFFLINE_SUCCESS'
    } else {
        'FAILURE'
    }

    return [PSCustomObject]@{
        GradleExitCode = $GradleExitCode
        OfflineStatus = $offlineStatus
        AcceptanceStatus = $acceptanceStatus
        OverallStatus = $overallStatus
        PassedCount = $passedCount
        FailedCount = $failedCount
        BlockedCount = $blockedCount
        NotRunCount = $notRunCount
        Suites = $results
    }
}
