/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import java.awt.Color;
import java.io.*;
import java.util.*;
import maspack.matrix.*;
import maspack.util.*;

public class PropertyDescTest extends UnitTest implements HasProperties {
   enum GeomObj {
      POINT, LINE, PLANE
   };

   byte myByte = 0x4;
   char myChar = 't';
   short myShort = 1234;
   int myInt = 12345678;
   long myLong = 123456781234L;
   float myFloat = 1.23e4f;
   double myDouble = 3.14567;
   boolean myBoolean = true;
   short[] myShortArray = new short[] { 1, 2, 3 };
   int[] myIntArray = new int[] { 11, 22, 33 };
   long[] myLongArray = new long[] { 111, 222, 333, 444 };
   float[] myFloatArray = new float[] { 1.1f, 2.2f, 3.3f };
   double[] myDoubleArray = new double[] { 2.4e3, 3.56e-5, 0.0 };
   Color myColor = Color.GREEN;
   GeomObj myGeomObj = GeomObj.LINE;
   VectorNd myVectorNd = new VectorNd (new double[] { 1.3, -9.8, 1e4 });
   MatrixNd myMatrixNd =
      new MatrixNd (2, 3, new double[] { -9.8, 1e3, 2.4, 1.1, 2.233, 4 });
   String myString = "Like, \"hi\" dude!";

   static protected PropertyList props =
      new PropertyList (PropertyDescTest.class);
   // static protected ArrayList<PropertyDesc> writeProps;

   static final PropertyInfo.Edit EditNever = PropertyInfo.Edit.Never;
   static final PropertyInfo.Edit EditAlways = PropertyInfo.Edit.Always;
   static final PropertyInfo.Edit EditSingle = PropertyInfo.Edit.Single;

   static void verifyOptions (
      String name, String fmt, String rng, boolean autoWrite,
      PropertyInfo.Edit editing) {
      PropertyDesc desc = props.get (name);
      if (desc == null) {
         throw new TestException ("property '" + name + "' not found");
      }
      String descFmt = desc.getPrintFormat();
      if ((fmt == null && descFmt != null) ||
          (fmt != null && descFmt == null) ||
          (fmt != null && descFmt != null && !fmt.equals (descFmt))) {
         throw new TestException ("property '" + name + "': print format is "
         + descFmt + ", expected " + fmt);
      }
      NumericInterval descRng = desc.getDefaultNumericRange();
      NumericInterval testRng = new DoubleInterval (rng);
      if ((testRng == null && descRng != null) ||
          (testRng != null && descRng == null) ||
          (testRng != null && descRng != null && !testRng.equals (descRng))) {
         throw new TestException ("property '" + name + "': numeric range is "
         + (descRng == null ? "null" : descRng.toString()) + ", expected "
         + rng);
      }
      if (desc.getAutoWrite() != autoWrite) {
         throw new TestException ("property '" + name + "': auto write is "
         + desc.getAutoWrite() + ", expected " + autoWrite);
      }
      if (desc.getEditing() != editing) {
         throw new TestException ("property '" + name + "': editing is "
         + desc.getEditing() + ", expected " + editing);
      }
   }

   static {
      props.add ("byte * *", "test byte", (byte)0x4);
      props.remove ("byte");
      props.add ("byte * *", "test byte", (byte)0x4, "NW %8.3f NE [0,5]");
      verifyOptions ("byte", "%8.3f", "[0,5]", false, EditNever);
      props.setOptions ("byte", "AW %12.3f [-1,1] 1E");
      verifyOptions ("byte", "%12.3f", "[-1,1]", true, EditSingle);
      props.setOptions ("byte", "AutoWrite AlwaysEdit %12.3f [-1,1]");
      verifyOptions ("byte", "%12.3f", "[-1,1]", true, EditAlways);
      props.get ("byte").setPrintFormat (null);
      props.add ("char * *", "test char", 't');
      props.add ("short * *", "test short", (short)1234);
      props.add ("int * *", "test int", 12345678);
      props.add ("long * *", "test long", 123456781234L);
      props.add ("float * *", "test float", 1.23e4f);
      props.add ("double * *", "test double", 3.14567);
      props.add ("boolean * *", "test boolean", true);
      props.add ("shortArray * *", "test shortArray", new short[] { 1, 2, 3 });
      props.add ("intArray * *", "test intArray", new int[] { 11, 22, 33 });
      props.add ("longArray * *", "test longArray", new long[] { 111, 222, 333,
                                                                444 });
      props.add ("floatArray * *", "test floatArray", new float[] { 1.1f, 2.2f,
                                                                   3.3f });
      props.add ("doubleArray * *", "test doubleArray", new double[] { 2.4e3,
                                                                      3.56e-5,
                                                                      0.0 });
      props.add ("color * *", "test color", Color.GREEN);
      props.add ("geomObj * *", "test geomObj", GeomObj.LINE);
      props.add ("vectorNd * *", "test vectorNd", new VectorNd (
         new double[] { 1.3, -9.8, 1e4 }));
      props.add ("matrixNd * *", "test matrixNd", new MatrixNd (
         2, 3, new double[] { -9.8, 1e3, 2.4, 1.1, 2.233, 4 }));
      props.add ("string * *", "test string", "Like, \"hi\" dude!");
      // props.initialize (PropertyDescTest.class);
   }

   public byte getByte() {
      return myByte;
   }

   public void setByte (byte val) {
      myByte = val;
   }

   public char getChar() {
      return myChar;
   }

   public void setChar (char val) {
      myChar = val;
   }

   public short getShort() {
      return myShort;
   }

   public void setShort (short val) {
      myShort = val;
   }

   public int getInt() {
      return myInt;
   }

   public void setInt (int val) {
      myInt = val;
   }

   public long getLong() {
      return myLong;
   }

   public void setLong (long val) {
      myLong = val;
   }

   public float getFloat() {
      return myFloat;
   }

   public void setFloat (float val) {
      myFloat = val;
   }

   public double getDouble() {
      return myDouble;
   }

   public void setDouble (double val) {
      myDouble = val;
   }

   public boolean getBoolean() {
      return myBoolean;
   }

   public void setBoolean (boolean val) {
      myBoolean = val;
   }

   public short[] getShortArray() {
      return myShortArray;
   }

   public void setShortArray (short[] vals) {
      if (vals.length != myShortArray.length) {
         myShortArray = new short[vals.length];
      }
      for (int i = 0; i < vals.length; i++) {
         myShortArray[i] = vals[i];
      }
      myShortArray = vals;
   }

   public int[] getIntArray() {
      return myIntArray;
   }

   public void setIntArray (int[] vals) {
      if (vals.length != myIntArray.length) {
         myIntArray = new int[vals.length];
      }
      for (int i = 0; i < vals.length; i++) {
         myIntArray[i] = vals[i];
      }
      myIntArray = vals;
   }

   public long[] getLongArray() {
      return myLongArray;
   }

   public void setLongArray (long[] vals) {
      if (vals.length != myLongArray.length) {
         myLongArray = new long[vals.length];
      }
      for (int i = 0; i < vals.length; i++) {
         myLongArray[i] = vals[i];
      }
      myLongArray = vals;
   }

   public float[] getFloatArray() {
      return myFloatArray;
   }

   public void setFloatArray (float[] vals) {
      if (vals.length != myFloatArray.length) {
         myFloatArray = new float[vals.length];
      }
      for (int i = 0; i < vals.length; i++) {
         myFloatArray[i] = vals[i];
      }
      myFloatArray = vals;
   }

   public double[] getDoubleArray() {
      return myDoubleArray;
   }

   public void setDoubleArray (double[] vals) {
      if (vals.length != myDoubleArray.length) {
         myDoubleArray = new double[vals.length];
      }
      for (int i = 0; i < vals.length; i++) {
         myDoubleArray[i] = vals[i];
      }
      myDoubleArray = vals;
   }

   public Color getColor() {
      return myColor;
   }

   public void setColor (Color val) {
      myColor = val;
   }

   public GeomObj getGeomObj() {
      return myGeomObj;
   }

   public void setGeomObj (GeomObj val) {
      myGeomObj = val;
   }

   public VectorNd getVectorNd() {
      return myVectorNd;
   }

   public void setVectorNd (VectorNd val) {
      myVectorNd.set (val);
   }

   public MatrixNd getMatrixNd() {
      return myMatrixNd;
   }

   public void setMatrixNd (MatrixNd val) {
      myMatrixNd.set (val);
   }

   public String getString() {
      return myString;
   }

   public void setString (String val) {
      myString = val;
   }

   public Property getProperty (String name) {
      return props.getProperty (name, this);
   }

   public PropertyList getAllPropertyInfo() {
      return null;
   }

   // public void write (PrintWriter pw, NumberFormat fmt) throws IOException {
   //    if (props.writeNonDefaultProps (this, pw, fmt, "\n[ ")) {
   //       IndentingPrintWriter.addIndentation (pw, -2);
   //       pw.println ("]");
   //    }
   //    else {
   //       pw.println ("[ ]");
   //    }
   // }

   public void writeAll (IndentingPrintWriter pw, NumberFormat fmt)
      throws IOException {
      pw.print ("\n[ ");
      pw.addIndentation (2);
      props.writeProps (this, pw, fmt, null);
      pw.addIndentation (-2);
      pw.println ("]");
   }

   public void scan (ReaderTokenizer rtok) throws IOException {
      rtok.scanToken ('[');
      while (props.scanProp (this, rtok))
         ;
      if (rtok.ttype != ']') {
         throw new IOException ("unexpected input: " + rtok);
      }
   }

   public void test() {
      StringWriter sw = new StringWriter (1024);
      IndentingPrintWriter pw = new IndentingPrintWriter (sw);
      
      try {
         writeAll (pw, null);
         pw.flush();
         String str0 = sw.toString();
         //System.out.println (str0);
         ReaderTokenizer rtok = new ReaderTokenizer (new StringReader (str0));
         scan (rtok);
         sw = new StringWriter (1024);
         pw = new IndentingPrintWriter (sw);
         writeAll (pw, null);
         pw.flush();
         String str1 = sw.toString();
         if (!str1.equals (str0)) {
            System.out.println ("First string:");
            System.out.println (str0);
            System.out.println ("Second string:");
            System.out.println (str1);
            throw new TestException ("contents changed by rescaning output");
         }
      }
      catch (IOException e) {
         throw new TestException ("IOException during test: "+e);
      }
   }

   public static void main (String[] args) {
      PropertyDescTest tester = new PropertyDescTest();
      tester.runtest();
   }
}
