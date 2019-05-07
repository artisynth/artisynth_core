/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.*; //import artisynth.core.mechmodels.DynamicMechComponent.Activity;
import artisynth.core.util.*;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.*;
import maspack.render.*;

import java.util.*;
import java.io.*;

import maspack.render.*;

/**
 * A 3-dimensional point that is confined to a plane.
 */
public class PlanarPoint extends Point implements PlanarComponent {
   Point3d myPos3d;
   Vector3d myVel3d;
   Matrix2x2Block myPointBlock; // XXX co-exists with mySolveBlock
   PlanarComponent myPlanarComponent;

   public static PropertyList myProps =
      new PropertyList (PlanarPoint.class, Point.class);

   static {
      myProps.get ("position").setAutoWrite (false);
      myProps.get ("velocity").setAutoWrite (false);
      myProps.add ("position2d", "2d position state", Point2d.ZERO, "%8.3f");
      myProps.add ("velocity2d", "2d velocity state", Vector2d.ZERO, "%8.3f");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public PlanarPoint() {
      super();
      myPos3d = new Point3d();
      myVel3d = new Vector3d();
   }

   public PlanarPoint (Point2d p) {
      this();
      setPosition2d (p);
   }

   public PlanarPoint (double x, double y) {
      this();
      setPosition2d (x, y);
   }

   public void transformToWorld (Point3d pw, Point3d pp) {
      RigidTransform3d X = getPlaneToWorld();
      RotationMatrix3d R = X.R;

      double x = pp.x;
      double y = pp.y;

      pw.x = R.m00 * x + R.m01 * y + X.p.x;
      pw.y = R.m10 * x + R.m11 * y + X.p.y;
      pw.z = R.m20 * x + R.m21 * y + X.p.z;
   }

   public void transformToWorld (Vector3d vw, Vector3d vp) {
      RigidTransform3d X = getPlaneToWorld();
      RotationMatrix3d R = X.R;

      double x = vp.x;
      double y = vp.y;

      vw.x = R.m00 * x + R.m01 * y;
      vw.y = R.m10 * x + R.m11 * y;
      vw.z = R.m20 * x + R.m21 * y;
   }

   public void transformToPlane (Point3d pp, Point3d pw) {
      RigidTransform3d X = getPlaneToWorld();
      RotationMatrix3d R = X.R;

      double x = pw.x - X.p.x;
      double y = pw.y - X.p.y;
      double z = pw.z - X.p.z;
      pp.x = R.m00 * x + R.m10 * y + R.m20 * z;
      pp.y = R.m01 * x + R.m11 * y + R.m21 * z;
      pp.z = 0;
   }

   public void transformToPlane (Vector3d vp, Vector3d vw) {
      RigidTransform3d X = getPlaneToWorld();
      RotationMatrix3d R = X.R;

      double x = vw.x;
      double y = vw.y;
      double z = vw.z;

      vp.x = R.m00 * x + R.m10 * y + R.m20 * z;
      vp.y = R.m01 * x + R.m11 * y + R.m21 * z;
      vp.z = 0;
   }

   public void setPlanarComponent (PlanarComponent pcomp) {
      myPlanarComponent = pcomp;
      // XXXX
   }

   public PlanarComponent getPlanarComponent() {
      return myPlanarComponent;
   }

   public RigidTransform3d getPlaneToWorld() {
      if (myPlanarComponent != null) {
         return myPlanarComponent.getPlaneToWorld();
      }
      else {
         return RigidTransform3d.IDENTITY;
      }
   }

   public Point2d getPosition2d() {
      return new Point2d (myState.pos.x, myState.pos.y);
   }

   public void setPosition2d (Point2d p) {
      myState.pos.set (p.x, p.y, 0);
      transformToWorld (myPos3d, myState.pos);
   }

   public void setPosition2d (double x, double y) {
      myState.pos.set (x, y, 0);
      transformToWorld (myPos3d, myState.pos);
   }

   public void setVelocity2d (Vector2d v) {
      myState.vel.set (v.x, v.y, 0);
      transformToWorld (myVel3d, myState.vel);
   }

   public Vector2d getVelocity2d() {
      return new Vector2d (myState.vel.x, myState.vel.y);
   }

   // overrides from Point

   // OK
   public Point3d getPosition() {
      return myPos3d;
   }

   public int getPosState (double[] x, int idx) {
      x[idx++] = myState.pos.x;
      x[idx++] = myState.pos.y;
      return idx;
   }

   public void getPosition2d (Vector2d p) {
      p.x = myState.pos.x;
      p.y = myState.pos.y;
   }

   public void setPosition (Point3d p) {
      transformToPlane (myState.pos, p);
      // transform back to world in case orginal point not on the plane:
      transformToWorld (myPos3d, myState.pos);
   }

   public void setPosition (double x, double y, double z) {
      myPos3d.set (x, y, z); // use as a temporary
      transformToPlane (myState.pos, myPos3d);
      // transform back to world in case orginal point not on the plane:
      transformToWorld (myPos3d, myState.pos);
   }

   public void addPosition (double dx, double dy, double dz) {
      myPos3d.add (dx, dy, dz); // use as a temporary
      transformToPlane (myState.pos, myPos3d);
      // transform back to world in case orginal point not on the plane:
      transformToWorld (myPos3d, myState.pos);
   }

   public int setPosState (double[] p, int idx) {
      myState.pos.x = p[idx++];
      myState.pos.y = p[idx++];
      transformToWorld (myPos3d, myState.pos);
      return idx;
   }

   // OK
   public Vector3d getVelocity() {
      return myVel3d;
   }

   public int getVelState (double[] v, int idx) {
      v[idx++] = myState.vel.x;
      v[idx++] = myState.vel.y;
      return idx;
   }

   public void getVelocity2d (Vector2d v) {
      v.x = myState.vel.x;
      v.y = myState.vel.y;
   }

   public void setVelocity (Vector3d v) {
      transformToPlane (myState.vel, v);
      // transform back to world in case orginal velocity not on the plane:
      transformToWorld (myVel3d, myState.vel);
   }

   public void addVelocity (double dx, double dy, double dz) {
      myVel3d.add (dx, dy, dz); // use as a temporary
      transformToPlane (myState.vel, myVel3d);
      // transform back to world in case orginal velocity not on the plane:
      transformToWorld (myVel3d, myState.vel);
   }

   public int setVelState (double[] v, int idx) {
      myState.vel.x = v[idx++];
      myState.vel.y = v[idx++];
      transformToWorld (myVel3d, myState.vel);
      return idx;
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();      
      if (scanAndStoreReference (rtok, "plane", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }


   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      setDefaultValues();
      super.scan (rtok, ref);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "plane")) {
         setPlanarComponent (postscanReference (
            tokens, PlanarComponent.class, ancestor));
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.println ("plane=" +ComponentUtils.getWritePathName (
                     ancestor, myPlanarComponent));
      super.writeItems (pw, fmt, ancestor);
   }

   public void prerender (RenderList list) {
      myRenderCoords[0] = (float)myPos3d.x;
      myRenderCoords[1] = (float)myPos3d.y;
      myRenderCoords[2] = (float)myPos3d.z;
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      myPos3d.updateBounds (pmin, pmax);
   }

   public void scaleDistance (double s) {
      myState.scaleDistance (s);
      transformToWorld (myVel3d, myState.vel);
      if (myRenderProps != null) {
         RenderableUtils.cloneRenderProps (this);
         myRenderProps.scaleDistance (s);
      }
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      
      super.transformGeometry (gtr, context, flags);
      gtr.transformPnt (myPos3d);

      transformToPlane (myState.pos, myPos3d);
      transformToWorld (myPos3d, myState.pos);     
   }  

   // OK
   public void addSolveBlock (SparseNumberedBlockMatrix S) {
      int bi = getSolveIndex();
      Matrix2x2Block blk = new Matrix2x2Block();
      S.addBlock (bi, bi, blk);
      myPointBlock = blk;
   }
   
   public MatrixBlock createSolveBlock () {
      Matrix2x2Block blk = new Matrix2x2Block();
      myPointBlock = blk;
      return blk;
   }

   // OK
   public MatrixBlock getSolveBlock() {
      return myPointBlock;
   }


   public void addToSolveBlock (Matrix2d M) {
      if (myPointBlock != null) {
         myPointBlock.add (M);
      }
   }

   public void addToSolveBlockDiagonal (double d) {
      if (myPointBlock != null) {
         myPointBlock.m00 += d;
         myPointBlock.m11 += d;
      }
   }

   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      PlanarPoint comp = (PlanarPoint)super.copy (flags, copyMap);
      comp.myPointBlock = null;
      comp.myPos3d = new Point3d (myPos3d);
      comp.myVel3d = new Vector3d (myVel3d);
      comp.setPlanarComponent (myPlanarComponent);
      return comp;
   }
}
