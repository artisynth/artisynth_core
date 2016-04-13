package maspack.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.jogamp.common.nio.Buffers;

public class BufferUtilities {

   /**
    * Frees memory from buffer
    * @param buff
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
      ByteBuffer buff = Buffers.newDirectByteBuffer (size);
      buff.order (ByteOrder.nativeOrder ());
      return buff;
   }

}
