%
%
% normalizeAllProbedata.m
%
% -- searches for all .txt files in specified directory and normalizes data
%
%
% Ian Stavness - 05/11/2008

function normalizeAllProbedata(dirname)

if (nargin < 1 || ~isdir(dirname))
    disp('normalizeProbedata requires valid directory as argument');
    return;
end

files = dir(sprintf('%s/*.txt', dirname));
for i = 1:length(files)
    fullpath = sprintf('%s/%s',dirname,files(i).name);
    normalizeProbedata(fullpath);
    disp(sprintf('normalizing %s', fullpath));
end
