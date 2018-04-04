@ECHO OFF

setlocal EnableDelayedExpansion

set ARGS=-updateLibs -remoteSource http://www.artisynth.org/files/lib
set BATCHFILE=%~f0

if not defined ARTISYNTH_HOME set ARTISYNTH_HOME=%~dp0..
set CLASSPATH=%ARTISYNTH_HOME%\lib\*;%ARTISYNTH_HOME%\bin\libraryInstaller.jar

java artisynth.core.driver.LibraryInstaller %ARGS%

set JAVAERROR=%ERRORLEVEL%
if %JAVAERROR% EQU 9009 (
   echo Error: Java executable not found. Please ensure that Java is in your path
   pause
   exit /B 1
)
if %JAVAERROR% EQU 1 (
   echo Error: couldn't update all libraries
   pause
   exit /B 1
)
