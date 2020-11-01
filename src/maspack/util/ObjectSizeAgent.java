package maspack.util;

import java.lang.instrument.*;

public class ObjectSizeAgent {
   private static volatile Instrumentation globalInstrumentation;
 
   public static void premain (
      final String agentArgs, final Instrumentation inst) {
      globalInstrumentation = inst;
   }
 
   public static long getObjectSize(final Object object) {
      if (globalInstrumentation == null) {
         throw new IllegalStateException("Agent not initialized.");
      }
      return globalInstrumentation.getObjectSize(object);
   }
}
