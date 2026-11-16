@echo off
chcp 65001 > nul
title Khoi dong Dich vu Oracle 23ai Database

echo ========================================================================
echo        KHOI DONG DICH VU ORACLE DATABASE 23ai TREN WINDOWS
echo ========================================================================
echo.

:: Kiem tra quyen Administrator
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [THONG BAO] Dang yeu cau quyen Administrator de bat dich vu Windows...
    powershell -Command "Start-Process cmd -ArgumentList '/k \"\"%~f0\"\"' -Verb RunAs"
    exit /b
)

cd /d "%~dp0"
set "ORACLE_HOME=C:\app\sumaytinh0912350956\product\26ai\dbhomeFree"
set "ORACLE_SID=FREE"
set "PATH=%ORACLE_HOME%\bin;%PATH%"

echo [1/3] Dang bat dich vu OracleServiceFREE...
net start OracleServiceFREE 2>nul
oradim -STARTUP -SID FREE -STARTTYPE srvc,inst 2>nul

echo.
echo [2/3] Dang bat dich vu Listener cong 1521...
net start OracleOraDB23Home1TNSListener 2>nul

echo.
echo ========================================================================
echo [THANH CONG] Da kich hoat dich vu va instance Oracle Database!
echo ========================================================================
echo.

echo [3/3] Dang kiem tra va khoi tao database slms_user...
call database\init_oracle.bat

pause
