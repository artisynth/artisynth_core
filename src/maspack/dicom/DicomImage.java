package maspack.dicom;

import java.util.Arrays;

import maspack.dicom.DicomPixelBuffer.PixelType;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;

public class DicomImage {

   public static final int MAX_CAP_ADJUSTMENT = 128;
   
   String title;
   int rows;
   int cols;
   double pixelSpacingRows;
   double pixelSpacingCols;
   double pixelSpacingSlice;
   PixelType pixelType;
   
   RigidTransform3d trans;
   
   DicomSlice[] slices;
   int timeOffsets[];
   int size;
   
   public DicomImage(String title, DicomSlice firstSlice) {
      this.title = title;
      this.rows = firstSlice.info.rows;
      this.cols = firstSlice.info.cols;
      this.pixelSpacingRows = firstSlice.info.pixelSpacingRows;
      this.pixelSpacingCols = firstSlice.info.pixelSpacingCols;
      this.pixelSpacingSlice = firstSlice.getHeader().getDecimalValue(DicomTag.SPACING_BETWEEN_SLICES, 
         (float)firstSlice.info.pixelSpacingSlice);
      this.pixelType = firstSlice.pixelBuff.getPixelType();
      
      slices = new DicomSlice[16];
      timeOffsets = new int[1];
      timeOffsets[0] = 0;
      size = 0;
      
      this.trans = new RigidTransform3d(firstSlice.info.imagePosition);
      addSlice(firstSlice);
   }
   
   public boolean compatible(DicomSlice slice) {
      if (slice.info.rows == rows &&
         slice.info.cols == cols &&
         slice.info.pixelSpacingRows == pixelSpacingRows &&
         slice.info.pixelSpacingCols == pixelSpacingCols &&
         slice.getPixelType() == pixelType) {
         // XXX check if same coordinate system?
         return true;
      }
      return false;
   }
   
   public boolean addSlice(DicomSlice slice) {
      if (compatible(slice)) {
         maybeGrowCapacity(size+1);
         doAddSlice(slice);
         return true;
      } else {
         return false;
      }
   }
   
   private void doAddSlice(DicomSlice slice) { 
      
      if (size == 0) {
         slices[0] = slice;
         timeOffsets[0] = 0;
         size++;
         return;
      }
      
      // sort first by time, then by zval
      long tadd = slice.info.temporalPosition;
      long t = slices[size-1].info.temporalPosition;
      
      Vector3d zdir = new Vector3d();
      Vector3d orig = trans.p;
      trans.R.getColumn(2, zdir);
      Vector3d tmp = new Vector3d();
   
      tmp.sub(slice.info.imagePosition.p, orig);
      double zadd = tmp.dot(zdir);
      
      // check if it's the last slice
      tmp.sub(slices[size-1].info.imagePosition.p, orig);
      double z = tmp.dot(zdir);
      if (tadd > t) {
         timeOffsets = Arrays.copyOf(timeOffsets, timeOffsets.length+1);
         timeOffsets[timeOffsets.length-1] = size;
         slices[size++] = slice;
         return;
      } else if ((tadd == t) && (zadd >= z)) {
         slices[size++] = slice;
         return;
      }
      
      // look for time
      int pos = 0;
      for (int i=0; i<timeOffsets.length; i++) {
         pos = timeOffsets[i];
         if (slices[pos] == null) {
            System.err.println("hmm...");
         }
         t = slices[pos].info.temporalPosition;
         if (tadd < t) {
            
            // expand/shift time offsets
            int[] newTimeOffsets = new int[timeOffsets.length+1];
            for (int j=0; j<i; j++) {
               newTimeOffsets[j] = timeOffsets[j];
            }
            newTimeOffsets[i] = pos; 
            for (int j=i+1; j < newTimeOffsets.length; j++) {
               newTimeOffsets[j] = timeOffsets[j-1]+1;
            }
            timeOffsets = newTimeOffsets;

            break;
         } else if (tadd == t) {
            
            boolean found = false;
            for (int j=timeOffsets[i]; j<size; j++) {
               tmp.sub(slices[j].info.imagePosition.p, orig);
               t = slices[j].info.temporalPosition;
               z = tmp.dot(zdir);
               if (t > tadd || zadd < z) {
                  pos = j;
                  
                  for (int k = i+1; k < timeOffsets.length; k++) {
                     timeOffsets[k]++;
                     if (timeOffsets[k] > size+1) {
                        System.err.println("bad time offset");
                     }
                  }
                  found = true;
                  break;
               }
            }   
            
            if (found) {
               break;
            }
         }
      }
      
      // shift slices up
      for (int i=size-1; i >= pos; i--) {
         slices[i+1] = slices[i];
      }
      slices[pos] = slice;
            
      if (pos == 0) {
         trans.set(slice.info.imagePosition);
      }
      size++;
   }
   
   private void maybeGrowCapacity(int cap) {
      if (slices.length < cap) {
         ensureCapacity(Math.min(2*cap, cap+MAX_CAP_ADJUSTMENT));
      }
   }
   
   public void ensureCapacity(int cap) {
      if (slices.length < cap) {
         DicomSlice[] oldSlices = slices;
         slices = new DicomSlice[cap];
         for (int i=0; i<size; i++) {
            slices[i] = oldSlices[i];
         }
      }
   }
   
   public void trim() {
      if (size < slices.length) {
         DicomSlice[] oldSlices = slices;
         slices = new DicomSlice[size];
         for (int i=0; i<size; i++) {
            slices[i] = oldSlices[i];
         }
      }
   }
   
   public void complete() {
      trim();
   }
   
   public PixelType getPixelType() {
      return pixelType;
   }
   
   @Override
   public String toString() {
      StringBuilder str = new StringBuilder();
      str.append("Title: " + title + "\n");
      str.append("Size: " + rows + "x" + cols + "x" + size + "\n");
      str.append("Times: " + timeOffsets.length + "\n");
      str.append("Resolution: " + pixelSpacingRows + "x" + pixelSpacingCols + "x" + pixelSpacingSlice + "\n");
      str.append("Type: " + pixelType + "\n");
      str.append("Location: " + trans.toString() + "\n");
      return str.toString();
   }
   
   public int getPixelsRGB(int x, int y, int z, 
      int dx, int dy, int dz,
      int nx, int ny, int nz, 
      int time,
      byte[] pixels,
      DicomPixelConverter interp) {
      z = timeOffsets[time] + z;
      
      return getPixelsRGB(x,y,z,dx,dy,dz,nx,ny,nz,pixels,interp);
   }
   
   public int getPixelsByte(int x, int y, int z, 
      int dx, int dy, int dz,
      int nx, int ny, int nz, 
      int time,
      byte[] pixels,
      DicomPixelConverter interp) {
      z = timeOffsets[time] + z;
      
      return getPixelsByte(x,y,z,dx,dy,dz,nx,ny,nz,pixels,interp);
   }
   
   public int getPixelsShort(int x, int y, int z, 
      int dx, int dy, int dz,
      int nx, int ny, int nz, 
      int time,
      short[] pixels,
      DicomPixelConverter interp) {
      z = timeOffsets[time] + z;
      
      return getPixelsShort(x,y,z,dx,dy,dz,nx,ny,nz,pixels,interp);
   }
   
   public int getPixels(int x, int y, int z, 
      int dx, int dy, int dz,
      int nx, int ny, int nz, 
      int time,
      DicomPixelBuffer pixels,
      DicomPixelConverter interp) {
      z = timeOffsets[time] + z;
      
      return getPixels(x,y,z,dx,dy,dz,nx,ny,nz,pixels,interp);
   }
   
   public int getPixelsRGB(int x, int y, int z, 
      int dx, int dy, int dz,
      int nx, int ny, int nz, byte[] pixels,
      DicomPixelConverter interp) {

      int offset = 0;
      for (int i=0; i<nz; i++) {
         int idx = z+dz*i;
         offset = slices[idx].getPixelsRGB(x, y, dx, dy, nx, ny, pixels, offset, interp);
      }
      return offset;
   }
   
   public int getPixelsByte(int x, int y, int z, 
      int dx, int dy, int dz,
      int nx, int ny, int nz, byte[] pixels,
      DicomPixelConverter interp) {
      
      int offset = 0;
      for (int i=0; i<nz; i++) {
         int idx = z+dz*i;
         offset = slices[idx].getPixelsByte(x, y, dx, dy, nx, ny, pixels, offset, interp);
      }
      return offset;
   }
   
   public int getPixelsShort(int x, int y, int z, 
      int dx, int dy, int dz,
      int nx, int ny, int nz, short[] pixels,
      DicomPixelConverter interp) {
      
      int offset = 0;
      for (int i=0; i<nz; i++) {
         int idx = z+dz*i;
         offset = slices[idx].getPixelsShort(x, y, dx, dy, nx, ny, pixels, offset, interp);
      }
      return offset;
   }
   
   public int getPixels(int x, int y, int z,  
      int dx, int dy, int dz,
      int nx, int ny, int nz,
      DicomPixelBuffer pixels,
      DicomPixelConverter interp) {
      
      int offset = 0;
      for (int i=0; i<nz; i++) {
         int idx = z+dz*i;
         offset = slices[idx].getPixels(x, y, dx, dy, nx, ny, pixels, offset, interp);
      }
      return offset;
   }
   
   public int getNumRows() {
      return rows;
   }
   
   public int getNumCols() {
      return cols;
   }
   
   public int getNumTimes() {
      return timeOffsets.length;
   }
   
   public int getNumSlices() {
      if (timeOffsets.length == 1) {
         return size;
      } else {
         // number of slices before first offset?
         return timeOffsets[1];
      }
   }
   
   public int getMaxIntensity() {
      
      int max = 0;
      for (int i=0; i<size; i++) {
         int maxb = slices[i].getMaxIntensity();
         if (maxb > max) {
            max = maxb;
         }
      }
      return max;
   }
   
   public int getMinIntensity() {
      int min = Integer.MAX_VALUE;
      for (int i=0; i<size; i++) {
         int minb = slices[i].getMinIntensity();
         if (minb < min) {
            min = minb;
         }
      }
      return min;
   }
   
   public DicomSlice getSlice(int slice) {
      return slices[slice];
   }
   
   public DicomSlice getSlice(int timeIdx, int slice ) {
      return getSlice(timeOffsets[timeIdx]+slice);
   }
   
   
   public RigidTransform3d getTransform() {
      return trans;
   }
   
   public AffineTransform3d getPixelTransform() {
      
      int nSlices = getNumSlices();
      // compute slice thickness directly from slice separation
      // (slice thickness is sometimes different)
      if (nSlices > 0) {
         double zsize = slices[nSlices-1].info.imagePosition.p.distance(slices[0].info.imagePosition.p);
         pixelSpacingSlice = zsize/(nSlices-1);
      }      
      
      AffineTransform3d pixelTrans = new AffineTransform3d(trans);
      pixelTrans.applyScaling(pixelSpacingRows, pixelSpacingCols, pixelSpacingSlice);
      return pixelTrans;
   }
   
}
