/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.ArrayList;
import java.util.LinkedList;

import javax.media.opengl.GL2;

import maspack.collision.AbstractCollider;
import maspack.collision.ContactInfo;
import maspack.collision.ContactPenetratingPoint;
import maspack.collision.ContactRegion;
import maspack.collision.SurfaceMeshCollider;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.MatrixNdBlock;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.properties.HasProperties;
import maspack.render.GLRenderable;
import maspack.render.GLRenderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.spatialmotion.RigidBodyConstraint;
import maspack.spatialmotion.Wrench;
import maspack.util.DataBuffer;
import maspack.util.IntHolder;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.mechmodels.MechSystem.FrictionInfo;
import artisynth.core.util.TransformableGeometry;

public class RigidBodyContact
   implements RigidBodyConstrainer, GLRenderable, Constrainer {
   AbstractCollider collider = SurfaceMeshCollider.newCollider();

   RigidBody myBodyA = null;
   PolygonalMesh myMeshA = null;
   RigidBody myBodyB = null;
   PolygonalMesh myMeshB = null;
//   int myBodyIdxA = -1;
//   int myBodyIdxB = -1;

   MatrixNdBlock myBilateralBlkA;
   MatrixNdBlock myBilateralBlkB;
   MatrixNdBlock myUnilateralBlkA;
   MatrixNdBlock myUnilateralBlkB;

   protected int myNumUnilaterals = 0;
   protected int myMaxUnilaterals = 100;
   protected RigidBodyConstraint[] myConstraints;
   //private RigidTransform3d myXBA = new RigidTransform3d();
   //private RigidTransform3d myXAW = new RigidTransform3d();

   private ArrayList<RigidBodyConstraint> myUnilaterals; 

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

   public double getFriction() {
      return myFriction;
   }

   public void setFriction (double mu) {
      myFriction = mu;
   }

   public MatrixNdBlock getBilateralBlockA (int numb) {
      if (myBilateralBlkA == null || myBilateralBlkA.colSize() != numb) {
         myBilateralBlkA = new MatrixNdBlock (6, numb);
      }
      return myBilateralBlkA;
   }

   public MatrixNdBlock getBilateralBlockB (int numb) {
      if (myBilateralBlkB == null || myBilateralBlkB.colSize() != numb) {
         myBilateralBlkB = new MatrixNdBlock (6, numb);
      }
      return myBilateralBlkB;
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

   public RigidBodyContact (RigidBody b0, PolygonalMesh m0, RigidBody b1, PolygonalMesh m1) {
      super();

      myBodyA = b0;
      myBodyB = b1;
      myMeshA = m0;
      myMeshB = m1;
      
      myConstraints = new RigidBodyConstraint[myMaxUnilaterals];
      for (int i = 0; i < myMaxUnilaterals; i++) {
         myConstraints[i] = new RigidBodyConstraint();
      }

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

   public synchronized void updateBodyStates (double t, boolean setEngaged) {
   }

//   public void checkRegion (ContactRegion r, int num) {
//      RootModel root = Main.getRootModel();
//      if (root.isCheckEnabled()) {
//         String pfx = "R" + num + " ";
//
//         root.checkWrite (pfx + "normal=" + r.normal);
//         root.checkWrite (pfx + "depth=" + r.depth);
//         int k = 0;
//         for (Point3d pnt : r.points) {
//            root.checkWrite (pfx + "point" + k + "=" + pnt);
//            k++;
//         }
//      }
//   }
//
//   static void checkMesh (PolygonalMesh mesh) {
//      RootModel root = Main.getRootModel();
//      if (root.isCheckEnabled()) {
//         int k = 0;
//         for (Vertex3d vtx : mesh.getVertices()) {
//            root.checkWrite ("vtx" + k + "=" + vtx.pnt);
//            k++;
//         }
//         k = 0;
//         for (Face face : mesh.getFaces()) {
//            root.checkWrite ("nrm" + k + "=" + face.getNormal());
//            k++;
//         }
//      }
//   }

   /* Contact normal should point from mesh1 into mesh0 */
   public double updateValues (double t) {
      // System.out.println("updating values " + t);

      // if(linfo != null && linfo.points1.size() == 6)
      // return;

      // renderlines.clear();

      // super.updateValues(t);
      double maxpen = 0;

      PolygonalMesh mesh0 = myMeshA;
      PolygonalMesh mesh1 = myMeshB;
      Point3d pInBodyA = new Point3d(); // contact point wrt bodyA

//      checkMesh (mesh0);
//      checkMesh (mesh1);

      double nrmlLen = 0;
      if (myCollisionHandler != null) {
         nrmlLen = myCollisionHandler.getContactNormalLen();
         myCollisionHandler.clearLineSegments();
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

         int regionNum = 0;
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

               // ///////////////////////////////////////
               // Point3d np = new Point3d();
               // np.scaledAdd(0.5, region.normal, p);
               // renderlines.add(new Point3d[] {p, np});
               // ///////////////////////////////////////

               RigidBodyConstraint c = myConstraints[numc];

               Wrench w0 = new Wrench();

               if (nrmlLen != 0) {
                  myCollisionHandler.addLineSegment (p, region.normal, nrmlLen);
               }

               w0.f.set (region.normal);
               w0.m.cross (p, region.normal);
               w0.inverseTransform (XAW);
               c.setWrenchA (w0);

               // XBA.mulInverseLeft (XAW, myBodyB.getPose());
               // w0.inverseTransform (XBA);
               // w0.negate();
               // c.setWrenchB (w0);

               w0.f.set (region.normal);
               w0.m.cross (p, region.normal);
               w0.inverseTransform (XBW);
               w0.negate();
               c.setWrenchB (w0);

               maxpen = region.depth;
               c.setDistance (-region.depth); // XXX check this
               // System.out.println("Rigid Constraint distance: " + c.getDistance());
               // c.setBodyIndexA (myBodyIdxA);

               // new code to update rigid body constraint
               pInBodyA.inverseTransform (XAW, p);
               c.setContactPoint (pInBodyA);

               c.setFriction (myFriction);
               //c.setBodyIndexB (myBodyIdxB);
               c.setDerivative (0);

               // pass on compliance and damping parameters from collision handler
               c.setCompliance (myCollisionHandler.getCompliance());
               c.setDamping (myCollisionHandler.getDamping());

               // ideally the multiplier values should be based on previous
               // contact values, if possible               
               c.setMultiplier (0);
               numc++;
            }
         }
      }
      if (myCollisionHandler != null) {
         myCollisionHandler.setLastContactInfo(info);
      }
      myNumUnilaterals = numc;
      return maxpen;
      // for(int i = numc; i < myNumConstraints; i++)
      // {
      // myWrenches[i].set(0, 0, 1, 0, 0, 1);
      // myDerivatives[i] = 0;
      // myDistances[i] = Double.POSITIVE_INFINITY;
      // }
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

   // public void handleSelection (
   // LinkedList<Renderable> list, int[] nameList, int idx)
   // {
   // }

   public synchronized void render (GLRenderer renderer, int flags) {
      GL2 gl = renderer.getGL2().getGL2();
      renderer.setLightingEnabled (false);
      renderer.setColor (0f, 1f, 0f);

      // if (renderPoints.size() > 0)
      // {
      // gl.glPointSize(4);
      // gl.glBegin (GL2.GL_POINTS);
      // for (Point3d p : renderPoints)
      // { gl.glVertex3d (p.x, p.y, p.z);
      // }
      // gl.glEnd();
      // }
      // renderer.setLightingEnabled (true);

      // System.out.println("rendering");

      // if(linfo != null)
      // {
      // Vector3d v = new Vector3d();
      // gl.glBegin (GL2.GL_LINES);
      // int k = 0;
      // for(ContactRegion region:linfo.regions)
      // {

      // for(Point3d p:region.points)
      // {
      // if (myConstraints[k++].getMultiplier() > 0)
      // { renderer.setColor (1f, 0f, 0f);
      // }
      // else
      // { renderer.setColor (0f, 1f, 0f);
      // }
      // gl.glVertex3d(p.x, p.y, p.z);
      // v.add (p, region.normal);
      // gl.glVertex3d (v.x, v.y, v.z);
      // }
      // }
      // gl.glEnd();
      // }

      // gl.glLineWidth(3);
      // renderer.setColor  (0f, 0f, 1f);
      // //// gl.glDisable(GL2.GL_DEPTH_TEST);
      // //
      // for(Point3d[] ps:renderlinesBuf)
      // {
      // gl.glBegin(GL2.GL_LINE_STRIP);
      // for(Point3d p:ps)
      // {
      // gl.glVertex3d(p.x, p.y, p.z);
      // }
      // gl.glEnd();
      // }
      // //
      // //// gl.glEnable(GL2.GL_DEPTH_TEST);

      renderer.setLightingEnabled (true);
   }

   public RenderProps createRenderProps() {
      return defaultRenderProps (null);
   }

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createFaceProps (null);
      props.setFaceStyle (RenderProps.Faces.FRONT_AND_BACK);
      return props;
   }

   public void scaleDistance (double s) {
   }

   public void transformGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject) {
   }

   public boolean hasUnilateralConstraints() {
      return true;
   }

   public int numUnilateralConstraints() {
      return myNumUnilaterals;
   }
   
   public double getUnilateralConstraints (
      ArrayList<RigidBodyConstraint> unilaterals, boolean setEngaged) {

      double maxpen = 0;
      if (setEngaged) {
         maxpen = updateValues (0);
      }
      for (int i = 0; i < myNumUnilaterals; i++) {
         unilaterals.add (myConstraints[i]);
      }
      return maxpen;
   }

   public void updateUnilateralConstraints (
      ArrayList<RigidBodyConstraint> unilaterals, int offset, int num) {

      for (int i = 0; i < num; i++) {
         RigidBodyConstraint u = unilaterals.get (offset + i);
      }
   }

   public int setUnilateralImpulses (VectorNd the, double h, int idx) {
      double[] buf = the.getBuffer();
      for (int i=0; i<myNumUnilaterals; i++) {
         myConstraints[i].setMultiplier (buf[idx++]);
      }
      return idx;
   }
   
   public void zeroImpulses() {
      for (int i=0; i<myNumUnilaterals; i++) {
         myConstraints[i].setMultiplier (0);
      }     
   }

   public int getUnilateralImpulses (VectorNd the, int idx) {
      double[] buf = the.getBuffer();
      for (int i=0; i<myNumUnilaterals; i++) {
         buf[idx++] = myConstraints[i].getMultiplier();
      }
      return idx;
   }


   // added to implement Constrainer

   public void getBilateralSizes (VectorNi sizes) {
      // no bilaterals, so do nothin
   }

   public void getUnilateralSizes (VectorNi sizes) {
      if (myNumUnilaterals > 0) {
         sizes.append (myNumUnilaterals);
      }
   }

   public double updateConstraints (double t, int flags) {
      boolean setEngaged = (flags & MechSystem.UPDATE_CONTACTS) == 0;

      double maxpen = 0;

      updateBodyStates (t, setEngaged);
      if (myUnilaterals == null) {
         myUnilaterals = new ArrayList<RigidBodyConstraint>();
      }
      if (setEngaged) {
         myUnilaterals.clear();
         double dist = getUnilateralConstraints (myUnilaterals, setEngaged);
         if (dist > maxpen) {
            maxpen = dist;
         }            
      }
      else {
         updateUnilateralConstraints (
            myUnilaterals, 0, myUnilaterals.size());
      }
      return maxpen;
   }

   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb, IntHolder changeCnt) {

      // there are no bilateral constraints, so nothing to add
      return numb;
   }

   public int getBilateralInfo (ConstraintInfo[] ginfo, int idx) {

      // there are no bilateral constraints, so no info to update
      return idx;
   }

//   private void setContactSpeed (RigidBodyConstraint c) {
//      double speed = 0;
//      Twist bodyVel = new Twist();
//
//      RigidBody bodyA = getBodyA();
//      if (bodyA != null) {
//         bodyA.getBodyVelocity (bodyVel);
//         speed += c.getWrenchA().dot (bodyVel);
//      }
//      RigidBody bodyB = getBodyB();
//      if (bodyB != null) {
//         bodyB.getBodyVelocity (bodyVel);
//         speed += c.getWrenchB().dot (bodyVel);
//      }
//      c.setContactSpeed (speed);
//   }

   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dn, int numu, IntHolder changeCnt) {

      double[] dbuf = (dn != null ? dn.getBuffer() : null);

      int k = 0;
      int cidx = 0;
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
            if (blkA != null) {
               RigidBodyConnector.setBlockCol (
                  blkA, j, u.getWrenchA(), bodyA, 
                  RigidTransform3d.IDENTITY, null, /*negate=*/false);
            }
            if (blkB != null) {
               RigidBodyConnector.setBlockCol (
                  blkB, j, u.getWrenchB(), bodyB, 
                  RigidTransform3d.IDENTITY, null, /*negate=*/false);
            }
            if (dbuf != null) {
               dbuf[numu+j] = u.getDerivative();
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
            ni.compliance = uc.getCompliance();
            ni.damping = uc.getDamping();
         }
      }
      return idx;
   }

   public int maxFrictionConstraintSets() {
      return myUnilaterals.size();
   }

   public int addFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo, int numf) {

      return RigidBodyConnector.addFrictionConstraints (
         DT, finfo, numf, myUnilaterals, getBodyA(), getBodyB(), null);
   }

   // end implement Constrainer

   public int numBilateralConstraints() {
      return 0;
   }

   public int getBilateralConstraints (ArrayList<RigidBodyConstraint> bilaterals) {
      return 0;
   }

   public int setBilateralImpulses (VectorNd lam, double h, int idx) {
      return idx;
   }

   public int getBilateralImpulses (VectorNd lam, int idx) {
      return idx;
   }

//   public void updateBodyIndices (HashMap<RigidBody,Integer> indexMap) {
//      Integer idxObj = null;
//      if ((idxObj = indexMap.get (myBodyA)) == null) {
//         throw new InternalErrorException ("bodyA (name=" + myBodyA.getName()
//         + ") not found in indexMap");
//      }
//      myBodyIdxA = idxObj.intValue();
//      if ((idxObj = indexMap.get (myBodyB)) == null) {
//         throw new InternalErrorException ("bodyB (name=" + myBodyB.getName()
//         + ") not found in indexMap");
//      }
//      myBodyIdxB = idxObj.intValue();
//   }

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
      int numu = data.zpeek();
      data.zskip (1);
      data.dskip (numu*RigidBodyConstraint.getStateSize());
   }

   public void getAuxState (DataBuffer data) {
      data.zput (myNumUnilaterals);
      for (int i=0; i<myNumUnilaterals; i++) {
         RigidBodyConstraint c = myConstraints[i];
         c.getState (data);
       }
   }

   public void getInitialAuxState (DataBuffer newData, DataBuffer oldData) {

      if (oldData == null) {
         getAuxState (newData);
      }
      else {
         int numu = oldData.zget();
         newData.zput (numu);
         newData.putData (oldData, numu*RigidBodyConstraint.getStateSize(), 0);
      }
   }

   public void setAuxState (DataBuffer data) {
      myNumUnilaterals = data.zget();
      for (int i=0; i<myNumUnilaterals; i++) {
         RigidBodyConstraint c = myConstraints[i];
         c.setState (data);
//         c.setBodyIndexA (myBodyIdxA);
//         c.setBodyIndexB (myBodyIdxB);
         c.setFriction (myFriction);
      }
      if (hasUnilateralConstraints()) {
         if (myUnilaterals == null) {
            myUnilaterals = new ArrayList<RigidBodyConstraint>();
         }
         else {
            myUnilaterals.clear();
         }
         getUnilateralConstraints (myUnilaterals, /*setEngaged=*/false);
      }
   }
   
   public boolean hasActiveContact() {
      return (myNumUnilaterals > 0);
   }
   
   // /**
   // * {@inheritDoc}
   // */
   // @Override
   // public void updateForBodyPositionChange (
   // RigidBody body, RigidTransform3d XPoseOld)
   // {
   // if (body == myBodies.get(0))
   // { // bodyA
   // }
   // else if (body == myBodies.get(1))
   // { // bodyB
   // }
   // else
   // { throw new IllegalArgumentException (
   // "body is not referenced by this connector");
   // }
   // }

}
