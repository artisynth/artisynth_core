package maspack.image.nifti;

import java.nio.ByteBuffer;

import maspack.properties.CompositePropertyBase;
import maspack.util.DynamicArray;
import maspack.util.Versioned;

public abstract class NiftiPixelGenerator extends CompositePropertyBase implements Versioned {

   static DynamicArray<Class<?>> mySubclasses = new DynamicArray<>(new Class<?>[] {});

   public static void registerSubclass(Class<? extends NiftiPixelGenerator> cls) {
      if (!mySubclasses.contains(cls)) {
         mySubclasses.add(cls);
      }
   }
   
   public static Class<?>[] getSubClasses() {
      return mySubclasses.getArray();
   }
   
   public static enum Format {
      /**
       * 1-byte grayscale
       */
      GRAYSCALE,
      /**
       * 2-byte grayscale-alpha
       */
      GRAYSCALE_ALPHA,
      /**
       * 3-byte RGB
       */
      RGB,
      /**
       * 4-byte RGBA
       */
      RGBA
   }
   
   public abstract Format getFormat();
   
   
   /**
    * Fills a byte buffer with pixels
    * @param i  column index
    * @param di column step
    * @param ni number of columns
    * @param j  row index
    * @param dj row step
    * @param nj number of rows
    * @param k  slice index
    * @param dk slice step
    * @param nk number of slices
    * @param t  time
    * @param scanline number of bytes per line (y-stride)
    * @param pageline number of bytes per page (z-stride)
    * @param rgb output buffer
    */
   public abstract void getPixels(NiftiDataBuffer buff, int i, int di, int ni, 
      int j, int dj, int nj, 
      int k, int dk, int nk,
      int t,
      int scanline, int pageline,
      ByteBuffer rgb);
   
}
