/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import maspack.geometry.LineSegment;
import maspack.geometry.OBB;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.*;
import maspack.properties.PropertyList;
import maspack.render.Renderer;
import maspack.render.Renderer.PointStyle;
import maspack.render.Renderer.LineStyle;
import maspack.render.PointRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.RenderableUtils;
import maspack.render.Renderable;
import maspack.util.DataBuffer;
import maspack.util.IndentingPrintWriter;
import maspack.util.ListRemove;
import maspack.util.IntHolder;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.materials.AxialMaterial;
import artisynth.core.materials.LinearAxialMaterial;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

/**
 * Multi-segment point-based spring that supports wrapping of selected
 * segments.
 */
public class MultiPointSpring extends PointSpringBase
   implements ScalableUnits, TransformableGeometry,
              CopyableComponent, RequiresPrePostAdvance,
              HasSlaveObjects, HasAuxState {

   protected ArrayList<Point3d[]> myWrapPaths; // used only for scanning
   protected ArrayList<Segment> mySegments;
   protected ArrayList<Wrappable> myWrappables;
   protected boolean mySubSegsValidP = false;
   protected boolean mySegsValidP = true;
   protected int myNumBlks; // set to numPoints()
   protected int[] mySolveBlkNums;

   public double mySor = 1.0;
   public double myDnrmGain = 0.0;

   // Composite component structure for storing wrap points
   //protected ComponentListImpl<ModelComponent> myComponents;
   //private NavpanelDisplay myDisplayMode = NavpanelDisplay.NORMAL;
   //protected PointList<Point> myWrapPoints;

   protected RenderProps myABRenderProps;
   protected RenderObject myRenderObj; // used to render the strands
   protected boolean myRenderObjValidP = false;

   protected static double DEFAULT_WRAP_STIFFNESS = 1;
   protected static double DEFAULT_WRAP_DAMPING = -1;
   protected static double DEFAULT_CONTACT_STIFFNESS = 10;
   protected static double DEFAULT_CONTACT_DAMPING = 0;
   protected static int DEFAULT_NUM_WRAP_POINTS = 0;
   protected static double DEFAULT_LENGTH_CONV_TOL = 1e-4;
   protected static boolean DEFAULT_DRAW_KNOTS = false;
   protected static boolean DEFAULT_DRAW_AB_POINTS = false;
   protected static int DEFAULT_MAX_WRAP_ITERATIONS = 100;
   protected static int DEFAULT_MAX_WRAP_DISPLACEMENT = -1;

   protected double myWrapStiffness = DEFAULT_WRAP_STIFFNESS;
   protected double myWrapDamping = DEFAULT_WRAP_DAMPING;
   protected double myContactStiffness = DEFAULT_CONTACT_STIFFNESS;
   protected double myContactDamping = DEFAULT_CONTACT_DAMPING;
   protected static boolean DEFAULT_CONTACT_RESCALING = true;
   protected boolean myContactRescaling = DEFAULT_CONTACT_RESCALING;
   protected double myWrapH = 1;
   protected double myLengthConvTol = DEFAULT_LENGTH_CONV_TOL;
   protected boolean myDrawKnotsP = DEFAULT_DRAW_KNOTS;
   protected boolean myDrawABPointsP = DEFAULT_DRAW_AB_POINTS;

   protected static int DEFAULT_DEBUG_LEVEL = 0;
   protected int myDebugLevel = DEFAULT_DEBUG_LEVEL;
   
   protected int myMaxWrapIterations = DEFAULT_MAX_WRAP_ITERATIONS;
   protected double myMaxWrapDisplacement = DEFAULT_MAX_WRAP_DISPLACEMENT;

   public double getSor() {
      return mySor;
   }

   public void setSor (double sor) {
      mySor = sor;
   }

   public double getDnrmGain() {
      return myDnrmGain;
   }

   public void setDnrmGain (double dnrmGain) {
      myDnrmGain = dnrmGain;
   }

   /**
    * Stores information for a single knot point in a wrappable segment.
    */
   public class WrapKnot {
      public Point3d myPos;         // knot position 
      public Point3d myLocPos;      // local position wrt wrappble if in contact 
      public Vector3d myForce;      // first-order force on the knot
 
      // attributes used if the knot is is contact with a wrappable:
      Vector3d myNrml;              // contact normal
      Matrix3d myDnrm;              // derivative of normal wrt knot position
      double myDist;                // distance to surface
      double myPrevDist;            // previous distance to surface
      int myWrappableIdx;           // index of contacting wrappable
      int myPrevWrappableIdx;       // index of previous contacting wrappable
      
      // attributes used to store knot's components in the block triadiagonal
      // stiffness/force system
      Matrix3d myBmat;
      Matrix3d myBinv;
      Matrix3d myCinv;
      Vector3d myDvec;
      Vector3d myVtmp;
      
      float[] myRenderPos;          // knot position used for rendering
      
      public void setWrappableIdx (int idx) {
         myPrevWrappableIdx = myWrappableIdx;
         myWrappableIdx = idx;
      }
      
      public Wrappable getWrappable() {
         if (myWrappableIdx < 0) {
            return null;
         }
         else {
            return myWrappables.get(myWrappableIdx);
         }
      }

      public Wrappable getPrevWrappable() {
         if (myPrevWrappableIdx < 0) {
            return null;
         }
         else {
            return myWrappables.get(myPrevWrappableIdx);
         }
      }

      WrapKnot () {
         myPos = new Point3d();
         myLocPos = new Point3d();
         myForce = new Vector3d();
         myNrml = new Vector3d();
         myDnrm = new Matrix3d();
         myBmat = new Matrix3d();
         myBinv = new Matrix3d();
         myCinv = new Matrix3d();
         myDvec = new Vector3d();
         myVtmp = new Vector3d();
         myRenderPos = new float[3];
         myWrappableIdx = -1;
         myPrevWrappableIdx = -1;
         myDist = Wrappable.OUTSIDE;
         myPrevDist = Wrappable.OUTSIDE;
      }

      float[] updateRenderPos() {
         myRenderPos[0] = (float)myPos.x;
         myRenderPos[1] = (float)myPos.y;
         myRenderPos[2] = (float)myPos.z;
         return myRenderPos;
      }
   }

   /**
    * Stores information for an individual segment of this spring.  This
    * includes both segments between fixed via-points, and subsegments within
    * a wrappable segments.
    */
   public class Segment {

      public Point myPntB; // end-point B
      public Point myPntA; // end-point A
      Vector3d mydFdxB;    // derivative of tension force F wrt point B
      Vector3d mydFdxA;    // derivative of tension force F wrt point A
      
      // passiveP, if true, means segment does not contribute to overall
      // "length" of its MultiPointSpring
      boolean myPassiveP = false; 

      Vector3d myUvec;     // unit vector in direction of segment
      Matrix3d myP;        // (I - uvec uvec^T)
      double myLength;     // length of the segment

      Segment() {
         myP = new Matrix3d();
         myUvec = new Vector3d();
         mydFdxB = new Vector3d();
         mydFdxA = new Vector3d();
      }

      /**
       * Applies the tension of this segment to the forces of its
       * end-points. Assumes that uvec is up to date.
       */
      void applyForce (double F) {
         Vector3d f = new Vector3d();
         f.scale (F, myUvec);
         myPntB.addForce (f);
         myPntA.subForce (f);
      }

      /**
       * Updates the unit vector and length of this segment.
       */
      double updateU () {
         myUvec.sub (myPntA.getPosition(), myPntB.getPosition());
         myLength = myUvec.norm();
         if (myLength != 0) {
            myUvec.scale (1 / myLength);
         }
         return myLength;
      }

      /**
       * Computes the derivative of the length of this segment.
       * Assumes that uvec is up to date.
       */
      double getLengthDot () {
         Vector3d velA = myPntA.getVelocity();
         Vector3d velB = myPntB.getVelocity();
         double dvx = velA.x-velB.x;
         double dvy = velA.y-velB.y;
         double dvz = velA.z-velB.z;
         return myUvec.x*dvx + myUvec.y*dvy + myUvec.z*dvz;
      } 

      /**
       * Update the P matrix of this segment, defined as
       * <pre>
       * I - u u^T
       * </pre>
       * where u is the segment unit vector. Assumes that uvec is up to date.
       */
      void updateP () {
         myP.outerProduct (myUvec, myUvec);
         myP.negate();
         myP.m00 += 1;
         myP.m11 += 1;
         myP.m22 += 1;
      }

      /**
       * Updates the derivatives of the tension force F with respect to changes
       * in end-points A and B. Assumes that uvec and P are both up to date.
       */
      void updateDfdx (double dFdl, double dFdldot) {
         if (!myPassiveP) {
            mydFdxA.scale (dFdl, myUvec);
            if (!myIgnoreCoriolisInJacobian) {
               Vector3d y = new Vector3d();
               y.sub (myPntA.getVelocity(), myPntB.getVelocity());
               myP.mulTranspose (y, y);
               y.scale (dFdldot/myLength);
               mydFdxA.add (y);  
            }
            mydFdxB.negate (mydFdxA);
         }
         else {
            mydFdxB.setZero();
            mydFdxA.setZero();
         }
      }

      /**
       * If this segment has subsegments, return the first subsegment.
       * Otherwise, return <code>null</code>.
       */
      public SubSegment firstSubSegment() {
         return null;
      }

      /**
       * If this segment has subsegments, return the last subsegment.
       * Otherwise, return <code>null</code>.
       */
      public SubSegment lastSubSegment() {
         return null;
      }

      /**
       * Queries whether this segment has subsegments.
       * @return <code>true</code> if this segment has subsegments.
       */
      public boolean hasSubSegments() {
         return false;
      }

      /**
       * Scan attributes of this segment from a ReaderTokenizer. Used to
       * implement scanning for the MultiPointSpring.
       */
      boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
         throws IOException {

         rtok.nextToken();
         if (ScanWriteUtils.scanAttributeName (rtok, "passive")) {
            myPassiveP = rtok.scanBoolean();
            return true;
         }
         else if (ScanWriteUtils.scanAndStoreReference (rtok, "pntB", tokens)) {
            return true;
         }
         else if (ScanWriteUtils.scanAndStoreReference (rtok, "pntA", tokens)) {
            return true;
         }
         rtok.pushBack();
         return false;
      }

      /**
       * Postscans end-point information for this segment. Used to implement
       * postscan for the MultiPointSpring.
       */
      boolean postscanItem (
         Deque<ScanToken> tokens, CompositeComponent ancestor)
         throws IOException {
         if (ScanWriteUtils.postscanAttributeName (tokens, "pntB")) {
            myPntB = ScanWriteUtils.postscanReference (
               tokens, Point.class, ancestor);
            return true;
         }
         else if (ScanWriteUtils.postscanAttributeName (tokens, "pntA")) {
            myPntA = ScanWriteUtils.postscanReference (
               tokens, Point.class, ancestor);
            return true;
         }
         return false;         
      }

      /**
       * Writes attributes of this segment to a PrintWriter. Used to implement
       * writing for the MultiPointSpring.
       */
      void writeItems (
         PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
         throws IOException {
         
         pw.println (
            "pntB=" + ComponentUtils.getWritePathName (ancestor, myPntB));
         pw.println (
            "pntA=" + ComponentUtils.getWritePathName (ancestor, myPntA));
         pw.println ("passive=" + myPassiveP);
      }

      /**
       * Applies distance scaling to this segment.
       */
      void scaleDistance (double s) {
      }

      /**
       * Transforms the geometry of this segment.
       */
      public void transformGeometry (GeometryTransformer gtr) {
      }
   }

   /**
    * Contains subsegment information. If a wrappable segment is in contact
    * with one or more wrappables, it is divided into subsegments, which
    * connect the A/B points of different wrappables to either each other, or
    * to the terminating via points.
    */   
   public class SubSegment extends Segment {

      // The end points A and B of a subsegment are either a fixed via point of
      // the MultiPointSpring, or the A/B points of a wrappable. In the latter
      // case, an attachment is assigned to transmit the sub-segment forces to
      // the wrappable.
      public Wrappable myWrappableB; // possible wrappable associated with B
      public Wrappable myWrappableA; // possible wrappable associated with A
      PointAttachment myAttachmentB; // possible attachment for B
      PointAttachment myAttachmentA; // possible attachment for A

      // Link to the next subsegment
      SubSegment myNext;

      SubSegment() {
         super();
      }

      public SubSegment getNext() {
         return myNext;
      }

      /**
       * Applies the tension F in this subsegment to the via points or
       * wrappables associated with its end-points.
       */
      void applyForce (double F) {
         if (myAttachmentB != null) {
            myPntB.zeroForces();
         }
         if (myAttachmentA != null) {
            myPntA.zeroForces();
         }
         super.applyForce (F);
         if (myAttachmentB != null) {
            myAttachmentB.applyForces();
         }
         if (myAttachmentA != null) {
            myAttachmentA.applyForces();
         }
      }

      /**
       * Computes the derivative of the length of this subsegment.
       * Assumes that uvec is up to date.
       */
      double getLengthDot () {
         if (myAttachmentB != null) {
            myAttachmentB.updateVelStates();
         }
         if (myAttachmentA != null) {
            myAttachmentA.updateVelStates();
         }
         Vector3d velA = myPntA.getVelocity();
         Vector3d velB = myPntB.getVelocity();
         double dvx = velA.x-velB.x;
         double dvy = velA.y-velB.y;
         double dvz = velA.z-velB.z;
         return myUvec.x*dvx + myUvec.y*dvy + myUvec.z*dvz;
      } 
   }

   /**
    * Implements a wrappable segment. This is done by dividing the segment into
    * a fixed number of "knots", which are attracted to each other and out of
    * the interior of "wrappable" objects using linear elastic forces. Once per
    * time step, these forces are used to iteratively update the knot positions
    * so as to "shrink wrap" the segment around whatever wrappables it is
    * associated with. The physics used to do this is first-order, and
    * independent of the second order physics of the overall simulation.
    */
   public class WrapSegment extends Segment {
      int myNumKnots;                       // number of knot points
      WrapKnot[] myKnots;                   // list of knot points
      double myDscale;

      int debugLevel = myDebugLevel;

      ArrayList<float[]> myRenderABPoints;  // rendering positions for A/B points

      // Optional list of points that are used to help provide an initial
      // "path" for the segment
      Point3d[] myInitialPnts;             

      SubSegment mySubSegHead; // first subsegment
      SubSegment mySubSegTail; // last subsegment

      protected int[] myContactCnts; // number of knots contacting each wrappable
      
      public int[] getContactCnts() {
         if (myContactCnts == null) {
            myContactCnts = new int[myWrappables.size()];
         }
         return myContactCnts;
      }
     
      WrapSegment () {
         this (0, null);
      }

      WrapSegment (int numk, Point3d[] initialPnts) {
         super();
         myNumKnots = numk;
         myKnots = new WrapKnot[numk];
         for (int i=0; i<numk; i++) {
            myKnots[i] = new WrapKnot();
         }
         myRenderABPoints = null; // will be created in prerender()
         myInitialPnts = initialPnts;
         myDscale = 1.0;
      }

      void clearSubSegs () {
         mySubSegHead = null;
         mySubSegTail = null;
      }

      /**
       * Remove seg and all SubSegments following it.
       */
      void removeSubSegs (SubSegment seg) {
         SubSegment sg = mySubSegHead;
         if (sg == seg) {
            clearSubSegs();
         }
         else {
            while (sg != null) {
               if (sg.myNext == seg) {
                  sg.myNext = null;
                  mySubSegTail = sg;
                  break;
               }
               sg = sg.myNext;
            }
         }
     }

      /**
       * Append seg to the list of SubSegments
       */
      void addSubSeg (SubSegment seg) {
         if (mySubSegHead == null) {
            mySubSegHead = seg;
         }
         else {
            mySubSegTail.myNext = seg;         
         }
         mySubSegTail = seg;
         seg.myNext = null;
      }


      /**
       * initialize the knots in the strand so that they are distributed evenly
       * along the piecewise-linear path specified by the start and end points
       * and any initialization points that may have been specified.
       */      
      void initializeStrand (Point3d[] initialPnts) {
         
         myContactCnts = null;

         // create the piece-wise path
         ArrayList<Point3d> pnts = new ArrayList<Point3d>();
         pnts.add (myPntB.getPosition());
         if (initialPnts != null) {
            for (int i=0; i<initialPnts.length; i++) {
               pnts.add (initialPnts[i]);
            }
         }
         pnts.add (myPntA.getPosition());
         // compute the length of this path
         double length = 0;
         for (int i=1; i<pnts.size(); i++) {
            length += pnts.get(i).distance (pnts.get(i-1));
         }
         // distribute knots along the path according to distance
         
         double seglen;  // length of each segment
         double dist0;   // path distance at the beginning of each segment
         int pidx;       // index of last point on each segment
         pidx = 1;
         dist0 = 0;
         seglen = pnts.get(pidx).distance (pnts.get(pidx-1));
         for (int k=0; k<myNumKnots; k++) {
            // interpolate knot position along current segment
            double dist = (k+1)*length/(myNumKnots+1);
            double s = (dist-dist0)/seglen;
            while (s > 1 && pidx < pnts.size()-1) {
               pidx++;
               dist0 += seglen;
               seglen = pnts.get(pidx).distance (pnts.get(pidx-1));
               s = (dist-dist0)/seglen;
            }
            if (s > 1) {               
               s = 1; // paranoid
            }
            myKnots[k].myPos.combine (
                1-s, pnts.get(pidx-1), s, pnts.get(pidx));
         }
         myLength = computeLength();
         myDscale = 1.0;
      }

      void updateContactingKnotPositions() {
         int cnt = 0;
         for (int k=0; k<myNumKnots; k++) {
            WrapKnot knot = myKnots[k];
            Wrappable wrappable = knot.getWrappable();
            if (wrappable != null) {
               knot.myPos.transform (wrappable.getPose(), knot.myLocPos);
               cnt++;
            }
         }
      }
      
      void saveContactingKnotPositions() {
         int cnt = 0;
         for (int k=0; k<myNumKnots; k++) {
            WrapKnot knot = myKnots[k];
            Wrappable wrappable = knot.getWrappable();
            if (wrappable != null) {
               knot.myLocPos.inverseTransform (wrappable.getPose(), knot.myPos);
               cnt++;
            }
         }
      }
      
      /**
       * Checks each knot in this segment to see if it is intersecting any
       * wrappables, and if so, computes the contact normal and distance. If a
       * knot intersects multiple wrappables, then the one with the deepest
       * penetration is used.
       *
       * <p>This method returns <code>true</code> if the contact configuration
       * has changed.
       */
      boolean updateContacts (int[] contactCnts) {
         boolean changed = false;
         Vector3d nrml = new Vector3d();
         Matrix3d dnrm = new Matrix3d();
         if (contactCnts != null) {
            for (int i=0; i<contactCnts.length; i++) {
               contactCnts[i] = 0;
            }
         }
         for (int k=0; k<myNumKnots; k++) {
            WrapKnot knot = myKnots[k];
            double dist = 0;
            Wrappable lastWrappable = knot.getWrappable();
            knot.setWrappableIdx (-1);
            knot.myPrevDist = knot.myDist;
            knot.myDist = Wrappable.OUTSIDE;
            for (int i=0; i<myWrappables.size(); i++) {
               Wrappable wrappable = myWrappables.get(i);
               double d = wrappable.penetrationDistance (
                  nrml, dnrm, knot.myPos);
               if (d < knot.myDist) {
                  knot.myDist = d;
                  if (d < 0) {
                     knot.setWrappableIdx (i);
                     if (contactCnts != null) {
                        contactCnts[i]++;
                     }
                     knot.myNrml.set (nrml);
                     knot.myDnrm.set (dnrm);
                  }
               }
            }
            if (knot.getWrappable() != lastWrappable) {
               changed = true;
            }
         }
         return changed;
      }

      /**
       * Projects knots which are penetrating a wrappable onto the surface of
       * that wrappable.
       */
      void projectContacts () {

         double ptol = myMaxWrapDisplacement/10;
         for (int k=0; k<myNumKnots; k++) {
            WrapKnot knot = myKnots[k];
            if (knot.myDist < -ptol) {
               double del = -ptol-knot.myDist;
               knot.myPos.scaledAdd (del, knot.myNrml);
               knot.myDist += del;
            }
         }
      }

      /**
       * Updates the forces on each knot point. These forces are the sum of the
       * tension between adjacent knots, plus the repulsion forces pushing
       * knots out of any wrappable which it is penetrating.
       */
      void updateForces() {

         for (int k=0; k<myNumKnots; k++) {
            WrapKnot knot = myKnots[k];
            if (knot.myDist < 0) {
               knot.myForce.scale (
                  -knot.myDist*myContactStiffness, knot.myNrml);
            }
            else {
               knot.myForce.setZero();
            }
         }
         Vector3d dprev = new Vector3d();
         Vector3d dnext = new Vector3d();
         dprev.sub (myKnots[0].myPos, myPntB.getPosition());
         for (int k=0; k<myNumKnots; k++) {
            if (k<myNumKnots-1) {
               dnext.sub (myKnots[k+1].myPos, myKnots[k].myPos);
            }
            else {
               dnext.sub (myPntA.getPosition(), myKnots[k].myPos);
            }
            WrapKnot knot = myKnots[k];
            knot.myForce.scaledAdd (-myWrapStiffness, dprev);
            knot.myForce.scaledAdd ( myWrapStiffness, dnext);
            dprev.set (dnext);
         }
      }        

      /**
       * Updates the stiffness matrix terms associated with each knot point.
       * These give the force derivatives with respect to changes in knot
       * position. The stiffness matrix structure is block-tridiagonal, where
       * the diagonal blocks account for self-motion and changes wrappable
       * repulsion forces, while the off-diagonal blocks account for the
       * coupling between adjacent blocks.
       */
      protected void updateStiffness (double dnrmGain, double dscale) {
         double stiffness = myWrapStiffness;
         double cstiffness = myContactStiffness;
         double d = dscale*myWrapDamping/(myNumKnots*myNumKnots);
         double cd = dscale*myContactDamping/(myNumKnots*myNumKnots);
         for (int k=0; k<myNumKnots; k++) {
            WrapKnot knot = myKnots[k];
            // double s = myWrapDamping +2*stiffness;
            // knot.myBmat.setDiagonal (s, s, s);
            // if (knot.myDist < 0) {
            //    knot.myBmat.addScaledOuterProduct (
            //       cstiffness, knot.myNrml, knot.myNrml);
            // }
            double s = d+2*stiffness;
            knot.myBmat.setDiagonal (s, s, s);
            if (knot.myDist < 0) {
               knot.myBmat.addScaledOuterProduct (
                  cd+cstiffness, knot.myNrml, knot.myNrml);
               if (dnrmGain != 0) {
                  knot.myBmat.scaledAdd (
                     dnrmGain*knot.myDist*cstiffness, knot.myDnrm);
               }
            }
         }
      }

      /**
       * Collects the forces of all knots into a single vector <code>f</code>.
       */
      private void getForces (VectorNd f) {
         for (int k=0; k<myNumKnots; k++) {         
            f.setSubVector (k*3, myKnots[k].myForce);
         }
      }

      /**
       * Collects the positions of all knots into a single vector
       * <code>q</code>.
       */
      private void getPositions (VectorNd q) {
         for (int k=0; k<myNumKnots; k++) {         
            q.setSubVector (k*3, myKnots[k].myPos);
         }
      }

      /**
       * Sets the positions of all knots from a single vector <code>q</code>.
       */
      private void setPositions (VectorNd q) {
         for (int k=0; k<myNumKnots; k++) {         
            q.getSubVector (k*3, myKnots[k].myPos);
         }
      }

      /**
       * For each knot, save the myDvec field into myVtmp.
       */
      private void saveDvecToVtmp () {
         for (int k=0; k<myNumKnots; k++) {         
            WrapKnot knot = myKnots[k];
            knot.myVtmp.set (knot.myDvec);
         }
      }

      /**
       * For each knot, advance myPos by s*myDvec
       */
      private void advancePosByDvec (double s) {
         for (int k=0; k<myNumKnots; k++) {         
            WrapKnot knot = myKnots[k];
            knot.myPos.scaledAdd (s, knot.myDvec);
         }
         myLength = computeLength();
      }

      /**
       * For each knot, advance myPos by s*myVtmp
       */
      private void advancePosByVtmp (double s) {
         for (int k=0; k<myNumKnots; k++) {         
            WrapKnot knot = myKnots[k];
            knot.myPos.scaledAdd (s, knot.myVtmp);
         }
         myLength = computeLength();
      }

      /**
       * Uses numerical differentiation to estimate the stiffness
       * matrix for all the knots. Used for debugging.
       */
      protected MatrixNd computeNumericStiffness() {

         int numk = myNumKnots;
         VectorNd f0 = new VectorNd (3*numk);
         VectorNd q0 = new VectorNd (3*numk);
         VectorNd f = new VectorNd (3*numk);
         VectorNd q = new VectorNd (3*numk);
         MatrixNd K = new MatrixNd (3*numk, 3*numk);

         updateContacts(null);
         updateForces();

         boolean changed = false;

         getPositions (q0);
         getForces (f0);
         double h = 1e-8;
         for (int i=0; i<3*numk; i++) {
            q.set (q0);
            q.add (i, h);
            setPositions (q);
            if (updateContacts(null)) {
               changed = true;
            }
            updateForces();
            getForces (f);
            f.sub (f0);
            f.scale (1/h);
            K.setColumn (i, f);
         }
         setPositions (q0);
         if (updateContacts(null)) {
            changed = true;
         }
         if (changed) {
            //System.out.println ("CONTACT STATE CHANGED");
         }
         updateForces();
         return K;
      }

      //
      // Computes the required displacements d_i for each knot point
      // resulting from solving the block tridiagonal spring system
      //
      // [ B_0 C_0                 0    ] [   x_0   ]   [   f_0   ]
      // [                              ] [         ]   [         ]
      // [ A_1 B_1 C_1                  ] [   x_1   ]   [   f_1   ]
      // [                              ] [         ]   [         ]
      // [     A_2 B_2   ...            ] [   x_2   ] = [   f_2   ]
      // [                              ] [         ]   [         ]
      // [         ...   ...     C_{n-2}] [         ]   [         ]
      // [                              ] [         ]   [         ]
      // [  0           A_{n-1}  B_{n-1}] [ x_{n-1} ]   [ f_{n-1} ]
      //
      // that results from first order physics. e use the block tridiagonal
      // matrix algorithm:
      //
      //         { inv(B_i) C_i                                   i = 0
      //  C'_i = {
      //         { inv(B_i - A_i*C'_{i-1}) C_i                    i = 1, 2, ...
      //
      //         { inv(B_i) f_i                                   i = 0
      //  d_i =  {
      //         { inv(B_i - A_i*C'_{i-1}) (f_i - A_i d_{i-1})   i > 0
      //
      //  x_n  = d_n
      //
      //  x_i  = d_i - C'_i x_{i+1}    i = n-2, ... 0
      //
      // We also exploit the fact that all C_i and A_i matrices are given by
      //
      // C_i = A_i = wrapStiffness I,
      //
      // where I is the 3x3 identity matrix, so their multiplications can be
      // done by simple scaling.
      //
      // For each knot, B_i, inv(B_i), C'_i, f_i and d_i are stored in the
      // myBmat, myBinv, myCinv, myForce and myDvec fields.
      //
      void factorAndSolve () {
         double c = -myWrapStiffness;
         WrapKnot knot = myKnots[0];
         knot.myBinv.invert (knot.myBmat);
         knot.myCinv.scale (c, knot.myBinv);
         for (int i=1; i<myNumKnots; i++) {
            knot = myKnots[i];
            Matrix3d Binv = knot.myBinv;
            Binv.scaledAdd (-c, myKnots[i-1].myCinv, knot.myBmat);
            Binv.invert();
            if (i<myNumKnots-1) {
               knot.myCinv.scale (c, Binv);
            }
         }
         Vector3d vec = new Vector3d();
         Vector3d tmp = new Vector3d();
         knot = myKnots[0];
         knot.myBinv.mul (knot.myDvec, knot.myForce);
         for (int i=1; i<myNumKnots; i++) {
            knot = myKnots[i];
            vec.set (knot.myForce);
            vec.scaledAdd (-c, myKnots[i-1].myDvec);
            knot.myBinv.mul (knot.myDvec, vec);
         }

         int k = myNumKnots-1;
         vec.set (myKnots[k].myDvec);
         double maxd = 0; //maximum displacement
         while (--k >= 0) {
            knot = myKnots[k];
            knot.myCinv.mul (tmp, vec);
            vec.sub (knot.myDvec, tmp);
            knot.myDvec.set (vec);
            double m = vec.infinityNorm();
            if (m > maxd) {
               maxd = m;
            }
         }
         if (maxd > getMaxWrapDisplacement()) {
            // scale solution to maximum displacement
            double s = getMaxWrapDisplacement()/maxd;
            for (int i=0; i<myNumKnots; i++) {
               myKnots[i].myDvec.scale (s);
            }
         }
         // for (int i=0; i<myNumKnots; i++) {
         //    knot = myKnots[i];
         //    knot.myPos.add (knot.myDvec);
         // }
         // myLength = computeLength();
      }

//      void rescaleSolution (double s) {
//         for (int i=0; i<myNumKnots; i++) {
//            WrapKnot knot = myKnots[i];
//            knot.myPos.scaledAdd (s, knot.myDvec);
//         }
//         myLength = computeLength();
//      }

//      double maxDisplacement() {
//         double maxDisp = 0;
//         for (int i=0; i<myNumKnots; i++) {
//            double disp = myKnots[i].myDvec.norm();
//            if (disp > maxDisp) {
//               maxDisp = disp;
//            }
//         }
//         return maxDisp;
//      }         

      double maxLateralDisplacement() {
         Vector3d u = new Vector3d();
         Vector3d xprod = new Vector3d();
         double maxDisp = 0;
         for (int i=0; i<myNumKnots-1; i++) {
            u.sub (myKnots[i+1].myPos, myKnots[i].myPos);
            double len = u.norm();
            if (len != 0) {
               xprod.cross (u, myKnots[i].myDvec);
               double disp = xprod.norm()/len;
               if (disp > maxDisp) {
                  maxDisp = disp;
               }
            }
         }
         return maxDisp;
      }         

//      double computeDecrement() {
//         double dot = 0;
//         for (int i=0; i<myNumKnots; i++) {
//            WrapKnot knot = myKnots[i];
//            dot += knot.myForce.dot (knot.myDvec);
//         }
//         return Math.sqrt(dot);
//      }

      double forceNorm() {
         double sumSqr = 0;
         for (int i=0; i<myNumKnots; i++) {
            sumSqr += myKnots[i].myForce.normSquared();
         }
         return Math.sqrt(sumSqr);
      }

//      double maxContactScaling () {
//         double maxs = 0;
//         for (int i=0; i<myNumKnots; i++) {
//            WrapKnot knot = myKnots[i];
//            if (knot.myDist < 0) {
//               double s = -knot.myDist/knot.myDvec.norm();
//               if (s > maxs) {
//                  maxs = s;
//               }
//            }
//         }
//         return maxs;
//      }

//      double averageContactScaling () {
//         double avgs = 0;
//         int cnt = 0;
//         for (int i=0; i<myNumKnots; i++) {
//            WrapKnot knot = myKnots[i];
//            if (knot.myDist < 0) {
//               avgs += -knot.myDist/knot.myDvec.norm();
//               cnt++;
//            }
//         }
//         if (cnt > 0) {
//            return avgs/cnt;
//         }
//         else {
//            return 0;
//         }
//      }

      double computeLength() {
         double len = 0;
         Point3d pos0 = myPntB.getPosition();
         for (int k=0; k<myNumKnots; k++) {
            Point3d pos1 = myKnots[k].myPos;
            len += pos1.distance(pos0);
            pos0 = pos1;
         }
         len += myPntA.getPosition().distance(pos0);
         return len;
      }

      int totalIterations;
      int totalCalls;
      int totalFails;

      void checkStiffness() {
         updateContacts(null);
         updateForces();
         updateStiffness(1.0, 1.0);
         int numk = myNumKnots;
         MatrixNd K = new MatrixNd(3*numk, 3*numk);
         double c = myWrapStiffness;
         Matrix3d C = new Matrix3d();
         C.setDiagonal (-c, -c, -c);

         for (int k=0; k<numk; k++) {
            WrapKnot knot = myKnots[k];
            K.setSubMatrix (3*k, 3*k, knot.myBmat);
            if (k > 0) {
               K.setSubMatrix (3*k, 3*(k-1), C);
            }
            if (k < numk-1) {
               K.setSubMatrix (3*k, 3*(k+1), C);
            }
         }
         K.negate();
         //System.out.println ("K=\n" + K.toString ("%8.4f"));
         MatrixNd KC = computeNumericStiffness();
         //System.out.println ("KC=\n" + KC.toString ("%8.4f"));

         MatrixNd E = new MatrixNd (K);
         E.sub (KC);
         //System.out.println ("E=\n" + E.toString ("%8.4f"));
         System.out.println ("Enorm=" + E.frobeniusNorm());
      }         

      /**
       * Updates the knot points in this wrappable segment. This is done by
       * iterating until the first order physics resulting from the attractive
       * forces between adjacent knots and repulsive forces from contacting
       * wrappables results in a stable configuration.
       */
      void updateWrapStrand (int maxIter) {
         int icnt = 0;
         boolean converged = false;
         double prevForceNorm = -1;
         double dscale = myDscale;

         int noContactChangeCnt = 0;
         updateContactingKnotPositions();
         //boolean contactChanged = updateContacts(numc); 
         //System.out.println ("updateWrapStrand");
         int[] contactCnts = getContactCnts();
         int[] newContactCnts = new int[myWrappables.size()];

         do {
            double prevLength = myLength;
            updateForces();
            double forceNorm = forceNorm();
            double dnrmGain = (noContactChangeCnt >= 2 ? myDnrmGain : 0);
            updateStiffness(dnrmGain, dscale);
            factorAndSolve();
            advancePosByDvec (1.0);
            //double dec = computeDecrement();
            //double alpha = (dec <= 1/4.0 ? 1.0 : 1/(1+dec));
            boolean contactChanged = updateContacts (newContactCnts);
            if (contactChanged) {
               noContactChangeCnt = 0;
            }
            else {
               //System.out.println ("contact stable");
               noContactChangeCnt++;
            }
            boolean contactBroken = false;
            for (int i=0; i<myWrappables.size(); i++) {
               if (contactCnts[i] != 0 && newContactCnts[i] == 0) {
                  if (debugLevel > 0) {
                     System.out.println (
                        "  broken, old numc=" + contactCnts[i]);
                  }
                  newContactCnts[i] = contactCnts[i];
                  contactCnts[i] = 0;
                  contactBroken = true;
               }
               else if (contactCnts[i] == 0 && newContactCnts[i] != 0) {
                  if (debugLevel > 0) {
                     System.out.println (
                        "  contacting, new numc=" + newContactCnts[i]);
                     System.out.println (
                        "TWW=\n" + myWrappables.get(0).getPose());
                     for (int k=0; k<myNumKnots; k++) {
                        if (myKnots[k].getWrappable() != null) {
                           System.out.println (
                              "    "+k+" "+myKnots[k].myDist+"   "+
                              myKnots[k].myPos);
                        }
                     }
                  }
                  contactCnts[i] = newContactCnts[i];
                  newContactCnts[i] = 0;
               }
               else {
                  contactCnts[i] = newContactCnts[i];
                  newContactCnts[i] = 0;
               }
            }
            if (contactBroken) {
               updateForces();
               updateStiffness(dnrmGain, dscale);
               saveDvecToVtmp();
               factorAndSolve();
               double scale = 1.0;
               for (int k=0; k<myNumKnots; k++) {
                  WrapKnot knot = myKnots[k];
                  int widx = knot.myPrevWrappableIdx;
                  if (widx != -1 && newContactCnts[widx] != 0) {
                     // contact was broken for this knot
                     if (knot.myDvec.dot (knot.myVtmp) < 0) {
                        // knot is trying to renter object, so find scale factor
                        double dist = knot.myDist;
                        double prev = knot.myPrevDist;
                        if (dist != Wrappable.OUTSIDE && dist > 0 && prev < 0) {
                           double s = dist/(dist-prev);
                           if (s < scale) {
                              scale = s;
                           }
                        }
                     }
                  }
               }
               if (scale < 1.0) {
                  if (debugLevel > 0) {
                     System.out.println ("  rescale " + scale);
                  }
                  if (myContactRescaling) {
                     advancePosByVtmp (-scale);
                     updateContacts (contactCnts);
                  }
               }
            }
            if (prevForceNorm >= 0) {
               if (forceNorm >= prevForceNorm) {
                  dscale *= 2.0;
                  if (debugLevel > 0) {
                     System.out.printf (
                        "  force=%5g ^ dscale=%g\n", forceNorm, dscale);
                  }
               }
               else if (dscale > 1.0) {
                  dscale *= 0.5;
                  if (debugLevel > 0) {
                     System.out.printf (
                        "  force=%5g V dscale=%g\n", forceNorm, dscale);
                  }
               }
            }
            
                  
            // //System.out.println (
            // // "numc=" + numc.value+" oldc="+oldc+" changed=" + contactChanged);
            // if (numc.value == 0 && oldc > 0) {
            //    //System.out.println ("break contact scale");
            //    if (0.9 < alpha) {
            //       alpha = 0.9;
            //    }
            // }
            // else if (numc.value > 0 && oldc == 0) {
            //    double s = maxContactScaling();
            //    if (1 - 0.9*s < alpha) {
            //       alpha = 1 - 0.9*s;
            //    }
            //    //System.out.println ("make contact scale " + (-0.9*s));
            // }
            // // if (mySor != 1.0) {
            // //    rescaleSolution (mySor-1.0);
            // //    updateContacts (numc);
            // // }
            // if (alpha < 1.0) {
            //    //System.out.println (
            //    // "rescaling " + alpha + " " + noContactChangeCnt);

            //    //rescaleSolution (alpha-1.0);
            //    //updateContacts (numc);
            // }
            prevForceNorm = forceNorm;
            double ltol = myLength*myLengthConvTol;
            // check for convergence - at moment, this is when the knots stop
            // moving.
            if (!contactChanged) {
               double maxDisp = maxLateralDisplacement();
               if (maxDisp/myLength < 1e-4 &&
                   Math.abs(prevLength-myLength) < ltol) {
                  converged = true;
               }
            }
            
         }
         while (++icnt < maxIter && !converged);
         saveContactingKnotPositions();
         //checkStiffness();
         totalIterations += icnt;
         totalCalls++;
         if (converged) {
            if (debugLevel > 0 && icnt != 1) {
               System.out.println ("converged, icnt="+icnt);
            }
         }
         else {
            if (debugLevel > 0) {
               System.out.println ("did not converge");
            }
            totalFails++;
         }
         if ((totalCalls % 100) == 0) {
            // System.out.println (
            //    "fails=" + totalFails + "/" + totalCalls + "  avg icnt=" +
            //    totalIterations/(double)totalCalls);
         }
         myDscale = dscale;
      }

      /**
       * Returns the position for knot k. If k < 0, returns the position of the
       * initial via point. If k >= numKnots, returns the position of the
       * final via point.
       */
      Point3d getKnotPos (int k) {
         if (k < 0) {
            if (k == -1) {
               return myPntB.getPosition();
            }
            else {
               return null;
            }
         }
         else if (k >= myNumKnots) {
            if (k == myNumKnots) {
               return myPntA.getPosition();
            }
            else {
               return null;
            }
         }
         else {
            return myKnots[k].myPos;
         }
      }               

      public int numKnots() {
         return myNumKnots;
      }

      public WrapKnot getKnot(int idx) {
         return myKnots[idx];
      }

      public void setKnotPositions (Point3d[] plist) {
         if (plist.length < myNumKnots) {
            throw new IllegalArgumentException (
               "Number of positions "+plist.length+
               " less than number of knots");
         }
         for (int i=0; i<myNumKnots; i++) {
            myKnots[i].myPos.set (plist[i]);
         }
      }

      /**
       * Computes a normal for the plane containing the three knot points
       * <code>p0</code>, <code>pk</code>, and <code>p1</code>.  This plane can
       * be used to help compute the A/B points on the wrappable surface.
       *
       * <p>If the three knots are colinear, knots past <code>p1</code> are
       * searched (starting at index <code>k1</code> and advancing in the
       * direction <code>kinc</code>) until a non-colinear one is found.
       */
      void computeSideNormal (
         Vector3d sideNrm, Point3d p0, Point3d pk, Point3d p1, 
         int k1, int kinc) {

         Vector3d delk0 = new Vector3d();
         Vector3d del10 = new Vector3d();

         delk0.sub (pk, p0);
         del10.sub (p1, p0);
         double tol = 1e-8*del10.norm();
         sideNrm.cross (del10, delk0);
         double mag = sideNrm.norm();
         while (mag < tol) {
            k1 += kinc;
            p1 = getKnotPos (k1);
            if (p1 == null) {
               break;
            }
            del10.sub (p1, p0);
            sideNrm.cross (del10, delk0);
            mag = sideNrm.norm();
         }
         if (mag == 0) {
            // No apparent side normal. Just pick something perpendicular to
            // d0x.
            sideNrm.perpendicular (del10);
            sideNrm.normalize();
         }
         else {
            sideNrm.scale (1/mag);
         }
      }

      private Point createTanPoint (Point3d pos) {
         Point pnt = new Point (pos);
         return pnt;
      }

      /**
       * Creates a subsegment between two knots indexed by kb and ka. The
       * information is stored in <code>sugseg</code>, unless
       * <code>sugseg</code> is <code>null</code>, in which case a new
       * subsegment object is created and addded.
       *
       *<p> kb is the index of the last knot contacting the previous wrappable,
       * unless kb = -1, in which case the subsegment is formed between knot ka
       * and the initial via point. If knot kb is contacting a wrappable, an
       * exit point B is determined on that wrappable by computing the surface
       * tangent associated with knots ka and kb-1.
       *
       * <p> ka is the index of the first knot contacting the next wrappable,
       * unless ka = numKnots, in which case the subsegment is formed between
       * knot kb and the final via point. If knot ka is contacting a wrappable,
       * an exit point A is determined on that wrappable by computing the
       * surface tangent associated with knots kb and ka+1.
       */
      SubSegment addOrUpdateSubSegment (int ka, int kb, SubSegment subseg) {

         Wrappable wrappableA = null;
         if (ka >= 0 && ka < myNumKnots) {
            wrappableA = myKnots[ka].getWrappable();
         }
         Wrappable wrappableB = null;
         if (kb >= 0 && kb < myNumKnots) {
            wrappableB = myKnots[kb].getWrappable();
         }

         Vector3d sideNrm = new Vector3d();
         Point3d tanA = null;
         if (wrappableA != null) {
            tanA = new Point3d();
            Point3d pb = getKnotPos (kb);
            Point3d p0 = getKnotPos (ka-1);
            Point3d pa = getKnotPos (ka);
            Point3d p1 = getKnotPos (ka+1);
            computeSideNormal (sideNrm, p0, pa, p1, ka+1, 1);
            wrappableA.surfaceTangent (
               tanA, pb, p1,
               LineSegment.projectionParameter (pb, p1, p0), sideNrm);
            // if (tanA.epsilonEquals (Vector3d.ZERO, 1e-6)) {
            //    System.out.println ("TANA=0");
            // }
         }
         Point3d tanB = null;
         if (wrappableB != null) {
            tanB = new Point3d();
            Point3d pa = getKnotPos (ka);
            Point3d p0 = getKnotPos (kb+1);
            Point3d pb = getKnotPos (kb);
            Point3d p1 = getKnotPos (kb-1);
            computeSideNormal (sideNrm, p0, pb, p1, kb-1, -1);
            wrappableB.surfaceTangent (
               tanB, pa, p1,
               LineSegment.projectionParameter (pa, p1, p0), sideNrm);
            // if (tanB.epsilonEquals (Vector3d.ZERO, 1e-6)) {
            //    System.out.println ("TANB=0");
            // }
         
         }
         
         if (subseg == null) {
            subseg = new SubSegment();
            addSubSeg (subseg);
         }
         if (wrappableB == null) {
            subseg.myPntB = myPntB;
            subseg.myAttachmentB = null;
            subseg.myWrappableB = null;
         }
         else if (subseg.myWrappableB != wrappableB) {
            subseg.myPntB = createTanPoint (tanB);
            subseg.myAttachmentB =
               wrappableB.createPointAttachment (subseg.myPntB);
            subseg.myWrappableB = wrappableB;            
         }
         else {
            subseg.myPntB.setPosition (tanB);
            subseg.myAttachmentB.updateAttachment();
         }

         if (wrappableA == null) {
            subseg.myPntA = myPntA;
            subseg.myAttachmentA = null;
            subseg.myWrappableA = null;
         }
         else if (subseg.myWrappableA != wrappableA) {
            subseg.myPntA = createTanPoint (tanA);
            subseg.myAttachmentA =
               wrappableA.createPointAttachment (subseg.myPntA);
            subseg.myWrappableA = wrappableA;
         }
         else {
            subseg.myPntA.setPosition (tanA);
            subseg.myAttachmentA.updateAttachment();
         }
         return subseg.myNext;
      }

      /**
       * Updates the subsegments associated with this wrappable segment.
       * Assumes that updateWrapStrand() has already been called. If any knots
       * are in contact with wrappables, subsegments are created between (a)
       * the initial via point and the first wrappable, (b) each distinct
       * wrappable, and (c) the last wrappable and the final via point.
       */
      void updateSubSegments() {
         Wrappable wrappable = null;
         SubSegment subseg = mySubSegHead;
         int lastContactK = -1;
         for (int k=0; k<myNumKnots; k++) {
            WrapKnot knot = myKnots[k];
            if (knot.getWrappable() != null) {
               if (knot.getWrappable() != wrappable) {
                  // transitioning to a new wrappable
                  subseg = addOrUpdateSubSegment (k, lastContactK, subseg);
                  wrappable = knot.getWrappable();
               }
               lastContactK = k;
            }
         }
         if (wrappable != null) {
            subseg = addOrUpdateSubSegment (myNumKnots, lastContactK, subseg);
         }
         if (subseg != null) {
            removeSubSegs (subseg);
         }
      }

      /**
       * If this segment has subsegments, return the first subsegment.
       * Otherwise, return <code>null</code>.
       */
      public SubSegment firstSubSegment() {
         return mySubSegHead;
      }

      /**
       * If this segment has subsegments, return the last subsegment.
       * Otherwise, return <code>null</code>.
       */
      public SubSegment lastSubSegment() {
         return mySubSegTail;
      }

       /**
       * Queries whether this segment has subsegments.
       * @return <code>true</code> if this segment has subsegments.
       */
     public boolean hasSubSegments() {
         return mySubSegHead != null;
      }

      // Begin methods to save and restore auxiliary state.
      //
      // Auxiliary state for a wrappable segment consists of the positions of
      // all the knot points.

      void skipAuxState (DataBuffer data) {
         data.dskip (6*myNumKnots+1);
         data.zskip (myWrappables.size());
      }

      void getAuxState (DataBuffer data) {
         for (int k=0; k<myNumKnots; k++) {
            Point3d pos = myKnots[k].myPos;
            data.dput (pos.x);
            data.dput (pos.y);
            data.dput (pos.z);
            pos = myKnots[k].myLocPos;
            data.dput (pos.x);
            data.dput (pos.y);
            data.dput (pos.z);
         }
         data.dput(myDscale);
         int[] contactCnts = getContactCnts();
         for (int i=0; i<myWrappables.size(); i++) {
            data.zput (contactCnts[i]);
         }
      }

      void getInitialAuxState (DataBuffer newData, DataBuffer oldData) {
         if (oldData == null) {
            getAuxState (newData);
         }
         else {
            newData.putData (oldData, 6*myNumKnots+1, myWrappables.size());
         }
      }

      void setAuxState (DataBuffer data) {
         for (int k=0; k<myNumKnots; k++) {
            Point3d pos = myKnots[k].myPos;
            pos.x = data.dget();
            pos.y = data.dget();
            pos.z = data.dget();
            pos = myKnots[k].myLocPos;
            pos.x = data.dget();
            pos.y = data.dget();
            pos.z = data.dget();
         }
         updateContacts(getContactCnts());
         updateSubSegments();
         myDscale = data.dget();
         int[] contactCnts = getContactCnts();
         for (int i=0; i<myWrappables.size(); i++) {
            contactCnts[i] = data.zget();
         }        
      }

      // End methods to save and restore auxiliary state.

      /**
       * Scan attributes of this wrappable segment from a ReaderTokenizer. Used
       * to implement scanning for the MultiPointSpring.
       */
      boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
         throws IOException {

         rtok.nextToken();
         if (ScanWriteUtils.scanAttributeName (rtok, "knots")) {
            Vector3d[] list = ScanWriteUtils.scanVector3dList (rtok);
            myNumKnots = list.length;
            myKnots = new WrapKnot[myNumKnots];
            for (int i=0; i<list.length; i++) {
               WrapKnot knot = new WrapKnot();
               knot.myPos.set (list[i]);
               myKnots[i] = knot;
            }
            return true;
         }
         else if (ScanWriteUtils.scanAttributeName (rtok, "initialPoints")) {
            Vector3d[] list = ScanWriteUtils.scanVector3dList (rtok);
            myInitialPnts = new Point3d[list.length];
            for (int i=0; i<list.length; i++) {
               myInitialPnts[i] = new Point3d (list[i]);
            }
            return true;
         }
         rtok.pushBack();
         return super.scanItem (rtok, tokens);
      }

      /**
       * Writes attributes of this wrappable segment to a PrintWriter. Used to
       * implement writing for the MultiPointSpring.
       */
      void writeItems (
         PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
         throws IOException {

         super.writeItems (pw, fmt, ancestor);
         if (myNumKnots > 0) {
            pw.print ("knots=");
            Vector3d[] list = new Vector3d[myKnots.length];
            for (int i=0; i<myKnots.length; i++) {
               list[i] = myKnots[i].myPos;
            }
            ScanWriteUtils.writeVector3dList (pw, fmt, list);
         }
         if (myInitialPnts != null) {
            pw.print ("initialPoints=");
            ScanWriteUtils.writeVector3dList (pw, fmt, myInitialPnts);
         }
      }

      /**
       * Applies distance scaling to this wrappable segment.
       */      
      void scaleDistance (double s) {
         for (int k=0; k<myNumKnots; k++) {
            myKnots[k].myPos.scale (s);
         }
      }

      /**
       * Transforms the geometry of this wrappable segment.
       */      
      public void transformGeometry (GeometryTransformer gtr) {
         for (int k=0; k<myNumKnots; k++) {
            gtr.transformPnt (myKnots[k].myPos);
         }
      }

   }

   private void updateABRenderProps() {
      if (myABRenderProps == null) {
         myABRenderProps = new PointRenderProps();
      }
      myABRenderProps.setPointColor (Color.CYAN);
      myABRenderProps.setPointStyle (myRenderProps.getPointStyle());
      myABRenderProps.setPointRadius (myRenderProps.getPointRadius());
   }

   public double getWrapStiffness () {
      return myWrapStiffness;
   }

   public void setWrapStiffness (double stiffness) {
      myWrapStiffness = stiffness;
   }

   public int getMaxWrapIterations () {
      return myMaxWrapIterations;
   }

   public void setMaxWrapIterations (int num) {
      myMaxWrapIterations = num;
   }

   public double getMaxWrapDisplacement () {
      if (myMaxWrapDisplacement == -1) {
         myMaxWrapDisplacement = computeDefaultMaxWrapDisplacement();
      }
      return myMaxWrapDisplacement;
   }

   public void setMaxWrapDisplacement (double d) {
      if (d == -1) {
         d = computeDefaultMaxWrapDisplacement();
      }
      myMaxWrapDisplacement = d;
   }

   private double computeDefaultMaxWrapDisplacement() {
      double mind = Double.POSITIVE_INFINITY;

      CompositeComponent comp =
         ComponentUtils.nearestEncapsulatingAncestor (this);
      if (comp instanceof Renderable) {
         mind = RenderableUtils.getRadius ((Renderable)comp)/10;
      }
      for (Wrappable w : myWrappables) {
         if (w instanceof HasSurfaceMesh) {
            PolygonalMesh mesh = ((HasSurfaceMesh)w).getSurfaceMesh();
            if (mesh != null) {
               OBB obb = new OBB (mesh);
               Vector3d widths = new Vector3d();
               obb.getWidths (widths);
               double hw = widths.minElement()/2;
               if (hw < mind) {
                  mind = hw/2;
               }
            }
         }
      }
      if (mind == Double.POSITIVE_INFINITY) {
         mind = 1.0;
      }
      return mind;
   }

   protected double getDefaultWrapDamping() {
      // for now, just set default wrap damping to 10 X wrap stiffness, so that
      // the stiffness/damping ratio will be 0.1 (and 1 for contact
      // damping). This which should imply relatively fast convergence.
      return 10*myWrapStiffness;
   }

   public double getWrapDamping () {
      if (myWrapDamping < 0) {
         return getDefaultWrapDamping();
      }
      else {
         return myWrapDamping;
      }
   }

   public void setWrapDamping (double damping) {
      myWrapDamping = damping;
   }

   public double getContactStiffness () {
      return myContactStiffness;
   }

   public void setContactStiffness (double stiffness) {
      myContactStiffness = stiffness;
   }

   public double getContactDamping () {
      return myContactDamping;
   }

   public void setContactDamping (double damping) {
      myContactDamping = damping;
   }

   public boolean getContactRescaling () {
      return myContactRescaling;
   }

   public void setContactRescaling (boolean enable) {
      myContactRescaling = enable;
   }

   public double getLengthConvTol () {
      return myLengthConvTol;
   }

   public void setLengthConvTol (double h) {
      myLengthConvTol = h;
   }

   public boolean getDrawKnots () {
      return myDrawKnotsP;
   }

   public void setDrawKnots (boolean enable) {
      myDrawKnotsP = enable;
   }

   public boolean getDrawABPoints () {
      return myDrawABPointsP;
   }

   public void setDrawABPoints (boolean enable) {
      myDrawABPointsP = enable;
   }

   public int getDebugLevel () {
      return myDebugLevel;
   }

   public void setDebugLevel (int level) {
      myDebugLevel = level;
      for (Segment seg : mySegments) {
         if (seg instanceof WrapSegment) {
            ((WrapSegment)seg).debugLevel = level;
         }
      }
   }

   protected double convTol = 1e-6;

   public static boolean myIgnoreCoriolisInJacobian = true;
   public static boolean myDrawWrapPoints = true;

   public static PropertyList myProps =
      new PropertyList (MultiPointSpring.class, PointSpringBase.class);

   static {
      myProps.add (
         "wrapStiffness", "stiffness for wrapping strands",
         DEFAULT_WRAP_STIFFNESS);
      myProps.add (
         "wrapDamping", "damping for wrapping strands",
         DEFAULT_WRAP_DAMPING);
      myProps.add (
         "contactStiffness", "contact stiffness for wrapping strands",
         DEFAULT_CONTACT_STIFFNESS);
      myProps.add (
         "contactDamping", "contact damping for wrapping strands",
         DEFAULT_CONTACT_DAMPING);
      myProps.add (
         "contactRescaling", "contact rescaling for wrapping strands",
         DEFAULT_CONTACT_RESCALING);
      myProps.add (
         "maxWrapIterations", "max number of wrap iterations per step",
         DEFAULT_MAX_WRAP_ITERATIONS);
      myProps.add (
         "drawKnots", "draw wrap strand knots",
         DEFAULT_DRAW_KNOTS);
      myProps.add (
         "drawABPoints", "draw A and B points on wrapping obstacles",
         DEFAULT_DRAW_AB_POINTS);
      myProps.add (
         "sor", "successive overrelaxation parameter", 1.0);
      myProps.add (
         "dnrmGain", "gain for dnrm K term", 1.0);
      myProps.add (
         "debugLevel", "turns on debug prints if > 0", DEFAULT_DEBUG_LEVEL);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public MultiPointSpring() {
      this (null);
   }

   public MultiPointSpring (String name) {
      super (name);
      mySegments = new ArrayList<Segment>();
      myWrappables = new ArrayList<Wrappable>();
      mySolveBlkNums = new int[0];
      myNumBlks = 0;
      mySegsValidP = true;
      // myComponents =
      //    new ComponentListImpl<ModelComponent>(ModelComponent.class, this);
      //myWrapPoints = new PointList<Point> (Point.class, "wrapPoints");
      //addFixed (myWrapPoints);
   }

   public MultiPointSpring (String name, double k, double d, double l) {
      this (name);
      setRestLength (l);
      setMaterial (new LinearAxialMaterial (k, d));
   }

   public MultiPointSpring (double k, double d, double l) {
      this (null);
      setRestLength (l);
      setMaterial (new LinearAxialMaterial (k, d));
   }

   /**
    * Sets the rest length of the spring from the current point locations
    * @return the new rest length
    */
   public double setRestLengthFromPoints() {
      double l = getActiveLength();
      setRestLength(l);
      return l;
   }

   protected int numSegments() {
      // we ignore the last segment because that is used to simply store the
      // terminating point
      return mySegments.size()-1;
   }

   public void setSegmentPassive (int segIdx, boolean passive) {
      if (segIdx >= numSegments()) {
         throw new IndexOutOfBoundsException (
            "Segment "+segIdx+" is not defined");
      }
      Segment seg = mySegments.get(segIdx);
      if (seg.myPassiveP != passive) {
         seg.myPassiveP = passive;
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
      }
   }

   public int numPassiveSegments() {
      int num = 0;
      for (int i=0; i<numSegments(); i++) {
         if (mySegments.get(i).myPassiveP) {
            num++;
         }
      }
      return num;
   }

   public boolean isSegmentPassive (int segIdx) {
      if (segIdx >= numSegments()) {
         throw new IndexOutOfBoundsException (
            "Segment "+segIdx+" is not defined");
      }
      return mySegments.get(segIdx).myPassiveP;
   }

   public int numKnots (int segIdx) {
      if (segIdx >= numSegments()) {
         throw new IndexOutOfBoundsException (
            "Segment "+segIdx+" is not defined");
      }
      Segment seg = mySegments.get(segIdx);
      if (seg instanceof WrapSegment) {
         return ((WrapSegment)seg).myNumKnots;
      }
      else {
         return 0;
      }
   }

   public void checkStiffness (int segIdx) {
      if (segIdx >= numSegments()) {
         throw new IndexOutOfBoundsException (
            "Segment "+segIdx+" is not defined");
      }
      Segment seg = mySegments.get(segIdx);
      if (seg instanceof WrapSegment) {
         WrapSegment wseg = (WrapSegment)seg;
         wseg.checkStiffness();
      }
   }

   public WrapKnot getKnot (int segIdx, int k) {
      if (segIdx >= numSegments()) {
         throw new IndexOutOfBoundsException (
            "Segment "+segIdx+" is not defined");
      }
      Segment seg = mySegments.get(segIdx);
      if (seg instanceof WrapSegment) {
         WrapSegment wseg = (WrapSegment)seg;
         if (k >= 0 && k < wseg.myNumKnots) {
            return wseg.myKnots[k];
         }
      }
      return null;
   }
   
   protected void invalidateSegments() {
      mySegsValidP = false;
      myRenderObjValidP = false;
   }

   protected void updateSegsIfNecessary() {
      if (!mySegsValidP) {
         updateSegs(/*updateWrapSegs=*/false);
      }
   }

   protected void updateSegs (boolean updateWrapSegs) {
      int nump = numPoints();
      myNumBlks = nump;
      mySolveBlkNums = new int[myNumBlks*myNumBlks];
      mySegsValidP = true;
      // make sure segment pointers are correct
      for (int i=0; i<nump-1; i++) {
         Segment seg = mySegments.get(i);
         Segment segNext = mySegments.get(i+1);
         if (seg.myPntA != segNext.myPntB) {
            // then this segment was changed
            seg.myPntA = segNext.myPntB;
            if (updateWrapSegs && seg instanceof WrapSegment) {
               WrapSegment wrapSeg = (WrapSegment)seg;
               wrapSeg.initializeStrand (/*initialPnts=*/null);
               wrapSeg.updateWrapStrand(myMaxWrapIterations);
               wrapSeg.updateSubSegments();
            }
         }
      }
      mySegments.get(nump-1).myPntA = null;
   }

   protected void updateWrapSegments (int maxIter) {
      for (int i=0; i<numSegments(); i++) {
         Segment seg = mySegments.get(i);
         if (seg instanceof WrapSegment) {
            WrapSegment wrapSeg = (WrapSegment)seg;
            wrapSeg.updateWrapStrand(maxIter);
            wrapSeg.updateSubSegments();
         }
      }
   }      

   // /**
   //  * Calculates the sum of distances between the entry and exit points of the
   //  * two paths.
   //  * @param p1         first path
   //  * @param p2         second path
   //  * @return           "distance" between the two paths
   //  */
   // private double pathDistance (WrapPath path1, WrapPath path2) {
   //    WrapPoint pnt1_0 = path1.getPoint(0);
   //    WrapPoint pnt1_1 = path1.numPoints() > 1 ? path1.getPoint(1) : pnt1_0;
   //    WrapPoint pnt2_0 = path2.getPoint(0);
   //    WrapPoint pnt2_1 = path2.numPoints() > 1 ? path2.getPoint(1) : pnt2_0;
   //    return (pnt1_0.getPosition().distanceSquared(pnt2_0.getPosition()) +
   //            pnt1_1.getPosition().distanceSquared(pnt2_1.getPosition()));
   // }

   // private Point nextDefinedPointA (int idx) {
   //    WrapData wdata = myWrapData.get(idx++);
   //    while (wdata.wrappable != null && !wdata.pointsDefined) {
   //       wdata = myWrapData.get(idx++);
   //    }
   //    return wdata.pntA;
   // }

   public void applyForces (double t) {
      updateSegsIfNecessary();
      double len = getActiveLength();
      double dldt = getActiveLengthDot();
      double F = computeF (len, dldt);
      for (int i=0; i<numSegments(); i++) {
         Segment seg = mySegments.get(i);
         if (seg.hasSubSegments()) {
            for (SubSegment sg=seg.firstSubSegment(); sg!=null; sg=sg.myNext) {
               sg.applyForce (F);
            }
         }
         else {
            seg.applyForce (F);
         }
      }
   }

   protected void writeSegments (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor) 
      throws IOException {
      
      pw.println ("segments=[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (int i=0; i<mySegments.size(); i++) {
         Segment seg = mySegments.get(i);
         if (seg instanceof WrapSegment) {
            pw.println ("WrapSegment [");
         }
         else {
            pw.println ("Segment [");
         }
         IndentingPrintWriter.addIndentation (pw, 2);
         seg.writeItems (pw, fmt, ancestor);
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");      
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");      
   }

   protected void scanSegments (ReaderTokenizer rtok, Deque<ScanToken> tokens) 
      throws IOException {
      tokens.offer (new StringToken ("segments", rtok.lineno()));
      rtok.scanToken ('[');
      tokens.offer (ScanToken.BEGIN);
      while (rtok.nextToken() != ']') {
         Segment seg;
         if (rtok.tokenIsWord ("Segment")) {
            seg = new Segment();
         }
         else if (rtok.tokenIsWord ("WrapSegment")) {
            seg = new WrapSegment();            
         }
         else {
            throw new IOException (
               "Expecting word token, Segment or WrapSegment, " + rtok); 
         }
         rtok.scanToken ('[');
         tokens.offer (ScanToken.BEGIN);
         while (rtok.nextToken() != ']') {
            rtok.pushBack();
            if (!seg.scanItem (rtok, tokens)) {
               throw new IOException ("Unexpected token: "+rtok);
            }
         }
         tokens.offer (ScanToken.END); // terminator token
         mySegments.add (seg);
      }
      tokens.offer (ScanToken.END);
   }

   protected void postscanSegments (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      ScanToken tok = tokens.poll();
      if (tok != ScanToken.BEGIN) {
         throw new IOException (
            "BEGIN token expected for segment list, got " + tok);
      }
      for (int i=0; i<mySegments.size(); i++) {
         Segment seg = mySegments.get(i);
         tok = tokens.poll();
         if (tok != ScanToken.BEGIN) {
            throw new IOException (
               "BEGIN token expected for segment "+i+", got " + tok);
         }
         while (tokens.peek() != ScanToken.END) {
            if (!seg.postscanItem (tokens, ancestor)) {
               throw new IOException (
                  "Unexpected token for segment "+i+": " + tokens.poll());
            }
         }
         tokens.poll(); // eat END token      
      }
      tok = tokens.poll();
      if (tok != ScanToken.END) {
         throw new IOException (
            "END token expected for segment list, got " + tok);
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "segments")) {
         scanSegments (rtok, tokens);
         return true;
      }
      else if (scanAndStoreReferences (rtok, "wrappables", tokens) != -1) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }  

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      clearPoints();
      clearWrappables();
      super.scan (rtok, ref);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      if (postscanAttributeName (tokens, "segments")) {
         postscanSegments (tokens, ancestor);
         return true;
      }
      else if (postscanAttributeName (tokens, "wrappables")) {
         ScanWriteUtils.postscanReferences (
            tokens, myWrappables, Wrappable.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.print ("wrappables=");
      ScanWriteUtils.writeBracketedReferences (pw, myWrappables, ancestor);
      writeSegments (pw, fmt, ancestor);
      super.writeItems (pw, fmt, ancestor);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      // just update bounds for the via points, since the wrap segments will
      // hug the wrappables, and bounds are updated elsewhere to account for
      // wrappables.
      for (int i=0; i<numPoints(); i++) {
         getPoint(i).updateBounds (pmin, pmax);
      }
   }

   private float[] getRenderCoords (Point pnt) {
      Point3d pos = pnt.getPosition();
      return new float[] { (float)pos.x, (float)pos.y, (float)pos.z };
   }

   private void addRenderPos (RenderObject robj, float[] xyz, boolean isKnot) {
      int vidx = robj.vertex(xyz);
      if (isKnot) {
         robj.addPoint (vidx);
      }
      if (vidx > 0) {
         robj.addLine (vidx-1, vidx);
      }
   }

   protected RenderObject buildRenderObject() {
      RenderObject robj = new RenderObject();
      for (int i=0; i<numSegments(); i++) {
         Segment seg = mySegments.get(i);
         addRenderPos (robj, seg.myPntB.getRenderCoords(), /*knot=*/false);
         if (seg instanceof WrapSegment) {
            WrapSegment wrapSeg = (WrapSegment)seg;
            for (int k=0; k<wrapSeg.myNumKnots; k++) {
               addRenderPos (
                  robj, wrapSeg.myKnots[k].updateRenderPos(), /*knot=*/true);
            }
         }
         if (i == numSegments()-1) {
            addRenderPos (robj, seg.myPntA.getRenderCoords(), /*knot=*/false);
         }
      }
      return robj;
   }

   protected void updateRenderObject (RenderObject robj) {
      // updating the render object involves updating the knot render positions
      for (int i=0; i<numSegments(); i++) {
         Segment seg = mySegments.get(i);
         if (seg instanceof WrapSegment) {
            WrapSegment wrapSeg = (WrapSegment)seg;
            for (int k=0; k<wrapSeg.myNumKnots; k++) {
               wrapSeg.myKnots[k].updateRenderPos();
            }
         }
      }
      robj.notifyPositionsModified();
   }

   public void prerender (RenderList list) {
      // A render object is used to render the strands and the knots.  AB
      // points are rendered using basic point primitives on a per-segment list
      // of current AB points.


      // create or update the render object, as needed
      if (!myRenderObjValidP) {
         myRenderObj = buildRenderObject();
         myRenderObjValidP = true;
      }
      else {
         updateRenderObject(myRenderObj);
      }

      if (myDrawABPointsP) {
         // for each wrappable segment, update the current list of AB points to
         // be rendered:
         updateABRenderProps();
         for (int i=0; i<numSegments(); i++) {
            Segment seg = mySegments.get(i);
            if (seg instanceof WrapSegment) {
               WrapSegment wrapSeg = (WrapSegment)seg;
               ArrayList<float[]> renderPoints = null;
               SubSegment sg = wrapSeg.firstSubSegment();
               if (sg != null) {
                  renderPoints = new ArrayList<float[]>(10);
                  while (sg!=null) {
                     if (sg.myAttachmentB != null) {
                        renderPoints.add (getRenderCoords (sg.myPntB));
                     }
                     if (sg.myAttachmentA != null) {
                        renderPoints.add (getRenderCoords (sg.myPntA));
                     }
                     sg = sg.myNext;
                  }
               }
               wrapSeg.myRenderABPoints = renderPoints;
            }
         }
      }
   }

   void dorender (Renderer renderer, RenderProps props) {
      RenderObject robj = myRenderObj;

      if (myDrawABPointsP) {
         // draw AB points
         for (int i=0; i<numSegments(); i++) {
            Segment seg = mySegments.get(i);
            if (seg instanceof WrapSegment) {
               WrapSegment wrapSeg = (WrapSegment)seg;
               ArrayList<float[]> renderPoints = wrapSeg.myRenderABPoints;
               if (renderPoints != null) {
                  for (int k=0; k<renderPoints.size(); k++) {
                     renderer.drawPoint (
                        myABRenderProps, renderPoints.get(k), isSelected());
                  }
               }
            }
         }
      }
      
      if (robj != null) {
         double size;

         // draw the strands
         LineStyle lineStyle = props.getLineStyle();
         if (lineStyle == LineStyle.LINE) {
            size = props.getLineWidth();
         }
         else {
            size = props.getLineRadius();
         }
         if (getRenderColor() != null) {
            renderer.setColor (getRenderColor(), isSelected());
         }
         else {
            renderer.setLineColoring (props, isSelected());
         }
         renderer.drawLines (robj, lineStyle, size);

         if (myDrawKnotsP) {
            // draw the knots, if any
            if (robj.numPoints() > 0) {
               PointStyle pointStyle = props.getPointStyle();
               if (pointStyle == PointStyle.POINT) {
                  size = props.getPointSize();
               }
               else {
                  size = props.getPointRadius();
               }
               renderer.setPointColoring (props, isSelected());
               renderer.drawPoints (robj, pointStyle, size);
            }
         }
      }
   }     

   public void render (Renderer renderer, int flags) {
      dorender (renderer, myRenderProps);
   }

   protected MultiPointSpring newComponent (String classId)
      throws InstantiationException, IllegalAccessException {
      return (MultiPointSpring)ClassAliases.newInstance (
         classId, MultiPointSpring.class);
   }

   public void scaleDistance (double s) {
      super.scaleDistance (s);
      if (myMaterial != null) {
         myMaterial.scaleDistance (s);
      }
      if (myRenderProps != null) {
         RenderableUtils.cloneRenderProps (this);
         myRenderProps.scaleDistance (s);
      }
      for (int i=0; i<numSegments(); i++) {
         mySegments.get(i).scaleDistance (s);
      }
   }

   public void scaleMass (double s) {
      super.scaleMass (s);
      if (myMaterial != null) {
         myMaterial.scaleMass (s);
      }
   }

   public void transformGeometry (AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      // just transform the segments. Via points and wrappables will be
      // transformed elsewhere      
      for (int i=0; i<numSegments(); i++) {
         mySegments.get(i).transformGeometry (gtr);
      }
   }
   
   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      // no dependencies
   }

   public double computeLength (boolean activeOnly) {
      double len = 0;
      updateSegsIfNecessary();
      for (int i=0; i<numSegments(); i++) {
         Segment seg = mySegments.get(i);
         
         if (seg.hasSubSegments()) {
            for (SubSegment sg=seg.firstSubSegment(); sg!=null; sg=sg.myNext) {
               sg.updateU();
            }
         }
         else {
            seg.updateU();
         }
         if (activeOnly && seg.myPassiveP) {
            continue;
         }
         len += seg.myLength;
      }
      return len;
   }

   public double getActiveLengthDot() {
      return computeLengthDot (/*activeOnly=*/true);
   }

   public double getLengthDot() {
      return computeLengthDot (/*activeOnly=*/false);
   }

   private double computeLengthDot (boolean activeOnly) {
      double lenDot = 0;
      updateSegsIfNecessary();
      for (int i=0; i<numSegments(); i++) {
         Segment seg = mySegments.get(i);
         if (activeOnly && seg.myPassiveP) {
            continue;
         }
         // TODO: need to make sure uvec is up to for the segments
         if (seg.hasSubSegments()) {
            for (SubSegment sg=seg.firstSubSegment(); sg!=null; sg=sg.myNext) {
               lenDot += sg.getLengthDot();
            }
         }
         else {
            lenDot += seg.getLengthDot();         
         }
      }
      return lenDot;
   }

   public double getActiveLength() {
      return computeLength (/*activeOnly=*/true);
   }         

   public double getLength() {
      return computeLength (/*activeOnly=*/false);
   }         

   protected double computeU (Vector3d u, Point p0, Point p1) {
      u.sub (p1.getPosition(), p0.getPosition());
      double l = u.norm();
      if (l != 0) {
         u.scale (1 / l);
      }
      return l;
   }

   public void addSolveBlocks (SparseNumberedBlockMatrix M) {
      updateSegsIfNecessary();
      // TODO: FINISH - currently adds solve blocks only for the
      // via points
      int nump = numPoints();
      if (nump > 1) {
         for (int i=0; i<nump; i++) {
            int bi = getPoint(i).getSolveIndex();
            for (int j=0; j<nump; j++) {
               int bj = getPoint(j).getSolveIndex();
               MatrixBlock blk = null;
               if (bi != -1 && bj != -1) {
                  blk = M.getBlock (bi, bj);
                  if (blk == null) {
                     blk = MatrixBlockBase.alloc (3, 3);
                     M.addBlock (bi, bj, blk);
                  }
               }
               if (blk != null) {
                  mySolveBlkNums[i*nump+j] = blk.getBlockNumber();
               }
               else {
                  mySolveBlkNums[i*nump+j] = -1;
               }
            }
         }
      }
   }

   // assume that PointData v vectors and segment P and u is up to date
   private void updateDfdx (double dFdl, double dFdldot) {
      for (int i=0; i<numSegments(); i++) {
         Segment seg = mySegments.get(i);
         if (seg.hasSubSegments()) {
            for (SubSegment sg=seg.firstSubSegment(); sg!=null; sg=sg.myNext) {
               sg.updateDfdx (dFdl, dFdldot);
            }
         }
         else {
            seg.updateDfdx (dFdl, dFdldot);
         }
      }
   }

   private void updateP () {
      for (int i=0; i<numSegments(); i++) {
         Segment seg = mySegments.get(i);
         if (seg.hasSubSegments()) {
            for (SubSegment sg=seg.firstSubSegment(); sg!=null; sg=sg.myNext) {
               sg.updateP();
            }
         }
         else {
            seg.updateP();
         }
      }
   }

   private MatrixBlock getSolveBlock (
      SparseNumberedBlockMatrix M, int i, int j, int numBlks) {
      int blkNum = mySolveBlkNums[i*myNumBlks+j];
      if (blkNum != -1) {
         return M.getBlockByNumber (blkNum);
      }
      else {
         return null;
      }
   }

   private void addToBlock (
      SparseNumberedBlockMatrix M, int bi, int bj, Matrix3d X, double s) {

      int blkNum = mySolveBlkNums[bi*myNumBlks+bj];
      if (blkNum != -1) {
         MatrixBlock blk =  M.getBlockByNumber (blkNum);
         blk.scaledAdd (s, X);
      }
   }

   public void addPosJacobian (SparseNumberedBlockMatrix M, double s) {
      // TODO: FINISH. currently computes Jacobian only for via points

      Matrix3d X = new Matrix3d();
      MatrixBlock blk = null;
      int numSegs = numSegments();
      updateSegsIfNecessary();
      if (numSegs > 0) {
         double l = getActiveLength();
         double ldot = getActiveLengthDot();
         double F = computeF (l, ldot);
         double dFdl = computeDFdl (l, ldot);
         double dFdldot = computeDFdldot (l, ldot);
         updateP();
         updateDfdx (dFdl, dFdldot);

         for (int i=0; i<numSegs; i++) {
            Segment seg_i = mySegments.get(i);
            Vector3d uvecB_i, uvecA_i;
            if (seg_i.hasSubSegments()) {
               Segment sub;
               sub = seg_i.firstSubSegment();
               X.scale (F/sub.myLength, sub.myP);
               addToBlock (M, i, i, X, -s);
               uvecB_i = sub.myUvec;
               sub = seg_i.lastSubSegment();
               X.scale (F/sub.myLength, sub.myP);
               addToBlock (M, i+1, i+1, X, -s);
               uvecA_i = sub.myUvec;
            }
            else {
               X.scale (F/seg_i.myLength, seg_i.myP);
               addToBlock (M, i, i, X, -s);
               addToBlock (M, i+1, i, X, s);
               addToBlock (M, i, i+1, X, s);
               addToBlock (M, i+1, i+1, X, -s);
               uvecB_i = seg_i.myUvec;
               uvecA_i = seg_i.myUvec;
            }
            for (int j=0; j<numSegs; j++) {
               Segment seg_j = mySegments.get(j);
               Vector3d dFdxB_j, dFdxA_j; 
               if (seg_j.hasSubSegments()) {
                  dFdxB_j = seg_j.firstSubSegment().mydFdxB;
                  dFdxA_j = seg_j.lastSubSegment().mydFdxA;
               }
               else {
                  dFdxB_j = seg_j.mydFdxB;
                  dFdxA_j = seg_j.mydFdxA;
               }
               X.outerProduct (uvecB_i, dFdxB_j);
               addToBlock (M, i, j, X, s);
               addToBlock (M, i+1, j, X, -s);
               X.outerProduct (uvecA_i, dFdxA_j);
               addToBlock (M, i, j+1, X, s);
               addToBlock (M, i+1, j+1, X, -s);
            }
         }
      }
   }

   public void addVelJacobian (SparseNumberedBlockMatrix M, double s) {
      // TODO: FINISH. currently computes Jacobian only for via points
      Matrix3d X = new Matrix3d();
      Vector3d tmp = new Vector3d();
      Vector3d dLdot = new Vector3d();
      int numSegs = numSegments();
      updateSegsIfNecessary();
      if (numSegs > 0) {
         double l = getActiveLength();
         double ldot = getActiveLengthDot();
         double dFdldot = computeDFdldot (l, ldot);
         for (int i=0; i<numSegs; i++) {
            Segment seg_i = mySegments.get(i);
            Vector3d uvecB_i, uvecA_i;
            if (seg_i.hasSubSegments()) {
               uvecB_i = seg_i.firstSubSegment().myUvec;
               uvecA_i = seg_i.lastSubSegment().myUvec;
            }
            else {
               uvecB_i = seg_i.myUvec;
               uvecA_i = seg_i.myUvec;
            }
            for (int j=0; j<numSegs; j++) {
               Segment seg_j = mySegments.get(j);
               Vector3d uvecB_j, uvecA_j;
               if (!seg_j.myPassiveP) {
                  if (seg_j.hasSubSegments()) {
                     uvecB_j = seg_j.firstSubSegment().myUvec;
                     uvecA_j = seg_j.lastSubSegment().myUvec;
                  }
                  else {
                     uvecB_j = seg_j.myUvec;
                     uvecA_j = seg_j.myUvec;
                  }
                  X.outerProduct (uvecB_i, uvecB_j);
                  addToBlock (M, i, j, X, -s*dFdldot);
                  if (uvecB_j != uvecA_j) {
                     X.outerProduct (uvecB_i, uvecA_j);
                  }
                  addToBlock (M, i, j+1, X, s*dFdldot);                  
                  X.outerProduct (uvecA_i, uvecB_j);
                  addToBlock (M, i+1, j, X, s*dFdldot);
                  if (uvecB_j != uvecA_j) {
                     X.outerProduct (uvecA_i, uvecA_j);
                  }
                  addToBlock (M, i+1, j+1, X, -s*dFdldot);
               }
            }
         }
      }
   }

   public int getJacobianType() {
      AxialMaterial mat = getEffectiveMaterial();
      if (numPassiveSegments() == 0 &&
          (myIgnoreCoriolisInJacobian || mat.isDFdldotZero())) {
         return Matrix.SYMMETRIC;
      }
      else {
         return 0;
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean isDuplicatable() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      // copying not currently supported
      return false;
//      for (int i=0; i<myWrapData.size(); i++) {
//         ModelComponent wobj = getWrapObject(i);
//         if (!(wobj instanceof CopyableComponent) ||
//             !ComponentUtils.addCopyReferences (refs, wobj, ancestor)) {
//            return false;
//         }
//      }
//      return true;
   }

   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      MultiPointSpring comp = (MultiPointSpring)super.copy (flags, copyMap);

      // copying not currently supported. This method must be completed
      // if that changes.
//      comp.mySegData = new ArrayList<SegmentData>();
//      comp.myWrapData = new ArrayList<WrapData>();
//      for (int i=0; i<myWrapData.size(); i++) {
//         ModelComponent wobj = (ModelComponent)ComponentUtils.maybeCopy (
//            flags, copyMap, (CopyableComponent)getWrapObject(i));
//         if (wobj instanceof Wrappable) {
//            WrapData wdata = myWrapData.get(i);
//            comp.addWrappable (
//               (Wrappable)wobj,
//               wdata.pntA.getPosition(), wdata.pntA.getPosition());
//         }
//         else if (wobj instanceof Point) {
//            comp.addPoint ((Point)wobj);
//         }
//         else {
//            throw new InternalErrorException (
//               "Unknown wrap object "+wobj.getClass());
//         }
//      }
//      comp.setRenderProps (myRenderProps);
//      if (myMaterial != null) {
//         comp.setMaterial (myMaterial.clone());
//      }
      // if (myPassiveSegs != null) {
      //    comp.myPassiveSegs = new HashSet<Integer>();
      //    for (Integer ival : myPassiveSegs) {
      //       comp.myPassiveSegs.add (ival);
      //    }
      // }
      return comp;
   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      int nump = numPoints();
      if (nump > 0) {
         refs.add (getPoint(0));
      }
      if (nump > 1) {
         refs.add (getPoint(nump-1));
      }
   }

   @Override
   public void getSoftReferences (List<ModelComponent> refs) {
      super.getSoftReferences (refs);
      int nump = numPoints();
      if (nump > 2) {
         for (int i=1; i<nump-1; i++) {
            refs.add (getPoint(i));
         }
      }
      refs.addAll (myWrappables);
   }

   @Override
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      super.updateReferences (undo, undoInfo);

      if (undo) {
         Object obj = undoInfo.removeFirst();
         if (obj != NULL_OBJ) {
            ((ListRemove<Wrappable>)obj).undo();
         }
         obj = undoInfo.removeFirst();
         if (obj != NULL_OBJ) {
            ((ListRemove<Segment>)obj).undo();
            updateSegs(/*updateWrapSegs=*/false);
         }
      }
      else {
         // remove soft references which aren't in the hierarchy any more:
         ListRemove<Wrappable> wrappableRemove = null;
         for (int i=0; i<myWrappables.size(); i++) {
            if (!ComponentUtils.isConnected (
                   this, myWrappables.get(i))) {
               if (wrappableRemove == null) {
                  wrappableRemove = new ListRemove<Wrappable>(myWrappables);
               }
               wrappableRemove.requestRemove(i);
            }
         }
         if (wrappableRemove != null) {
            wrappableRemove.remove();
            undoInfo.addLast (wrappableRemove);
         }
         else {
            undoInfo.addLast (NULL_OBJ);
         }
         ListRemove<Segment> segmentRemove = null;
         for (int i=1; i<mySegments.size(); i++) {
            if (!ComponentUtils.isConnected (this, mySegments.get(i).myPntB)) {
               if (segmentRemove == null) {
                  segmentRemove = new ListRemove<Segment>(mySegments);
               }
               segmentRemove.requestRemove(i);
            }
         }
         if (segmentRemove != null) {
            segmentRemove.remove();
            undoInfo.addLast (segmentRemove);
            myRenderObjValidP = false;
            updateSegs(/*updateWrapSegs=*/true);
            // remove render object
         }
         else {
            undoInfo.addLast (NULL_OBJ);
         }         
      }
   }

   public void preadvance (double t0, double t1, int flags) {
      updateWrapSegments(myMaxWrapIterations);
   }
   
   public void postadvance (double t0, double t1, int flags) {
      updateStructure();
   }

   /** 
    * Hook method to allow sub-classes to update their structure by adding
    * or removing points.
    */
   public void updateStructure() {
   }

   // ///////////////////////////////////////////////////
   // // Begin Composite component stuff
   // ///////////////////////////////////////////////////

   // /**
   //  * {@inheritDoc}
   //  */
   // public void updateNameMap (
   //    String newName, String oldName, ModelComponent comp) {
   //    myComponents.updateNameMap (newName, oldName, comp);
   // }

   // /**
   //  * {@inheritDoc}
   //  */
   // public ModelComponent findComponent (String path) {
   //    return ComponentUtils.findComponent (this, path);
   // }

   // protected void addFixed (ModelComponent comp) {
   //    comp.setFixed (true);
   //    myComponents.add (comp);
   // }
 
   // /**
   //  * {@inheritDoc}
   //  */
   // public ModelComponent get (String nameOrNumber) {
   //    return myComponents.get (nameOrNumber);
   // }

   // /**
   //  * {@inheritDoc}
   //  */
   // public ModelComponent get (int idx) {
   //    return myComponents.get (idx);
   // }

   // /**
   //  * {@inheritDoc}
   //  */
   // public ModelComponent getByNumber (int num) {
   //    return myComponents.getByNumber (num);
   // }

   // /**
   //  * {@inheritDoc}
   //  */
   // public int getNumberLimit() {
   //    return myComponents.getNumberLimit();
   // }

   // /**
   //  * {@inheritDoc}
   //  */
   // public int indexOf (ModelComponent comp) {
   //    return myComponents.indexOf (comp);
   // }

   // /**
   //  * {@inheritDoc}
   //  */
   // public int numComponents() {
   //    return myComponents.size();
   // }

   // /**
   //  * {@inheritDoc}
   //  */
   // public void componentChanged (ComponentChangeEvent e) {
   //    myComponents.componentChanged (e);
   //    notifyParentOfChange (e);
   // }

   // protected void notifyStructureChanged (Object comp) {
   //    if (comp instanceof CompositeComponent) {
   //       notifyParentOfChange (new StructureChangeEvent (
   //          (CompositeComponent)comp));
   //    }
   //    else {
   //       notifyParentOfChange (StructureChangeEvent.defaultEvent);
   //    }
   // }

   // /**
   //  * {@inheritDoc}
   //  */
   // public NavpanelDisplay getNavpanelDisplay() {
   //    return myDisplayMode;
   // }
   
   // /**
   //  * Sets the display mode for this component. This controls
   //  * how the component is displayed in a navigation panel. The default
   //  * setting is <code>NORMAL</code>.
   //  *
   //  * @param mode new display mode
   //  */
   // public void setDisplayMode (NavpanelDisplay mode) {
   //    myDisplayMode = mode;
   // }

   // /**
   //  * {@inheritDoc}
   //  */
   // public boolean hierarchyContainsReferences() {
   //    return false;
   // }

   // ///////////////////////////////////////////////////
   // // End Composite component stuff
   // ///////////////////////////////////////////////////

   // public void getAttachments (List<DynamicAttachment> list) {
   //    for (int i=0; i<myWrapData.size(); i++) {
   //       WrapData wdata = myWrapData.get(i);
   //       if (wdata.wrappable != null) {
   //          list.add (wdata.attachmentA);
   //          list.add (wdata.attachmentB);
   //       }
   //    }
   // }

   public void updateSlavePos() {
   }

   public void updateSlaveVel() {
   }

   @Override
   public void advanceAuxState (double t0, double t1) {
      // nothing needed here
   }

   @Override
   public void skipAuxState (DataBuffer data) {
      for (int i=0; i<numSegments(); i++) {
         Segment seg = mySegments.get(i);
         if (seg instanceof WrapSegment) {
            ((WrapSegment)seg).skipAuxState (data);
         }
      }
   }

   @Override
   public void getAuxState (DataBuffer data) {
      for (int i=0; i<numSegments(); i++) {
         Segment seg = mySegments.get(i);
         if (seg instanceof WrapSegment) {
            ((WrapSegment)seg).getAuxState (data);
         }
      }
   }

   @Override
   public void getInitialAuxState (DataBuffer newData, DataBuffer oldData) {
      for (int i=0; i<numSegments(); i++) {
         Segment seg = mySegments.get(i);
         if (seg instanceof WrapSegment) {
            ((WrapSegment)seg).getInitialAuxState (newData, oldData);
         }
      }
   }

   @Override
   public void setAuxState (DataBuffer data) {
      for (int i=0; i<numSegments(); i++) {
         Segment seg = mySegments.get(i);
         if (seg instanceof WrapSegment) {
            ((WrapSegment)seg).setAuxState (data);
         }
      }
   }

   public void addPoint (int idx, Point pnt) {
      if (idx > mySegments.size()) {
         throw new ArrayIndexOutOfBoundsException (
            "specified index "+idx+
            " exceeds number of points "+mySegments.size());
      }
      Segment seg = new Segment();
      seg.myPntB = pnt;
      if (idx > 0) {
         Segment prev = mySegments.get(idx-1);
         prev.myPntA = pnt;
         if (idx == numPoints() && prev instanceof WrapSegment) {
            WrapSegment wrapSeg = (WrapSegment)prev;
            wrapSeg.initializeStrand (wrapSeg.myInitialPnts);
         }
      }
      if (idx < mySegments.size()-1) {
         seg.myPntA = mySegments.get(idx+1).myPntB;
      }
      mySegments.add (idx, seg);
      invalidateSegments();
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
   }

   public void addPoint (Point pnt) {
      addPoint (numPoints(), pnt);
   }

   public Point getPoint (int idx) {
      if (idx < 0 || idx >= numPoints()) {
         throw new IndexOutOfBoundsException (
            "idx=" + idx + ", number of points=" + numPoints());
      }
      return mySegments.get(idx).myPntB;
   }

   public int numPoints() {
      return mySegments.size();
   }

   public Segment getSegment(int idx) {
      return mySegments.get(idx);
   }

   public int indexOfPoint (Point pnt) {
      for (int i=0; i<mySegments.size(); i++) {
         Segment seg = mySegments.get(i);
         if (seg.myPntB == pnt) {
            return i;
         }
      }
      return -1;
   }

   public boolean containsPoint (Point pnt) {
      return indexOfPoint (pnt) != -1;
   }

   public void setPoint (Point pnt, int idx) {
      int nump = numPoints();
      if (idx < 0 || idx >= nump) {
         throw new IndexOutOfBoundsException (
            "idx=" + idx + ", number of points=" + nump);
      }
      if (idx == nump) {
         addPoint (pnt);
      }
      else {
         if (idx > 0) {
            Segment prev = mySegments.get(idx-1);
            prev.myPntA = pnt;
         }
      }
      invalidateSegments();
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
   }

   public boolean removePoint (Point pnt) {
      int idx = indexOfPoint (pnt);
      if (idx != -1) {      
         if (idx > 0) {
            mySegments.get(idx-1).myPntA = mySegments.get(idx).myPntA;
         }
         mySegments.remove (idx);
         invalidateSegments();
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
         return true;
      }
      else {
         return false;
      }
   }

   public void clearPoints() {
      mySegments.clear();
      invalidateSegments();
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
   }

   public void setSegmentWrappable (int numk) {
      setSegmentWrappable (numk, null);
   }

   public void setSegmentWrappable (int numk, Point3d[] initialPnts) {
      if (numPoints() == 0) {
         throw new IllegalStateException (
            "setSegmentWrappable() called before first call to addPoint()");
      }
      WrapSegment seg = new WrapSegment(numk, initialPnts);
      seg.myPntB = mySegments.get(mySegments.size()-1).myPntB;
      mySegments.set (mySegments.size()-1, seg);
      myRenderObjValidP = false;
   }

   public void initializeSegment (int segIdx, Point3d[] initialPnts) {
      if (segIdx >= mySegments.size()) {
         throw new IllegalArgumentException (
            "Segment "+segIdx+" does not exist");
      }
      Segment seg = mySegments.get (segIdx);
      if (seg instanceof WrapSegment) {
         ((WrapSegment)seg).initializeStrand (initialPnts);
      }
   }

   public void setKnotPositions (int segIdx, Point3d[] plist) {
      if (segIdx >= mySegments.size()) {
         throw new IllegalArgumentException (
            "Segment "+segIdx+" does not exist");
      }
      Segment seg = mySegments.get (segIdx);
      if (seg instanceof WrapSegment) {
         ((WrapSegment)seg).setKnotPositions (plist);
      }
      else {
         throw new IllegalArgumentException (
            "Segment "+segIdx+" is not a wrappable segment");
      }
   }

   public void addWrappable (Wrappable wrappable) {
      if (!myWrappables.contains(wrappable)) {
         myWrappables.add (wrappable);
         invalidateSegments();
      }        
   }

   public boolean containsWrappable (Wrappable wrappable) {
      return indexOfWrappable (wrappable) != -1;
   }

   public int numWrappables() {
      return myWrappables.size();
   }

   public int indexOfWrappable (Wrappable wrappable) {
      return myWrappables.indexOf (wrappable);
   }

   public Wrappable getWrappable (int idx) {
      if (idx < 0 || idx >= myWrappables.size()) {
         throw new IndexOutOfBoundsException (
            "idx=" + idx + ", num wrappables=" + numWrappables());
      }
      return myWrappables.get(idx);
   }

   public boolean removeWrappable (Wrappable wrappable) {
      if (myWrappables.remove (wrappable)) {
         invalidateSegments();
         return true;
      }
      else {
         return false;
      }
   }

   public void clearWrappables() {
      myWrappables.clear();
      invalidateSegments();
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
   }

   /**
    * Applies one iteration of the wrap segment updating method.
    */
   public void updateWrapSegments() {
      updateWrapSegments(myMaxWrapIterations);
   }

   /**
    * Returns all the AB points which are currently active on the
    * segments. This should be called in sync with the simulation, since the
    * set of AB points varies across time steps.
    *
    * @param pnts returns the AB points. Will be cleared at the start
    * of the method.
    * @return number of AB points found
    */
   public int getAllABPoints (ArrayList<Point> pnts) {
      pnts.clear();
      for (int i = 0; i < numSegments (); i++) {
         Segment seg = mySegments.get (i);
         if (seg instanceof WrapSegment) {
            WrapSegment wrapSeg = (WrapSegment)seg;
            SubSegment sg = wrapSeg.firstSubSegment ();
            while (sg != null) {
               if (sg.myAttachmentB != null) {
                  pnts.add (sg.myPntB);
               }
               if (sg.myAttachmentA != null) {
                  pnts.add (sg.myPntA);
               }
               sg = sg.myNext;
            }
         }
      }
      return pnts.size();
   }

   // DONE: add an initialize() method
   // DONE: computing tangent with just p1 can get the wrong answer if
   //       knot density is low

   // DONE: notifyStructureChanged() - do we need this?
   // DONE: what to do with transform geometry?
   // DONE: what to do with scale distance?
   // DONE: save and restore state
   // TODO: FINISH add soft references

   // TODO: finish parameter interface for wrap segments. Do we only need k/d?
   // What is the optimal ratio for a given number of knots? Do we need
   // separate stiffnesses for spring and pentration?

   // TODO: FINISH Jacobian stuff

}

