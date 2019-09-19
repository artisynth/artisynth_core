classdef ArtisynthHandle 
   properties
      main_
   end

   methods 
      function obj = ArtisynthHandle (main)
         obj.main_ = main;
      end

      function [m] = getMain(obj)
         m = obj.main_;
	 if (m.isDisposed()) 
            error ('main object in handler no longer active');
         end
      end

      function [status] = loadModel (obj, name, varargin) 
	 main = obj.getMain();
         classname = main.getDemoClassName (name);
	 % TODO add arguments         
         if ( isempty(classname) ) 
            error (['No class found for model ', name]);
         end
         args = [];
         if (isempty(varargin) == 0)
            nargs = length(varargin);
            args = javaArray('java.lang.String', nargs);
            for i = 1:nargs
               args(i) = java.lang.String(varargin{i});
            end
         end
         main.loadModel (classname, name, args);
      end

      function [status] = loadModelFile (obj, filename)
	 main = obj.getMain();
	 file = java.io.File (filename);
         if (file.canRead() == 0) 
            error (['File ', filename, ' not found or not readable']);
         end
         main.loadModelFile (file);
      end

      function [status] = saveModelFile (obj, filename, varargin) 
	 main = obj.getMain();
	 file = java.io.File (filename);
         if (isempty(varargin) == 0)
            nargs = length(varargin);
            if (nargs == 1)
               main.saveModelFile (file,[],varargin{1},false);
            else
               main.saveModelFile (file,[],varargin{1},varargin{2});
            end
         else
            main.saveModelFile (file);
         end
      end

      function play (obj, time)
         if (nargin < 2) 
            obj.getMain().play();
         else 
            obj.getMain().play(time);
         end
      end
         
      function pause (obj)
         obj.getMain().pause();
      end         

      function delay (obj, t)
         obj.getMain().delay(t);
      end         

      function waitForStop (obj)
         obj.getMain().waitForStop();
      end         

      function [exception] = getSimulationException (obj)
         exception = obj.getMain().getSimulationException();
      end         

      function [playing] = isPlaying (obj)
         playing = obj.getMain().isSimulating();
      end         

      function [t] = getTime (obj)
         t = obj.getMain().getTime();
      end         

      function reset (obj)
         obj.getMain().reset();
      end         

      function [moved] = rewind (obj)
         moved = obj.getMain().rewind();
      end

      function [moved] = forward (obj)
         moved = obj.getMain().forward();
      end

      function step (obj)
         obj.getMain().step();
      end   
      
      function reload (obj)
         obj.getMain().reloadModel();
      end         

      function [way] = addWayPoint (obj, t)
         way = obj.getMain().addWayPoint (t);
      end

      function [brk] = addBreakPoint (obj, t)
         brk = obj.getMain().addBreakPoint (t);
      end

      function [status] = removeWayPoint (obj, t)
         status = obj.getMain().removeWayPoint (t);
      end

      function clearWayPoints (obj)
         obj.getMain().clearWayPoints ();
      end

      function loadWayPoints (obj, filename)
	 main = obj.getMain();
	 file = java.io.File (filename);
         if (file.canRead() == 0) 
            error (['File ', filename, ' not found or not readable']);
         end
         main.loadWayPoints (file);
      end

      function saveWayPoints (obj, filename)
	 main = obj.getMain();
	 file = java.io.File (filename);
         main.saveWayPoints (file);
      end

      function [r] = root(obj)
         r = obj.getMain().getRootModel();
      end

      function [c] = find (obj, path)
         r = obj.getMain().getRootModel();
         c = r.findComponent (path);
      end

      function [res] = getsel (obj, idx)
         cur = obj.getMain().getSelectionManager().getCurrentSelection();
	 list = cell(cur.toArray());
         if nargin < 2
            res = cell(cur.toArray());
         else 
   	    dimen = size(list);
            if (idx <= dimen(1)) 
               res = list{idx};
            else 
               res = [];
            end
         end
      end

      function [M] = getIprobeData (obj, name) 
         probe = find (obj, ['inputProbes/', name]);
         if isempty (probe) == 1
            M = [];
         else 
            M = probe.getValues();
         end
      end            

      function [status] = setIprobeData (obj, name, M) 
         probe = find (obj, ['inputProbes/', name]);
         if isempty (probe) == 1
            status = 0;
         else 
            probe.setValues (M);
            status = 1;
         end
      end      

      function [M] = getOprobeData (obj, name) 
         probe = find (obj, ['outputProbes/', name]);
         if isempty (probe) == 1
            M = [];
         else 
            M = probe.getValues();
         end
      end            

      function [status] = setOprobeData (obj, name, M) 
         probe = find (obj, ['outputProbes/', name]);
         if isempty (probe) == 1
            status = 0;
         else 
            probe.setValues (M);
            status = 1;
         end
      end            

      function quit (obj) 
         obj.getMain().quit() 
      end        
   end
end
