@echo off
title Perfume Platform - Arranque Nativo

echo =====================================================================
echo INICIANDO SISTEMA DE MICROSERVICIOS (MODO NATIVO)
echo =====================================================================
echo.
echo NOTA: Requiere Java 21+ configurado en PATH y MySQL en localhost:3306
echo.

set "ROOT=%~dp0"

REM ==========================================
REM  1. Eureka Server (puerto 8761)
REM ==========================================
echo [1/3] Lanzando Eureka Server...
start "Eureka-Server" /b java -jar "%ROOT%eureka-server\target\eureka-server-0.0.1-SNAPSHOT.jar"
echo       Esperando 20 segundos para que Eureka se active...
timeout /t 20 /nobreak >nul
echo       [OK] Eureka Server activo.
echo.

REM ==========================================
REM  2. Microservicios de Negocio
REM ==========================================
echo [2/3] Lanzando Microservicios de Negocio...

start "ms-catalogo" /b java -jar "%ROOT%ms-catalogo\target\ms-catalogo-0.0.1-SNAPSHOT.jar"
timeout /t 3 /nobreak >nul

start "ms-stock" /b java -jar "%ROOT%ms-stock\target\ms-stock-0.0.1-SNAPSHOT.jar"
timeout /t 3 /nobreak >nul

start "ms-carrito" /b java -jar "%ROOT%ms-carrito\target\ms-carrito-0.0.1-SNAPSHOT.jar"
timeout /t 3 /nobreak >nul

start "ms-pagos" /b java -jar "%ROOT%ms-pagos\target\ms-pagos-0.0.1-SNAPSHOT.jar"
timeout /t 3 /nobreak >nul

start "ms-pedidos" /b java -jar "%ROOT%ms-pedidos\target\ms-pedidos-0.0.1-SNAPSHOT.jar"
timeout /t 3 /nobreak >nul

start "ms-envios" /b java -jar "%ROOT%ms-envios\target\ms-envios-0.0.1-SNAPSHOT.jar"
timeout /t 3 /nobreak >nul

start "ms-notificaciones" /b java -jar "%ROOT%ms-notificaciones\target\ms-notificaciones-0.0.1-SNAPSHOT.jar"
timeout /t 3 /nobreak >nul

start "auth-service" /b java -jar "%ROOT%auth-service\target\auth-service-0.0.1-SNAPSHOT.jar"
timeout /t 3 /nobreak >nul

start "security-service" /b java -jar "%ROOT%security-service\target\security-service-0.0.1-SNAPSHOT.jar"
timeout /t 3 /nobreak >nul

start "user-service" /b java -jar "%ROOT%user-service\target\user-service-0.0.1-SNAPSHOT.jar"
timeout /t 3 /nobreak >nul

echo       [OK] Microservicios iniciados.
echo.

REM ==========================================
REM  3. API Gateway (puerto 8080)
REM ==========================================
echo [3/3] Lanzando API Gateway...
start "API-Gateway" /b java -jar "%ROOT%api-gateway\target\api-gateway-0.0.1-SNAPSHOT.jar"
echo       [OK] API Gateway iniciado.
echo.

echo =====================================================================
echo COMPONENTES LANZADOS
echo =====================================================================
echo  Gateway: http://localhost:8080
echo  Eureka:  http://localhost:8761
echo.
echo Para DETENER: cierra las ventanas o ejecuta: taskkill /F /FI "WINDOWTITLE eq Perfume*"
echo =====================================================================
pause
