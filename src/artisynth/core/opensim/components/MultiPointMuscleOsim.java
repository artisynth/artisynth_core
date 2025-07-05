package artisynth.core.opensim.components;

import java.util.*;
import java.io.*;

import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.Marker;
import maspack.properties.HierarchyNode;
import maspack.util.*;
import maspack.render.*;

/**
 * Extension to ArtiSynth MultiPointMuscle component that contains its path
 * points in a component list named "pathpoints".
 */
public class MultiPointMuscleOsim extends MultiPointMuscle 
   implements CompositeComponent {

   protected ComponentListImpl<ModelComponent> myComponents;
   protected NavpanelDisplay myDisplayMode = NavpanelDisplay.NORMAL;
   protected PointList<Marker> myPathPoints = null;

   protected void initializeChildComponents() {
      myComponents = 
         new ComponentListImpl<ModelComponent>(ModelComponent.class, this);
      myPathPoints = new PointList<Marker>(Marker.class, "pathpoints");

      add (myPathPoints);
   }

   public MultiPointMuscleOsim() {
      super();
      initializeChildComponents();
   }
   
   public MultiPointMuscleOsim (String name) {
      this();
      setName (name);
   }
   
   /* --- Path point management --- */

   public PointList<Marker> getPathPoints() {
      return myPathPoints;
   }

   /* --- Composite component --- */

   public void updateNameMap (
      String newName, String oldName, ModelComponent comp) {
      myComponents.updateNameMap (newName, oldName, comp);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent findComponent (String path) {
      return ComponentUtils.findComponent (this, path);
   }

   public void add (ModelComponent comp) {
      myComponents.add (comp);
   }

   public boolean remove (ModelComponent comp) {
      return myComponents.remove (comp);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent get (String nameOrNumber) {
      return myComponents.get (nameOrNumber);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent get (int idx) {
      return myComponents.get (idx);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent getByNumber (int num) {
      return myComponents.getByNumber (num);
   }

   /**
    * {@inheritDoc}
    */
   public int getNumberLimit() {
      return myComponents.getNumberLimit();
   }

   /**
    * {@inheritDoc}
    */
   public int indexOf (ModelComponent comp) {
      return myComponents.indexOf (comp);
   }

   /**
    * {@inheritDoc}
    */
   public int numComponents() {
      return myComponents.size();
   }

   /**
    * {@inheritDoc}
    */
   public void componentChanged (ComponentChangeEvent e) {
      myComponents.componentChanged (e);
      notifyParentOfChange (e);
   }

   protected void notifyStructureChanged (Object comp) {
      if (comp instanceof CompositeComponent) {
         notifyParentOfChange (new StructureChangeEvent (
            (CompositeComponent)comp));
      }
      else {
         notifyParentOfChange (StructureChangeEvent.defaultEvent);
      }
   }

   /**
    * {@inheritDoc}
    */
   public NavpanelDisplay getNavpanelDisplay() {
      return myDisplayMode;
   }

   /**
    * Sets the display mode for this component. This controls
    * how the component is displayed in a navigation panel. The default
    * setting is <code>NORMAL</code>.
    *
    * @param mode new display mode
    */
   public void setDisplayMode (NavpanelDisplay mode) {
      myDisplayMode = mode;
   }

   /**
    * {@inheritDoc}
    */
   public boolean hierarchyContainsReferences() {
      return false;
   }

   public Iterator<? extends HierarchyNode> getChildren() {
      return myComponents.iterator();
   }

   public boolean hasChildren() {
      return myComponents != null && myComponents.size() > 0;
   }

   /* --- render overrides needed for composite component --- */

   @Override
   public void prerender(RenderList list) {
      super.prerender(list);
      list.addIfVisible (myPathPoints);
   }   

   /* --- I/O overrides needed for composite component --- */

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
         throws IOException {

      super.writeItems (pw, fmt, ancestor);
      myComponents.writeComponentsByName (pw, fmt, ancestor);
   }

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      myComponents.scanBegin();
      super.scan (rtok, ref);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (myComponents.scanAndStoreComponentByName (rtok, tokens)) {
         return true;
      }      
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) 
         throws IOException {

      if (myComponents.postscanComponent (tokens, ancestor)) {
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   @Override
   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      myComponents.scanEnd();
   }

   // public void getState (DataBuffer data) {
   //    System.out.println ("getState");
   //    super.getState (data);
   // } 

   // public void setState (DataBuffer data) {
   //    System.out.println ("setState");
   //    super.setState (data);
   // }

}
