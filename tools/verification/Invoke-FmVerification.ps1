<#
.SYNOPSIS
    Forbidden Magic Verification Suite Launcher (Offline / Acceptance).
.DESCRIPTION
    Executes offline JVM checks, codec checks, and content verification tasks.
    Produces per-suite logs and a consolidated JSON & Markdown verification summary.
    Acceptance / Server / Client tests are strictly gated and marked blocked/not_run
    until all B/C functional slices are complete and explicit user approval is granted.
#>
[CmdletBinding()]
param(
    [ValidateSet('Offline', 'Acceptance', 'All')]
    [string]$Phase = 'Offline',

    [string]$ProjectRoot = '',

    [string]$CoreRoot = '',

    [string]$ReportRoot = '',

    [switch]$IncludeAcceptance = $false
)

$ErrorActionPreference = 'Continue'

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
} else {
    $ProjectRoot = (Resolve-Path $ProjectRoot).Path
}

if ([string]::IsNullOrWhiteSpace($ReportRoot)) {
    $ReportRoot = Join-Path $ProjectRoot 'build/reports/verification'
}

New-Item -ItemType Directory -Force -Path $ReportRoot | Out-Null

$gitCommit = ''
try {
    $gitCommit = (git -C $ProjectRoot rev-parse HEAD).Trim()
} catch {
    $gitCommit = 'UNKNOWN'
}

$worktreeBranch = ''
try {
    $worktreeBranch = (git -C $ProjectRoot rev-parse --abbrev-ref HEAD).Trim()
} catch {
    $worktreeBranch = 'UNKNOWN'
}

$gradlew = Join-Path $ProjectRoot 'gradlew.ps1'
if (-not (Test-Path $gradlew)) {
    throw "Gradle wrapper not found at $gradlew"
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Forbidden Magic Verification Launcher" -ForegroundColor Cyan
Write-Host " Phase:            $Phase" -ForegroundColor Cyan
Write-Host " Project Root:     $ProjectRoot" -ForegroundColor Cyan
Write-Host " Branch / Commit:  $worktreeBranch ($gitCommit)" -ForegroundColor Cyan
Write-Host " Report Directory: $ReportRoot" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$testDefinitions = @(
    @{ Id = 'V-RULE-01'; Task = 'foundationRules'; Name = 'Foundation Architecture & Bytecode Rules'; Phase = 'Offline' },
    @{ Id = 'V-CODEC-01'; Task = 'componentCodecCheck'; Name = 'Component Codecs & Persistence Check'; Phase = 'Offline' },
    @{ Id = 'V-P01-NORM'; Task = 'p01ContentNormal'; Name = 'P01 Content Standard Profile'; Phase = 'Offline' },
    @{ Id = 'V-P01-NOLUST'; Task = 'p01ContentNoLust'; Name = 'P01 Content No-Lust Profile'; Phase = 'Offline' },
    @{ Id = 'V-P01-NOCAGE'; Task = 'p01ContentNoCage'; Name = 'P01 Content No-Cage Profile'; Phase = 'Offline' },
    @{ Id = 'V-P02-TOOLS'; Task = 'p02ToolsCheck'; Name = 'P02 Tools & Accessories Mechanics'; Phase = 'Offline' },
    @{ Id = 'V-P02-WAND'; Task = 'p02WandCheck'; Name = 'P02 Wands, Foci & Upgrades Check'; Phase = 'Offline' },
    @{ Id = 'V-P03-CAGE'; Task = 'p03WrathCageCheck'; Name = 'P03 Wrath Cage & Spawner Logic Check'; Phase = 'Offline' },
    @{ Id = 'V-P03-VILLAGER'; Task = 'p03VillagerCheck'; Name = 'P03 Heretic Villager & Study POI Check'; Phase = 'Offline' },
    @{ Id = 'V-SRV-01'; Task = 'serverAcceptance'; Name = 'Dedicated Server Runtime & World Load'; Phase = 'Acceptance' },
    @{ Id = 'V-CLI-01'; Task = 'clientAcceptance'; Name = 'Client Render & Screen Interaction'; Phase = 'Acceptance' }
)

$results = [System.Collections.Generic.List[PSObject]]::new()
$startTime = Get-Date

$offlineDefs = $testDefinitions | Where-Object { $_.Phase -eq 'Offline' }
$offlineTasks = @($offlineDefs | ForEach-Object { $_.Task })

$batchLogFile = Join-Path $ReportRoot 'offline-batch.log'
$batchOutput = @()

if ($Phase -ne 'Acceptance') {
    Write-Host "[*] Executing offline verification suite via Gradle..." -ForegroundColor Yellow
    $taskStart = Get-Date

    Push-Location $ProjectRoot
    try {
        $batchOutput = & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $gradlew $offlineTasks --continue 2>&1 | Tee-Object -FilePath $batchLogFile
    } finally {
        Pop-Location
    }

    $batchDuration = [Math]::Round(((Get-Date) - $taskStart).TotalSeconds, 2)
    Write-Host "[+] Offline suite execution completed in ${batchDuration}s" -ForegroundColor Green
}

$fullOutputText = $batchOutput -join "`n"

foreach ($def in $testDefinitions) {
    $testId = $def.Id
    $task = $def.Task
    $testName = $def.Name
    $targetPhase = $def.Phase

    $taskLogFile = Join-Path $ReportRoot "$task.log"

    if ($targetPhase -eq 'Acceptance') {
        if (-not $IncludeAcceptance) {
            Write-Host "[-] SKIP ${testId}: $testName (BLOCKED: Phase is Offline; live server/client acceptance not permitted)" -ForegroundColor Yellow
            $results.Add([PSCustomObject]@{
                Id = $testId
                Name = $testName
                Task = $task
                Phase = $targetPhase
                Status = 'blocked'
                ExitCode = -1
                DurationSec = 0
                Reason = 'Gated: Client/Server acceptance requires completed P04 and explicit acceptance permission.'
                LogFile = $null
            })
            continue
        }
    }

    # Extract task logs from batchOutput
    $taskLines = [System.Collections.Generic.List[string]]::new()
    $recording = $false
    foreach ($line in $batchOutput) {
        if ($line -match "^> Task :$task\b") {
            $recording = $true
        } elseif ($recording -and $line -match "^> Task :") {
            $recording = $false
        }
        if ($recording) {
            $taskLines.Add($line)
        }
    }
    if ($taskLines.Count -eq 0) {
        $taskLines.Add("Task :$task output from batch run")
        foreach ($line in $batchOutput) {
            if ($line -match $task -or $line -match "_PASS") {
                $taskLines.Add($line)
            }
        }
    }
    [System.IO.File]::WriteAllLines($taskLogFile, $taskLines, [System.Text.Encoding]::UTF8)

    $taskPassed = $fullOutputText -match "(FORBIDDEN_[A-Z0-9_]+_PASS.*> Task :$task|> Task :$task[\s\S]*?FORBIDDEN_[A-Z0-9_]+_PASS)" -or
                  ($fullOutputText -match "BUILD SUCCESSFUL" -and ($fullOutputText -match "> Task :$task\b"))

    if ($taskPassed) {
        Write-Host " [PASS] ${testId}: $testName" -ForegroundColor Green
        $results.Add([PSCustomObject]@{
            Id = $testId
            Name = $testName
            Task = $task
            Phase = $targetPhase
            Status = 'passed'
            ExitCode = 0
            DurationSec = $batchDuration
            Reason = 'All assertions passed'
            LogFile = $taskLogFile
        })
    } else {
        Write-Host " [FAIL] ${testId}: $testName" -ForegroundColor Red
        $results.Add([PSCustomObject]@{
            Id = $testId
            Name = $testName
            Task = $task
            Phase = $targetPhase
            Status = 'failed'
            ExitCode = 1
            DurationSec = $batchDuration
            Reason = "Execution failed or check assertion failed. See log: $taskLogFile"
            LogFile = $taskLogFile
        })
    }
}

$totalDuration = [Math]::Round(((Get-Date) - $startTime).TotalSeconds, 2)
$passedCount = ($results | Where-Object { $_.Status -eq 'passed' }).Count
$failedCount = ($results | Where-Object { $_.Status -eq 'failed' }).Count
$blockedCount = ($results | Where-Object { $_.Status -eq 'blocked' }).Count

$overallStatus = if ($failedCount -eq 0) { 'SUCCESS' } else { 'FAILURE' }

$summary = [PSCustomObject]@{
    Timestamp = (Get-Date -Format 'o')
    Commit = $gitCommit
    Branch = $worktreeBranch
    Phase = $Phase
    DurationSec = $totalDuration
    Total = $results.Count
    Passed = $passedCount
    Failed = $failedCount
    Blocked = $blockedCount
    Overall = $overallStatus
    Suites = $results
}

$jsonPath = Join-Path $ReportRoot 'verification-summary.json'
$jsonContent = $summary | ConvertTo-Json -Depth 5
[System.IO.File]::WriteAllText($jsonPath, $jsonContent, [System.Text.Encoding]::UTF8)

$mdPath = Join-Path $ReportRoot 'VERIFICATION_REPORT.md'
$mdList = [System.Collections.Generic.List[string]]::new()
$mdList.Add("# Forbidden Magic Verification Report")
$mdList.Add("")
$mdList.Add("- **Date / Time:** $($summary.Timestamp)")
$mdList.Add("- **Branch / Commit:** $worktreeBranch ($gitCommit)")
$mdList.Add("- **Phase:** $Phase")
$mdList.Add("- **Duration:** ${totalDuration}s")
$mdList.Add("- **Overall Status:** **$overallStatus** (Passed: $passedCount, Failed: $failedCount, Blocked: $blockedCount)")
$mdList.Add("")
$mdList.Add("## Results by Verification Item")
$mdList.Add("")
$mdList.Add("| ID | Suite / Task | Phase | Status | Duration | Note |")
$mdList.Add("|---|---|---|---|---|---|")

foreach ($r in $results) {
    $statusEmoji = switch ($r.Status) {
        'passed' { '[PASS] PASSED' }
        'failed' { '[FAIL] FAILED' }
        'blocked' { '[SKIP] BLOCKED' }
        default { '[?] NOT RUN' }
    }
    $idStr = $r.Id
    $nameStr = $r.Name
    $taskStr = $r.Task
    $phaseStr = $r.Phase
    $durStr = $r.DurationSec
    $reasonStr = $r.Reason
    $mdList.Add("| $idStr | **$nameStr** (``$taskStr``) | $phaseStr | $statusEmoji | ${durStr}s | $reasonStr |")
}

$mdList.Add("")
$mdList.Add("### Logs Location")
$mdList.Add("Individual logs saved to: $ReportRoot")

[System.IO.File]::WriteAllLines($mdPath, $mdList, [System.Text.Encoding]::UTF8)

Write-Host "==========================================================" -ForegroundColor Cyan
$color = if ($failedCount -eq 0) { [System.ConsoleColor]::Green } else { [System.ConsoleColor]::Red }
Write-Host " Summary: $passedCount passed, $failedCount failed, $blockedCount blocked (${totalDuration}s)" -ForegroundColor $color
Write-Host " JSON:    $jsonPath" -ForegroundColor Cyan
Write-Host " Report:  $mdPath" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

if ($failedCount -gt 0) {
    exit 1
}
exit 0
