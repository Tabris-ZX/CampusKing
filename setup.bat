@echo off
setlocal

cd /d "%~dp0"

start "backend-dev" cmd /k "cd /d ""%~dp0"" && mvn spring-boot:run"
    echo backend success

ping 127.0.0.1 -n 4 >nul

start "frontend-dev" cmd /k "cd /d ""%~dp0webui"" && npm run dev"
    echo frontend success

exit /b 0
