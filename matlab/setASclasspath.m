%
% setASclasspath.m
% - script to start setup java classpath in matlab for artisynth 
% 
% -- Ian Stavness -- 02/6/2009

%%% add artisynth jars and classes to dynamic java path
if (isempty(getenv('ARTISYNTH_HOME')))
    disp('ARTISYNTH_HOME environment variable not set');
    return
end

libpath = strcat(getenv('ARTISYNTH_HOME'), '/lib/');
jars = dir(strcat(libpath, '*.jar'));
javaclasspath(strcat(repmat({libpath},length(jars),1),{jars.name}'));
javaaddpath(strcat(getenv('ARTISYNTH_HOME'), '/classes'));
