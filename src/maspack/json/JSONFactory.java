package maspack.json;

import java.util.List;
import java.util.Map;

/**
 * Factory interface for creating JSON objects
 * @author antonio
 *
 * @param <P> common parent for all nodes, required to allow mixed-type flexibility
 * @param <O> Object type, implements Map interface
 * @param <A> Array type, implements List interface
 */
public interface JSONFactory<P, O extends Map<String,P>, A extends List<P>> {

   /**
    * Preate a JSON Object
    * @return a new empty object
    */
   public O createObject();
   
   /**
    * Add a member pair to an object
    */
   public void addObjectMember(O object, String key, P val);
   
   /**
    * Preate a JSON array
    * @return a new empty array
    */
   public A createArray();
   
   /**
    * Add an element to an array
    */
   public void addArrayElement(A array, P elem);
   
   /**
    * Preate a String element
    * @return newly created string element
    */
   public P createString(String str);
   
   /**
    * Preate a number element
    * @return newly created number element
    */
   public P createNumber(double v);
   
   /**
    * Preate a 'true' boolean element
    * @return newly created boolean
    */
   public P createTrue();
   
   /**
    * Preate a 'false' boolean element
    * @return a newly created boolean
    */
   public P createFalse();
   
   /**
    * Preate a 'null' element
    * @return a newly created null element
    */
   public P createNull();
   
}
