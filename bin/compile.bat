@echo off
rem batch command to compile all .java files in or below the current
rem working directory. The top level directory is the nearest one at or
rem above the current directory that contains a source file root src\;
rem if it is not found the command terminates with an error. Its class
rem file directory classes\ is created if necessary. The compilation
rem classpath is set to include the ArtiSynth class files and libraries
rem plus the CLASSPATH environment variable.

rem make variables local:
setlocal 
rem look for the top level directory. Parentheses are avoided around
rem %%cd%%, which may itself contain parentheses.
set "curdir=%cd%"
:keepsearching
if exist src\ goto foundTop
set "dir=%cd%"
cd ..
if not "%dir%"=="%cd%" goto keepsearching
cd /d "%curdir%"
echo top level folder containing src\ not found
exit /B 1
:foundTop
set "TOP=%cd%"
rem reset to current directory
cd /d "%curdir%"
rem create classes folder if it does not exist (it may not be present in a
rem fresh checkout, and javac 8 does not create the -d folder)
if not exist "%TOP%\classes\" mkdir "%TOP%\classes"
rem Set AH to <ARTISYNTH_HOME>, assumed to be one level above the
rem directory containing this bacth file:
set AH=%~dp0..
set "JAVAC=javac"
rem if JAVA_HOME is set, use this to locate javac. Parentheses are avoided
rem around %%JAVA_HOME%%, which may itself contain parentheses.
if DEFINED JAVA_HOME set "JAVAC=%JAVA_HOME%\bin\javac"
rem set CP to classpath, consisting of AH\classes and the jar files listed
rem in AH\lib\LIBRARIES, followed by CLASSPATH minus any entries for
rem AH\lib\* or jar files in AH\lib (so that older versions of the
rem libraries that may still be in AH\lib are excluded). If LIBRARIES can't
rem be read, use AH\lib\* followed by CLASSPATH. 'call' keeps cmd from
rem stripping the quotes around a path containing spaces. Parentheses are
rem avoided around %%-expanded paths, which may themselves contain
rem parentheses.
set "CP="
for /f "usebackq delims=" %%C in (`call "%AH%\bin\makeClasspath.bat" -addEnv 2^>NUL`) do set "CP=%%C"
if DEFINED CP goto haveCP
set "CP=%AH%\classes;%AH%\lib\*"
if DEFINED CLASSPATH set "CP=%CP%;%CLASSPATH%"
:haveCP
rem find all java files in and under the CWD, and list them in the
rem _sources_.txt. 
set cwd=%cd%
(for /f "delims=" %%f in ('dir /b /a /s *.java') do call :ProcessFileName "%%f") > _sources_.txt
rem -Xlint:-removal suppresses warnings about APIs deprecated for removal;
rem the 'removal' category exists only in Java 9 and later (javac 8 reports
rem version 1.8)
set "LINT_REMOVAL="
set "JCMAJOR="
for /f "tokens=2 delims=. " %%V in ('call "%JAVAC%" -version 2^>^&1 ^| findstr /b /i "javac"') do if not defined JCMAJOR set "JCMAJOR=%%V"
rem default JCMAJOR to 0 if javac couldn't be run, since an empty
rem %%JCMAJOR%% would be a syntax error in the GEQ test
if not defined JCMAJOR set "JCMAJOR=0"
if %JCMAJOR% GEQ 9 set "LINT_REMOVAL=-Xlint:-removal"
rem call javac with ArtiSynth arguments, classpath, and the the java
rem files listed in _sources_.txt
echo on
"%JAVAC%" -J-Xms500m -J-Xmx500m -d "%TOP%\classes" -cp "%CP%" -sourcepath "%TOP%\src" -source 1.8 -target 1.8 -encoding UTF-8 -Xlint:-options %LINT_REMOVAL% @_sources_.txt
@echo off
set JAVACERR=%ERRORLEVEL%
rem remove the sources file
del /f _sources_.txt
exit /B %JAVACERR%

rem DOS-style pseudo subroutine to process a file name by
rem making it shorter (and relative) by removing the leading CWD.
rem To ensure it is readable by javac, we also have to surround
rem it with quotes ("") in case it contains whitespace. Using
rem quotes then also means we need to replace \ with \\
:ProcessFileName
set "file=%~1"
call set "file=%%file:%cwd%\=%%"
set file="%file:\=\\%"
echo %file%
goto :eof
