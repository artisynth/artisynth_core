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
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;

import artisynth.core.femmodels.FemModel.ElementFilter;
import artisynth.core.femmodels.FemModel.Ranging;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.CollidableBody;
import artisynth.core.mechmodels.CollidableDynamicComponent;
import artisynth.core.mechmodels.CollisionHandler;
import artisynth.core.mechmodels.ContactMaster;
import artisynth.core.mechmodels.DistanceGridComp;
import artisynth.core.mechmodels.DynamicAttachment;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointAttachable;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.PointList;
import artisynth.core.mechmodels.PointParticleAttachment;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentChangeEvent.Code;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ContactPoint;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.modelbase.TransformGeometryAction;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ArtisynthIO;
import artisynth.core.util.IntegerToken;
import artisynth.core.util.ObjectToken;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.Face;
import maspack.geometry.GeometryTransformer;
import maspack.geometry.HalfEdge;
import maspack.geometry.MeshBase;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.Shading;
import maspack.util.ArraySupport;
import maspack.util.DynamicIntArray;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Describes a mesh that is "skinned" onto an FEM, such that its vertex
 * positions are determined by weighted combinations of FEM node positions.
 **/
public class FemMeshComp extends FemMeshBase
implements CollidableBody, PointAttachable {

   FemModel3d myFem;
   protected static double EPS = 1e-10;

   protected static final Vertex3d NO_SINGLE_VERTEX = new Vertex3d();
   protected static final Collidability DEFAULT_COLLIDABILITY =
      Collidability.ALL;   

   /**
    * Container for vertex attachment info so we can update this info
    * atomically.
    */
   protected static class VertexInfo {
      protected ArrayList<PointAttachment> myAttachments = new ArrayList<>();
      // myNodeMap maps each node used by this FemMeshComp onto either
      // a single vertex which it completely controls, or the dummy variable 
      // NO_SINGLE_VERTEX if there is no such vertex.
      protected HashMap<FemNode3d,Vertex3d> myNodeMap = new HashMap<>();
      protected int myNumSingleAttachments;
      // myVertexAdjacencies describe which vertices should be considered
      // adjacent to each other, overiding the adjacency relationships
      // specified by half-edge connectivity. At present, this is only used for
      // computing isSurfaceEdge(), and only in the case where we automatically
      // create a fine surface mesh but want to preserve the connectivity of
      // the original surface mesh,
      protected int[][] myVertexAdjacencies;

      protected VertexInfo() {
      }

      protected void buildNodeMap(MeshBase mesh) {
         myNodeMap = new HashMap<FemNode3d,Vertex3d>();
         myNumSingleAttachments = 0;
         for (int i=0; i<myAttachments.size(); i++) {
            PointAttachment pa = myAttachments.get(i);
            if (pa instanceof PointParticleAttachment) {
               FemNode3d node = 
                  (FemNode3d)((PointParticleAttachment)pa).getParticle();
               myNodeMap.put (node, mesh.getVertex(i));
               myNumSingleAttachments++;
            }
            else if (pa instanceof PointFem3dAttachment) {
               PointFem3dAttachment pfa = (PointFem3dAttachment)pa;
               for (FemNode node : pfa.getNodes()) {
                  if (myNodeMap.get(node) == null) {
                     myNodeMap.put ((FemNode3d)node, NO_SINGLE_VERTEX);
                  }
               }
            }
         }
      }

      protected int[][] getVertexAdjacencies() {
         return myVertexAdjacencies;
      }

      protected void setVertexAdjacencies (int[][] adjacencies) {
         myVertexAdjacencies = adjacencies;
      }

      protected int[][] buildVertexAdjacencies (MeshBase mesh) {
         // Vertex adjacency is stored in an upper triangular sparse format:
         // for each vertex vtx there is an array that stores the indices of
         // all adjacent vertices whose index is higher than that of vtx.
         if (mesh instanceof PolygonalMesh) {
            DynamicIntArray[] adjArrays = new DynamicIntArray[mesh.numVertices()];
            int i = 0;
            for (Vertex3d vtx : mesh.getVertices()) {
               adjArrays[i] = new DynamicIntArray();
               Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
               while (it.hasNext()) {
                  HalfEdge he = it.next();
                  if (he.isPrimary()) {
                     Vertex3d tail = he.getTail();
                     if (tail.getIndex() > vtx.getIndex()) {
                        adjArrays[i].add (tail.getIndex());
                     }
                     else {
                        adjArrays[tail.getIndex()].add (i);
                     }
                  }
               }
               i++;
            }
            int[][] adjacencies = new int[mesh.numVertices()][];
            for (i=0; i<mesh.numVertices(); i++) {
               adjacencies[i] = adjArrays[i].getArray();
            }
            return adjacencies;
         }
         else {
            return null;
         }
      }

      protected Vertex3d getVertexForNode (FemNode3d node) {
         if (myNodeMap == null) {
            return null;
         }
         Vertex3d vtx = myNodeMap.get (node);
         if (vtx == NO_SINGLE_VERTEX) {
            vtx = null;
         }
         return vtx;
      }

      private boolean contains (int[] array, int idx) {
         for (int i=0; i<array.length; i++) {
            if (array[i] == idx) {
               return true;
            }
         }
         return false;
      }

      public boolean isSurfaceEdge (FemNode3d node0, FemNode3d node1) {
         Vertex3d vtx0 = getVertexForNode (node0);
         Vertex3d vtx1 = getVertexForNode (node1);
         if (vtx0 == null || vtx1 == null) {
            return false;
         }
         if (myVertexAdjacencies != null) {
            int vidx0 = vtx0.getIndex();
            int vidx1 = vtx1.getIndex();
            // make sure vertices are within range of the adjacency info
            if (vidx0 < myVertexAdjacencies.length &&
                vidx1 < myVertexAdjacencies.length) {
               if (vidx0 < vidx1) {
                  return contains (myVertexAdjacencies[vidx0], vidx1);
               }
               else {
                  return contains (myVertexAdjacencies[vidx1], vidx0);
               }
            }
         }
         // default: use half-edge connectivity
         return (vtx0.findIncidentHalfEdge(vtx1) != null ||
                 vtx1.findIncidentHalfEdge(vtx0) != null);
      }

      public boolean containsContactMaster (CollidableDynamicComponent comp) {
         if (myNodeMap != null && comp instanceof FemNode3d &&
             myNodeMap.get((FemNode3d)comp) != null) {
            return true;
         }
         else {
            return false;
         }
      }

      protected VertexInfo copy (
         int flags, MeshBase mesh, Map<ModelComponent,ModelComponent> copyMap) {

         VertexInfo info = new VertexInfo();
         info.myAttachments.ensureCapacity (myAttachments.size());
         for (PointAttachment pa : myAttachments) {
            PointAttachment newPa = pa.copy(flags, copyMap);
            info.myAttachments.add(newPa);
         }
         if (myVertexAdjacencies != null) {
            info.myVertexAdjacencies = new int[myVertexAdjacencies.length][];
            for (int i=0; i<myVertexAdjacencies.length; i++) {
               int[] vadj = myVertexAdjacencies[i];
               if (vadj != null) {
                  info.myVertexAdjacencies[i] = Arrays.copyOf (vadj, vadj.length);
               }
            }
         }
         info.buildNodeMap(mesh);
         return info;
      }
   }

   protected VertexInfo myVertexInfo;

   private boolean isSurfaceMesh;
   private boolean isGeneratedSurface;

   protected Collidability myCollidability = DEFAULT_COLLIDABILITY;
   protected int myCollidableIndex;

   private float[] colorArray = new float[3];
   private boolean myMeshScannedDirectly = false;

   public static PropertyList myProps =
      new PropertyList (FemMeshComp.class, FemMeshBase.class);

   static {
      myProps.add (
         "collidable", 
         "sets the collidability of the mesh", DEFAULT_COLLIDABILITY);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FemMeshComp () {
      super();
      myVertexInfo = new VertexInfo();
   }

   public FemMeshComp (FemModel3d fem) {
      this();
      myFem = fem;
      isSurfaceMesh = false;
      isGeneratedSurface = false;
   }

   public FemMeshComp(FemModel3d fem, String name) {
      this(fem);
      setName(name);
   }
   
   @Override
   public void setSurfaceRendering(SurfaceRender mode) {
      SurfaceRender oldMode = getSurfaceRendering();
      super.setSurfaceRendering(mode);

      if (oldMode != mode) {
         if (myFem != null) { // paranoid: myFem should always be non-null here
            if (!isScanning()) {
               myFem.updateInternalNodalStressSettings();
               myFem.updateInternalNodalStrainSettings();
               if (mode.usesStressOrStrain()) {
                  myFem.updateStressAndStiffness();
               }
            }
         }
         // save/restore original vertex colors
         MeshBase mesh = getMesh();   
         if (mesh != null) {
            boolean oldStressOrStrain = oldMode.usesStressOrStrain();
            boolean newStressOrStrain = mode.usesStressOrStrain();

            if (newStressOrStrain != oldStressOrStrain) {
               if (newStressOrStrain) {
                  saveShading();
                  saveMeshColoring (mesh);
                  mesh.setVertexColoringEnabled();
                  mesh.setVertexColorMixing (ColorMixing.REPLACE);
                  myRenderProps.setShading (Shading.NONE);
                  // enable stress/strain rendering *after* vertex coloring set
                  updateVertexColors(); // not sure we need this here
               }
               else {
                  // disable stress/strain rendering *before* restoring colors
                  restoreMeshColoring (mesh);
                  restoreShading();
               }
            }
         }
      }
   }

   @Override
   public void render(Renderer renderer, int flags) {
      if (myFem != null && myFem.isSelected() ) {
         flags |= Renderer.HIGHLIGHT;
      }
      super.render(renderer, flags);
   }

   public int numVertexAttachments () {
      return myVertexInfo.myAttachments.size();
   }

   public PointAttachment getVertexAttachment(Vertex3d vtx) {
      return getVertexAttachment (vtx.getIndex());
   }

   public PointAttachment getVertexAttachment (int idx) {
      return myVertexInfo.myAttachments.get (idx);
   }

   /** 
    * Initialize data structures prior to adding vertices and faces to
    * this surface.
    */
   protected void initializeSurfaceBuild() {
      super.initializeSurfaceBuild();
   }

   /** 
    * Finalize data structures after vertices and faces have been added.
    */
   protected void finalizeSurfaceBuild() {
      super.finalizeSurfaceBuild();

      notifyParentOfChange (
         new ComponentChangeEvent (Code.STRUCTURE_CHANGED, this));

      // save render info in case render gets called immediately
      myMeshInfo.getMesh ().saveRenderInfo (myRenderProps);
   }

   private static class EdgeDesc {

      Vertex3d myVtx0;
      Vertex3d myVtx1;

      EdgeDesc (Vertex3d vtx0, Vertex3d vtx1) {
         myVtx0 = vtx0;
         myVtx1 = vtx1;
      }

      public int hashCode() {
         return myVtx0.hashCode()/2 + myVtx1.hashCode()/2;
      }

      public boolean equals (Object obj) {
         if (obj instanceof EdgeDesc) {
            EdgeDesc desc = (EdgeDesc)obj;
            return ((desc.myVtx0 == myVtx0 && desc.myVtx1 == myVtx1) ||
               (desc.myVtx1 == myVtx0 && desc.myVtx0 == myVtx1));
         }
         else {
            return false;
         }
      }

      public EdgeDesc copy(Map<Vertex3d,Vertex3d> copyMap) {

         if (copyMap == null) {
            return new EdgeDesc(myVtx0, myVtx1);
         }

         Vertex3d v0Ref = null;
         Vertex3d v1Ref = null;

         if (myVtx0 != null) {
            v0Ref = copyMap.get(myVtx0);
            if (v0Ref == null) {
               v0Ref = myVtx0;
            }
         }
         if (myVtx1 != null) {
            v1Ref = copyMap.get(myVtx1);
            if (v1Ref == null) {
               v1Ref = myVtx1;
            }
         }

         return new EdgeDesc(v0Ref, v1Ref);
      }
   }

   protected FemNode3d getEdgeNode (HalfEdge he) {
      return getNodeForVertex (he.getHead());
   }


   /**
    * Returns <code>true</code> if every vertex in this mesh is
    * directly attached to a single FEM node. If this is the case, then
    * {@link #getNodeForVertex} and {@link #getVertexForNode} will never return
    * <code>null</code>.
    * 
    * @return <code>true</code> if every vertex is attached
    * a single node
    */
   public boolean isSingleNodeMapped() {
      return myVertexInfo.myNumSingleAttachments == getMesh().numVertices();
   }

   /**
    * Returns the node to which a specified vertex is directly attached,
    * or <code>null</code> if no such node exists. In particular,
    * if the specified vertex is controlled by <i>multiple</i> nodes,
    * then this method will return <code>null</code>.
    * 
    * @param vtx vertex being queried
    * @return node to which <code>vtx</code> is directly attached, or
    * <code>null</code>.
    */
   public FemNode3d getNodeForVertex (Vertex3d vtx) {
      int idx = vtx.getIndex();
      ArrayList<PointAttachment> attachments = myVertexInfo.myAttachments;
      if (idx < attachments.size()) {
         return getNodeForVertex (attachments.get(idx));
      }
      else {
         return null;
      }
   }

   private FemNode3d getNodeForVertex (PointAttachment pa) {
      if (pa instanceof PointParticleAttachment) {
         Particle p = ((PointParticleAttachment)pa).getParticle();
         if (p instanceof FemNode3d) {
            FemNode3d node = (FemNode3d)p;
            if (node.getGrandParent() == myFem) {
               return node;
            }
         }
      }
      return null;     
   }

   /**
    * Returns a vertex which is directly attached to the specified
    * node, or <code>null</code> if no such vertex exists.
    * 
    * @param node node whose vertex is being queried
    * @return vertex directly attached to the node, or <code>null</code>.
    */
   public Vertex3d getVertexForNode (FemNode3d node) {
      return myVertexInfo.getVertexForNode (node);
   }

   /**
    * If {@code nodeTail} and {@code nodeHead} are directly attached to the
    * tail and head vertices of a half-edge on this mesh, return the
    * half-edge. Otherwise, return {@code null}.
    * 
    * @param nodeTail node attached to the tail vertex
    * @param nodeHead node attached to the head vertex
    * @return half-edge connecting nodeTail to nodeHead
    */
   public HalfEdge getHalfEdgeForNodes (FemNode3d nodeTail, FemNode3d nodeHead) {
     Vertex3d vtxTail = myVertexInfo.getVertexForNode (nodeTail);
     Vertex3d vtxHead = myVertexInfo.getVertexForNode (nodeHead);
     if (vtxTail != null && vtxHead != null) {
        return vtxHead.findIncidentHalfEdge (vtxTail);
     }
     else {
        return vtxHead.findIncidentHalfEdge (vtxTail);
     }
   }

   /**
    * If {@code node0} and {@code node1} are directly attached to vertices of
    * the mesh, return {@code true} if they are connected by a half-edge.
    * 
    * @param node0 first node to query
    * @param node1 second node to query
    * @return {@code true} if nodes are attached to a half-edge on the mesh
    */
   public boolean isSurfaceEdge (FemNode3d node0, FemNode3d node1) {
      return myVertexInfo.isSurfaceEdge (node0, node1);
   }

   private void addVertexNodes (HashSet<FemNode3d> nodes, PointAttachment pa) {
      if (pa instanceof PointFem3dAttachment) {
         PointFem3dAttachment pfa = (PointFem3dAttachment)pa;
         FemNode[] masters = pfa.getNodes();
         for (int j=0; j<masters.length; j++) {
            nodes.add ((FemNode3d)masters[j]);
         }
      }
      else {
         PointParticleAttachment ppa = (PointParticleAttachment)pa;
         nodes.add ((FemNode3d)ppa.getParticle());
      }      
   }

   private void addVertexNodes (HashSet<FemNode3d> nodes, Vertex3d vtx) {
      addVertexNodes (nodes, getVertexAttachment(vtx.getIndex()));
   }

   protected ArrayList<FemElement3d> getAdjacentVolumetricElems (
      FemNode3d node) {
      ArrayList<FemElement3d> deps = new ArrayList<FemElement3d>();
      for (FemElement3dBase eb : node.getElementDependencies()) {
         if (eb instanceof FemElement3d) {
            deps.add ((FemElement3d)eb);
         }
      }
      return deps;
   }

   public FemElement3dBase getFaceElement (Face face) {

      // Form the intersection of all the element dependencies of all three
      // nodes associated with the face. If the face is on the surface,
      // there should be only one element left.
      HashSet<FemNode3d> nodes = new HashSet<FemNode3d>();
      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he = he0;
      do {
         addVertexNodes (nodes, he.getHead());
         he = he.getNext();
      }
      while (he != he0);

      HashSet<FemElement3dBase> elems = null;
      // check volumetric elements first
      for (FemNode3d node : nodes) {
         if (elems == null) {
            elems =
               new HashSet<FemElement3dBase>(node.getAdjacentVolumeElements());
         }
         else {
            elems.retainAll (node.getAdjacentVolumeElements());
         }
      }
      if (elems.size() != 1) {
         // check shell elements next
         elems = null;
         for (FemNode3d node : nodes) {
            if (elems == null) {
               elems =
                  new HashSet<FemElement3dBase>(node.getAdjacentShellElements());
            }
            else {
               elems.retainAll (node.getAdjacentShellElements());
            }
         }
      }
      if (elems.size() != 1) {
         FemElement3d[] elemArray = elems.toArray(new FemElement3d[0]);
         int[] idxs = face.getVertexIndices();
         System.out.print (
            "Error in FemMeshComp.getFaceElement(): " +
            " couldn't isolate element for face [ ");
         for (int i=0; i<idxs.length; i++) {
            System.out.print ("" + idxs[i]+" ");
         }
         System.out.println ("]");
         System.out.println ("Candidate elements are:");
         for (int i=0; i<elemArray.length; i++) {
            System.out.println (" element "+elemArray[i].getNumber());
         }
         return null;
         // ignore for now ...
         // throw new InternalErrorException (
         //    "Face "+face+" associated with "+elems.size()+" elements");
      }
      return (FemElement3dBase)elems.toArray()[0];
   }

   private Vertex3d createVertex (
      ArrayList<PointAttachment> vtxAttachments, FemNode3d node, Vertex3d v) {
      Vertex3d vtx = new Vertex3d (v.getPosition());
      PointParticleAttachment attacher =
         new PointParticleAttachment (node, null);
      vtxAttachments.add (attacher);
      getMesh().addVertex (vtx);
      return vtx;
   }

   private void addNodeCoords (
      Vector3d coords, double s0, double s1, double s2, FemElement3dBase elem, 
      FemNode3d n0, FemNode3d n1, FemNode3d n2) {

      Vector3d coordsv = new Vector3d();
      int lidx = elem.getLocalNodeIndex (n0);
      if (elem instanceof QuadpyramidElement && lidx == 4) {
         // special handling required for the apex of a QuadtetPyramid, since
         // the apex is formed by condensing the corner nodes of a brick. That
         // means natural coordinates are actually in a rectangular brick
         // coordinate system.

         // get the coordinates for the top brick node corresponding to v1,
         // and find the weight for it from s0, s1, and s2.
         elem.getNodeCoords (coordsv, elem.getLocalNodeIndex (n1));
         coordsv.z = 1;
         coords.scaledAdd (s0*s1/(s1+s2), coordsv);

         if (n2 != null) {
            // get the coordinates for the top brick node corresponding to v2,
            // and fine the weight for it as well.
            elem.getNodeCoords (coordsv, elem.getLocalNodeIndex (n2));
            coordsv.z = 1;
            coords.scaledAdd (s0*s2/(s1+s2), coordsv);         
         }
      }
      else {
         elem.getNodeCoords (coordsv, lidx);
         coords.scaledAdd (s0, coordsv);
      }
   }

   private Vertex3d createVertex (
      ArrayList<PointAttachment> vtxAttachments,
      double s0, double s1, double s2, FemElement3dBase elem, 
      FemNode3d n0, FemNode3d n1, FemNode3d n2) {

      Vector3d coords = new Vector3d();

      addNodeCoords (coords, s0, s1, s2, elem, n0, n1, n2);
      addNodeCoords (coords, s1, s0, s2, elem, n1, n0, n2);

      if (n2 != null) {
         addNodeCoords (coords, s2, s1, s0, elem, n2, n1, n0);
      }

      ArrayList<FemNode> nodes = new ArrayList<FemNode>();
      VectorNd weights = new VectorNd();

      Point3d pos = new Point3d();
      for (int i=0; i<elem.numNodes(); i++) {
         double w = elem.getN (i, coords);
         if (Math.abs(w) > EPS) {
            FemNode3d n = elem.getNodes()[i];
            nodes.add (n);
            weights.append (w);
            pos.scaledAdd (w, n.getPosition());
         }
      }

      Vertex3d vtx = new Vertex3d (pos);
      PointFem3dAttachment attacher = new PointFem3dAttachment();
      attacher.setFromNodes (nodes, weights);
      vtxAttachments.add (attacher);

      getMesh().addVertex (vtx);
      return vtx;
   }

   private Vertex3d[] collectEdgeVertices (
      ArrayList<PointAttachment> vtxAttachments,
      HashMap<EdgeDesc,Vertex3d[]> edgeVtxMap, Vertex3d v0, Vertex3d v1, 
      FemNode3d n0, FemNode3d n1, FemElement3dBase elem, int res) {

      int numv = res+1;
      // numv is total number of vertices along the edge, including
      // the end vertices. The number of intermediate vertices is numv-2
      Vertex3d[] vtxs = edgeVtxMap.get (new EdgeDesc (v0, v1));
      if (vtxs == null) {
         vtxs = new Vertex3d[numv];
         vtxs[0] = v0;
         vtxs[numv-1] = v1;
         // create vertices along edge
         for (int i=1; i<numv-1; i++) {
            double s1 = i/(double)res;
            vtxs[i] = createVertex (
               vtxAttachments, 1-s1, s1, 0, elem, n0, n1, null);
         }
         edgeVtxMap.put (new EdgeDesc (v0, v1), vtxs);
         return vtxs;
      }
      else if (vtxs[0] == v0) {
         return vtxs;
      }
      else {
         // vertices in the wrong order; return a reversed list of them.
         Vertex3d[] vtxsRev = new Vertex3d[numv];
         for (int i=0; i<numv; i++) {
            vtxsRev[i] = vtxs[numv-1-i];
         }
         return vtxsRev;
      }
   }

   public static FemMeshComp createEmbedded (
      FemMeshComp surf, MeshBase mesh) {
      if (surf == null || surf.myFem == null) {
         throw new IllegalArgumentException("Cannot determine proper FEM");
      }
      return createEmbedded (surf, mesh, surf.myFem);
   }

   public FemModel3d getFem() {
      return myFem;
   }

   public void addVertexAttachment (PointAttachment attachment) {
      setVertexAttachment (
         myVertexInfo.myAttachments.size(), attachment);
   }

   // XXX Sanchez: needed when attachment info already known and want to
   //              construct manually (more efficient).  Currently used
   //              in in-progess hex-mesher
   public void setVertexAttachment(int vidx, PointAttachment attachment) {
      // update vertex attachment size
      ArrayList<PointAttachment> vtxAttachments = myVertexInfo.myAttachments;
      if (vidx == vtxAttachments.size()) {
         vtxAttachments.add(attachment);
      } else if (vidx < vtxAttachments.size()) {
         vtxAttachments.set(vidx, attachment);   
      } else {
         // add null attachments
         for (int i=vtxAttachments.size(); i<vidx; i++) {
            vtxAttachments.add(null);
         }
         vtxAttachments.add(attachment);
      }
   }

   public void setVertexAttachment (
      int vidx, double [] weights, FemNode3d[] nodes) {
      if (weights.length > 1) {
         PointFem3dAttachment pattacher = new PointFem3dAttachment();
         pattacher.setFromNodes (nodes, weights);
         setVertexAttachment(vidx, pattacher);
      } else if (weights.length == 1) {
         PointParticleAttachment attacher = 
            new PointParticleAttachment(nodes[0], null);
         setVertexAttachment(vidx, attacher);
      }
   }

   public static FemMeshComp createEmbedded (
      FemMeshComp surf, MeshBase mesh, FemModel3d fem) {

      if (surf == null) {
         surf = new FemMeshComp(fem);
      }
      surf.setMesh (mesh);
      surf.createEmbeddingAttachments();
      return surf;
   }

   public void createEmbeddingAttachments () {

      ArrayList<FemNode> nodes = new ArrayList<FemNode>();
      VectorNd weights = new VectorNd();
      double reduceTol = 1e-8;

      ArrayList<Vertex3d> verts = getMesh().getVertices();
      VertexInfo info = new VertexInfo();
      for (int i=0; i<verts.size(); i++) {
         // this code works very similarly to the code that adds
         // marker points into a mesh
         Vertex3d vtx = verts.get(i);
         FemElement3dBase elem = myFem.findContainingElement (vtx.pnt);
         Point3d newLoc = new Point3d(vtx.pnt);
         if (elem == null) {
            // won't use newLoc since we're not projecting vertex onto FEM
            elem = myFem.findNearestSurfaceElement (newLoc, vtx.pnt);
         }
         VectorNd coords = new VectorNd (elem.numNodes());

         // first see if there's a node within reduceTol of the point, and if
         // so just use that.
         double maxd = Double.NEGATIVE_INFINITY;
         double mind = Double.POSITIVE_INFINITY;
         FemNode3d nearestNode = null;
         FemNode3d[] elemNodes = elem.getNodes();
         for (int k=0; k<elemNodes.length; k++) {
            double d = vtx.pnt.distance(elemNodes[k].getPosition());
            if (d > maxd) {
               maxd = d;
            }
            if (d < mind) {
               mind = d;
               nearestNode = elemNodes[k];
            }
         }
         if (mind/maxd <= reduceTol) {
            // weight everything to the nearest node
            nodes.clear();
            nodes.add(nearestNode);
            weights.setSize(0);
            weights.append(1.0);
         }
         else {
            Vector3d c3 = new Vector3d();
            boolean converged = 
               elem.getNaturalCoordinates (c3, vtx.pnt, 1000) >= 0;
            if (!converged) {
               System.err.println(
                  "Warning: getNaturalCoordinates() did not converge, "+
                  "element=" + ComponentUtils.getPathName(elem) +
                  ", point=" + vtx.pnt);
               c3.setZero ();
               // elem.getNaturalCoordinatesGSS (coords, vtx.pnt, 1000);
               // c3.setZero();
               // XXX debugging:
               //    elem.getNaturalCoordinates(c3,  vtx.pnt, 1000); // try again once more
            }
            for (int j=0; j<elem.numNodes(); j++) {
               coords.set (j, elem.getN (j, c3));
            }

            nodes.clear();
            weights.setSize(0);
            for (int k=0; k<coords.size(); k++) {
               if (Math.abs(coords.get(k)) >= reduceTol) {
                  nodes.add (elem.getNodes()[k]);
                  weights.append(coords.get(k));                            
               }
            }
         }

         if (weights.size() > 1) {
            PointFem3dAttachment attacher = new PointFem3dAttachment();
            attacher.setFromNodes (nodes, weights);
            info.myAttachments.add (attacher);
         } else if (weights.size() == 1) {
            PointParticleAttachment attacher =
               new PointParticleAttachment(nodes.get(0), null);
            info.myAttachments.add(attacher);
         }
      }
      info.buildNodeMap (getMesh());
      myVertexInfo = info;
   }

   public static FemMeshComp createEmbedded (
      FemModel3d fem, MeshBase mesh) {
      return createEmbedded (new FemMeshComp(fem), mesh);
   }

   public static FemMeshComp createNodalEmbedded (
      FemMeshComp surf, MeshBase mesh, FemModel3d fem) {
      if (surf == null) {
         surf = new FemMeshComp(fem);
      }
      surf.setMesh (mesh);
      VertexInfo info = new VertexInfo();
      for (Vertex3d vtx : mesh.getVertices()) {
         // attach vertex to nearest node
         FemNode3d node = surf.myFem.findNearestNode (vtx.pnt, 1e-8);
         if (node == null) {
            System.out.println ("no node found for vertex " + vtx.getIndex());
         }
         else {
            PointParticleAttachment attacher =
               new PointParticleAttachment(node, null);
            info.myAttachments.add(attacher);
         }
         if ((vtx.getIndex() % 1000) == 0) {
            System.out.println ("vertex " + vtx.getIndex());
         }
      }
      info.buildNodeMap (surf.getMesh());
      surf.myVertexInfo = info;
      return surf;
   }

   public static FemMeshComp createNodalEmbedded (FemModel3d fem, MeshBase mesh) {
      return createNodalEmbedded (null, mesh, fem);
   }

   public static FemMeshComp createSurface (
      String name, FemModel3d fem, ElementFilter efilter) {
      if (fem.numElements() > 0) {
         return createVolumetricSurface (name, fem, efilter);
      }
      else{
         return createShellSurface (name, fem, efilter);
      }
   }

   public static FemMeshComp createSurface (String name, FemModel3d fem) {
      return createSurface (name, fem, e -> true);
   }

   public static FemMeshComp createVolumetricSurface (
      String name, FemModel3d fem, Collection<FemElement3d> elems) {
      FemMeshComp femMesh = new FemMeshComp(fem);
      femMesh.setName (name);
      femMesh.createVolumetricSurface (elems);
      return femMesh; 
   }

   public static FemMeshComp createVolumetricSurface (
      String name, FemModel3d fem, ElementFilter efilter) {
      return createVolumetricSurface (
         name, fem, getFilteredElements (fem.getElements(), efilter));
   }

   public static FemMeshComp createVolumetricSurface (
      String name, FemModel3d fem) {
      return createVolumetricSurface (name, fem, fem.getElements());
   }

   public static FemMeshComp createShellSurface (
      String name, FemModel3d fem, Collection<ShellElement3d> elems) {
      FemMeshComp femMesh = new FemMeshComp(fem);
      femMesh.setName (name);
      femMesh.createShellSurface (elems);
      return femMesh; 
   }

   public static FemMeshComp createShellSurface (
      String name, FemModel3d fem, ElementFilter efilter) {
      return createShellSurface (
         name, fem, getFilteredElements (fem.getShellElements(), efilter));
   }

   public static FemMeshComp createShellSurface (String name, FemModel3d fem) {
      return createShellSurface (name, fem, fem.getShellElements());
   }

   protected void createFineSurface (int resolution, ElementFilter efilter) {

      // build from nodes/element filter
      createSurface(efilter);
      isGeneratedSurface = true;

      if (resolution < 2) {
         // if resolution < 2, just return regular surface
         return;
      }

      // Note: can no longer rely on the surface mesh consisting of only 
      // FemMeshVertex
      PolygonalMesh baseMesh = (PolygonalMesh)getMesh(); // since previously built
      ArrayList<Face> baseFaces = baseMesh.getFaces();
      ArrayList<Vertex3d> baseVertices = baseMesh.getVertices();
      // create vertex adjacencies to pass on for refined mesh
      int[][] vertexAdjacencies = myVertexInfo.buildVertexAdjacencies(baseMesh);

      int numv = resolution+1; // num vertices along the edge of each sub face
      initializeSurfaceBuild();

      //ArrayList<PointAttachment> baseAttachments = info.myAttachments;
      ArrayList<PointAttachment> baseAttachments = myVertexInfo.myAttachments;
      VertexInfo info = new VertexInfo();

      HashMap<EdgeDesc,Vertex3d[]> edgeVtxMap =
         new HashMap<EdgeDesc,Vertex3d[]>();

      // get newly empty mesh
      PolygonalMesh surfMesh = (PolygonalMesh)getMesh();

      for (Vertex3d vtx : baseVertices) {
         FemNode3d node =
            getNodeForVertex (baseAttachments.get(vtx.getIndex()));
         createVertex (info.myAttachments, node, vtx);
      }
      // System.out.println ("num base faces: " + baseFaces.size());
      for (int k=0; k<baseFaces.size(); k++) {
         Face face = baseFaces.get(k);
         // store sub vertices for the face in the upper triangular half of
         // subv.
         MeshFactory.VertexSet subv = new MeshFactory.VertexSet (numv);

         FemElement3dBase elem = getFaceElement (face);
         if (elem == null) {
            continue;
         }

         HalfEdge he = face.firstHalfEdge();
         Vertex3d v0 = (Vertex3d)he.getHead();
         FemNode3d n0 = getNodeForVertex(baseAttachments.get(v0.getIndex()));
         he = he.getNext();
         Vertex3d v1 = (Vertex3d)he.getHead();
         FemNode3d n1 = getNodeForVertex(baseAttachments.get(v1.getIndex()));
         he = he.getNext();
         Vertex3d v2 = (Vertex3d)he.getHead();
         FemNode3d n2 = getNodeForVertex(baseAttachments.get(v2.getIndex()));

         subv.set (0, 0, getVertex(v0.getIndex()));
         subv.set (0, numv-1, getVertex(v2.getIndex()));
         subv.set (numv-1, numv-1, getVertex(v1.getIndex()));

         Vertex3d[] vtxs01 = collectEdgeVertices (
            info.myAttachments,edgeVtxMap, v0, v1, n0, n1, elem, resolution);
         for (int i=1; i<numv-1; i++) {
            subv.set (i, i, vtxs01[i]);
         }
         Vertex3d[] vtxs02 = collectEdgeVertices (
            info.myAttachments, edgeVtxMap, v0, v2, n0, n2, elem, resolution);
         for (int j=1; j<numv-1; j++) {
            subv.set (0, j, vtxs02[j]);
         }
         Vertex3d[] vtxs21 = collectEdgeVertices (
            info.myAttachments, edgeVtxMap, v2, v1, n2, n1, elem, resolution);
         for (int i=1; i<numv-1; i++) {
            subv.set (i, numv-1, vtxs21[i]);
         }

         for (int i=1; i<numv-1; i++) {
            for (int j=i+1; j<numv-1; j++) {
               double s1 = i/(double)resolution;
               double s0 = 1-j/(double)resolution;
               Vertex3d vtx = createVertex (
                  info.myAttachments, s0, s1, 1-s0-s1, elem, n0, n1, n2);
               subv.set (i, j, vtx);
            }
         }

         subv.check();
         for (int i=0; i<resolution; i++) {
            for (int j=i; j<resolution; j++) {
               surfMesh.addFace (
                  subv.get(i,j), subv.get(i+1,j+1), subv.get(i,j+1));
               if (i != j) {
                  surfMesh.addFace (
                     subv.get(i,j), subv.get(i+1,j), subv.get(i+1,j+1));
               }
            }
         }
      }
      info.buildNodeMap (getMesh());
      myVertexInfo = info;
      myVertexInfo.setVertexAdjacencies (vertexAdjacencies);
      finalizeSurfaceBuild();
   }

   private ArrayList<FemNode3d> getEdgeNodes(
      FemNode3d n0, FemNode3d n1, FemModel3d fem) {

      LinkedList<DynamicAttachment> masters = n0.getMasterAttachments();
      if (masters == null) {
         return null;
      }
      ArrayList<FemNode3d> nodes = new ArrayList<FemNode3d>();
      ArrayList<Double> weights = new ArrayList<Double>();
      for (DynamicAttachment a : masters) {
         if (a instanceof PointFem3dAttachment) {
            PointFem3dAttachment pfa = (PointFem3dAttachment)a;
            if (pfa.numMasters() == 2 && pfa.getSlave() instanceof FemNode3d) {
               FemNode3d slaveNode = (FemNode3d)pfa.getSlave();
               if (slaveNode.getGrandParent() == fem &&
                  FemModel3d.containsNode(n1, pfa.getNodes())) {
                  nodes.add(slaveNode);
                  double w = pfa.getCoordinates().get(0);
                  if (n0 == pfa.getNodes()[0]) {
                     weights.add(w);
                  }
                  else {
                     weights.add(1 - w);
                  }
               }
            }
         }
      }
      // just do a bubble sort; easier to implement
      for (int i = 0; i < nodes.size() - 1; i++) {
         for (int j = i + 1; j < nodes.size(); j++) {
            if (weights.get(j) < weights.get(i)) {
               double w = weights.get(i);
               weights.set(i, weights.get(j));
               weights.set(j, w);
               FemNode3d n = nodes.get(i);
               nodes.set(i, nodes.get(j));
               nodes.set(j, n);
            }
         }
      }
      return nodes;
   }

   private void addEdgeNodesToFace (
      FaceNodes3d face, FemModel3d fem) {

      LinkedList<FemNode3d> allNodes = new LinkedList<FemNode3d>();
      FemNode3d[] nodes = face.getNodes();
      for (int i = 0; i < nodes.length; i++) {
         FemNode3d n0 = nodes[i];
         FemNode3d n1 = (i < nodes.length - 1) ? nodes[i + 1] : nodes[0];
         ArrayList<FemNode3d> edgeNodes = getEdgeNodes (n0, n1, fem);
         allNodes.add(n0);
         if (edgeNodes != null) {
            allNodes.addAll(edgeNodes);
         }
      }
      face.setAllNodes(allNodes.toArray(new FemNode3d[0]));
   }

   private static <E extends FemElement3dBase> ArrayList<E> getFilteredElements (
      FemElement3dList<E> elems, ElementFilter efilter) {
      ArrayList<E> filtered = new ArrayList<E>(elems.size());
      // create a list of all the faces for all the elements, and for
      // each node, create a list of all the faces it is associated with
      for (E e : elems) {
         if (efilter.elementIsValid(e)) {
            filtered.add (e);
         }
      }
      return filtered;
   }

   public void createSurface (ElementFilter efilter) {

      if (myFem.numElements() > 0) {
         createVolumetricSurface (
            getFilteredElements (myFem.getElements(), efilter));
         // createVolumetricShellSurface (
         //    getFilteredElements (myFem.getElements(), efilter),
         //    getFilteredElements (myFem.getShellElements(), efilter));
      }
      else {
         createShellSurface (
            getFilteredElements (myFem.getShellElements(), efilter));
      }
   }

   //Throwable throwable = null;

   public void createVolumetricSurface (Collection<FemElement3d> elems) {

      initializeSurfaceBuild();
      // nodeVertexMap is used during the construction of this surface,
      // so we build it during the construction rather then letting
      // it be built in finalizeSurfaceBuild()
      VertexInfo info = new VertexInfo();

      // faces nodes adjacent to each node
      ArrayList<LinkedList<FaceNodes3d>> faceNodesPerNode =
         new ArrayList<LinkedList<FaceNodes3d>>(myFem.numNodes());
      for (int i = 0; i < myFem.numNodes(); i++) {
         faceNodesPerNode.add(new LinkedList<FaceNodes3d>());
      }
      LinkedList<FaceNodes3d> faceNodes = createFaceNodes (
         faceNodesPerNode, elems, myFem);

      markOverlappingFaces (faceNodes, faceNodesPerNode, myFem);
      faceNodes = removeOverlappingFaces (faceNodes);
      createMeshFromFaceNodes (info, (PolygonalMesh)getMesh(), faceNodes);

      info.buildNodeMap (getMesh());
      myVertexInfo = info;
      finalizeSurfaceBuild();
      isGeneratedSurface = true;
   }

   public void createShellSurface (Collection<ShellElement3d> elems) {

      initializeSurfaceBuild();
      // nodeVertexMap is used during the construction of this surface,
      // so we build it during the construction rather then letting
      // it be built in finalizeSurfaceBuild()
      VertexInfo info = new VertexInfo();

      LinkedList<FaceNodes3d> faceNodes = createFaceNodes (null, elems, myFem);
      createMeshFromFaceNodes (info, (PolygonalMesh)getMesh(), faceNodes);
      info.buildNodeMap (getMesh());
      myVertexInfo = info;
      finalizeSurfaceBuild();
      isGeneratedSurface = true;
   }

   public void createVolumetricShellSurface (
      Collection<FemElement3d> volElems, Collection<ShellElement3d> shellElems) {

      initializeSurfaceBuild();
      // nodeVertexMap is used during the construction of this surface,
      // so we build it during the construction rather then letting
      // it be built in finalizeSurfaceBuild()
      VertexInfo info = new VertexInfo();

      // faces nodes adjacent to each node
      ArrayList<LinkedList<FaceNodes3d>> faceNodesPerNode =
         new ArrayList<LinkedList<FaceNodes3d>>(myFem.numNodes());
      for (int i = 0; i < myFem.numNodes(); i++) {
         faceNodesPerNode.add(new LinkedList<FaceNodes3d>());
      }
      LinkedList<FaceNodes3d> faceNodes = createFaceNodes (
         faceNodesPerNode, volElems, myFem);

      markOverlappingFaces (faceNodes, faceNodesPerNode, myFem);
      faceNodes = removeOverlappingFaces (faceNodes);
      faceNodes.addAll (createFaceNodes (null, shellElems, myFem));
      createMeshFromFaceNodes (info, (PolygonalMesh)getMesh(), faceNodes);

      info.buildNodeMap (getMesh());
      myVertexInfo = info;
      finalizeSurfaceBuild();
      isGeneratedSurface = true;
   }

   // create a list of all faces for a collection of elements. If
   // faceNodesPerNode is not null, use it to record all the faces associated
   // with each node.
   LinkedList<FaceNodes3d> createFaceNodes (
      ArrayList<LinkedList<FaceNodes3d>> faceNodesPerNode,
      Collection<? extends FemElement3dBase> elems, FemModel3d fem) {

      LinkedList<FaceNodes3d> faceNodes = new LinkedList<FaceNodes3d>();
      PointList<FemNode3d> femNodes = fem.getNodes();

      // create a list of all the faces for all the elements, and for
      // each node, create a list of all the faces it is associated with
      for (FemElement3dBase e : elems) {
         FaceNodes3d[] faces = e.getFaces();
         for (FaceNodes3d f : faces) {
            addEdgeNodesToFace (f, myFem);
            for (FemNode3d n : f.getAllNodes()) {
               int idx = femNodes.indexOf(n);
               if (idx == -1) {
                  throw new InternalErrorException(
                     "Element " + e.getNumber() + ": bad node "
                        + n.getNumber());
               }
               if (faceNodesPerNode != null) {
                  faceNodesPerNode.get(femNodes.indexOf(n)).add(f);
               }
            }
            faceNodes.add(f);
         }
      }
      return faceNodes;
   }

   void markOverlappingFaces (
      LinkedList<FaceNodes3d> faceNodes,
      ArrayList<LinkedList<FaceNodes3d>> faceNodesPerNode,
      FemModel3d fem) {
      
      PointList<FemNode3d> femNodes = fem.getNodes();      
      HashSet<FaceNodes3d> adjacentFaces = new HashSet<FaceNodes3d>();
      for (FaceNodes3d f : faceNodes) {
         if (!f.isHidden()) {
            adjacentFaces.clear();
            for (FemNode3d n : f.getAllNodes()) {
               Iterator<FaceNodes3d> it =
                  faceNodesPerNode.get(femNodes.indexOf(n)).iterator();
               while (it.hasNext()) {
                  FaceNodes3d g = it.next();
                  if (g.isHidden()) {
                     it.remove();
                  }
                  else if (g.getElement() != f.getElement()) {
                     adjacentFaces.add(g);
                  }
               }
            }
            for (FaceNodes3d g : adjacentFaces) {
               if (f.isContained(g)) {
                  f.setHidden(true);
                  g.setOverlapping(true);
               }
               if (g.isContained(f)) {
                  g.setHidden(true);
                  f.setOverlapping(true);
               }
            }
         }
      }
   }

   LinkedList<FaceNodes3d> removeOverlappingFaces (
      LinkedList<FaceNodes3d> faceNodes) {
      LinkedList<FaceNodes3d> newNodes = new LinkedList<FaceNodes3d>();

      for (FaceNodes3d fn : faceNodes) {
         if (!fn.isOverlapping() &&
            !fn.hasSelfAttachedNode() &&
            !fn.isSelfAttachedToFace()) {
            newNodes.add (fn);
         }
      }
      return newNodes;
   }

   private int indexOfNode (FemNode3d n, FemNode3d[] nodes) {
      for (int i=0; i<nodes.length; i++) {
         if (nodes[i] == n) {
            return i;
         }
      }
      return -1; // shouldn't happen
   }

   /**
    * Mark the half edges in a triangular face which correspond to actual
    * elements edges. To check this, we check the FEM nodes associated with
    * each half edge. The nodes of element edges will appear in sequence in the
    * set of face nodes.
    *
    * @param face face whose edges should be marked
    * @param tnodes nodes associated with the triangle
    * @param fnodes associated with the FEM face
    */
   private void markElementEdges (
      Face face, FemNode3d[] tnodes, FemNode3d[] fnodes) {
      
      HalfEdge he = face.firstHalfEdge();
      FemNode3d tailNode = tnodes[2];
      for (int j = 0; j < 3; j++) {
         FemNode3d headNode = tnodes[j];
         // find the index of the tailNode in fnodes
         int tidx = 0;
         while (fnodes[tidx] != tailNode) {
            tidx++;
         }
         // now see if the node at the next index equals the headNode
         int nidx = (tidx < fnodes.length-1 ? tidx+1 : 0);
         if (fnodes[nidx] == headNode) {
            // mark edge
            he.setFlag (HalfEdge.ELEM_EDGE);
         }
         tailNode = headNode;
         he = he.getNext();
      }
   }

   protected void createMeshFromFaceNodes (
      VertexInfo info, PolygonalMesh mesh, LinkedList<FaceNodes3d> faceNodes) {

      for (FaceNodes3d fn : faceNodes) {
         FemNode3d[][] triangles = fn.triangulate();
         boolean triangulatedQuad =
            (triangles.length == 2 && triangles[0][0] == triangles[1][0]);
         for (int i = 0; i < triangles.length; i++) {
            FemNode3d[] tri = triangles[i];
            Vertex3d[] vtxs = new Vertex3d[3];
            for (int j = 0; j < 3; j++) {
               FemNode3d node = tri[j];
               if ((vtxs[j] = info.myNodeMap.get(node)) == null) {
                  Vertex3d vtx =
                     new Vertex3d (new Point3d(node.getPosition()));
                  mesh.addVertex (vtx);
                  info.myAttachments.add (
                     new PointParticleAttachment (node, null));
                  info.myNumSingleAttachments++;
                  info.myNodeMap.put (node, vtx);
                  vtxs[j] = vtx;
               }
            }
            Face face = mesh.addFace(vtxs);
            // mark edges which correspond to actual element edges.
            markElementEdges (face, tri, fn.getNodes());

            if (triangulatedQuad && i == 0) {
               face.setFirstQuadTriangle(true);
            }
         }
      }
   }

   /**
    * Adds all nodes which are referenced by this FemMeshComp to a HashSet.
    * Used internally by the system.
    */
   protected void addAllVertexNodes (HashSet<FemNode3d> nodes) {
      ArrayList<PointAttachment> vtxAttachments = myVertexInfo.myAttachments;
      for (int i=0; i<vtxAttachments.size(); i++) {
         addVertexNodes (nodes, vtxAttachments.get(i));
      }
   }

   protected void updateVertexColors() {

      if (!mySurfaceRendering.usesStressOrStrain()) {
         return;
      }

      if (myStressPlotRanging == Ranging.Auto) {
         myStressPlotRange.merge (myFem.getNodalPlotRange(mySurfaceRendering));
      } 

      RenderProps rprops = getRenderProps();
      float alpha = (float)rprops.getAlpha();

      MeshBase mesh = getMesh();

      double sval = 0;
      ArrayList<PointAttachment> vtxAttachments = myVertexInfo.myAttachments;
      for (int i=0; i<vtxAttachments.size(); i++) {
         PointAttachment attacher = vtxAttachments.get(i);
         sval = 0;
         if (attacher instanceof PointFem3dAttachment) {
            PointFem3dAttachment pfa = (PointFem3dAttachment)attacher;
            FemNode[] nodes = pfa.getNodes();
            VectorNd weights = pfa.getCoordinates();
            for (int j=0; j<nodes.length; j++) {
               if (nodes[j] instanceof FemNode3d) { // paranoid!
                  FemNode3d node = (FemNode3d)nodes[j];
                  double w = weights.get(j);
                  if (mySurfaceRendering == SurfaceRender.Stress) {
                     sval += w*node.getVonMisesStress();
                  }
                  else if (mySurfaceRendering == SurfaceRender.MAPStress) {
                     sval += w*node.getMAPStress();
                  }
                  else if (mySurfaceRendering == SurfaceRender.MaxShearStress) {
                     sval += w*node.getMaxShearStress();
                  }
                  else if (mySurfaceRendering == SurfaceRender.Strain) {
                     sval += w*node.getVonMisesStrain();
                  } 
                  else if (mySurfaceRendering == SurfaceRender.MAPStrain) {
                     sval += w*node.getMAPStrain();
                  }
                  else if (mySurfaceRendering == SurfaceRender.MaxShearStrain) {
                     sval += w*node.getMaxShearStrain();
                  }
               }
            }
         }
         else if (attacher instanceof PointParticleAttachment) {
            PointParticleAttachment ppa = (PointParticleAttachment)attacher;
            FemNode3d node = (FemNode3d)ppa.getParticle();
            if (mySurfaceRendering == SurfaceRender.Stress) {
               sval = node.getVonMisesStress();
            }
            else if (mySurfaceRendering == SurfaceRender.MAPStress) {
               sval = node.getMAPStress();
            }
            else if (mySurfaceRendering == SurfaceRender.MaxShearStress) {
               sval = node.getMaxShearStress();
            }
            else if (mySurfaceRendering == SurfaceRender.Strain) {
               sval = node.getVonMisesStrain();
            } 
            else if (mySurfaceRendering == SurfaceRender.MAPStrain) {
               sval = node.getMAPStrain();
            }
            else if (mySurfaceRendering == SurfaceRender.MaxShearStrain) {
               sval = node.getMaxShearStrain();
            }
         }
         double smin = myStressPlotRange.getLowerBound();
         double srng = myStressPlotRange.getRange();
         double c = (sval-smin)/srng;
         c = Math.max (0, Math.min (c, 1.0));
         myColorMap.getRGB(c, colorArray);
         mesh.setColor (
            i, colorArray[0], colorArray[1], colorArray[2], alpha);
      }
   }

   private void writeVertexInfo (PrintWriter pw, Vertex3d vtx, NumberFormat fmt) {
      PointAttachment pa = null;
      if (vtx.getIndex() < numVertexAttachments()) {
         pa = getVertexAttachment(vtx.getIndex());
      }
      if (pa instanceof PointFem3dAttachment) {
         PointFem3dAttachment pfa = (PointFem3dAttachment)pa;
         FemNode[] masters = pfa.getNodes();
         pw.print ("v");
         for (int j=0; j<masters.length; j++) {
            pw.print (
               " " + masters[j].getNumber()+" "+fmt.format(pfa.getCoordinate(j)));
         }
         pw.println ("");               
      }
      else if (pa instanceof PointParticleAttachment) {
         PointParticleAttachment ppa = (PointParticleAttachment)pa;
         FemNode3d n = (FemNode3d)ppa.getParticle();
         pw.println ("v " + n.getNumber() + " 1.0");
      }      
      else {
         pw.println ("v -1 "+vtx.getPosition().toString(fmt));
      }
   }

   /**
    * Writes the mesh for this FemMeshComp out to a named file,
    * using the format described for
    * {@link #writeMesh(PrintWriter)}.
    *
    * @param fileName Name of file to write the mesh to
    * @return <code>true</code> if the mesh is a polygonal mesh and was written,
    * and <code>false</code> otherwise.
    */
   public boolean writeMesh (String fileName) {
      IndentingPrintWriter pw = null;
      boolean status = false;
      try {
         pw = ArtisynthIO.newIndentingPrintWriter(fileName);
         status = writeMesh (pw);
      } catch (IOException e) {
         e.printStackTrace();
      }
      finally {
         if (pw != null) {
            pw.close();
         }
      }
      return status;
   }

   /**
    * Writes the mesh for this FemMeshComp out to a PrintWriter, using a format
    * of the form
    * <pre>
    * [ v 3 1.0
    *   v 3 0.5 1 0.5
    *   v 1 0.25 2 0.35 3 0.25 4 0.25
    *   v 2 0.5 4 0.5
    *   v 4 1.0
    *   f 1 3 2
    *   f 1 5 3
    *   f 3 5 4
    * ]
    * </pre>
    * Here each line beginning with <code>v</code> describes a mesh
    * vertex in terms of the FEM nodes to which it is attached.
    * This takes the form of a list of number/weight pairs:
    * <pre>
    *   v n_0 w_0 n_1 w_1 n_2 w_2 ...
    * </pre>
    * where <code>n_i</code> and <code>w_i</code> indicate, respectively,
    * a FEM node (using its number as returned by
    * {@link artisynth.core.modelbase.ModelComponent#getNumber getNumber()}),
    * and its corresponding weight. In the case where a vertex
    * is not associated with any nodes, then it is described by
    * a line of the form
    * <pre>
    *   v -1 pz py pz
    * </pre>
    * where <code>px</code>, <code>py</code>, and <code>pz</code>
    * give the vertex's (fixed) position in space.
    * <p>
    * Each line beginning with <code>f</code> describes a mesh face,
    * using the vertex numbers in counter-clockwise order about the
    * outward facing normal. To be consistent with Wavefront <code>.obj</code>
    * format, the vertex numbers are 1-based and so are equal to the
    * vertex indices plus one.
    * <p>
    * If the mesh is not an instance of
    * {@link maspack.geometry.PolygonalMesh}, then the method does
    * nothing and returns <code>null</code>.
    *
    * @param pw writer to whichmesh is written
    * @return <code>true</code> if the mesh is a polygonal mesh and was written,
    * and <code>false</code> otherwise.
    */
   public boolean writeMesh (PrintWriter pw) {
      return writeMesh (pw, /*nodeFormat=*/false);
   }      

   public boolean writeMesh (PrintWriter pw, boolean nodeFormat) {
      PolygonalMesh mesh = null;
      if (!(getMesh() instanceof PolygonalMesh)) {
         return false;
      }
      mesh = (PolygonalMesh)getMesh();
      pw.print("[ ");
      NumberFormat fmt = new NumberFormat ("%.8g");
      IndentingPrintWriter.addIndentation(pw, 2);
      if (!nodeFormat) {
         for (Vertex3d vtx : mesh.getVertices()) {
            writeVertexInfo (pw, vtx, fmt);
         }
      }
      ArrayList<Integer> nodeNums = new ArrayList<Integer>();
      for (Face face : mesh.getFaces()) {
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         pw.print("f");         
         do {
            int vidx = he.head.getIndex();
            if (nodeFormat) {
               PointParticleAttachment ppa =
                  (PointParticleAttachment)getVertexAttachment(vidx);
               FemNode3d node = (FemNode3d)ppa.getParticle();
               pw.print (" " + node.getNumber());
            }
            else {
               pw.print (" " + (vidx+1));
            }
            he = he.getNext();
         }
         while (he != he0);
         pw.println("");
      }
      IndentingPrintWriter.addIndentation(pw, -2);
      pw.println("]");
      return true;
   }      

   private FemNode3d getNodeFromNumber (
      ReaderTokenizer rtok, int nnum) throws IOException {
      FemNode3d node = null;
      if ((node = myFem.getNodes().getByNumber(nnum)) == null) {
         throw new IOException("Node " + nnum + " not found, " + rtok);
      }
      return node;
   }

   /**
    * Old method of scanning mesh usign node numbers only.
    */
   private void scanMeshUsingNodeNumbers (ReaderTokenizer rtok)
      throws IOException {
      PolygonalMesh mesh = (PolygonalMesh)getMesh();
      // nodeVertexMap is used during the construction of the mesh,
      // so we build it during the construction rather then letting
      // it be built in finalizeSurfaceBuild()
      VertexInfo info = new VertexInfo();
      ArrayList<Vertex3d> vtxList = new ArrayList<Vertex3d>();
      rtok.nextToken();
      while (rtok.tokenIsWord("f")) {
         vtxList.clear();
         rtok.nextToken();
         while (rtok.tokenIsInteger()) {
            int nnum = (int)rtok.lval;
            FemNode3d node = getNodeFromNumber (rtok, nnum);
            Vertex3d vtx = info.myNodeMap.get(node);
            if (vtx == null) {
               vtx = new Vertex3d (new Point3d(node.getPosition()));
               info.myNodeMap.put(node, vtx);
               info.myAttachments.add (
                  new PointParticleAttachment(node, null));
               info.myNumSingleAttachments++;
               mesh.addVertex(vtx);
            }
            vtxList.add(vtx);
            rtok.nextToken();
         }
         mesh.addFace(vtxList.toArray(new Vertex3d[0]));
      }
      myVertexInfo = info;
      rtok.pushBack();
   }

   /**
    * New scan method where the vertex attachments are also scanned
    */
   private void scanMeshUsingVertexInfo (ReaderTokenizer rtok)
      throws IOException {
      PolygonalMesh mesh = (PolygonalMesh)getMesh();
      ArrayList<Vertex3d> vtxList = new ArrayList<Vertex3d>();
      ArrayList<FemNode> nodes = new ArrayList<FemNode>();
      VertexInfo info = new VertexInfo();
      VectorNd weights = new VectorNd();
      rtok.nextToken();
      while (rtok.tokenIsWord()) {
         if (rtok.sval.equals("v")) {
            int nnum = rtok.scanInteger();
            if (nnum == -1) {
               double x = rtok.scanNumber();
               double y = rtok.scanNumber();
               double z = rtok.scanNumber();
               mesh.addVertex (new Vertex3d (x, y, z));
               info.myAttachments.add (null);
               rtok.nextToken();
            }
            else {
               PointAttachment ax;
               double w = rtok.scanNumber();
               rtok.nextToken();
               Vertex3d vtx = new Vertex3d();
               if (rtok.tokenIsInteger()) {
                  nodes.clear();
                  weights.setSize(0);
                  nodes.add (getNodeFromNumber (rtok, nnum));
                  weights.append (w);
                  while (rtok.tokenIsInteger()) {
                     nodes.add (getNodeFromNumber (rtok, (int)rtok.lval));
                     weights.append (rtok.scanNumber());
                     rtok.nextToken();
                  }
                  PointFem3dAttachment attacher = new PointFem3dAttachment();
                  attacher.setFromNodes (nodes, weights);
                  ax = attacher;
               }
               else {
                  FemNode3d node = getNodeFromNumber (rtok, nnum);
                  ax = new PointParticleAttachment (node, null);
                  info.myNodeMap.put(node, vtx);
                  info.myNumSingleAttachments++;
               }
               mesh.addVertex (vtx);
               info.myAttachments.add (ax);
            }
         }
         else if (rtok.sval.equals ("f")) {
            vtxList.clear();
            rtok.nextToken();
            while (rtok.tokenIsInteger()) {
               int vnum = (int)rtok.lval;
               if (vnum > mesh.numVertices()) {
                  throw new IOException(
                     "Vertex number "+vnum+" not found, "+rtok);
               }
               vtxList.add (mesh.getVertex(vnum-1));
               rtok.nextToken();
            }
            mesh.addFace(vtxList.toArray(new Vertex3d[0]));
         }
         else {
            throw new IOException ("Unexpected token: "+rtok);
         }
      }
      myVertexInfo = info;
      rtok.pushBack();
   }

   /**
    * Scans this mesh from a file, using the format described for
    * {@link #writeMesh}. For backward compatibility, a format
    * of the form
    * <pre>
    * [ f 0 1 2
    *   f 2 3 4
    *   f 4 5 6
    * ]
    * </pre>
    * Where no vertex information is given, and each face is instead
    * described using the numbers of the FEM nodes that underlie
    * each vertex. This makes the assumption that the mesh is
    * single node mapped (i.e., {@link #isSingleNodeMapped} returns
    * <code>true</code>), so that each vertex is associated with
    * a single FEM node.
    *
    * @param rtok tokenizer from which mesh is read
    * @return the scanned mesh
    */
   public MeshBase scanMesh (ReaderTokenizer rtok) throws IOException {

      initializeSurfaceBuild();
      isGeneratedSurface = false;

      rtok.scanToken('[');
      ArrayList<Vertex3d> vtxList = new ArrayList<Vertex3d>();
      rtok.nextToken();
      if (rtok.ttype  == ReaderTokenizer.TT_WORD && rtok.sval.startsWith("f")) {
         rtok.pushBack();
         scanMeshUsingNodeNumbers (rtok);
      }
      else {
         rtok.pushBack();
         scanMeshUsingVertexInfo (rtok);
      }
      rtok.scanToken(']');

      finalizeSurfaceBuild();
      updateSlavePos();
      return (PolygonalMesh)getMesh();
   }

   private void writeVertexAdjacencies (
      PrintWriter pw, int[][] vtxAdjacencies) throws IOException {
      pw.println ("["); 
      IndentingPrintWriter.addIndentation (pw, 2);        
      for (int i=0; i<vtxAdjacencies.length; i++) {
         int[] vadj = vtxAdjacencies[i];
         StringBuilder sb = new StringBuilder();
         sb.append (vadj.length + " ");
         for (int j=0; j<vadj.length; j++) {
            sb.append (" " + vadj[j]);
         }
         pw.println (sb.toString());
      }
      IndentingPrintWriter.addIndentation (pw, -2); 
      pw.println ("]");
   }

   private int[][] scanVertexAdjacencies (ReaderTokenizer rtok)
      throws IOException {
      ArrayList<int[]> adjacenciesList = new ArrayList<>();
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (!rtok.tokenIsInteger()) {
            throw new IOException (
               "Expected adjacency count, got " + rtok);
         }
         int nadj = (int)rtok.lval;
         int[] vadj = new int[nadj];
         for (int j=0; j<nadj; j++) {
            vadj[j] = rtok.scanInteger();
         }
         adjacenciesList.add (vadj);
      }
      return adjacenciesList.toArray(new int[0][]);
   }

   protected void writeAttachment (
      PointAttachment attacher, PrintWriter pw, NumberFormat fmt,
      CompositeComponent ancestor)
         throws IOException {
      pw.print ("[ ");
      if (attacher instanceof PointParticleAttachment) {
         PointParticleAttachment ppa = (PointParticleAttachment)attacher;
         FemNode node = (FemNode)ppa.getParticle();
         if (FemElement.writeNodeRefsByNumber) {
            pw.print (node.getNumber() + " ");
         }
         else {
            pw.print (
               ComponentUtils.getWritePathName (ancestor, node) + " 1 ");
         }
      }
      else if (attacher instanceof PointFem3dAttachment) {
         PointFem3dAttachment pfa = (PointFem3dAttachment)attacher;
         FemNode[] nodes = pfa.getNodes();
         VectorNd weights = pfa.getCoordinates();
         for (int i=0; i<nodes.length; i++) {
            if (FemElement.writeNodeRefsByNumber) {
               pw.print (
                  nodes[i].getNumber() + " " + fmt.format(weights.get(i)) + " ");
            }
            else {
               pw.print (
                  ComponentUtils.getWritePathName (ancestor, nodes[i]) +
                  " " + fmt.format(weights.get(i)) + " ");
            }
         }
      }
      pw.println ("]");
   }

   protected void scanAttachment (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

      rtok.scanToken ('[');
      rtok.nextToken();
      // look at first token to see if nodes are by reference or number
      boolean scanNodesByNumber;
      if (rtok.tokenIsQuotedString('"')) {
         scanNodesByNumber = false;
      }
      else if (rtok.tokenIsInteger()) {
         scanNodesByNumber = true;
      }
      else {
         throw new IOException (
            "Expected node reference or number; got "+rtok);
      }
      ArrayList<PointAttachment> vtxAttachments = myVertexInfo.myAttachments;
      if (scanNodesByNumber) {
         int num0 = (int)rtok.lval;
         if (rtok.nextToken() == ']') {
            tokens.offer (new IntegerToken (num0));
            PointParticleAttachment ppa = new PointParticleAttachment ();
            vtxAttachments.add (ppa);            
         }
         else {
            ArrayList<Integer> nodeNums = new ArrayList<Integer>();
            ArrayList<Double> weights = new ArrayList<Double>();
            if (!rtok.tokenIsNumber()) {
               throw new IOException ("Expected node weight, got "+rtok);
            }
            nodeNums.add (num0);
            weights.add (rtok.nval);
            while (rtok.nextToken() != ']') {
               if (!rtok.tokenIsInteger()) {
                  throw new IOException ("Expected node number, got "+rtok);
               }
               nodeNums.add ((int)rtok.lval);
               weights.add (rtok.scanNumber());
            }
            tokens.offer (new ObjectToken(ArraySupport.toIntArray (nodeNums)));
            tokens.offer (new ObjectToken(ArraySupport.toDoubleArray (weights)));
            PointFem3dAttachment pfa = new PointFem3dAttachment(); 
            vtxAttachments.add (pfa);
         }
      }
      else {
         // scan nodes by reference
         rtok.pushBack();
         ArrayList<Double> weights = new ArrayList<Double>();
         tokens.offer (ScanToken.BEGIN);
         while (ScanWriteUtils.scanAndStoreReference (rtok, tokens)) {
            weights.add (rtok.scanNumber());
         }
         if (rtok.ttype != ']') {
            throw new IOException ("Expected ']', got " + rtok);
         }
         tokens.offer (ScanToken.END); // add null terminator
         if (weights.size() == 1) {
            PointParticleAttachment ppa = new PointParticleAttachment ();
            vtxAttachments.add (ppa);
         }
         else {
            PointFem3dAttachment pfa = new PointFem3dAttachment();
            tokens.offer (new ObjectToken(ArraySupport.toDoubleArray (weights)));
            vtxAttachments.add (pfa);
         }
      }
   }
   
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName(rtok, "surfaceMesh")) {
         isSurfaceMesh = rtok.scanBoolean();
         return true;
      }
      else if (scanAttributeName(rtok, "generatedSurface")) {
         isGeneratedSurface = rtok.scanBoolean();
         return true;
      }
      else if (scanAttributeName (rtok, "attachments")) {
         tokens.offer (new StringToken ("attachments", rtok.lineno()));
         rtok.scanToken ('[');
         while (rtok.nextToken() != ']') {
            rtok.pushBack();
            scanAttachment (rtok, tokens);
         }
         return true;
      }
      else if (scanAttributeName (rtok, "vertexAdjacencies")) {
         myVertexInfo.setVertexAdjacencies (scanVertexAdjacencies(rtok));
         return true;
      }
      else if (scanAttributeName (rtok, "meshdata")) {
         scanDirectMesh (rtok);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "fem")) {
         myFem = postscanReference (tokens, FemModel3d.class, ancestor);
         return true;
      }
      else if (postscanAttributeName (tokens, "attachments")) {
         for (int i=0; i<myVertexInfo.myAttachments.size(); i++) {
            PointAttachment va = myVertexInfo.myAttachments.get(i);
            if (tokens.peek() == ScanToken.BEGIN) {
               // nodes are by reference
               FemNode[] nodes = ScanWriteUtils.postscanReferences (
                  tokens, FemNode.class, ancestor);
               if (va instanceof PointParticleAttachment) {
                  PointParticleAttachment ppa = (PointParticleAttachment)va;
                  ppa.setParticle (nodes[0]);
               }
               else if (va instanceof PointFem3dAttachment) {
                  PointFem3dAttachment pfa = (PointFem3dAttachment)va;
                  double[] coords = (double[])tokens.poll().value();
                  pfa.setFromNodes (nodes, coords);
               }
            }
            else {
               // nodes are by number
               if (va instanceof PointParticleAttachment) {
                  int nodeNum = (int)tokens.poll().value();
                  PointParticleAttachment ppa = (PointParticleAttachment)va;
                  FemNode node = myFem.getNodeByNumber (nodeNum);
                  if (node == null) {
                     throw new IOException (
                        "Fem attachment node num "+nodeNum+" not found");
                  }
                  ppa.setParticle (node);
               }
               else {
                  int[] nodeNums = (int[])tokens.poll().value();
                  FemNode[] nodes = new FemNode[nodeNums.length];
                  for (int k=0; k<nodes.length; k++) {
                     FemNode node = myFem.getNodeByNumber (nodeNums[k]);
                     if (node == null) {
                        throw new IOException (
                           "Fem attachment node num "+nodeNums[k]+" not found");
                     }
                     nodes[k] = node;
                  }
                  PointFem3dAttachment pfa = (PointFem3dAttachment)va;
                  double[] coords = (double[])tokens.poll().value();
                  pfa.setFromNodes (nodes, coords);
               }
            }
         }
         myVertexInfo.buildNodeMap(getMesh());
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
         throws IOException {

      if (myFem != null) {
         pw.print ("fem=");
         pw.println (ComponentUtils.getWritePathName (ancestor, myFem));
      }
      pw.println("surfaceMesh=" + isSurfaceMesh());
      if (isGeneratedSurface()) {
         pw.println("generatedSurface=true");
      }
      pw.println ("attachments=["); 
      IndentingPrintWriter.addIndentation (pw, 2);
      ArrayList<PointAttachment> vtxAttachments = myVertexInfo.myAttachments;
      for (int i=0; i<vtxAttachments.size(); i++) {
         writeAttachment (vtxAttachments.get(i), pw, fmt, ancestor);
      }
      IndentingPrintWriter.addIndentation (pw, -2); 
      pw.println ("]");
      int[][] vtxAdjacencies = myVertexInfo.getVertexAdjacencies();
      if (vtxAdjacencies != null) {
         pw.print ("vertexAdjacencies="); 
         writeVertexAdjacencies (pw, vtxAdjacencies);
      }
      // have to write attachments before calling super.writeItems() because
      // they have to be written *before* the surfaceRender property
      super.writeItems (pw, fmt, ancestor);
   }

   /**
    * Overrides the method that writes the mesh info. If the mesh is a simple
    * polygonal mesh with no color, normals, or texture, and is not associated
    * with a file, we can write it directly and specify only the vertices which
    * are unattached. This can save a lot of file space in certain cases, such
    * as surfaces meshes for large FEMs.
    */
   @Override
   protected void writeMeshInfo (PrintWriter pw, NumberFormat fmt)
      throws IOException {
      MeshBase mesh = myMeshInfo.getMesh();
      if (!(mesh instanceof PolygonalMesh) ||
          mesh.hasColors() ||
          mesh.hasExplicitNormals() ||
          mesh.hasTextureCoords() ||
          myMeshInfo.getFileName() != null) {
         super.writeMeshInfo (pw, fmt);
      }
      else {
         pw.print ("meshdata=");
         writeDirectMesh (pw, (PolygonalMesh)mesh, fmt);
      }
   }

   private void writeDirectMesh (
      PrintWriter pw, PolygonalMesh mesh, NumberFormat fmt) {
      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      ArrayList<PointAttachment> vtxAttachments = myVertexInfo.myAttachments;
      if (vtxAttachments.size() < mesh.numVertices()) {
         // note: right now, all mesh creation methods ensure that
         // the number of attachments equals the number of mesh
         // vertices; there are no unattached vertices.
         pw.println ("# unattached vertices:");
         for (int i=vtxAttachments.size(); i<mesh.numVertices(); i++) {
            pw.println (
               "v " + mesh.getVertex(i).getPosition().toString(fmt));
         }
      }
      for (Face face : mesh.getFaces()) {
         // print out faces using Alias Wavefront notation, with 1-based
         // indices
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         pw.print("f");         
         do {
            int vidx = he.head.getIndex();
            pw.print (" " + (vidx+1));
            he = he.getNext();
         }
         while (he != he0);
         pw.println("");
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   void createBlankAttachmentVertices (PolygonalMesh mesh) {
      int numa = myVertexInfo.myAttachments.size();
      while (mesh.numVertices() < numa) {
         // fill in missing vertices
         mesh.addVertex (new Vertex3d (0, 0, 0));
      }      
   }
   
   private void scanDirectMesh (ReaderTokenizer rtok) throws IOException {
      PolygonalMesh mesh = new PolygonalMesh();
      rtok.scanToken('[');
      boolean scanningVertices = false;
      boolean scanningFaces = false;
      rtok.nextToken();
      ArrayList<Vertex3d> vtxList = new ArrayList<Vertex3d>();
      while (rtok.tokenIsWord()) {
         if (rtok.sval.equals("v")) {
            // note: we will get v entries only if there are more
            // vertices than attachments, which is not currently
            // supported by any of the mesh creation methods
            if (!scanningVertices) {
               createBlankAttachmentVertices (mesh);
               scanningVertices = true;
            }
            if (scanningFaces) {
               throw new IOException (
                  "Vertex entry after face entries, line "+rtok.lineno());
            }
            double x = rtok.scanNumber();
            double y = rtok.scanNumber();
            double z = rtok.scanNumber();           
            mesh.addVertex (new Vertex3d (x, y, z));
            rtok.nextToken();
         }
         else if (rtok.sval.equals("f")) {
            if (!scanningVertices) {
               // attachment vertices have not been created yet
               createBlankAttachmentVertices (mesh);
            }           
            if (!scanningFaces) {
               scanningFaces = true;
               scanningVertices = false;
            }
            vtxList.clear();
            rtok.nextToken();
            while (rtok.tokenIsInteger()) {
               int vnum = (int)rtok.lval;
               if (vnum > mesh.numVertices()) {
                  throw new IOException(
                     "Vertex number "+vnum+" not found, "+rtok);
               }
               vtxList.add (mesh.getVertex(vnum-1));
               rtok.nextToken();
            }
            mesh.addFace(vtxList.toArray(new Vertex3d[0]));           
         }
         else {
            throw new IOException ("Unexpected token: "+rtok);
         }
      }
      if (rtok.ttype != ']') {
         throw new IOException ("Expected token ']', got " + rtok);
      }
      myMeshInfo.set (mesh);
      setMeshFromInfo();
      myMeshScannedDirectly = true; // update vertex values in postscan
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

   @Override
   public FemMeshComp copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FemMeshComp fm = (FemMeshComp)super.copy(flags, copyMap);

      FemModel3d newFem = (FemModel3d)copyMap.get(myFem);
      if (newFem != null) {
         fm.myFem = newFem;
      } 
      else {
         fm.myFem = myFem;
      }
      fm.myVertexInfo = myVertexInfo.copy (flags, fm.getMesh(), copyMap);

      fm.isSurfaceMesh = isSurfaceMesh();
      fm.isGeneratedSurface = isGeneratedSurface();

      return fm;
   }

   public boolean isSurfaceMesh() {
      return isSurfaceMesh;
   }

   /**
    * Returns {@code true} is this mesh was generated by one of the {@code
    * createSurface} methdods.
    *
    * @return {@code true} if this is a generated surface
    */
   public boolean isGeneratedSurface() {
      return isGeneratedSurface;
   }

   protected void markSurfaceMesh(boolean set) {
      MeshBase mesh = getMesh();
      if (mesh != null && !(mesh instanceof PolygonalMesh)) {
         throw new IllegalArgumentException(
            "Mesh must be a PolygonalMesh to be set as a surface");
      }
      isSurfaceMesh = set;
   }

   /**
    * Check if this mesh depends on a particular node
    */
   public boolean hasNodeDependency(FemNode3d node) {

      for (PointAttachment pa : myVertexInfo.myAttachments) {
         for (DynamicComponent dmc : pa.getMasters()) {
            if (dmc == node) {
               return true;
            }
         }
      }
      return false;

   }

   // begin Collidable implementation

   public PolygonalMesh getCollisionMesh() {
      MeshBase mesh = getMesh ();
      if (mesh instanceof PolygonalMesh) {
         return (PolygonalMesh)mesh;
      }
      return null;
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
      return myFem;
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
   public double getMass () {
      return myFem.getMass ();
   }
   
   /**
    * {@inheritDoc}
    */
   public void collectVertexMasters (
      List<ContactMaster> mlist, Vertex3d vtx) {
      PointAttachment pa = getVertexAttachment(vtx.getIndex());
      if (pa instanceof PointFem3dAttachment) {
         PointFem3dAttachment pfa = (PointFem3dAttachment)pa;
         mlist.add (pfa);
      }
      else {
         PointParticleAttachment ppa = (PointParticleAttachment)pa;
         mlist.add (ppa);
      }      
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean hasInvariantMasters() {
      return false;
   }

   public boolean containsContactMaster (CollidableDynamicComponent comp) {
      if (myVertexInfo.containsContactMaster (comp)) {
         return true;
      }
      else if (myFem.getFrame() == comp) {
         return true;
      }
      else {
         return false;
      }
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
   
   // end Collidable implementation

   /**
    * {@inheritDoc}
    */
   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {

      // if we are simulating, make no changes
      if ((flags & TransformableGeometry.TG_SIMULATING) != 0) {
         return;
      } 

      // see if context contains only FemMeshComps
      boolean onlyFemMeshComps = true;
      for (TransformableGeometry comp : context.getTransformables()) {
         if (!(comp instanceof FemMeshComp)) {
            onlyFemMeshComps = false;
            break;
         }
      }
      // revert to default behavior is mesh is generated or components other
      // than FemMeshComps are being transformed
      if (!onlyFemMeshComps || isGeneratedSurface) {
         super.transformGeometry (gtr, context, flags);
         return;
      }
      
      if (getMesh() != null) {
         gtr.transform (getMesh());
      }
      // request an action to update vertex attachments
      context.addAction (
         new TransformGeometryAction() {
            public void transformGeometry (
               GeometryTransformer gtr,
               TransformGeometryContext context, int flags) {
               createEmbeddingAttachments();
            }
         });
      //super.transformGeometry (gtr, context, flags);
   }   


   private void accumulateNodeWeights (
      FemNode node, double w, HashMap<FemNode,Double> nodeWeights) {

      if (nodeWeights.get (node) == null) {
         nodeWeights.put (node, w);
      }
      else {
         double prevw = nodeWeights.get (node);
         nodeWeights.put (node, w+prevw);
      }
   }

   public PointFem3dAttachment createPointAttachment (Point pnt) {

      if (!(getMesh() instanceof PolygonalMesh)) {
         return null;
      }
      PolygonalMesh mesh = (PolygonalMesh)getMesh();
      if (!mesh.isTriangular()) {
         return null;
      }
      // Find nearest face to the point. The vertices of this face will be used
      // to find the nodes and weight for the attachment.
      BVFeatureQuery query = new BVFeatureQuery();
      Point3d near = new Point3d();
      Vector2d uv = new Vector2d();
      Face face = query.nearestFaceToPoint (near, uv, mesh, pnt.getPosition());

      Vertex3d[] vtxs = face.getTriVertices();
      double[] wgts = new double[] { 1-uv.x-uv.y, uv.x, uv.y };          

      HashMap<FemNode,Double> nodeWeights = new HashMap<FemNode,Double>();
      ArrayList<PointAttachment> vtxAttachments = myVertexInfo.myAttachments;
      for (int i=0; i<vtxs.length; i++) {
         PointAttachment va = vtxAttachments.get(vtxs[i].getIndex());
         if (va instanceof PointParticleAttachment) {
            PointParticleAttachment ppa = (PointParticleAttachment)va;
            FemNode node = (FemNode)ppa.getParticle();
            accumulateNodeWeights (node, wgts[i], nodeWeights);
         }
         else if (va instanceof PointFem3dAttachment) {
            PointFem3dAttachment pfa = (PointFem3dAttachment)va;
            for (int k=0; k<pfa.numMasters(); k++) {
               FemNode node = pfa.getNodes()[k];
               double w = pfa.getCoordinate(k);
               accumulateNodeWeights (node, w*wgts[i], nodeWeights);
            }
         }
      }
      // Create a new PointFem3dAttachment

      PointFem3dAttachment ax = new PointFem3dAttachment (pnt);
      VectorNd weightVec = new VectorNd();
      for (Double d : nodeWeights.values()) {
         weightVec.append (d);
      }
      ax.setFromNodes (nodeWeights.keySet(), weightVec);
      return ax;
   }

   public FemModel3d getModel() {
      return myFem;
   }

   // uncomment to produce timings for FemMeshComp scanning operations:

   // public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
   //    FunctionTimer timer = new FunctionTimer();
   //    timer.start();
   //    super.scan (rtok, ref);
   //    timer.stop();
   //    System.out.println ("FemMeshComp scan: " + timer.result(1));
   // }

   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      // FunctionTimer timer = new FunctionTimer();
      // timer.start();
      super.postscan (tokens, ancestor);
      if (myMeshScannedDirectly) {
         updateSlavePos();
         myMeshScannedDirectly = false;
      }
      //timer.stop();
      //System.out.println ("FemMeshComp postscan: " + timer.result(1));
   }
}
