/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.util;

/**
 * Monitors a Process, checking for completion
 * @author Antonio
 *
 */
public class ProcessMonitor implements Runnable {

   private final Process _proc;
   private volatile boolean _complete;
   private volatile int _procExitVal;

   /**
    * Monitor the supplied process
    */
   public ProcessMonitor(Process proc) {
      _proc = proc;
      _complete = false;
      _procExitVal = -1;
   }
   
   /**
    * Whether or not the process has completed
    */
   public boolean isComplete() { 
      return _complete; 
   }
   
   /**
    * Exit value of the process
    */
   public int getExitValue() {
      return _procExitVal;
   }

   /**
    * Runs the process, waiting for completion
    */
   public void run() {
      try {
         _procExitVal = _proc.waitFor();
      } catch (InterruptedException e) {}
      _complete = true;
   }

   /**
    * Creates a process monitor in a new thread, waits for
    * completion
    * @param proc process to monitor
    * @return the running monitor
    */
   public static ProcessMonitor create(Process proc) {
      ProcessMonitor procMon = new ProcessMonitor(proc);
      Thread t = new Thread(procMon);
      t.start();
      return procMon;
   }
   
}