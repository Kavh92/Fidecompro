@echo off
setlocal
cd /d "%~dp0" || exit /b 1

if not exist certs\truststore.p12 (
    echo Inicie primero servidor.bat.
    pause
    exit /b 1
)

call compilar.bat
if errorlevel 1 exit /b 1

java "-Djavax.net.ssl.trustStore=certs/truststore.p12" "-Djavax.net.ssl.trustStoreType=PKCS12" "-Djavax.net.ssl.trustStorePassword=cambioLocal2026" -cp build/classes fidecompro.cliente.ClienteApp
set "FIDE_RESULTADO=%errorlevel%"
if not "%FIDE_RESULTADO%"=="0" pause
exit /b %FIDE_RESULTADO%