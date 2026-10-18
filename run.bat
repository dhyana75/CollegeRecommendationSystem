@echo off
setlocal
cd /d "%~dp0"
call compile.bat
if errorlevel 1 (
    echo Build failed. Fix the compiler errors shown above.
    pause
    exit /b 1
)
java -cp "out;lib\mysql-connector-j-26.7.0.jar" com.college.Main
if errorlevel 1 pause
