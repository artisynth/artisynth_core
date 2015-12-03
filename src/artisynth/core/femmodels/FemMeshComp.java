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
import java.util.Map.Entry;
import java.util.Set;

import maspack.geometry.BVFeatureQuery;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.MeshBase;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector2d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.render.RenderProps;
import maspack.spatialmotion.Wrench;
import maspack.util.ArraySupport;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.femmodels.FemModel.ElementFilter;
import artisynth.core.femmodels.FemModel.Ranging;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.Collidable.Collidability;
import artisynth.core.mechmodels.CollidableBody;
import artisynth.core.mechmodels.CollidableDynamicComponent;
import artisynth.core.mechmodels.CollisionHandler;
import artisynth.core.mechmodels.ContactPoint;
import artisynth.core.mechmodels.ContactMaster;
import artisynth.core.mechmodels.DynamicAttachment;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.PointList;
import artisynth.core.mechmodels.PointParticleAttachment;
import artisynth.core.mechmodels.PointAttachable;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentChangeEvent.Code;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.util.ObjectToken;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import artisynth.core.util.ArtisynthIO;

/**
 * Describes a surface mesh that is "skinned" onto an FEM, such that its vertex
 * positions are determined by weighted combinations of FEM node positions.
 **/
public class FemMeshComp extends FemMeshBase
   implements CollidableBody, PointAttachable {

   protected static double EPS = 1e-10;
   protected ArrayList<PointAttachment> myVertexAttachments;
   // myNodeVertexMap maps each node used by this FemMeshComp onto either
   // a single vertex which it completely controls, or the dummy variable 
   // NO_SINGLE_VERTEX if there is no such vertex.
   protected HashMap<FemNode3d,Vertex3d> myNodeVertexMap;
   protected static final Vertex3d NO_SINGLE_VERTEX = new Vertex3d();
   protected static final Collidability DEFAULT_COLLIDABILITY =
      Collidability.ALL;   
   
   HashMap<EdgeDesc,Vertex3d[]> myEdgeVtxs;
   private boolean isSurfaceMesh;
   private int myNumSingleAttachments;
   protected Collidability myCollidability = DEFAULT_COLLIDABILITY;

   private float[] colorArray = new float[3];

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
      myVertexAttachments = new ArrayList<PointAttachment>();
   }

   public FemMeshComp (FemModel3d fem) {
      this();
      myFem = fem;
      isSurfaceMesh = false;
   }

   public FemMeshComp(FemModel3d fem, String name) {
      this(fem);
      setName(name);
   }

   public int numAttachments () {
      return myVertexAttachments.size();
   }
   
   public PointAttachment getAttachment(Vertex3d vtx) {
      return getAttachment (vtx.getIndex());
   }

   public PointAttachment getAttachment (int idx) {
      return myVertexAttachments.get (idx);
   }

   /** 
    * Initialize data structures prior to adding vertices and faces to
    * this surface.
    */
   void initializeSurfaceBuild() {
      super.initializeSurfaceBuild();
      //myEdgeVtxs = new HashMap<EdgeDesc,Vertex3d[]>();
      myVertexAttachments = new ArrayList<PointAttachment>();
      myNodeVertexMap = null;
   }

   protected void buildNodeVertexMap() {
      myNodeVertexMap = new HashMap<FemNode3d,Vertex3d>();
      myNumSingleAttachments = 0;
      for (int i=0; i<myVertexAttachments.size(); i++) {
         PointAttachment pa = myVertexAttachments.get(i);
         if (pa instanceof PointParticleAttachment) {
            FemNode3d node = 
               (FemNode3d)((PointParticleAttachment)pa).getParticle();
            myNodeVertexMap.put (node, getMesh().getVertex(i));
            myNumSingleAttachments++;
         }
         else if (pa instanceof PointFem3dAttachment) {
            PointFem3dAttachment pfa = (PointFem3dAttachment)pa;
            for (FemNode node : pfa.getNodes()) {
               if (myNodeVertexMap.get(node) == null) {
                  myNodeVertexMap.put ((FemNode3d)node, NO_SINGLE_VERTEX);
               }
            }
         }
      }
   }
   
   /** 
    * Finalize data structures after vertices and faces have been added.
    */
   void finalizeSurfaceBuild() {
      super.finalizeSurfaceBuild();
      if (myNodeVertexMap == null) {
         buildNodeVertexMap();
      }
      //myEdgeVtxs.clear();
      
      notifyParentOfChange (
         new ComponentChangeEvent (Code.STRUCTURE_CHANGED, this));
      
      // save render info in case render gets called immediately
      myMeshInfo.getMesh ().saveRenderInfo ();
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
      return myNumSingleAttachments == getMesh().getNumVertices();
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
      if (idx < myVertexAttachments.size()) {
         return getNodeForVertex (myVertexAttachments.get(idx));
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
      if (myNodeVertexMap == null) {
         return null;
      }
      Vertex3d vtx = myNodeVertexMap.get (node);
      if (vtx == NO_SINGLE_VERTEX) {
         vtx = null;
      }
      return vtx;
   }
   
   /**
    * Find the FEM element corresponding to a surface face.
    */
   FemElement3d getFaceElementOld (Face face) {

      // Form the intersection of all the element dependencies of all three
      // nodes associated with the face. If the face is on the surface,
      // there should be only one element left.
      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he1 = he0.getNext();
      HalfEdge he2 = he1.getNext();

      HashSet<FemElement3d> elems =
         new HashSet<FemElement3d>(getEdgeNode(he0).getElementDependencies());
      elems.retainAll (getEdgeNode(he1).getElementDependencies());
      elems.retainAll (getEdgeNode(he2).getElementDependencies());

      if (elems.size() != 1) {
         FemElement3d[] elemArray = elems.toArray(new FemElement3d[0]);
         System.out.println (
            "face is "+
               getEdgeNode(he0).getNumber()+" "+
               getEdgeNode(he1).getNumber()+" "+
               getEdgeNode(he2).getNumber());

         for (int i=0; i<elemArray.length; i++) {
            System.out.println (" element "+elemArray[i].getNumber());
         }
         return null;
         // ignore for now ...
         // throw new InternalErrorException (
         //    "Face "+face+" associated with "+elems.size()+" elements");

      }
      return (FemElement3d)elems.toArray()[0];
   }

   private void addVertexNodes (HashSet<FemNode3d> nodes, Vertex3d vtx) {

      PointAttachment pa = getAttachment(vtx.getIndex());
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

   public FemElement3d getFaceElement (Face face) {

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

      HashSet<FemElement3d> elems = null;
      for (FemNode3d node : nodes) {
         if (elems == null) {
            elems = new HashSet<FemElement3d>(node.getElementDependencies());
         }
         else {
            elems.retainAll (node.getElementDependencies());
         }
      }
      if (elems.size() != 1) {
         FemElement3d[] elemArray = elems.toArray(new FemElement3d[0]);
         int[] idxs = face.getVertexIndices();
         System.out.print ("Error: couldn't get element for face [ ");
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
      return (FemElement3d)elems.toArray()[0];
   }

   private Vertex3d createVertex (FemNode3d node, Vertex3d v) {
      Vertex3d vtx = new Vertex3d (v.getPosition());
      PointParticleAttachment attacher =
         new PointParticleAttachment (node, null);
      myVertexAttachments.add (attacher);
      getMesh().addVertex (vtx);
      return vtx;
   }

   private void addNodeCoords (
      Vector3d coords, double s0, double s1, double s2, FemElement3d elem, 
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
      double s0, double s1, double s2, FemElement3d elem, 
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
      myVertexAttachments.add (attacher);

      getMesh().addVertex (vtx);
      return vtx;
   }

   private Vertex3d[] collectEdgeVertices (
      HashMap<EdgeDesc,Vertex3d[]> edgeVtxMap, Vertex3d v0, Vertex3d v1, 
      FemNode3d n0, FemNode3d n1, FemElement3d elem, int res) {

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
            vtxs[i] = createVertex (1-s1, s1, 0, elem, n0, n1, null);
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

   public static FemMeshComp createEmbedded(FemMeshComp surf, MeshBase mesh) {
      if (surf == null || surf.myFem == null) {
         throw new IllegalArgumentException("Cannot determine proper FEM");
      }
      return createEmbedded(surf, mesh, surf.myFem);
   }
   
   public void addVertexAttachment (PointAttachment attachment) {
      setVertexAttachment (myVertexAttachments.size(), attachment);
   }
   
   // XXX Sanchez: needed when attachment info already known and want to
   //              construct manually (more efficient).  Currently used
   //              in in-progess hex-mesher
   public void setVertexAttachment(int vidx, PointAttachment attachment) {
      // update vertex attachment size
      if (vidx == myVertexAttachments.size()) {
         myVertexAttachments.add(attachment);
      } else if (vidx < myVertexAttachments.size()) {
         myVertexAttachments.set(vidx, attachment);   
      } else {
         // add null attachments
         for (int i=myVertexAttachments.size(); i<vidx; i++) {
            myVertexAttachments.add(null);
         }
         myVertexAttachments.add(attachment);
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

      double reduceTol = 1e-8;

      ArrayList<FemNode> nodes = new ArrayList<FemNode>();
      VectorNd weights = new VectorNd();

      if (surf == null) {
         surf = new FemMeshComp(fem);
      }
      surf.setMesh (mesh);
      ArrayList<Vertex3d> verts = mesh.getVertices();

      surf.myVertexAttachments.clear();
      for (int i=0; i<verts.size(); i++) {
         // this could works very similarly to the code that adds
         // marker points into a mesh
         Vertex3d vtx = verts.get(i);
         // if (vtx instanceof FemMeshVertex) {
         //    nodes.clear();
         //    nodes.add(((FemMeshVertex)vtx).getPoint());
         //    weights.clear();
         //    weights.add(1.0);
         // }
         // else 
         {
            FemElement3d elem = surf.myFem.findContainingElement (vtx.pnt);
            Point3d newLoc = new Point3d(vtx.pnt);
            if (elem == null) {
               // won't use newLoc since we're not projecting vertex onto FEM
               elem = surf.myFem.findNearestSurfaceElement (newLoc, vtx.pnt);
            }
            VectorNd coords = new VectorNd (elem.numNodes());

            // first see if there's a node within reduceTol of the point,
            // and if so just use that
            double maxDist = Double.NEGATIVE_INFINITY;
            double minDist = Double.POSITIVE_INFINITY;
            FemNode3d nearestNode = null;
            FemNode3d[] elemNodes = elem.getNodes();
            for (int k=0; k<elemNodes.length; k++) {
               double d = vtx.pnt.distance(elemNodes[k].getPosition());
               if (d > maxDist) {
                  maxDist = d;
               }
               if (d < minDist) {
                  minDist = d;
                  nearestNode = elemNodes[k];
               }
            }
            if (minDist/maxDist <= reduceTol) {
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
                     "Warning: getNaturalCoordinatesRobust() did not converge, "+
                     "element=" + ComponentUtils.getPathName(elem) +
                     ", point=" + vtx.pnt);
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
         }

         if (weights.size() > 1) {
            PointFem3dAttachment attacher = new PointFem3dAttachment();
            attacher.setFromNodes (nodes, weights);
            surf.myVertexAttachments.add (attacher);
         } else if (weights.size() == 1) {
            PointParticleAttachment attacher =
               new PointParticleAttachment(nodes.get(0), null);
            surf.myVertexAttachments.add(attacher);
         }
      }
      surf.buildNodeVertexMap();

      // we manually control display lists, so this should work
      mesh.setUseDisplayList(true);

      return surf;
   }

   public static FemMeshComp createEmbedded (FemModel3d fem, MeshBase mesh) {
      return createEmbedded (new FemMeshComp(fem), mesh);
   }

   public static FemMeshComp createSurface (
      String name, FemModel3d fem, Collection<FemElement3d> elems) {
      FemMeshComp femMesh = new FemMeshComp(fem);
      femMesh.setName (name);
      femMesh.createSurface (elems);
      return femMesh; 
   }
   
   public static FemMeshComp createSurface (
      String name, FemModel3d fem, ElementFilter efilter) {
      return createSurface (name, fem, getFilteredElements (fem, efilter));
   }
   
   public static FemMeshComp createSurface (String name, FemModel3d fem) {
      return createSurface (name, fem, fem.getElements());
   }
   
   protected void createFineSurface (int resolution, ElementFilter efilter) {

      // build from nodes/element filter
      createSurface(efilter);

      if (resolution < 2) {
         // if resolution < 2, just return regular surface
         return;
      }

      // Note: can no longer rely on the surface mesh consisting of only 
      // FemMeshVertex
      // PolygonalMesh baseMesh = myFem.getSurfaceMesh();
      PolygonalMesh baseMesh = (PolygonalMesh)getMesh(); // since previously built
      ArrayList<Face> baseFaces = baseMesh.getFaces();
      ArrayList<Vertex3d> baseVertices = baseMesh.getVertices();
      ArrayList<PointAttachment> baseAttachments = myVertexAttachments;
      
      int numv = resolution+1; // num vertices along the edge of each sub face
      initializeSurfaceBuild();
      HashMap<EdgeDesc,Vertex3d[]> edgeVtxMap = new HashMap<EdgeDesc,Vertex3d[]>();

      // get newly empty mesh
      PolygonalMesh surfMesh = (PolygonalMesh)getMesh();
      //myNodeVertexMap.clear();
      
      for (Vertex3d vtx : baseVertices) {
         FemNode3d node = getNodeForVertex (baseAttachments.get(vtx.getIndex()));
         createVertex (node, vtx);
         //myNodeVertexMap.put (node, newVtx);
      }
      for (int k=0; k<baseFaces.size(); k++) {
         Face face = baseFaces.get(k);
         // store sub vertices for the face in the upper triangular half of
         // subv.
         MeshFactory.VertexSet subv = new MeshFactory.VertexSet (numv);

         FemElement3d elem = getFaceElement (face);
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
            edgeVtxMap, v0, v1, n0, n1, elem, resolution);
         for (int i=1; i<numv-1; i++) {
            subv.set (i, i, vtxs01[i]);
         }
         Vertex3d[] vtxs02 = collectEdgeVertices (
            edgeVtxMap, v0, v2, n0, n2, elem, resolution);
         for (int j=1; j<numv-1; j++) {
            subv.set (0, j, vtxs02[j]);
         }
         Vertex3d[] vtxs21 = collectEdgeVertices (
            edgeVtxMap, v2, v1, n2, n1, elem, resolution);
         for (int i=1; i<numv-1; i++) {
            subv.set (i, numv-1, vtxs21[i]);
         }

         for (int i=1; i<numv-1; i++) {
            for (int j=i+1; j<numv-1; j++) {
               double s1 = i/(double)resolution;
               double s0 = 1-j/(double)resolution;
               Vertex3d vtx = createVertex (s0, s1, 1-s0-s1, elem, n0, n1, n2);
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

      finalizeSurfaceBuild();
   }

   //   public static FemMeshComp createSurface (FemModel3d fem, int resolution) {
   //      return createSurface (new FemMeshComp (fem), resolution);
   //   }

   // private static boolean containsNode(FemNode3d n, FemNode[] nodes) {
   //    for (int i = 0; i < nodes.length; i++) {
   //       if (nodes[i] == n) {
   //          return true;
   //       }
   //    }
   //    return false;
   // }

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

   private static ArrayList<FemElement3d> getFilteredElements (
      FemModel3d fem, ElementFilter efilter) {
      ArrayList<FemElement3d> elems = 
         new ArrayList<FemElement3d>(fem.numElements());
      // create a list of all the faces for all the elements, and for
      // each node, create a list of all the faces it is associated with
      for (FemElement3d e : fem.getElements()) {
         if (efilter.elementIsValid(e)) {
            elems.add (e);
         }
      }
      return elems;
   }

   public void createSurface (ElementFilter efilter) {

      createSurface (getFilteredElements (myFem, efilter));
   }

   //Throwable throwable = null;

   public void createSurface (Collection<FemElement3d> elems) {

      // if (throwable != null) {
      //    System.out.println ("already inside createSurface.");
      //    System.out.println ("prev:");
      //    throwable.printStackTrace();
      //    System.out.println ("now:");
      //    (new Throwable()).printStackTrace();
      // }
      // else {
      //    throwable = new Throwable();
      // }
      

      initializeSurfaceBuild();
      // nodeVertexMap is used during the construction of this surface,
      // so we build it during the construction rather then letting
      // it be built in finalizeSurfaceBuild()
      myNodeVertexMap = new HashMap<FemNode3d,Vertex3d>();
      myNumSingleAttachments = 0;

      LinkedList<FaceNodes3d> allFaces = new LinkedList<FaceNodes3d>();
      // faces adjacent to each node
      ArrayList<LinkedList<FaceNodes3d>> nodeFaces =
         new ArrayList<LinkedList<FaceNodes3d>>(myFem.numNodes());

      for (int i = 0; i < myFem.numNodes(); i++) {
         nodeFaces.add(new LinkedList<FaceNodes3d>());
      }

      PointList<FemNode3d> femNodes = myFem.getNodes();

      // create a list of all the faces for all the elements, and for
      // each node, create a list of all the faces it is associated with
      for (FemElement3d e : elems) {
         FaceNodes3d[] faces = e.getTriFaces();
         for (FaceNodes3d f : faces) {
            addEdgeNodesToFace(f, myFem);
            for (FemNode3d n : f.getAllNodes()) {
               int idx = femNodes.indexOf(n);
               if (idx == -1) {
                  throw new InternalErrorException(
                     "Element " + e.getNumber() + ": bad node "
                     + n.getNumber());
               }
               nodeFaces.get(femNodes.indexOf(n)).add(f);
            }
            allFaces.add(f);
         }
      }

      // now for each face, check to see if it is overlapping with other faces
      HashSet<FaceNodes3d> adjacentFaces = new HashSet<FaceNodes3d>();
      for (FaceNodes3d f : allFaces) {
         if (!f.isHidden()) {
            adjacentFaces.clear();
            for (FemNode3d n : f.getAllNodes()) {
               Iterator<FaceNodes3d> it =
                  nodeFaces.get(femNodes.indexOf(n)).iterator();
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

      // form the surface mesh from the non-overlapping faces
      PolygonalMesh mesh = (PolygonalMesh)getMesh();
      for (FaceNodes3d f : allFaces) {
         if (!f.isOverlapping() &&
            !f.hasSelfAttachedNode() &&
            !f.isSelfAttachedToFace()) {
            FemNode3d[][] triangles = f.triangulate();
            boolean triangulatedQuad =
               (triangles.length == 2 && triangles[0][0] == triangles[1][0]);
            for (int i = 0; i < triangles.length; i++) {
               FemNode3d[] tri = triangles[i];
               Vertex3d[] vtxs = new Vertex3d[3];
               for (int j = 0; j < 3; j++) {
                  FemNode3d node = tri[j];
                  if ((vtxs[j] = myNodeVertexMap.get(node)) == null) {
                     Vertex3d vtx = new Vertex3d (new Point3d(node.getPosition()));
                     mesh.addVertex (vtx);
                     myVertexAttachments.add (
                        new PointParticleAttachment (node, null));
                     myNumSingleAttachments++;
                     myNodeVertexMap.put (node, vtx);
                     vtxs[j] = vtx;
                  }
               }
               Face face = mesh.addFace(vtxs);
               if (triangulatedQuad && i == 0) {
                  face.setFirstQuadTriangle(true);
               }
            }
         }
      }

      finalizeSurfaceBuild();

      //throwable = null;
   }

   protected void updateVertexColors() {
      
      if (mySurfaceRendering != SurfaceRender.Stress &&
         mySurfaceRendering != SurfaceRender.Strain) {
         return;
      }

      if (myStressPlotRanging == Ranging.Auto) {
         myStressPlotRange.merge (myFem.getNodalPlotRange(mySurfaceRendering));
      } 

      RenderProps rprops = getRenderProps();
      float alpha = (float)rprops.getAlpha();

      MeshBase mesh = getMesh();
      
      double sval = 0;
      for (int i=0; i<myVertexAttachments.size(); i++) {
         PointAttachment attacher = myVertexAttachments.get(i);
         sval = 0;
         if (attacher instanceof PointFem3dAttachment) {
            PointFem3dAttachment pfa = (PointFem3dAttachment)attacher;
            FemNode[] nodes = pfa.getNodes();
            VectorNd weights = pfa.getCoordinates();
            for (int j=0; j<nodes.length; j++) {
               if (nodes[j] instanceof FemNode3d) { // paranoid!
                  FemNode3d node = (FemNode3d)nodes[j];
                  double w = weights.get(j);
                  if (mySurfaceRendering == SurfaceRender.Strain) {
                     sval += w*node.getVonMisesStrain();
                  } else if (mySurfaceRendering == SurfaceRender.Stress) {
                     sval += w*node.getVonMisesStress();
                  }
               }
            }
         }
         else if (attacher instanceof PointParticleAttachment) {
            PointParticleAttachment ppa = (PointParticleAttachment)attacher;
            FemNode3d node = (FemNode3d)ppa.getParticle();
            if (mySurfaceRendering == SurfaceRender.Strain) {
               sval = node.getVonMisesStrain();
            } else if (mySurfaceRendering == SurfaceRender.Stress) {
               sval = node.getVonMisesStress();
            }
         }
         double smin = myStressPlotRange.getLowerBound();
         double srng = myStressPlotRange.getRange();
         double c = (sval-smin)/srng;
         c = Math.max (0, Math.min (c, 1.0));
         myColorMap.getRGB(c, colorArray);
         mesh.setVertexColor(i, colorArray[0], colorArray[1], colorArray[2], alpha);
      }
   }

   private void writeVertexInfo (PrintWriter pw, Vertex3d vtx, NumberFormat fmt) {
      PointAttachment pa = null;
      if (vtx.getIndex() < myVertexAttachments.size()) {
         pa = getAttachment(vtx.getIndex());
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
                  (PointParticleAttachment)getAttachment(vidx);
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
      myNodeVertexMap = new HashMap<FemNode3d,Vertex3d>();
      myNumSingleAttachments = 0;
      ArrayList<Vertex3d> vtxList = new ArrayList<Vertex3d>();
      rtok.nextToken();
      while (rtok.tokenIsWord("f")) {
         vtxList.clear();
         rtok.nextToken();
         while (rtok.tokenIsInteger()) {
            int nnum = (int)rtok.lval;
            FemNode3d node = getNodeFromNumber (rtok, nnum);
            Vertex3d vtx = myNodeVertexMap.get(node);
            if (vtx == null) {
               vtx = new Vertex3d (new Point3d(node.getPosition()));
               myNodeVertexMap.put(node, vtx);
               myVertexAttachments.add (
                  new PointParticleAttachment(node, null));
               myNumSingleAttachments++;
               mesh.addVertex(vtx);
            }
            vtxList.add(vtx);
            rtok.nextToken();
         }
         mesh.addFace(vtxList.toArray(new Vertex3d[0]));
      }
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
               myVertexAttachments.add (null);
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
               }
               mesh.addVertex (vtx);
               myVertexAttachments.add (ax);
            }
         }
         else if (rtok.sval.equals ("f")) {
            vtxList.clear();
            rtok.nextToken();
            while (rtok.tokenIsInteger()) {
               int vnum = (int)rtok.lval;
               if (vnum > mesh.getNumVertices()) {
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

   protected void writeAttachment (
      PointAttachment attacher, PrintWriter pw, NumberFormat fmt,
      CompositeComponent ancestor)
         throws IOException {
      pw.print ("[ ");
      if (attacher instanceof PointParticleAttachment) {
         PointParticleAttachment ppa = (PointParticleAttachment)attacher;
         FemNode node = (FemNode)ppa.getParticle();
         pw.print (ComponentUtils.getWritePathName (ancestor, node) + " 1 ");
      }
      else if (attacher instanceof PointFem3dAttachment) {
         PointFem3dAttachment pfa = (PointFem3dAttachment)attacher;
         FemNode[] nodes = pfa.getNodes();
         VectorNd weights = pfa.getCoordinates();
         for (int i=0; i<nodes.length; i++) {
            pw.print (
               ComponentUtils.getWritePathName (ancestor, nodes[i]) +
               " " + fmt.format(weights.get(i)) + " ");
         }
      }
      pw.println ("]");
   }

   protected void scanAttachment (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

      ArrayList<Double> weights = new ArrayList<Double>();
      rtok.scanToken ('[');
      tokens.offer (ScanToken.BEGIN);
      while (ScanWriteUtils.scanAndStoreReference (rtok, tokens)) {
         weights.add (rtok.scanNumber());
      }
      if (rtok.ttype != ']') {
         throw new IOException ("Expected ']', got " + rtok);
      }
      //		      while (rtok.nextToken() != ']') {
      //		         rtok.pushBack();
      //		         ScanWriteUtils.scanReferenceToken (rtok, tokens);
      //		         weights.add (rtok.scanNumber());
      //		      }
      tokens.offer (ScanToken.END); // add null terminator
      if (weights.size() == 1) {
         PointParticleAttachment ppa = new PointParticleAttachment ();
         myVertexAttachments.add (ppa);
      }
      else {
         PointFem3dAttachment pfa = new PointFem3dAttachment();
         tokens.offer (new ObjectToken(ArraySupport.toDoubleArray (weights)));
         myVertexAttachments.add (pfa);
      }
   }

   @Override
   public void scan(ReaderTokenizer rtok, Object ref) throws IOException {
      SurfaceRender shad1 = mySurfaceRendering;
      super.scan(rtok, ref);
      SurfaceRender shad2 = mySurfaceRendering;
      if (shad1 != shad2) {
         System.out.println("Different shading");
      }
      if (mySurfaceRenderingMode == PropertyMode.Inherited) {
         System.out.println("Why isn't it explicit?");
      }
      
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName(rtok, "surfaceMesh")) {
         isSurfaceMesh = rtok.scanBoolean();
         return true;
      } else if (scanAttributeName (rtok, "attachments")) {
         tokens.offer (new StringToken ("attachments", rtok.lineno()));
         rtok.scanToken ('[');
         while (rtok.nextToken() != ']') {
            rtok.pushBack();
            scanAttachment (rtok, tokens);
         }
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "attachments")) {
         for (int i=0; i<myVertexAttachments.size(); i++) {
            PointAttachment va = myVertexAttachments.get(i);
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
         buildNodeVertexMap();
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
         throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.println("surfaceMesh=" + isSurfaceMesh());
      pw.println ("attachments=["); 
      IndentingPrintWriter.addIndentation (pw, 2);
      for (int i=0; i<myVertexAttachments.size(); i++) {
         writeAttachment (myVertexAttachments.get(i), pw, fmt, ancestor);
      }
      IndentingPrintWriter.addIndentation (pw, -2); 
      pw.println ("]");
   }

   @Override
   public void connectToHierarchy () {
      // XXX not sure what to do here. Probably don't want to add back
      // references to all master components, but then we need a way to remove
      // masters from the attachments when masters disappear
      super.connectToHierarchy ();
   }

   @Override
   public void disconnectFromHierarchy() {
      // XXX not sure what to do here ... see comment in connectToParent()
      super.disconnectFromHierarchy();
   }

   @Override
   public FemMeshComp copy(int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FemMeshComp fm = (FemMeshComp)super.copy(flags, copyMap);

      HashMap<Vertex3d,Vertex3d> vertMap = null;
      if (getMesh() != fm.getMesh()) {
         vertMap = new HashMap<Vertex3d,Vertex3d>(myMeshInfo.numVertices());
         for (int i=0; i<myMeshInfo.numVertices(); i++) {
            vertMap.put(getVertex(i), fm.getVertex(i));
         }
      }

      fm.myVertexAttachments = 
         new ArrayList<PointAttachment>(myVertexAttachments.size());
      for (PointAttachment pa : myVertexAttachments) {
         PointAttachment newPa = pa.copy(flags, copyMap);
         fm.myVertexAttachments.add(newPa);
      }
      fm.buildNodeVertexMap();

//      fm.myEdgeVtxs = new HashMap<EdgeDesc,Vertex3d[]>(myEdgeVtxs.size());
//      for (Entry<EdgeDesc,Vertex3d[]> het : myEdgeVtxs.entrySet()) {
//         het.getKey();
//         Vertex3d[] oldVerts = het.getValue();
//         EdgeDesc newEdge = het.getKey().copy(vertMap);
//         Vertex3d[] newVerts = new Vertex3d[oldVerts.length];
//
//         if (vertMap != null) {
//            for (int i=0; i<newVerts.length; i++) {
//               newVerts[i] = vertMap.get(oldVerts[i]);
//            }
//         } else {
//            for (int i=0; i<newVerts.length; i++) {
//               newVerts[i] = oldVerts[i];
//            }
//         }
//         fm.myEdgeVtxs.put(newEdge, newVerts);
//      }

      fm.isSurfaceMesh = isSurfaceMesh();

      return fm;
   }

   public boolean isSurfaceMesh() {
      return isSurfaceMesh;
   }

   protected void markSurfaceMesh(boolean set) {
      MeshBase mesh = getMesh();
      if (mesh != null && !(mesh instanceof PolygonalMesh)) {
         throw new IllegalArgumentException("Mesh must be a PolygonalMesh to be set as a surface");
      }
      isSurfaceMesh = set;
   }
   
   /**
    * Check if this mesh depends on a particular node
    */
   public boolean hasNodeDependency(FemNode3d node) {
      
      for (PointAttachment pa : myVertexAttachments) {
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
   public boolean isDeformable () {
      return true;
   }

   @Override
   public double getMass () {
      return myFem.getMass ();
   }
   
   public void getVertexMasters (List<ContactMaster> mlist, Vertex3d vtx) {
      PointAttachment pa = getAttachment(vtx.getIndex());
      if (pa instanceof PointFem3dAttachment) {
         PointFem3dAttachment pfa = (PointFem3dAttachment)pa;
         FemNode[] masters = pfa.getNodes();
         for (int j=0; j<masters.length; j++) {
            mlist.add (new ContactMaster (masters[j], pfa.getCoordinate(j)));
         }
      }
      else {
         PointParticleAttachment ppa = (PointParticleAttachment)pa;
         mlist.add (new ContactMaster ((FemNode3d)ppa.getParticle(), 1));
      }      
   }
   
   public boolean containsContactMaster (CollidableDynamicComponent comp) {
      if (myNodeVertexMap != null && comp instanceof FemNode3d &&
          myNodeVertexMap.get((FemNode3d)comp) != null) {
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
   
   // end Collidable implementation

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
      for (int i=0; i<vtxs.length; i++) {
         PointAttachment va = myVertexAttachments.get(vtxs[i].getIndex());
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

}
