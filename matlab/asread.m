%
% asread.m
% 
% reads artisynth probe data file and returns matlab struct with 
%  probe header info and data
% - see doc/misc/artisynthFileFormat.txt
%
% params:
%  filename - filename to load
%
% Ian Stavness - 05/11/2008


function probedata = asread(filename)


% check number and type of arguments
if nargin < 1
  error('Function requires one input argument');
elseif ~isstr(filename)
  error('Input must be a string representing a filename');
end

fid = fopen(filename);
if fid==-1
  error('File not found or permission denied');
end

% parse header
[timeinfo, cnt] = fscanf(fid, '%d %d %f');
[interp, cnt] = fscanf(fid, '%s%c', 1);
[N, cnt] = fscanf(fid, '%d', 1);
[tmp, cnt] = fscanf(fid, '%s%c\n', 1);
if isnumeric(tmp)
    timetype = 'implicit'
    timestep = str2num(tmp);
else
    timetype = tmp; % 'explicit'
    timestep = 0;
end

fgetl(fid); % get newline of header

% read data from rest of file
alldata = [];
line = fgetl(fid);
while (line ~= -1)
    alldata = [alldata; str2num(line)];
    line = fgetl(fid);
end

if (timestep == 'explicit')
    
    [N,M] = size(alldata);
    time = alldata(:,1);
    data = alldata(:,2:M);
else
    data = alldata;
    time = [];
end

probedata = struct('timestart', timeinfo(1), 'timestop', timeinfo(2), ...
    'timescale', timeinfo(3), 'timetype', timetype, 'timestep', timestep, ...
    'interptype', interp, 'numdata', N, 'time', time, 'data', data);

fclose(fid);


