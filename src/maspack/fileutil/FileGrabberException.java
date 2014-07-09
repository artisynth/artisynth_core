package maspack.fileutil;


public class FileGrabberException extends RuntimeException {

   /**
    * 
    */
   private static final long serialVersionUID = 2507562867513637080L;
   
   public FileGrabberException(String msg) {
      super(msg);
   }
   
   public FileGrabberException(String msg, Throwable cause) {
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
