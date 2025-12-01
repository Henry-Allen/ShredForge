@echo off
setlocal EnableDelayedExpansion
REM Run ShredForge with JCEF on Windows
cd /d "%~dp0"

REM Build if needed
call mvn -q -DskipTests compile

REM Get classpath from Maven into a temp file
call mvn -q dependency:build-classpath -Dmdep.outputFile=target\classpath.txt

REM Read classpath from file (using for loop to handle long lines)
set "DEPCP="
for /f "usebackq delims=" %%a in ("target\classpath.txt") do set "DEPCP=%%a"

REM Run with required JVM args for JCEF
java ^
  --add-opens java.desktop/sun.awt=ALL-UNNAMED ^
  --add-opens java.desktop/java.awt=ALL-UNNAMED ^
  --enable-native-access=ALL-UNNAMED ^
  -cp "target\classes;!DEPCP!" ^
  com.shredforge.SwingApp

endlocal
pause
