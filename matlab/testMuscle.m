%testMuscle.m
% script to test axial muscle material force length curves
% 
% Ian Stavness, 10/5/2012

setASclasspath;
import artisynth.core.materials.*

lmax = 1.5;
l = [0.4:0.02:lmax];
ex = [0:0.1:1];
F = zeros(length(ex),length(l));

% m = ConstantAxialMuscleMaterial();
% m = LinearAxialMuscleMaterial();
% m = PeckAxialMuscleMaterial();

%%% set properties for AxialMuscleMaterial
% m.setMaxForce(1);
% m.setOptLength(1);
% m.setMaxLength(1.4);
% m.setPassiveFraction(0.015);
% m.setTendonRatio(0);

m = BlemkerAxialMuscle();
m.setMaxForce(1);


for i = 1:length(ex)
    for j = 1:length(l)
        F(i,j) = m.computeF(l(j),0,0,ex(i));
    end
end

Fp = F(1,:);
Fa = F-repmat(Fp,length(ex),1);

clf;
plot(l,F);
hold on;
plot(l,Fa,':');