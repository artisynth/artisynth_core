@echo off
rem batch command to compile all .java files in or below the current
rem working directoty. It is assumed that there exists a top
rem level directory containing a source file root src\ and
rem class file directory \classes. If this top directory is
rem not found the command terminates with an error. The
rem compilation classpsth is set to include the ArtiSynth
rem class files plus the CLASSPATH environment variable.

rem make variables local:
setlocal 
rem look for the top level directory
set curdir=%cd%
set dir="%cd% starting"
:keepsearching
if exist classes\ ( 
  if exist src\ (
    set TOP=%cd%
  )
)
if not DEFINED TOP (
  if not %dir%==%cd% (
    set dir=%cd%
    cd ..
    goto:keepsearching
  )
)
rem reset to current directory
cd %curdir%
rem exiy if top level not found
if not DEFINED TOP (
  echo "top level folder containing classes\ and src\ not found"
  exit /B
)
rem Set AH to <ARTISYNTH_HOME>, assumed to be one level above the
rem directory containing this bacth file:
set AH=%~dp0..
set JAVAC=javac
rem if JAVA_HOME is set, use this to locate javac
if DEFINED JAVA_HOME (
  set JAVAC=%JAVA_HOME%\bin\javac
)
rem set CP to classpath, including AH\classes and jar files in AH\lib
if DEFINED CLASSPATH (
  set CP=%AH%\classes;%AH%\lib\*;%CLASSPATH%
) else (
  set CP=%AH%\classes;%AH%\lib\*
)
rem create classes folder if it does not exist
if not exist "%AH%\classes\" mkdir "%AH%\classes"
rem find all java files in and under the CWD, and list them in the
rem _sources_.txt. 
set cwd=%cd%
(for /f "delims=" %%f in ('dir /b /a /s *.java') do call :ProcessFileName %%f) > _sources_.txt
rem call javac with ArtiSynth arguments, classpath, and the the java
rem files listed in _sources_.txt
echo on
%JAVAC% -J-Xms500m -J-Xmx500m -d %TOP%\classes -cp %CP% -sourcepath %TOP%\src -source 1.8 -target 1.8 -encoding UTF-8 @_sources_.txt
@echo off
rem remove the sources file
del /f _sources_.txt
goto :End

rem DOS-style pseudo subroutine to process a file name by
rem making it shorter (and relative) by removing the leading CWD.
rem To ensure it is readable by javac, we also have to surround
rem it with quotes ("") in case it contains whitespace. Using
rem quotes then also means we need to replace \ with \\
:ProcessFileName
set file=%1
call set file=%%file:%cwd%\=%%
set file="%file:\=\\%"
echo %file%
goto :eof

:End
