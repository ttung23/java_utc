@echo off
chcp 65001 > nul
cd /d "%~dp0"

set "ORACLE_HOME=C:\app\sumaytinh0912350956\product\26ai\dbhomeFree"
set "ORACLE_SID=FREE"
set "PATH=%ORACLE_HOME%\bin;%PATH%"

echo ====================================================================
echo        KIEM TRA VA GHI LOG TRANG THAI ORACLE DATABASE
echo ====================================================================
echo.

(
    echo CONNECT / as sysdba
    echo SELECT instance_name, status, database_status FROM v$instance;
    echo SELECT name, open_mode FROM v$database;
    echo SELECT name, open_mode FROM v$pdbs;
    echo ALTER DATABASE OPEN;
    echo ALTER PLUGGABLE DATABASE ALL OPEN;
    echo ALTER SESSION SET "_ORACLE_SCRIPT"=true;
    echo SELECT username FROM all_users WHERE username = 'SLMS_USER';
    echo EXIT;
) > temp_check.sql

sqlplus -S / as sysdba @temp_check.sql > debug_log.txt 2>&1
del temp_check.sql > nul 2>&1

type debug_log.txt

echo.
echo ====================================================================
echo Ket qua da duoc ghi vao file debug_log.txt
echo ====================================================================
pause

