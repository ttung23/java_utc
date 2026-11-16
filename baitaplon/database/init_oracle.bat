@echo off
chcp 65001 > nul
echo ====================================================================
echo        KHOI TAO CSDL CHO ORACLE 23ai DATABASE (SLMS SYSTEM)
echo ====================================================================
echo.

cd /d "%~dp0"

REM Thiet lap bien moi truong Oracle 23ai
set "ORACLE_HOME=C:\app\sumaytinh0912350956\product\26ai\dbhomeFree"
set "ORACLE_SID=FREE"
set "PATH=%ORACLE_HOME%\bin;%PATH%"

echo Dang ket noi vao SQL*Plus va chay script setup_oracle_23ai.sql...
echo.

sqlplus / as sysdba @setup_oracle_23ai.sql

echo.
echo ====================================================================
echo Hoan thanh tien trinh khoi tao!
echo ====================================================================

pause
