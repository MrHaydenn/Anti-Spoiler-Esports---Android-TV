@echo off
setlocal
pushd "%~dp0"
if not defined JAVA_HOME if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
if not defined ANDROID_HOME if exist "%LOCALAPPDATA%\Android\Sdk" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
where java >nul 2>nul
if errorlevel 1 (
  echo Install Android Studio first, or set JAVA_HOME to a JDK 17 or newer.
  pause
  exit /b 1
)
if not defined ANDROID_HOME if not exist local.properties (
  echo Open this folder in Android Studio to install Android SDK 36 and accept its license.
  pause
  exit /b 1
)
call gradlew.bat --no-daemon assembleDebug
set "buildExit=%ERRORLEVEL%"
if "%buildExit%"=="0" echo APK ready: %CD%\app\build\outputs\apk\debug\app-debug.apk
popd
pause
exit /b %buildExit%
