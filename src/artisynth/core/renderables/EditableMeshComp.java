/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;

import artisynth.core.mechmodels.MeshComponent;
import artisynth.core.mechmodels.MeshInfo;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentListImpl;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.*;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.util.ScanToken;
import maspack.geometry.MeshBase;
import maspack.geometry.Vertex3d;
import maspack.matrix.*;
import maspack.properties.*;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

public class EditableMeshComp extends RenderableComponentBase
   implements CompositeComponent {

   MeshComponent myMeshComp = null;
   protected MeshInfo myMeshInfo;
   // reference to mesh for rendering - used in case mesh changes
   protected MeshBase myRenderMesh;
   
   VertexList<VertexComponent> myVertexList = null;

   public static boolean DEFAULT_SELECTABLE = true;
   protected boolean mySelectable = DEFAULT_SELECTABLE;

   public static PropertyList myProps = new PropertyList(
      EditableMeshComp.class, RenderableComponentBase.class);
   
   static {
       myProps.add (
         "selectable isSelectable", 
         "true if this mesh is selectable", DEFAULT_SELECTABLE);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public EditableMeshComp() {
      myComponents =
         new ComponentListImpl<ModelComponent>(ModelComponent.class, this);
      myVertexList =
         new VertexList<VertexComponent>(VertexComponent.class, "vertices", "v");
      add(myVertexList);
   }
   
   public EditableMeshComp(MeshBase mesh) {
      this();
      setMesh (mesh);
   }

   public EditableMeshComp(MeshComponent mcomp) {
      this();
      setMeshComp (mcomp);
   }

   public void setMesh (MeshBase mesh) {
      myMeshInfo = new MeshInfo();
      myMeshInfo.set (mesh);
      myMeshComp = null;
      updateComponents();
   }
   
   public void setMeshComp (MeshComponent mcomp) {
      myMeshComp = mcomp;
      myMeshInfo = null;
      updateComponents();
   }

   public MeshBase getMesh() {
      if (myMeshInfo != null) {
         return myMeshInfo.getMesh();
      }
      else if (myMeshComp != null) {
         return myMeshComp.getMesh();
      }
      else {
         return null;
      }
   }      
   
   public void setMeshToWorld (RigidTransform3d TMW) {
      MeshBase mesh = getMesh();
      if (mesh != null) {
         mesh.setMeshToWorld (TMW);
      }
   }

   public RigidTransform3d getMeshToWorld () {
      MeshBase mesh = getMesh();
      if (mesh != null) {
         return mesh.getMeshToWorld();
      }
      else {
         return null;
      }
   }

   public void updateComponents() {
      updateVertices();
   }

   public void updateVertices() { // XXX
      myVertexList.clear();
      MeshBase mesh = getMesh();
      if (mesh == null) {
         return;
      }
      for (Vertex3d vtx : mesh.getVertices()) {
         myVertexList.add(new VertexComponent(vtx));
      }
   }

   ///////////////////////////////////////////////////
   // Composite component stuff
   ///////////////////////////////////////////////////

   protected ComponentListImpl<ModelComponent> myComponents;
   private NavpanelDisplay myDisplayMode = NavpanelDisplay.NORMAL;

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

   protected void add (ModelComponent comp) {
      myComponents.add (comp);
   }
   
   protected boolean remove (ModelComponent comp) {
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
      if (isScanning()) {
         return;
      }
      if (comp instanceof CompositeComponent) {
         notifyParentOfChange (new StructureChangeEvent (
            (CompositeComponent)comp));
      }
      else {
         notifyParentOfChange (StructureChangeEvent.defaultEvent);
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "mesh")) {
         myMeshInfo = new MeshInfo();
         myMeshInfo.scan (rtok); 
         getMesh().setFixed (true);
         return true;
      }
      else if (scanAndStoreReference (rtok, "meshComp", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      if (postscanAttributeName (tokens, "meshComp")) {
         myMeshComp = 
            postscanReference (tokens, MeshComponent.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   @Override
   public void postscan (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      updateComponents();
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      if (myMeshComp != null) {
         pw.println (
            "meshComp="+ComponentUtils.getWritePathName (ancestor,myMeshComp));
      }
      else if (myMeshInfo != null) {
         pw.print ("mesh=");
         myMeshInfo.write (pw, fmt);
      }
      super.writeItems (pw, fmt, ancestor);
   }

   public Iterator<? extends HierarchyNode> getChildren() {
      return myComponents.iterator();
   }

   public boolean hasChildren() {
      return myComponents != null && myComponents.size() > 0;
   }

   public void setSelected (boolean selected) {
      super.setSelected (selected);
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

   public EditableMeshComp copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {

      EditableMeshComp ccomp =
         (EditableMeshComp)super.copy (flags, copyMap);

      ccomp.myComponents =
         new ComponentListImpl<ModelComponent>(ModelComponent.class, this);
      ccomp.myDisplayMode = myDisplayMode;

      return ccomp;
   }

   public boolean hasState() {
      return true;
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      myVertexList.updateBounds(pmin, pmax);
   }

   public void prerenderMesh () {
      MeshBase renderMesh = getMesh();
      if (renderMesh != null) {
         if (!renderMesh.isFixed()) {
            renderMesh.notifyVertexPositionsModified();
         }
         renderMesh.prerender (myRenderProps);
      }
      myRenderMesh = renderMesh;
   }

   @Override
   public void prerender(RenderList list) {
      if (myMeshInfo != null) {
         prerenderMesh();
      }
      else {
         myRenderMesh = null;
      }
      list.addIfVisible(myVertexList);
   }
   
   //@Override
   public void updateSlavePos () {
      // nothing
   }

   //@Override
   public void scaleDistance(double s) {
      myVertexList.scaleDistance(s);
   }

   // @Override
   // public MeshBase getMesh() {
   //    return myMesh;
   // }

  /* --- Renderable interface ---

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return mySelectable;
   }

   public void setSelectable (boolean enable) {
      mySelectable = enable;
   }

   protected boolean isAncestorSelected() {
      ModelComponent comp = this;
      while (comp != null) {
         if (comp.isSelected()) {
            return true;
         }
         comp = comp.getParent();
      }
      return false;
   }

   /* --- begin Renderable implementation --- */
   
   public void render (Renderer renderer, RenderProps props, int flags) {     
      MeshBase renderMesh = myRenderMesh;
      if (renderMesh != null) {
         renderMesh.render (renderer, props, flags);
      }
   }

   @Override
   public void render(Renderer renderer, int flags) { 
      MeshBase renderMesh = myRenderMesh;
      if (renderMesh != null) {
         if (isSelected() || isAncestorSelected()) {
            flags |= Renderer.HIGHLIGHT;
         }
         // call render(,,) instead of renderMesh.render(,,) in case the former
         // is overridden
         render (renderer, getRenderProps(), flags);
      }
   }

   public VertexList<VertexComponent> getVertexComponents() {
      return myVertexList;
   }
   
   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      context.addAll (myVertexList);
   } 
}
