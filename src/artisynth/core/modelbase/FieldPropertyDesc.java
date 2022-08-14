package artisynth.core.modelbase;

import java.lang.reflect.Method;
import java.io.*;
import java.util.Deque;
import artisynth.core.util.ScanToken;
import maspack.properties.*;
import maspack.util.*;

/**
 * Extension to PropertyDesc which can optionally contain information about a
 * FieldComponent associated with the property.
 */
public class FieldPropertyDesc extends PropertyDesc {

   protected Method myGetFieldMethod;
   protected Method mySetFieldMethod;
   protected Class<?> myFieldClass;
   protected String myFieldName;

   private void setGetFieldMethod (String methodName) {
      myGetFieldMethod = locateMethod (methodName);
      if (myFieldClass == null) {
         Class<?> fieldClass = myGetFieldMethod.getReturnType();
         if (!FieldComponent.class.isAssignableFrom (fieldClass)) {
            throw new IllegalStateException (
               "Method "+methodName+" for property "+getName()+
               " does not return an instance of FieldComponent");
         }
         myFieldClass = fieldClass;
      }
      checkReturnType (myGetFieldMethod, myFieldClass);     
   }   

   private void setSetFieldMethod (String methodName) {
      mySetFieldMethod = locateMethod (methodName, myFieldClass);
   }

   protected void initializeFieldMethods () {
      myFieldName = (getName() + "Field");
      setGetFieldMethod ("get"+capitalize(myFieldName));
      setSetFieldMethod ("set"+capitalize(myFieldName));
   }

   public void set (PropertyDesc desc, Class<?> hostClass) {
      super.set (desc, hostClass);
      if (desc instanceof FieldPropertyDesc) {
         FieldPropertyDesc fdesc = (FieldPropertyDesc)desc;
         myFieldClass = fdesc.myFieldClass;
         myFieldName = fdesc.myFieldName;
         if (fdesc.myGetFieldMethod != null) {
            setGetFieldMethod (fdesc.myGetFieldMethod.getName());
         }
         if (fdesc.mySetFieldMethod != null) {
            setSetFieldMethod (fdesc.mySetFieldMethod.getName());
         }
      }
   }

   public FieldComponent getField (HasProperties host) {
      checkHostClass (host);
      try {
         Object obj = myGetFieldMethod.invoke (host);
         if (obj instanceof FieldComponent) {
            return ((FieldComponent)obj);
         }
         else if (obj != null) {
            throw new InternalErrorException (
               "Method "+myGetFieldMethod.getName()+
               " returned "+obj+" instead of an instance of FieldComponent");
         }
      }
      catch (Exception e) {
         methodInvocationError (e, host, myGetFieldMethod);
      }
      return null;
   }

   public void setField (HasProperties host, FieldComponent field) {
      checkHostClass (host);
      try {
         mySetFieldMethod.invoke (host, field);
      }
      catch (Exception e) {
         methodInvocationError (e, host, mySetFieldMethod);
      }
   }

   public void maybeWritePropertyField (
      PrintWriter pw, HasProperties host, NumberFormat fmt,
      CompositeComponent ancestor) throws IOException {
      if (myGetFieldMethod != null) {
         FieldComponent field = getField (host);
         String fieldPath = 
            ComponentUtils.getWritePathName (ancestor, field);
         pw.println (myFieldName + "=" + fieldPath);
      }
   }

   public void postscanPropertyField (
      Deque<ScanToken> tokens, HasProperties host,
      CompositeComponent ancestor) throws IOException {

      FieldComponent field =
         ScanWriteUtils.postscanReference (
            tokens, FieldComponent.class, ancestor);
      setField (host, field);
   }
}
