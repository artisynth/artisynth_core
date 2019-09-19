/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import artisynth.core.util.*;
import maspack.properties.HierarchyNode;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.util.*;

/**
 * Base class providing some default implementations of the ModelComponent
 * interface.
 */
public abstract class ModelComponentBase implements ModelComponent, Cloneable {
   // public int myIndex = -1;
   public int myNumber = -1;
   protected String myName;
   protected CompositeComponent myParent;
   protected int myFlags = 0;
   //protected LinkedList<ModelComponent> myBackRefs = null;

   protected static NavpanelVisibility DEFAULT_NAVPANEL_VISIBILITY =
      NavpanelVisibility.VISIBLE;

   // special object to indicate NULL in the Deque<Object> passed to
   // updateReferences()
   public static final Object NULL_OBJ = new Integer(0);

   protected static final int SELECTED = 0x1;
   protected static final int MARKED = 0x2;
   protected static final int ANCESTOR_SELECTED = 0x4;
   protected static final int FIXED = 0x8;
   protected static final int NAVPANEL_HIDDEN = 0x10;
   protected static final int NAVPANEL_ALWAYS = 0x20;
   protected static final int SCANNING = 0x40;
   protected static final int NON_WRITABLE = 0x80;
   
   // Allow for creation of custom flags
   protected static int FREE_FLAG_MASK = 0xFF00;
   protected static int freeFlags = FREE_FLAG_MASK;
   
   public static int createTempFlag() {
      // find first free flag
      int flag = Integer.highestOneBit(freeFlags);
      freeFlags = freeFlags & (~flag);
      return flag;
   }

   public static void removeTempFlag(int mask) {
      mask = mask & FREE_FLAG_MASK;
      freeFlags |= mask;
   }
   
   // If true, component names must be unique among all siblings
   public static boolean enforceUniqueNames = true;
   // If true, composite component names must be unique among all siblings
   public static boolean enforceUniqueCompositeNames = false;
   // If true, compnent references in .art files will use compact paths
   public static boolean useCompactPathNames = false;
 
   /**
    * Returns true if the system requires that a particular ModelComponent
    * have a unique name. This will depend on static settings in
    * ModelComponentBase.
    * 
    * @param comp Component to check
    * @return <code>true</code> if <code>comp</code> must have a unique
    * name.
    */
   static boolean mustHaveUniqueName (ModelComponent comp) {
      return (ModelComponentBase.enforceUniqueNames ||
               (comp instanceof CompositeComponent && 
                ModelComponentBase.enforceUniqueCompositeNames));
   }
   
   public static PropertyList myProps =
      new PropertyList (ModelComponentBase.class);

   /**
    * Sets the attributes of this component to their default values.
    */
   protected void setDefaultValues() {
      // setName (null);
      myFlags = 0;
   }

   static {
      myProps.add ("name * *", "name for this component", null, "1E");
      myProps.add (
         "navpanelVisibility", "visibility in the navigation panel",
         DEFAULT_NAVPANEL_VISIBILITY, "NE");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public ModelComponentBase (String name) {
      this();
      if (name != null) {
         setName (name);
      }
   }

   public ModelComponentBase() {
      setDefaultValues();
   }

   /**
    * {@inheritDoc}
    */
   public Property getProperty (String path) {
      return ComponentUtils.findProperty (this, path);
   }

   /**
    * {@inheritDoc}
    */
   public String getName() {
      return myName;
   }

   /**
    * {@inheritDoc}
    */
   public void setName (String name) {
      String err = checkName (name, this);
      if (err != null) {
         throw new IllegalArgumentException (
            "Invalid name '" + name + "': " + err);
      }
      if (name != null && name.length() == 0) {
         name = null;
      }
      NameChangeEvent e = new NameChangeEvent (this, myName);
      myName = name;
      notifyParentOfChange (e);
   }
   
   public Range getNameRange() {
      return new NameRange(this);
   }
   
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor) 
      throws IOException {
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt, ancestor);
   }

   protected void dowrite (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {

      CompositeComponent ancestor = ComponentUtils.castRefToAncestor(ref);
      IndentingPrintWriter.printOpening (pw, "[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      writeItems (pw, fmt, ancestor);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
   throws IOException {
      dowrite (pw, fmt, ref);
   }

   /**
    * {@inheritDoc}
    */
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      @SuppressWarnings("unchecked")
      Deque<ScanToken> tokens = (Deque<ScanToken>)ref;
      if (tokens == null) {
         tokens = new ArrayDeque<> ();
      }
      setScanning (true);
      rtok.scanToken ('[');
      tokens.offer (ScanToken.BEGIN);
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         if (!scanItem (rtok, tokens)) {
            throw new IOException (
               "Error scanning " + getClass().getName() +
               ": unexpected token: " + rtok);
         }
      }
      tokens.offer (ScanToken.END); // terminator token
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      // if keyword is a property name, try scanning that
      rtok.nextToken();
      if (ScanWriteUtils.scanProperty (rtok, this, tokens)) {
         return true;
      }
      rtok.pushBack();
      return false;
   }

   protected boolean postscanAttributeName (Deque<ScanToken> tokens, String name)
      throws IOException {
      return ScanWriteUtils.postscanAttributeName (tokens, name);
   }

   protected <C> C postscanReference (
      Deque<ScanToken> tokens, Class<C> clazz, CompositeComponent ancestor)
      throws IOException {
      return ScanWriteUtils.postscanReference (tokens, clazz, ancestor);
   }

   protected <C> void postscanReferences (
      Deque<ScanToken> tokens, Collection<C> refs, Class<C> clazz,
      CompositeComponent ancestor) throws IOException {
      ScanWriteUtils.postscanReferences (tokens, refs, clazz, ancestor);
   }

   protected boolean scanAndStoreReference (
      ReaderTokenizer rtok, String refName, Deque<ScanToken> tokens) 
      throws IOException {
      return ScanWriteUtils.scanAndStoreReference (rtok, refName, tokens);
   }

   protected int scanAndStoreReferences (
      ReaderTokenizer rtok, String refName, Deque<ScanToken> tokens) 
      throws IOException {
      return ScanWriteUtils.scanAndStoreReferences (
         rtok, refName, tokens);
   }

   protected boolean scanAttributeName (ReaderTokenizer rtok, String name)
   throws IOException {
      if (rtok.ttype == ReaderTokenizer.TT_WORD && rtok.sval.equals (name)) {
         rtok.scanToken ('=');
         return true;
      }
      return false;
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      if (ScanWriteUtils.postscanPropertyValue (tokens, ancestor)) {
         return true;
      }
      return false;
   }

   public void postscan (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      ScanWriteUtils.postscanBeginToken (tokens, this);
      while (tokens.peek() != ScanToken.END) {
         if (!postscanItem (tokens, ancestor)) {
            throw new IOException (
               "Unexpected token for " + 
               ComponentUtils.getDiagnosticName(this) + ": " + tokens.poll());
         }
      }      
      tokens.poll(); // eat END token
      setScanning (false);
   }   

   /**
    * {@inheritDoc}
    */
   public void connectToHierarchy (CompositeComponent hcomp) {
   }

   /**
    * {@inheritDoc}
    */
   public void disconnectFromHierarchy(CompositeComponent hcomp) {
   }

   /**
    * {@inheritDoc}
    */
   public CompositeComponent getParent() {
      return myParent;
   }

   /**
    * {@inheritDoc}
    */
   public void setParent (CompositeComponent parent) {
      myParent = parent;
   }

   public CompositeComponent getGrandParent() {
      if (myParent != null) {
         return myParent.getParent();
      }
      else {
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelected() {
      return (myFlags & SELECTED) != 0;
   }

   /**
    * {@inheritDoc}
    */
   public void setSelected (boolean selected) {
      if (selected) {
         myFlags |= SELECTED;
      }
      else {
         myFlags &= ~SELECTED;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isMarked() {
      return (myFlags & MARKED) != 0;
   }

   /**
    * {@inheritDoc}
    */
   public void setMarked (boolean marked) {
      if (marked) {
         myFlags |= MARKED;
      }
      else {
         myFlags &= ~MARKED;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isWritable() {
      return (myFlags & NON_WRITABLE) == 0;
   }

   /**
    * {@inheritDoc}
    */
   public void setWritable (boolean writable) {
      if (writable) {
         myFlags &= ~NON_WRITABLE;
      }
      else {
         myFlags |= NON_WRITABLE;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isFixed() {
      return (myFlags & FIXED) != 0;
   }

   /**
    * {@inheritDoc}
    */
   public void setFixed (boolean fixed) {
      if (fixed) {
         myFlags |= FIXED;
      }
      else {
         myFlags &= ~FIXED;
      }
   }

   public static int setNavpanelVisibility (
      NavpanelVisibility visibility, int flags) {
      switch (visibility) {
         case VISIBLE: {
            flags &= ~(NAVPANEL_ALWAYS|NAVPANEL_HIDDEN);
            break;
         }
         case ALWAYS: {
            flags |= NAVPANEL_ALWAYS;
            flags &= ~NAVPANEL_HIDDEN;
            break;
         }
         case HIDDEN: {
            flags |= NAVPANEL_HIDDEN;
            flags &= ~NAVPANEL_ALWAYS;
            break;
         }
      }
      return flags;
   }

   public static NavpanelVisibility getNavpanelVisibility (int flags) {
      if ((flags & NAVPANEL_ALWAYS) != 0) {
         return NavpanelVisibility.ALWAYS;
      }
      else if ((flags & NAVPANEL_HIDDEN) != 0) {
         return NavpanelVisibility.HIDDEN;
      }
      else {
         return NavpanelVisibility.VISIBLE;
      }
   }

   public NavpanelVisibility getNavpanelVisibility () {
      return getNavpanelVisibility (myFlags);
   }

   public void setNavpanelVisibility (NavpanelVisibility visibility) {
      int flags = setNavpanelVisibility (visibility, myFlags);
      if (flags != myFlags) {
         myFlags = flags;
         notifyParentOfChange (
            new PropertyChangeEvent (this, "navpanelVisibility"));
      }
   }
   
   public void setScanning (boolean scanning) {
      if (scanning) {
         myFlags |= SCANNING;
      }
      else {
         myFlags &= ~SCANNING;
      }
   }

   public boolean isScanning() {
      return (myFlags & SCANNING) != 0;
   }

   public boolean checkFlag (int mask) {
      if ( (myFlags & mask) > 0) {
         return true;
      }
      return false;
   }
   
   public void setFlag (int mask) {
      myFlags |= mask;
   }
   
   public void clearFlag (int mask) {
      myFlags = myFlags & ~mask;
   }

   /**
    * {@inheritDoc}
    */
   public void getHardReferences (List<ModelComponent> refs) {
   }

   /**
    * {@inheritDoc}
    */
   public void getSoftReferences (List<ModelComponent> refs) {
   }

   /**
    * Queries whether if a component is a descendant of a specified
    * ancestor. If the component is not a descendant of the ancestor, or if the
    * component and the ancestor are equal, the method returns false.
    * 
    * @param ancestor
    * ancestor component to check
    * @param comp
    * component to check for descendence from ancestor
    * @return <code>true</code> if <code>comp</code> is a descendant of
    * <code>ancestor</code>
    */
   public static boolean recursivelyContains (
      ModelComponent ancestor, ModelComponent comp) {

      if (ancestor == comp) {
         return false;
      }
      CompositeComponent parent = comp.getParent();
      while (parent != null) {
         if (parent == ancestor) {
            return true;
         }
         parent = parent.getParent();
      }
      return false;
   }

   public static boolean recursivelyContained (
      ModelComponent comp, CompositeComponent ancestor,
      Collection<? extends ModelComponent> collection) {
      CompositeComponent parent = comp.getParent();
      while (parent != null) {
         if (parent == ancestor) {
            return true;
         }
         parent = parent.getParent();
      }
      if (collection != null) {
         return collection.contains (comp);
      }
      else {
         return false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public int getNumber() {
      return myNumber;
   }

   /**
    * {@inheritDoc}
    */
   public void setNumber (int num) {
      myNumber = num;
   }

   /**
    * {@inheritDoc}
    */
   public void notifyParentOfChange (ComponentChangeEvent e) {
      if (myParent != null) {
         myParent.componentChanged (e);
      }
   }

   public Iterator<? extends HierarchyNode> getChildren() {
      return null;
   }

   public boolean hasChildren() {
      return false;
   }

   public Object clone() throws CloneNotSupportedException {
      ModelComponentBase comp = (ModelComponentBase)super.clone();
      comp.myName = null;
      comp.myNumber = -1;
      comp.myParent = null;
      comp.myFlags = 0;
      //comp.myBackRefs = null;
      return comp;
   }

   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      ModelComponentBase comp;
      try {
         comp = (ModelComponentBase)clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException ("clone not supported for "
         + getClass());
      }
      comp.myName = null;
      comp.myNumber = -1;
      comp.myParent = null;
      comp.myFlags = 0;
      //comp.myBackRefs = null;
      return comp;
   }

   /** 
    * Diagnostic method that prints the references associated with
    * this component.
    */
   public void printReferences() {
      LinkedList<ModelComponent> refs = new LinkedList<ModelComponent>();
      getHardReferences (refs);
      getSoftReferences (refs);
      for (ModelComponent c : refs) {
         System.out.println (ComponentUtils.getPathName(c));
      }
   }

   /**
    * Makes a valid component name from the string provided. If 
    * <code>comp</code> (and possibly <code>parent</code>) is
    * also specified, then the method will also ensure that the
    * name is unique to the component's parent.
    *   
    * @param name name to validate
    * @param comp Component for which the name is intended. 
    * If <code>null</code>, name uniqueness will not be checked.
    * @param parent Parent of the component. May be <code>null</code>, in
    * which case the component's current parent will be used.
    * @return valid component name
    */
   public static String makeValidName (
      String name, ModelComponent comp, CompositeComponent parent) {
      return (new NameRange(comp, parent)).makeValid(name);
   }
   
   /**
    * Makes a valid component name from the string provided. Does
    * not check uniqueness with respect to the component's parent.
    *  
    * @param name name to modify
    * @return valid name
    */
   public static String makeValidName (String name) {
      return makeValidName (name, null, null);
   }

   /**
    * Check name uniqueness restrictions of a component with respect
    * to a specific parent, and throw an exception if these are violated.
    * These restrictions will depend on static settings in ModelComponentBase.
    * 
    * @param comp component whose name should be checked
    * @param parent parent component in which name may have to be unique.
    */
   public static void checkNameUniqueness (
      ModelComponent comp, CompositeComponent parent) {
      String name = comp.getName();
      if (name != null && mustHaveUniqueName(comp)) {
         ModelComponent c = parent.get(name);
         if (c != null && c != comp) {
            throw NonUniqueNameException.create (comp, parent);
         }
      }
   }
   
   /**
    * Check to see if a String represents a valid name for a model
    * component. Returns <code>null</code> if it is, and 
    * an error message otherwise.
    * 
    * @param name name to be validated
    * @param comp Component for which the name is intended.
    * May be <code>null</code>, in which case duplicate sibling names
    * will not be checked.
    * @return <code>null</code>, or error message if the name is not valid.
    */  
   public static String checkName (String name, ModelComponent comp) {
      StringHolder errMsg = new StringHolder();
      (new NameRange(comp)).isValid (name, errMsg);
      return errMsg.value;
   }

   public boolean hasState() {
      return false;
   }
   
   /**
    * {@inheritDoc}
    */
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
   }
}
