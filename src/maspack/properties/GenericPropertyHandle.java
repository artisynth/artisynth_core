/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import maspack.util.InternalErrorException;
import maspack.util.Range;

public class GenericPropertyHandle implements Property {
   HasProperties myHost;
   Method myGetMethod;
   Method mySetMethod;
   Method myGetRangeMethod;
   protected PropertyDesc myDesc;

   // private boolean returnByReferenceP;
   // private boolean returnByReferenceKnownP = false;

   public GenericPropertyHandle (HasProperties host, PropertyDesc desc) {
      myHost = host;
      myGetMethod = desc.myGetMethod;
      mySetMethod = desc.mySetMethod;
      myGetRangeMethod = desc.myGetRangeMethod;
      myDesc = desc;
   }

   public String getName() {
      return myDesc.myName;
   }

   public HasProperties getHost() {
      return myHost;
   }

   public Object get() {
      try {
         return myGetMethod.invoke (myHost);
      }
      catch (RuntimeException e) {
         System.out.println ("exception invoking getMethod for " + getName());
         throw e;
      }
      catch (InvocationTargetException e) {
         e.printStackTrace();
         throw new RuntimeException (e.getTargetException().getMessage());
      }
      catch (Exception e) {
         e.printStackTrace();
         throw new RuntimeException (e.getMessage());
      }
   }

   public void set (Object obj) {
      if (mySetMethod == null) {
         if (!myDesc.isReadOnly()) {
            throw new InternalErrorException ("null set method for '"
            + getName() + "' in " + myHost.getClass());
         }
         return;
      }
      try {
         mySetMethod.invoke (myHost, obj);
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

   public Range getRange () {
      if (myGetRangeMethod != null) {
         try {
            return (Range)myGetRangeMethod.invoke (myHost);
         }
         catch (RuntimeException e) {
            System.out.println (
               "exception invoking getRangeMethod for " + getName());
            throw e;
         }
         catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException (e.getTargetException().getMessage());
         }
         catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException (e.getMessage());
         }        
      }
      else if (myDesc.typeIsNumeric()) {
         return myDesc.myNumericRange;
      }
      else {
         return null;
      }
   }
   
//   public Object validate (Object value, StringHolder errMsg) {
//      if (myValidateMethod == null) {
//         if (errMsg != null) {
//            errMsg.value = null;
//         }
//         return value;
//      }
//      try {
//         return myValidateMethod.invoke (myHost, value, errMsg);
//      }
//      catch (RuntimeException e) {
//         throw e;
//      }
//      catch (InvocationTargetException e) {
//         throw (RuntimeException)e.getCause();
//      }
//      catch (Exception e) {
//         throw new RuntimeException (e.getMessage());
//      }
//   }

   // public boolean valueReturnedByReference()
   // {
   // if (!returnByReferenceKnownP)
   // { returnByReferenceP = (get() == get());
   // returnByReferenceKnownP = true;
   // }
   // return returnByReferenceP;
   // }

   /**
    * {@inheritDoc}
    */
   public PropertyInfo getInfo() {
      return myDesc;
   }
   
   @Override
   public int hashCode() {
      return Objects.hash (getHost(), getName(), getInfo());
   }
   
   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof Property)) {
         return false;
      }
      
      Property p = (Property)obj;
      HasProperties ohost = p.getHost();
      if (ohost != getHost()) {
         return false;
      }
      
      String name = p.getName();
      if (!name.equals(getName())) {
         return false;
      }
      
      PropertyInfo pdesc = p.getInfo();
      PropertyInfo desc = getInfo();
      
      if (!pdesc.equals(desc)) {
         return false;
      }
      
      return true;
   }
}
