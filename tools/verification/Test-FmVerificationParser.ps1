<#
.SYNOPSIS
    Regression tests for FmVerificationParser.
#>

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'FmVerificationParser.ps1')

$sampleDefs = @(
    @{ Id = 'V-A'; Task = 'taskA'; Name = 'Suite A'; Marker = 'MARKER_A_PASS'; Phase = 'Offline' },
    @{ Id = 'V-B'; Task = 'taskB'; Name = 'Suite B'; Marker = 'MARKER_B_PASS'; Phase = 'Offline' },
    @{ Id = 'V-ACC'; Task = 'serverAcceptance'; Name = 'Server Acceptance'; Marker = 'ACC_PASS'; Phase = 'Acceptance' }
)

$allTestsPassed = $true

function Assert-Condition($name, $condition, $message) {
    if ($condition) {
        Write-Host "  [PASS] $name" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] ${name}: $message" -ForegroundColor Red
        $script:allTestsPassed = $false
    }
}

Write-Host "Running FmVerificationParser Regression Tests..." -ForegroundColor Cyan

# Test Case 1: A failed, then B passed -> A failed, B passed, overall failure
Write-Host "Case 1: A failed, then B passed" -ForegroundColor Yellow
$fixture1 = @(
    "> Task :taskA FAILED",
    "java.lang.AssertionError: expected true but was false",
    "> Task :taskB",
    "MARKER_B_PASS checks=42",
    "BUILD FAILED in 5s"
)
$res1 = Parse-FmGradleOutput -OutputLines $fixture1 -GradleExitCode 1 -TestDefinitions $sampleDefs
$suiteA1 = $res1.Suites | Where-Object { $_.Task -eq 'taskA' }
$suiteB1 = $res1.Suites | Where-Object { $_.Task -eq 'taskB' }
Assert-Condition "Case 1: Suite A failed" ($suiteA1.Status -eq 'failed') "Expected taskA to fail"
Assert-Condition "Case 1: Suite B passed" ($suiteB1.Status -eq 'passed' -and $suiteB1.Checks -eq 42) "Expected taskB to pass"
Assert-Condition "Case 1: Overall failure" ($res1.OverallStatus -eq 'FAILURE') "Expected overall status FAILURE"
Assert-Condition "Case 1: Exit code is non-zero" ($res1.GradleExitCode -ne 0) "Expected non-zero exit code"

# Test Case 2: A passed, then B failed -> A passed, B failed, overall failure
Write-Host "Case 2: A passed, then B failed" -ForegroundColor Yellow
$fixture2 = @(
    "> Task :taskA",
    "MARKER_A_PASS checks=10",
    "> Task :taskB FAILED",
    "Exception in thread 'main' java.lang.RuntimeException: failed",
    "BUILD FAILED in 6s"
)
$res2 = Parse-FmGradleOutput -OutputLines $fixture2 -GradleExitCode 1 -TestDefinitions $sampleDefs
$suiteA2 = $res2.Suites | Where-Object { $_.Task -eq 'taskA' }
$suiteB2 = $res2.Suites | Where-Object { $_.Task -eq 'taskB' }
Assert-Condition "Case 2: Suite A passed" ($suiteA2.Status -eq 'passed') "Expected taskA to pass"
Assert-Condition "Case 2: Suite B failed" ($suiteB2.Status -eq 'failed') "Expected taskB to fail"
Assert-Condition "Case 2: Overall failure" ($res2.OverallStatus -eq 'FAILURE') "Expected overall status FAILURE"

# Test Case 3: Failed to launch Gradle / prerequisites failed -> dependent checks not_run/blocked, overall failure
Write-Host "Case 3: Failed to launch Gradle / prerequisites failed" -ForegroundColor Yellow
$fixture3 = @(
    "Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain"
)
$res3 = Parse-FmGradleOutput -OutputLines $fixture3 -GradleExitCode 1 -TestDefinitions $sampleDefs
$suiteA3 = $res3.Suites | Where-Object { $_.Task -eq 'taskA' }
Assert-Condition "Case 3: Suite A marked not_run" ($suiteA3.Status -eq 'not_run') "Expected taskA to be not_run"
Assert-Condition "Case 3: Overall failure" ($res3.OverallStatus -eq 'FAILURE') "Expected overall status FAILURE"

# Test Case 4: Task name exists, its marker is missing -> NOT passed
Write-Host "Case 4: Task header exists, marker missing" -ForegroundColor Yellow
$fixture4 = @(
    "> Task :taskA",
    "Starting tests...",
    "Done without marker",
    "> Task :taskB",
    "MARKER_B_PASS checks=5",
    "BUILD SUCCESSFUL in 4s"
)
$res4 = Parse-FmGradleOutput -OutputLines $fixture4 -GradleExitCode 0 -TestDefinitions $sampleDefs
$suiteA4 = $res4.Suites | Where-Object { $_.Task -eq 'taskA' }
Assert-Condition "Case 4: Suite A without marker failed" ($suiteA4.Status -eq 'failed') "Expected taskA to fail due to missing marker"
Assert-Condition "Case 4: Overall failure" ($res4.OverallStatus -eq 'FAILURE') "Expected overall status FAILURE when a suite fails marker check"

# Test Case 5: Another task's _PASS, followed by failure of tested task -> tested task failed
# (The exact false-PASS bug reproducing lines 154-155 of the old launcher)
Write-Host "Case 5: Alien _PASS, then failure of tested task" -ForegroundColor Yellow
$fixture5 = @(
    "> Task :taskA",
    "MARKER_A_PASS checks=10",
    "> Task :taskB FAILED",
    "AssertionError: failed in taskB",
    "BUILD FAILED in 5s"
)
$res5 = Parse-FmGradleOutput -OutputLines $fixture5 -GradleExitCode 1 -TestDefinitions $sampleDefs
$suiteB5 = $res5.Suites | Where-Object { $_.Task -eq 'taskB' }
Assert-Condition "Case 5: Suite B is strictly failed despite preceding _PASS" ($suiteB5.Status -eq 'failed') "taskB must be failed!"
Assert-Condition "Case 5: Suite B did not borrow taskA's marker" ($suiteB5.Checks -eq 0) "taskB checks should be 0"

# Test Case 6: All mandatory offline suites passed -> success of offline phase only
Write-Host "Case 6: All mandatory offline suites passed" -ForegroundColor Yellow
$fixture6 = @(
    "> Task :taskA",
    "MARKER_A_PASS checks=100",
    "> Task :taskB",
    "MARKER_B_PASS checks=200",
    "BUILD SUCCESSFUL in 8s"
)
$res6 = Parse-FmGradleOutput -OutputLines $fixture6 -GradleExitCode 0 -TestDefinitions $sampleDefs -Phase 'Offline'
$suiteA6 = $res6.Suites | Where-Object { $_.Task -eq 'taskA' }
$suiteB6 = $res6.Suites | Where-Object { $_.Task -eq 'taskB' }
$suiteAcc6 = $res6.Suites | Where-Object { $_.Task -eq 'serverAcceptance' }
Assert-Condition "Case 6: Suite A passed" ($suiteA6.Status -eq 'passed' -and $suiteA6.Checks -eq 100) "Expected taskA passed"
Assert-Condition "Case 6: Suite B passed" ($suiteB6.Status -eq 'passed' -and $suiteB6.Checks -eq 200) "Expected taskB passed"
Assert-Condition "Case 6: Offline phase status is SUCCESS" ($res6.OfflineStatus -eq 'SUCCESS') "Expected Offline SUCCESS"
Assert-Condition "Case 6: Overall status clearly denotes offline success with acceptance blocked" ($res6.OverallStatus -eq 'OFFLINE_SUCCESS_ACCEPTANCE_BLOCKED') "Expected OFFLINE_SUCCESS_ACCEPTANCE_BLOCKED"
Assert-Condition "Case 6: Acceptance suite is blocked" ($suiteAcc6.Status -eq 'blocked') "Expected acceptance to remain blocked"

# Test Case 7: Acceptance not allowed -> game not launched, blocked/not_run
Write-Host "Case 7: Acceptance phase alone" -ForegroundColor Yellow
$res7 = Parse-FmGradleOutput -OutputLines @() -GradleExitCode 0 -TestDefinitions $sampleDefs -Phase 'Acceptance'
$suiteA7 = $res7.Suites | Where-Object { $_.Task -eq 'taskA' }
$suiteAcc7 = $res7.Suites | Where-Object { $_.Task -eq 'serverAcceptance' }
Assert-Condition "Case 7: Offline suites marked not_run" ($suiteA7.Status -eq 'not_run') "Expected taskA not_run in Acceptance phase"
Assert-Condition "Case 7: Acceptance suite marked blocked" ($suiteAcc7.Status -eq 'blocked') "Expected serverAcceptance blocked"

if (-not $allTestsPassed) {
    Write-Error "One or more regression tests failed!"
    exit 1
} else {
    Write-Host "ALL 7 REGRESSION CASES PASSED SUCCESSFULLY." -ForegroundColor Green
}
