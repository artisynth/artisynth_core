/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

/**
 * Interface for basic logging capabilities
 * 
 * @author Antonio Sanchez
 * Creation date: 21 Oct 2012
 *
 */
public abstract class Logger implements Clonable {

   /**
    * Level of detail to log.
    * @author Antonio
    *
    */
   public enum LogLevel {
      /** Everything */ ALL, 
      /** Detailed information (e.g. stack traces) */ TRACE, 
      /** Debugging information */ DEBUG, 
      /** Information messages */ INFO, 
      /** Warnings */ WARN, 
      /** Errors (somewhat recoverable) */ ERROR, 
      /** Fatalities (not recoverable) */ FATAL, 
      /** Nothing */ OFF;
      
      public static int numLevels() {
         return LogLevel.values().length;
      }
      
      /**
       * Find a level based on text input
       * @param level string description of the level
       * @return corresponding level, or null if not found
       */
      public static LogLevel find(String level) {
         for (LogLevel l : values()) {
            if (l.toString().toLowerCase().equals(level.trim().toLowerCase())) {
               return l;
            }
         }
         return null;
      }
   }
   
   // ignore OFF
   public static final int NUM_LEVELS = LogLevel.numLevels()-1;
   
   /**
    * Log information at the `TRACE' level
    * @param obj object to log
    */
   public abstract void trace(Object obj);
   
   /**
    * Log information at the `DEBUG' level
    * @param obj object to log
    */
   public abstract void debug(Object obj);
   
   /**
    * Log information at the `INFO' level
    * @param obj object to log
    */
   public abstract void info(Object obj);
   
   /**
    * Log information at the `WARN' level
    * @param obj object to log
    */
   public abstract void warn(Object obj);
   
   /**
    * Log information at the `ERROR' level
    * @param obj object to log
    */
   public abstract void error(Object obj);
   
   /**
    * Log information at the `FATAL' level
    * @param obj object to log
    */
   public abstract void fatal(Object obj);
   
   /**
    * Print information at the supplied level to the log
    * @param obj object to log
    * @param level desired level
    */
   public abstract void print(Object obj, LogLevel level);
   
   /**
    * Print information at the supplied level to the log, with a trailing newline
    * @param obj object to log
    * @param level desired level
    */
   public abstract void println(Object obj, LogLevel level);
   
   /**
    * Print information to the `INFO' log
    * @param obj object to log
    */
   public abstract void print(Object obj);
   
   /**
    * Print information to the `INFO' log with trailing newline
    * @param obj object to log
    */
   public abstract void println(Object obj);
   
   /**
    * Set the minimum log level to record
    * @param level minimum log level
    */
   public abstract void setLogLevel(LogLevel level);
   
   /**
    * Get the minimum log level that is being recorded
    * @return current log level
    */
   public abstract LogLevel getLogLevel();
   
   private static Logger systemLogger;
   /**
    * @return the system's default logger
    */
   public static Logger getSystemLogger() {
      if (systemLogger != null) {
         return systemLogger;
      }
      return StreamLogger.getDefaultLogger();
   }
   
   public static void setSystemLogger(Logger logger) {
      systemLogger = logger;
   }
   
   @Override
   public Logger clone() throws CloneNotSupportedException {
      return (Logger)super.clone();
   }
   
}
