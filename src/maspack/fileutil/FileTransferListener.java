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
    * @param event
    */
   public void transferUpdated(FileTransferEvent event);
   
   /**
    * Fired when the transfer is started
    * @param event
    */
   public void transferStarted(FileTransferEvent event);

   /**
    * Fired when the transfer is complete
    * @param event
    */
   public void transferCompleted(FileTransferEvent event);
}
