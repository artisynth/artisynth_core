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
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import maspack.geometry.BVFeatureQuery;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.MeshBase;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyMode;
import maspack.render.MouseRayEvent;
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
import artisynth.core.mechmodels.CollisionData;
import artisynth.core.mechmodels.DynamicAttachment;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.PointList;
import artisynth.core.mechmodels.PointParticleAttachment;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.util.ObjectToken;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import artisynth.core.workspace.PullController.Pullable;

/**
 * Describes a surface mesh that is "skinned" onto an FEM, such that its vertex
 * positions are determined by weighted combinations of FEM node positions.
 **/
public class FemMesh extends FemMeshBase implements Pullable, Collidable {

   protected static double EPS = 1e-10;
   protected ArrayList<PointAttachment> myVertexAttachments;
   HashMap<EdgeDesc,Vertex3d[]> myEdgeVtxs;
   private boolean isSurfaceMesh;

   private float[] colorArray = new float[3];

   public FemMesh () {
      super();
      myVertexAttachments = new ArrayList<PointAttachment>();
   }

   public FemMesh (FemModel3d fem) {
      this();
      myFem = fem;
      isSurfaceMesh = false;
   }

   public FemMesh(FemModel3d fem, String name) {
      this(fem);
      setName(name);
   }

   public int numAttachments () {
      return myVertexAttachments.size();
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
      myEdgeVtxs = new HashMap<EdgeDesc,Vertex3d[]>();
      myVertexAttachments.clear();
   }

   /** 
    * Finalize data structures after vertices and faces have been added.
    */
   void finalizeSurfaceBuild() {
      super.finalizeSurfaceBuild();
      myEdgeVtxs.clear();
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
      return (FemNode3d)((FemMeshVertex)he.getHead()).getPoint();
   }

   /**
    * Find the FEM element corresponding to a surface face.
    */
   FemElement3d getFaceElement (Face face) {

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

   private FemMeshVertex createVertex (FemMeshVertex v) {
      FemMeshVertex vtx = new FemMeshVertex (v.getPoint(), v.getPosition());
      PointParticleAttachment attacher =
         new PointParticleAttachment ((FemNode3d)v.getPoint(), null);
      myVertexAttachments.add (attacher);
      getMesh().addVertex (vtx);
      return vtx;
   }

   private void addNodeCoords (
      Vector3d coords, double s0, double s1, double s2, FemElement3d elem, 
      FemMeshVertex v0, FemMeshVertex v1, FemMeshVertex v2) {

      Vector3d coordsv = new Vector3d();
      int lidx = elem.getLocalNodeIndex (v0.myPnt);
      if (elem instanceof QuadpyramidElement && lidx == 4) {
         // special handling required for the apex of a QuadtetPyramid, since
         // the apex is formed by condensing the corner nodes of a brick. That
         // means natural coordinates are actually in a rectangular brick
         // coordinate system.

         // get the coordinates for the top brick node corresponding to v1,
         // and find the weight for it from s0, s1, and s2.
         elem.getNodeCoords (coordsv, elem.getLocalNodeIndex (v1.myPnt));
         coordsv.z = 1;
         coords.scaledAdd (s0*s1/(s1+s2), coordsv);

         if (v2 != null) {
            // get the coordinates for the top brick node corresponding to v2,
            // and fine the weight for it as well.
            elem.getNodeCoords (coordsv, elem.getLocalNodeIndex (v2.myPnt));
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
      FemMeshVertex v0, FemMeshVertex v1, FemMeshVertex v2) {

      Vector3d coords = new Vector3d();

      addNodeCoords (coords, s0, s1, s2, elem, v0, v1, v2);
      addNodeCoords (coords, s1, s0, s2, elem, v1, v0, v2);

      if (v2 != null) {
         addNodeCoords (coords, s2, s1, s0, elem, v2, v1, v0);
      }

      ArrayList<FemNode> nodes = new ArrayList<FemNode>();
      ArrayList<Double> weights = new ArrayList<Double>();

      Point3d pos = new Point3d();
      for (int i=0; i<elem.numNodes(); i++) {
         double w = elem.getN (i, coords);
         if (Math.abs(w) > EPS) {
            FemNode3d n = elem.getNodes()[i];
            nodes.add (n);
            weights.add (w);
            pos.scaledAdd (w, n.getPosition());
         }
      }

      Vertex3d vtx = new Vertex3d (pos);
      PointFem3dAttachment attacher = new PointFem3dAttachment();
      attacher.setNodes (nodes, weights);
      myVertexAttachments.add (attacher);

      getMesh().addVertex (vtx);
      return vtx;
   }

   private Vertex3d[] collectEdgeVertices (
      FemMeshVertex v0, FemMeshVertex v1, FemElement3d elem, int res) {

      int numv = res+1;
      // numv is total number of vertices along the edge, including
      // the end vertices. The number of intermediate vertices is numv-2
      Vertex3d[] vtxs = myEdgeVtxs.get (new EdgeDesc (v0, v1));
      if (vtxs == null) {
         vtxs = new Vertex3d[numv];
         vtxs[0] = v0;
         vtxs[numv-1] = v1;
         // create vertices along edge
         for (int i=1; i<numv-1; i++) {
            double s1 = i/(double)res;
            vtxs[i] = createVertex (1-s1, s1, 0, elem, v0, v1, null);
         }
         myEdgeVtxs.put (new EdgeDesc (v0, v1), vtxs);
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

   public static FemMesh createEmbedded(FemMesh surf, MeshBase mesh) {
      if (surf == null || surf.myFem == null) {
         throw new IllegalArgumentException("Cannot determine proper FEM");
      }
      return createEmbedded(surf, mesh, surf.myFem);
   }
   
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
   
   public void setVertexAttachment(int vidx, double [] weights, FemNode3d[] nodes) {
      if (weights.length > 1) {
         PointFem3dAttachment pattacher = new PointFem3dAttachment();
         pattacher.setNodes (nodes, weights);
         setVertexAttachment(vidx, pattacher);
      } else if (weights.length == 1) {
         PointParticleAttachment attacher = new PointParticleAttachment(nodes[0], null);
         setVertexAttachment(vidx, attacher);
      }
      
   }

   public static FemMesh createEmbedded (
      FemMesh surf, MeshBase mesh, FemModel3d fem) {

      double reduceTol = 1e-8;

      ArrayList<FemNode> nodes = new ArrayList<FemNode>();
      ArrayList<Double> weights = new ArrayList<Double>();

      if (surf == null) {
         surf = new FemMesh(fem);
      }
      surf.setMesh (mesh);
      ArrayList<Vertex3d> verts = mesh.getVertices();

      surf.myVertexAttachments.clear();
      for (int i=0; i<verts.size(); i++) {
         // this could works very similarly to the code that adds
         // marker points into a mesh
         Vertex3d vtx = verts.get(i);
         if (vtx instanceof FemMeshVertex) {
            nodes.clear();
            nodes.add(((FemMeshVertex)vtx).getPoint());
            weights.clear();
            weights.add(1.0);
         } else {
            FemElement3d elem = surf.myFem.findContainingElement (vtx.pnt);
            Point3d newLoc = new Point3d(vtx.pnt);
            if (elem == null) {
               // won't use newLoc since we're not projecting vertex onto FEM
               elem = surf.myFem.findNearestSurfaceElement (newLoc, vtx.pnt);
            }
            VectorNd coords = new VectorNd (elem.numNodes());
            Vector3d c3 = new Vector3d();
            boolean converged = elem.getNaturalCoordinatesRobust(c3, vtx.pnt, 1000);
            if (!converged) {
               System.err.println(
                  "Warning: system did not converge when computing natural coordinates");
            }
            for (int j=0; j<elem.numNodes(); j++) {
               coords.set (j, elem.getN (j, c3));
            }

            nodes.clear();
            weights.clear();
            for (int k=0; k<coords.size(); k++) {
               if (Math.abs(coords.get(k)) >= reduceTol) {
                  nodes.add (elem.getNodes()[k]);
                  weights.add (coords.get(k));                            
               }
            }
         }

         if (weights.size() > 1) {
            PointFem3dAttachment attacher = new PointFem3dAttachment();
            attacher.setNodes (nodes, weights);
            surf.myVertexAttachments.add (attacher);
         } else if (weights.size() == 1) {
            PointParticleAttachment attacher = new PointParticleAttachment(nodes.get(0), null);
            surf.myVertexAttachments.add(attacher);
         }
      }

      // we manually control display lists, so this should work
      mesh.setUseDisplayList(true);

      return surf;
   }

   public static FemMesh createEmbedded (FemModel3d fem, MeshBase mesh) {
      return createEmbedded (new FemMesh(fem), mesh);
   }

   protected void createFineSurface (int resolution, 
      ElementFilter efilter,
      Map<FemNode3d,FemMeshVertex> nodeVtxMap) {

      // build from nodes/element filter
      createSurface(efilter, nodeVtxMap);

      if (resolution < 2) {
         // if resolution < 2, just return regular surface
         return;
      }

      // Note: can no longer rely on the surface mesh consisting of only 
      // FemMeshVertex
      // PolygonalMesh baseMesh = myFem.getSurfaceMesh();
      PolygonalMesh baseMesh = (PolygonalMesh)getMesh();    // since previously built

      int numv = resolution+1; // num vertices along the edge of each sub face
      initializeSurfaceBuild();

      // get newly empty mesh
      PolygonalMesh surfMesh = (PolygonalMesh)getMesh();

      ArrayList<Face> baseFaces = baseMesh.getFaces();
      ArrayList<Vertex3d> baseVerts = baseMesh.getVertices();
      if (nodeVtxMap == null) {
         nodeVtxMap = new HashMap<FemNode3d, FemMeshVertex>(baseVerts.size());
      } else {
         nodeVtxMap.clear();  // clear map to start building new mesh
      }

      for (int k=0; k<baseVerts.size(); k++) {
         FemMeshVertex newVtx = createVertex ((FemMeshVertex)baseVerts.get(k));
         nodeVtxMap.put((FemNode3d)newVtx.getPoint(), newVtx);
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
         FemMeshVertex v0 = (FemMeshVertex)he.getHead();
         he = he.getNext();
         FemMeshVertex v1 = (FemMeshVertex)he.getHead();
         he = he.getNext();
         FemMeshVertex v2 = (FemMeshVertex)he.getHead();

         subv.set (0, 0, getVertex(v0.getIndex()));
         subv.set (0, numv-1, getVertex(v2.getIndex()));
         subv.set (numv-1, numv-1, getVertex(v1.getIndex()));

         Vertex3d[] vtxs01 = collectEdgeVertices (v0, v1, elem, resolution);
         for (int i=1; i<numv-1; i++) {
            subv.set (i, i, vtxs01[i]);
         }
         Vertex3d[] vtxs02 = collectEdgeVertices (v0, v2, elem, resolution);
         for (int j=1; j<numv-1; j++) {
            subv.set (0, j, vtxs02[j]);
         }
         Vertex3d[] vtxs21 = collectEdgeVertices (v2, v1, elem, resolution);
         for (int i=1; i<numv-1; i++) {
            subv.set (i, numv-1, vtxs21[i]);
         }

         for (int i=1; i<numv-1; i++) {
            for (int j=i+1; j<numv-1; j++) {
               double s1 = i/(double)resolution;
               double s0 = 1-j/(double)resolution;
               Vertex3d vtx =
                  createVertex (s0, s1, 1-s0-s1, elem, v0, v1, v2);
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

   //   public static FemMesh createSurface (FemModel3d fem, int resolution) {
   //      return createSurface (new FemMesh (fem), resolution);
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
                  FemModel3d.containsNode(n1, pfa.getMasters())) {
                  nodes.add(slaveNode);
                  double w = pfa.getCoordinates().get(0);
                  if (n0 == pfa.getMasters()[0]) {
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

   public void createSurface (ElementFilter efilter, 
      Map<FemNode3d,FemMeshVertex> surfaceNodeMap) {

      initializeSurfaceBuild();
      if (surfaceNodeMap == null) {
         surfaceNodeMap = new HashMap<FemNode3d,FemMeshVertex>();
      }
      else {
         surfaceNodeMap.clear();
      }

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
      for (FemElement3d e : myFem.getElements()) {
         if (efilter.elementIsValid(e)) {
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
               FemMeshVertex[] vtxs = new FemMeshVertex[3];
               for (int j = 0; j < 3; j++) {
                  FemNode3d node = tri[j];
                  if ((vtxs[j] = surfaceNodeMap.get(node)) == null) {
                     FemMeshVertex vtx = new FemMeshVertex (node);
                     mesh.addVertex (vtx);
                     myVertexAttachments.add (
                        new PointParticleAttachment (node, null));
                     surfaceNodeMap.put (node, vtx);
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
            FemNode[] nodes = pfa.getMasters();
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
         myColorMap.getRGB(sval/myStressPlotRange.getRange(), colorArray);
         mesh.setVertexColor(i, colorArray[0], colorArray[1], colorArray[2], alpha);
      }
   }

   // public void scaleDistance (double s) {
   //    super.scaleDistance (s);
   //    // shouldn't need to change anything since everything is weight-based
   //    updatePosState();
   // }  

   // public void transformGeometry (
   //    AffineTransform3dBase X, TransformableGeometry topObject, int flags) {

   //    if ((flags & TransformableGeometry.SIMULATING) != 0) {
   //       return;
   //    }
   // }

   protected void writeAttachment (
      PointAttachment attacher, PrintWriter pw, NumberFormat fmt,
      CompositeComponent ancestor)
         throws IOException {
      pw.print ("[ ");
      if (attacher instanceof PointParticleAttachment) {
         PointParticleAttachment ppa = (PointParticleAttachment)attacher;
         FemNode node = (FemNode)ppa.getParticle();
         pw.print (
            ComponentUtils.getWritePathName (ancestor, node) +
            " 1 ");
      }
      else if (attacher instanceof PointFem3dAttachment) {
         PointFem3dAttachment pfa = (PointFem3dAttachment)attacher;
         FemNode[] nodes = pfa.getMasters();
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
               pfa.setNodes (nodes, coords);
            }
         }
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
   public FemMesh copy(int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FemMesh fm = (FemMesh)super.copy(flags, copyMap);

      HashMap<Vertex3d,Vertex3d> vertMap = null;
      if (getMesh() != fm.getMesh()) {
         vertMap = new HashMap<Vertex3d,Vertex3d>(myMeshInfo.numVertices());
         for (int i=0; i<myMeshInfo.numVertices(); i++) {
            vertMap.put(getVertex(i), fm.getVertex(i));
         }
      }

      fm.myVertexAttachments = new ArrayList<PointAttachment>(myVertexAttachments.size());
      for (PointAttachment pa : myVertexAttachments) {
         PointAttachment newPa = pa.copy(flags, copyMap);
         fm.myVertexAttachments.add(newPa);
      }

      fm.myEdgeVtxs = new HashMap<EdgeDesc,Vertex3d[]>(myEdgeVtxs.size());
      for (Entry<EdgeDesc,Vertex3d[]> het : myEdgeVtxs.entrySet()) {
         het.getKey();
         Vertex3d[] oldVerts = het.getValue();
         EdgeDesc newEdge = het.getKey().copy(vertMap);
         Vertex3d[] newVerts = new Vertex3d[oldVerts.length];

         if (vertMap != null) {
            for (int i=0; i<newVerts.length; i++) {
               newVerts[i] = vertMap.get(oldVerts[i]);
            }
         } else {
            for (int i=0; i<newVerts.length; i++) {
               newVerts[i] = oldVerts[i];
            }
         }
         fm.myEdgeVtxs.put(newEdge, newVerts);
      }

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

   // Pullable interface
   protected class SkinOriginData {
      PointAttachment[] attachments;
      Vector3d weights;

      public SkinOriginData(Face face, Vector3d duv) {
         int[] vidxs = face.getVertexIndices();
         attachments = new PointAttachment[3];
         attachments[0] = myVertexAttachments.get(vidxs[0]);
         attachments[1] = myVertexAttachments.get(vidxs[1]);
         attachments[2] = myVertexAttachments.get(vidxs[2]);
         weights = new Vector3d(1 - duv.y - duv.z, duv.y, duv.z);
      }

      public void computeOrig(Point3d pnt) {
         pnt.setZero();
         Point3d tmp = new Point3d();
         for (int i=0; i<attachments.length; i++) {
            attachments[i].computePosState(tmp);
            pnt.scaledAdd(weights.get(i), tmp);
         }
      }

      public void applyForce(Point3d pnt, Vector3d f) {
         // XXX zero out external force? 
         for (int i=0; i<attachments.length; i++) {
            DynamicComponent[] comps = attachments[i].getMasters();
            for (int j=0; j<comps.length; j++) {
               if (comps[j] instanceof Point) {
                  Point p = (Point)comps[j];
                  p.setExternalForce(Vector3d.ZERO);
               } else if (comps[j] instanceof Frame) {
                  // should never be here for regular FEM
                  Frame fr = (Frame)comps[j];
                  fr.setExternalForce(Wrench.ZERO);
               }
            }
         }
         for (int i=0; i<attachments.length; i++) {
            attachments[i].addScaledExternalForce(pnt, weights.get(i), f);
         }
      }
   }

   @Override
   public boolean isPullable() {
      return true;
   }

   @Override
   public Object getOriginData(MouseRayEvent ray) {
      SkinOriginData origin = null;
      BVFeatureQuery query = new BVFeatureQuery();
      Point3d isectPoint = new Point3d();
      Vector3d duv = new Vector3d();
      Face faceHit = query.nearestFaceAlongRay (
         isectPoint, duv, (PolygonalMesh)getMesh(),
         ray.getRay().getOrigin(), ray.getRay().getDirection());

      if (faceHit != null) {
         origin = new SkinOriginData(faceHit, duv);
      }
      return origin;
   }

   @Override
   public Point3d getOriginPoint(Object data) {
      SkinOriginData orig = (SkinOriginData)data;
      Point3d pnt = new Point3d();
      orig.computeOrig(pnt);
      return pnt;
   }

   @Override
   public double getPointRenderRadius() {
      return 0;
   }

   @Override
   public void applyForce(Object orig, Vector3d force) {
      SkinOriginData origin = (SkinOriginData)orig;
      Point3d loc = new Point3d();
      origin.computeOrig(loc);
      origin.applyForce(loc, force);
   }

   // Collidable implementation
   
   private PolygonalMesh getCollisionMesh() {
      MeshBase mesh = getMesh ();
      if (mesh instanceof PolygonalMesh) {
         return (PolygonalMesh)mesh;
      }
      return null;
   }
   
   @Override
   public CollisionData createCollisionData () {
      return new EmbeddedCollisionData (myFem, this);
   }

   @Override
   public boolean isCollidable () {
      PolygonalMesh mesh = getCollisionMesh ();
      if (mesh != null) {
         return true;
      }
      return false;
   }

   @Override
   public double getMass () {
      return myFem.getMass ();
   }

}
