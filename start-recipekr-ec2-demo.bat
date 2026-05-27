@echo off
setlocal EnableExtensions DisableDelayedExpansion

chcp 65001 >nul

echo ========================================================
echo RecipeKR - local demo launcher
echo ========================================================
echo.

REM Move to the directory where this batch file exists.
cd /d "%~dp0"

if not exist "gradlew.bat" (
    echo.
    echo gradlew.bat was not found. Run this file from the project root.
    pause
    exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
    echo.
    echo Java was not found.
    echo This demo launcher does not require Docker, .env, TiDB, Gemini, or Python.
    echo It does require Java 21 or a packaged Java runtime.
    echo.
    echo For a no-install demo PC, build a portable package on a developer PC
    echo and include a bundled runtime with this project.
    pause
    exit /b 1
)

echo.
echo Starting RecipeKR demo mode.
echo Demo mode uses temporary H2 data and does not call TiDB, Gemini, or RPA.
echo Open http://localhost:8080 after the server starts.
echo Demo admin login: test / 1234
echo Admin login: admin / Admin1234!
echo.

call ".\gradlew.bat" bootRun --console=plain --args="--spring.profiles.active=demo"

echo.
echo Demo server stopped. If an error occurred, check the messages above.
pause
