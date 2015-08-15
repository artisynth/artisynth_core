package maspack.dicom;

public class ShortPixelBuffer implements DicomPixelBuffer {

   short[] pixels;

   public ShortPixelBuffer(int size) {
      this.pixels = new short[size];
   }
   
   public ShortPixelBuffer(short[] pixels) {
      this.pixels = pixels;
   }

   @Override
   public Object getPixels() {
      return pixels;
   }

   @Override
   public PixelType getPixelType() {
      return PixelType.SHORT;
   }

   @Override
   public int getNumPixels() {
      return pixels.length;
   }

   @Override
   public Short getPixel(int n) {
      return pixels[n];
   }

   public int getPixelsRGB(int x,
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelConverter interp) {

      int iidx = x;
      int oidx = offset;
      for (int i = 0; i < nx; i++) {
         oidx = interp.interpShortRGB(this.pixels, iidx, pixels, oidx);
         iidx += dx;
      }
      return oidx;
   }

   public int getPixelsByte(int x, 
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelConverter interp) {

      int iidx = x;
      int oidx = offset;
      for (int i = 0; i < nx; i++) {
         oidx = interp.interpShortByte(this.pixels, iidx, pixels, oidx);
         iidx += dx;
      }
      return oidx;
   }

   public int getPixelsShort(int x,
      int dx,
      int nx, short[] pixels, int offset,
      DicomPixelConverter interp) {

      int iidx = x;
      int oidx = offset;
      for (int i = 0; i < nx; i++) {
         oidx = interp.interpShortShort(this.pixels, iidx, pixels, oidx);
         iidx += dx;
      }
      return oidx;
   }

   public int getPixels(int x, 
      int dx,
      int nx,
      DicomPixelBuffer pixels, int offset,
      DicomPixelConverter interp) {

      return pixels.setPixelsShort(x, dx, nx, this.pixels, offset, interp);
   }

   public int setPixelsRGB(int x,
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelConverter interp) {

      int oidx = x;
      int iidx = offset;
      for (int i = 0; i < nx; i++) { 
         iidx = interp.interpRGBShort(pixels, iidx, this.pixels, oidx);
         oidx += dx;
      }
      return iidx;
   }

   public int setPixelsByte(int x, 
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelConverter interp) {

      int oidx = x;
      int iidx = offset;
      for (int i = 0; i < nx; i++) {
         iidx = interp.interpByteShort(pixels, iidx, this.pixels, oidx);
         oidx += dx;
      }
      return iidx;
   }

   public int setPixelsShort(int x,
      int dx,
      int nx, short[] pixels, int offset,
      DicomPixelConverter interp) {

      int oidx = x;
      int iidx = offset;
      for (int i = 0; i < nx; i++) {
         iidx = interp.interpShortShort(pixels, iidx, this.pixels, oidx);
         oidx += dx;
      }
      return iidx;
   }

   public int setPixels(int x, 
      int dx,
      int nx,
      DicomPixelBuffer pixels, int offset,
      DicomPixelConverter interp) {

      return pixels.getPixelsShort(x, dx, nx, this.pixels, offset, interp);
   }
   
   public int getMaxIntensity() {
      int max = Short.MIN_VALUE;
      for (int i=0; i<pixels.length; i++) {
         int val = pixels[i];
         if (val > max) {
            max = val;
         }
      }
      return max;
   }
   
   public int getMinIntensity() {
      int min = Short.MAX_VALUE;
      for (int i=0; i<pixels.length; i++) {
         int val = pixels[i];
         if (val < min) {
            min = val;
         }
      }
      return min;
   }

   public short[] getBuffer() {
      return pixels;
   }
   
}
