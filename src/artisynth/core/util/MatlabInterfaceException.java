package artisynth.core.util;

import matlabcontrol.*;

public class MatlabInterfaceException extends Exception {
   
   private static final long serialVersionUID = 1L;

   public MatlabInterfaceException (String msg) {
      super (msg);
   }

   public MatlabInterfaceException (MatlabInvocationException e) {
      super (e.getMessage(), e);
   }

   public MatlabInterfaceException (MatlabConnectionException e) {
      super (e.getMessage(), e);
   }

}


   
