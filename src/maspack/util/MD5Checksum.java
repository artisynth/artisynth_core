package maspack.util;

import java.util.Collection;

import maspack.matrix.Matrix;
import maspack.matrix.Vector;

import java.security.MessageDigest;

/**
 * Convenience class to help create MD5 checksums.
 */
public class MD5Checksum {

   MessageDigest myMD5;
   
   public MD5Checksum() {
      try {
         myMD5 = MessageDigest.getInstance("MD5");
      }
      catch (Exception e) {
         throw new InternalErrorException ("can't create MD5 sum", e);
      }
   }

   public void update (byte b) {
      myMD5.update (b);
   }

   public void update (byte[] bytes) {
      myMD5.update (bytes);
   }

   public void update (long lval) {
      byte[] bytes = new byte[8];
      for (int k=0; k<8; k++) {
         bytes[k] = (byte)((lval >> (k*8)) & 0xff);
      }
      myMD5.update (bytes);
   }

   public void update (int ival) {
      byte[] bytes = new byte[4];
      for (int k=0; k<4; k++) {
         bytes[k] = (byte)((ival >> (k*8)) & 0xff);
      }
      myMD5.update (bytes);
   }

   public void update (double dval) {
      update (Double.doubleToLongBits (dval));
   }

   public String toString() {
      StringBuilder sb = new StringBuilder();
      byte[] bytes = getBytes();
      for (int i=0; i<bytes.length; i++) {
         sb.append (String.format("%02x", bytes[i]));
      }
      return sb.toString();
   }

   public byte[] getBytes() {
      return myMD5.digest();
   }

   public void update (Matrix mat) {
      update (mat.rowSize());
      update (mat.colSize());
      for (int i=0; i<mat.rowSize(); i++) {
         for (int j=0; j<mat.colSize(); j++) {
            update (mat.get(i,j));
         }
      }      
   }

   public void update (Vector vec) {
      update (vec.size());
      for (int i=0; i<vec.size(); i++) {
         update (vec.get(i));
      }      
   }

}
