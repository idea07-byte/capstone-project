@echo off
chcp 65001 >nul
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

echo Starting BuyIt backend...

set "JAVA_BIN=C:\Program Files\Java\jdk-26.0.2.1\bin"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_BIN=%JAVA_HOME%\bin"

if exist "..\out\buyit.jar" (
    "%JAVA_BIN%\java.exe" -cp "..\out\buyit.jar;lib\postgresql-42.7.4.jar" Main %*
) else (
    "%JAVA_BIN%\java.exe" -cp "..\out;lib\postgresql-42.7.4.jar" Main %*
)
