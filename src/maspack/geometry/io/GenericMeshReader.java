package maspack.geometry.io;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import maspack.geometry.MeshBase;
import maspack.geometry.io.MeshWriter.DataFormat;
import maspack.geometry.io.MeshWriter.FloatType;
import maspack.util.ClassFinder;

public class GenericMeshReader implements MeshReader {

   private static ArrayList<MeshReaderFactory> factoryList = findFactoryList();
   protected MeshReader myReader;

   DataFormat myDataFormat = null;
   FloatType myFloatType = null;
   int myLastPrecision = -1;

   private static ArrayList<MeshReaderFactory> findFactoryList() {

      // Find all appropriate factories
      ArrayList<MeshReaderFactory> factoryList = new ArrayList<MeshReaderFactory>();
      try {
         ArrayList<Class<?>> clazzes = 
            ClassFinder.findClasses("", MeshReaderFactory.class);

         for (Class<?> clazz : clazzes) {
            // if not abstract or an interface, add the factory to the list
            try {
               if (!Modifier.isAbstract(clazz.getModifiers()) && 
                  !Modifier.isInterface(clazz.getModifiers())) {
                  factoryList.add((MeshReaderFactory)clazz.newInstance());
               }
            } catch (Exception e){}
         }

      } catch (Exception e){
      }

      factoryList.trimToSize();
      return factoryList;
   }

   public GenericMeshReader (String fileName) throws IOException {
      this (new File(fileName));
   }

   public GenericMeshReader (File file) throws IOException {
      String fileName = file.getName();
      String lfileName = fileName.toLowerCase();
      if (lfileName.endsWith (".ply")) {
         myReader = new PlyReader (file);
      }
      else if (lfileName.endsWith (".obj")) {
         myReader = new WavefrontReader(file);
      }
      else if (lfileName.endsWith (".off")) {
         myReader = new OffReader(file);
      }
      else if (lfileName.endsWith (".stl")) {
         myReader = new StlReader(file);
      }
      else if (lfileName.endsWith (".xyzb")) {
         myReader = new XyzbReader(file);
      }
      else {

         for (MeshReaderFactory factory : factoryList) {
            for (String ext : factory.getFileExtensions()) {
               String lext = ext.toLowerCase();
               if (lfileName.endsWith(lext)) {
                  myReader = factory.newReader(file);
                  break;
               }
            }
         }

         if (myReader == null) {
            throw new UnsupportedOperationException (
               "File "+fileName+" has unrecognized extension");
         }
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
