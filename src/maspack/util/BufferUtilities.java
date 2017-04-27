package maspack.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BufferUtilities {

   /**
    * Frees memory from buffer
    * 
    * @param buff buffer for which memory should be freed
    * @return null (for assigning to old buffer)
    */
   public static<T extends Buffer> T freeDirectBuffer(T buff) {
      if(buff == null) { 
         return null;
      }
      
      buff.clear ();
      if (buff.isDirect ()) {
         // XXX maybe do something in the future to clear the buffer
      }
      
      return null;
   }

   public static ByteBuffer newNativeByteBuffer(int size) {
      ByteBuffer buff = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()); 
      return buff;
   }
   
   public static FloatBuffer newNativeFloatBuffer(int size) {
      FloatBuffer buff = ByteBuffer.allocateDirect(size*Float.SIZE).order(
         ByteOrder.nativeOrder()).asFloatBuffer();
      return buff;
   }

}
