/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

import org.apache.commons.vfs2.FileObject;

/**
 * Interface for class responsible for monitoring one or more file transfers by
 * examining the destination file. It is associated with a set of
 * {@link FileTransferListener} objects that listen to transfer events fired by
 * this monitor.
 * 
 * @author "Antonio Sanchez" Creation date: 17 Nov 2012
 * 
 */
public interface FileTransferMonitor extends Runnable {

   /**
    * Adds a listener object to respond to transfer events
    * @param listener listener to add
    */
   public void addListener(FileTransferListener listener);

   /**
    * Removes a listener object
    * @param listener listener to remove
    */
   public void removeListener(FileTransferListener listener);

   /**
    * @return the set of all listeners
    */
   public FileTransferListener[] getListeners();

   /**
    * Time in milliseconds between polls when checking for an update
    * in the transfer status.
    *  
    * @return the polling period
    */
   public long getPollSleep();

   /**
    * Sets the polling period
    */
   public void setPollSleep(long period);

   /**
    * Fires the supplied event to all listeners associated with this monitor
    */
   public void fireEvent(FileTransferEvent event);

   /**
    * Convenience function for firing a "start" event for the transfer
    * @param destFile The destination file for the transfer to uniquely
    *    identify the FileTransferMonitorAgent responsible for firing events
    */
   public void fireStartEvent(FileObject destFile);

   /**
    * Convenience function for firing a "complete" event for the transfer
    * @param destFile The destination file for the transfer to uniquely
    *    identify the FileTransferMonitorAgent responsible for firing events
    */
   public void fireCompleteEvent(FileObject destFile);

   /**
    * Informs this monitor object to monitor a file transfer.  If the monitor
    * supports multiple files, adds this to a list of monitored files.  Otherwise,
    * sets the current transfer to monitor.
    * 
    * @param destFile transfer destination
    * @param srcFile transfer source
    */
   public void monitor(FileObject destFile, FileObject srcFile);

   /** 
    * Informs this monitor object to monitor a file transfer.  If the monitor
    * supports multiple files, adds this to a list of monitored files.  Otherwise,
    * sets the current transfer to monitor.
    *  
    * @param destFile transfer destination
    * @param srcFile transfer source
    * @param size the size of the source file (total transfer size).  If &lt; 0, then we determine
    *        the transfer size from srcFile
    * @param displayName the name associated with the transfer (may be different than 
    *    destFile if .part is used)
    */
   public void monitor(FileObject destFile, FileObject srcFile, long size, String displayName);

   /**
    * Informs this monitor object to monitor a file transfer.  If the monitor
    * supports multiple files, adds this to a list of monitored files.  Otherwise,
    * sets the current one.
    *  
    * @param destFile transfer destination
    * @param size Total transfer size
    */
   public void monitor(FileObject destFile, long size);

   /**
    * Discontinues monitoring a file transfer based on the supplied
    * destination file.
    * 
    * @param destFile destination file
    */
   public void release(FileObject destFile);

   /**
    * Starts the monitoring thread
    */
   public void start();

   /**
    * Stops the monitoring thread
    */
   public void stop();

}
