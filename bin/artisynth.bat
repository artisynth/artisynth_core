@ECHO OFF

setlocal EnableDelayedExpansion

set FAST=no
set HELP=no
set SILENT=no
set JAVA_OPTS=-Xms200M -Xmx6G -Xmn100M
set BATCHFILE=%~f0

set COUNT=0
set modelArgsFound=false

set JAVA=java
if DEFINED JAVA_HOME (
	set JAVA=%JAVA_HOME%\bin\java
)

for %%G IN (%*) DO (
        if !modelArgsFound!==true (
                if defined MAIN_OPTS (
                        set MAIN_OPTS=!MAIN_OPTS! %%G
                ) else (
                        set MAIN_OPTS=%%G
                )
        ) else (
        if defined ART_HOME_PENDING (
                set ART_HOME=%%G
                set ART_HOME_PENDING=
        ) else (
                if %%G==-fast (
                        set FAST=yes
                        set JAVA_OPTS=%JAVA_OPTS% -server -Dsun.java2d.opengl=true
                ) else (
                if %%G==-help (
                        set HELP=yes
                ) else (
                if %%G==-home (
                        set ART_HOME_PENDING=true
                ) else (
                if %%G==-s (
                        set SILENT=yes
                ) else (
                        if %%G==-M (
                                set modelArgsFound=true
                        )
                        if defined MAIN_OPTS (
                                set MAIN_OPTS=!MAIN_OPTS! %%G
                        ) else (
                                set MAIN_OPTS=%%G
                        )
                ))))
        ))
)
if defined ART_HOME_PENDING (
        echo Error: option -home need to be followed by a path name
        exit /B 1
)
if defined ART_HOME set ARTISYNTH_HOME=%ART_HOME%
if not defined ARTISYNTH_HOME set ARTISYNTH_HOME=%~dp0..
set ARTISYNTH_PATH=.;%HOMEPATH%;%ARTISYNTH_HOME%
if defined CLASSPATH (
        set "CLASSPATH=%ARTISYNTH_HOME%\classes;%ARTISYNTH_HOME%\lib\*;%CLASSPATH%"
) else (
        set "CLASSPATH=%ARTISYNTH_HOME%\classes;%ARTISYNTH_HOME%\lib\*"
)
if %PROCESSOR_ARCHITECTURE%==AMD64 (
        set TEMP_PATH="%ARTISYNTH_HOME%\lib\Windows64;%PATH%"
) else (
        set TEMP_PATH="%ARTISYNTH_HOME%\lib\Windows;%PATH%"
)
@rem need to create TEMP_PATH first because we need to quote it 
set PATH=%TEMP_PATH:"=%
if %HELP%==yes (
        echo synopsis:
        echo %  artisynth [PROGRAM_OPTIONS]
        echo options:
        echo %  -fast                 run using -server option
        echo %  -s                    suppress all console output
        echo %  -home ^<AHOME^>         ^set ARTISYNTH_HOME to AHOME
    java artisynth.core.driver.Launcher -options
    exit /B 0
 )

rem John Lloyd, June 2021: Removed logging. The log file file did not
rem record much that was useful, and was anyway inapplicable when
rem running from an IDE

if %SILENT%==yes ( 
        %JAVA% %JAVA_OPTS% artisynth.core.driver.Launcher %MAIN_OPTS% > NUL 2>&1
) else (
        %JAVA% %JAVA_OPTS% artisynth.core.driver.Launcher %MAIN_OPTS%
)

set JAVAERROR=%ERRORLEVEL%
if %JAVAERROR% EQU 0 exit /B 0

REM Error Handling

if %JAVAERROR% EQU 9009 (
  echo Error: Java executable not found. Please ensure that Java is in your path
  pause
  exit /B 1
)
else (
  echo Error: ArtiSynth encountered a problem. Please check the stack trace for information.
  pause
  exit /B 2
)
