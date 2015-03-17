function [M] = mget (obj)
%
% function [M] = mget (obj)
%
%      Takes an ArtiSynth matrix or vector object and returns the corresponding
%      MATLAB matrix object. Sparse matrices are returned as the MATLAB sparse
%      matrix type. If the object is not an ArtiSynth matrix or vector, then
%      the empty matrix is returned.
%
if (isa(obj,'maspack.matrix.Vector')) 
   M = zeros(obj.size(),1);
   for i=1:obj.size()
      % have to force conversion to integer using int32 because otherwise
      % matlab tries to call get(double[])
      M(i) = obj.get(int32(i-1));
   end
elseif (isa(obj,'maspack.matrix.DenseMatrix')) 
   nr = obj.rowSize();
   nc = obj.colSize();
   M = zeros(nr,nc);
   for i=1:nr
      for j=1:nc
         % have to force conversion to integer using int32 because otherwise
         % matlab tries to call get(double[],idx)
         M(i,j) = obj.get(int32(i-1),int32(j-1));
      end
   end
elseif (isa(obj,'maspack.matrix.SparseMatrix')) 
   nrows = obj.rowSize();
   ncols = obj.colSize();
   crs = maspack.matrix.CRSValues (obj);
   rowOffs = crs.getRowOffs();
   colIdxs = crs.getColIdxs();
   rowIdxs = zeros (crs.numNonZeroVals(), 1);
   for i=1:nrows
      for j=rowOffs(i):rowOffs(i+1)-1
         rowIdxs(j) = i;
      end
   end
   M = sparse (double(rowIdxs), double(colIdxs), crs.getValues(), nrows, ncols);
else 
   M = []
end
