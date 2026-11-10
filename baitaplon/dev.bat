@echo off
chcp 65001 > nul
title SLMS Dev Server - Auto Reload Watcher

echo ========================================================================
echo       SLMS DEV SERVER - CHE DO TU DONG BUILD & LIVE RELOAD
echo ========================================================================
echo.
echo [Watcher] Dang khoi chay che do tu dong theo doi thay doi ma nguon...
echo.

cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0watch.ps1"

pause

