@echo off
echo Opening firewall ports for TerritoryPaint Game...
echo Requires Administrator privileges.

netsh advfirewall firewall add rule name="TerritoryPaint Backend (8080)" dir=in action=allow protocol=TCP localport=8080 profile=any
netsh advfirewall firewall add rule name="TerritoryPaint GameServer (8888)" dir=in action=allow protocol=TCP localport=8888 profile=any

echo.
echo Done! Ports 8080 and 8888 are now open for inbound connections.
pause
