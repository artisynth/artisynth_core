function [A] = amat (M)
%
% function [A] = amat (M)
%
%     Takes a matlab matrix M and returns a corresponding Artisynth matrix A.
%     If M is a dense matrix, then a MatrixNd is returned; otherwise, a
%     SparseMatrixNd is returned.
%
[nrows,ncols] = size(M);
if (issparse(M)) 
   % handle sparse matrix
   [rowIdxs,colIdxs,values] = find(M);
   nnz = length(rowIdxs);
   % set to 0-based indexing for ArtiSynth
   for k=1:nnz
      rowIdxs(k) = rowIdxs(k)-1;
      colIdxs(k) = colIdxs(k)-1;
   end
   A = maspack.matrix.SparseMatrixNd (nrows, ncols);
   A.set (rowIdxs, colIdxs, values, nnz);
else
   A = maspack.matrix.MatrixNd (nrows,ncols);
   A.set (M);
end
