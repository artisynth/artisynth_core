package artisynth.core.modelbase;

import java.lang.reflect.Method;
import java.util.Deque;
import java.io.*;
import artisynth.core.util.*;
import maspack.properties.*;
import maspack.util.*;

/**
 * Extension to PropertyList in which properties can optionally be associated
 * with FieldPointFunction information. This information can then be easily
 * saved or loaded from external files, using {@link #writePropertyFunctions},
 * {@link #scanPropertyFunction}, and {@link #postscanPropertyFunction}.
 */
public class FunctionPropertyList extends PropertyList {

   public FunctionPropertyList (Class<?> hostClass) {
      super (hostClass);
   }

   public FunctionPropertyList (Class<?> hostClass, Class<?> ancestorClass) {
      super (hostClass, ancestorClass);
   }

   public FunctionPropertyDesc addInheritableWithFunction (
      String nameAndMethods, String description, Object defaultValue,
      String options) {
      FunctionPropertyDesc desc = new FunctionPropertyDesc();
      if (PropertyDesc.initialize (
            desc, nameAndMethods, myHostClass, description, defaultValue,
            options, PropertyDesc.INHERITABLE)) {
         desc.initializeFunctionMethods();
         add (desc);
         return desc;
      }
      else {
         return null;
      }
   } 

   public FunctionPropertyDesc addInheritableWithFunction  (
      String nameAndMethods, String description, Object defaultValue) {
      return addInheritableWithFunction (
         nameAndMethods, description, defaultValue, null);
   }
  
   public FunctionPropertyDesc addWithFunction (
      String nameAndMethods, String description, Object defaultValue,
      String options) {
      FunctionPropertyDesc desc = new FunctionPropertyDesc();
      if (PropertyDesc.initialize (
            desc, nameAndMethods, myHostClass, description, defaultValue,
            options, PropertyDesc.REGULAR)) {
         desc.initializeFunctionMethods();
         add (desc);
         return desc;
      }
      else {
         return null;
      }
   }

   public FunctionPropertyDesc addWithFunction (
      String nameAndMethods, String description, Object defaultValue) {
      return addWithFunction (
         nameAndMethods, description, defaultValue, null);
   }

   protected void copy (PropertyList list, Class<?> hostClass) {
      for (int i = 0; i < list.size(); i++) {
         FunctionPropertyDesc desc = new FunctionPropertyDesc();
         desc.set (list.get(i), hostClass);
         add (desc);
      }
   }

   public void writePropertyFunctions (
      PrintWriter pw, HasProperties host,
      NumberFormat fmt, CompositeComponent ancestor) throws IOException {
      for (int i = 0; i < size(); i++) {
         PropertyDesc desc = get(i);
         if (desc instanceof FunctionPropertyDesc) {
            ((FunctionPropertyDesc)desc).maybeWritePropertyFunction(
               pw, host, fmt, ancestor);
         }
      }
   }

   FunctionPropertyDesc getDescForFunctionName (String fxnName) {
      if (fxnName.endsWith ("Function")) {
         PropertyDesc desc = get (fxnName.substring (0, fxnName.length()-8));
         if (desc instanceof FunctionPropertyDesc) {
            return (FunctionPropertyDesc)desc;
         }
      }
      return null;
   }

   public boolean scanPropertyFunction (
      ReaderTokenizer rtok, HasProperties host,
      Deque<ScanToken> tokens) throws IOException {

      if (rtok.ttype == ReaderTokenizer.TT_WORD) {
         FunctionPropertyDesc desc = getDescForFunctionName (rtok.sval);
         if (desc != null) {
            rtok.scanToken ('=');
            desc.scanPropertyFunction (rtok, host, tokens);
            return true;
         }
      }
      return false;
   }

   public boolean postscanPropertyFunction (
      Deque<ScanToken> tokens, HasProperties host,
      CompositeComponent ancestor) throws IOException {

      if (tokens.peek() instanceof StringToken) {
         StringToken tok = (StringToken)tokens.poll();
         FunctionPropertyDesc desc = getDescForFunctionName (tok.value());
         if (desc != null) {
            desc.postscanPropertyFunction (tokens, host, ancestor);
            return true;
         }
      }
      return false;
   }
}
