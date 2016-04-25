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
public class ParticleMeshConstraint extends ParticleConstraintBase
   implements ScalableUnits {

   protected class MyParticleInfo extends ParticleInfo {

      double myOff;

      MyParticleInfo (Particle p) {
         super (p);
      }
   }

   MeshInfo myMeshInfo;
   double myMeshRadius;
   private NagataInterpolator myNagataInterpolator;

   public static PropertyList myProps =
      new PropertyList (
         ParticleMeshConstraint.class, ParticleConstraintBase.class);

   static {
      //myProps.add (
      // "damping", "damping for this constraint", 0);      
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
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

   public void setDefaultValues() {
      super.setDefaultValues();
      // setRenderProps (defaultRenderProps (null));
   }

   public ParticleMeshConstraint () {
      myMeshInfo = new MeshInfo();
      myParticleInfo = new ArrayList<ParticleInfo>();     
   }

   public ParticleMeshConstraint (Particle p, PolygonalMesh mesh) {
      this();
      addParticle (p);
      setMesh (mesh, null, null);
   }
   
   public ParticleMeshConstraint (
      Particle p, PolygonalMesh mesh, String fileName) {
      this();
      addParticle (p);
      setMesh (mesh, fileName, null);
   }

   protected MyParticleInfo createParticleInfo (Particle p) {
      return new MyParticleInfo(p);
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

   private static double EPS = 1e-8;

   private double getEdgeNormal (
      Vector3d nrm, Point3d pnt, Face face, int edgeIdx) {

      Vector3d enrm = new Vector3d();
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
      enrm.set (face.getNormal());
      enrm.add (he.opposite.getFace().getNormal());
      enrm.normalize();
      if (mag < EPS*myMeshRadius) {
         nrm.set (enrm);
      }
      else {
         nrm.scale (1/mag);
         if (nrm.dot(enrm) < 0) {
            nrm.negate();
         }
      }
      return nrm.dot (he.head.pnt);
   }

   private double getVertexNormal (
      Vector3d nrm, Point3d pnt, Face face, int vtxIdx) {

      Vector3d vnrm = new Vector3d();
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
      he.head.computeNormal (vnrm);
      if (mag < EPS*myMeshRadius) {
         nrm.set (vnrm);
      }
      else {
         nrm.scale (1/mag);
         if (nrm.dot(vnrm) < 0) {
            nrm.negate();
         }
      }
      return nrm.dot (he.head.pnt);
   }

   private void updateSmoothConstraints (MyParticleInfo pi, boolean setEngaged) {

      Point3d loc = new Point3d();
      Vector3d nrm = new Vector3d();
      Point3d pnt = pi.myPart.getPosition();

      myNagataInterpolator.nearestPointOnMesh (
         loc, nrm, getMesh(), pnt, myMeshRadius*1e-8, 
         new BVFeatureQuery());

      Vector3d del = new Vector3d();
      del.sub (pnt, loc);

      pi.myBlk.set (nrm);
      pi.myDist = del.dot (nrm);
   }

   private void updateLinearConstraints (MyParticleInfo pi, boolean setEngaged) {
      Vector2d coords = new Vector2d();
      Point3d loc = new Point3d();
      Point3d pnt = new Point3d(pi.myPart.getPosition());

      PolygonalMesh mesh = getMesh();

      //System.out.println (" update=" + setEngaged);
      if (true /*setEngaged*/) {

         Face face = BVFeatureQuery.getNearestFaceToPoint (
            loc, coords, mesh, pnt);

         double w1 = coords.x;
         double w2 = coords.y;
         double w0 = 1 - w1 - w2;
         
         Vector3d nrm = new Vector3d();

         if (!mesh.meshToWorldIsIdentity()) {
            // convert to local mesh coordinates
            loc.inverseTransform (mesh.getMeshToWorld());
            pnt.inverseTransform (mesh.getMeshToWorld());
         }

         Vector3d del = new Vector3d();
         del.sub (pnt, loc);
         double dmag = del.norm();

         //System.out.println (" coords=" + w0 + " " + w1 + " " + w2);
         
         if (w0 < EPS) {
            if (w1 < EPS) {
               // base normal on vertex v2
               pi.myOff = getVertexNormal (nrm, pnt, face, 2);
               //System.out.println (" v2 ");
            }
            else if (w2 < EPS) {
               // base normal on vertex v1
               pi.myOff = getVertexNormal (nrm, pnt, face, 1);
               //System.out.println (" v1 ");
            }
            else {
               // base normal on edge v1-v2
               pi.myOff = getEdgeNormal (nrm, pnt, face, 2);
               //System.out.println (" v1-v2");
            }
         }
         else if (w1 < EPS) {
            if (w2 < EPS) {
               // base normal on vertex v0
               pi.myOff = getVertexNormal (nrm, pnt, face, 0);
               //System.out.println (" v0");
            }
            else {
               // base normal on edge v0-v2
               pi.myOff = getEdgeNormal (nrm, pnt, face, 0);
               //System.out.println (" v0-v2");
            }
         }
         else if (w2 < EPS) {
            // base normal on edge v0-v1
            pi.myOff = getEdgeNormal (nrm, pnt, face, 1);
            //System.out.println (" v0-v1");
         }
         else {
            // base normal on face
            nrm.set (face.getNormal());
            pi.myOff = nrm.dot (face.firstHalfEdge().head.pnt);
            //System.out.println (" face "+face);
         }
         if (!mesh.meshToWorldIsIdentity()) {
            // convert back to world coordinates
            nrm.transform (mesh.getMeshToWorld());
            del.transform (mesh.getMeshToWorld());
            pi.myOff += nrm.dot (mesh.getMeshToWorld().p);
         }

         pi.myBlk.set (nrm);
         pi.myDist = del.dot (nrm);
         //System.out.println (" d=" + myDist + " nrm=" + nrm);
      }
      else {
         pi.myDist = pi.myBlk.dot (pi.myPart.getPosition()) - pi.myOff;
      }
   }

   public double updateConstraints (double t, int flags) {

      boolean setEngaged = ((flags & MechSystem.UPDATE_CONTACTS) == 0);
      double maxpen = 0;

      for (int i=0; i<myParticleInfo.size(); i++) {
         MyParticleInfo pi = (MyParticleInfo)myParticleInfo.get(i);

         if (myNagataInterpolator != null) {
            updateSmoothConstraints (pi, setEngaged);
         }
         else {
            updateLinearConstraints (pi, setEngaged);
         }

         if (setEngaged && myUnilateralP) {
            maxpen = updateEngagement (pi, maxpen);
         }
      }
      return maxpen;
   }

   public void updateBounds (Vector3d min, Vector3d max) {
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

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();      
      if (scanAttributeName (rtok, "mesh")) {
         myMeshInfo.scan (rtok);  
         myMeshRadius = RenderableUtils.getRadius (myMeshInfo.myMesh);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      myMeshInfo.write (pw, fmt); 
   }

}
