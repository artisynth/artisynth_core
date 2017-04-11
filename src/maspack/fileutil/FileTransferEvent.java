/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

import org.apache.commons.vfs2.FileObject;

/**
 * Transfer event containing information useful for monitoring
 * file transfer progress.
 * 
 * @author "Antonio Sanchez"
 * Creation date: 17 Nov 2012
 *
 */
public class FileTransferEvent  {

   /**
    * Event types
    */
   public enum Type {
      START, UPDATE, COMPLETE
   }
   
   private String myDisplayName;   // name to display for transfered file
   private Type myType;          // event type
   private FileObject mySource;  // source file
   private FileObject myDest;    // transfer destination
   private long eventTime;       // time (ms) event was created
   
   // required for retrieving transfer stats (calling methods on dest
   //     while it is being written to will cause an error to be thrown
   private long mySourceSize;
   private long myDestSize;
   private long myDestModTime;
   
   /**
    * Constructor, sets properties of event and automatically computes
    * the event's time from the system clock.
    * 
    * @param source The source file
    * @param dest  Transfer destination
    * @param type  Type of event
    * @param sourceSize  Size of file to transfer
    * @param destSize  Current size of the destination file
    * @param destTime  Current timestamp on the destination file
    * @param displayName Can be used to display file, for example in case 
    *    an intermediary file is used such as file.txt.part, you may want 
    *    to display file.txt instead.
    */
   public FileTransferEvent (FileObject source, FileObject dest, Type type,
      long sourceSize, long destSize, long destTime, String displayName) {
      
      eventTime = System.currentTimeMillis();   // time of event
      mySource = source;
      myDest = dest;
      myType = type;
      
      mySourceSize = sourceSize;
      myDestSize = destSize;
      myDestModTime = destTime;
      myDisplayName = displayName;
   }
   
   /**
    * Fires the event to all listeners
    */
   public void notify(FileTransferListener listener) {
      
      switch(myType) {
         case START:
            listener.transferStarted(this);
            break;
         case UPDATE:
            listener.transferUpdated(this);
            break;
         case COMPLETE:
            listener.transferCompleted(this);
      }
      
   }
   
   /**
    * @return the source file
    */
   public FileObject getSourceFile() {
      return mySource;
   }
   
   /**
    * @return the transfer destination
    */
   public FileObject getDestinationFile() {
      return myDest;
   }
   
   /**
    * @return the event type
    */
   public Type getType() {
      return myType;
   }
   
   
   /**
    * @return the current size of the transfer
    */
   public long getDestinationSize() {
      return myDestSize;
   }
   
   /**
    * @return the size of the source, total transfer size
    */
   public long getSourceSize() {
      return mySourceSize;
   }
   
   /**
    * @return the time of the event
    */
   public long getTransferTime() {
      return myDestModTime;
   }
   
   /**
    * Fractional progress, -1 if cannot be determined
    * @return size(dest)/size(source)
    */
   public double getProgress() {
      
      if (mySourceSize > 0) {
         return ((double)myDestSize)/mySourceSize;
      }
      return -1;
   }
   
   /**
    * Returns the system time in milliseconds that the event
    * was created.  Useful for determining transfer speed.
    * @return time event was created
    */
   public long getEventTime() {
      return eventTime;
   }
   
   /**
    * Returns the display name.  This can be useful for specifying the final
    * destination when an intermediate file is used.  The transfer destination
    * may be file.ext.part, but the final destination file.ext
    * @return the display name
    */
   public String getDisplayName() {
      return myDisplayName;
   }

}
