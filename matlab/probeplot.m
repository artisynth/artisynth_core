%
% probeplot.m
% 
% script to import and plot probe data from artisynth
%
% usage: probeplot(filename)
% - where filename is the probe data filename
%
% Note: the script assumes that the probe data uses explicit time
%       and therefore the first column of data is time data
%
% Ian Stavness - 05/11/2008

function probeplot(filename)

%close all;

%load data file
probedata = asread(filename);

if (probedata.timetype == 'explicit')
    % plot all data columms versus time on the same plot
    figure;
    plot(t, probedata.data);
else
    disp('probeplot required probe data with explicit time');
end


title('Probe Data');
xlabel('Time (s)');
ylabel('Data');