/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.PointTarget;
import artisynth.core.modelbase.*;
import artisynth.core.modelbase.ComponentChangeEvent.Code;
import artisynth.core.util.*;
import maspack.matrix.*;
import maspack.properties.PropertyList;
import maspack.util.*;

import java.util.*;
import java.io.*;

public class FemNode3d extends FemNode {

   protected Point3d myRest;
   protected Vector3d myInternalForce;

   private LinkedList<FemElement3d> myElementDeps;
   protected LinkedList<FemNodeNeighbor> myNodeNeighbors;
   private LinkedList<FemNodeNeighbor> myIndirectNeighbors;
   int myIndex = -1;
   private int myIncompressIdx = -1;
   //private int myLocalIncompressIdx = -1;
   protected float myRenderStress = 0;
   protected SymmetricMatrix3d myAvgStress = null;
   protected SymmetricMatrix3d myAvgStrain = null;

   // used for computing nodal-based incompressibility
   protected double myVolume;
   protected double myRestVolume;
   protected double myPressure;

   public static PropertyList myProps =
      new PropertyList (FemNode3d.class, Particle.class);

   static {
      myProps.get ("mass").setAutoWrite (false);
      myProps.add (
         "targetDisplacement",
         "target specified as a displacement from rest position", Point3d.ZERO,
         "%.8g NW");
      myProps.addReadOnly (
         "displacement", "displacement from rest position");
      myProps.addReadOnly (
         "stress", "average stress in this node");
      myProps.addReadOnly (
         "vonMisesStress", "average von Mises stress in this node");
      myProps.addReadOnly (
         "strain", "average strain in this node");
      myProps.addReadOnly (
         "vonMisesStrain", "average von Mises strain in this node");
      myProps.add("index", "Index of node (for external use)", -1);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FemNode3d() {
      super();
      myRest = new Point3d();
      myInternalForce = new Vector3d();
      myElementDeps = new LinkedList<FemElement3d>();
      myNodeNeighbors = new LinkedList<FemNodeNeighbor>();
   }

   public FemNode3d (Point3d p) {
      this();
      setPosition (p);
      myRest.set (p);
   }

   public FemNode3d (double x, double y, double z) {
      this();
      setPosition (x, y, z);
      myRest.set (x, y, z);
   }
   
   public int getIndex() {
      return myIndex;
   }
   
   public void setIndex(int idx) {
      myIndex = idx;
   }

   // index of the tetrahedral incompressibility constraint associated with
   // this node, if any
   public int getIncompressIndex() {
      return myIncompressIdx;
   }

   public void setIncompressIndex (int idx) {
      myIncompressIdx = idx;
   }

   private double computeVonMises (SymmetricMatrix3d M) {
      double sig00_11 = M.m00 - M.m11; 
      double sig11_22 = M.m11 - M.m22;
      double sig22_00 = M.m22 - M.m00;

      double J2 = ((sig00_11*sig00_11 + sig11_22*sig11_22 + sig22_00*sig22_00)/6
                   + M.m01*M.m01 + M.m12*M.m12 + M.m20*M.m20);
      return Math.sqrt (3*J2);    
   }

   /** 
    * Returns the von Mises stress for this node. This is equal to sqrt (3 J2),
    * where J2 is the second invariant of the average deviatoric stress for the
    * node.  This quantity is computed from the average nodal stress, which is
    * in turn computed only when computeNodeStresses is enabled for the FEM
    * model containing this node.
    *
    * @return van Mises stress
    */   
   public double getVonMisesStress () {
      if (myAvgStress == null) {
         return 0;
      }
      else {
         return computeVonMises (myAvgStress);
      }
   }

   public Vector3d getDisplacement () {
      Vector3d del = new Vector3d();
      del.sub (getPosition(), myRest);
      return del;
   }         

   public Vector3d getTargetDisplacement () {
      if (myTarget == null) {
         return getDisplacement();
      }
      else {
         Vector3d del = new Vector3d();
         del.sub (getTargetPosition(), myRest);
         return del;
      }
   }

   public void setTargetDisplacement (Vector3d del) {
      if (myTarget == null) {
         myTarget = new PointTarget (myTargetActivity);
      }
      Point3d pos = new Point3d();
      pos.add (del, myRest);
      myTarget.setTargetPos (pos);
   }

   /** 
    * Returns the averge stress for this node. Average node stresses are
    * computed by extrapolating the integration point stresses back to the
    * nodes for each element, and then computing the average of these
    * extrapolated values at each node. Average nodal stress is computed only
    * when computeNodeStresses is enabled for the FEM model containing this
    * node.
    *
    * @return average nodal stress (should not be modified)
    */   
   public SymmetricMatrix3d getStress () {
      if (myAvgStress == null) {
         myAvgStress = new SymmetricMatrix3d();
      }
      return myAvgStress;
   }

   public void setStress (double vms) {
      if (myAvgStress == null) {
         myAvgStress = new SymmetricMatrix3d();
      }
      myAvgStress.setZero ();
      myAvgStress.m00 = vms;
      myAvgStress.m22 = vms;
   }

   public void zeroStress() {
      if (myAvgStress == null) {
         myAvgStress = new SymmetricMatrix3d();
      }
      myAvgStress.setZero();
   }      

   protected void addScaledStress (double s, SymmetricMatrix3d sig) {
      if (myAvgStress == null) {
         myAvgStress = new SymmetricMatrix3d();
      }
      myAvgStress.scaledAdd (s, sig);
   }      

   /** 
    * Returns the von Mises strain for this node. This is equal to sqrt (3 J2),
    * where J2 is the second invariant of the average deviatoric strain for the
    * node.  This quantity is computed from the average nodal strain, which is
    * in turn computed only when computeNodeStrain is enabled for the FEM
    * model containing this node.
    *
    * @return van Mises strain
    */   
   public double getVonMisesStrain () {
      if (myAvgStrain == null) {
         return 0;
      }
      else {
         return computeVonMises (myAvgStrain);
      }
   }

   /** 
    * Returns the averge strain for this node. Average node strains are
    * computed by extrapolating the integration point strains back to the
    * nodes for each element, and then computing the average of these
    * extrapolated values at each node. Average nodal strain is computed only
    * when computeNodeStrain is enabled for the FEM model containing this
    * node.
    *
    * @return average nodal strain (should not be modified)
    */   
   public SymmetricMatrix3d getStrain () {
      if (myAvgStrain == null) {
         myAvgStrain = new SymmetricMatrix3d();
      }
      return myAvgStrain;
   }

   public void setStrain (double vms) {
      if (myAvgStrain == null) {
         myAvgStrain = new SymmetricMatrix3d();
      }
      myAvgStrain.setZero ();
      myAvgStrain.m00 = vms;
      myAvgStrain.m22 = vms;
   }

   public void zeroStrain() {
      if (myAvgStrain == null) {
         myAvgStrain = new SymmetricMatrix3d();
      }
      myAvgStrain.setZero();
   }      

   protected void addScaledStrain (double s, SymmetricMatrix3d sig) {
      if (myAvgStrain == null) {
         myAvgStrain = new SymmetricMatrix3d();
      }
      myAvgStrain.scaledAdd (s, sig);
   }
   
   public Vector3d getInternalForce() {
      return myInternalForce;
   }

   public LinkedList<FemNodeNeighbor> getNodeNeighbors() {
      return myNodeNeighbors;
   }

   public FemNodeNeighbor getNodeNeighborBySolveIndex (int idx) {
      for (FemNodeNeighbor nbr : myNodeNeighbors) {
         if (nbr.myNode.getSolveIndex() == idx) {
            return nbr;
         }
      }
      return null;
   }

   public FemNodeNeighbor getNodeNeighbor (FemNode3d node) {
      for (FemNodeNeighbor nbr : myNodeNeighbors) {
         if (nbr.myNode == node) {
            return nbr;
         }
      }
      return null;
   }

   /**
    * Indirect neighbors are the neighbors-of-neighbors. We need to keep track
    * of them in the special situation where we are computing soft nodal-based
    * incompressibility. That's because in soft nodal-based incompressibility,
    * all the nodes for all the elements connected to a node form a single
    * force-producing - essentially a "super-element", in that a change in any
    * nodes position affects the soft-nodal-incompressible force on all of the
    * nodes. Hence when computing overall forces and stiffness matrices,
    * each node has a larger set of nodes that influence it.
    */
   public LinkedList<FemNodeNeighbor> getIndirectNeighbors() {
      return myIndirectNeighbors;
   }

   public FemNodeNeighbor getIndirectNeighborBySolveIndex (int idx) {
      if (myIndirectNeighbors != null) {
         for (FemNodeNeighbor nbr : myIndirectNeighbors) {
            if (nbr.myNode.getSolveIndex() == idx) {
               return nbr;
            }
         }
      }
      return null;
   }

   public FemNodeNeighbor getIndirectNeighbor (FemNode3d node) {
      if (myIndirectNeighbors != null) {
         for (FemNodeNeighbor nbr : myIndirectNeighbors) {
            if (nbr.myNode == node) {
               return nbr;
            }
         }
      }
      return null;
   }

   public void clearIndirectNeighbors() {
      if (myIndirectNeighbors != null) {
         myIndirectNeighbors.clear();
         myIndirectNeighbors = null;
      }
   }

   public FemNodeNeighbor addIndirectNeighbor (FemNode3d nbrNode) {
      FemNodeNeighbor nbr = new FemNodeNeighbor (nbrNode);
      if (myIndirectNeighbors == null) {
         myIndirectNeighbors = new LinkedList<FemNodeNeighbor>();
      }
      myIndirectNeighbors.add (nbr);
      return nbr;
   }

   protected void registerNodeNeighbor (FemNode3d nbrNode) {
      FemNodeNeighbor nbr = getNodeNeighbor (nbrNode);
      if (nbr == null) {
         nbr = new FemNodeNeighbor (nbrNode);
         myNodeNeighbors.add (nbr);
      }
      else {
         nbr.myRefCnt++;
      }
   }

   public void deregisterNodeNeighbor (FemNode3d nbrNode) {
      FemNodeNeighbor nbr = getNodeNeighbor (nbrNode);
      if (nbr == null) {
         throw new InternalErrorException ("node not registered as a neighbor");
      }
      if (--nbr.myRefCnt == 0) {
         myNodeNeighbors.remove (nbr);
      }
   }

  @Override
   public boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "rest")) {
         myRest.scan (rtok);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      pw.print ("rest=");
      myRest.write (pw, fmt, /* withBrackets= */true);
      pw.println ("");
   }

   public void transformGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {
      super.transformGeometry (X, topObject, flags);
      // rest position should be transformed only if the entire FemModel is
      // being transformed.
      // myRest.transform (X);
   }

   public void scaleDistance (double s) {
      super.scaleDistance (s);
      myRest.scale (s);
      myInternalForce.scale (s);
   }
   
   public void addElementDependency (FemElement3d e) {
      myElementDeps.add (e);
   }

   public void removeElementDependency (FemElement3d e) {
      myElementDeps.remove (e);
   }

   public LinkedList<FemElement3d> getElementDependencies() {
      return myElementDeps;
   }

   public int numAdjacentElements() {
      return myElementDeps.size();
   }

   @Override
   public void connectToHierarchy () {
      super.connectToHierarchy ();
      // paranoid; do this in both connect and disconnect
      myNodeNeighbors.clear();
      clearIndirectNeighbors();
   }

   @Override
   public void disconnectFromHierarchy() {
      super.disconnectFromHierarchy();
      myNodeNeighbors.clear();
      clearIndirectNeighbors();
   }

   public void resetRestPosition() {
      myRest.set (getPosition());
      notifyParentOfChange (new ComponentChangeEvent (Code.STRUCTURE_CHANGED));
   }

   public Point3d getRestPosition() {
      return myRest;
   }

   public void setRestPosition (Point3d pos) {
      myRest.set (pos);
   }

   public FemNode3d copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {

      FemNode3d node = (FemNode3d)super.copy (flags, copyMap);

      node.myRest = new Point3d (myRest);
      node.myInternalForce = new Vector3d();
      node.myElementDeps = new LinkedList<FemElement3d>();
      node.myNodeNeighbors = new LinkedList<FemNodeNeighbor>();
      node.myIndirectNeighbors = null;

      node.myIncompressIdx = -1;
      //node.myLocalIncompressIdx = -1;

      node.myRenderStress = 0;
      node.myAvgStress = null;
      node.myAvgStrain = null;

      return node;   

   }

}
