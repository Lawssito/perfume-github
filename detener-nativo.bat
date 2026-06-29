@echo off
title Perfume Platform - Detener Servicios
cd /d "%~dp0"

echo ==========================================
echo   Perfume Platform - Detener Servicios
echo ==========================================
echo.

echo Deteniendo API Gateway...
taskkill /f /fi "WINDOWTITLE eq API-Gateway" >nul 2>&1

echo Deteniendo microservicios de negocio...
taskkill /f /fi "WINDOWTITLE eq ms-pedidos" >nul 2>&1
taskkill /f /fi "WINDOWTITLE eq ms-pagos" >nul 2>&1
taskkill /f /fi "WINDOWTITLE eq ms-notificaciones" >nul 2>&1
taskkill /f /fi "WINDOWTITLE eq ms-envios" >nul 2>&1
taskkill /f /fi "WINDOWTITLE eq ms-carrito" >nul 2>&1
taskkill /f /fi "WINDOWTITLE eq ms-stock" >nul 2>&1
taskkill /f /fi "WINDOWTITLE eq ms-catalogo" >nul 2>&1
taskkill /f /fi "WINDOWTITLE eq user-service" >nul 2>&1
taskkill /f /fi "WINDOWTITLE eq security-service" >nul 2>&1
taskkill /f /fi "WINDOWTITLE eq auth-service" >nul 2>&1

echo Deteniendo Eureka Server...
taskkill /f /fi "WINDOWTITLE eq Eureka-Server" >nul 2>&1

echo.
echo Todos los servicios han sido detenidos.
echo.
pause
