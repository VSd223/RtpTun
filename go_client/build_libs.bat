@echo off
chcp 65001 >nul
setlocal

echo ==================================================
echo   Сборка ядра libclient.so для всех платформ
echo ==================================================

set "OUT_DIR=jniLibs"
set "LDFLAGS=-checklinkname=0 -s -w"
set "NDK_HINT="
set "NDK_PATH="

if defined ANDROID_NDK_HOME set "NDK_HINT=%ANDROID_NDK_HOME%"
if not defined NDK_HINT if defined ANDROID_NDK_ROOT set "NDK_HINT=%ANDROID_NDK_ROOT%"
if not defined NDK_HINT if defined ANDROID_SDK_ROOT set "NDK_HINT=%ANDROID_SDK_ROOT%\ndk"
if not defined NDK_HINT if defined LOCALAPPDATA set "NDK_HINT=%LOCALAPPDATA%\Android\Sdk\ndk"

if exist "%NDK_HINT%\toolchains\llvm\prebuilt\windows-x86_64\bin\clang.exe" set "NDK_PATH=%NDK_HINT%"
if not defined NDK_PATH (
    for /f "delims=" %%D in ('dir /b /ad "%NDK_HINT%" 2^>nul ^| sort /r') do (
        if not defined NDK_PATH if exist "%NDK_HINT%\%%D\toolchains\llvm\prebuilt\windows-x86_64\bin\clang.exe" set "NDK_PATH=%NDK_HINT%\%%D"
    )
)
if not defined NDK_PATH (
    echo [ОШИБКА] Android NDK не найден. Установите NDK или задайте ANDROID_NDK_HOME.
    goto error
)
set "NDK_BIN=%NDK_PATH%\toolchains\llvm\prebuilt\windows-x86_64\bin"

if not exist "%OUT_DIR%\arm64-v8a" mkdir "%OUT_DIR%\arm64-v8a"
if not exist "%OUT_DIR%\armeabi-v7a" mkdir "%OUT_DIR%\armeabi-v7a"
if not exist "%OUT_DIR%\x86_64" mkdir "%OUT_DIR%\x86_64"

echo.
echo [1/3] Сборка arm64-v8a (Android 64-bit)...
set GOOS=android
set GOARCH=arm64
set CGO_ENABLED=0
go build -trimpath -ldflags="%LDFLAGS%" -buildmode=pie -o "%OUT_DIR%\arm64-v8a\libclient.so" .
if errorlevel 1 goto error

echo [2/3] Сборка armeabi-v7a (ARM 32-bit)...
set GOOS=android
set GOARCH=arm
set GOARM=7
set CGO_ENABLED=1
set "CC=%NDK_BIN%\clang.exe --target=armv7a-linux-androideabi21"
go build -trimpath -ldflags="%LDFLAGS%" -buildmode=pie -o "%OUT_DIR%\armeabi-v7a\libclient.so" .
if errorlevel 1 goto error
set GOARM=

echo [3/3] Сборка x86_64 (Эмуляторы 64-bit)...
set GOOS=android
set GOARCH=amd64
set CGO_ENABLED=1
set "CC=%NDK_BIN%\clang.exe --target=x86_64-linux-android21"
go build -trimpath -ldflags="%LDFLAGS%" -buildmode=pie -o "%OUT_DIR%\x86_64\libclient.so" .
if errorlevel 1 goto error

echo.
echo ==================================================
echo [УСПЕХ] Все 3 архитектуры успешно собраны в %OUT_DIR%!
echo ==================================================
pause
exit /b 0

:error
echo.
echo ==================================================
echo [ОШИБКА] Сборка прервана с ошибкой!
echo ==================================================
pause
exit /b 1