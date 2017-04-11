package maspack.json;

public interface JSONHandler {

   /**
    * Start of an object
    */
   public void beginObject();
   
   /**
    * End of an object
    */
   public void yieldObject();
   
   /**
    * Key has been defined (likely through yieldString()).
    */
   public void yieldKey();
   
   /**
    * Start of an array
    */
   public void beginArray();
   
   /**
    * End of an array
    */
   public void yieldArray();
   
   /**
    * End of an object member or array element, marked by a comma
    */
   public void yieldSeparator();
   
   /**
    * Handle a String value.  Note that this String might be
    * a key in an object.  It is the handler's responsibility
    * to detect this.
    * @param str string
    */
   public void yieldString(String str);
   
   /**
    * Handle a numeric value
    * @param v number
    */
   public void yieldNumber(double v);
   
   /**
    * Handle a 'true' boolean
    */
   public void yieldTrue();
   
   /**
    * Handle a 'false' boolean
    */
   public void yieldFalse();
   
   /**
    * Handle a 'null' value
    */
   public void yieldNull();
   
   /**
    * Handle garbage.  Garbage is content that doesn't satisfy the JSON spec.  
    * The most common sources of garbage are comments and String values that
    * are not quoted.
    * 
    * <p>
    * To facilitate handling of these two cases, we parse each 'garbage' term
    * as follows:
    * <ul>
    * <li>If text starts with a // or #, the content is parsed until the end of the line</li>
    * <li>If text starts with a /* , the content is parsed until the closing *\/</li>
    * <li>Otherwise, the content is parsed until the next whitespace, colon, or comma</li>
    * </ul>
    * 
    * @param str garbage string
    */
   public void yieldGarbage(String str);
   
   /**
    * Check whether a JSON element is available
    * @return true if an element can be popped
    */
   public boolean hasElement();
   
   /**
    * Returns and removes the next successfully created JSON element, throws
    * an exception if no such element exists
    * @return next JSON element
    */
   public Object pop();

   /**
    * Clear any stored info, i.e. starting from scratch
    */
   public void clear();
   
}
