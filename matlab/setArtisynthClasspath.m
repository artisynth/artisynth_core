function [status] = setArtisynthClasspath (homedir) 
% procedure setArtisynthClasspath (homedir) 
%
%    Sets the java classpath to enable running ArtiSynth. 'homedir' should be
%    a string giving the path to the ArtiSynth home directory
%
C = javaclasspath('-all');
%
% add ARTISYNTH_HOME/classes to the java class path
%
newentries = [];
psep = char(java.lang.System.getProperty ('path.separator'));
fsep = char(java.lang.System.getProperty ('file.separator'));
classespath = strcat(homedir, fsep, 'classes');
if isempty(find(strcmp(C,classespath))) == 1 
   javaaddpath (classespath);
   newentries = [ psep, classespath ];
end
%
% add jar files in ARTISYNTH_HOME/lib to the java class path
%
libpath = strcat(homedir, fsep, 'lib', fsep );
jars = dir(strcat(libpath, '*.jar'));
P = strcat(repmat({libpath},length(jars),1),{jars.name}');
for i = 1:length(P) 
   if isempty(find(strcmp(C,P{i}))) == 1 
      javaaddpath (P{i});
      newentries = [ newentries, psep, P{i} ];
   end
end
%
% verify the needed libraries and download if necessary
%
artisynth.core.driver.Launcher.verifyLibraries(0);
%
% add any newly downloaded jar files to the java class path
%
jars = dir(strcat(libpath, '*.jar'));
P = strcat(repmat({libpath},length(jars),1),{jars.name}');
for i = 1:length(P) 
   if isempty(find(strcmp(C,P{i}))) == 1 
      javaaddpath (P{i});
      newentries = [ newentries, psep, P{i} ];
   end
end
% 
% add externals as specified in the EXTCLASSPATH file
%
exts = cell(artisynth.core.driver.Launcher.getExtFilePathNames(homedir));
for i = 1:length(exts)
   if isempty(find(strcmp(C,exts{i}))) == 1 
      javaaddpath (exts{i});
      newentries = [ newentries, psep, exts{i} ];
   end
end
%
% Add new classes and jars to the 'artisynth.class.path' property.
% This is needed when starting a Jython console from matlab, in order
% to enable wildcard imports of the form "from package.xxx import *".
% That's because this importing is controlled by Jython's
% SysPackageManager, which by default uses a search path consisting of
% java.class.path and sun.boot.class.path. But classeses added using
% javaaddpath() don't appear in java.class.path, so we put them in our
% own property 'artisynth.class.path' and then reset the search path
% (which is itself defined by the properties listed in the property
% 'python.packages.paths') to include 'artisynth.class.path'.
%
pypaths='java.class.path,sun.boot.class.path,artisynth.class.path';
java.lang.System.setProperty ('python.packages.paths', pypaths);
if isempty(newentries) == 0 
   cp = char(java.lang.System.getProperty ('artisynth.class.path'));
   java.lang.System.setProperty ('artisynth.class.path', [cp, newentries]);
end
status = 1;
% Done

