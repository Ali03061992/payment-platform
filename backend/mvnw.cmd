@echo off
setlocal

REM Bootstrap Maven Wrapper - downloads Maven 3.9.16 on first use
set "MAVEN_VERSION=3.9.16"
set "MAVEN_DIST_DIR=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_HOME=%MAVEN_DIST_DIR%\apache-maven-%MAVEN_VERSION%"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  echo [mvnw] Downloading Maven %MAVEN_VERSION% ...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; $u='https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip'; $z=\"$env:TEMP\apache-maven-%MAVEN_VERSION%-bin.zip\"; Invoke-WebRequest -Uri $u -OutFile $z; if (-not (Test-Path '%MAVEN_DIST_DIR%')) { New-Item -ItemType Directory -Path '%MAVEN_DIST_DIR%' -Force | Out-Null }; Expand-Archive -Path $z -DestinationPath '%MAVEN_DIST_DIR%' -Force; Remove-Item $z"
  if errorlevel 1 (
    echo [mvnw] ERROR: failed to download Maven
    exit /b 1
  )
)

call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%