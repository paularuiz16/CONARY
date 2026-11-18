@echo off
title Generar JAR - Papeleria Rosita (CONARY)
cd /d "%~dp0"

echo ===================================================
echo EMPAQUETANDO PAPELERIA DE ROSITA EN .JAR
echo ===================================================

set "JAR_CMD=C:\Program Files\Java\jdk-27.0.1.0.3\bin\jar.exe"
if not exist "%JAR_CMD%" (
    set "JAR_CMD=jar"
)

"%JAR_CMD%" -cfm "target\papeleria-rosita.jar" "manifest.txt" -C "target\classes" .

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [EXITO] Archivo target\papeleria-rosita.jar generado correctamente.
) else (
    echo.
    echo [ERROR] No se pudo empaquetar el archivo JAR.
)

pause
