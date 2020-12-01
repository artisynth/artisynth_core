/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;

import artisynth.core.materials.FemMaterial;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.util.ScanToken;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix2d;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.MatrixBlock;
import maspack.matrix.MatrixNd;
import maspack.matrix.MatrixNdBlock;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.render.Renderer;
import maspack.spatialmotion.SpatialInertia;
import maspack.spatialmotion.Twist;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.RandomGenerator;
import maspack.util.ReaderTokenizer;

public class EBBeamBody extends DeformableBody {

   public static boolean useNewMass = true;
   public static boolean useNewMassDamping = true;
   public static boolean useMassCrossTerms = true;
   
   public boolean applyXCorrection = false;
   public double myXScale = 1.0;

   double myLen;
   // elastic mass coefficients
   double mySig00 = 0;
   double mySig01 = 0;
   double mySig11 = 0;
   double mySigx0 = 0;
   double mySigx1 = 0;
   double mySigxx = 0;
   Point3d myCom = new Point3d();
   MatrixNd myUnitMassMatrix = new MatrixNd(10,10);
   MatrixNd myAttachedFrameMassMatrix = null;
   double myEffectiveMass = 0;
   double myAttachedPointMass = 0;

   private static double DEFAULT_STIFFNESS = 1.0;

   double myStiffness = DEFAULT_STIFFNESS;

   public static PropertyList myProps =
      new PropertyList (EBBeamBody.class, DeformableBody.class);

   static {
      myProps.add (
         "stiffness", "beam stiffness", DEFAULT_STIFFNESS);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getStiffness () {
      return myStiffness;
   }

   public void setStiffness (double E) {
      myStiffness = E;
      invalidateStiffness();
   }
   
   protected void setLength (double len) {
      myLen = len;
      updateMassTerms();
      invalidateStiffness();
   }
   
   protected double getLength() {
      return myLen;
   }
   
   void updateMassTerms() {
      double len = getLength();
      mySig00 = 13/35.0;
      mySig01 = -len*11/210.0;
      mySig11 = len*len/105.0;
      mySigx0 = len*7/20.0;
      mySigx1 = -len*len/20.0;
      mySigxx = len*len/3.0;
   }

   @Override public <T extends FemMaterial> T setMaterial (T mat) {
      T newMat = super.setMaterial (mat);
      invalidateStiffness();
      return newMat;
   }

   public EBBeamBody () {
   }

   public EBBeamBody (
      PolygonalMesh mesh, double len, double density, double E) {

      setDensity (density);
      setMesh (mesh, /*meshFileName=*/null);
      setLength (len);
      setStiffness (E);
   }

   public EBBeamBody (
      double len, double rad, double density, double E) {
      
      PolygonalMesh mesh = MeshFactory.createCylinder (
         rad, len, /*nslices=*/20, /*nr=*/1, /*nh=*/20);
      mesh.transform (
         new RigidTransform3d (len/2, 0, 0,  0, Math.PI/2, 0));

      setDensity (density);
      setMesh (mesh, /*meshFileName=*/null);
      setLength (len);
      setStiffness (E);
   }

   public int numElasticCoords() {
      return 4;
   }

   @Override
   public void computeDeformedLocation (Vector3d pos, Vector3d pos0) {
      double a0 = myElasticPos.get(0);
      double a1 = myElasticPos.get(1);
      double a2 = myElasticPos.get(2);
      double a3 = myElasticPos.get(3);
      double xi = 2*pos0.x/myLen-1;
      double s0 = shape0(xi);
      double s1 = shape1(xi);
      
      if (applyXCorrection) {
         pos.x = myXScale*pos0.x;
      }
      else {
         pos.x = pos0.x;
      }
      pos.y = pos0.y + s0*a0 + s1*a1;
      pos.z = pos0.z + s0*a2 + s1*a3;
   }

   @Override
   public double computeUndeformedLocation (
      Vector3d pos0, Vector3d pos, double tol) {
      // since x does not change, and the shape functions are functions of x,
      // we can directly determine the shape functions and hence the
      // deformation.
      int numc = numElasticCoords();
      Vector3d def = new Vector3d();
      Vector3d shp = new Vector3d();
      for (int i=0; i<numc; i++) {
         getShape (shp, i, pos);
         def.scaledAdd (myElasticPos.get(i), shp);
      }
      pos0.sub (pos, def);
      return 0;
   }

   protected void computeDeformedRotation (RotationMatrix3d R, Vector3d p0) {
      double xi = 2*p0.x/myLen-1;
      double dshp0 = dshape0(xi);
      double dshp1 = dshape1(xi);
      double dy = dshp0*myElasticPos.get(0) + dshp1*myElasticPos.get(1);
      double dz = dshp0*myElasticPos.get(2) + dshp1*myElasticPos.get(3);

      Vector3d xdir = new Vector3d (1, dy, dz);
      //xdir.normalize();
      R.setXDirection (xdir);      
   }

   @Override
   public void computeDeformedFrame (RigidTransform3d A, RigidTransform3d A0) {
      computeDeformedLocation (A.p, A0.p);
      RotationMatrix3d R = new RotationMatrix3d();
      computeDeformedRotation (R, A0.p);
      A.R.mul (R, A0.R);
   }

   @Override
   public void computeUndeformedFrame (
      RigidTransform3d A0, RigidTransform3d A) {

      // Note: this class does not need tolerance to compute undeformed location
      computeUndeformedLocation (A0.p, A.p, /*tol=*/0); 
      RotationMatrix3d R = new RotationMatrix3d();
      computeDeformedRotation (R, A.p);    
      A0.R.mulInverseLeft (R, A.R);
   }

   @Override
   public void computeDeformedFrameVel (Twist vel, RigidTransform3d A0) {
      
      computeDeformedVelocity (vel.v, A0.p);

      double xi = 2*A0.p.x/myLen-1;
      double dshp0 = dshape0(xi);
      double dshp1 = dshape1(xi);
      double dy = dshp0*myElasticPos.get(0) + dshp1*myElasticPos.get(1);
      double dz = dshp0*myElasticPos.get(2) + dshp1*myElasticPos.get(3);
      double dyvel = dshp0*myElasticVel.get(0) + dshp1*myElasticVel.get(1);
      double dzvel = dshp0*myElasticVel.get(2) + dshp1*myElasticVel.get(3);

      double t = Math.sqrt(dy*dy + dz*dz);

      if (t <= 1e-12) {
         vel.w.set (0, -1/(1+dz*dz)*dzvel, 1/(1+dy*dy)*dyvel);
      }
      else {
         double uy = -dz/t;
         double uz = dy/t;
         double c2 = 1/(1+t*t);
         double c = Math.sqrt(c2);
         double vt = (1-c)/t;

         double ayy = c*(1-c)*uy*uz;
         double ayz = c*(c*uy*uy + uz*uz);
         double azy = c*(c*uz*uz + uy*uy);

         vel.w.set (
            vt*(uy*dyvel+uz*dzvel),
            -ayy*dyvel-ayz*dzvel, 
            azy*dyvel+ayy*dzvel);
      }
   }

   protected void setRotationCol (MatrixNd Pi, Vector3d col, int j) {
      Pi.set (3, j, col.x);
      Pi.set (4, j, col.y);
      Pi.set (5, j, col.z);
   }

   @Override
   public void computeElasticJacobian (
      MatrixNd Pi, RigidTransform3d A0, boolean worldCoords) {

      int numc = numElasticCoords();   
      Vector3d shp = new Vector3d();
      RotationMatrix3d RBW = myState.getPose().R;
      
      Pi.setSize (6, numc);
      for (int i=0; i<numc; i++) {
         getShape (shp, i, A0.p);
         shp.transform (RBW);
         Pi.set (0, i, shp.x);
         Pi.set (1, i, shp.y);
         Pi.set (2, i, shp.z);
      }
     
      double xi = 2*A0.p.x/myLen-1;
      double dshp0 = dshape0(xi);
      double dshp1 = dshape1(xi);
      double dy = dshp0*myElasticPos.get(0) + dshp1*myElasticPos.get(1);
      double dz = dshp0*myElasticPos.get(2) + dshp1*myElasticPos.get(3);

      Vector3d wcol = new Vector3d();

      double t = Math.sqrt(dy*dy + dz*dz);

      if (t <= 1e-12) {
         wcol.set (0, 0, 1/(1+dy*dy)*dshp0);
         wcol.transform (RBW);
         setRotationCol (Pi, wcol, 0);

         wcol.set (0, 0, 1/(1+dy*dy)*dshp1);
         wcol.transform (RBW);
         setRotationCol (Pi, wcol, 1);

         wcol.set (0, -1/(1+dz*dz)*dshp0, 0);
         wcol.transform (RBW);
         setRotationCol (Pi, wcol, 2);

         wcol.set (0, -1/(1+dz*dz)*dshp1, 0);
         wcol.transform (RBW);
         setRotationCol (Pi, wcol, 3);  
      }
      else {
         double uy = -dz/t;
         double uz = dy/t;
         double c2 = 1/(1+t*t);
         double c = Math.sqrt(c2);
         double vt = (1-c)/t;

         double ayy = c*(1-c)*uy*uz;
         double ayz = c*(c*uy*uy + uz*uz);
         double azy = c*(c*uz*uz + uy*uy);
         
         wcol.set (vt*uy*dshp0, -ayy*dshp0, azy*dshp0);
         wcol.transform (RBW);
         setRotationCol (Pi, wcol, 0);

         wcol.set (vt*uy*dshp1, -ayy*dshp1, azy*dshp1);
         wcol.transform (RBW);
         setRotationCol (Pi, wcol, 1);

         wcol.set (vt*uz*dshp0, -ayz*dshp0, ayy*dshp0);
         wcol.transform (RBW);
         setRotationCol (Pi, wcol, 2);
         
         wcol.set (vt*uz*dshp1, -ayz*dshp1, ayy*dshp1);
         wcol.transform (RBW);
         setRotationCol (Pi, wcol, 3);
      }
   }

   protected void computeFrameFrameJacobian (
      MatrixBlock J, RigidTransform3d TFL0) {

      int numc = numElasticCoords();
      if (!(J instanceof MatrixNdBlock)) {
          throw new IllegalArgumentException (
             "J is not an instance of MatrixNdBlock; is " + J.getClass());
      }
      MatrixNdBlock Jb = (MatrixNdBlock)J;
      if (Jb.colSize() != 6 || Jb.rowSize() != 6+numc) {
         throw new IllegalArgumentException (
            "J expected to be "+(6+numc)+"x6, is " + J.getSize());
      }

      // TODO Auto-generated method stub
      super.computeFrameFrameJacobian (J, TFL0);
   }

   public void updateStiffnessMatrix() {
      if (!myStiffnessValidP) {
         computeStiffnessMatrix();
      }
   }

   public void computeStiffnessMatrix() {
      myStiffnessMatrix.setZero();

      //computeStiffnessFromIntegration();

      Matrix2d Ksub = new Matrix2d();

      double l = myLen;
      double lsqr = l*l;
      double lcub = l*lsqr;
      double Jxx0 = mySpatialInertia.getRotationalInertia().m00;

      Ksub.set (new double[] {
            12/lcub, -6/lsqr,
            -6/lsqr, 4/l });

      Ksub.scale (Jxx0*myStiffness);

      myStiffnessMatrix.setSubMatrix (0, 0, Ksub);
      myStiffnessMatrix.setSubMatrix (2, 2, Ksub);

      myStiffnessValidP = true;
   }

   private void setCrossTerms (
      MatrixNd MN, int i, int j,
      RotationMatrix3d R, double x, double y, double z) {

      Vector3d v = new Vector3d (x, y, z);
      v.transform (R);                          
      MN.set (i, j, v.x);
      MN.set (j, i, v.x);
      i++;
      MN.set (i, j, v.y);
      MN.set (j, i, v.y);
      i++;
      MN.set (i, j, v.z);
      MN.set (j, i, v.z);
   }

   private void addCrossTerms (
      MatrixNd MN, int i, int j,
      RotationMatrix3d R, double x, double y, double z) {

      Vector3d v = new Vector3d (x, y, z);
      v.transform (R);                          
      MN.add (i, j, v.x);
      MN.add (j, i, v.x);
      i++;
      MN.add (i, j, v.y);
      MN.add (j, i, v.y);
      i++;
      MN.add (i, j, v.z);
      MN.add (j, i, v.z);
   }

   private void computeMass (MatrixNd M, double m, Vector3d com) {
      RotationMatrix3d R = getPose().R;
      
      double a0 = myElasticPos.get(0);
      double a1 = myElasticPos.get(1);
      double a2 = myElasticPos.get(2);
      double a3 = myElasticPos.get(3);

      double sig0 = 0.5;
      double sig1 = -myLen/12.0;

      double cx = myLen/2.0;
      double cy = sig0*a0 + sig1*a1;
      double cz = sig0*a2 + sig1*a3;

      double ms00 = m*mySig00;
      double ms01 = m*mySig01;
      double ms11 = m*mySig11;
      double msx0 = m*mySigx0;
      double msx1 = m*mySigx1;
      double msxx = m*mySigxx;

      double cxx = msxx;
      double cyy = ms00*a0*a0 + 2*ms01*a0*a1 + ms11*a1*a1;
      double czz = ms00*a2*a2 + 2*ms01*a2*a3 + ms11*a3*a3;
      double cxy = msx0*a0 + msx1*a1;
      double cyz = ms00*a0*a2 + ms01*(a0*a3+a1*a2) + ms11*a1*a3;
      double czx = msx0*a2 + msx1*a3;

      double Jxx0 = mySpatialInertia.getRotationalInertia().m00;

      SpatialInertia SI = new SpatialInertia();
      SymmetricMatrix3d J = new SymmetricMatrix3d();
      J.set (cyy + czz - m*(cy*cy+cz*cz) + Jxx0,
             czz + cxx - m*(cx*cx+cz*cz),
             cxx + cyy - m*(cx*cx+cy*cy), 
             -cxy + m*cx*cy,
             -czx + m*cz*cx,
             -cyz + m*cy*cz);
      SI.set (m, J, new Point3d (cx, cy, cz));

      SymmetricMatrix3d Jchk =
         new SymmetricMatrix3d(cyy+czz, czz+cxx, cxx+cyy, -cxy, -czx, -cyz);
      SymmetricMatrix3d Jres =
         new SymmetricMatrix3d(SI.m33, SI.m44, SI.m55, SI.m34, SI.m35, SI.m45);

      Matrix6d MR = new Matrix6d();
      SI.getRotated (MR, R);
      M.setSubMatrix (0, 0, MR);

      if (useMassCrossTerms) {
         setCrossTerms (M, 0, 6, R,  0, m*sig0, 0);
         setCrossTerms (M, 0, 7, R,  0, m*sig1, 0);
         setCrossTerms (M, 0, 8, R,  0, 0, m*sig0);
         setCrossTerms (M, 0, 9, R,  0, 0, m*sig1);

         setCrossTerms (M, 3, 6, R,  -(ms00*a2+ms01*a3), 0, msx0);
         setCrossTerms (M, 3, 7, R,  -(ms01*a2+ms11*a3), 0, msx1);
         setCrossTerms (M, 3, 8, R,   (ms00*a0+ms01*a1), -msx0, 0);
         setCrossTerms (M, 3, 9, R,   (ms01*a0+ms11*a1), -msx1, 0);
      }      

      M.set (6, 6, ms00);
      M.set (6, 7, ms01);
      M.set (6, 8, 0);
      M.set (6, 9, 0);

      M.set (7, 6, ms01);
      M.set (7, 7, ms11);
      M.set (7, 8, 0);
      M.set (7, 9, 0);

      M.set (8, 6, 0);
      M.set (8, 7, 0);
      M.set (8, 8, ms00);
      M.set (8, 9, ms01);

      M.set (9, 6, 0);
      M.set (9, 7, 0);
      M.set (9, 8, ms01);
      M.set (9, 9, ms11);
   }

   @Override
   public void getMass (Matrix M, double t) {
      // for now, assume that the rigid and elastic mass matrices are decoupled
      // with a diagonal elastic mass matrix
      if (useNewMass) {
         checkMassMatrixType ("M", M);
         ((MatrixNd)M).scale (getMass(), myUnitMassMatrix);
      }
      else {
         super.getMass (M, t);
      }
   }


   @Override
   public int getEffectiveMassForces (VectorNd f, double t, int idx) {
      // XXX for now, just set everything to zero
      //idx = super.getEffectiveMassForces (f, t, idx);
      double[] buf = f.getBuffer();
      for (int i=0; i<6+numElasticCoords(); i++) {
         buf[idx++] = 0;
      }
      return idx;
   }

   @Override
   public void getEffectiveMass (Matrix M, double t) {
      // for now, assume that the rigid and elastic mass matrices are decoupled
      // with a diagonal elastic mass matrix
      if (useNewMass) {
         checkMassMatrixType ("M", M);
         double m = getMass() + myAttachedPointMass; 
         ((MatrixNd)M).scale (m, myUnitMassMatrix);
         if (myAttachedFrameMassMatrix != null) {
            ((MatrixNd)M).add (myAttachedFrameMassMatrix);
         }
      }
      else {
         super.getEffectiveMass (M, t);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public double getEffectiveMass() {
      return myEffectiveMass;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public SpatialInertia getEffectiveInertia() {
      // just extract the regular inertia part of the mass matrix
      // and rotate back to body coordinates
      SpatialInertia SI = new SpatialInertia();
      getSubMatrix00 (SI, myUnitMassMatrix);
      SI.scale (getMass() + myAttachedPointMass);
      if (myAttachedFrameMassMatrix != null) {
         Matrix6d MA = new Matrix6d();
         getSubMatrix00 (MA, myAttachedFrameMassMatrix);
         SI.add (MA);
      }
      SI.invalidateComponents();
      SI.inverseTransform (getPose().R);
      return SI;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void addEffectivePointMass (double m, Vector3d loc) {
      // simplify by simply adding to the overall mass
      myAttachedPointMass += m;
      myEffectiveMass += m;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void addEffectiveFrameMass (SpatialInertia M, RigidTransform3d TFL0) {
      if (myAttachedFrameMassMatrix == null) {
         myAttachedFrameMassMatrix = new MatrixNd (10, 10);
      }
      MatrixNd MA = myAttachedFrameMassMatrix;
      RotationMatrix3d R = getPose().R;
      double m = M.getMass();
      
      RigidTransform3d TFL = new RigidTransform3d();

      computeDeformedFrame (TFL, TFL0);
      // The additional mass for an attached frame is complex. We simplify by 
      // assuming it corresponds to a simple lumped mass at the frames's center
      // of mass.

      Point3d cl = new Point3d(M.getCenterOfMass());
      // compute center of mass frame in beam coordinates
      cl.transform (TFL);
      
      double xi = 2*TFL0.p.x/myLen-1;
      double s0 = shape0(xi);
      double s1 = shape1(xi);
      double ms0 = m*s0;
      double ms1 = m*s1;
      double ms00 = ms0*s0;
      double ms01 = ms0*s1;
      double ms11 = ms1*s1;

      // add elastic terms

      MA.add (6, 6, ms00);
      MA.add (6, 7, ms01);
      MA.add (7, 6, ms01);
      MA.add (7, 7, ms11);

      MA.add (8, 8, ms00);
      MA.add (8, 9, ms01);
      MA.add (9, 8, ms01);
      MA.add (9, 9, ms11);

      // add elastic-inertia cross terms     
      
      addCrossTerms (MA, 0, 6, R,  0, ms0, 0);
      addCrossTerms (MA, 0, 7, R,  0, ms1, 0);
      addCrossTerms (MA, 0, 8, R,  0, 0, ms0);
      addCrossTerms (MA, 0, 9, R,  0, 0, ms1);

      addCrossTerms (MA, 3, 6, R,  ms0*cl.z, 0, -ms0*cl.x);
      addCrossTerms (MA, 3, 7, R,  ms1*cl.z, 0, -ms1*cl.x);
      addCrossTerms (MA, 3, 8, R, -ms0*cl.y, ms0*cl.x, 0);
      addCrossTerms (MA, 3, 9, R, -ms1*cl.y, ms1*cl.x, 0);

      // add inertia terms

      MA.add (0, 0, m);
      MA.add (1, 1, m);
      MA.add (2, 2, m);

      Vector3d cw = new Vector3d();
      Vector3d mc = new Vector3d();
      // compute com rotated into world coords:
      cw.transform (R, cl);
      // also compute com*mass
      mc.scale (m, cw);
      
      MA.add (0, 4, mc.z);
      MA.add (0, 5, -mc.y);
      MA.add (1, 3, -mc.z);
      MA.add (1, 5, mc.x);
      MA.add (2, 3, mc.y);
      MA.add (2, 4, -mc.x);

      MA.add (4, 0, mc.z);
      MA.add (5, 0, -mc.y);
      MA.add (3, 1, -mc.z);
      MA.add (5, 1, mc.x);
      MA.add (3, 2, mc.y);
      MA.add (4, 2, -mc.x);

      double mcx2 = mc.x*cw.x;
      double mcy2 = mc.y*cw.y;
      double mcz2 = mc.z*cw.z;
      double mcxy = mc.x*cw.y;
      double mcyz = mc.y*cw.z;
      double mczx = mc.z*cw.x;

      MA.add (3, 3, mcy2 + mcz2);
      MA.add (4, 4, mcz2 + mcx2);
      MA.add (5, 5, mcx2 + mcy2);
      MA.add (3, 4, -mcxy);
      MA.add (4, 3, -mcxy);
      MA.add (3, 5, -mczx);
      MA.add (5, 3, -mczx);
      MA.add (4, 5, -mcyz);
      MA.add (5, 4, -mcyz);

      myEffectiveMass += m;
   }

   @Override
   public void resetEffectiveMass() {
      myEffectiveMass = getMass();
      myAttachedPointMass = 0;
      myAttachedFrameMassMatrix = null;
   }

   @Override
   protected void addElasticMassDamping (VectorNd eforce) {
      if (useNewMass) {
         double d = -myMassDamping*getMass();
         double v0 = myElasticVel.get(0);
         double v1 = myElasticVel.get(1);
         double v2 = myElasticVel.get(2);
         double v3 = myElasticVel.get(3);
         
         double d00 = d*mySig00;
         double d01 = d*mySig01;
         double d11 = d*mySig11;
         
         eforce.add (0, d00*v0 + d01*v1);
         eforce.add (1, d01*v0 + d11*v1);
         eforce.add (2, d00*v2 + d01*v3);
         eforce.add (3, d01*v2 + d11*v3);
      }
      else {
         super.addElasticMassDamping (eforce);
      }
   }
   
   @Override
   protected void addElasticMassDampingJacobian (
      MatrixNdBlock blk, double s) {

      if (useNewMass) {
         double d = -s*myMassDamping*getMass();
         double d00 = d*mySig00;
         double d01 = d*mySig01;
         double d11 = d*mySig11;
     
         blk.add (6, 6, d00);
         blk.add (6, 7, d01);
         blk.add (7, 6, d01);
         blk.add (7, 7, d11);

         blk.add (8, 8, d00);
         blk.add (8, 9, d01);
         blk.add (9, 8, d01);
         blk.add (9, 9, d11);
      }
      else {
         super.addElasticMassDampingJacobian (blk, s);
      }
   } 

   @Override 
   public void applyForces (double t) {
      if (useNewMassDamping && useNewMass) {
         super.applyForces (t);
         updateStiffnessMatrix();

         // add damping forces first ...
         myStiffnessMatrix.mul (myElasticTmp, myElasticVel);
         myElasticTmp.scale (-myStiffnessDamping);
         VectorNd vel = new VectorNd(10);
         VectorNd dforce = new VectorNd(10);
         vel.setSubVector (0, getVelocity());
         vel.setSubVector (6, myElasticVel);
         myUnitMassMatrix.mul (dforce, vel);
         dforce.scale (-myMassDamping);

         myForce.f.x += dforce.get(0);
         myForce.f.y += dforce.get(1);
         myForce.f.z += dforce.get(2);
         myForce.m.x += dforce.get(3);
         myForce.m.y += dforce.get(4);
         myForce.m.z += dforce.get(5);
         myElasticForce.add (0, dforce.get(6));
         myElasticForce.add (1, dforce.get(7));
         myElasticForce.add (2, dforce.get(8));
         myElasticForce.add (3, dforce.get(9));

         // then add elastic forces ...
         myStiffnessMatrix.mul (myElasticTmp, myElasticPos);
         myElasticForce.sub (myElasticTmp);
      }
      else {
         super.applyForces (t);
      }
   }

   @Override public void addVelJacobian (SparseNumberedBlockMatrix S, double s) {
      if (useNewMassDamping && useNewMass) {
         MatrixNdBlock blk =
            (MatrixNdBlock)S.getBlockByNumber (mySolveBlockNum);
         if (myMassDamping != 0) {
            blk.scaledAdd (-s*myMassDamping, myUnitMassMatrix);
         }
         if (myStiffnessDamping != 0) {
            blk.addScaledSubMatrix (
               6, 6, -s*myStiffnessDamping, myStiffnessMatrix);
         }
      }
      else {
         super.addVelJacobian (S, s);
      }     
   }  

   @Override
   public void applyGravity (Vector3d gacc) {
      double mass = getMass();

      Vector3d com = new Vector3d(myCom);
      Vector3d fgrav = new Vector3d();
      com.transform (myState.XFrameToWorld.R);
      fgrav.scale (mass, gacc);
      myForce.f.add (fgrav, myForce.f);
      myForce.m.crossAdd (com, fgrav, myForce.m);
      
      //super.applyGravity (gacc);

      // transform gravity to local coordinates
      Vector3d gloc = new Vector3d();
      gloc.inverseTransform (getPose().R, gacc);
      

      myElasticForce.add (0, mass*gloc.y/2.0);
      myElasticForce.add (1, -mass*myLen/12.0*gloc.y);

      myElasticForce.add (2, mass*gloc.z/2.0);
      myElasticForce.add (3, -mass*myLen/12.0*gloc.z);
   }

   void computeCenterOfMass (Vector3d com) {
      double a0 = myElasticPos.get(0);
      double a1 = myElasticPos.get(1);
      double a2 = myElasticPos.get(2);
      double a3 = myElasticPos.get(3);

      // compute center of mass
      double sig0 = 0.5;
      double sig1 = -myLen/12.0;

      com.set (myLen/2.0, sig0*a0 + sig1*a1, sig0*a2 + sig1*a3);
   }      

   private void updateXCorrectionTerms() {
      double a0 = myElasticPos.get(0);
      double a2 = myElasticPos.get(2);
      
      double h2 = a0*a0 + a2*a2;
      double h = Math.sqrt(h2);
      myXScale = Math.sqrt (myLen*myLen-h2)/myLen;
      System.out.println ("scale=" + myXScale);
   }
   
   @Override protected void updateSlavePosStates() {
      super.updateSlavePosStates();

      if (applyXCorrection) {
         updateXCorrectionTerms();
      }
      computeCenterOfMass (myCom);
      computeMass (myUnitMassMatrix, 1.0, myCom);
   }  

   private static double sqr (double x) {
      return x*x;
   }

   public void getShape (Vector3d shp, int i, Vector3d pos0) {

      double xi = 2*pos0.x/myLen-1;
      shp.setZero();
      
      switch (i) {
         case 0: shp.y = shape0 (xi); return;
         case 1: shp.y = shape1 (xi); return;
         case 2: shp.z = shape0 (xi); return;
         case 3: shp.z = shape1 (xi); return;
         default: {
            throw new InternalErrorException (
               "shape function index "+i+" exceeds "+(numElasticCoords()-1));
         }
      }
   }

   public double shape0 (double xi) {
      return 0.25*sqr(1+xi)*(2-xi);
   }

   public double shape1 (double xi) {
      return -0.125*myLen*sqr(1+xi)*(1-xi);
   }

   public double dshape0 (double xi) {
      return 1.5*(1-xi*xi)/myLen; 
   }

   public double dshape1 (double xi) {
      return 0.25*(3*xi*xi+2*xi-1);
   }

   public void getDShape (Matrix3d Dshp, int i, Vector3d pos0) {

      double xi = 2*pos0.x/myLen-1;
      Dshp.setZero();

      switch (i) {
         case 0: Dshp.m10 = dshape0 (xi); return;
         case 1: Dshp.m10 = dshape1 (xi); return;
         case 2: Dshp.m20 = dshape0 (xi); return;
         case 3: Dshp.m20 = dshape1 (xi); return;
         default: {
            throw new InternalErrorException (
               "shape function index "+i+" exceeds "+(numElasticCoords()-1));
         }
      }
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "length")) {
         double len = rtok.scanNumber();
         setLength (len);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }   

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.println ("length=" + fmt.format (myLen));
      super.writeItems (pw, fmt, ancestor);
   }

   /**
    * Extracts the 6x6 submatrix of MN starting at 0,0. Assumes
    * that MN is 10x10.
    */
   private void getSubMatrix00 (Matrix6d M6, MatrixNd MN) {
      double[] buf = MN.getBuffer();

      M6.m00 = buf[00]; M6.m01 = buf[01]; M6.m02 = buf[02];
      M6.m03 = buf[03]; M6.m04 = buf[04]; M6.m05 = buf[05];

      M6.m10 = buf[10]; M6.m11 = buf[11]; M6.m12 = buf[12];
      M6.m13 = buf[13]; M6.m14 = buf[14]; M6.m15 = buf[15];

      M6.m20 = buf[20]; M6.m21 = buf[21]; M6.m22 = buf[22];
      M6.m23 = buf[23]; M6.m24 = buf[24]; M6.m25 = buf[25];

      M6.m30 = buf[30]; M6.m31 = buf[31]; M6.m32 = buf[32];
      M6.m33 = buf[33]; M6.m34 = buf[34]; M6.m35 = buf[35];

      M6.m40 = buf[40]; M6.m41 = buf[41]; M6.m42 = buf[42];
      M6.m43 = buf[43]; M6.m44 = buf[44]; M6.m45 = buf[45];

      M6.m50 = buf[50]; M6.m51 = buf[51]; M6.m52 = buf[52];
      M6.m53 = buf[53]; M6.m54 = buf[54]; M6.m55 = buf[55];
   }

   public static void main (String[] args) {

      RandomGenerator.setSeed (0x1234);

      PolygonalMesh mesh =
         MeshFactory.createBox (2, 0.1, 0.1);
      EBBeamBody body = new EBBeamBody (mesh, 2.0, 1000.0, 100.0);
      MatrixNd M = new MatrixNd(10,10);
      body.setElasticPos (
         new VectorNd (new double[] {0.5, 1.0, 1.5, 2.0 }));
      Vector3d com = new Vector3d();
      body.computeCenterOfMass (com);
      body.computeMass (M, 10.0, com);
      System.out.println ("M=\n" + M.toString("%12.6f"));


   }

}

