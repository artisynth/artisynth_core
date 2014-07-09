package maspack.geometry.io;

import java.io.*;

import maspack.util.*;
import maspack.geometry.*;
import maspack.geometry.io.MeshWriter.DataFormat;
import maspack.geometry.io.MeshWriter.FloatType;
import maspack.geometry.io.PlyWriter.DataType;

public class GenericMeshWriter implements MeshWriter {

   protected MeshWriter myWriter;

   public GenericMeshWriter (String fileName) throws IOException {
      this (new File(fileName));
   }

   public GenericMeshWriter (File file) throws IOException {
      String fileName = file.getName();
      if (fileName.endsWith (".ply")) {
         myWriter = new PlyWriter (file);
      }
      else if (fileName.endsWith (".obj")) {
         myWriter = new WavefrontWriter(file);
      }
      else if (fileName.endsWith (".off")) {
         myWriter = new OffWriter(file);
      }
      else if (fileName.endsWith (".stl")) {
         myWriter = new StlWriter(file);
      }
      else if (fileName.endsWith (".xyzb")) {
         myWriter = new XyzbWriter(file);
      }
      else {
         throw new UnsupportedOperationException (
            "File "+fileName+" has unrecognized extension");
      }
   }

   public void setFormat (GenericMeshReader reader) {
      if (myWriter instanceof PlyWriter) {
         PlyWriter plyWriter = (PlyWriter)myWriter;
         plyWriter.setDataFormat (reader.getDataFormat());
         if (reader.getFloatType() == FloatType.FLOAT) {
            plyWriter.setFloatType (DataType.FLOAT);
         }
         else if (reader.getFloatType() == FloatType.DOUBLE) {
            plyWriter.setFloatType (DataType.DOUBLE);
         }
      }
   }

   public void setFormat(String fmtStr) {
      myWriter.setFormat (fmtStr);
   }

   public void setFormat(NumberFormat fmt) {
      myWriter.setFormat (fmt);
   }

   public NumberFormat getFormat() {
      return myWriter.getFormat();
   }

   public void writeMesh (MeshBase mesh) throws IOException {
      myWriter.writeMesh (mesh);
   }

   public void close() {
      myWriter.close();
   }

   @Override
   protected void finalize() throws Throwable {
      super.finalize();
      close();
   }

   public static void writeMesh (
      String fileName, MeshBase mesh) throws IOException {
      writeMesh (new File(fileName), mesh);
   }

   public static void writeMesh (File file, MeshBase mesh)
      throws IOException {
      GenericMeshWriter writer = new GenericMeshWriter (file);
      writer.writeMesh (mesh);
   }


}
