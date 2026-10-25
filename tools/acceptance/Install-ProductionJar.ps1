<#
.SYNOPSIS
    Acceptance Preparation: Production Deployment Installer.
.DESCRIPTION
    Packages and sets up a production server environment outside dev:
    - Target: build/install/forbidden-production/
    - Copies ForbiddenMagic release JAR.
    - Strictly stages exact pinned dependency JARs (Thaumcraft, API, TT, Botania, Blood Magic, Curios, Patchouli, Modopedia, AOP UI).
    - Prevents duplicates and incomplete runtime setups.
    - Deeply validates transitive neoforge.mods.toml dependency closure.
    - Sets default server configurations.
    - Strictly creates eula.txt with eula=false (NEVER automatically accepts EULA).
    - Generates PowerShell-only launch script requiring explicit manual confirmation.
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

# 0. Clean old or duplicate JARs from mods directory
Get-ChildItem -Path $modsDir -Filter "*.jar" -ErrorAction SilentlyContinue | Remove-Item -Force

# 1. Main Gameplay JAR
$buildLibs = Join-Path $ProjectRoot 'build/libs'
$mainJars = Get-ChildItem -Path $buildLibs -Filter "ForbiddenMagic-1.21.1-*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch '-sources\.jar$' }
if ($mainJars -and $mainJars.Count -gt 0) {
    Copy-Item -Path $mainJars[0].FullName -Destination (Join-Path $modsDir $mainJars[0].Name) -Force
    Write-Host "[+] Copied release JAR: $($mainJars[0].Name)" -ForegroundColor Green
} else {
    Write-Warning "Main JAR not found in $buildLibs. Run assemble/build first."
}

# 1b. Locate Workspace Root
$workspaceRootCandidates = @(
    (Resolve-Path (Join-Path $ProjectRoot '../../..') -ErrorAction SilentlyContinue),
    (Resolve-Path (Join-Path $ProjectRoot '../../../../..') -ErrorAction SilentlyContinue)
) | Where-Object { $_ -ne $null } | ForEach-Object { $_.Path }

$workspaceRoot = $workspaceRootCandidates | Where-Object { Test-Path (Join-Path $_ 'repo/api') } | Select-Object -First 1
if (-not $workspaceRoot) {
    throw "Unable to locate workspace root (thaumcraft4_port) from $ProjectRoot"
}

# 1c. Exact Pinned Dependencies Manifest (Single JAR per Mod ID, No Duplicates)
$pinnedDependencies = @(
    @{
        ModId = 'thaumcraft'
        Path  = Join-Path $workspaceRoot 'repo/mod/build/libs/thaumcraft-1.21.1-1.0.0.jar'
    },
    @{
        ModId = 'thaumcraft_api'
        Path  = Join-Path $workspaceRoot 'repo/api/build/libs/thaumcraft-api-1.0.0.jar'
    },
    @{
        ModId = 'aop_ui'
        Path  = Join-Path $workspaceRoot 'aopui/build/libs/aopui-1.21.1-0.7.0.jar'
    },
    @{
        ModId = 'thaumictinkerer'
        Path  = Join-Path $workspaceRoot 'port_addons/Thaumic_Tinkerer/repo/build/libs/ThaumicTinkerer-1.21.1-1.0.0.jar'
    },
    @{
        ModId = 'botania'
        Path  = Join-Path $workspaceRoot 'port_addons/Thaumic_Tinkerer/analysis/compat_reference/botania/botania-neoforge-1.21.1-456-20260912.043547-15.jar'
    },
    @{
        ModId = 'bloodmagic'
        Path  = Join-Path $workspaceRoot 'port_addons/Thaumic_Tinkerer/analysis/compat_reference/bloodmagic/bloodmagic-3.4.0-46-alpha.jar'
    },
    @{
        ModId = 'modopedia'
        Path  = Join-Path $workspaceRoot 'port_addons/Thaumic_Tinkerer/analysis/compat_reference/bloodmagic/modopedia-neoforge-1.21.1-1.1.5.jar'
    },
    @{
        ModId = 'curios'
        Path  = Join-Path $workspaceRoot '.cache/aopui-verification-mods/curios-neoforge-9.5.1+1.21.1.jar'
    },
    @{
        ModId = 'patchouli'
        Path  = Join-Path $workspaceRoot '.cache/gradle/caches/modules-2/files-2.1/vazkii.patchouli/Patchouli/1.21.1-92-NEOFORGE/f4b32d25521f898b3c1c80b88cfbd1bce30cf23b/Patchouli-1.21.1-92-NEOFORGE.jar'
    }
)

$copiedCount = 0
foreach ($dep in $pinnedDependencies) {
    $src = $dep.Path
    if (-not (Test-Path $src)) {
        throw "Required pinned dependency for $($dep.ModId) not found at: $src"
    }
    $fileName = [System.IO.Path]::GetFileName($src)
    $dest = Join-Path $modsDir $fileName
    Copy-Item -Path $src -Destination $dest -Force
    Write-Host "[+] Staged pinned dependency ($($dep.ModId)): $fileName" -ForegroundColor Green
    $copiedCount++
}

Write-Host "[*] Total dependency JARs staged: $copiedCount / $($pinnedDependencies.Count) (0 duplicates, full transitive set)" -ForegroundColor Cyan

# 1d. Deep Transitive Dependency Verification via neoforge.mods.toml
Write-Host "[*] Validating NeoForge mods.toml dependencies across all staged JARs..." -ForegroundColor Cyan
Add-Type -AssemblyName System.IO.Compression.FileSystem

$providedMods = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
$providedMods.Add("minecraft") | Out-Null
$providedMods.Add("neoforge") | Out-Null
$providedMods.Add("forge") | Out-Null
$providedMods.Add("java") | Out-Null

$stagedJars = Get-ChildItem -Path $modsDir -Filter "*.jar"
foreach ($jar in $stagedJars) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
    try {
        $entry = $zip.GetEntry('META-INF/neoforge.mods.toml')
        if ($entry) {
            $reader = [System.IO.StreamReader]::new($entry.Open())
            try {
                $content = $reader.ReadToEnd()
                $matches = [regex]::Matches($content, '(?m)^\s*modId\s*=\s*"([^"]+)"')
                foreach ($m in $matches) {
                    $providedMods.Add($m.Groups[1].Value) | Out-Null
                }
            } finally {
                $reader.Close()
            }
        }
    } finally {
        $zip.Dispose()
    }
}

Write-Host "    Provided Mod IDs: $(-join ($providedMods | ForEach-Object { "$_, " })).TrimEnd(', ')" -ForegroundColor DarkGray

$missingRequiredDeps = [System.Collections.Generic.List[string]]::new()
foreach ($jar in $stagedJars) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
    try {
        $entry = $zip.GetEntry('META-INF/neoforge.mods.toml')
        if ($entry) {
            $reader = [System.IO.StreamReader]::new($entry.Open())
            try {
                $content = $reader.ReadToEnd()
                $depBlocks = [regex]::Matches($content, '(?s)\[\[dependencies\.[^\]]+\]\](.*?)(?=\[\[|\z)')
                foreach ($block in $depBlocks) {
                    $bText = $block.Groups[1].Value
                    $targetMod = if ($bText -match '(?m)^\s*modId\s*=\s*"([^"]+)"') { $matches[1] } else { $null }
                    $type = if ($bText -match '(?m)^\s*type\s*=\s*"([^"]+)"') { $matches[1] } else { 'required' }
                    $mandatory = if ($bText -match '(?m)^\s*mandatory\s*=\s*(true|false)') { [bool]::Parse($matches[1]) } else { $null }

                    $isRequired = ($type -eq 'required') -or ($mandatory -eq $true)
                    if ($isRequired -and $targetMod) {
                        if (-not $providedMods.Contains($targetMod)) {
                            $missingRequiredDeps.Add("JAR $($jar.Name) requires '$targetMod' which is not present in staged mods")
                        } else {
                            Write-Host "    [+] Verified: $($jar.Name) -> $targetMod satisfied" -ForegroundColor Green
                        }
                    }
                }
            } finally {
                $reader.Close()
            }
        }
    } finally {
        $zip.Dispose()
    }
}

if ($missingRequiredDeps.Count -gt 0) {
    foreach ($err in $missingRequiredDeps) {
        Write-Host "[-] Missing Dependency: $err" -ForegroundColor Red
    }
    throw "Staged production JAR set fails transitive dependency verification! ($($missingRequiredDeps.Count) missing dependencies)"
}
Write-Host "[+] All mandatory dependencies verified and satisfied across staged JARs." -ForegroundColor Green

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

# 4. Generate runner script with PowerShell-only NeoForge bootstrap detection and manual confirmation
$runnerFile = Join-Path $InstallDir 'run-server.ps1'
$runnerContent = @(
    "# Dedicated Server Runner for Production Acceptance (PowerShell only)",
    "`$eula = Get-Content eula.txt | Where-Object { `$_ -match '^eula=(true|false)' }",
    "if (`$eula -notmatch 'eula=true') {",
    "    Write-Host '[-] EULA is not accepted. Edit eula.txt manually if you agree to the Minecraft EULA.' -ForegroundColor Red",
    "    exit 1",
    "}",
    "if (Test-Path 'run.ps1') {",
    "    Write-Host '[*] Launching NeoForge Dedicated Server via official run.ps1 bootstrap...' -ForegroundColor Yellow",
    "    & .\run.ps1 `$args",
    "} else {",
    "    Write-Host '[-] Official NeoForge runtime bootstrap (run.ps1) not detected in this folder.' -ForegroundColor Yellow",
    "    Write-Host '[*] To complete standalone server installation: run the official NeoForge 1.21.1 installer with --installServer into this folder.' -ForegroundColor Cyan",
    "    exit 1",
    "}"
)
[System.IO.File]::WriteAllLines($runnerFile, $runnerContent, [System.Text.Encoding]::UTF8)
Write-Host "[+] Generated run-server.ps1 runner script with safety gates and PowerShell bootstrap detection" -ForegroundColor Green

Write-Host "Production deployment structure prepared at $InstallDir." -ForegroundColor Cyan
