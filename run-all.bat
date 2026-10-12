@echo off
title Territorial Paint Game Launcher
echo ========================================================
echo   Starting Territorial Paint Game - 3 Components
echo ========================================================
echo.

echo [1/3] Starting Spring Boot Backend on port 8080...
start "Backend (Spring Boot - Port 8080)" cmd /k "cd /d "%~dp0" && mvn spring-boot:run -pl game-backend"

echo Waiting 5 seconds for Backend to initialize...
timeout /t 5 /nobreak >nul

echo [2/3] Starting Game Server TCP on port 8888...
start "Game Server (TCP - Port 8888)" cmd /k "cd /d "%~dp0" && mvn exec:java -pl game-server -Dexec.mainClass=com.territorypaint.server.GameServer"

echo Waiting 3 seconds for Game Server to initialize...
timeout /t 3 /nobreak >nul

echo [3/3] Starting Game Client JavaFX UI...
start "Game Client (JavaFX UI)" cmd /k "cd /d "%~dp0" && mvn javafx:run -pl game-client"

echo.
echo ========================================================
echo   All 3 components have been started successfully!
echo   1. Backend (Spring Boot REST API)
echo   2. Game Server (Authoritative TCP Server)
echo   3. Game Client (JavaFX Desktop UI)
echo ========================================================
echo Notice: To open a 2nd Client for 2 players, run:
echo mvn javafx:run -pl game-client
echo.
pause
