package maspack.dicom;

public interface DicomPixelBuffer {
   
   public enum PixelType {
      BYTE, SHORT, RGB
   }
   
   Object getPixels();
   PixelType getPixelType();
   int getNumPixels();
   Object getPixel(int n);
   
   public int getPixelsRGB(int x,
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelConverter interp);
   
   public int getPixelsByte(int x, 
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelConverter interp);
   
   public int getPixelsShort(int x,
      int dx,
      int nx, short[] pixels, int offset,
      DicomPixelConverter interp);
   
   public int getPixels(int x, 
      int dx,
      int nx,
      DicomPixelBuffer pixels, int offset,
      DicomPixelConverter interp);
   
   public int setPixelsRGB(int x,
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelConverter interp);
   
   public int setPixelsByte(int x, 
      int dx,
      int nx, byte[] pixels, int offset,
      DicomPixelConverter interp);
   
   public int setPixelsShort(int x,
      int dx,
      int nx, short[] pixels, int offset,
      DicomPixelConverter interp);
   
   public int setPixels(int x, 
      int dx,
      int nx,
      DicomPixelBuffer pixels, int offset,
      DicomPixelConverter interp);
   
   public int getMaxIntensity();
   public int getMinIntensity();
   
   public Object getBuffer();
   
}
