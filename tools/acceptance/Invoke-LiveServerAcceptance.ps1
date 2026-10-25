<#
.SYNOPSIS
Runs isolated, windowless dedicated-server scenarios against the gameplay JAR.
.DESCRIPTION
Requires an explicit EULA opt-in. Each run has a new directory and nonce; write/read
phases use two server processes and the same world. No desktop, audio or client is used.
#>
[CmdletBinding()]
param(
    [string[]]$Profiles = @('base', 'no-lust', 'no-cage', 'no-enchantments', 'gluttony-off', 'gluttony-hard'),
    [switch]$AcceptEula,
    [int]$TimeoutSeconds = 300,
    [string]$ProjectRoot = '',
    [switch]$Lifecycle,
    [switch]$ContinueOnFailure
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'LiveAcceptanceEvidence.ps1')
if (-not $AcceptEula) { throw 'Accept the Minecraft EULA explicitly with -AcceptEula before launching a server.' }
if (-not $ProjectRoot) { $ProjectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..')) }
$workspace = @('../../..', '../../../../..') | ForEach-Object { [IO.Path]::GetFullPath((Join-Path $ProjectRoot $_)) } |
    Where-Object { Test-Path -LiteralPath (Join-Path $_ 'aopui/build/libs/aopui-1.21.1-0.7.0.jar') } | Select-Object -First 1
if (-not $workspace) { throw 'Cannot locate the pinned workspace/AOP UI dependency.' }
$root = [IO.Path]::GetFullPath((Join-Path $ProjectRoot 'build/acceptance'))
$runId = (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 8)
$runRoot = Join-Path $root $runId
New-Item -ItemType Directory -Path $runRoot -Force | Out-Null
$results = [System.Collections.Generic.List[object]]::new()
$powershellExe = (Get-Command pwsh.exe -ErrorAction SilentlyContinue).Source
if (-not $powershellExe) { $powershellExe = (Get-Command powershell.exe).Source }
function Quote-ProcessArgument([string]$Value) {
    if ($Value.Contains('"')) { throw 'Unexpected quote in process argument.' }
    '"' + $Value + '"'
}
foreach ($profile in $Profiles) {
    if ($profile -notin @('base', 'no-lust', 'no-cage', 'no-enchantments', 'gluttony-off', 'gluttony-hard', 'botania', 'bloodmagic', 'tinkerer', 'integrations', 'free-cage')) { throw "Unknown profile: $profile" }
    $directory = [IO.Path]::GetFullPath((Join-Path $runRoot $profile))
    if (-not $directory.StartsWith($root + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Run directory escaped acceptance root.' }
    $config = Join-Path $directory 'config'
    New-Item -ItemType Directory -Force -Path $config | Out-Null
    $noLust = ($profile -eq 'no-lust').ToString().ToLowerInvariant()
    $cage = ($profile -ne 'no-cage').ToString().ToLowerInvariant()
    $enchant = ($profile -ne 'no-enchantments').ToString().ToLowerInvariant()
    $gluttony = if ($profile -eq 'gluttony-off') { 1 } elseif ($profile -eq 'gluttony-hard') { 2 } else { 0 }
    $wrathCost = if ($profile -eq 'free-cage') { 0 } else { 5 }
    # NeoForge's actual default STARTUP filename, not the old preparation-only forbiddenmagic.toml.
    [IO.File]::WriteAllText((Join-Path $config 'forbidden-startup.toml'), @"
[general]
noLust=$noLust
wrathCageEnabled=$cage
enchantmentsEnabled=$enchant
gluttony=$gluttony
wrathCageFuelCost=$wrathCost
[compatibility]
crossModInteraction=true
"@, [Text.UTF8Encoding]::new($false))
    [IO.File]::WriteAllText((Join-Path $directory 'eula.txt'), "# Explicitly authorized by the user for isolated acceptance on 2026-10-25.`neula=true`n", [Text.UTF8Encoding]::new($false))
    [IO.File]::WriteAllText((Join-Path $directory 'server.properties'), @"
level-name=acceptance-world
level-type=minecraft:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}
generate-structures=false
online-mode=false
server-ip=127.0.0.1
server-port=0
enable-query=false
enable-rcon=false
view-distance=2
simulation-distance=2
spawn-protection=0
max-tick-time=60000
sync-chunk-writes=true
"@, [Text.UTF8Encoding]::new($false))
    $phases = if ($Lifecycle) { @('write', 'read') } else { @('write') }
    foreach ($phase in $phases) {
        Write-Output "Starting dedicated server: profile=$profile phase=$phase run=$runId (no client/window/audio)"
        $stdout = Join-Path $directory "launch-$phase.log"
        $stderr = Join-Path $directory "launch-$phase.err.log"
        $gradleArguments = @('runAcceptanceServer', '--offline', '--no-configuration-cache',
            "-PaopUiJar=$workspace/aopui/build/libs/aopui-1.21.1-0.7.0.jar",
            "-PacceptanceDirectory=$directory", "-PacceptancePhase=$phase", "-PacceptanceRunId=$runId", "-PacceptanceProfile=$profile")
        $runtimeMods = switch ($profile) {
            'botania' { 'botania' }
            'bloodmagic' { 'bloodmagic' }
            'tinkerer' { 'thaumictinkerer' }
            'integrations' { 'botania,bloodmagic,thaumictinkerer' }
            default { '' }
        }
        if ($runtimeMods) { $gradleArguments += "-PacceptanceMods=$runtimeMods" }
        # PowerShell -File reparses unbound -Pname=C: tokens. A generated array preserves each argument.
        $driver = Join-Path $directory "launch-$phase.ps1"
        $literalArguments = ($gradleArguments | ForEach-Object { "'" + $_.Replace("'", "''") + "'" }) -join ','
        $wrapper = (Join-Path $ProjectRoot 'gradlew.ps1').Replace("'", "''")
        [IO.File]::WriteAllText($driver, "`$taskArguments=@($literalArguments)`n& '$wrapper' @taskArguments`nexit `$LASTEXITCODE`n", [Text.UTF8Encoding]::new($false))
        $arguments = @('-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-File', $driver)
        $quotedArguments = ($arguments | ForEach-Object { Quote-ProcessArgument $_ }) -join ' '
        $process = Start-Process -FilePath $powershellExe -ArgumentList $quotedArguments -WorkingDirectory $ProjectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
        $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
        while (-not $process.HasExited -and [DateTime]::UtcNow -lt $deadline) { Start-Sleep -Seconds 1; $process.Refresh() }
        if (-not $process.HasExited) {
            # Only stop descendants belonging to this exact acceptance launcher.
            $allProcesses = Get-CimInstance Win32_Process
            $taskOwnedIds = [System.Collections.Generic.HashSet[int]]::new()
            $taskOwnedIds.Add($process.Id) | Out-Null
            # A reused Gradle daemon is not a descendant of this PowerShell process.
            # The harness records its own PID/start time before world loading.
            $ownershipFile = Join-Path $directory 'acceptance-process.json'
            if (Test-Path -LiteralPath $ownershipFile) {
                $owner = Get-Content -Raw -LiteralPath $ownershipFile | ConvertFrom-Json
                if ($owner.runId -ceq $runId -and $owner.profile -ceq $profile -and $owner.phase -ceq $phase) {
                    $candidate = $allProcesses | Where-Object ProcessId -EQ $owner.pid | Select-Object -First 1
                    if ($candidate -and $candidate.Name -eq 'java.exe') {
                        $started = ([DateTimeOffset]$candidate.CreationDate.ToUniversalTime()).ToUnixTimeMilliseconds()
                        if ([Math]::Abs($started - [long]$owner.startedAt) -lt 1000) { $taskOwnedIds.Add([int]$owner.pid) | Out-Null }
                    }
                }
            }
            do {
                $added = $false
                foreach ($candidate in $allProcesses) {
                    if ($taskOwnedIds.Contains([int]$candidate.ParentProcessId) -and $taskOwnedIds.Add([int]$candidate.ProcessId)) { $added = $true }
                }
            } while ($added)
            foreach ($taskProcessId in $taskOwnedIds) { Stop-Process -Id $taskProcessId -Force -ErrorAction SilentlyContinue }
            throw "Acceptance timeout; failed profile=$profile phase=$phase. Inspect $stdout"
        }
        $process.WaitForExit()
        $reportPath = Join-Path $directory "acceptance-$phase.json"
        if (-not (Test-Path -LiteralPath $reportPath)) {
            if (-not $ContinueOnFailure) { throw "No fresh acceptance report. Exit=$($process.ExitCode); inspect $stdout and $stderr" }
            $results.Add([pscustomobject]@{ runId=$runId; profile=$profile; phase=$phase; passed=$false; error='Server did not produce a report'; exitCode=$process.ExitCode; log=$stdout })
            Write-Output "FAIL profile=$profile phase=$phase no server report; inspect $stdout"
            break
        }
        $report = Get-Content -Raw -LiteralPath $reportPath | ConvertFrom-Json
        $evidenceError = $null
        try { Assert-FmLiveEvidence -Report $report -ExitCode $process.ExitCode -RunId $runId -Profile $profile -Phase $phase } catch { $evidenceError = $_.Exception.Message }
        $serverLog = Join-Path $directory 'logs/latest.log'
        $loadErrors = if (Test-Path -LiteralPath $serverLog) { @(Select-String -LiteralPath $serverLog -Pattern '/ERROR\]|Parsing error loading recipe|Failed to load datapacks|Rejected active research catalog') } else { @('Missing server log') }
        if ($loadErrors.Count -gt 0) { $evidenceError = 'Server log contains loading errors: ' + ($loadErrors -join '; ') }
        if ($evidenceError) {
            $report.passed = $false
            $report.error = $evidenceError
            if (-not $ContinueOnFailure) { throw "Acceptance failed profile=$profile phase=${phase}: $($report.error); exit=$($process.ExitCode)" }
            $results.Add($report)
            Write-Output "FAIL profile=$profile phase=${phase}: $($report.error)"
            break
        }
        $results.Add($report)
        Write-Output "PASS profile=$profile phase=$phase assertions=$(@($report.checks).Count) serverPID=$($report.processId)"
    }
}
$passed = @($results | Where-Object { -not $_.passed }).Count -eq 0
$jar = Get-ChildItem -LiteralPath (Join-Path $ProjectRoot 'build/libs') -Filter 'ForbiddenMagic-1.21.1-*.jar' | Where-Object Name -NotLike '*-sources.jar' | Select-Object -First 1
$summary = [ordered]@{ runId=$runId; passed=$passed; runRoot=$runRoot; results=$results; gameplaySha256=(Get-FileHash -LiteralPath $jar.FullName -Algorithm SHA256).Hash.ToLowerInvariant(); gitCommit=(git -C $ProjectRoot rev-parse HEAD); clientAcceptance='NOT_RUN: desktop capture/control unavailable'; eula='Explicit user authorization, isolated worlds only' }
$summary | ConvertTo-Json -Depth 15 | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $runRoot 'summary.json')
Write-Output "FM_LIVE_SERVER_ACCEPTANCE passed=$passed reports=$($results.Count) root=$runRoot"
if (-not $passed) { exit 1 }
