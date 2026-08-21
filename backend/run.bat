@echo off
chcp 65001 >nul
echo Starting BuyIt backend...

set JAVA_BIN=C:\Program Files\Java\jdk-26\bin
"%JAVA_BIN%\java.exe" -cp "..\out;lib\postgresql-42.7.4.jar" Main
