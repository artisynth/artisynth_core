/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

import java.io.Console;
import java.lang.reflect.Method;

/**
 * Prints the progress of file transfers to the console. When the output is a
 * terminal, progress is shown on a single line that is rewritten in place
 * using carriage returns. Otherwise (e.g., when the output is an IDE console
 * or is captured by a build tool such as Ant), carriage returns are not
 * interpreted, and would cause all the updates to be appended to one long
 * line; progress is then printed on separate lines instead, at most once
 * every {@link #PRINT_INTERVAL_MSEC} milliseconds.
 */
public class DefaultConsoleFileTransferListener implements FileTransferListener {

   long startTime = 0;
   private final String[] units = {" B", "KB", "MB", "GB"};
   private static final String CARRIAGE = "\r";
   private int lastLength = 0;

   /**
    * Minimum interval between progress lines when the output is not a
    * terminal.
    */
   public static final long PRINT_INTERVAL_MSEC = 2000;

   private static Boolean myOutputIsTerminal = null;
   private long myLastPrintTime = 0;

   /**
    * Returns true if standard output appears to be a terminal. Before Java
    * 22, {@code System.console()} is non-null only for an interactive
    * terminal. From Java 22, it may be non-null even when the output is
    * redirected, and {@code Console.isTerminal()} (called by reflection, so
    * that this compiles for Java 8) must be used instead.
    */
   private static boolean outputIsTerminal() {
      if (myOutputIsTerminal == null) {
         boolean isTerminal = false;
         Console console = System.console();
         if (console != null) {
            isTerminal = true;
            try {
               Method method = Console.class.getMethod ("isTerminal");
               isTerminal = (Boolean)method.invoke (console);
            }
            catch (Exception e) {
               // method not available before Java 22
            }
         }
         myOutputIsTerminal = isTerminal;
      }
      return myOutputIsTerminal;
   }
   
   /**
    * 
    */
   public void transferUpdated(FileTransferEvent event) {
      transferUpdated (event, /*force=*/false);
   }

   private void transferUpdated(FileTransferEvent event, boolean force) {
      boolean terminal = outputIsTerminal();
      if (!terminal && !force &&
          event.getEventTime() - myLastPrintTime < PRINT_INTERVAL_MSEC) {
         return;
      }

      double pp = event.getProgress();
      double eta = -1;
      
      String ppStr = "??";
      String etaStr = "  ??:??";
      if (pp >= 0) {
         ppStr = String.format("% 6.1f%%", pp*100);
         if (ppStr.startsWith(" ")) {
            ppStr = ppStr.substring(1);   // trim off first space
         }
      }
      if (pp > 0) {
         eta = (event.getEventTime() - startTime)/1000.0/pp; // estimated total time
         eta = eta*(1-pp);  // estimated time remaining
         int min = (int)Math.floor(eta/60);
         int sec = (int)Math.round(eta-min*60);
         
         etaStr = String.format("% 4d:%02d", min, sec);
      }
      
      double sizeDiff = (event.getDestinationSize());
      double timeDiff = (event.getEventTime() - startTime)/1000.0;
      
      int speedIdx = 0;
      double speedDiv = 1; 
      for (int i=0; i<units.length; i++) {
         if (sizeDiff/timeDiff/speedDiv < 1000) {
            speedIdx = i;
            break;
         }
         speedIdx = i;
         speedDiv = speedDiv * 1024;
      }
      if (timeDiff == 0) {
         speedIdx = 0;
      }
      
      long sizeDiv = 1;
      int sizeIdx = 0;
      for (int i=0; i<units.length; i++) {
         if (event.getSourceSize()/sizeDiv < 1000) {
            sizeIdx = i;
            break;
         }
         sizeIdx = i;
         sizeDiv = sizeDiv * 1024;
      }     
      
      double size = ((double)event.getSourceSize())/sizeDiv;
      double soFar = ((double)event.getDestinationSize())/sizeDiv;
      double speed = sizeDiff/timeDiff/speedDiv;
      if (timeDiff == 0) {
         speed = 0;
      }
      
      String out = String.format("%s  %5.1f/%5.1f%s (%s)  %5.1f%s/s %s ETA",  
         event.getDisplayName(), soFar, size, units[sizeIdx], ppStr, speed, units[speedIdx], etaStr);
      
      myLastPrintTime = event.getEventTime();
      if (!terminal) {
         System.out.println (out);
         return;
      }
      int length = out.length();      
      System.out.print(CARRIAGE + out);
      System.out.flush();
      if (length < lastLength) {
         System.out.print(repChar(' ',lastLength-length));
      }
      lastLength = length;
   }

   public void transferStarted(FileTransferEvent event) {
      String out = "Started downloading " + event.getDisplayName() + "..."; 
      System.out.println(out);
      
      startTime = event.getEventTime();
      myLastPrintTime = startTime;
      lastLength = 0;
   }

   public void transferCompleted(FileTransferEvent event) {
      if (outputIsTerminal()) {
         transferUpdated(event, /*force=*/true); // print final update
         System.out.print("\n");
      }
      String out = "Download of " + event.getDisplayName() + " complete";
      System.out.println(out);
   }
   
   private String repChar(char c, int len) {
      StringBuilder str = new StringBuilder(len);
      for (int i=0; i<len; i++) {
         str.append(c);
      }
      return str.toString();
   }
}
