% run experiment suite 

prefix='AS1_';
tasks={'GGP_2N','GGA_0.5N','STY_2N','HG_2.0N','GGP_1N_TRANS_2N','IL_0.5N','SL_0.5N','TRANS_2.0N'};
outputprobePrefix = java.lang.String([prefix tasks{1} '_']);
tongueTest;
position2displacement([prefix tasks{1} '_']);




% for all tasks 
for t=1:size(tasks,2),
    fem.setActivations(0.0);

    disp(tasks{t});
    if t==1
        fem.getMuscleList.get(0).setActivation(2.0);
    elseif t==2
        fem.getMuscleList.get(2).setActivation(0.5);
    elseif t==3
        fem.getMuscleList.get(3).setActivation(2.0);
    elseif t==4
        fem.getMuscleList.get(6).setActivation(2.0);
    elseif t==5
        fem.getMuscleList.get(0).setActivation(1.0);
        fem.getMuscleList.get(7).setActivation(2.0);
    elseif t==6
        fem.getMuscleList.get(9).setActivation(0.5);
    elseif t==7
        fem.getMuscleList.get(10).setActivation(0.5);
    elseif t==8
        fem.getMuscleList.get(7).setActivation(2.0);
    else
        disp('what t??');
    end

    outputprobePrefix = java.lang.String([prefix tasks{t} '_']);
    tongueTest;
    position2displacement([prefix tasks{t} '_']);

end