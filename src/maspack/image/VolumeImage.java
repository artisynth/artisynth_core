package maspack.image;

import maspack.matrix.AffineTransform3d;

/**
 * Base class for volumetric images
 */
public interface VolumeImage {

   /**
    * Voxel raw value at a given column, row, slice
    * @param channel channel number
    * @param col column index
    * @param row row index
    * @param slice slice index
    * @return voxel value
    */
   public double getValue(int channel, int col, int row, int slice);
   
   /**
    * Number of channels per the voxel (e.g. grayscale=1, RGBA=4)
    * @return number of channels
    */
   int getNumChannels();
   
   /**
    * Number of rows in the image, corresponds to image y-axis
    * @return number of rows in the image
    */
   public int getNumRows();

   /**
    * Number of columns in the image, corresponds to image x-axis
    * @return number of columns
    */
   public int getNumCols();
   
   /**
    * Number of slices in the image, corresponds to image z-axis
    * @return number of slices
    */
   public int getNumSlices();
   
   /**
    * Transform that converts voxels [col, row, slice] to world-coordinates of the center of the voxel
    * @return voxel transformation
    */
   public AffineTransform3d getVoxelTransform();
}
