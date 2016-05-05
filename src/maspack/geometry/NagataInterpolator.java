/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;

import maspack.matrix.*;
import maspack.util.InternalErrorException;
import maspack.util.CubicSolver;

/**
 * Class that implements quadratic surface interpolation based on the paper
 * "Simple local interpolation of surfaces using normal vectors", by Takashi
 * Nagata.
 */
public class NagataInterpolator {

   Point3d myP1 = new Point3d();
   Point3d myP2 = new Point3d();
   Point3d myP3 = new Point3d();

   Vector3d myC1 = new Vector3d();
   Vector3d myC2 = new Vector3d();
   Vector3d myC3 = new Vector3d();

   Vector3d myD1 = new Vector3d();
   Vector3d myD2 = new Vector3d();
   Vector3d myD3 = new Vector3d();

   Vector3d myG = new Vector3d();
   Vector3d myC = new Vector3d();

   Vector3d myTanEta = new Vector3d();
   Vector3d myTanZeta = new Vector3d();

   Matrix2d myH = new Matrix2d();
   Vector2d myGrad = new Vector2d();
   Vector2d myS = new Vector2d();

   Vector3d myD = new Vector3d();

   Vector3d myTmp = new Vector3d();

   double[] myRoots = new double[3];

   double myDistSqr;
   double[] myEdgeDistSqrs = new double[3];
   double[] myEdgeMins = new double[3];

   private double EPS = 1e-8;
   private static final double sqrtOneHalf = Math.sqrt(2)/2;

   public boolean debug = false;

   public void checkMesh (PolygonalMesh mesh) {
      if (!mesh.isTriangular()) {
         throw new IllegalArgumentException ("Mesh is not triangular");
      }
      if (mesh.getNormals() == null) {
         throw new IllegalArgumentException ("Mesh does not have normals");
      }
   }

   public void setFace (Face face, Vector3d n0, Vector3d n1, Vector3d n2) {

      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he = he0;
      int k = 0;
      do {
         switch (k) {
            case 0: {
               myP1.set (he.head.pnt);
               break;
            }
            case 1: {
               myP2.set (he.head.pnt);
               break;
            }
            case 2: {
               myP3.set (he.head.pnt);
               break;
            }
            default: {
               throw new IllegalArgumentException ("Face not triangular");
            }
         }
         he = he.next;
         k++;
      }
      while (he != he0);

      myD1.sub (myP2, myP1);
      computeC (myC1, myD1, n0, n1);
      myD2.sub (myP3, myP2);
      computeC (myC2, myD2, n1, n2);
      myD3.sub (myP3, myP1);
      computeC (myC3, myD3, n0, n2);
   }

   private void computeC (Vector3d c, Vector3d d, Vector3d n0, Vector3d n1) {
      double dot = n0.dot (n1);
      if (Math.abs(Math.abs(dot)-1) < EPS) {
         c.setZero();
      }
      else {
         double n0d = n0.dot(d);
         double n1d = n1.dot(d);
         c.combine (n0d+dot*n1d, n0, -dot*n0d-n1d, n1);
         c.scale (1/(1-dot*dot));
      }
      //c.setZero();
   }

   public void interpolateVertex (Point3d pnt, double eta, double zeta) {      

      double oneMinusEta = 1 - eta;
      double etaMinusZeta = eta - zeta;

      pnt.scale (oneMinusEta, myP1);
      pnt.scaledAdd (etaMinusZeta, myP2);
      pnt.scaledAdd (zeta, myP3);

      pnt.scaledAdd (-oneMinusEta*etaMinusZeta, myC1);
      pnt.scaledAdd (-etaMinusZeta*zeta, myC2);
      pnt.scaledAdd (-oneMinusEta*zeta, myC3);
   }

   private void computeTangents (double eta, double zeta) {

      double oneMinusEta = 1 - eta;
      double etaMinusZeta = eta - zeta;

      myTanEta.set (myD1);
      myTanEta.scaledAdd (etaMinusZeta-oneMinusEta, myC1);
      myTanEta.scaledAdd (-zeta, myC2);
      myTanEta.scaledAdd (zeta, myC3);

      myTanZeta.set (myD2);
      myTanZeta.scaledAdd (zeta-etaMinusZeta, myC2);
      myTanZeta.scaledAdd (oneMinusEta, myC1);
      myTanZeta.scaledAdd (-oneMinusEta, myC3);
   }      

   private void computeGradAndHessian (Vector2d g, Matrix2d H) {

      g.x = -2*myD.dot(myTanEta);
      g.y = -2*myD.dot(myTanZeta);

      double ddotC1 = myD.dot(myC1);
      double ddotC2 = myD.dot(myC2);
      double ddotC3 = myD.dot(myC3);

      H.m00 = 2*(myTanEta.dot(myTanEta) - 2*ddotC1);
      H.m01 = 2*(myTanEta.dot(myTanZeta) - (ddotC3-ddotC1-ddotC2));
      H.m11 = 2*(myTanZeta.dot(myTanZeta) - 2*ddotC2);
      H.m10 = H.m01;
   }

   static public final int INSIDE = 0;

   static public final int VERTEX_1 = 1;
   static public final int VERTEX_2 = 2;
   static public final int VERTEX_3 = 3;

   static public final int EDGE_1 = 4;
   static public final int EDGE_2 = 5;
   static public final int EDGE_3 = 6;

   int clipCoords (Vector2d s) {

      double eta = s.x;
      double zeta = s.y;

      int code = INSIDE;

      if (eta >= 1) {
         eta = 1;
         if (zeta <= 0) {
            zeta = 0;
            code = VERTEX_2;
         }
         else if (zeta >= 1) {
            zeta = 1;
            code = VERTEX_3;
         }
         else {
            code = EDGE_2;
         }
      }
      else if (zeta <= 0) {
         zeta = 0;
         if (eta <= 0) {
            eta = 0;
            code = VERTEX_1;
         }
         else {
            code = EDGE_1;
         }
      }
      else if (zeta >= eta) {
         double del = (zeta-eta)/2;
         eta += del;
         zeta -= del;
         zeta = eta; // make sure against round-off
         if (eta <= 0) {
            eta = 0;
            zeta = 0;
            code = VERTEX_1;
         }
         else if (zeta >= 1) {
            eta = 1;
            zeta = 1;
            code = VERTEX_3;
         }
         else {
            code = EDGE_3;
         }
      }
      s.x = eta;
      s.y = zeta;
      return code;
   }

   public void interpolateNormal (Vector3d nrm, double eta, double zeta) {      

      computeTangents (eta, zeta);

      nrm.cross (myTanEta, myTanZeta);
      nrm.normalize();
   }

   public void setBoundsForCurve (double[] bounds, Vector2d svec, Vector2d dir) {

      double alpha = dir.x;
      double beta = dir.y;
      double alphaMinusBeta = alpha-beta;

      double ximin = Double.MAX_VALUE;
      double ximax = Double.MIN_VALUE;

      if (alpha > 0) {
         ximax = Math.max ((1-svec.x)/alpha, ximax);
      }
      else if (alpha < 0) {
         ximin = Math.min ((1-svec.x)/alpha, ximin);
      }
      if (beta > 0) {
         ximin = Math.min (-svec.y/beta, ximin);
      }
      else if (beta < 0) {
         ximax = Math.max (-svec.y/beta, ximax);
      }
      if (alphaMinusBeta > 0) {
         ximin = Math.min ((svec.y-svec.x)/alphaMinusBeta, ximin);
      }
      else if (alphaMinusBeta < 0) {
         ximax = Math.max ((svec.y-svec.x)/alphaMinusBeta, ximax);
      }
      bounds[0] = ximin;
      bounds[1] = ximax;
   }

   private static double TOL = 1e-8;

   public void interpolateCurve (
      Point3d pos, double xi, Point3d pos0, Vector2d svec, Vector2d dir) {

      double eta0 = svec.x;
      double zeta0 = svec.y;
      double etaMinusZeta0 = eta0-zeta0;

      double alpha = dir.x;
      double beta =  dir.y;
      double alphaMinusBeta = alpha-beta;

      pos.set (pos0);

      myG.scale (-alpha, myP1);
      myG.scaledAdd (alphaMinusBeta, myP2);
      myG.scaledAdd (beta, myP3);

      myG.scaledAdd (-(1-eta0)*alphaMinusBeta+etaMinusZeta0*alpha, myC1);
      myG.scaledAdd (-etaMinusZeta0*beta-zeta0*alphaMinusBeta, myC2);
      myG.scaledAdd (-(1-eta0)*beta+zeta0*alpha, myC3);

      myC.scale (alpha*(alphaMinusBeta), myC1);
      myC.scaledAdd (-beta*(alphaMinusBeta), myC2);
      myC.scaledAdd (beta*alpha, myC3);

      pos.scaledAdd (xi, myG);
      pos.scaledAdd (xi*xi, myC);
   }

   int findMinimumAlongCurve (
      Vector2d svec, Point3d x0, Vector2d dir, Point3d pos, double tol) {

      double eta0 = svec.x;
      double zeta0 = svec.y;
      double etaMinusZeta0 = eta0-zeta0;

      double alpha = dir.x;
      double beta =  dir.y;
      double alphaMinusBeta = alpha-beta;

      myG.scale (-alpha, myP1);
      myG.scaledAdd (alphaMinusBeta, myP2);
      myG.scaledAdd (beta, myP3);

      myG.scaledAdd (-(1-eta0)*alphaMinusBeta+etaMinusZeta0*alpha, myC1);
      myG.scaledAdd (-etaMinusZeta0*beta-zeta0*alphaMinusBeta, myC2);
      myG.scaledAdd (-(1-eta0)*beta+zeta0*alpha, myC3);

      myC.scale (alpha*(alphaMinusBeta), myC1);
      myC.scaledAdd (-beta*(alphaMinusBeta), myC2);
      myC.scaledAdd (beta*alpha, myC3);

      double ximin = Double.MAX_VALUE;
      double ximax = Double.MIN_VALUE;

      int maxcode = INSIDE;
      int mincode = INSIDE;

      if (alpha > 0) {
         ximax = Math.max ((1-svec.x)/alpha, ximax);
         maxcode = EDGE_2;
      }
      else if (alpha < 0) {
         ximin = Math.min ((1-svec.x)/alpha, ximin);
         mincode = EDGE_2;
      }
      if (beta > 0) {
         ximin = Math.min (-svec.y/beta, ximin);
         mincode = EDGE_1;
      }
      else if (beta < 0) {
         ximax = Math.max (-svec.y/beta, ximax);
         maxcode = EDGE_1;
      }
      if (alphaMinusBeta > 0) {
         ximin = Math.min ((svec.y-svec.x)/alphaMinusBeta, ximin);
         mincode = EDGE_3;
      }
      else if (alphaMinusBeta < 0) {
         ximax = Math.max ((svec.y-svec.x)/alphaMinusBeta, ximax);
         maxcode = EDGE_3;
      }

      double xiAtMin = nearestPointOnCurve (x0, myG, myC, pos, ximin, ximax);

      int code = INSIDE;
      if (xiAtMin == ximin) {
         code = mincode;
      }
      else if (xiAtMin == ximax) {
         code = maxcode;
      }

      svec.x += alpha*xiAtMin;
      svec.y += beta*xiAtMin;

      return code;
   }

   public double distanceToEdge (Point3d nearest, Point3d pos, int edgeNum) {

      Vector2d svec = new Vector2d();      
      findMinimumOnEdge (svec, pos, edgeNum);
      interpolateVertex (nearest, svec.x, svec.y);      
      return nearest.distance (pos);
   }

   public double distanceToCurve (
      Point3d nearest, Vector2d svec, Vector2d dir, Point3d pos, double posTol) {
      
      Point3d x0 = new Point3d();
      interpolateVertex (x0, svec.x, svec.y);
      Vector2d sres = new Vector2d(svec);
      findMinimumAlongCurve (sres, x0, dir, pos, posTol);
      interpolateVertex (nearest, sres.x, sres.y);
      return nearest.distance (pos);
   }

   public double nearestPointOnCurve (
      Point3d x0, Vector3d gv, Vector3d cv, Point3d pos,
      double ximin, double ximax) {

      myTmp.sub (x0, pos);

      double a = 4*cv.dot(cv);
      double b = 6*cv.dot(gv);
      double c = 4*myTmp.dot(cv) + 2*gv.dot(gv);
      double d = 2*myTmp.dot(gv);

      int nr = CubicSolver.getRoots (myRoots, a, b, c, d, ximin, ximax);

      double dsqr;
      double minDsqr = Double.MAX_VALUE;
      double xiAtMin = 0;
         
      for (int i=0; i<nr; i++) {
         double xi = myRoots[i];
         myTmp.scaledAdd (xi, gv, x0);
         myTmp.scaledAdd (xi*xi, cv);
         dsqr = myTmp.distanceSquared (pos);
         if (dsqr < minDsqr) {
            minDsqr = dsqr;
            xiAtMin = xi;
         }
      }

      myTmp.scaledAdd (ximin, gv, x0);
      myTmp.scaledAdd (ximin*ximin, cv);
      dsqr = myTmp.distanceSquared (pos);
      if (dsqr < minDsqr) {
         minDsqr = dsqr;
         xiAtMin = ximin;
      }      

      myTmp.scaledAdd (ximax, gv, x0);
      myTmp.scaledAdd (ximax*ximax, cv);
      dsqr = myTmp.distanceSquared (pos);
      if (dsqr < minDsqr) {
         minDsqr = dsqr;
         xiAtMin = ximax;
      }      
      myDistSqr = minDsqr;
      return xiAtMin;
   }      

   private int setEdgeCoords (Vector2d svec, double xiAtMin, int edgeNum) {
      int code;

      switch (edgeNum) {
         case 0: {
            svec.set (xiAtMin, 0.0); 
            if (xiAtMin == 0) {
               code = VERTEX_1;
            }
            else if (xiAtMin == 1.0) {
               code = VERTEX_2;
            }
            else {
               code = EDGE_1;
            }
            break;
         }
         case 1: {
            svec.set (1.0, xiAtMin);
            if (xiAtMin == 0) {
               code = VERTEX_2;
            }
            else if (xiAtMin == 1.0) {
               code = VERTEX_3;
            }
            else {
               code = EDGE_2;
            }
            break;
         }
         case 2: {
            svec.set (xiAtMin, xiAtMin);
            if (xiAtMin == 0) {
               code = VERTEX_1;
            }
            else if (xiAtMin == 1.0) {
               code = VERTEX_3;
            }
            else {
               code = EDGE_3;
            }
            break;
         }
         default: {
            throw new IllegalArgumentException (
               "Invalid edge number "+edgeNum);
         }
      }               
      return code;
   }

   public int findMinimumOnEdge (Vector2d svec, Point3d pos, int edgeNum) {

      Vector3d cv;
      Point3d x0;

      switch (edgeNum) {
         case 0: {
            cv = myC1;
            myG.sub (myD1, myC1);
            x0 = myP1;
            break;
         }
         case 1: {
            cv = myC2;
            myG.sub (myD2, myC2);
            x0 = myP2;
            break;
         }
         case 2: {
            cv = myC3;
            myG.sub (myD3, myC3);
            x0 = myP1;
            break;
         }
         default: {
            throw new IllegalArgumentException (
               "Invalid edge number "+edgeNum);
         }
      }

      double xiAtMin = nearestPointOnCurve (x0, myG, cv, pos, 0, 1);

      int code = setEdgeCoords (svec, xiAtMin, edgeNum);
      myEdgeMins[edgeNum] = xiAtMin;
      // myDistSqr was set by nearestPointOnCurve
      myEdgeDistSqrs[edgeNum] = myDistSqr;

      return code;
   }


   boolean gradientIsAdmissible (Vector2d grad, int code) {
      // returns true if the gradient is pointing into the
      // admissible (eta,zeta) region. This will always be true
      // if we are not on the boundary; otherwise, we need to
      // be pointing in the right direction depending on the boundary.

      switch (code) {
         case INSIDE: {
            return true;
         }
         case VERTEX_1: {
            return grad.y < 0 && sqrtOneHalf*(grad.x-grad.y) < 0;
         }
         case VERTEX_2: {
            return grad.y < 0 && grad.x > 0;
         }
         case VERTEX_3: {
            return grad.x > 0 && sqrtOneHalf*(grad.x-grad.y) < 0;
         }
         case EDGE_1: {
            return grad.y < 0;
         }
         case EDGE_2: {
            return grad.x > 0;
         }
         case EDGE_3: {
            return sqrtOneHalf*(grad.x-grad.y) < 0;
         }
         default: {
            throw new InternalErrorException ("Unknown boundary code: "+code);
         }
      }
   }

   public int nearestPointOnFace (
      Point3d nearest, Vector3d nrm, Face face, PolygonalMesh mesh,
      Vector2d svec, Point3d pos, double posTol) {

      ArrayList<Vector3d> nrmls = mesh.getNormals();
      if (nrmls == null) {
         throw new IllegalArgumentException ("Mesh does not have normals");
      }
      int[] nidxs = mesh.getNormalIndices();
      int foff = mesh.getFeatureIndexOffsets()[face.idx];

      Vector3d n0 = nrmls.get (nidxs[foff  ]);
      Vector3d n1 = nrmls.get (nidxs[foff+1]);
      Vector3d n2 = nrmls.get (nidxs[foff+2]);

      setFace (face, n0, n1, n2);
      return nearestPoint (nearest, nrm, svec, pos, posTol);
   }

   private class FaceRequest {

      Vector2d svec = new Vector2d();
      Face face;

      FaceRequest (Face face) {
         this.face = face;
      }
   }

   private void maybeAddEdgeFace (
      LinkedList<FaceRequest> requests, HalfEdge he, int edgeNum, Vector2d svec,
      HashSet<Face> faceSet) {

      if (he.opposite != null) {
         Face reqFace = he.opposite.face;
         if (!faceSet.contains (reqFace)) {
            FaceRequest req = new FaceRequest (reqFace);
            double xi = 0;
            switch (edgeNum) {
               case 1: xi = svec.x; break;
               case 2: xi = svec.y; break;
               case 3: xi = svec.x; break;
            }
            switch (reqFace.indexOfEdge (he.opposite)) {
               // edge indices 0, 1, 2 correspond to edge numbers 3, 1, 2
               //case 0: req.svec.set (1-xi, 1-xi); break;
               //case 1: req.svec.set (1-xi, 0.0); break;
               //case 2: req.svec.set (1.0, 1-xi); break;

               case 0: req.svec.set (xi, xi); break;
               case 1: req.svec.set (xi, 0.0); break;
               case 2: req.svec.set (1.0, xi); break;
            }
            requests.offer (req);
            faceSet.add (reqFace);
         }
      }
   }

   private void maybeAddVertexFaces (
      LinkedList<FaceRequest> requests, Vertex3d vtx, Vector2d svec,
      HashSet<Face> faceSet) {

      HalfEdgeNode node;
      for (node=vtx.getIncidentHedges(); node!=null; node=node.next) {
         Face reqFace = node.he.face;
         if (reqFace != null && !faceSet.contains (reqFace)) {
            FaceRequest req = new FaceRequest (reqFace);
            int idx = reqFace.indexOfVertex (node.he.head);
            if (idx == -1) {
               throw new InternalErrorException (
                  "Vertex "+node.he.head.idx+" not found in face "+reqFace.idx);
            }
            switch (idx) {
               case 0: req.svec.set (0.0, 0.0); break;
               case 1: req.svec.set (1.0, 0.0); break;
               case 2: req.svec.set (1.0, 1.0); break;
            }
            requests.offer (req);
            faceSet.add (reqFace);
         }
      }
   }

   private void addAdjoiningFaces (
      LinkedList<FaceRequest> requests, Face face, int code, Vector2d svec,
      HashSet<Face> faceSet) {

      HalfEdge he0 = face.firstHalfEdge();

      switch (code) {
         case EDGE_1: {
            maybeAddEdgeFace (requests, he0.next, 1, svec, faceSet);
            break;
         }
         case EDGE_2: {
            maybeAddEdgeFace (requests, he0.next.next, 2, svec, faceSet);
            break;
         }
         case EDGE_3: {
            maybeAddEdgeFace (requests, he0, 3, svec, faceSet);
            break;
         }
         case VERTEX_1: {
            maybeAddVertexFaces (requests, he0.head, svec, faceSet);
            break;
         }
         case VERTEX_2: {
            maybeAddVertexFaces (requests, he0.next.head, svec, faceSet);
            break;
         }
         case VERTEX_3: {
            maybeAddVertexFaces (requests, he0.next.next.head, svec, faceSet);
            break;
         }
      }
   }

   public void nearestPointOnMesh (
      Point3d nearest, Vector3d nrm, PolygonalMesh mesh,
      Point3d pos, double posTol, BVFeatureQuery query) {

      Vector2d svec = new Vector2d();
      //OBBTree obbTree = mesh.getObbtree();

      Face face = query.nearestFaceToPoint (nearest, svec, mesh, pos);
      //Face face = obbTree.nearestFace (pos, nrm, nearest, svec, ti); 

      System.out.println ("Face=" + face.getIndex() + " svec=" + svec);
      svec.x = svec.x + svec.y; // convert to eta, zeta
      svec.y = svec.y;

      // face = mesh.getFace(0);
      // svec.y = svec.x;
      // svec.x = 0;

      int code = nearestPointOnFace (nearest, nrm, face, mesh, svec, pos, posTol);

      if (code == INSIDE) {
         if (debug) System.out.println ("Done");
         // done!
         return;
      }
      else {
         double minDsqr = nearest.distanceSquared (pos);
         if (debug) {
            System.out.println ("Code=" + code + " minDsqr=" + minDsqr);
         }
         
         Point3d near = new Point3d();
         Vector3d nrml = new Vector3d();
         HashSet<Face> faceSet = new HashSet<Face>();
         faceSet.add (face);
         LinkedList<FaceRequest> requests = new LinkedList<FaceRequest>();
         addAdjoiningFaces (requests, face, code, svec, faceSet);
         while (!requests.isEmpty()) {
            FaceRequest req = requests.poll();

            code = nearestPointOnFace (
               near, nrml, req.face, mesh, req.svec, pos, posTol);
            double dsqr = near.distanceSquared (pos);
            if (debug) {
               System.out.println (
                  "  checked face " + req.face.getIndex() + " svec=" + req.svec);
               System.out.println ("  code=" + code + " dsqr=" + dsqr);
            }
            if (dsqr < minDsqr) {
               minDsqr = dsqr;
               nearest.set (near);
               nrm.set (nrml);
            }
            if (code != INSIDE) {
               addAdjoiningFaces (requests, req.face, code, req.svec, faceSet);
            }
         }
      }
   }         

   private int nearestPoint (
      Point3d nearest, Vector3d nrm, Vector2d svec, Point3d pos, double posTol) {

      int code = clipCoords (svec);

      int maxIterations = 10;

      myEdgeDistSqrs[0] = -1;
      myEdgeDistSqrs[1] = -1;
      myEdgeDistSqrs[2] = -1;

      for (int iteration=0; iteration<maxIterations; iteration++) {

         interpolateVertex (nearest, svec.x, svec.y);
         computeTangents (svec.x, svec.y);
         myD.sub (pos, nearest);
         computeGradAndHessian (myGrad, myH);

         double det = myH.m00*myH.m11 - myH.m01*myH.m10;
         double trace = myH.m00 + myH.m11;

         boolean hessianIsSPD = (trace > 0 && det > 0);
         double gnorm = myGrad.oneNorm();

         if (hessianIsSPD && gnorm < posTol) {
            if (debug) System.out.println ("converged");
            break; // we are done
         }
         else if (gnorm >= posTol && gradientIsAdmissible (myGrad, code)) {
            if (hessianIsSPD) {
               // use Newtons's method
               if (debug) System.out.println ("Newton step");
               double dx = - ( myH.m11*myGrad.x - myH.m01*myGrad.y)/det;
               double dy = - (-myH.m10*myGrad.x + myH.m00*myGrad.y)/det;
               svec.x += dx;
               svec.y += dy;
               code = clipCoords (svec);
            }
            else {
               if (debug) System.out.println ("Gradient line search");
               // just do a line search along the gradient
               myGrad.scale (1/gnorm);
               code = findMinimumAlongCurve (
                  svec, nearest, myGrad, pos, posTol);
            }
         }
         else if (code == INSIDE && gnorm < posTol) {
            if (debug) System.out.println ("Choosing a direction");
            // choose a direction. Head for the farthest vertex,
            // which will be either 1 or 3
            if (svec.x + svec.y > 1) {
               myGrad.set (-svec.x, -svec.y);
            }
            else {
               myGrad.set (1-svec.x, 1-svec.y);
            }           
            code = findMinimumAlongCurve (
               svec, nearest, myGrad, pos, posTol);
         }
         else if (code > VERTEX_3) {

            // then we are on an edge
            int edgeNum = code-EDGE_1;
            if (debug) System.out.println ("On edge "+edgeNum);
            if (myEdgeDistSqrs[edgeNum] == -1) {
               code = findMinimumOnEdge (svec, pos, edgeNum);
            }
            else {
               code = setEdgeCoords (svec, myEdgeMins[edgeNum], edgeNum);
               interpolateVertex (nearest, svec.x, svec.y);
               break;
            }
         }
         else {
            if (debug) System.out.println ("On vertex "+code);
            // we are at a vertex
            int edgeNum1 = -1;
            int edgeNum2 = -1;
            switch (code) {
               case VERTEX_1: edgeNum1 = 2; edgeNum2 = 0; break;
               case VERTEX_2: edgeNum1 = 0; edgeNum2 = 1; break;
               case VERTEX_3: edgeNum1 = 1; edgeNum2 = 2; break;
            }
            if (myEdgeDistSqrs[edgeNum1] == -1) {
               findMinimumOnEdge (svec, pos, edgeNum1);
            }
            if (myEdgeDistSqrs[edgeNum2] == -1) {
               findMinimumOnEdge (svec, pos, edgeNum2);
            }
            if (iteration == 0) {
               // try cutting across the minimum points
               setEdgeCoords (svec, myEdgeMins[edgeNum1], edgeNum1);
               interpolateVertex (nearest, svec.x, svec.y);
               // use myGrad to compute direction vector to other edge
               setEdgeCoords (myGrad, myEdgeMins[edgeNum2], edgeNum2);
               myGrad.sub (svec);
               code = findMinimumAlongCurve (
                  svec, nearest, myGrad, pos, posTol);

            }
            else {
               if (myEdgeDistSqrs[edgeNum1] < myEdgeDistSqrs[edgeNum2]) {
                  code = setEdgeCoords (svec, myEdgeMins[edgeNum1], edgeNum1);
               }
               else {
                  code = setEdgeCoords (svec, myEdgeMins[edgeNum2], edgeNum2);
               }
               interpolateVertex (nearest, svec.x, svec.y);
               break;
            }
         }
      }
      //System.out.println ("return with svec=" + svec);
      myD.sub (pos, nearest);
      interpolateNormal (nrm, svec.x, svec.y);
      return code;
   }
}
