/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.geometry.*;
import maspack.matrix.*;
import maspack.properties.*;
import maspack.render.*;
import maspack.spatialmotion.*;
import maspack.util.*;
import artisynth.core.modelbase.*;
import artisynth.core.materials.*;
import artisynth.core.modelbase.PropertyChangeEvent;
import artisynth.core.modelbase.PropertyChangeListener;
import java.io.*;
import java.util.*;

public abstract class DeformableBody extends RigidBody
   implements PropertyChangeListener {

   protected static double DEFAULT_STIFFNESS_DAMPING = 0;
   protected static double DEFAULT_MASS_DAMPING = 0;

   protected static boolean freezeFrame = false;

   FemMaterial myMaterial;
   VectorNd myElasticPos;
   VectorNd myElasticVel;
   VectorNd myElasticForce;
   VectorNd myExternalElasticForce;
   VectorNd myElasticTmp;
   MatrixNd myStiffnessMatrix;
   boolean myStiffnessValidP = false;
   SVDecomposition3d mySVD = new SVDecomposition3d();

   protected double myStiffnessDamping = DEFAULT_STIFFNESS_DAMPING;
   protected double myMassDamping = DEFAULT_MASS_DAMPING;

   Point3d[] myRestVertices;

   public void invalidateStiffness() {
      myStiffnessValidP = false;
   }

   protected DeformableBody () {
      int numc = numElasticCoords();
      myElasticPos = new VectorNd (numc);
      myElasticVel = new VectorNd (numc);
      myElasticForce = new VectorNd (numc);
      myElasticTmp = new VectorNd (numc);
      myExternalElasticForce = new VectorNd (numc);
      myStiffnessMatrix = new MatrixNd (numc, numc);
      setMaterial (createDefaultMaterial());
   }
 
   public static PropertyList myProps =
      new PropertyList (DeformableBody.class, RigidBody.class);

   static {
      myProps.add (
         "massDamping * *", "damping on each particle",
         DEFAULT_MASS_DAMPING);
      myProps.add (
         "stiffnessDamping * *", "damping on stiffness matrix",
         DEFAULT_STIFFNESS_DAMPING);
      myProps.add (
         "material", "deformable body material", createDefaultMaterial(), "CE");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public abstract int numElasticCoords();

   @Override
   public boolean isDeformable () {
      return true;
   }

   public void setMassDamping (double d) {
      myMassDamping = d;
   }

   public double getMassDamping() {
      return myMassDamping;
   }

   public void setStiffnessDamping (double d) {
      myStiffnessDamping = d;
   }

   public double getStiffnessDamping() {
      return myStiffnessDamping;
   }

   public static FemMaterial createDefaultMaterial() {
      return new LinearMaterial();
   }

   public FemMaterial getMaterial() {
      return myMaterial;
   }

   public void setMaterial (FemMaterial mat) {
      if (mat == null) {
         throw new IllegalArgumentException (
            "Material not allowed to be null");
      }
      myMaterial = (FemMaterial)MaterialBase.updateMaterial (
         this, "material", myMaterial, mat);
      // issue DynamicActivityChange in case solve matrix symmetry has changed:
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
   }

   public void propertyChanged (PropertyChangeEvent e) {
      myStiffnessValidP = false;
      if (e.getHost() instanceof FemMaterial) {
          // issue DynamicActivityChange in case solve matrix symmetry has
          // changed:
          notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
      }
   }

   public VectorNd getElasticPos() {
      return myElasticPos;
   }
  
   public VectorNd getElasticVel() {
      return myElasticVel;
   }
  
   public void getElasticPos (VectorNd pos) {
      pos.set (myElasticPos);
   }
  
   public void getElasticVel (VectorNd vel) {
      vel.set (myElasticVel);
   }

   public VectorNd getElasticForce () {
      return myElasticForce;
   }
  
   public void setElasticForce (VectorNd f) {
      myElasticForce.set (f);
   }
  
   public void addElasticForce (VectorNd f) {
      myElasticForce.add (f);
   }

   public VectorNd getExternalElasticForce () {
      return myExternalElasticForce;
   }   

   public void setExternalElasticForce (VectorNd f) {
      myExternalElasticForce.set (f);
   }
   
   public void addExternalElasticForce (VectorNd f) {
      myExternalElasticForce.add(f);
   }
   
   public void addScaledExternalElasticForce (double s, VectorNd f) {
      myExternalElasticForce.scaledAdd(s, f);
   }
  
   public void setElasticPos (VectorNd pos) {
      if (pos.size() != numElasticCoords()) {
         throw new IllegalArgumentException (
            "argument pos has size of "+pos.size()+
            ", expected "+numElasticCoords());
      }
      myElasticPos.set (pos);
      updatePosState();
   }
  
   public void setElasticVel (VectorNd vel) {
      if (vel.size() != numElasticCoords()) {
         throw new IllegalArgumentException (
            "argument vel has size of "+vel.size()+
            ", expected "+numElasticCoords());
      }
      myElasticVel.set (vel);
      updateVelState();
   }

   @Override
   public MatrixBlock createMassBlock() {
      int msize = 6 + numElasticCoords();
      return new MatrixNdBlock (msize, msize);
   }

   private void checkMassMatrixType (String name, Matrix M) {
      int msize = 6 + numElasticCoords();      
      if (M instanceof MatrixNd) {
         MatrixNd Mn = (MatrixNd)M;
         if (Mn.rowSize() == msize && Mn.colSize() == msize) {
            return;
         }
      }
      throw new IllegalArgumentException (
         name + " is not a MatrixNd with size " + msize);
   }

   @Override
   public void getMass (Matrix M, double t) {
      // for now, assume that the rigid and elastic mass matrices are decoupled
      // with a diagonal elastic mass matrix
      checkMassMatrixType ("M", M);
      int msize = 6 + numElasticCoords();      

      MatrixNd MN = (MatrixNd)M;
      Matrix6d MR = new Matrix6d();
      mySpatialInertia.getRotated (MR, getPose().R);
      MN.setSubMatrix (0, 0, MR);
      for (int i=6; i<msize; i++) {
         MN.set (i, i, getMass());
      }
   }
  
   @Override
   public int getMassForces (VectorNd f, double t, int idx) {
      // for now, assume that the rigid and elastic mass matrices are decoupled
      // with a diagonal elastic mass matrix
      idx = super.getMassForces (f, t, idx);
      double[] buf = f.getBuffer();
      for (int i=0; i<numElasticCoords(); i++) {
         buf[idx++] = 0;
      }
      return idx;
   }

   @Override   
   public void getInverseMass (Matrix Minv, Matrix M) {
      checkMassMatrixType ("Minv", Minv);
      checkMassMatrixType ("M", M);
      ((MatrixNd)Minv).invert ((MatrixNd)M);
   }

   @Override   
   public void addSolveBlock (SparseNumberedBlockMatrix S) {
      int bi = getSolveIndex();
      int msize = 6 + numElasticCoords();      
      MatrixNdBlock blk = new MatrixNdBlock (msize, msize);
      mySolveBlockNum = S.addBlock (bi, bi, blk);
      mySolveBlock = blk;
   }

//   @Override   
//   public MatrixBlock createSolveBlock () {
//      int msize = 6 + numElasticCoords();      
//      MatrixNdBlock blk = new MatrixNdBlock (msize, msize);
//      mySolveBlock = blk;
//      return blk;
//   }   

//   @Override      
//   public void setState (DynamicComponent c) {
//      if (c.getClass() == getClass()) {
//         DeformableBody bod = (DeformableBody)c;
//         super.setState (bod);
//         myElasticPos.set (bod.myElasticPos);
//         myElasticVel.set (bod.myElasticVel);
//      }
//      else {
//         throw new IllegalArgumentException (
//            "component c is not an instance of "+getClass());
//      }
//   }   

   @Override      
   public void addPosImpulse (
      double[] xbuf, int xidx, double h, double[] vbuf, int vidx) {

      super.addPosImpulse (xbuf, xidx, h, vbuf, vidx);
      xidx += 7;
      vidx += 6;
      int numc = numElasticCoords();
      for (int i=0; i<numc; i++) {
         xbuf[xidx++] += h*vbuf[vidx++];
      }
   }

   @Override      
   public int getPosDerivative (double[] dxdt, int idx) {
      idx = super.getPosDerivative (dxdt, idx);
      myElasticVel.get (dxdt, idx);
      return idx + numElasticCoords();
   }

   @Override      
   public int getPosState (double[] buf, int idx) {
      idx = super.getPosState (buf, idx);
      myElasticPos.get (buf, idx);
      return idx + numElasticCoords();
   }

   @Override      
   public int setPosState (double[] buf, int idx) {
      if (freezeFrame) {
         idx += 7;
      }
      else {
         idx = super.setPosState (buf, idx);
      }
      myElasticPos.set (buf, idx);
      updatePosState();
      return idx + numElasticCoords();
   }

   @Override      
   public int getVelState (double[] buf, int idx) {
      idx = super.getVelState (buf, idx);
      myElasticVel.get (buf, idx);
      return idx + numElasticCoords();
   }

   @Override      
   public int setVelState (double[] buf, int idx) {
      if (freezeFrame) {
         idx += 6;
      }
      else {
         idx = super.setVelState (buf, idx);
      }
      myElasticVel.set (buf, idx);
      updateVelState();
      return idx + numElasticCoords();
   }

   @Override      
   public int setForce (double[] f, int idx) {
      idx = super.setForce (f, idx);
      myElasticForce.set (f, idx);
      return idx + numElasticCoords();
   }
 
   @Override      
   public int getForce (double[] f, int idx) {
      idx = super.getForce (f, idx);
      myElasticForce.get (f, idx);
      return idx + numElasticCoords();
   }

   @Override
      public int getPosStateSize() {
      return super.getPosStateSize() + numElasticCoords();
   }

   @Override
      public int getVelStateSize() {
      return super.getVelStateSize() + numElasticCoords();
   }

   @Override
      public void zeroForces() {
      super.zeroForces();
      myElasticForce.setZero();
   }

   @Override
      public void zeroExternalForces() {
      super.zeroExternalForces();
      myExternalElasticForce.setZero();
   }

   @Override
   public void setForcesToExternal() {
      super.setForcesToExternal();
      myElasticForce.set (myExternalElasticForce);
   }

   @Override
   public void applyExternalForces() {
      super.applyExternalForces();
      myElasticForce.add (myExternalElasticForce);
   }

   @Override protected void setMeshFromInfo () {
      super.setMeshFromInfo();
      PolygonalMesh mesh = getMesh();
      mesh.setFixed (false);
      ArrayList<Vertex3d> verts = mesh.getVertices();
      myRestVertices = new Point3d[verts.size()];
      for (int i=0; i<verts.size(); i++) {
         myRestVertices[i] = new Point3d (verts.get(i).pnt);
      }
   }

   public abstract void updateStiffnessMatrix ();

   @Override public void applyForces (double t) {
      super.applyForces (t);
      updateStiffnessMatrix();
      int numc = numElasticCoords();
      myStiffnessMatrix.mul (myElasticTmp, myElasticVel);
      myElasticTmp.scale (myStiffnessDamping);
      // assume elastic mass matrix is diagonal for now
      double mass = getMass();
      for (int i=0; i<numc; i++) {
         myElasticTmp.add (i, mass*myMassDamping*myElasticVel.get(i));
      }
      myStiffnessMatrix.mulAdd (myElasticTmp, myElasticPos);
      myElasticForce.sub (myElasticTmp);
   }

   @Override public void addVelJacobian (SparseNumberedBlockMatrix S, double s) {
       if (mySolveBlockNum != -1) {
         if (myFrameDamping != 0 || myRotaryDamping != 0) {
            MatrixNdBlock blk =
               (MatrixNdBlock)S.getBlockByNumber (mySolveBlockNum);
            blk.add (0, 0, s*myFrameDamping);
            blk.add (1, 1, s*myFrameDamping);
            blk.add (2, 2, s*myFrameDamping);
            blk.add (3, 3, s*myRotaryDamping);
            blk.add (4, 4, s*myRotaryDamping);
            blk.add (5, 5, s*myRotaryDamping);
         }
      }     
   }
   
   @Override public void addPosJacobian (SparseNumberedBlockMatrix S, double s) {
       if (mySolveBlockNum != -1) {
          MatrixNdBlock blk =
             (MatrixNdBlock)S.getBlockByNumber (mySolveBlockNum);
          // assumes that the stiffness matrix has already been updated
          blk.addScaledSubMatrix (6, 6, -s, myStiffnessMatrix);
       }
   }

   @Override protected void updatePosState() {
      super.updatePosState();
      PolygonalMesh mesh = getMesh();
      if (mesh != null) {
         // adjust mesh vertex positions
         ArrayList<Vertex3d> verts = mesh.getVertices();
         for (int i=0; i<verts.size(); i++) {
            computeDeformedPos (verts.get(i).pnt, myRestVertices[i]);
         }
         mesh.notifyVertexPositionsModified();        
      }         
   }

   public abstract void getShape (Vector3d shp, int i, Vector3d pos0);

   public abstract void getDShape (Matrix3d Dshp, int i, Vector3d pos0);

   @Override
   public void addPointMass (Matrix M, double m, Vector3d pos) {
      SpatialInertia.addPointMass (M, m, pos);
      if (M instanceof MatrixNd) {
         // for now, just assuming that elastic mass is diagonal
         MatrixNd MN = (MatrixNd)M;
         int numc = numElasticCoords();
         for (int i=0; i<numc; i++) {
            MN.add (i+6, i+6, m);
         }
      }
   }

  @Override 
   public void computePointPosition (Vector3d pos, Point3d loc) {
      computeDeformedPos (pos, loc);
      pos.transform (myState.XFrameToWorld.R, pos);
      pos.add (myState.XFrameToWorld.p);
   }

   @Override 
   public void computePointLocation (Vector3d loc, Vector3d pos) {
      Vector3d locdef = new Vector3d();
      locdef.sub (pos, myState.XFrameToWorld.p);
      locdef.inverseTransform (myState.XFrameToWorld.R);
      computeUndeformedPos (loc, locdef, 1e-8);
   }

   @Override 
      public void computePointVelocity (
         Vector3d vel, Point3d loc, Twist frameVel) {

      // compute elastic velocity in body coords
      computeDeformedVel (vel, loc);
      // rotate to world coords
      vel.transform (myState.XFrameToWorld.R);
      // add additional velocity components from body motion
      vel.add (frameVel.v); 
      computeDeformedPos (myTmpPos, loc);
      myTmpPos.transform (myState.XFrameToWorld.R);
      vel.crossAdd (frameVel.w, myTmpPos, vel);
   }

   @Override public void computePointCoriolis (Vector3d cor, Point3d loc) {
      RotationMatrix3d R = myState.XFrameToWorld.R;
      Twist tw = getVelocity();
      Vector3d tmp = new Vector3d();

      // elastic terms first
      cor.setZero();
      int numc = numElasticCoords();
      for (int i=0; i<numc; i++) {
         getShape (tmp, i, loc);
         cor.scaledAdd (myElasticVel.get(i), tmp);
      }     
      cor.transform (R);
      cor.scale (2);

      computeDeformedPos (tmp, loc);
      tmp.transform (R);
      cor.crossAdd (tw.w, tmp, cor);
      cor.cross (tw.w, cor);
   }

   /**
    * Adds to <code>wr</code> and <code>fe</code> the wrench and elastic forces
    * arising from applying a force <code>f</code> on a point <code>loc</code>.
    *
    * @param wr accumulates the wrench (world coordinates)
    * @param fe accumulates the elastic forces
    * @param loc location of the point (undeformed body coordinates)
    * @param f force applied to the point (world coordinates)
    */   
   public void addPointForce (
      Wrench wr, VectorNd fe, Point3d loc, Vector3d f) {

      Vector3d pos = new Vector3d();
      // compute deformed position in frame
      computeDeformedPos (pos, loc);
      // rotate position to world coordinates
      pos.transform (myState.XFrameToWorld.R);

      wr.f.add (f);
      wr.m.crossAdd (pos, f, wr.m);

      // now update elastic forces, using force in body frame
      int numc = numElasticCoords();
      Vector3d fbody = new Vector3d(f);
      fbody.inverseTransform (myState.XFrameToWorld.R);
      Vector3d shp = new Vector3d();
      for (int i=0; i<numc; i++) {
         getShape (shp, i, loc);
         fe.add (i, shp.dot(fbody));
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override public void addPointForce (Point3d loc, Vector3d f) {
      addPointForce (myForce, myElasticForce, loc, f);
   }

//   /**
//    * {@inheritDoc}
//    */
//   @Override public void addExternalPointForce (Point3d loc, Vector3d f) {
//      addPointForce (myExternalForce, myExternalElasticForce, loc, f);
//   }

   @Override
   public void computePointForceJacobian (MatrixBlock GT, Point3d loc) {
       MatrixNdBlock blk;
       try {
          blk = (MatrixNdBlock)GT;
       }
       catch (ClassCastException e) {
          throw new IllegalArgumentException (
             "GT is not an instance of MatrixNdBlock, is "+GT.getClass());
       }
       if (blk.rowSize() != getVelStateSize() ||
           blk.colSize() != 3) {
          throw new IllegalArgumentException (
             "GT has wrong size "+GT.getSize()+
             ", expecting "+getVelStateSize()+"x3");
       }
       RotationMatrix3d R = getPose().R;
       blk.setZero();

       blk.set (0, 0, 1.0);
       blk.set (1, 1, 1.0);
       blk.set (2, 2, 1.0);
       
       computeDeformedPos (myTmpPos, loc);
       myTmpPos.transform (R);

       blk.set (4, 0,  myTmpPos.z);
       blk.set (5, 0, -myTmpPos.y);
       blk.set (5, 1,  myTmpPos.x);
       
       blk.set (3, 1, -myTmpPos.z);
       blk.set (3, 2,  myTmpPos.y);
       blk.set (4, 2, -myTmpPos.x);

       int numc = numElasticCoords();
       Vector3d shp = new Vector3d();
       for (int i=0; i<numc; i++) {
          getShape (shp, i, loc);
          shp.transform (R);
          blk.set (6+i, 0, shp.x);
          blk.set (6+i, 1, shp.y);
          blk.set (6+i, 2, shp.z);
       }
   }

   /**
    * Computes the deformed position in body coordinates
    */
   public void computeDeformedPos (Vector3d pos, Vector3d pos0) {
      int numc = numElasticCoords();
      Vector3d shp = new Vector3d();
      pos.setZero();
      for (int i=0; i<numc; i++) {
         getShape (shp, i, pos0);
         pos.scaledAdd (myElasticPos.get(i), shp);
      }
      pos.add (pos0);
   }

   private static int maxIter = 100;

   /**
    * Computes the undeformed position of a given position in body coordinates
    */
   public double computeUndeformedPos (Vector3d pos0, Vector3d pos, double tol) {
      int numc = numElasticCoords();
      Vector3d shp = new Vector3d();
      Vector3d res = new Vector3d();
      Matrix3d F = new Matrix3d();
      computeDeformedPos (res, pos0);
      res.sub (pos, res);
      double resNorm;
      int icnt = 0;
      while ((resNorm=res.norm()) > tol && icnt++ < maxIter) {
         computeDeformationGradient (F, pos0);
         F.fastInvert(F);
         F.mulAdd (pos0, res, pos0);
         computeDeformedPos (res, pos0);
         res.sub (pos, res);
      }
      return resNorm;
   }

   /**
    * Computes the deformed velocity in body coordinates
    */
   public void computeDeformedVel (Vector3d vel, Vector3d pos0) {
      int numc = numElasticCoords();
      Vector3d shp = new Vector3d();
      for (int i=0; i<numc; i++) {
         getShape (shp, i, pos0);
         vel.scaledAdd (myElasticVel.get(i), shp);
      }
   }

   public void computeDeformationGradient (Matrix3d F, Vector3d x0) {
      int numc = numElasticCoords();
      Matrix3d Dshp = new Matrix3d();
      F.setIdentity();
      for (int i=0; i<numc; i++) {
         getDShape (Dshp, i, x0);
         F.scaledAdd (myElasticPos.get(i), Dshp);
      }
   }

   public void computeDeformedFrame (
      RigidTransform3d A, PolarDecomposition3d polarDecomp, 
      RigidTransform3d A0) {

      int numc = numElasticCoords();
      computeDeformedPos (A.p, A0.p);
      Matrix3d F = new Matrix3d();
      computeDeformationGradient (F, A0.p);
      // get polar decomposition for F
      polarDecomp.factor (F);
      A.R.mul (polarDecomp.getR(), A0.R);
   }

   public void computeUndeformedFrame (
      RigidTransform3d A0, PolarDecomposition3d polarDecomp, 
      RigidTransform3d A) {

      int numc = numElasticCoords();
      computeUndeformedPos (A0.p, A.p, 1e-8);
      Matrix3d F = new Matrix3d();
      computeDeformationGradient (F, A0.p);
      // get polar decomposition for F
      polarDecomp.factor (F);
      A0.R.mulInverseLeft (polarDecomp.getR(), A.R);
   }

   public void computeDeformedFrame (RigidTransform3d A, RigidTransform3d A0) {
      int numc = numElasticCoords();
      computeDeformedPos (A.p, A0.p);
      Matrix3d Dshp = new Matrix3d();
      Matrix3d F = new Matrix3d();
      RotationMatrix3d R = new RotationMatrix3d();
      F.setIdentity();
      for (int i=0; i<numc; i++) {
         getDShape (Dshp, i, A0.p);
         F.scaledAdd (myElasticPos.get(i), Dshp);
      }
      // get polar decomposition for F
      mySVD.polarDecomposition (R, (Matrix3d)null, F);
      A.R.mul (R, A0.R);
   }

//   /**
//    * Computes the spatial velocity of an attached frame A, as represented
//    * in the coordinates of A.
//    */
//   public void computeDeformedFrameVel (
//      Twist vel, RigidTransform3d A, RigidTransform3d A0) {
//
//      int numc = numElasticCoords();      
//      computeDeformedVel (vel.v, A0.p);
//      vel.v.inverseTransform (A.R); // transform from body to A coords
//      Matrix3d Dshp = new Matrix3d();
//      Matrix3d D = new Matrix3d();
//      RotationMatrix3d R = new RotationMatrix3d();
//      for (int i=0; i<numc; i++) {
//         getDShape (Dshp, i, A0.p);
//         D.scaledAdd (myElasticVel.get(i), Dshp);
//      }
//      // recover R from A0 and A
//      R.mulInverseRight (A.R, A0.R);
//      // compute inv(R) * D
//      D.mulTransposeLeft (R, D);
//
//      vel.w.x = 0.5*(D.m21 - D.m12);
//      vel.w.y = 0.5*(D.m02 - D.m20);
//      vel.w.z = 0.5*(D.m10 - D.m01);
//      // uncomment if we want vel in frame coords
//      // vel.w.transform (A.R);
//   }

   void computeNumericFrameVel (Twist vel, RigidTransform3d A0) {
      int numc = numElasticCoords();

      RigidTransform3d A = new RigidTransform3d();
      RigidTransform3d Ax = new RigidTransform3d();

      VectorNd eposSave = new VectorNd (numc);
      getElasticPos (eposSave);
      double h = 1e-8;

      computeDeformedFrame (A, A0);
      for (int i=0; i<numc; i++) {
         setElasticPos (i, getElasticPos(i)+h*getElasticVel(i));
      }
      computeDeformedFrame (Ax, A0);    
      Ax.mulInverseLeft (A, Ax);
      vel.set (Ax);
      vel.scale (1/h);
      //vel.transform (A.R);
      setElasticPos (eposSave);
   }

   // /**
   //  * Compute the transform that maps elastic velocities onto the spatial
   //  * velocity of an attached frame A, as represented in the coordinates
   //  * of A.
   //  */
   // public void computeElasticJacobian (
   //    MatrixNd Pi, RigidTransform3d A, RigidTransform3d A0) {

   //    int numc = numElasticCoords();   
   //    Vector3d shp = new Vector3d();
      
   //    Pi.setSize (6, numc);

   //    for (int i=0; i<numc; i++) {
   //       getShape (shp, i, A0.p);
   //       // transform from body coords to A coords
         
   //       shp.inverseTransform (A.R);
   //       Pi.set (0, i, shp.x);
   //       Pi.set (1, i, shp.y);
   //       Pi.set (2, i, shp.z);
   //    }

   //    Matrix3d Dshp = new Matrix3d();
   //    RotationMatrix3d R = new RotationMatrix3d();
   //    // recover R from A0 and A
   //    R.mulInverseRight (A.R, A0.R);

   //    for (int i=0; i<numc; i++) {
   //       getDShape (Dshp, i, A0.p);
   //       // compute inv(R) * D
   //       Dshp.mulTransposeLeft (R, Dshp);
   //       Pi.set (3, i, 0.5*(Dshp.m21 - Dshp.m12));
   //       Pi.set (4, i, 0.5*(Dshp.m02 - Dshp.m20));
   //       Pi.set (5, i, 0.5*(Dshp.m10 - Dshp.m01));
   //    }      
   // }

   /**
    * Computes the spatial velocity of an attached frame A, as represented
    * in the coordinates of A.
    */
   public void computeDeformedFrameVel (
      Twist vel, PolarDecomposition3d polarDecomp, RigidTransform3d A0) {

      int numc = numElasticCoords();      
      computeDeformedVel (vel.v, A0.p);
      // transform from body to A coords:
      vel.v.inverseTransform (polarDecomp.getR());
      vel.v.inverseTransform (A0.R);
      Matrix3d Dshp = new Matrix3d();
      Matrix3d D = new Matrix3d();
      for (int i=0; i<numc; i++) {
         getDShape (Dshp, i, A0.p);
         D.scaledAdd (myElasticVel.get(i), Dshp);
      }
      // compute inv(R) * D
      D.mulTransposeLeft (polarDecomp.getR(), D);

      Vector3d sig = polarDecomp.getSig();
      Matrix3d Binv = new Matrix3d(polarDecomp.getV());
      Binv.mulDiagonalRight (
         -1/(sig.y+sig.z), -1/(sig.x+sig.z), -1/(sig.x+sig.y));
      Binv.mulTransposeRight (Binv, polarDecomp.getV());

      Vector3d avec = new Vector3d();
      avec.x =  D.m12 - D.m21;
      avec.y = -D.m02 + D.m20;
      avec.z =  D.m01 - D.m10;
      Binv.mul (vel.w, avec);

      vel.w.inverseTransform (A0.R);

      // uncomment if we want velocity in frame coords
      //vel.w.transform (A0.R);
      //vel.w.transform (polarDecomp.getR());
   }

   /**
    * Compute the transform that maps elastic velocities onto the spatial
    * velocity of an attached frame A, as represented in the coordinates of A.
    */
   public void computeElasticJacobian (
      MatrixNd Pi, PolarDecomposition3d polarDecomp, RigidTransform3d A0) {

      int numc = numElasticCoords();   
      Vector3d shp = new Vector3d();
      
      Pi.setSize (6, numc);

      for (int i=0; i<numc; i++) {
         getShape (shp, i, A0.p);
         // transform from body coords to A coords
         shp.inverseTransform (polarDecomp.getR());
         shp.inverseTransform (A0.R);

         Pi.set (0, i, shp.x);
         Pi.set (1, i, shp.y);
         Pi.set (2, i, shp.z);
      }

      Matrix3d Dshp = new Matrix3d();
      
      Vector3d sig = polarDecomp.getSig();
      Matrix3d Binv = new Matrix3d(polarDecomp.getV());
      Binv.mulDiagonalRight (
         -1/(sig.y+sig.z), -1/(sig.x+sig.z), -1/(sig.x+sig.y));
      Binv.mulTransposeRight (Binv, polarDecomp.getV());
      Vector3d avec = new Vector3d();

      for (int i=0; i<numc; i++) {
         getDShape (Dshp, i, A0.p);
         // compute inv(R) * Dshp
         Dshp.mulTransposeLeft (polarDecomp.getR(), Dshp);
         avec.x =  Dshp.m12 - Dshp.m21;
         avec.y = -Dshp.m02 + Dshp.m20;
         avec.z =  Dshp.m01 - Dshp.m10;
         Binv.mul (avec, avec);

         avec.inverseTransform (A0.R);
         // uncomment this if we want Jacobian for velocity in frame coordinates
         //avec.transform (A0.R);
         //avec.transform (polarDecomp.getR());
         
         Pi.set (3, i, avec.x);
         Pi.set (4, i, avec.y);
         Pi.set (5, i, avec.z);
      }      
   }

   // /**
   //  * Adds the effect of a force applied at a specific postion with respect to
   //  * this frame. The force is in world coordinates and the position in frame
   //  * coordinates.
   //  */
   // @Override public void addPointForce (Vector3d f, Point3d loc) {
   //    // compute deformed position in frame
   //    computeDeformedPos (myTmpPos, loc);
   //    // rotate position to world coordinates
   //    myTmpPos.transform (myState.XFrameToWorld.R);
   //    myForce.f.add (f, myForce.f);
   //    myForce.m.crossAdd (myTmpPos, f, myForce.m);

   //    // now update elastic forces, using force in body frame
   //    int numc = numElasticCoords();
   //    Vector3d fbody = new Vector3d(f);
   //    fbody.inverseTransform (myState.XFrameToWorld.R);
   //    Vector3d shp = new Vector3d();
   //    for (int i=0; i<numc; i++) {
   //       getShape (shp, i, loc);
   //       myElasticForce.add (i, shp.dot(fbody));
   //    }
   // }

   public void setElasticPos (int idx, double value) {
      myElasticPos.set (idx, value);
      updatePosState();
   }

   public double getElasticPos (int idx) {
      return myElasticPos.get (idx);
   }

   public void setElasticVel (int idx, double value) {
      myElasticVel.set (idx, value);
      updateVelState();
   }

   public double getElasticVel (int idx) {
      return myElasticVel.get (idx);
   }

   public void setContactConstraint (
      double[] buf, double w, Vector3d dir, ContactPoint cpnt) {

      double lx = cpnt.myPoint.x - myState.pos.x;
      double ly = cpnt.myPoint.y - myState.pos.y;
      double lz = cpnt.myPoint.z - myState.pos.z;

      double nx = w*dir.x;
      double ny = w*dir.y;
      double nz = w*dir.z;

      buf[0] = nx;
      buf[1] = ny;
      buf[2] = nz;
      buf[3] = ly*nz - lz*ny;
      buf[4] = lz*nx - lx*nz;
      buf[5] = lx*ny - ly*nx;

      // transform dir to local coords
      Vector3d dirl = new Vector3d(dir);
      dirl.inverseTransform (myState.XFrameToWorld.R);
      Vector3d loc = new Vector3d();
      Vector3d shp = new Vector3d();      
      computePointLocation (loc, cpnt.myPoint);
      int numc = numElasticCoords();
      for (int i=0; i<numc; i++) {
         getShape (shp, i, loc);
         buf[6+i] = w*shp.dot(dirl);
      }
   }

   public void addToPointVelocity (
      Vector3d vel, double w, ContactPoint cpnt) {

      Vector3d loc = new Vector3d();
      Vector3d tmp = new Vector3d();
      computePointLocation (loc, cpnt.myPoint);
      // compute elastic velocity in body coords
      computeDeformedVel (tmp, loc);
      // rotate to world coords
      tmp.transform (myState.XFrameToWorld.R);

      // add additional velocity components from body motion
      Vector3d v = myState.vel.v;
      Vector3d o = myState.vel.w; // o for omega
      double lx = cpnt.myPoint.x - myState.pos.x;
      double ly = cpnt.myPoint.y - myState.pos.y;
      double lz = cpnt.myPoint.z - myState.pos.z;
      vel.x += w*(v.x - ly*o.z + lz*o.y + tmp.x);
      vel.y += w*(v.y - lz*o.x + lx*o.z + tmp.y);
      vel.z += w*(v.z - lx*o.y + ly*o.x + tmp.z);
   }
}
