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
import java.util.List;

import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.Face;
import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.util.*;

/**
 * An experimental component that generates forces between a set of points and
 * collision with a mesh specified by a mesh contained within a {@link
 * MeshComponent}.
 */
public class PointMeshForce extends RenderableComponentBase
   implements ForceComponent {

   /**
    * Information and compute functions for each point that may contact the
    * mesh.
    */
   private class PointInfo {
      Point myPoint;
      Vector3d myNrm;
      double myDist;
      double myDistDot;

      PointInfo (Point point) {
         myPoint = point;
         myNrm = new Vector3d();
      }

      void applyForce() {
         Vector3d tmp = new Vector3d();
         tmp.scale (computeForce(), myNrm);
         myPoint.addForce (tmp);
      }

      double computeForce() {
         double d = myDist;
         double ddot = myDistDot;
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

      void addPosJacobian (SparseNumberedBlockMatrix M, double s) {
         double d = myDist;
         if (myUnilateral && d > 0) {
            // no force to apply
            return;
         }
         else if (d < 0) {
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

      public void addVelJacobian (SparseNumberedBlockMatrix M, double s) {
         double d = myDist;
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
   }

   MeshComponent myMeshComp; // contains the contacting mesh
   PolygonalMesh myMesh;     // explicit reference to the contacting mesh
   ArrayList<PointInfo> myPointInfo;  // list of contacting points

   /**
    * Determines force on the mesh-interacting points are computed in response
    * to the point's distance from the mesh
    */
   public enum ForceType {
      /**
       * Force is a linear function of mesh distance.
       */
      LINEAR,

      /**
       * Force is a quadratic function of mesh distance.
       */
      QUADRATIC
   };

   public static boolean DEFAULT_UNILATERAL = true;
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
      return RenderProps.createRenderProps (host);
   }

   public static PropertyList myProps =
      new PropertyList (
         PointMeshForce.class, RenderableComponentBase.class);

   static {
      myProps.add ("renderProps", "render properties", defaultRenderProps(null));
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
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public PointMeshForce () {
      myPointInfo = new ArrayList<>();
      myRenderProps = createRenderProps();
   }

   /**
    * Creates a new PointMeshForce for a mesh contained within a specified
    * mesh component.
    *
    * @param mcomp contains the mesh
    */
   public PointMeshForce (MeshComponent mcomp) {
      this ();
      setMeshComp (mcomp);
   }

   /**
    * Creates a new PointMeshForce for a mesh contained within a specified
    * mesh component.
    *
    * @param name name of the PointMeshForce component
    * @param mcomp contains the mesh
    */
   public PointMeshForce (String name, MeshComponent mcomp) {
      this (mcomp);
      setName (name);      
   }

   /**
    * Creates a new PointMeshForce for a mesh contained within a specified
    * mesh component.
    *
    * @param name name of the PointMeshForce component
    * @param mcomp contains the mesh
    * @param stiffness contact stiffness term
    * @param damping contact damping term
    */
   public PointMeshForce (
      String name, MeshComponent mcomp, double stiffness, double damping) {
      this (mcomp);
      setName (name);   
      setStiffness (stiffness);
      setDamping (damping);
   }

   private void setMeshComp (MeshComponent mcomp) {
      MeshBase mesh = mcomp.getMesh();
      if (!(mesh instanceof PolygonalMesh) ||
          !((PolygonalMesh)mesh).isTriangular()) {
         throw new IllegalArgumentException (
            "Supplied mesh must be a triangular polygonal mesh");
      }
      myMeshComp = mcomp;
      myMesh = (PolygonalMesh)mesh;
   }
   
   /**
    * Returns the mesh component containing the mesh.
    *
    * @return the mesh component
    */
   public MeshComponent getMeshComp() {
      return myMeshComp;
   }

   /**
    * Adds a mesh-interacting point to this PointMeshForce.
    *
    * @param point point to add
    */
   public void addPoint (Point point) {
      myPointInfo.add (new PointInfo (point));
   }

   private int getPointIndex (Point point) {
      for (int i=0; i<myPointInfo.size(); i++) {
         if (myPointInfo.get(i).myPoint == point) {
            return i;
         }
      }
      return -1;
   }

   /**
    * Removes a mesh-interacting point from this PointMeshForce.
    *
    * @param point point to remove
    */
   public boolean removePoint (Point point) {
      int idx = getPointIndex (point);
      if (idx != -1) {
         myPointInfo.remove (idx);
         return true;
      }
      else {
         return false;
      }
   }
   
   /**
    * Queries the number of mesh-interacting in this PointMeshForce.
    *
    * @return number of mesh-interacting points
    */
   public int numPoints() {
      return myPointInfo.size();
   }

   /**
    * Gets the {@code idx}-th mesh-interacting point in this PointMeshForce.
    *
    * @param idx index of the requested point
    * @return requested mesh-interacting point
    */
   public Point getPoint (int idx) {
      return myPointInfo.get(idx).myPoint;
   }

   /**
    * Removes all mesh-interacting points from this PointMeshForce.
    *
    * @param idx index of the requested point
    * @return requested mesh-interacting point
    */
   public void clearAllPoints() {
      myPointInfo.clear();
   }

   /**
    * Queries whether the point-mesh interactions are unilateral.  See {@link
    * #setUnilateral}.
    *
    * @return {@code true} if the point-mesh interactions are unilateral
    */
   public boolean getUnilateral() {
      return myUnilateral;
   }

   /**
    * Sets whether the point-mesh interactions are unilateral. Unilateral
    * interactions are contact-like: points are prevented from penetrating the
    * inside of the mesh, but are allowed to move freely outside it.
    * Non-unilateral interactions constrain the points to the mesh surface.
    *
    * @param enable if {@code true}, enables unilateral point-mesh
    * interactions.
    * @return requested mesh-interacting point
    */
   public void setUnilateral (boolean enable) {
      if (enable != myUnilateral) {
         myUnilateral = enable;
      }
   }

   /**
    * Queries the force type for this PointMeshForce. See {@link #setForceType}.
    *
    * @return current force type
    */
   public ForceType getForceType() {
      return myForceType;
   }

   /**
    * Sets the force type for this PointMeshForce. The force type is described
    * by {@link ForceType} and determines how the point forces are computed in
    * response to their distance from the mesh.
    *
    * @param type new force type
    */
   public void setForceType (ForceType type) {
      if (type != myForceType) {
         myForceType = type;
      }
   }

   /**
    * Queries the stiffness force term for this PointMeshForce.
    *
    * @return stiffness force term
    */
   public double getStiffness() {
      return myStiffness;
   }

   /**
    * Sets the stiffness force term for this PointMeshForce.
    *
    * @param stiffness new stiffness term value
    */
   public void setStiffness (double stiffness) {
      if (stiffness != myStiffness) {
         myStiffness = stiffness;
      }
   }

   /**
    * Queries the damping force term for this PointMeshForce.
    *
    * @return damping force term
    */
   public double getDamping() {
      return myDamping;
   }

   /**
    * Sets the damping force term for this PointMeshForce.
    *
    * @param damping new damping term value
    */
   public void setDamping (double damping) {
      if (damping != myDamping) {
         myDamping = damping;
      }
   }

   /**
    * Queries whether this PointMeshForce is enabled. If it is not
    * enabled, it generates no forces on its points.
    *
    * @return {@code true} if this PointMeshForce is enabled
    */
   public boolean getEnabled() {
      return myEnabledP;
   }

   /**
    * Sets whether this PointMeshForce is enabled. If it is not
    * enabled, it generates no forces on its points.
    *
    * @param enabled if {@code true}, enables this PointMeshForce
    */
   public void setEnabled (boolean enabled) {
      if (enabled != myEnabledP) {
         myEnabledP = enabled;
      }
   }

   // ----- ForceEffector interface ------

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
    * Called at least once per simulation step to compute the contact distance
    * and normals of each point with respect to the mesh.
    */
   private void updatePointInfo() {
      BVFeatureQuery query = new BVFeatureQuery();
      Point3d nearest = new Point3d();
      for (PointInfo pinfo : myPointInfo) {
         Point3d pos = pinfo.myPoint.getPosition();
         boolean inside = query.isInsideOrientedMesh (myMesh, pos, -1);
         if (inside || !myUnilateral) {
            Face face = query.getFaceForInsideOrientedTest (nearest, /*uv=*/null);
            if (!myMesh.meshToWorldIsIdentity()) {
               nearest.transform (myMesh.getMeshToWorld());
            }
            if (inside) {
               pinfo.myNrm.sub (nearest, pos);
            }
            else {
               pinfo.myNrm.sub (pos, nearest);
            }
            double dist = pinfo.myNrm.norm();
            if (dist != 0) {
               pinfo.myNrm.scale (1/dist);
            }
            else {
               pinfo.myNrm.set (face.getWorldNormal());
            }
            pinfo.myDist = (inside ? -dist : dist);
            pinfo.myDistDot =  pinfo.myNrm.dot (pinfo.myPoint.getVelocity());
         }
         else {
            pinfo.myDist = 0;
            pinfo.myDistDot = 0;
            pinfo.myNrm.setZero();
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void applyForces (double t) {
      if (!myEnabledP) {
         return;
      }
      updatePointInfo();
      for (PointInfo pinfo : myPointInfo) {
         pinfo.applyForce();
      }
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
      for (PointInfo pinfo : myPointInfo) {
         pinfo.addPosJacobian (M, s);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addVelJacobian (SparseNumberedBlockMatrix M, double s) {
      if (!myEnabledP) {
         return;
      }
      for (PointInfo pinfo : myPointInfo) {
         pinfo.addVelJacobian (M, s);
      }
   }

   /**
    * {@inheritDoc}
    */
   public int getJacobianType() {
      return Matrix.SYMMETRIC;
   }

   // ----- rendering interface -----

   /**
    * {@inheritDoc}
    */
   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      // nothing to do at the moment
   }

   /**
    * {@inheritDoc}
    */
   public RenderProps createRenderProps() {
      return defaultRenderProps (this);
   }

   /**
    * {@inheritDoc}
    */
   public void prerender (RenderList list) {
      super.prerender (list);
   }

   /**
    * {@inheritDoc}
    */
   public void render (Renderer renderer, int flags) {
      // nothing to do at the moment
   }
   
   // public void scaleMass (double s) {
   // }

   // public void scaleDistance (double s) {
   //    myOff *= s;
   // }

   // public void transformGeometry (AffineTransform3dBase X) {
   //    TransformGeometryContext.transform (this, X, 0);
   // }

   // public void transformGeometry (
   //    GeometryTransformer gtr, TransformGeometryContext context, int flags) {

   //    // Simply return if this constraint is not being transformed. This
   //    // could happen if (a) this class were to register with each particle
   //    // using their addConstrainer() methods, and then (b) one of the
   //    // particles was transformed, hence invoking a call to this method.
   //    if (!context.contains(this)) {
   //       return;
   //    }
      
   //    Plane plane = new Plane (myNrm, myOff);
   //    gtr.transformPnt (myCenter);
   //    gtr.transform (plane, myCenter);
   //    myNrm.set (plane.normal);
   //    myOff = myNrm.dot(myCenter);
   // }  
   
   // public void addTransformableDependencies (
   //    TransformGeometryContext context, int flags) {
   //    // no dependencies
   // }

   // Begin I/O methods

   /**
    * {@inheritDoc}
    */
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "mesh", tokens)) {
         return true;
      }
      else if (
         ScanWriteUtils.scanAndStoreReferences (rtok, "points", tokens) != -1) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   /**
    * {@inheritDoc}
    */
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);   
      if (myMeshComp != null) {
         pw.println (
            "mesh="+ComponentUtils.getWritePathName (ancestor,myMeshComp));
      }
      ArrayList<Point> points = new ArrayList<>(myPointInfo.size());
      for (PointInfo pinfo : myPointInfo) {
         points.add (pinfo.myPoint);
      }
      pw.print ("points=");
      ScanWriteUtils.writeBracketedReferences (
         pw, points, ancestor);
   }

   /**
    * {@inheritDoc}
    */
   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      if (postscanAttributeName (tokens, "mesh")) {
         MeshComponent mcomp = 
            postscanReference (tokens, MeshComponent.class, ancestor);
         setMeshComp (mcomp);
         return true;
      }
      else if (postscanAttributeName (tokens, "points")) {
         myPointInfo.clear();
         ArrayList<Point> points = new ArrayList<>();
         ScanWriteUtils.postscanReferences (
            tokens, points, Point.class, ancestor);
         for (Point p : points) {
            addPoint (p);
         }
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   // Begin editing support methods

   /**
    * {@inheritDoc}
    */
   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      if (myMeshComp != null) {
         refs.add (myMeshComp);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void getSoftReferences (List<ModelComponent> refs) {
      super.getSoftReferences (refs);
      for (int i=0; i<myPointInfo.size(); i++) {
         refs.add (myPointInfo.get(i).myPoint);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      super.updateReferences (undo, undoInfo);   

      if (undo) {
         Object obj = undoInfo.removeFirst();
         if (obj != NULL_OBJ) {
            ((ListRemove<PointInfo>)obj).undo();
         }
      }
      else {
         // remove soft references which aren't in the hierarchy any more:
         ListRemove<PointInfo> pointRemove = null;
         for (int i=0; i<myPointInfo.size(); i++) {
            PointInfo pi = myPointInfo.get(i);
            if (!ComponentUtils.areConnected (this, pi.myPoint)) {
               if (pointRemove == null) {
                  pointRemove =
                     new ListRemove<PointInfo>(myPointInfo);
               }
               pointRemove.requestRemove(i);
            }
         }
         if (pointRemove != null) {
            pointRemove.remove();
            undoInfo.addLast (pointRemove);
         }
         else {
            undoInfo.addLast (NULL_OBJ);
         }
      }
   }

}     
