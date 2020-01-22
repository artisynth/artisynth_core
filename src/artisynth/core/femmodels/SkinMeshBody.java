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

import maspack.function.ConstantFuntion1x1;
import maspack.function.SISOFunction;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.Face;
import maspack.geometry.GeometryTransformer;
import maspack.geometry.HalfEdge;
import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.geometry.DistanceGrid;
import maspack.matrix.DualQuaternion;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.util.IndentingPrintWriter;
import maspack.util.ListRemove;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.femmodels.PointSkinAttachment.Connection;
import artisynth.core.femmodels.PointSkinAttachment.FrameConnection;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.CollidableBody;
import artisynth.core.mechmodels.CollidableDynamicComponent;
import artisynth.core.mechmodels.CollisionHandler;
import artisynth.core.mechmodels.ContactMaster;
import artisynth.core.mechmodels.ContactPoint;
import artisynth.core.mechmodels.DistanceGridComp;
import artisynth.core.mechmodels.DynamicAttachment;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointAttachable;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.SkinMeshBase;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;

/**
 * A MeshComponent that supports "skinning", in which the position of each mesh
 * vertex is controlled by the position of one or more "master" components
 * (such as Frames or FEM nodes). 
 *
 * <p><code>SkinMeshBody</code> manages this skinning by assigning a
 * {@link PointSkinAttachment} to each vertex within the mesh, which stores the
 * master components used to control the vertex position and computes that
 * position using the
 * {@link PointSkinAttachment#computePosState computePosState()} method.
 *
 * <p> {@link PointSkinAttachment} is an instance of {@link PointAttachment},
 * and when used by <code>SkinMeshBody</code> to control vertex positions it does
 * not connect to any {@link Point} component as a slave (i.e., {@link
 * PointSkinAttachment#getSlave() getSlave()} returns <code>null</code>.
 * However, <code>PointSkinAttachments</code> can also
 * be used to attach a point or a marker to the "skin" of a
 * <code>SkinMeshBody</code>, and so allow velocities to propagate from the master
 * components to the point, and forces to propagate from the point to the
 * masters.
 *
 * <p>
 * Master components can currently include {@link Frame}, {@link FemNode3d}
 * (within a specific {@link FemModel3d}), and individual {@link Particle}
 * objects. The set of master components can vary from vertex to
 * vertex. <code>PointSkinAttachment</code> maintains a set of
 * <i>connections</i> to each master component, each with a specified
 * weight, and computes the final vertex position as a weighted
 * sum of the positions computed for each connection. In pseudocode,
 * this can be expressed as:
 * <pre>
 *    vertexPos = 0;
 *    for (each connection c) {
 *       vertexPos += c.weight * c.connectionPos();
 *    }
 * </pre>
 *
 * <p> <code>Frames</code> control vertex positions using various skinning
 * techniques known in the literature. These include linear, linear dual
 * quaternion, and iterative dual quaternion skinning. The technique used is
 * controlled by the {@link SkinMeshBody#getFrameBlending()} and {@link
 * SkinMeshBody#setFrameBlending setFrameBlending()} methods. Implementing these
 * techniques requires <code>SkinMeshBody</code> to maintain additional information
 * for each Frame.  Therefore it is necessary for all Frames used in skinning to
 * be registered with the SkinMeshBody using
 * {@link SkinMeshBody#addFrame addFrame()}.
 *
 * <p>
 * FEM nodes can control vertex positions using linear combinations of
 * either their current position values, or their current displacements from the
 * rest position. It is also recommended that any FemModel used in skinning be
 * registered with the SkinMeshBody using
 * {@link SkinMeshBody#addFemModel addFemModel()},
 * although neither of the currently implemented <code>FemNode</code> control
 * methods actually require this.
 *
 * <p>
 * Finally, it is also possible to use a vertex's initial <i>base</i>
 * position to control a portion of its final position. Vertex base
 * positions are also used for Frame-based skinning. Vertex base
 * positions are set initially when a mesh is assigned to the skin.
 * Base positions can be queried and set using
 * {@link SkinMeshBody#getBasePosition getBasePosition()} and
 * {@link SkinMeshBody#setBasePosition setBasePosition()}, and can be reset
 * <i>en mass</i> using {@link SkinMeshBody#resetBasePositions}.
 *
 * <p>
 * Setting up a <code>SkinMeshBody</code> involves three basic steps:
 * <ul>
 * <li>Creating the SkinMeshBody and assigning it a PolygonalMesh;</li>
 * <li>Registering all Frames and FemModels that will be used to control it;</li>
 * <li>Creating appropriate attachments for each vertex.
 * </ul>
 * This process can be as simple as follows:
 * <pre>
 *    skinMesh = new SkinMeshBody (polyMesh);
 *    skinMesh.addFrame (rigidBody1);
 *    skinMesh.addFrame (rigidBody2);
 *    skinMesh.addFemModel (femModel1);
 *    skinMesh.computeDisplacementAttachments();
 * </pre>
 * Here, {@link SkinMeshBody#computeDisplacementAttachments 
 * computeDisplacementAttachments()}
 * automatically computes an attachment for each vertex involving
 * all registered Frames and FemModels that contain a polygonal mesh.
 * The weights connecting each vertex to these master components is based
 * on their current distance to the vertex. Other functions for
 * computing attachments, based on different schemes, may be added
 * in the future.
 *
 * <p>
 * Alternatively, the application may compute attachments directly.
 * A sketch of the required code might look like this:
 * <pre>
 *    skinMesh.clearAttachments();
 *    for (int i=0; i &lt; skinMesh.numVertices(); i++) {
 *       Vertex3d vtx = skinMesh.getVertex (i);
 *
 *       ... use vertex position to compute weights w1, w2, w3, w4
 *           relating it to various master components ...
 *
 *       PointSkinAttachment a = new PointSkinAttachment();
 *       // add new connections:
 *       a.addFrameConnection (body1Index, w1);
 *       a.addFrameConnection (body2Index, w2);
 *       a.addFemDisplacementConnection (femNode4, w3);
 *       a.addFemDisplacementConnection (femNode6, w4);
 *       a.finalizeConnections();
 *       skinMesh.addAttachment (a);
 *    }
 * </pre>
 * 
 * The initial call to <code>clearAttachments()</code> is necessary if the
 * skinMesh already contains attachments. Then for each vertex, master
 * components are determined, along with their associated weights, and an
 * attachment is created with master connections added using various
 * <code>addXXXConnection()</code> methods followed by a call to {@link
 * PointSkinAttachment#finalizeConnections finalizeConnections()}. The method
 * {@link PointSkinAttachment#addFrameConnection addFrameConnection()}
 * specifies a <code>Frame</code> master, which controls the vertex position
 * using the skinning technique returned by {@link
 * SkinMeshBody#getFrameBlending()}.  The Frame itself is specified using its
 * <code>FrameInfo</code> structure within <code>SkinMeshBody</code>, which
 * can be obtained
 * using {@link SkinMeshBody#getFrameInfo(Frame) getFrameInfo()}. The method {@link
 * PointSkinAttachment#addFemDisplacementConnection
 * addFemDisplacementConnection()} specifies a {@link FemNode3d} as a master,
 * which controls the vertex position using its current displacement from its
 * own rest position.  Other types of connections are possible; one should
 * consult the source code or documentation for {@link PointSkinAttachment}.
 *
 * <p> When calculating attachment weights, an application will likely to need
 * access to the Frames and FemModels that are registered with the SkinMeshBody
 * (for example, to make distance queries on their surface meshes).
 * This can be done using methods such as {@link
 * SkinMeshBody#numFrames()}, {@link SkinMeshBody#getFrame}, {@link
 * SkinMeshBody#numFemModels() numFemModels()}, and {@link
 * SkinMeshBody#getFemModel getFemModel()}.
 */
public class SkinMeshBody extends SkinMeshBase 
   implements CollidableBody, PointAttachable {

   /**
    * Characterizes the blend mechanism used for the frame-based
    * portion of the skinning.
    */
   public enum FrameBlending {
      LINEAR, DUAL_QUATERNION_LINEAR, DUAL_QUATERNION_ITERATIVE};

   public static FrameBlending DEFAULT_FRAME_BLENDING = FrameBlending.LINEAR;
   public static int DQ_MAX_BLEND_STEPS = 3;
   public static double DQ_BLEND_TOLERANCE = 1e-8;
   
   FrameBlending myFrameBlending = DEFAULT_FRAME_BLENDING;

   protected ArrayList<FrameInfo> myFrameInfo;
   protected ArrayList<FemModelInfo> myFemModelInfo;
   protected ComponentList<PointSkinAttachment> myVertexAttachments;

   // default sigma value to be used by computeDisplacementAndWeights()
   public static double myDefaultSigma = -1;
   // last sigma value used by computeDisplacementAndWeights()
   protected double myLastSigma = myDefaultSigma;

   protected static final Collidability DEFAULT_COLLIDABILITY =
      Collidability.ALL;   
   protected Collidability myCollidability = DEFAULT_COLLIDABILITY;
   protected int myCollidableIndex;

   /**
    * Base class for information about bodies (e.g., Frames or
    * FemNodels) used to control the skinning.
    */
   public abstract class BodyInfo {

      BodyInfo () {
      }

      /**
       * Returns the PolygonalMesh, if any, associated with this body.
       */
      public abstract PolygonalMesh getMesh();
   }

   /**
    * Contains information for each frame controlling this SkinMeshBody.
    */
   public class FrameInfo extends BodyInfo {

      protected Frame myFrame;
      protected RigidTransform3d myBasePose;
      protected DualQuaternion myBlendQuaternion;
      protected RigidTransform3d myDeltaPose;
      protected int myIndex;

      FrameInfo () {
         super ();
         myBasePose = new RigidTransform3d();
         myDeltaPose = new RigidTransform3d();
         myBlendQuaternion = new DualQuaternion();
      }

      FrameInfo (Frame frame) {
         this();
         setFrame (frame);
      }

      public void setIndex (int idx) {
         myIndex = idx;
      }
      
      public int getIndex() {
         return myIndex;
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
       * The computation is done in 
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
            return ((RigidBody)myFrame).getMesh();
         }
         else {
            return null;
         }
      }

      /**
       * Computes the quantities returned by {@link #getDeltaPose()}, and, for
       * non-linear blending, {@link #getBlendQuaternion()}. This should be
       * called, before these quantities are used, whenever the Frame's
       * pose changes.
       */
      public void updatePosState() {
         myDeltaPose.mulInverseRight (
            myFrame.getPose(), myBasePose);
         if (myFrameBlending != FrameBlending.LINEAR) {
            updateDualQuaternions();
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
      }
   }
  
   /**
    * Contains information for each FemModel controlling this SkinMeshBody.
    * Doesn't do very much now - can be expanded later as needed.
    */
   public class FemModelInfo extends BodyInfo {

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
         if (myDefaultMesh != myFemModel.getSurfaceMesh()) {
            myDefaultMesh = myFemModel.getSurfaceMesh();
            if (myFemModel.numQuadraticElements() > 0) {
               FemMeshComp mcomp = FemMeshComp.createSurface (null, myFemModel);
               myMesh = (PolygonalMesh)mcomp.getMesh();
            }
            else {
               myMesh = myDefaultMesh;
            }
         }
         return myMesh;
      }

   }

   /**
    * Special class to calculate distances and weights from a specified
    * position to each of the currently registered bodies that contain a mesh.
    */
   protected class MeshDistCalc {

      // List of all Frames and FemModels that have meshes
      ArrayList<BodyInfo> meshBodies;
      BVFeatureQuery query;
      Point3d nearest;
      Vector2d uv;
      double[] dtmp;
      double[] wgts;
      Face[] faces;
      double[][] coords;
      PolygonalMesh[] meshes;

      MeshDistCalc() {
         query = new BVFeatureQuery();
         nearest = new Point3d();
         uv = new Vector2d();

         meshBodies = new ArrayList<BodyInfo>();
         // create lists of the bodies that actually have meshes
         for (FrameInfo finfo : myFrameInfo) {
            if (finfo.getMesh() != null) {
               meshBodies.add (finfo);
            }
         }
         for (FemModelInfo finfo : myFemModelInfo) {
            if (finfo.getMesh() != null) {
               meshBodies.add (finfo);
            }
         }
         int nbodies = meshBodies.size();
         dtmp = new double[nbodies];
         wgts = new double[nbodies];
         faces = new Face[nbodies];
         coords = new double[nbodies][];        
         meshes = new PolygonalMesh[nbodies];
      }

      int numBodies() {
         return meshBodies.size();
      }

      double getWeight (int idx) {
         return wgts[idx];
      }

      BodyInfo getBodyInfo (int idx) {
         return meshBodies.get(idx);
      }

      Face getFace (int idx) {
         return faces[idx];
      }

      double[] getCoords (int idx) {
         return coords[idx];
      }      

      PointSkinAttachment computeDisplacementAttachment (
         Point3d pos, double sigma) {

         double dmin = Double.POSITIVE_INFINITY;
         int nbodies = meshBodies.size();

         for (int j=0; j<nbodies; j++) {
            PolygonalMesh bmesh = meshBodies.get(j).getMesh();
            faces[j] = query.nearestFaceToPoint (nearest, uv, bmesh, pos);
            coords[j] = new double[] { 1.0-uv.x-uv.y, uv.x, uv.y };
            double d = pos.distance (nearest);
            if (d < dmin) {
               dmin = d;
            }
            dtmp[j] = d;
         }

         double sumw = 0;
         for (int j=0; j<dtmp.length; j++) {
            double d, w;
            d = dtmp[j];
            if (d == Double.MAX_VALUE) {
               w = 0;
            }
            else if (d == dmin) {
               w = 1;
            }
            else {
               if (sigma <= 0) {
                  w = dmin*dmin/(d*d);
               } else {
                  w = Math.exp(-(d-dmin)*(d-dmin)/2/sigma/sigma);
               }
            }
            wgts[j] = w;
            sumw += w;
         }
         for (int j=0; j<nbodies; j++) {
            wgts[j] /= sumw;
         }

         // create an attachment for each body. Assume that
         // that vertexAttachments have already be allocated 
         PointSkinAttachment a = new PointSkinAttachment();
         for (int j=0; j<numBodies(); j++) {
            if (meshBodies.get(j) instanceof FrameInfo) {
               FrameInfo finfo = (FrameInfo)meshBodies.get(j);
               a.addFrameConnection (finfo, wgts[j]);
            }
            else {
               // body is an FEM - create a weighted connection to face nodes
               FemModel3d fem = ((FemModelInfo)meshBodies.get(j)).myFemModel;
               Vertex3d[] vtxs = faces[j].getVertices();
               for (int k=0; k<3; k++) {
                  double w = wgts[j]*coords[j][k];
                  FemNode3d node = fem.getSurfaceNode (vtxs[k]);
                  if (node != null) {
                     a.addFemDisplacementConnection (node, w);
                  }
               }
               a.addBaseConnection (wgts[j]);
            }
         }        
         return a;
      }
   }
  
   public static PropertyList myProps =
      new PropertyList (SkinMeshBody.class, SkinMeshBase.class);

   static {
      myProps.add("frameBlending", "frame blending mechanism",
                  DEFAULT_FRAME_BLENDING);
      myProps.add (
         "collidable", 
         "sets the collidability of this SkinMeshBody mesh", DEFAULT_COLLIDABILITY);      
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Creates an empty SkinMeshBody.
    */
   public SkinMeshBody () {
      super();
      myVertexAttachments =
         new ComponentList<PointSkinAttachment>(
            PointSkinAttachment.class, "attachments");
      add (myVertexAttachments);
      myFrameInfo = new ArrayList<FrameInfo>();
      myFemModelInfo = new ArrayList<FemModelInfo>();
   }

   /**
    * Creates a SkinMeshBody with a specified mesh.
    */
   public SkinMeshBody (MeshBase mesh) {
      this();
      setMesh (mesh);
   }

   /**
    * Sets the blend type for that part of the skinning that depends on frames.
    */
   public void setFrameBlending(FrameBlending type) {
      myFrameBlending = type;
   }
   
   /**
    * Returns the blend type for that part of the skinning that depends on
    * frames.
    */
   public FrameBlending getFrameBlending() {
      return myFrameBlending;
   }

   /**
    * Returns the number of attachments currently in this SkinMeshBody.
    */
   public int numAttachments() {
      return myVertexAttachments.size();
   }

   /**
    * Adds an attachment to this SkinMeshBody. The associated vertex is assumed
    * from the index of the new attachment point, and the attachment's base
    * position is set to the current vertex position.  The number of
    * attachments cannot exceed the current number of mesh vertices.
    */
   public void addAttachment (PointSkinAttachment a) {
      addAttachment (a, /*initBase=*/true);
   }

   // XXX
   /**
    * Adds an attachment to this SkinMeshBody. The associated vertex is assumed
    * from the index of the new attachment point. If <code>initBase</code> is
    * true, then the attachment's base position is set to the current vertex
    * position. The number of attachments cannot exceed the current number of
    * mesh vertices.
    */
   public void addAttachment (PointSkinAttachment a, boolean initBase) {
      if (numAttachments() >= numVertices()) {
         throw new IllegalArgumentException (
            "Number of attachments may not exceed number of vertices");
      }
      if (a == null) {
         throw new IllegalArgumentException ("Attachment is null");
      }
      int vidx = myVertexAttachments.size();
      myVertexAttachments.addNumbered (a, vidx);
      a.setSkinMesh (this);
      if (initBase) {
         a.setBasePosition (getVertex(vidx).getPosition());
      }
   }
   
   public void setAttachment(int idx, PointSkinAttachment a) {
      setAttachment(idx, a, true);
   }
   
   public void setAttachment( int idx, PointSkinAttachment a, boolean initBase) {
      myVertexAttachments.set(idx, a);
      a.setSkinMesh (this);
      if (initBase) {
         a.setBasePosition (getVertex(idx).getPosition());
      }
   }

   /**
    * Clears all the attachments associated with this SkinMeshBody.
    */
   public void clearAttachments() {
      myVertexAttachments.clear();
   }

   /**
    * Returns the attachment for the idx-th vertex of this SkinMeshBody.
    *
    * @param idx index of the vertex for which the attachment is desired.
    * Must be less that {@link #numAttachments()}.
    */
   public PointSkinAttachment getAttachment (int idx) {
      return myVertexAttachments.getByNumber (idx);
   }
   
   public PointSkinAttachment getAttachment(Vertex3d vtx) {
      return getAttachment(vtx.getIndex ());
   }

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
    * Sets the mesh associated with the this SkinMeshBody. A new set of attachment
    * components will be created for each vertex, each with their base position
    * set to the current vertex position.
    */
   public void setMesh (MeshBase mesh) {
      // super method will call setMeshFromInfo()
      super.setMesh (mesh);
      initializeAttachments();
   }

   private void initializeAttachments() {
      MeshBase mesh = getMesh();
      int numVtxs = mesh.numVertices();
      myVertexAttachments.removeAll();
      myVertexAttachments.ensureCapacity (numVtxs);
   }

   /**
    * Registers a Frame with this SkinMeshBody so that it can be used for skinning
    * control.
    *
    * @param frame Frame to be registered
    */
   public void addFrame (Frame frame) {
      if (hasFrame (frame)) {
         throw new IllegalArgumentException (
            "Frame "+frame+" already present");
      }
      FrameInfo finfo = new FrameInfo (frame);
      finfo.setIndex (myFrameInfo.size());
      myFrameInfo.add (finfo);
   }

   /**
    * Removes a Frame from this SkinMeshBody
    * @param frame Frmae to be removed
    * @return true if frame found aand removed, false otherwise
    */
   public boolean removeFrame (Frame frame) {
      
      FrameInfo found = null;;
      for (FrameInfo frameInfo : myFrameInfo) {
         if (frameInfo.getFrame() == frame) {
            // mark for removal
            found = frameInfo;
            break;
         }
      }
      
      if (found != null) {
         int idx = found.getIndex();
         int lastIdx = myFrameInfo.size ()-1;
         // replace with last item
         if (idx != lastIdx) {
            FrameInfo last = myFrameInfo.get(lastIdx);
            last.setIndex(idx);  // renumber
            myFrameInfo.set(idx, last);
            myFrameInfo.remove(lastIdx);
         }
         return true;
      }
      
      return false;
      
   }

   
   /**
    * Returns true is a specified Frame is currently registered with this
    * SkinMeshBody.
    *
    * @param frame Frame to be queried
    */
   public boolean hasFrame (Frame frame) {
      for (FrameInfo fi : myFrameInfo) {
         if (fi.getFrame() == frame) {
            return true;
         }
      }
      return false;
   }

   /**
    * Returns the number of Frames currently registered with this SkinMeshBody.
    */
   public int numFrames() {
      return myFrameInfo.size();
   }

   /**
    * Returns an information structure for a Frame that is currently registered
    * with this SkinMeshBody.
    *
    * @param idx identities the Frame; must be between 0 and
    * the number returned by {@link #numFrames()}.
    */
   public FrameInfo getFrameInfo (int idx) {
      return myFrameInfo.get (idx);
   }

   /**
    * Returns the information structures of all Frames that are currently
    * registered with this SkinMeshBody.
    */
   public List<FrameInfo> getAllFrameInfo() {
      return myFrameInfo;
   }

   /**
    * Returns a Frame that is currently registered with SkinMeshBody.
    *
    * @param idx identities the Frame; must be between 0 and
    * the number returned by {@link #numFrames()}.
    */
   public Frame getFrame (int idx) {
      return myFrameInfo.get(idx).myFrame;
   }

   /**
    * Returns the FrameInfo for a Frame that is currently registered with this
    * SkinMeshBody, or null if the Frame is not registered.
    * 
    * @param frame Frame whose FrameInfo is desired
    */
   public FrameInfo getFrameInfo (Frame frame) {
      for (int i=0; i<myFrameInfo.size(); i++) {
         FrameInfo finfo = myFrameInfo.get(i);
         if (finfo.myFrame == frame) {
            return finfo;
         }
      }
      return null;
   }
   
   /**
    * Registers a FemModel with this SkinMeshBody so that it can be used for skinning
    * control.
    *
    * @param fem FemModel to be registered
    */
   public void addFemModel (FemModel3d fem) {
      if (hasFemModel (fem)) {
         throw new IllegalArgumentException (
            "FemModel "+fem+" already present");
      }
      FemModelInfo finfo = new FemModelInfo (fem);
      myFemModelInfo.add (finfo);
   }
   
   /**
    * Returns true is a specified FemModel is currently registered with this
    * SkinMeshBody.
    *
    * @param fem FemModel to be queried
    */
   public boolean hasFemModel (FemModel3d fem) {
      return myFemModelInfo.contains (fem);
   }

   /**
    * Returns the number of FemModels currently registered with this SkinMeshBody.
    */
   public int numFemModels() {
      return myFemModelInfo.size();
   }

   /**
    * Returns an information structure for a FemModel that is currently registered
    * with this SkinMeshBody.
    *
    * @param idx identities the FemModel; must be between 0 and
    * the number returned by {@link #numFemModels()}.
    */
   public FemModelInfo getFemModelInfo (int idx) {
      return myFemModelInfo.get (idx);
   }

   /**
    * Returns the information structures of all FemModels that are currently
    * registered with this SkinMeshBody.
    */
   public List<FemModelInfo> getAllFemModelInfo() {
      return myFemModelInfo;
   }

   /**
    * Returns a FemModel that is currently registered with SkinMeshBody.
    *
    * @param idx identities the FemModel; must be between 0 and
    * the number returned by {@link #numFemModels()}.
    */
   public FemModel3d getFemModel (int idx) {
      return myFemModelInfo.get(idx).myFemModel;
   }

   /**
    * Returns the index of a FemModelthat is currently registered with t
    * this SkinMeshBody, or -1 if the FemModel is not registered.
    * 
    * @param fem FemModel whose index is desired
    */
   public int getFemModelIndex (FemModel fem) {
      for (int i=0; i<myFemModelInfo.size(); i++) {
         if (myFemModelInfo.get(i).myFemModel == fem) {
            return i;
         }
      }
      return -1;
   }
   
   /**
    * Returns the base position for a specified vertex attachment in this
    * SkinMeshBody.  Base positions are used to calculate vertex positions for
    * skinning strategies that are based on displacement.
    *
    * @param idx index of the attachment. Must be less than
    * {@link #numAttachments}.
    */
   public Point3d getBasePosition (int idx) {
      return myVertexAttachments.getByNumber(idx).getBasePosition();
   }

   /**
    * Sets the base position for a specified vertex attachment in the
    * SkinMeshBody.
    *
    * @param idx index of the attachment. Must be less than
    * {@link #numAttachments}.
    * @param pos new base positon
    */
   public void setBasePosition (int idx, Vector3d pos) {
      myVertexAttachments.getByNumber(idx).setBasePosition(pos);
   }

   /**
    * Resets the base positions for all attachments in this SkinMeshBody
    * to the current vertex position.
    */
   protected void resetBasePositions () {
      MeshBase mesh = getMesh();
      for (int i=0; i<myVertexAttachments.size(); i++) {
         PointSkinAttachment a = myVertexAttachments.get(i);
         Vertex3d vtx = mesh.getVertices().get(a.getNumber());
         a.setBasePosition (vtx.getPosition());
      }
   }

   /**
    * Smooths weights according to a weighting function of distance.  At
    * present, this <i>only</i> smooths the weights associated with Frame
    * connections. Results when the SkinMeshBody contains other types of
    * attachment connections are undefined.
    *
    * @param weightFunction single-input single-output function of distance
    *       to use as weights
    * @param networkDist number of paths to traverse to collect vertices
    */
   public void smoothWeights (SISOFunction weightFunction, int networkDist) {
      
      ArrayList<Vertex3d> vtxs = getMesh().getVertices();
      ArrayList<Vertex3d> vtxList = new ArrayList<Vertex3d>();

      int numf = numFrames();      

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
               for (int j=0; j<attacher.numConnections(); j++) {
                  Connection c = attacher.getConnection(j);
                  if (c instanceof FrameConnection) {
                     int fidx = ((FrameConnection)c).getFrameIndex();
                     w[fidx] += wd*attacher.getWeight(j);
                  }
               }
            }
         }
         PointSkinAttachment attacher = myVertexAttachments.getByNumber(i);
         if (attacher != null) {
            for (int j=0; j<attacher.numConnections(); j++) {
               Connection c = attacher.getConnection(j);
               if (c instanceof FrameConnection) {
                  int fidx = ((FrameConnection)c).getFrameIndex();
                  attacher.setWeight (j, w[fidx]/wTotal);
               }
            }
         }
      }
   }
   
   public void smoothWeights(int networkDist) {
      smoothWeights(new ConstantFuntion1x1(1), networkDist);
   }
   
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


   public void computeWeights() {
      computeDisplacementAttachments (myDefaultSigma);
   }

   /**
    * Computes displacement-based attachments for each vertex attachment in
    * this skin mesh.  For each vertex, the weights are determined using the
    * relative distances from the vertex to the surface meshes of each of the
    * controlling bodies (Frame, FemModel, etc.). Controlling bodies which
    * don't have a surface mesh are ignored.
    * 
    * <p>
    * Bodies further away have a lower weighting. If <code>sigma</code>
    * is non-positive, the weighting is determined using an inverse-square
    * attenuation. Otherwise, the weighting is determined using a
    * Gaussian attention controlled by <code>sigma</code>.
    *
    * @param sigma if greater than 0, specifies a Gaussian weighting
    * attenuation.
    */
   public void computeDisplacementAttachments (double sigma) {
      
      MeshBase mesh = getMesh();
      MeshDistCalc dcalc = new MeshDistCalc();

      clearAttachments();
      for (int i=0; i<mesh.numVertices(); i++) {
         Vertex3d vtx = mesh.getVertices().get(i);
         PointSkinAttachment a =
            dcalc.computeDisplacementAttachment (vtx.getPosition(), sigma);
         addAttachment (a);
      }
      myLastSigma = sigma;
   }
   
   protected void updateDualQuaternions () {
      for (FrameInfo finfo : myFrameInfo) {
         finfo.updateDualQuaternion();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void updateSlavePos () {
      for (FrameInfo finfo : myFrameInfo) {
         finfo.updatePosState();
      }
      super.updateSlavePos();
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      
      // if we are simulating, make no changes
      if ((flags & TransformableGeometry.TG_SIMULATING) != 0) {
         return;
      } 
      // update base positions for vertices
      for (int i=0; i<myVertexAttachments.size(); i++) {
         Point3d base = myVertexAttachments.get(i).getBasePosition();
         gtr.transformPnt (base);
      }
      // update base poses for frames that are being transformed
      for (FrameInfo finfo : myFrameInfo) {
         if (context.contains (finfo.myFrame)) {
            gtr.transform (finfo.myBasePose);
         }         
      }
      super.transformGeometry (gtr, context, flags);      
   }   

   protected void scanFrameInfo (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {
      myFrameInfo.clear();
      rtok.scanToken ('[');
      while (ScanWriteUtils.scanAndStoreReference (rtok, tokens)) {
         RigidTransform3d basePose = new RigidTransform3d();
         basePose.scan (rtok);
         // frame will be filled in by postscan ...
         FrameInfo finfo = new FrameInfo ();
         finfo.myBasePose.set (basePose);
         finfo.setIndex (myFrameInfo.size());
         myFrameInfo.add (finfo);
      }
      if (rtok.ttype != ']') {
         throw new IOException ("Expected ']', got " + rtok);
      }      
   }

   protected void writeFrameInfo (
      PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      
      int numw = 0;
      for (FrameInfo finfo : myFrameInfo) {
         if (finfo.myFrame.isWritable()) {
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
         for (FrameInfo finfo : myFrameInfo) {
            if (finfo.myFrame.isWritable()) {
               pw.println (ComponentUtils.getWritePathName (
                              ancestor, finfo.myFrame));
               pw.print ("[ ");
               IndentingPrintWriter.addIndentation (pw, 2);
               finfo.myBasePose.write (pw, fmt);
               IndentingPrintWriter.addIndentation (pw, -2);
               pw.println ("]");
            }
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }

   protected void scanFemModelInfo (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {
      myFemModelInfo.clear();
      rtok.scanToken ('[');
      while (ScanWriteUtils.scanAndStoreReference (rtok, tokens)) {
         FemModelInfo finfo = new FemModelInfo (null);
         myFemModelInfo.add (finfo);
      }
      if (rtok.ttype != ']') {
         throw new IOException ("Expected ']', got " + rtok);
      }      

   }

   protected void writeFemModelInfo (
      PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {

      int numw = 0;
      for (FemModelInfo finfo : myFemModelInfo) {
         if (finfo.myFemModel.isWritable()) {
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
         for (FemModelInfo finfo : myFemModelInfo) {
            if (finfo.myFemModel.isWritable()) {
               pw.println (ComponentUtils.getWritePathName (
                              ancestor, finfo.myFemModel));
            }
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      
      rtok.nextToken();
      if (scanAttributeName (rtok, "frames")) {
         tokens.offer (new StringToken ("frames", rtok.lineno()));
         scanFrameInfo (rtok, tokens);
         return true;
      }
      else if (scanAttributeName (rtok, "femModels")) {
         tokens.offer (new StringToken ("femModels", rtok.lineno()));
         scanFemModelInfo (rtok, tokens);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      super.scan (rtok, ref);
      for (int i=0; i<myVertexAttachments.size(); i++) {
         myVertexAttachments.get(i).setSkinMesh (this);
      }      
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "frames")) {
         for (int i=0; i<myFrameInfo.size(); i++) {
            Frame frame = postscanReference (tokens, Frame.class, ancestor);
            myFrameInfo.get(i).setFrame (frame);
         }
         return true;                     
      }
      else if (postscanAttributeName (tokens, "femModels")) {
         for (int i=0; i<myFemModelInfo.size(); i++) { 
            FemModel3d fem = postscanReference (
               tokens, FemModel3d.class, ancestor);
            myFemModelInfo.get(i).myFemModel = fem;
         }  
         return true;         
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.print ("frames=");
      writeFrameInfo (pw, fmt, ancestor);
      pw.print ("femModels=");
      writeFemModelInfo (pw, fmt, ancestor);
      super.writeItems (pw, fmt, ancestor);
      // pw.print ("attachments=");
      // writeAttachments (pw, fmt, ancestor);
   }

   @Override
   public void connectToHierarchy (CompositeComponent hcomp) {
      // XXX not sure what to do here. Probably don't want to add back
      // references to all master components, but then we need a way to remove
      // masters from the attachments when masters disappear
      super.connectToHierarchy (hcomp);
   }

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
      for (FrameInfo finfo : myFrameInfo) {
         refs.add (finfo.myFrame);
      }
      for (FemModelInfo finfo : myFemModelInfo) {
         refs.add (finfo.myFemModel);
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
            ((ListRemove<FrameInfo>)obj).undo();
         }
         obj = undoInfo.removeFirst();
         if (obj != NULL_OBJ) {
            ((ListRemove<FemModelInfo>)obj).undo();
         }
      }
      else {
         ListRemove<FrameInfo> frameRemove = null;
         for (int i=0; i<myFrameInfo.size(); i++) {
            if (!ComponentUtils.areConnected (
                   this, myFrameInfo.get(i).myFrame)) {
               if (frameRemove == null) {
                  frameRemove = new ListRemove<FrameInfo>(myFrameInfo);
               }
               frameRemove.requestRemove(i);
            }
         }
         if (frameRemove != null) {
            frameRemove.remove();
            undoInfo.addLast (frameRemove);
         }
         else {
            undoInfo.addLast (NULL_OBJ);
         }
         ListRemove<FemModelInfo> femModelRemove = null;
         for (int i=0; i<myFemModelInfo.size(); i++) {
            if (!ComponentUtils.areConnected (
                   this, myFemModelInfo.get(i).myFemModel)) {
               if (femModelRemove == null) {
                  femModelRemove = new ListRemove<FemModelInfo>(myFemModelInfo);
               }
               femModelRemove.requestRemove(i);
            }
         }
         if (femModelRemove != null) {
            femModelRemove.remove();
            undoInfo.addLast (femModelRemove);
         }
         else {
            undoInfo.addLast (NULL_OBJ);
         }
      }
   }

   // Collidable interface

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

   @Override
   public boolean hasDistanceGrid() {
      return false;
   }
   
   @Override   
   public DistanceGridComp getDistanceGridComp() {
      return null;
   }

   @Override
   public Collidability getCollidable () {
      getSurfaceMesh(); // build surface mesh if necessary
      return myCollidability;
   }

   public void setCollidable (Collidability c) {
      if (myCollidability != c) {
         myCollidability = c;
         notifyParentOfChange (new StructureChangeEvent (this));
      }
   }

   @Override
   public Collidable getCollidableAncestor() {
      return null;
   }

   @Override
   public boolean isCompound() {
      return false;
   }

   @Override
   public boolean isDeformable () {
      return true;
   }

   @Override
   public double getMass() {
      // XXX how to estimate mass?
      // mass of all dependencies?
      double m = 0;
      for (PointAttachment pa : myVertexAttachments) {
         for (DynamicComponent dmc : pa.getMasters()) {
            if (dmc != null && !dmc.isMarked()) {
               m += dmc.getMass(0);
               dmc.setMarked(true);
            }
         }
      }
      
      // unmark
      for (PointAttachment pa : myVertexAttachments) {
         for (DynamicComponent dmc : pa.getMasters()) {
            if (dmc != null && dmc.isMarked()) {
               dmc.setMarked(false);
            }
         }
      }
      return m;
   }

   public void getVertexMasters (List<ContactMaster> mlist, Vertex3d vtx) {

      PointSkinAttachment pa = getAttachment(vtx.getIndex());
      for (int j=0; j<pa.numConnections(); j++) {
         DynamicComponent m = pa.getMaster (j);
         // note that m may be null - not all connections are associated
         // with a master component (i.e., numMasters <= numConnections
         if (m instanceof CollidableDynamicComponent) {
            double wm = pa.getWeight (j);
            //System.out.println (
            //   "adding for "+ComponentUtils.getPathName(m)+" w=" + w*wm);
            mlist.add (
               new ContactMaster (
                  (CollidableDynamicComponent)m, wm));
         }
      }
   }
   
   public boolean containsContactMaster (CollidableDynamicComponent comp) {
      LinkedList<DynamicAttachment> malist = comp.getMasterAttachments();
      if (malist != null) {
         for (DynamicAttachment ma : malist) {
            if (ma instanceof PointSkinAttachment) {
               return ((PointSkinAttachment)ma).mySkinMesh == this;
            }
         }
      }
      return false;
   }
   
   public boolean allowCollision (
      ContactPoint cpnt, Collidable other, Set<Vertex3d> attachedVertices) {
      if (CollisionHandler.attachedNearContact (
         cpnt, other, attachedVertices)) {
         return false;
      }
      return true;
   }
   
   public int getCollidableIndex() {
      return myCollidableIndex;
   }
   
   public void setCollidableIndex (int idx) {
      myCollidableIndex = idx;
   }
   
   public PointSkinAttachment createPointAttachment (Point pnt) {
      
      if (!(getMesh() instanceof PolygonalMesh)) {
         return null;
      }
      PolygonalMesh mesh = (PolygonalMesh)getMesh();
      if (!mesh.isTriangular()) {
         return null;
      }
      // Find nearest face to the point; we'll need this to
      // estimate a basePosition for the attachments from the
      // start by find
      BVFeatureQuery query = new BVFeatureQuery();
      Point3d near = new Point3d();
      Vector2d uv = new Vector2d();
      Face face = query.nearestFaceToPoint (near, uv, mesh, pnt.getPosition());

      // Create a new PointSkinAttachment

      MeshDistCalc dcalc = new MeshDistCalc();
      PointSkinAttachment a =
         dcalc.computeDisplacementAttachment(pnt.getPosition(), myLastSigma);
      a.setSkinMesh (this);

      // Now estimate the basePosition from the face vertices
      Point3d basePos = new Point3d();
      Vertex3d[] vtxs = face.getTriVertices();
      double[] wgts = new double[] { 1-uv.x-uv.y, uv.x, uv.y };
      for (int i=0; i<vtxs.length; i++) {
         PointSkinAttachment va =
            (PointSkinAttachment)myVertexAttachments.get(vtxs[i].getIndex());
         basePos.scaledAdd (wgts[i], va.getBasePosition());
      }
      a.setBasePosition (basePos);
      a.setPoint (pnt);
      return a;
   }


}




