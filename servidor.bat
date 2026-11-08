@echo off
setlocal
cd /d "%~dp0" || exit /b 1

call compilar.bat
if errorlevel 1 exit /b 1

call preparar_tls.bat
if errorlevel 1 (
    echo No se pudo preparar TLS. Revise la salida de keytool.
    pause
    exit /b 1
)

java "-Djavax.net.ssl.keyStore=certs/servidor.p12" "-Djavax.net.ssl.keyStoreType=PKCS12" "-Djavax.net.ssl.keyStorePassword=cambioLocal2026" -cp build/classes fidecompro.red.Servidor
set "FIDE_RESULTADO=%errorlevel%"
if not "%FIDE_RESULTADO%"=="0" pause
exit /b %FIDE_RESULTADO%