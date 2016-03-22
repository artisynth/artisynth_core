package maspack.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.jogamp.common.nio.Buffers;

import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;

public class BufferUtilities {

   public static void cleanDirectBuffer(Buffer buff) {
      if(buff == null) { 
         return;
      }

      if (buff instanceof DirectBuffer) {
         Cleaner cleaner = ((DirectBuffer) buff).cleaner();
         if (cleaner != null) cleaner.clean();   
      }
   }

   public static ByteBuffer newNativeByteBuffer(int size) {
      ByteBuffer buff = Buffers.newDirectByteBuffer (size);
      buff.order (ByteOrder.nativeOrder ());
      return buff;
   }

}
