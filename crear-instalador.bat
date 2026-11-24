@echo off
REM ============================================================
REM  Xera o instalador de XsTOR para Windows (.exe)
REM  Requisitos: JDK 17+ (con jpackage), Maven e WiX Toolset 3.x no PATH
REM  Executar en Windows (o JAR inclúe as librarías nativas de JavaFX
REM  da plataforma na que se compila).
REM ============================================================
setlocal
cd /d "%~dp0"

set APP_NAME=XsTOR
set APP_VERSION=2.0.0
set MAIN_JAR=xestorBibliotecaFX-%APP_VERSION%.jar
set MAIN_CLASS=com.acarballeira.jdbc_sqlite.xestorBiblioteca.Launcher

echo [1/3] Compilando co Maven...
call mvn clean package -DskipTests || goto :erro

echo [2/3] Preparando entrada de jpackage...
if exist build-jpackage rmdir /s /q build-jpackage
mkdir build-jpackage\input
copy "target\%MAIN_JAR%" build-jpackage\input\ >nul || goto :erro
if exist doc\Manual_de_uso_XsTOR.pdf copy doc\Manual_de_uso_XsTOR.pdf build-jpackage\input\ >nul

echo [3/3] Xerando instalador...
jpackage ^
  --type exe ^
  --name "%APP_NAME%" ^
  --app-version %APP_VERSION% ^
  --vendor "acarballeira" ^
  --description "XsTOR - Xestor de Bibliotecas" ^
  --input build-jpackage\input ^
  --main-jar %MAIN_JAR% ^
  --main-class %MAIN_CLASS% ^
  --icon packaging\xstor.ico ^
  --add-modules java.desktop,java.sql,java.logging,java.xml,java.naming,jdk.unsupported ^
  --win-menu ^
  --win-menu-group "XsTOR" ^
  --win-shortcut ^
  --win-dir-chooser ^
  --win-upgrade-uuid 6d1f3c52-8a4b-4e57-9b0e-2f7a1c9d4e81 ^
  --dest instalador || goto :erro

rmdir /s /q build-jpackage
echo.
echo Listo: instalador\%APP_NAME%-%APP_VERSION%.exe
exit /b 0

:erro
echo.
echo ERRO: revisa a mensaxe anterior.
exit /b 1
