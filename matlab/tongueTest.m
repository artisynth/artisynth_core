    % test the Simple muscle Model for a single damping

msec = 1000000;

if exist('main')
    %timeline.getOps.rewindTimeline;
    % reset timeline
    timeline.getOps.stopTimeline;
else
% startup Artisynth and the muscle model
main = artisynth.core.driver.Main;
main.start('Tongue3d');
root = main.getRoot();
sched = main.getScheduler;
timeline = main.getTimeline;
fem  = root.getModel('Tongue');
fem.getMuscleList.get(0).setActivation(2.0);
%fem.setGravity(9.8);

% Rayleigh Damping
fem.setMaxStepSize(msec); % 1ms
end
%fem.setParticleDamping(6.0);
%fem.setStiffnessDamping(0.01);
%fem.setParticleDamping(6.0);
%fem.setStiffnessDamping(0.05);
fem.setParticleDamping(2.0);
fem.setStiffnessDamping(0.01);

%fem.setDensity(1000);
fem.setPoissonsRatio(.45);
fem.setYoungsModulus(6000);
fem.setWarping(false);


% prefix output probe filenames with experiments names 
% prefix must be a java String
% example: outputprobePrefix = java.lang.String('implicit_nu_0.1_');
if exist('outputprobePrefix')
    if exist('probenames')==0
        for probe=1:root.numOutputProbes-1,
            probenames{probe}=root.getOutputProbe(probe).getAttachedFileName;
        end
    end
    for probe=1:root.numOutputProbes-1,
        root.getOutputProbe(probe).setAttachedFileName(outputprobePrefix.concat(probenames{probe}));
    end

end


% simulate for 120ms
timeline.getOps.stepForwardTimeline(120*msec);
sched.waitForPlayingToStop;
timeline.getOps.saveAllProbes;

clear probe;


% Display results
%plotOutputProbes;