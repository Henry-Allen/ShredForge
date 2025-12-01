@echo off
REM Run ShredForge with JCEF on Windows
cd /d "%~dp0"

REM Build if needed
call mvn -q -DskipTests compile

REM Get classpath from Maven
for /f "delims=" %%i in ('mvn -q dependency:build-classpath -Dmdep.outputFile=CON') do set CLASSPATH=%%i

REM Run with required JVM args for JCEF
java ^
  --add-opens java.desktop/sun.awt=ALL-UNNAMED ^
  --add-opens java.desktop/java.awt=ALL-UNNAMED ^
  -cp "target\classes;%CLASSPATH%" ^
  com.shredforge.SwingApp

pause
