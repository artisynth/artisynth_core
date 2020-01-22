@set AH=%~dp0..
@set JAVAC=javac
@if DEFINED JAVA_HOME (
	set JAVAC=%JAVA_HOME%\bin\javac
)
@set CLASSPATH=%AH%\classes;%AH%\lib\*
@dir /b /a /s *java > _sources_.txt
%JAVAC% -J-Xms500m -J-Xmx500m -d %AH%\classes -cp %CLASSPATH% -sourcepath %AH%\src -source 1.8 -target 1.8 -encoding UTF-8 @_sources_.txt
@del /f _sources_.txt
