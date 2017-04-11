/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

/**
 * Interface for class that responds to transfer events.  Three
 * events are supported: start, update, complete.  Details of the
 * transfer status including progress can be determined from the
 * supplied {@link FileTransferEvent}
 * 
 * @author "Antonio Sanchez"
 * Creation date: 17 Nov 2012
 *
 */
public interface FileTransferListener {

   /**
    * Fired when destination file is changed
    */
   public void transferUpdated(FileTransferEvent event);
   
   /**
    * Fired when the transfer is started
    */
   public void transferStarted(FileTransferEvent event);

   /**
    * Fired when the transfer is complete
    */
   public void transferCompleted(FileTransferEvent event);
}
