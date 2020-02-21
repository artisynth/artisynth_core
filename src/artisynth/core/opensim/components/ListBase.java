package artisynth.core.opensim.components;

import java.util.ArrayList;
import java.util.Iterator;

// Like "Set" but doesn't have objects vs groups, only immediate content
// Also is not a true object so doesn't have a path 
public abstract class ListBase<E extends OpenSimObject> extends OpenSimObject implements Iterable<E> {
   
   // 
   ArrayList<E> objects;
   
   public ListBase() {
      objects = new ArrayList<> ();
   }
   
   public void add(E obj) {
      objects.add (obj);
      obj.setParent (this);
   }
   
   public int size() {
      return objects.size ();
   }
   
   public E get(int idx) {
      return objects.get(idx);
   }
   
   public ArrayList<E> objects() {
      return objects;
   }
   
   @Override
   public ListBase<E> clone () {
      @SuppressWarnings("unchecked")
      ListBase<E> set = (ListBase<E>)super.clone ();
      
      // deep clone of objects
      set.objects = new ArrayList<>();
      for (E obj : objects) {
         set.add (obj);
      }
    
      return set;
   }
   
   @Override
   public String getPath () {
      // same as parent path - not considered a path object
      return getParent().getPath ();
   }

   @Override
   public Iterator<E> iterator () {
      return objects.iterator ();
   }
   
}
