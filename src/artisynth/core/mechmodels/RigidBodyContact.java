/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.ArrayList;
import java.util.LinkedList;

import maspack.collision.AbstractCollider;
import maspack.collision.ContactInfo;
import maspack.collision.ContactPenetratingPoint;
import maspack.collision.ContactRegion;
import maspack.collision.SurfaceMeshCollider;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.*;
import maspack.properties.HasProperties;
import maspack.render.GLRenderable;
import maspack.render.GLRenderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.spatialmotion.RigidBodyConstraint;
import maspack.spatialmotion.Wrench;
import maspack.util.*;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.mechmodels.MechSystem.FrictionInfo;

public class RigidBodyContact
   implements HasAuxState, GLRenderable, Constrainer {
   AbstractCollider collider = SurfaceMeshCollider.newCollider();

   RigidBody myBodyA = null;
   PolygonalMesh myMeshA = null;
   RigidBody myBodyB = null;
   PolygonalMesh myMeshB = null;

   MatrixNdBlock myUnilateralBlkA;
   MatrixNdBlock myUnilateralBlkB;

   protected int myNumUnilaterals = 0;
   protected int myMaxUnilaterals = 100;

   private boolean useNew = true;
   boolean worldWrench = true;
   boolean checkNew = false;

   private ArrayList<RigidBodyConstraint> myUnilaterals; 
   private ArrayList<ContactConstraint> myUnilateralsX; 

   private RenderProps myRenderProps;
   // back reference to collision handler provided for rendering purposes
   CollisionHandler myCollisionHandler;

   protected double myFriction;

   private double myPenetrationTol = 0;

   public void setPenetrationTol (double tol) {
      myPenetrationTol = tol;
   }

   public double getPenetrationTol() {
      return myPenetrationTol;
   }

   public RigidBody getBodyA() {
      return myBodyA;
   }

   public RigidBody getBodyB() {
      return myBodyB;
   }

   public void setFriction (double mu) {
      myFriction = mu;
   }

   public MatrixNdBlock getUnilateralBlockA (int numu) {
      if (myUnilateralBlkA == null || myUnilateralBlkA.colSize() != numu) {
         myUnilateralBlkA = new MatrixNdBlock (6, numu);
      }
      return myUnilateralBlkA;
   }

   public MatrixNdBlock getUnilateralBlockB (int numu) {
      if (myUnilateralBlkB == null || myUnilateralBlkB.colSize() != numu) {
         myUnilateralBlkB = new MatrixNdBlock (6, numu);
      }
      return myUnilateralBlkB;
   }

   public RigidBodyContact (
      RigidBody b0, PolygonalMesh m0, RigidBody b1, PolygonalMesh m1) {
      super();

      myBodyA = b0;
      myBodyB = b1;
      myMeshA = m0;
      myMeshB = m1;
      
      myUnilaterals = new ArrayList<RigidBodyConstraint>();
      myUnilateralsX = new ArrayList<ContactConstraint>();
      myRenderProps = defaultRenderProps (null);
   }
   
   public RigidBodyContact (RigidBody b0, RigidBody b1) {
      this(b0,b0.getMesh(),b1,b1.getMesh());
   }

   // ArrayList<Point3d[]> renderlines = new ArrayList<Point3d[]>();
   // ArrayList<Point3d[]> renderlinesBuf = new ArrayList<Point3d[]>();

   ContactInfo linfo = null;
   LinkedList<Point3d> renderPoints = new LinkedList<Point3d>();
   Vector3d renderNormal = new Vector3d();

   /* Contact normal should point from mesh1 into mesh0 */
   public double updateValues (double t) {

      myUnilaterals.clear();
      double maxpen = 0;

      PolygonalMesh mesh0 = myMeshA;
      PolygonalMesh mesh1 = myMeshB;
      Point3d pInBodyA = new Point3d(); // contact point wrt bodyA

//      checkMesh (mesh0);
//      checkMesh (mesh1);

      double nrmlLen = 0;
      if (myCollisionHandler != null) {
         nrmlLen = myCollisionHandler.getContactNormalLen();
         myCollisionHandler.clearRenderData();
      }

      // Currently, no correspondence is established between new contacts and
      // previous contacts. If there was, then we could set the multipliers for
      // the new contacts to something based on the old contacts, thereby
      // providing initial "warm start" values for the solver.

      ContactInfo info = collider.getContacts (mesh0, mesh1, true);
      linfo = info;
      int numc = 0;

      if (info != null) {
         RigidTransform3d XAW = myBodyA.getPose();
         RigidTransform3d XBW = myBodyB.getPose();

         for (ContactRegion region : info.regions) {
            //checkRegion (region, regionNum);
            if (region.depth > 0.1) {
               renderPoints.clear();

               for (ContactPenetratingPoint cpp : info.points1) {
                  Point3d pnt = new Point3d (cpp.vertex.pnt);
                  pnt.transform (myBodyB.getPose());
                  renderPoints.add (pnt);
               }
               for (ContactPenetratingPoint cpp : info.points0) {
                  Point3d pnt = new Point3d (cpp.vertex.pnt);
                  pnt.transform (myBodyA.getPose());
                  renderPoints.add (pnt);
               }
            }
            // region.depth = 0;
            for (Point3d p : region.points) {
               if (numc >= myMaxUnilaterals)
                  break;

               //RigidBodyConstraint c = myConstraints[numc];
               RigidBodyConstraint c = new RigidBodyConstraint();

               Wrench w0 = new Wrench();

               if (nrmlLen != 0) {
                  myCollisionHandler.addLineSegment (p, region.normal, nrmlLen);
               }

               w0.f.set (region.normal);
               if (worldWrench) {
                  Vector3d l = new Vector3d();
                  l.sub (p, XAW.p);
                  w0.m.cross (l, region.normal);
               }
               else {
                  w0.m.cross (p, region.normal);
                  w0.inverseTransform (XAW);
               }
               c.setWrenchA (w0);

               w0.f.set (region.normal);
               if (worldWrench) {
                  Vector3d l = new Vector3d();
                  l.sub (p, XBW.p);
                  w0.m.cross (l, region.normal);
               }
               else {
                  w0.m.cross (p, region.normal);
                  w0.inverseTransform (XBW);
               }
               w0.negate();
               c.setWrenchB (w0);

               maxpen = region.depth;
               c.setDistance (-region.depth); // XXX check this

               // new code to update rigid body constraint
               if (worldWrench) {
                  pInBodyA.set (p);
               }
               else {
                  pInBodyA.inverseTransform (XAW, p);

               }
               c.setContactPoint (pInBodyA);

               c.setFriction (myFriction);
               //c.setBodyIndexB (myBodyIdxB);
               c.setDerivative (0);

               // ideally the multiplier values should be based on previous
               // contact values, if possible               
               c.setMultiplier (0);
               myUnilaterals.add (c);
               numc++;
            }
         }
      }
      if (myCollisionHandler != null) {
         myCollisionHandler.setLastContactInfo(info);
      }
      if (!useNew) {
         myNumUnilaterals = numc;
      }
      return maxpen;
   }

   /* Contact normal should point from mesh1 into mesh0 */
   public double updateValuesX (double t) {

      myUnilateralsX.clear();
      double maxpen = 0;

      PolygonalMesh mesh0 = myMeshA;
      PolygonalMesh mesh1 = myMeshB;

      double nrmlLen = 0;
      if (myCollisionHandler != null) {
         nrmlLen = myCollisionHandler.getContactNormalLen();
         myCollisionHandler.clearRenderData();
      }

      // Currently, no correspondence is established between new contacts and
      // previous contacts. If there was, then we could set the multipliers for
      // the new contacts to something based on the old contacts, thereby
      // providing initial "warm start" values for the solver.

      ContactInfo info = collider.getContacts (mesh0, mesh1, true);
      linfo = info;
      int numc = 0;

      if (info != null) {

         for (ContactRegion region : info.regions) {
            //checkRegion (region, regionNum);
            if (region.depth > 0.1) {
               renderPoints.clear();

               for (ContactPenetratingPoint cpp : info.points1) {
                  Point3d pnt = new Point3d (cpp.vertex.pnt);
                  pnt.transform (myBodyB.getPose());
                  renderPoints.add (pnt);
               }
               for (ContactPenetratingPoint cpp : info.points0) {
                  Point3d pnt = new Point3d (cpp.vertex.pnt);
                  pnt.transform (myBodyA.getPose());
                  renderPoints.add (pnt);
               }
            }
            // region.depth = 0;
            for (Point3d p : region.points) {
               if (numc >= myMaxUnilaterals)
                  break;

               ContactConstraint c = new ContactConstraint(myCollisionHandler);
               c.setContactPoint0 (p);
               c.equateContactPoints();
               c.setNormal (region.normal);
               c.assignMasters (myBodyA, myBodyB);

               if (nrmlLen != 0) {
                  myCollisionHandler.addLineSegment (p, region.normal, nrmlLen);
               }

               maxpen = region.depth;
               c.setDistance (-region.depth);
               myUnilateralsX.add (c);
               numc++;
            }
         }
      }
      if (myCollisionHandler != null) {
         myCollisionHandler.setLastContactInfo(info);
      }
      if (useNew) {
         myNumUnilaterals = numc;
      }
      return maxpen;
   }

   public void updateBounds (Point3d pmin, Point3d pmax) {
   }

   public int getRenderHints() {
      return 0;
   }

   public void prerender (RenderList list) {
      // renderlinesBuf.clear();
      // renderlinesBuf.addAll (renderlines);
   }

   public synchronized void render (GLRenderer renderer, int flags) {
      //GL2 gl = renderer.getGL2().getGL2();
      //renderer.setLightingEnabled (false);
      //renderer.setColor (0f, 1f, 0f);
      //renderer.setLightingEnabled (true);
   }

   public RenderProps createRenderProps() {
      return defaultRenderProps (null);
   }

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createFaceProps (null);
      props.setFaceStyle (RenderProps.Faces.FRONT_AND_BACK);
      return props;
   }

   public int setUnilateralImpulses (VectorNd the, double h, int idx) {
      int idx0 = idx;
      setUnilateralImpulsesO (the, h, idx);
      idx = setUnilateralImpulsesX (the, h, idx0);
      return idx;
   }
   
   public int setUnilateralImpulsesX (VectorNd the, double h, int idx) {
      double[] buf = the.getBuffer();
      for (int i=0; i<myUnilateralsX.size(); i++) {
         myUnilateralsX.get(i).setImpulse (buf[idx++]);
      }
      return idx;
   }

   public int setUnilateralImpulsesO (VectorNd the, double h, int idx) {
      double[] buf = the.getBuffer();
      for (int i=0; i<myUnilaterals.size(); i++) {
         myUnilaterals.get(i).setMultiplier (buf[idx++]);
      }
      return idx;
   }
   
   public void zeroImpulses() {
      for (int i=0; i<myUnilaterals.size(); i++) {
         myUnilaterals.get(i).setMultiplier (0);
      }     
      for (int i=0; i<myUnilateralsX.size(); i++) {
         myUnilateralsX.get(i).setImpulse (0);
      }     
   }

   public int getUnilateralImpulses (VectorNd the, int idx) {
      if (useNew) {
         return getUnilateralImpulsesX (the, idx);
      }
      else {
         return getUnilateralImpulsesO (the, idx);
      }
   }

   public int getUnilateralImpulsesX (VectorNd the, int idx) {
      double[] buf = the.getBuffer();
      for (int i=0; i<myUnilateralsX.size(); i++) {
         buf[idx++] = myUnilateralsX.get(i).getImpulse();
      }
      return idx;
   }

   public int getUnilateralImpulsesO (VectorNd the, int idx) {
      double[] buf = the.getBuffer();
      for (int i=0; i<myUnilaterals.size(); i++) {
         buf[idx++] = myUnilaterals.get(i).getMultiplier();
      }
      return idx;
   }

   // added to implement Constrainer

   public void getBilateralSizes (VectorNi sizes) {
      // no bilaterals, so do nothing to do
   }

   public void getUnilateralSizes (VectorNi sizes) {
      if (myNumUnilaterals > 0) {
         sizes.append (myNumUnilaterals);
      }
   }

   public double updateConstraints (double t, int flags) {

      if ((flags & MechSystem.COMPUTE_CONTACTS) != 0) {
         if (useNew) {
            updateValues(t);
            return updateValuesX(t);
         }
         else {
            updateValuesX(t);
            return updateValues(t);
         }
      }
      else {
         // update - nothing to do
         return 0;
      }
   }

   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb) {

      // there are no bilateral constraints, so nothing to add
      return numb;
   }

   public int getBilateralInfo (ConstraintInfo[] ginfo, int idx) {

      // there are no bilateral constraints, so no info to update
      return idx;
   }

   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dn, int numu) {

      SparseBlockMatrix XT = NT.clone();
      VectorNd dx = new VectorNd(dn);
      int numu0 = numu;
      int numx;

      if (useNew) {
         numx = addUnilateralConstraintsO (XT, dx, numu);
         numu = addUnilateralConstraintsX (NT, dn, numu);
      }
      else {
         numx = addUnilateralConstraintsX (XT, dx, numu);
         numu = addUnilateralConstraintsO (NT, dn, numu);
      }
      
      if (checkNew) {
         if (numx != numu) {
            throw new TestException ("numx=" + numx+" numu=" + numu);
         }
         for (int i=numu0; i<numu; i++) {
            if (dx.get(i) != dn.get(i)) {
               throw new TestException ("dn differs at i=" + i);
            }
         }
         checkMatrix ("NT", NT, XT);
      }
      return numu;
   }

   public int addUnilateralConstraintsX (
      SparseBlockMatrix NT, VectorNd dn, int numu) {

      double[] dbuf = (dn != null ? dn.getBuffer() : null);
      int bj = NT.numBlockCols();
      for (int i=0; i<myUnilateralsX.size(); i++) {
         ContactConstraint c = myUnilateralsX.get(i);
         c.addConstraintBlocks (NT, bj++);
         if (dbuf != null) {
            dbuf[numu] = c.getDerivative();
         }
         numu++;
      }
      return numu;
   }

   public int addUnilateralConstraintsO (
      SparseBlockMatrix NT, VectorNd dn, int numu) {

      double[] dbuf = (dn != null ? dn.getBuffer() : null);

      int nc = myUnilaterals.size();
      if (nc > 0) {
         int bj = NT.numBlockCols();
         
         RigidBody bodyA = getBodyA();
         RigidBody bodyB = getBodyB();

         int idxA = RigidBodyConnector.getSolveIdx (bodyA);
         MatrixNdBlock blkA = null;
         if (idxA != -1) {
            blkA = getUnilateralBlockA (nc);
            NT.addBlock (idxA, bj, blkA);
         }
         int idxB = RigidBodyConnector.getSolveIdx (bodyB);
         MatrixNdBlock blkB = null;
         if (idxB != -1) {
            blkB = getUnilateralBlockB (nc);
            NT.addBlock (idxB, bj, blkB);
         }

         for (int j=0; j<nc; j++) {
            RigidBodyConstraint u = myUnilaterals.get (j);
            Wrench wr = new Wrench();
            if (blkA != null) {
               wr.set (u.getWrenchA());
               if (worldWrench) {
                  Vector3d l = new Vector3d();
                  l.sub (u.getContactPoint(), bodyA.getPosition());
                  wr.m.cross (l, wr.f);
               }
               RigidBodyConnector.setBlockCol (
                  blkA, j, wr, bodyA, 
                  RigidTransform3d.IDENTITY, null, /*negate=*/false, worldWrench);
            }
            if (blkB != null) {
               wr.set (u.getWrenchB());
               if (worldWrench) {
                  Vector3d l = new Vector3d();
                  l.sub (u.getContactPoint(), bodyB.getPosition());
                  wr.m.cross (l, wr.f);
               }
               RigidBodyConnector.setBlockCol (
                  blkB, j, wr, bodyB, 
                  RigidTransform3d.IDENTITY, null, /*negate=*/false, worldWrench);
            }
            if (dbuf != null) {
               dbuf[numu+j] = u.getDerivative();
               if (dbuf[numu+j] != 0) {
                  throw new TestException ("zero expected for dn term");
               }
            }
            if (!MechModel.addConstraintForces) {
               RigidBodyConnector.setContactSpeed (
                  u, bodyA, bodyB, null, null);
            }
         }
      }
      return numu + nc;
   }

   public int getUnilateralInfo (ConstraintInfo[] ninfo, int idx) {
      ConstraintInfo[] xinfo = allocCinfo (ninfo.length);
      int idx0 = idx;
      int ixx;

      if (useNew) {
         ixx = getUnilateralInfoO (xinfo, idx0);
         idx = getUnilateralInfoX (ninfo, idx);
      }
      else {
         ixx = getUnilateralInfoX (xinfo, idx0);
         idx = getUnilateralInfoO (ninfo, idx);
      }
      if (checkNew) {
         if (ixx != idx) {
            throw new TestException ("ixx="+ixx+" idx="+idx);
         }
         checkCinfo (ninfo, xinfo, idx0, idx);
      }
      return idx;
   }

   public int getUnilateralInfoX (ConstraintInfo[] ninfo, int idx) {

     for (int i=0; i<myUnilateralsX.size(); i++) {
         ContactConstraint c = myUnilateralsX.get(i);
         c.setSolveIndex (idx);
         ConstraintInfo ni = ninfo[idx++];
         if (c.getDistance() < -myPenetrationTol) {
            ni.dist = (c.getDistance() + myPenetrationTol);
         }
         else {
            ni.dist = 0;
         }
         ni.compliance = myCollisionHandler.getCompliance();
         ni.damping = myCollisionHandler.getDamping();
      }
      return idx;
   }

   public int getUnilateralInfoO (ConstraintInfo[] ninfo, int idx) {
      
      int nc = myUnilaterals.size();
      if (nc > 0) {
         for (int j = 0; j < nc; j++) {
            RigidBodyConstraint uc = myUnilaterals.get (j);
            uc.setSolveIndex (idx);
            ConstraintInfo ni = ninfo[idx++];
            if (uc.getDistance() < -myPenetrationTol) {
               ni.dist = (uc.getDistance() + myPenetrationTol);
            }
            else {
               ni.dist = 0;
            }
            ni.compliance = myCollisionHandler.getCompliance();
            ni.damping = myCollisionHandler.getDamping();
         }
      }
      return idx;
   }

   public int maxFrictionConstraintSets() {
      return myNumUnilaterals;
   }

   private void checkMatrix (
      String msg, SparseBlockMatrix M, SparseBlockMatrix X) {

      MatrixNd XX = new MatrixNd (X);
      MatrixNd MM = new MatrixNd (M);
      double norm = M.frobeniusNorm();
      if (!XX.epsilonEquals (MM, norm*1e-12)) {
         System.out.println ("X=\n" + XX.toString("%12.7f"));
         System.out.println ("M=\n" + MM.toString("%12.7f"));
         throw new TestException (msg + " matrices not equal");
      }
   }
 
   private FrictionInfo[] allocFinfo (int size) {
      FrictionInfo[] finfo = new FrictionInfo[size];
      for (int i=0; i<finfo.length; i++) {
         finfo[i] = new FrictionInfo();
      }    
      return finfo;
   }

   private void checkFinfo (
      FrictionInfo[] finfo1, FrictionInfo[] finfo2, int min, int max) {

      for (int i=min; i<max; i++) {
         FrictionInfo f1 = finfo1[i];
         FrictionInfo f2 = finfo2[i];
         if (f1.mu != f2.mu) {
            throw new TestException (
               "friction mu unequal: "+f1.mu+", expected "+f2.mu+" i=" + i);
         }
         else if (f1.contactIdx != f2.contactIdx) {
            throw new TestException (
               "friction contactIdx unequal: "+f1.contactIdx+
               ", expected "+f2.contactIdx);
         }
         else if (f1.flags != f2.flags) {
            throw new TestException (
               "friction flags unequal: " + f1.flags +
               ", expected "+f2.flags);
         }
      }
   }

   private ConstraintInfo[] allocCinfo (int size) {
      ConstraintInfo[] cinfo = new ConstraintInfo[size];
      for (int i=0; i<cinfo.length; i++) {
         cinfo[i] = new ConstraintInfo();
      }    
      return cinfo;
   }

   private void checkCinfo (
      ConstraintInfo[] cinfo1, ConstraintInfo[] cinfo2, int min, int max) {

      for (int i=min; i<max; i++) {
         ConstraintInfo c1 = cinfo1[i];
         ConstraintInfo c2 = cinfo2[i];
         if (c1.dist != c2.dist) {
            throw new TestException (
               "cinfo dist unequal: "+c1.dist+", expected "+c2.dist+" i=" + i);
         }
         else if (c1.compliance != c2.compliance) {
            throw new TestException (
               "cinfo compliance unequal: "+c1.compliance+
               ", expected "+c2.compliance);
         }
         else if (c1.damping != c2.damping) {
            throw new TestException (
               "cinfo damping unequal: " + c1.damping +
               ", expected "+c2.damping);
         }
      }
   }

   public int addFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo, int numf) {

      SparseBlockMatrix XT = DT.clone();
      FrictionInfo[] xinfo = allocFinfo (finfo.length);
      int numf0 = numf;
      int numfx;

      if (useNew) {
         numfx = addFrictionConstraintsO (XT, xinfo, numf);
         numf = addFrictionConstraintsX (DT, finfo, numf);
      }
      else {
         numfx = addFrictionConstraintsX (XT, xinfo, numf);
         numf = addFrictionConstraintsO (DT, finfo, numf);
      }

      if (checkNew) {
         if (numfx != numf) {
            throw new TestException ("numf="+numf+" numfx="+numfx);
         }
         checkMatrix ("DT", DT, XT);
         checkFinfo (finfo, xinfo, numf0, numf);
      }
      return numf;
   }

   public int addFrictionConstraintsX (
      SparseBlockMatrix DT, FrictionInfo[] finfo, int numf) {

      for (int i=0; i<myUnilateralsX.size(); i++) {
         ContactConstraint c = myUnilateralsX.get(i);
         if (Math.abs(c.getImpulse())*myFriction < 1e-4) {
            continue;
         }
         c.add2DFrictionConstraints (DT, finfo, myFriction, numf++);
      }
      return numf;
   }

   public int addFrictionConstraintsO (
      SparseBlockMatrix DT, FrictionInfo[] finfo, int numf) {

      numf = RigidBodyConnector.addFrictionConstraints (
         DT, finfo, numf, myUnilaterals, getBodyA(), getBodyB(), null, 
         worldWrench);
      return numf;
   }

   // end implement Constrainer

   public int setBilateralImpulses (VectorNd lam, double h, int idx) {
      return idx;
   }

   public int getBilateralImpulses (VectorNd lam, int idx) {
      return idx;
   }

   public boolean equals (Object obj) {
      if (obj instanceof RigidBodyContact) {
         RigidBodyContact other = (RigidBodyContact)obj;
         return ((myBodyA == other.myBodyA && myBodyB == other.myBodyB) ||
                 (myBodyB == other.myBodyA && myBodyA == other.myBodyB));
      }
      else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return myBodyA.hashCode() / 2 + myBodyB.hashCode() / 2;
   }

   public void advanceAuxState (double t0, double t1) {
   }

   /** 
    * {@inheritDoc}
    */
   public void skipAuxState (DataBuffer data) {
      int numu = data.zget();
      if (useNew) {
         for (int i=0; i<numu; i++) {
            ContactConstraint.skipState (data);
         }
      }
      else {
         data.dskip (numu*RigidBodyConstraint.getStateSize());
      }
   }

   public void getAuxState (DataBuffer data) {
      data.zput (myNumUnilaterals);
      if (useNew) {
         for (int i=0; i<myNumUnilaterals; i++) {
            myUnilateralsX.get(i).getState (data);
         }
      }
      else {
         for (int i=0; i<myNumUnilaterals; i++) {
            myUnilaterals.get(i).getState (data);
         }
      }
   }

   public void getInitialAuxState (DataBuffer newData, DataBuffer oldData) {

      if (oldData == null) {
         getAuxState (newData);
      }
      else {
         int numu = oldData.zget();
         newData.zput (numu);
         if (useNew) {
            for (int i=0; i<numu; i++) {
               ContactConstraint.putState (newData, oldData);
            }
         }
         else {
            newData.putData (oldData, numu*RigidBodyConstraint.getStateSize(), 0);
         }
      }
   }

   public void setAuxState (DataBuffer data) {
      myNumUnilaterals = data.zget();
      if (useNew) {
         myUnilateralsX.clear();
         for (int i=0; i<myNumUnilaterals; i++) {
            ContactConstraint c = new ContactConstraint(myCollisionHandler);
            c.setState (data, myBodyA, myBodyB);
            myUnilateralsX.add (c);
         }
      }
      else {
         myUnilaterals.clear();
         for (int i=0; i<myNumUnilaterals; i++) {
            RigidBodyConstraint c = new RigidBodyConstraint();
            c.setState (data);
            c.setFriction (myFriction);
            myUnilaterals.add (c);
         }
      }
   }

   public ArrayList<ContactConstraint> getUnilateralContacts() {
      return myUnilateralsX;
   }

   public boolean hasActiveContact() {
      return (myNumUnilaterals > 0);
   }

}
