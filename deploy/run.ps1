# Payment Platform - Start/Stop Scripts
# Usage:
#   .\run.ps1          Start all services
#   .\run.ps1 -Build   Rebuild images and start
#   .\stop.ps1         Stop all services
#   .\stop.ps1 -Clean  Stop and remove volumes

param(
    [switch]$Build
)

Set-Location $PSScriptRoot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Payment Platform - Starting..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if ($Build) {
    Write-Host "[1/2] Building images..." -ForegroundColor Yellow
    docker compose up --build -d
} else {
    Write-Host "[1/2] Starting containers..." -ForegroundColor Yellow
    docker compose up -d
}

Write-Host "[2/2] Waiting for services..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  All services started!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Angular App:      http://localhost:8080" -ForegroundColor White
Write-Host "  Register:         http://localhost:8080/register" -ForegroundColor White
Write-Host "  Login:            http://localhost:8080/login" -ForegroundColor White
Write-Host "  API Gateway:      http://localhost:8081" -ForegroundColor White
Write-Host "  Swagger:          http://localhost:8081/swagger-ui.html" -ForegroundColor White
Write-Host "  Adminer (MySQL):  http://localhost:8086" -ForegroundColor White
Write-Host "  RabbitMQ Mgmt:    http://localhost:15673" -ForegroundColor White
Write-Host ""
Write-Host "  Default login: system.admin / Admin@123" -ForegroundColor Yellow
Write-Host ""
