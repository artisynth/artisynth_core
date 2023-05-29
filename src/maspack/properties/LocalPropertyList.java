package maspack.properties;

import java.io.*;
import java.util.*;
import maspack.util.*;

/**
 * Implements a local property list that allows the addition of properties
 * which are specific to a class-instance.
 */
public class LocalPropertyList implements PropertyInfoList {

   PropertyList myClassProps;
   LinkedHashMap<String,PropertyInfo> myLocalProps;
   boolean myHasNoLocalInheritableP = true;

   class MyIterator implements Iterator<PropertyInfo> {

      boolean myIteratingClassProps = false;
      Iterator<PropertyInfo> myIterator;

      MyIterator() {
         if (myClassProps != null) {
            myIterator = myClassProps.iterator();
            myIteratingClassProps = true;
         }
         else {
            myIterator = myLocalProps.values().iterator();
         }
      }

      public boolean hasNext() {
         if (myIterator.hasNext()) {
            return true;
         }
         if (myIteratingClassProps) {
            myIterator = myLocalProps.values().iterator();
            myIteratingClassProps = false;
         }        
         return myIterator.hasNext();
      }

      public PropertyInfo next() {
         if (myIterator.hasNext()) {
            return myIterator.next();
         }
         if (myIteratingClassProps) {
            myIterator = myLocalProps.values().iterator();
            myIteratingClassProps = false;
         }
         return myIterator.next();
      }
   }


   public LocalPropertyList (PropertyList classProps) {
      this();
      myClassProps = classProps;
   }

   public LocalPropertyList () {
      myLocalProps = new LinkedHashMap<>();
   }

   Class<?> getHostClass() {
      if (myClassProps != null) {
         return myClassProps.myHostClass;
      }
      else {
         return null;
      }
   }

   public PropertyInfo get (String name) {
      PropertyInfo info;
      if (myClassProps != null) {
         if ((info=myClassProps.get (name)) != null) {
            return info;
         }
      }
      return myLocalProps.get(name);
   }

   public Iterator<PropertyInfo> iterator() {
      return new MyIterator();
   }

   public int size() {
      int size = myLocalProps.size();
      if (myClassProps != null) {
         size += myClassProps.size();
      }
      return size;
   }

   public boolean hasNoInheritableProperties() {
      if (myClassProps != null) {
         if (!myClassProps.hasNoInheritableProperties()) {
            return false;
         }
      }
      return myHasNoLocalInheritableP;
   }

    private boolean hasNoLocalInheritable() {
      for (PropertyInfo info : myLocalProps.values()) {
         if (info.isInheritable() ||
             CompositeProperty.class.isAssignableFrom (info.getValueClass())) {
            return false;
         }
      }
      return true;
   }  

   public PropertyDesc add (
      String name, String methods, Object key, String description,
      Object defaultValue, String options) {
      PropertyDesc desc = new PropertyDesc();
      if (PropertyDesc.initialize (
             desc, name, methods, key, getHostClass(), description,
            defaultValue, options, PropertyDesc.REGULAR)) {
         add (desc);
         return desc;
      }
      else {
         return null;
      }
   }

   public PropertyDesc add (
      String name, String methods, Object key, String description,
      Object defaultValue) {
      return add (name, methods, key, description, defaultValue, null);
   }

   public void add (PropertyInfo info) {
      String name = info.getName();
      if (get(name) != null) {
         throw new IllegalArgumentException (
            "Property " + info.getName() + " already exists");
      }
      if (info.isInheritable() ||
          CompositeProperty.class.isAssignableFrom (info.getValueClass())) {
         myHasNoLocalInheritableP = false;
      }
      myLocalProps.put (name, info);
   }

   public boolean remove (String name) {
      if (myLocalProps.remove (name) != null) {
         myHasNoLocalInheritableP = hasNoLocalInheritable();
         return true;
      }
      else {
         return false;
      }
   }
   
   /**
    * Writes properties in this list whose current values differ from their
    * default values to a PrintWriter. This allows the conservation of space
    * within persistent storage. Otherwise, the behaviour of this method is
    * identical to {@link #writeProps writeProps}; in particular only those
    * properties for which {@link maspack.properties.PropertyInfo#getAutoWrite
    * PropertyInfo.getAutoWrite} returns true are written.
    * 
    * @param host
    * class exporting the properties
    * @param pw
    * PrintWriter to which properties are written
    * @param fmt
    * Numeric formatting information. This is only used when the value to be
    * written is itself {@link maspack.util.Scannable Scannable}, in which case
    * it is passed to that value's {@link maspack.util.Scannable#write write}
    * method.
    * @param ref
    * Reference object. This is only used when the value to be
    * written is itself {@link maspack.util.Scannable Scannable}, in which case
    * it is passed to that value's {@link maspack.util.Scannable#write write}
    * method.
    * @throws IOException
    * if an I/O error occurred writing to the stream
    */
   public boolean writeNonDefaultProps (
      HasProperties host, PrintWriter pw, NumberFormat fmt, Object ref) 
      throws IOException {
      boolean wroteSomething = false;
      if (myClassProps != null) {
         wroteSomething = myClassProps.writeNonDefaultProps (
            host, pw, fmt, ref);
      }
      for (PropertyInfo desc : myLocalProps.values()) {
         if (desc.getAutoWrite()) {
            boolean wrote = desc.writeIfNonDefault (host, pw, fmt, ref);
            wroteSomething |= wrote;
         }
      }
      return wroteSomething;
   }

}
