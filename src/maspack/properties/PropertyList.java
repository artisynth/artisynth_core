/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;

import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Implements a list of exported properties.
 */
public class PropertyList implements PropertyInfoList {
   protected LinkedHashMap<String,PropertyDesc> myProps =
      new LinkedHashMap<String,PropertyDesc>();
   protected Class<?> myHostClass = null;
   protected boolean myHasNoInheritableP = true;

   private class InfoIterator implements Iterator<PropertyInfo> {
      Iterator<PropertyDesc> myIterator;

      InfoIterator() {
         myIterator = myProps.values().iterator();
      }

      public boolean hasNext() {
         return myIterator.hasNext();
      }

      public PropertyInfo next() throws NoSuchElementException {
         return myIterator.next();
      }

      public void remove() throws UnsupportedOperationException {
         throw new UnsupportedOperationException();
      }
   }

   /**
    * Creates an empty property list for a specified exporting host class.
    * 
    * @param hostClass
    * class exporting the properties
    */
   public PropertyList (Class<?> hostClass) {
      myHostClass = hostClass;
   }

   /**
    * Creates a property list for a specified exporting host class. The list is
    * initialized by copying the properties from s specified ancestor class of
    * the host class.
    * 
    * @param hostClass
    * class exporting the properties
    * @param superClass
    * ancestor class containing properties to copy
    */
   public PropertyList (Class<?> hostClass, Class<?> superClass) {
      this (hostClass);
      if (superClass.isInterface() ||
          !(superClass.isAssignableFrom (hostClass))) {
         fatal (hostClass, "It is not a subclass of " + superClass.getName());
      }
      PropertyInfoList infoList = null;
      try {
         infoList = findPropertyInfoList (superClass);
      }
      catch (Exception e) {
         fatal (hostClass, e.getMessage());
      }
      PropertyList list = null;
      if (!(infoList instanceof PropertyList)) {
         fatal (hostClass,
                "PropertyInfoList for super class "+superClass+" not an \n"+
                "instance of PropertyList");
      }
      copy ((PropertyList)infoList, hostClass);
   }
   
   protected void copy (PropertyList list, Class<?> hostClass) {
      for (int i = 0; i < list.size(); i++) {
         PropertyDesc desc = new PropertyDesc();
         desc.set (list.get(i), hostClass);
         add (desc);
      }
   }

//   public PropertyList (Class<?> hostClass, PropertyList list) {
//      this (hostClass);
//      for (int i = 0; i < list.size(); i++) {
//         add (new PropertyDesc (list.get(i), hostClass));
//      }
//   }

   protected void fatal (Class<?> hostClass, String msg) {
      throw new InternalErrorException (
         "Error creating Properties for "+hostClass.getName()+": "+msg);
   }

   private static PropertyInfoList getListFromField (
      Class<?> declaringClass, Field field) {
      //PropertyInfoList list = null;
      int requiredFlags = (Modifier.STATIC | Modifier.PUBLIC);
      if ((field.getModifiers() & requiredFlags) != requiredFlags) {
         throw new UnsupportedOperationException (
            "Field "+field.getName()+" in "+declaringClass+
            " not static and/or not public");
      }
      Object fieldValue = null;
      try {
         fieldValue = field.get (null);
      }
      catch (Exception e) {
      }
      if (fieldValue == null) {
         throw new IllegalStateException (
            "Field "+field.getName()+" in "+declaringClass+
            " is inaccessible or null valued");
      }
      else if (!(fieldValue instanceof PropertyInfoList)) {
         throw new ClassCastException (
            "Field "+field.getName()+" in "+declaringClass+
            " is not a PropertyInfoList");
      }
      else {
         return (PropertyInfoList)fieldValue;
      }
   }

   public static PropertyInfoList findPropertyInfoList (Class<?> hostClass) {
      if (!HasProperties.class.isAssignableFrom (hostClass)) {
         throw new IllegalArgumentException (
            ""+hostClass+" is not an instance of HasProperties");
      }
      Field field = null;
      PropertyList list = null;
      try {
         field = hostClass.getDeclaredField ("myProps");
      }
      catch (Exception e) { // no such field
      }
      if (field != null) {
         try {
            return getListFromField (hostClass, field);
         }
         catch (Exception e) {
            throw new IllegalArgumentException (
               "Can't find property info for "+hostClass+":\n"+e.getMessage());
         }
      }
      for (Class<?> declaringClass = hostClass.getSuperclass();
           declaringClass != Object.class; 
           declaringClass = declaringClass.getSuperclass()) {
         field = null;
         try {
            field = declaringClass.getDeclaredField ("myProps");
         }
         catch (Exception e) { // no such field
         }
         if (field != null) {
            try {
               return getListFromField (declaringClass, field);
            }
            catch (Exception e) {
               throw new IllegalArgumentException (
                  "Can't find property info from ancestor of "+
                  hostClass+":\n"+e.getMessage());
            }
         }
      }
      HasProperties hostObj;
      try {
         hostObj = (HasProperties)hostClass.newInstance();
      }
      catch (Exception e) {
         hostObj = null;
      }
      if (hostObj != null) {
         try {
            list = (PropertyList)hostObj.getAllPropertyInfo();
         }
         catch (Exception e) { // cast failed
            list = null;
         }
      }
      if (list == null) {
         throw new IllegalArgumentException (
            "Class "+hostClass+" (or one of its ancestors) must contain a\n"+
            "   PropertyInfoList, accessible either through a public static\n"+
            "   field 'myProps', or a no-args constructor and the method\n"+
            "   'getAllPropertyInfo()'");
      }
      return list;
   }

   /**
    * Adds a new property to this list.
    * 
    * @param desc
    * descriptor for the property to be exported
    */
   public void add (PropertyDesc desc) {
      String name = desc.myName;
      if (myProps.get (name) != null) {
         throw new IllegalStateException ("Property list for "
         + myHostClass.getName() + ": property " + desc.myName
         + " already exists");
      }
      if (desc.isInheritable() ||
          CompositeProperty.class.isAssignableFrom (desc.getValueClass())) {
         myHasNoInheritableP = false;
      }
      myProps.put (name, desc);
   }

   // public void replace (PropertyDesc desc)
   // {
   // PropertyDesc oldProp = get (desc.myName);
   // if (oldProp == null)
   // { throw new IllegalStateException (
   // "Property list for " + myHostClass.getName() +
   // ": no original property " + desc.myName);
   // }
   // myProps.remove (oldProp);
   // myProps.add (desc);
   // }

   /**
    * Adds a new property to this list, creating the appropriate description
    * from the supplied arguments.
    * 
    * @param nameAndMethods
    * a string giving the name of the property, optionally followed by the names
    * of host class get and set methods which should be used to get and set the
    * property's value. The property and method names should be separated by
    * whitespace. If the method names are absent, or are specified using the
    * character `<code>*</code>`, then the default method names
    * <code>get</code><i>Prop</i> or <code>set</code><i>Prop</i> will be
    * used, as appropriate, where <i>Prop</i> is the capitalized property name.
    * @param description
    * a textual description of the property, for use in constructing
    * documentation or GUI tool-tips, etc.
    * @param defaultValue
    * a default value for the property. Generally, this should be the value
    * assigned to the property when the class is initialized.
    * @param options
    * a string setting various property options, having the same format as that
    * used by {@link #setOptions setOptions}.
    * @return descriptor which was created for the property
    */
   public PropertyDesc add (
      String nameAndMethods, String description, Object defaultValue,
      String options) {
      PropertyDesc desc = new PropertyDesc();
      if (PropertyDesc.initialize (
            desc, nameAndMethods, myHostClass, description, defaultValue,
            options, PropertyDesc.REGULAR)) {
         add (desc);
         return desc;
      }
      else {
         return null;
      }
   }

   /**
    * Adds a new property to this list, creating the appropriate description
    * from the supplied arguments.
    * 
    * @param nameAndMethods
    * a string giving the name of the property, optionally followed by the names
    * of host class get and set methods which should be used to get and set the
    * property's value. The property and method names should be separated by
    * whitespace. If the method names are absent, or are specified using the
    * character `<code>*</code>`, then the default method names
    * <code>get</code><i>Prop</i> or <code>set</code><i>Prop</i> will be
    * used, as appropriate, where <i>Prop</i> is the capitalized property name.
    * @param description
    * a textual description of the property, for use in constructing
    * documentation or GUI tool-tips, etc.
    * @param defaultValue
    * a default value for the property. Generally, this should be the value
    * assigned to the property when the class is initialized.
    * @return descriptor which was created for the property
    */
   public PropertyDesc add (
      String nameAndMethods, String description, Object defaultValue) {
      return add (nameAndMethods, description, defaultValue, null);
   }

   /**
    * Sets options for a specifed property within this list. The options are
    * specified as whitespace-separated tokens within a string, and may appear
    * in any order. Available options are:
    * 
    * <ul>
    * <li>
    * <p>
    * <code>NE</code>, <code>NeverEdit</code> Disables interactive editing.
    * This will cause {@link maspack.properties.PropertyInfo#getEditing
    * PropertyInfo.getEditing} to return
    * {@link maspack.properties.PropertyInfo.Edit#Never Edit.Never}.
    * <li>
    * <p>
    * <code>AE</code>, <code>AlwaysEdit</code> Enables interactive editing.
    * This will cause {@link maspack.properties.PropertyInfo#getEditing
    * PropertyInfo.getEditing} to return
    * {@link maspack.properties.PropertyInfo.Edit#Always Edit.Always}.
    * <li>
    * <p>
    * <code>1E</code>, <code>SingleEdit</code> Enables interactive editing
    * for one property host at a time. This will cause
    * {@link maspack.properties.PropertyInfo#getEditing PropertyInfo.getEditing}
    * to return {@link maspack.properties.PropertyInfo.Edit#Always Edit.Single}.
    * <li>
    * <p>
    * <code>NW</code>, <code>NoAutoWrite</code> Disable auto-write. This
    * will cause {@link maspack.properties.PropertyInfo#getAutoWrite
    * PropertyInfo.getAutoWrite} to return false.
    * <li>
    * <p>
    * <code>AW</code>, <code>AutoWrite</code> Enables auto-write. This will
    * cause {@link maspack.properties.PropertyInfo#getAutoWrite
    * PropertyInfo.getAutoWrite} to return true, <i>if</i> the property is not
    * read-only.
    * <li>
    * <p>
    * <code>SH</code>, <code>Sharable</code> Indicates that the property
    * value can be shared among several hosts.
    * <li>
    * <p>
    * <code>[l,u]</code> Sets a numeric range with a lower bound of
    * <code>l</code> and an upper bound of <code>u</code>. The numeric
    * range is returned by
    * {@link maspack.properties.PropertyInfo#getDefaultNumericRange
    * PropertyInfo.getDefaultNumericRange}.
    * <li>
    * <p>
    * <code>fmt</code> Sets a print format, using a printf-style format string
    * beginning with '<code>%</code>'. The print format is returned by
    * {@link maspack.properties.PropertyInfo#getPrintFormat
    * PropertyInfo.getPrintFormat} and is used for printing numeric property
    * values.
    * </ul>
    * 
    * @param name
    * name of the property for which options are to be set
    * @param optionStr
    * option string, conforming to the format described above
    * @throws IllegalArgumentException
    * if the specified property is not found, or if one of the tokens in the
    * option string is not recognized.
    */
   public void setOptions (String name, String optionStr) {
      PropertyDesc desc = get (name);
      if (desc == null) {
         throw new IllegalArgumentException ("property '" + name
         + "' not found");
      }
      desc.parseOptions (optionStr);
   }

   public void setDefaultValue (String name, Object value) {
      PropertyDesc desc = get (name);
      if (desc == null) {
         throw new IllegalArgumentException ("property '" + name
         + "' not found");
      }
      desc.setDefaultValue (value);
   }

   /**
    * Adds a new inheritable property to this list, creating the appropriate
    * description from the supplied arguments.
    * 
    * @param nameAndMethods
    * a string giving the name of the property, optionally followed by the names
    * of host class get and set methods for both the propertie's value and its
    * mode. The property and method names should be separated by whitespace. If
    * the method names are absent, or are specified using the character `<code>*</code>`,
    * then the default method names <code>get</code><i>Prop</i>,
    * <code>set</code><i>Prop</i>, <code>get</code><i>Prop</i><code>Mode</code>,
    * and <code>set</code><i>Prop</i><code>Mode</code> will be used, as
    * appropriate, where <i>Prop</i> is the capitalized property name.
    * @param description
    * a textual description of the property, for use in constructing
    * documentation or GUI tool-tips, etc.
    * @param defaultValue
    * a default value for the property. Generally, this should be the value
    * assigned to the property when the class is initialized.
    * @param options
    * a string setting various property options, having the same format as that
    * used by {@link #setOptions setOptions}.
    * @return descriptor which was created for the property
    */
   public PropertyDesc addInheritable (
      String nameAndMethods, String description, Object defaultValue,
      String options) {
      PropertyDesc desc = new PropertyDesc();
      if (PropertyDesc.initialize (
            desc, nameAndMethods, myHostClass, description, defaultValue,
            options, PropertyDesc.INHERITABLE)) {
         add (desc);
         return desc;
      }
      else {
         return null;
      }
   }

   /**
    * Adds a new inheritable property to this list, creating the appropriate
    * description from the supplied arguments.
    * 
    * @param nameAndMethods
    * a string giving the name of the property, optionally followed by the names
    * of host class get and set methods for both the propertie's value and its
    * mode. The property and method names should be separated by whitespace. If
    * the method names are absent, or are specified using the character `<code>*</code>`,
    * then the default method names <code>get</code><i>Prop</i>,
    * <code>set</code><i>Prop</i>, <code>get</code><i>Prop</i><code>Mode</code>,
    * and <code>set</code><i>Prop</i><code>Mode</code> will be used, as
    * appropriate, where <i>Prop</i> is the capitalized property name.
    * @param description
    * a textual description of the property, for use in constructing
    * documentation or GUI tool-tips, etc.
    * @param defaultValue
    * a default value for the property. Generally, this should be the value
    * assigned to the property when the class is initialized.
    * @return descriptor which was created for the property
    */
   public PropertyDesc addInheritable (
      String nameAndMethods, String description, Object defaultValue) {
      return addInheritable (nameAndMethods, description, defaultValue, null);
   }

   /**
    * Adds a new read-only property to this list, creating the appropriate
    * description from the supplied arguments.
    * 
    * @param nameAndMethods
    * a string giving the name of the property, optionally followed by the name
    * of the host class get method which should be used to get the property's
    * value. The property and method names should be separated by whitespace. If
    * the method names are absent, or are specified using the character `<code>*</code>`,
    * then the default method name <code>get</code><i>Prop</i> will be used,
    * where <i>Prop</i> is the capitalized property name.
    * @param description
    * a textual description of the property, for use in constructing
    * documentation or GUI tool-tips, etc.
    * @param options
    * a string setting various property options, having the same format as that
    * used by {@link #setOptions setOptions}.
    * @return descriptor which was created for the property
    */
   public PropertyDesc addReadOnly (
      String nameAndMethods, String description, String options) {
      PropertyDesc desc = new PropertyDesc();
      if (PropertyDesc.initialize (
            desc, nameAndMethods, myHostClass, description,
            Property.VoidValue, options, PropertyDesc.READ_ONLY)) {
         add (desc);
         return desc;
      }
      else {
         return null;
      }
   }

   /**
    * Adds a new read-only property to this list, creating the appropriate
    * description from the supplied arguments.
    * 
    * @param nameAndMethods
    * a string giving the name of the property, optionally followed by the name
    * of the host class get method which should be used to get the property's
    * value. The property and method names should be separated by whitespace. If
    * the method names are absent, or are specified using the character `<code>*</code>`,
    * then the default method name <code>get</code><i>Prop</i> will be used,
    * where <i>Prop</i> is the capitalized property name.
    * @param description
    * a textual description of the property, for use in constructing
    * documentation or GUI tool-tips, etc.
    * @return descriptor which was created for the property
    */
   public PropertyDesc addReadOnly (String nameAndMethods, String description) {
      return addReadOnly (nameAndMethods, description, null);
   }

   // public PropertyDesc replace (
   // String nameAndMethods, String description,
   // Object defaultValue, String options)
   // {
   // PropertyDesc desc =
   // PropertyDesc.create (
   // nameAndMethods, myHostClass, description,
   // defaultValue, options, /*readOnly=*/false);
   // if (desc != null)
   // { replace (desc);
   // }
   // return desc;
   // }

   // public PropertyDesc replace (
   // String nameAndMethods, String description, Object defaultValue)
   // {
   // return replace (nameAndMethods, description, defaultValue, null);
   // }

   /**
    * Removes a specific property from this list.
    * 
    * @param desc
    * descriptor of the property to remove
    * @return true if the property was present and actually removed
    */
   public boolean remove (PropertyDesc desc) {
      return remove (desc.getName());
   }

   /**
    * Removes a specific named property from this list.
    * 
    * @param name
    * name of the property to remove
    * @return true if the property was present and actually removed
    */
   public boolean remove (String name) {
      if (myProps.remove (name) != null) {
         myHasNoInheritableP = hasNoInheritable();
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * Gets a descriptor for a specific named propertreturn myProps;y. If the named property is
    * not defined in this list, <code>null</code> is returned.
    * 
    * @param name
    * name of the property
    * @return descriptor for this property, if present
    */
   public PropertyDesc get (String name) {
      return myProps.get (name);
   }

   /**
    * Gets a descriptor for a specific property by index, where index describes
    * the numeric location of the property within this list.
    * 
    * @param idx
    * index of the property within this list
    * @return descriptor for this property
    * @throws ArrayIndexOutOfBoundsException
    * if index is not in the range [0, {@link #size size()}-1].
    */
   public PropertyDesc get (int idx) {
      int i = 0;
      for (PropertyDesc desc : myProps.values()) {
         if (i++ == idx) {
            return desc;
         }
      }
      return null;
   }

   /**
    * Recursively locates a property within this list and creates a handle for
    * it. Used by a host class to implement {@link
    * maspack.properties.HasProperties#getProperty HasProperties.getProperty}.
    * 
    * @param pathName
    * property name
    * @param host
    * host object exporting the property
    */
   static public Property getProperty (String pathName, HasProperties host) {
      String name;
      if (pathName.indexOf ('/') != -1) {
         throw new IllegalArgumentException ("property path '" + pathName
         + "' contains a '/'");
      }
      int dotIdx = pathName.indexOf ('.');
      if (dotIdx == -1) {
         name = pathName;
      }
      else if (dotIdx == 0) {
         throw new IllegalArgumentException ("property path '" + pathName
         + "' starts with a '.'");
      }
      else if (dotIdx == pathName.length() - 1) {
         throw new IllegalArgumentException ("property path '" + pathName
         + "' ends with a '.'");
      }
      else {
         name = pathName.substring (0, dotIdx);
      }
      PropertyInfo info = host.getAllPropertyInfo().get (name);
      if (info == null) {
         System.out.println ("Property " + name + " not found for "
         + host.getClass());
         return null;
      }
      if (dotIdx == -1) { // path contains just a regular property name, so
         // look for that
         return info.createHandle (host);
      }
      else { // path contains one or more subproperties. Check the first
         // property and make sure that (1) the value itself contains
         // properties, and (2) the value is returned by reference. If
         // so, recurse downward ...
         Object value = PropertyUtils.getValue (info, host);
         if (!(value instanceof HasProperties)) {
            System.out.println ("getProperty(" + pathName + ") for class "
            + host.getClass().getName() + ":");
            System.out.println ("   property '" + name
            + "' does not contain sub-properties");
            return null;
         }
         if (value != PropertyUtils.getValue (info, host)) {
            System.out.println ("getProperty(" + pathName + ") for class "
            + host.getClass().getName() + ":");
            System.out.println ("   property '" + name
            + "' does not return its value by reference");
            return null;
         }
         else {
            return ((HasProperties)value).getProperty (
               pathName.substring (dotIdx + 1));
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public Iterator<PropertyInfo> iterator() {
      return new InfoIterator();
   }

   /**
    * Returns an array of all the PropertyDesc objects in this list.
    * 
    * @return array of all PropertyDesc objects
    */
   public PropertyDesc[] toArray() {
      return myProps.values().toArray (new PropertyDesc[0]);
      // return myProps.toArray (new PropertyDesc[0]);
   }

   /**
    * Writes properties in this list to a PrintWriter. Only those properties for
    * which {@link maspack.properties.PropertyInfo#getAutoWrite
    * PropertyInfo.getAutoWrite} returns true are written. Properties are
    * written in the format
    * 
    * <pre>
    *    &lt;propertyName&gt; = &lt;value&gt;
    * </pre>
    * 
    * i.e., the property name, followed by an <code>=</code> sign, followed by
    * the property's current value.
    * 
    * @param host
    * class exporting the properties
    * @param pw
    * PrintWriter to which properties are written
    * @param fmt
    * Numeric formatting information. This is only used when the value to be
    * writtem is itself {@link maspack.util.Scannable Scannable}, in which case
    * it is passed to that value's {@link maspack.util.Scannable#write write}
    * method.
    * @param ref
    * Reference object. This is only used when the value to be
    * writtem is itself {@link maspack.util.Scannable Scannable}, in which case
    * it is passed to that value's {@link maspack.util.Scannable#write write}
    * method.
    * @throws IOException
    * if an I/O error occurred writing to the stream
    */
   public void writeProps (
      HasProperties host, PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      for (PropertyDesc desc : myProps.values()) {
         if (desc.getAutoWrite()) {
            PropertyMode mode = PropertyMode.Explicit;
            if (desc.isInheritable()) {
               mode = desc.getMode (host);
            }
            if (mode == PropertyMode.Inherited) {
               pw.print (desc.myName + ":" + mode);
            }
            else if (mode == PropertyMode.Explicit) {
               pw.print (desc.myName + "=");
               desc.writeValue (desc.getValue (host), pw, fmt, ref);
            }
         }
      }
   }

   private boolean excluded (String name, String[] excludeList) {
      if (excludeList != null) {
         for (int i=0; i<excludeList.length; i++) {
            if (name.equals(excludeList[i])) {
               return true;
            }
         }
      }
      return false;
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
      for (PropertyDesc desc : myProps.values()) {
         if (desc.getAutoWrite()) {
            boolean wrote = desc.writeIfNonDefault (host, pw, fmt, ref);
            wroteSomething |= wrote;
         }
      }
      return wroteSomething;
   }
   
   /**
    * Identical to {@link
    * #writeNonDefaultProps(HasProperties,PrintWriter,NumberFormat,Object)
    * writeNonDefaultProps(host,pw,fmt,obj)}
    * but also takes an optional array of property names which are
    * to be excluded.
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
    * @param exclude
    * optional array of property names which are to be excluded.
    * @throws IOException
    * if an I/O error occurred writing to the stream
    */
   public boolean writeNonDefaultProps (
      HasProperties host, PrintWriter pw, NumberFormat fmt, Object ref, 
      String[] exclude)
      throws IOException {
      boolean wroteSomething = false;
      for (PropertyDesc desc : myProps.values()) {
         if (desc.getAutoWrite()) {
            if (!excluded (desc.myName, exclude)) {
               wroteSomething |= desc.writeIfNonDefault (host, pw, fmt, ref);
            }
            // Object value = desc.getValue (host);
            // PropertyMode mode = PropertyMode.Explicit;

            // if (desc.isInheritable()) {
            //    mode = desc.getMode (host);
            // }
            // if (!excluded (desc.myName, exclude)) {
            //    if (mode == PropertyMode.Explicit &&
            //        (desc.getDefaultMode() == PropertyMode.Inherited ||
            //         !desc.valueEqualsDefault (value))) {
            //       if (!wroteSomething) {
            //          wroteSomething = true;
            //       }
            //       pw.print (desc.myName + "=");
            //       desc.writeValue (desc.getValue (host), pw, fmt);
            //    }
            //    else if (mode != desc.getDefaultMode()) {
            //       if (!wroteSomething) {
            //          wroteSomething = true;
            //       }
            //       pw.println (desc.myName + ":" + mode + " ");
            //    }
            // }
         }
      }
      return wroteSomething;
   }

   public void setDefaultModes (HasProperties host) {
      for (PropertyDesc desc : myProps.values()) {
         if (desc.isInheritable()) {
            desc.setMode (host, desc.getDefaultMode());
         }
      }
   }

   public void setDefaultValues (HasProperties host) {
      for (PropertyDesc desc : myProps.values()) {
         if (!desc.isReadOnly()) {
            desc.setValue (host, desc.getDefaultValue());
         }
      }
   }

   public void setDefaultValuesAndModes (HasProperties host) {
      setDefaultValues (host);
      setDefaultModes (host);
   }


//   public boolean writeNonDefaultProps (
//      HasProperties host, PrintWriter pw, NumberFormat fmt) throws IOException {
//      return writeNonDefaultProps (host, pw, fmt, null);
//   }

   protected void scanQualifier (
      ReaderTokenizer rtok, HasProperties host, PropertyDesc desc)
      throws IOException {
      if (!desc.isInheritable()) {
         throw new IOException (
            "qualifier inappropriate for non-inheritable property '"
            + desc.getName() + "', line " + rtok.lineno());
      }
      String qualifier = rtok.scanWord();
      if (qualifier.equals ("Inherited")) {
         desc.setMode (host, PropertyMode.Inherited);
      }
      else if (qualifier.equals ("Inactive")) {
         desc.setMode (host, PropertyMode.Inactive);
      }
      else {
         throw new IOException ("unrecognized property qualifier " + qualifier
         + ", line " + rtok.lineno());
      }
   }

   /**
    * Scans a property from a ReaderTokenizer. The input is expected to be
    * arranged as
    * 
    * <pre>
    *    &lt;propertyName&gt; = &lt;value&gt;
    * </pre>
    * 
    * i.e., the property name, followed by an <code>=</code> sign, followed by
    * the property's value.
    * 
    * <p>
    * If the first token read is not a word, or if it does not correspond to the
    * name of a known property on this list, then the token is pushed back and
    * this routine returns false. Otherwise, the remaining input is expected to
    * consist of an <code>=</code> equal sign followed by whatever input is
    * required to specify the property, and an exception is thrown if this is
    * not the case.
    * 
    * @param host
    * Object exporting the property
    * @param rtok
    * Tokenizer stream from which the property is read
    * @return true if a property known to this list was successfully read, or
    * false if the first input token is not a word or does not identify a
    * property known to this list.
    * @throws IOException
    * if an I/O error occurred or if the input does not conform to the required
    * format.
    */
   public boolean scanProp (HasProperties host, ReaderTokenizer rtok)
      throws IOException {
      if (rtok.nextToken() != ReaderTokenizer.TT_WORD) {
         rtok.pushBack();
         return false;
      }
      PropertyDesc desc = get (rtok.sval);
      if (desc == null) {
         rtok.pushBack();
         return false;
      }
      int tok = rtok.nextToken();
      if (tok == '=') {
         desc.setValue (host, desc.scanValue (rtok));
      }
      else if (tok == ':') {
         scanQualifier (rtok, host, desc);
      }
      else {
         throw new IOException ("Expected '=' or ':', got " + rtok);
      }
      return true;
   }

   public void scanNamedProp (
      HasProperties host, String name, ReaderTokenizer rtok) throws IOException {
      PropertyDesc desc = get (name);
      if (desc == null) {
         throw new IOException (
            "Property '"+name+"' not found in host "+host.getClass()+", "+rtok);
      }
      int tok = rtok.nextToken();
      if (tok == '=') {
         desc.setValue (host, desc.scanValue (rtok));
      }
      else if (tok == ':') {
         scanQualifier (rtok, host, desc);
      }
      else {
         throw new IOException ("Expected '=' or ':', got " + rtok);
      }
   }

   /**
    * {@inheritDoc}
    */
   public int size() {
      return myProps.size();
   }

   // public void initialize (Class hostClass)
   // {
   // for (PropertyDesc desc : myProps)
   // { desc.initialize (hostClass);
   // }
   // }

   private boolean hasNoInheritable() {
      for (PropertyDesc desc : myProps.values()) {
         if (desc.isInheritable() ||
             CompositeProperty.class.isAssignableFrom (desc.getValueClass())) {
            return false;
         }
      }
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasNoInheritableProperties() {
      return myHasNoInheritableP;
   }

}
