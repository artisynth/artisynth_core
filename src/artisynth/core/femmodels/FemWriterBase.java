package artisynth.core.femmodels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import maspack.geometry.MeshBase;
import maspack.util.NumberFormat;

public abstract class FemWriterBase implements FemWriter {
   
   public static final String DEFAULT_FORMAT = "%g";
   protected NumberFormat myFmt = new NumberFormat(DEFAULT_FORMAT);

   OutputStream myOstream;
   PrintWriter myPrintWriter;
   File myFile;

   // XXX stub - get rid of this when refactoring done
   protected FemWriterBase() {
   }

   protected FemWriterBase (OutputStream os) {
      myOstream = os;
   }

   protected FemWriterBase (PrintWriter pw) {
      myPrintWriter = pw;
   }

   //
//   public void write (MeshBase mesh) throws IOException {
//      write (myOstream, mesh);
//   }

//   public abstract void write (OutputStream os, MeshBase mesh)
//      throws IOException;      
   
   public abstract void writeFem (FemModel3d mesh) throws IOException;
   
   public void setFormat(String fmtStr) {
      if (fmtStr == null) {
         fmtStr = DEFAULT_FORMAT;
      }
      myFmt = new NumberFormat(fmtStr);
   }
   
   public void setFormat(NumberFormat fmt) {
      myFmt.set(fmt);
   }
   
   public NumberFormat getFormat() {
      return myFmt;
   }
   
   private void closeQuietly(OutputStream out) {
      if (out != null) {
         try {
            out.close();
         } catch (IOException e) {}
      }
   }
   
   public void flush() {
      if (myOstream != null) {
         flushQuietly(myOstream);
      }
      if (myPrintWriter != null) {
         myPrintWriter.flush();
      }
   }
   
   private void flushQuietly(OutputStream out) {
      if (out != null) {
         try {
            out.flush();
         } catch (IOException e) {}
      }
   }
   
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
