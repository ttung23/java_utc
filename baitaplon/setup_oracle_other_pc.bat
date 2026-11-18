@echo off
chcp 65001 >nul
title SLMS - Setup Oracle Database (Universal)
setlocal EnableDelayedExpansion

echo ========================================================================
echo   SLMS - SETUP ORACLE DATABASE TREN MAY MOI (TU DONG DETECT DUONG DAN)
echo ========================================================================
echo.

:: =========================================================================
:: BUOC 0: Kiem tra quyen Administrator
:: =========================================================================
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [CANH BAO] Can quyen Administrator de bat dich vu Oracle.
    echo [INFO] Dang yeu cau quyen Administrator...
    powershell -Command "Start-Process cmd -ArgumentList '/k \"\"%~f0\"\"' -Verb RunAs"
    exit /b
)

cd /d "%~dp0"

:: =========================================================================
:: BUOC 1: Tu dong tim ORACLE_HOME tren may nay
:: =========================================================================
echo [1/5] Dang tim Oracle Database tren may nay...

set "ORACLE_HOME="

REM Cach 1: Doc tu Registry (chinh xac nhat)
for /f "tokens=2*" %%A in ('reg query "HKLM\SOFTWARE\ORACLE" /v ORACLE_HOME 2^>nul') do set "ORACLE_HOME=%%B"
if defined ORACLE_HOME if exist "!ORACLE_HOME!\bin\sqlplus.exe" goto oracle_found

REM Cach 2: Tim trong Registry subkeys
for /f "tokens=*" %%K in ('reg query "HKLM\SOFTWARE\ORACLE" 2^>nul') do (
    for /f "tokens=2*" %%A in ('reg query "%%K" /v ORACLE_HOME 2^>nul') do (
        if exist "%%B\bin\sqlplus.exe" (
            set "ORACLE_HOME=%%B"
            goto oracle_found
        )
    )
)

REM Cach 3: Tim theo thu muc pho bien
for /d %%D in (
    "C:\app\*\product\*\dbhomeFree"
    "C:\app\*\product\*\dbhome_1"
    "C:\oracle\*"
    "C:\Oracle\product\*\dbhome_1"
    "D:\oracle\*"
    "D:\app\*\product\*\dbhomeFree"
) do (
    if exist "%%~D\bin\sqlplus.exe" (
        set "ORACLE_HOME=%%~D"
        goto oracle_found
    )
)

REM Cach 4: Tim sqlplus trong PATH
where sqlplus >nul 2>&1
if %errorLevel% equ 0 (
    for /f "tokens=*" %%F in ('where sqlplus 2^>nul') do (
        set "SQLPLUS_PATH=%%F"
        for %%P in ("!SQLPLUS_PATH!\..\..") do set "ORACLE_HOME=%%~fP"
        goto oracle_found
    )
)

echo.
echo [LOI] Khong tim thay Oracle Database duoc cai tren may nay!
echo.
echo Vui long cai dat Oracle Database 23ai Free tu:
echo   https://www.oracle.com/database/free/
echo.
echo Hoac chi can chay run.bat (se tu dong dung H2 embedded database).
echo.
pause
exit /b 1

:oracle_found
echo [OK] Da tim thay Oracle tai: !ORACLE_HOME!
set "PATH=!ORACLE_HOME!\bin;%PATH%"

:: =========================================================================
:: BUOC 2: Kiem tra Oracle SID (FREE hoac ORCL hoac ten khac)
:: =========================================================================
echo.
echo [2/5] Dang kiem tra Oracle SID...

set "ORACLE_SID=FREE"

REM Kiem tra SERVICE_NAME/SID trong registry
for /f "tokens=2*" %%A in ('reg query "HKLM\SOFTWARE\ORACLE" /v ORACLE_SID 2^>nul') do set "ORACLE_SID=%%B"
if not defined ORACLE_SID set "ORACLE_SID=FREE"

echo [OK] Oracle SID: !ORACLE_SID!

:: =========================================================================
:: BUOC 3: Bat dich vu Oracle
:: =========================================================================
echo.
echo [3/5] Dang bat cac dich vu Oracle...

REM Tim ten service Oracle (OracleServiceFREE, OracleServiceORCL, v.v.)
set "ORACLE_SERVICE_NAME="
for /f "tokens=*" %%S in ('sc query type= all state= all 2^>nul ^| findstr /i "OracleService"') do (
    for /f "tokens=4" %%N in ("%%S") do set "ORACLE_SERVICE_NAME=%%N"
)

if defined ORACLE_SERVICE_NAME (
    echo [INFO] Tim thay Oracle Service: !ORACLE_SERVICE_NAME!
    net start !ORACLE_SERVICE_NAME! >nul 2>&1
    echo [OK] Da bat service: !ORACLE_SERVICE_NAME!
) else (
    echo [CANH BAO] Khong tim thay OracleService. Thu khoi dong bang oradim...
    oradim -STARTUP -SID !ORACLE_SID! -STARTTYPE srvc,inst >nul 2>&1
)

REM Bat Listener
set "LISTENER_SERVICE="
for /f "tokens=*" %%S in ('sc query type= all state= all 2^>nul ^| findstr /i "OracleOra.*Listener\|OracleTNSListener"') do (
    for /f "tokens=4" %%N in ("%%S") do set "LISTENER_SERVICE=%%N"
)

if defined LISTENER_SERVICE (
    net start !LISTENER_SERVICE! >nul 2>&1
    echo [OK] Da bat Listener: !LISTENER_SERVICE!
) else (
    echo [INFO] Dang thu khoi dong listener theo ten mac dinh...
    net start OracleOraDB23Home1TNSListener >nul 2>&1
    net start OracleTNSListener >nul 2>&1
)

echo [INFO] Cho 10 giay de Oracle khoi dong...
timeout /t 10 /nobreak >nul

:: =========================================================================
:: BUOC 4: Chay SQL Script tao user slms_user
:: =========================================================================
echo.
echo [4/5] Dang tao user slms_user va schema SLMS...
echo.

REM Tao file SQL tam thoi de chay (tranh loi duong dan)
set "TEMP_SQL=%TEMP%\slms_setup_temp.sql"
set "SCHEMA_SQL=%~dp0database\schema.sql"

(
echo -- Auto-generated by setup_oracle_other_pc.bat
echo WHENEVER SQLERROR CONTINUE;
echo ALTER SESSION SET "_ORACLE_SCRIPT" = TRUE;
echo.
echo -- Xoa user cu neu ton tai
echo DROP USER slms_user CASCADE;
echo.
echo -- Tao user moi
echo CREATE USER slms_user IDENTIFIED BY slms_password
echo DEFAULT TABLESPACE USERS
echo TEMPORARY TABLESPACE TEMP;
echo.
echo -- Cap quyen
echo GRANT CONNECT, RESOURCE, DBA TO slms_user;
echo GRANT CREATE SESSION TO slms_user;
echo GRANT CREATE TABLE TO slms_user;
echo GRANT CREATE VIEW TO slms_user;
echo GRANT CREATE SEQUENCE TO slms_user;
echo GRANT UNLIMITED TABLESPACE TO slms_user;
echo.
echo ALTER SESSION SET CURRENT_SCHEMA = slms_user;
echo.
echo PROMPT >>> Da tao user slms_user thanh cong!
echo PROMPT >>> Dang tao cac bang...
) > "!TEMP_SQL!"

REM Noi tiep schema.sql vao file tam
type "%SCHEMA_SQL%" >> "!TEMP_SQL!"

echo PROMPT >>> Hoan thanh setup Oracle SLMS! >> "!TEMP_SQL!"
echo EXIT; >> "!TEMP_SQL!"

REM Chay SQL*Plus voi SYSDBA
sqlplus -S / as sysdba @"!TEMP_SQL!"

if %errorLevel% neq 0 (
    echo.
    echo [LOI] Co loi khi chay SQL*Plus!
    echo Kiem tra xem Oracle da duoc bat chua, roi thu lai.
    del "!TEMP_SQL!" >nul 2>&1
    pause
    exit /b 1
)

del "!TEMP_SQL!" >nul 2>&1

:: =========================================================================
:: BUOC 5: Kiem tra ket noi cuoi cung
:: =========================================================================
echo.
echo [5/5] Dang kiem tra ket noi voi slms_user...

echo SELECT 'KET NOI THANH CONG! User: ' ^|^| USER FROM DUAL; > "%TEMP%\slms_test.sql"
echo EXIT; >> "%TEMP%\slms_test.sql"

sqlplus -S slms_user/slms_password@localhost:1521/!ORACLE_SID! @"%TEMP%\slms_test.sql"
set "CONN_RESULT=%errorLevel%"
del "%TEMP%\slms_test.sql" >nul 2>&1

echo.
echo ========================================================================
if %CONN_RESULT% equ 0 (
    echo   [THANH CONG] Oracle SLMS da san sang!
    echo.
    echo   Host     : localhost
    echo   Port     : 1521
    echo   SID      : !ORACLE_SID!
    echo   Username : slms_user
    echo   Password : slms_password
    echo.
    echo   Bay gio hay chay: run.bat
) else (
    echo   [CANH BAO] Tao user xong nhung kiem tra ket noi co loi.
    echo   Thu chay: run.bat - App se tu dong fallback sang H2 neu can.
)
echo ========================================================================
echo.

pause
