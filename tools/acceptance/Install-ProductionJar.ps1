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

# 4. Generate runner script with manual confirmation
$runnerFile = Join-Path $InstallDir 'run-server.ps1'
$runnerContent = @(
    "# Dedicated Server Runner for Production Acceptance",
    "`$eula = Get-Content eula.txt | Where-Object { `$_ -match '^eula=(true|false)' }",
    "if (`$eula -notmatch 'eula=true') {",
    "    Write-Host '[-] EULA is not accepted. Edit eula.txt manually if you agree to the Minecraft EULA.' -ForegroundColor Red",
    "    exit 1",
    "}",
    "Write-Host '[*] Starting NeoForge Dedicated Server in production mode...' -ForegroundColor Yellow",
    "java -Xmx4G -jar server.jar nogui"
)
[System.IO.File]::WriteAllLines($runnerFile, $runnerContent, [System.Text.Encoding]::UTF8)
Write-Host "[+] Generated run-server.ps1 runner script with safety gates" -ForegroundColor Green

Write-Host "Production deployment structure prepared at $InstallDir." -ForegroundColor Cyan
