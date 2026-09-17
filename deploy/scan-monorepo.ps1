#Requires -Version 5.1
# Scan monorepo local : backend (JaCoCo XML) + frontend (lcov) -> 1 projet SonarQube "payment-platform"
# Prerequis : SonarQube UP (.\deploy\start-sonar.ps1), Java 26, Node 20+
# Usage :
#   $env:SONAR_TOKEN="squ_xxx"
#   .\deploy\scan-monorepo.ps1                    # scan via docker scanner (recommande, pas d'install)
#   .\deploy\scan-monorepo.ps1 -UseNpx            # scan via npx sonar-scanner (npm i -g sonar-scanner)
#   .\deploy\scan-monorepo.ps1 -SkipBuild         # saute mvn/npm si deja faits

param(
  [switch]$UseNpx,
  [switch]$SkipBuild,
  [string]$SonarHost = "http://localhost:9000",
  [string]$ProjectKey = "payment-platform"
)

$ErrorActionPreference = "Stop"

function Assert-Token {
  if (-not $env:SONAR_TOKEN -or $env:SONAR_TOKEN.Trim() -eq "") {
    throw @"
SONAR_TOKEN manquant.
1) Ouvre http://localhost:9000/account/security
2) Genere un token (ex: squ_xxx)
3) Relance :
   `$env:SONAR_TOKEN="squ_xxx"; .\deploy\scan-monorepo.ps1
"@
  }
}

function Test-SonarUp {
  try {
    $s = Invoke-RestMethod -Uri "$SonarHost/api/system/status" -TimeoutSec 3 -ErrorAction Stop
    if ($s.status -ne "UP") { throw "Sonar status=$($s.status)" }
    Write-Host "SonarQube UP ($SonarHost) – version $($s.version)" -ForegroundColor Green
  } catch {
    throw "SonarQube injoignable a $SonarHost. Lance d'abord: .\deploy\start-sonar.ps1  `n$_"
  }
}

Assert-Token
Test-SonarUp

$root = (Resolve-Path "$PSScriptRoot/..").Path
Set-Location $root
Write-Host "`n== Scan monorepo : $ProjectKey ==" -ForegroundColor Cyan
Write-Host "Root        : $root"
Write-Host "Sonar Host  : $SonarHost"
Write-Host "SONAR_TOKEN : $($env:SONAR_TOKEN.Substring(0,[Math]::Min(6,$env:SONAR_TOKEN.Length)))***"

if (-not $SkipBuild) {
  Write-Host "`n--- [1/3] Backend : mvn verify (JaCoCo XML) ---" -ForegroundColor Yellow
  # Clean verify genere backend/**/target/site/jacoco/jacoco.xml + aggregate
  & "$root/backend/mvnw.cmd" -f "$root/backend/pom.xml" clean verify -DskipTests=false
  if ($LASTEXITCODE -ne 0) { throw "Backend verify failed" }

  Write-Host "`n--- [2/3] Frontend : npm run test:coverage (lcov) ---" -ForegroundColor Yellow
  Push-Location "$root/payment-platform-ui"
  try {
    # --legacy-peer-deps requis pour Angular 16/21 mix
    npm ci --legacy-peer-deps
    if ($LASTEXITCODE -ne 0) { throw "npm ci failed" }
    # ChromeHeadlessCI defini dans karma.conf.js (flags --no-sandbox)
    npx ng test --code-coverage --watch=false --browsers=ChromeHeadlessCI
    if ($LASTEXITCODE -ne 0) { throw "ng test coverage failed – assure Chrome installe (ChromeHeadlessCI)" }
    if (-not (Test-Path "$root/payment-platform-ui/coverage/payment-platform-ui/lcov.info")) {
      if (Test-Path "$root/payment-platform-ui/coverage/lcov.info") {
        Write-Host "lcov.info trouve a coverage/lcov.info (fallback)" -ForegroundColor DarkYellow
      } else {
        Write-Warning "Aucun lcov.info trouve. Sonar affichera 0% coverage frontend."
        Get-ChildItem -Recurse -Filter "lcov.info" -ErrorAction SilentlyContinue | ForEach-Object { Write-Host "  trouve: $($_.FullName)" }
      }
    } else {
      Write-Host "lcov OK : payment-platform-ui/coverage/payment-platform-ui/lcov.info" -ForegroundColor Green
    }
  } finally { Pop-Location }

  # Frontend legacy (optionnel) – si tu veux aussi scanner frontend/ (Angular 16)
  if (Test-Path "$root/frontend/package.json") {
    Write-Host "`n--- [2b] Frontend legacy (frontend/) : test:coverage (optionnel) ---" -ForegroundColor DarkGray
    Push-Location "$root/frontend"
    try {
      npm ci --legacy-peer-deps 2>$null
      npx ng test --code-coverage --watch=false --browsers=ChromeHeadlessCI 2>$null
      if (Test-Path "$root/frontend/coverage/payment-platform-ui/lcov.info") {
        Write-Host "frontend lcov OK" -ForegroundColor Green
      }
    } catch { Write-Host "frontend legacy skip (non bloquant): $_" -ForegroundColor DarkGray }
    finally { Pop-Location }
  }
} else {
  Write-Host "`n[SkipBuild] Verification rapide des rapports existants..." -ForegroundColor DarkYellow
  Get-ChildItem -Path "$root/backend" -Recurse -Filter "jacoco.xml" | Select-Object FullName | ForEach-Object { Write-Host "  jacoco: $($_.FullName)" }
  Get-ChildItem -Path "$root/payment-platform-ui/coverage" -Recurse -Filter "lcov.info" -ErrorAction SilentlyContinue | ForEach-Object { Write-Host "  lcov: $($_.FullName)" }
}

Write-Host "`n--- [3/3] Sonar scanner (monorepo) ---" -ForegroundColor Yellow
Write-Host "Properties : $root/sonar-project.properties (projectKey=$ProjectKey)"

if ($UseNpx) {
  # Necessite: npm i -g sonar-scanner  ou npx sonar-scanner
  Write-Host "Mode : npx sonar-scanner" -ForegroundColor DarkGray
  npx sonar-scanner -Dsonar.host.url=$SonarHost -Dsonar.token=$env:SONAR_TOKEN
  if ($LASTEXITCODE -ne 0) { throw "sonar-scanner (npx) failed" }
} else {
  # Mode Docker (recommande Windows) – evite d'installer Java/Scanner localement
  Write-Host "Mode : docker sonarsource/sonar-scanner-cli" -ForegroundColor DarkGray
  # host.docker.internal -> localhost de l'hote depuis le container
  $dockerHost = $SonarHost -replace "localhost", "host.docker.internal" -replace "127.0.0.1", "host.docker.internal"
  Write-Host "Docker SONAR_HOST_URL : $dockerHost"
  docker run --rm `
    --add-host=host.docker.internal:host-gateway `
    -v "${root}:/usr/src" `
    -w /usr/src `
    -e SONAR_TOKEN="$env:SONAR_TOKEN" `
    sonarsource/sonar-scanner-cli:latest `
    -Dsonar.host.url=$dockerHost `
    -Dsonar.token="$env:SONAR_TOKEN"
  if ($LASTEXITCODE -ne 0) {
    Write-Host "`nAstuce : si l'image n'est pas tiree, lance : docker pull sonarsource/sonar-scanner-cli" -ForegroundColor DarkYellow
    throw "docker sonar-scanner failed"
  }
}

Write-Host @"

== Scan termine ==
Projet : $ProjectKey
UI     : $SonarHost/dashboard?id=$ProjectKey
Attends 10-20s puis rafraichis. Quality Gate : $SonarHost/quality_gates/show/built-in

Pour re-scanner sans rebuild : .\deploy\scan-monorepo.ps1 -SkipBuild
Pour scanner frontend seul   : cd payment-platform-ui; sonar-scanner -Dsonar.host.url=$SonarHost
Pour scanner backend seul    : cd backend; ./mvnw.cmd sonar:sonar -Dsonar.host.url=$SonarHost

Stop SonarQube : docker compose -f deploy/docker-compose.sonar.yml down
"@ -ForegroundColor Green
