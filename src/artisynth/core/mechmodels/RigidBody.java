/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentListImpl;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.modelbase.TransformGeometryAction;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.ScanToken;
import maspack.geometry.DistanceGrid;
import maspack.geometry.GeometryTransformer;
import maspack.geometry.MeshBase;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix3d;
import maspack.matrix.MatrixBlock;
import maspack.matrix.Matrix6x2Block;
import maspack.matrix.MatrixBlockBase;
import maspack.matrix.Matrix6x1Block;
import maspack.matrix.Matrix6d;
import maspack.matrix.Point3d;
import maspack.matrix.Quaternion;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.matrix.VectorNd;
import maspack.properties.HierarchyNode;
import maspack.properties.PropertyList;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.RenderableUtils;
import maspack.render.Renderer;
import maspack.spatialmotion.SpatialInertia;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;
import maspack.util.DoubleInterval;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.Range;
import maspack.util.ReaderTokenizer;

public class RigidBody extends Frame 
   implements CollidableBody, HasSurfaceMesh,
              ConnectableBody, CompositeComponent, Wrappable {

   protected SpatialInertia mySpatialInertia;
   protected SpatialInertia myEffectiveInertia;
   protected GeometryTransformer.Constrainer myTransformConstrainer = null;
   
   //MeshInfo myMeshInfo = null;
   protected ArrayList<BodyConnector> myConnectors;

   // pre-allocated temporary storage variables
   protected Wrench myCoriolisForce = new Wrench();
   protected Twist myBodyVel = new Twist();
   protected Twist myBodyAcc = new Twist();

   private static double DEFAULT_DENSITY = 1.0;
   protected double myDensity = DEFAULT_DENSITY;

   static final boolean useExternalAttachments = false;

   private static final Vector3d zeroVect = new Vector3d();
   
   /* Indicates how inertia is determined for a RigidBody */
   public enum InertiaMethod {
      /**
       * Inertia is determined implicitly from the surface mesh and a specified 
       * density.
       */
      DENSITY, 
      
      /**
       * Inertia is determined implicitly from the surface mesh and a specified 
       * mass (which is divided by the mesh volume to determine a density).
       */     
      MASS, 
      
      /** 
       * Inertia is explicitly specified.
       */
      EXPLICIT
   };
   
   protected InertiaMethod myInertiaMethod = DEFAULT_INERTIA_METHOD;
   
   public static PropertyList myProps =
      new PropertyList (RigidBody.class, Frame.class);

   private static InertiaMethod DEFAULT_INERTIA_METHOD =
      InertiaMethod.DENSITY;
   private static SymmetricMatrix3d DEFAULT_INERTIA =      
      new SymmetricMatrix3d (1, 1, 1, 0, 0, 0);
   private static double DEFAULT_MASS = 1.0;
   private static Point3d DEFAULT_CENTER_OF_MASS = new Point3d (0, 0, 0);

   protected static final Collidability DEFAULT_COLLIDABILITY =
      Collidability.ALL;   
   protected Collidability myCollidability = DEFAULT_COLLIDABILITY;
   protected int myCollidableIndex;

   PolygonalMesh mySDRenderSurface = null;
   static boolean DEFAULT_GRID_SURFACE_RENDERING = false;
   boolean myGridSurfaceRendering = DEFAULT_GRID_SURFACE_RENDERING;

   //double myGridMargin = 0.1;

   protected ComponentListImpl<ModelComponent> myComponents;
   protected NavpanelDisplay myDisplayMode = NavpanelDisplay.NORMAL;
   protected MeshComponentList<RigidMeshComp> myMeshList = null;
   protected ArrayList<MeshComponent> myCollisionMeshes = new ArrayList<>();
   protected PolygonalMesh myCompoundCollisionMesh;

   DistanceGridComp myDistanceGridComp;

    static {
      myProps.remove ("renderProps");
      myProps.add ("renderProps * *", "render properties", null);
      myProps.add (
         "inertiaMethod", "means by which inertia is determined",
         DEFAULT_INERTIA_METHOD);
      myProps.add (
         "density", "average density of this body", DEFAULT_DENSITY, "NW");
      myProps.add ("mass", "mass of this body", DEFAULT_MASS, "1E NW");
      myProps.add (
         "inertia getRotationalInertia setRotationalInertia",
         "rotational inertia of this body", DEFAULT_INERTIA, "1E NW");
      myProps.add (
         "centerOfMass", "center of mass of this body", DEFAULT_CENTER_OF_MASS,
         "1E NW");
      myProps.add (
         "dynamic isDynamic", "true if component is dynamic (non-parametric)",
         true);
      myProps.add (
         "collidable", 
         "sets the collidability of this body", DEFAULT_COLLIDABILITY);
      myProps.add (
         "gridSurfaceRendering", 
         "renders the grid surface instead of the meshes", 
         DEFAULT_GRID_SURFACE_RENDERING);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public boolean isMassConstant() {
      return !dynamicVelInWorldCoords;
   }

   public double getMass (double t) {
      return getMass();
   }

   public void getMass (Matrix M, double t) {
      if (dynamicVelInWorldCoords) {
         if (M instanceof Matrix6d) {
            mySpatialInertia.getRotated ((Matrix6d)M, getPose().R);
         }
         else {
            throw new IllegalArgumentException (
               "Matrix not instance of Matrix6d");
         }
      }
      else {
         doGetInertia (M, mySpatialInertia);
      }
   }

   public SpatialInertia getEffectiveInertia() {
      if (myEffectiveInertia == null) {
         return mySpatialInertia;
      }
      else {
         return myEffectiveInertia;
      }
   }
   
   public double getEffectiveMass() {
      return getEffectiveInertia().getMass();
   }
   
   public void getEffectiveMass (Matrix M, double t) {
      SpatialInertia S = getEffectiveInertia();
      if (dynamicVelInWorldCoords) {
         if (M instanceof Matrix6d) {
            S.getRotated ((Matrix6d)M, getPose().R);
         }
         else {
            throw new IllegalArgumentException (
               "Matrix not instance of Matrix6d");
         }
      }
      else {
         doGetInertia (M, S);
      }
   }
   
   public int mulInverseEffectiveMass (
      Matrix M, double[] a, double[] f, int idx) {
      return mulInverseEffectiveMass (getEffectiveInertia(), a, f, idx);
   }
   
   public static int mulInverseEffectiveMass (
      SpatialInertia S, double[] a, double[] f, int idx) {
      Twist tw = new Twist();
      Wrench wr = new Wrench();
      wr.f.x = f[idx];
      wr.f.y = f[idx+1];
      wr.f.z = f[idx+2];
      wr.m.x = f[idx+3];
      wr.m.y = f[idx+4];
      wr.m.z = f[idx+5];
      S.mulInverse (tw, wr);
      a[idx++] = tw.v.x;
      a[idx++] = tw.v.y;
      a[idx++] = tw.v.z;
      a[idx++] = tw.w.x;
      a[idx++] = tw.w.y;
      a[idx++] = tw.w.z;
      return idx;     
   }
   
   public static int getEffectiveMassForces (
      VectorNd f, double t, FrameState state, 
      SpatialInertia effectiveInertia, int idx) {
      
      Wrench coriolisForce = new Wrench();
      Twist bodyVel = new Twist();
      bodyVel.inverseTransform (state.XFrameToWorld.R, state.vel);
      SpatialInertia S = effectiveInertia;
      S.coriolisForce (coriolisForce, bodyVel);
      if (dynamicVelInWorldCoords) {
         coriolisForce.transform (state.XFrameToWorld.R);
      }
      double[] buf = f.getBuffer();
      buf[idx++] = -coriolisForce.f.x;
      buf[idx++] = -coriolisForce.f.y;
      buf[idx++] = -coriolisForce.f.z;
      buf[idx++] = -coriolisForce.m.x;
      buf[idx++] = -coriolisForce.m.y;
      buf[idx++] = -coriolisForce.m.z;
      return idx;     
   }
   
   public int getEffectiveMassForces (VectorNd f, double t, int idx) {
      return getEffectiveMassForces (
         f, t, myState, getEffectiveInertia(), idx);
   }

   public void resetEffectiveMass() {
      myEffectiveInertia = null;
   }

   /**
    * {@inheritDoc}
    */
   public void addEffectivePointMass (double m, Vector3d loc) {
      if (myEffectiveInertia == null) {
         myEffectiveInertia = new SpatialInertia (mySpatialInertia);
      }
      SpatialInertia.addPointMass (myEffectiveInertia, m, loc);
   }

   /**
    * {@inheritDoc}
    */
   public void addEffectiveFrameMass (SpatialInertia M, RigidTransform3d TFL) {
      if (myEffectiveInertia == null) {
         myEffectiveInertia = new SpatialInertia (mySpatialInertia);
      }
      SpatialInertia MB = new SpatialInertia (M);
      MB.transform (TFL);
      myEffectiveInertia.add (MB);
   }

   public void subEffectiveInertia (SpatialInertia M) {
      if (myEffectiveInertia == null) {
         myEffectiveInertia = new SpatialInertia (mySpatialInertia);
      }
      myEffectiveInertia.sub (M);
   }

   public void getInverseMass (Matrix Minv, Matrix M) {
      if (!(Minv instanceof Matrix6d)) {
         throw new IllegalArgumentException ("Minv not instance of Matrix6d");
      }
      if (!(M instanceof Matrix6d)) {
         throw new IllegalArgumentException ("M not instance of Matrix6d");
      }
      SpatialInertia.invert ((Matrix6d)Minv, (Matrix6d)M);
   }

   // *************** Methods for setting the inertia ***********************

   /*
    * Returns the {@link #InertiaMethod InertiaMethod} method used to determine 
    * the inertia for this RigidBody.
    * 
    * @return inertia method for this RigidBody
    * @see #setInertia
    * @see #setInertiaFromDensity
    * @see #setInertiaFromMass
    */
   public InertiaMethod getInertiaMethod() {
      return myInertiaMethod;
   }
   

   /**
    * Sets the {@link RigidBody.InertiaMethod InertiaMethod} method used to
    * determine the inertia for this RigidBody.
    * 
    * @param method inertia method for this RigidBody
    * @see #setInertia
    * @see #setInertiaFromDensity
    * @see #setInertiaFromMass
    */
   public void setInertiaMethod (InertiaMethod method) {
      if (method != myInertiaMethod) {
         switch (method) {
            case DENSITY: {
               updateInertiaFromMeshes();
               break;
            }
            case MASS: {
               updateInertiaFromMeshes();
               break;
            }
            case EXPLICIT: {
               break;
            }
            default: {
               throw new InternalErrorException (
                  "Unimplemented inertia method " + method);
            }
         }
         myInertiaMethod = method;
      }
   }

   protected SpatialInertia getInertia() {
      return mySpatialInertia;
   }

   public void getInertia (SpatialInertia M) {
      M.set (mySpatialInertia);
   }

   public void setCenterOfMass (Point3d com) {
      mySpatialInertia.setCenterOfMass (com);
      myInertiaMethod = InertiaMethod.EXPLICIT;
   }

   public void getCenterOfMass (Point3d com) {
      mySpatialInertia.getCenterOfMass (com);
   }

   public Point3d getCenterOfMass() {
      return mySpatialInertia.getCenterOfMass();
   }

   /**
    * Adjusts the pose so that it reflects the rigid body's center of mass.
    * Returns the previous center of mass position, with respect to body
    * coordinates. The negative of this gives the previous location of body
    * frame's origin, also with respect to body coordinates.
    *
    * @return previous center of mass position
    */
   public Vector3d centerPoseOnCenterOfMass() {
      Point3d com = new Point3d(getCenterOfMass());
      translateCoordinateFrame (com);
      return com;
   }
   
   /**
    * Shifts the coordinate frame by a specified offset. The offset
    * is added to the pose, and subtracted from the mesh vertex
    * positions and inertia.
    */
   public void translateCoordinateFrame (Point3d off) {
      Point3d newPos = new Point3d(getPosition());
      Point3d newCom = new Point3d(getCenterOfMass());
      newPos.add (off);
      newCom.sub (off);
      Vector3d del = new Vector3d();
      del.negate (off);
      if (myInertiaMethod == InertiaMethod.EXPLICIT) {
         mySpatialInertia.setCenterOfMass (newCom);
      }
      for (RigidMeshComp mcomp : myMeshList) {
         mcomp.transformMesh (new RigidTransform3d (del.x, del.y, del.z));
      }
      setPosition (newPos);
   }

   /**
    * Transform the body coordinate frame. The transformation from the current
    * to the new coordinate frame is given by {@code TNB}, such the if the
    * body's current pose is given by {@code TBW}, then the new pose {@code
    * TNW} will be given by
    * <pre>
    *  TNW = TBW TNB
    * </pre>
    * This method also updates the vertex positions of the body's
    * meshes (using the inverse {@code TNB}), as
    * well as the inertia tensor.
    */
   public void transformCoordinateFrame (RigidTransform3d TNB) {
      RigidTransform3d TNW = new RigidTransform3d();
      TNW.mul (getPose(), TNB);
      if (myInertiaMethod == InertiaMethod.EXPLICIT) {
         mySpatialInertia.inverseTransform (TNB);
      }
      for (RigidMeshComp mcomp : myMeshList) {
         RigidTransform3d invTNB = new RigidTransform3d();
         invTNB.invert (TNB);
         mcomp.transformMesh (invTNB);
      }
      setPose (TNW);
   }

   /** 
    * Causes the inertia to be automatically computed from the mesh volume
    * and a given density. If the mesh is currently <code>null</code> then
    * the inertia remains unchanged. Subsequent (non-<code>null</code>) changes
    * to the mesh will cause the inertia to be recomputed.
    * The inertia method is set to
    * {@link RigidBody.InertiaMethod#DENSITY DENSITY}. 
    * 
    * @param density desired uniform density
    */
   public void setInertiaFromDensity (double density) {
      if (density < 0) {
         throw new IllegalArgumentException ("density must be non-negative");
      }
      myInertiaMethod = InertiaMethod.DENSITY;
      setBodyDensity (density);
   }

   /** 
    * Causes the inertia to be automatically computed from the mesh volume
    * and a given mass (with the density computed by dividing the mass
    * by the mesh volume). If the mesh is currently <code>null</code> the mass 
    * of the inertia is updated but the otherwise the inertia and density
    * are left unchanged. Subsequent (non-<code>null</code>) changes
    * to the mesh will cause the inertia to be recomputed.
    * The inertia method is set to {@link RigidBody.InertiaMethod#MASS MASS}. 
    * 
    * @param mass desired body mass
    */
   public void setInertiaFromMass (double mass) {
      if (mass < 0) {
         throw new IllegalArgumentException ("mass must be non-negative");
      }
      myInertiaMethod = InertiaMethod.MASS;      
      setBodyMass (mass);
   }
   
   public double getVolume() {
      return sumMeshVolumes();
   }

   /**
    * Sets the density for the mesh, which is defined at the mass divided
    * by the mesh volume. If the mesh is currently non-null, the mass
    * will be updated accordingly. If the current InertiaMethod
    * is either {@link RigidBody.InertiaMethod#DENSITY DENSITY} or 
    * {@link RigidBody.InertiaMethod#MASS MASS}, the other components of
    * the spatial inertia will also be updated.
    * 
    * @param density
    * new density value
    */
   public void setDensity (double density) {
      if (density < 0) {
         throw new IllegalArgumentException ("density must be non-negative");
      }
      if (density != myDensity) {
         setBodyDensity(density);
      }
   }

   public Range getDensityRange () {
      return DoubleInterval.NonNegative;
   }

   /**
    * Returns the density of this body. This is either the value set explictly 
    * by {@link #setInertiaFromDensity setInertiaFromDensity}, or is the
    * mass/volume ratio for the most recently defined mesh.
    * 
    * @return density for this body.
    */
   public double getDensity() {
      return myDensity;
   }

   /**
    * Sets the mass for the mesh. If the mesh is currently non-null, then the
    * density (defined as the mass divided by the mesh volume) will be updated
    * accordingly. If the current InertiaMethod is either {@link
    * InertiaMethod#DENSITY DENSITY} or
    * {@link RigidBody.InertiaMethod#MASS MASS}, the
    * other components of the spatial inertia will also be updated.
    * 
    * @param mass
    * new mass value
    */
   public void setMass (double mass) {
      if (mass < 0) {
         throw new IllegalArgumentException ("Mass must be non-negative");
      }
      setBodyMass (mass);
   }

   /**
    * Returns the mass of this body.
    */
   public double getMass() {
      return mySpatialInertia.getMass();
   }

   public Range getMassRange () {
      return DoubleInterval.NonNegative;
   }

   /**
    * Explicitly sets the rotational inertia of this body. Also sets the uniform
    * density (as returned by {@link #getDensity getDensity}) to be undefined).
    */
   public void setRotationalInertia (SymmetricMatrix3d J) {
      mySpatialInertia.setRotationalInertia (J);
      myInertiaMethod = InertiaMethod.EXPLICIT;
   }

   public void getRotationalInertia (SymmetricMatrix3d J) {
      mySpatialInertia.getRotationalInertia (J);
   }

   /**
    * Returns the rotational inertia of this body.
    * 
    * @return rotational inertia (should not be modified).
    */
   public SymmetricMatrix3d getRotationalInertia() {
      return mySpatialInertia.getRotationalInertia();
   }

   protected void doSetInertia (SpatialInertia M) {
      setBodyInertia (M);
      myInertiaMethod = InertiaMethod.EXPLICIT;
   }

   /**
    * Explicitly sets the spatial inertia of this body. Also sets the uniform
    * density (as returned by {@link #getDensity getDensity}) to be undefined).
    */
   public void setInertia (SpatialInertia M) {
      doSetInertia (M);
   }

   /**
    * Explicitly sets the mass and rotational inertia of this body. Also sets
    * the uniform density (as returned by {@link #getDensity getDensity}) to be
    * undefined).
    */
   public void setInertia (double m, SymmetricMatrix3d J) {
      SpatialInertia M = new SpatialInertia();
      M.set (m, J);
      doSetInertia (M);
   }

   /**
    * Explicitly sets the mass, rotational inertia, and center of mass of this
    * body. Also sets the uniform density (as returned by {@link #getDensity
    * getDensity}) to be undefined).
    */
   public void setInertia (double m, SymmetricMatrix3d J, Point3d com) {
      SpatialInertia M = new SpatialInertia();
      M.set (m, J, com);
      doSetInertia (M);
   }

   /**
    * Explicitly sets the mass and rotational inertia of this body. Also sets
    * the uniform density (as returned by {@link #getDensity getDensity}) to be
    * undefined).
    */
   public void setInertia (double m, double Jxx, double Jyy, double Jzz) {
      doSetInertia (
         new SpatialInertia (m, Jxx, Jyy, Jzz));
   }

   protected boolean hasMassMeshes() {
      for (RigidMeshComp mcomp : myMeshList) {
         if (mcomp.hasMass()) {
            return true;
         }
      }
      return false;
   }

   protected void updateInertiaFromMeshes () {
      if (hasMassMeshes()) {
         SpatialInertia Mtotal = new SpatialInertia();         
         for (RigidMeshComp mcomp : myMeshList) {
            if (mcomp.hasMass()) {
               Mtotal.add (mcomp.createInertia());
            }
         }
         mySpatialInertia.set (Mtotal);
      }
      else if (myInertiaMethod == InertiaMethod.DENSITY) {
         mySpatialInertia.setZero();
      }
   }

   protected void scaleExplicitMeshMasses (double scale) {
      for (RigidMeshComp mcomp : myMeshList) {
         if (mcomp.hasMass()) {
            if (mcomp.hasExplicitMass()) {
               mcomp.setMass (scale*mcomp.getMass());
            }
            else if (mcomp.hasExplicitDensity()) {
               mcomp.setDensity (scale*mcomp.getDensity());
            }
         }
      }
   }

   protected double sumMeshVolumes() {
      double volume = 0;
      for (RigidMeshComp mcomp : myMeshList) {
         if (mcomp.hasMass()) {
            volume += mcomp.getVolume();
         }
      }
      return volume;
   }

   protected double sumMeshMasses() {
      double mass = 0;
      for (RigidMeshComp mcomp : myMeshList) {
         if (mcomp.hasMass()) {
            mass += mcomp.getMass();
         }
      }
      return mass;
   }

   protected double sumExplicitMeshMasses() {
      double mass = 0;
      for (RigidMeshComp mcomp : myMeshList) {
         if (mcomp.hasMass() && mcomp.hasExplicitMassOrDensity()) {
            mass += mcomp.getMass();
         }
      }
      return mass;
   }

   protected double sumImplicitMeshVolumes() {
      double volume = 0;
      for (RigidMeshComp mcomp : myMeshList) {
         if (mcomp.hasMass() && !mcomp.hasExplicitMassOrDensity()) {
            volume += mcomp.getVolume();
         }
      }
      return volume;
   }

   protected void setBodyMass (double mass) {
      double oldMass = mySpatialInertia.getMass();
      mySpatialInertia.setMass (mass);
      if (oldMass != 0) {
         double scale = mass/oldMass;
         myDensity *= scale;
         scaleExplicitMeshMasses (scale);
      }
      if (myInertiaMethod != InertiaMethod.EXPLICIT) {
         updateInertiaFromMeshes();
      }
   }

   protected void setBodyInertia (SpatialInertia M) {
      double oldMass = mySpatialInertia.getMass();
      mySpatialInertia.set (M);
      if (oldMass != 0) {
         double scale = M.getMass()/oldMass;
         myDensity *= scale;
         scaleExplicitMeshMasses (scale);
      }
   }

   protected void setBodyDensity (double density) {
      myDensity = density;
      mySpatialInertia.setMass (sumMeshMasses());
      if (myInertiaMethod != InertiaMethod.EXPLICIT) {
         updateInertiaFromMeshes();
      }
   }

   protected void updateInertiaForVolumeChanges () {

      if (myInertiaMethod == InertiaMethod.DENSITY) {
         mySpatialInertia.setMass (sumMeshMasses());
      }
      else {
         double implicitMass = getMass()-sumExplicitMeshMasses();
         double implicitVolume = sumImplicitMeshVolumes();
         if (implicitVolume > 0) {
            myDensity = implicitMass/implicitVolume;
         }        
      }
      if (myInertiaMethod != InertiaMethod.EXPLICIT) {
         updateInertiaFromMeshes();
      }
   }

   protected void updateInertiaForMeshChanges (RigidMeshComp mcomp) {

      if (mcomp.hasExplicitMassOrDensity() || 
          myInertiaMethod == InertiaMethod.DENSITY) {
         mySpatialInertia.setMass (sumMeshMasses());
      }
      else if (mcomp.getVolume() > 0) { // then mesh has mass
         double implicitMass = getMass()-sumExplicitMeshMasses();
         double implicitVolume = sumImplicitMeshVolumes();
         if (implicitVolume > 0) {
            myDensity = implicitMass/implicitVolume;
         }
      }
      if (myInertiaMethod != InertiaMethod.EXPLICIT) {
         updateInertiaFromMeshes();
      }
   }

   //**************************************************************************

   /**
    * Returns the mesh component, if any, associated with the surface
    * mesh for this body. If there is no surface mesh, then {@code null}
    * is returned.
    * 
    * @return surface mesh component, or {@code null}.
    */
   public RigidMeshComp getSurfaceMeshComp() {
      for (RigidMeshComp mc : myMeshList) {
         if (mc.getMesh() instanceof PolygonalMesh) {
            return mc;
         }
      }
      return null;
   }

   /**
    * @deprecated Use {@link #getSurfaceMesh()} instead.
    */
   public PolygonalMesh getMesh() {
      return getSurfaceMesh();
   }

   /**
    * Returns the surface mesh for this rigid body. By definition, this
    * is the first mesh in the component mesh list which is
    * a {@link PolygonalMesh}. If no such mesh exists, then
    * {@code null} is returned.
    * 
    * @return surface mesh for this rigid body
    */
   public PolygonalMesh getSurfaceMesh() {
      RigidMeshComp mc = getSurfaceMeshComp();
      if (mc != null) {
         return (PolygonalMesh)mc.getMesh();
      }
      else {
         return null;
      }
   }

   @Override
   public int numSurfaceMeshes() {
      return MeshComponent.numSurfaceMeshes (myMeshList);
   }
   
   @Override
   public PolygonalMesh[] getSurfaceMeshes() {
      return MeshComponent.getSurfaceMeshes (myMeshList);
   }

   /**
    * @deprecated Use {@link #setSurfaceMesh(PolygonalMesh)} instead.
    */
   public void setMesh (PolygonalMesh mesh) {
      setSurfaceMesh (mesh, null, null);
   }
   
   /**
    * @deprecated Use {@link #setSurfaceMesh(PolygonalMesh,String)} instead.
    */   
   public void setMesh (PolygonalMesh mesh, String fileName) {
      setSurfaceMesh (mesh, fileName, null);
   }
   
   /**
    * @deprecated Use {@link 
    * #setSurfaceMesh(PolygonalMesh,String,AffineTransform3dBase)} instead.
    */
   public void setMesh (
      PolygonalMesh mesh, String fileName, AffineTransform3dBase X) {
      setSurfaceMesh (mesh, fileName, X);
   }
   
   /**
    * Sets the surface mesh for this body. This method is equivalent
    * to {@link #setSurfaceMesh(PolygonalMesh,String,AffineTransform3dBase)
    * setSurfaceMesh(mesh,fileName,X)} with {@code fileName} and
    * {@code X} set to {@code null}.
    * 
    * @param mesh new surface mesh
    */
   public void setSurfaceMesh (PolygonalMesh mesh) {
      setSurfaceMesh (mesh, null, null);
   }
    
   /**
    * Sets the surface mesh for this body. This method is equivalent
    * to {@link #setSurfaceMesh(PolygonalMesh,String,AffineTransform3dBase)
    * #setSurfaceMesh(mesh,fileName,X)} with {@code X} set to {@code null}.
    * 
    * @param mesh new surface mesh
    * @param fileName optional file name to be associated with the mesh
    */
   public void setSurfaceMesh (PolygonalMesh mesh, String fileName) {
      setSurfaceMesh (mesh, fileName, null);
   }
   
   /**
    * Sets the surface mesh for this body. If a surface mesh already exists
    * (i.e., {@link #getSurfaceMesh} returns non-{@code null}), then this mesh
    * replaces the existing one, at the same location within the {\tt meshes}
    * sublist. Otherwise, a new surface mesh component is created and placed at
    * the top of the {\tt meshes} sublist.
    *
    * <p>If {@code mesh} is {@code null}, then the any existing surface mesh is
    * removed.
    *
    * <p>The arguments {@code fileName} and {@code X} specify an optional file
    * name and rigid or affine transform for the mesh. If specified, these are
    * used by {@link #scan} and {@link #write} for saving and restoring mesh
    * information. For details, see {@link MeshComponent#getFileName} and
    * {@link MeshComponent#getFileTransform}.
    *
    * <p>If the body's inertia method is {@link InertiaMethod#MASS} or {@link
    * InertiaMethod#DENSITY}, then its inertia will be updated to reflect the
    * new mesh geometry.
    * 
    * @param mesh new surface mesh
    * @param fileName optional file name to be associated with the mesh
    * @param X optional affine transform to be associated with the mesh
    */
   public void setSurfaceMesh (
      PolygonalMesh mesh, String fileName, AffineTransform3dBase X) {
         
      // see if there is a surface mesh component already
      RigidMeshComp oldComp = getSurfaceMeshComp();
      if (oldComp != null) {
         if (mesh == null) {
            // remove the existing surface mesh
            removeMeshComp (oldComp);
         }
         else {
            oldComp.setMesh (mesh, fileName, X);
         }
      }
      else if (mesh != null) {
         RigidMeshComp newComp = new RigidMeshComp (mesh, fileName, X);
         if (myMeshList.get ("surfaceMesh") == null) {
            // name the component "surfaceMesh" if that name is not taken
            newComp.setName ("surfaceMesh");
         }
         addMeshComp (newComp, 0);
      }
      setSurfaceMeshFromInfo();
   }

   public void scaleSurfaceMesh (double sx, double sy, double sz) {
      RigidMeshComp mc = getSurfaceMeshComp();
      if (mc != null) {
         mc.scaleMesh (sx, sy, sz);
      }
   }

   protected void setSurfaceMeshFromInfo () {
      PolygonalMesh mesh = getSurfaceMesh();
      if (mesh != null) {
         mesh.setFixed (true);
         mesh.setMeshToWorld (myState.XFrameToWorld);
      }
   }

   /* --- methods for mesh components --- */

   public MeshComponentList<RigidMeshComp> getMeshComps() {
      return myMeshList;
   }
   
   public RigidMeshComp getMeshComp(int idx) {
      return myMeshList.get (idx);
   }

   public RigidMeshComp getMeshComp(String name) {
      return myMeshList.get (name);
   }

   /**
    * Adds a mesh to this object.  Can be of any type.
    * @param mesh Instance of MeshBase
    * @return a special "mesh component" object that is created internally 
    */
   public RigidMeshComp addMesh(MeshBase mesh) {
      return addMesh (
         mesh, null, null, 
         RigidMeshComp.DEFAULT_HAS_MASS, RigidMeshComp.DEFAULT_IS_COLLIDABLE);
   }

   /**
    * Adds a mesh to this object.  Can be of any type.
    * @param mesh Instance of MeshBase
    * @param hasMass true if to be used for mass/inertia calculations 
    * @param collidable true if to be used for collisions
    * @return a special "mesh component" object that is created internally 
    */
   public RigidMeshComp addMesh (
      MeshBase mesh, boolean hasMass, boolean collidable) {
      return addMesh(mesh, null, null, hasMass, collidable);
   }

   /**
    * Adds a mesh to this object
    * @param mesh Instance of MeshBase
    * @param fileName name of file (can be null)
    * @param Xh transform associated with mesh
    * @param hasMass if true, this mesh is used for computing mass and inertia
    * @return a special "mesh component" object that is created internally 
    */
   public RigidMeshComp addMesh (
      MeshBase mesh, String fileName, AffineTransform3dBase Xh, 
      boolean hasMass, boolean collidable) {

      RigidMeshComp mc = new RigidMeshComp();
      mc.setMesh(mesh, fileName, Xh);
      if (mesh.getName() != null) {
         mc.setName(mesh.getName());
      }
      mc.setHasMass(hasMass);
      mc.setIsCollidable (collidable);

      if (mesh.getRenderProps() != null) {
         mc.setRenderProps(mesh.getRenderProps());
      }

      addMeshComp(mc);

      return mc;
   }

   /**
    * Explicitly adds a mesh component.  If the flag mc.hasMass() is true,
    * then this mesh is used for mass/inertia computations
    */
   public void addMeshComp(RigidMeshComp mc) {
      addMeshComp (mc, myMeshList.size());
   }

   protected void addMeshComp (RigidMeshComp mc, int idx) {
      mc.getMesh().setFixed(true);
      mc.getMesh().setMeshToWorld (myState.XFrameToWorld);
      myMeshList.add(mc, idx);
   }

   /**
    * Number of meshes associated with this object
    */
   public int numMeshComps() {
      return myMeshList.size();
   }

   /**
    * Checks if this object contains a particular geometry
    */
   public boolean containsMeshComp(RigidMeshComp mc) {
      return myMeshList.contains(mc);
   }

   public boolean removeMeshComp(RigidMeshComp mc) {
      return myMeshList.remove(mc);
   }

   public RigidMeshComp removeMeshComp(String name) {
      RigidMeshComp mesh = getMeshComp(name);
      if (mesh != null) {
         myMeshList.remove(mesh);
      }
      return null;
   }

   public void clearMeshComps() {
      myMeshList.removeAll();
   }  
 
   /* --- --- */

   @Override
   protected void updatePosState() {
      updateAttachmentPosStates();
      updateSlavePosStates();
   }

   @Override
   protected void updateSlavePosStates() {
      PolygonalMesh mesh = getSurfaceMesh();
      if (mesh != null) {
         mesh.setMeshToWorld (myState.XFrameToWorld);
      }
      for (RigidMeshComp mc : myMeshList) {
         MeshBase mbase = mc.getMesh();
         mbase.setMeshToWorld(myState.XFrameToWorld);
      }
      myDistanceGridComp.setLocalToWorld (myState.XFrameToWorld);
      if (myCompoundCollisionMesh != null) {
         myCompoundCollisionMesh.setMeshToWorld (myState.XFrameToWorld);
      }
   } 

   /**
    * Replace updatePosState() with updateSlavePosStates() so we don't update
    * attachments when doing general state uodates. (Attachment updates should
    * be done by the general state update itself).
    */
   public int setPosState (double[] buf, int idx) {
      idx = myState.setPos (buf, idx);
      updateSlavePosStates(); // replaces updatePosState()
      return idx;
   }

   public void setPose (double x, double y, double z,
                        double roll, double pitch, double yaw) {
      RigidTransform3d X = new RigidTransform3d();
      X.p.set (x, y, z);
      X.R.setRpy (Math.toRadians(roll),
                  Math.toRadians(pitch),
                  Math.toRadians (yaw));
      setPose (X);
   }

   public void extrapolatePose (Twist vel, double h) {
      myState.adjustPose (vel, h);
      updatePosState();
   }

   public RigidBody() {
      super();
      // Give the default inertia unit mass and rotational inertia,
      // so that even if the body is inactives, we don't need to
      // handle zero inertia in the numerics code.
      mySpatialInertia = new SpatialInertia (1, 1, 1, 1);
      //mySpatialInertia = new SpatialInertia ();
      // myEffectiveInertia = new SpatialInertia (1, 1, 1, 1);
      setDynamic (true);
      setRenderProps (createRenderProps());
      initializeChildComponents();
   }

   public RigidBody (String name) {
      this();
      setName (name);
   }

   public RigidBody (RigidTransform3d XBodyToWorld, SpatialInertia M,
   PolygonalMesh mesh, String meshFileName) {
      this();
      myState.setPose (XBodyToWorld);
      setInertia (M);
      setMesh (mesh, meshFileName);
   }
   
   public RigidBody (
      String name, PolygonalMesh mesh, String meshFilePath, 
      double density, double scale) {
      this (name);
      if (!mesh.isTriangular()) {
         throw new IllegalArgumentException("Mesh is not triangular");
      }
      AffineTransform3d X = null;
      if (scale != 1) {
         mesh.scale (scale);
         X = AffineTransform3d.createScaling (scale);
      }    
      setDensity (density);
      setMesh (mesh, meshFilePath, X);
   }

   protected DistanceGridComp createDistanceGridComp() {
      DistanceGridComp comp = new DistanceGridComp ("distanceGrid");
      //RenderProps.setVisible (comp, false);
      comp.setRenderGrid (false);
      RenderProps.setVisible (comp, true);
      // RenderProps.setPointRadius (comp, 0.0);
      // RenderProps.setPointSize (comp, 0);
      // RenderProps.setLineStyle (comp, LineStyle.LINE);
      // RenderProps.setLineColor (comp, Color.BLUE);      
      return comp;
   }

   /**
    * {@inheritDoc}
    */
   public DistanceGridComp getDistanceGridComp() {
      return myDistanceGridComp;
   }

   protected void initializeChildComponents() {
      myComponents = 
         new ComponentListImpl<ModelComponent>(ModelComponent.class, this);
      myMeshList =
         new MeshComponentList<RigidMeshComp>(
            RigidMeshComp.class, "meshes", "msh");

      myDistanceGridComp = createDistanceGridComp();

      add (myMeshList);
      add (myDistanceGridComp);
   }

   public void applyGravity (Vector3d gacc) {
      // apply a force of -mass gacc at the bodies's center of mass
      Point3d com = new Point3d();
      Vector3d fgrav = new Vector3d();
      //      SpatialInertia inertia = MechModel.getEffectiveInertia(this);
      SpatialInertia inertia = mySpatialInertia;
      inertia.getCenterOfMass (com);
      com.transform (myState.XFrameToWorld.R);
      fgrav.scale (inertia.getMass(), gacc);
      myForce.f.add (fgrav, myForce.f);
      myForce.m.crossAdd (com, fgrav, myForce.m);
   }

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      myComponents.scanBegin();
      myState.vel.setZero();
      super.scan (rtok, ref);
      updateCollisionMeshes();
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      // if (ScanWriteUtils.scanProperty (rtok, this)) {
      //    return true;
      // }
      if (myComponents.scanAndStoreComponentByName (rtok, tokens)) {
         return true;
      }
      // else if (scanAttributeName (rtok, "mesh")) {
      //    myMeshInfo.scan (rtok);  
      //    setMeshFromInfo();
      //    return true;
      // }
      else if (scanAttributeName (rtok, "pose")) {
         RigidTransform3d X = new RigidTransform3d();
         X.scan (rtok);
         myState.setPose (X);
         return true;
      }
      else if (scanAttributeName (rtok, "position")) {
         Point3d pos = new Point3d();
         pos.scan (rtok);
         setPosition (pos);
         return true;
      }
      else if (scanAttributeName (rtok, "rotation")) {
         Quaternion q = new Quaternion();
         q.scan (rtok);
         setRotation (q);
         return true;
      }
      else if (scanAttributeName (rtok, "vel")) {
         rtok.scanToken ('[');
         myState.vel.scan (rtok);
         rtok.scanToken (']');
         return true;
      }
      else if (scanAttributeName (rtok, "inertia")) {
         SpatialInertia M = new SpatialInertia();
         M.scan (rtok);
         setInertia (M);           
         return true;
      }
      else if (scanAttributeName (rtok, "mass")) {
         double mass = rtok.scanNumber();
         setMass (mass);
         return true;
      }
      else if (scanAttributeName (rtok, "density")) {
         double density = rtok.scanNumber();
         setDensity (density);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) 
         throws IOException {

      if (myComponents.postscanComponent (tokens, ancestor)) {
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   @Override
   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      myComponents.scanEnd();
      updatePosState();
      if (myInertiaMethod != InertiaMethod.EXPLICIT) {
         updateInertiaFromMeshes();
      }
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt, ancestor);
      pw.println ("position=[ " + myState.getPosition().toString (fmt) + "]");
      pw.println ("rotation=[ " + myState.getRotation().toString (fmt) + "]");
      // pw.println ("pose=" + myState.XFrameToWorld.toString (
      //    fmt, RigidTransform3d.AXIS_ANGLE_STRING));
      if (!myState.vel.v.equals (zeroVect) || !myState.vel.w.equals (zeroVect)) {
         pw.print ("vel=[ ");
         pw.print (myState.vel.toString (fmt));
         pw.println (" ]");
      }
      switch (myInertiaMethod) {
         case EXPLICIT: {
            pw.println (
               "inertia=" + mySpatialInertia.toString (
                  fmt, SpatialInertia.MASS_INERTIA_STRING));
            break;
         }
         case MASS: {
            pw.println ("mass=" + fmt.format (getMass()));
            break;
         }
         case DENSITY: {
            pw.println ("density=" + fmt.format (getDensity()));
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented inertia method " + myInertiaMethod);
         }
      }
      //myMeshInfo.write (pw, fmt);
      myComponents.writeComponentsByName (pw, fmt, ancestor);
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      dowrite (pw, fmt, ref);
   }


   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createRenderProps (this);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      PolygonalMesh mesh = getMesh();
      if (mesh != null) {
         mesh.updateBounds (pmin, pmax);
      }
      else {
         myState.pos.updateBounds (pmin, pmax);
      }
      for (RigidMeshComp mc : myMeshList) {
         mc.updateBounds(pmin, pmax);
      }
   }

   public void prerender (RenderList list) {
      myRenderFrame.set (myState.XFrameToWorld);
      if (myRenderProps == null) {
         throw new InternalErrorException (
            "RigidBody has null RenderProps");
      }
      PolygonalMesh surf = null;
      if (myGridSurfaceRendering && (surf = getDistanceSurface()) != null) {
         surf.prerender (myRenderProps); 
      }
      else {
         if (myMeshList.size() > 1) {
            // If more than one mesh, render them separately. 
            list.addIfVisible (myMeshList);
         }
         else if (myMeshList.size() == 1) {
            // Otherwise, render directly, so rigid body can be 
            // selected directly
            RigidMeshComp mcomp = myMeshList.get(0);
            if (mcomp.getRenderProps().isVisible()) {
               mcomp.myMeshInfo.prerender(mcomp.getRenderProps());
            }
         }
      }
      mySDRenderSurface = surf;
      list.addIfVisible (myDistanceGridComp);
      //list.addIfVisible (myMeshList);
   }
   
   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
      if (isSelected()) {
         flags |= Renderer.HIGHLIGHT;
      }
      PolygonalMesh surf = null;
      if (myGridSurfaceRendering && (surf = mySDRenderSurface) != null) {
         surf.render (renderer, myRenderProps, flags);
      }
      else if (myMeshList.size() == 1) {
         // If only one mesh, render directly, so rigid body can be selected
         // directly
         RigidMeshComp mcomp = myMeshList.get(0);
         if (mcomp.getRenderProps().isVisible()) {
            mcomp.myMeshInfo.render (renderer, mcomp.getRenderProps(), flags);
         }
      }
   }
   
   protected static class UpdateInertiaAction implements TransformGeometryAction {

      RigidBody myBody;

      UpdateInertiaAction (RigidBody body) {
         myBody = body;
      }

      public void transformGeometry (
         GeometryTransformer gtr, TransformGeometryContext context, int flags) {
         myBody.updateInertiaForVolumeChanges();     
      }
      
      public int hashCode() {
         return myBody.hashCode();
      }

      public boolean equals (Object obj) {
         return (obj instanceof UpdateInertiaAction &&
                 ((UpdateInertiaAction)obj).myBody == myBody);
      }     
   }

   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      context.addAll (myMeshList);
      context.add (myDistanceGridComp);
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      
      super.transformGeometry (gtr, context, flags);
      
      if (!gtr.isRigid()) {
         context.addAction (new UpdateInertiaAction(this));
      }
      myCompoundCollisionMesh = null;
   }   
   
   public void scaleDistance (double s) {
      super.scaleDistance (s);
      this.myAxisLength *= s;

      for (RigidMeshComp mc : myMeshList) {
         mc.scaleDistance(s);
         mc.getMesh().setMeshToWorld(myState.XFrameToWorld);
      }
      myDensity /= (s * s * s);
      if (myInertiaMethod == InertiaMethod.EXPLICIT) {
         mySpatialInertia.scaleDistance (s);
      }
      else {
         updateInertiaFromMeshes();
      }
      if (myDistanceGridComp != null) {
         myDistanceGridComp.scaleDistance (s);
      }
      if (myCompoundCollisionMesh != null) {
         myCompoundCollisionMesh.scale (s);
      }
   }

   public void scaleMass (double s) {
      mySpatialInertia.scaleMass (s);
      for (RigidMeshComp mc : myMeshList) {
         mc.scaleMass(s);
      }
      myDensity *= s;
      myFrameDamping *= s;
      myRotaryDamping *= s;
      if (myInertiaMethod == InertiaMethod.EXPLICIT) {
         mySpatialInertia.scaleMass (s);
      }
      else {
         updateInertiaFromMeshes();
      }
   }

   public void setDynamic (boolean dynamic) {
      super.setDynamic (dynamic);
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
      return true;
   }

   @Override
   public RigidBody copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      RigidBody comp = (RigidBody)super.copy (flags, copyMap);

      comp.setDynamic (true);

      comp.mySpatialInertia = new SpatialInertia (mySpatialInertia);

      comp.myBodyForce = new Wrench();
      comp.myCoriolisForce = new Wrench();
      comp.myBodyVel = new Twist();
      comp.myBodyAcc = new Twist();
      comp.myQvel = new Quaternion();
      comp.myTmpPos = new Point3d();
      comp.myConnectors = null;
      
      comp.initializeChildComponents();
      comp.setName(null);
      comp.setNavpanelVisibility(getNavpanelVisibility());

      for (int i=0; i<myMeshList.size(); i++) {
         RigidMeshComp mc = myMeshList.get(i);
         RigidMeshComp newFmc = mc.copy(flags, copyMap);
         comp.addMeshComp(newFmc);
      }     

      return comp;
   }

   /**
    * Returns an array of all FrameMarkers currently associated with this rigid
    * body.
    * 
    * @return list of all frame markers
    */
   public FrameMarker[] getFrameMarkers() {
      LinkedList<FrameMarker> list = new LinkedList<FrameMarker>();
      if (myMasterAttachments != null) {
         for (DynamicAttachment a : myMasterAttachments) {
            if (a.getSlave() instanceof FrameMarker) {
               list.add ((FrameMarker)a.getSlave());
            }
         }
      }
      return list.toArray (new FrameMarker[0]);
   }

   public void addConnector (BodyConnector c) {
      if (myConnectors == null) {
         myConnectors = new ArrayList<BodyConnector>();
      }
      myConnectors.add (c);
   }

   public void removeConnector (BodyConnector c) {
      if (myConnectors == null || !myConnectors.remove (c)) {
         throw new InternalErrorException ("connector not found");
      }
      if (myConnectors.size() == 0) {
         myConnectors = null;
      }
   }
   
   public boolean containsConnector (BodyConnector c) {
      return (myConnectors != null && myConnectors.contains(c)); 
   }

   public List<BodyConnector> getConnectors() {
      return myConnectors;
   }
   
   public boolean isFreeBody() {
      return !isParametric();
   }

   /** 
    * Creates an spherical RigidBody with a prescribed uniform density.
    * The sphere is centered on the origin.
    * 
    * @param bodyName name of the RigidBody
    * @param r radius of the sphere
    * @param density density of the body
    * @param nslices number of slices used in creating the mesh
    * @return spherical rigid body
    */
   public static RigidBody createSphere (
      String bodyName, double r, double density, int nslices) {

      RigidBody body = new RigidBody (bodyName);
      PolygonalMesh mesh = MeshFactory.createSphere (r, nslices);
      body.setMesh (mesh, null);
      double mass = 4/3.0*Math.PI*r*r*r*density;
      body.setInertia (SpatialInertia.createSphereInertia (mass, r));
      return body;
   }

   /** 
    * Creates an icosahedrally spherical RigidBody with a prescribed uniform
    * density.  The sphere is centered on the origin.
    * 
    * @param bodyName name of the RigidBody
    * @param r radius of the sphere
    * @param density density of the body
    * @param ndivisions number of divisions used in creating the mesh
    * @return spherical rigid body
    */
   public static RigidBody createIcosahedralSphere (
      String bodyName, double r, double density, int ndivisions) {

      RigidBody body = new RigidBody (bodyName);
      PolygonalMesh mesh = MeshFactory.createIcosahedralSphere (
         r, Point3d.ZERO, ndivisions);
      body.setMesh (mesh, null);
      double mass = 4/3.0*Math.PI*r*r*r*density;
      body.setInertia (SpatialInertia.createSphereInertia (mass, r));
      return body;
   }

   /** 
    * Creates an ellipsoidal RigidBody with a prescribed uniform density.
    * The ellipsoid is centered on the origin.
    * 
    * @param bodyName name of the RigidBody
    * @param a semi-axis length in the x direction
    * @param b semi-axis length in the y direction
    * @param c semi-axis length in the z direction
    * @param density density of the body
    * @param nslices number of slices used in creating the mesh
    * @return ellipsoidal rigid body
    */
   public static RigidBody createEllipsoid (
      String bodyName, double a, double b, double c,
      double density, int nslices) {

      RigidBody body = new RigidBody (bodyName);
      PolygonalMesh mesh = MeshFactory.createSphere (1.0, nslices);
      AffineTransform3d XScale = new AffineTransform3d();
      XScale.applyScaling (a, b, c);
      mesh.transform (XScale);
      body.setMesh (mesh, null);
      double mass = 4/3.0*Math.PI*a*b*c*density;
      body.setInertia (
         SpatialInertia.createEllipsoidInertia (mass, a, b, c));
      return body;
   }

   /** 
    * Creates a box-shaped RigidBody with a prescribed uniform density.
    * The box is centered on the origin.
    * 
    * @param bodyName name of the RigidBody
    * @param wx width of the box in the x direction
    * @param wy width of the box in the y direction
    * @param wz width of the box in the z direction
    * @param density density of the body
    * @return box-shaped rigid body
    */
   public static RigidBody createBox (
      String bodyName, double wx, double wy, double wz, double density) {

      RigidBody body = new RigidBody (bodyName);
      PolygonalMesh mesh = MeshFactory.createBox (wx, wy, wz);
      body.setInertiaFromDensity (density);
      body.setMesh (mesh, null);
      return body;
   }

   /** 
    * Creates a box-shaped RigidBody with a prescribed uniform density.
    * The box is centered on the origin.
    * 
    * @param bodyName name of the RigidBody
    * @param wx width of the box in the x direction
    * @param wy width of the box in the y direction
    * @param wz width of the box in the z direction
    * @param density density of the body
    * @return box-shaped rigid body
    */
   public static RigidBody createBox (
      String bodyName, double wx, double wy, double wz, double density,
      boolean addNormals) {

      RigidBody body = new RigidBody (bodyName);
      PolygonalMesh mesh = MeshFactory.createBox (wx, wy, wz, addNormals);
      body.setInertiaFromDensity (density);
      body.setMesh (mesh, null);
      return body;
   }
   
   /** 
    * Creates a box-shaped RigidBody with a prescribed uniform density.
    * The box is centered on the origin.
    * 
    * @param bodyName name of the RigidBody
    * @param wx width of the box in the x direction
    * @param wy width of the box in the y direction
    * @param wz width of the box in the z direction
    * @param nx number of mesh divisions in the x direction
    * @param ny number of mesh divisions in the y direction
    * @param nz number of mesh divisions in the z direction
    * @param density density of the body
    * @return box-shaped rigid body
    */
   public static RigidBody createBox (
      String bodyName, double wx, double wy, double wz,
      int nx, int ny, int nz, double density,
      boolean addNormals) {

      RigidBody body = new RigidBody (bodyName);
      PolygonalMesh mesh =
         MeshFactory.createBox (
            wx, wy, wz, Point3d.ZERO, nx, ny, nz, addNormals);
      body.setInertiaFromDensity (density);
      body.setMesh (mesh, null);
      return body;
   }
   
   /** 
    * Creates a cylindrical RigidBody with a prescribed uniform density.  The
    * cylinder is centered on the origin and is parallel to the z axis.
    * 
    * @param bodyName name of the RigidBody
    * @param r cylinder radius
    * @param h cylinder height
    * @param density density of the body
    * @param nsides number of sides used in creating the cylinder mesh
    * @return cylindrical rigid body
    */
   public static RigidBody createCylinder (
      String bodyName, double r, double h,
      double density, int nsides) {

      RigidBody body = new RigidBody (bodyName);
      PolygonalMesh mesh =
         MeshFactory.createCylinder (r, h, nsides);
      body.setMesh (mesh, null);
      // body.setDensity (density);
      double mass = Math.PI*r*r*h*density;
      body.setInertia (
         SpatialInertia.createCylinderInertia (mass, r, h));
      return body;
   }

   public static RigidBody createFromMesh (
      String bodyName, PolygonalMesh mesh, double density, double scale) {
      
      return createFromMesh (bodyName, mesh, null, density, scale);
   }

   public static RigidBody createFromMesh (
      String bodyName, PolygonalMesh mesh, String meshFilePath,
      double density, double scale) {

      RigidBody body = new RigidBody (bodyName);
      mesh.triangulate(); // just to be sure ...
      mesh.scale (scale);
      AffineTransform3d X = null;
      if (scale != 1) {
         X = AffineTransform3d.createScaling (scale);
      }
      body.setDensity (density);
      body.setMesh (mesh, meshFilePath, X);
      return body;
   }
      
   public static RigidBody createFromMesh (
      String bodyName, String meshPath, double density, double scale) {

      PolygonalMesh mesh = null;
      try {
         mesh = new PolygonalMesh (new File (meshPath));
      }
      catch (IOException e) {
         System.out.println ("Can't create mesh from "+meshPath);
         e.printStackTrace();
         return null;
      }
      return createFromMesh (bodyName, mesh, meshPath, density, scale);
   }

   public static RigidBody createFromMesh (
      String bodyName, Object obj, String relPath, double density, double scale){

      String path = ArtisynthPath.getSrcRelativePath (obj, relPath);
      return createFromMesh (bodyName, path, density, scale);
   }
    
   protected PolygonalMesh buildCompoundMesh (ArrayList<MeshComponent> meshes) {
      PolygonalMesh compound = null;
      for (MeshComponent mcomp : meshes) {
         if (mcomp.getMesh() instanceof PolygonalMesh) {
            PolygonalMesh mesh = (PolygonalMesh)mcomp.getMesh();
            if (compound == null) {
               compound = mesh.copy();
            }
            else {
               compound.addMesh (mesh, false);
            }
         }
      }
      return compound;
   }

   @Override
   public PolygonalMesh getCollisionMesh() {
      if (myCollisionMeshes.size() > 0) {
         if (myCollisionMeshes.size() == 1) {
            return (PolygonalMesh)myCollisionMeshes.get(0).getMesh();
         }
         else {
            if (myCompoundCollisionMesh == null) {
               myCompoundCollisionMesh = buildCompoundMesh (myCollisionMeshes);
               myCompoundCollisionMesh.setMeshToWorld (getPose());
            }
            return myCompoundCollisionMesh;
         }
      }
      else {
         return null;
      }
   }

   /**
    * Returns <code>true</code> if this RigidBody supports a signed
    * distance grid that can be used with a SignedDistanceCollider.
    * The grid itself can be obtained with {@link #getDistanceGrid}.
    * 
    * @return <code>true</code> if a signed distance grid is available
    * for this RigidBody
    */
   public boolean hasDistanceGrid() {
      return myDistanceGridComp.hasGrid();
   }

   protected void updateCollisionMeshes() {
      myCollisionMeshes.clear();      
      for (RigidMeshComp mc : myMeshList) {
         if (mc.isCollidable()) {
            myCollisionMeshes.add (mc);
         }
         myDistanceGridComp.setGeneratingMeshes (myCollisionMeshes);
      }
      myCompoundCollisionMesh = null;
   }

   /**
    * Returns a signed distance grid that can be used with a
    * SignedDistanceCollider, or <code>null</code> if a grid is not available.
    * Aspects of the grid, including its visibility, resolution, and
    * mesh fit, can be controlled using properties of its encapsulating
    * {@link DistanceGridComp}, returned by {@link #getDistanceGridComp}.
    *
    * @return signed distance grid, or <code>null</code> if a grid is
    * not available this RigidBody
    */
   public DistanceGrid getDistanceGrid() {
      return myDistanceGridComp.getGrid();
   }
   
   protected PolygonalMesh getDistanceSurface() {
      return myDistanceGridComp.getSurface();
   }

   @Override
   public Collidability getCollidable () {
      getSurfaceMesh(); // build surface mesh if necessary
      return myCollidability;
   }

   @Override
   public Collidable getCollidableAncestor() {
      return null;
   }

   @Override
   public boolean isCompound() {
      return true;
   }

   public void setCollidable (Collidability c) {
      if (myCollidability != c) {
         myCollidability = c;
         notifyParentOfChange (new StructureChangeEvent (this));
      }
   }

   @Override
   public boolean isDeformable () {
      return false;
   }

   public void collectVertexMasters (
      List<ContactMaster> mlist, Vertex3d vtx) {
      mlist.add (this);
   }
   
   public boolean containsContactMaster (CollidableDynamicComponent comp) {
      return comp == this;
   }  
   
   public boolean allowCollision (
      ContactPoint cpnt, Collidable other, Set<Vertex3d> attachedVertices) {
      return true;
   }

   public int getCollidableIndex() {
      return myCollidableIndex;
   }
   
   public void setCollidableIndex (int idx) {
      myCollidableIndex = idx;
   }

   /* --- Wrappable --- */

   /**
    * {@inheritDoc}
    */
   public void surfaceTangent (
      Point3d pr, Point3d pa, Point3d p1, double lam0, Vector3d sideNrm) {
      myDistanceGridComp.surfaceTangent (pr, pa, p1, lam0, sideNrm);
   }

   /**
    * {@inheritDoc}
    */   
   public double penetrationDistance (Vector3d nrm, Matrix3d Dnrm, Point3d p0) {
      return myDistanceGridComp.penetrationDistance (nrm, Dnrm, p0);
   }

   /**
    * {@inheritDoc}
    */
   public double getCharacteristicRadius() {
      DistanceGrid grid = getDistanceGrid();
      if (grid != null) {
         return grid.getWidths().minElement()/2;
      }
      else {
         return RenderableUtils.getRadius (this);         
      }
   }

   // end Collidable interface

   // begin HasDistanceGrid

   /**
    * Convenience method to call {@link DistanceGridComp#setResolution}
    * for this body's distance grid.
    * 
    * @param res x, y, and z cell divisions to be used in constructing the grid
    */
   public void setDistanceGridRes (Vector3i res) {
      myDistanceGridComp.setResolution (res);
   }

   /**
    * Convenience method to call {@link DistanceGridComp#getResolution}
    * for this body's distance grid.
    * 
    * @return x, y, and z cell divisions to be used in constructing the grid
    */
   public Vector3i getDistanceGridRes () {
      return myDistanceGridComp.getResolution();
   }

   /**
    * Queries if grid surface rendering is enabled.
    *
    * @return {@code true} if grid surface rendering is enabled
    * @see #setGridSurfaceRendering
    */
   public boolean getGridSurfaceRendering() {
      return myGridSurfaceRendering;
   }

   /**
    * Enables or disables grid surface rendering. If enabled, this means that
    * the iso surface of this rigid body's distance grid will be rendered
    * <i>instead</i> of its surface mesh(es). This rendering will occur
    * independently of the visibility settings for the meshes or the body's
    * distance grid component. Characteristics of the grid
    * surface (such its distance value, or whether it is created using linear
    * of quadratic interpolation) can be controlled by setting properties of
    * this body's {@link DistanceGridComp}, returned by
    * {@link #getDistanceGridComp}.
    *
    * @param enable if {@code true}, enables grid surface rendering.
    * @see #getGridSurfaceRendering
    */
   public void setGridSurfaceRendering (boolean enable) {
      if (myGridSurfaceRendering != enable) {
         myGridSurfaceRendering = enable;
      }
   }

   /* --- Composite component --- */

   public void updateNameMap (
      String newName, String oldName, ModelComponent comp) {
      myComponents.updateNameMap (newName, oldName, comp);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent findComponent (String path) {
      return ComponentUtils.findComponent (this, path);
   }

   public void add (ModelComponent comp) {
      myComponents.add (comp);
   }

   public boolean remove (ModelComponent comp) {
      return myComponents.remove (comp);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent get (String nameOrNumber) {
      return myComponents.get (nameOrNumber);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent get (int idx) {
      return myComponents.get (idx);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent getByNumber (int num) {
      return myComponents.getByNumber (num);
   }

   /**
    * {@inheritDoc}
    */
   public int getNumberLimit() {
      return myComponents.getNumberLimit();
   }

   /**
    * {@inheritDoc}
    */
   public int indexOf (ModelComponent comp) {
      return myComponents.indexOf (comp);
   }

   /**
    * {@inheritDoc}
    */
   public int numComponents() {
      return myComponents.size();
   }

   /**
    * {@inheritDoc}
    */
   public void componentChanged (ComponentChangeEvent e) {
      myComponents.componentChanged (e);
      if (e.getComponent() == myMeshList) {
         updateInertiaForVolumeChanges();
         updateCollisionMeshes();
      }
      else if (e instanceof StructureChangeEvent &&
               e.getComponent() instanceof RigidMeshComp) {
         updateCollisionMeshes();
      }
      notifyParentOfChange (e);
   }

   protected void notifyStructureChanged (Object comp) {
      if (comp instanceof CompositeComponent) {
         notifyParentOfChange (new StructureChangeEvent (
            (CompositeComponent)comp));
      }
      else {
         notifyParentOfChange (StructureChangeEvent.defaultEvent);
      }
   }

   /**
    * {@inheritDoc}
    */
   public NavpanelDisplay getNavpanelDisplay() {
      return myDisplayMode;
   }

   /**
    * Sets the display mode for this component. This controls
    * how the component is displayed in a navigation panel. The default
    * setting is <code>NORMAL</code>.
    *
    * @param mode new display mode
    */
   public void setDisplayMode (NavpanelDisplay mode) {
      myDisplayMode = mode;
   }

   /**
    * {@inheritDoc}
    */
   public boolean hierarchyContainsReferences() {
      return false;
   }

   public Iterator<? extends HierarchyNode> getChildren() {
      return myComponents.iterator();
   }

   public boolean hasChildren() {
      return myComponents != null && myComponents.size() > 0;
   }
}
