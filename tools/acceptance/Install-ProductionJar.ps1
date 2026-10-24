<#
.SYNOPSIS
    Acceptance Preparation: Production Deployment Installer.
.DESCRIPTION
    Packages and sets up a production server environment outside dev:
    - Target: build/install/forbidden-production/
    - Copies ForbiddenMagic release JAR.
    - Copies pinned dependency JARs (Thaumcraft, API, Curios).
    - Sets default server configurations.
    - Strictly creates eula.txt with eula=false (NEVER automatically accepts EULA).
    - Generates launch script requiring explicit manual confirmation.
#>
[CmdletBinding()]
param(
    [string]$ProjectRoot = '',
    [string]$InstallDir = ''
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
}

if ([string]::IsNullOrWhiteSpace($InstallDir)) {
    $InstallDir = Join-Path $ProjectRoot 'build/install/forbidden-production'
}

New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
$modsDir = Join-Path $InstallDir 'mods'
$configDir = Join-Path $InstallDir 'config'
New-Item -ItemType Directory -Force -Path $modsDir, $configDir | Out-Null

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Forbidden Magic Production Installer Preparation" -ForegroundColor Cyan
Write-Host " Target Directory: $InstallDir" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Main Gameplay JAR
$buildLibs = Join-Path $ProjectRoot 'build/libs'
$mainJars = Get-ChildItem -Path $buildLibs -Filter "ForbiddenMagic-1.21.1-*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch '-sources\.jar$' }
if ($mainJars -and $mainJars.Count -gt 0) {
    Copy-Item -Path $mainJars[0].FullName -Destination $modsDir -Force
    Write-Host "[+] Copied release JAR: $($mainJars[0].Name)" -ForegroundColor Green
} else {
    Write-Warning "Main JAR not found in $buildLibs. Run build first."
}

# 1b. First-Wave and Cross-Mod Dependencies Copying
$workspaceRootCandidates = @(
    (Resolve-Path (Join-Path $ProjectRoot '../../..') -ErrorAction SilentlyContinue),
    (Resolve-Path (Join-Path $ProjectRoot '../../../../..') -ErrorAction SilentlyContinue)
) | Where-Object { $_ -ne $null } | ForEach-Object { $_.Path }

$workspaceRoot = $workspaceRootCandidates | Where-Object { Test-Path (Join-Path $_ 'repo/api') } | Select-Object -First 1

$depCandidates = @()
if ($workspaceRoot) {
    $depCandidates += (Join-Path $workspaceRoot 'repo/mod/build/libs')
    $depCandidates += (Join-Path $workspaceRoot 'repo/api/build/libs')
    $depCandidates += (Join-Path $workspaceRoot 'port_addons/Thaumic_Tinkerer/repo/build/libs')
    $depCandidates += (Join-Path $workspaceRoot 'port_addons/Thaumic_Tinkerer/analysis/compat_reference')
}

$copiedCount = 0
foreach ($dir in $depCandidates) {
    if (Test-Path $dir) {
        $jars = Get-ChildItem -Path $dir -Recurse -Filter "*.jar" -ErrorAction SilentlyContinue | Where-Object {
            $_.Name -notmatch '-sources\.jar$' -and
            ($_.Name -match 'thaumcraft' -or $_.Name -match 'ThaumicTinkerer' -or $_.Name -match 'curios' -or $_.Name -match 'botania' -or $_.Name -match 'bloodmagic')
        }
        foreach ($j in $jars) {
            $targetPath = Join-Path $modsDir $j.Name
            if (-not (Test-Path $targetPath)) {
                Copy-Item -Path $j.FullName -Destination $targetPath -Force
                Write-Host "[+] Copied dependency JAR: $($j.Name)" -ForegroundColor Green
                $copiedCount++
            }
        }
    }
}
Write-Host "[*] Total dependency JARs staged: $copiedCount" -ForegroundColor Cyan

# 2. Strict EULA enforcement: eula=false
$eulaFile = Join-Path $InstallDir 'eula.txt'
$eulaContent = @(
    "# By changing the setting below to TRUE you are indicating your agreement to the Minecraft EULA.",
    "# Rule: universal-modder strictly forbids automated EULA acceptance.",
    "eula=false"
)
[System.IO.File]::WriteAllLines($eulaFile, $eulaContent, [System.Text.Encoding]::UTF8)
Write-Host "[+] Generated eula.txt with eula=false (Strict manual agreement required)" -ForegroundColor Green

# 3. Default server properties
$serverPropsFile = Join-Path $InstallDir 'server.properties'
$propsContent = @(
    "server-port=25565",
    "online-mode=false",
    "spawn-protection=0",
    "view-distance=10",
    "difficulty=easy",
    "gamemode=survival"
)
[System.IO.File]::WriteAllLines($serverPropsFile, $propsContent, [System.Text.Encoding]::UTF8)

# 4. Generate runner script with NeoForge bootstrap detection and manual confirmation
$runnerFile = Join-Path $InstallDir 'run-server.ps1'
$runnerContent = @(
    "# Dedicated Server Runner for Production Acceptance",
    "`$eula = Get-Content eula.txt | Where-Object { `$_ -match '^eula=(true|false)' }",
    "if (`$eula -notmatch 'eula=true') {",
    "    Write-Host '[-] EULA is not accepted. Edit eula.txt manually if you agree to the Minecraft EULA.' -ForegroundColor Red",
    "    exit 1",
    "}",
    "if (Test-Path 'run.ps1') {",
    "    Write-Host '[*] Launching NeoForge Dedicated Server via official run.ps1 bootstrap...' -ForegroundColor Yellow",
    "    & .\run.ps1 `$args",
    "} elseif (Test-Path 'run.bat') {",
    "    Write-Host '[*] Launching NeoForge Dedicated Server via official run.bat bootstrap...' -ForegroundColor Yellow",
    "    & .\run.bat `$args",
    "} else {",
    "    Write-Host '[-] Official NeoForge runtime bootstrap (run.ps1 / run.bat) not detected in this folder.' -ForegroundColor Yellow",
    "    Write-Host '[*] To complete standalone server installation: run the official NeoForge 1.21.1 installer with --installServer into this folder.' -ForegroundColor Cyan",
    "    exit 1",
    "}"
)
[System.IO.File]::WriteAllLines($runnerFile, $runnerContent, [System.Text.Encoding]::UTF8)
Write-Host "[+] Generated run-server.ps1 runner script with safety gates and NeoForge bootstrap detection" -ForegroundColor Green

Write-Host "Production deployment structure prepared at $InstallDir." -ForegroundColor Cyan
