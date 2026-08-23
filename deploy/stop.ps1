# Payment Platform - Stop Script
# Usage:
#   .\stop.ps1          Stop all services (keeps data)
#   .\stop.ps1 -Clean   Stop and remove volumes (full reset)

param(
    [switch]$Clean
)

Set-Location $PSScriptRoot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Payment Platform - Stopping..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if ($Clean) {
    Write-Host "Stopping and removing volumes..." -ForegroundColor Yellow
    docker compose down -v
    Write-Host "Done. All data removed." -ForegroundColor Green
} else {
    Write-Host "Stopping containers..." -ForegroundColor Yellow
    docker compose down
    Write-Host "Done. Data preserved." -ForegroundColor Green
}
