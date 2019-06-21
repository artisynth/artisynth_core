/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import java.util.*;
import java.lang.reflect.*;
import maspack.util.*;
import maspack.matrix.Vector;
import maspack.matrix.Vectori;

public class PropertyUtils {

   public static String[] maybeInitializeValues (
      Property prop, CompositeProperty prevValue) {

      Class<?> primaryClass = prop.getInfo().getValueClass();
      Object value = prop.get();
      if (!(value instanceof CompositeProperty)) {
         return new String[0];
      }
      CompositeProperty curValue = (CompositeProperty)value;
      Method m = null;
      try {
         m = value.getClass().getMethod (
            "initializePropertyValues", primaryClass);
         if (m.getReturnType() != String[].class) {
            m = null;
         }
      }
      catch (Exception e) {
         // do nothing, m will remain null
      }
      if (m != null) {
         if (prop instanceof EditingProperty) {
            EditingProperty eprop = (EditingProperty)prop;
            HostList hostList = eprop.getHostList();
            Object[] values = hostList.getAllValues (eprop.getCell());
            for (Object val : values) {
               if (val instanceof CompositeProperty) {
                  try {
                     return (String[])m.invoke (val, prevValue);
                  }
                  catch (Exception e) {
                     System.out.println (
                        "Error invoking initializeValues(): " + e);
                     return null;
                  }
               }
            }
         }
         else {
            try {
               return (String[])m.invoke (curValue, prevValue);
            }
            catch (Exception e) {
               System.out.println ("Error invoking initializeValues(): " + e);
               return null;
            }
         }
      }
      return new String[0];
   }

   public static Class<?>[] findCompositePropertySubclasses (
      PropertyInfo info) {
      List<Class<?>> allowedTypes = info.getAllowedTypes();
      if (allowedTypes != null) {
         return allowedTypes.toArray(new Class<?>[0]);
      }
      Class<?> clazz = info.getValueClass();
      Method m = null;
      try {
         m = clazz.getMethod ("getSubClasses");
      }
      catch (Exception e) {
         // do nothing, m will remain null
      }
      if (m != null && Modifier.isStatic (m.getModifiers())) {
         Object obj = null;
         try {
            obj = m.invoke (null);
         }
         catch (Exception e) {
            // do nothing, obj will remain null
         }
         if (obj instanceof Class[]) {
            return (Class[])obj;
         }
      }
      return null;
   }

   /**
    * Propagate all explicit properties contained in a particular host to any
    * descendant nodes in the associated hierarchy.
    * 
    * @param host
    * host containing the properties to be propagated
    */
   static void propagateExplicitProperties (HasProperties host) {
      if (!host.getAllPropertyInfo().hasNoInheritableProperties()) {
         PropTreeCell explicit = new PropTreeCell();
         explicit.addExplicitPropertyTree (host);
         if (explicit.hasChildren()) {
            HierarchyNode node = getHierarchyNode (host);
            if (node != null) {
               explicit = explicit.extendToHierarchyNode (host);
               propagateProperties (node, explicit);
            }
         }
      }
   }

   /**
    * Propagate a specific property contained in a particular host to any
    * descendant nodes in the associated hierarchy.
    * 
    * @param host
    * host containing the properties to be propagated
    * @param propName
    * name of the property
    * @param val
    * value to be propagated
    */
   public static void propagateExplicitProperty (
      HasProperties host, String propName, Object val) {
      HierarchyNode node = getHierarchyNode (host);
      // System.out.println ("hierarchy node = " + node);
      if (node != null && node.hasChildren()) {
         PropertyInfo info = getPropertyInfo (host, propName);
         PropTreeCell explicit = new PropTreeCell();
         explicit.addChild (info, val);
         PropTreeCell cellExplicit = explicit.extendToHierarchyNode (host);
         propagateProperties (node, cellExplicit);
      }
   }

   /**
    * Returns the hierarchy node associated with a particular property host.
    * Typically, this will be the host itself, unless the host is itself
    * property, in which case the node is found by climbing up the property
    * hierarchy.
    * 
    * @param host
    * host for which the hierarchy node is desired
    * @return hierarchy node associated with the host
    */
   static HierarchyNode getHierarchyNode (HasProperties host) {
      Object obj = host;
      while (obj != null) {
         // System.out.println ("HN " + obj.getClass());
         if (obj instanceof HierarchyNode) {
            return (HierarchyNode)obj;
         }
         else if (obj instanceof CompositeProperty) {
            obj = ((CompositeProperty)obj).getPropertyHost();
         }
         else {
            return null;
         }
      }
      return null;
   }

   /**
    * Sets the value of an inheritable property to an inherited value. The
    * reason for using this routine instead of the property's <code>set</code>
    * method is to prevent the property's mode from being automatically set to
    * {@link maspack.properties.PropertyMode#Explicit Explicit}.
    * 
    * @param info
    * property information
    * @param host
    * host of the property
    * @param value
    * value to set it to
    */
   public static void setInheritedValue (
      PropertyInfo info, HasProperties host, Object value) {
      if (info instanceof PropertyDesc) {
         PropertyDesc desc = (PropertyDesc)info;
         // ModeObject modeObj = desc.getPropertyMode(host);

         PropertyMode mode;

         {
            if ((mode = desc.getMode (host)) == PropertyMode.Explicit) {
               throw new InternalErrorException (
                  "inherited value being set on explicit property");
            }
            desc.setMode (host, PropertyMode.Inactive);
            if (value != Property.VoidValue) {
               desc.setValue (host, value);
            }
            else if (desc.myDefaultValue != Property.VoidValue) {
               desc.setValue (host, desc.myDefaultValue);
            }
            desc.setMode (host, mode);
         }
      }
      else {
         InheritableProperty prop = createInheritableHandle (info, host);
         PropertyMode mode = prop.getMode();
         if (mode == PropertyMode.Explicit) {
            throw new InternalErrorException (
               "inherited value being set on explicit property");
         }
         prop.setMode (PropertyMode.Inactive);
         // SETTING INHERITED
         if (value != Property.VoidValue) {
            prop.set (value);
         }
         else {
            Object defaultValue = prop.getInfo().getDefaultValue();
            if (defaultValue != Property.VoidValue) {
               prop.set (defaultValue);
            }
         }
         prop.setMode (mode);
      }
   }

   /**
    * Recursively propagates the values contained within a property tree
    * throughout the descendants of a hierarchy node.
    * 
    * @param node
    * hierarchy node whose descendants are to be updated
    * @param props
    * root cell of the property tree
    */
   private static void propagateProperties (
      HierarchyNode node, PropTreeCell props) {
      //System.out.println ("propagate properites " + node.getClass());
      if (node.hasChildren()) {
         Iterator<? extends HierarchyNode> it = node.getChildren();
         while (it.hasNext()) {
            HierarchyNode child = it.next();
            PropTreeCell propsForChild = props;
            if (child instanceof HasProperties) {
               //System.out.println ("updated " + child.getClass());
               propsForChild =
                  props.updateTreeValuesInHost (
                     (HasProperties)child, /* reduce= */true);
            }
            if (child.hasChildren() && propsForChild != null) {
               propagateProperties (child, propsForChild);
            }
         }
      }
   }

   /**
    * Wrapper routine to get a property from a host and throw an <code>
    * InternalErrorException</code>
    * if it is not found.
    * 
    * @param host
    * property host
    * @param propName
    * property name
    * @return the property's handle
    */
   static Property getProperty (HasProperties host, String propName) {
      Property prop = host.getProperty (propName);
      if (prop == null) {
         throw new InternalErrorException ("property '" + propName
         + "' not found for " + host.getClass());
      }
      return prop;
   }

   /**
    * Wrapper routine to create an InheritableHandle and throw an exception if
    * there is a propblem.
    * 
    */
   static InheritableProperty createInheritableHandle (
      PropertyInfo info, HasProperties host) {
      try {
         return (InheritableProperty)info.createHandle (host);
      }
      catch (ClassCastException e) {
         throw new InternalErrorException ("Property '" + info.getName()
         + "' is not inheritable");
      }
   }

   /**
    * Wrapper routine to get property information from a host and throw an
    * <code> InternalErrorException</code> if the property is not found.
    * 
    * @param host
    * property host
    * @param propName
    * property name
    * @return the property's information structure
    */
   static PropertyInfo getPropertyInfo (HasProperties host, String propName) {
      PropertyInfo info = host.getAllPropertyInfo().get (propName);
      if (info == null) {
         throw new InternalErrorException ("property '" + propName
         + "' not found for " + host.getClass());
      }
      return info;
   }

   /**
    * Updates a composite property when it is introduced to a host. The host
    * should call this routine whenever such an introduction occurs, which will
    * typically occurs within the associated <code>set</code> routine. The
    * routine will update the property and host information for the composite
    * property. Also, if the host is associated with a node hierarchy, then (1)
    * any inherited property values within the composite will be updated to
    * reflect the explicit settings of ancestor nodes, and (2) any explicit
    * property values will be propagated to descendant nodes.
    * 
    * @param host
    * host to which the composite property is being introduced
    * @param propName
    * name of the composite property within the host
    * @param oldObj
    * previous composite property, if any, or <code>null</code> otherwise
    * @param newObj
    * composite property which is being introduced
    */
   public static void updateCompositeProperty (
      HasProperties host, String propName, CompositeProperty oldObj,
      CompositeProperty newObj) {
      if (oldObj != null) {
         propagateRemoval (oldObj);
         oldObj.setPropertyHost (null);
      }
      if (newObj != null) {
         newObj.setPropertyHost (host);
         PropertyInfo info = getPropertyInfo (host, propName);
         newObj.setPropertyInfo (info);
         updateInheritedInHost (newObj);
         propagateExplicitProperties (newObj);

      }
   }

   /**
    * Updates the hierarchy to reflect property changes in a particular
    * composite property. It is assumed that the composite property is already
    * attached to the hierarchy, and that the changes have not been made through
    * the property value accesssors (since otherwise they would already have
    * been propagated). This routine will update and propagate all inherited
    * values, as well as all explicit values.
    * 
    * @param obj
    * composite property object
    */
   public static void updateCompositeProperty (CompositeProperty obj) {
      updateAndPropagateInheritedProperties (obj);
      propagateExplicitProperties (obj);
   }

   /**
    * Find the inherited value for a specific property. The inherited value, if
    * any, is determined from the first explicitly set value within the
    * ancestors of the hierarchy node associated with the property's host.
    * 
    * @param host
    * property's host
    * @param info
    * property's information structure
    * @return inherited value for the property, or
    * <code>Property.VoidValue</code> if no such value is found
    */
   static Object findInheritedValue (
      HasProperties host, PropertyInfo info) {
      HierarchyNode node = getHierarchyNode (host);
      if (node != null) {
         PropTreeCell inherited = new PropTreeCell();
         inherited.addChild (info, Property.VoidValue);
         PropTreeCell nodeInherited = inherited.extendToHierarchyNode (host);
         return nodeInherited.findInheritedValue (node);
         // return inherited.myFirstChild.myData.myValue;
      }
      return Property.VoidValue;
   }

   /**
    * Updates the all inherited properties within a specified host. This
    * involves finding the associated hierarchy node (if any) and then searching
    * the ancestors of that node for the first explicit instances of any
    * inherited properties. The explicit instances provide the inherited values.
    * 
    * <p>
    * If the host has inherited properties, then this routine returns a property
    * tree giving the inherited values for these properties, which can be
    * reused to set inherited values for another host with the same inherited
    * properties and the same parent.
    * 
    * @param host
    * property host to be updated
    * @return property tree giving inherited property values, or null if there
    * are no inherited properties.
    */
   private static PropTreeCell updateInheritedInHost (HasProperties host) {
      if (host.getAllPropertyInfo() == null) {
         throw new InternalErrorException ("host " + host.getClass()
         + " has no property info");
      }
      if (!host.getAllPropertyInfo().hasNoInheritableProperties()) {
         PropTreeCell inherited = new PropTreeCell();
         inherited.addNonexplicitPropertyTree (host);
         if (inherited.hasChildren()) {
            HierarchyNode node = getHierarchyNode (host);
            if (node != null) {
               PropTreeCell nodeInherited =
                  inherited.extendToHierarchyNode (host);
               nodeInherited.inheritTreeValuesFromHierachy (node);
               inherited.updateTreeValuesInHost (host, /* reduce= */false);
               return inherited;
            }
         }
      }
      return null;
   }

   /**
    * Updates the inherited properties within a specified CompositeProperty, in
    * the context of a specified host and property name. The CompositeProperty
    * does not need to be attached to the host in order to do the update.
    * Instead, it will be temporarily attached and then detached.
    * 
    * @param cprop
    * CompositeProperty to update
    * @param host
    * Host for this composite property
    * @param cpropName
    * Name for the composite property within hosthost
    */
   public static void updateInheritedProperties (
      CompositeProperty cprop, HasProperties host, String cpropName) {
      PropertyInfo info = getPropertyInfo (host, cpropName);
      if (info == null) {
         throw new IllegalArgumentException ("Property '" + cpropName
         + "' not found within host");
      }
      HasProperties savedHost = cprop.getPropertyHost();
      PropertyInfo savedInfo = cprop.getPropertyInfo();
      cprop.setPropertyHost (host);
      cprop.setPropertyInfo (info);
      updateInheritedInHost (cprop);
      cprop.setPropertyHost (savedHost);
      cprop.setPropertyInfo (savedInfo);
   }

   /**
    * Updates the all inherited properties within a specified hierarchy node and
    * all its descendants. This routine should be called whenever a node is
    * added to the hierarchy.
    * 
    * <p>
    * If the node itself has inherited properties, then this routine returns a
    * property tree giving the inherited values for thsese properties, which can
    * be resused to set inherited values for another node with the same
    * inherited properties and the same parent.
    * 
    * @param node
    * hierarchy node which needs updating
    * @return property tree giving inherited property values for the node, or
    * null if the node does not have inherited values.
    */
   public static PropTreeCell updateAllInheritedProperties (HierarchyNode node) {
      PropTreeCell inherited = null;
      if (node instanceof HasProperties) {
         inherited = updateInheritedInHost ((HasProperties)node);
      }
      if (node.hasChildren()) {
         Iterator<? extends HierarchyNode> it = node.getChildren();
         while (it.hasNext()) {
            updateAllInheritedProperties (it.next());
         }
      }
      return inherited;
   }

   /**
    * Updates the all inherited properties within a specified hierarchy node and
    * all its descendants. Inherited values for the node itself are supplied by
    * a property tree which has been precomputed (most likely using a call to
    * {@link #updateAllInheritedProperties(HierarchyNode)
    * updateInheritedProperties(node)}). It is the responsibility of the caller
    * to ensure that this property tree is consistent with the properties
    * exported by node.
    * 
    * @param node
    * hierarchy node which needs updating
    * @param inherited
    * property tree giving inherited values for node
    */
   public static void updateInheritedProperties (
      HierarchyNode node, PropTreeCell inherited) {
      if (node instanceof HasProperties && inherited != null) {
         inherited.updateTreeValuesInHost (
            (HasProperties)node, /* reduce= */false);
      }
      if (node.hasChildren()) {
         Iterator<? extends HierarchyNode> it = node.getChildren();
         while (it.hasNext()) {
            updateAllInheritedProperties (it.next());
         }
      }
   }

   /**
    * Updates a node hierarchy to reflect the removal of a property host. This
    * involves resetting any inherited properties in the descendant nodes which
    * depended on explicitly set properties in the removed host.
    * 
    * @param host
    * property host which has been removed
    */
   public static void propagateRemoval (HasProperties host) {
      if (!host.getAllPropertyInfo().hasNoInheritableProperties()) {
         PropTreeCell explicit = new PropTreeCell();
         explicit.addExplicitPropertyTree (host);
         if (explicit.hasChildren()) {
            HierarchyNode node = getHierarchyNode (host);
            if (node != null) {
               explicit = explicit.extendToHierarchyNode (host);
               explicit.setLeafValuesVoid();
               explicit.inheritTreeValuesFromHierachy (node);
               propagateProperties (node, explicit);
            }
         }
      }
   }

   /**
    * Updates all inherited properties within a host, and then propagates the
    * resulting values down throughout a node hierarchy.
    * 
    * @param host
    * property host which has been removed
    */
   public static void updateAndPropagateInheritedProperties (HasProperties host) {
      if (!host.getAllPropertyInfo().hasNoInheritableProperties()) {
         PropTreeCell inherited = new PropTreeCell();
         inherited.addNonexplicitPropertyTree (host);
         if (inherited.hasChildren()) {
            HierarchyNode node = getHierarchyNode (host);
            if (node != null) {
               PropTreeCell nodeInherited =
                  inherited.extendToHierarchyNode (host);
               nodeInherited.setLeafValuesVoid();
               nodeInherited.inheritTreeValuesFromHierachy (node);
               inherited.updateTreeValuesInHost (host, /* reduce= */false);
               propagateProperties (node, inherited);
            }
         }
      }
   }

   static Object getValue (PropertyInfo info, HasProperties host) {
      if (info instanceof PropertyDesc) {
         return ((PropertyDesc)info).getValue (host);
      }
      else {
         return info.createHandle (host).get();
      }
   }

   static Range getRange (PropertyInfo info, HasProperties host) {
      if (info instanceof PropertyDesc) {
         return ((PropertyDesc)info).getRange (host);
      }
      else {
         return info.createHandle (host).getRange();
      }
   }

   static void setValue (PropertyInfo info, HasProperties host, Object value) {
      if (info instanceof PropertyDesc) {
         ((PropertyDesc)info).setValue (host, value);
      }
      else {
         info.createHandle (host).set (value);
      }
   }

//   static Object validateValue (
//      PropertyInfo info, HasProperties host, Object value, StringHolder errMsg) {
//      if (info instanceof PropertyDesc) {
//         return ((PropertyDesc)info).validateValue (host, value, errMsg);
//      }
//      else {
//         return info.createHandle (host).validate (value, errMsg);
//      }
//   }

   static PropertyMode getMode (PropertyInfo info, HasProperties host) {
      if (info instanceof PropertyDesc) {
         return ((PropertyDesc)info).getMode (host);
      }
      else {
         return createInheritableHandle (info, host).getMode();
      }
   }

   static void setMode (PropertyInfo info, HasProperties host, PropertyMode mode) {
      if (info instanceof PropertyDesc) {
         ((PropertyDesc)info).setMode (host, mode);
      }
      else {
         createInheritableHandle (info, host).setMode (mode);
      }
   }

   /**
    * Creates a default instance of a composite property.
    * 
    * @param info
    * information about the property
    * @param host
    * property host for this property
    * @return new composite property instance
    */
   public static CompositeProperty createInstance (
      PropertyInfo info, HasProperties host) {
      Class<?> valueClass = info.getValueClass();
      try {
         if (info instanceof PropertyDesc) {
            PropertyDesc desc = (PropertyDesc)info;
            return (CompositeProperty)desc.createInstance (host);
         }
         else {
            return (CompositeProperty)valueClass.newInstance();
         }
      }
      catch (ClassCastException e) {
         throw new InternalErrorException ("class " + valueClass
         + " is not an instance of CompositeProperty");
      }
      catch (Exception e) {
         throw new InternalErrorException ("could not create instance of "
         + valueClass);
      }
   }

   public static boolean propertiesMatch (PropertyInfo info1, PropertyInfo info2) {
      if (!info1.getName().equals (info2.getName())) {
         return false;
      }
      // XXX should we use equality of instanceof here???
      if (info1.getValueClass() != info2.getValueClass()) {
         return false;
      }
      if (info1.isInheritable() != info2.isInheritable()) {
         return false;
      }
      if (info1.isReadOnly() != info2.isReadOnly()) {
         return false;
      }
      if (info1.getDimension() != info2.getDimension()) {
         return false;
      }
      return true;
   }

   /**
    * Returns true if two objects are equal, <i>including</i> if they are both
    * equal to <code>null</code>.
    * 
    * @param val1
    * first value to compare
    * @param val2
    * second value to compare
    * @return true if both values are null or if val1.equals(val2) == true
    */
   public static boolean equalValues (Object val1, Object val2) {
      if (val1 == null && val2 == null) {
         return true;
      }
      else if (val1 != null && val2 != null) {
         if (val1 instanceof Vector && val2 instanceof Vector) {
            return ((Vector)val1).equals ((Vector)val2);
         }
         else if (val1 instanceof Vectori && val2 instanceof Vectori) {
            return ((Vectori)val1).equals ((Vectori)val2);
         }
         else {
            return val1.equals (val2);
         }
      }
      else {
         return false;
      }
   }

   public static LinkedList<Property> createProperties (HasProperties host) {
      LinkedList<Property> propList = new LinkedList<Property>();
      for (PropertyInfo info : host.getAllPropertyInfo()) {
         propList.add (info.createHandle (host));
      }
      return propList;
   }

//    public static LinkedList<Property> createProperties (Class cls) {
//       LinkedList<Property> propList = new LinkedList<Property>();
//       if (!HasProperties.class.isAssignableFrom (cls)) {
//          throw new IllegalArgumentException (
//             "Class +"+cls+" not an instanceof of HasProperties");
//       }
//       PropertyInfoList infoList = null;
//       try {
//          infoList = PropertyList.findPropertyInfoList (cls);
//       }
//       catch (Exception e) {
//         throw new IllegalArgumentException (
//            "Cannot find PropertyInfoList for "+cls+":\n"+
//            e.getMessage());
//       }
//       for (PropertyInfo info : infoList) {
//          propList.add (info.createHandle (host));
//       }
//       return propList;
//    }

   public static PropertyMode setModeAndUpdate (
      HasProperties host, String propName, PropertyMode oldMode,
      PropertyMode newMode) {
      if (oldMode != newMode) {
         if (newMode == PropertyMode.Explicit) {
            PropertyInfo info = getPropertyInfo (host, propName);
            PropertyUtils.propagateExplicitProperty (host, propName, getValue (
               info, host));
         }
         else if (oldMode == PropertyMode.Explicit) {
            PropertyInfo info = getPropertyInfo (host, propName);
            Object value = PropertyUtils.findInheritedValue (host, info);
            oldMode = PropertyMode.Inactive; // block propagation in set ...
            // SETTING INHERITED
            if (value != Property.VoidValue) {
               setValue (info, host, value);
            }
            else {
               Object defaultValue = info.getDefaultValue();
               if (defaultValue != Property.VoidValue) {
                  setValue (info, host, defaultValue);
               }
            }
            PropertyUtils.propagateExplicitProperty (host, propName, value);
         }
      }
      return newMode;
   }

   public static boolean isConnectedToHierarchy (Property prop) {
      HasProperties host = prop.getHost();
      while (host != null) {
         if (host instanceof HierarchyNode) {
            return true;
         }
         else if (host instanceof CompositeProperty) {
            CompositeProperty cprop = (CompositeProperty)host;
            HasProperties superHost = cprop.getPropertyHost();

            if (superHost != null) { // make sure the superHost refers back to
                                       // cprop
               PropertyInfo info = cprop.getPropertyInfo();
               Property superProp = superHost.getProperty (info.getName());
               if (superProp == null || superProp.get() != cprop) {
                  return false;
               }
            }
            host = superHost;
         }
         else {
            host = null;
         }
      }
      return false;
   }

   public static PropertyMode propagateValue (
      HasProperties host, String propName, Object value, PropertyMode mode) {
      if (mode != PropertyMode.Inactive) {
         mode = PropertyMode.Explicit;
         // System.out.println ("propagateExplicitProperty");
         PropertyUtils.propagateExplicitProperty (host, propName, value);
      }
      return mode;
   }

   public static Object validValue (Object value, StringHolder errMsg) {
      if (errMsg != null) {
         errMsg.value = null;
      }
      return value;
   }

   public static Object illegalValue (String err, StringHolder errMsg) {
      if (errMsg != null) {
         errMsg.value = err;
      }
      return Property.IllegalValue;
   }

   public static Object correctedValue (
      Object value, String err, StringHolder errMsg) {
      if (errMsg != null) {
         errMsg.value = err;
      }
      return value;
   }

}
