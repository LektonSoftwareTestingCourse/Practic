$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$ComposeFiles = @('-f', 'docker-compose.yaml', '-f', 'docker-compose.practice5.yaml')
$KeepStack = $env:KEEP_SMP_STACK -eq '1'

if (-not (Test-Path '.env')) {
    Copy-Item '.env.example' '.env'
    Write-Host 'Created .env from .env.example. Review credentials and ports before production use.'
}

function Get-DotEnvValue([string]$Name, [string]$Fallback) {
    $Line = Get-Content '.env' | Where-Object { $_ -match "^$Name=" } | Select-Object -Last 1
    if ($Line) { return ($Line -split '=', 2)[1] }
    return $Fallback
}

$GatewayPort = Get-DotEnvValue 'GATEWAY_PORT' '8080'
$SwitchPort = Get-DotEnvValue 'SWITCH_PORT' '8082'
$AuthPort = Get-DotEnvValue 'AUTH_PORT' '8083'
$LoggerPort = Get-DotEnvValue 'LOGGER_PORT' '8088'
$TerminalPort = Get-DotEnvValue 'TERMINAL_PORT' '8085'
$MerchantPort = Get-DotEnvValue 'MERCHANT_PORT' '8084'

if (-not $env:SMP_GATEWAY_URL) { $env:SMP_GATEWAY_URL = "http://localhost:$GatewayPort" }
if (-not $env:SMP_SWITCH_URL) { $env:SMP_SWITCH_URL = "http://localhost:$SwitchPort" }
if (-not $env:SMP_AUTHORIZATION_URL) { $env:SMP_AUTHORIZATION_URL = "http://localhost:$AuthPort" }
if (-not $env:SMP_LOGGER_URL) { $env:SMP_LOGGER_URL = "http://localhost:$LoggerPort" }
$GatewayBase = $env:SMP_GATEWAY_URL.TrimEnd('/')
$HealthUrl = "$GatewayBase/health"

$ExitCode = 0
try {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Docker is required.' }
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'Maven is required.' }

    & docker compose @ComposeFiles up -d --build
    if ($LASTEXITCODE -ne 0) { throw 'Docker Compose failed to start the stack.' }

    Write-Host "Waiting for complete Gateway health: $HealthUrl"
    $Ready = $false
    for ($Attempt = 0; $Attempt -lt 120; $Attempt++) {
        try {
            $Health = Invoke-WebRequest -Uri $HealthUrl -TimeoutSec 4 -UseBasicParsing
            if ($Health.StatusCode -eq 200) { $Ready = $true; break }
        } catch {
            # A 503 means the Gateway is up but at least one downstream service is not ready yet.
        }
        Start-Sleep -Seconds 2
    }
    if (-not $Ready) { throw 'Gateway did not become healthy within 240 seconds.' }

    # The aggregate Gateway health endpoint does not include the simulators.
    foreach ($Simulator in @(@{ Name = 'terminal'; Port = $TerminalPort }, @{ Name = 'merchant'; Port = $MerchantPort })) {
        $SimulatorUrl = "http://localhost:$($Simulator.Port)/health"
        $SimulatorReady = $false
        Write-Host "Waiting for $($Simulator.Name) simulator: $SimulatorUrl"
        for ($Attempt = 0; $Attempt -lt 120; $Attempt++) {
            try {
                $Health = Invoke-WebRequest -Uri $SimulatorUrl -TimeoutSec 4 -UseBasicParsing
                if ($Health.StatusCode -eq 200) { $SimulatorReady = $true; break }
            } catch {
                # Wait until the simulator health endpoint becomes available.
            }
            Start-Sleep -Seconds 2
        }
        if (-not $SimulatorReady) { throw "$($Simulator.Name) simulator did not become healthy within 240 seconds." }
    }

    $Repetitions = 3
    if ($env:PRACTICE5_REPETITIONS) { $Repetitions = [int]$env:PRACTICE5_REPETITIONS }
    if ($Repetitions -lt 1) { throw 'PRACTICE5_REPETITIONS must be a positive integer.' }
    for ($Run = 1; $Run -le $Repetitions; $Run++) {
        Write-Host "Practice 5 API test run $Run/$Repetitions"
        & mvn -B -f services/pom.xml -P api-tests verify
        if ($LASTEXITCODE -ne 0) { throw "Maven API tests failed on run $Run." }
    }
    Write-Host 'Practice 5 API tests passed.'
} catch {
    $ExitCode = 1
    Write-Error $_
    & docker compose @ComposeFiles logs --no-color --tail=160
} finally {
    if (-not $KeepStack) {
        & docker compose @ComposeFiles down --remove-orphans
    } else {
        Write-Host 'KEEP_SMP_STACK=1: containers left running.'
    }
}
exit $ExitCode
