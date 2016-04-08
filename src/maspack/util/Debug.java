package maspack.util;

import maspack.util.Logger.LogLevel;

/**
 * Class to help with debug printing
 * @author Antonio
 *
 */
public class Debug {
   
   private static boolean enabled = true;
   private static Logger logger = null;
   
   public static Logger getLogger() {
      if (logger == null) {
         logger = new StreamLogger ();  // default
      }
      return logger;
   }
   
   public static void setLogger(Logger log) {
      logger = log;
   }
   
   public static void setLogLevel(LogLevel level) {
      getLogger ().setLogLevel (level);
   }
   
   /**
    * For adding a breakpoint on a line that does nothing
    */
   public static void noop() {};

 
   public static void enable(boolean set) {
      enabled = set;
   }
   
   public static void trace(String str) {
      if (enabled) {
         getLogger ().trace (str);
      }
   }
   
   public static void debug(String str) {
      if (enabled) {
         getLogger ().debug (str);
      }
   }
   
   public static void info(String str) {
      if (enabled) {
         getLogger ().debug (str);
      }
   }
   
   public static void warn(String str) {
      if (enabled) {
         getLogger ().warn (str);
      }
   }
   
   public static void error(String str) {
      if (enabled) {
         getLogger ().equals (str);
      }
   }
   
   public static void fatal(String str) {
      if (enabled) {
         getLogger ().fatal (str);
      }
   }
   
   public static void print(String str, LogLevel level) {
      if (enabled) {
         getLogger ().print (str, level);
      }
   }
   
   public static void println(String str, LogLevel level) {
      if (enabled) {
         getLogger ().println (str, level);
      }
   }
   
}
