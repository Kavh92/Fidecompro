@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Java\jdk-24"
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "%~dp0"

call compilar.bat

if errorlevel 1 exit /b 1

call preparar_tls.bat

if errorlevel 1 (
    echo No se pudo preparar TLS.
    pause
    exit /b 1
)

java -Djavax.net.ssl.keyStore=certs/servidor.p12 -Djavax.net.ssl.keyStorePassword=cambioLocal2026 -cp build/classes fidecompro.red.Servidor

pause