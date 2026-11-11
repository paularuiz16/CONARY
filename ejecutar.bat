@echo off
title Sistema Papeleria de Rosita (CONARY)
REM ====================================================================
REM Lanzador portable optimizado para Windows 8 y 2 GB de RAM
REM ====================================================================

cd /d "%~dp0"

REM Detectar JRE 8 de 64 o 32 bits instalado en el sistema
set "JAVA_CMD=C:\Program Files\Java\jre1.8.0_511\bin\javaw.exe"
if not exist "%JAVA_CMD%" (
    set "JAVA_CMD=javaw"
)

echo Iniciando Sistema Papeleria de Rosita...
echo Memoria: Heap inicial 64MB, Maximo 256MB, SerialGC.

start "" "%JAVA_CMD%" -Xms64m -Xmx256m -XX:+UseSerialGC -cp "target\papeleria-rosita.jar;lib\mysql-connector-j-8.0.33.jar" com.conary.rosita.MainLauncher

exit
