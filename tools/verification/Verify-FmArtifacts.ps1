<#
.SYNOPSIS
    Offline verification of compiled Forbidden Magic JAR artifacts.
.DESCRIPTION
    Checks built gameplay JAR and sources JAR:
    - Verifies absence of test, verification, or core classes.
    - Verifies presence of main + client classes and neoforge.mods.toml.
    - Computes and returns SHA-256 hashes of the verified artifacts.
#>
[CmdletBinding()]
param(
    [string]$ProjectRoot = '',
    [switch]$BuildFirst = $false
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
} else {
    $ProjectRoot = (Resolve-Path $ProjectRoot).Path
}

Add-Type -AssemblyName System.IO.Compression.FileSystem

if ($BuildFirst) {
    Write-Host "[*] Building jar and sourcesJar offline..." -ForegroundColor Yellow
    $gradlew = Join-Path $ProjectRoot 'gradlew.ps1'
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $gradlew jar sourcesJar --offline
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to build JARs via Gradle (exit code $LASTEXITCODE)"
    }
}

$buildLibsDir = Join-Path $ProjectRoot 'build/libs'
if (-not (Test-Path $buildLibsDir)) {
    throw "build/libs directory not found at $buildLibsDir. Build artifacts first."
}

$mainJars = Get-ChildItem -Path $buildLibsDir -Filter "ForbiddenMagic-1.21.1-*.jar" | Where-Object { $_.Name -notmatch '-sources\.jar$' }
if ($mainJars.Count -eq 0) {
    throw "No main ForbiddenMagic JAR found in $buildLibsDir"
}
$mainJar = $mainJars[0]

$sourcesJars = Get-ChildItem -Path $buildLibsDir -Filter "ForbiddenMagic-1.21.1-*-sources.jar"
$sourcesJar = if ($sourcesJars.Count -gt 0) { $sourcesJars[0] } else { $null }

function Get-FileSha256($filePath) {
    $sha = [System.Security.Cryptography.SHA256]::Create()
    $stream = [System.IO.File]::OpenRead($filePath)
    try {
        $hashBytes = $sha.ComputeHash($stream)
        return -join ($hashBytes | ForEach-Object { $_.ToString('x2') })
    } finally {
        $stream.Close()
    }
}

$mainHash = Get-FileSha256 $mainJar.FullName
$sourcesHash = if ($sourcesJar) { Get-FileSha256 $sourcesJar.FullName } else { 'NONE' }

Write-Host "Verifying Artifact: $($mainJar.Name)" -ForegroundColor Cyan
Write-Host "  Main JAR SHA-256:    $mainHash" -ForegroundColor Cyan
Write-Host "  Sources JAR SHA-256: $sourcesHash" -ForegroundColor Cyan

$zip = [System.IO.Compression.ZipFile]::OpenRead($mainJar.FullName)
$violations = [System.Collections.Generic.List[string]]::new()
$entryNames = [System.Collections.Generic.HashSet[string]]::new()

try {
    foreach ($entry in $zip.Entries) {
        $name = $entry.FullName
        $entryNames.Add($name) | Out-Null

        # Check for forbidden payloads
        if ($name -match '^forbidden/rules/' -or $name -match '^forbidden/verification/') {
            $violations.Add("Forbidden test/verification class present in release JAR: $name")
        }
        if ($name -match '^thaumcraft/' -or $name -match '^net/minecraft/' -or $name -match '^net/neoforged/') {
            $violations.Add("Core/runtime class leaked into addon JAR: $name")
        }
        if ($name -match '^\.git/' -or $name -match '^runs/' -or $name -match '\.log$') {
            $violations.Add("Private/temporary file leaked into addon JAR: $name")
        }
    }

    # Verify essential files exist
    if (-not $entryNames.Contains('META-INF/neoforge.mods.toml')) {
        $violations.Add("Missing META-INF/neoforge.mods.toml in JAR")
    }
    if (-not $entryNames.Contains('forbidden/content/ForbiddenContent.class')) {
        $violations.Add("Missing forbidden/content/ForbiddenContent.class in JAR")
    }
    if (-not $entryNames.Contains('forbidden/content/HereticStudyBlock.class')) {
        $violations.Add("Missing forbidden/content/HereticStudyBlock.class in JAR")
    }
    if (-not $entryNames.Contains('assets/forbidden/blockstates/heretic_study.json')) {
        $violations.Add("Missing assets/forbidden/blockstates/heretic_study.json in JAR")
    }
    if (-not $entryNames.Contains('data/forbidden/recipe/heretic_study.json')) {
        $violations.Add("Missing data/forbidden/recipe/heretic_study.json in JAR")
    }
} finally {
    $zip.Dispose()
}

$passed = ($violations.Count -eq 0)

$report = [PSCustomObject]@{
    MainJarName = $mainJar.Name
    MainJarPath = $mainJar.FullName
    MainJarSha256 = $mainHash
    MainJarSizeBytes = $mainJar.Length
    SourcesJarName = if ($sourcesJar) { $sourcesJar.Name } else { $null }
    SourcesJarSha256 = $sourcesHash
    Passed = $passed
    Violations = $violations
    TotalEntries = $entryNames.Count
}

if (-not $passed) {
    Write-Host "[-] JAR Artifact Verification FAILED with $($violations.Count) violations:" -ForegroundColor Red
    foreach ($v in $violations) {
        Write-Host "    - $v" -ForegroundColor Red
    }
    exit 1
} else {
    Write-Host "[+] JAR Artifact Verification PASSED ($($entryNames.Count) entries verified, zero violations)." -ForegroundColor Green
    Write-Output ($report | ConvertTo-Json -Compress)
    exit 0
}
