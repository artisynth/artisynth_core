/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

public interface Logger {

   /**
    * Interface for basic logging capabilities
    * 
    * @author Antonio Sanchez
    * Creation date: 21 Oct 2012
    *
    */
   public enum LogLevel {
      TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF;
      
      public static int numLevels() {
         return LogLevel.values().length;
      }
   }
   
   // ignore OFF
   public static final int NUM_LEVELS = LogLevel.numLevels()-1;
   
   public void trace(Object obj);
   public void debug(Object obj);
   public void info(Object obj);
   public void warn(Object obj);
   public void error(Object obj);
   public void fatal(Object obj);
   
   public void print(Object obj, LogLevel level);
   public void println(Object obj, LogLevel level);
   public void print(Object obj);
   public void println(Object obj);
   
   public void setLogLevel(LogLevel level);
   public void setLogLevel(int level);
   public LogLevel getLogLevel();
   
}
