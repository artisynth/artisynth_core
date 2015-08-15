package maspack.dicom;

import maspack.dicom.DicomElement.VR;

public class DicomPixelData {

   VR vr;

   public final byte[] b;
   public final short[] s;
   public final float[] f;
   public final Object data;

   public DicomPixelData(VR type, int size) {
      this.vr = type;
      switch (type) {
         case OB:
            b = new byte[size];
            data = b;
            f = null;
            s = null;
            break;
         case OW:
            s = new short[size];
            data = s;
            f = null;
            b = null;
            break;
         case OF:
            f = new float[size];
            data = f;
            b = null;
            s = null;
            break;
         default:
            throw new IllegalArgumentException("Only types OB, OW, OF can be initialized with a size");
      }
   }
   
   public DicomPixelData(VR type, Object data) {
      this.vr = type;
      switch (type) {
         case OB:
            b = (byte[])data;
            this.data = b;
            f = null;
            s = null;
            break;
         case OW:
            s = (short[])data;
            this.data = s;
            f = null;
            b = null;
            break;
         case OF:
            f = (float[])data;
            this.data = f;
            b = null;
            s = null;
            break;
         default:
            this.data = data;
            b = null;
            s = null;
            f = null;
      }
   }
   
   public VR getType() {
      return vr;
   }
   
   public Object getData() {
      return data;
   }
   
   public byte[] getByteData() {
      return b;
   }
   
   public short[] getShortData() {
      return s;
   }
   
   public float[] getFloatData() {
      return f;
   }

}
