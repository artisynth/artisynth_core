/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Map;

import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.Shading;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.DrawMode;
import maspack.spatialmotion.SegmentedPlanarCoupling;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.Scan;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.util.ScanToken;

/**
 * Implements a 5 DOF connector in which the origin of C is constrained to lie
 * on a piecewise linear surface defined by a piecewise linear curve in the x-z
 * plane of D.
 *
 * <p>The planar segments are defined by a sequence of x-z coordinate pairs in
 * the D coordinate frame. The number of segments equals the number of pairs
 * minus one, so there must be at least two pairs (which would define a single
 * plane). Segments are assumed to be contiguous, and the normal for each is
 * defined by u X y, where u is a vector in the direction of the segment (from
 * first to last coordinate pair) and y is the direction of the y axis. The
 * first and last plane segments are assumed to extend to infinity.
 */
public class SegmentedPlanarConnector extends BodyConnector 
   implements CopyableComponent {

   private double myPlaneSize;
   private static final double defaultPlaneSize = 1;
   private boolean myRenderNormalReversedP = false;

   private SegmentedPlanarCoupling mySegPlaneCoupling;
   private Point3d[] myRenderVtxs;
   private float[] myRenderCoords = new float[3];

   public static PropertyList myProps =
      new PropertyList (
         SegmentedPlanarConnector.class, BodyConnector.class);

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createRenderProps (host);
      props.setFaceStyle (Renderer.FaceStyle.FRONT_AND_BACK);
      return props;
   }

   protected static VectorNd ZERO_VEC1 = new VectorNd(1);

   static {
      myProps.addReadOnly (
         "activation getPlanarActivation", "activation of planar constraint");
      myProps.add (
         "unilateral isUnilateral *", "unilateral constraint flag", false);
      myProps.add ("planeSize * *", "renderable size of the plane", null);
      myProps.addReadOnly (
         "engaged", "true if the coupling's constraint engaged");
      myProps.get ("renderProps").setDefaultValue (defaultRenderProps(null));
      myProps.get ("compliance").setDefaultValue (ZERO_VEC1);
      myProps.get ("damping").setDefaultValue (ZERO_VEC1);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void setDefaultValues() {
      super.setDefaultValues();
      myPlaneSize = defaultPlaneSize;
      setRenderProps (defaultRenderProps (null));
   }

   public double getPlaneSize() {
      return myPlaneSize;
   }

   public void setPlaneSize (double len) {
      myPlaneSize = len;
   }

   public double getPlanarActivation() {
      // segmented planar connector is defined by only one constraint
      return super.getActivation (0);
   }

   private void initializeCoupling() {
      mySegPlaneCoupling = new SegmentedPlanarCoupling();
      setCoupling (mySegPlaneCoupling);
   }

   public int getEngaged() {
      return myCoupling.getConstraint(0).getEngaged();
   }

   /**
    * Creates a {@code SegmentedPlanarConnector} which is not attached to any
    * bodies.  It can subsequently be connected using one of the {@code set}
    * methods.
    */
   public SegmentedPlanarConnector() {
      myTransformDGeometryOnly = true;
      setDefaultValues();
      myRenderVtxs = new Point3d[4];
      for (int i = 0; i < myRenderVtxs.length; i++) {
         myRenderVtxs[i] = new Point3d();
      }
      initializeCoupling();
   }

   /**
    * Creates a {@code SegmentedPlanarConnector} connecting two rigid bodies,
    * {@code bodyA} and {@code bodyB}. If A and B describe the coordinate
    * frames of {@code bodyA} and {@code bodyB}, and then {@code pCA} gives the
    * origin of C with respect to A and {@code TDB} gives the pose of D with
    * respect to B. The planar segments are defined by {@code segs}, as
    * described in the header documentation for this class.
    *
    * @param bodyA rigid body A
    * @param pCA origin of C with respect to A, as seen in A
    * @param bodyB rigid body B (or {@code null})
    * @param TDB transform from frame D to body frame B
    * @param segs
    * segment boundaries, given as pairs of coordinates in the x-z plane
    */
   public SegmentedPlanarConnector (
      RigidBody bodyA, Vector3d pCA,
      RigidBody bodyB, RigidTransform3d TDB, double[] segs) {
      this();
      set (bodyA, pCA, bodyB, TDB, segs);
   }

   /**
    * Creates a {@code SegmentedPlanarConnector} connecting a single rigid
    * body, {@code bodyA}, to ground. If A describes the coordinate frame of
    * {@code bodyA}, then {@code pCA} gives the origin of C with respect to A
    * and {@code TDW} gives the pose of D with respect to world. The planar
    * segments are defined by {@code segs}, as described in the header
    * documentation for this class.
    *
    * @param bodyA rigid body A
    * @param pCA origin of C with respect to A, as seen in A
    * @param TDW transform from frame D to world coordinates
    * @param segs
    * segment boundaries, given as pairs of coordinates in the x-z plane
    */
   public SegmentedPlanarConnector (
      RigidBody bodyA, Vector3d pCA,
      RigidTransform3d TDW, double[] segs) {
      this();
      set (bodyA, pCA, TDW, segs);
   }

   /**
    * Creates a {@code SegmentedPlanarConnector} connecting two connectable
    * bodies, {@code bodyA} and {@code bodyB}. The joint frames D and C are
    * assumed to be initially coincident. The planar segments are defined by
    * {@code segs}, as described in the header documentation for this class.
    *
    * <p>Specifying {@code bodyB} as {@code null} will cause {@code bodyA} to
    * be connected to ground.
    *
    * @param bodyA body A
    * @param bodyB body B
    * @param TDW initial transform from connector frames D and C to world
    * @param segs
    * segment boundaries, given as pairs of coordinates in the x-z plane
    */
   public SegmentedPlanarConnector (
      ConnectableBody bodyA, ConnectableBody bodyB,
      RigidTransform3d TDW, double[] segs) {
      this();
      setBodies (bodyA, bodyB, TDW);
      mySegPlaneCoupling.setSegments (segs);
   }   

   /**
    * Sets this SegmentedPlanarConnector to connect two rigid bodies. The first
    * body (A) is the one in which the contact point is fixed, while the second
    * body (B) is the one in which the planes are fixed. The planar segments
    * are specfified by {@code segs}, as described in the header documentation
    * for this class.
    * 
    * @param bodyA
    * first rigid body
    * @param pCA
    * location of contact point relative to body A
    * @param bodyB
    * second rigid body
    * @param TDB
    * transformation from frame D to body B
    * @param segs
    * segment boundaries, given as pairs of coordinates in the x-z plane.
    */
   public void set (
      RigidBody bodyA, Vector3d pCA,
      RigidBody bodyB, RigidTransform3d TDB, double[] segs) {
      doset (bodyA, pCA, bodyB, TDB, segs);
   }

   /**
    * Sets this SegmentedPlanarConnector to connect a rigid body with the world
    * frame. The contact point is fixed in the body frame, while the planes are
    * fixed in the world frame. The planar segments are specfified by {@code
    * segs}, as described in the header documentation for this class.
    * 
    * @param bodyA
    * rigid body
    * @param pCA
    * location of contact point relative to body
    * @param TDW
    * transformation from frame D to world coordinates
    * @param segs
    * segment boundaries, given as pairs of coordinates in the x-z plane.
    */
   public void set (
      RigidBody bodyA, Vector3d pCA, RigidTransform3d TDW, double[] segs) {
      doset (bodyA, pCA, null, TDW, segs);
   }

   private void doset (
      RigidBody bodyA, Vector3d pCA, RigidBody bodyB, RigidTransform3d TDB,
      double[] segs) {
      RigidTransform3d TCA = new RigidTransform3d();
      TCA.p.set (pCA);
      setBodies (bodyA, TCA, bodyB, TDB);
      mySegPlaneCoupling.setSegments (segs);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      RigidTransform3d TDW = getCurrentTDW();
      for (int i = 0; i < mySegPlaneCoupling.numPlanes(); i++) {
         computeRenderVtxs (i, TDW);
         for (int k = 0; k < myRenderVtxs.length; k++) {
            myRenderVtxs[k].updateBounds (pmin, pmax);
         }
      }
   }

   public RenderProps createRenderProps() {
      return defaultRenderProps (this);
   }

   public void prerender (RenderList list) {
      super.prerender (list);
      RigidTransform3d TFW = getCurrentTCW();
      myRenderCoords[0] = (float)TFW.p.x;
      myRenderCoords[1] = (float)TFW.p.y;
      myRenderCoords[2] = (float)TFW.p.z;
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);

      Vector3d nrm = new Vector3d (0, 0, 1);
      RigidTransform3d TDW = getCurrentTDW();

      RenderProps props = myRenderProps;

      Shading savedShading = renderer.setPropsShading (props);
      renderer.setFaceColoring (props, isSelected());
      renderer.setFaceStyle (props.getFaceStyle());
      ArrayList<Plane> planes = mySegPlaneCoupling.getPlanes();

      for (int i = 0; i < planes.size(); i++) {
         Plane plane = planes.get (i);
         nrm.set (plane.getNormal());
         computeRenderVtxs (i, TDW);

         renderer.beginDraw (DrawMode.TRIANGLE_STRIP);
         if (myRenderNormalReversedP) {
            renderer.setNormal (-nrm.x, -nrm.y, -nrm.z);
         }
         else {
            renderer.setNormal (nrm.x, nrm.y, nrm.z);
         }
         renderer.addVertex (myRenderVtxs[3]);
         renderer.addVertex (myRenderVtxs[0]);
         renderer.addVertex (myRenderVtxs[2]);
         renderer.addVertex (myRenderVtxs[1]);
         renderer.endDraw();
      }
      renderer.setShading (savedShading);
      renderer.setFaceStyle (FaceStyle.FRONT);
      renderer.drawPoint (myRenderProps, myRenderCoords, isSelected());
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "segs")) {
         double[] segs = Scan.scanDoubles (rtok);
         mySegPlaneCoupling.setSegments (segs);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      pw.println ("segs=[");
      ArrayList<Point3d> segPnts = mySegPlaneCoupling.getSegmentPoints();
      for (int i = 0; i < segPnts.size(); i++) {
         Point3d p = segPnts.get (i);
         pw.println ("  " + fmt.format (p.x) + " " + fmt.format (p.z));
      }
      pw.println ("]");
      super.writeItems (pw, fmt, ancestor);
   }

   protected void computeRenderVtxs (int planeIdx, RigidTransform3d TDW) {
      ArrayList<Point3d> segPnts = mySegPlaneCoupling.getSegmentPoints();

      if (planeIdx >= segPnts.size() - 1) {
         throw new InternalErrorException ("index " + planeIdx
         + " exceeds number of planes");
      }
      Point3d p0 = segPnts.get (planeIdx);
      Point3d p1 = segPnts.get (planeIdx + 1);

      Vector3d yaxis = Vector3d.Y_UNIT;

      myRenderVtxs[0].set (p0);
      myRenderVtxs[0].scaledAdd (-myPlaneSize / 2, yaxis, myRenderVtxs[0]);

      myRenderVtxs[3].set (p0);
      myRenderVtxs[3].scaledAdd (myPlaneSize / 2, yaxis, myRenderVtxs[3]);

      myRenderVtxs[1].set (p1);
      myRenderVtxs[1].scaledAdd (-myPlaneSize / 2, yaxis, myRenderVtxs[1]);

      myRenderVtxs[2].set (p1);
      myRenderVtxs[2].scaledAdd (myPlaneSize / 2, yaxis, myRenderVtxs[2]);

      for (int i = 0; i < myRenderVtxs.length; i++) {
         myRenderVtxs[i].transform (TDW);
      }
   }

   public void scaleDistance (double s) {
      super.scaleDistance (s);
      myPlaneSize *= s;
   }

   public boolean isRenderNormalReversed() {
      return myRenderNormalReversedP;
   }

   public void setRenderNormalReversed (boolean reversed) {
      myRenderNormalReversedP = reversed;
   }

   public boolean isUnilateral() {
      return mySegPlaneCoupling.isUnilateral();
   }

   public void setUnilateral (boolean unilateral) {
      if (isUnilateral() != unilateral) {
         mySegPlaneCoupling.setUnilateral (unilateral);
         myStateVersion++;
         notifyParentOfChange (StructureChangeEvent.defaultEvent);
      }
   }

   @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      SegmentedPlanarConnector copy =
         (SegmentedPlanarConnector)super.copy (flags, copyMap);
      copy.initializeCoupling();
      copy.setPlaneSize (myPlaneSize);
      copy.setUnilateral (isUnilateral());
      ArrayList<Point3d> segPnts = mySegPlaneCoupling.getSegmentPoints();
      double[] segs = new double[segPnts.size() * 2];
      for (int i = 0; i < segPnts.size(); i++) {
         Point3d pnt = segPnts.get (i);
         segs[i * 2] = pnt.x;
         segs[i * 2 + 1] = pnt.z;
      }
      copy.mySegPlaneCoupling.setSegments (segs);
      return copy;
   }

}
