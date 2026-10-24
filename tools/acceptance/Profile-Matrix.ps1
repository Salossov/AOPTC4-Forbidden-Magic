<#
.SYNOPSIS
    Acceptance Preparation: STARTUP Profile Matrix Definition for Forbidden Magic.
.DESCRIPTION
    Configures and defines 8 isolated startup profiles in separate processes:
    - Base: Minimal baseline with Thaumcraft + Forbidden Magic.
    - NoLust: forbidden.no_lust = true (Lust shard, whip, aspect disabled).
    - NoCage: forbidden.wrath_cage_enabled = false (Wrath cage block & spawner logic disabled).
    - Standalone: Zero optional cross-mods (no Botania, Blood Magic, Tinkerer, TF).
    - WithBotania: FM + Botania only.
    - WithBloodMagic: FM + Blood Magic only.
    - WithTinkerer: FM + Thaumic Tinkerer only (T01 costs registered with EnchanterCatalog).
    - FullAddons: All allowed addons combined.
    NOTE: Execution of game processes remains strictly gated until explicit user approval.
#>
[CmdletBinding()]
param(
    [string]$ProjectRoot = '',
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
        ConfigOverrides = @{ 'forbidden.no_lust' = 'false'; 'forbidden.wrath_cage_enabled' = 'true' }
        OptionalMods = @()
        ExpectedRegistries = @('forbidden:infernus', 'forbidden:mob_crystal', 'forbidden:wrath_cage', 'forbidden:heretic_study')
    },
    @{
        Name = 'Profile-NoLust'
        Description = 'Lust disabled: verifies complete gating of lust shard, riding crop, luxuria aspect'
        ConfigOverrides = @{ 'forbidden.no_lust' = 'true'; 'forbidden.wrath_cage_enabled' = 'true' }
        OptionalMods = @()
        ExpectedRegistries = @('forbidden:infernus', 'forbidden:wrath_cage')
    },
    @{
        Name = 'Profile-NoCage'
        Description = 'Wrath Cage disabled: verifies clean absence of spawner entity without crash'
        ConfigOverrides = @{ 'forbidden.no_lust' = 'false'; 'forbidden.wrath_cage_enabled' = 'false' }
        OptionalMods = @()
        ExpectedRegistries = @('forbidden:infernus', 'forbidden:mob_crystal')
    },
    @{
        Name = 'Profile-Standalone'
        Description = 'No optional mods present: verifies clean ClassLoader isolation'
        ConfigOverrides = @{ 'forbidden.cross_mod' = 'true' }
        OptionalMods = @()
        ExpectedRegistries = @('forbidden:taint_shovel', 'forbidden:morph_pickaxe')
    },
    @{
        Name = 'Profile-WithBotania'
        Description = 'FM + Botania: Livingwood/Dreamwood wand parts, Mana Vis conversion, 4 flowers'
        ConfigOverrides = @{ 'forbidden.cross_mod' = 'true' }
        OptionalMods = @('botania')
        ExpectedRegistries = @('forbidden:wand_rod_livingwood', 'forbidden:wand_rod_dreamwood')
    },
    @{
        Name = 'Profile-WithBloodMagic'
        Description = 'FM + Blood Magic: Blood wand parts, Sanguine Rapier, Bloodthorn'
        ConfigOverrides = @{ 'forbidden.cross_mod' = 'true' }
        OptionalMods = @('bloodmagic')
        ExpectedRegistries = @('forbidden:wand_rod_blood', 'forbidden:wand_cap_blood')
    },
    @{
        Name = 'Profile-WithTinkerer'
        Description = 'FM + Thaumic Tinkerer: 8 Osmotic costs in EnchanterCatalog, KAMI aspects'
        ConfigOverrides = @{ 'forbidden.cross_mod' = 'true' }
        OptionalMods = @('thaumictinkerer')
        ExpectedRegistries = @('forbidden:nether_shard', 'forbidden:gluttony_shard')
    },
    @{
        Name = 'Profile-FullAddons'
        Description = 'Full cross-mod environment: FM + Botania + BM + TT + TF + Viewers'
        ConfigOverrides = @{ 'forbidden.cross_mod' = 'true' }
        OptionalMods = @('botania', 'bloodmagic', 'thaumictinkerer', 'twilightforest', 'thaumcraft_viewers')
        ExpectedRegistries = @('forbidden:wand_rod_blood', 'forbidden:wand_rod_livingwood', 'forbidden:nether_shard')
    }
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Forbidden Magic Startup Profile Matrix (Preparation)" -ForegroundColor Cyan
Write-Host " Total Defined Profiles: $($profiles.Count)" -ForegroundColor Cyan
Write-Host " Gating: Execution strictly blocked until user approval" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

foreach ($p in $profiles) {
    Write-Host "[*] $($p.Name): $($p.Description)" -ForegroundColor Green
    Write-Host "    - Config overrides: $(($p.ConfigOverrides.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" }) -join ', ')"
    Write-Host "    - Optional mods:    $($p.OptionalMods -join ', ')"
    Write-Host "    - Expected content: $($p.ExpectedRegistries -join ', ')"
}

Write-Host "Profile matrix preparation complete. All profiles ready for acceptance phase." -ForegroundColor Cyan
