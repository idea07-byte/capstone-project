@echo off
chcp 65001 >nul
echo Building BuyIt Marketplace...

set JAVA_BIN=C:\Program Files\Java\jdk-26\bin
set LIB_DIR=lib
set OUT_DIR=..\out
set RES_DIR=resources

if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

"%JAVA_BIN%\javac.exe" -cp "%LIB_DIR%\postgresql-42.7.4.jar" -d "%OUT_DIR%" *.java model\*.java service\*.java db\*.java

if %ERRORLEVEL% EQU 0 (
    echo Copying resources...
    if not exist "%OUT_DIR%\resources" mkdir "%OUT_DIR%\resources"
    xcopy /Y /E "%RES_DIR%\*" "%OUT_DIR%\resources\"
    copy /Y "%RES_DIR%\database.properties" "%OUT_DIR%\database.properties" >nul
    echo Build successful. Classes in %OUT_DIR%
) else (
    echo Build failed.
    exit /b 1
)
