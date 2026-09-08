@echo off
chcp 65001 >nul
echo Starting BuyIt backend...

set "JAVA_BIN=C:\Program Files\Java\jdk-26.0.2.1\bin"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_BIN=%JAVA_HOME%\bin"

"%JAVA_BIN%\java.exe" -cp "..\out;lib\postgresql-42.7.4.jar" Main %*
