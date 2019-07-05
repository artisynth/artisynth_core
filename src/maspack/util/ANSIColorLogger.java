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
 * Allows different colours for log levels.  Works on *nix
 * terminals, and on eclipse with the ANSI Console plugin 
 * http://www.mihai-nita.net/eclipse
 * 
 * @author Antonio Sanchez
 * Creation date: 21 Oct 2012
 *
 */
public class ANSIColorLogger extends StreamLogger {
   
   public enum ANSIColor {
      NONE ("", ""),
      BLACK ("\u001B[30m", "\u001B[0m"),
      BRIGHT_BLACK ("\u001B[30;1m", "\u001B[0m"),
      BLUE ("\u001B[34m", "\u001B[0m"),
      BRIGHT_BLUE ("\u001B[34;1m", "\u001B[0m"),
      GREEN ("\u001B[32m", "\u001B[0m"),
      BRIGHT_GREEN ("\u001B[32;1m", "\u001B[0m"),
      CYAN ("\u001B[36m", "\u001B[0m"),
      BRIGHT_CYAN ("\u001B[36;1m", "\u001B[0m"),
      RED ("\u001B[31m", "\u001B[0m"),
      BRIGHT_RED ("\u001B[31;1m", "\u001B[0m"),
      MAGENTA ("\u001B[35m", "\u001B[0m"),
      BRIGHT_MAGENTA ("\u001B[35;1m", "\u001B[0m"),
      YELLOW ("\u001B[33m", "\u001B[0m"),
      BRIGHT_YELLOW ("\u001B[33;1m", "\u001B[0m"),
      WHITE ("\u001B[37m", "\u001B[0m"),
      BRIGHT_WHITE ("\u001B[33;1m", "\u001B[0m");

      private String sVal;
      private String eVal;
      private ANSIColor (String s, String e) {
         sVal = s;
         eVal = e;
      }
      public String start() {
         return sVal;
      }
      public String end() {
         return eVal;
      }
   }

   // still use bright to stand out
   public static ANSIColor[] DEFAULT_COLOURS_DARK =
   { ANSIColor.GREEN, ANSIColor.GREEN, ANSIColor.BRIGHT_BLUE, ANSIColor.NONE,
    ANSIColor.YELLOW, ANSIColor.BRIGHT_RED, ANSIColor.RED };

   public static ANSIColor[] DEFAULT_COLOURS_BRIGHT =
   {  ANSIColor.BRIGHT_GREEN, ANSIColor.BRIGHT_GREEN, ANSIColor.BRIGHT_BLUE,
    ANSIColor.NONE, ANSIColor.BRIGHT_YELLOW,
    ANSIColor.BRIGHT_RED, ANSIColor.RED };

   private ANSIColor[] colour = new ANSIColor[NUM_LEVELS];
   
   public ANSIColorLogger () {
      super();
      setDefaultDarkColours();
   }
   
   public void setDefaultBrightColours() {
      for (int i=0; i<NUM_LEVELS; i++) {
         colour[i] = DEFAULT_COLOURS_BRIGHT[i];
      }
   }

   public void setDefaultDarkColours() {
      for (int i=0; i<NUM_LEVELS; i++) {
         colour[i] = DEFAULT_COLOURS_DARK[i];
      }
   }
   
   public void removeColours() {
      for (int i=0; i<NUM_LEVELS; i++) {
         colour[i] = ANSIColor.NONE;
      }
   }
   
   public void setColour(LogLevel level, ANSIColor colour) {
      if (level != null) {
         this.colour[level.ordinal()] = colour;         
      }
   }
   
   public ANSIColor getColor(LogLevel level) {
      if (level != null) {
         return colour[level.ordinal()];
      }
      return null;
   }

   @Override
   public void print(Object obj, LogLevel level) {
      if (level == null) {
         return;
      }
      
      int idx = level.ordinal();
      PrintStream out = stream[idx];
      if (out == null) {
         return;
      }
      
      out.print(colour[idx].start());
      if (obj==null) {
         out.print("null");  
      } else {
         out.print(obj.toString());
      }
      out.print(colour[idx].end());
   }
   
   @Override
   public void println(Object obj, LogLevel level) {
      if (level == null) {
         return;
      }
      
      int idx = level.ordinal();
      PrintStream out = stream[idx];
      if (out == null) {
         return;
      }
      
      out.print(colour[idx].start());
      if (obj==null) {
         out.print("null");  
      } else {
         out.print(obj.toString());
      }
      out.println(colour[idx].end());
   }
   
   @Override
   public ANSIColorLogger clone() throws CloneNotSupportedException {
      ANSIColorLogger logger = (ANSIColorLogger)super.clone();
      this.colour = Arrays.copyOf(this.colour, this.colour.length);
      return logger;
   }
   
   private static ANSIColorLogger defaultLogger = new ANSIColorLogger();
   /**
    * @return the static default ANSIColorLogger
    */
   public static ANSIColorLogger getDefaultLogger() {
      return defaultLogger;
   }

   public static void main(String[] args) {
      
      String msg = "This is a test message";
      
      Logger log = new ANSIColorLogger();
      System.out.println("Colour logger:");
      log.trace(msg);
      log.debug(msg);
      log.info(msg);
      log.warn(msg);
      log.error(msg);
      log.fatal(msg);
      
      log = new StreamLogger();
      System.out.println("Stream logger:");
      log.trace(msg);
      log.info(msg);
      log.info(msg);
      log.warn(msg);
      log.error(msg);
      log.fatal(msg);      
      
   }
}
