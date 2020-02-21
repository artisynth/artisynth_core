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
import artisynth.core.mechmodels.SoftPlaneCollider;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.DynamicAttachment;
import artisynth.core.mechmodels.DynamicAttachmentBase;
import artisynth.core.mechmodels.MechSystemBase;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentChangeEvent.Code;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.femmodels.FemElement.ElementClass;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.util.ScanToken;
import maspack.geometry.GeometryTransformer;
import maspack.geometry.Boundable;
import maspack.matrix.*;
import maspack.render.RenderList;
import maspack.properties.PropertyList;
import maspack.util.DataBuffer;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

public class FemNode3d extends FemNode implements Boundable {

   /**
    * Flag indicating if stress values should be computed for this node
    */
   public static int NEEDS_STRESS = 0x1;
   
   /**
    * Flag indicating if strain values should be computed for this node
    */
   public static int NEEDS_STRAIN = 0x2;
   
   /**
    * Flag indicating external (application) request to compute stress
    * for this node.
    */
   public static int COMPUTE_STRESS_EXTERNAL = 0x10000;
   
   /**
    * Flag indicating internal request to compute stress for this node.
    */
   public static int COMPUTE_STRESS_INTERNAL = 0x20000;
   
   /**
    * Flag indicating external (application) request to compute stress
    * for this node.
    */
   public static int COMPUTE_STRAIN_EXTERNAL = 0x40000;
   
   /**
    * Flag indicating internal request to compute stress for this node.
    */
   public static int COMPUTE_STRAIN_INTERNAL = 0x80000;
   
   
    /**
    * Specifies a nodal coordinate type
    */
   public enum CoordType {
      /**
       * regular positions
       */
      SPATIAL,
   
      /**
       * rest positions
       */
      REST,

      /**
       * render coordinates
       */
      RENDER
   };

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
      myProps.add (
         "computeStress", "compute stress for this node", false);
      myProps.addReadOnly (
         "stress", "average stress in this node");
      myProps.addReadOnly (
         "vonMisesStress", "average von Mises stress in this node");
      myProps.addReadOnly (
         "MAPStress",
         "maximum absolute principal stress value");
      myProps.add (
         "computeStrain", "compute strain for this node", false);
      myProps.addReadOnly (
         "strain", "average strain in this node");
      myProps.addReadOnly (
         "vonMisesStrain", "average von Mises strain in this node");
      myProps.addReadOnly (
         "MAPStrain",
         "maximum absolute principal strain value");
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

   public Point3d getLocalCoordinates (CoordType ctype) {
      switch (ctype) {
         case SPATIAL: {
            return getLocalPosition();
         }
         case REST: {
            return getLocalRestPosition();
         }
         case RENDER: {
            return getLocalRenderPosition();
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented coordinate type " + ctype);
         }
      }      
   }

   public Point3d getCoordinates (CoordType ctype) {
      switch (ctype) {
         case SPATIAL: {
            return getPosition();
         }
         case REST: {
            return getRestPosition();
         }
         case RENDER: {
            return getRenderPosition();
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented coordinate type " + ctype);
         }
      }      
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
    * Computes and returns the eigenvalue with the maximum absolute value for
    * a symmetric 3x3 matrix.
    *
    * @param M matrix to compute maximum absolute eigenvalue for
    * @return maximum absolute eigenvalue
    */
   private double computeMaxAbsEigenvalue (SymmetricMatrix3d M) {    
      Vector3d eigs = new Vector3d();
      M.getEigenValues (eigs);
      return eigs.get(eigs.maxAbsIndex());
   }

   /**
    * Compute the Von Mises Stress criterion
    * https://en.wikipedia.org/wiki/Von_Mises_yield_criterion
    * The Von Mises Stress is equal to sqrt(3 J2), where J2 is
    * the second invariant of the average deviatoric strain for the node.
    * @param e stress tensor
    * @return Von Mises stress
    */
   private double computeVonMisesStress (SymmetricMatrix3d stress) {
      double J2 = computeJ2 (stress);
      return Math.sqrt (3.0*J2);    
   }
   
   /** 
    * Returns the von Mises stress for this node. This is equal to sqrt (3 J2),
    * where J2 is the second invariant of the average deviatoric stress for the
    * node.
    *
    * <p>This quantity is available only when stress values are being computed
    * for this node, as described in the documentation for {@link #getStress}.
    * If stress values are not being computed, 0 is returned.
    *
    * @return van Mises stress, or 0 is stress is not being computed
    */   
   public double getVonMisesStress () {
      return (myAvgStress == null) ? 0 : computeVonMisesStress(myAvgStress);
   }

   /** 
    * Returns the principal stress with the maximum absolute value for this
    * node.
    *
    * <p>This quantity is available only when stress values are being computed
    * for this node, as described in the documentation for {@link #getStress}.
    * If stress values are not being computed, 0 is returned.
    * 
    * @return max abs principal stress, or 0 is stress is not being computed
    */   
   public double getMAPStress () {
      return (myAvgStress == null) ? 0 : computeMaxAbsEigenvalue(myAvgStress);
   }

   /**
    * Compute the Von Mises strain equivalent according to
    * http://www.continuummechanics.org/vonmisesstress.html
    * which is equivalent to 
    * https://dianafea.com/manuals/d944/Analys/node405.html
    * The Von Mises Strain Equivalent is equal to sqrt(4/3 J2), where J2 is
    * the second invariant of the average deviatoric strain for the node.
    * @param strain strain tensor
    * @return von Mises strain Equivalent
    */
   private double computeVonMisesStrain(SymmetricMatrix3d strain) {
      double J2 = computeJ2 (strain);
      return Math.sqrt (4.0/3.0*J2);    
   }

   /** 
    * Returns the von Mises strain for this node. This is equal to sqrt (4/3 J2),
    * where J2 is the second invariant of the average deviatoric strain for the
    * node.
    *
    * <p>This quantity is available only when strain values are being computed
    * for this node, as described in the documentation for {@link #getStrain}.
    * If strain values are not being computed, 0 is returned.
    *
    * @return von Mises strain, or 0 is strain is not being computed
    */   
   public double getVonMisesStrain () {
      return (myAvgStrain == null) ? 0 : computeVonMisesStrain (myAvgStrain);
   }

   /** 
    * Returns the principal strain with the maximum absolute value for this
    * node.
    * 
    * <p>This quantity is available only when strain values are being computed
    * for this node, as described in the documentation for {@link #getStrain}.
    * If strain values are not being computed, 0 is returned.

    * @return max abs strain, or 0 is strain is not being computed
    */
   public double getMAPStrain () {
      return (myAvgStrain == null) ? 0 : computeMaxAbsEigenvalue(myAvgStrain);
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
 
   public void zeroStressStrain() {
      if (myAvgStress != null) {
         myAvgStress.setZero();
      }
      if (myAvgStrain != null) {
         myAvgStrain.setZero();
      }
   }
   
   /**
    * Returns flags indicating if stress or strain values should be
    * computed for this node.
    * 
    * @return stress/strain compute flags
    */
   public int needsStressStrain() {
      int flags = 0;
      if (myAvgStress != null) {
         flags |= NEEDS_STRESS;
      }
      if (myAvgStrain != null) {
         flags |= NEEDS_STRAIN;
      }
      return flags;      
   }

   /** 
    * Returns the average stress for this node. Average node stresses are
    * computed by extrapolating the integration point stresses back to the
    * nodes for each element, and then computing the average of these
    * extrapolated values at each node. 
    * 
    * <p>Average nodal stress is computed for this node only when explicitly
    * requested using {@link #setComputeStress} (or {@link
    * FemModel3d#setComputeNodalStress} in its FEM model), or when needed
    * internally, such as when an FEM mesh containing this node is rendered
    * using stress information. If stress is not being computed for this node,
    * then this method returns a zero-valued matrix.
    *
    * @return average nodal stress (should not be modified)
    */   
   public SymmetricMatrix3d getStress () {
      if (myAvgStress != null) {
         return myAvgStress;
      }
      else {
         return new SymmetricMatrix3d();
      }
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
      if (myAvgStress != null) {
         myAvgStress.setZero();
      }
   }      

   protected void addScaledStress (double s, SymmetricMatrix3d sig) {
      myAvgStress.scaledAdd (s, sig);
   }      

   public void setComputeStress (boolean enable) {
      if (enable) {
         setFlag (COMPUTE_STRESS_EXTERNAL);
      }
      else {
         clearFlag (COMPUTE_STRESS_EXTERNAL);
      }
      updateStressAllocation();
   }
   
   public boolean getComputeStress() {
      return (myFlags & COMPUTE_STRESS_EXTERNAL) != 0;
   }
   
   protected void setComputeStressInternal (boolean enable) {
      if (enable) {
         setFlag (COMPUTE_STRESS_INTERNAL);
      }
      else {
         clearFlag (COMPUTE_STRESS_INTERNAL);
      }
      updateStressAllocation();
   }
   
   protected boolean getComputeStressInternal() {
      return (myFlags & COMPUTE_STRESS_INTERNAL) != 0;
   }
   
   protected void updateStressAllocation() {
      if ((myFlags & (COMPUTE_STRESS_EXTERNAL|COMPUTE_STRESS_INTERNAL)) != 0) {
         if (myAvgStress == null) {
            myAvgStress = new SymmetricMatrix3d();
         }
      }
      else {
         myAvgStress = null;
      }
   }
   
   /** 
    * Returns the average strain for this node. Average node strains are
    * computed by extrapolating the integration point strains back to the
    * nodes for each element, and then computing the average of these
    * extrapolated values at each node.
    *
    * <p>Average nodal strain is computed for this node only when explicitly
    * requested using {@link #setComputeStrain} (or {@link
    * FemModel3d#setComputeNodalStrain} in its FEM model), or when needed
    * internally, such as when an FEM mesh containing this node is rendered
    * using strain information. If strain is not being computed for this node,
    * then this method returns a zero-valued matrix.
    *
    * @return average nodal strain (should not be modified)
    */   
   public SymmetricMatrix3d getStrain () {
      if (myAvgStrain != null) {
         return myAvgStrain;
      }
      else {
         return new SymmetricMatrix3d();
      }
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
      if (myAvgStrain != null) {
         myAvgStrain.setZero();
      }
   }      

   public void addScaledStrain (double s, SymmetricMatrix3d sig) {
      myAvgStrain.scaledAdd (s, sig);
   }
   
   public void setComputeStrain (boolean enable) {
      if (enable) {
         setFlag (COMPUTE_STRAIN_EXTERNAL);
      }
      else {
         clearFlag (COMPUTE_STRAIN_EXTERNAL);
      }
      updateStrainAllocation();
   }
   
   public boolean getComputeStrain() {
      return (myFlags & COMPUTE_STRAIN_EXTERNAL) != 0;
   }
   
   protected void setComputeStrainInternal (boolean enable) {
      if (enable) {
         setFlag (COMPUTE_STRAIN_INTERNAL);
      }
      else {
         clearFlag (COMPUTE_STRAIN_INTERNAL);
      }
      updateStrainAllocation();
   }
   
   protected boolean getComputeStrainInternal() {
      return (myFlags & COMPUTE_STRAIN_INTERNAL) != 0;
   }
   
   protected void updateStrainAllocation() {
      if ((myFlags & (COMPUTE_STRAIN_EXTERNAL|COMPUTE_STRAIN_INTERNAL)) != 0) {
         if (myAvgStrain == null) {
            myAvgStrain = new SymmetricMatrix3d();
         }
      }
      else {
         myAvgStrain = null;
      }
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
         myBackNode.setDynamic (true);
         // call myBackNode with tokens=null since we don't want it 
         // to generate BEGIN/END on the token stream
         myBackNode.scan (rtok, null);
         myBackNode.setDynamic (isDynamic());
         myDirectorActive = true;
         return true;
      }
      else if (scanAttributeName (rtok, "stressStrainInternal")) {
         int stressStrainInternal = rtok.scanInteger();
         myFlags |= stressStrainInternal;
         return true;
      }
      else if (scanAttributeName (rtok, "avgStress")) {
         if (myAvgStress == null) {
            myAvgStress = new SymmetricMatrix3d();
         }
         myAvgStress.scanAsVector (rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "avgStrain")) {
         if (myAvgStrain == null) {
            myAvgStrain = new SymmetricMatrix3d();
         }
         myAvgStrain.scanAsVector (rtok);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      // need to write director before super.writeItems(), so that it
      // already exists if we later scan an explicit mass
      if (hasDirector()) {
         pw.print ("backNode=");
         myBackNode.write (pw, fmt, ancestor);
      }
      super.writeItems (pw, fmt, ancestor);
      pw.print ("rest=");
      myRest.write (pw, fmt, /* withBrackets= */true);
      pw.println ("");
      int stressStrainInternal =
         (myFlags & (COMPUTE_STRESS_INTERNAL|COMPUTE_STRAIN_INTERNAL));
      if (stressStrainInternal != 0) {
         pw.printf ("stressStrainInternal=0x%x\n", stressStrainInternal);
      }
      if (myAvgStress != null) {
         pw.print ("avgStress=[ ");
         myAvgStress.writeAsVector (pw, fmt);
         pw.println (" ]");
      }
      if (myAvgStrain != null) {
         pw.print ("avgStrain=[ ");
         myAvgStrain.writeAsVector (pw, fmt);
         pw.println (" ]");
      }
      
   }

   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      super.addTransformableDependencies (context, flags);
      if (hasDirector()) {
         context.add (myBackNode);
      }
   }

   public void transformGeometry (
      GeometryTransformer gt, TransformGeometryContext context, int flags) {
      super.transformGeometry (gt, context, flags);
      // transform the rest position if we are not simulating.
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
    * {@link #getAdjacentVolumeElements}, or  
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
      if (myBackNode != null) {
         myBackNode.myMass = 0;
      }
      for (FemElement3dBase e : myElementDeps) {
         double restMass = e.getRestVolume()*e.getDensity();
         int nidx = e.getLocalNodeIndex(this);
         double massPerNode;
         if (FemModel3d.useNodalMassWeights) {
            massPerNode = restMass*e.getNodeMassWeights()[nidx];
         }
         else {
            massPerNode = restMass/e.numNodes();
         }
         if (e.getElementClass() == ElementClass.SHELL) {
            if (getBackNode() == null) {
               // should be allocated already, but just in case
               allocateBackNode();
            }
            // back node takes half the shell mass
            myBackNode.myMass += 0.5*massPerNode;
         }
         mass += massPerNode;
      }
      return mass;
   }
   
   protected double computeDefaultMass() {
      double mass = 0;
      for (FemElement3dBase e : myElementDeps) {
         double restMass = e.getRestVolume()*e.getDensity();
         int nidx = e.getLocalNodeIndex(this);
         //double massPerNode = restMass/e.numNodes();
         double massPerNode = restMass*e.getNodeMassWeights()[nidx];
         mass += massPerNode;
      }
      return mass;     
   }
   
   protected double computeDensityScale() {
      if (myMassExplicitP) {
         // XXX what if default mass is 0?
         return myMass/computeDefaultMass();
      }
      else {
         return 1.0;
      }
   }

   @Override
   public void resetEffectiveMass() {
      myEffectiveMass = getMass();
      if (hasDirector()) {
         // share mass with the back node
         myEffectiveMass -= myBackNode.getMass();
      }
   }
   
   @Override
   public void setExplicitMass (double m) {
      super.setExplicitMass (m);
      if (hasDirector()) {
         // back node takes half the mass
         myMass = m;
         myBackNode.myMass = 0.5*m;
      }
   }
   
   @Override
   public void connectToHierarchy (CompositeComponent hcomp) {
      super.connectToHierarchy (hcomp);
      if (hcomp == getParent()) {
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
   }

   @Override
   public void disconnectFromHierarchy(CompositeComponent hcomp) {
      super.disconnectFromHierarchy(hcomp);
      if (hcomp == getParent()) {
         myNodeNeighbors.clear();
         clearIndirectNeighbors();
         setFrameNode (null);
      }
   }

   public void resetRestPosition() {
      myRest.set (getLocalPosition());
      invalidateAdjacentNodeMasses();
      notifyParentOfChange (new ComponentChangeEvent (Code.STRUCTURE_CHANGED));
   }

   public Point3d getRestPosition() {
      return myRest;
   }

   public Point3d getRenderPosition() {
      Point3d pos =
         new Point3d (myRenderCoords[0], myRenderCoords[1], myRenderCoords[2]);
      return pos;
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

      if ((flags & CopyableComponent.REST_POSITION) != 0) {
         node.setPosition (myRest);
         node.setVelocity (Vector3d.ZERO);
         node.zeroForces ();
      }

      node.myRest = new Point3d (myRest);
      node.myInternalForce = new Vector3d();
      node.myElementDeps = new LinkedList<FemElement3dBase>();
      node.myShellElemCnt = 0;
      node.myNodeNeighbors = new LinkedList<FemNodeNeighbor>();
      node.myIndirectNeighbors = null;

      if (myBackNode != null) {
         node.myBackNode = myBackNode.copy (flags, copyMap);
         node.myBackNode.myNode = node;
      }

      node.myIncompressIdx = -1;
      //node.myLocalIncompressIdx = -1;

      node.myRenderStress = 0;
      node.myAvgStress = null;
      node.myAvgStrain = null;
      node.setComputeStress (getComputeStress());
      node.setComputeStrain (getComputeStrain());

      return node;   
   }

   /* --- Boundable --- */

   /**
    * {@inheritDoc}
    */
   public int numPoints() {
      return 1;
   }

   /**
    * {@inheritDoc}
    */
   public Point3d getPoint (int idx) {
      return getPosition();
   }  

   /**
    * {@inheritDoc}
    */
   public void computeCentroid (Vector3d centroid) {
      centroid.set (getPosition());
   }

   /**
    * Not implemented for FemNode3d.
    *
    * @return -1 (not implemented)
    */
   public double computeCovariance (Matrix3d C) {
      return -1;
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
   
   public Point3d getLocalRenderPosition() {
      Point3d pos = 
         new Point3d (myRenderCoords[0], myRenderCoords[1], myRenderCoords[2]);
      if (myFrameNode != null) {
         pos.inverseTransform (myFrameNode.myFrame.getPose());
      }
      return pos;
   }

   /* --- Extensions to save/load stress and strain as state --- */

   private static byte HAS_STRESS = 0x01;
   private static byte HAS_STRAIN = 0x02;

   public void getState (DataBuffer data) {
      super.getState (data);
      if (MechSystemBase.mySaveForcesAsState) {
        int flags = 0;
        if (myAvgStress != null) {
           data.dput (myAvgStress);
           flags |= HAS_STRESS;
        }
        if (myAvgStrain != null) {
           data.dput (myAvgStrain);
           flags |= HAS_STRAIN;
        }
        data.zput (flags);
     }
   }

   public void setState (DataBuffer data) {
      super.setState (data);
     if (MechSystemBase.mySaveForcesAsState) {
        int flags = data.zget();
        if ((flags & HAS_STRESS) != 0) {
           if (myAvgStress == null) {
              myAvgStress = new SymmetricMatrix3d();
           }
           data.dget (myAvgStress);
        }
        if ((flags & HAS_STRAIN) != 0) {
           if (myAvgStrain == null) {
              myAvgStrain = new SymmetricMatrix3d();
           }
           data.dget (myAvgStrain);
        }
     }
   }

   /* --- Methods for shell directors --- */

   protected BackNode3d myBackNode = null;
   protected boolean myDirectorActive = false;

   public int getBackSolveIndex() {
      if (myDirectorActive) {
         return myBackNode.getSolveIndex();
      }
      else {
         return -1;
      }
   }

   public BackNode3d getBackNode() {
      return myBackNode;
   }
   
   private void allocateBackNode() {
      myBackNode = new BackNode3d (this);
      myBackNode.setDynamic (isDynamic());     
   }

   protected void setDirectorActive (boolean active) {
      // XXX should we also try to initialize this with values?
      if (active != hasDirector()) {
         if (active) {
            if (myBackNode == null) {
               allocateBackNode();
            }
            if (myMassExplicitP) {
               // back node takes half the mass
               myBackNode.myMass = 0.5*myMass;
            }
         }
         myDirectorActive = active;
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
      return myDirectorActive;
   }

   public Vector3d getDirector() {
      Vector3d dir = new Vector3d();
      getDirector (dir);
      return dir;
   }

   public void getDirector (Vector3d dir) {
      if (myBackNode == null) {
         dir.setZero();
      }
      else {
         dir.sub (myState.getPos(), myBackNode.getPosition());
      }
   }

   public void setDirector (Vector3d dir) {
      if (myBackNode == null) {
         allocateBackNode();
      }
      myBackNode.setPosition (myState.getPos(), dir);
   }
   
   public void clearDirector() {
      if (myBackNode != null) {
         myBackNode.clearPosition();
      }
   }

   public Vector3d getDirectorVel() {
      if (myBackNode == null) {
         return new Vector3d();
      }
      else {
         Vector3d dir = new Vector3d();
         dir.sub (myState.getVel(), myBackNode.myVel);
         return dir;
      }
   }

   public void setDirectorVel (Vector3d vel) {
      if (myBackNode == null) {
         allocateBackNode();
      }
      myBackNode.myVel.sub (myState.getVel(), vel);
   }

   public Point3d getRestDirector() {
      Point3d rdir = new Point3d();
      getRestDirector (rdir);
      return rdir;
   }

   public void getRestDirector (Vector3d rdir) {
      if (myBackNode == null) {
         rdir.setZero();
      }
      else {
         rdir.sub (getRestPosition(), myBackNode.getRestPosition());
      }
   }

   public void setRestDirector (Vector3d restDir) {
      if (myBackNode == null) {
         allocateBackNode();
      }
      myBackNode.setRestPosition (myRest, restDir);
      invalidateRestData();
   }

   public boolean isRestDirectorExplicit() {
      if (myBackNode != null) {
         return myBackNode.isRestPositionExplicit();
      }
      else {
         return false;
      }
   }

   public void clearRestDirector() {
      if (myBackNode != null) {
         myBackNode.clearRestPosition();
      }
   }

   public void invalidateRestDirectorIfNecessary() {
      if (myBackNode != null) {
         if (!myBackNode.isRestPositionExplicit()) {
            myBackNode.clearRestPosition();
         }
      }
   }

   public Point3d getBackPosition() {
      if (myBackNode == null) {
         return new Point3d();
      }
      else {
         return myBackNode.getPosition();
      }
   }

   public void setBackPosition (Point3d pos) {
      if (myBackNode == null) {
         allocateBackNode();
      }
      myBackNode.setPosition (pos);
   }

   public Point3d getBackRestPosition() {
      if (myBackNode == null) {
         return new Point3d();
      }
      else {
         return myBackNode.getRestPosition();
      }
   }

   public void setBackRestPosition (Point3d rest) {
      if (myBackNode == null) {
         allocateBackNode();
      }
      myBackNode.setRestPosition (rest);
      invalidateRestData();
   }

   public Point3d getBackRenderPosition() {
      if (myBackNode == null) {
         return new Point3d();
      }
      else {
         return myBackNode.getRenderPosition();
      }
   }

   public Vector3d getBackVelocity () {
      if (myBackNode == null) {
         return new Vector3d();
      }
      else {
         return myBackNode.myVel;
      }
   }

   public void setBackVelocity (Vector3d vel) {
      if (myBackNode == null) {
         allocateBackNode();
      }
      myBackNode.myVel.set (vel);
   }

   public Vector3d getBackForce() {
      if (myBackNode == null) {
         return new Vector3d();
      }
      else {
         return myBackNode.myForce;
      }
   }

   public void setBackForce (Vector3d f) {
      if (myBackNode == null) {
         allocateBackNode();
      }
      myBackNode.myForce.set (f);
   }
   
   public Vector3d getBackInternalForce() {
      if (myBackNode == null) {
         return new Vector3d();
      }
      else {
         return myBackNode.myInternalForce;
      }
   }

   @Override
   public void zeroForces() {
      super.zeroForces();
      if (myBackNode != null) {
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
         if (e.getElementClass() == ElementClass.SHELL) {
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
   
   protected boolean rescaleDirectorsIfNecessary (
      ShellElement3d elem, double prevThickness) {
      
      if (myBackNode == null) {
         // shouldn't happen
         return false;
      }
      if (!myBackNode.isRestPositionExplicit() && 
          myBackNode.isRestPositionValid()) {
         double thicknessSum = 0;
         for (FemElement e : myElementDeps) {
            if (e.getElementClass() == ElementClass.SHELL && e != elem) {
               ShellElement3d se = (ShellElement3d)e;
               thicknessSum += se.getDefaultThickness();
            }
         }
         double scale = 
            (thicknessSum+elem.getDefaultThickness()) /
            (thicknessSum+prevThickness);
         if (scale != 1) {
            myBackNode.scaleRestPosition (scale, getRestPosition());
            if (myBackNode.isPositionValid()) {
               // then scale the director as well
               myBackNode.scalePosition (scale, getPosition());
               if (myBackNode.isAttached()) {
                  myBackNode.getAttachment().updateAttachment();
               }
            }
            return true;
         }
      }
      return false;
   }

   public void prerender (RenderList list) {
      super.prerender (list);
      if (hasDirector()) {
         myBackNode.saveRenderCoords();
      }
   }

   public Point3d getBackCoordinates (CoordType ctype) {
      switch (ctype) {
         case SPATIAL: {
            return getBackPosition();
         }
         case REST: {
            return getBackRestPosition();
         }
         case RENDER: {
            return getBackRenderPosition();
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented coordinate type " + ctype);
         }
      }      
   }

}
