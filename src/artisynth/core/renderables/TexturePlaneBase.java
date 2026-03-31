package artisynth.core.renderables;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;

import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.HasSurfaceMesh;
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
   implements TransformableGeometry, HasSurfaceMesh {

   protected TexturePlaneBase() {
      super();
   }

   protected TexturePlaneBase(String name) {
      this();
      setName (name);
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
      if (getImageMesh() != null) {
         getImageMesh().setMeshToWorld (getPose ());
      }
   }

   public void prerender (RenderList list) {
      super.prerender (list);
      getImageMeshInfo().prerender (myRenderProps);
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

   // ========== Begin ModelComponent overrides ==========

   public Iterator<ModelComponent> iterator() {
      return myComponents.iterator();
   }

   public boolean hasState() {
      return true;
   }

   @Override
   public boolean isDuplicatable() {
      // need to finish copy() before this can return true
      return false;
   }

   public TexturePlaneBase copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {

      // XXX need to finish
      TexturePlaneBase ccomp =
      (TexturePlaneBase)super.copy (flags, copyMap);

      return ccomp;
   }


}
