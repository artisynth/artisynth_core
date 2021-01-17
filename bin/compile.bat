@echo off
rem batch command to compile all ArtiSynth .java files in or below
rem the current working directory (CWD). It is assumed that the CWD
rem is located somewhere under <ARTISYNTH_HOME>/src.

rem make variables local:
setlocal 
rem Set AH to <ARTISYNTH_HOME>, assumed to be one level above the
rem directory containing this bacth file:
set AH=%~dp0..
set JAVAC=javac
rem if JAVA_HOME is set, use this to locate javac
if DEFINED JAVA_HOME (
	set JAVAC=%JAVA_HOME%\bin\javac
)
rem set CP to classpath, including AH\classes and jar files in AH\lib
set CP=%AH%\classes;%AH%\lib\*
rem find all java files in and under the CWD, and list them in the
rem _sources_.txt. 
set cwd=%cd%
(for /f "delims=" %%f in ('dir /b /a /s *.java') do call :ProcessFileName %%f) > _sources_.txt
rem call javac with ArtiSynth arguments, classpath, and the the java
rem files listed in _sources_.txt
echo on
%JAVAC% -J-Xms500m -J-Xmx500m -d %AH%\classes -cp %CP% -sourcepath %AH%\src -source 1.8 -target 1.8 -encoding UTF-8 @_sources_.txt
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
