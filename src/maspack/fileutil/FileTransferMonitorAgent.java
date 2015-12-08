/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

/**
 * Monitors a particular file transfer
 * @author "Antonio Sanchez"
 * Creation date: 17 Nov 2012
 * 
 * Modified from org.apache.commons.vfs2.impl.DefaultFileMonitor.FileMonitorAgent
 * 
 * Detects changes to a single file, triggering events when the destination file
 * has either changed size or modification time.  This is always associated with
 * a single FileTransferMonitor.
 * 
 */
public class FileTransferMonitorAgent {
   private final FileObject dest;      // destination file
   private final FileObject source;    // source file
   private final String displayName;   // display name
   private final FileTransferMonitor fm;  // monitor associated with this agent

   private boolean started = false;
   private boolean completed = false;
   
   private boolean exists;       // if on last check the destination file exists
   private long timestamp;       // modification timestamp on destination at last check
   private long transfersize;    // destination filesize at last check
   private long sourceSize;    // total transfer size

   /**
    * Main constructor
    * @param fm associated FileTransferMonitor
    * @param dest transfer destination
    * @param source transfer source
    * @param sourceSize total size of transfer (size of source).  Provided for cases when source size
    *        cannot be determined due to the file system type
    * @param displayName the file name associated with the transfer (may be different from dest.fileName)
    */
   public FileTransferMonitorAgent (FileTransferMonitor fm, FileObject dest, FileObject source, long sourceSize, String displayName) {
      this.fm = fm;
      this.dest = dest;
      this.sourceSize = sourceSize;
      this.source = source;
      this.displayName = displayName;
      
      this.refresh();

      try {
         this.exists = this.dest.exists();
      } catch (FileSystemException fse) {
         this.exists = false;
         this.timestamp = -1;
      }

      if (this.exists) {
         try  {
            // this fails if file is being written to
            this.timestamp = this.dest.getContent().getLastModifiedTime();
         } catch (FileSystemException fse) {
            this.timestamp = -1;
         }
      }
   }

   /**
    * @param fm FileTransferMonitor associated with this agent
    * @param dest Transfer destination
    * @param source Transfer source
    * @param displayName Name associated with file transfer
    */
   public FileTransferMonitorAgent (FileTransferMonitor fm, FileObject dest, FileObject source, String displayName) {
      this(fm,dest,source, -1, displayName);
      try {
         sourceSize = source.getContent().getSize();
      } catch (FileSystemException e) {
         sourceSize = -1;
      }
   }
   
   /**
    * @param fm Associated FileTransferMonitor
    * @param dest transfer destination
    * @param sourceSize transfer size
    * @param displayName name associated with transfer
    */
   public FileTransferMonitorAgent (FileTransferMonitor fm, FileObject dest, long sourceSize, String displayName) {
      this(fm,dest,null,sourceSize, displayName);
   }
   
   /**
    * @param fm Associated FileTransferMonitor
    * @param dest transfer destination
    * @param sourceSize transfer size
    */
   public FileTransferMonitorAgent (FileTransferMonitor fm, FileObject dest, long sourceSize) {
      this(fm,dest,null,sourceSize, null);
   }
   
   /**
    * @param fm Associated FileTransferMonitor
    * @param dest transfer destination
    */
   public FileTransferMonitorAgent (FileTransferMonitor fm, FileObject dest) {
      this(fm,dest,null,null);
   }
   
   /**
    * Clear the cache and re-request the file object
    */
   private void refresh() {
      try {
         dest.refresh();
      } catch (FileSystemException fse)  { }
   }

   /**
    * Refreshes the destination file, checking if the file has changed and fires
    * an event if either the size or modification time is different.
    */
   public void check()  {
      this.refresh();

      boolean destExists = false;
      long destTimestamp = -1;
      long destTransfersize = 0;
      boolean destIsDirectory = false;

      try  {         
         destExists = this.dest.exists();
         if (destExists) {
            destTimestamp = this.dest.getContent().getLastModifiedTime();
            destIsDirectory = this.dest.getType().hasChildren();
            if (this.dest.isFile()) {
               destTransfersize = this.dest.getContent().getSize();
            } else {
               destTransfersize = -1;
            }
         }
      } catch (FileSystemException fse) {
         fse.printStackTrace();
      }

      //         // Check the timestamp to see if it has been modified
      //         System.out.println("Source: " + this.source.getURL());
      //         System.out.println("Destination: " + this.source.getURL());
      //         System.out.println("Previous time: " + timestamp + ",\t New time: " + destTimestamp);
      //         System.out.println("Previous size: " + transfersize + ",\t New size: " + destTransfersize);

      if (this.timestamp != destTimestamp || this.transfersize != destTransfersize)  {
         // Fire change event
         // Don't fire if it's a folder
         if (!destIsDirectory) {
            if (!started) {
               fireStart();
            } else {
               fireUpdate();
            }
            
            // check if transfer is complete
            if (!completed) {
               if ((destTransfersize == sourceSize) && (sourceSize >= 0)) {
                  fireComplete();
               }
            }
         }
      }
      
      this.exists = destExists;
      this.timestamp = destTimestamp;
      this.transfersize = destTransfersize;

   }

   /**
    * Fires a "start" event for this transfer to all listeners in the associated 
    * FileTransferMonitor.
    */
   public void fireStart() {
      
      // prevent firing more than once
      if (started) {
         return;
      }
      exists = false;
      timestamp = -1;
      transfersize = 0;
      try  {         
         dest.refresh();
         exists = this.dest.exists();
         if (exists) {
            timestamp = this.dest.getContent().getLastModifiedTime();
            transfersize = this.dest.getContent().getSize();
         }
      } catch (FileSystemException fse) {}
      
      FileTransferEvent event = new FileTransferEvent(source, dest, 
         FileTransferEvent.Type.START, sourceSize, 0, timestamp, displayName);
      fm.fireEvent(event);
      
      started = true; // assume we've started transfer
   }

   /**
    * Fires on update event
    */
   public void fireUpdate() {
      FileTransferEvent event = new FileTransferEvent(source, dest, 
         FileTransferEvent.Type.UPDATE, sourceSize, transfersize, timestamp, displayName);
      fm.fireEvent(event);
   }

   /**
    * Fires a transfer complete event
    */
   public void fireComplete() {
      
      // prevent firing more than once
      if (completed) {
         return;
      }
      
      exists = false;
      timestamp = -1;
      transfersize = 0;
      try  {
         dest.refresh();
         exists = this.dest.exists();
         if (exists) {
            timestamp = this.dest.getContent().getLastModifiedTime();
            transfersize = this.dest.getContent().getSize();
         }
      } catch (FileSystemException fse) {}
      
      FileTransferEvent event = new FileTransferEvent(source, dest, 
         FileTransferEvent.Type.COMPLETE, sourceSize, transfersize, timestamp, displayName);
      fm.fireEvent(event);
      
      completed = true;
   }

   /**
    * @return the transfer destination file
    */
   public FileObject getDestinationFile() {
      return dest;
   }

   /**
    * @return the transfer source file
    */
   public FileObject getSourceFile() {
      return source;
   }

   /**
    * @return the associated FileTransferMonitor
    */
   public FileTransferMonitor getMonitor() {
      return fm;
   }

   /**
    * @return the timestamp of the destination file at last check
    */
   public long getLastTimestamp() {
      return timestamp;
   }

   /**
    * @return the size of the destination file at last check
    */
   public long getTransferedSize() {
      return transfersize;
   }

   /**
    * @return the total size of the source file
    */
   public long getSourceSize() {
      return sourceSize;
   }

}
