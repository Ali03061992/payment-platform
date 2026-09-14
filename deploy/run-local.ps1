# Payment Platform - Local Runner
# Run infrastructure in Docker, backend/frontend in IDE
#
# Usage:
#   .\run-local.ps1              Start infrastructure only
#   .\run-local.ps1 -Build       Rebuild infrastructure images
#   .\run-local.ps1 -Stop        Stop infrastructure
#
# After running this script:
#   1. Open backend/ in IntelliJ IDEA
#   2. Run all services with profile "local" (use ALL_SERVICES.run.xml)
#   3. Open payment-platform-ui/ in another terminal: npm start
#   4. Access Angular at http://localhost:4200

param(
    [switch]$Build,
    [switch]$Stop
)

Set-Location $PSScriptRoot

if ($Stop) {
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  Payment Platform - Stopping infra..." -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    docker compose -f docker-compose.dev.yml down
    Write-Host "Infrastructure stopped." -ForegroundColor Green
    exit 0
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Payment Platform - Local Dev Mode" -ForegroundColor Cyan
Write-Host "  Infrastructure only (Docker)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check prerequisites
$errors = @()

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    $errors += "Java 26 not found. Install Eclipse Temurin 26."
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue) -and -not (Test-Path "../backend/mvnw")) {
    $errors += "Maven not found. Use backend/mvnw wrapper."
}

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    $errors += "Node.js 18+ not found."
}

if ($errors.Count -gt 0) {
    Write-Host "Prerequisites missing:" -ForegroundColor Red
    $errors | ForEach-Object { Write-Host "  - $_" -ForegroundColor Yellow }
    Write-Host ""
    Write-Host "You can still run services from IntelliJ IDEA." -ForegroundColor Yellow
    Write-Host ""
}

# Start infrastructure
Write-Host "[1/3] Starting infrastructure containers..." -ForegroundColor Yellow

if ($Build) {
    docker compose -f docker-compose.dev.yml up -d --build
} else {
    docker compose -f docker-compose.dev.yml up -d
}

Write-Host "[2/3] Waiting for MySQL, RabbitMQ, Redis..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# Check health
Write-Host "[3/3] Checking services..." -ForegroundColor Yellow
$mysqlReady = docker exec payment-mysql mysqladmin ping -h localhost -u root -prabbit-password-change-me 2>&1
if ($mysqlReady -match "alive") {
    Write-Host "  MySQL:      OK (port 3307)" -ForegroundColor Green
} else {
    Write-Host "  MySQL:      Starting... (port 3307)" -ForegroundColor Yellow
}

$rabbitReady = docker exec payment-rabbitmq rabbitmq-diagnostics -q ping 2>&1
if ($rabbitReady -match "pong") {
    Write-Host "  RabbitMQ:   OK (ports 5673/15673)" -ForegroundColor Green
} else {
    Write-Host "  RabbitMQ:   Starting... (ports 5673/15673)" -ForegroundColor Yellow
}

$redisReady = docker exec payment-redis redis-cli ping 2>&1
if ($redisReady -match "PONG") {
    Write-Host "  Redis:      OK (port 6379)" -ForegroundColor Green
} else {
    Write-Host "  Redis:      Starting... (port 6379)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Infrastructure ready!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host ""
Write-Host "  1. Backend (IntelliJ IDEA):" -ForegroundColor White
Write-Host "     - Open backend/ in IntelliJ" -ForegroundColor Gray
Write-Host "     - Run ALL_SERVICES.run.xml (profile: local)" -ForegroundColor Gray
Write-Host "     - Or run each service individually" -ForegroundColor Gray
Write-Host ""
Write-Host "  2. Frontend (terminal):" -ForegroundColor White
Write-Host "     cd payment-platform-ui" -ForegroundColor Gray
Write-Host "     npm install" -ForegroundColor Gray
Write-Host "     npm start" -ForegroundColor Gray
Write-Host ""
Write-Host "  3. Access:" -ForegroundColor White
Write-Host "     Angular App:    http://localhost:4200" -ForegroundColor Gray
Write-Host "     API Gateway:    http://localhost:8081" -ForegroundColor Gray
Write-Host "     Swagger:        http://localhost:8081/swagger-ui.html" -ForegroundColor Gray
Write-Host "     Adminer:        http://localhost:8086" -ForegroundColor Gray
Write-Host "     RabbitMQ:       http://localhost:15673 (payment/rabbit)" -ForegroundColor Gray
Write-Host "     Mailpit:        http://localhost:8025" -ForegroundColor Gray
Write-Host ""
Write-Host "  Default login: system.admin / Admin@123" -ForegroundColor Yellow
Write-Host ""
