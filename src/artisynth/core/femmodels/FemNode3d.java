/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.PointTarget;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.DynamicAttachment;
import artisynth.core.mechmodels.DynamicAttachmentBase;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentChangeEvent.Code;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScanToken;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.*;
import maspack.properties.PropertyList;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

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

   protected FrameNode3d myFrameNode = null;
   protected DynamicAttachment myFrameAttachment = null;

   public static PropertyList myProps =
      new PropertyList (FemNode3d.class, FemNode.class);

   static {
      myProps.add ("restPosition",
         "rest position for the node", Point3d.ZERO, "%.8g");
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
   
   /**
    * Compute the second deviatoric invariant J2
    * https://en.wikipedia.org/wiki/Cauchy_stress_tensor#Stress_deviator_tensor
    * @param M Tensor
    * @return J2
    */
   private double computeJ2 (SymmetricMatrix3d M) {    
      double sig00_11 = M.m00 - M.m11; 
      double sig11_22 = M.m11 - M.m22;
      double sig22_00 = M.m22 - M.m00;
      double J2 = ((sig00_11*sig00_11 + sig11_22*sig11_22 + sig22_00*sig22_00)/6
                   + M.m01*M.m01 + M.m12*M.m12 + M.m20*M.m20);
      return J2;
   }
   
   /**
    * Compute the Von Mises Stress criterion
    * https://en.wikipedia.org/wiki/Von_Mises_yield_criterion
    * The Von Mises Stress is equal to sqrt(3 J2), where J2 is
    * the second invariant of the average deviatoric strain for the node.
    * @param e Strain tensor
    * @return Von Mises Strain Equivalent
    */
   private double computeVonMisesStress (SymmetricMatrix3d stress) {
      double J2 = computeJ2 (stress);
      return Math.sqrt (3.0*J2);    
   }
   
   /**
    * Compute the Von Mises strain equivalent according to
    * http://www.continuummechanics.org/vonmisesstress.html
    * which is equivalent to 
    * https://dianafea.com/manuals/d944/Analys/node405.html
    * The Von Mises Strain Equivalent is equal to sqrt(4/3 J2), where J2 is
    * the second invariant of the average deviatoric strain for the node.
    * @param e Strain tensor
    * @return Von Mises Strain Equivalent
    */
   private double computeVonMisesStrain(SymmetricMatrix3d strain) {
      double J2 = computeJ2 (strain);
      return Math.sqrt (4.0/3.0*J2);    
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
      return (myAvgStress == null) ? 0 : computeVonMisesStress(myAvgStress);
   }

   public Vector3d getDisplacement () {
      Vector3d del = new Vector3d();
      del.sub (getLocalPosition(), getRestPosition());
      return del;
   }         

   public Vector3d getTargetDisplacement () {
      if (myTarget == null) {
         return getDisplacement();
      }
      else {
         Vector3d del = new Vector3d();
         del.sub (getTargetPosition(), getRestPosition());
         return del;
      }
   }

   public void setTargetDisplacement (Vector3d del) {
      if (myTarget == null) {
         myTarget = new PointTarget (myTargetActivity);
      }
      Point3d pos = new Point3d();
      pos.add (del, getRestPosition());
      myTarget.setTargetPos (pos);
   }

   /** 
    * Returns the average stress for this node. Average node stresses are
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
      myAvgStress.m11 = vms;
      myAvgStress.m22 = vms;
   }

   public void zeroStress() {
      if (myAvgStress == null) {
         myAvgStress = new SymmetricMatrix3d();
      }
      myAvgStress.setZero();
   }      

   public void addScaledStress (double s, SymmetricMatrix3d sig) {
      if (myAvgStress == null) {
         myAvgStress = new SymmetricMatrix3d();
      }
      myAvgStress.scaledAdd (s, sig);
   }      

   /** 
    * Returns the von Mises strain for this node. This is equal to sqrt (4/3 J2),
    * where J2 is the second invariant of the average deviatoric strain for the
    * node.  This quantity is computed from the average nodal strain, which is
    * in turn computed only when computeNodeStrain is enabled for the FEM
    * model containing this node.
    *
    * @return von Mises strain
    */   
   public double getVonMisesStrain () {
      return (myAvgStrain == null) ? 0 : computeVonMisesStrain (myAvgStrain);
   }

   /** 
    * Returns the average strain for this node. Average node strains are
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
      myAvgStrain.m11 = vms;
      myAvgStrain.m22 = vms;
   }

   public void zeroStrain() {
      if (myAvgStrain == null) {
         myAvgStrain = new SymmetricMatrix3d();
      }
      myAvgStrain.setZero();
   }      

   public void addScaledStrain (double s, SymmetricMatrix3d sig) {
      if (myAvgStrain == null) {
         myAvgStrain = new SymmetricMatrix3d();
      }
      myAvgStrain.scaledAdd (s, sig);
   }
   
   public Vector3d getInternalForce() {
      return myInternalForce;
   }

   /* --- Methods pertaining to node neighbors --- */

   public LinkedList<FemNodeNeighbor> getNodeNeighbors() {
      return myNodeNeighbors;
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

   public FemNodeNeighbor getNodeNeighbor (FemNode3d node) {
      for (FemNodeNeighbor nbr : myNodeNeighbors) {
         if (nbr.myNode == node) {
            return nbr;
         }
      }
      return null;
   }

   public FemNodeNeighbor getNodeNeighborBySolveIndex (int idx) {
      for (FemNodeNeighbor nbr : myNodeNeighbors) {
         if (nbr.myNode.getLocalSolveIndex() == idx) {
            return nbr;
         }
      }
      return null;
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

   /* --- Methods related to indirect neighbors --- */
   /*
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

   public FemNodeNeighbor getIndirectNeighborBySolveIndex (int idx) {
      if (myIndirectNeighbors != null) {
         for (FemNodeNeighbor nbr : myIndirectNeighbors) {
            if (nbr.myNode.getLocalSolveIndex() == idx) {
               return nbr;
            }
         }
      }
      return null;
   }

   public FemNodeNeighbor addIndirectNeighbor (FemNode3d nbrNode) {
      FemNodeNeighbor nbr = new FemNodeNeighbor (nbrNode);
      if (myIndirectNeighbors == null) {
         myIndirectNeighbors = new LinkedList<FemNodeNeighbor>();
      }
      myIndirectNeighbors.add (nbr);
      return nbr;
   }

   public void clearIndirectNeighbors() {
      if (myIndirectNeighbors != null) {
         myIndirectNeighbors.clear();
         myIndirectNeighbors = null;
      }
   }
   
   /* --- --- */

   protected void invalidateAdjacentNodeMasses() {
      for (FemNodeNeighbor nbr : myNodeNeighbors) {
         nbr.myNode.invalidateMassIfNecessary();
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
      GeometryTransformer gt, TransformGeometryContext context, int flags) {
      super.transformGeometry (gt, context, flags);
      // transform the rest position if we are not simulating. The
      if ((flags & TransformableGeometry.TG_SIMULATING) == 0) {
         gt.transformPnt (myRest);
         // invalidate rest data for adjacent elements
         for (int i=0; i<myElementDeps.size(); i++) {
            myElementDeps.get(i).invalidateRestData();
         }
         // invalidate masses of the adjacent nodes, so that they
         // can be recomputed from the new elements volumes
         invalidateAdjacentNodeMasses();
      }
      context.addParentToNotify(getParent());
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
   
   public double computeMassFromDensity() {
      double mass = 0;
      Iterator<FemElement3d> it = myElementDeps.iterator();
      while (it.hasNext()) {
         FemElement3d e = it.next();
         mass += e.getRestVolume()*e.getDensity()/e.numNodes();
      }
      return mass;
   }

   @Override
   public void connectToHierarchy () {
      super.connectToHierarchy ();
      // paranoid; do this in both connect and disconnect
      myNodeNeighbors.clear();
      clearIndirectNeighbors();
      ModelComponent gp = getGrandParent();
      if (gp instanceof FemModel3d) {
         FemModel3d fem = (FemModel3d)gp;
         if (fem.isFrameRelative()) {
            setFrameNode (new FrameNode3d (this, fem.getFrame()));
         }
      }
   }

   @Override
   public void disconnectFromHierarchy() {
      super.disconnectFromHierarchy();
      myNodeNeighbors.clear();
      clearIndirectNeighbors();
      setFrameNode (null);
   }

   public void resetRestPosition() {
      myRest.set (getLocalPosition());
      invalidateAdjacentNodeMasses();
      notifyParentOfChange (new ComponentChangeEvent (Code.STRUCTURE_CHANGED));
   }

   public Point3d getRestPosition() {
      return myRest;
   }

   public Point3d getLocalRestPosition() {
      if (myFrameNode != null) {
         Point3d rest = new Point3d(myRest);
         rest.inverseTransform (myFrameNode.myFrame.getPose());
         return rest;
      }
      else {
         return myRest;
      }
   }
   
   private FemModel3d findFem() {
      // XXX current hack to get FEM,
      // dependent on current hierarchy
      CompositeComponent gp = getGrandParent();
      if (gp instanceof FemModel3d) {
         return (FemModel3d)gp;
      }
      return null;
   }

   public void setRestPosition (Point3d pos) {
      myRest.set (pos);
      invalidateAdjacentNodeMasses();
      // invalidate rest data for attached elements
      FemModel3d fem = findFem();
      if (fem != null) {
         fem.invalidateNodalRestVolumes();
         fem.invalidateStressAndStiffness();
      }
      for (FemElement3d elem : myElementDeps) {
         elem.invalidateRestData();
      }
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

   /* --- FrameFemNode --- */

   private DynamicAttachment getRegularAttachment() {
      DynamicAttachment at = getAttachment();
      if (at != null && at instanceof ModelComponent) {
         return at;
      }
      else {
         return null;
      }
   }

   private void updateFrameAttachment() {
      DynamicAttachment current = myFrameAttachment;
      if (myFrameNode == null) {
         // then clear any attachment
         if (current != null) {
            current.removeBackRefs();
            myFrameAttachment = null;
         }
      }
      else {
         if (!isDynamic() || getRegularAttachment() != null) {
            // need to attach frameNode to this node
            if (!(current instanceof FrameNodeNodeAttachment) ||
                ((FrameNodeNodeAttachment)current).myFrameNode != myFrameNode) {
               System.out.println ("adding frameNode to node attachment");
               if (current != null) {
                  current.removeBackRefs();
               }            
               myFrameAttachment =
                  new FrameNodeNodeAttachment (myFrameNode, this);
               if (getRegularAttachment() == null) {
                  super.setAttached (null);
               }
               myFrameNode.setAttached (myFrameAttachment);
               myFrameAttachment.addBackRefs();
            }
         }
         else {
            // need to attach this node to frameNode
            if (!(current instanceof NodeFrameNodeAttachment) ||
                ((NodeFrameNodeAttachment)current).myFrameNode != myFrameNode) {
               System.out.println ("adding node to frameNode attachment");
               if (current != null) {
                  current.removeBackRefs();
               }
               myFrameAttachment =
                  new NodeFrameNodeAttachment (this, myFrameNode);

               super.setAttached (myFrameAttachment);
               myFrameNode.setAttached (null);
               myFrameAttachment.addBackRefs();
            }
         }
      }
   }      

   public void setFrameNode (FrameNode3d fnode) {
      myFrameNode = fnode;
      updateFrameAttachment();
   }

   public FrameNode3d getFrameNode() {
      return myFrameNode;
   }

   public DynamicAttachment getFrameAttachment() {
      return myFrameAttachment;
   }

   @Override 
   public void setAttached (DynamicAttachment at) {
      super.setAttached (at);
      if (myFrameNode != null) {
         updateFrameAttachment();
      }
   }

   @Override
   public void setDynamic (boolean enable) {
      super.setDynamic (enable);
      if (myFrameNode != null) {
         updateFrameAttachment();
      }
   }

   public Point3d getLocalPosition() {
      if (myFrameNode != null) {
         return myFrameNode.getPosition();
      }
      else {
         return myState.getPos();
      }
   }
      
   public Vector3d getLocalVelocity() {
      if (myFrameNode != null) {
         return myFrameNode.getVelocity();
      }
      else {
         return myState.getVel();
      }
   }

   public int getLocalSolveIndex() {
      if (myFrameNode != null) {
         return myFrameNode.getSolveIndex();
      }
      else {
         return getSolveIndex();
      }
   }

   public Vector3d getLocalForce() {
      if (myFrameNode != null) {
         return myFrameNode.getForce();
      }
      else {
         return getForce();
      }
   }
      
   public void addLocalForce (Vector3d f) {
      if (myFrameNode != null) {
         myFrameNode.addForce(f);
      }
      else {
         addForce(f);
      }
   }

   public void setLocalForce (Vector3d f) {
      if (myFrameNode != null) {
         myFrameNode.setForce(f);
      }
      else {
         setForce(f);
      }
   }

   public boolean isActiveLocal() {
      if (myFrameNode != null) {
         return myFrameNode.isActive();
      }
      else {
         return isActive();
      }
   }

   /* --- Methods for shell directors --- */

   protected Point3d myBackPos = null;
   protected Vector3d myBackVel = null;
   protected Vector3d myBackForce = null;
   protected Point3d myBackRest = null;

   protected float[] myBackRenderPos = null;

   public int getBackSolveIndex() {
      // XXX TODO finish
      return -1;
   }

   protected void setDirectorActive (boolean active) {
      // XXX should we also try to initialize this will values?
      if (active != hasDirector()) {
         if (active) {
            myBackPos = new Point3d();
            myBackVel = new Vector3d();
            myBackForce = new Vector3d();
            myBackRest = new Point3d();
         }
         else {
            myBackPos = null;
            myBackVel = null;
            myBackForce = null;
            myBackRest = null;
         }
      }
   }

   public boolean hasDirector() {
      return myBackPos != null;
   }

   public Vector3d getDirector() {
      if (!hasDirector()) {
         return new Vector3d();
      }
      else {
         Vector3d dir = new Vector3d();
         dir.sub (myState.getPos(), myBackPos);
         return dir;
      }
   }

   public void setDirector (Vector3d dir) {
      if (hasDirector()) {
         myBackPos.sub (myState.getPos(), dir);
      }
   }

   public Vector3d getDirectorVel() {
      if (!hasDirector()) {
         return new Vector3d();
      }
      else {
         Vector3d dir = new Vector3d();
         dir.sub (myState.getVel(), myBackVel);
         return dir;
      }
   }

   public void setDirectorVel (Vector3d vel) {
      if (hasDirector()) {
         myBackVel.sub (myState.getVel(), vel);
      }
   }

   public Point3d getDirectorRest() {
      if (!hasDirector()) {
         return new Point3d();
      }
      else {
         Point3d rest = new Point3d();
         rest.sub (myRest, myBackRest);
         return rest;
      }
   }

   public void setDirectorRest (Point3d rest) {
      if (hasDirector()) {
         myBackRest.sub (myRest, rest);
      }
   }

   public Vector3d getBackForce() {
      if (!hasDirector()) {
         return new Vector3d();
      }
      else {
         return myBackForce;
      }
   }

   public void setBackForce (Vector3d f) {
      if (hasDirector()) {
         myBackForce.set (f);
      }
   }

   @Override
   public int getPosState(double[] x, int idx) {
      idx = super.getPosState(x, idx);
      if (hasDirector()) {
         x[idx++] = myBackPos.x;
         x[idx++] = myBackPos.y;
         x[idx++] = myBackPos.z;
      }
      return idx;
   }
   
   @Override 
   public int setPosState(double[] p, int idx) {
      idx = super.setPosState(p, idx);
      if (hasDirector()) {
         myBackPos.x = p[idx++];
         myBackPos.y = p[idx++];
         myBackPos.z = p[idx++];
      }
      return idx;
   }
   
   @Override 
   public void addPosImpulse (
      double[] xbuf, int xidx, double h, double[] vbuf, int vidx) {
      xbuf[xidx  ] += h*vbuf[vidx  ];
      xbuf[xidx+1] += h*vbuf[vidx+1];
      xbuf[xidx+2] += h*vbuf[vidx+2];
      if (hasDirector()) {
         xbuf[xidx+3] += h*vbuf[vidx+3];
         xbuf[xidx+4] += h*vbuf[vidx+4];
         xbuf[xidx+5] += h*vbuf[vidx+5];
      }
   }
   
   @Override
   public int getPosDerivative (double[] dxdt, int idx) {
      idx = super.getPosDerivative(dxdt, idx);
      if (hasDirector()) {
         dxdt[idx++] = myBackVel.x;
         dxdt[idx++] = myBackVel.y;
         dxdt[idx++] = myBackVel.z;
      }
      return idx;
   }
   
   @Override
   public int getVelState (double[] v, int idx) {
      idx = super.getVelState(v, idx);
      if (hasDirector()) {
         v[idx++] = myBackVel.x;
         v[idx++] = myBackVel.y;
         v[idx++] = myBackVel.z;
      }
      return idx;
   }
   
   @Override
   public int setVelState (double[] v, int idx) {
      idx = super.setVelState (v, idx);
      if (hasDirector()) {
         myBackVel.x = v[idx++];
         myBackVel.y = v[idx++];
         myBackVel.z = v[idx++];
      }
      return idx;
   }
   
   @Override 
   public int getVelStateSize() {
      return hasDirector() ? 6 : 3;
   }
   
   @Override 
   public int getPosStateSize() {
      return hasDirector() ? 6 : 3;
   }
   
   @Override
   public void setState (Point pt) {
      super.setState(pt);
      if (hasDirector() && pt instanceof FemNode3d) {
         FemNode3d fn = (FemNode3d)pt;
         if (fn.hasDirector()) {
            myBackPos.set (fn.myBackPos);
            myBackVel.set (fn.myBackVel);
         }
      }
   }
   
   @Override
   public int setState (VectorNd x, int idx) {
      idx = super.setState(x, idx);
      if (hasDirector()) {
         double[] xb = x.getBuffer();
         myBackPos.x = xb[idx++];
         myBackPos.y = xb[idx++];
         myBackPos.z = xb[idx++];
         myBackVel.x = xb[idx++];
         myBackVel.y = xb[idx++];
         myBackVel.z = xb[idx++];
      }
      return idx;
   }
   
   @Override
   public int getState (VectorNd x, int idx) {
      idx = super.getState(x, idx);
      if (hasDirector()) {
         double[] xb = x.getBuffer();
         xb[idx++] = myBackPos.x;
         xb[idx++] = myBackPos.y;
         xb[idx++] = myBackPos.z;
         xb[idx++] = myBackVel.x;
         xb[idx++] = myBackVel.y;
         xb[idx++] = myBackVel.z;
      }
      return idx;
   }
   
   @Override
   public boolean velocityLimitExceeded (double tlimit, double rlimit) {
      if (super.velocityLimitExceeded (tlimit, rlimit)) {
         return true;
      }
      if (hasDirector()) {
         return (myBackVel.containsNaN() || myBackVel.infinityNorm() > tlimit);
      }
      else {
         return false;
      }
   }
   
   @Override
   public int getForce (double[] f, int idx) {
      idx = super.getForce(f, idx);
      if (hasDirector()) {
         f[idx++] = myBackForce.x;
         f[idx++] = myBackForce.y;
         f[idx++] = myBackForce.z;
      }
      return idx;
   }
   
   @Override
   public int setForce (double[] f, int idx) {
      idx = super.setForce(f, idx);
      if (hasDirector()) {
         myBackForce.x = f[idx++];
         myBackForce.y = f[idx++];
         myBackForce.z = f[idx++];
      }
      return idx;
   }
   
   @Override
   public void zeroForces() {
      super.zeroForces();
      if (hasDirector()) {
         myBackForce.setZero();
      }
   }

   @Override
   public MatrixBlock createMassBlock() {
      if (!hasDirector()) {
         return super.createMassBlock();
      }
      else {
         return new Matrix6dDiagBlock();
      }
   }
   
   @Override 
   protected void doGetMass(Matrix M, double m) {
      if (!hasDirector()) {
         super.doGetMass (M, m);
      }
      else {
         if (M instanceof Matrix6d) {
            Matrix6d M6 = (Matrix6d)M;
            M6.setDiagonal (m, m, m, m, m, m);
         }
         else {
            throw new IllegalArgumentException (
               "Matrix not instance of Matrix6d");
         }
      }
   }
   
   @Override
   public void getInverseMass (Matrix Minv, Matrix M) {
      if (!hasDirector()) {
         super.getInverseMass (Minv, M);
      }
      else {
         if (!(Minv instanceof Matrix6d)) {
            throw new IllegalArgumentException ("Minv not instance of Matrix6d");
         }
         if (!(M instanceof Matrix6d)) {
            throw new IllegalArgumentException ("M not instance of Matrix6d");
         }
         double inv = 1/((Matrix6d)M).m00;
         ((Matrix6d)Minv).setDiagonal (inv, inv, inv, inv, inv, inv);
      }
   }
   
   @Override 
   public void addSolveBlock (SparseNumberedBlockMatrix S) {
      if (!hasDirector()) {
         super.addSolveBlock (S);
      }
      else {
         int bi = getSolveIndex();
         Matrix6dBlock blk = new Matrix6dBlock();
         S.addBlock(bi, bi, blk);
      }
   }

   @Override 
   public MatrixBlock createSolveBlock() {
      if (!hasDirector()) {
         return super.createSolveBlock();
      }
      else {
         Matrix6dBlock blk = new Matrix6dBlock();
         return blk;
      }
   }
   
   @Override 
   public void addToSolveBlockDiagonal(SparseNumberedBlockMatrix S, double d) {
      if (!hasDirector()) {
         super.addToSolveBlockDiagonal (S, d);
      }
      else {
         if (getSolveIndex() != -1) {
            Matrix6dBlock blk = 
               (Matrix6dBlock)S.getBlockByNumber(getSolveIndex());
            blk.m00 += d;
            blk.m11 += d;
            blk.m22 += d;
            blk.m33 += d;
            blk.m44 += d;
            blk.m55 += d;
         }
      }
   }
   
   @Override
   public int mulInverseEffectiveMass (
      Matrix M, double[] a, double[] f, int idx) {
      if (!hasDirector()) {
         return super.mulInverseEffectiveMass (M, a, f, idx);
      }    
      else {
         double minv = 1/myEffectiveMass;
         a[idx++] = minv*f[idx];
         a[idx++] = minv*f[idx];
         a[idx++] = minv*f[idx];
         a[idx++] = minv*f[idx];
         a[idx++] = minv*f[idx];
         a[idx++] = minv*f[idx];
         return idx;
      }
   }

   @Override
   public int getEffectiveMassForces (VectorNd f, double t, int idx) {
      if (!hasDirector()) {
         return super.getEffectiveMassForces (f, t, idx);
      }
      else {
         double[] buf = f.getBuffer();
         // Note that if the point is attached to a moving frame, then that
         // will produce mass forces that are not computed here.
         buf[idx++] = 0;
         buf[idx++] = 0;
         buf[idx++] = 0;
         buf[idx++] = 0;
         buf[idx++] = 0;
         buf[idx++] = 0;
         return idx;
      }
   }
}
