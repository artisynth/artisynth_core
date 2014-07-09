%testSpring.m
% script to test axial spring material force length curves
% 
% Ian Stavness, 10/5/2012

% setASclasspath;
import artisynth.core.materials.*

m = LinearAxialMaterial();
m.setStiffness(1);

lmax = 2;
l = [0:0.1:lmax];
nl = length(l);
F = zeros(nl,1);

for i = 1:nl
   F(i) = m.computeF(l(i),0,0,0);
end

plot(l,F);