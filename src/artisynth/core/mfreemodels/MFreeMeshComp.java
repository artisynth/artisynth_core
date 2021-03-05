/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemMeshBase;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.FemModel.Ranging;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.PointFem3dAttachment;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.CollidableBody;
import artisynth.core.mechmodels.CollidableDynamicComponent;
import artisynth.core.mechmodels.CollisionHandler;
import artisynth.core.mechmodels.ContactMaster;
import artisynth.core.mechmodels.ContactPoint;
import artisynth.core.mechmodels.DistanceGridComp;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointAttachable;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.PointParticleAttachment;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentChangeEvent.Code;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.util.ArtisynthIO;
import artisynth.core.util.ObjectToken;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.DistanceGrid;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.Shading;
import maspack.util.ArraySupport;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Describes a surface mesh that is "skinned" onto an FEM, such that its vertex
 * positions are determined by weighted combinations of FEM node positions.
 **/
public class MFreeMeshComp extends FemMeshComp implements CollidableBody, PointAttachable {

   protected MFreeModel3d myModel;

   protected static double EPS = 1e-10;
   protected ArrayList<PointAttachment> myVertexAttachments;
   // myNodeVertexMap maps each node used by this MFreeMeshComp onto either
   // a single vertex which it completely controls, or the dummy variable 
   // NO_SINGLE_VERTEX if there is no such vertex.
   protected HashMap<MFreeNode3d,Vertex3d> myNodeVertexMap;
   protected static final Vertex3d NO_SINGLE_VERTEX = new Vertex3d();
   protected static final Collidability DEFAULT_COLLIDABILITY =
      Collidability.ALL;   

   HashMap<EdgeDesc,Vertex3d[]> myEdgeVtxs;
   private boolean isSurfaceMesh;
   private int myNumSingleAttachments;
   protected Collidability myCollidability = DEFAULT_COLLIDABILITY;
   protected int myCollidableIndex;

   private float[] colorArray = new float[3];

   public static PropertyList myProps =
      new PropertyList (MFreeMeshComp.class, FemMeshBase.class);

   static {
      myProps.add (
         "collidable", 
         "sets the collidability of the mesh", DEFAULT_COLLIDABILITY);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public MFreeMeshComp () {
      super();
      myVertexAttachments = new ArrayList<PointAttachment>();
   }

   public MFreeMeshComp (MFreeModel3d mfree) {
      this();
      myModel = mfree;
      isSurfaceMesh = false;
   }

   public MFreeMeshComp(MFreeModel3d fem, String name) {
      this(fem);
      setName(name);
   }

   @Override
   public void setSurfaceRendering(SurfaceRender mode) {
      SurfaceRender oldMode = getSurfaceRendering();
      super.setSurfaceRendering(mode);

      if (oldMode != mode) {
         if (myModel != null) { // paranoid: myModel should always be non-null here
            myModel.updateInternalNodalStressSettings();
            myModel.updateInternalNodalStrainSettings();
            if (mode.usesStressOrStrain()) {
               myModel.updateStressAndStiffness();
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
      if (myModel != null && myModel.isSelected() ) {
         flags |= Renderer.HIGHLIGHT;
      }
      super.render(renderer, flags);
   }

   public int numVertexAttachments () {
      return myVertexAttachments.size();
   }

   public PointAttachment getVertexAttachment(Vertex3d vtx) {
      return getVertexAttachment (vtx.getIndex());
   }

   public PointAttachment getVertexAttachment (int idx) {
      return myVertexAttachments.get (idx);
   }

   /** 
    * Initialize data structures prior to adding vertices and faces to
    * this surface.
    */
   protected void initializeSurfaceBuild() {
      super.initializeSurfaceBuild();
      //myEdgeVtxs = new HashMap<EdgeDesc,Vertex3d[]>();
      myVertexAttachments = new ArrayList<PointAttachment>();
      myNodeVertexMap = null;
   }

   protected void buildNodeVertexMap() {
      myNodeVertexMap = new HashMap<MFreeNode3d,Vertex3d>();
      myNumSingleAttachments = 0;
      for (int i=0; i<myVertexAttachments.size(); i++) {
         PointAttachment pa = myVertexAttachments.get(i);
         if (pa instanceof PointParticleAttachment) {
            MFreeNode3d node = 
               (MFreeNode3d)((PointParticleAttachment)pa).getParticle();
            myNodeVertexMap.put (node, getMesh().getVertex(i));
            myNumSingleAttachments++;
         }
         else if (pa instanceof PointFem3dAttachment) {
            PointFem3dAttachment pfa = (PointFem3dAttachment)pa;
            for (FemNode node : pfa.getNodes()) {
               if (myNodeVertexMap.get(node) == null) {
                  myNodeVertexMap.put ((MFreeNode3d)node, NO_SINGLE_VERTEX);
               }
            }
         }
      }
   }

   /** 
    * Finalize data structures after vertices and faces have been added.
    */
   protected void finalizeSurfaceBuild() {
      super.finalizeSurfaceBuild();
      if (myNodeVertexMap == null) {
         buildNodeVertexMap();
      }
      //myEdgeVtxs.clear();

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

   protected MFreeNode3d getEdgeNode (HalfEdge he) {
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
      return myNumSingleAttachments == getMesh().numVertices();
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
   public MFreeNode3d getNodeForVertex (Vertex3d vtx) {
      int idx = vtx.getIndex();
      if (idx < myVertexAttachments.size()) {
         return getNodeForVertex (myVertexAttachments.get(idx));
      }
      else {
         return null;
      }
   }

   private MFreeNode3d getNodeForVertex (PointAttachment pa) {
      if (pa instanceof PointParticleAttachment) {
         Particle p = ((PointParticleAttachment)pa).getParticle();
         if (p instanceof MFreeNode3d) {
            MFreeNode3d node = (MFreeNode3d)p;
            if (node.getGrandParent() == myModel) {
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
   public Vertex3d getVertexForNode (MFreeNode3d node) {
      if (myNodeVertexMap == null) {
         return null;
      }
      Vertex3d vtx = myNodeVertexMap.get (node);
      if (vtx == NO_SINGLE_VERTEX) {
         vtx = null;
      }
      return vtx;
   }

   private void addVertexNodes (HashSet<MFreeNode3d> nodes, Vertex3d vtx) {

      PointAttachment pa = getVertexAttachment(vtx.getIndex());
      if (pa instanceof PointFem3dAttachment) {
         PointFem3dAttachment pfa = (PointFem3dAttachment)pa;
         FemNode[] masters = pfa.getNodes();
         for (int j=0; j<masters.length; j++) {
            nodes.add ((MFreeNode3d)masters[j]);
         }
      }
      else {
         PointParticleAttachment ppa = (PointParticleAttachment)pa;
         nodes.add ((MFreeNode3d)ppa.getParticle());
      }      
   }

   public MFreeElement3d getFaceElement (Face face) {

      // Form the intersection of all the element dependencies of all three
      // nodes associated with the face. If the face is on the surface,
      // there should be only one element left.
      HashSet<MFreeNode3d> nodes = new HashSet<MFreeNode3d>();
      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he = he0;
      do {
         addVertexNodes (nodes, he.getHead());
         he = he.getNext();
      }
      while (he != he0);

      HashSet<FemElement3d> elems = null;
      for (MFreeNode3d node : nodes) {
         if (elems == null) {
            elems = new HashSet<>(getAdjacentVolumetricElems(node));
         }
         else {
            elems.retainAll (getAdjacentVolumetricElems(node));
         }
      }
      if (elems.size() != 1) {
         MFreeElement3d[] elemArray = elems.toArray(new MFreeElement3d[0]);
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
      return (MFreeElement3d)elems.toArray()[0];
   }

   
   
   public static MFreeMeshComp createEmbedded(MFreeMeshComp surf, MeshBase mesh) {
      if (surf == null || surf.myModel == null) {
         throw new IllegalArgumentException("Cannot determine proper FEM");
      }
      return createEmbedded(surf, mesh, surf.myModel);
   }

   public MFreeModel3d getFem() {
      return myModel;
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
      int vidx, double [] weights, MFreeNode3d[] nodes) {
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
   
   /**
    * Assumes the mesh is in "rest" coordinates, not current coordinates
    * @param surf mfree surface to populate (or null to create one)
    * @param mesh mesh to embed
    * @param mfree model to embed mesh in
    * @return populated or created mesh
    */
   public static MFreeMeshComp createEmbedded (
      MFreeMeshComp surf, MeshBase mesh, MFreeModel3d mfree) {

      double reduceTol = 1e-8;

      ArrayList<MFreeNode3d> nodes = new ArrayList<MFreeNode3d>();
      VectorNd weights = new VectorNd();

      if (surf == null) {
         surf = new MFreeMeshComp(mfree);
      }
      surf.setMesh (mesh);
      ArrayList<Vertex3d> verts = mesh.getVertices();

      Point3d coords = new Point3d();
      MLSShapeFunction sfunc = new MLSShapeFunction();
      
      surf.myVertexAttachments.clear();
      for (int i=0; i<verts.size(); i++) {
         // this could works very similarly to the code that adds
         // marker points into a mesh
         Vertex3d vtx = verts.get(i);
         
         VectorNd N = new VectorNd();
         MFreeNode3d[] dnodes = (MFreeNode3d[])mfree.findNaturalRestCoordinates (vtx.pnt, coords, N);
         
         // first see if there's a node within reduceTol of the point,
         // and if so just use that
         double maxDist = Double.NEGATIVE_INFINITY;
         double minDist = Double.POSITIVE_INFINITY;
         MFreeNode3d nearestNode = null;
         
         for (int k=0; k<dnodes.length; k++) {
            double d = vtx.pnt.distance(dnodes[k].getRestPosition());
            if (d > maxDist) {
               maxDist = d;
            }
            if (d < minDist) {
               minDist = d;
               nearestNode = dnodes[k];
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
            nodes.clear();
            weights.setSize(0);
            for (int k=0; k<N.size(); k++) {
               if (Math.abs(N.get(k)) >= reduceTol) {
                  nodes.add (dnodes[k]);
                  weights.append(N.get(k));                            
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

      return surf;
   }
   
   public static MFreeMeshComp createEmbedded (MFreeModel3d fem, MeshBase mesh) {
      return createEmbedded (new MFreeMeshComp(fem), mesh);
   }

   protected void updateVertexColors() {

      if (!mySurfaceRendering.usesStressOrStrain()) {
         return;
      }

      if (myStressPlotRanging == Ranging.Auto) {
         myStressPlotRange.merge (myModel.getNodalPlotRange(mySurfaceRendering));
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
               if (nodes[j] instanceof MFreeNode3d) { // paranoid!
                  MFreeNode3d node = (MFreeNode3d)nodes[j];
                  double w = weights.get(j);
                  if (mySurfaceRendering == SurfaceRender.Stress) {
                     sval += w*node.getVonMisesStress();
                  }
                  else if (mySurfaceRendering == SurfaceRender.MAPStress) {
                     sval += w*node.getMAPStress();
                  }
                  else if (mySurfaceRendering == SurfaceRender.Strain) {
                     sval += w*node.getVonMisesStrain();
                  } 
                  else if (mySurfaceRendering == SurfaceRender.MAPStrain) {
                     sval += w*node.getMAPStrain();
                  }
               }
            }
         }
         else if (attacher instanceof PointParticleAttachment) {
            PointParticleAttachment ppa = (PointParticleAttachment)attacher;
            MFreeNode3d node = (MFreeNode3d)ppa.getParticle();
            if (mySurfaceRendering == SurfaceRender.Stress) {
               sval = node.getVonMisesStress();
            }
            else if (mySurfaceRendering == SurfaceRender.MAPStress) {
               sval = node.getMAPStress();
            }
            else if (mySurfaceRendering == SurfaceRender.Strain) {
               sval = node.getVonMisesStrain();
            } 
            else if (mySurfaceRendering == SurfaceRender.MAPStrain) {
               sval = node.getMAPStrain();
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
      if (vtx.getIndex() < myVertexAttachments.size()) {
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
         MFreeNode3d n = (MFreeNode3d)ppa.getParticle();
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
               MFreeNode3d node = (MFreeNode3d)ppa.getParticle();
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
      if ((node = myModel.getNodes().getByNumber(nnum)) == null) {
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
      myNodeVertexMap = new HashMap<MFreeNode3d,Vertex3d>();
      myNumSingleAttachments = 0;
      ArrayList<Vertex3d> vtxList = new ArrayList<Vertex3d>();
      rtok.nextToken();
      while (rtok.tokenIsWord("f")) {
         vtxList.clear();
         rtok.nextToken();
         while (rtok.tokenIsInteger()) {
            int nnum = (int)rtok.lval;
            MFreeNode3d node = (MFreeNode3d)getNodeFromNumber (rtok, nnum);
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
                  MFreeNode3d node = (MFreeNode3d)getNodeFromNumber (rtok, nnum);
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

   //   @Override
   //   public void scan(ReaderTokenizer rtok, Object ref) throws IOException {
   //      SurfaceRender shad1 = mySurfaceRendering;
   //      super.scan(rtok, ref);
   //      SurfaceRender shad2 = mySurfaceRendering;
   //      if (shad1 != shad2) {
   //         System.out.println("Different shading");
   //      }
   //      if (mySurfaceRenderingMode == PropertyMode.Inherited) {
   //         System.out.println("Why isn't it explicit?");
   //      }
   //   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName(rtok, "surfaceMesh")) {
         isSurfaceMesh = rtok.scanBoolean();
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
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "fem")) {
         myModel = postscanReference (tokens, MFreeModel3d.class, ancestor);
         return true;
      } else if (postscanAttributeName (tokens, "attachments")) {
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

      if (myModel != null) {
         pw.print ("fem=");
         pw.println (ComponentUtils.getWritePathName (ancestor, myModel));
      }
      pw.println("surfaceMesh=" + isSurfaceMesh());
      pw.println ("attachments=["); 
      IndentingPrintWriter.addIndentation (pw, 2);
      for (int i=0; i<myVertexAttachments.size(); i++) {
         writeAttachment (myVertexAttachments.get(i), pw, fmt, ancestor);
      }
      IndentingPrintWriter.addIndentation (pw, -2); 
      pw.println ("]");
      // have to write attachments before calling super.writeItems() because
      // they have to be written *before* the surfaceRender property
      super.writeItems (pw, fmt, ancestor);
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
   public MFreeMeshComp copy(int flags, Map<ModelComponent,ModelComponent> copyMap) {
      MFreeMeshComp fm = (MFreeMeshComp)super.copy(flags, copyMap);

      MFreeModel3d newFem = (MFreeModel3d)copyMap.get(myModel);
      if (newFem != null) {
         fm.myModel = newFem;
      } 
      else {
         fm.myModel = myModel;
      }

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
   public boolean hasNodeDependency(MFreeNode3d node) {

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
      return myModel;
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
      return myModel.getMass ();
   }

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

   @Override
   public boolean containsContactMaster (CollidableDynamicComponent comp) {
      if (myNodeVertexMap != null && comp instanceof MFreeNode3d &&
         myNodeVertexMap.get((MFreeNode3d)comp) != null) {
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

   public FemModel3d getModel() {
      return myModel;
   }

}
