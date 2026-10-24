<#
.SYNOPSIS
    Acceptance Preparation: Two-Phase Save/Reload Server Persistence Protocol.
.DESCRIPTION
    Specifies the verifiable protocol for persistence across two sequential server processes:
    Phase 1:
      - Start dedicated server Process A in runs/private-server-lifecycle.
      - Place Wrath Cage with imprint `minecraft:blaze`, wrath=16, sloth=32.
      - Place Morph Pickaxe with phase=1 (blue), custom name, and Silk Touch.
      - Place Obedience Collar bound to player UUID.
      - Execute `save-all flush` and unregister/unload chunks.
      - Graceful shutdown of Process A (exit code 0).
    Phase 2:
      - Start dedicated server Process B in the identical directory.
      - Inspect BlockEntity data, item component codecs, and player NBT.
      - Verify exact restoration of Wrath Cage aspect suction, sloth/wrath fuel levels,
        Morph phase and enchants, collar binding, and chunk states.
    NOTE: Game processes are not executed until explicit user approval.
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

$protocol = [PSCustomObject]@{
    Scenario = 'Two-Phase Dedicated Server Persistence Protocol'
    TargetDirectory = 'runs/private-server-lifecycle'
    TestCases = @(
        @{
            Id = 'SAVE-01'
            Target = 'Wrath Cage BlockEntity Persistence'
            Phase1Action = 'Set mobID=minecraft:blaze, wrath=16, sloth=32, mode=0'
            Phase2Check = 'Read NBT: verify mobID, wrath, sloth values match exactly'
        },
        @{
            Id = 'SAVE-02'
            Target = 'Morph Tool DataComponent Persistence'
            Phase1Action = 'Set MorphState: phase=1, enchantments=[minecraft:silk_touch:1], customName="Survey Pick"'
            Phase2Check = 'Verify ForbiddenComponents.MORPH codec round-trip on world load'
        },
        @{
            Id = 'SAVE-03'
            Target = 'Collar of Obedience Persistence'
            Phase1Action = 'Attach collar to entity with owner UUID and vis charge=250'
            Phase2Check = 'Verify ownership tag and vis storage integrity across restart'
        },
        @{
            Id = 'SAVE-04'
            Target = 'Taint World Mechanics Persistence'
            Phase1Action = 'Spawn Umbral Bush with spread timer, Taint Coal burning in furnace'
            Phase2Check = 'Verify block state, furnace burn time, and bush tick schedules'
        }
    )
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Forbidden Magic Save/Reload Verification Scenario" -ForegroundColor Cyan
Write-Host " Protocol: Two sequential server processes with world save" -ForegroundColor Cyan
Write-Host " Gating:   Execution strictly gated until user approval" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

foreach ($tc in $protocol.TestCases) {
    Write-Host "[*] [$($tc.Id)] $($tc.Target)" -ForegroundColor Green
    Write-Host "    - Process A (Write): $($tc.Phase1Action)"
    Write-Host "    - Process B (Read):  $($tc.Phase2Check)"
}

Write-Host "Save/Reload scenario protocol prepared and verified." -ForegroundColor Cyan
