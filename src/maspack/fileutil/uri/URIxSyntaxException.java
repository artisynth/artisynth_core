/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil.uri;

public class URIxSyntaxException extends IllegalArgumentException {

   private static final long serialVersionUID = -2954757104212747962L;

   public URIxSyntaxException (String msg) {
      super(msg);
   }
   
   public URIxSyntaxException (String msg, Throwable cause) {
      super(msg, cause);
   }
   
   public URIxSyntaxException(String uri, String msg) {
      super(getMsg(msg,uri));
   }
   
   public URIxSyntaxException(String uri, String msg, Throwable cause) {
      super(getMsg(msg,uri), cause);
   } 
   
   private static String getMsg(String msg, String uri){
      return msg + " caused by URI: " + uri;
   }
   
}
