<#
.SYNOPSIS
    Negative regression tests for Verify-FmArtifacts.ps1.
    Tests that defect/corrupted JARs are strictly detected and rejected with non-zero exit code.
#>

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$verifyScript = (Resolve-Path (Join-Path $PSScriptRoot 'Verify-FmArtifacts.ps1')).Path
$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("fm_artifact_test_" + [System.Guid]::NewGuid().ToString('N'))
[System.IO.Directory]::CreateDirectory($tempDir) | Out-Null

$allPassed = $true

function Assert-ArtifactTest($name, $condition, $msg) {
    if ($condition) {
        Write-Host "  [PASS] $name" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] ${name}: $msg" -ForegroundColor Red
        $script:allPassed = $false
    }
}

try {
    Write-Host "Running Negative Artifact Regression Tests..." -ForegroundColor Cyan

    # Helper: Create test zip
    function New-TestJar($path, $entries) {
        if (Test-Path $path) { Remove-Item $path -Force }
        $archive = [System.IO.Compression.ZipFile]::Open($path, [System.IO.Compression.ZipArchiveMode]::Create)
        try {
            foreach ($entryName in $entries) {
                $e = $archive.CreateEntry($entryName)
                $stream = $e.Open()
                $bytes = [System.Text.Encoding]::UTF8.GetBytes("dummy content for $entryName")
                $stream.Write($bytes, 0, $bytes.Length)
                $stream.Dispose()
            }
        } finally {
            $archive.Dispose()
        }
    }

    $validEntries = @(
        'META-INF/neoforge.mods.toml',
        'forbidden/content/ForbiddenContent.class',
        'forbidden/content/HereticStudyBlock.class',
        'assets/forbidden/blockstates/heretic_study.json',
        'data/forbidden/recipe/heretic_study.json'
    )

    # 1. Defect: Leaked test/rules class
    Write-Host "Artifact Case 1: Leaked test class" -ForegroundColor Yellow
    $badJar1 = Join-Path $tempDir 'ForbiddenMagic-1.21.1-leaked-rules.jar'
    New-TestJar $badJar1 ($validEntries + @('forbidden/rules/SneakyRuleTest.class'))
    $p1 = Start-Process powershell -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "`"$verifyScript`"", "-SpecificJarPath", "`"$badJar1`"" -WindowStyle Hidden -Wait -PassThru
    Assert-ArtifactTest "Case 1: Exit code is non-zero" ($p1.ExitCode -ne 0) "Expected non-zero exit code"

    # 2. Defect: Leaked core thaumcraft class
    Write-Host "Artifact Case 2: Leaked core runtime class" -ForegroundColor Yellow
    $badJar2 = Join-Path $tempDir 'ForbiddenMagic-1.21.1-leaked-core.jar'
    New-TestJar $badJar2 ($validEntries + @('thaumcraft/api/aspects/Aspect.class'))
    $p2 = Start-Process powershell -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "`"$verifyScript`"", "-SpecificJarPath", "`"$badJar2`"" -WindowStyle Hidden -Wait -PassThru
    Assert-ArtifactTest "Case 2: Exit code is non-zero" ($p2.ExitCode -ne 0) "Expected non-zero exit code"

    # 3. Defect: Missing mods.toml
    Write-Host "Artifact Case 3: Missing mods.toml" -ForegroundColor Yellow
    $badJar3 = Join-Path $tempDir 'ForbiddenMagic-1.21.1-missing-toml.jar'
    $withoutToml = @($validEntries | Where-Object { $_ -ne 'META-INF/neoforge.mods.toml' })
    New-TestJar $badJar3 $withoutToml
    $p3 = Start-Process powershell -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "`"$verifyScript`"", "-SpecificJarPath", "`"$badJar3`"" -WindowStyle Hidden -Wait -PassThru
    Assert-ArtifactTest "Case 3: Exit code is non-zero" ($p3.ExitCode -ne 0) "Expected non-zero exit code"

    # 4. New live acceptance harness must never ship with gameplay.
    $badJar4 = Join-Path $tempDir 'ForbiddenMagic-1.21.1-leaked-acceptance.jar'
    New-TestJar $badJar4 ($validEntries + @('forbidden/acceptance/ServerAcceptance.class'))
    $p4 = Start-Process powershell -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "`"$verifyScript`"", "-SpecificJarPath", "`"$badJar4`"" -WindowStyle Hidden -Wait -PassThru -RedirectStandardOutput (Join-Path $tempDir 'acceptance-leak.log')
    Assert-ArtifactTest "Case 4: Acceptance harness rejected" ($p4.ExitCode -ne 0) "Expected non-zero exit code"

} finally {
    if (Test-Path $tempDir) {
        $resolvedTemporary = [IO.Path]::GetFullPath($tempDir)
        $allowedTemporary = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\') + '\'
        if (-not $resolvedTemporary.StartsWith($allowedTemporary, [StringComparison]::OrdinalIgnoreCase) -or [IO.Path]::GetFileName($resolvedTemporary) -notlike 'fm_artifact_test_*') { throw 'Refusing cleanup outside test-owned temporary directory.' }
        Remove-Item -LiteralPath $resolvedTemporary -Recurse -Force -ErrorAction SilentlyContinue
    }
}

if (-not $allPassed) {
    Write-Error "Negative artifact regressions failed!"
    exit 1
} else {
    Write-Host "ALL 4 NEGATIVE ARTIFACT REGRESSIONS PASSED." -ForegroundColor Green
}
