/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import artisynth.core.femmodels.PointSkinAttachment.FrameConnection;
import artisynth.core.femmodels.PointSkinAttachment.FemConnection;
import artisynth.core.femmodels.PointSkinAttachment.SkinConnection;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.CollidableBody;
import artisynth.core.mechmodels.CollidableDynamicComponent;
import artisynth.core.mechmodels.CollisionHandler;
import artisynth.core.mechmodels.ContactMaster;
import artisynth.core.mechmodels.ContactPoint;
import artisynth.core.mechmodels.DistanceGridComp;
import artisynth.core.mechmodels.DynamicAttachment;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.DualQuaternionDeriv;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointAttachable;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.PointList;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.SkinMeshBase;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformGeometryAction;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import maspack.function.ConstantFuntion1x1;
import maspack.function.SISOFunction;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.Face;
import maspack.geometry.GeometryTransformer;
import maspack.geometry.HalfEdge;
import maspack.geometry.LineSegment;
import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.PolylineMesh;
import maspack.geometry.PointMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.DualQuaternion;
import maspack.matrix.Point3d;
import maspack.matrix.PolarDecomposition3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.Matrix4x3;
import maspack.matrix.Matrix3d;
import maspack.matrix.Quaternion;
import maspack.properties.PropertyList;
import maspack.properties.PropertyUtils;
import maspack.render.RenderList;
import maspack.util.DoubleHolder;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.ListRemove;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * A MeshComponent that supports "skinning", in which the position of each mesh
 * vertex is controlled by the configuration of one or more referenced
 * <i>master bodies</i>}. Master bodies may currently include Frames and FEM
 * models.
 * 
 * <p>Each vertex is connected internally to the master bodies by a {@link
 * PointSkinAttachment}, which contains a weighted {@link
 * PointSkinAttachment.SkinConnection} to each body. The connection weights are
 * computed based on the distance from the vertex to each body, and sum to
 * one. All connections collectively determine the vertex position as the
 * master bodies move and/or deform.
 *
 * A typical procedure for creating a {@code SkinMeshBody} in code
 * is as follows:
 * <pre>
 *    // create the body with an underlying mesh:
 *    SkinMeshBody skinMesh = new SkinMeshBody(mesh);
 *
 *    // add the master body references:
 *    skinMesh.addMasterBody (rigidBody1);
 *    skinMesh.addMasterBody (rigidBody2);
 *    skinMesh.addMasterBody (femModel1);
 *
 *    // compute the weighted connections for each vertex:
 *    skinMesh.computeAllVertexConnections();
 * </pre>
 * 
 * <p>By default, {@link #computeAllVertexConnections} uses an <i>inverse
 * square</i> distance weighting scheme to determine the connection
 * weights. Applications may determine the connection weights in other ways,
 * either by specifying a different weighting function or by specifying them
 * explicitly. Applications can also add point-based markers or create
 * point-based attachments for a {\tt SkinMeshBody}.
 *
 * <p>For full details, see the Chapter ``Skinning'' in the <a
 * href="https://www.artisynth.org/doc/pdf/modelguide.pdf">ArtiSynth Modeling
 * Guide</a>.
 */
public class SkinMeshBody extends SkinMeshBase 
   implements CollidableBody, PointAttachable {

   /**
    * Characterizes the blend mechanism used for the frame-based
    * portion of the skinning.
    */
   public enum FrameBlending {
      LINEAR, DUAL_QUATERNION_LINEAR, DUAL_QUATERNION_ITERATIVE};

   /**
    * Decsribes the connection type used for FEM bodies.
    */
   public enum FemConnectionType {
      
      /**
       * Vertices are attached to elements (the default)
       */
      ELEMENT,

      /**
       * Vertices are attached to nodal displacements (legacy type;
       * only works for small rotations).
       */
      DISPLACEMENT
   }

   /**
    * Contains information about the nearest point on a master body to a
    * another point or mesh vertex.
    */
   static public class NearestPoint {
      /**
       * Nearest point on the master body.
       */
      public Point3d nearPoint;

      /**
       * Distance to the master body.
       */
      public double distance;

      /**
       * Master body
       */
      public ModelComponent body;

      Vector2d uv;
      Face face;

      FemElement3dBase elem;

      NearestPoint () {
         nearPoint = new Point3d();
         uv = new Vector2d();
      }
   }

   /* --- property attributes --- */

   public static FrameBlending DEFAULT_FRAME_BLENDING = FrameBlending.LINEAR;   
   FrameBlending myFrameBlending = DEFAULT_FRAME_BLENDING;

   protected static final Collidability DEFAULT_COLLIDABILITY =
      Collidability.ALL;   
   protected Collidability myCollidability = DEFAULT_COLLIDABILITY;

   static final SkinWeightingFunction DEFAULT_WEIGHTING_FXN =
      new InverseSquareWeighting(); 
   SkinWeightingFunction myWeightingFxn = DEFAULT_WEIGHTING_FXN.clone();

   public static double DEFAULT_DQ_BLEND_TOLERANCE = 1e-8;
   double myDQBlendTolerance = DEFAULT_DQ_BLEND_TOLERANCE;

   public static int DEFAULT_DQ_MAX_BLEND_STEPS = 3;
   int myDQMaxBlendSteps = DEFAULT_DQ_MAX_BLEND_STEPS;

   public static boolean DEFAULT_ATTACH_POINTS_TO_MESH = true;
   protected boolean myAttachPointsToMesh = DEFAULT_ATTACH_POINTS_TO_MESH;

   /* --- misc attributes --- */

   protected int myCollidableIndex;

   /* --- sub component lists --- */

   protected ComponentList<PointSkinAttachment> myVertexAttachments;
   protected PointList<SkinMarker> myMarkers;

   /* --- attributes relating to master components --- */

   protected ArrayList<BodyInfo> myBodyInfos;
   protected boolean myBodyInfoUpdated = false;
   // used for automatically computing weights:
   protected NearestPoint[] myNearestPoints = new NearestPoint[0];
   protected double[] myWeights = new double[0];

   public static final FemConnectionType 
      DEFAULT_FEM_CONNECTION_TYPE = FemConnectionType.ELEMENT;
   protected FemConnectionType myFemConnectionType = 
      DEFAULT_FEM_CONNECTION_TYPE;

   /**
    * Contains information about the rotation of a FEM element, which is needed
    * when connecting a point or vertex to an FEM model using an
    * ElementConnection.
    */
   public static class ElementRotationData {
      // Element rotation R is determined using the deformation gradient F of
      // the "warping" integration point at its center. R is then recovered
      // using the special polar decomposition
      //
      // F = R H
      //
      // where R is right-handed and H is symmetric but not necessarily
      // positive definite.

      Matrix3d myR; // rotation of the element center
      Matrix3d myH; // deformation of the element center
   }

   /**
    * Base class for information about bodies (e.g., Frames or
    * FemNodels) used to control the skinning.
    */
   public static abstract class BodyInfo {

      protected int myIndex;

      BodyInfo () {
      }

      public void setIndex (int idx) {
         myIndex = idx;
      }
      
      public int getIndex() {
         return myIndex;
      }
      
      /**
       * Returns the PolygonalMesh, if any, associated with this body.
       */
      public abstract PolygonalMesh getMesh();

      public abstract ModelComponent getBody();
   }

   /**
    * Contains information for each frame controlling this SkinMeshBody.
    */
   public static class FrameInfo extends BodyInfo {

      protected Frame myFrame;
      protected RigidTransform3d myBasePose;
      protected RigidTransform3d myDeltaPose;

      // dual quaternion representation, used for DUAL_QUATERNION blending:
      protected DualQuaternion myBlendQuaternion;
      // dual quaternion derivatives:
      protected Matrix4x3 myDQrDr; // deriv of rotational part wrt rotation
      protected Matrix4x3 myDQtDr; // deriv of translational part wrt rotation
      // Note: deriv of translational part wrt translation is same as myDQtDr

      FrameInfo () {
         super ();
         myBasePose = new RigidTransform3d();
         myDeltaPose = new RigidTransform3d();
         myBlendQuaternion = new DualQuaternion();
         myDQrDr = new Matrix4x3();
         myDQtDr = new Matrix4x3();
      }

      FrameInfo (Frame frame) {
         this();
         setFrame (frame);
      }

      /**
       * Returns the Frame component itself.
       */
      public Frame getFrame() {
         return myFrame;
      }

      public void setFrame (Frame frame) {
         myFrame = frame;
         myBasePose.set (frame.getPose());
      }

      /**
       * Returns the current base pose for this frame. Skin displacements
       * are computed with respect to the base pose.
       */
      public RigidTransform3d getBasePose() {
         return myBasePose;
      }

      /**
       * Sets the base pose for this frame.
       */
      public void setBasePose (RigidTransform3d T) {
         myBasePose.set (T);
      }

      /**
       * Returns the displacement from the base pose to the Frame's
       * current pose. If the base pose is XBW, and the current
       * pose is TFW, then this is computed as
       * <pre>
       *  TFW * inv(XBW)
       * </pre>
       */
      public RigidTransform3d getDeltaPose() {
         return myDeltaPose;
      }

      /**
       * Returns the DualQuaternion representation of the transform
       * returned by {@link #getDeltaPose()}.
       */
      public DualQuaternion getBlendQuaternion() {
         return myBlendQuaternion;
      }

      public PolygonalMesh getMesh() {
         if (myFrame instanceof RigidBody) {
            return ((RigidBody)myFrame).getSurfaceMesh();
         }
         else {
            return null;
         }
      }

      public Frame getBody() {
         return myFrame;
      }

      /**
       * Computes the quantities returned by {@link #getDeltaPose()}, and, for
       * non-linear blending, {@link #getBlendQuaternion()}. This should be
       * called, before these quantities are used, whenever the Frame's
       * pose changes.
       */
      public void updatePosState (FrameBlending frameBlending) {
         myDeltaPose.mulInverseRight (
            myFrame.getPose(), myBasePose);
         if (frameBlending != FrameBlending.LINEAR) {
            updateDualQuaternion();
         }
      }

      public void updateDualQuaternion () {
         DualQuaternion tmpQ = new DualQuaternion();
         tmpQ.set(myBlendQuaternion);
         myBlendQuaternion.set(myDeltaPose);
         // Dual quaternion has two representations; ensure the one
         // computed is consistent with the last one.
         if (tmpQ.dot(myBlendQuaternion) <0) {
            myBlendQuaternion.scale(-1);
         }
         DualQuaternionDeriv.computeDerivs (
            myDQrDr, myDQtDr, myBlendQuaternion, myBasePose);

         // // compute derivatives, in case they are needed:
         // tmpQ.set (myFrame.getPose());
         // Quaternion qr = new Quaternion(); // rotational part
         // Quaternion qt = new Quaternion(); // translation part
         // tmpQ.getReal (qr);
         // tmpQ.getDual (qt);
         
         // Matrix4x3 DQrDr = new Matrix4x3();
         // Matrix4x3 DQtDr = new Matrix4x3();

         // DualQuaternionDeriv.computeDQrDr (DQrDr, qr);
         // DualQuaternionDeriv.computeDQtDr (DQtDr, qt, qr);

         // // postmultiply DQrDr and DQtDr by the dual quaternion representing
         // // inv(myBasePos) so they reflect derivatives of the blendQuaternion
         // tmpQ.set (myBasePose);
         // tmpQ.invert();
         // tmpQ.getReal (qr);
         // tmpQ.getDual (qt);

         // DualQuaternionDeriv.postmulDeriv (myDQrDr, DQrDr, qr);

         // Matrix4x3 Dtmp = new Matrix4x3();
         // DualQuaternionDeriv.postmulDeriv (myDQtDr, DQtDr, qr);
         // DualQuaternionDeriv.postmulDeriv (Dtmp, DQrDr, qt);
         // myDQtDr.add (Dtmp);
      }
   }
  
   /**
    * Contains information for each FemModel controlling this SkinMeshBody.
    * Doesn't do very much now - can be expanded later as needed.
    */
   public static class FemModelInfo extends BodyInfo {

      protected FemModel3d myFemModel;
      protected PolygonalMesh myDefaultMesh;
      protected PolygonalMesh myMesh;

      FemModelInfo (FemModel3d fem) {
         super ();
         myFemModel = fem;
      }

      public FemModel3d getFemModel() {
         return myFemModel;
      }

      public PolygonalMesh getMesh() {
         if (myMesh == null) {
            FemMeshComp surfaceComp = myFemModel.getSurfaceMeshComp();
            // Make sure surfaceMesh is generated internally, so that element
            // edges are properly marked. Also make sure that it is single node
            // mapped (which it won't be if it is a highres meshes for a FEM
            // with quadratic elements)
            if (surfaceComp == null ||
                !surfaceComp.isGeneratedSurface() ||
                !surfaceComp.isSingleNodeMapped()) {
               FemMeshComp mcomp = FemMeshComp.createSurface (null, myFemModel);
               myMesh = (PolygonalMesh)mcomp.getMesh();
            }
            else {
               myMesh = (PolygonalMesh)surfaceComp.getMesh();
            }
         }
         return myMesh;
      }

      public FemModel3d getBody() {
         return myFemModel;
      }
   }

   public static PropertyList myProps =
      new PropertyList (SkinMeshBody.class, SkinMeshBase.class);

   static {
      myProps.add (
         "frameBlending", "frame blending mechanism",
         DEFAULT_FRAME_BLENDING);
      myProps.add (
         "collidable", 
         "sets the collidability of this SkinMeshBody mesh",
         DEFAULT_COLLIDABILITY);      
      myProps.add (
         "weightingFunction",
         "weighting function used to create connection weights",
         DEFAULT_WEIGHTING_FXN, "CE");
      myProps.add (
         "DQBlendTolerance",
         "blend tolerance for iterative dual-quaternion blending",
         DEFAULT_DQ_BLEND_TOLERANCE);
      myProps.add (
         "DQMaxBlendSteps",
         "max blend steps for iterative dual-quaternion blending",
         DEFAULT_DQ_MAX_BLEND_STEPS);
      myProps.add (
         "attachPointsToMesh",
         "createPointAttachment() should attach points to mesh if possible",
         DEFAULT_ATTACH_POINTS_TO_MESH);
   }

   /**
    * {@inheritDoc}
    */
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Creates an empty SkinMeshBody with no mesh.
    */
   public SkinMeshBody () {
      super();
      myVertexAttachments =
         new ComponentList<PointSkinAttachment>(
            PointSkinAttachment.class, "attachments");
      add (myVertexAttachments);
      myMarkers = new PointList<SkinMarker> (SkinMarker.class, "markers");
      add (myMarkers);      
      myBodyInfos = new ArrayList<BodyInfo>();
   }

   /**
    * Creates a SkinMeshBody with a specified mesh.
    * 
    * @param mesh mesh to be associated with the body
    */
   public SkinMeshBody (MeshBase mesh) {
      this (null, mesh);
   }

   /**
    * Creates a SkinMeshBody with a specified name and mesh.
    *
    * @param name name of the body
    * @param mesh mesh to be associated with the body
    */
   public SkinMeshBody (String name, MeshBase mesh) {
      this();
      setName (name);      
      setMesh (mesh);
   }

   /* --- property accessors --- */

   /**
    * Sets the blend type for that part of the skinning that
    * depends on frames.
    * 
    * @param type frame blending type
    */
   public void setFrameBlending(FrameBlending type) {
      if (myFrameBlending != type) {
         myFrameBlending = type;
         myBodyInfoUpdated = false;
      }
   }
   
   /**
    * Queries the blend type for that part of the skinning that depends on
    * frames.
    * 
    * @return current frame blending type
    */
   public FrameBlending getFrameBlending() {
      return myFrameBlending;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collidability getCollidable () {
      getSurfaceMesh(); // build surface mesh if necessary
      return myCollidability;
   }

   /**
    * {@inheritDoc}
    */
   public void setCollidable (Collidability c) {
      if (myCollidability != c) {
         myCollidability = c;
         notifyParentOfChange (new StructureChangeEvent (this));
      }
   }

   /**
    * Returns the weighting function used to compute connections.
    * 
    * @return current weighting function
    * @see #setWeightingFunction
    */
   public SkinWeightingFunction getWeightingFunction() {
      return myWeightingFxn;
   }

   /**
    * Sets the weighting function to be used in computing connections in
    * subsequent calls to the methods {@link #computeAllVertexConnections},
    * {@link #computeVertexConnections}, {@link #addMarker(Point3d)}, {@link
    * #createPointAttachment(Point)}, etc. Current vertex and marker
    * connections will remain unchanged.
    * 
    * @param fxn new weighting function.
    */
   public void setWeightingFunction (SkinWeightingFunction fxn) {
      SkinWeightingFunction newWeighting = fxn.clone();
      PropertyUtils.updateCompositeProperty (
         this, "weightingFunction", null, newWeighting);
      myWeightingFxn = newWeighting;
   }

   /**
    * Sets the weighting function to be used in computing connections to an
    * <i>inverse square</i> weighting implemented by {@link
    * InverseSquareWeighting}. This is equivalent to calling
    * <pre>
    *    setWeightingFunction (new InverseSquareWeighting())
    * </pre>
    */
   public void setInverseSquareWeighting () {
      setWeightingFunction (new InverseSquareWeighting());
   }

   /**
    * Sets the weighting function to be used in computing connections to an
    * <i>inverse square</i> weighting implemented by {@link
    * InverseSquareWeighting}. This is equivalent to calling
    * <pre>
    *    setWeightingFunction (new InverseSquareWeighting())
    * </pre>
    */
   public void setGaussianWeighting (double sigma) {
      setWeightingFunction (new GaussianWeighting (sigma));
   }

   /**
    * Queries the tolerance used for iterative dual-quaternion frame blending.
    *
    * @return iterative dual-quaternion blending tolerance
    */
   public double getDQBlendTolerance () {
      return myDQBlendTolerance;
   }

   /**
    * Sets the tolerance used for iterative dual-quaternion frame blending.
    *
    * @param tol new blending tolerance
    */
   public void setDQBlendTolerance (double tol) {
      myDQBlendTolerance = tol;
   }

   /**
    * Queries the maximum number of steps used for iterative dual-quaternion
    * frame blending.
    *
    * @return max dual-quaternion blending steps
    */
   public int getDQMaxBlendSteps () {
      return myDQMaxBlendSteps;
   }

   /**
    * Sets the maximum number of steps used for iterative dual-quaternion
    * frame blending.
    *
    * @param max maximum number of blending steps
    */
   public void setDQMaxBlendSteps (int max) {
      myDQMaxBlendSteps = max;
   }

   /**
    * Queries whether the point attachments created by {@link
    * #createPointAttachment(Point)} should connect directly to the skin body's
    * mesh. See {@link setAttachPointsToMesh} for details.
    *
    * @return if {@code true}, point attachments created by {@link
    * #createPointAttachment(Point)} should connect to skin body's mesh
    * @see #setAttachPointsToMesh
    */
   public boolean getAttachPointsToMesh() {
      return myAttachPointsToMesh;
   }

   /**
    * Sets whether the point attachments created by {@link
    * #createPointAttachment(Point)} should connect directly to the skin body's
    * mesh. If this is {@code true}, and the body's mesh is both an instance of
    * {@link PolygonalMesh} and triangular, then the point attachment is
    * created using {@link #createPointMeshAttachment(Point)}. Otherwise, a
    * direct connection to the master bodies is created using {@link
    * #createPointAttachment(Point,VectorNd)}.
    *
    * @param enable if {@code true}, subsequent point attachments
    * created by {@link #createPointAttachment(Point)}
    * should connect to skin body's mesh
    * @see #getAttachPointsToMesh
    */
   public void setAttachPointsToMesh (boolean enable) {
      myAttachPointsToMesh = enable;
   }

   /* --- mesh and vertex attachment methods --- */
   
   protected void setMeshFromInfo () {
      // Overloaded from super class. Is called by super.setMesh() and by scan
      // (whenever a mesh is scanned) to set mesh properties and auxiliary
      // data structures specific to the class.
      MeshBase mesh = getMesh();
      if (mesh != null) {
         mesh.setFixed (false);
      }
   }

   /**
    * Sets the mesh associated with the this SkinMeshBody. Connections between
    * the vertices and master bodies will <i>not</i> be generated; this must be
    * done subsequently by calling {@link #computeAllVertexConnections()} or
    * {@link #computeVertexConnections(int,VectorNd)}.
    * 
    * @param mesh new mesh to be associated with this body
    */
   public void setMesh (MeshBase mesh) {
      // super method will call setMeshFromInfo()
      super.setMesh (mesh);
      initializeAttachments();
   }

   /**
    * Returns the base (i.e., initial) position for a specified vertex in this
    * SkinMeshBody.
    *
    * @param idx index of the vertex. Must be less than
    * {@link #numVertexAttachments}.
    */
   public Point3d getVertexBasePosition (int idx) {
      return myVertexAttachments.getByNumber(idx).getBasePosition();
   }

   /**
    * Sets the base (i.e., initial) position for a specified vertex in this
    * SkinMeshBody.
    *
    * @param idx index of the vertex. Must be less than {@link
    * #numVertexAttachments}.
    * @param pos new base position
    */
   public void setVertexBasePosition (int idx, Vector3d pos) {
      myVertexAttachments.getByNumber(idx).setBasePosition(pos);
   }

   /**
    * Returns the base weight for a specified vertex in this SkinMeshBody.
    *
    * @param idx index of the vertex. Must be less than
    * {@link #numVertexAttachments}.
    */
   public double getVertexBaseWeight (int idx) {
      return myVertexAttachments.getByNumber(idx).getBaseWeight();
   }

   /**
    * Sets the base weight for a specified vertex in this SkinMeshBody.
    *
    * @param idx index of the vertex. Must be less than {@link
    * #numVertexAttachments}.
    * @param w new base weight to set
    * @param normalize if {@code true}, and the other connection weights
    * sum to a non-zero value, scales the other connection weights
    * so that the total weight sum remains unchanged.
    */
   public void setVertexBaseWeight (int idx, double w, boolean normalize) {
      myVertexAttachments.getByNumber(idx).setBaseWeight(w, normalize);
   }

   private void initializeAttachments() {
      MeshBase mesh = getMesh();
      int numVtxs = mesh.numVertices();
      myVertexAttachments.removeAll();
      for (int i=0; i<numVtxs; i++) {
         PointSkinAttachment a = new PointSkinAttachment();
         setBaseAttachment (a, mesh.getVertex(i).getPosition());
         myVertexAttachments.add (a);
      }
   }

   /**
    * Returns the number of vertex attachments in this SkinMeshBody.
    * This will equal the same number returned by {@link #numVertices}.
    */
   public int numVertexAttachments() {
      return myVertexAttachments.size();
   }

   /**
    * Returns the attachment associated with the idx-th mesh vertex of this
    * SkinMeshBody.
    *
    * @param idx index of the vertex for which the attachment is desired.
    * Must be less that {@link #numVertexAttachments()}.
    */
   public PointSkinAttachment getVertexAttachment (int idx) {
      return myVertexAttachments.getByNumber (idx);
   }
   
   /**
    * Returns the attachment associated with a specific mesh vertex of this
    * SkinMeshBody.
    *
    * @param vtx vertex for which the attachment is desired.
    */
   public PointSkinAttachment getVertexAttachment(Vertex3d vtx) {
      if (vtx.getMesh() != getMesh()) {
         throw new IllegalArgumentException (
            "Vertex does not belong to the SkinMeshBody's mesh");
      }
      return getVertexAttachment(vtx.getIndex());
   }

   /**
    * {@inheritDoc}
    */
   public void getAttachments (List<DynamicAttachment> list, int level) {
      //list.addAll (myAttachments);
      for (SkinMarker mkr : myMarkers) {
         list.add (mkr.getAttachment());
      }
   }         


   /* --- marker methods --- */

   /**
    * Creates and adds a marker to this SkinMeshBody. This method
    * functions identically to {@link #addMarker(String,Point3d)},
    * with {@code name} equal to {@code null}.
    *
    * @param pos initial marker position
    * @return the created marker
    */
   public SkinMarker addMarker (Point3d pos) {
      return addMarker (null, pos);
   }

   /**
    * Creates and adds a marker to this SkinMeshBody. The marker's connections
    * are based on its initial position (described by {@code pos}) and the
    * current master body configurations, with the connection weights for each
    * master body determined by the skin weighting function (as returned by
    * {@link #getWeightingFunction}).
    *
    * @param name name of the marker
    * @param pos initial marker position
    * @return the created marker
    */
   public SkinMarker addMarker (String name, Point3d pos) {
      return addMarker (name, pos, null);
   }

   /**
    * Creates and adds a marker to this SkinMeshBody. The marker's connections
    * are based on its initial position (described by {@code pos}) and the
    * current master body configurations, with the connection weights for each
    * master body specified by {@code weights}.
    *
    * <p>The size of {@code weights} must be {@code >=} {@link
    * #numMasterBodies()}. If it is {@code >= numMasterBodies()+1}, the
    * additional value specifies the marker attachment's base weight.
    *
    * @param name name of the marker
    * @param pos initial marker position
    * @param weights connection weights
    * @return the created marker
    */
   public SkinMarker addMarker (String name, Point3d pos, VectorNd weights) {
      if (weights != null && weights.size() < numMasterBodies()) {
         throw new IllegalArgumentException (
            "weights size "+weights.size()+
            " less than number of master bodies "+numMasterBodies());
      }
      SkinMarker mkr = new SkinMarker (pos);
      mkr.setName (name);
      double[] wbuf = (weights != null ? weights.getBuffer() : null);
      PointSkinAttachment a = mkr.getAttachment();
      computeAttachment (a, mkr.getPosition(), wbuf);
      a.setSkinMesh (this);
      myMarkers.add (mkr);
      return mkr;
   }
   
   /**
    * Creates and adds a marker to the nearest mesh feature of this skin body.
    * functions identically to {@link #addMeshMarker(String,Point3d)}, with
    * {@code name} equal to {@code null}.
    *
    * @param pos requested initial marker position
    * @return the created marker
    */
   public SkinMarker addMeshMarker (Point3d pos) {
      return addMeshMarker (null, pos);
   }
   
   /**
    * Creates and adds a marker to the nearest mesh feature of this skin body.
    * The mesh must be both an instance of {@link PolygonalMesh} and
    * triangular. The requested marker position {@code pos} is then projected
    * onto the nearest mesh feature (e.g., a face for a {@link PolygonalMesh}
    * or a line segment for a {@link PolylineMesh}), and the master body
    * connections are formed by a linear combination of the connections of the
    * nearest feature vertices, based on the barycentric coordinates of the
    * projected point position.
    *
    * <p>Attaching markers to the mesh entails a greater computational cost but
    * ensures that the marker will remain connected to the mesh in the same
    * relative position. If the vertex connections are subsequently changed,
    * such as through a call to {@link #computeAllVertexConnections}, the
    * marker will no longer follow the mesh position.
    *
    * @param name name of the marker
    * @param pos requested initial marker position
    * @return the created marker
    */
   public SkinMarker addMeshMarker (String name, Point3d pos) {
      SkinMarker mkr = new SkinMarker (pos);
      mkr.setName (name);
      PointSkinAttachment a = mkr.getAttachment();
      computeMeshAttachment (a, mkr.getPosition());
      a.setSkinMesh (this);
      myMarkers.add (mkr);
      return mkr;
   }

   /**
    * Removes a marker from this SkinMeshBody.
    * 
    * @param mkr marker to remove
    * @return {@code true} if the marker was present and was removed
    */
   public boolean removeMarker (SkinMarker mkr) {
      if (myMarkers.remove (mkr)) {
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * Removes all markers from this SkinMeshBody.
    */
   public void clearMarkers() {
      myMarkers.clear();
   }

   /**
    * Returns the list of markers associated with this SkinMeshBody.
    * 
    * @return marker list
    */
   public PointList<SkinMarker> markers() {
      return myMarkers;
   }

   /* --- master bodies --- */

   /**
    * @deprecated Use {@link #addMasterBody} instead
    */
   public void addFrame (Frame frame) {
      addMasterBody (frame);
   }

   /**
    * Removes a master body from this SkinMeshBody.
    *
    * <i>This is a special purpose method and does not update any existing
    * connections to reflect the removed frame.  Connections should therefore
    * be computed (or recomputed) after this method has been called.</i>
    * 
    * @param body master body to be removed
    * @return true if body found and removed, false otherwise
    */
   public boolean removeMasterBody (ModelComponent body) {
      
      BodyInfo found = null;;
      for (BodyInfo bodyInfo : myBodyInfos) {
         if (bodyInfo.getBody() == body) {
            // mark for removal
            found = bodyInfo;
            break;
         }
      }
      
      if (found != null) {
         int idx = found.getIndex();
         int lastIdx = myBodyInfos.size ()-1;
         // replace with last item
         if (idx != lastIdx) {
            BodyInfo last = myBodyInfos.get(lastIdx);
            last.setIndex(idx);  // renumber
            myBodyInfos.set(idx, last);
            myBodyInfos.remove(lastIdx);
         }
         return true;
      }
      
      return false;
      
   }

   private BodyInfo getBodyInfo (ModelComponent body) {
      for (BodyInfo bi : myBodyInfos) {
         if (bi.getBody() == body) {
            return bi;
         }
      }
      return null;
   }

   /**
    * Returns true is a specified master body is currently registered 
    * with this SkinMeshBody.
    *
    * @param body body to be queried
    */
   public boolean hasMasterBody (ModelComponent body) {
      return (getBodyInfo (body) != null);
   }

   /**
    * Gets the {@code idx}-th master body registered with this
    * SkinMeshBody.
    * 
    * @param idx index of the desired body; must be between 0 and
    * the number returned by {@link #numMasterBodies()}
    * {@link #numMasterBodies}.
    * @return idx-th master body
    */
   public ModelComponent getMasterBody (int idx) {
      return myBodyInfos.get(idx).getBody();
   }
   
   /**
    * Returns the number of master bodies currently registered with this
    * SkinMeshBody.
    */
   public int numMasterBodies() {
      return myBodyInfos.size();
   }

   /**
    * Adds a master body with this SkinMeshBody so that it can be used for
    * skinning control. At present, master bodies are restricted to
    * instances of {@link Frame} and {@link FemModel3d}.
    *
    * @param body master body to be registered
    */
   public void addMasterBody (ModelComponent body) {
      if (hasMasterBody (body)) {
         throw new IllegalArgumentException (
            "Body "+body+" already present");
      }
      BodyInfo binfo;
      if (body instanceof Frame) {
         binfo = new FrameInfo ((Frame)body);
      }
      else if (body instanceof FemModel3d) {
         binfo = new FemModelInfo ((FemModel3d)body);
      }
      else {
         throw new IllegalArgumentException (
            "Body is not an instance of Frame or FemModel3d");
      }
      binfo.setIndex (myBodyInfos.size());
      myBodyInfos.add (binfo);
   }

   /**
    * Queries the base pose of a {@link Frame} based master body.
    *
    * @param frame frame for which the base pose is requested
    * @return base pose for {@code frame}
    */
   public RigidTransform3d getBasePose (Frame frame) {
      BodyInfo bi = getBodyInfo (frame);
      if (bi == null) {
         throw new IllegalArgumentException (
            "Frame "+ComponentUtils.getPathName(frame)+" is not a master body");
      }
      return new RigidTransform3d (((FrameInfo)bi).myBasePose);
   }

   /**
    * Sets the base pose of a {@link Frame} based master body.
    *
    * @param frame frame for which the base pose is to be set
    * @param TBW0 new base pose
    */
   public void setBasePose (Frame frame, RigidTransform3d TBW0) {
      BodyInfo bi = getBodyInfo (frame);
      if (bi == null) {
         throw new IllegalArgumentException (
            "Frame "+ComponentUtils.getPathName(frame)+" is not a master body");
      }
      ((FrameInfo)bi).myBasePose.set (TBW0);
   }

   /**
    * Returns the information structures of all master bodies that are
    * currently registered with this SkinMeshBody.
    */
   public List<BodyInfo> getAllMasterBodyInfo() {
      return myBodyInfos;
   }

   /**
    * @deprecated Use {@link #addMasterBody} instead
    */
   public void addFemModel (FemModel3d fem) {
      addMasterBody (fem);
   }

   /* --- vertex connections --- */

   /**
    * @deprecated
    * Use {@link #computeAllVertexConnections} instead.
    */
   public void computeWeights() {
      computeDisplacementAttachments (0, /*useNew=*/false);
   }

   /**
    * Computes master body connections for every vertex of this SkinMeshBody's
    * mesh. The current vertex positions are used as initial positions, and the
    * connection weights are determined by using the skin weighting function
    * (as returned by {@link #getWeightingFunction}).
    */
   public void computeAllVertexConnections() {
      computeDisplacementAttachments (0, true);
   }

   protected void computeDisplacementAttachments (
      double sigma, boolean useNew) {
      
      MeshBase mesh = getMesh();
      if (mesh == null) {
         return;
      }
      
      // PointSkinAttacher attacher =
      //    new PointSkinAttacher (myBodyInfos);

      for (int i=0; i<mesh.numVertices(); i++) {
         Vertex3d vtx = mesh.getVertices().get(i);
         PointSkinAttachment a = myVertexAttachments.get(i);
         if (useNew) {
            computeAttachment (a, vtx.getPosition(), null);
         }
         else {
            computeAttachment (
               a, vtx.getPosition(), null, FemConnectionType.DISPLACEMENT);
         }
      }
   }

   /**
    * Computes master body connections for a specified vertex of this
    * SkinMeshBody's mesh. The current vertex position is used as the initial
    * position.
    *
    * <p>The connection weights for each master body can be specified by the
    * optional argument {@code weights}. If {@code weights} is {@code null},
    * then the weights are determined by the skin weighting function (as
    * returned by {@link #getWeightingFunction}). If non-{@code null}, the size
    * of {@code weights} must be {@code >=} {@link #numMasterBodies()}. If it
    * is {@code >= numMasterBodies()+1}, the additional value specifies the
    * vertex attachment's base weight.
    * 
    * @param vidx index of the vertex for which connections are to be computed
    * @param weights if non-{@code null}, specifies connection weights
    */
   public void computeVertexConnections (int vidx, VectorNd weights) {
      double[] wbuf = null;
      if (weights != null) {
         if (weights.size() < numMasterBodies()) {
            throw new IllegalArgumentException (
               "weights size "+weights.size()+
               " less than number of master bodies "+numMasterBodies());
         }
         wbuf = weights.getBuffer();
      }
      MeshBase mesh = getMesh();
      if (mesh == null) {
         throw new IllegalStateException (
            "mesh is currently not set");
      }
      if (vidx >= mesh.numVertices()) {
         throw new IllegalArgumentException (
            "vidx "+vidx+
            " is out of range; number of mesh vertices is "+mesh.numVertices());
      }
      Vertex3d vtx = mesh.getVertex (vidx);
      PointSkinAttachment a = myVertexAttachments.get(vidx);
      computeAttachment (a, vtx.getPosition(), wbuf);
   }
   
   /**
    * Clears master body connections for a specified vertex of this
    * SkinMeshBody's mesh. This means that the vertex position will remain
    * fixed and will not move in response to the motion of any of the master
    * bodies. The position at which the vertex will remain fixed is set to its
    * current position.
    * 
    * @param vidx index of the vertex for which connections are to be cleared
`    */
   public void clearVertexConnections (int vidx) {
      MeshBase mesh = getMesh();
      if (mesh == null) {
         throw new IllegalStateException (
            "mesh is currently not set");
      }
      if (vidx >= mesh.numVertices()) {
         throw new IllegalArgumentException (
            "vidx "+vidx+
            " is out of range; number of mesh vertices is "+mesh.numVertices());
      }
      Vertex3d vtx = mesh.getVertex (vidx);
      PointSkinAttachment a = myVertexAttachments.get(vidx);
      setBaseAttachment (a, vtx.getPosition());      
   }
   

   /**
    * Smooths vertex connection weights according to a function of
    * distance.  At present, this <i>only</i> smooths the weights associated
    * with Frame connections. Results when the SkinMeshBody contains other
    * types of attachment connections are undefined.
    *
    * <p>This method is experimental.
    *
    * @param weightFunction single-input single-output function of distance
    *       to use as weights
    * @param networkDist number of paths to traverse to collect vertices
    */
   public void smoothWeights (SISOFunction weightFunction, int networkDist) {
      
      for (BodyInfo binfo : myBodyInfos) {
         if (!(binfo instanceof FrameInfo)) {
            throw new UnsupportedOperationException (
               "Not supported for SkinMeshBodies that reference FEM models");
         }
      }
      ArrayList<Vertex3d> vtxs = getMesh().getVertices();
      ArrayList<Vertex3d> vtxList = new ArrayList<Vertex3d>();

      int numf = numMasterBodies();      

      //double[] newWeights = new double[nBodies*vtxs.size()];
      double [] w = new double[numf];

      for (int i=0; i<vtxs.size(); i++) {
         Vertex3d vtx = vtxs.get(i);
         vtxList.clear();
         collectVertices(vtx,networkDist,vtxList);
         Arrays.fill(w, 0);
         
         double wTotal = 0;
         for (Vertex3d vtxl : vtxList) {
            double d = vtxl.getPosition().distance(vtx.getPosition());
            double wd = weightFunction.eval(d);
            wTotal += wd;
            int idx = vtxs.indexOf(vtxl);
            
            PointSkinAttachment attacher = myVertexAttachments.getByNumber(idx);
            if (attacher != null) {
               FrameConnection fcon = attacher.getFrameConnections();
               while (fcon != null) {
                  int fidx = fcon.myFrameInfo.getIndex();
                  w[fidx] += wd*fcon.getWeight();
                  fcon = fcon.getNext();
               }
            }
         }
         PointSkinAttachment attacher = myVertexAttachments.getByNumber(i);
         if (attacher != null) {
            FrameConnection fcon = attacher.getFrameConnections();
            while (fcon != null) {
               int fidx = fcon.myFrameInfo.getIndex();
               fcon.setWeight (w[fidx]/wTotal);
               fcon = fcon.getNext();
            }
         }
      }
   }
   

   /**
    * Smooths vertex connection weights according to a function of
    * distance.  Identical to {@link #smoothWeights(SISOFunction,int)} with
    * {@code weightFunction} set to a constant value of 1.
    *
    * <p>This method is experimental.
    * 
    * @param networkDist number of paths to traverse to collect vertices
    */
   public void smoothWeights(int networkDist) {
      smoothWeights(new ConstantFuntion1x1(1), networkDist);
   }
   
   /**
    * Smooths vertex connection weights by looking at neighbouring vertices.
    * Identical to {@link #smoothWeights(SISOFunction,int)} with {@code
    * weightFunction} set to a constant value of 1 and {@code networkDist}
    * set to 1.
    *
    * <p>This method is experimental.
    */
   public void smoothWeights() {
      smoothWeights(1);
   }
   
   private void collectVertices (
      Vertex3d vtx, int networkDist, ArrayList<Vertex3d> list) {
      
      if (list.contains(vtx)) {
         return;
      }
      
      list.add(vtx);
      if (networkDist > 0) {
         Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
         while (it.hasNext()) {
            HalfEdge he = it.next();
            Vertex3d vtxTail = he.tail;
            collectVertices(vtxTail, networkDist-1,list);
         }
      }
   }

   /* --- MeshComponent implementations --- */

   protected void updateDualQuaternions () {
      for (BodyInfo binfo : myBodyInfos) {
         if (binfo instanceof FrameInfo) {
            ((FrameInfo)binfo).updateDualQuaternion();
         }
      }
   }

   protected void updateBodyPositionInfo() {
      // update body information to accomodate changes in body positions
      for (BodyInfo binfo : myBodyInfos) {
         if (binfo instanceof FrameInfo) {
            ((FrameInfo)binfo).updatePosState(myFrameBlending);
         }
         else if (binfo instanceof FemModelInfo) {
            ((FemModelInfo)binfo).myFemModel.invalidateElementRotationData();
         }
      }
   }


   protected void invalidateBodyPositionInfo() {
     myBodyInfoUpdated = false;
   }

   protected void maybeUpdateBodyPositionInfo() {
      if (!myBodyInfoUpdated) {
         updateBodyPositionInfo();
         myBodyInfoUpdated = true;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void updateSlavePos () {
      // override super.updateSlavePos so that we can use PointSkinAttachment's
      // own version of getCurrentPos()

      maybeUpdateBodyPositionInfo();
      MeshBase mesh = getMesh();
      if (mesh != null) {
         PolarDecomposition3d polard = new PolarDecomposition3d();
         Point3d pos = new Point3d();
         int numa = numVertexAttachments();
         for (int i=0; i<numa; i++) {
            PointSkinAttachment a = myVertexAttachments.getByNumber(i);
            if (a != null) {
               Vertex3d vtx = mesh.getVertices().get(i);
               a.computePosState (pos, polard);
               vtx.setPosition(pos);
            }
         }
         mesh.notifyVertexPositionsModified();
      }
      myBodyInfoUpdated = false;
   }

   /* --- attachment computation --- */

   /**
    * Sets the connection type to be used when creating subsequent connections
    * to FEM master bodies.  The default is an <i>element</i> based connection
    * (type {@link FemConnectionType#ELEMENT}. The alternative is the legacy
    * <i>displacement</i> based connection (type {@link
    * FemConnectionType#DISPLACEMENT}. This method is provided to allow
    * applications to employ the legacy displacement connection type ( which
    * works well only for FEM deformations that do not involve a large amount
    * of rotation).
    * 
    * <p>Changing the connection type will only affect connections that are
    * created through subsequent calls to the methods {@link
    * #computeAllVertexConnections}, {@link #computeVertexConnections}, {@link
    * #addMarker(Point3d)}, {@link #createPointAttachment(Point)}, etc.
    * 
    * @param type new element connection type
    */
   public void setFemConnectionType (FemConnectionType type) {
      myFemConnectionType = type;
   }
   
   /**
    * Queries the connection type to be used when creating subsequent
    * connections to FEM master bodies.
    * 
    * @return element connection type
    */
   public FemConnectionType getFemConnectionType () {
      return myFemConnectionType;
   } 

   /**
    * Creates an attachment between a {@link Point} and this skin body.  If
    * {@link #getAttachPointsToMesh()} returns {@code true}, and the body has a
    * triangular mesh, then the point is connected to the mesh using {@link
    * #createPointMeshAttachment(Point)}.  Otherwise, the point is connected to
    * the masters using a direct connection via {@link
    * #createPointAttachment(Point,VectorNd)}.
    *
    * @param pnt point to be attached
    * @return attachment component
    */
   public PointSkinAttachment createPointAttachment (Point pnt) {
      // attach to mesh if myAttachPointsToMesh == true and mesh is not null
      // and mesh is triangular if it is polygonal
      boolean attachToMesh =
         (myAttachPointsToMesh &&
          getMesh() != null &&
          (!(getMesh() instanceof PolygonalMesh) ||
           ((PolygonalMesh)getMesh()).isTriangular()));
      if (attachToMesh) {
         return createPointMeshAttachment (pnt);
      }
      else {
         return createPointAttachment (pnt, null);      
      }
   }

   /**
    * Creates an attachment between a {@link Point} and the nearest mesh
    * feature of this skin mesh body. The mesh must be both an instance of
    * {@link PolygonalMesh} and triangular. The point to be attached is then
    * projected onto the nearest mesh feature (e.g., a face for a {@link
    * PolygonalMesh} or a line segment for a {@link PolylineMesh}), and the
    * master body connections are formed by a linear combination of the
    * connections of the nearest feature vertices, based on the barycentric
    * coodinates of the projected point position.
    *
    * <p>Attaching points to the mesh entails a greater computational cost but
    * ensures that the attached point will remain connected to the mesh in the
    * same relative position. If the vertex connections are subsequently
    * changed, such as through a call to {@link #computeAllVertexConnections},
    * the attached point will no longer follow the mesh position.
    *
    * @param pnt point to be attached
    * @return attachment component
    */
   public PointSkinAttachment createPointMeshAttachment (Point pnt) {
      PointSkinAttachment a = new PointSkinAttachment (pnt);
      a.setSkinMesh (this);
      computeMeshAttachment (a, pnt.getPosition());
      return a;
   }
   
   /**
    * Creates an attachment between a {@link Point} and the master bodies of
    * this SkinMeshBody. The connection weights for each master body are
    * specified by {@code weights}.
    *
    * <p>The size of {@code weights} must be {@code >=} {@link
    * #numMasterBodies()}. If it is {@code >= numMasterBodies()+1}, the
    * additional value specifies the attachment's base weight.
    *
    * @param pnt point to be attached
    * @param weights connection weights
    * @return attachment component
    */
   public PointSkinAttachment createPointAttachment (
      Point pnt, VectorNd weights) {
      if (weights != null && weights.size() < numMasterBodies()) {
         throw new IllegalArgumentException (
            "weights size "+weights.size()+
            " less than number of master bodies "+numMasterBodies());
      }
      // Create a new PointSkinAttachment
      double[] wbuf = (weights != null ? weights.getBuffer() : null);
      PointSkinAttachment a = new PointSkinAttachment (pnt);
      a.setSkinMesh (this);
      computeAttachment (a, pnt.getPosition(), wbuf);
      return a;
   }
   
   void computeAttachment (
      PointSkinAttachment a, Point3d pos, double[] weights) {
      computeAttachment (a, pos, weights, myFemConnectionType);
   }
   
   /**
    * Computes a PointSkinAttachment that connects a specified point to the
    * master bodies of this SkinMeshBody.
    */
   void computeAttachment (
      PointSkinAttachment a, Point3d pos,
      double[] weights, FemConnectionType femConType) {
      
      int nbodies = myBodyInfos.size();

      if (myNearestPoints.length < nbodies) {
         myNearestPoints = new NearestPoint[nbodies];
         for (int i=0; i<nbodies; i++) {
            myNearestPoints[i] = new NearestPoint();
         }
         myWeights = new double[nbodies+1];
      }

      BVFeatureQuery query = new BVFeatureQuery();

      for (int j=0; j<nbodies; j++) {
         NearestPoint near = myNearestPoints[j];
         BodyInfo binfo = myBodyInfos.get(j);
         if (binfo instanceof FrameInfo ||
             femConType == FemConnectionType.DISPLACEMENT) {
            // Frame body or FEM body with legacy displacement connection type
            PolygonalMesh bmesh = binfo.getMesh();
            near.face = query.nearestFaceToPoint (
               near.nearPoint, near.uv, bmesh, pos);
         }
         else {
            // FEM body with element connection type
            FemModelInfo finfo = (FemModelInfo)binfo;
            FemModel3d fem = finfo.getFemModel();
            near.elem = fem.findNearestElement (near.nearPoint, pos);            
         }
         double d = pos.distance (near.nearPoint);
         near.distance = d;
         near.body = binfo.getBody();
      }

      if (weights == null) {
         myWeights[nbodies] = 0; // preset base weight in case it is not set
         myWeightingFxn.computeWeights (myWeights, pos, myNearestPoints);
         weights = myWeights;
      }

      // create an attachment for each body. Assume that
      // thmyBodyInfos.get(j);at vertexAttachments have already be allocated 
      a.removeBackRefsIfConnected();
      a.clearConnections();
      for (int j=0; j<nbodies; j++) {
         NearestPoint drec = myNearestPoints[j];
         double wgt = weights[j];
         if (wgt != 0) {
            BodyInfo binfo = myBodyInfos.get(j);
            if (binfo instanceof FrameInfo) {
               FrameInfo finfo = (FrameInfo)binfo;
               a.addFrameConnection (finfo, wgt);
            }
            else {
               // body is an FEM - create a weighted connection to face nodes
               FemModelInfo finfo = (FemModelInfo)binfo;
               switch (femConType) {
                  case ELEMENT: {
                     a.addElementConnection (pos, drec, finfo, wgt);
                     break;
                  }
                  case DISPLACEMENT: {
                     a.addDisplacementConnection (pos, drec, finfo, wgt);
                     break;
                  }
                  default: {
                     throw new InternalErrorException (
                        "unsupported fem connection " + femConType);
                  }
               }
            }
         }
      } 
      a.setBasePosition (pos);
      double baseWeight = 0;
      if (weights.length > nbodies) {
         baseWeight = weights[nbodies];
      }
      a.setBaseWeight (baseWeight, false);
      a.addBackRefsIfConnected();
   }

   void setBaseAttachment (PointSkinAttachment a, Point3d pos) {
      a.removeBackRefsIfConnected();
      a.clearConnections();
      a.setBasePosition (pos);
      a.setBaseWeight (1, false);
      // no backrefs to add 
   }

   /**
    * Computes a PointSkinAttachment that connects a point on the mesh surface
    * to the master bodies of this SkinMeshBody. This is done by combining the
    * attachments of the three mesh vertices for the face containing the point.
    */
   void computeMeshAttachment (
      PointSkinAttachment a, Point3d pos) {

      Vertex3d[] vtxs;
      double[] weights;
      BVFeatureQuery query = new BVFeatureQuery();
      
      if (getMesh() == null) {
         throw new InternalErrorException (
            "skin mesh body does not have a mesh");
      }
      else if (getMesh() instanceof PolygonalMesh) {
         PolygonalMesh pmesh = (PolygonalMesh)getMesh();
         if (!pmesh.isTriangular()) {
            throw new InternalErrorException (
               "mesh is not triangular");
         }
         Vector2d uv = new Vector2d();
         Point3d nearPoint = new Point3d();
         Face face = query.nearestFaceToPoint (nearPoint, uv, pmesh, pos);
         if (face == null) {
            throw new InternalErrorException (
               "no nearest face found");
         }
         vtxs = face.getVertices();
         weights = new double[] { 1.0 - uv.x - uv.y, uv.x, uv.y };
      }
      else if (getMesh() instanceof PolylineMesh) {
         PolylineMesh pmesh = (PolylineMesh)getMesh();
         DoubleHolder sval = new DoubleHolder();
         Point3d nearPoint = new Point3d();
         LineSegment seg =
            (LineSegment)query.nearestEdgeToPoint (nearPoint, sval, pmesh, pos);
         vtxs = new Vertex3d[] { seg.myVtx0, seg.myVtx1 };
         weights = new double[] { 1.0-sval.value, sval.value };
      }
      else if (getMesh() instanceof PointMesh) {
         PointMesh pmesh = (PointMesh)getMesh();
         Vertex3d vtx = query.nearestVertexToPoint (pmesh, pos);
         vtxs = new Vertex3d[] { vtx };
         weights = new double[] { 1.0 };
      }
      else {
         throw new InternalErrorException (
            "unsupported mesh type " + getMesh().getClass());
      }
      a.removeBackRefsIfConnected();
      a.clearConnections();
      double EPS = 1e-8;
      Point3d basePos = new Point3d();
      double baseWeight = 0;
      for (int j=0; j<weights.length; j++) {
         if (Math.abs(weights[j]) > EPS) {
            //System.out.println (" w["+j+"]=" + weights[j]);
            PointSkinAttachment va = myVertexAttachments.get(vtxs[j].getIndex());
            a.addScaledConnections (weights[j], va);
            basePos.scaledAdd (weights[j], va.getBasePosition());
            baseWeight += weights[j]*va.getBaseWeight();
         }
      }
      a.normalize();
      a.setBasePosition (basePos);
      a.setBaseWeight (baseWeight, false);
      a.myFeatureBased = true;
      a.addBackRefsIfConnected();
   }

   /* --- scan/write methods --- */

   protected void scanBodyInfo (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {
      myBodyInfos.clear();
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (!rtok.tokenIsWord()) {
            throw new IOException ("Expected body identifier, "+rtok);
         }
         else if (rtok.sval.equals ("Frame")) {
            if (!ScanWriteUtils.scanAndStoreReference (rtok, tokens)) {
               throw new IOException ("Frame reference expected, "+rtok);
            }
            //rtok.scanToken ('[');
            RigidTransform3d basePose = new RigidTransform3d();
            basePose.scan (rtok);
            // frame will be filled in by postscan ...
            FrameInfo finfo = new FrameInfo ();
            finfo.myBasePose.set (basePose);
            finfo.setIndex (myBodyInfos.size());
            myBodyInfos.add (finfo);           
            //rtok.scanToken (']');
         }
         else if (rtok.sval.equals ("FemModel3d")) {
            if (!ScanWriteUtils.scanAndStoreReference (rtok, tokens)) {
               throw new IOException ("FemModel3d reference expected, "+rtok);
            }
            FemModelInfo finfo = new FemModelInfo (null);
            finfo.setIndex (myBodyInfos.size());
            myBodyInfos.add (finfo);            
         }
         else {
            throw new IOException ("Unexpected body identifier "+rtok.sval);
         }
      }
   }   

   protected void writeBodyInfo (
      PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {

      int numw = 0;
      for (BodyInfo binfo : myBodyInfos) {
         if (binfo.getBody().isWritable()) {
            numw++;
         }
      }
      if (numw == 0) {
         pw.println ("[ ]");
      }
      else {
         CompositeComponent ancestor =
            ComponentUtils.castRefToAncestor (ref);
         pw.println ("[");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (BodyInfo binfo : myBodyInfos) {
            if (binfo.getBody().isWritable()) {
               if (binfo.getBody() instanceof FemModel3d) {
                  pw.println ("FemModel3d " +
                              ComponentUtils.getWritePathName (
                                 ancestor, binfo.getBody()));
               }
               else if (binfo.getBody() instanceof Frame) {
                  pw.println ("Frame " +
                               ComponentUtils.getWritePathName (
                                 ancestor, binfo.getBody()));
                  pw.print ("[ ");
                  IndentingPrintWriter.addIndentation (pw, 2);
                  ((FrameInfo)binfo).myBasePose.write (pw, fmt);
                  IndentingPrintWriter.addIndentation (pw, -2);
                  pw.println ("]");           
               }
            }
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      
      rtok.nextToken();
      if (scanAttributeName (rtok, "bodies")) {
         tokens.offer (new StringToken ("bodies", rtok.lineno()));
         scanBodyInfo (rtok, tokens);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "bodies")) {
         for (int i=0; i<myBodyInfos.size(); i++) {
            ModelComponent body =
               postscanReference (tokens, ModelComponent.class, ancestor);
            BodyInfo binfo = myBodyInfos.get(i);
            if (body instanceof Frame) {
               ((FrameInfo)binfo).myFrame = (Frame)body;
            }
            else if (body instanceof FemModel3d) {
               ((FemModelInfo)binfo).myFemModel = (FemModel3d)body;
            }
            else {
               throw new IOException (
                  "master body "+body+" not supported");
            }
         }
         return true;                     
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.print ("bodies=");
      writeBodyInfo (pw, fmt, ancestor);
      super.writeItems (pw, fmt, ancestor);
   }

   /* ---  Collidable interface --- */

   /**
    * {@inheritDoc}
    */
   @Override
   public PolygonalMesh getCollisionMesh() {
      MeshBase mesh = getMesh();
      if (mesh instanceof PolygonalMesh) {
         return (PolygonalMesh)mesh;
      }
      else {
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean hasDistanceGrid() {
      return false;
   }
   
   /**
    * {@inheritDoc}
    */  
   @Override   
   public DistanceGridComp getDistanceGridComp() {
      return null;
   }
   
   /**
    * {@inheritDoc}
    */ 
   @Override
   public Collidable getCollidableAncestor() {
      return null;
   }

   /**
    * {@inheritDoc}
    */  
   @Override
   public boolean isCompound() {
      return false;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isDeformable () {
      return true;
   }

   /**
    * Returns an estimate of the mass of this SkinMeshBody (as determined by
    * the underlying master bodies) for purposes of handling collisions.
    *
    * @return estimated mass
    */
   @Override
   public double getMass() {
      // XXX how to estimate mass?
      // right now, just take mass of all masters
      double m = 0;
      for (BodyInfo binfo : myBodyInfos) {
         if (binfo.getBody() instanceof RigidBody) {
            m += ((RigidBody)binfo.getBody()).getMass();
         }
         else if (binfo.getBody() instanceof FemModel3d) {
            m += ((FemModel3d)binfo.getBody()).getMass();
         }
      }
      return m;
   }

   /**
    * {@inheritDoc}
    */
   public void collectVertexMasters (
      List<ContactMaster> mlist, Vertex3d vtx) {
      mlist.add (getVertexAttachment(vtx.getIndex()));
   }
   
   /**
    * {@inheritDoc}
    */   
   public boolean containsContactMaster (CollidableDynamicComponent comp) {
      if (comp instanceof Frame) {
         return hasMasterBody (comp);
      }
      else if (comp instanceof FemNode3d) {
         FemNode3d node = (FemNode3d)comp;
         CompositeComponent gparent = node.getGrandParent();
         if (gparent instanceof FemModel3d) {
            return hasMasterBody (gparent);
         }
      }
      return false;
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean allowCollision (
      ContactPoint cpnt, Collidable other, Set<Vertex3d> attachedVertices) {
      if (CollisionHandler.attachedNearContact (
         cpnt, other, attachedVertices)) {
         return false;
      }
      return true;
   }
   
   /**
    * {@inheritDoc}
    */  
   public int getCollidableIndex() {
      return myCollidableIndex;
   }
   
   /**
    * {@inheritDoc}
    */  
   public void setCollidableIndex (int idx) {
      myCollidableIndex = idx;
   }

   /* --- geometry transformation, rendering and references --- */

   /**
    * {@inheritDoc}
    */
   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {

      // if we are simulating, make no changes
      if ((flags & TransformableGeometry.TG_SIMULATING) != 0) {
         return;
      } 

      ArrayList<TransformableGeometry> masterBodies = new ArrayList<>();
      for (BodyInfo bi : myBodyInfos) {
         masterBodies.add ((TransformableGeometry)bi.getBody());
      }

      if (!gtr.isRigid() || !context.containsAll (masterBodies)) {
         if (getMesh() != null) {
            gtr.transform (getMesh());
         }
         //myMeshInfo.transformGeometry (gtr, /*constrainer=*/null);
         // request an action to update vertex attachments if transform is
         // non-rigid or context does not contain all bodies
         context.addAction (
            new TransformGeometryAction() {
               public void transformGeometry (
                  GeometryTransformer gtr,
                  TransformGeometryContext context, int flags) {
                  computeAllVertexConnections();
                  updateBodyPositionInfo();
                  myBodyInfoUpdated = true;
               }
            });
      }
      else {
         for (PointSkinAttachment a : myVertexAttachments) {
            a.transformGeometry (gtr, context, flags);
         }
         context.addAction (
            new TransformGeometryAction() {
               public void transformGeometry (
                  GeometryTransformer gtr,
                  TransformGeometryContext context, int flags) {
                  updateSlavePos();
               }
            });
      }

      for (BodyInfo binfo : myBodyInfos) {
         if (binfo instanceof FrameInfo) {
            FrameInfo finfo = (FrameInfo)binfo;
            if (context.contains (finfo.myFrame)) {
               gtr.transform (finfo.myBasePose);
            }
         }         
      }
      myBodyInfoUpdated = false;
      //super.transformGeometry (gtr, context, flags);
   }   
   
   /**
    * {@inheritDoc}
    */  
   @Override
   public void prerender(RenderList list) {
      super.prerender (list);
      list.addIfVisible (myMarkers);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void connectToHierarchy (CompositeComponent hcomp) {
      // XXX not sure what to do here. Probably don't want to add back
      // references to all master components, but then we need a way to remove
      // masters from the attachments when masters disappear
      super.connectToHierarchy (hcomp);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void disconnectFromHierarchy(CompositeComponent hcomp) {
      // XXX not sure what to do here ... see comment in connectToParent()
      super.disconnectFromHierarchy(hcomp);
   }

   /**
    * {@inheritDoc}
    */
   public void getSoftReferences (List<ModelComponent> refs) {
      super.getSoftReferences (refs);
      for (BodyInfo binfo : myBodyInfos) {
         refs.add (binfo.getBody());
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      if (undo) {
         Object obj = undoInfo.removeFirst();
         if (obj != NULL_OBJ) {
            ((ListRemove<BodyInfo>)obj).undo();
         }
      }
      else {
         ListRemove<BodyInfo> bodyRemove = null;
         for (int i=0; i<myBodyInfos.size(); i++) {
            if (!ComponentUtils.areConnected (
                   this, myBodyInfos.get(i).getBody())) {
               if (bodyRemove == null) {
                  bodyRemove = new ListRemove<BodyInfo>(myBodyInfos);
               }
               bodyRemove.requestRemove(i);
            }
         }
         if (bodyRemove != null) {
            bodyRemove.remove();
            undoInfo.addLast (bodyRemove);
         }
         else {
            undoInfo.addLast (NULL_OBJ);
         }
      }
   }

}
