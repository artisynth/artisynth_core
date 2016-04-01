package maspack.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.jogamp.common.nio.Buffers;

public class BufferUtilities {

   public static void freeDirectBuffer(Buffer buff) {
      if(buff == null) { 
         return;
      }
      
      buff.clear ();
      if (buff.isDirect ()) {
         // XXX maybe do something in the future to clear the buffer
      }
   }

   public static ByteBuffer newNativeByteBuffer(int size) {
      ByteBuffer buff = Buffers.newDirectByteBuffer (size);
      buff.order (ByteOrder.nativeOrder ());
      return buff;
   }

}
