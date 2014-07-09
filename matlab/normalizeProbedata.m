%
%
% normalizeProbedata.m
%
% -- reads specified artisynth probe data file and scales the explicit
%    time data such that the scale is 1.0
% 
% Ian Stavness - 05/11/2008

function normalizeProbedata(filename)

if (nargin < 1)
    disp('normalizeProbedata requires filename argument');
    return;
end

probedata = asread(filename);

t = probedata.time * probedata.timescale;
probedata.time = t;
probedata.timescale = 1.0;

aswrite(filename, probedata);
