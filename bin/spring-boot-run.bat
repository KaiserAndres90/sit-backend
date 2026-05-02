@echo off
REM Arranca el backend desde la carpeta raiz del proyecto (usa el modulo sit-backend)
cd /d "%~dp0"
mvn -pl sit-backend spring-boot:run
pause
