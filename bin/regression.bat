@ECHO OFF

REM A shell script to call the regression tests and run the comparison

setlocal EnableDelayedExpansion

REM set some environment variables
set OMP_NUM_THREADS=1

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

set script=%1
set output=%2
set reference=%3

if [%script%] == [] (
    set script=basicRegressionTest.py
)

FOR %%i IN ("%script%") DO (
set scriptFilename=%%~ni
)

if [%output%] == [] (
    set output=%scriptFileName%.out
)

if [%reference%] == [] (
    set reference=%scriptFileName%.ref
)

REM run artisynth

java artisynth.core.driver.Launcher -posCorrection GlobalMass -disableHybridSolves -script %script%

REM If reference file does not exist, prompt to create it
if not exist %reference% (
    set /p reply="Reference file '%reference%' does not exist.  Do you wish to set it?  [Y/n] "
	echo !reply!

    if [!reply!] == [] (
		set reply=Y
	) else (
		set reply=!reply:~0,1!
	)

    if [!reply!] == [Y] (
		echo Found reply: !reply!
        copy %output% %reference%
    )
    if [!reply!] == [y] (
		echo Found reply: !reply!
        copy %output% %reference%
    )

	exit /b 0
    
)

echo.
echo.
echo Running Comparisons
echo.
java artisynth.core.util.CompareStateFiles -a %output% %reference%

endlocal

exit /b 0
