/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package artisynth.core.renderables;

import java.awt.Color;
import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Deque;
import java.util.ArrayList;
import java.util.List;

import artisynth.core.mechmodels.MeshInfo;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.TransformGeometryContext;
import maspack.geometry.GeometryTransformer;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.MeshFactory;
import maspack.geometry.Vertex3d;
import maspack.geometry.Face;
import maspack.image.dicom.DicomImage;
import maspack.image.dicom.DicomPixelInterpolator;
import maspack.image.dicom.DicomPlaneTextureContent;
import maspack.matrix.Point2d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Quaternion;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.ColorMapProps;
import maspack.render.LineRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.Shading;
import maspack.util.IntegerInterval;
import maspack.util.StringRange;

import artisynth.core.util.ScanToken;
import artisynth.core.modelbase.*;
import artisynth.core.modelbase.CompositeComponent;
import maspack.matrix.Vector3d;
import maspack.util.ReaderTokenizer;
import maspack.util.NumberFormat;

public class DicomPlaneViewer extends TexturePlaneBase {

   DicomViewer myDicomViewer;
   DicomPlaneTextureContent myTexture;
   MeshInfo myMeshInfo;
   RigidTransform3d myTVI; // optional transform from image to viewer
   
   Vector2d widths;

   public static boolean DEFAULT_DICOM_VISIBLE = true;
   protected boolean myDicomVisible = DEFAULT_DICOM_VISIBLE;

   public static PropertyList myProps = new PropertyList(
      DicomPlaneViewer.class, TexturePlaneBase.class);
   
   static {
      myProps.add(
         "dicomVisible", "controls whether the dicom image is visible",
         DEFAULT_DICOM_VISIBLE);
      myProps.add("size", "plane dimensions", null);
      myProps.add("timeIndex", "time coordinate", 0);
      myProps.add(
         "spatialInterpolation", "trilinearly interpolate between voxels", false);
      myProps.addReadOnly("pixelInterpolator", "pixel converter");
   }
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public static RenderProps createDefaultRenderProps() {
      return new LineRenderProps();
   }

   public boolean getDicomVisible() {
      return myDicomVisible && myDicomViewer != null;
   }

   public void setDicomVisible (boolean enable) {
      myDicomVisible = enable;
      updateRenderProps();
   }
   
   /**
    * Creates an empty image-plane viewer widget. Used by scan methods.
    */
   public DicomPlaneViewer () {
      super();
      widths = new Point2d ();
      myMeshInfo = new MeshInfo();
      myRenderProps = createRenderProps();
   }
   
   /**
    * Creates a new image-plane viewer widget, with supplied name and DICOM image
    * 
    * @param name widget name
    * @param dviewer DicomViewer providing the DICOM image data
    * @param templateMesh defines the topology of the mesh onto which the image
    * will be mapped
    * @param TPW location/orientation of image plane center (world coordinates)
    * @param TVI if non-null, gives transform from image to viewer
    */
   public DicomPlaneViewer(
      String name, DicomViewer dviewer, PolygonalMesh templateMesh,
      RigidTransform3d TPW, RigidTransform3d TVI) {
      this();
      init(name, dviewer, templateMesh, TPW, TVI);
   }

   /**
    * Creates a new image-plane viewer widget, with supplied name and DICOM image
    * 
    * @param name widget name
    * @param dviewer DicomViewer providing the DICOM image data
    * @param widths widths of image plane
    * @param TPW location/orientation of image plane center (world coordinates)
    * @param TVI if non-null, gives transform from image to viewer
    */
   public DicomPlaneViewer(
      String name, DicomViewer dviewer, Vector2d widths, 
      RigidTransform3d TPW, RigidTransform3d TVI) {
      this();
      PolygonalMesh templateMesh = MeshFactory.createRectangle (
         widths.x, widths.y, /*addTextureCoords=*/false);
      init(name, dviewer, templateMesh, TPW, TVI);
   }
   
   /**
    * Creates a new image-plane viewer widget, with supplied name and DICOM image
    * 
    * @param name widget name
    * @param dviewer DicomViewer providing the DICOM image data
    * @param widths widths of image plane
    * @param TPW location/orientation of image plane center (world coordinates)
    */
   public DicomPlaneViewer(
      String name, DicomViewer dviewer, Vector2d widths, RigidTransform3d TPW) {
      this();
      PolygonalMesh templateMesh = MeshFactory.createRectangle (
         widths.x, widths.y, /*addTextureCoords=*/false);
      init(name, dviewer, templateMesh, TPW, null);
   }

   private void get2DBounds (
      Point2d origin, Vector2d widths, PolygonalMesh mesh) {
      Point3d pmin = new Point3d();
      Point3d pmax = new Point3d();
      mesh.getLocalBounds (pmin, pmax);

      if (origin != null) {
         origin.set (pmin.x, pmin.y);
      }
      if (widths != null) {
         widths.set (pmax.x-pmin.x, pmax.y-pmin.y);
      }
   }

   private void init (
      String name, DicomViewer dviewer, PolygonalMesh templateMesh,
      RigidTransform3d TPW, RigidTransform3d TVI) {
      setName(ModelComponentBase.makeValidName(name));

      Point2d origin = new Point2d();
      get2DBounds (origin, widths, templateMesh);

      if (TVI != null) {
         myTVI = new RigidTransform3d(TVI);
      }
      else {
         myTVI = null;
      }
      if (dviewer != null) {
         setImageFromViewer (dviewer, TPW);
      }
      else {
         updateRenderProps();
      }
      PolygonalMesh imageMesh =
         buildImageMesh (origin, widths, templateMesh);
      imageMesh.setMeshToWorld (getPose());                                
      myMeshInfo.set (imageMesh);
      setPose(TPW);
   }

   public void scaleMesh (double s) {
      PolygonalMesh mesh = getImageMesh();
      if (mesh != null) {
         mesh.scale (s);
         // //widths.scale (s);
         // if (myTexture != null) {
         //    Point2d origin = new Point2d();
         //    get2DBounds (origin, null, mesh);
         //    int[] idxs = mesh.createVertexIndices ();
         //    Vector2d tscale = myTexture.getTextureCoordinateScaling();
         //    ArrayList<Vector3d> tcoords = new ArrayList<>();
         //    for (Vertex3d v : mesh.getVertices()) {
         //       Point3d p = v.getPosition();
         //       Vector3d tcoord =
         //          new Vector3d (
         //             tscale.x*(p.x-origin.x)/widths.x, 
         //             tscale.y*(p.y-origin.y)/widths.y, 0);
         //       tcoords.add (tcoord);
         //    }
         //    System.out.println ("here " + tscale + "  " + widths);
         //    mesh.setTextureCoords (tcoords, idxs);
         // }
         widths.scale (s);
         if (myTexture != null) {
            myTexture.setWidths (widths);
         }
      }
   }

   private void setImageFromViewer (DicomViewer dviewer, RigidTransform3d TPW) {
      myDicomViewer = dviewer;
      myTexture = new DicomPlaneTextureContent (
         dviewer.getImage(), computeTTW(TPW), this.widths);
      myRenderProps.getColorMap ().setContent (myTexture);
   }

   /**
    * Compute the texture-to-world transform from the plane-to-world
    * transform TPW.
    */
   private RigidTransform3d computeTTW (RigidTransform3d TPW) {
      RigidTransform3d TTW = new RigidTransform3d();
      if (myTVI != null) {
         RigidTransform3d TIW = getImage().getTransform();
         TTW.mulInverseLeft (TIW, TPW);
         TTW.mulInverseLeft (myTVI, TTW);
         TTW.mul (TIW, TTW);
      }
      else {
         TTW.set (TPW);
      }
      return TTW;
   }
 
   @Override
   public RenderProps createRenderProps() {
      RenderProps props = RenderProps.createLineFaceProps (this);
      props.setFaceColor (Color.WHITE);
      props.setShading (Shading.NONE);
      ColorMapProps cprops = new ColorMapProps ();
      cprops.setEnabled (true);
      cprops.setColorMixing (ColorMixing.MODULATE);
      props.setColorMap (cprops);
      return props;
   }

   public void updateRenderProps() {
      RenderProps props = myRenderProps;
      if (getDicomVisible()) {
         if (props.getColorMap() == null) {
            ColorMapProps cprops = new ColorMapProps ();
            cprops.setEnabled (true);
            cprops.setColorMixing (ColorMixing.MODULATE);
            cprops.setColorMixing (ColorMixing.MODULATE);
            cprops.setContent(myTexture);
            props.setColorMap (cprops);
            props.setFaceColor (Color.WHITE);
            props.setShading (Shading.NONE);
         }
      }
      else {
         if (props.getColorMap() != null) {
            props.setColorMap (null);
            props.setFaceColor (new Color (0.5f, 0.5f, 0.5f));
            props.setShading (Shading.FLAT);
         }
      }
   }
   
   /**
    * @return the DICOM image being displayed
    */
   public DicomImage getImage() {
      if (myDicomViewer != null) {
         return myDicomViewer.getImage();
      }
      else {
         return null;
      }
   }
   
   @Override
   public PolygonalMesh getImageMesh () {
      return (PolygonalMesh)myMeshInfo.getMesh();
   }
   
   @Override
   protected MeshInfo getImageMeshInfo () {
      return myMeshInfo;
   }
 
   /**
    * @return the number of interpolation windows available in the DICOM image,
    * or 0 if there is no dicom image.
    */
   public int numWindows() {
      return myTexture != null ? myTexture.getWindowConverter().numWindows() : 0;
   }
   
   /**
    * @return the names of all possible interpolation windows available in the
    * DICOM image. Returns a zero-length array if there is no texture image.
    */
   public String[] getWindowNames() {
      if (myTexture != null) {
         return myTexture.getWindowConverter().getWindowNames();
      }
      else {
         return new String[0];
      }
   }
   
   /**
    * Sets the current interpolation window to use, based on a preset name.
    * Does nothing if there is no DICOM image.
    * @param presetName name of the interpolation window
    */
   public void setWindow(String presetName) {
      if (myTexture != null) {
         myTexture.getWindowConverter ().setWindow (presetName);
      }
   }
   
   @Override
   public void prerender(RenderList list) {
      super.prerender(list);
      if (myTexture != null) {
         myTexture.prerender();
      }
      else {
         myMeshInfo.prerender(getRenderProps());
      }
   }
   
   protected PolygonalMesh buildImageMesh (
      Vector2d origin, Vector2d widths, PolygonalMesh templateMesh) {
      
      PolygonalMesh mesh = new PolygonalMesh();
      
      for (Vertex3d v : templateMesh.getVertices()) {
         Point3d p = v.getPosition();
         mesh.addVertex (new Vertex3d (new Point3d (p.x, p.y, 0)));
      }
      for (Face f : templateMesh.getFaces()) {
         mesh.addFace (f.getVertexIndices());
      }

      // add normals
      int[] idxs = mesh.createFeatureIndices ();
      for (int i=0; i<idxs.length; ++i) {
         idxs[i] = 0;
      }
      ArrayList<Vector3d> normals = new ArrayList<>(1);
      normals.add (new Vector3d(0,0,1));
      mesh.setNormals (normals, idxs);

      // add texture coordinates
      if (myTexture != null) {
         idxs = mesh.createVertexIndices ();
         Vector2d tscale = myTexture.getTextureCoordinateScaling();
         ArrayList<Vector3d> tcoords = new ArrayList<>();
         for (Vertex3d v : mesh.getVertices()) {
            Point3d p = v.getPosition();
            Vector3d tcoord =
               new Vector3d (
                  tscale.x*(p.x-origin.x)/widths.x, 
                  tscale.y*(p.y-origin.y)/widths.y, 0);
            tcoords.add (tcoord);
         }
         mesh.setTextureCoords (tcoords, idxs);
      }
      
      return mesh;
   }   
   
   /**
    * @return the current pixel interpolator, or null if there is no DICOM image
    */
   public DicomPixelInterpolator getPixelInterpolator() {
      return myTexture != null ? myTexture.getWindowConverter() : null;
   }
   
   /**
    * @return range of valid window names, or null if there is no DICOM image
    */
   public StringRange getWindowRange() {
      if (myTexture != null) {
         return myTexture.getWindowConverter().getWindowRange();
      }
      else {
         return null;
      }
   }
   
   @Override
   public boolean isSelectable() {
      return true;
   }
   
   /**
    * Sets the widths of the plane
    * @param widths plane widths
    */
   public void setSize (Vector2d widths) {
      if (!isScanning()) {
         Point2d origin = new Point2d();
         PolygonalMesh imageMesh = getImageMesh();
         get2DBounds (origin, null, imageMesh);
         
         // get centers and scale factors
         double cx = origin.x + this.widths.x/2;
         double cy = origin.y + this.widths.y/2;
         double sx = widths.x/this.widths.x;
         double sy = widths.y/this.widths.y;
         
         // scale vertex positions
         for (Vertex3d v : imageMesh.getVertices()) {
            Point3d p = v.getPosition();
            v.setPosition (new Point3d(cx+sx*(p.x-cx), cy+sy*(p.y-cy), 0));
         }
         imageMesh.notifyVertexPositionsModified ();
         
         myTexture.setWidths(widths);
      }
      this.widths.set(widths);
   }
   
   /**
    * Returns the size of the plane
    * @return size of plane (width/height)
    */
   public Vector2d getSize() {
      return widths.clone();
   }
   
   
   /**
    * Checks if spatial interpolation between voxels is enabled
    * @return true if interpolating, false if using nearest neighbour
    * or if there is no DICOM image
    */
   public boolean getSpatialInterpolation() {
      return myTexture != null ? myTexture.getSpatialInterpolation() : false;
   }
   
   /**
    * Sets whether to use spatial interpolation (tri-linear) or nearest neighbour
    * when computing voxels. Does nothing if there is no dicom image.
    * @param set enable/disable spatial interpolation
    */
   public void setSpatialInterpolation(boolean set) {
      if (myTexture != null) {
         myTexture.setSpatialInterpolation(set);
      }
   }
   
     
   /**
    * @return the current time index, or -1 if there s no dicom image
    */
   public int getTimeIndex() {
      return myTexture != null ? myTexture.getTime() : -1;
   }
   
   /**
    * Sets the current time index. Does nothing if there is no dicom image.
    * 
    * @param idx time index
    */
   public void setTimeIndex(int idx) {
      if (myTexture != null) {
         DicomImage image = getImage();
         if (idx < 0) {
            idx = 0;
         } else if (idx > image.getNumTimes()) {
            idx = image.getNumTimes()-1;
         }
         myTexture.setTime (idx);
      }
   }
   
   /**
    * @return the number of time indices available in the DICOM image
    */
   public IntegerInterval getTimeIndexRange() {
      return new IntegerInterval(0, getImage().getNumTimes()-1);
   }

   @Override
   public void transformGeometry(
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      super.transformGeometry (gtr, context, flags);
      if (getImageMesh() != null) {
         get2DBounds (null, widths, getImageMesh());
      }
      if (myTexture != null) {
         myTexture.setLocation (computeTTW (getPose()));
         myTexture.setWidths (widths);
      }
   }

   // public RigidTransform3d getTextureLocation() {
   //    return myTexture.getLocation();
   // }
   
   // public void setTextureLocation (RigidTransform3d TTW) {
   //    myTexture.setLocation (TTW);
   // }
   
   // --- I/O methods --- 

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.println ("position=[ " + myState.getPosition().toString (fmt) + "]");
      pw.println ("rotation=[ " + myState.getRotation().toString (fmt) + "]");
      if (myDicomViewer != null) {
         pw.println (
            "dicomViewer="+
            ComponentUtils.getWritePathName (ancestor,myDicomViewer));
      }
      if (myTVI != null) {
         pw.println (
            "TVI=" + myTVI.toString (fmt, RigidTransform3d.AXIS_ANGLE_STRING));
      }
      pw.print ("imageMesh=");
      myMeshInfo.write (pw, fmt);
      // pw.print ("renderables=");
      // ScanWriteUtils.writeBracketedReferences (pw, myRenderables, ancestor);
      // pw.print ("vec=");
      // myVec.write (pw, fmt, /*withBrackets=*/true);
      // pw.println ("");
      // pw.print ("scalar=" + myScalar);
      // pw.print ("string=\"" + myString + "\"");
      super.writeItems (pw, fmt, ancestor);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "position")) {
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
      else if (scanAttributeName (rtok, "TVI")) {
         myTVI = new RigidTransform3d();
         myTVI.scan (rtok);
         return true;         
      }
      else if (scanAndStoreReference (rtok, "dicomViewer", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "imageMesh")) {
         myMeshInfo.scan (rtok);  
         getImageMesh().setMeshToWorld (getPose());
         return true;
      }
      else if (ScanWriteUtils.scanAndStorePropertyValue (
                  rtok, this, "timeIndex", tokens)) {
         return true;
      }
      else if (ScanWriteUtils.scanAndStorePropertyValue (
                  rtok, this, "spatialInterpolation", tokens)) {
         return true;
      }
      // else if (ScanWriteUtils.scanAndStoreReferences (
      //             rtok, "renderables", tokens) != -1) {
      //    return true;
      // }
      // else if (scanAttributeName (rtok, "vec")) {
      //    myVec.scan (rtok);
      //    return true;
      // }
      // else if (scanAttributeName (rtok, "scalar")) {
      //    myScalar = rtok.scanNumber();
      //    return true;
      // }
      // else if (scanAttributeName (rtok, "string")) {
      //    myString = rtok.scanQuotedString('"');
      //    return true;
      // }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      if (postscanAttributeName (tokens, "dicomViewer")) {
         DicomViewer dviewer = 
            postscanReference (tokens, DicomViewer.class, ancestor);
         System.out.println ("viewer\n" + getPose().toString("%10.6f"));
         setImageFromViewer (dviewer, getPose());
         return true;
      }
      else if (ScanWriteUtils.postscanPropertyValue (tokens, ancestor)) {
         return true;
      }
      // else if (postscanAttributeName (tokens, "renderables")) {
      //    myRenderables.clear();
      //    ScanWriteUtils.postscanReferences (
      //       tokens, myRenderables, RenderableComponent.class, ancestor);
      //    return true;
      // }
      return super.postscanItem (tokens, ancestor);
   }

   // --- editing ---

   /**
    * {@inheritDoc}
    */
   public void getSoftReferences (List<ModelComponent> refs) {
      if (myDicomViewer != null) {
         refs.add (myDicomViewer);
      }
   }  

   @Override
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      super.updateReferences (undo, undoInfo);   

      if (undo) {
         Object obj = undoInfo.removeFirst();
         if (obj != NULL_OBJ) {
            myDicomViewer = (DicomViewer)obj;
            myTexture = (DicomPlaneTextureContent)undoInfo.removeFirst();
            updateRenderProps();
         }
      }
      else {
         // remove dicomViewer if it is no longer in the hierarchy:
         if (!ComponentUtils.areConnected (this, myDicomViewer)) {
            undoInfo.addLast (myDicomViewer);
            undoInfo.addLast (myTexture);
            myDicomViewer = null;
            myTexture = null;
            updateRenderProps();
         }
         else {
            undoInfo.addLast (NULL_OBJ);
         }
      }
   }


}
