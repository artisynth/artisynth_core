/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import artisynth.core.mechmodels.Marker;
import artisynth.core.mechmodels.MeshComponent;
import artisynth.core.mechmodels.Point;
import artisynth.core.modelbase.*; //import artisynth.core.mechmodels.DynamicMechComponent.Activity;
import maspack.geometry.*;
import maspack.matrix.*;
import maspack.spatialmotion.Twist;
import maspack.properties.*;
import maspack.util.*;

import java.io.*;
import java.util.*;

import artisynth.core.util.*;

public class MeshMarker extends Point implements HasCoordinateFrame {

   protected MeshComponent myMeshComp = null;
   protected int myFaceIdx;
   protected Vector2d myCoords;
   protected Vector3d myNormal;
   // if belongs to a MeshCurve, pnt index of the marker along that curve:
   protected int myPntIdx;

   public static PropertyList myProps =
      new PropertyList (MeshMarker.class, Marker.class);

   protected static final double DEFAULT_NORMAL_COMPUTE_RADIUS = 0;
   protected double myNormalComputeRadius = DEFAULT_NORMAL_COMPUTE_RADIUS;
   protected PropertyMode myNormalComputeRadiusMode = PropertyMode.Inherited;

   static {
      myProps.get ("position").setAutoWrite (false);
      myProps.remove ("velocity");
      myProps.remove ("targetPosition");
      myProps.remove ("targetVelocity");
      myProps.remove ("targetActivity");
      myProps.remove ("force");
      myProps.remove ("externalForce");
      myProps.remove ("pointDamping");
      myProps.add (
         "coords", "barycentric coordinates with respect to face", null, "NW");
      myProps.addInheritable (
         "normalComputeRadius:Inherited",
         "max distance for vertices used to compute the marker surface normal",
         DEFAULT_NORMAL_COMPUTE_RADIUS);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public MeshMarker() {
      super();
      myCoords = new Vector2d();
      myPntIdx = -1;
   }

   private void validateMesh (MeshComponent mcomp) {
      MeshBase mesh = mcomp.getMesh();
      if (!(mesh instanceof PolygonalMesh) ||
          !((PolygonalMesh)mesh).isTriangular()) {
         throw new IllegalArgumentException (
            "Mesh component does not containt a triangular mesh");
      }
   }

   public MeshMarker (
      MeshComponent mcomp, int faceIdx, double s1, double s2) {
      this ();
      validateMesh (mcomp);
      myMeshComp = mcomp;
      myFaceIdx = faceIdx;
      myCoords.set (s1, s2);
      updatePosition();
   }

   public MeshMarker (MeshComponent mcomp, Point3d pos) {
      this ();
      validateMesh (mcomp);
      myMeshComp = mcomp;
      projectToMesh (pos);
   }

   /* --- property accessors --- */

   /**
    * Sets the radius around the marker used to collect vertices for estimating
    * its surface normal. The surface normal is computed as a weighted average
    * of the normals of these vertices plus the normal of the marker's face.  A
    * value of 0 means that only the face normal is used. Negative values are
    * set to 0. The weighting is Gaussian, based on the distance of the vertex
    * from the marker, with the radius corresponding to two standard
    * deviations. By default, this value is inherited from an ancestor
    * component, or otherwise set to 0.
    *
    * @param rad normal compute radius
    */
   public void setNormalComputeRadius (double rad) {
      if (rad < 0) {
         rad = 0;
      }
      if (myNormalComputeRadius != rad) {
         myNormalComputeRadius = rad;
         myNormal = null;
      }
      myNormalComputeRadiusMode =
         PropertyUtils.propagateValue (
            this, "normalComputeRadius", rad, myNormalComputeRadiusMode);
   }

   /**
    * Queries the radius around the marker used to collect vertices for
    * estimating its surface normal. See {@link #setNormalComputeRadius} for a
    * description of how this is done.
    *
    * @return normal compute radius
    */
   public double getNormalComputeRadius () {
      return myNormalComputeRadius;
   }

   public PropertyMode getNormalComputeRadiusMode() {
      return myNormalComputeRadiusMode;
   }

   public void setNormalComputeRadiusMode (PropertyMode mode) {
      myNormalComputeRadiusMode =
         PropertyUtils.setModeAndUpdate (
            this, "normalComputeRadius", myNormalComputeRadiusMode, mode);
   }

   public PolygonalMesh getMesh() {
      return (PolygonalMesh)myMeshComp.getMesh();
   }

   public MeshComponent getMeshComp() {
      return myMeshComp;
   }

   public Vector2d getCoords() {
      return new Vector2d (myCoords);
   }

   public void setCoords (Vector2d coords) {
      myCoords.set (coords);
      updatePosition();
   }

   public int getFaceIndex() {
      return myFaceIdx;
   }

   public Face getFace() {
      if (myFaceIdx != -1) {
         // paranoid - idx should not be -1
         return getMesh().getFace(myFaceIdx);
      }
      else {
         return null;
      }
   }

   public Vector3d getNormal() {
      if (myNormal == null) {
         myNormal = getMesh().estimateSurfaceNormal (
            getPosition(), getFace(), myNormalComputeRadius);
      }
      return new Vector3d(myNormal);
   }

   protected void projectToMesh (Point3d pos) {
      Point3d surfacePos = new Point3d();
      Face face = BVFeatureQuery.getNearestFaceToPoint (
         null, myCoords, getMesh(), pos);
      if (face == null) {
         // couldn't compute for some reason
         myFaceIdx = 0;
         myCoords.setZero();
      }
      else {
         myFaceIdx = face.getIndex();
      }
      updatePosition();
   }

   public void updatePosition() {
      PolygonalMesh mesh = getMesh();
      if (mesh != null) {
         if (myFaceIdx >= mesh.numFaces()) {
            setPosition (0, 0, 0);
         }
         else {
            double s1 = myCoords.x;
            double s2 = myCoords.y;
            double s0 = 1 - s1 - s2;
            Face face = mesh.getFace (myFaceIdx);
            Point3d pos = new Point3d();
            HalfEdge he = face.firstHalfEdge();
            pos.scaledAdd (s0, he.getHead().getWorldPoint());
            he = he.getNext();
            pos.scaledAdd (s1, he.getHead().getWorldPoint());
            he = he.getNext();
            pos.scaledAdd (s2, he.getHead().getWorldPoint());
            setPosition (pos);
         }
      }
   }

   public void setWorldPosition (Point3d pos) {
      projectToMesh (pos);
      myNormal = null;
   }

   public void getPose (RigidTransform3d TMW) {
      CompositeComponent gparent = getGrandParent();
      Vector3d zdir = getNormal();
      if (gparent instanceof MeshCurve && myPntIdx != -1) {
         MeshCurve curve = (MeshCurve)gparent;
         curve.updateCurveIfNecessary();
         Vector3d xdir = curve.getTangent (myPntIdx);
         TMW.R.setZXDirections (zdir, xdir);
      }
      else {
         TMW.R.setZDirection (zdir);
      }
      TMW.p.set (getPosition());
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "meshComp", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "coords")) {
         myCoords.scan (rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "faceIdx")) {
         myFaceIdx = rtok.scanInteger();
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "meshComp")) {
         myMeshComp = 
            postscanReference (tokens, MeshComponent.class, ancestor);
         updatePosition();
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.println (
         "meshComp="+ComponentUtils.getWritePathName (ancestor,myMeshComp));
      pw.println ("faceIdx=" + myFaceIdx);
      pw.print ("coords=");
      myCoords.write (pw, fmt, /*withBrackets=*/true);
      pw.println ("");
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      if (!context.contains(myMeshComp)) {
         super.transformGeometry (gtr, context, flags);
         projectToMesh (getPosition());
         myNormal = null;
      }
   }

   // public void applyForces() {
   //    myFrameAttachment.applyForces();
   // }

   public boolean isDuplicatable() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      return true;
   }

   @Override
   public MeshMarker copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      MeshMarker m = (MeshMarker)super.copy (flags, copyMap);

      if (copyMap != null) {
         m.myMeshComp = (MeshComponent)copyMap.get(myMeshComp);
      }
      if (m.myMeshComp == null) {
         m.myMeshComp = myMeshComp;
      }
      m.myFaceIdx = myFaceIdx;
      m.myCoords = new Vector2d (myCoords);
      m.updatePosition();
      m.myPntIdx = -1;
      return m;
   }
}
