@echo off
REM Backend con H2 en memoria (no usa PostgreSQL)
cd /d "%~dp0sit-backend"
if not exist "pom.xml" (
    echo ERROR: No se encuentra sit-backend\pom.xml
    pause
    exit /b 1
)
echo Perfil: dev (H2 en memoria) — Directorio: %CD%
set SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
pause
