/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

public class DefaultConsoleFileTransferListener implements FileTransferListener {

   long startTime = 0;
   private final String[] units = {" B", "KB", "MB", "GB"};
   private static final String CARRIAGE = "\r";
   private int lastLength = 0;
   
   /**
    * 
    */
   public void transferUpdated(FileTransferEvent event) {

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
         eta = (event.getEventTime() - startTime)/1000/pp; // estimated total time
         eta = eta*(1-pp);  // estimated time remaining
         int min = (int)Math.floor(eta/60);
         int sec = (int)Math.round(eta-min*60);
         
         etaStr = String.format("% 4d:%02d", min, sec);
      }
      
      double sizeDiff = (event.getDestinationSize());
      double timeDiff = (event.getEventTime() - startTime)/1000;
      
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
      
      String out = String.format("%s     %5.1f/%5.1f%s (%s)     %5.1f%s/s    %s ETA",  
         event.getDisplayName(), soFar, size, units[sizeIdx], ppStr, speed, units[speedIdx], etaStr);
      
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
   }

   public void transferCompleted(FileTransferEvent event) {
      transferUpdated(event); // print final update
      System.out.print("\n");
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
