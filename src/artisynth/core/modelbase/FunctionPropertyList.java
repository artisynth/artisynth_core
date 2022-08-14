package artisynth.core.modelbase;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;

import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import maspack.properties.HasProperties;
import maspack.properties.PropertyDesc;
import maspack.properties.PropertyList;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Extension to PropertyList in which properties can optionally be associated
 * with Fields. This information can then be easily saved or loaded from
 * external files, using {@link #writePropertyFields}, {@link
 * #scanPropertyField}, and {@link #postscanPropertyField}.
 */
public class FunctionPropertyList extends PropertyList {

   public FunctionPropertyList (Class<?> hostClass) {
      super (hostClass);
   }

   public FunctionPropertyList (Class<?> hostClass, Class<?> ancestorClass) {
      super (hostClass, ancestorClass);
   }

   public FieldPropertyDesc addInheritableWithField (
      String nameAndMethods, String description, Object defaultValue,
      String options) {
      FieldPropertyDesc desc = new FieldPropertyDesc();
      if (PropertyDesc.initialize (
            desc, nameAndMethods, myHostClass, description, defaultValue,
            options, PropertyDesc.INHERITABLE)) {
         desc.initializeFieldMethods();
         add (desc);
         return desc;
      }
      else {
         return null;
      }
   } 

   public FieldPropertyDesc addInheritableWithField  (
      String nameAndMethods, String description, Object defaultValue) {
      return addInheritableWithField (
         nameAndMethods, description, defaultValue, null);
   }
  
   public FieldPropertyDesc addWithField (
      String nameAndMethods, String description, Object defaultValue,
      String options) {
      FieldPropertyDesc desc = new FieldPropertyDesc();
      if (PropertyDesc.initialize (
            desc, nameAndMethods, myHostClass, description, defaultValue,
            options, PropertyDesc.REGULAR)) {
         desc.initializeFieldMethods();
         add (desc);
         return desc;
      }
      else {
         return null;
      }
   }

   public FieldPropertyDesc addWithField (
      String nameAndMethods, String description, Object defaultValue) {
      return addWithField (
         nameAndMethods, description, defaultValue, null);
   }

   protected void copy (PropertyList list, Class<?> hostClass) {
      for (int i = 0; i < list.size(); i++) {
         PropertyDesc oldDesc = list.get(i);
         PropertyDesc newDesc;
         if (oldDesc instanceof FieldPropertyDesc) {
            newDesc = new FieldPropertyDesc();
         }
         else {
            newDesc = new PropertyDesc();            
         }
         newDesc.set (oldDesc, hostClass);
         add (newDesc);
      }
   }

   public void writePropertyFields (
      PrintWriter pw, HasProperties host,
      NumberFormat fmt, CompositeComponent ancestor) throws IOException {
      for (int i = 0; i < size(); i++) {
         PropertyDesc desc = get(i);
         if (desc instanceof FieldPropertyDesc) {
            ((FieldPropertyDesc)desc).maybeWritePropertyField(
               pw, host, fmt, ancestor);
         }
      }
   }

   FieldPropertyDesc getDescForFieldName (String fieldName) {
      if (fieldName.endsWith ("Field")) {
         PropertyDesc desc = get (fieldName.substring (0, fieldName.length()-5));
         if (desc instanceof FieldPropertyDesc) {
            return (FieldPropertyDesc)desc;
         }
      }
      return null;
   }

   public boolean scanPropertyField (
      ReaderTokenizer rtok, HasProperties host,
      Deque<ScanToken> tokens) throws IOException {

      if (rtok.ttype == ReaderTokenizer.TT_WORD) {
         FieldPropertyDesc fieldDesc = getDescForFieldName (rtok.sval);

         if (fieldDesc != null) {
            rtok.scanToken ('=');
            tokens.offer (new StringToken (rtok.sval, rtok.lineno()));
            if (!ScanWriteUtils.scanAndStoreReference (rtok, tokens)) {
               throw new IOException ("Expected reference path, got " + rtok);
            }
            return true;
         }         
      }
      return false;
   }

   public boolean postscanPropertyField (
      Deque<ScanToken> tokens, HasProperties host,
      CompositeComponent ancestor) throws IOException {

      if (tokens.peek() instanceof StringToken) {
         StringToken tok = (StringToken)tokens.poll();
         FieldPropertyDesc fieldDesc = getDescForFieldName (tok.value());
         if (fieldDesc != null) {
            fieldDesc.postscanPropertyField (tokens, host, ancestor);
            return true;
         }
      }
      return false;
   }
}
