package artisynth.core.renderables;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;

import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.HasSurfaceMesh;
import artisynth.core.mechmodels.IsMarkable;
import artisynth.core.mechmodels.Marker;
import artisynth.core.mechmodels.MeshInfo;
import artisynth.core.mechmodels.PointList;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentListImpl;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScanToken;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.Face;
import maspack.geometry.GeometryTransformer;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Line;
import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.properties.HierarchyNode;
import maspack.render.RenderList;
import maspack.render.Renderer;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Base class for texture-based planes, supports affine transforms and frame-markers
 */
public abstract class TexturePlaneBase extends Frame
implements CompositeComponent, TransformableGeometry, HasSurfaceMesh, IsMarkable {

   RenderableComponentList<FrameMarker> markers;

   protected TexturePlaneBase() {
      markers = new PointList<FrameMarker>(FrameMarker.class, "markers");
      add(markers);

   }

   protected TexturePlaneBase(String name) {
      this();
      setName (name);
   }

   @Override
   public boolean addMarker (Marker mkr) {

      if (mkr instanceof FrameMarker) {
         markers.add ((FrameMarker)mkr);
         return true;
      }

      return false;

   }

   @Override
   public boolean canAddMarker (Marker mkr) {
      if (mkr instanceof FrameMarker) {
         return true;
      }
      return false;
   }

   @Override
   public Marker createMarker (Point3d pnt) {
      return createMarker(pnt, true);
   }

   protected abstract PolygonalMesh getImageMesh();

   protected abstract MeshInfo getImageMeshInfo();

   @Override
   public void scaleDistance (double s) {
      super.scaleDistance (s);
      getImageMeshInfo().scale (s, s, s);
   }

   public void transformGeometry (AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      super.transformGeometry (gtr, context, flags);
      getImageMeshInfo().transformGeometryAndPose (gtr, null);
   }

   @Override
   protected void updatePosState () {
      super.updatePosState ();
      getImageMesh().setMeshToWorld (getPose ());
   }

   public void prerender (RenderList list) {
      super.prerender (list);
      getImageMeshInfo().prerender (myRenderProps);
      list.addIfVisible (markers);
      for (FrameMarker m : markers) {
         m.updatePosState ();
      }
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
      getImageMeshInfo().render (
         renderer, myRenderProps, isSelected() ? Renderer.HIGHLIGHT : 0);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      getImageMesh().updateBounds (pmin, pmax);
   }

   @Override
   public PolygonalMesh getSurfaceMesh () {
      return getImageMesh();
   }

   @Override
   public int numSurfaceMeshes () {
      return 1;
   }

   @Override
   public PolygonalMesh[] getSurfaceMeshes () {
      return new PolygonalMesh[] {getImageMesh()};
   }


   /**
    * Create a marker for the image, optionally projecting to the surface of the image
    * @param pnt location to add marker
    * @param project project to image surface
    * @return created marker
    */
   public FrameMarker createMarker(Point3d pnt, boolean project) {

      Point3d wloc = new Point3d(pnt);
      if (project) {
         // nearest point on image to point
         BVFeatureQuery query = new BVFeatureQuery ();
         Point3d near = new Point3d();
         Vector2d uv = new Vector2d();

         PolygonalMesh imageMesh = getImageMesh ();
         Face face = query.nearestFaceToPoint (near, uv, imageMesh, pnt);
         if (face != null) {
            wloc.set (near);
         }
      }

      return createMarkerInternal(wloc);
   }

   protected FrameMarker createMarkerInternal(Point3d wloc) {
      FrameMarker mkr = new FrameMarker ();
      mkr.setFrame (this);
      mkr.setWorldLocation (wloc);
      return mkr;
   }

   @Override
   public Marker createMarker (Line ray) {
      BVFeatureQuery query = new BVFeatureQuery ();
      Point3d near = new Point3d();
      Vector3d duv = new Vector3d();
      PolygonalMesh imageMesh = getImageMesh ();
      Face face = query.nearestFaceAlongLine (near, duv, 
         imageMesh.getBVTree (), ray.getOrigin (), ray.getDirection (), 
         0, Double.POSITIVE_INFINITY);

      if (face == null) {
         // intersect line with plane, take nearest point
         face = imageMesh.getFace (0);
         Vector3d nrm = new Vector3d();
         Point3d c = new Point3d();
         face.computeNormal (nrm);
         face.getVertex (0).getWorldPoint (c);
         Plane p = new Plane (nrm, c);

         if (p.intersectRay (c, ray.getDirection (), ray.getOrigin ())) {
            Vector2d uv = new Vector2d();
            face = query.nearestFaceToPoint (near, uv, imageMesh, c);
         } else {
            face = null;
         }
      }

      if (face != null) {
         return createMarkerInternal (near);
      }

      return null;
   }

   public RenderableComponentList<? extends FrameMarker> getMarkers () {
      return markers;
   }

   /*
    * Composite component
    */
   protected ComponentListImpl<ModelComponent> myComponents =
   new ComponentListImpl<ModelComponent>(ModelComponent.class, this);

   private NavpanelDisplay myDisplayMode = NavpanelDisplay.NORMAL;


   // ========== Begin ModelComponent overrides ==========

   public Iterator<? extends HierarchyNode> getChildren() {
      return myComponents.iterator();
   }

   public Iterator<ModelComponent> iterator() {
      return myComponents.iterator();
   }

   public boolean hasChildren() {
      // hasChildren() might be called in the super() constructor, from the
      // property progagation code, before myComponents has been instantiated
      return myComponents != null && myComponents.size() > 0;
   }

   public boolean hasState() {
      return true;
   }

   // ========== End ModelComponent overrides ==========

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

   // ========== End CompositeComponent implementation ==========

   protected void add (ModelComponent comp) {
      myComponents.add (comp);
   }

   protected boolean remove (ModelComponent comp) {
      return myComponents.remove (comp);
   }

   protected void removeAll() {
      myComponents.removeAll();
   }

   protected void notifyStructureChanged (Object comp) {
      notifyStructureChanged (comp, /*stateIsChanged=*/true);
   }

   protected void notifyStructureChanged (Object comp, boolean stateIsChanged) {
      if (comp instanceof CompositeComponent) {
         notifyParentOfChange (
            new StructureChangeEvent ((CompositeComponent)comp,stateIsChanged));
      }
      else if (!stateIsChanged) {
         notifyParentOfChange (
            StructureChangeEvent.defaultStateNotChangedEvent);
      }
      else {
         notifyParentOfChange (
            StructureChangeEvent.defaultEvent);
      }
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
      return false;
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

   public TexturePlaneBase copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {

      TexturePlaneBase ccomp =
      (TexturePlaneBase)super.copy (flags, copyMap);

      ccomp.myComponents = new ComponentListImpl<ModelComponent>(ModelComponent.class, this);
      ccomp.myDisplayMode = myDisplayMode;

      return ccomp;
   }


}
