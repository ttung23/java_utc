@echo off
chcp 65001 > nul
echo ====================================================================
echo   BO SUNG COT PASSWORD_HASH, EMAIL, PHONE VA BANG STAFF_USERS
echo ====================================================================
echo.

cd /d "%~dp0"

REM Thiet lap bien moi truong Oracle 23ai
set "ORACLE_HOME=C:\app\sumaytinh0912350956\product\26ai\dbhomeFree"
set "ORACLE_SID=FREE"
set "PATH=%ORACLE_HOME%\bin;%PATH%"

echo Dang ket noi vao user slms_user tren Oracle 23ai de chay fix...
echo.

sqlplus slms_user/slms_password@localhost:1521/FREE @fix_readers_columns.sql

echo.
echo ====================================================================
echo Hoan tat!
echo ====================================================================

pause

