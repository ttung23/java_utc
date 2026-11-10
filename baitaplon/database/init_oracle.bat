@echo off
chcp 65001 > nul
echo ====================================================================
echo        KHOI TAO CSDL CHO ORACLE 23ai DATABASE (SLMS SYSTEM)
echo ====================================================================
echo.

cd /d "%~dp0"
cd ..

echo [1/2] Dang kiem tra ket noi Oracle qua sqlplus...
echo.

sqlplus / as sysdba @database/setup_oracle_23ai.sql

if %errorlevel% neq 0 (
    echo.
    echo [CANH BAO] Neu gap loi ORA-12241 (Service not running):
    echo Hay mo CMD duoi quyen Administrator va chay 2 lenh sau de bat Oracle:
    echo   net start OracleServiceFREE
    echo   net start OracleOraDB26Home1TNSListener
    echo Sau do chay lai file nay!
    echo.
) else (
    echo.
    echo [THANH CONG] Da nap toan bo database vao Oracle 23ai that!
)

pause

