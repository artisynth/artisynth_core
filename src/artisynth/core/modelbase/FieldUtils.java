package artisynth.core.modelbase;

import java.util.Deque;
import java.util.ArrayDeque;
import java.lang.reflect.Constructor;
import java.io.PrintWriter;
import java.io.IOException;
import artisynth.core.util.*;
import maspack.util.*;

public class FieldUtils {

   public static abstract class FieldFunction<T> 
      implements FieldPointFunction<T>  {
      public abstract Field<T> getField();
      public boolean useRestPos() {
         return true;
      }
      
      public boolean isWritable() {
         return (getField() instanceof ModelComponent);
      }
   }

   private static String FieldFunctionClassName = FieldFunction.class.getName();

   static class NodalFieldFunction<T>
      extends FieldFunction<T> {

      NodalField<T> myField;

      NodalFieldFunction (NodalField<T> field) {
         myField = field;
      }

      public NodalField<T> getField() {
         return myField;
      }

      public T eval (FieldPoint def) {
         return myField.getValue (def.getNodeNumbers(), def.getNodeWeights());
      }
   }

   static class IndexedNodalFieldFunction<T>
      extends FieldFunction<T> {

      NodalField<T> myField;
      IndexedField<T> myCache;

      IndexedNodalFieldFunction (NodalField<T> field) {
         myField = field;
         myCache = (IndexedField<T>)field;
      }

      public NodalField<T> getField() {
         return myField;
      }

      public T eval (FieldPoint def) {
         T val = myCache.getValueByIndex (def.getPointIndex());
         if (val == null) {
            val = myField.getValue (
               def.getNodeNumbers(), def.getNodeWeights());
            myCache.setValueByIndex (def.getPointIndex(), val);
         }
         return val;
      }
   }

   static class ElementFieldFunction<T>
      extends FieldFunction<T> {

      ElementField<T> myField;

      ElementFieldFunction (ElementField<T> field) {
         myField = field;
      }

      public ElementField<T> getField() {
         return myField;
      }

      public T eval (FieldPoint def) {
         return myField.getValue (def.getElementNumber());
      }
   }

   static abstract class StandardFieldFunction<T>
      extends FieldFunction<T> {

      Field<T> myField;

      StandardFieldFunction (Field<T> field) {
         myField = field;
      }

      public Field<T> getField() {
         return myField;
      }
   }

   static class RestFieldFunction<T> extends StandardFieldFunction<T> {
      
      RestFieldFunction (Field<T> field) {
         super (field);
      }

      public T eval (FieldPoint def) {
         return myField.getValue (def.getRestPos());
      }
   }

   static class SpatialFieldFunction<T> extends StandardFieldFunction<T> {

      SpatialFieldFunction (Field<T> field) {
         super (field);
      }

      public T eval (FieldPoint def) {
         return myField.getValue (def.getSpatialPos());
      }
      
      public boolean useRestPos() {
         return false;
      }
   }

   static abstract class IndexedFieldFunction<T>
      extends FieldFunction<T> {

      Field<T> myField;
      IndexedField<T> myCache;

      IndexedFieldFunction (Field<T> field) {
         myField = field;
         myCache = (IndexedField<T>)field;
      }

      public Field<T> getField() {
         return myField;
      }
   }

   static class IndexedRestFieldFunction<T> extends IndexedFieldFunction<T> {

      IndexedRestFieldFunction (Field<T> field) {
         super (field);
      }

      public T eval (FieldPoint def) {
         T val = myCache.getValueByIndex (def.getPointIndex());
         if (val == null) {
            val = myField.getValue (def.getRestPos());
            myCache.setValueByIndex (def.getPointIndex(), val);
         }
         return val;
      }
   }

   static class IndexedSpatialFieldFunction<T> extends IndexedFieldFunction<T> {

      IndexedSpatialFieldFunction (Field<T> field) {
         super (field);
      }

      public T eval (FieldPoint def) {
         T val = myCache.getValueByIndex (def.getPointIndex());
         if (val == null) {
            val = myField.getValue (def.getSpatialPos());
            myCache.setValueByIndex (def.getPointIndex(), val);
         }
         return val;
      }
   }

   public static <T> FieldPointFunction<T> createFieldFunction (
      Field<T> field, boolean useRestPos) { 

      FieldPointFunction<T> fxn = null;
      if (field instanceof NodalField<?>) {
         if (field instanceof IndexedField<?>) {
            fxn = new IndexedNodalFieldFunction<T> ((NodalField<T>)field);
         }
         else {
            fxn = new NodalFieldFunction<T>((NodalField<T>)field);
         }
      }
      else if (field instanceof ElementField<?>) {
         fxn = new ElementFieldFunction<T>((ElementField<T>)field);
      }
      else { // regular field
         if (field instanceof IndexedField<?>) {
            if (useRestPos) {
               fxn = new IndexedRestFieldFunction<T>(field);
            }
            else {
               fxn = new IndexedSpatialFieldFunction<T>(field);
            }
         }
         else {
            if (useRestPos) {
               fxn = new RestFieldFunction<T>(field);
            }
            else {
               fxn = new SpatialFieldFunction<T>(field);
            }
         }
      }
      return fxn;
   }

   public static <T> Field<T> getFieldFromFunction (
      FieldPointFunction<T> func) {
      if (func instanceof FieldFunction<?>) {
         return ((FieldFunction<T>)func).getField();
      }
      else {
         return null;
      }
   }

   public static void writeFieldFunction (
      PrintWriter pw, FieldFunction fxn, NumberFormat fmt, Object ref) 
      throws IOException {
      Field field = fxn.getField();
      if (field instanceof ModelComponent) {
         String fieldPath = 
            ComponentUtils.getWritePathName (
               ComponentUtils.castRefToAncestor(ref), (ModelComponent)field);
         IndentingPrintWriter.addIndentation (pw, 2);
         pw.println ("[");
         pw.println ("field=" + fieldPath);
         if (fxn.useRestPos()) {
            pw.println ("useRestPos=true");
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }
      
   public static void scanFieldFunction (
      ReaderTokenizer rtok, Object ref) throws IOException {
      Deque<ScanToken> tokens = (Deque<ScanToken>)ref;
      if (tokens == null) {
         tokens = new ArrayDeque<> ();
      }
      tokens.offer (ScanToken.BEGIN);
      rtok.scanToken ('[');
      boolean useRest = false;
      while (rtok.nextToken() != ']') {
         if (ScanWriteUtils.scanAttributeName(rtok, "field")) {
            if (!ScanWriteUtils.scanAndStoreReference (rtok, tokens)) {
               throw new IOException ("Expected quoted string, got " + rtok);
            }
         }
         else if (ScanWriteUtils.scanAttributeName(rtok, "useRestPos")) {
            useRest = rtok.scanBoolean();
         }
      }
      tokens.offer (new IntegerToken (useRest ? 1 : 0));
      tokens.offer (ScanToken.END);
   }

   public static FieldPointFunction postscanFieldFunction (
      Deque<ScanToken> tokens,
      CompositeComponent ancestor) throws IOException {

      ScanToken tok;
      ScanWriteUtils.postscanBeginToken (tokens, "FieldPointFunction");
      Field field = ScanWriteUtils.postscanReference (
         tokens, Field.class, ancestor);
      if (!((tok=tokens.poll()) instanceof IntegerToken)) {
         throw new IOException ("Expected IntegerToken, got "+tok);
      }    
      ScanWriteUtils.postscanEndToken (tokens, "FieldPointFunction");
      boolean useRest = (((IntegerToken)tok).value() != 0 ? true : false);
      return createFieldFunction (field, useRest);
   }

   private static boolean hasNoArgsConstructor (Class<?> clazz) {
      Constructor<?> constructor = null;
      try {
         constructor = clazz.getConstructor();
      }
      catch (Exception e) {
         // ignore
      }
      return constructor != null;
   }

   public static void writeFunctionInfo (
      PrintWriter pw, String name,
      FieldPointFunction func, NumberFormat fmt,
      CompositeComponent ancestor) throws IOException {
      
      if (func instanceof FieldFunction) {
         pw.print (name + "=" + FieldFunctionClassName);
         writeFieldFunction (pw, (FieldFunction)func, fmt, ancestor);
      }
      else if (func != null) {
         Class fclass = func.getClass();
         if (hasNoArgsConstructor (fclass)) {
            pw.print (name + "=" + fclass.getName());
            if (func instanceof Scannable) {
               ((Scannable)func).write (pw, fmt, ancestor);
            }
            else {
               pw.println("");
            }
         }
      }
   }

   public static FieldPointFunction scanFunctionInfo (
      ReaderTokenizer rtok, String name,
      Deque<ScanToken> tokens) throws IOException {

      Class<?> clazz = Scan.scanClass (rtok);
      if (clazz == FieldFunction.class) {
         tokens.offer (new StringToken (name));
         scanFieldFunction (rtok, tokens);
         return null;
      }
      else {
         // instantiate class
         Object obj;
         try {
            obj = clazz.newInstance();
         }
         catch (Exception e) {
            throw new IOException (
               "Cannot instantiate class "+clazz.getName()+": "+e.getMessage());
         }
         if (!(obj instanceof FieldPointFunction)) {
            throw new IOException (
               "Class "+clazz.getName()+" not an instance of FieldPointFunction");
         }
         if (obj instanceof Scannable) {
            if (obj instanceof PostScannable) {
               tokens.offer (new StringToken (name));
               tokens.offer (new ObjectToken (obj));
            }
            ((Scannable)obj).scan (rtok, tokens);
         }
         return (FieldPointFunction)obj;         
      }
   }

   public static FieldPointFunction postscanFunctionInfo (
      Deque<ScanToken> tokens,
      CompositeComponent ancestor) throws IOException {
      
      if (tokens.peek() instanceof ObjectToken) {
         // application defined function
         FieldPointFunction fxn = (FieldPointFunction)tokens.poll().value();
         ((PostScannable)fxn).postscan (tokens, ancestor);
         return fxn;
      }
      else {
         return postscanFieldFunction (tokens, ancestor);
      }
   }
   
}
