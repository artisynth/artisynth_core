package maspack.fileutil;

public class NativeLibraryException extends RuntimeException {
   private static final long serialVersionUID = 1L;

   public NativeLibraryException (String msg) {
      super (msg);
   }
}
