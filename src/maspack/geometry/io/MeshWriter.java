package maspack.geometry.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import maspack.geometry.MeshBase;
import maspack.util.NumberFormat;

public interface MeshWriter {

   public enum DataFormat {
      ASCII, BINARY_LITTLE_ENDIAN, BINARY_BIG_ENDIAN };

   public enum FloatType {
      ASCII, FLOAT, DOUBLE };

   public void setFormat(String fmtStr);
   public void setFormat(NumberFormat fmt);
   public NumberFormat getFormat();

   public void setWriteNormals (int enable);
   public int getWriteNormals ();
   
   //public void write(File file, MeshBase mesh) throws IOException;
   //public void write(String filename, MeshBase mesh) throws IOException;
   //public void write(OutputStream out, MeshBase mesh) throws IOException;
   public void writeMesh (MeshBase mesh) throws IOException;
   public void close();
   
}
