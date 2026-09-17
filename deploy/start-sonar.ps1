#Requires -Version 5.1
# Start SonarQube monorepo local (isolé, sans MySQL/Rabbit)
# Usage : .\deploy\start-sonar.ps1  [-Logs]
param([switch]$Logs)

$ErrorActionPreference = "Stop"
$compose = "deploy/docker-compose.sonar.yml"

Write-Host "== Payment Platform – SonarQube local (monorepo) ==" -ForegroundColor Cyan
Write-Host "Compose : $compose"

docker compose -f $compose up -d
if ($LASTEXITCODE -ne 0) { throw "docker compose up failed" }

Write-Host "`nAttente SonarQube (http://localhost:9000) ..." -ForegroundColor Yellow
$max = 60
for ($i=1; $i -le $max; $i++) {
  try {
    $r = Invoke-RestMethod -Uri "http://localhost:9000/api/system/status" -TimeoutSec 2 -ErrorAction SilentlyContinue
    if ($r.status -eq "UP") {
      Write-Host "`nSonarQube UP en ${i} tentatives." -ForegroundColor Green
      break
    }
    Write-Host "  [$i/$max] status=$($r.status)" -ForegroundColor DarkGray
  } catch {
    Write-Host "  [$i/$max] en demarrage..." -ForegroundColor DarkGray
  }
  Start-Sleep -Seconds 5
  if ($i -eq $max) { throw "SonarQube n'est pas UP apres $max tentatives. Logs: docker compose -f $compose logs sonarqube" }
}

Write-Host @"
`n--- SonarQube pret ---
UI  : http://localhost:9000  (admin / admin - changer au 1er login)
API : http://localhost:9000/api/system/status

1) Genere un token : http://localhost:9000/account/security  (ou Administration > Security > Tokens)
   ex : squ_xxx
2) Lance le scan monorepo :
   `$env:SONAR_TOKEN="squ_xxx"; .\deploy\scan-monorepo.ps1`

Logs : docker compose -f $compose logs -f sonarqube
Stop : docker compose -f $compose down     (ajoute -v pour effacer les donnees)
"@ -ForegroundColor Cyan

if ($Logs) { docker compose -f $compose logs -f sonarqube }
