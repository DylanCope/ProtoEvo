@echo off
setlocal
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"
set "PATH=%JAVA_HOME%\bin;C:\Users\keera\tools\gradle-8.10.2\bin;%PATH%"
cd /d "%~dp0"
rem Stream output to the console AND tee it to run.log so the trace
rem survives a crash that auto-closes the window. Tee-Object is
rem PowerShell's equivalent of unix `tee`.
powershell -NoProfile -Command "& gradle desktop:runGame 2>&1 | Tee-Object -FilePath run.log"
echo.
echo === run.log saved next to run.bat ===
endlocal
pause
