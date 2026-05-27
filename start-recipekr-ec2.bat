@echo off
setlocal EnableExtensions DisableDelayedExpansion

chcp 65001 >nul

echo ========================================================
echo RecipeKR - Docker server launcher
echo ========================================================
echo.

REM Move to the directory where this batch file exists.
cd /d "%~dp0"

where docker >nul 2>nul
if errorlevel 1 (
    echo.
    echo Docker was not found. Please install Docker Desktop and run this file again.
    echo Download: https://www.docker.com/products/docker-desktop/
    pause
    exit /b 1
)

docker info >nul 2>nul
if errorlevel 1 (
    echo.
    echo Docker is installed, but the Docker engine is not running.
    echo Start Docker Desktop, wait until it is ready, and run this file again.
    pause
    exit /b 1
)

docker compose version >nul 2>nul
if not errorlevel 1 (
    set "COMPOSE=docker compose"
) else (
    where docker-compose >nul 2>nul
    if errorlevel 1 (
        echo.
        echo Docker Compose was not found. Update Docker Desktop and try again.
        pause
        exit /b 1
    )
    set "COMPOSE=docker-compose"
)

echo.
echo Starting RecipeKR in Docker.
echo Java, Python libraries, and Playwright Chromium are installed inside the image.
echo The first Docker build can take several minutes.
echo Open http://localhost:8080 after the server starts.
echo.

%COMPOSE% up --build

echo.
echo Server stopped. If an error occurred, check the messages above.
pause
