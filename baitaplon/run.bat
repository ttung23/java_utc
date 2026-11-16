@echo off
chcp 65001 > nul
setlocal

echo ========================================================================
echo       SMART LIBRARY MANAGEMENT SYSTEM (SLMS) - KHOI DONG HE THONG
echo ========================================================================
echo.

cd /d "%~dp0"

REM 1. Tim duong dan JDK
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" goto java_ready
if exist "C:\Program Files\Java\jdk-22\bin\javac.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-22"
    goto java_ready
)
for /d %%D in ("%ProgramFiles%\Java\jdk-*") do (
    if exist "%%~fD\bin\javac.exe" set "JAVA_HOME=%%~fD"
)

if not defined JAVA_HOME (
    echo [LOI] Khong tim thay JDK. Hay cai JDK 17+ hoac thiet lap bien JAVA_HOME.
    pause
    exit /b 1
)

:java_ready
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM 2. Dam bao thu vien da san sang
if not exist "lib" mkdir "lib"
if not exist "lib\ojdbc11.jar" (
    if exist "C:\app\sumaytinh0912350956\product\26ai\dbhomeFree\jdbc\lib\ojdbc11.jar" (
        copy "C:\app\sumaytinh0912350956\product\26ai\dbhomeFree\jdbc\lib\ojdbc11.jar" "lib\ojdbc11.jar" > nul 2>&1
    )
)
if not exist "lib\gson.jar" (
    if exist "C:\app\sumaytinh0912350956\product\26ai\dbhomeFree\sqlcl\lib\gson.jar" (
        copy "C:\app\sumaytinh0912350956\product\26ai\dbhomeFree\sqlcl\lib\gson.jar" "lib\gson.jar" > nul 2>&1
    )
)

REM 3. Bien dich ma nguon Java
echo [1/2] Dang kiem tra va bien dich ma nguon Java...
if not exist "bin" mkdir "bin"
dir /s /b src\main\java\*.java > sources.txt
javac -encoding UTF-8 -cp "lib/gson.jar;lib/ojdbc11.jar" -d bin @sources.txt
if errorlevel 1 (
    echo [LOI] Bien dich that bai!
    if exist sources.txt del sources.txt
    pause
    exit /b 1
)
if exist sources.txt del sources.txt
echo [1/2] Bien dich thanh cong!

REM 4. Khoi chay Web Server
echo.
echo ========================================================================
echo [2/2] DANG KHOI CHAY SERVER TAI: http://localhost:8080
echo Hay mo trinh duyet (Chrome/Edge) va truy cap: http://localhost:8080
echo Nhan Ctrl+C de dung server.
echo ========================================================================
echo.

java -cp "bin;lib/gson.jar;lib/ojdbc11.jar" com.slms.App %*

pause
