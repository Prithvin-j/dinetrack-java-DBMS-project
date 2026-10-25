@echo off
title DineTrack Restaurant Management System
cd /d "%~dp0"
java -cp "mysql-connector-j-9.1.0.jar" DineTrack.java
pause
