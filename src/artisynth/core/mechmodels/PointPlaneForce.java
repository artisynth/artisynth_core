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

import maspack.geometry.GeometryTransformer;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.*;
import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.Shading;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.DrawMode;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;

public class PointPlaneForce extends RenderableComponentBase
   implements ForceComponent, ScalableUnits, TransformableGeometry {

   Vector3d myNrm;
   double myOff;
   Point3d myCenter;
   Point myPoint;

   public enum ForceType {
      LINEAR,
      QUADRATIC
   };

   private Point3d[] myRenderVtxs;
   private double myPlaneSize = defaultPlaneSize;
   private static final double defaultPlaneSize = 1;

   public static boolean DEFAULT_UNILATERAL = false;
   protected boolean myUnilateral = DEFAULT_UNILATERAL;

   public static double DEFAULT_STIFFNESS = 1.0;
   protected double myStiffness = DEFAULT_STIFFNESS;

   public static double DEFAULT_DAMPING = 0.0;
   protected double myDamping = DEFAULT_DAMPING;

   public static ForceType DEFAULT_FORCE_TYPE = ForceType.LINEAR;
   protected ForceType myForceType = DEFAULT_FORCE_TYPE;

   public static boolean DEFAULT_ENABLED = true;
   protected boolean myEnabledP = DEFAULT_ENABLED;

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createFaceProps (null);
      props.setFaceStyle (Renderer.FaceStyle.FRONT_AND_BACK);
      return props;
   }

   public static PropertyList myProps =
      new PropertyList (
         PointPlaneForce.class, RenderableComponentBase.class);

   static {
      myProps.add (
         "offset", "offset from center of the plane in normal direction", 0);
      myProps.add ("renderProps", "render properties", defaultRenderProps(null));
      myProps.add ("planeSize", "plane size", defaultPlaneSize);
      myProps.add (
         "unilateral",
         "if true, force is only applied on the negative side of the plane",
         DEFAULT_UNILATERAL);
      myProps.add (
         "stiffness", "force proportionality constant", DEFAULT_STIFFNESS);
      myProps.add (
         "damping", "velocity based damping force", DEFAULT_DAMPING);
      myProps.add (
         "forceType", "formula by which force is computed", DEFAULT_FORCE_TYPE);
      myProps.add (
         "enabled", "enables/disables forces", DEFAULT_ENABLED);
      myProps.addReadOnly (
         "force", "current force along the normal");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public PointPlaneForce () {
      myNrm = new Vector3d();
      myCenter = new Point3d();
      myRenderVtxs = new Point3d[4];
      for (int i = 0; i < myRenderVtxs.length; i++) {
         myRenderVtxs[i] = new Point3d();
      }
      myRenderProps = createRenderProps();
   }

   public PointPlaneForce (Point p, Plane plane) {
      this ();
      myNrm.set (plane.normal);
      myOff = plane.offset;
      myCenter.scale (myOff, plane.normal);
      myPoint = p;
   }

   public PointPlaneForce (Point p, Vector3d nrml, Point3d center) {
      this ();
      Vector3d n = new Vector3d (nrml);
      n.normalize();
      myNrm.set (n);
      myCenter.set (center);
      myOff = n.dot (myCenter);
      myPoint = p;
   }

   public void setPlane(Plane p) {
      myNrm.set (p.normal);
      myOff = p.getOffset();
      myCenter.scale(myOff, myNrm);
   }
   
   public Plane getPlane() {
      return new Plane(myNrm, myOff);
   }
   
   public void setOffset(double off) {
      double ooff = myOff;
      myOff = off;
      myCenter.scaledAdd(off-ooff, myNrm);
   }
   
   public double getOffset() {
      return myOff;
   }

   public Point3d getCenter() {
      return new Point3d (myCenter);
   }
   
   public void setCenter(Point3d c) {
      myCenter.set (c);
      myOff = c.dot (myNrm);
   }

   public Vector3d getNormal() {
      return new Vector3d (myNrm);
   }

   public double getPlaneSize() {
      return myPlaneSize;
   }

   public void setPlaneSize (double len) {
      myPlaneSize = len;
   }

   public boolean getUnilateral() {
      return myUnilateral;
   }

   public void setUnilateral (boolean unilateral) {
      if (unilateral != myUnilateral) {
         myUnilateral = unilateral;
      }
   }

   public ForceType getForceType() {
      return myForceType;
   }

   public void setForceType (ForceType type) {
      if (type != myForceType) {
         myForceType = type;
      }
   }

   public double getStiffness() {
      return myStiffness;
   }

   public void setStiffness (double stiffness) {
      if (stiffness != myStiffness) {
         myStiffness = stiffness;
      }
   }

   public double getDamping() {
      return myDamping;
   }

   public void setDamping (double damping) {
      if (damping != myDamping) {
         myDamping = damping;
      }
   }

   public boolean getEnabled() {
      return myEnabledP;
   }

   public void setEnabled (boolean enabled) {
      if (enabled != myEnabledP) {
         myEnabledP = enabled;
      }
   }

   // ----- ForceEffector interface ------

   protected double distance() {
      return myNrm.dot (myPoint.getPosition()) - myOff;
   }

   protected double distanceDot() {
      return myNrm.dot (myPoint.getVelocity());
   }

   protected double computeF (double d) {
      switch (myForceType) {
         case LINEAR: {
            return -myStiffness*d;
         }
         case QUADRATIC: {
            return -myStiffness*d*d;            
         }
         default: {
            throw new UnsupportedOperationException (
               "computeF() not implemented for force type "+myForceType);
         }
      }
   }

   protected double computeDfdd (double d) {
      switch (myForceType) {
         case LINEAR: {
            return -myStiffness;
         }
         case QUADRATIC: {
            return -2*myStiffness*d;
         }
         default: {
            throw new UnsupportedOperationException (
               "computeDfdd() not implemented for force type "+myForceType);
         }
      }
   }

   /**
    * Computes the current force along the plane normal
    */
   protected double computeForce () {
      double d = distance();
      double ddot = distanceDot();
      double sgn = 1;
      if (myUnilateral && d > 0) {
         // no force to apply
         return 0;
      }
      else if (d < 0) {
         sgn = -1;
         d = -d;
      }
      double f = sgn*computeF (d);
      f += (-myDamping*ddot);
      return f;
   }

   /**
    * Cpmputes and returns the current force along the plane normal.
    * @return force along the normal
    */
   public double getForce () {
      return computeForce();
   }

   /**
    * {@inheritDoc}
    */
   public void applyForces (double t) {
      if (!myEnabledP) {
         return;
      }
      Vector3d tmp = new Vector3d();
      tmp.scale (computeForce(), myNrm);
      myPoint.addForce (tmp);
   }

   /**
    * {@inheritDoc}
    */
   public void addSolveBlocks (SparseNumberedBlockMatrix M) {
      if (!myEnabledP) {
         return;
      }
      // no need to add anything unless the plane is attached to a Frame
   }

   /**
    * {@inheritDoc}
    */
   public void addPosJacobian (SparseNumberedBlockMatrix M, double s) {
      if (!myEnabledP) {
         return;
      }
      double d = distance();
      double ddot = distanceDot();
      double sgn = 1;
      if (myUnilateral && d > 0) {
         // no force to apply
         return;
      }
      else if (d < 0) {
         sgn = -1;
         d = -d;
      }
      int idx = myPoint.getSolveIndex();
      if (idx != -1) {
         Matrix3d K = new Matrix3d();
         K.outerProduct (myNrm, myNrm);
         double dfdd = computeDfdd (d);
         K.scale (s*dfdd);
         M.getBlock(idx,idx).add (K);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addVelJacobian (SparseNumberedBlockMatrix M, double s) {
      if (!myEnabledP) {
         return;
      }
      double d = distance();
      if (myUnilateral && d > 0) {
         // no force to apply
         return;
      }
      int idx = myPoint.getSolveIndex();
      if (idx != -1 && myDamping != 0) {
         Matrix3d K = new Matrix3d();
         K.outerProduct (myNrm, myNrm);
         K.scale (-s*myDamping);
         M.getBlock(idx,idx).add (K);
      }
   }

   /**
    * {@inheritDoc}
    */
   public int getJacobianType() {
      return Matrix.SYMMETRIC;
   }

   // ----- rendering interface -----

   protected void computeRenderVtxs () {
      RotationMatrix3d RPD = new RotationMatrix3d();
      RPD.setZDirection (myNrm);
      myRenderVtxs[0].set (myPlaneSize / 2, myPlaneSize / 2, 0);
      myRenderVtxs[1].set (-myPlaneSize / 2, myPlaneSize / 2, 0);
      myRenderVtxs[2].set (-myPlaneSize / 2, -myPlaneSize / 2, 0);
      myRenderVtxs[3].set (myPlaneSize / 2, -myPlaneSize / 2, 0);
      for (int i = 0; i < myRenderVtxs.length; i++) {
         myRenderVtxs[i].transform (RPD);
         myRenderVtxs[i].add (myCenter);
      }
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      computeRenderVtxs ();
      for (int i = 0; i < myRenderVtxs.length; i++) {
         myRenderVtxs[i].updateBounds (pmin, pmax);
      }
   }

   public RenderProps createRenderProps() {
      return defaultRenderProps (this);
   }

   public void prerender (RenderList list) {
      super.prerender (list);
   }

   public void render (Renderer renderer, int flags) {

      if (myPlaneSize > 0) {
         computeRenderVtxs ();

         RenderProps props = myRenderProps;

         Shading savedShading = renderer.setPropsShading (props);
         renderer.setFaceColoring (props, isSelected());
         renderer.setFaceStyle (props.getFaceStyle());

         renderer.beginDraw (DrawMode.TRIANGLE_STRIP);
         renderer.setNormal (myNrm.x, myNrm.y, myNrm.z);
         renderer.addVertex (myRenderVtxs[3]);
         renderer.addVertex (myRenderVtxs[0]);
         renderer.addVertex (myRenderVtxs[2]);
         renderer.addVertex (myRenderVtxs[1]);
         renderer.endDraw();

         renderer.setShading (savedShading);
         renderer.setFaceStyle (FaceStyle.FRONT); // set default
      }
   }
   
   public void scaleMass (double s) {
   }

   public void scaleDistance (double s) {
      myOff *= s;
   }

   public void transformGeometry (AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {

      // Simply return if this constraint is not being transformed. This
      // could happen if (a) this class were to register with each particle
      // using their addConstrainer() methods, and then (b) one of the
      // particles was transformed, hence invoking a call to this method.
      if (!context.contains(this)) {
         return;
      }
      
      Plane plane = new Plane (myNrm, myOff);
      gtr.transformPnt (myCenter);
      gtr.transform (plane, myCenter);
      myNrm.set (plane.normal);
      myOff = myNrm.dot(myCenter);
   }  
   
   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      // no dependencies
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "point", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "center")) {
         myCenter.scan (rtok);
         myOff = myNrm.dot(myCenter);
         return true;
      }
      else if (scanAttributeName (rtok, "normal")) {
         myNrm.scan (rtok);
         myOff = myNrm.dot(myCenter);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);   
      if (myPoint != null) {
         pw.println (
            "point="+ComponentUtils.getWritePathName (ancestor,myPoint));
      }
      pw.println ("normal=[" + myNrm.toString (fmt) + "]");
      pw.println ("center=[" + myCenter.toString (fmt) + "]");
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      if (postscanAttributeName (tokens, "point")) {
         myPoint = 
            postscanReference (tokens, Point.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

}     
