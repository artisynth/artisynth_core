/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import maspack.matrix.Matrix;
import maspack.matrix.Matrix3d;
import maspack.matrix.MatrixBlock;
import maspack.matrix.MatrixBlockBase;
import maspack.matrix.Point3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.GLRenderer;
import maspack.render.RenderProps;
import maspack.render.RenderableUtils;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.materials.AxialMaterial;
import artisynth.core.materials.LinearAxialMaterial;
import artisynth.core.materials.LinearAxialMuscle;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CompositeComponentBase;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.util.*;

public class MultiPointSpring extends PointSpringBase
   implements ScalableUnits, CopyableComponent, RequiresPrePostAdvance {

   protected ArrayList<PointData> myPnts;
   protected ArrayList<SegmentData> mySegs;
   protected boolean mySegsValidP = true;
   protected MatrixBlock[] mySolveBlks;
   protected int[] mySolveBlkNums;
   // a set of which segments are passive, if any
   protected HashSet<Integer> myPassiveSegs = null;

   protected class PointData {
      Point pnt;
      Vector3d dFdx;
      Vector3d v;

      PointData (Point p) {
         pnt = p;
         dFdx = new Vector3d();
         v = new Vector3d();
      }
   }

   protected Vector3d myTmp = new Vector3d();
   protected Matrix3d myMat = null;

   public static boolean myIgnoreCoriolisInJacobian = true;

   public static PropertyList myProps =
      new PropertyList (MultiPointSpring.class, PointSpringBase.class);

   static {
      //myProps.addReadOnly ("length *", "current spring length");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public MultiPointSpring() {
      this (null);
   }

   public MultiPointSpring (String name) {
      super (name);
      myPnts = new ArrayList<PointData>();
      mySegs = new ArrayList<SegmentData>();
      mySolveBlks = new MatrixBlock[0];
      mySolveBlkNums = new int[0];
      mySegsValidP = true;
   }

   public MultiPointSpring (String name, double k, double d, double l) {
      this (name);
//      myStiffness = k;
//      myDamping = d;
      setRestLength (l);
      setMaterial (new LinearAxialMaterial (k, d));
   }

   public MultiPointSpring (double k, double d, double l) {
      this (null);
      // myGain = 1;
//      myStiffness = k;
//      myDamping = d;
      setRestLength (l);
      setMaterial (new LinearAxialMaterial (k, d));
   }


   public Point getPoint (int idx) {
      if (idx < 0 || idx >= myPnts.size()) {
         throw new IndexOutOfBoundsException (
            "Point "+idx+" does not exist");
      }
      return myPnts.get(idx).pnt;
   }

   public int numPoints() {
      return myPnts.size();
   }

   public boolean containsPoint (Point pnt) {
      return indexOfPoint (pnt) != -1;
   }

   public void addPoint (int idx, Point pnt) {
      if (idx < 0 || idx > myPnts.size()) {
         throw new IndexOutOfBoundsException (
            "idx=" + idx + ", numPnts=" + myPnts.size());
      }
      myPnts.add (idx, new PointData(pnt));
//      if (getParent() != null) {
//         // then change is happening when connected to hierarchy
//         pnt.addBackReference (this);
//      }
      mySegsValidP = false;
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
   }

   public void addPoint (Point pnt) {
      addPoint (myPnts.size(), pnt);
   }

   public int indexOfPoint (Point pnt) {
      for (int i=0; i<myPnts.size(); i++) {
         if (myPnts.get(i).pnt == pnt) {
            return i;
         }
      }
      return -1;
   }

   public boolean removePoint (Point pnt) {
      int idx = indexOfPoint (pnt);
      if (idx != -1) {
         myPnts.remove (idx);
//         if (getParent() != null) {
//            // then change is happening when connected to hierarchy
//            pnt.removeBackReference (this);
//         }
         mySegsValidP = false;
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
         return true;
      }
      else {
         return false;
      }
   }
   
   /**
    * Sets the rest length of the spring from the current point locations
    * @return the new rest length
    */
   public double setRestLengthFromPoints() {
      double l = 0;
      for (int i=0; i<myPnts.size()-1; i++) {
         l += myPnts.get(i).pnt.distance(myPnts.get(i+1).pnt);
      }
      setRestLength(l);
      return l;
   }

   public void clearPoints() {
      if (myPnts.size() > 0) {
//         if (getParent() != null) {
//            // then change is happening when connected to hierarchy
//            for (int i=0; i<myPnts.size(); i++) {
//               myPnts.get(i).pnt.removeBackReference (this);
//            }
//         }
         myPnts.clear();
         mySegsValidP = false;
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
      }
   }

   public void setPoint (Point pnt, int idx) {
      if (idx < 0 || idx > myPnts.size()) {
         throw new IndexOutOfBoundsException (
            "idx=" + idx + ", numPnts=" + myPnts.size());
      }
      if (idx == myPnts.size()) {
         addPoint (pnt);
      }
      else {
         PointData data = myPnts.get(idx);
//         if (getParent() != null) {
//            // then change is happening when connected to hierarchy
//            data.pnt.removeBackReference (this);
//            pnt.addBackReference (this);
//         }
         data.pnt = pnt;
         mySegsValidP = false;
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
      }
   }

   public void clearPassiveSegments() {
      myPassiveSegs = null;
   }

   public void setSegmentPassive (int segIdx, boolean passive) {
      if (passive) {
         if (myPassiveSegs == null) {
            myPassiveSegs = new HashSet<Integer>();
         }
         myPassiveSegs.add (segIdx);
      }
      else {
         if (myPassiveSegs != null) {
            myPassiveSegs.remove (segIdx);
            if (myPassiveSegs.isEmpty()) {
               myPassiveSegs = null;
            }
         }
      }
      mySegsValidP = false;
   }

   public boolean isSegmentPassive (int segIdx) {
      if (myPassiveSegs != null) {
         return myPassiveSegs.contains (segIdx);
      }
      else {
         return false;
      }
   }

   protected void updateSegsIfNecessary() {
      if (!mySegsValidP) {
         int numPnts = myPnts.size();
         mySegs.clear();
         if (numPnts > 1) {
            for (int i=0; i<numPnts-1; i++) {
               SegmentData seg = 
                  new SegmentData (
                     myPnts.get(i).pnt, myPnts.get(i+1).pnt);
               mySegs.add (seg);
               if (myPassiveSegs != null && myPassiveSegs.contains (i)) {
                  seg.isActive = false;
               }
            }
         }
         mySolveBlks = new MatrixBlock[numPnts*numPnts];
         mySolveBlkNums = new int[numPnts*numPnts];
         mySegsValidP = true;
      }
   }

   /**
    * Computes the force acting on point i due to the spring segment between i
    * and i+1.
    * 
    * @param f
    * computed force acting between i and i+1
    * @param i
    * index of the point in the segment
    * @param F
    * scalar force in the entire spring
    */
   public void computeSegmentForce (Vector3d f, int i, double F) {
      updateSegsIfNecessary();
      SegmentData seg = mySegs.get(i);
      computeU (f, seg.pnt0, seg.pnt1);
      f.scale (F);
   }

   public void applyForces (double t) {
      double len = 0;
      double dldt = 0;
      updateSegsIfNecessary();
      for (int i=0; i<mySegs.size(); i++) {
         SegmentData seg = mySegs.get(i);
         if (seg.isActive) {
            len += computeU (myTmp, seg.pnt0, seg.pnt1);
            dldt += myTmp.dot (
               seg.pnt1.getVelocity())-myTmp.dot(seg.pnt0.getVelocity());
         }
      }
      double F = computeF (len, dldt);
      for (int i=0; i<mySegs.size(); i++) {
         SegmentData seg = mySegs.get(i);
         computeSegmentForce (myTmp, i, F);
         seg.pnt0.addForce (myTmp);
         seg.pnt1.subForce (myTmp);
      }
   }

   public void printPointReferences (PrintWriter pw, CompositeComponent ancestor)
      throws IOException {
      if (myPnts.size() == 0) {
         pw.println ("points=[ ]");
      }
      else {
         pw.println ("points=[");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (int i=0; i<myPnts.size(); i++) {
            pw.println (ComponentUtils.getWritePathName (
                           ancestor, myPnts.get(i).pnt));
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "passiveSegs")) {
         scanPassiveSegs (rtok);
         return true;
      }
      else if (scanAndStoreReferences (rtok, "points", tokens) >= 0) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }  

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      clearPoints();
      clearPassiveSegments();
      super.scan (rtok, ref);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "points")) {
         Point[] points = ScanWriteUtils.postscanReferences (
            tokens, Point.class, ancestor);
         for (int i=0; i<points.length; i++) {
            addPoint (points[i]);
         }
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected void scanPassiveSegs (ReaderTokenizer rtok) throws IOException {
      rtok.scanToken ('[');
      while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
         setSegmentPassive ((int)rtok.nval, true);
      }
      if (rtok.ttype != ']') {
         throw new IOException ("']' expected, line " + rtok.lineno());
      }
   }

   protected void writePassiveSegs (PrintWriter pw) {
      if (myPassiveSegs != null) {
         pw.print ("passiveSegs=[");
         for (Integer ival : myPassiveSegs) {
            pw.print (" "+ival);
         }
         pw.println (" ]");
      }
   }

    protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      printPointReferences (pw, ancestor);
      super.writeItems (pw, fmt, ancestor);
      writePassiveSegs (pw);
   }

   public void updateBounds (Point3d pmin, Point3d pmax) {
      for (int i=0; i<myPnts.size(); i++) {
         myPnts.get(i).pnt.updateBounds (pmin, pmax);
      }
   }

   void dorender (GLRenderer renderer, RenderProps props) {
      updateSegsIfNecessary();
      for (int i=0; i<mySegs.size(); i++) {
         SegmentData seg = mySegs.get(i);
         renderer.drawLine (
            props, seg.pnt0.myRenderCoords,
            seg.pnt1.myRenderCoords, /*isCapped=*/false,
            getRenderColor(), isSelected());
      }
   }     

   public void render (GLRenderer renderer, int flags) {
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
   }

   public void scaleMass (double s) {
      super.scaleMass (s);
      if (myMaterial != null) {
         myMaterial.scaleMass (s);
      }
      //myStiffness *= s;
      //myDamping *= s;
   }

   public double getLength() {
      double len = 0;
      updateSegsIfNecessary();
      for (int i=0; i<mySegs.size(); i++) {
         len += mySegs.get(i).updateU();
      }
      return len;
   }

   public double getLengthDot() {
      double lenDot = 0;
      updateSegsIfNecessary();
      for (int i = 0; i < mySegs.size(); i++) {
         lenDot += mySegs.get(i).getLengthDot();
      }
      return lenDot;
   }

   public double getActiveLength() {
      double len = 0;
      updateSegsIfNecessary();
      for (int i=0; i<mySegs.size(); i++) {
         SegmentData seg = mySegs.get(i);
         double l = seg.updateU();
         if (seg.isActive) {
            len += l;
         }
      }
      return len;
   }

   public double getActiveLengthDot() {
      double lendot = 0;
      updateSegsIfNecessary();
      for (int i=0; i<mySegs.size(); i++) {
         SegmentData seg = mySegs.get(i);
         double ld = seg.getLengthDot();
         if (seg.isActive) {
            lendot += ld;
         }
      }
      return lendot;
   }

   protected double computeU (Vector3d u, Point p0, Point p1) {
      u.sub (p1.getPosition(), p0.getPosition());
      double l = u.norm();
      if (l != 0) {
         u.scale (1 / l);
      }
      return l;
   }

   private MatrixBlock addBlockIfNeeded (
      SparseBlockMatrix M, int bi, int bj, Class type) {
      MatrixBlock blk = M.getBlock (bi, bj);
      if (blk == null) {
         try {
            blk = (MatrixBlock)type.newInstance();
         }
         catch (Exception e) {
            throw new InternalErrorException ("Cannot create instance of "
            + type);
         }
         M.addBlock (bi, bj, blk);
      }
      else if (!type.isAssignableFrom (blk.getClass())) {
         throw new InternalErrorException ("bad off-diagonal block type: "
         + blk.getClass());
      }
      return blk;
   }

   public void addSolveBlocks (SparseNumberedBlockMatrix M) {
      updateSegsIfNecessary();
      int numPnts = myPnts.size();
      if (numPnts > 1) {
         for (int i=0; i<numPnts; i++) {
            int bi = myPnts.get(i).pnt.getSolveIndex();
            for (int j=0; j<numPnts; j++) {
               int bj = myPnts.get(j).pnt.getSolveIndex();
               MatrixBlock blk = null;
               if (bi != -1 && bj != -1) {
                  blk = M.getBlock (bi, bj);
                  if (blk == null) {
                     blk = MatrixBlockBase.alloc (3, 3);
                     M.addBlock (bi, bj, blk);
                  }
               }
               mySolveBlks[i*numPnts+j] = blk;
               if (blk != null) {
                  mySolveBlkNums[i*numPnts+j] = blk.getBlockNumber();
               }
               else {
                  mySolveBlkNums[i*numPnts+j] = -1;
               }
            }
         }
      }
   }

   // assumes that u vectors for segments are up to date
   private void updateV () {
      int numPnts = myPnts.size();
      if (numPnts > 1) {
         for (int i=0; i<numPnts; i++) {
            PointData pdata = myPnts.get(i);
            if (i == 0) {
               pdata.v.set (mySegs.get(i).uvec);
            }
            else if (i < numPnts-1) {
               pdata.v.sub (mySegs.get(i).uvec, mySegs.get(i-1).uvec);
            }
            else {
               pdata.v.negate (mySegs.get(i-1).uvec);
            }
         }
      }
   }

   // assume that PointData v vectors and segment P and u is up to date
   private void updateDfdx (double dFdl, double dFdldot) {
      Vector3d y = new Vector3d();
      int numPnts = myPnts.size();
      if (numPnts > 1) {
         SegmentData segPrev = null;
         for (int i=0; i<numPnts; i++) {
            SegmentData segNext = null;
            if (i < numPnts-1) {
               segNext = mySegs.get(i);
            }
            PointData data = myPnts.get(i);
            data.dFdx.setZero();
            if (segPrev != null && segPrev.isActive) {
               data.dFdx.scaledAdd (dFdl, segPrev.uvec); 
               data.dFdx.add (y);
            }
            if (segNext != null && segNext.isActive) {
               data.dFdx.scaledAdd (-dFdl, segNext.uvec); 
               PointData next = myPnts.get(i+1);
               y.sub (next.pnt.getVelocity(), data.pnt.getVelocity());
               segNext.P.mulTranspose (y, y);
               y.scale (dFdldot/segNext.len);
               data.dFdx.sub (y);
            }
            segPrev = segNext;
         }
      }
   }

   public void addPosJacobian (SparseNumberedBlockMatrix M, double s) {
      if (myMat == null)
       { myMat = new Matrix3d();
       }
      int numPnts = myPnts.size();
      updateSegsIfNecessary();
      if (numPnts > 1) {
         double l = getActiveLength();
         double ldot = getActiveLengthDot();
         double F = computeF (l, ldot);
         double dFdl = computeDFdl (l, ldot);
         double dFdldot = computeDFdldot (l, ldot);
         for (int i=0; i<mySegs.size(); i++) {
            mySegs.get(i).updateP();
         }
         updateV();
         updateDfdx (dFdl, dFdldot);
         for (int i=0; i<numPnts; i++) {
            PointData data_i = myPnts.get(i);
            for (int j=0; j<numPnts; j++) {
               int blkNum = mySolveBlkNums[i*numPnts+j];
               if (blkNum != -1) {
                  MatrixBlock blk = M.getBlockByNumber (blkNum);
                  PointData data_j = myPnts.get(j);
                  myMat.outerProduct (data_i.v, data_j.dFdx);
                  if (j == i) {
                     if (i < numPnts-1) {
                        SegmentData seg_i = mySegs.get(i);
                        myMat.scaledAdd (-F/seg_i.len, seg_i.P);
                     }
                     if (i > 0) {
                        SegmentData seg_p = mySegs.get(i-1);
                        myMat.scaledAdd (-F/seg_p.len, seg_p.P);
                     }
                  }
                  else if (j == i+1) {
                     SegmentData seg_i = mySegs.get(i);
                     myMat.scaledAdd (F/seg_i.len, seg_i.P);
                  }
                  else if (j == i-1) {
                     SegmentData seg_p = mySegs.get(i-1);
                     myMat.scaledAdd (F/seg_p.len, seg_p.P);
                  }
                  blk.scaledAdd (s, myMat);
               }
            }
         }
      }
   }

   public void addVelJacobian (SparseNumberedBlockMatrix M, double s) {
      if (myMat == null)
       { myMat = new Matrix3d();
       }
      Vector3d tmp = new Vector3d();
      Vector3d dLdot = new Vector3d();
      int numPnts = myPnts.size();
      updateSegsIfNecessary();
      if (numPnts > 1) {
         double l = getActiveLength();
         double ldot = getActiveLengthDot();
         double dFdldot = computeDFdldot (l, ldot);
         updateV();
         for (int i=0; i<numPnts; i++) {
            tmp.scale (dFdldot, myPnts.get(i).v);
            for (int j=0; j<numPnts; j++) {
               int blkNum = mySolveBlkNums[i*numPnts+j];
               if (blkNum != -1) {
                  MatrixBlock blk = M.getBlockByNumber (blkNum);
                  dLdot.setZero();
                  if (j > 0) {
                     SegmentData segPrev = mySegs.get(j-1);
                     if (segPrev.isActive) {
                        dLdot.add (segPrev.uvec);
                     }
                  }
                  if (j < numPnts-1) {
                     SegmentData segNext = mySegs.get(j);
                     if (segNext.isActive) {
                        dLdot.sub (segNext.uvec);
                     }
                  }
                  myMat.outerProduct (tmp, dLdot);
                  blk.scaledAdd (s, myMat);
               }
            }
         }
      }
   }

   public int getJacobianType() {
      AxialMaterial mat = getEffectiveMaterial();
      if (myIgnoreCoriolisInJacobian || mat.isDFdldotZero()) {
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
      for (int i=0; i<myPnts.size(); i++) {
         if (!ComponentUtils.addCopyReferences (refs, getPoint(i), ancestor)) {
            return false;
         }
      }
      return true;
   }

   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      MultiPointSpring comp = (MultiPointSpring)super.copy (flags, copyMap);

      comp.mySegs = new ArrayList<SegmentData>();
      comp.myPnts = new ArrayList<PointData>();
      for (int i=0; i<myPnts.size(); i++) {
         Point pnt = (Point)ComponentUtils.maybeCopy (
            flags, copyMap, getPoint(i));
         comp.addPoint (pnt);
      }
      comp.setRenderProps (myRenderProps);
      if (myMaterial != null) {
         comp.setMaterial (myMaterial.clone());
      }
      //comp.setStiffness (myStiffness);
      //comp.setDamping (myDamping);
      comp.myTmp = new Vector3d();
      comp.myMat = null;
      if (myPassiveSegs != null) {
         comp.myPassiveSegs = new HashSet<Integer>();
         for (Integer ival : myPassiveSegs) {
            comp.myPassiveSegs.add (ival);
         }
      }
      return comp;
   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      for (int i=0; i<myPnts.size(); i++) {
         refs.add (myPnts.get(i).pnt);
      }
   }

//   @Override
//   public void connectToHierarchy () {
//      super.connectToHierarchy ();
//      for (int i=0; i<myPnts.size(); i++) {
//         myPnts.get(i).pnt.addBackReference (this);
//      }
//   }
//
//   @Override
//   public void disconnectFromHierarchy() {
//      super.disconnectFromHierarchy();
//      for (int i=0; i<myPnts.size(); i++) {
//         myPnts.get(i).pnt.removeBackReference (this);
//      }
//   }
   
   public void preadvance (double t0, double t1, int flags) {
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

}
