%
% startAS.m
% - script to start artisynth from within Matlab and load necessary
% objects into Matlab workspace
% 
% -- Ian Stavness -- 02/6/2009

%%% add artisynth jars and classes to dynamic java path
setASclasspath;

starter = artisynth.core.driver.StartFromMatlab;
main = starter.getMain();

%%% cache artisynth objects
workspace = main.getWorkspace();
sched = main.getScheduler();
timeline = main.getTimeline();

%%% load demo
% demo = 'Spring Mesh';
% class = main.getDemoClassName(demo);
% main.loadModel(demo, class);
% root = workspace.getRootModel();

%%% uncomment to start simulation from within Matlab
% timeline.getOps.playTimeline();
% sched.waitForPlayingToStop;
% timeline.getOps.rewindTimeline();
