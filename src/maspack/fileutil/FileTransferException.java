/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;


public class FileTransferException extends RuntimeException {

   /**
    * 
    */
   private static final long serialVersionUID = 2507562867513637080L;
   
   public FileTransferException(String msg) {
      super(msg);
   }
   
   public FileTransferException(String msg, Throwable cause) {
      super(msg, cause);
   }
   
   public String getRootMessage() {
      
      Throwable root = getRootThrowable(this);
      return root.getMessage();
      
   }
    
   public static Throwable getRootThrowable(Throwable t) {
      
      Throwable root = t;
      while(root.getCause() != null) {
         root = root.getCause();
      }
      
      return root;
      
   }




}
