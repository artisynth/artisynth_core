@ECHO OFF

REM set proper paths and run artisynth-related app

setlocal EnableDelayedExpansion

REM set JAVA_OPTS=-Xms200M -Xmx1G -Xmn100M
set JAVA_OPTS=

if not defined ARTISYNTH_HOME set ARTISYNTH_HOME=%~dp0..
set ARTISYNTH_PATH=.;%HOMEPATH%;%ARTISYNTH_HOME%

if defined CLASSPATH (
        set CLASSPATH=%ARTISYNTH_HOME%\classes;%ARTISYNTH_HOME%\lib\*;%CLASSPATH%
) else (
        set CLASSPATH=%ARTISYNTH_HOME%\classes;%ARTISYNTH_HOME%\lib\*
)

REM need to create TEMP_PATH first because we need to quote it 
if %PROCESSOR_ARCHITECTURE%==AMD64 (
        set TEMP_PATH="%ARTISYNTH_HOME%\lib\Windows64;%PATH%"
) else (
        set TEMP_PATH="%ARTISYNTH_HOME%\lib\Windows;%PATH%"
)
set PATH=%TEMP_PATH:"=%
@ECHO ON

java %JAVA_OPTS% %*

@ECHO OFF
endlocal
