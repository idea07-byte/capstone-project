@echo off
chcp 65001 >nul
echo Starting BuyIt backend...

java -cp "..\out;lib\postgresql-42.7.4.jar" Main
