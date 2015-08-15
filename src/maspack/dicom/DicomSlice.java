package maspack.dicom;

import maspack.dicom.DicomPixelBuffer.PixelType;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;


public class DicomSlice {

   public static class SliceInfo {
      
      String title;
      RigidTransform3d imagePosition;
      int rows;
      int cols;
      double pixelSpacingRows;
      double pixelSpacingCols;
      double pixelSpacingSlice;
      
      int seriesNumber;
      DicomDateTime seriesTime;
      int imageNumber;
      DicomDateTime imageTime;
      int acquisitionNumber;
      DicomDateTime acquisitionTime;
      
      int temporalPosition;
      
      public SliceInfo(String title) {
         this.title = title;
      }
      
   }
   
   SliceInfo info;
   private DicomHeader header;
   DicomPixelBuffer pixelBuff;
   
   public DicomSlice(String title, DicomHeader header, DicomPixelBuffer pixels) {
      this.header = header;
      this.pixelBuff = pixels;
      this.info = new SliceInfo(title);
      
      this.info.cols = header.getIntValue(DicomTag.COLUMNS, 1);
      this.info.rows = header.getIntValue(DicomTag.ROWS, 1);
      VectorNd imagePosition = header.getVectorValue(DicomTag.IMAGE_POSITION_PATIENT);
      VectorNd imageOrientation = header.getVectorValue(DicomTag.IMAGE_ORIENTATION_PATIENT);
      
      this.info.imagePosition = new RigidTransform3d();
      if (imagePosition != null) {
         this.info.imagePosition.p.set(imagePosition);
      }
      if (imageOrientation != null) {
         Vector3d x = new Vector3d(imageOrientation.get(0), imageOrientation.get(1), imageOrientation.get(2));
         Vector3d y = new Vector3d(imageOrientation.get(3), imageOrientation.get(4), imageOrientation.get(5));
         Vector3d z = new Vector3d();
         z.cross(x, y);
         this.info.imagePosition.R.setColumn(0, x);
         this.info.imagePosition.R.setColumn(1, y);
         this.info.imagePosition.R.setColumn(2, z);
      }
      VectorNd pixelSpacing = header.getVectorValue(DicomTag.PIXEL_SPACING);
      this.info.pixelSpacingRows = pixelSpacing.get(0);
      this.info.pixelSpacingCols = pixelSpacing.get(1);
      this.info.pixelSpacingSlice = header.getDecimalValue(DicomTag.SLICE_THICKNESS, 1);
      
      this.info.seriesNumber = header.getIntValue(DicomTag.SERIES_NUMBER, 1);
      this.info.seriesTime = header.getDateTime(DicomTag.SERIES_TIME);
      this.info.imageNumber = header.getIntValue(DicomTag.IMAGE_NUMBER, 1);
      this.info.imageTime = header.getDateTime(DicomTag.IMAGE_TIME);
      this.info.acquisitionNumber = header.getIntValue(DicomTag.AQUISITION_NUMBER, 0);
      this.info.acquisitionTime = header.getDateTime(DicomTag.AQUISITION_TIME);
      
      this.info.acquisitionTime = header.getDateTime(DicomTag.AQUISITION_TIME);
      this.info.temporalPosition = header.getIntValue(DicomTag.TEMPORAL_POSITON_IDENTIFIER, -1);
      
   }
   
   public PixelType getPixelType() {
      return pixelBuff.getPixelType();
   }
   
   public int getPixelsRGB(int x, int y, 
      int dx, int dy,
      int nx, int ny, byte[] pixels, int offset,
      DicomPixelConverter interp) {
    
      for (int i=0; i<ny; i++) {
         int idx = (y + dy*i)*nx;
         offset = pixelBuff.getPixelsRGB(idx, dx, nx, pixels, offset, interp);
      }
      
      return offset;
   }
   
   public int getPixelsByte(int x, int y, 
      int dx, int dy,
      int nx, int ny, byte[] pixels, int offset,
      DicomPixelConverter interp) {
      
      for (int i=0; i<ny; i++) {
         int idx = (y + dy*i)*info.cols + x;
         offset = pixelBuff.getPixelsByte(idx, dx, nx, pixels, offset, interp);
      }
      
      //      for (int i=0; i<nx; i++) {
      //         int idx = (x + dx*i)*info.cols + y;
      //         offset = pixelBuff.getPixelsByte(idx, dy, ny, pixels, offset, interp);
      //      }
      
      return offset;
   }
   
   public int getPixelsShort(int x, int y,
      int dx, int dy, 
      int nx, int ny, short[] pixels, int offset,
      DicomPixelConverter interp) {
      
      for (int i=0; i<ny; i++) {
         int idx = (y + dy*i)*info.cols + x;
         offset = pixelBuff.getPixelsShort(idx, dx, nx, pixels, offset, interp);
      }
      
      return offset;
   }
   
   public int getPixels(int x, int y, 
      int dx, int dy,
      int nx, int ny,
      DicomPixelBuffer pixels, int offset,
      DicomPixelConverter interp) {
      
      for (int i=0; i<ny; i++) {
         int idx = (y + dy*i)*info.cols + x;
         offset = pixelBuff.getPixels(idx, dx, nx, pixels, offset, interp);
      }
      return offset;
   }
   
   public int getMaxIntensity() {
      return pixelBuff.getMaxIntensity();
   }
   
   public int getMinIntensity() {
      return pixelBuff.getMinIntensity();
   }

   public DicomHeader getHeader() {
      return header;
   }
   
}
