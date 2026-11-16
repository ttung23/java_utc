@echo off
chcp 65001 > nul
setlocal

echo ========================================================================
echo       SMART LIBRARY MANAGEMENT SYSTEM (SLMS) - KHOI DONG HE THONG
echo ========================================================================
echo.

cd /d "%~dp0"

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" goto java_ready
set "JAVA_HOME="
for /d %%D in ("%ProgramFiles%\Java\jdk-*") do (
    if exist "%%~fD\bin\javac.exe" set "JAVA_HOME=%%~fD"
)

if not defined JAVA_HOME (
    echo [LOI] Khong tim thay JDK. Hay cai JDK 17+ hoac sua bien JAVA_HOME.
    pause
    exit /b 1
)

:java_ready
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "PORT=%~1"
if not defined PORT set "PORT=8081"

if not exist "mvnw.cmd" (
    echo [LOI] Khong tim thay Maven Wrapper trong thu muc baitaplon.
    pause
    exit /b 1
)

echo [1/1] Dang khoi dong server tai http://localhost:%PORT%
echo Nhan Ctrl+C de dung server.
echo.

call "mvnw.cmd" exec:java "-Dexec.args=--port=%PORT%"
if errorlevel 1 (
    echo [LOI] Server khong khoi dong duoc.
    pause
    exit /b 1
)
