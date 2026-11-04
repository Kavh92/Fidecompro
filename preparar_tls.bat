@echo off
setlocal

cd /d "%~dp0"

if exist certs\truststore.p12 if exist certs\servidor.p12 exit /b 0

if not exist certs mkdir certs

if not exist certs\servidor.p12 (
    keytool -genkeypair -alias fidecompro -keyalg RSA -keysize 2048 -validity 365 -dname "CN=localhost,OU=Academico,O=Fidecompro,C=CR" -ext "SAN=dns:localhost,ip:127.0.0.1" -keystore certs\servidor.p12 -storetype PKCS12 -storepass cambioLocal2026 -keypass cambioLocal2026 -noprompt

    if errorlevel 1 exit /b 1
)

keytool -exportcert -alias fidecompro -keystore certs\servidor.p12 -storepass cambioLocal2026 -file certs\localhost.cer

if errorlevel 1 exit /b 1

keytool -importcert -alias fidecompro -file certs\localhost.cer -keystore certs\truststore.p12 -storetype PKCS12 -storepass cambioLocal2026 -noprompt

if errorlevel 1 exit /b 1