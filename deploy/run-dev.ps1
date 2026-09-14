# Payment Platform - Dev Runner
# Full stack with Docker (infrastructure + backend + frontend)
#
# Usage:
#   .\run-dev.ps1              Start all services
#   .\run-dev.ps1 -Build       Rebuild all images and start
#   .\run-dev.ps1 -Stop        Stop all services
#   .\run-dev.ps1 -Logs        Tail logs
#
# Access:
#   Angular App:    http://localhost:8080
#   API Gateway:    http://localhost:8081
#   Swagger:        http://localhost:8081/swagger-ui.html
#   Adminer:        http://localhost:8086
#   RabbitMQ:       http://localhost:15673

param(
    [switch]$Build,
    [switch]$Stop,
    [switch]$Logs
)

Set-Location $PSScriptRoot

if ($Stop) {
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  Payment Platform - Stopping..." -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    docker compose down
    Write-Host "All services stopped. Data preserved." -ForegroundColor Green
    exit 0
}

if ($Logs) {
    docker compose logs -f
    exit 0
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Payment Platform - Dev Mode (Full)" -ForegroundColor Cyan
Write-Host "  All services in Docker" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check Docker
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "Docker not found. Install Docker Desktop." -ForegroundColor Red
    exit 1
}

$dockerRunning = docker info 2>&1 | Select-String "Server Version"
if (-not $dockerRunning) {
    Write-Host "Docker daemon not running. Start Docker Desktop." -ForegroundColor Red
    exit 1
}

# Start full stack
Write-Host "[1/2] Building and starting all services..." -ForegroundColor Yellow

if ($Build) {
    docker compose up --build -d
} else {
    docker compose up -d
}

Write-Host "[2/2] Waiting for services to start..." -ForegroundColor Yellow
Write-Host "       (This may take 2-3 minutes on first run)" -ForegroundColor Gray
Start-Sleep -Seconds 20

# Health checks
Write-Host ""
Write-Host "Service Status:" -ForegroundColor Cyan

$services = @(
    @{Name="MySQL";      Check="docker exec payment-mysql mysqladmin ping -h localhost -u root -prabbit-password-change-me 2>&1"; Match="alive"; Port="3307"},
    @{Name="RabbitMQ";   Check="docker exec payment-rabbitmq rabbitmq-diagnostics -q ping 2>&1"; Match="pong"; Port="5673/15673"},
    @{Name="Redis";      Check="docker exec payment-redis redis-cli ping 2>&1"; Match="PONG"; Port="6379"},
    @{Name="API Gateway"; Check="curl -s http://localhost:8081/actuator/health 2>&1"; Match="UP"; Port="8081"},
    @{Name="Angular";    Check="curl -s http://localhost:8080 2>&1"; Match="<!DOCTYPE"; Port="8080"}
)

foreach ($svc in $services) {
    $result = Invoke-Expression $svc.Check 2>&1
    if ($result -match $svc.Match) {
        Write-Host "  $($svc.Name): OK (port $($svc.Port))" -ForegroundColor Green
    } else {
        Write-Host "  $($svc.Name): Starting... (port $($svc.Port))" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  All services started!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Angular App:      http://localhost:8080" -ForegroundColor White
Write-Host "  API Gateway:      http://localhost:8081" -ForegroundColor White
Write-Host "  Swagger:          http://localhost:8081/swagger-ui.html" -ForegroundColor White
Write-Host "  Adminer (MySQL):  http://localhost:8086" -ForegroundColor White
Write-Host "  RabbitMQ Mgmt:    http://localhost:15673" -ForegroundColor White
Write-Host "  Mailpit:          http://localhost:8025" -ForegroundColor White
Write-Host ""
Write-Host "  Default login: system.admin / Admin@123" -ForegroundColor Yellow
Write-Host ""
Write-Host "  Commands:" -ForegroundColor Cyan
Write-Host "    .\run-dev.ps1 -Stop     Stop all services" -ForegroundColor Gray
Write-Host "    .\run-dev.ps1 -Logs     Tail logs" -ForegroundColor Gray
Write-Host ""
