/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import java.lang.reflect.*;

public class InheritablePropertyHandle extends GenericPropertyHandle implements
InheritableProperty {
   Method myGetModeMethod;
   Method mySetModeMethod;

   // ModeObject myMode;

   public InheritablePropertyHandle (HasProperties host, PropertyDesc desc) {
      super (host, desc);
      // if ((myMode = desc.getPropertyMode(host)) == null)
      {
         myGetModeMethod = desc.myGetModeMethod;
         mySetModeMethod = desc.mySetModeMethod;
      }
   }

   public PropertyMode getMode() {
      // if (myMode != null)
      // { return myMode.get();
      // }
      // else
      {
         try {
            return (PropertyMode)myGetModeMethod.invoke (myHost);
         }
         catch (RuntimeException e) {
            throw e;
         }
         catch (Exception e) {
            throw new RuntimeException (e.getMessage());
         }
      }
   }

   public void setMode (PropertyMode mode) {
      // if (myMode != null)
      // { myMode.setAndUpdate (myHost, myDesc.getName(), mode);
      // }
      // else
      {
         try {
            mySetModeMethod.invoke (myHost, mode);
         }
         catch (RuntimeException e) {
            throw e;
         }
         catch (InvocationTargetException e) {
            throw (RuntimeException)e.getCause();
         }
         catch (Exception e) {
            throw new RuntimeException (e.getMessage());
         }
      }
   }
}
