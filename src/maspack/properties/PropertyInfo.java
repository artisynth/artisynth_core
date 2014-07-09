/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import java.io.*;

import maspack.util.*;

/**
 * Provides property information that is static with respect to a property's
 * exporting class.
 */
public interface PropertyInfo {
   /**
    * Describes conditions under which this property should be interactively
    * edited.
    */
   public enum Edit {
      /**
       * Can always be interactively edited.
       */
      Always,

      /**
       * Can be interactively edited only for a single host at a time
       */
      Single,

      /**
       * Should not be interactively edited.
       */
      Never
   }

   public enum ExpandState {
      /*
       * Indicates, where appropriate, a widget that can be expanded or
       * contracted in order to save GUI space, and which is initially
       * contracted.
       */    
      Contracted,
      
      /*
       * Indicates, where appropriate, a widget that can be expanded or
       * contracted in order to save GUI space, and which is initially
       * expanded.
       */    
      Expanded,
      
      /*
       * Indicates a widget that cannot be expanded or contracted. This is the
       * default setting.
       */    
      Unexpandable
   }
 
   /**
    * Returns the name of the property.
    * 
    * @return property name
    */
   public String getName();

   /**
    * Returns true if the property is read-only. A property is read-only if it's
    * value cannot be set using the the {@link maspack.properties.Property#set
    * Property.set} method. {@link #getAutoWrite getAutoWrite} should return
    * false for read-only properties, since it makes no sense to try to save
    * such properties in persistent storage.
    * 
    * @return true if the property is read-only
    */
   public boolean isReadOnly();

   /**
    * Returns true if the property has a restricted value range. If the property
    * has a restricted range, the {@link maspack.properties.Property#getRange} 
    * method can be used to obtain a <code>Range</code> object to
    * determine whether or not a particular value is legal.
    * 
    * @return true if the property has a restricted value range
    */
   public boolean hasRestrictedRange();

   /**
    * Returns true if the property value may be null.
    * 
    * @return true if the property value may be null
    */
   public boolean getNullValueOK();

   /**
    * Returns a text description of the property. This can be used for
    * generating documentation, GUI tool tips, etc.
    * 
    * @return text description of the property
    */
   public String getDescription();

   /**
    * Returns a format string used to convert numeric components of the
    * property's value into text. This string may be null if there is no numeric
    * data associated with the property.
    * 
    * @return numeric format string
    */
   public String getPrintFormat();

   /**
    * Returns the class type for the property's value.
    * 
    * @return property value class type
    */
   public Class getValueClass();

   /**
    * Returns the host class of the property.
    * 
    * @return host class of the property
    */
   public Class getHostClass();

   /**
    * Returns true if auto-writing is enabled for this property. Auto-writing
    * means that the property and it's value should be written out by the method
    * {@link maspack.properties.PropertyList PropertyList.writeProps}.
    * 
    * @return true if auto-writing is enabled for this property
    */
   public boolean getAutoWrite();

   /**
    * Returns information describing the conditions under which this property
    * should be interactively edited.
    * 
    * @return conditions for interactive editing.
    */
   public Edit getEditing();

   /*
    * Returns information about whether the widget should be able to expand or
    * contract in order to save GUI space, and if so whether it should be
    * initially expanded or contracted. This information is generally relevant
    * only to widgets that contain sub-widgets, such as those for composite
    * properties.
    */
   public ExpandState getWidgetExpandState();
   
   /**
    * Returns the default value for the property, or
    * <code>Property.VoidValue</code> if there is not default value.
    * 
    * @return property's default value
    */
   public Object getDefaultValue();

   /**
    * Returns a default numeric range for this property, if any. If there
    * is no default numeric range, null is returned.
    *
    * <p> If a <code>getRangeMethod</code> is not defined for the property, and
    * the property has a numeric type, then the default numeric range is
    * returned by the property's
    * {@link maspack.properties.Property#getRange getRange} method. The
    * default numeric range is also used to determine bounds on slider widgets
    * for manipulating the property's value, in case the upper or lower limits
    * returned by the <code>getRange</code> method are unbounded.
    * 
    * @return default numeric range for this property
    */
   public NumericInterval getDefaultNumericRange();

   /**
    * Writes a value of the type associated with the property out to a
    * PrintWriter.
    * 
    * @param value
    * value to be written
    * @param pw
    * PrintWriter to which value is written
    * @param fmt
    * Numeric formatting information. This is only used when the value to be
    * writtem is itself {@link maspack.util.Scannable Scannable}, in which case
    * it is passed to that value's {@link maspack.util.Scannable#write write}
    * method.
    * @throws IOException
    * if an I/O error occurred, or the value has a type which PropertyInfo does
    * not know about
    */
   public void writeValue (Object value, PrintWriter pw, NumberFormat fmt)
      throws IOException;

   /**
    * Scans a value of the type associated with the property from a
    * ReaderTokenizer.
    * 
    * @param rtok
    * ReaderTokenizer supplying input tokens used to specify the property value
    * @throws IOException
    * if the input is not in the correct format, an I/O error occurred, or the
    * value has a type which PropertyInfo does not know about
    */
   public Object scanValue (ReaderTokenizer rtok) throws IOException;

   /**
    * Creates a handle to the property for a specified host object. The host
    * must export the property (i.e., it must be an instance of the class
    * returned by {@link #getHostClass getHostClass}).
    * 
    * @return property handle
    * @throws IllegalArgumentException
    * if the host does not export the specified property
    */
   public Property createHandle (HasProperties host);

   /**
    * Returns true if a specified value equals the default value of the
    * property.
    * 
    * @param value
    * value to check against the default
    * @return true if the value equals the default value
    */
   public boolean valueEqualsDefault (Object value);

   /**
    * Returns true if the property is a {@link
    * maspack.properties.InheritableProperty inheritable} property.
    */
   public boolean isInheritable();

   // /**
   // * Returns true if the value associated with this property is immutable.
   // * Immutable objects can not be changed after creation, and
   // * so can be shared without side-effects by different components
   // * of a program.
   // *
   // * @return true if this object's value is immutable.
   // */
   // public boolean isImmutable();

   /**
    * Returns the default inheritance mode for the property. If the property is
    * not inheritable, then this method should return PropertyMode.Explicit.
    * 
    * @return default inheritance mode
    */
   public PropertyMode getDefaultMode();

   /**
    * Returns the numeric dimension of this property, or -1 if the property is
    * not numeric or does not have a fixed dimension.
    */
   public int getDimension();

   /**
    * Returns true if the value of this property is sharable among several
    * hosts. This implies that the host does not maintain an internal copy of
    * the value. Changes to the value itself may therefore effect several hosts.
    * 
    * @return true if the property value can be shared by several hosts
    */
   public boolean isSharable();
}
