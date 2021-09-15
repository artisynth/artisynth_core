/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.geometry.GeometryTransformer;
import maspack.properties.*;
import maspack.render.*;
import maspack.render.Renderer.Shading;
import maspack.util.*;
import maspack.util.ClassAliases;
import maspack.matrix.*;
import artisynth.core.util.*;
import artisynth.core.modelbase.*;

import java.io.*;
import java.util.*;

public class PointForce extends ModelComponentBase 
   implements RenderableComponent, ScalableUnits, 
   ForceComponent, TransformableGeometry, CopyableComponent {
   
   protected Point myPnt;

   protected Vector3d myForce = new Vector3d();
   protected Vector3d myU = new Vector3d (0, 0, 1.0);
   protected double myMag = 0;

   double myAxisLength = 1.0;

   // If true, causes arrow to be rendered outward from the point
   public static boolean DEFAULT_RENDER_OUTWARD = false;
   protected boolean myRenderOutward = DEFAULT_RENDER_OUTWARD;

   // If > 0, scales the force norm to determine the axis length
   public static double DEFAULT_LENGTH_FORCE_RATIO = 0;
   protected double myForceLengthRatio = DEFAULT_LENGTH_FORCE_RATIO;

   // If > 0, scales the axis length to determine its radius
   public static double DEFAULT_AXIS_RADIUS_RATIO = 0;
   protected double myAxisRadiusRatio = DEFAULT_AXIS_RADIUS_RATIO;

   // Allows scaling of force to account for units (1000 for mm)
   public static double DEFAULT_FORCE_SCALING = 1;
   protected double forceScaling = DEFAULT_FORCE_SCALING; 
   Vector3d ftmp = new Vector3d();

   public static PropertyList myProps =
      new PropertyList (PointForce.class, ModelComponentBase.class);

   static {
      myProps.add ("renderProps * *", "renderer properties", null);
      myProps.add ("force", "3d force vector", null);
      myProps.add ("direction", "3d force direction", null, "NW");
      myProps.add ("magnitude", "force magnitude", 0d, "NW");
      myProps.add ("axisLength", "length of rendered frame axes", 1f);
      myProps.add (
         "forceLengthRatio",
         "if > 0, scales force norm to determine the axis length",
         DEFAULT_LENGTH_FORCE_RATIO);
      myProps.add (
         "axisRadiusRatio",
         "if > 0, scales the axis length to determine its radius",
         DEFAULT_AXIS_RADIUS_RATIO);
      myProps.add (
         "renderOutward", "if true, draw arrow away from the point",
         DEFAULT_RENDER_OUTWARD);
      myProps.add (
         "forceScaling",
         "scale factor from normial force units", DEFAULT_FORCE_SCALING, "%.8g");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public PointForce() {
      super();
      setRenderProps (createRenderProps());
   }

   public PointForce (String name) {
      this();
      setName (name);
   }

   public PointForce (Point p) {
      this();
      setPoint (p);
   }

   public PointForce (Vector3d f, Point p) {
      this();
      setPoint (p);
      setForce (f);
   }

   public Point getPoint() {
      return myPnt;
   }

   public void setPoint (Point pnt) {
      myPnt = pnt;
   }

   public void applyForces (double t) {
      myPnt.addForce (myForce);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "point", tokens)) {
         return true;
      }
//      else if (scanAttributeName (rtok, "force")) {
//         myForce.scan (rtok);
//         return true;
//      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      myForce.setZero();
      super.scan (rtok, ref);
   }


   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "point")) {
         setPoint (postscanReference (
            tokens, Point.class, ancestor));
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }
   
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.println ("point=" +
                  ComponentUtils.getWritePathName (ancestor, myPnt));
//      if (!myForce.equals (Vector3d.ZERO)) {
//         pw.print ("force=");
//         myForce.write (pw, fmt);
//         pw.println ("");
//      }
      super.writeItems (pw, fmt, ancestor);
   }

   /* ======== Renderable implementation ======= */

   protected RenderProps myRenderProps;

   private double getEffectiveAxisLength() {
      if (myForceLengthRatio > 0) {
         return myForceLengthRatio*myMag;
      }
      else {
         return myAxisLength;
      }
   }

   private float[] getTipCoords() {
      if (myRenderOutward) {
         Vector3d tip = new Vector3d();
         tip.scaledAdd (getEffectiveAxisLength(), myU, myPnt.getPosition());
         return new float[] {(float)tip.x, (float)tip.y,(float) tip.z};
      }
      else {
         return myPnt.myRenderCoords;         
      }
   }

   private float[] getEndCoords() {
      if (myRenderOutward) {
         return myPnt.myRenderCoords; 
      }
      else {
         Vector3d end = new Vector3d();
         end.scaledAdd (-getEffectiveAxisLength(), myU, myPnt.getPosition());
         return new float[] {(float)end.x, (float)end.y,(float) end.z};
      }
   }

   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   public void setRenderProps (RenderProps props) {
      myRenderProps =
         RenderableComponentBase.updateRenderProps (this, myRenderProps, props);
   }

   public RenderProps createRenderProps() {
      return RenderProps.createLineProps (this);
   }

   public void prerender (RenderList list) {
      // nothing to do
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      myPnt.updateBounds (pmin, pmax);
      Vector3d other = new Vector3d();
      if (myRenderOutward) {
         other.scaledAdd (getEffectiveAxisLength(), myU, myPnt.getPosition());
      }
      else {
         other.scaledAdd (-getEffectiveAxisLength(), myU, myPnt.getPosition());
      }
      other.updateBounds (pmin, pmax);
   }

   public int getRenderHints() {
      int code = 0;
      if (myRenderProps != null && myRenderProps.isTransparent()) {
         code |= TRANSPARENT;
      }
      return code;
   }

   public boolean isSelectable() {
      return true;
   }

   public int numSelectionQueriesNeeded() {
      return -1;
   }

   private void drawArrow (
      Renderer renderer, float[] pnt0, float[] pnt1) {
      //boolean savedHighlighting = getHighlighting();
   }

   public void render (Renderer renderer, int flags) {
      if (myMag > 0) {
         Shading savedShading = renderer.setLineShading (myRenderProps);
         renderer.setLineColoring (myRenderProps, isSelected());     
         double rad;
         if (myAxisRadiusRatio > 0) {
            rad = myAxisRadiusRatio*getEffectiveAxisLength();
         }
         else {
            rad = myRenderProps.getLineRadius();
         }
         renderer.drawArrow (
            getEndCoords(), getTipCoords(), rad, true /* capped */);
         renderer.setShading(savedShading);
      }
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   protected PointForce newComponent (String classId)
      throws InstantiationException, IllegalAccessException {
      return (PointForce)ClassAliases.newInstance (classId, PointForce.class);
   }

   public void scaleDistance (double s) {
      if (myRenderProps != null) {
         RenderableUtils.cloneRenderProps (this);
         myRenderProps.scaleDistance (s);
      }
      myMag *= s;
      myForce.scale (myMag, myU);
      myAxisLength *= s;
   }

   public void scaleMass (double s) {
      myMag *= s;
      myForce.scale (myMag, myU);
      setForce (myForce);
   }
   public Vector3d getDirection() {
      return myU;
   }

   public void setDirection (Vector3d dir) {
      if (dir.norm() == 0) {
         return;
      }
      myU.normalize (dir);
      myForce.scale (myMag, myU);
   }
   
//   public RigidTransform3d getPose() {
//      return new RigidTransform3d (myXPF);
//   }

   public Vector3d getForce() {
      ftmp.scale (1 / forceScaling, myForce);
      return ftmp;
   }

   public void setForce (Vector3d f) {
      myForce.scale (forceScaling, f);
      updateDirectionAndMag();
   }

   public double getMagnitude() {
      return myMag / forceScaling;
   }

   public void setMagnitude (double mag) {
      myMag = mag * forceScaling;
      myForce.scale (myMag, myU);
   }

   public boolean getRenderOutward() {
      return myRenderOutward;
   }

   public void setRenderOutward (boolean enable) {
      myRenderOutward = enable;
   }

   public double getAxisLength() {
      return myAxisLength;
   }

   public void setAxisLength (double len) {
      myAxisLength = len;
   }

   public double getForceLengthRatio() {
      return myForceLengthRatio;
   }

   public void setForceLengthRatio (double r) {
      myForceLengthRatio = r;
   }

   public double getAxisRadiusRatio() {
      return myAxisRadiusRatio;
   }

   public void setAxisRadiusRatio (double r) {
      myAxisRadiusRatio = r;
   }

   public double getForceScaling() {
      return forceScaling;
   }

   public void setForceScaling (double forceScaling) {
      // adjust current force based on new force scaling
      myMag *= forceScaling/this.forceScaling;
      this.forceScaling = forceScaling;
      myForce.scale (myMag, myU);
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {

      if (context.isTransformed (myPnt) && !gtr.isAffine()) {
         System.out.println (
            "WARNING: non-linear geometric transformation not fully " +
            "supported for PointForce; results may be inaccurate");
      }
      gtr.transformNormal (myForce, myPnt.getPosition());
      updateDirectionAndMag();
   }
   
   private void updateDirectionAndMag() {
      myMag = myForce.norm();
      if (myMag != 0) {
         myU.scale (1/myMag, myForce);
      }
      else {
         myU.setZero();
      }
   }
   
   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      // no dependencies
   }

   public void transformGeometry (AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   public void addSolveBlocks (SparseNumberedBlockMatrix M) {
      // jacobian is zero
   }

   public void addPosJacobian (SparseNumberedBlockMatrix M, double s) {
      // jacobian is zero
   }

   public void addVelJacobian (SparseNumberedBlockMatrix M, double s) {
      // jacobian is zero
   }

   public int getJacobianType() {
      // jacobian is zero
      return Matrix.SPD;
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean isDuplicatable() {
      return true;
   }

   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      if (myPnt != null) {
         if (!ComponentUtils.addCopyReferences (refs, myPnt, ancestor)) {
            return false;
         }
      }
      return true;
   }

   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      PointForce comp = (PointForce)super.copy (flags, copyMap);
      Point pnt0 = (Point)ComponentUtils.maybeCopy (flags, copyMap, myPnt);
      comp.setPoint (pnt0);

      comp.setRenderProps (myRenderProps);
      comp.forceScaling = forceScaling;
      comp.myU = new Vector3d();
      comp.myMag = 0.0;
      comp.myForce = new Vector3d();
      comp.myForce.set (myForce);

      return comp;
   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      if (myPnt != null) {
         refs.add (myPnt);
      }
   }

//   @Override
//   public void connectToHierarchy () {
//      super.connectToHierarchy ();
//      if (myPnt != null) {
//         myPnt.addBackReference (this);
//      }
//   }
//
//   @Override
//   public void disconnectFromHierarchy() {
//      super.disconnectFromHierarchy();
//      if (myPnt != null) {
//         myPnt.removeBackReference (this);
//      }
//   }

}
