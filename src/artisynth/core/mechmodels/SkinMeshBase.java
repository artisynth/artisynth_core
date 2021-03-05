/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.*;
import java.io.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.geometry.*;
import maspack.properties.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

/**
 * Base class for a SkinMeshBody, which is a type of mesh component in which each
 * vertex is attached to one or more underlying dynamic master components using
 * a {@link PointAttachment}.
 */
public abstract class SkinMeshBase extends DynamicMeshComponent
   implements HasSlaveObjects, CompositeComponent, HasSurfaceMesh {

   protected ComponentListImpl<ModelComponent> myComponents;

   private NavpanelDisplay myNavpanelDisplay = NavpanelDisplay.NORMAL;

   public abstract int numVertexAttachments();

   public abstract PointAttachment getVertexAttachment (int idx);

   public static PropertyList myProps =
      new PropertyList (SkinMeshBase.class, MeshComponent.class);

   static {
      myProps.add(
         "navpanelDisplay", "display mode in the navigation panel",
         NavpanelDisplay.NORMAL);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public SkinMeshBase () {
      this (null);
   }

   // public int numAttachments () {
   //    return myAttachments.size();
   // }

   // public int getAttachment (int vidx) {
   //    return myAttachments.getByNumber (vidx);
   // }

   public SkinMeshBase (String name) {
      super (name);
      myComponents =
         new ComponentListImpl<ModelComponent>(ModelComponent.class, this);
      // myAttachments =
      //    new ComponentList<PointAttachment> (
      //       PointAttachment.class, "attachments");
      // add (myAttachments);
   }

   // ========== Begin CompositeComponent implementation ==========

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
   public int numComponents() {
      return myComponents.size();
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
   public ModelComponent findComponent (String path) {
      return ComponentUtils.findComponent (this, path);
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
   public NavpanelDisplay getNavpanelDisplay() {
      return myNavpanelDisplay;
   }
   
   /**
    * Sets the display mode for this component. This controls
    * how the component is displayed in a navigation panel. The default
    * setting is <code>NORMAL</code>.
    *
    * @param mode new display mode
    */
   public void setNavpanelDisplay (NavpanelDisplay mode) {
      if (myNavpanelDisplay != mode) {
         myNavpanelDisplay = mode;
         PropertyChangeEvent e =
            new PropertyChangeEvent (this, "navpanelDisplay");
         notifyParentOfChange (e);
      }
   }

   /**
    * Sets the display mode for this component. This controls
    * how the component is displayed in a navigation panel. The default
    * setting is <code>NORMAL</code>.
    *
    * @param mode new display mode
    */
   public void setDisplayMode (NavpanelDisplay mode) {
      myNavpanelDisplay = mode;
   }
   
   @Override
   public NavpanelVisibility getNavpanelVisibility() {
      return NavpanelVisibility.ALWAYS;   // XXX HACK to make sure mesh is displayed in nav
   }

   /**
    * {@inheritDoc}
    */
   public void componentChanged (ComponentChangeEvent e) {
      myComponents.componentChanged (e);
      notifyParentOfChange (e);
   }

   /**
    * {@inheritDoc}
    */
   public void updateNameMap (
      String newName, String oldName, ModelComponent comp) {
      myComponents.updateNameMap (newName, oldName, comp);
   }

   /**
    * {@inheritDoc}
    */
   public boolean hierarchyContainsReferences() {
      return false;
   }

   protected void add (ModelComponent comp) {
      myComponents.add (comp);
   }
   
   protected boolean remove (ModelComponent comp) {
      return myComponents.remove (comp);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (ScanWriteUtils.scanProperty (rtok, this, tokens)) {
         return true;
      }
      else if (myComponents.scanAndStoreComponentByName (rtok, tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      if (myComponents.postscanComponent (tokens, ancestor)) {
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   @Override
      public void scan (
         ReaderTokenizer rtok, Object ref) throws IOException {
      myComponents.scanBegin();
      super.scan (rtok, ref);
   }

   @Override
   public void postscan (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      if (hierarchyContainsReferences()) {
         ancestor = this;
      }
      super.postscan (tokens, ancestor);
      myComponents.scanEnd();
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      myComponents.writeComponentsByName (pw, fmt, ancestor);
   }

   public Iterator<? extends HierarchyNode> getChildren() {
      return myComponents.iterator();
   }

   public boolean hasChildren() {
      return myComponents != null && myComponents.size() > 0;
   }

   // ========== End CompositeComponent implementation ==========

   /**
    * Updates the mesh vertices to reflect the current position of the
    * attached Frames, FemModels, and points.
    */
   public void updateSlavePos() {
      MeshBase mesh = getMesh();
      if (mesh != null) {
         Point3d pos = new Point3d();
         int numa = numVertexAttachments();
         for (int i=0; i<numa; i++) {
            PointAttachment a = getVertexAttachment(i);
            if (a != null) {
               Vertex3d vtx = mesh.getVertices().get(i);
               a.getCurrentPos (pos);
               vtx.setPosition(pos);
            }
         }
         mesh.notifyVertexPositionsModified();
      }
   }
   
   public void updateSlaveVel() {
      // nothing to do - mesh vertices don't have velocities      
   }

   public void scaleDistance (double s) {
      // Inefficiency: Calling super.scale will cause scale to be passed into
      // MeshInfo.  It will also cause the whole mesh to be scaled, which is
      // not needed because of later call updatePosState() *unless* some
      // vertices don't have attachments
      super.scaleDistance (s);
      int numa = numVertexAttachments();
      for (int i=0; i<numa; i++) {
         PointAttachment a = getVertexAttachment (i);
         if (a instanceof ScalableUnits) {
            ((ScalableUnits)a).scaleDistance (s);
         }
      }
      updateSlavePos();
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {

      // shouldn't need to change anything since everything is weight-based
      updateSlavePos();
   }  
   
   @Override
   public SkinMeshBase copy(int flags, Map<ModelComponent,ModelComponent> copyMap) {
      SkinMeshBase smb = (SkinMeshBase)super.copy(flags, copyMap);
      return smb;
   }

   public PolygonalMesh getSurfaceMesh() {
      if (getMesh() instanceof PolygonalMesh) {
         return (PolygonalMesh)getMesh();
      }
      else {
         return null;
      }
   }
   
   public int numSurfaceMeshes() {
      return getSurfaceMesh() != null ? 1 : 0;
   }
   
   public PolygonalMesh[] getSurfaceMeshes() {
      return MeshComponent.createSurfaceMeshArray (getSurfaceMesh());
   }
   
}
