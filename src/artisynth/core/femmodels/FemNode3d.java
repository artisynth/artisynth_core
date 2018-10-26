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
import java.util.List;
import java.util.ArrayList;
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
import artisynth.core.femmodels.FemElement.ElementClass;
import artisynth.core.util.ScanToken;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.*;
import maspack.render.RenderList;
import maspack.properties.PropertyList;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

public class FemNode3d extends FemNode {

   protected Point3d myRest;
   protected Vector3d myInternalForce;

   private LinkedList<FemElement3dBase> myElementDeps;
   protected int myShellElemCnt;
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
      myProps.add (
         "backPosition", "back position used for shell nodes",
         Point3d.ZERO, "%.8g NW");
      myProps.add (
         "backRestPosition", "rest back position used for shell nodes",
         Point3d.ZERO, "%.8g NW");
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
      myElementDeps = new LinkedList<FemElement3dBase>();
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

   protected void registerNodeNeighbor (FemNode3d nbrNode, boolean shell) {
      FemNodeNeighbor nbr = getNodeNeighbor (nbrNode);
      if (nbr == null) {
         nbr = new FemNodeNeighbor (nbrNode);
         myNodeNeighbors.add (nbr);
      }
      if (shell) {
         if (nbr.myShellRefCnt == 0) {
            nbr.allocateDirectorStorage();
         }        
         nbr.myShellRefCnt++;
      }
      else {
         nbr.myVolumeRefCnt++;
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

   public void deregisterNodeNeighbor (FemNode3d nbrNode, boolean shell) {
      FemNodeNeighbor nbr = getNodeNeighbor (nbrNode);
      if (nbr == null) {
         throw new InternalErrorException ("node not registered as a neighbor");
      }
      if (shell) {
         nbr.myShellRefCnt--;
         if (nbr.myShellRefCnt == 0 && nbr.myVolumeRefCnt != 0) {
            nbr.deallocateDirectorStorage();
         }
      }
      else {
         nbr.myVolumeRefCnt--;
      }
      if (nbr.myShellRefCnt + nbr.myVolumeRefCnt == 0) {
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
      else if (scanAttributeName (rtok, "backNode")) {
         myBackNode = new BackNode3d (this);
         // call myBackNode with tokens=null since we don't want it 
         // to generate BEGIN/END on the token stream
         myBackNode.scan (rtok, null);
         myBackNode.setDynamic (isDynamic());
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
      if (myBackNode != null) {
         pw.print ("backNode=");
         myBackNode.write (pw, fmt, ancestor);
      }
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
   
   public void addElementDependency (FemElement3dBase e) {
      myElementDeps.add (e);
      if (e.getElementClass() == ElementClass.SHELL) {
         myShellElemCnt++;
         if (!hasDirector()) {
            setDirectorActive (true);
         }
      }
   }

   public void removeElementDependency (FemElement3dBase e) {
      myElementDeps.remove (e);
      if (e.getElementClass() == ElementClass.SHELL) {
         myShellElemCnt--;
         if (myShellElemCnt == 0 && hasDirector()) {
            setDirectorActive (false);
         }
      }
   }

   /**
    * Returns all the volumetric elements referencing this node.
    * Should use {@link #getAdjacentElements},
    * {@link #getAdjacentVolumetricElements}, or  
    * {@link #getAdjacentShellElements}, as appropriate.
    * 
    * @deprecated
    * @return list of all volumetric elements referencing this node
    */
   public LinkedList<FemElement3d> getElementDependencies() {
      LinkedList<FemElement3d> elems = new LinkedList<FemElement3d>();
      for (FemElement3dBase e : myElementDeps) {
         if (e instanceof FemElement3d) {
            elems.add ((FemElement3d)e);
         }
      }
      return elems;
   }
   
   /**
    * Returns all elements (either shell or volumetric) 
    * that reference this node.
    * 
    * @return List of all elements referencing this node.
    */
   public List<FemElement3dBase> getAdjacentElements() {
      return myElementDeps;
   }
   
   /**
    * Returns all volumetric elements that reference this node.
    * 
    * @return List of all volumetric elements referencing this node.
    */    
   public List<FemElement3d> getAdjacentVolumeElements() {
      ArrayList<FemElement3d> elems = new ArrayList<FemElement3d>();
      for (FemElement3dBase e : myElementDeps) {
         if (e instanceof FemElement3d) {
            elems.add ((FemElement3d)e);
         }
      }
      return elems;
   }

   /**
    * Returns all shell elements that reference this node.
    * 
    * @return List of all shell elements referencing this node.
    */  
   public List<ShellElement3d> getAdjacentShellElements() {
      ArrayList<ShellElement3d> elems = new ArrayList<ShellElement3d>();
      for (FemElement3dBase e : myElementDeps) {
         if (e instanceof ShellElement3d) {
            elems.add ((ShellElement3d)e);
         }
      }
      return elems;
   }

   public int numAdjacentElements() {
      return myElementDeps.size();
   }
   
   public double computeMassFromDensity() {
      double mass = 0;
      for (FemElement3dBase e : myElementDeps) {
         if (e instanceof FemElement3d) {
            FemElement3d ee = (FemElement3d)e;
            mass += ee.getRestVolume()*ee.getDensity()/ee.numNodes();
         }
         else if (e instanceof ShellElement3d) {
            // XXX TODO use area?
            ShellElement3d ee = (ShellElement3d)e;
            mass += ee.getRestVolume()*ee.getDensity()/ee.numNodes();           
         }
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

   private void invalidateRestData() {
      invalidateAdjacentNodeMasses();
      // invalidate rest data for attached elements
      FemModel3d fem = findFem();
      if (fem != null) {
         fem.invalidateNodalRestVolumes();
         fem.invalidateStressAndStiffness();
      }
      for (FemElement elem : myElementDeps) {
         elem.invalidateRestData();
      }
   }

   public void setRestPosition (Point3d pos) {
      myRest.set (pos);
      invalidateRestData();
   }

   public FemNode3d copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {

      FemNode3d node = (FemNode3d)super.copy (flags, copyMap);

      node.myRest = new Point3d (myRest);
      node.myInternalForce = new Vector3d();
      node.myElementDeps = new LinkedList<FemElement3dBase>();
      node.myShellElemCnt = 0;
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
      if (myBackNode != null) {
         myBackNode.setDynamic (enable);
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

   protected BackNode3d myBackNode = null;

   public int getBackSolveIndex() {
      if (myBackNode != null) {
         return myBackNode.getSolveIndex();
      }
      else {
         return -1;
      }
   }

   public BackNode3d getBackNode() {
      return myBackNode;
   }

   protected void setDirectorActive (boolean active) {
      // XXX should we also try to initialize this with values?
      if (active != hasDirector()) {
         if (active) {
            myBackNode = new BackNode3d (this, myState.getPos());
            myBackNode.setDynamic (isDynamic());
         }
         else {
            myBackNode = null;
         }
         // if this node is attached to a frame using a 
         // ShellNodeFrameAttachment, we may need to update whether or 
         // not that frame needs a director attachment
         if (getAttachment() instanceof ShellNodeFrameAttachment) {
            ShellNodeFrameAttachment at = 
               (ShellNodeFrameAttachment)getAttachment();
            at.updateDirectorAttachment();
         }
      }
   }

   public boolean hasDirector() {
      return myBackNode != null;
   }

   public Vector3d getDirector() {
      if (!hasDirector()) {
         return new Vector3d();
      }
      else {
         Vector3d dir = new Vector3d();
         dir.sub (myState.getPos(), myBackNode.getPosition());
         return dir;
      }
   }

   public void setDirector (Vector3d dir) {
      if (hasDirector()) {
         myBackNode.setPosition (myState.getPos(), dir);
         myBackNode.setPositionExplicit (true);
      }
   }

   public void initializeDirectorIfNecessary() {
      if (hasDirector()) {
         if (!myBackNode.isPositionExplicit()) {
            myBackNode.setPositionToRest();
         }
      }
   }

   public Vector3d getDirectorVel() {
      if (!hasDirector()) {
         return new Vector3d();
      }
      else {
         Vector3d dir = new Vector3d();
         dir.sub (myState.getVel(), myBackNode.myVel);
         return dir;
      }
   }

   public void setDirectorVel (Vector3d vel) {
      if (hasDirector()) {
         myBackNode.myVel.sub (myState.getVel(), vel);
      }
   }

   public Point3d getRestDirector() {
      if (!hasDirector()) {
         return new Point3d();
      }
      else {
         Point3d rest = new Point3d();
         rest.sub (myRest, myBackNode.getRestPosition());
         return rest;
      }
   }

   public void setRestDirector (Vector3d restDir) {
      if (hasDirector()) {
         myBackNode.setRestPosition (myRest, restDir);
         invalidateRestData();
      }
   }

   public boolean isRestDirectorExplicit() {
      if (hasDirector()) {
         return myBackNode.isRestPositionExplicit();
      }
      else {
         return false;
      }
   }

   public boolean isRestDirectorValid() {
      if (hasDirector()) {
         return myBackNode.isRestPositionValid();
      }
      else {
         return false;
      }
   }

   public void invalidateRestDirectorIfNecessary() {
      if (hasDirector()) {
         if (!myBackNode.isRestPositionExplicit()) {
            myBackNode.setRestPositionValid (false);
         }
      }
   }

   public Point3d getBackPosition() {
      if (!hasDirector()) {
         return new Point3d();
      }
      else {
         return myBackNode.getPosition();
      }
   }

   public void setBackPosition (Point3d pos) {
      if (hasDirector()) {
         myBackNode.setPosition (pos);
         myBackNode.setPositionExplicit (true);
      }
   }

   public Point3d getBackRestPosition() {
      if (!hasDirector()) {
         return new Point3d();
      }
      else {
         return myBackNode.getRestPosition();
      }
   }

   public void setBackRestPosition (Point3d rest) {
      if (hasDirector()) {
         myBackNode.setRestPosition (rest);
         invalidateRestData();
      }
   }

   public Vector3d getBackVelocity () {
      if (!hasDirector()) {
         return new Vector3d();
      }
      else {
         return myBackNode.myVel;
      }
   }

   public void setBackVelocity (Vector3d vel) {
      if (hasDirector()) {
         myBackNode.myVel.set (vel);
      }
   }

   public Vector3d getBackForce() {
      if (!hasDirector()) {
         return new Vector3d();
      }
      else {
         return myBackNode.myForce;
      }
   }

   public void setBackForce (Vector3d f) {
      if (hasDirector()) {
         myBackNode.myForce.set (f);
      }
   }
   
   @Override
   public void zeroForces() {
      super.zeroForces();
      if (hasDirector()) {
         myBackNode.myForce.setZero();
      }
   }

   public void computeRestDirector () {
      Vector3d dir = new Vector3d();
      computeRestDirector (dir);
      setRestDirector (dir);
   }

   public void computeRestDirector (Vector3d dir) {

      double thickness = 0;
      int ecnt = 0;

      for (FemElement e : myElementDeps) {
         if (e instanceof ShellElement3d) {
            ShellElement3d se = (ShellElement3d)e;

            Vector3d d = new Vector3d();
            double area = se.computeRestNodeNormal (d, this);
            dir.scaledAdd (area, d);
            thickness += se.getDefaultThickness();
            ecnt++;
         }
      }
      if (ecnt > 0) {
         dir.normalize();
         dir.scale (thickness/ecnt);         
      }
   }

   public void prerender (RenderList list) {
      super.prerender (list);
      if (hasDirector()) {
         myBackNode.saveRenderCoords();
      }
   }

}
