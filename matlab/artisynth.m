function [handle] = artisynth (varargin)
% check to make sure that ARTISYNTH_HOME is set
if (isempty(getenv('ARTISYNTH_HOME')))
    disp('ARTISYNTH_HOME environment variable not set');
    status = 0;
    return
end
AH = getenv('ARTISYNTH_HOME');
AP = getenv('ARTISYNTH_PATH');
if (isempty(AP))
    % set up an appropriate default ARTISYNTH_PATH
    psep = char(java.lang.System.getProperty ('path.separator'));
    AP = [ '.', psep ];
    if (~isempty(getenv('HOME')))
       AP = [ AP , getenv('HOME'), psep ];
    end
    AP = [ AP, AH ];
    setenv ('ARTISYNTH_PATH', AP);    
end
args = [];
if (isempty(varargin) == 0)
   nargs = length(varargin);
   args = javaArray('java.lang.String', nargs);
   for i = 1:nargs
      args(i) = java.lang.String(varargin{i});
   end
end
artisynth.core.driver.Main.setRunningUnderMatlab (1);
% quit any existing ArtiSynth that is running
main = artisynth.core.driver.Main.getMain();
if (isempty (main) == 0) 
   main.quit()
end
artisynth.core.driver.Main.setMain([]);
artisynth.core.driver.Main.main (args);
main = artisynth.core.driver.Main.getMain();
if (isempty (main) == 1) 
   handle = [];
else 
   handle = ArtisynthHandle(main);
end

