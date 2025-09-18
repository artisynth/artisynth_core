/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;

import maspack.geometry.BVFeatureQuery;
import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Line;
import maspack.matrix.Point3d;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.PolarDecomposition3d;
import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.properties.PropertyUtils;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorInterpolation;
import maspack.render.Renderer.ColorMixing;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.*;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.renderables.MeshCurve;
import artisynth.core.renderables.MeshMarker;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;

/**
 * Contains information about a mesh, including the mesh itself, and it's
 * possible file name and transformation with respect to the original file
 * definition.
 */
public class MeshComponent extends RenderableCompositeBase
   implements TransformableGeometry, ScalableUnits {

   protected MeshInfo myMeshInfo;
   // reference to mesh for rendering - used in case mesh changes
   protected MeshBase myRenderMesh;

   protected PointList<MeshMarker> myMarkers = null;
   protected RenderableComponentList<MeshCurve> myCurves = null;
   protected LinkedList<MeshCurve> myExternalCurves = null;

   public static boolean DEFAULT_SELECTABLE = true;
   protected boolean mySelectable = DEFAULT_SELECTABLE;

   public static PropertyList myProps = new PropertyList(
      MeshComponent.class, RenderableComponentBase.class);
   
   static final public ColorInterpolation 
      DEFAULT_COLOR_INTERPOLATION = ColorInterpolation.RGB;
   protected ColorInterpolation myColorInterp = DEFAULT_COLOR_INTERPOLATION;

   static final public ColorMixing 
      DEFAULT_VERTEX_COLOR_MIXING = ColorMixing.REPLACE;
   protected ColorMixing myVertexColorMixing = DEFAULT_VERTEX_COLOR_MIXING;

   static {
      myProps.add (
         "colorInterpolation", "interpolation for vertex coloring", 
         DEFAULT_COLOR_INTERPOLATION);
      myProps.add (
         "vertexColorMixing", "color mixing for vertex coloring", 
         DEFAULT_VERTEX_COLOR_MIXING);
      myProps.add (
         "selectable isSelectable", 
         "true if this mesh is selectable", DEFAULT_SELECTABLE);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public MeshComponent(String name) {
      this();
      setName(name);
   }

   public MeshComponent () {
      myMeshInfo = new MeshInfo();
      setRenderProps (createRenderProps());
      initializeChildComponents();
   }

   public MeshComponent (
      MeshBase mesh, String fileName, AffineTransform3dBase X) {
      this();
      setMesh (mesh, fileName, X);
   }

   protected void initializeChildComponents() {
      myMarkers =
         new PointList<MeshMarker>(MeshMarker.class, "meshMarkers");
      add (myMarkers);
      myCurves =
         new RenderableComponentList<MeshCurve>(MeshCurve.class, "curves");
      add (myCurves);
   }    

   protected void setMeshFromInfo () {
      MeshBase mesh = getMesh();
      if (mesh != null) {
         mesh.setFixed (true);
         mesh.setColorInterpolation (getColorInterpolation());
         mesh.setVertexColorMixing (getVertexColorMixing());
      }
   }

   protected void doSetMesh (
      MeshBase mesh, String fileName, AffineTransform3dBase X) {
      if (mesh == null) {
         throw new IllegalArgumentException ("mesh cannot be null");
      }
      myMeshInfo.set (mesh, fileName, X);
      setMeshFromInfo ();
      // notify parent, to get a rerender and since collisions might change
      clearMeshMarkers();
      clearCurves();
      notifyParentOfChange (new StructureChangeEvent (this));
   }
   
   public void setMesh (
      MeshBase mesh, String fileName, AffineTransform3dBase X) {
      doSetMesh (mesh, fileName, X);
      RenderProps meshProps = mesh.getRenderProps();
      if (meshProps != null) {
         setRenderProps(meshProps);
      }
   }

   public void setMesh(MeshBase mesh, String fileName) {
      setMesh (mesh, fileName, null);
   }

   public void setMesh(MeshBase mesh) {
      setMesh (mesh, null, null);
   }
   
   public ColorInterpolation getColorInterpolation() {
      return myColorInterp;
   }
   
   public void setColorInterpolation (ColorInterpolation interp) {
      if (interp != myColorInterp) {
         MeshBase mesh = getMesh();
         if (mesh != null) {
            mesh.setColorInterpolation (interp);
         }
         myColorInterp = interp;
      }
   }

   public ColorMixing getVertexColorMixing() {
      return myVertexColorMixing;
   }
   
   public void setVertexColorMixing (ColorMixing cmix) {
      if (cmix != myVertexColorMixing) {
         MeshBase mesh = getMesh();
         if (mesh != null) {
            mesh.setVertexColorMixing (cmix);
         }
         myVertexColorMixing = cmix;
      }
   }

   public Vertex3d getVertex (int idx) {
      return getMesh().getVertex (idx);
   }

   public int numVertices() {
      if (myMeshInfo != null) {
         return myMeshInfo.numVertices();
      }
      else {
         return 0;
      }
   }

   public MeshBase getMesh() {
      // Check for null since myMeshInfo will be null if getMesh() is called 
      // via setDefaultValues() before MeshComponent construction has finished.
      if (myMeshInfo != null) {
         return myMeshInfo.getMesh();
      }
      else {
         return null;
      }
   }

   public RigidTransform3d getMeshToWorld() {
      MeshBase mesh = getMesh();
      if (mesh != null) {
         return mesh.getMeshToWorld();
      }
      else {
         return null;
      }
   }      

   public void setMeshToWorld (RigidTransform3d TMW) {
      MeshBase mesh = getMesh();
      if (mesh != null) {
         mesh.setMeshToWorld(TMW);
         updateMarkerAndCurvePositions();
      }
   }      
   
   /**
    * Returns the file name, if any, associated with this mesh.  If the mesh
    * does <i>not</i> have a file name, then when this component is written out
    * (using its {@link #write} method), the mesh itself is written out inline
    * using an {@code .obj} format. Otherwise, if the mesh has a file name,
    * then that is written out instead, so that when the component is read in
    * (using its {@link #scan} method), the mesh is read from the indicated
    * file.
    * 
    * @return file name for this mesh.
    */
   public String getFileName() {
      return myMeshInfo.getFileName();
   }
   
   /**
    * Sets a file name for this mesh, or clears it if {@code name} is {@code
    * null}. See {@link #getFileName} for a description of what the file name
    * does.
    * 
    * @param name file name for this mesh.
    */
   public void setFileName (String name) {
      myMeshInfo.setFileName (name);
   }
   
   /**
    * Returns the file transform, if any, associated with this mesh.  If
    * non-null, and if the mesh also has a file name (see {@link
    * #getFileName}), then this specifies a rigid or affine transform that was
    * used to modify the mesh after it was read in from the file. When this
    * component is written out (using its {@link #write} method), this
    * transform is written out along with the file name, so that it can be
    * applied by {@link #scan} after reading the mesh from the file, thus
    * ensuring that the mesh is properly reconstructed.
    *
    * <p>The file transform has no relevance if the mesh does not have a file
    * name.
    * 
    * @return file transform for this mesh.
    */   
   public AffineTransform3d getFileTransform() {
      return new AffineTransform3d(myMeshInfo.myFileTransform);
   }

   /**
    * Sets a file transform for this mesh, or clears it if {@code name} is
    * {@code null}. See {@link #getFileTransform} for a description of what the
    * file transform does.
    * 
    * @param X file transform for this mesh.
    */
   public void setFileTransform (AffineTransform3dBase X) {
      myMeshInfo.setFileTransform (X);
   }

   public boolean isFileTransformRigid() {
      return myMeshInfo.myFileTransformRigidP;
   }

   public boolean isMeshModified() {
      return myMeshInfo.myMeshModifiedP;
   }

   public boolean isMeshTriangular() {
      return ((getMesh() instanceof PolygonalMesh) &&
              ((PolygonalMesh)getMesh()).isTriangular());
   }

   public boolean isMeshPolygonal() {
      return ((getMesh() instanceof PolygonalMesh));
   }

   /* --- Renderable interface ---

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return mySelectable;
   }

   public void setSelectable (boolean enable) {
      mySelectable = enable;
   }

   /* --- begin Renderable implementation --- */
   
   public void prerenderMesh () {
      MeshBase renderMesh = getMesh();
      if (renderMesh != null) {
         if (!renderMesh.isFixed()) {
            renderMesh.notifyVertexPositionsModified();
         }
         renderMesh.prerender (myRenderProps);
      }
      myRenderMesh = renderMesh;
   }

   @Override
   public void prerender(RenderList list) {
      prerenderMesh();
      list.addIfVisible (myMarkers);
      list.addIfVisible (myCurves);
   }

   public void render (Renderer renderer, RenderProps props, int flags) {     
      MeshBase renderMesh = myRenderMesh;
      if (renderMesh != null) {
         renderMesh.render (renderer, props, flags);
      }
   }
   
   @Override
   public void render(Renderer renderer, int flags) {
      MeshBase renderMesh = myRenderMesh;
      if (renderMesh != null) {
         if (isSelected() || isAncestorSelected()) {
            flags |= Renderer.HIGHLIGHT;
         }
         // call render(,,) instead of renderMesh.render(,,) in case the former
         // is overridden
         render (renderer, getRenderProps(), flags);
      }
   }
   
   protected boolean isAncestorSelected() {
      ModelComponent comp = this;
      while (comp != null) {
         if (comp.isSelected()) {
            return true;
         }
         comp = comp.getParent();
      }
      return false;
   }

   protected boolean isParentOrGrandParentSelected() {
      CompositeComponent parent = getParent();
      if (parent != null) {
         if (parent.isSelected()) {
            return true;
         }
         parent = parent.getParent();
         if (parent != null) {
            return parent.isSelected();
         }
      }
      return false;
   }

   @Override
   public void updateBounds(Vector3d pmin, Vector3d pmax) {
      MeshBase mesh = getMesh();
      if (mesh != null) {
         mesh.updateBounds(pmin, pmax);
      }
   }

   public void updatePosition (int flags) {
      MeshBase mesh = getMesh();
      mesh.notifyVertexPositionsModified();
   }

   protected void writeMeshInfo (PrintWriter pw, NumberFormat fmt) 
      throws IOException {
      pw.print ("mesh=");
      myMeshInfo.write (pw, fmt);
   }
   
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
         throws IOException {

      // write mesh info first so that the mesh will be read before renderProps;
      // this is necessary because the mesh determines what type of renderProps
      // should be instantiated
      writeMeshInfo (pw, fmt);
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt, ancestor);
      myComponents.writeComponentsByName (pw, fmt, ancestor);
   }
   
   protected void scanMeshInfo (ReaderTokenizer rtok) throws IOException {
      myMeshInfo.scan (rtok);  
      setMeshFromInfo();
   }   

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      myComponents.scanBegin();
      super.scan (rtok, ref);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (myComponents.scanAndStoreComponentByName (rtok, tokens)) {
         return true;
      }      
      else if (scanAttributeName (rtok, "mesh")) {
         scanMeshInfo (rtok);
         return true;
      }
      rtok.pushBack();
      // scan properties
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
   }

   public void scaleDistance (double s) {
      myMeshInfo.myMesh.scale (s);      
   }

   public void scaleMass (double s) {
   }

   /* --- mesh markers --- */

   public void addMeshMarker (MeshMarker mkr) {
      myMarkers.add (mkr);
   }

   public MeshMarker addMeshMarker (Point3d pos) {
      MeshMarker mkr = new MeshMarker (this, pos);
      myMarkers.add (mkr);
      return mkr;
   }
   
   public PointList<MeshMarker> getMeshMarkers() {
      return myMarkers;
   }

   public boolean removeMeshMarker (MeshMarker mkr) {
      return myMarkers.remove (mkr);
   }

   public void clearMeshMarkers() {
      myMarkers.removeAll();
   }
   
   /* --- mesh curves --- */

   public MeshCurve addCurve () {
      MeshCurve curve = new MeshCurve (this);
      myCurves.add (curve);
      return curve;
   }
   
   public void addCurve (MeshCurve curve) {
      if (curve.getMeshComp() != this) {
         throw new IllegalArgumentException (
            "Mesh curve not associated with body");
      }
      myCurves.add (curve);
   }
   
   public RenderableComponentList<MeshCurve> getCurves() {
      return myCurves;
   }

   public boolean removeCurve (MeshCurve curve) {
      return myCurves.remove (curve);
   }

   public void clearCurves() {
      myCurves.removeAll();
   }

   /*-- management of external mesh curves that are not children of this mesh --*/

   /**
    * Called by external MeshCurves when they connect to the component
    * hierarchy, to ensure that they get updated when this mesh changes.
    */
   public void addExternalCurve (MeshCurve curve) {
      if (myExternalCurves == null) {
         myExternalCurves = new LinkedList<>();
      }
      myExternalCurves.add (curve);
   }

   /**
    * Called by external MeshCurves when they disconnect from the component
    * hierarchy, so that they not longer get updated when this mesh changes.
    */
   public boolean removeExternalCurve (MeshCurve curve) {
      if (myExternalCurves == null) {
         return false;
      }
      boolean removed = myExternalCurves.remove (curve);
      if (myExternalCurves.size() == 0) {
         myExternalCurves = null;
      }
      return removed;
   }

   /* --- transform geometry --- */

   public void transformGeometry (AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {

      // transform the mesh itself. Subclasses that provide local pose 
      // information may override this method to instead use
      //myMeshInfo.transformGeometryAndPose(gtr, null);
      myMeshInfo.transformGeometry (gtr);
   }   
   
   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      // no dependencies
   }

   protected void updateMarkerAndCurvePositions() {
      if (!isScanning()) {
         // if scanning, markers and curves may not exist yet
         for (MeshMarker mm : myMarkers) {
            mm.updateState();
         }
         for (MeshCurve mc : myCurves) {
            mc.updateMarkerPositions();
         }
         if (myExternalCurves != null) {
            for (MeshCurve mc : myExternalCurves) {
               mc.updateMarkerPositions();
            }
         }
      }
   }
   
   public void updateSlavePos () {
      // potentially notify of vertex modification
      // TODO: potentially remove this once MFreeModel3d transitions to FemMeshComp
      if (!myMeshInfo.myMesh.isFixed()) {
         myMeshInfo.myMesh.notifyVertexPositionsModified ();
      }
   }

   public static PolygonalMesh[] createSurfaceMeshArray (PolygonalMesh mesh) {
      if (mesh != null) {
         return new PolygonalMesh[] { mesh };
      }
      else {
         return new PolygonalMesh[] {};
      }
   }
   
   public static int numSurfaceMeshes (
      MeshComponentList<?> list) {
      int num = 0;
      for (MeshComponent mc : list) {
         MeshBase mesh = mc.getMesh();
         if (mesh != null && mesh instanceof PolygonalMesh) {
            num++;
         }
      }
      return num;
   }
   
   public static PolygonalMesh[] getSurfaceMeshes (
      MeshComponentList<?> list) {
      PolygonalMesh[] meshes = new PolygonalMesh[numSurfaceMeshes(list)];
      int k = 0;
      for (MeshComponent mc : list) {
         MeshBase mesh = mc.getMesh();
         if (mesh != null && mesh instanceof PolygonalMesh) {
            meshes[k++] = (PolygonalMesh)mesh;
         }
      }     
      return meshes;
   } 

   public MeshComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      MeshComponent comp = (MeshComponent)super.copy (flags, copyMap);

      comp.myMeshInfo = myMeshInfo.copy();
      
      comp.initializeChildComponents();
      comp.setName(null);
      comp.setNavpanelVisibility(getNavpanelVisibility());

      if (copyMap != null) {
         copyMap.put (this, comp);
      }
      for (int i=0; i<myMarkers.size(); i++) {
         comp.myMarkers.add (myMarkers.get(i).copy (flags, copyMap));
      }     
      for (int i=0; i<myCurves.size(); i++) {
         comp.myCurves.add (myCurves.get(i).copy (flags, copyMap));
      }
      
      return comp;
   }

   /**
    * Queries whether this MeshComponent intersects a ray. For an intersection
    * to occur, the mesh itself must be a {@link PolygonalMesh}. If {@code
    * nearest} is non-{@code null}, it is used to return the nearest
    * intersection point if it exists.
    * 
    * @param nearest if non-{@code null}, returns the nearest intersect point
    * @param ray ray to intersect the mesh
    * @return true if the mesh is a {@code PolygonalMesh} and an intersection
    * exists
    */
   public boolean computeRayIntersection (Point3d nearest, Line ray) {
      
      if (!(getMesh() instanceof PolygonalMesh)) {
         return false;
      }
      
      PolygonalMesh mesh = (PolygonalMesh)getMesh();
      Point3d pos = BVFeatureQuery.nearestPointAlongRay (
            mesh, ray.getOrigin(), ray.getDirection());
      if (pos != null) {
         if (nearest != null) {
            nearest.set (pos);
         }
         return true;
      }
      else {
         return false;
      }
   }

   
}
