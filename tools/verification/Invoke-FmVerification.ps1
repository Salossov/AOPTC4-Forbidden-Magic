<#
.SYNOPSIS
    Forbidden Magic Verification Suite Launcher (Offline / Acceptance).
.DESCRIPTION
    Executes offline JVM checks, codec checks, and content verification tasks.
    Produces per-suite isolated logs and a consolidated JSON & Markdown verification summary.
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

    [string[]]$Tasks = @(),

    [switch]$IncludeAcceptance = $false,

    [switch]$VerifyArtifacts = $false
)

$ErrorActionPreference = 'Continue'
. (Join-Path $PSScriptRoot 'FmVerificationParser.ps1')

# 1. Resolve Project Root
if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
} else {
    $ProjectRoot = (Resolve-Path $ProjectRoot).Path
}

# 2. Resolve Core Root
if ([string]::IsNullOrWhiteSpace($CoreRoot)) {
    $candidates = @(
        (Join-Path $ProjectRoot '../../repo'),
        (Join-Path $ProjectRoot '../../../repo'),
        (Join-Path $ProjectRoot '../../../../../repo')
    )
    foreach ($cand in $candidates) {
        if (Test-Path (Join-Path $cand 'api/src/main/java/thaumcraft/api')) {
            $CoreRoot = (Resolve-Path $cand).Path
            break
        }
    }
} else {
    $CoreRoot = (Resolve-Path $CoreRoot).Path
}

if ([string]::IsNullOrWhiteSpace($CoreRoot) -or (-not (Test-Path $CoreRoot))) {
    Write-Warning "Core root not found or unverified: $CoreRoot"
}

# 3. Resolve Report Root
if ([string]::IsNullOrWhiteSpace($ReportRoot)) {
    $ReportRoot = Join-Path $ProjectRoot 'build/reports/verification'
}
New-Item -ItemType Directory -Force -Path $ReportRoot | Out-Null

# 4. Git Provenance
$gitCommit = 'UNKNOWN'
$worktreeBranch = 'UNKNOWN'
$isDirty = $false
$diffHash = '0000000000000000000000000000000000000000'

try {
    $gitCommit = (git -C $ProjectRoot rev-parse HEAD).Trim()
    $worktreeBranch = (git -C $ProjectRoot rev-parse --abbrev-ref HEAD).Trim()
    $statusOutput = git -C $ProjectRoot status --porcelain
    if (-not [string]::IsNullOrWhiteSpace($statusOutput)) {
        $isDirty = $true
        $diffRaw = git -C $ProjectRoot diff
        $sha = [System.Security.Cryptography.SHA256]::Create()
        $diffBytes = [System.Text.Encoding]::UTF8.GetBytes($diffRaw)
        $diffHash = -join ($sha.ComputeHash($diffBytes) | ForEach-Object { $_.ToString('x2') })
    }
} catch {
    Write-Warning "Could not query git provenance: $_"
}

# 5. Dependency Hashes
$dependencyHashes = @{}
if (-not [string]::IsNullOrWhiteSpace($CoreRoot)) {
    $apiJars = Get-ChildItem -Path (Join-Path $CoreRoot 'api/build/libs') -Filter "thaumcraft-api-*.jar" -ErrorAction SilentlyContinue
    if ($apiJars -and $apiJars.Count -gt 0) {
        $sha = [System.Security.Cryptography.SHA256]::Create()
        $stream = [System.IO.File]::OpenRead($apiJars[0].FullName)
        try {
            $dependencyHashes[$apiJars[0].Name] = -join ($sha.ComputeHash($stream) | ForEach-Object { $_.ToString('x2') })
        } finally {
            $stream.Close()
        }
    }
}

$gradlew = Join-Path $ProjectRoot 'gradlew.ps1'
if (-not (Test-Path $gradlew)) {
    throw "Gradle wrapper not found at $gradlew"
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Forbidden Magic Verification Launcher" -ForegroundColor Cyan
Write-Host " Phase:            $Phase" -ForegroundColor Cyan
Write-Host " Project Root:     $ProjectRoot" -ForegroundColor Cyan
Write-Host " Core Root:        $CoreRoot" -ForegroundColor Cyan
Write-Host " Branch / Commit:  $worktreeBranch ($gitCommit)" -ForegroundColor Cyan
Write-Host " Dirty / Diff:     $isDirty ($diffHash)" -ForegroundColor Cyan
Write-Host " Report Directory: $ReportRoot" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$testDefinitions = @(
    @{ Id = 'V-RULE-01'; Task = 'foundationRules'; Name = 'Foundation ContentGates Cross-Product Rules'; Marker = 'FORBIDDEN_FOUNDATION_RULES_PASS'; Phase = 'Offline' },
    @{ Id = 'V-CODEC-01'; Task = 'componentCodecCheck'; Name = 'Component Codecs & Persistence Check'; Marker = 'FORBIDDEN_COMPONENT_CODECS_PASS'; Phase = 'Offline' },
    @{ Id = 'V-P01-NORM'; Task = 'p01ContentNormal'; Name = 'P01 Content Standard Profile'; Marker = 'FORBIDDEN_P01_CONTENT_PASS noLust=false cage=true'; Phase = 'Offline' },
    @{ Id = 'V-P01-NOLUST'; Task = 'p01ContentNoLust'; Name = 'P01 Content No-Lust Profile'; Marker = 'FORBIDDEN_P01_CONTENT_PASS noLust=true cage=true'; Phase = 'Offline' },
    @{ Id = 'V-P01-NOCAGE'; Task = 'p01ContentNoCage'; Name = 'P01 Content No-Cage Profile'; Marker = 'FORBIDDEN_P01_CONTENT_PASS noLust=false cage=false'; Phase = 'Offline' },
    @{ Id = 'V-P02-TOOLS'; Task = 'p02ToolsCheck'; Name = 'P02 Tools & Accessories Mechanics'; Marker = 'FORBIDDEN_P02_TOOLS_PASS'; Phase = 'Offline' },
    @{ Id = 'V-P02-WAND'; Task = 'p02WandCheck'; Name = 'P02 Wands, Foci & Upgrades Check'; Marker = 'FORBIDDEN_P02_WAND_PASS'; Phase = 'Offline' },
    @{ Id = 'V-P03-CAGE'; Task = 'p03WrathCageCheck'; Name = 'P03 Wrath Cage & Spawner Logic Check'; Marker = 'FORBIDDEN_P03_WRATH_CAGE_PASS'; Phase = 'Offline' },
    @{ Id = 'V-P03-VILLAGER'; Task = 'p03VillagerCheck'; Name = 'P03 Heretic Villager & Study POI Check'; Marker = 'FORBIDDEN_P03_VILLAGER_PASS'; Phase = 'Offline' },
    @{ Id = 'V-SRV-01'; Task = 'serverAcceptance'; Name = 'Dedicated Server Runtime & World Load'; Marker = 'ACC_SERVER_PASS'; Phase = 'Acceptance' },
    @{ Id = 'V-CLI-01'; Task = 'clientAcceptance'; Name = 'Client Render & Screen Interaction'; Marker = 'ACC_CLIENT_PASS'; Phase = 'Acceptance' }
)

$targetTasks = if ($Tasks.Count -gt 0) {
    $Tasks
} else {
    @($testDefinitions | Where-Object { $_.Phase -eq 'Offline' } | ForEach-Object { $_.Task })
}

$batchLogFile = Join-Path $ReportRoot 'offline-batch.log'
$batchOutput = @()
$gradleExitCode = 0
$batchDuration = 0
$startTime = Get-Date

if ($Phase -ne 'Acceptance') {
    Write-Host "[*] Executing offline verification suite via Gradle (batch: $($targetTasks -join ', '))..." -ForegroundColor Yellow
    $taskStart = Get-Date

    $gradleArgs = [System.Collections.Generic.List[string]]::new()
    if (-not [string]::IsNullOrWhiteSpace($CoreRoot)) {
        $gradleArgs.Add("-PcoreRoot=$CoreRoot")
    }
    foreach ($t in $targetTasks) {
        $gradleArgs.Add($t)
    }
    $gradleArgs.Add('--continue')

    Push-Location $ProjectRoot
    try {
        $batchOutput = & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $gradlew @gradleArgs 2>&1
        $gradleExitCode = $LASTEXITCODE
        [System.IO.File]::WriteAllLines($batchLogFile, [string[]]$batchOutput, [System.Text.Encoding]::UTF8)
        foreach ($line in $batchOutput) {
            Write-Host $line
        }
    } finally {
        Pop-Location
    }

    $batchDuration = [Math]::Round(((Get-Date) - $taskStart).TotalSeconds, 2)
    $finishColor = if ($gradleExitCode -eq 0) { [System.ConsoleColor]::Green } else { [System.ConsoleColor]::Red }
    Write-Host "[+] Gradle execution finished with exit code $gradleExitCode in ${batchDuration}s" -ForegroundColor $finishColor
}

# Parse execution output with robust parser
$evalResult = Parse-FmGradleOutput `
    -OutputLines $batchOutput `
    -GradleExitCode $gradleExitCode `
    -TestDefinitions $testDefinitions `
    -Phase $Phase `
    -IncludeAcceptance $IncludeAcceptance `
    -ReportRoot $ReportRoot

$totalDuration = [Math]::Round(((Get-Date) - $startTime).TotalSeconds, 2)

# Optional Artifact Verification
$artifactReport = $null
if ($VerifyArtifacts) {
    $artifactScript = Join-Path $PSScriptRoot 'Verify-FmArtifacts.ps1'
    if (Test-Path $artifactScript) {
        Write-Host "[*] Performing artifact inspection on built JARs..." -ForegroundColor Yellow
        try {
            $artifactReport = & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $artifactScript -ProjectRoot $ProjectRoot
        } catch {
            Write-Warning "Artifact verification encountered error: $_"
        }
    }
}

$summary = [PSCustomObject]@{
    Timestamp = (Get-Date -Format 'o')
    Commit = $gitCommit
    Branch = $worktreeBranch
    ProjectRoot = $ProjectRoot
    CoreRoot = $CoreRoot
    IsDirty = $isDirty
    DiffHash = $diffHash
    DependencyHashes = $dependencyHashes
    Phase = $Phase
    BatchDurationSec = $batchDuration
    TotalDurationSec = $totalDuration
    GradleExitCode = $gradleExitCode
    OfflineStatus = $evalResult.OfflineStatus
    AcceptanceStatus = $evalResult.AcceptanceStatus
    OverallStatus = $evalResult.OverallStatus
    TotalSuites = $evalResult.Suites.Count
    PassedSuites = $evalResult.PassedCount
    FailedSuites = $evalResult.FailedCount
    BlockedSuites = $evalResult.BlockedCount
    NotRunSuites = $evalResult.NotRunCount
    Suites = $evalResult.Suites
    ArtifactReport = $artifactReport
}

$jsonPath = Join-Path $ReportRoot 'verification-summary.json'
$jsonContent = $summary | ConvertTo-Json -Depth 6
[System.IO.File]::WriteAllText($jsonPath, $jsonContent, [System.Text.Encoding]::UTF8)

# Also write dated copy to prevent loss across runs
$datedFile = Join-Path $ReportRoot ("verification-summary-" + (Get-Date -Format 'yyyyMMdd-HHmmss') + ".json")
[System.IO.File]::WriteAllText($datedFile, $jsonContent, [System.Text.Encoding]::UTF8)

# Generate Markdown Report
$mdPath = Join-Path $ReportRoot 'VERIFICATION_REPORT.md'
$mdList = [System.Collections.Generic.List[string]]::new()
$mdList.Add("# Forbidden Magic Verification Report")
$mdList.Add("")
$mdList.Add("- **Date / Time:** $($summary.Timestamp)")
$mdList.Add("- **Checkout / Worktree:** '$ProjectRoot'")
$mdList.Add("- **Branch / Commit:** '$worktreeBranch' ('$gitCommit') | Dirty: $isDirty (Diff SHA: '$diffHash')")
$mdList.Add("- **Core Root:** '$CoreRoot'")
$mdList.Add("- **Phase:** $Phase | **Batch Duration:** ${batchDuration}s | **Total Duration:** ${totalDuration}s")
$mdList.Add("- **Offline Status:** **$($summary.OfflineStatus)**")
$mdList.Add("- **Acceptance Status:** **$($summary.AcceptanceStatus)** (Live client/server tests remain gated)")
$mdList.Add("- **Overall Status:** **$($summary.OverallStatus)**")
$mdList.Add("- **Gradle Child Exit Code:** $gradleExitCode")
$mdList.Add("")
$mdList.Add("## Results by Verification Suite")
$mdList.Add("")
$mdList.Add("| ID | Suite / Task | Phase | Status | Checks | Duration | Note |")
$mdList.Add("|---|---|---|---|---|---|---|")

foreach ($r in $evalResult.Suites) {
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
    $chkStr = if ($r.Checks -gt 0) { $r.Checks } else { '-' }
    $durStr = if ($null -ne $r.DurationSec) { "${durStr}s" } else { "batch (${batchDuration}s total)" }
    $reasonStr = $r.Reason
    $mdList.Add("| $idStr | **$nameStr** (`$taskStr`) | $phaseStr | $statusEmoji | $chkStr | $durStr | $reasonStr |")
}

$mdList.Add("")
$mdList.Add("### Verification Provenance & Logs")
$mdList.Add("- **Batch Log:** '$batchLogFile'")
$mdList.Add("- **Per-suite Task Logs:** '$ReportRoot/<task>.log'")
$mdList.Add("- **Summary JSON:** '$jsonPath'")
if ($dependencyHashes.Count -gt 0) {
    $mdList.Add("- **Dependencies:**")
    foreach ($dep in $dependencyHashes.Keys) {
        $depHash = $dependencyHashes[$dep]
        $mdList.Add("  - '$dep': '$depHash'")
    }
}

[System.IO.File]::WriteAllLines($mdPath, $mdList, [System.Text.Encoding]::UTF8)

Write-Host "==========================================================" -ForegroundColor Cyan
$color = if ($evalResult.OverallStatus -match 'SUCCESS') { [System.ConsoleColor]::Green } else { [System.ConsoleColor]::Red }
Write-Host " Summary: $($evalResult.PassedCount) passed, $($evalResult.FailedCount) failed, $($evalResult.BlockedCount) blocked, $($evalResult.NotRunCount) not run" -ForegroundColor $color
Write-Host " Report written to: $mdPath" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

if ($evalResult.OverallStatus -eq 'FAILURE' -or $evalResult.FailedCount -gt 0 -or $gradleExitCode -ne 0) {
    exit 1
} else {
    exit 0
}
