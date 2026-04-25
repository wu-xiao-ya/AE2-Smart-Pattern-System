@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "TASK=runClient"
if not "%~1"=="" (
    set "TASK=%~1"
    shift
)
set "EXTRA_ARGS="
:collect_args
if "%~1"=="" goto args_done
set "EXTRA_ARGS=!EXTRA_ARGS! %~1"
shift
goto collect_args
:args_done

set "SCRIPT_DIR=%~dp0"
set "MODS_DIR=%SCRIPT_DIR%run\mods"
set "DISABLED_DIR=%MODS_DIR%\_disabled_by_run_dev_safe"
set "MOVED_ANY=0"
set "SAFE_DISABLE=0"

if /I "%TASK%"=="runClient" set "SAFE_DISABLE=1"
if /I "%TASK%"=="runServer" set "SAFE_DISABLE=1"

if not exist "%MODS_DIR%" (
    echo [run-dev-safe] Mods directory not found: "%MODS_DIR%"
    call "%SCRIPT_DIR%build-dev.bat" %TASK% --console=plain %EXTRA_ARGS%
    exit /b %ERRORLEVEL%
)

if "%SAFE_DISABLE%"=="1" (
    if not exist "%DISABLED_DIR%" mkdir "%DISABLED_DIR%"
    call :move_match "appliedenergistics2-forge-*.jar"
    call :move_match "guideme-*.jar"
    call :move_match "jei-*.jar"
    echo [run-dev-safe] Disabled runtime jars in "%DISABLED_DIR%".
) else (
    echo [run-dev-safe] Task "%TASK%" does not require runtime jar disabling.
)
echo [run-dev-safe] Running task: %TASK% %EXTRA_ARGS%
call "%SCRIPT_DIR%build-dev.bat" %TASK% --console=plain %EXTRA_ARGS%
set "EXIT_CODE=%ERRORLEVEL%"

if "%SAFE_DISABLE%"=="1" (
    call :restore_all
    if "%MOVED_ANY%"=="0" (
        rem If nothing was moved, keep directory tidy.
        rd "%DISABLED_DIR%" >nul 2>nul
    )
)

echo [run-dev-safe] Restore complete. Exit code: %EXIT_CODE%
exit /b %EXIT_CODE%

:move_match
set "PATTERN=%~1"
for %%F in ("%MODS_DIR%\%PATTERN%") do (
    if exist "%%~fF" (
        echo [run-dev-safe] Disable %%~nxF
        move /Y "%%~fF" "%DISABLED_DIR%\" >nul
        set "MOVED_ANY=1"
    )
)
exit /b 0

:restore_all
if not exist "%DISABLED_DIR%" exit /b 0
for %%F in ("%DISABLED_DIR%\*.jar") do (
    if exist "%%~fF" (
        echo [run-dev-safe] Restore %%~nxF
        move /Y "%%~fF" "%MODS_DIR%\" >nul
    )
)
rd "%DISABLED_DIR%" >nul 2>nul
exit /b 0
