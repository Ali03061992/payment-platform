# Payment Platform - E2E Tests (Dev)
# Run Cypress tests against dev environment (localhost:8080)
#
# Prerequisites:
#   1. Run deploy/run-dev.ps1 (full Docker stack)
#   2. Wait for all services to be healthy
#
# Usage:
#   .\e2e-dev.ps1                    Run all tests headless
#   .\e2e-dev.ps1 -Open              Open Cypress UI
#   .\e2e-dev.ps1 -Spec "01-auth"    Run specific spec
#   .\e2e-dev.ps1 -Browser firefox   Run with Firefox
#   .\e2e-dev.ps1 -Build             Rebuild stack before tests

param(
    [switch]$Open,
    [switch]$Build,
    [string]$Spec,
    [string]$Browser = "chrome"
)

Set-Location "$PSScriptRoot/../payment-platform-ui"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  E2E Tests - Dev Environment" -ForegroundColor Cyan
Write-Host "  baseUrl: http://localhost:8080" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check Docker
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "Docker not found." -ForegroundColor Red
    exit 1
}

# Start stack if needed
Set-Location "$PSScriptRoot"
$containers = docker compose ps --format "{{.Name}}" 2>&1

if ($containers -notmatch "payment-nginx" -or $Build) {
    Write-Host "Starting Docker stack..." -ForegroundColor Yellow
    if ($Build) {
        docker compose up --build -d
    } else {
        docker compose up -d
    }
    Write-Host "Waiting for services to be healthy..." -ForegroundColor Yellow
    Start-Sleep -Seconds 30
}

Set-Location "$PSScriptRoot/../payment-platform-ui"

# Check services
Write-Host "Checking services..." -ForegroundColor Yellow

$frontendOk = $false
$gatewayOk = $false

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080" -TimeoutSec 5 -UseBasicParsing
    if ($response.StatusCode -eq 200) {
        $frontendOk = $true
        Write-Host "  Frontend (8080):  OK" -ForegroundColor Green
    }
} catch {
    Write-Host "  Frontend (8080):  NOT RUNNING" -ForegroundColor Red
}

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -TimeoutSec 5 -UseBasicParsing
    if ($response.Content -match "UP") {
        $gatewayOk = $true
        Write-Host "  Gateway (8081):   OK" -ForegroundColor Green
    }
} catch {
    Write-Host "  Gateway (8081):   NOT RUNNING" -ForegroundColor Red
}

if (-not $frontendOk -or -not $gatewayOk) {
    Write-Host ""
    Write-Host "Services not ready. Waiting 30s more..." -ForegroundColor Yellow
    Start-Sleep -Seconds 30

    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080" -TimeoutSec 5 -UseBasicParsing
        $frontendOk = $response.StatusCode -eq 200
    } catch {}

    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -TimeoutSec 5 -UseBasicParsing
        $gatewayOk = $response.Content -match "UP"
    } catch {}

    if (-not $frontendOk -or -not $gatewayOk) {
        Write-Host "Services still not ready." -ForegroundColor Red
        Write-Host "Run: deploy\run-dev.ps1" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host ""
Write-Host "Running Cypress tests..." -ForegroundColor Yellow
Write-Host ""

# Install Cypress if needed
if (-not (Test-Path "node_modules/.bin/cypress")) {
    Write-Host "Installing Cypress..." -ForegroundColor Yellow
    npm install
}

# Run tests
if ($Open) {
    npx cypress open --config-file cypress.config.dev.ts
} elseif ($Spec) {
    npx cypress run --config-file cypress.config.dev.ts --browser $Browser --spec "cypress/e2e/*${Spec}*.cy.ts"
} else {
    npx cypress run --config-file cypress.config.dev.ts --browser $Browser --headless
}

$exitCode = $LASTEXITCODE

Write-Host ""
if ($exitCode -eq 0) {
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  All tests passed!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
} else {
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "  Some tests failed" -ForegroundColor Red
    Write-Host "  Check cypress/screenshots/ for details" -ForegroundColor Yellow
    Write-Host "========================================" -ForegroundColor Red
}

exit $exitCode
