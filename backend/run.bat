@echo off
chcp 65001 >nul
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

echo Starting BuyIt backend...

set "JAVA_CMD=java"

if exist "C:\Program Files\Java\jdk-26.0.2.1\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-26.0.2.1\bin\java.exe"
)
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
)

if exist "..\out\buyit.jar" (
    "%JAVA_CMD%" -cp "..\out\buyit.jar;lib\postgresql-42.7.4.jar" Main %*
) else (
    "%JAVA_CMD%" -cp "..\out;lib\postgresql-42.7.4.jar" Main %*
)
