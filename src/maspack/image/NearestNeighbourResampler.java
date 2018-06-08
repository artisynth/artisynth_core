package maspack.image;

/**
 * Resamples image by doing a nearest-neighbour look-up
 */
public class NearestNeighbourResampler implements VolumeImageResampler {

   @Override
   public double resample (VolumeImage image, int channel, double col, double row, double slice) {
      
      int icol = (int)Math.round (col);
      int irow = (int)Math.round (row);
      int islice = (int)Math.round (slice);
      
      // out of range, return 0
      if (irow < 0 || irow > image.getNumRows ()-1
         || icol < 0 || icol > image.getNumCols ()-1
         || islice < 0 || islice > image.getNumSlices ()-1) {
         return 0;
      }
      
      return image.getValue (channel, icol, irow, islice);
   }

   
   
}
