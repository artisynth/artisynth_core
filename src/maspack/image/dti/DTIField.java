package maspack.image.dti;

import maspack.image.VolumeImage;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.Vector3d;
import maspack.util.Clonable;

public class DTIField implements VolumeImage, Clonable {
   
   AffineTransform3d voxelTransform;
   Vector3d voxelSpacing;
   int cols;
   int rows;
   int slices;
   DTIVoxel[] voxels;
     
   public DTIField(int ncols, int nrows, int nslices, DTIVoxel[] voxels, 
      Vector3d voxelSpacing, AffineTransform3d voxelTransform) {
      this.voxelTransform = voxelTransform;
      this.voxelSpacing = voxelSpacing;
      this.voxels = voxels;
      this.cols = ncols;
      this.rows = nrows;
      this.slices = nslices;
   }

   public int getNumCols() {
     return cols;
   }
   
   public int getNumRows() {
      return rows;
   }
   
   public int getNumSlices() {
      return slices;
   }
   
   public DTIVoxel getVoxel(int c, int r, int s) {
      return voxels[c + cols*r + cols*rows*s];
   }
   
   public AffineTransform3d getVoxelTransform() {
      return voxelTransform;
   }
   
   public Vector3d getVoxelSpacing() {
      return voxelSpacing;
   }
   
   public DTIField clone() {
      DTIField copy;
      try {
         copy = (DTIField)super.clone();
      }
      catch (CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
      
      copy.voxelTransform = voxelTransform.copy();
      copy.voxels = new DTIVoxel[voxels.length];
      for (int i=0; i<voxels.length; ++i) {
         copy.voxels[i] = voxels[i].clone();
      }
      
      return copy;
   }

   /**
    * Channels correspond to the 6 diffusion tensor entries d00, d11, d22, d01, d02, d12
    */
   @Override
   public double getValue (int channel, int col, int row, int slice) {
      DTIVoxel voxel = getVoxel(col, row, slice);
      switch(channel) {
         case 0:
            return voxel.D.m00;
         case 1:
            return voxel.D.m11;
         case 2:
            return voxel.D.m22;
         case 3:
            return voxel.D.m01;
         case 4:
            return voxel.D.m02;
         case 5:
            return voxel.D.m12;
      }
      return 0;
   }

   /**
    * Channels correspond to the 6 diffusion tensor entries d00, d11, d22, d01, d02, d12
    * @return number of channels (6)
    */
   @Override
   public int getNumChannels () {
      return 6;
   }
   
}
