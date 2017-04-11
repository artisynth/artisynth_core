package maspack.json;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class JSONFactoryHandler<P, O extends Map<String,P>, A extends List<P>> implements JSONHandler {

   private static enum ElemType {
      OBJECT,
      ARRAY,
      ROOT
   }

   private class Node {
      Node parent;
      ElemType type;
      O map;
      A list;
      LinkedList<P> elements;

      String key;
      boolean hasKey;
      P val;

      public Node(Node parent, ElemType type) {
         this.type = type;
         this.parent = parent;
         if (type == ElemType.OBJECT) {
            map = factory.createObject();
            hasKey = false;
         } else if (type == ElemType.ARRAY) {
            list = factory.createArray();
         } else {
            elements = new LinkedList<>();
         }
      }
   }

   private JSONFactory<P,O,A> factory;

   private Node root;
   private Node current;

   public JSONFactoryHandler(JSONFactory<P,O,A> factory) {
      this.factory = factory;
      clear();
   }

   @Override
   public void beginObject() {
      Node next = new Node(current, ElemType.OBJECT);
      current = next;
   }

   @Override
   public void yieldObject() {
      // last member
      if (current.type == ElemType.OBJECT) {
         if (current.hasKey) {
            if (current.val == null) {
               current.val = factory.createNull();
            }
            current.map.put(current.key, current.val);
         }
         current.key = null;
         current.val = null;
         current.hasKey = false;

         Node parent = current.parent;
         @SuppressWarnings("unchecked")
         P pobj = (P)current.map;
         parent.val = pobj; 
         current = parent;
         maybeYieldRoot();
      }
   }

   @Override
   public void yieldKey() {
      current.hasKey = true;
   }

   @Override
   public void yieldSeparator() {
      if (current.type == ElemType.OBJECT) {
         if (current.hasKey) {
            if (current.val == null) {
               current.val = factory.createNull();
            }
            current.map.put(current.key, current.val);
         }
         current.key = null;
         current.val = null;
         current.hasKey = false;
      } else if (current.type == ElemType.ARRAY) {
         if (current.val != null) {
            current.list.add(current.val);
         }
         current.val = null;
      }
   }

   @Override
   public void beginArray() {
      Node next = new Node(current, ElemType.ARRAY);
      current = next;
   }

   @Override
   public void yieldArray() {

      // last member
      if (current.type == ElemType.ARRAY) {
         if (current.val != null) {
            current.list.add(current.val);
         }

         current.val = null;
         Node parent = current.parent;
         @SuppressWarnings("unchecked")
         P pobj = (P)current.list;
         parent.val = pobj; 
         current = parent;
         maybeYieldRoot();
      }
   }

   private void maybeYieldRoot() {
      if (current.type == ElemType.ROOT) {
         current.elements.add(current.val);
         current.val = null;
      }
   }

   @Override
   public void yieldString(String str) {
      // check if could be key
      if (current.type == ElemType.OBJECT) {
         if (!current.hasKey) {
            current.key = str;
         } else {
            current.val = factory.createString(str);
         }
      } else {
         current.val = factory.createString(str);
         if (current.type == ElemType.ROOT) {
            maybeYieldRoot();     
         }
      }
   }

   @Override
   public void yieldNumber(double v) {
      current.val = factory.createNumber(v);
      maybeYieldRoot();
   }

   @Override
   public void yieldTrue() {
      current.val = factory.createTrue();
      maybeYieldRoot();
   }

   @Override
   public void yieldFalse() {
      current.val = factory.createFalse();
      maybeYieldRoot();
   }

   @Override
   public void yieldNull() {
      current.val = factory.createNull();
      maybeYieldRoot();
   }

   /**
    * Attempt to detect if garbage is actually a missing data:
    * <pre>
    *    If we are currently parsing an object
    *        Check the key has been defined
    *           If not, set the key
    *           Otherwise, check if the value is defined
    *           If not, set the value
    *           Otherwise, ignore 
    *    If we are currently parsing an Array
    *        Check the element has been defined
    *           If not, set the element
    *           Otherwise, ignore
    * </pre>
    *     
    */
   @Override
   public void yieldGarbage(String str) {
      if (current.type == ElemType.OBJECT) {
         if (!current.hasKey) {
            current.key = str;
         } else if (current.val == null) {
            current.val = factory.createString(str);
         }
      } else if (current.type == ElemType.ARRAY) {
         if (current.val != null) {
            current.val = factory.createString(str);
         }
      }
   }

   @Override
   public boolean hasElement() {
      return !(root.elements.isEmpty());
   }

   @Override
   public void clear() {
      root = new Node(null, ElemType.ROOT);
      current = root;
   }

   /**
    * Pops a parsed element
    * @return object
    */
   public P pop() {
      return root.elements.removeFirst();
   }

}
