@echo off
chcp 65001 >nul
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

echo Building BuyIt Marketplace Backend...

set "JAVAC_CMD=javac"
set "JAR_CMD=jar"

if exist "C:\Program Files\Java\jdk-26.0.2.1\bin\javac.exe" (
    set "JAVAC_CMD=C:\Program Files\Java\jdk-26.0.2.1\bin\javac.exe"
    set "JAR_CMD=C:\Program Files\Java\jdk-26.0.2.1\bin\jar.exe"
)
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" (
    set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
    set "JAR_CMD=%JAVA_HOME%\bin\jar.exe"
)

set LIB_DIR=lib
set OUT_DIR=..\out
set RES_DIR=resources

if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

"%JAVAC_CMD%" -cp "%LIB_DIR%\postgresql-42.7.4.jar" -d "%OUT_DIR%" *.java model\*.java service\*.java db\*.java util\*.java

if %ERRORLEVEL% EQU 0 (
    echo Copying resources and libraries...
    if not exist "%OUT_DIR%\resources" mkdir "%OUT_DIR%\resources"
    if not exist "%OUT_DIR%\lib" mkdir "%OUT_DIR%\lib"
    xcopy /Y /E "%RES_DIR%\*" "%OUT_DIR%\resources\" >nul 2>nul
    copy /Y "%RES_DIR%\database.properties" "%OUT_DIR%\database.properties" >nul 2>nul
    copy /Y "%LIB_DIR%\*.jar" "%OUT_DIR%\lib\" >nul 2>nul

    echo Creating executable buyit.jar...
    echo Main-Class: Main> "%OUT_DIR%\manifest.txt"
    echo Class-Path: lib/postgresql-42.7.4.jar resources/>> "%OUT_DIR%\manifest.txt"
    "%JAR_CMD%" cfm "%OUT_DIR%\buyit.jar" "%OUT_DIR%\manifest.txt" -C "%OUT_DIR%" .
    del "%OUT_DIR%\manifest.txt" >nul 2>nul

    echo Build successful. Classes and buyit.jar in %OUT_DIR%
) else (
    echo Build failed.
    exit /b 1
)
