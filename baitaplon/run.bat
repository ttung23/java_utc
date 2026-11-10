@echo off
chcp 65001 > nul

echo ========================================================================
echo       SMART LIBRARY MANAGEMENT SYSTEM (SLMS) - KHOI DONG HE THONG
echo ========================================================================
echo.

cd /d "%~dp0"

REM 1. Uu tien duong dan JDK 22 tren he thong
if exist "C:\Program Files\Java\jdk-22\bin" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-22"
    set "PATH=C:\Program Files\Java\jdk-22\bin;%PATH%"
)

REM 2. Dam bao thu vien da san sang
if not exist "lib" mkdir "lib"
if not exist "lib\ojdbc11.jar" (
    copy "C:\app\sumaytinh0912350956\product\26ai\dbhomeFree\jdbc\lib\ojdbc11.jar" "lib\ojdbc11.jar" > nul 2>&1
)
if not exist "lib\gson.jar" (
    copy "C:\app\sumaytinh0912350956\product\26ai\dbhomeFree\sqlcl\lib\gson.jar" "lib\gson.jar" > nul 2>&1
)

REM 3. Kiem tra va bien dich ma nguon Java neu can
if not exist "bin\com\slms\App.class" (
    echo [1/2] Dang bien dich cac file ma nguon Java...
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
) else (
    echo [1/2] Ma nguon da duoc bien dich san sang!
)

REM 4. Khoi chay Web Server
echo.
echo ========================================================================
echo [2/2] DANG KHOI CHAY SERVER TAI: http://localhost:8080
echo Hay mo trinh duyet (Chrome/Edge) va truy cap: http://localhost:8080
echo Nhan Ctrl+C de dung server.
echo ========================================================================
echo.

java -cp "bin;lib/gson.jar;lib/ojdbc11.jar" com.slms.App

pause
