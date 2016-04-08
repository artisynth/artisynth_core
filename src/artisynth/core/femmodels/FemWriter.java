package artisynth.core.femmodels;

import java.io.IOException;

public interface FemWriter {
   
      public enum DataFormat {
         ASCII, BINARY_LITTLE_ENDIAN, BINARY_BIG_ENDIAN };

      public enum FloatType {
         ASCII, FLOAT, DOUBLE };

         //      public void setFormat(String fmtStr);
         //      public void setFormat(NumberFormat fmt);
         //      public NumberFormat getFormat();
      
      public void writeFem (FemModel3d fem) throws IOException;
      //    public void close();
 
}
