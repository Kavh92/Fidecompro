@echo off
setlocal EnableDelayedExpansion

cd /d "%~dp0"

if not exist build mkdir build
if not exist build\classes mkdir build\classes

(for /r src\main\java %%f in (*.java) do (
    set "ruta=%%f"
    echo "!ruta:\=/!"
)) > build\sources.txt

javac -encoding UTF-8 --release 17 -d build\classes @build\sources.txt

if errorlevel 1 (
    echo Error de compilacion. Revise las clases y el JDK.
    pause
    exit /b 1
)

echo Compilacion completada.