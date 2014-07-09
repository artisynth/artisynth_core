@ECHO OFF

setlocal EnableDelayedExpansion

set FAST=no
set HELP=no
set HOME_ARG=-1
set SILENT=no
set JAVA_OPTS=-Xms200M -Xmx1G -Xmn100M
set BATCHFILE=%~f0

set COUNT=0

for %%G IN (%*) DO (
	set /a COUNT+=1
	if %%G==-fast (
		shift /!COUNT!
		set FAST=yes
		set JAVA_OPTS=%JAVA_OPTS% -server -Dsun.java2d.opengl=true
		set /a COUNT-=1
	) else (
	if %%G==-help (
		shift /!COUNT!
		set HELP=yes
		set /a COUNT-=1
	) else (
	if %%G==-home (
		shift /!COUNT!
		set HOME_ARG=!COUNT!
		set /a COUNT-=1
	) else (
	if %%G==-s (
		shift /!COUNT!
		set SILENT=yes
		set /a COUNT-=1
	) )))
)

set COUNT=0

:MAIN_OPTS_LOOP
set /a COUNT+=1
if [%1]==[] goto SETVARS
if %COUNT%==%HOME_ARG% (
	set ART_HOME=%1
	shift
)
if defined MAIN_OPTS (
	set MAIN_OPTS=%MAIN_OPTS% %1
) else (
	set MAIN_OPTS=%1
)
shift
goto MAIN_OPTS_LOOP

:SETVARS
if defined ART_HOME set ARTISYNTH_HOME=%ART_HOME%
if not defined ARTISYNTH_HOME set ARTISYNTH_HOME=%~dp0..
set ARTISYNTH_PATH=.;%HOMEPATH%;%ARTISYNTH_HOME%
if defined CLASSPATH (
        set CLASSPATH=%ARTISYNTH_HOME%\classes;%ARTISYNTH_HOME%\lib\*;%CLASSPATH%
) else (
        set CLASSPATH=%ARTISYNTH_HOME%\classes;%ARTISYNTH_HOME%\lib\*
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

set LOG=%ARTISYNTH_HOME%\tmp\artisynth.log

if not exist %LOG% echo > %LOG%
if exist %LOG% echo --------Start Artisynth------------------------------ >> %LOG%

date /T >> %LOG%
time /T >> %LOG%
ver     >> %LOG%
echo BATCHFILE: %BATCHFILE% >> %LOG%
echo ARTISYNTH_HOME= %ARTISYNTH_HOME% >> %LOG%
echo PATH= %PATH% >> %LOG%
echo CLASSPATH= %CLASSPATH% >> %LOG%
java -version >> %log% 2>&1

@rem java artisynth.core.util.JVMInfo >> %LOG%

if exist %LOG% echo ------------------------------------------------ >> %LOG%

if %SILENT%==yes ( 
	java %JAVA_OPTS% artisynth.core.driver.Launcher %MAIN_OPTS%  >> %LOG% 2>&1
) else (
	java %JAVA_OPTS% artisynth.core.driver.Launcher %MAIN_OPTS%
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
