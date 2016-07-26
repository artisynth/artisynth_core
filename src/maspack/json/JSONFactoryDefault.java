package maspack.json;

import java.util.ArrayList;
import java.util.HashMap;

public class JSONFactoryDefault implements JSONFactory<Object, HashMap<String,Object>, ArrayList<Object>> {

   @Override
   public HashMap<String,Object> createObject() {
      return new HashMap<String,Object>();
   }

   @Override
   public void addObjectMember(HashMap<String,Object> object, String key, Object val) {
      object.put(key, val);
   }

   @Override
   public ArrayList<Object> createArray() {
      return new ArrayList<Object>();
   }

   @Override
   public void addArrayElement(ArrayList<Object> array, Object elem) {
      array.add(elem);
   }

   @Override
   public Object createString(String str) {
      return str;
   }

   @Override
   public Object createNumber(double v) {
      return new Double(v);
   }

   @Override
   public Object createTrue() {
      return Boolean.TRUE;
   }

   @Override
   public Object createFalse() {
      return Boolean.FALSE;
   }

   @Override
   public Object createNull() {
      return null;
   }

}
