@echo off
REM Usage: install-to-tv.bat <TV_IP_ADDRESS> <path\to\app.apk>
REM Example: install-to-tv.bat 192.168.1.50 app\build\outputs\apk\debug\app-debug.apk
REM Requires adb (Android SDK Platform-Tools) on PATH, or edit ADB below.
setlocal
set ADB=adb
if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" set ADB="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if "%~2"=="" (echo Usage: %~nx0 TV_IP APK_PATH & exit /b 1)
%ADB% connect %1:5555
%ADB% -s %1:5555 install -r "%~2"
if errorlevel 1 (echo Install failed. See docs\BUILD_AND_INSTALL.md "Troubleshooting". & exit /b 1)
%ADB% -s %1:5555 shell am start -n com.dramaease.tv/.MainActivity
echo Done. DramaEase should now be open on the TV.
