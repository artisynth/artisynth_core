/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.*;
import java.io.*;

public class BinaryStreamTest {

   ArrayList<Object> myTests;

   // marker class to indicate bytes should skipped
   private class Skip {
      int nskip;

      Skip (int n) {
         nskip = n;
      }
   }       

   void test() throws IOException {
      BinaryOutputStream out;
      BinaryInputStream in;

      ByteArrayOutputStream buf;

      buf = new ByteArrayOutputStream();
      out = new BinaryOutputStream (buf);
      loadData (out);
      in = new BinaryInputStream (new ByteArrayInputStream (buf.toByteArray()));
      checkData (in);

      buf = new ByteArrayOutputStream();
      out = new BinaryOutputStream (
         buf, BinaryOutputStream.LITTLE_ENDIAN);
      loadData (out);
      in = new BinaryInputStream (
         new ByteArrayInputStream (buf.toByteArray()),
         BinaryInputStream.LITTLE_ENDIAN);
      checkData (in);

      buf = new ByteArrayOutputStream();
      out = new BinaryOutputStream (
         buf, BinaryOutputStream.LITTLE_ENDIAN);
      out.setByteChar (true);
      loadData (out);
      in = new BinaryInputStream (
         new ByteArrayInputStream (buf.toByteArray()),
         BinaryInputStream.LITTLE_ENDIAN);
      in.setByteChar (true);
      checkData (in);
   }

   private void checkObject (Object objChk, Object objRead) {
      if (objChk instanceof byte[]) {
         if (objRead instanceof byte[]) {
            byte[] byteChk = (byte[])objChk;
            byte[] byteRead = (byte[])objRead;
            
            if (byteChk.length == byteRead.length) {
               int i;
               for (i=0; i<byteChk.length; i++) {
                  if (byteChk[i] != byteRead[i]) {
                     break;
                  }
               }
               if (i == byteChk.length) {
                  return;
               }
            }
         }
      }
      else if (objChk.equals (objRead)) {
         return;
      }
      if (objChk instanceof Long) {
         NumberFormat fmt = new NumberFormat ("0x%x");
         throw new TestException (
            "Object read from stream was "+
            fmt.format((Long)objRead)+", expected " +
            fmt.format((Long)objChk));
      }
      else {
         throw new TestException (
            "Object read from stream was "+objRead+", expected " + objChk);
      }
   }         

   private void checkUnsigned (Object objChk, int value) {
      if (objChk instanceof Short) {
         int schk = ((Short)objChk).shortValue();
         if (value == (0xffff & schk)) {
            return;
         }
      }
      else if (objChk instanceof Byte) {
         int bchk = ((Byte)objChk).byteValue();
         if (value == (0xff & bchk)) {
            return;
         }
      }
      throw new TestException (
         "Object read from stream was "+value+", expected " + objChk);
   }         

   private int myCheckCount = 0;

   // Check that the specified stream has advanced its count by size
   private void checkCount (Object stream, int size) {
      int cnt;
      if (stream instanceof BinaryOutputStream) {
         cnt = ((BinaryOutputStream)stream).size();
      }
      else if (stream instanceof BinaryInputStream) {
         cnt = ((BinaryInputStream)stream).getByteCount();
      }
      else {
         throw new TestException ("Unknown stream type " + stream);
      }
      if (cnt != myCheckCount+size) {
         throw new TestException (
            "Stream count is "+cnt+", expected " + (myCheckCount+size));
      }

      myCheckCount += size;

   }

   void loadData (BinaryOutputStream out) throws IOException {
      
      myCheckCount = 0;
      for (Object obj : myTests) {
         if (obj instanceof Boolean) {
            out.writeBoolean (((Boolean)obj).booleanValue());
            checkCount (out, 1);
         }
         else if (obj instanceof Byte) {
            out.writeByte (((Byte)obj).byteValue());
            checkCount (out, 1);
            out.writeByte (((Byte)obj).byteValue());
            checkCount (out, 1);
         }
         else if (obj instanceof Character) {
            out.writeChar (((Character)obj).charValue());
            checkCount (out, out.usesByteChar() ? 1 : 2);
         }
         else if (obj instanceof Short) {
            out.writeShort (((Short)obj).shortValue());
            checkCount (out, 2);
            out.writeShort (((Short)obj).shortValue());
            checkCount (out, 2);
         }
         else if (obj instanceof Integer) {
            out.writeInt (((Integer)obj).intValue());
            checkCount (out, 4);
         }
         else if (obj instanceof Long) {
            out.writeLong (((Long)obj).longValue());
            checkCount (out, 8);            
         }
         else if (obj instanceof Float) {
            out.writeFloat (((Float)obj).floatValue());
            checkCount (out, 4);            
         }
         else if (obj instanceof Double) {
            out.writeDouble (((Double)obj).doubleValue());
            checkCount (out, 8);
         }
         else if (obj instanceof byte[]) {
            byte[] data = (byte[])obj;
            out.write (data, 0, data.length);
            checkCount (out, data.length);
         }
         else if (obj instanceof String) {
            String s = (String)obj;
            int len = s.length();
            out.writeBytes (s);
            checkCount (out, len);
            out.writeChars (s);
            checkCount (out, out.usesByteChar() ? len : 2*len);
            out.writeUTF (s);
            checkCount (out, 2+len);
         }
         else if (obj instanceof Skip) {
            int n = ((Skip)obj).nskip;
            for (int i=0; i<n; i++) {
               out.write (0);
            }
            checkCount (out, n);
         }
      }
   }

   void checkData (BinaryInputStream in) throws IOException {
      
      myCheckCount = 0;
      for (Object obj : myTests) {
         if (obj instanceof Boolean) {
            checkObject (obj, in.readBoolean());
            checkCount (in, 1);
         }
         else if (obj instanceof Byte) {
            checkObject (obj, in.readByte());
            checkCount (in, 1);
            checkUnsigned (obj, in.readUnsignedByte());
            checkCount (in, 1);
         }
         else if (obj instanceof Character) {
            checkObject (obj, in.readChar());
            checkCount (in, in.usesByteChar() ? 1 : 2);
         }
         else if (obj instanceof Short) {
            checkObject (obj, in.readShort());
            checkCount (in, 2);
            checkUnsigned (obj, in.readUnsignedShort());
            checkCount (in, 2);
         }
         else if (obj instanceof Integer) {
            checkObject (obj, in.readInt());
            checkCount (in, 4);
         }
         else if (obj instanceof Long) {
            checkObject (obj, in.readLong());
            checkCount (in, 8);
         }
         else if (obj instanceof Float) {
            checkObject (obj, in.readFloat());
            checkCount (in, 4);
         }
         else if (obj instanceof Double) {
            checkObject (obj, in.readDouble());
            checkCount (in, 8);
         }
         else if (obj instanceof byte[]) {
            int len = ((byte[])obj).length;
            byte[] data = new byte[len];
            in.readFully (data);
            checkObject (obj, data);
            checkCount (in, len);
         }
         else if (obj instanceof String) {
            int len = ((String)obj).length();
            byte[] data = new byte[len];
            in.readFully (data);
            checkObject (obj, new String(data));
            checkCount (in, len);
            char[] chars = new char[len];
            for (int i=0; i<len; i++) {
               chars[i] = in.readChar ();
            }
            checkObject (obj, new String(chars));
            checkCount (in, in.usesByteChar() ? len : 2*len);
            checkObject (obj, in.readUTF());
            checkCount (in, 2+len);
         }
         else if (obj instanceof Skip) {
            int n = ((Skip)obj).nskip;
            in.skipBytes (n);
            checkCount (in, n);
         }
      }
   }
   
   public BinaryStreamTest() {
      myTests = new ArrayList<Object>();
      myTests.add (0x12345678);
      myTests.add (Math.PI);
      myTests.add (Math.PI*1e-78);
      myTests.add (Math.PI*1.7895e56);
      myTests.add (145.879f);
      myTests.add (0xdeadbeef);
      myTests.add ((short)0xabcd);
      myTests.add (new Skip(3));
      myTests.add ((char)0x18);
      myTests.add ((byte)0x77);
      myTests.add ("hi there!");
      myTests.add (new byte[] { 0x12, (byte)0xff, 0x06, (byte)0x89 });
      myTests.add (true);
      myTests.add (false);
      myTests.add (new Skip(7));
      myTests.add (new Long (0xff66772211009977L));
      myTests.add (new Long (0x8066772211009977L));
      myTests.add (new Long (0x1234567891234577L));
      myTests.add (new Long (0x1234567fff234577L));
   }

   public static void main (String[] args) {

      BinaryStreamTest tester = new BinaryStreamTest();
      try {
         tester.test();
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit(1); 
      }
      System.out.println ("\nPassed\n");
   }
}
