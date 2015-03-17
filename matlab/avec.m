function [v] = avec (M)
% function [v] = avec (M)
%
%     Takes a matlab matrix M and returns a corresponding Artisynth VectorNd.
%     In order for this method to work, M must have a size of 1 in at least
%     one dimension.
%
[nrows,ncols] = size(M);
if (nrows == 1) 
   v = maspack.matrix.VectorNd (int32(ncols));
   for k=1:ncols
      v.set (k-1, M(1,k));
   end
elseif (ncols == 1)
   v = maspack.matrix.VectorNd (int32(nrows));
   for k=1:nrows
      v.set (k-1, M(k,1));
   end
else
   error ('Input must have size 1 in at least one dimension');
end
