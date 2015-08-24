/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

/**
 * Adapted from org.apache.commons.vfs2.impl.FileTransferMonitor
 */

public class MultiFileTransferMonitor implements Runnable, FileTransferMonitor {

   private static final long DEFAULT_DELAY = 1000; // time in ms between polls
   private static final int DEFAULT_MAX_FILES = 1000; // maximum # files to poll
                                                      // in a period

   // map of all monitor agents with associated destination files
   private final Map<FileObject,FileTransferMonitorAgent> monitorMap =
      new HashMap<FileObject,FileTransferMonitorAgent>(); // map to a file
                                                          // monitor

   private Thread monitorThread;

   private volatile boolean shouldRun = true; // used for inter-thread
   // communication

   private long delay = DEFAULT_DELAY; // delay between polls
   private int checksPerRun = DEFAULT_MAX_FILES; // checks this many files per
                                                 // poll period

   /**
    * A listener object that if set, is notified on file creation, modification
    * and deletion.
    */
   private ArrayList<FileTransferListener> myListeners =
      new ArrayList<FileTransferListener>();

   public MultiFileTransferMonitor () {
   }

   /**
    * Access method to get the current FileListener object notified when there
    * are changes with the files added.
    * 
    * @return The FileListener.
    */
   public FileTransferListener[] getListeners() {
      return myListeners.toArray(new FileTransferListener[myListeners.size()]);
   }

   /**
    * Adds a file to be monitored.
    * 
    * @param dest
    * The destination file to monitor
    * @param source
    * The source, from which transfer size is computed
    */
   public boolean addFile(FileObject dest, FileObject source) {
      return doAddFile(dest, source, -1, null);
   }
   
   private long getFileSize(FileObject file) {
      long size = -1;
      try {
         size = file.getContent().getSize();
      } catch (FileSystemException e) {
         size = -1;
      }      
      return size;
   }

   /**
    * Adds a file to be monitored.
    * 
    * @param dest
    * The destination file to monitor
    * @param size
    * The total transfer size (size of source)
    */
   public boolean addFile(FileObject dest, long size) {
      return doAddFile(dest, null, size, null);
   }

   /**
    * Adds a file to be monitored.
    * 
    * @param dest
    * The destination file to monitor
    * @param source
    * The source
    * @param size
    * The total transfer size (size of source)
    * @param displayName
    * The name of the file associated with this file transfer (may be different from destination)
    * 
    */
   public boolean addFile(FileObject dest, FileObject source, long size, String displayName) {
      return doAddFile(dest, source, size, displayName);
   }

   /**
    * Adds a file to be monitored.
    * 
    * @param dest
    * The FileObject to add.
    * @param size
    * The total transfer size
    * @param displayName
    * name associated with file transfer
    */
   private boolean doAddFile(FileObject dest, FileObject source, long size, String displayName) {

      // determine some defaults
      if (size <0) {
         size = getFileSize(source);
      }
      
      synchronized (this.monitorMap) {
         if (this.monitorMap.get(dest) == null) {
            this.monitorMap.put(dest, new FileTransferMonitorAgent(
               this, dest, source, size, displayName));
         }
      }
      return true;
   }

   /**
    * Removes a file from being monitored.
    * 
    * @param file
    * The FileObject to remove from monitoring.
    */
   public void release(FileObject file) {
      synchronized (this.monitorMap) {
         if (this.monitorMap.get(file) != null) {
            this.monitorMap.remove(file);
         }
      }
   }

   /**
    * Get the delay between runs.
    * 
    * @return The delay period.
    */
   public long getPollSleep() {
      return delay;
   }

   /**
    * Set the delay between runs.
    * 
    * @param delay
    * The delay period.
    */
   public void setPollSleep(long delay) {
      if (delay > 0) {
         this.delay = delay;
      }
      else {
         this.delay = DEFAULT_DELAY;
      }
   }

   /**
    * get the number of files to check per run.
    * 
    * @return The number of files to check per iteration.
    */
   public int getChecksPerRun() {
      return checksPerRun;
   }

   /**
    * set the number of files to check per run. a additional delay will be added
    * if there are more files to check
    * 
    * @param checksPerRun
    * a value less than 1 will disable this feature
    */
   public void setChecksPerRun(int checksPerRun) {
      this.checksPerRun = checksPerRun;
   }

   /**
    * Starts monitoring the files that have been added.
    */
   public void start() {
      if (this.monitorThread == null
         || monitorThread.getState() == State.TERMINATED) {
         this.monitorThread = new Thread(this);
         this.monitorThread.setDaemon(true);
         this.monitorThread.setPriority(Thread.MIN_PRIORITY);
      }

      try {
         this.monitorThread.start();
      } catch (IllegalThreadStateException e) {
         this.monitorThread = new Thread(this);
         this.monitorThread.setDaemon(true);
         this.monitorThread.setPriority(Thread.MIN_PRIORITY);
      }
   }

   /**
    * Stops monitoring the files that have been added.
    */
   public void stop() {
      this.shouldRun = false;
   }

   /**
    * Asks the agent for each file being monitored to check its file for
    * changes.
    */
   public void run() {

      while (!monitorThread.isInterrupted() && this.shouldRun) {
         // while (!this.deleteStack.empty()) {
         // this.removeFile(this.deleteStack.pop());
         // }

         // For each entry in the map
         FileObject[] files = new FileObject[0];
         synchronized (this.monitorMap) {
            files = this.monitorMap.keySet().toArray(files);
         }

         for (int i = 0; i < files.length; i++) {
            FileObject file = files[i];
            FileTransferMonitorAgent agent;
            synchronized (this.monitorMap) {
               agent = this.monitorMap.get(file);
            }

            if (agent != null) {
               agent.check();
            }

            if (getChecksPerRun() > 0) {
               if (i % getChecksPerRun() == 0) {
                  try {
                     Thread.sleep(getPollSleep());
                  } catch (InterruptedException e) {
                     // Woke up.
                  }
               }
            }

            if (monitorThread.isInterrupted() || !this.shouldRun) {
               this.shouldRun = true;
               return;
            }
         }

         try {
            Thread.sleep(getPollSleep());
         } catch (InterruptedException e) {
            continue;
         }
      }

      this.shouldRun = true;
   }

   /**
    * Fires an event to all listeners
    */
   public void fireEvent(FileTransferEvent event) {
      for (FileTransferListener listener : myListeners) {
         event.notify(listener);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void fireStartEvent(FileObject dest) {
      FileTransferMonitorAgent fma = monitorMap.get(dest);
      if (fma != null) {
         fma.fireStart();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void fireCompleteEvent(FileObject dest) {
      FileTransferMonitorAgent fma = monitorMap.get(dest);
      if (fma != null) {
         fma.fireComplete();
      }
   }

   /**
    * @see #addFile(FileObject, FileObject)
    */
   public void monitor(FileObject destFile, FileObject srcFile) {
      doAddFile(destFile, srcFile, -1, null);
   }

   /**
    * @see #addFile(FileObject, FileObject, long, String)
    */
   public void monitor(FileObject destFile, FileObject srcFile, long size) {
      doAddFile(destFile, srcFile, size, null);
   }

   /**
    * @see #addFile(FileObject, long)
    */
   public void monitor(FileObject destFile, long size) {
      doAddFile(destFile, null, size, null);
   }
   
   /**
    * {@inheritDoc}
    */
   public void monitor(FileObject destFile, FileObject srcFile, long size,
      String displayName) {
     doAddFile(destFile, srcFile, size, displayName);
      
   }

   /**
    * {@inheritDoc}
    */
   public void addListener(FileTransferListener listener) {
      synchronized (myListeners) {
         if (!myListeners.contains(listener)) {
            myListeners.add(listener);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void removeListener(FileTransferListener listener) {
      synchronized (myListeners) {
         if (myListeners.contains(listener)) {
            myListeners.remove(listener);
         }
      }
   }

}
