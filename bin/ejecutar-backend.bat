@echo off
REM Ejecuta Spring Boot desde la carpeta correcta (sit-backend)
cd /d "%~dp0sit-backend"
if not exist "pom.xml" (
    echo ERROR: No se encuentra sit-backend\pom.xml
    pause
    exit /b 1
)
echo Directorio: %CD%
echo Si falla la conexion a PostgreSQL, use ejecutar-backend-dev.bat o defina DB_PASSWORD.
mvn spring-boot:run
pause
