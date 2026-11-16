@echo off
chcp 65001 > nul
cd /d "%~dp0"

set "ORACLE_HOME=C:\app\sumaytinh0912350956\product\26ai\dbhomeFree"
set "ORACLE_SID=FREE"
set "PATH=%ORACLE_HOME%\bin;%PATH%"

echo ====================================================================
echo        DANG KHOI PHUC VA MO ORACLE DATABASE 23ai
echo ====================================================================
echo.

sqlplus / as sysdba @fix_database.sql > fix_log.txt 2>&1

type fix_log.txt

echo.
echo ====================================================================
echo Hoan thanh tien trinh sua loi va nap database!
echo ====================================================================
pause
