package maspack.image;

import java.util.Arrays;

/**
 * Resamples image by taking a convolution with 3D kernel
 */
public class ConvolutionResampler implements VolumeImageResampler {

   public static class ConvolutionKernel3d {
      double[][][] values;
      double[] offsets;
      
      public ConvolutionKernel3d(double[] kernel, double offset) {
         this(kernel, offset, kernel, offset, kernel, offset);
      }
      
      public ConvolutionKernel3d(double[] xkernel, double xoffset, double[] ykernel, double yoffset, double[] zkernel, double zoffset) {
         this.values = new double[xkernel.length][ykernel.length][zkernel.length];
         this.offsets = new double[] {xoffset, yoffset, zoffset};
         
         for (int i=0; i<xkernel.length; ++i) {
            for (int j=0; j<ykernel.length; ++j) {
               for (int k=0; k<zkernel.length; ++k) {
                  values[i][j][k] = xkernel[i]*ykernel[j]*zkernel[k];
               }
            }
         }
      }
      
      public ConvolutionKernel3d(double[][][] values, double[] offsets) {
         this.values = new double[values.length][][];
         for (int i=0; i<values.length; ++i) {
            this.values[i] = new double[values[i].length][];
            for (int j=0; j<values[i].length; ++j) {
               this.values[i][j] = Arrays.copyOf (values[i][j], values[i][j].length);
            }
         }
         this.offsets = Arrays.copyOf (offsets, offsets.length);
      }
   }
   
   ConvolutionKernel3d kernel;
   
   public ConvolutionResampler(ConvolutionKernel3d kernel) {
      this.kernel = kernel;
   }
   
   @Override
   public double resample (
      VolumeImage image, int channel, double col, double row, double slice) {

      int icol = (int)Math.floor (col + kernel.offsets[0]);
      int irow = (int)Math.floor (row + kernel.offsets[1]);
      int islice = (int)Math.floor (slice + kernel.offsets[2]);
      
      double dx = col + kernel.offsets[0] - icol;
      double dy = row + kernel.offsets[1] - irow;
      double dz = slice + kernel.offsets[2] - islice;
      
      // integrate over kernel function
      double v = 0;
      
      // fractional interpolation
      for (int i=0; i<kernel.values.length; ++i) {
         int ic = icol + i; // floor

         for (int j=0; j<kernel.values[i].length; ++j) {
            int ir = irow + j;
            
            for (int k = 0; k<kernel.values[i][j].length; ++k) {
               int is = islice + k;
               
               double a000 = 0;
               double a100 = 0;
               double a010 = 0;
               double a001 = 0;
               double a110 = 0;
               double a101 = 0;
               double a011 = 0;
               double a111 = 0;
               
               if (ic >= 0 && ic < image.getNumCols ()) {
                  if (ir >= 0 && ir < image.getNumRows ()) {
                     if (is >= 0 && is < image.getNumSlices ()) {
                        a000 = image.getValue (channel, ic, ir, is);
                     }
                     if (is+1 >= 0 && is+1 < image.getNumSlices ()) {
                        a001 = image.getValue (channel, ic, ir, is+1);
                     }
                  }
                  if (ir+1 >= 0 && ir+1 < image.getNumRows ()) {
                     if (is >= 0 && is < image.getNumSlices ()) {
                        a010 = image.getValue (channel, ic, ir+1, is);
                     }
                     if (is+1 >= 0 && is+1 < image.getNumSlices ()) {
                        a011 = image.getValue (channel, ic, ir+1, is+1);
                     }                     
                  }
               }
               if (ic+1 >= 0 && ic+1 < image.getNumCols ()) {
                  if (ir >= 0 && ir < image.getNumRows ()) {
                     if (is >= 0 && is < image.getNumSlices ()) {
                        a100 = image.getValue (channel, ic+1, ir, is);
                     }
                     if (is+1 >= 0 && is+1 < image.getNumSlices ()) {
                        a101 = image.getValue (channel, ic+1, ir, is+1);
                     }                     
                  }
                  if (ir+1 >= 0 && ir+1 < image.getNumRows ()) {
                     if (is >= 0 && is < image.getNumSlices ()) {
                        a110 = image.getValue (channel, ic+1, ir+1, is);
                     }
                     if (is+1 >= 0 && is+1 < image.getNumSlices ()) {
                        a111 = image.getValue (channel, ic+1, ir+1, is+1);
                     }                     
                  }
               }
               
               double cv = a000*(1-dx)*(1-dy)*(1-dz) + a100*dx*(1-dy)*(1-dz) + a010*(1-dx)*dy*(1-dz)
                  + a001*(1-dx)*(1-dy)*dz + a110*dx*dy*(1-dz) + a101*dx*(1-dy)*dz
                  + a011*(1-dx)*dy*dz + a111*dx*dy*dz;
               
               v += kernel.values[i][j][k] * cv;
            }
         }
      }
      
      return v;
   }

}
