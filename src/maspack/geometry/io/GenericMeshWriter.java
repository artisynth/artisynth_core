package maspack.geometry.io;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import maspack.geometry.MeshBase;
import maspack.geometry.io.PlyWriter.DataType;
import maspack.util.ClassFinder;
import maspack.util.NumberFormat;

public class GenericMeshWriter implements MeshWriter {

   private static ArrayList<MeshWriterFactory> factoryList = findFactoryList();
   
   protected MeshWriter myWriter;

   private static ArrayList<MeshWriterFactory> findFactoryList() {
      
      // Find all appropriate factories
      ArrayList<MeshWriterFactory> factoryList = new ArrayList<MeshWriterFactory>();
      try {
         ArrayList<Class<?>> clazzes = 
            ClassFinder.findClasses("", MeshWriterFactory.class);
         
         for (Class<?> clazz : clazzes) {
            // if not abstract or an interface, add the factory to the list
            try {
               if (!Modifier.isAbstract(clazz.getModifiers()) && 
                  !Modifier.isInterface(clazz.getModifiers())) {
                  factoryList.add((MeshWriterFactory)clazz.newInstance());
               }
            } catch (Exception e){}
         }
         
      } catch (Exception e){
      }
      
      factoryList.trimToSize();
      return factoryList;
   }
   
   public GenericMeshWriter (String fileName) throws IOException {
      this (new File(fileName));
   }

   
   public GenericMeshWriter (File file) throws IOException {
      
      myWriter = null;
      
      myWriter = createWriter(file);

      if (myWriter == null) {
         throw new UnsupportedOperationException (
            "File "+file.getName ()+" has unrecognized extension");
      }
   }

   
   public static MeshWriter createWriter(String fileName) throws IOException {
      return createWriter(new File(fileName));
   }
   
   public static MeshWriter createWriter(File file) throws IOException {

      String fileName = file.getName();
      String lfileName = fileName.toLowerCase();
      
      if (lfileName.endsWith (".ply")) {
         return new PlyWriter (file);
      }
      else if (lfileName.endsWith (".obj")) {
        return new WavefrontWriter(file);
      }
      else if (lfileName.endsWith (".off")) {
         return new OffWriter(file);
      }
      else if (lfileName.endsWith (".stl")) {
         return new StlWriter(file);
      }
      else if (lfileName.endsWith (".gts")) {
         return new GtsWriter(file);
      }
      else if (lfileName.endsWith (".xyz")) {
         return new XyzWriter(file);
      }
      else if (lfileName.endsWith (".xyzb")) {
         return new XyzbWriter(file);
      }
      else {
         
         for (MeshWriterFactory factory : factoryList) {
            for (String ext : factory.getFileExtensions()) {
               String lext = ext.toLowerCase();
               if (lfileName.endsWith(lext)) {
                  return factory.newWriter(file);
               }
            }
         }
      }
      
      return null;
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

   public void setWriteNormals (int enable) {
      myWriter.setWriteNormals (enable);
   }

   public int getWriteNormals () {
      return myWriter.getWriteNormals();
   }
   
   public void writeMesh (MeshBase mesh) throws IOException {
      myWriter.writeMesh (mesh);
   }

   public void close() {
      myWriter.close();
   }

   public boolean getZeroIndexed () {
      if (myWriter instanceof WavefrontWriter) {
         return ((WavefrontWriter)myWriter).getZeroIndexed();
      }
      else {
         return false;
      }
   }

   public void setZeroIndexed (boolean enable) { 
      if (myWriter instanceof WavefrontWriter) {
         ((WavefrontWriter)myWriter).setZeroIndexed(enable);
      }
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
