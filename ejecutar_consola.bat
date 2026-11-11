@echo off
title Depuracion Papeleria de Rosita (CONARY)
cd /d "%~dp0"

set "JAVA_CMD=C:\Program Files\Java\jre1.8.0_511\bin\java.exe"
if not exist "%JAVA_CMD%" (
    set "JAVA_CMD=java"
)

echo ===================================================
echo INICIANDO PAPELERIA DE ROSITA EN MODO CONSOLA
echo ===================================================
"%JAVA_CMD%" -Xms64m -Xmx256m -XX:+UseSerialGC -cp "target\papeleria-rosita.jar;lib\mysql-connector-j-8.0.33.jar" com.conary.rosita.MainLauncher

echo.
echo La aplicacion ha finalizado.
pause

