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

   public static double DEFAULT_FORCE_SCALING = 1;
   
   protected Point myPnt;
   protected Point3d myTail = new Point3d();
   float[] myTailCoords = new float[3];

   protected Vector3d myForce = new Vector3d();
   protected Vector3d myU = new Vector3d (0, 0, 1.0);
   protected double myMag = 0;
   private final double tol = 1e-6;
   protected RigidTransform3d myXPF = new RigidTransform3d();
   RigidTransform3d XPFnew = new RigidTransform3d();
   Point3d up = new Point3d (0, 0, 1);

   double myAxisLength = 1.0;

   // Allows scaling of force to account for units (1000 for mm)
   protected double forceScaling = DEFAULT_FORCE_SCALING; 
   Vector3d ftmp = new Vector3d();

   public static PropertyList myProps =
      new PropertyList (PointForce.class, ModelComponentBase.class);

   static {
      myProps.add ("renderProps * *", "renderer properties", null);
      myProps.add ("force", "3d force vector", null);
      myProps.add ("direction", "3d force direction", null);
      myProps.add ("magnitude", "force magnitude", 0d);
      myProps.add ("axisLength", "length of rendered frame axes", 1f);
      myProps.add (
         "forceScaling * *", "scale factor from normial force units", DEFAULT_FORCE_SCALING,
         "%.8g");
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
      else if (scanAttributeName (rtok, "force")) {
         myForce.scan (rtok);
         return true;
      }
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
      if (!myForce.equals (Vector3d.ZERO)) {
         pw.print ("force=");
         myForce.write (pw, fmt);
         pw.println ("");
      }
      super.writeItems (pw, fmt, ancestor);
   }

   /* ======== Renderable implementation ======= */

   protected RenderProps myRenderProps = null;

   private float[] getRenderCoords0() {
      return myPnt.myRenderCoords;
   }

   private float[] getRenderCoords1() {
      myTail.scaledAdd (-myAxisLength, myU, myPnt.getPosition());

      myTailCoords[0] = (float)myTail.x;
      myTailCoords[1] = (float)myTail.y;
      myTailCoords[2] = (float)myTail.z;
      return myTailCoords;
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
      myTail.updateBounds (pmin, pmax);
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

   public void render (Renderer renderer, int flags) {
      if (myMag > 0) {
      renderer.drawArrow (
         myRenderProps, getRenderCoords1(), getRenderCoords0(),
         true /* capped */, isSelected());
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
      forceScaling *= s;
      setForce (myForce);
      myAxisLength *= s;
   }

   public void scaleMass (double s) {
      forceScaling *= s;
      setForce (myForce);
   }

   private void updateForceVector() {
      myU.transform (myXPF, up);
      myU.normalize();
      if (myMag < tol)
         myForce.setZero();
      else
         myForce.scale (myMag, myU);

   }

   public Vector3d getDirection() {
      return myU;
   }

   public void setDirection (Vector3d dir) {
      if (dir.norm() < tol)
         return;

      Vector3d k = new Vector3d(), j = new Vector3d(), i = new Vector3d();
      k.normalize (dir);
      i.cross (up, k);
      if (i.norm() > 1e-6) {
         i.normalize();
         j.cross (k, i);

         myXPF.R.setColumn (0, i);
         myXPF.R.setColumn (1, j);
         myXPF.R.setColumn (2, k);
      }
      else {
         myXPF.R.setZDirection (k);
      }

      myU.set (k);
      updateForceVector();
   }
   
   public RigidTransform3d getPose() {
      return new RigidTransform3d (myXPF);
   }

   public Vector3d getForce() {
      ftmp.scale (1 / forceScaling, myForce);
      return ftmp;
   }

   public void setForce (Vector3d f) {
      myMag = f.norm() * forceScaling;
      setDirection (f);
      updateForceVector(); // Edit: Sanchez, Nov 2013, added since setForce(ZERO) doesn't update force vector 
      // setDirection ((Vector3d)validateDirection (f, null));
   }

   public double getMagnitude() {
      return myMag / forceScaling;
   }

   public void setMagnitude (double mag) {
      myMag = mag * forceScaling;
      updateForceVector();
   }

   public double getAxisLength() {
      return myAxisLength;
   }

   public void setAxisLength (double len) {
      myAxisLength = Math.max (0, len);
   }

   public double getForceScaling() {
      return forceScaling;
   }

   public void setForceScaling (double forceScaling) {
      // adjust current force based on new force scaling
      myMag = myMag/this.forceScaling*forceScaling;
      this.forceScaling = forceScaling;
      updateForceVector();
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      
      XPFnew.set (myXPF);
      gtr.transform (XPFnew);
      myXPF.R.set (XPFnew.R); // take only rotational part
      updateForceVector();     
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
