package artisynth.core.opensim.components;

import java.util.ArrayList;
import java.util.Iterator;

public abstract class SetBase<E extends OpenSimObject> extends OpenSimObject implements Iterable<E> {
   
   // display geometry
   ArrayList<E> objects;
   ArrayList<ObjectGroup> groups;
   
   public SetBase() {
      objects = new ArrayList<> ();
      groups = new ArrayList<> ();
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
   
   public void addGroup(ObjectGroup group) {
      groups.add (group);
      group.setParent (this);
   }
   
   public ArrayList<ObjectGroup> groups() {
      return groups;
   }
   
   public int numGroups() {
      return groups.size ();
   }
   
   public ObjectGroup getGroup(int idx) {
      return groups.get (idx);
   }
   
   @Override
   public SetBase<E> clone () {
      @SuppressWarnings("unchecked")
      SetBase<E> set = (SetBase<E>)super.clone ();
      
      // deep clone of objects
      set.objects = new ArrayList<>();
      for (E obj : objects) {
         set.add (obj);
      }
      
      // deep clone of groups
      set.groups = new ArrayList<>();
      for (ObjectGroup group : groups) {
         set.addGroup (group);
      }
      
      return set;
   }

   @Override
   public Iterator<E> iterator () {
      return objects.iterator ();
   }
   
}
