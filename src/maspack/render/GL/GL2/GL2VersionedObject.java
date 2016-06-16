package maspack.render.GL.GL2;

/**
 * A GL2 renderable object that additionally stores a finger-print, to be used
 * for detecting whether or not the internal object has been updated to a new
 * version.
 * @author Antonio
 *
 */
public class GL2VersionedObject extends GL2Object {

   private Object fingerPrint;
   
   public GL2VersionedObject (GL2DisplayList displayList, Object fingerPrint) {
      super (displayList);
      this.fingerPrint = fingerPrint;
   }
   
   /**
    * For atomically comparing and exchanging a supplied fingerprint.  Two
    * fingerprints are deemed to be equal according to the "object" equality 
    * sense.
    * @param fingerPrint to compare and set as current
    * @return true if matches previous fingerprint
    */
   public boolean compareExchangeFingerPrint(Object fingerPrint) {
      boolean match = this.fingerPrint.equals (fingerPrint);
      this.fingerPrint = fingerPrint;
      return match;
   }
   
   

}
