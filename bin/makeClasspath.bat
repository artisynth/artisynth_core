@ECHO OFF

REM Prints out ArtiSynth's full required classpath consisting of its classes
REM folder plus all the .jar files listed in lib\LIBRARIES. Jar files preceded
REM by a system specifier in LIBRARIES (which contain only native libraries)
REM are omitted. This is the Windows command shell version of makeClasspath.
REM
REM The ArtiSynth home folder is assumed to be '..' relative to this file; the
REM environment variable ARTISYNTH_HOME is not used.
REM
REM With the option -addEnv, the entries of the CLASSPATH environment variable
REM are appended, except for those that refer to ArtiSynth's lib folder (i.e.,
REM lib\* or jar files in lib\), since these may include older versions of
REM the libraries that are not listed in LIBRARIES.

setlocal DisableDelayedExpansion

set "ADD_ENV="
if "%~1"=="" goto argsDone
if not "%~1"=="-addEnv" goto usage
if not "%~2"=="" goto usage
set "ADD_ENV=yes"
:argsDone

for %%I in ("%~dp0..") do set "AH=%%~fI"
if exist "%AH%\lib\LIBRARIES" goto haveLibs
echo makeClasspath: can't read %AH%\lib\LIBRARIES 1>&2
exit /B 1
:haveLibs
set "CP=%AH%\classes"

REM The outer loop strips '#' comments and skips blank and comment lines; the
REM inner loop splits what remains into words. The work is done in :addJar,
REM outside of any parenthesized block, so that paths containing ')' or '!'
REM (such as "C:\Program Files (x86)") are handled correctly.
for /f "usebackq eol=# tokens=1 delims=#" %%L in ("%AH%\lib\LIBRARIES") do (
   for /f "tokens=1,2" %%a in ("%%L") do (
      if "%%b"=="" if /i "%%~xa"==".jar" call :addJar "%%a"
   )
)
if not defined ADD_ENV goto printCP

REM Append the CLASSPATH entries one at a time. eol is set to '|', which
REM can't appear in a path, since the default ';' would cause a CLASSPATH
REM starting with ';' to be skipped entirely.
set "LIBDIR=%AH%\lib"
set "REST=%CLASSPATH%"
:nextEntry
set "E="
set "CUR=%REST%"
set "REST="
if not defined CUR goto printCP
for /f "eol=| tokens=1* delims=;" %%a in ("%CUR%") do (
   set "E=%%a"
   set "REST=%%b"
)
if not defined E goto printCP
call :addEnvEntry
goto nextEntry

:printCP
echo(%CP%
exit /B 0

:usage
echo Usage: makeClasspath [-addEnv] 1>&2
exit /B 1

REM Appends the CLASSPATH entry E to CP, unless it is a wildcard entry for
REM the lib folder, or a jar file in it. Paths are compared after being
REM made absolute with %%~f. A plain 'for' is not used since it would expand
REM wildcards.
:addEnvEntry
set "ED="
if "%E:~-2%"=="\*" set "ED=%E:~0,-2%"
if "%E:~-2%"=="/*" set "ED=%E:~0,-2%"
if defined ED goto checkDir
if /i not "%E:~-4%"==".jar" goto keepEntry
for /f "eol=| delims=" %%F in ("%E%") do set "ED=%%~dpF"
if "%ED:~-1%"=="\" set "ED=%ED:~0,-1%"
:checkDir
if not defined ED goto keepEntry
for /f "eol=| delims=" %%F in ("%ED%") do set "ED=%%~fF"
if /i "%ED%"=="%LIBDIR%" exit /B 0
:keepEntry
set "CP=%CP%;%E%"
exit /B 0

:addJar
set "CP=%CP%;%AH%\lib\%~1"
exit /B 0
