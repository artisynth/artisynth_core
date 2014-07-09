package maspack.geometry.io;

import java.io.*;

import maspack.util.NumberFormat;
import maspack.geometry.MeshBase;

public abstract class MeshWriterBase implements MeshWriter {
   
   public static final String DEFAULT_FORMAT = "%g";
   protected NumberFormat myFmt = new NumberFormat(DEFAULT_FORMAT);

   OutputStream myOstream;
   File myFile;

   // XXX stub - get rid of this when refactoring done
   protected MeshWriterBase() {
      myOstream = null;
   }

   protected MeshWriterBase (OutputStream os) throws IOException{
      myOstream = os;
   }

   protected MeshWriterBase (File file) throws IOException {
      this (new FileOutputStream (file));
      myFile = file;
   }

   protected MeshWriterBase (String fileName) throws IOException {
      this (new File(fileName));
   }
//
//   public void write (MeshBase mesh) throws IOException {
//      write (myOstream, mesh);
//   }

//   public abstract void write (OutputStream os, MeshBase mesh)
//      throws IOException;      
   
   public abstract void writeMesh (MeshBase mesh) throws IOException;
   
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
   
   public void close() {
      closeQuietly(myOstream);
   }
   
   @Override
   protected void finalize() throws Throwable {
      super.finalize();
      closeQuietly(myOstream);
      
   }

}
