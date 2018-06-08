package maspack.image;

/**
 * Helper object for resampling a volumetric image
 */
public interface VolumeImageResampler {

   /**
    * Samples an image channel at new (potentially fractional) col, row, slice
    * @param image image to resample
    * @param channel image channel
    * @param col image column
    * @param row image row 
    * @param slice image slice
    * @return resampled channel value
    */
   public double resample(VolumeImage image, int channel, double col, double row, double slice);
   
}
