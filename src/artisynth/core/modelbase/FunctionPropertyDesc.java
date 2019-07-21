package artisynth.core.modelbase;

import java.lang.reflect.Method;
import java.io.*;
import java.util.Deque;
import artisynth.core.util.ScanToken;
import maspack.properties.*;
import maspack.util.*;

/**
 * Extension to PropertyDesc which can optionally contain information about a
 * FieldPointFunction associated with the property.
 */
public class FunctionPropertyDesc extends PropertyDesc {

   protected Method myGetFunctionMethod;
   protected Method mySetFunctionMethod;
   protected Class<?> myFunctionClass;
   protected boolean myFunctionIsScalar;
   protected String myFunctionName;

   private void setGetFunctionMethod (String methodName) {
      myGetFunctionMethod = locateMethod (methodName);
      if (myFunctionClass == null) {
         Class<?> fxnClass = myGetFunctionMethod.getReturnType();
         if (ScalarFieldPointFunction.class.isAssignableFrom (fxnClass)) {
            myFunctionIsScalar = true;
         }
         else if (VectorFieldPointFunction.class.isAssignableFrom (fxnClass)) {
            myFunctionIsScalar = false;
         }
         else {
            throw new IllegalStateException (
               "Method "+methodName+" for property "+getName()+
               " does not return an instance of FieldPointFunction");
         }
         myFunctionClass = fxnClass;
      }
      checkReturnType (myGetFunctionMethod, myFunctionClass);     
   }   

   private void setSetFunctionMethod (String methodName) {
      mySetFunctionMethod = locateMethod (methodName, myFunctionClass);
   }

   protected void initializeFunctionMethods () {
      myFunctionName = (getName() + "Function");
      setGetFunctionMethod ("get"+capitalize(myFunctionName));
      setSetFunctionMethod ("set"+capitalize(myFunctionName));
   }

   public void set (PropertyDesc desc, Class<?> hostClass) {
      super.set (desc, hostClass);
      if (desc instanceof FunctionPropertyDesc) {
         FunctionPropertyDesc fdesc = (FunctionPropertyDesc)desc;
         myFunctionClass = fdesc.myFunctionClass;
         if (fdesc.myGetFunctionMethod != null) {
            setGetFunctionMethod (fdesc.myGetFunctionMethod.getName());
         }
         if (fdesc.mySetFunctionMethod != null) {
            setSetFunctionMethod (fdesc.mySetFunctionMethod.getName());
         }
      }
   }

   public FieldPointFunction getFunction (HasProperties host) {
      checkHostClass (host);
      try {
         Object obj = myGetFunctionMethod.invoke (host);
         if (obj instanceof FieldPointFunction) {
            return ((FieldPointFunction)obj);
         }
         else if (obj != null) {
            throw new InternalErrorException (
               "Method "+myGetFunctionMethod.getName()+
               " returned "+obj+" instead of an instance of FieldPointFunction");
         }
      }
      catch (Exception e) {
         methodInvocationError (e, host, myGetFunctionMethod);
      }
      return null;
   }

   public void setFunction (HasProperties host, FieldPointFunction function) {
      checkHostClass (host);
      try {
         mySetFunctionMethod.invoke (host, function);
      }
      catch (Exception e) {
         methodInvocationError (e, host, mySetFunctionMethod);
      }
   }

   public void maybeWritePropertyFunction (
      PrintWriter pw, HasProperties host, NumberFormat fmt,
      CompositeComponent ancestor) throws IOException {
      if (myGetFunctionMethod != null) {
         FieldPointFunction func = getFunction (host);
         FieldUtils.writeFunctionInfo (
            pw, myFunctionName, func, fmt, ancestor);
      }
   }

   public void scanPropertyFunction (
      ReaderTokenizer rtok, HasProperties host,
      Deque<ScanToken> tokens) throws IOException {

      FieldPointFunction fxn;
      if (myFunctionIsScalar) {
         fxn = FieldUtils.scanScalarFunctionInfo (
            rtok, myFunctionName, tokens);
      }
      else {
         fxn = FieldUtils.scanVectorFunctionInfo (
            rtok, myFunctionName, tokens);
      }
      if (fxn != null) {
         setFunction (host, fxn);
      }
   }

   public void postscanPropertyFunction (
      Deque<ScanToken> tokens, HasProperties host,
      CompositeComponent ancestor) throws IOException {

      FieldPointFunction fxn;
      if (myFunctionIsScalar) {
         fxn = FieldUtils.postscanScalarFunctionInfo (tokens, ancestor);
      }
      else {
         fxn = FieldUtils.postscanVectorFunctionInfo (tokens, ancestor);
      }
      setFunction (host, fxn);
   }
}
