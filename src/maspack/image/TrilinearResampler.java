package maspack.image;

/**
 * Resamples image using trilinear interpolation
 */
public class TrilinearResampler implements VolumeImageResampler {

   @Override
   public double resample (VolumeImage image, int channel, double col, double row, double slice) {
      
      int icol = (int)Math.floor (col);
      int irow = (int)Math.floor (row);
      int islice = (int)Math.floor (slice);
      
      // out of range, return 0
      if (row < -0.5 || row > image.getNumRows ()-0.5
         || col < -0.5 || col > image.getNumCols ()-0.5
         || slice < -0.5 || slice > image.getNumSlices ()-0.5) {
         return 0;
      }
      
      if (icol < 0) {
         ++icol;
      } else if (icol >= image.getNumCols ()-1) {
         --col;
      }
      if (irow < 0) {
         ++irow;
      } else if (irow >= image.getNumRows ()-1) {
         --irow;
      }
      if (islice < 0) {
         ++islice;
      } else if (islice >= image.getNumSlices ()-1) {
         --islice;
      }
      
      double dx = col - icol;
      double dy = row - irow;
      double dz = slice - islice;
      
      double a000 = image.getValue (channel, icol, irow, islice);
      double a100 = image.getValue (channel, icol+1, irow, islice);
      double a010 = image.getValue (channel, icol, irow+1, islice);
      double a001 = image.getValue (channel, icol, irow, islice+1);
      double a110 = image.getValue (channel, icol+1, irow+1, islice);
      double a101 = image.getValue (channel, icol+1, irow, islice+1);
      double a011 = image.getValue (channel, icol, irow+1, islice+1);
      double a111 = image.getValue (channel, icol+1, irow+1, islice+1);
      
      double v = a000*(1-dx)*(1-dy)*(1-dz) + a100*dx*(1-dy)*(1-dz) + a010*(1-dx)*dy*(1-dz)
         + a001*(1-dx)*(1-dy)*dz + a110*dx*dy*(1-dz) + a101*dx*(1-dy)*dz
         + a011*(1-dx)*dy*dz + a111*dx*dy*dz;
      
      return v;
   }

   
   
}
