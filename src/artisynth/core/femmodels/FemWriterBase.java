package artisynth.core.femmodels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import maspack.geometry.MeshBase;
import maspack.util.NumberFormat;

public abstract class FemWriterBase implements FemWriter {
   
   public static final String DEFAULT_FORMAT = "%g";
   protected NumberFormat myFmt = new NumberFormat(DEFAULT_FORMAT);

   OutputStream myOstream;
   File myFile;

   // XXX stub - get rid of this when refactoring done
   protected FemWriterBase() {
      myOstream = null;
   }

   protected FemWriterBase (OutputStream os) {
      myOstream = os;
   }

   protected FemWriterBase (File file) throws IOException {
      this (new FileOutputStream (file));
      myFile = file;
   }

   protected FemWriterBase (String fileName) throws IOException {
      this (new File(fileName));
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
      flushQuietly(myOstream);
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
      closeQuietly(myOstream);
   }
   
   @Override
   protected void finalize() throws Throwable {
      super.finalize();
      close();
      
   }

}
