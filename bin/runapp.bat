@ECHO OFF

REM Runs a Java class with the environment needed by ArtiSynth applications:
REM
REM   runapp [JAVA_OPTIONS] <className> [ARGS]
REM
REM ARTISYNTH_HOME is set to '..' relative to this file, unless it is already
REM set. The classpath consists of ArtiSynth's classes directory and the jar
REM files listed in lib\LIBRARIES (see bin\makeClasspath.bat), followed by
REM any existing CLASSPATH.

setlocal EnableDelayedExpansion

if not defined ARTISYNTH_HOME set "ARTISYNTH_HOME=%~dp0.."
set "ARTISYNTH_PATH=.;%HOMEPATH%;%ARTISYNTH_HOME%"

REM MumpsJNI links the Intel Fortran runtime, whose Ctrl-C handler would
REM otherwise abort the process instead of letting the JVM shut down.
set FOR_DISABLE_CONSOLE_CTRL_HANDLER=1

REM Classpath consists of the main classes directory and the jar files listed
REM in lib\LIBRARIES (so that older versions of these that may still be in
REM lib\ are excluded). If LIBRARIES can't be read, use lib\*. 'call' keeps
REM cmd from stripping the quotes around a path containing spaces.
set "ACP="
for /f "usebackq delims=" %%C in (`call "%ARTISYNTH_HOME%\bin\makeClasspath.bat" 2^>NUL`) do set "ACP=%%C"
if not defined ACP set "ACP=%ARTISYNTH_HOME%\classes;%ARTISYNTH_HOME%\lib\*"
if defined CLASSPATH (
        set "CLASSPATH=!ACP!;!CLASSPATH!"
) else (
        set "CLASSPATH=!ACP!"
)

REM native libraries can depend on other libraries in lib\Windows64
set "PATH=%ARTISYNTH_HOME%\lib\Windows64;%PATH%;%ARTISYNTH_HOME%\bin"

java %*
exit /B %ERRORLEVEL%
