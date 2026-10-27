@ECHO OFF

REM Runs a Java class with the environment needed by ArtiSynth applications:
REM
REM   runapp [JAVA_OPTIONS] <className> [ARGS]
REM
REM ARTISYNTH_HOME is set to '..' relative to this file, unless it is already
REM set. The classpath consists of ArtiSynth's classes directory and the jar
REM files listed in lib\LIBRARIES (see bin\makeClasspath.bat), followed by
REM any existing CLASSPATH, minus entries for ArtiSynth's lib folder.

setlocal EnableDelayedExpansion

if not defined ARTISYNTH_HOME set "ARTISYNTH_HOME=%~dp0.."
set "ARTISYNTH_PATH=.;%HOMEPATH%;%ARTISYNTH_HOME%"

REM MumpsJNI links the Intel Fortran runtime, whose Ctrl-C handler would
REM otherwise abort the process instead of letting the JVM shut down.
set FOR_DISABLE_CONSOLE_CTRL_HANDLER=1

REM Classpath consists of the main classes directory and the jar files listed
REM in lib\LIBRARIES, followed by the existing CLASSPATH minus any entries for
REM lib\* or jar files in lib\ (so that older versions of the libraries that
REM may still be in lib\ are excluded). If LIBRARIES can't be read, use lib\*
REM followed by the existing CLASSPATH. 'call' keeps cmd from stripping the
REM quotes around a path containing spaces.
set "ACP="
for /f "usebackq delims=" %%C in (`call "%ARTISYNTH_HOME%\bin\makeClasspath.bat" -addEnv 2^>NUL`) do set "ACP=%%C"
if defined ACP (
        set "CLASSPATH=!ACP!"
) else if defined CLASSPATH (
        set "CLASSPATH=!ARTISYNTH_HOME!\classes;!ARTISYNTH_HOME!\lib\*;!CLASSPATH!"
) else (
        set "CLASSPATH=!ARTISYNTH_HOME!\classes;!ARTISYNTH_HOME!\lib\*"
)

REM native libraries can depend on other libraries in lib\Windows64
set "PATH=%ARTISYNTH_HOME%\lib\Windows64;%PATH%;%ARTISYNTH_HOME%\bin"

REM Enable native access (used by JOGL and ArtiSynth's native libraries), to
REM avoid warnings from Java 24 and later, and blocking by future versions.
REM The option is supported only by Java 17 and later.
set "NATIVE_ACCESS="
set "JVER="
for /f "tokens=3" %%V in ('call java -version 2^>^&1 ^| findstr /i "version"') do if not defined JVER set "JVER=%%~V"
set "JMAJOR="
if defined JVER for /f "delims=.-+" %%M in ("!JVER!") do set "JMAJOR=%%M"
if defined JMAJOR if !JMAJOR! GEQ 17 set "NATIVE_ACCESS=--enable-native-access=ALL-UNNAMED"

java %NATIVE_ACCESS% %*
exit /B %ERRORLEVEL%
