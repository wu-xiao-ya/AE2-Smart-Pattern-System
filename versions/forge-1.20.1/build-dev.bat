@echo off
setlocal

set "JAVA_HOME=C:\Users\PC\AppData\Roaming\.hmcl\java\windows-x86_64\mojang-java-runtime-beta"
set "PATH=%JAVA_HOME%\bin;%PATH%"

call "%~dp0gradlew.bat" %*

endlocal
