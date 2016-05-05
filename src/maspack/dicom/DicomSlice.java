/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.dicom;

import java.nio.ByteBuffer;

import maspack.dicom.DicomPixelBuffer.PixelType;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;

/**
 * A single DICOM slice, with DICOM header information and image buffer
 * @author Antonio
 */
public class DicomSlice {

   /**
    * Size, location and series information of he DICOM slice
    * @author Antonio
    *
    */
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
   
   /**
    * Construct a DICOM slice with a given title, DICOM header information, and image pixels
    * @param title
    * @param header
    * @param pixels
    */
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
   
   /**
    * @return Returns the pixel type of the slice (byte/short grayscale, byte RGB)
    */
   public PixelType getPixelType() {
      return pixelBuff.getPixelType();
   }
   
   /**
    * @return the underlying pixel buffer
    */
   public DicomPixelBuffer getPixelBuffer() {
      return pixelBuff;
   }
   
   /**
    * Populates a buffer of pixels from the slice, 
    * interpolated using an interpolator
    * 
    * @param x starting x voxel
    * @param y starting y voxel
    * @param dx voxel step in x direction
    * @param dy voxel step in y direction
    * @param nx number of voxels in x direction
    * @param ny number of voxels in y direction
    * @param type pixel type to convert to
    * @param scanline width of line (stride between lines)
    * @param pixels buffer array to fill
    * @param interp interpolator for converting pixels to appropriate form
    */
   public void getPixels(int x, int y, 
      int dx, int dy,
      int nx, int ny, PixelType type, int scanline,
      ByteBuffer pixels,
      DicomPixelInterpolator interp) {
    
      
      for (int i=0; i<ny; i++) {
         int idx = (y + dy*i)*info.cols+x;
         int p = pixels.position ();
         pixelBuff.getPixels (idx, dx, nx, type, pixels, interp);
         if (scanline > 0) {
            pixels.position (p+scanline);
         }
      }
   }
   
   /**
    * Populates an array of RGB(byte) pixels from the slice, 
    * interpolated using an interpolator
    * 
    * @param x starting x voxel
    * @param y starting y voxel
    * @param dx voxel step in x direction
    * @param dy voxel step in y direction
    * @param nx number of voxels in x direction
    * @param ny number of voxels in y direction
    * @param pixels output array to fill
    * @param offset offset in output pixel array
    * @param interp interpolator for converting pixels to appropriate form
    * @return the next unfilled index in the output pixel array 
    */
   public int getPixelsRGB(int x, int y, 
      int dx, int dy,
      int nx, int ny, byte[] pixels, int offset,
      DicomPixelInterpolator interp) {
    
      for (int i=0; i<ny; i++) {
         int idx = (y + dy*i)*info.cols+x;
         offset = pixelBuff.getPixelsRGB(idx, dx, nx, pixels, offset, interp);
      }
      
      return offset;
   }
   
   /**
    * Populates an array of grayscale(byte) pixels from the slice, 
    * interpolated using an interpolator
    * 
    * @param x starting x voxel
    * @param y starting y voxel
    * @param dx voxel step in x direction
    * @param dy voxel step in y direction
    * @param nx number of voxels in x direction
    * @param ny number of voxels in y direction
    * @param pixels output array to fill
    * @param offset offset in output pixel array
    * @param interp interpolator for converting pixels to appropriate form
    * @return the next unfilled index in the output pixel array 
    */
   public int getPixelsByte(int x, int y, 
      int dx, int dy,
      int nx, int ny, byte[] pixels, int offset,
      DicomPixelInterpolator interp) {
      
      for (int i=0; i<ny; i++) {
         int idx = (y + dy*i)*info.cols+x;
         offset = pixelBuff.getPixelsByte(idx, dx, nx, pixels, offset, interp);
      }
      
      return offset;
   }
   
   /**
    * Populates an array of grayscale(short) pixels from the slice, 
    * interpolated using an interpolator
    * 
    * @param x starting x voxel
    * @param y starting y voxel
    * @param dx voxel step in x direction
    * @param dy voxel step in y direction
    * @param nx number of voxels in x direction
    * @param ny number of voxels in y direction
    * @param pixels output array to fill
    * @param offset offset in output pixel array
    * @param interp interpolator for converting pixels to appropriate form
    * @return the next unfilled index in the output pixel array 
    */
   public int getPixelsShort(int x, int y,
      int dx, int dy, 
      int nx, int ny, short[] pixels, int offset,
      DicomPixelInterpolator interp) {
      
      for (int i=0; i<ny; i++) {
         int idx = (y + dy*i)*info.cols + x;
         offset = pixelBuff.getPixelsShort(idx, dx, nx, pixels, offset, interp);
      }
      
      return offset;
   }
   
   /**
    * Populates a pixel buffer from the slice, interpolated using an interpolator
    * 
    * @param x starting x voxel
    * @param y starting y voxel
    * @param dx voxel step in x direction
    * @param dy voxel step in y direction
    * @param nx number of voxels in x direction
    * @param ny number of voxels in y direction
    * @param pixels output buffer to fill
    * @param offset offset in output pixel buffer
    * @param interp interpolator for converting pixels to appropriate form
    * @return the next unfilled index in the output pixel array 
    */
   public int getPixels(int x, int y, 
      int dx, int dy,
      int nx, int ny,
      DicomPixelBuffer pixels, int offset,
      DicomPixelInterpolator interp) {
      
      for (int i=0; i<ny; i++) {
         int idx = (y + dy*i)*info.cols + x;
         offset = pixelBuff.getPixels(idx, dx, nx, pixels, offset, interp);
      }
      return offset;
   }
   
   /**
    * @return maximum pixel intensity in the slice
    */
   public int getMaxIntensity() {
      return pixelBuff.getMaxIntensity();
   }
   
   /**
    * @return minimum pixel intensity in the slice
    */
   public int getMinIntensity() {
      return pixelBuff.getMinIntensity();
   }

   /**
    * @return the DICOM header information for the slice
    */
   public DicomHeader getHeader() {
      return header;
   }
   
}
