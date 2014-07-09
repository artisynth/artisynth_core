%
% aswrite.m
% 
% writes artisynth data file from matlab struct with 
% probe header info and data
% - see asread.m, doc/misc/artisynthFileFormat.txt
%
% params:
%  filename - filename to write
%  probedata - struct with probe data
%
% Ian Stavness - 05/11/2008


function  aswrite(filename, probedata)


% check number and type of arguments
if nargin < 2
  error('Function requires two input arguments');
end

header = sprintf('%d %d %f\n%s %d %s\n', ...
    probedata.timestart, probedata.timestop, probedata.timescale, ...
    probedata.interptype, probedata.numdata, probedata.timetype);
%     t(1)*1000000000, t(length(t))*1000000000, size(pdata,2)-1);

fid = fopen(filename, 'wt');
fprintf(fid,header);
fclose(fid);

if (probedata.timetype == 'explicit')
    rawdata = [probedata.time, probedata.data];
else
    rawdata = probedata.data;
end
save(filename, '-append', '-ascii', '-double', 'rawdata');
