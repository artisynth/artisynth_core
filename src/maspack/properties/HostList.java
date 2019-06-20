/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import java.util.*;
import maspack.util.InternalErrorException;
import maspack.util.Range;
import maspack.util.Clonable;

/**
 * Container class for a list of property hosts that can be queried for common
 * values, backed-up, etc.
 *
 *<p> HostLists are used for setting or querying elements of a property
 * hierarchy across multiple objects, mostly commonly through an
 * EditingProperty. The property hierarchy itself is described by a hierarchy
 * of PropTreeCells, which is separate from the HostList. The HostList contains
 * a list of PropTreeData objects, one per host, each of which forms the root
 * of a host-specific tree mirroring the hierarchy. This mirror hierarchy is
 * formed using PropTreeData objects instead of PropTreeCells in order to save
 * memory and boost efficiency (since there might in practice by a large number
 * of hosts). The purpose of the mirror hierarchy is to (1) provide
 * host-specific information needed to set or query property values on
 * different objects, and (2) to store object-specific backup information.
 *
 * <p>Typically, the master property hierarchy is described by the PropTreeCells
 * and looks like this:
 * <pre>
 * {@code
 *
 *                          ----------------
 *                          | PropTreeCell |
 *   ------------------------- firstChild  |
 *   |                      |--------------|
 *   |                      | PropTreeData |
 *   |                      |              |
 *   |                      | value = CompositeProperty.class
 *   |                      |              |
 *   |                      ----------------
 *   |
 *   |  ----------------    ----------------    ----------------
 *   |  | PropTreeCell |    | PropTreeCell |    | PropTreeCell |    
 *   +->|         next ---->|         next ---->|         next ---> null
 *      |--------------|    |--------------|    |--------------|
 *      | PropTreeData |    | PropTreeData |    | PropTreeData |
 *      |              |    |              |    |              |
 *      | value=xxx    |    | value=xxx    |    | value=xxx    |
 *      |              |    |              |    |              |
 *      ----------------    ----------------    ----------------
 * }
 * </pre>
 * while each corresponding mirror hierarchy looks like this:
 * <pre>
 * {@code
 *
 *                          ----------------
 *                          | PropTreeData |
 *                          |              |
 *   -------------------------- subData    |
 *   |                      |              |
 *   |                      | value=backup |
 *   |                      |   host = current composite property value
 *   |                      ----------------
 *   |
 *   |  --------------------------------------------------------
 *   +->|              0  |               1  |              2  |
 *      --------------------------------------------------------
 *              |                   |                   |
 *              V                   V                   V
 *      ----------------    ----------------    ----------------
 *      | PropTreeData |    | PropTreeData |    | PropTreeData |
 *      |              |    |              |    |              |
 *      | info         |    | info         |    | info         |
 *      | value=backup |    | value=backup |    | value=backup |
 *      |              |    |              |    |              |
 *      ----------------    ----------------    ----------------
 * }
 * </pre>
 * 
 * Within the master hierarchy, the value at non-leaf nodes is set to
 * CompositeProperty.class to indicate the presence of a composite property;
 * storing real value here makes little sense these vary among objects.
 * Likewise, within the mirror hierarchy, value fields are used to store backup
 * values. Since this is true for composite property nodes as well,
 * local composite property fields (which are needed to set and query nodes
 * below) are stored in the host field.
 *
 * <p>Since children within the mirror hierarchies are stored in arrays, a path
 * to a particular property within the mirror can be efficiently described by a
 * set of array indices. This set is determined by calling
 * <code>getIndexPath()</code> on the corresponding PropTreeCell within the
 * master hierarchy.
 */
public class HostList {
   private ArrayList<HasProperties> myHosts;
   private PropTreeData[] myDataList;

   public HostList (int initialSize) {
      myHosts = new ArrayList<HasProperties> (initialSize);
   }

   public HostList (HasProperties[] hosts) {
      set (hosts);
   }

   public HostList (Iterable<? extends HasProperties> hosts) {
      set (hosts);
   }

   public void addHost (HasProperties host) {
      myHosts.add (host);
   }

   public HasProperties getHost (int idx) {
      return myHosts.get (idx);
   }

   public int numHosts() {
      return myHosts.size();
   }

   public void set (HasProperties[] hosts) {
      myDataList = null;
      myHosts = new ArrayList<HasProperties> (hosts.length);
      for (int i = 0; i < hosts.length; i++) {
         myHosts.add (hosts[i]);
      }
   }

   public void set (Iterable<? extends HasProperties> hosts) {
      myDataList = null;
      myHosts = new ArrayList<HasProperties> (256);
      for (HasProperties host : hosts) {
         myHosts.add (host);
      }
   }

   public Iterator<HasProperties> getHosts() {
      return myHosts.iterator();
   }
   
   /**
    * Returns an array of the PropTreeData corresponding to <code>cell</code>
    * for every host.
    */
//   public PropTreeData[] getAllData (PropTreeCell cell) {
//      PropTreeData[] dataArray = new PropTreeData[myHosts.size()];
//      if (myDataList == null) {
//         initializeDataList();
//      }
//      int[] indexPath = cell.getIndexPath();
//      for (int i = 0; i < myHosts.size(); i++) {
//         PropTreeData data = myDataList[i];
//         for (int level = 0; level < indexPath.length; level++) {
//            if (data.getSubData() == null) {
//               throw new InternalErrorException (
//                  "data tree not initialized at level " + level + " for "
//                  + cell.pathString());
//            }
//            // XXSystem.out.println ("2");
//            data = data.getSubData()[indexPath[level]];
//         }
//         dataArray[i] = data;
//      }
//      return dataArray;
//   }
   
   /**
    * Returns an array containing the values corresponding to <code>cell</code>
    * for each host. The corresponding property must exist for each host.
    */
   public Object[] getAllValues (PropTreeCell cell) {
      Object[] values = new Object[myHosts.size()];
      if (myDataList == null) {
         initializeDataList();
      }
      int[] indexPath = cell.getIndexPath();
      for (int i = 0; i < myHosts.size(); i++) {
         PropTreeData data = myDataList[i];
         HasProperties host = null;
         for (int level = 0; level < indexPath.length; level++) {
            host = data.myHost;
            if (data.getSubData() == null) {
               throw new InternalErrorException (
                  "data tree not initialized at level " + level + " for "
                  + cell.pathString());
            }
            data = data.getSubData()[indexPath[level]];
         }
         values[i] = PropertyUtils.getValue (data.myInfo, host);
      }
      return values;
   }

   /**
    * Returns an array containing the PropertyInfo structures corresponding to 
    * <code>cell</code>for each host. The corresponding property must exist 
    * for each host.
    */
   public PropertyInfo[] getAllInfos (PropTreeCell cell) {
      PropertyInfo[] infos = new PropertyInfo[myHosts.size()];
      if (myDataList == null) {
         initializeDataList();
      }
      int[] indexPath = cell.getIndexPath();
      for (int i = 0; i < myHosts.size(); i++) {
         PropTreeData data = myDataList[i];
         //HasProperties host = null;
         for (int level = 0; level < indexPath.length; level++) {
            //host = data.myHost;
            if (data.getSubData() == null) {
               throw new InternalErrorException (
                  "data tree not initialized at level " + level + " for "
                  + cell.pathString());
            }
            data = data.getSubData()[indexPath[level]];
         }
         infos[i] = data.myInfo;
      }
      return infos;
   }

   private void initializeDataList() {
      // XXSystem.out.println ("1");
      myDataList = new PropTreeData[myHosts.size()];
      for (int i = 0; i < myHosts.size(); i++) {
         PropTreeData data =
            new PropTreeData (null, myHosts.get(i), PropertyMode.Inactive);
         data.myHost = myHosts.get(i);
         myDataList[i] = data;
      }
   }

   /**
    * Save backup property values corresponding to the children of a particular
    * cell. For every host in this list, we find the data entry that
    * corresponds to <code>cell</code>, and then create for that an
    * array of data entries containing the current values of the
    * child properties. This array is then assigned to the sub-data
    * field of the data entry.
    */
   public void saveBackupValues (PropTreeCell cell) {
      // indexPath will have length 0 for the top most set
      if (myDataList == null) {
         initializeDataList();
      }
      int[] indexPath = cell.getIndexPath();
      for (int i = 0; i < myHosts.size(); i++) {
         PropTreeData data = myDataList[i];
         for (int level = 0; level < indexPath.length; level++) {
            if (data.getSubData() == null) {
               throw new InternalErrorException (
                  "data tree not initialized at level " + level + " for "
                  + cell.pathString());
            }
            // XXSystem.out.println ("2");
            data = data.getSubData()[indexPath[level]];
         }
         saveBackupForHost (data.myHost, data, cell);
      }
   }

   /**
    * Save backup property values
    */
   public void saveBackupForHost (
      HasProperties host, PropTreeData data, PropTreeCell cell) {
      // assume that top level host is stored as value of top-level data
      PropTreeData[] dataList = new PropTreeData[cell.numChildren()];
      int i = 0;
      for (PropTreeCell child = cell.myFirstChild; child != null; child =
         child.next) {
         String propName = child.getInfo().getName();
         PropertyInfo info = PropertyUtils.getPropertyInfo (host, propName);
         if (info == null) {
            throw new InternalErrorException ("Property '" + propName
            + "' not found in " + host.getClass());
         }
         PropTreeData childData = new PropTreeData();
         childData.setInfo (info);
         Object value = PropertyUtils.getValue (info, host);
         if (!usesCompositePropertyWidget (info)) {
            // If not a composite property, set the backup value as a single
            // atomic entity. 
            if (value instanceof Clonable) {
               try {
                  value = ((Clonable)value).clone();
               }
               catch (Exception e) {
                  System.out.println (
                     "Warning: could not clone "+value.getClass());
               }
            }
         }
         childData.setValue (value);
         // XXSystem.out.println ("3");
         if (info.isInheritable()) {
            childData.setMode (PropertyUtils.getMode (info, host));
            // XXSystem.out.println ("4");
         }
         dataList[i++] = childData;
      }
      data.setSubData (dataList);
   }

   //
   // Restoring a multi-layered set of properties stored in an array
   // of PropTreeData.
   //
   public void restoreBackupValues() {
      if (myDataList == null) {
         throw new IllegalStateException ("backups values have not been set");
      }
      for (int i = 0; i < myDataList.length; i++) {
         restoreBackupForHost (myDataList[i]);
      }
   }

   /**
    * Returns true if a class type uses a CompositePropertyWidget.
    * These widgets require that the HostList structures be updated
    * as the widget is expanded.
    */
   private boolean usesCompositePropertyWidget (PropertyInfo info) {
      Class<?> type = info.getValueClass();
      return (CompositeProperty.class.isAssignableFrom (type) &&
              PropertyUtils.findCompositePropertySubclasses(info) == null);
   }

   public void restoreBackupForHost (PropTreeData parentData) {
      PropTreeData[] childData = parentData.getSubData();
      HasProperties host = (HasProperties)parentData.myValue;
      // System.out.println ("restoring for host " + host);

      for (int j = 0; j < childData.length; j++) {
         PropTreeData data = childData[j];
         PropertyInfo info = data.myInfo;
         if (usesCompositePropertyWidget (info)) {
            
            if (PropertyUtils.getValue (info, host) != data.myValue) {
               PropertyUtils.setValue (info, host, data.myValue);
               //System.out.println ("5");
            }
            if (data.myValue != null && data.getSubData() != null) {
               // do if even need to do this? Won't setting the value
               // take care of all sub-levels?
               restoreBackupForHost (data);
               //System.out.println ("6");
            }
         }
         else {
            if (info.isInheritable() && data.myMode != PropertyMode.Explicit) {
               PropertyUtils.setMode (info, host, data.myMode);
               //System.out.println ("7");
            }
            else {
               PropertyUtils.setValue (info, host, data.myValue);
               //System.out.println ("8 " + data.myValue);
            }
         }
      }
   }

   // private String intArrayToString (int[] array)
   // {
   // StringBuilder builder = new StringBuilder(256);
   // for (int i=0; i<array.length; i++)
   // { builder.append(i);
   // if (i < array.length-1)
   // { builder.append (' ');
   // }
   // }
   // return builder.toString();
   // }

   private boolean inList (String[] list, String str) {
      for (int i = 0; i < list.length; i++) {
         if (list[i].equals (str)) {
            return true;
         }
      }
      return false;
   }

   public PropTreeCell commonProperties (
      PropTreeCell cell, boolean allowReadonly) {
      return commonProperties (cell, allowReadonly, null, null);
   }

   /**
    * Returns a PropTreeCell whose children represent the first level of
    * properties common to all hosts, starting from the position in the property
    * tree indicated by cell. If cell is null, then the position is assumed to
    * be the hosts themselves.
    * 
    * We can restrict the properties chosen by providing names of restricted and
    * excluded properties.
    */
   public PropTreeCell commonProperties (
      PropTreeCell cell, boolean allowReadonly, String[] restricted,
      String[] excluded) {
      PropTreeCell newRoot = new PropTreeCell();
      int[] indexPath = (cell == null ? new int[0] : cell.getIndexPath());
      // PropTreeCell path = (cell == null ? null : cell.createPathToRoot());

      if (myDataList == null) {
         initializeDataList();
      }

      for (int i = 0; i < myHosts.size(); i++) {
         PropTreeData data = myDataList[i];
         HasProperties host = null;
         for (int level = 0; level < indexPath.length; level++) {
            host = data.myHost;
            if (data.getSubData() == null) {
               throw new InternalErrorException (
                  "data tree not initialized at level " + level + " for "
                  + cell.pathString());
            }
            // XXSystem.out.println ("9");
            data = data.getSubData()[indexPath[level]];
         }
         HasProperties subHost = (HasProperties)data.myValue;
         if (subHost == null) {
            Object newHost = PropertyUtils.createInstance (data.myInfo, host);
            // XXSystem.out.println ("10");
            if (!(newHost instanceof CompositeProperty)) {
               throw new InternalErrorException ("Property '"
               + data.myInfo.getName()
               + "' not an instance of CompositeProperty");
            }
            subHost = (HasProperties)newHost;
         }
         data.myHost = subHost;

         if (i == 0) {
            for (PropertyInfo info : subHost.getAllPropertyInfo()) {
               if ((restricted == null || inList (restricted, info.getName())) &&
                   (excluded == null || !inList (excluded, info.getName())) &&
                   (info.getEditing() == PropertyInfo.Edit.Always ||
                   (myHosts.size() == 1 &&
                    info.getEditing() == PropertyInfo.Edit.Single)) &&
                   (allowReadonly || !info.isReadOnly()) &&
                   (!info.isInheritable() || (PropertyUtils.getMode (
                  info, subHost) != PropertyMode.Inactive))) {
                  PropTreeCell child =
                     new PropTreeCell (info, PropertyUtils.getValue (
                        info, subHost));
                  newRoot.addChild (child);
                  // XXSystem.out.println ("11");
               }
            }
         }
         else {
            PropTreeCell nextChild;
            for (PropTreeCell child = newRoot.myFirstChild; child != null; child =
               nextChild) {
               nextChild = child.next;
               PropertyInfo info = child.getInfo();
               PropertyInfo matchingInfo =
                  PropTreeCell.findMatchingProperty (subHost, info);
               if (matchingInfo == null ||
                   matchingInfo.getEditing() != PropertyInfo.Edit.Always ||
                   (matchingInfo.isInheritable() &&
                    (PropertyUtils.getMode (matchingInfo, subHost) ==
                     PropertyMode.Inactive))) {
                  newRoot.removeChild (child);
                  // XXSystem.out.println ("12");
               }
            }
         }
      }
      return newRoot;
   }

   /**
    * Gets the common values of the properties represented by the children of a
    * specified host node.
    */
   public void getCommonValues (PropTreeCell cell, boolean live) {
      int[] indexPath = cell.getIndexPath();
      // copy the children because we may be pruning them to remove
      // properties which are not common
      PropTreeCell cellCopy = cell.copyChildren();
      for (int i = 0; i < myHosts.size(); i++) {
         PropTreeData data = myDataList[i];
         for (int level = 0; level < indexPath.length; level++) {
            if (data.getSubData() == null) {
               throw new InternalErrorException (
                  "data tree not initialized for " + cell.pathString());
            }
            // XXSystem.out.println ("13");
            data = data.getSubData()[indexPath[level]];
         }
         if (i == 0) {
            getValuesForHost (data, cellCopy, live);
            // XXSystem.out.println ("14");
         }
         else {
            adjustValuesForHost (data, cellCopy, live);
            // XXSystem.out.println ("15");
         }
         if (!cellCopy.hasChildren()) { // XXSystem.out.println ("16");
            return;
         }
      }
   }

   private void getValuesForHost (
      PropTreeData data, PropTreeCell cell, boolean live) {
      HasProperties host = (HasProperties)data.myHost;
      //PropTreeData[] dataList = data.getSubData();
      for (PropTreeCell child = cell.myFirstChild; child != null; child =
         child.next) {
         String propName = child.getPropName();
         PropertyInfo info = PropertyUtils.getPropertyInfo (host, propName);
         if (info == null) {
            throw new InternalErrorException ("Property '" + propName
            + "' not found in " + host.getClass());
         }
         Object value = PropertyUtils.getValue (info, host);
         if (usesCompositePropertyWidget (info)) {
            child.setValue (value == null ? null : CompositeProperty.class);
            // XXSystem.out.println ("17");
         }
         else {
            child.setValue (value);
            // XXSystem.out.println ("18");
         }
         if (info.isInheritable()) {
            PropertyMode mode = PropertyUtils.getMode (info, host);
            child.setMode (mode);
            // XXSystem.out.println ("19");
            if (!live && mode != PropertyMode.Explicit) {
               child.setValue (Property.VoidValue);
            }
         }
      }
   }

   private void adjustValuesForHost (
      PropTreeData data, PropTreeCell cell, boolean live) {
      HasProperties host = (HasProperties)data.myHost;
      //PropTreeData[] dataList = data.getSubData();
      PropTreeCell child, nextChild;
      for (child = cell.myFirstChild; child != null; child = nextChild) {
         nextChild = child.next;
         String propName = child.getPropName();
         PropertyInfo info = PropertyUtils.getPropertyInfo (host, propName);
         if (info == null) {
            throw new InternalErrorException ("Property '" + propName
            + "' not found in " + host.getClass());
         }
         Object value = PropertyUtils.getValue (info, host);
         if (usesCompositePropertyWidget (info)) {
            if (child.getValue() != value) {
               child.setValue (Property.VoidValue);
               // XXSystem.out.println ("20");
            }
         }
         else {
            if (!PropertyUtils.equalValues (child.getValue(), value)) {
               child.setValue (Property.VoidValue);
               // XXSystem.out.println ("21");
            }
            if (info.isInheritable()) {
               PropertyMode mode = PropertyUtils.getMode (info, host);
               if (mode != child.getMode()) {
                  child.setMode (PropertyMode.Void);
                  // XXSystem.out.println ("22");
               }
               if (!live && mode != PropertyMode.Explicit) {
                  child.setValue (Property.VoidValue);
               }
            }
         }
         if (child.getValue() == Property.VoidValue &&
             (!info.isInheritable() || child.getMode() == PropertyMode.Void)) {
            cell.removeChild (child);
            // XXSystem.out.println ("23");
         }
      }
   }

   // public void setTreeValuesAndModes (PropTreeCell tree)
   // {
   // for (int i=0; i<myHosts.size(); i++)
   // { tree.setTreeValuesInHost (myHosts.get(i));
   // }
   // }

   // public void setValue (PropTreeCell cell, Object value)
   // {
   // PropTreeCell path = cell.createPathToRoot();
   // for (int i=0; i<myHosts.size(); i++)
   // { path.setValueInHost (myHosts.get(i), value);
   // }
   // }

   public void setValue (PropTreeCell cell, Object value) {
      int[] indexPath = cell.getIndexPath();
      if (indexPath.length == 0) {
         throw new InternalErrorException (
            "setValue cannot be called for top-level cell");
      }
      for (int i = 0; i < myHosts.size(); i++) {
         PropTreeData data = myDataList[i];
         HasProperties host = null;
         for (int level = 0; level < indexPath.length; level++) {
            host = data.myHost;
            if (data.getSubData() == null) {
               throw new InternalErrorException (
                  "data tree not initialized for " + cell.pathString());
            }
            // XXSystem.out.println ("24");
            data = data.getSubData()[indexPath[level]];
         }
         PropertyUtils.setValue (data.myInfo, host, value);
      }
   }

   public Object getCommonValue (PropTreeCell cell) {
      int[] indexPath = cell.getIndexPath();
      if (indexPath.length == 0) {
         throw new InternalErrorException (
            "getCommonValue cannot be called for top-level cell");
      }
      Object commonValue = null;
      for (int i = 0; i < myHosts.size(); i++) {
         PropTreeData data = myDataList[i];
         HasProperties host = null;
         for (int level = 0; level < indexPath.length; level++) {
            host = data.myHost;
            if (data.getSubData() == null) {
               throw new InternalErrorException (
                  "data tree not initialized for " + cell.pathString());
            }
            // XXSystem.out.println ("25");
            data = data.getSubData()[indexPath[level]];
         }
         Object value = PropertyUtils.getValue (data.myInfo, host);
         if (i == 0) {
            commonValue = value;
         }
         else {
            if (!PropertyUtils.equalValues (commonValue, value)) {
               return Property.VoidValue;
            }
         }
      }
      return commonValue;
   }

   public Range getCommonRange (PropTreeCell cell) {
      int[] indexPath = cell.getIndexPath();
      if (indexPath.length == 0) {
         throw new InternalErrorException (
            "getCommonRange cannot be called for top-level cell");
      }
      Range commonRange = null;
      for (int i = 0; i < myHosts.size(); i++) {
         PropTreeData data = myDataList[i];
         HasProperties host = null;
         for (int level = 0; level < indexPath.length; level++) {
            host = data.myHost;
            if (data.getSubData() == null) {
               throw new InternalErrorException (
                  "data tree not initialized for " + cell.pathString());
            }
            // XXSystem.out.println ("25");
            data = data.getSubData()[indexPath[level]];
         }
         Range range = PropertyUtils.getRange (data.myInfo, host);
         if (range != null) {
            if (commonRange == null) {
               try {
                  commonRange = (Range)range.clone();
               }
               catch (Exception e) {
                  throw new InternalErrorException (
                     "Can't clone "+range.getClass());
               }
            }
            else {
               commonRange.intersect (range);
            }
         }
      }
      return commonRange;
   }

   public PropertyMode getCommonMode (PropTreeCell cell) {
      int[] indexPath = cell.getIndexPath();
      if (indexPath.length == 0) {
         throw new InternalErrorException (
            "getCommonValue cannot be called for top-level cell");
      }
      PropertyMode commonMode = null;
      for (int i = 0; i < myHosts.size(); i++) {
         PropTreeData data = myDataList[i];
         HasProperties host = null;
         for (int level = 0; level < indexPath.length; level++) {
            host = data.myHost;
            if (data.getSubData() == null) {
               throw new InternalErrorException (
                  "data tree not initialized for " + cell.pathString());
            }
            // XXSystem.out.println ("25");
            data = data.getSubData()[indexPath[level]];
         }
         PropertyMode mode = PropertyUtils.getMode (data.myInfo, host);
         if (i == 0) {
            commonMode = mode;
         }
         else {
            if (commonMode != mode) {
               return PropertyMode.Void;
            }
         }
      }
      return commonMode;
   }

   public void setMode (PropTreeCell cell, PropertyMode mode) {
      int[] indexPath = cell.getIndexPath();
      if (indexPath.length == 0) {
         throw new InternalErrorException (
            "setMode cannot be called for top-level cell");
      }
      for (int i = 0; i < myHosts.size(); i++) {
         PropTreeData data = myDataList[i];
         HasProperties host = null;
         for (int level = 0; level < indexPath.length; level++) {
            host = data.myHost;
            if (data.getSubData() == null) {
               throw new InternalErrorException (
                  "data tree not initialized for " + cell.pathString());
            }
            // XXSystem.out.println ("25");
            data = data.getSubData()[indexPath[level]];
         }
         PropertyUtils.setMode (data.myInfo, host, mode);
      }
   }

   /*
    * For every host in this list, make sure that the composite 
    * property corresponding to <code>cell</code> contains a non-null
    * value. If the current property value is <code>null</code>,
    * this is fixed by setting the property to the value of
    * <code>myHost</code> for the corresponding data field. The value
    * of myHost would have been set during a previous call
    * to <code>commonProperties</code>.
    */
   public void addSubHostsIfNecessary (PropTreeCell cell) {
      int[] indexPath = cell.getIndexPath();
      if (indexPath.length == 0) {
         return;
      }
      for (int i = 0; i < myHosts.size(); i++) {
         PropTreeData data = myDataList[i];
         HasProperties host = null;
         for (int level = 0; level < indexPath.length; level++) {
            host = data.myHost;
            if (data.getSubData() == null) {
               throw new InternalErrorException (
                  "data tree not initialized for " + cell.pathString());
            }
            data = data.getSubData()[indexPath[level]];
         }
         if (PropertyUtils.getValue (data.myInfo, host) == null) {
            PropertyUtils.setValue (data.myInfo, host, data.myHost);
            // reset this in case value was defensively duplicated by setValue
            data.myHost =
               (HasProperties)PropertyUtils.getValue (data.myInfo, host);
            // XXSystem.out.println ("28");
         }
      }
   }

   public void setSubHostsFromValue (PropTreeCell cell) {
      int[] indexPath = cell.getIndexPath();
      if (indexPath.length == 0) {
         return;
      }
      for (int i = 0; i < myHosts.size(); i++) {
         PropTreeData data = myDataList[i];
         HasProperties host = null;
         for (int level = 0; level < indexPath.length; level++) {
            host = data.myHost;
            if (data.getSubData() == null) {
               throw new InternalErrorException (
                  "data tree not initialized for " + cell.pathString());
            }
            data = data.getSubData()[indexPath[level]];
         }
         data.myHost = 
            (HasProperties)PropertyUtils.getValue (data.myInfo, host);
         //System.out.println ("data.myHost set to " + data.myHost);
      }
   }

   /*
    * For every host in this list, make sure that the composite 
    * property corresponding to <code>cell</code> contains a non-null
    * value. If the current property value is <code>null</code>,
    * this is fixed by creating a new property based on the value
    * of <code>myInfo</code> for the corresponding data field. The value
    * of <code>myHost</code> is set to the new value.
    */
   public void replaceSubHostsIfNecessary (PropTreeCell cell) {
      int[] indexPath = cell.getIndexPath();
      if (indexPath.length == 0) {
         return;
      }
      for (int i = 0; i < myHosts.size(); i++) {
         PropTreeData data = myDataList[i];
         HasProperties host = null;
         for (int level = 0; level < indexPath.length; level++) {
            host = data.myHost;
            if (data.getSubData() == null) {
               throw new InternalErrorException (
                  "data tree not initialized for " + cell.pathString());
            }
            data = data.getSubData()[indexPath[level]];
         }
         if (PropertyUtils.getValue (data.myInfo, host) == null) {
            CompositeProperty newHost =
               (CompositeProperty)PropertyUtils.createInstance (
                  data.myInfo, host);
            PropertyUtils.setValue (data.myInfo, host, newHost);
            // reset this in case value was defensively duplicated by setValue
            data.myHost =
               (HasProperties)PropertyUtils.getValue (data.myInfo, host);
            // XXSystem.out.println ("28");
         }
      }
   }

   public void clear() {
      myHosts.clear();
      myDataList = null;
   }

}
