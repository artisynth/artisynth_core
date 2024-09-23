package artisynth.core.femmodels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.*;
import java.util.*;

import maspack.geometry.MeshBase;
import maspack.util.NumberFormat;

/**
 * Base class that can be used to implement FEM geometry writers.
 * 
 * @author John E Lloyd
 */
public abstract class FemWriterBase extends FemReaderWriterBase
   implements FemWriter {
   
   public static final String DEFAULT_FORMAT = "%g";
   protected NumberFormat myFmt = new NumberFormat(DEFAULT_FORMAT);

   File myFile;                 // file associated with the FEM writer, if any
   OutputStream myOstream;      // OutpytStream associated with the FEM writer, if any
   PrintWriter myPrintWriter;   // PrintWriter associated with the FEM writer, if any

   // XXX stub - get rid of this when refactoring done
   protected FemWriterBase() {
   }

   protected FemWriterBase (OutputStream os) {
      myOstream = os;
   }

   protected FemWriterBase (PrintWriter pw) {
      myPrintWriter = pw;
   }

   protected FemWriterBase (File file) throws IOException {
      myPrintWriter =
         new PrintWriter (new BufferedWriter (new FileWriter (file)));
      myFile = file;
   }

   public abstract void writeFem (FemModel3d mesh) throws IOException;
 
   /**
    * Sets the format used for writing floating point numbers, using a string
    * specification as described in the documentation for {@link NumberFormat}.
    * A null string sets the format to the default, which corresponds to '"%g"'
    * and causes floats to be written out in full precision.
    *
    * @param fmtStr format specification for writing floating point numbers
    */
   public void setFormat(String fmtStr) {
      if (fmtStr == null) {
         fmtStr = DEFAULT_FORMAT;
      }
      myFmt = new NumberFormat(fmtStr);
   }
   
   /**
    * Sets the format used for writing floating point numbers, as described by
    * {@link NumberFormat}. A null value sets the format to the default, which
    * corresponds to '"%g"' and causes floats to be written out in full
    * precision.
    *
    * @param fmt format for writing floating point numbers
    */
   public void setFormat(NumberFormat fmt) {
      myFmt.set(fmt);
   }

   /**
    * Queries the format used for writing floating point numbers.
    *
    * @return format for writing floating point numbers
    */   
   public NumberFormat getFormat() {
      return myFmt;
   }
   
   protected void closeQuietly(OutputStream out) {
      if (out != null) {
         try {
            out.close();
         } catch (IOException e) {}
      }
   }

   /**
    * Flushes the outputs for this writer.
    */
   public void flush() {
      if (myOstream != null) {
         flushQuietly(myOstream);
      }
      if (myPrintWriter != null) {
         myPrintWriter.flush();
      }
   }
   
   protected void flushQuietly(OutputStream out) {
      if (out != null) {
         try {
            out.flush();
         } catch (IOException e) {}
      }
   }
   
   /**
    * Closes the outputs for this writer.
    */
   public void close() {
      flush();
      if (myOstream != null) {
         closeQuietly(myOstream);
      }
      if (myPrintWriter != null) {
         myPrintWriter.close();
      }     
   }
   
   @Override
   protected void finalize() throws Throwable {
      super.finalize();
      close();
   }

}
