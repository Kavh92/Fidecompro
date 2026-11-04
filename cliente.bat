@echo off

cd /d "%~dp0"

if not exist build\classes\fidecompro\cliente\ClienteApp.class (
    call compilar.bat

    if errorlevel 1 exit /b 1
)

if not exist certs\truststore.p12 (
    echo Inicie primero servidor.bat.
    pause
    exit /b 1
)

java -Djavax.net.ssl.trustStore=certs/truststore.p12 -Djavax.net.ssl.trustStorePassword=cambioLocal2026 -cp build/classes fidecompro.cliente.ClienteApp

pause