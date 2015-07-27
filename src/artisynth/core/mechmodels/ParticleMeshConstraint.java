/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.*;
import java.io.*;

import maspack.matrix.*;
import maspack.properties.*;
import maspack.geometry.*;
import maspack.util.*;
import maspack.spatialmotion.*;
import maspack.geometry.*;
import maspack.render.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.util.*;

/** 
 * NOTE: This class is still under construction. scaleDistance(),
 * transformGeometry(), and scan() and write() are unlikely to work properly.
 */
public class ParticleMeshConstraint extends ConstrainerBase
   implements ScalableUnits, TransformableGeometry {

   Matrix3x1Block myBlk;
   double myOff;
   MeshInfo myMeshInfo;
   double myMeshRadius;
   Particle myParticle;
   double myLam;
   double myDist;
   boolean myUnilateralP = false;
   boolean myEngagedP = false;
   double myBreakSpeed = Double.NEGATIVE_INFINITY;
   double myPenetrationTol = 0;
   private double myDamping = 0;
   private double myCompliance = 0;

   private NagataInterpolator myNagataInterpolator;

   public static PropertyList myProps =
      new PropertyList (ParticleMeshConstraint.class, ConstrainerBase.class);

   static {
      // myProps.add ("renderProps", "render properties", defaultRenderProps(null));
      myProps.add (
         "unilateral isUnilateral *", "unilateral constraint flag", false);
      myProps.add (
         "compliance", "compliance for this constraint", 0);
      myProps.add (
         "damping", "damping for this constraint", 0);      
   }

   public boolean getNagataInterpolation () {
      return myNagataInterpolator != null;
   }

   public void setNagataInterpolation (NagataInterpolator interp) {
      if (interp != null) {
         if (getMesh() != null) {
            interp.checkMesh (getMesh());
         }
      }            
      myNagataInterpolator = interp;
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void setDefaultValues() {
      super.setDefaultValues();
      // setRenderProps (defaultRenderProps (null));
   }

   public double getDamping() {
      return myDamping;
   }

   public void setDamping (double d) {
      myDamping = d;
   }

   public double getCompliance() {
      return myCompliance;
   }

   public void setCompliance (double c) {
      myCompliance = c;
   }

   public ParticleMeshConstraint () {
      myBlk = new Matrix3x1Block ();
      myMeshInfo = new MeshInfo();
   }

   public ParticleMeshConstraint (Particle p, PolygonalMesh mesh) {
      this();
      myParticle = p;
      setMesh (mesh, null, null);
   }
   
   public ParticleMeshConstraint (
      Particle p, PolygonalMesh mesh, String fileName) {
      this();
      myParticle = p;
      setMesh (mesh, fileName, null);
   }

   public double getPenetrationTol () {
      return myPenetrationTol;
   }

   public void setPenetrationTol (double tol) {
      myPenetrationTol = tol;
   }

   public boolean isUnilateral() {
      return myUnilateralP;
   }

   public void setUnilateral (boolean unilateral) {
      myUnilateralP = unilateral;
      notifyParentOfChange (StructureChangeEvent.defaultEvent);
   }

   public void setMesh (
      PolygonalMesh mesh, String fileName, AffineTransform3dBase X) {

      if (mesh != null && myNagataInterpolator != null) {
         myNagataInterpolator.checkMesh (mesh);
      }
      myMeshInfo.set (mesh, fileName, X);
      myMeshRadius = RenderableUtils.getRadius (mesh);
   }     

   public PolygonalMesh getMesh() {
      return (PolygonalMesh)myMeshInfo.myMesh;      
   }

   public Particle getParticle() {
      return myParticle;
   }

   public void getBilateralSizes (VectorNi sizes) {
      if (!myUnilateralP) {
         if (myParticle.getSolveIndex() != -1) {
            sizes.append (1);
         }
      }
   }

   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb) {

      if (!myUnilateralP) {
         int idx = myParticle.getSolveIndex();
         int bj = GT.numBlockCols();
         if (idx != -1) {
            GT.addBlock (idx, bj, myBlk);
            if (dg != null) {
               dg.set (numb, 0);
            }
            numb++;
         }
      }
      return numb;
   }

   public int getBilateralInfo (ConstraintInfo[] ginfo, int idx) {
      if (myUnilateralP || myParticle.getSolveIndex() == -1) {
         return idx;
      }
      ConstraintInfo gi = ginfo[idx++];
      gi.dist = myDist;
      gi.compliance = myCompliance;
      gi.damping = myDamping;
      return idx;
   }

   public int setBilateralImpulses (VectorNd lam, double h, int idx) {
      if (!myUnilateralP) {
         if (myParticle.getSolveIndex() != -1) {
            myLam = lam.get(idx++);
         }
      }
      return idx;
   }

   public int getBilateralImpulses (VectorNd lam, int idx) {
      if (!myUnilateralP) {
         if (myParticle.getSolveIndex() != -1) {
            lam.set (idx++, myLam);
         }
      }
      return idx;
   }
   
   public void zeroImpulses() {
      myLam = 0;
   }

   public void getUnilateralSizes (VectorNi sizes) {
      if (myUnilateralP) {
         if (myEngagedP && myParticle.getSolveIndex() != -1) {
            sizes.append (1);
         }
      }
   }

   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dn, int numu) {

      if (myUnilateralP) {
         int idx = myParticle.getSolveIndex();
         int bj = NT.numBlockCols();
         if (myEngagedP && idx != -1) {
            NT.addBlock (idx, bj, myBlk);
            if (dn != null) {
               dn.set (numu, 0);
            }
            numu++;
         }
      }
      return numu;
   }

   public int getUnilateralInfo (ConstraintInfo[] ninfo, int idx) {
      if (myUnilateralP) {
         if (myEngagedP && myParticle.getSolveIndex() != -1) {
            ConstraintInfo ni = ninfo[idx++];
            ni.dist = Math.min (0, myDist+myPenetrationTol);
            ni.compliance = myCompliance;
            ni.damping = myDamping;
         }
      }
      return idx;
   }

   public int getUnilateralImpulses (VectorNd the, int idx) {
      if (myUnilateralP) {
         if (myEngagedP && myParticle.getSolveIndex() != -1) {
            the.set (idx++, myLam);
         }
      }
      return idx;
   }

   public int setUnilateralImpulses (VectorNd the, double h, int idx) {
      if (myUnilateralP) {
         if (myEngagedP && myParticle.getSolveIndex() != -1) {
            myLam = the.get(idx++);
         }
      }
      return idx;
   }

   private static double EPS = 1e-8;

   private double getEdgeNormal (
      Vector3d nrm, Point3d pnt, Face face, int edgeIdx) {

      HalfEdge he = face.firstHalfEdge();
      nrm.set (face.getNormal());
      if (edgeIdx == 1) {
         he = he.getNext();
      }
      else if (edgeIdx == 2) {
         he = he.getNext();
         he = he.getNext();
      }
      nrm.sub (pnt, he.head.pnt);
      Vector3d u = new Vector3d();
      he.computeEdgeUnitVec (u);
      //Vector3d u = he.getU();
      nrm.scaledAdd (-u.dot(nrm), u, nrm);
      double mag = nrm.norm();
      if (mag < EPS*myMeshRadius) {
         nrm.set (face.getNormal());
         nrm.add (he.opposite.getFace().getNormal());
         nrm.normalize();
      }
      else {
         nrm.scale (1/mag);
      }
      return nrm.dot (he.head.pnt);
   }

   private double getVertexNormal (
      Vector3d nrm, Point3d pnt, Face face, int vtxIdx) {

      HalfEdge he = face.firstHalfEdge();
      if (vtxIdx == 1) {
         he = he.getNext();
      }
      else if (vtxIdx == 2) {
         he = he.getNext();
         he = he.getNext();
      }
      nrm.sub (pnt, he.head.pnt);
      double mag = nrm.norm();
      if (mag < EPS*myMeshRadius) {
         he.head.computeNormal (nrm);
      }
      else {
         nrm.scale (1/mag);
      }
      return nrm.dot (he.head.pnt);
   }

   private void updateSmoothConstraints (boolean setEngaged) {

      Point3d loc = new Point3d();
      Vector3d nrm = new Vector3d();
      Point3d pnt = myParticle.getPosition();

      myNagataInterpolator.nearestPointOnMesh (
         loc, nrm, getMesh(), pnt, myMeshRadius*1e-8, 
         new BVFeatureQuery());

      Vector3d del = new Vector3d();
      del.sub (pnt, loc);

      myBlk.set (nrm);
      myDist = del.dot (nrm);
   }

   private void updateLinearConstraints (boolean setEngaged) {
      Vector2d coords = new Vector2d();
      Point3d loc = new Point3d();
      Point3d pnt = myParticle.getPosition();

      Face face = BVFeatureQuery.getNearestFaceToPoint (
         loc, coords, getMesh(), pnt);

      double w1 = coords.x;
      double w2 = coords.y;
      double w0 = 1 - w1 - w2;

      Vector3d nrm = new Vector3d();

      //System.out.println (" update=" + setEngaged);
      if (true /*setEngaged*/) {

         Vector3d del = new Vector3d();
         del.sub (pnt, loc);
         double dmag = del.norm();

         //System.out.println (" coords=" + w0 + " " + w1 + " " + w2);
         
         if (w0 < EPS) {
            if (w1 < EPS) {
               // base normal on vertex v2
               myOff = getVertexNormal (nrm, pnt, face, 2);
               //System.out.println (" v2 ");
            }
            else if (w2 < EPS) {
               // base normal on vertex v1
               myOff = getVertexNormal (nrm, pnt, face, 1);
               
               //System.out.println (" v1 ");
            }
            else {
               // base normal on edge v1-v2
               myOff = getEdgeNormal (nrm, pnt, face, 2);
               //System.out.println (" v1-v2");
            }
         }
         else if (w1 < EPS) {
            if (w2 < EPS) {
               // base normal on vertex v0
               myOff = getVertexNormal (nrm, pnt, face, 0);
               //System.out.println (" v0");
            }
            else {
               // base normal on edge v0-v2
               myOff = getEdgeNormal (nrm, pnt, face, 0);
               //System.out.println (" v0-v2");
            }
         }
         else if (w2 < EPS) {
            // base normal on edge v0-v1
            myOff = getEdgeNormal (nrm, pnt, face, 1);
            //System.out.println (" v0-v1");
         }
         else {
            // base normal on face
            nrm.set (face.getNormal());
            myOff = nrm.dot (face.firstHalfEdge().head.pnt);
            //System.out.println (" face "+face);
         }
         myBlk.set (nrm);
         myDist = del.dot (nrm);
         //System.out.println (" d=" + myDist + " nrm=" + nrm);
      }
      else {
         myDist = myBlk.dot (myParticle.getPosition()) - myOff;
      }
   }

   public double updateConstraints (double t, int flags) {

      boolean setEngaged;
      double maxpen = 0;

      if ((flags & MechSystem.UPDATE_CONTACTS) != 0) {   
         setEngaged = false;
      }
      else {
         setEngaged = true;
      }

      if (myNagataInterpolator != null) {
         updateSmoothConstraints (setEngaged);
      }
      else {
         updateLinearConstraints (setEngaged);
      }

      if (setEngaged && myUnilateralP) {
         boolean oldEngaged = myEngagedP;
         if (myDist < 0) {
            myEngagedP = true;
            maxpen = -myDist;
         }
         if (myEngagedP && myDist >= 0) {
            if (myBlk.dot(myParticle.getVelocity()) > myBreakSpeed)  {
               myEngagedP = false;
               myLam = 0;
            }
         }
      }
      return maxpen;
   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      refs.add (myParticle);
   }

//   @Override
//   public void connectToHierarchy () {
//      super.connectToHierarchy ();
//      myParticle.addBackReference (this);
//   }
//
//   @Override
//   public void disconnectFromHierarchy() {
//      super.disconnectFromHierarchy();
//      myParticle.removeBackReference (this);
//   }

   public void updateBounds (Point3d min, Point3d max) {
      PolygonalMesh mesh = getMesh();
      if (mesh != null) {
         mesh.updateBounds (min, max);
      }      
   }

   // public void prerender (RenderList list) {
   // }

   public void render (Renderer renderer, int flags) {
      //myMeshInfo.render (renderer, myRenderProps, isSelected());
   }
   
   public void scaleMass (double s) {
   }

   public void scaleDistance (double s) {
      PolygonalMesh mesh = getMesh();
      if (mesh != null) {
         mesh.scale (s);
      }
   }

   public void transformGeometry (AffineTransform3dBase X) {
      transformGeometry (X, this, 0);
   }

   public void transformGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {

      PolygonalMesh mesh = getMesh();
      mesh.transform (X);
   }

    /** 
    * {@inheritDoc}
    */
   public void skipAuxState (DataBuffer data) {
      if (myUnilateralP) {
         data.zskip (1); // state stored is "myEngagedP"
      }
   }

   public void getAuxState (DataBuffer data) {
      if (myUnilateralP) {
         data.zput (myEngagedP ? 1 : 0);
      }
   }

   public void getInitialAuxState (
      DataBuffer newData, DataBuffer oldData) {

      if (oldData == null) {
         getAuxState (newData);
      }
      else {
         if (myUnilateralP) {
            newData.putData (oldData, 0, 1);
         }
      }
   }
   
   public void setAuxState (DataBuffer data) {
      if (myUnilateralP) {
         myEngagedP = (data.zget() != 0);
      }      
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();      
      if (scanAndStoreReference (rtok, "particle", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "mesh")) {
         myMeshInfo.scan (rtok);  
         myMeshRadius = RenderableUtils.getRadius (myMeshInfo.myMesh);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "particle")) {
         myParticle = postscanReference (
            tokens, Particle.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }
   
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt);
      pw.println ("particle=" +
                  ComponentUtils.getWritePathName(ancestor,myParticle));
      myMeshInfo.write (pw, fmt); 
   }

}
