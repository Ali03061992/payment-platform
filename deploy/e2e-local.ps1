# Payment Platform - E2E Tests (Local)
# Run Cypress tests against local environment (localhost:4200)
#
# Prerequisites:
#   1. Run deploy/run-local.ps1 (infrastructure)
#   2. Start backend services in IntelliJ (profile: local)
#   3. Start frontend: cd payment-platform-ui && npm start
#
# Usage:
#   .\e2e-local.ps1                    Run all tests headless
#   .\e2e-local.ps1 -Open              Open Cypress UI
#   .\e2e-local.ps1 -Spec "01-auth"    Run specific spec
#   .\e2e-local.ps1 -Browser firefox   Run with Firefox

param(
    [switch]$Open,
    [string]$Spec,
    [string]$Browser = "chrome"
)

Set-Location "$PSScriptRoot/../payment-platform-ui"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  E2E Tests - Local Environment" -ForegroundColor Cyan
Write-Host "  baseUrl: http://localhost:4200" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check services
Write-Host "Checking services..." -ForegroundColor Yellow

$frontendOk = $false
$gatewayOk = $false

try {
    $response = Invoke-WebRequest -Uri "http://localhost:4200" -TimeoutSec 3 -UseBasicParsing
    if ($response.StatusCode -eq 200) {
        $frontendOk = $true
        Write-Host "  Frontend (4200):  OK" -ForegroundColor Green
    }
} catch {
    Write-Host "  Frontend (4200):  NOT RUNNING" -ForegroundColor Red
}

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -TimeoutSec 3 -UseBasicParsing
    if ($response.Content -match "UP") {
        $gatewayOk = $true
        Write-Host "  Gateway (8081):   OK" -ForegroundColor Green
    }
} catch {
    Write-Host "  Gateway (8081):   NOT RUNNING" -ForegroundColor Red
}

if (-not $frontendOk -or -not $gatewayOk) {
    Write-Host ""
    Write-Host "Services not ready. Start them first:" -ForegroundColor Red
    Write-Host "  1. deploy\run-local.ps1 (infrastructure)" -ForegroundColor Yellow
    Write-Host "  2. IntelliJ: ALL_SERVICES.run.xml (profile: local)" -ForegroundColor Yellow
    Write-Host "  3. cd payment-platform-ui && npm start" -ForegroundColor Yellow
    exit 1
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
    npx cypress open --config-file cypress.config.local.ts
} elseif ($Spec) {
    npx cypress run --config-file cypress.config.local.ts --browser $Browser --spec "cypress/e2e/*${Spec}*.cy.ts"
} else {
    npx cypress run --config-file cypress.config.local.ts --browser $Browser --headless
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
