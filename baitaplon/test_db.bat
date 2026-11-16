@echo off
chcp 65001 > nul
cd /d "%~dp0"
if exist "C:\Program Files\Java\jdk-22\bin" (
    set "PATH=C:\Program Files\Java\jdk-22\bin;%PATH%"
)
java -cp "bin;lib/ojdbc11.jar" com.slms.TestConnection
pause

