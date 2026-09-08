@echo off
chcp 65001 >nul
echo ==================================================
echo       Building BuyIt Full-Stack Marketplace       
echo ==================================================

set ROOT_DIR=%~dp0
cd /d "%ROOT_DIR%"

echo [1/2] Building React Frontend...
if exist "frontend\package.json" (
    cd frontend
    call npm run build
    if %ERRORLEVEL% NEQ 0 (
        echo [WARNING] Frontend build failed or npm not configured. Continuing...
    )
    cd /d "%ROOT_DIR%"
)

echo [2/2] Building Java Backend...
cd backend
call build.bat
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Backend build failed.
    exit /b 1
)
cd /d "%ROOT_DIR%"

echo ==================================================
echo [SUCCESS] BuyIt Marketplace build completed!
echo To launch the application, run: run.bat
echo ==================================================
