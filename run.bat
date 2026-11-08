@echo off
title DineTrack Restaurant Management System (Web Application)
cd /d "%~dp0"

echo [DineTrack] Compiling application source code...
javac -cp ".;mysql-connector-j-9.1.0.jar" DineTrack.java dinetrack\db\*.java dinetrack\exception\*.java dinetrack\model\*.java dinetrack\payment\*.java dinetrack\dao\*.java dinetrack\web\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [Error] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [DineTrack] Launching Web Application...
java -cp ".;mysql-connector-j-9.1.0.jar" DineTrack %*
pause
