package maspack.geometry.io;

import java.io.*;

import maspack.geometry.*;
import maspack.geometry.io.MeshWriter.DataFormat;
import maspack.geometry.io.MeshWriter.FloatType;

public class GenericMeshReader implements MeshReader {

   protected MeshReader myReader;

   DataFormat myDataFormat = null;
   FloatType myFloatType = null;
   int myLastPrecision = -1;

   public GenericMeshReader (String fileName) throws IOException {
      this (new File(fileName));
   }

   public GenericMeshReader (File file) throws IOException {
      String fileName = file.getName();
      if (fileName.endsWith (".ply")) {
         myReader = new PlyReader (file);
      }
      else if (fileName.endsWith (".obj")) {
         myReader = new WavefrontReader(file);
      }
      else if (fileName.endsWith (".off")) {
         myReader = new OffReader(file);
      }
      else if (fileName.endsWith (".stl")) {
         myReader = new StlReader(file);
      }
      else if (fileName.endsWith (".xyzb")) {
         myReader = new XyzbReader(file);
      }
      else {
         throw new UnsupportedOperationException (
            "File "+fileName+" has unrecognized extension");
      }
   }

   public DataFormat getDataFormat() {
      return myDataFormat;
   }

   public FloatType getFloatType() {
      return myFloatType;
   }

   public int getPrecision() {
      return myLastPrecision;
   }

   public MeshBase readMesh () throws IOException {
      return readMesh ((MeshBase)null);
   }

   public MeshBase readMesh (MeshBase mesh) throws IOException {
      MeshBase newMesh = myReader.readMesh (mesh);
      if (myReader instanceof PlyReader) {
         myDataFormat = ((PlyReader)myReader).getDataFormat();
         myFloatType = ((PlyReader)myReader).getFloatType();
      }
      else if (myReader instanceof XyzbReader) {
         myDataFormat = DataFormat.BINARY_LITTLE_ENDIAN;
         myFloatType = FloatType.FLOAT;
      }
      else {
         myDataFormat = DataFormat.ASCII;
         myFloatType = FloatType.ASCII;
      }
      return newMesh;
   }

   public void close() {
      myReader.close();
   }

   @Override
   protected void finalize() throws Throwable {
      super.finalize();
      close();
   }

   public static MeshBase readMesh (String fileName) throws IOException {
      return readMesh (new File(fileName));
   }

   public static MeshBase readMesh (File file) throws IOException {
      return readMesh (file, null);
   }

   public static MeshBase readMesh (File file, MeshBase mesh) throws IOException {
      GenericMeshReader reader = new GenericMeshReader (file);
      return reader.readMesh (mesh); 
   }

}
