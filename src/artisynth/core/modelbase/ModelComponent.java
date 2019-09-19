/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.List;
import java.util.Deque;
import java.io.IOException;

import maspack.properties.HasProperties;
import maspack.properties.HierarchyNode;
import maspack.util.Scannable;
import maspack.util.ReaderTokenizer;
import artisynth.core.util.*;

/**
 * Basic interface for all Artisynth elements
 */
public interface ModelComponent
   extends HasProperties, HierarchyNode, PostScannable {
   
   /**
    * Controls the visibility of this component in the navigation panel.
    */
   public enum NavpanelVisibility {
      HIDDEN,
      VISIBLE,
      ALWAYS
   }

   /**
    * Gets the name of this component.
    * 
    * @return name of this component
    */
   public String getName();

   /**
    * Sets the name of this component. When the name is set, this method should
    * call {@link #notifyParentOfChange notifyParentOfChange} with a {@link
    * artisynth.core.modelbase.NameChangeEvent NameChangeEvent} so that it's
    * parent can update the name map entry and other ancestors can adjust for
    * the new name.
    * 
    * <p>
    * Component names can be specified as null. However, if not null, they
    * should have non-zero length and should not begin with a digit or contain
    * the characters '.' or '/'. Implementations can use {@link
    * artisynth.core.modelbase.ModelComponentBase#checkName
    * ModelComponentBase.checkName()} to determine if a proposed name is valid.
    * 
    * @param name
    * new component name
    * @throws IllegalArgumentException
    * if the name does not have a valid format
    */
   public void setName (String name) throws IllegalArgumentException;

   /**
    * Gets the number of this component. A unique component number is assigned
    * whenever a component is made a child of a parent component, and this
    * number will be persistent as long as the component remains a child that
    * parent. In particular, cmponent numbers are not indices, and will not
    * change as other components are added or removed from the parent. The
    * purpose of the component number is to provide a alternate name for a
    * component in case the string name is not set.
    * 
    * @return number of this component
    */
   public int getNumber();

   /**
    * Sets the number of this component. This method should only be called by
    * parent components when they add or remove this component as a child.
    * 
    * @param num
    * new component number
    */
   public void setNumber (int num);

   /**
    * Returns the current parent of this ModelComponent, if any.
    * 
    * @return parent of this ModelComponent
    */
   public CompositeComponent getParent();
   
   /**
    * Sets the parent for this component. A value of <code>null</code>
    * is specified when the component is being removed from the hierarchy.
    * <p>
    * This method is intended for internal use by the system only.
    * 
    * @param parent new parent component, or <code>null</code>.
    */
   public void setParent (CompositeComponent parent);

   /**
    * Called by the system after this component, or an ancestor of this
    * component, is added to the component hierarchy (i.e., added as
    * a child of another CompositeComponent). This method is responsible for
    * doing any required hierarchy-dependent initialization, including any
    * updating of referenced components.
    *
    * <p> When this method is called, {@link #getParent} will return the new
    * parent component; the system will have set this beforehand.
    * @param hcomp hierarchy component to which this component, 
    * or its ancestor, was attached
    */
   public void connectToHierarchy (CompositeComponent hcomp);

   /**
    * Called by the system after this component, or an ancestor of this
    * component, is removed from the component hierarchy (i.e., removed as a
    * child of its parent).  This method is responsible for any required
    * hierarchy-dependent deinitialization, including any updating of
    * referenced components.
    *
    * <p> When this method is called, {@link #getParent} will still return this
    * original parent component; the system will set this to <code>null</code>
    * after.
    * @param hcomp hierarchy component from which this component, 
    * or its ancestor, was detached
    */
   public void disconnectFromHierarchy (CompositeComponent hcomp);

   /**
    * Returns true if this model component is selected.
    * 
    * @return true if this component is selected
    */
   public boolean isSelected();

   /**
    * Selects or deselects this component. This method should <i>only</i> be
    * used by the selection manager, since object selection must be coordinated
    * with other system components.
    * 
    * @param selected
    * if true, this component is selected
    */
   public void setSelected (boolean selected);

   /**
    * Returns true if this model component is marked. Marking is used
    * temporarily for applications such as determining which components which
    * are referenced by a set of components.
    * 
    * @return true if this component is marked
    */
   public boolean isMarked();

   /**
    * Marks or unmarks this component.
    * 
    * @param marked
    * if true, this component is marked
    */
   public void setMarked (boolean marked);

   /**
    * Sets this component to be writable or non-writable. Non-writable
    * components (and any of their descendants) are not written out to
    * secondary stoarge.
    *
    * <p>Some components may be intrinsicly non-writable, in which case calling
    * this method will have no effect.
    * 
    * @param writable
    * if true, component is made writable
    */
   public void setWritable (boolean writable);

   /**
    * Returns true if this model component is fixed. Fixed is used to indicate
    * that the component should not be removed from its parent.
    * 
    * @return true if this component is fixed
    */
   public boolean isFixed();

   /**
    * Fixes or unfixes this component. This method should be used with care,
    * and only be component designers.
    * 
    * @param fixed
    * if true, this component will be fixed to its parent.
    */
   public void setFixed (boolean fixed);

   public NavpanelVisibility getNavpanelVisibility();

   /**
    * Notifies the parent of this component (if any) of changes within in its
    * descendants. This is done by calling the parent's {@link
    * artisynth.core.modelbase.CompositeComponent#componentChanged
    * componentChanged} method.
    * 
    * @param e
    * optional argument giving specific information about the change
    */
   public void notifyParentOfChange (ComponentChangeEvent e);

   /**
    * Appends all hard references for this component to a list.
    * References are other components, outside of this component's
    * immediate ancestry, on which this component depends. For
    * example, an AxialSpring refers to two Point components as
    * for it's end points. A <i>hard</i> reference is one which
    * the referring component <i>must</i> have, and which if deleted, implies
    * that the referring component should be deleted too.
    * 
    * @param refs
    * list to which hard references are appended
    */
   public void getHardReferences (List<ModelComponent> refs);

   /**
    * Appends all soft references for this component to a list.
    * References are other components, outside of this component's
    * immediate ancestry, on which this component depends. For
    * example, an ExcitationComponent may refer to one or more
    * other ExcitationComponents to act as excitation sources.
    * A <i>soft</i> reference is one which can be removed from the 
    * referring component. In particular, if any soft references 
    * for a component are deleted, then that component's
    * {@link #updateReferences updateReferences()} method will
    * be called to update its internal reference information.
    * 
    * @param refs
    * list to which soft references are appended
    */
   public void getSoftReferences (List<ModelComponent> refs);

   /**
    * Queries if this component has state. Structure change events involving
    * components that have state will cause the current state history of
    * of the system to be cleared.
    *
    * @return <code>true</code> if this component has state
    */
   public boolean hasState();
   
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException;

   /**
    * May be called by the system if any of the soft references for
    * this component are removed from the the component hierarchy.
    * If called with <code>undo</code> equal to <code>false</code>,
    * this component should then examine its soft references and
    * use {@link ComponentUtils#areConnected ComponentUtils.areConnected()}
    * to determine which of them have been disconnected from the hierarchy.
    * Disconnected references should be removed, and sufficient information
    * should be appended to <code>undoInfo</code> to allow this update
    * to be undone if this method is called later with <code>undo</code> 
    * equal to <code>true</code>. When undoing an update, the undo
    * information should be removed from the front of <code>undoInfo</code>.
    * 
    * @param undo if <code>true</code>, indicates that the most
    * recent reference update should be undone, using the supplied
    * undo information.
    * @param undoInfo if <code>undo</code> is <code>false</code>, should be used
    * to store information allowing the reference update to be undone.
    * Otherwise, if <code>undo</code> is <code>true</code>, then this
    * supplied information to undo the most recent update. 
    */
   public void updateReferences (boolean undo, Deque<Object> undoInfo);

}
