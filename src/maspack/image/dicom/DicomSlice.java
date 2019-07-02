/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

import java.nio.ByteBuffer;
import java.util.Map.Entry;

import maspack.image.dicom.DicomElement.VR;
import maspack.image.dicom.DicomPixelBuffer.PixelType;
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
      RigidTransform3d imagePose;
      int rows;
      int cols;
      double pixelSpacingRows;
      double pixelSpacingCols;
      double sliceThickness;
      
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
      
      public int getNumCols() {
         return cols;
      }
      
      public int getNumRows() {
         return rows;
      }
      
   }
   
   SliceInfo info;
   private DicomHeader header;
   DicomPixelBuffer pixelBuff;
   
   private static DicomElement findElement(DicomElement[] elems, int tagId) {
      for (DicomElement elem : elems) {
         if (elem.getTag() == tagId) {
            return elem;
         } else if (elem.getVR() == VR.SQ) {
            elem = findElement(elem.getSequenceValue(), tagId);
            if (elem != null) {
               return elem;
            }
         } else if (elem.getVR() == VR.DL) {
            elem = findElement(elem.getSequenceItemValue(), tagId);
            if (elem != null) {
               return elem;
            }
         }
      }
      return null;
   }
   
   private static DicomElement findElement(DicomHeader header, int tagId) { 
         // search through all subsequences?
         for (Entry<Integer,DicomElement> entry : header.headerMap.entrySet()) {
            DicomElement elem = entry.getValue();
            if (elem.getTag() == tagId) {
               return elem;
            } else if (elem.getVR() == VR.SQ) {
               elem = findElement(elem.getSequenceValue(), tagId);
               if (elem != null) {
                  return elem;
               }
            } else if (elem.getVR() == VR.DL) {
               elem = findElement(elem.getSequenceItemValue(), tagId);
               if (elem != null) {
                  return elem;
               }
            }
         }
      return null;
   }
   
   /**
    * Construct a DICOM slice with a given title, DICOM header information, and
    * image pixels
    */
   public DicomSlice(String title, DicomHeader header, DicomPixelBuffer pixels) {
      this.header = header;
      this.pixelBuff = pixels;
      this.info = new SliceInfo(title);
      
      this.info.cols = header.getIntValue(DicomTag.COLUMNS, 1);
      this.info.rows = header.getIntValue(DicomTag.ROWS, 1);
      VectorNd imagePosition = header.getVectorValue(DicomTag.IMAGE_POSITION_PATIENT);
      if (imagePosition == null) {
         DicomElement pos = findElement(header, DicomTag.IMAGE_POSITION_PATIENT);
         if (pos != null) {
            imagePosition = pos.getVectorValue();
         }
      }
      VectorNd imageOrientation = header.getVectorValue(DicomTag.IMAGE_ORIENTATION_PATIENT);
      if (imagePosition == null) {
         DicomElement orient = findElement(header, DicomTag.IMAGE_ORIENTATION_PATIENT);
         if (orient != null) {
            imageOrientation = orient.getVectorValue();
         }
      }
      
      this.info.imagePose = new RigidTransform3d();
      if (imagePosition != null) {
         this.info.imagePose.p.set(imagePosition);
      }
      if (imageOrientation != null) {
         Vector3d x = new Vector3d(imageOrientation.get(0), imageOrientation.get(1), imageOrientation.get(2));
         Vector3d y = new Vector3d(imageOrientation.get(3), imageOrientation.get(4), imageOrientation.get(5));
         Vector3d z = new Vector3d();
         z.cross(x, y);
         this.info.imagePose.R.setColumn(0, x);
         this.info.imagePose.R.setColumn(1, y);
         this.info.imagePose.R.setColumn(2, z);
      }
      VectorNd pixelSpacing = header.getVectorValue(DicomTag.PIXEL_SPACING);
      if (pixelSpacing == null) {
         DicomElement e = findElement(header, DicomTag.PIXEL_SPACING);
         if (e != null) {
            pixelSpacing = e.getVectorValue();
         } else {
            pixelSpacing = new VectorNd(new double[]{1,1});
         }
      }
      this.info.pixelSpacingRows = pixelSpacing.get(0);
      this.info.pixelSpacingCols = pixelSpacing.get(1);
      double sliceThickness = header.getDecimalValue(DicomTag.SLICE_THICKNESS, -1);
      if (sliceThickness <= 0) {
         // try to find
         DicomElement e = findElement(header, DicomTag.SLICE_THICKNESS);
         if (e != null) {
            sliceThickness = e.getDecimalValue();
         } else {
            sliceThickness = 1.0;
         }
      }
      this.info.sliceThickness = sliceThickness;
      
      this.info.seriesNumber = header.getIntValue(DicomTag.SERIES_NUMBER, 1);
      this.info.seriesTime = header.getDateTime(DicomTag.SERIES_TIME);
      this.info.imageNumber = header.getIntValue(DicomTag.IMAGE_NUMBER, 1);
      this.info.imageTime = header.getDateTime(DicomTag.IMAGE_TIME);
      this.info.acquisitionNumber = header.getIntValue(DicomTag.AQUISITION_NUMBER, 0);
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
    * @param interp interpolator for converting pixels to appropriate form
    * @param pixels buffer array to fill
    */
   public void getPixels(int x, int y, 
      int dx, int dy,
      int nx, int ny, PixelType type, int scanline,
      DicomPixelInterpolator interp,
      ByteBuffer pixels) {
    
      for (int i=0; i<ny; i++) {
         int idx = (y + dy*i)*info.cols+x;
         int p = pixels.position ();
         pixelBuff.getPixels (idx, dx, nx, type, interp, pixels);
         if (scanline > 0) {
            pixels.position (p+scanline);
         }
      }
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
    * @param interp interpolator for converting pixels to appropriate form
    * @param pixels buffer array to fill
    * @param offset offset in output buffer to start filling values
    */
   public void getPixels(int x, int y, 
      int dx, int dy,
      int nx, int ny, PixelType type, int scanline,
      DicomPixelInterpolator interp,
      int[] pixels, int offset) {
    
      for (int i=0; i<ny; i++) {
         int idx = (y + dy*i)*info.cols+x;
         pixelBuff.getPixels (idx, dx, nx, type, interp, pixels, offset);
         if (scanline > 0) {
            offset += scanline;
         }
      }
   }
   
   /**
    * Gets raw pixel value from the slice
    * @param channel image channel 
    * @param x starting x pixel
    * @param y starting y pixel
    * @return raw value
    */
   public double getPixelValue(int channel, int x, int y) {
      int idx = y*info.cols+x;
      return pixelBuff.getRescaledValue (idx);
   }
   
   /**
    * Number of channels in the slice
    */
   public int getNumChannels() {
      return pixelBuff.getNumChannels();
   }
   
   /**
    * @return maximum pixel intensity in the slice
    */
   public double getMaxIntensity() {
      return pixelBuff.getMaxIntensity();
   }
   
   /**
    * @return minimum pixel intensity in the slice
    */
   public double getMinIntensity() {
      return pixelBuff.getMinIntensity();
   }

   /**
    * @return the DICOM header information for the slice
    */
   public DicomHeader getHeader() {
      return header;
   }
   
   /**
    * @return position of first voxel in the slice image (top-left)
    */
   public RigidTransform3d getImagePose() {
      return info.imagePose;
   }
   
   /**
    * @return information extracted from the slice
    */
   public SliceInfo getInfo() {
      return info;
   }

   public double getThickness () {
      return info.sliceThickness;
   }
   
   public double getRowSpacing() {
      return info.pixelSpacingRows;
   }
   
   public double getColSpacing() {
      return info.pixelSpacingCols;
   }
   
   public int getNumRows() {
      return info.rows;
   }
   
   public int getNumCols() {
      return info.cols;
   }
   
}
