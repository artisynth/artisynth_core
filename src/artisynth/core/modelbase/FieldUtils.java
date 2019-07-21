package artisynth.core.modelbase;

import java.util.Deque;
import java.util.ArrayDeque;
import java.lang.reflect.Constructor;
import java.io.PrintWriter;
import java.io.IOException;
import artisynth.core.util.*;
import maspack.util.*;
import maspack.matrix.VectorObject;

public class FieldUtils {
   
   public static abstract class FieldFunctionBase {
      
      public abstract Field getField();
      
      public boolean useRestPos() {
         return true;
      }

      public abstract boolean isWritable();
   }
   
   public static abstract class VectorFieldFunction<T extends VectorObject<T>> 
      extends FieldFunctionBase implements VectorFieldPointFunction<T>  {
      public abstract VectorField<T> getField();
      
      public boolean isWritable() {
         return (getField() instanceof ModelComponent);
      }
   }

   public static abstract class ScalarFieldFunction
   extends FieldFunctionBase implements ScalarFieldPointFunction  {
      public abstract ScalarField getField();
      
      public boolean isWritable() {
         return (getField() instanceof ModelComponent);
      }
   }

   private static String VectorFieldFunctionClassName =
      VectorFieldFunction.class.getName();

   private static String ScalarFieldFunctionClassName =
      ScalarFieldFunction.class.getName();

//   static class NodalFieldFunction<T extends VectorObject<T>>
//      extends VectorFieldFunction<T> {
//
//      NodalField<T> myField;
//
//      NodalFieldFunction (NodalField<T> field) {
//         myField = field;
//      }
//
//      public NodalField<T> getField() {
//         return myField;
//      }
//
//      public T eval (FieldPoint def) {
//         return myField.getValue (def.getNodeNumbers(), def.getNodeWeights());
//      }
//   }
//
//   static class IndexedNodalFieldFunction<T extends VectorObject<T>>
//      extends VectorFieldFunction<T> {
//
//      NodalField<T> myField;
//      IndexedField<T> myCache;
//
//      IndexedNodalFieldFunction (NodalField<T> field) {
//         myField = field;
//         myCache = (IndexedField<T>)field;
//      }
//
//      public NodalField<T> getField() {
//         return myField;
//      }
//
//      public T eval (FieldPoint def) {
//         T val = myCache.getValueByIndex (def.getPointIndex());
//         if (val == null) {
//            val = myField.getValue (
//               def.getNodeNumbers(), def.getNodeWeights());
//            myCache.setValueByIndex (def.getPointIndex(), val);
//         }
//         return val;
//      }
//   }

//   static class ElementFieldFunction<T extends VectorObject<T>>
//      extends VectorFieldFunction<T> {
//
//      ElementField<T> myField;
//
//      ElementFieldFunction (ElementField<T> field) {
//         myField = field;
//      }
//
//      public ElementField<T> getField() {
//         return myField;
//      }
//
//      public T eval (FieldPoint def) {
//         return myField.getValue (def.getElementIndex());
//      }
//   }
//
//   static abstract class StandardFieldFunction<T extends VectorObject<T>>
//      extends VectorFieldFunction<T> {
//
//      VectorField<T> myField;
//
//      StandardFieldFunction (VectorField<T> field) {
//         myField = field;
//      }
//
//      public VectorField<T> getField() {
//         return myField;
//      }
//   }

//   static class RestFieldFunction<T extends VectorObject<T>> 
//      extends StandardFieldFunction<T> {
//      
//      RestFieldFunction (VectorField<T> field) {
//         super (field);
//      }
//
//      public T eval (FieldPoint def) {
//         return myField.getValue (def.getRestPos());
//      }
//   }
//
//   static class SpatialFieldFunction<T extends VectorObject<T>> 
//      extends StandardFieldFunction<T> {
//
//      SpatialFieldFunction (VectorField<T> field) {
//         super (field);
//      }
//
//      public T eval (FieldPoint def) {
//         return myField.getValue (def.getSpatialPos());
//      }
//      
//      public boolean useRestPos() {
//         return false;
//      }
//   }

//   static abstract class IndexedFieldFunction<T extends VectorObject<T>>
//      extends VectorFieldFunction<T> {
//
//      VectorField<T> myField;
//      IndexedField<T> myCache;
//
//      IndexedFieldFunction (VectorField<T> field) {
//         myField = field;
//         myCache = (IndexedField<T>)field;
//      }
//
//      public VectorField<T> getField() {
//         return myField;
//      }
//   }

//   static class IndexedRestFieldFunction<T extends VectorObject<T>> 
//      extends IndexedFieldFunction<T> {
//
//      IndexedRestFieldFunction (VectorField<T> field) {
//         super (field);
//      }
//
//      public T eval (FieldPoint def) {
//         T val = myCache.getValueByIndex (def.getPointIndex());
//         if (val == null) {
//            val = myField.getValue (def.getRestPos());
//            myCache.setValueByIndex (def.getPointIndex(), val);
//         }
//         return val;
//      }
//   }
//
//   static class IndexedSpatialFieldFunction<T extends VectorObject<T>> 
//      extends IndexedFieldFunction<T> {
//
//      IndexedSpatialFieldFunction (VectorField<T> field) {
//         super (field);
//      }
//
//      public T eval (FieldPoint def) {
//         T val = myCache.getValueByIndex (def.getPointIndex());
//         if (val == null) {
//            val = myField.getValue (def.getSpatialPos());
//            myCache.setValueByIndex (def.getPointIndex(), val);
//         }
//         return val;
//      }
//   }

//   public static <T extends VectorObject<T>> VectorFieldPointFunction<T> createFieldFunction (
//      VectorField<T> field, boolean useRestPos) { 
//
//      VectorFieldPointFunction<T> fxn = null;
//      if (field instanceof NodalField<?>) {
//         if (field instanceof IndexedField<?>) {
//            fxn = new IndexedNodalFieldFunction<T> ((NodalField<T>)field);
//         }
//         else {
//            fxn = new NodalFieldFunction<T>((NodalField<T>)field);
//         }
//      }
//      else if (field instanceof ElementField<?>) {
//         fxn = new ElementFieldFunction<T>((ElementField<T>)field);
//      }
//      else { // regular field
//         if (field instanceof IndexedField<?>) {
//            if (useRestPos) {
//               fxn = new IndexedRestFieldFunction<T>(field);
//            }
//            else {
//               fxn = new IndexedSpatialFieldFunction<T>(field);
//            }
//         }
//         else {
//            if (useRestPos) {
//               fxn = new RestFieldFunction<T>(field);
//            }
//            else {
//               fxn = new SpatialFieldFunction<T>(field);
//            }
//         }
//      }
//      return fxn;
//   }

   public static <T extends VectorObject<T>> VectorField<T> getFieldFromFunction (
      VectorFieldPointFunction<T> func) {
      if (func instanceof VectorFieldFunction<?>) {
         return ((VectorFieldFunction<T>)func).getField();
      }
      else {
         return null;
      }
   }

   public static ScalarField getFieldFromFunction (
      ScalarFieldPointFunction func) {
      if (func instanceof ScalarFieldFunction) {
         return ((ScalarFieldFunction)func).getField();
      }
      else {
         return null;
      }
   }

   public static <T extends VectorObject<T>> VectorFieldPointFunction<T>
      setFunctionFromField (VectorField<T> field, boolean useRestPos) {
      if (field == null) {
         return null;
      }
      else {
         return field.createFieldFunction (useRestPos);
      }
   }

   public static ScalarFieldPointFunction setFunctionFromField (
      ScalarField field, boolean useRestPos) {
      if (field == null) {
         return null;
      }
      else {
         return field.createFieldFunction (useRestPos);
      }
   }

   public static void writeFieldFunction (
      PrintWriter pw, FieldFunctionBase fxn, NumberFormat fmt, Object ref) 
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
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {
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

   public static <T extends Field> FieldPointFunction postscanFieldFunction (
      Deque<ScanToken> tokens, Class<T> fieldType,
      CompositeComponent ancestor) throws IOException {

      ScanToken tok;
      ScanWriteUtils.postscanBeginToken (tokens, "FieldPointFunction");
      T field = ScanWriteUtils.postscanReference (
         tokens, fieldType, ancestor);
      if (!((tok=tokens.poll()) instanceof IntegerToken)) {
         throw new IOException ("Expected IntegerToken, got "+tok);
      }    
      ScanWriteUtils.postscanEndToken (tokens, "FieldPointFunction");
      boolean useRest = (((IntegerToken)tok).value() != 0 ? true : false);
      return field.createFieldFunction (useRest);
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

   public static void writeScalarFunctionInfo (
      PrintWriter pw, String name, ScalarFieldPointFunction func,
      NumberFormat fmt, CompositeComponent ancestor) throws IOException {

      writeFunctionInfo (pw, name, func, fmt, ancestor);
   }

   public static <T extends VectorObject<T>> void writeVectorFunctionInfo (
      PrintWriter pw, String name, VectorFieldPointFunction<T> func,
      NumberFormat fmt, CompositeComponent ancestor) throws IOException {

      writeFunctionInfo (pw, name, func, fmt, ancestor);
   }

   protected static void writeFunctionInfo (
      PrintWriter pw, String name,
      FieldPointFunction func, NumberFormat fmt,
      CompositeComponent ancestor) throws IOException {

      if (func instanceof FieldFunctionBase) {
         if (func instanceof VectorFieldFunction) {
            pw.print (name + "=" + VectorFieldFunctionClassName);
         }
         else {
            pw.print (name + "=" + ScalarFieldFunctionClassName);
         }
         writeFieldFunction (pw, (FieldFunctionBase)func, fmt, ancestor);
      }
      else if (func != null) {
         Class<?> fclass = func.getClass();
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

   protected static Object scanApplicationFieldFunction (
      ReaderTokenizer rtok, String name, Class<?> clazz,
      Deque<ScanToken> tokens) throws IOException {

      Object obj;
      try {
         obj = clazz.newInstance();
      }
      catch (Exception e) {
         throw new IOException (
            "Cannot instantiate class "+clazz.getName(), e);
      }
      if (obj instanceof Scannable) {
         if (obj instanceof PostScannable) {
            tokens.offer (new StringToken (name));
            tokens.offer (new ObjectToken (obj));
         }
         ((Scannable)obj).scan (rtok, tokens);
      }
      return obj;
   }

   public static <T extends VectorObject<T>> 
   VectorFieldPointFunction<T> scanVectorFunctionInfo (
      ReaderTokenizer rtok, String name,
      Deque<ScanToken> tokens) throws IOException {

      Class<?> clazz = Scan.scanClass (rtok);
      if (clazz == VectorFieldFunction.class) {
         tokens.offer (new StringToken (name));
         scanFieldFunction (rtok, tokens);
         return null;
      }
      else {
         Object obj = scanApplicationFieldFunction (rtok, name, clazz, tokens);
         if (!(obj instanceof VectorFieldPointFunction)) {
            throw new IOException (
               "Class "+clazz.getName()+" not an instance of FieldPointFunction");
         }
         return (VectorFieldPointFunction<T>)obj;         
      }
   }

   public static ScalarFieldPointFunction scanScalarFunctionInfo (
      ReaderTokenizer rtok, String name,
      Deque<ScanToken> tokens) throws IOException {

      Class<?> clazz = Scan.scanClass (rtok);
      if (clazz == ScalarFieldFunction.class) {
         tokens.offer (new StringToken (name));
         scanFieldFunction (rtok, tokens);
         return null;
      }
      else {
         Object obj = scanApplicationFieldFunction (rtok, name, clazz, tokens);
         if (!(obj instanceof ScalarFieldPointFunction)) {
            throw new IOException (
               "Class "+clazz.getName()+" not an instance of FieldPointFunction");
         }
         return (ScalarFieldPointFunction)obj;         
      }
   }

   public static <T extends VectorObject<T>> 
   VectorFieldPointFunction<T> postscanVectorFunctionInfo (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      VectorFieldPointFunction<T> fxn;
      if (tokens.peek() instanceof ObjectToken) {
         // application defined function
         fxn = (VectorFieldPointFunction<T>)tokens.poll().value();
         ((PostScannable)fxn).postscan (tokens, ancestor);
      }
      else {
         fxn = (VectorFieldPointFunction<T>)postscanFieldFunction (
            tokens, VectorField.class, ancestor);
      }
      return fxn;
   }

   public static ScalarFieldPointFunction postscanScalarFunctionInfo (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      ScalarFieldPointFunction fxn;
      if (tokens.peek() instanceof ObjectToken) {
         // application defined function
         fxn = (ScalarFieldPointFunction)tokens.poll().value();
         ((PostScannable)fxn).postscan (tokens, ancestor);
      }
      else {
         fxn = (ScalarFieldPointFunction)postscanFieldFunction (
            tokens, ScalarField.class, ancestor);
      }
      return fxn;
   }

   
}
