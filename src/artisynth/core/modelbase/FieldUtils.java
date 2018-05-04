package artisynth.core.modelbase;

public class FieldUtils {

   public static abstract class FieldFunction<T>
      implements FieldPointFunction<T> {
      public abstract Field<T> getField();
   }

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
   
}
