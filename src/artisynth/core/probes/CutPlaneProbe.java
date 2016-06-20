/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import maspack.geometry.HalfEdge;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.AxisAngle;
import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.matrix.PolarDecomposition3d;
import maspack.properties.PropertyList;
import maspack.render.Dragger3dBase;
import maspack.render.Dragger3dEvent;
import maspack.render.Dragger3dListener;
import maspack.render.Renderer;
import maspack.render.Dragger3d.DraggerType;
import maspack.render.GL.GLViewer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Rotator3d;
import maspack.render.Translator3d;
import maspack.render.Transrotator3d;
import maspack.util.InternalErrorException;
import artisynth.core.driver.Main;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.modelbase.TransformableGeometry;

/**
 * Basic plane probe that can display information as it cuts through a model
 * @author Antonio
 *
 */
public abstract class CutPlaneProbe extends OutputProbe
   implements RenderableComponent, Dragger3dListener {

   // defaults to X-Z plane
   RenderProps myRenderProps = null;
   RigidTransform3d XGridToWorld = new RigidTransform3d();
   protected Dragger3dBase myDragger;
   GLViewer myViewer = null;
   
   protected boolean myDisplayValid = false;

   protected static AxisAngle myDefaultOrientation = new AxisAngle(
      1, 0, 0, Math.PI / 2);
   protected static Vector2d myDefaultSize = new Vector2d(1, 1);
   protected static Vector2d myDefaultResolution = new Vector2d(20, 20);

   protected Vector2d mySize = new Vector2d(myDefaultSize);
   protected Vector2d myResolution = new Vector2d(myDefaultResolution);

   protected Plane myPlane = new Plane();
   protected PolygonalMesh myPlaneSurface = null;
   ArrayList<Vertex3d[]> mySurfaceBoundaries;

   public static PropertyList myProps =
      new PropertyList(CutPlaneProbe.class, OutputProbe.class);
   static {
      myProps.add("renderProps * *", "render properties", null);
      myProps.add("dragger * *", "dragger type", DraggerType.None);
      myProps.add("resolution", "plane resolution", myDefaultResolution);
      myProps.add("size", "plane size", myDefaultSize);
      myProps.add("position", "position of the grid coordinate frame", null);
      myProps.get("position").setAutoWrite(false);
      myProps.add(
         "orientation", "orientation of the grid coordinate frame",
         myDefaultOrientation);
      myProps.get("orientation").setAutoWrite(false);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public CutPlaneProbe () {
      init();
      rebuildMesh();
      updateMeshDisplay();
   }

   /**
    * Creates a  basic plane surface which can display information
    * @param center center of plane
    * @param orientation orientation of plane (originally on x-y)
    * @param size 2D size of plane
    */
   public CutPlaneProbe (Point3d center, AxisAngle orientation, Vector2d size) {
      init();
      setPosition(center);
      setOrientation(orientation);
      setSize(size);
      // rebuildMesh(); // triggered by setSize()
      updateMeshDisplay();
   }

   private void init() {
      setRenderProps(createRenderProps()); // initialize render properties
      setViewer(Main.getMain().getViewer()); // initialize to main viewer;
      setOrientation(myDefaultOrientation);
   }
   

   /**
    * Reconstructs a plane mesh according to the resolution
    */
   protected void rebuildMesh() {
      myPlaneSurface = MeshFactory.createRectangle(mySize.x, mySize.y,
         (int)myResolution.x, (int)myResolution.y, /*texture=*/true);
      myPlaneSurface.setMeshToWorld(XGridToWorld);
      mySurfaceBoundaries = extractBoundaries(myPlaneSurface);
   }
   
   private void resizeDragger() {
      if (myDragger != null) {
         Point3d pmin =
            new Point3d(
               Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
               Double.POSITIVE_INFINITY);
         Point3d pmax =
            new Point3d(
               Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
               Double.NEGATIVE_INFINITY);
         myPlaneSurface.updateBounds(pmin, pmax);
         double s = pmax.distance(pmin)/8;
         myDragger.setSize(s);
      }
   }

   /**
    * Gets the boundaries of the supplied display mesh
    */
   protected static ArrayList<Vertex3d[]> extractBoundaries(PolygonalMesh surface) {

      ArrayList<Vertex3d[]> boundaries = new ArrayList<Vertex3d[]>(1);
      ArrayList<Vertex3d> vtxList = null;
      LinkedList<HalfEdge> borderEdges = new LinkedList<HalfEdge>(surface.findBorderEdges());

      while (borderEdges.size() > 0) {
         vtxList = new ArrayList<Vertex3d>();
         HalfEdge borderEdge = borderEdges.get(0);
         borderEdges.remove(0);
         
         vtxList.add(borderEdge.head);
         vtxList.add(borderEdge.tail);
         Vertex3d lastVtx = borderEdge.tail;

         while (lastVtx != vtxList.get(0)) {
            
            Iterator<HalfEdge> eit = borderEdge.tail.getIncidentHalfEdges();
            HalfEdge he;
            while (eit.hasNext()) {
               he = eit.next();
               if (he != borderEdge && he.opposite == null) {
                  borderEdge = he;
                  break;
               }
            }

            if (borderEdges.contains(borderEdge)) {
               borderEdges.remove(borderEdge);
            }

            lastVtx = borderEdge.tail;
            
            if (!vtxList.contains(lastVtx) || lastVtx == vtxList.get(0)) {
               vtxList.add(lastVtx);
            } else {
               System.out.println("boundary not closed");
               break;
            }

         }

         boundaries.add(vtxList.toArray(new Vertex3d[vtxList.size()]));

      }

      return boundaries;

   }   
   
   @Override
   public void initialize(double t) {
      updateMeshDisplay();
   }

   @Override
   public synchronized void apply(double t) {
      updateMeshDisplay();
   }

   @Override
   public void prerender(RenderList list) {
      if (myPlaneSurface != null) {
         myPlaneSurface.prerender (myRenderProps);
      }
   }

   public void render(Renderer renderer, int flags) {
      if (myPlaneSurface != null) {
         //renderer.drawMesh(myRenderProps, myPlaneSurface, flags);
         myPlaneSurface.render (renderer, myRenderProps, flags);
      }

      if (isSelected()) {
         drawBoundary(renderer, true);
         if (myDragger != null) {
            myDragger.render(renderer, flags);
         }
      }
   }

   protected void drawBoundary(Renderer renderer, boolean selected) {
      for (Vertex3d[] boundary : mySurfaceBoundaries) {
         drawContour(boundary, renderer, selected);
      }
   }

   protected void drawContour(Vertex3d[] contour, Renderer renderer,
      boolean selected) {

      float[] coords0 = new float[3];
      float[] coords1 = new float[3];
      float[] tmp;

      Vertex3d a = contour[0];
      Vertex3d b;
      getRenderCoords(coords0, a.getWorldPoint());

      for (int i = 1; i < contour.length; i++) {
         b = contour[i];
         getRenderCoords(coords1, b.getWorldPoint());
         renderer.drawLine(myRenderProps, coords0, coords1, selected);

         // swap
         a = b;
         tmp = coords0;
         coords0 = coords1;
         coords1 = tmp;
      }
   }

   protected void drawContour(List<? extends Point3d> contour,
      Renderer renderer, boolean selected) {

      float[] coords0 = new float[3];
      float[] coords1 = new float[3];
      float[] tmp;

      Iterator<? extends Point3d> cit = contour.iterator();

      Point3d a = cit.next();
      getRenderCoords(coords0, a);
      while (cit.hasNext()) {
         Point3d b = cit.next();
         getRenderCoords(coords1, b);
         renderer.drawLine(myRenderProps, coords0, coords1, selected);

         // swap
         a = b;
         tmp = coords0;
         coords0 = coords1;
         coords1 = tmp;
      }
   }

   protected float[] getRenderCoords(float[] out, Point3d pnt) {
      out[0] = (float)pnt.x;
      out[1] = (float)pnt.y;
      out[2] = (float)pnt.z;
      return out;
   }

   /**
    * Gets the 3D position of the centre of the display
    */
   public Point3d getPosition() {
      return new Point3d(XGridToWorld.p);
   }

   /**
    * Sets the 3D position of the centre of the display
    */
   public void setPosition(Point3d pos) {
      RigidTransform3d X = new RigidTransform3d(XGridToWorld);
      X.p.set(pos);
      setGridToWorld(X);
   }

   /**
    * Gets the orientation of the display
    */
   public AxisAngle getOrientation() {
      AxisAngle axisAng = new AxisAngle();
      XGridToWorld.R.getAxisAngle(axisAng);
      return axisAng;
   }

   /**
    * Sets the orientation of the display
    */
   public void setOrientation(AxisAngle axisAng) {
      RigidTransform3d X = new RigidTransform3d(XGridToWorld);
      X.R.setAxisAngle(axisAng);
      setGridToWorld(X);
   }

   /**
    * Sets the transform between the display grid and World
    */
   public void setGridToWorld(RigidTransform3d X) {
      XGridToWorld.set(X);

      if (myPlaneSurface != null) {
         myPlaneSurface.setMeshToWorld(X);
      }
      if (myDragger != null) {
         myDragger.setDraggerToWorld(XGridToWorld);
      }
      if (myViewer != null) {
         myViewer.repaint();
      }
   }

   /**
    * Updates the display orientation
    */
   protected void updateMeshDisplay() {
      myPlaneSurface.setMeshToWorld(XGridToWorld);
      myDisplayValid = true;
   }

   /**
    * Gets the size of the display grid
    */
   public Vector2d getSize() {
      return new Vector2d(mySize);
   }

   /**
    * Sets the size of the display grid
    */
   public void setSize(Vector2d size) {
      if (!mySize.equals(size)) {
         
         if (mySize.x == 0 || mySize.y == 0) {
            myPlaneSurface = null;
         }
         
         if (myPlaneSurface == null) {
            mySize.set(size);
            rebuildMesh();
         } else {
            double xScale = size.x / mySize.x;
            double yScale = size.y / mySize.y;
            myPlaneSurface.scale(xScale, yScale, 1.0);
            mySize.set(size);
         }
         resizeDragger();
         updateMeshDisplay();
      }
   }

   /**
    * Gets the resolution of the display, integers (nx,ny) corresponding
    * to the number of divisions along the x and y axes 
    */
   public Vector2d getResolution() {
      return new Vector2d(myResolution);
   }

   /**
    * Sets the resolution fo teh display, integers (nx, ny) corresponding
    * to the number of divisions along the x and y axes.  This triggers
    * a rebuild of the mesh.
    */
   public void setResolution(Vector2d res) {
      if (!myResolution.equals(res)) {
         myResolution = new Vector2d((int)res.x, (int)res.y);
         rebuildMesh();
         updateMeshDisplay();
      }
   }

   public RenderProps createRenderProps() {
      RenderProps props = RenderProps.createLineFaceProps(this);
      props.setFaceStyle(FaceStyle.FRONT_AND_BACK);
      props.setVisible(true);
      return props;
   }

   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   public void setRenderProps(RenderProps props) {
      myRenderProps =
         RenderableComponentBase.updateRenderProps(this, myRenderProps, props);
   }

   public void transformGeometry (
      AffineTransform3dBase X, PolarDecomposition3d pd,
      Map<TransformableGeometry,Boolean> transformSet, int flags) {
   }
   
   public int getTransformableDescendents (
      List<TransformableGeometry> list) {
      return 0;
   }
   
   public void transformGeometry(AffineTransform3dBase X) {
   }

   public boolean isSelectable() {
      return true;
   }

   public int numSelectionQueriesNeeded() {
      return -1;
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   public int getRenderHints() {
      int code = 0;
      if (myRenderProps != null && myRenderProps.isTransparent()) {
         code |= TRANSPARENT;
      }
      return code;
   }

   @Override
   public void updateBounds(Vector3d pmin, Vector3d pmax) {
      if (myPlaneSurface != null) {
         myPlaneSurface.updateBounds(pmin, pmax);
      }
   }

   /**
    * Sets the viewer associated with this display, for purposes of
    * interacting with the dragger
    */
   void setViewer(GLViewer viewer) {
      if (myViewer != viewer) {
         if (myDragger != null) {
            if (myViewer != null) {
               myViewer.removeDragger(myDragger);
            }
            if (viewer != null) {
               viewer.addDragger(myDragger);
            }
         }
         myViewer = viewer;
         viewer.repaint();
      }
   }

   /**
    * Gets the viewer associated with the display/dragger
    */
   GLViewer getViewer() {
      return myViewer;
   }
   

   /**
    * Returns the dragger type
    */
   public DraggerType getDragger() {
      if (myDragger == null) {
         return DraggerType.None;
      }
      else if (myDragger instanceof Translator3d) {
         return DraggerType.Translator;
      }
      else if (myDragger instanceof Transrotator3d) {
         return DraggerType.Transrotator;
      }
      else if (myDragger instanceof Rotator3d) {
         return DraggerType.Rotator;
      }
      else {
         throw new InternalErrorException(
            "Dragger set to unsupported type " +
               myDragger.getClass());
      }
   }

   /**
    * Sets the dragger type for manipulating the display
    */
   public void setDragger(DraggerType type) {
      if (type != getDragger()) {
         if (myDragger != null && myViewer != null) {
            myViewer.removeDragger(myDragger);
            myDragger.removeListener(this);
         }
         switch (type) {
            case None: {
               myDragger = null;
               break;
            }
            case Translator: {
               myDragger = new Translator3d();
               break;
            }
            case Transrotator: {
               myDragger = new Transrotator3d();
               break;
            }
            case Rotator: {
               myDragger = new Rotator3d();
               break;
            }
            default: {
               // throw new IllegalArgumentException (
               // "dragger " + type + " not supported");
            }
         }
         if (myDragger != null) {
            myDragger.setDraggerToWorld(XGridToWorld);
            myDragger.addListener(this);
            resizeDragger();
            //myDragger.setVisible(false);
         }

         if (myViewer != null) {
            if (myDragger != null) {
               myViewer.addDragger(myDragger);
            }
            myViewer.repaint();
         }
      }
   }

   /**
    * Centre the probe in the middle of the viewer
    */
   public void centerInViewer() {
      if (myViewer != null) {
         RigidTransform3d X = new RigidTransform3d(XGridToWorld);
         X.p.set(myViewer.getCenter());
         setGridToWorld(X);
      }
   }

   /**
    * Resets to a default viewer centre orientation
    */
   public void resetInViewer() {
      if (myViewer != null) {
         setGridToWorld(myViewer.getCenterToWorld());
      }
   }

   /**
    * Gets the plane along which the display lies
    */
   public void getPlane(Plane plane) {
      RigidTransform3d X = XGridToWorld;
      plane.set(-X.R.m02, -X.R.m12, -X.R.m22, -X.R.m02 * X.p.x - X.R.m12
         * X.p.y - X.R.m22 * X.p.z);
   }

   /**
    * Gets the plane along which the display lies
    *
    * @return plane for the display
    */
   public Plane getPlane() {
      getPlane(myPlane);
      return myPlane;
   }
   
   /**
    * Sets the plane along which the display lies
    */ 
   public void setPlane(Plane plane) {
      XGridToWorld.R.rotateZDirection(plane.getNormal());
      Point3d p = new Point3d(XGridToWorld.p);
      plane.project(p, p);
      XGridToWorld.p.set(p);
      setGridToWorld(XGridToWorld); // refresh other values
   }
   
   /**
    * Sets the plane along which the display lies
    */ 
   public void setPlane(Vector3d n, double offset) {
      myPlane.set(n, offset);
      setPlane(myPlane);
   }

   @Override
   public void draggerAdded(Dragger3dEvent e) {
   }

   @Override
   public void draggerBegin(Dragger3dEvent e) {
   }

   @Override
   public synchronized void draggerMove(Dragger3dEvent e) {
      XGridToWorld.mul(
         (RigidTransform3d)e.getIncrementalTransform());
      myDisplayValid = false;
      updateMeshDisplay();
   }

   @Override
   public void draggerEnd(Dragger3dEvent e) {
   }

   @Override
   public void draggerRemoved(Dragger3dEvent e) {
   }


}
