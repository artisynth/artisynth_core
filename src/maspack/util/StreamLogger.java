/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.io.PrintStream;
import java.util.Arrays;

/**
 * Allows customizing of streams for different log levels.
 * Each level corresponds to a PrintStream, so can use
 * files or System.out/System.err
 * 
 * Creation date: 21 Oct 2012, by Antono Sanchez
 *
 */
public class StreamLogger extends Logger {

   protected LogLevel logLevel = LogLevel.TRACE;
   protected PrintStream[] stream = new PrintStream[NUM_LEVELS];

   public StreamLogger() {
      setDefaultStreams();
   }

   /**
    * Sets streams to defaults (warn, err, fatal use System.err, others use System.out)
    */
   public void setDefaultStreams() {
      for (int i=0; i<NUM_LEVELS; i++) {
         stream[i] = System.out;
      }
      stream[LogLevel.WARN.ordinal()] = System.err;
      stream[LogLevel.ERROR.ordinal()] = System.err;
      stream[LogLevel.FATAL.ordinal()] = System.err;

   }

   public void trace(Object obj) {
      if (logLevel.ordinal() <= LogLevel.TRACE.ordinal()) {
         println(obj, LogLevel.TRACE);
      }
   }

   public void debug(Object obj) {
      if (logLevel.ordinal() <= LogLevel.DEBUG.ordinal()) {
         println(obj, LogLevel.DEBUG);
      }
   }

   public void info(Object obj) {
      if (logLevel.ordinal() <= LogLevel.INFO.ordinal()) {
         println(obj, LogLevel.INFO);
      }
   }

   public void warn(Object obj) {
      if (logLevel.ordinal() <= LogLevel.WARN.ordinal()) {
         println(obj, LogLevel.WARN);
      }
   }

   public void error(Object obj) {
      if (logLevel.ordinal() <= LogLevel.ERROR.ordinal()) {
         println(obj, LogLevel.ERROR);
      }
   }

   public void fatal(Object obj) {
      if (logLevel.ordinal() <= LogLevel.FATAL.ordinal()) {
         println(obj, LogLevel.FATAL);
      }
   }

   public void setLogLevel(LogLevel level) {
      if (level != null) {
         logLevel = level;
      }
   }

   public LogLevel getLogLevel() {
      return logLevel;
   }

   public PrintStream getStream(LogLevel level) {
      return (level == null ? null : stream[level.ordinal()]);
   }

   /**
    * Sets the stream for a given log level
    * @param level level to modify
    * @param stream output stream
    */
   public void setStream(LogLevel level, PrintStream stream) {
      if (level != null) {
         this.stream[level.ordinal()] = stream;
      }
   }

   public void print(Object obj) {
      if (logLevel.ordinal() <= LogLevel.INFO.ordinal()) {
         print(LogLevel.INFO);
      }
   }

   public void println(Object obj) {
      info(obj);
   }

   public void print(Object obj, LogLevel level) {
      if (level == null) {
         return;
      } else if (level == LogLevel.ALL) {
         level = LogLevel.INFO;
      }
      
      PrintStream out = stream[level.ordinal()];

      if (out == null) {
         return;
      }
      if (obj==null) {
         out.print("null");  
      } else if (obj instanceof Throwable) {
         Throwable t = (Throwable)obj;
         t.printStackTrace(out);
      } else {
         out.print(obj.toString());
      }
   }

   public void println(Object obj, LogLevel level) {
      if (level == null) {
         return;
      } else if (level == LogLevel.ALL) {
         level = LogLevel.INFO;
      }
      
      PrintStream out = stream[level.ordinal()];
      if (out == null) {
         return;
      }

      if (obj==null) {
         out.println("null");
      } else if (obj instanceof Throwable) {
         Throwable t = (Throwable)obj;
         t.printStackTrace(out);
      } else {
         out.println(obj.toString());
      }
   }
   
   @Override
   public StreamLogger clone() throws CloneNotSupportedException {
      StreamLogger logger = (StreamLogger)super.clone();
      // protected copy of array
      this.stream = Arrays.copyOf(this.stream, this.stream.length);
      return logger;
   }

   private static StreamLogger defaultLogger = new StreamLogger();

   /**
    * @return the static default StreamLogger
    */
   public static StreamLogger getDefaultLogger() {
      return defaultLogger;
   }

   public static void main(String[] args) {

      String msg = "This is a test message";

      StreamLogger log = new StreamLogger();
      System.out.println("Stream logger:");
      log.trace(msg);
      log.info(msg);
      log.info(msg);
      log.warn(msg);
      log.error(msg);
      log.fatal(msg);      
   }

}
