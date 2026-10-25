<#
.SYNOPSIS
    Acceptance Preparation: STARTUP Profile Matrix Definition for Forbidden Magic.
.DESCRIPTION
    Configures and defines 8 isolated startup profiles in separate processes:
    - Base: Minimal baseline with Thaumcraft + Forbidden Magic.
    - NoLust: collar/Luxuria absent; the single shard ID changes to despair; riding crop remains.
    - NoCage: cage block/BE stay registered; crystal, cage recipes/research and operation disabled.
    - Standalone: Zero optional cross-mods (compatibility.crossModInteraction = false).
    - WithBotania: FM + Botania only.
    - WithBloodMagic: FM + Blood Magic only.
    - WithTinkerer: FM + Thaumic Tinkerer only (T01 costs registered with EnchanterCatalog).
    - FullAddons: All allowed addons combined.
    NOTE: Execution of game processes remains strictly gated until explicit user approval.
#>
[CmdletBinding()]
param(
    [string]$ProjectRoot = '',
    [switch]$PrepareDirectories = $false,
    [switch]$DryRunOnly = $true
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
}

$profiles = @(
    @{
        Name = 'Profile-Base'
        Description = 'Clean baseline: Core Thaumcraft + Forbidden Magic minimal configuration'
        ConfigOverrides = @{ 'general.noLust' = 'false'; 'general.wrathCageEnabled' = 'true'; 'compatibility.crossModInteraction' = 'true' }
        OptionalMods = @()
        ExpectedRegistries = @('forbidden:infernus', 'forbidden:mob_crystal', 'forbidden:wrath_cage', 'forbidden:heretic_study')
    },
    @{
        Name = 'Profile-NoLust'
        Description = 'No Lust: Luxuria/collar absent; shard keeps its ID and becomes despair; riding crop remains'
        ConfigOverrides = @{ 'general.noLust' = 'true'; 'general.wrathCageEnabled' = 'true'; 'compatibility.crossModInteraction' = 'true' }
        OptionalMods = @()
        ExpectedRegistries = @('forbidden:infernus', 'forbidden:wrath_cage')
    },
    @{
        Name = 'Profile-NoCage'
        Description = 'No Cage: block and block entity remain; crystal/recipes/research and spawning disabled'
        ConfigOverrides = @{ 'general.noLust' = 'false'; 'general.wrathCageEnabled' = 'false'; 'compatibility.crossModInteraction' = 'true' }
        OptionalMods = @()
        ExpectedRegistries = @('forbidden:infernus', 'forbidden:wrath_cage')
    },
    @{
        Name = 'Profile-Standalone'
        Description = 'No optional mods present: verifies clean ClassLoader isolation'
        ConfigOverrides = @{ 'general.noLust' = 'false'; 'general.wrathCageEnabled' = 'true'; 'compatibility.crossModInteraction' = 'false' }
        OptionalMods = @()
        ExpectedRegistries = @('forbidden:taint_shovel', 'forbidden:morph_pickaxe')
    },
    @{
        Name = 'Profile-WithBotania'
        Description = 'FM + Botania: Livingwood/Dreamwood wand parts, Mana Vis conversion, 4 flowers'
        ConfigOverrides = @{ 'general.noLust' = 'false'; 'general.wrathCageEnabled' = 'true'; 'compatibility.crossModInteraction' = 'true'; 'compatibility.botania' = 'true' }
        OptionalMods = @('botania')
        ExpectedRegistries = @('forbidden:wand_rod_livingwood', 'forbidden:wand_rod_dreamwood', 'forbidden:euclidaisy')
    },
    @{
        Name = 'Profile-WithBloodMagic'
        Description = 'FM + Blood Magic: Blood wand parts, Sanguine Rapier, Bloodthorn'
        ConfigOverrides = @{ 'general.noLust' = 'false'; 'general.wrathCageEnabled' = 'true'; 'compatibility.crossModInteraction' = 'true'; 'compatibility.bloodMagic' = 'true' }
        OptionalMods = @('bloodmagic')
        ExpectedRegistries = @('forbidden:wand_rod_blood', 'forbidden:wand_cap_alchemical', 'forbidden:bloodthorn')
    },
    @{
        Name = 'Profile-WithTinkerer'
        Description = 'FM + Thaumic Tinkerer: 8 Osmotic costs in EnchanterCatalog, KAMI aspects'
        ConfigOverrides = @{ 'general.noLust' = 'false'; 'general.wrathCageEnabled' = 'true'; 'compatibility.crossModInteraction' = 'true'; 'compatibility.thaumicTinkerer' = 'true' }
        OptionalMods = @('thaumictinkerer')
        ExpectedRegistries = @('forbidden:gluttony_shard', 'forbidden:heretic_study')
    },
    @{
        Name = 'Profile-FullAddons'
        Description = 'Full cross-mod environment: FM + Botania + BM + TT + TF'
        ConfigOverrides = @{ 'general.noLust' = 'false'; 'general.wrathCageEnabled' = 'true'; 'compatibility.crossModInteraction' = 'true' }
        OptionalMods = @('botania', 'bloodmagic', 'thaumictinkerer', 'twilightforest')
        ExpectedRegistries = @('forbidden:wand_rod_blood', 'forbidden:wand_rod_livingwood', 'forbidden:gluttony_shard', 'forbidden:euclidaisy')
    }
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Forbidden Magic Startup Profile Matrix (Preparation)" -ForegroundColor Cyan
Write-Host " Total Defined Profiles: $($profiles.Count)" -ForegroundColor Cyan
Write-Host " Preparation only. Execute with Invoke-LiveServerAcceptance.ps1 -AcceptEula" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

foreach ($p in $profiles) {
    Write-Host "[*] $($p.Name): $($p.Description)" -ForegroundColor Green
    Write-Host "    - Config overrides: $(($p.ConfigOverrides.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" }) -join ', ')"
    Write-Host "    - Optional mods:    $(if ($p.OptionalMods.Count -gt 0) { $p.OptionalMods -join ', ' } else { 'none' })"
    Write-Host "    - Expected content: $($p.ExpectedRegistries -join ', ')"

    if ($PrepareDirectories -or (-not $DryRunOnly)) {
        $targetDir = Join-Path $ProjectRoot "build/matrix/$($p.Name)"
        $configDir = Join-Path $targetDir "config"
        $modsDir = Join-Path $targetDir "mods"
        New-Item -ItemType Directory -Force -Path $configDir, $modsDir | Out-Null

        $tomlPath = Join-Path $configDir "forbidden-startup.toml"
        $noLust = if ($p.ConfigOverrides.ContainsKey('general.noLust')) { $p.ConfigOverrides['general.noLust'] } else { 'false' }
        $wrathCage = if ($p.ConfigOverrides.ContainsKey('general.wrathCageEnabled')) { $p.ConfigOverrides['general.wrathCageEnabled'] } else { 'true' }
        $crossMod = if ($p.ConfigOverrides.ContainsKey('compatibility.crossModInteraction')) { $p.ConfigOverrides['compatibility.crossModInteraction'] } else { 'true' }
        $botania = if ($p.ConfigOverrides.ContainsKey('compatibility.botania')) { $p.ConfigOverrides['compatibility.botania'] } else { 'true' }
        $bloodMagic = if ($p.ConfigOverrides.ContainsKey('compatibility.bloodMagic')) { $p.ConfigOverrides['compatibility.bloodMagic'] } else { 'true' }
        $tt = if ($p.ConfigOverrides.ContainsKey('compatibility.thaumicTinkerer')) { $p.ConfigOverrides['compatibility.thaumicTinkerer'] } else { 'true' }

        $tomlLines = @(
            "[general]",
            "noLust = $noLust",
            "wrathCageEnabled = $wrathCage",
            "",
            "[compatibility]",
            "crossModInteraction = $crossMod",
            "botania = $botania",
            "bloodMagic = $bloodMagic",
            "thaumicTinkerer = $tt"
        )
        [System.IO.File]::WriteAllLines($tomlPath, $tomlLines, [System.Text.Encoding]::UTF8)

        $eulaPath = Join-Path $targetDir "eula.txt"
        [System.IO.File]::WriteAllLines($eulaPath, @("# universal-modder strict rule: no automated eula acceptance", "eula=false"), [System.Text.Encoding]::UTF8)

        Write-Host "    -> Prepared isolated directory: $targetDir" -ForegroundColor DarkGreen
    }
}

Write-Host "Profile matrix preparation complete. All profiles ready for acceptance phase." -ForegroundColor Cyan
