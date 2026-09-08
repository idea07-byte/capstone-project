@echo off
chcp 65001 >nul
echo ==================================================
echo       Starting BuyIt Full-Stack Marketplace       
echo ==================================================

set ROOT_DIR=%~dp0
cd /d "%ROOT_DIR%"

cd backend
call run.bat %*
cd /d "%ROOT_DIR%"
