@echo off
setlocal
cd /d "%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0compile.ps1"
if errorlevel 1 (
    echo.
    echo Build failed. Fix the compiler errors shown above.
    exit /b 1
)

echo.
echo Build successful.
exit /b 0
