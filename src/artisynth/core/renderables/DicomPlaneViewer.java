/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package artisynth.core.renderables;

import java.awt.Color;
import java.util.ArrayList;

import artisynth.core.mechmodels.MeshInfo;
import artisynth.core.modelbase.ModelComponentBase;
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

public class DicomPlaneViewer extends TexturePlaneBase {

   DicomImage myImage;
   DicomPlaneTextureContent texture;
   PolygonalMesh imageMesh;
   MeshInfo imageMeshInfo;
   RigidTransform3d myTVI; // optional transform from image to viewer
   
   Vector2d widths;

   public static PropertyList myProps = new PropertyList(
      DicomPlaneViewer.class, TexturePlaneBase.class);
   
   static {
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
   
   /**
    * Creates a new image-plane viewer widget, with supplied name and DICOM image
    * 
    * @param name widget name
    * @param image DICOM image data
    * @param templateMesh defines the topology of the mesh onto which the image
    * will be mapped
    * @param TPW location/orientation of image plane center (world coordinates)
    * @param TVI if non-null, gives transform from image to viewer
    */
   public DicomPlaneViewer(
      String name, DicomImage image, PolygonalMesh templateMesh,
      RigidTransform3d TPW, RigidTransform3d TVI) {
      super();
      init(name, image, templateMesh, TPW, TVI);
   }
   
   /**
    * Creates a new image-plane viewer widget, with supplied name and DICOM image
    * 
    * @param name widget name
    * @param image DICOM image data
    * @param widths widths of image plane
    * @param TPW location/orientation of image plane center (world coordinates)
    * @param TVI if non-null, gives transform from image to viewer
    */
   public DicomPlaneViewer(
      String name, DicomImage image, Vector2d widths, 
      RigidTransform3d TPW, RigidTransform3d TVI) {
      super();
      PolygonalMesh templateMesh = MeshFactory.createRectangle (
         widths.x, widths.y, /*addTextureCoords=*/false);
      init(name, image, templateMesh, TPW, TVI);
   }
   
   /**
    * Creates a new image-plane viewer widget, with supplied name and DICOM image
    * 
    * @param name widget name
    * @param image DICOM image data
    * @param widths widths of image plane
    * @param TPW location/orientation of image plane center (world coordinates)
    */
   public DicomPlaneViewer(
      String name, DicomImage image, Vector2d widths, RigidTransform3d TPW) {
      super();
      PolygonalMesh templateMesh = MeshFactory.createRectangle (
         widths.x, widths.y, /*addTextureCoords=*/false);
      init(name, image, templateMesh, TPW, null);
   }
   
   /**
    * Creates a new image-plane viewer widget, with supplied DICOM image.  The
    * name of the component becomes the image name
    * 
    * @param image DICOM image data
    * @param widths widths of image plane
    * @param TPW location/orientation of image plane center (world coordinates)
    */
   public DicomPlaneViewer (
      DicomImage image, Vector2d widths, RigidTransform3d TPW) {
      this(image.getTitle(), image, widths, TPW);
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
      String name, DicomImage image, PolygonalMesh templateMesh,
      RigidTransform3d TPW, RigidTransform3d TVI) {
      setName(ModelComponentBase.makeValidName(name));
      myRenderProps = createRenderProps();

      Point2d origin = new Point2d();
      this.widths = new Point2d ();
      get2DBounds (origin, widths, templateMesh);

      myImage = image;
      if (TVI != null) {
         myTVI = new RigidTransform3d(TVI);
      }
      else {
         myTVI = null;
      }
      texture = new DicomPlaneTextureContent (
         image, computeTTW(TPW), this.widths);
      myRenderProps.getColorMap ().setContent (texture);

      imageMesh = buildImageMesh (origin, this.widths, templateMesh);
      imageMeshInfo = new MeshInfo();
      imageMeshInfo.set (imageMesh);
      imageMesh.setMeshToWorld (getPose());
      
      setPose(TPW);
   }

   /**
    * Compute the texture-to-world transform from the plane-to-world
    * transform TPW.
    */
   private RigidTransform3d computeTTW (RigidTransform3d TPW) {
      RigidTransform3d TTW = new RigidTransform3d();
      if (myTVI != null) {
         RigidTransform3d TIW = myImage.getTransform();
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
   
   /**
    * @return the DICOM image being displayed
    */
   public DicomImage getImage() {
      return myImage;
   }
   
   @Override
   public PolygonalMesh getImageMesh () {
      return imageMesh;
   }
   
   @Override
   protected MeshInfo getImageMeshInfo () {
      return imageMeshInfo;
   }
 
   /**
    * @return the number of interpolation windows available in the DICOM image
    */
   public int numWindows() {
      return texture.getWindowConverter ().numWindows ();
   }
   
   /**
    * @return the names of all possible interpolation windows available in the DICOM image
    */
   public String[] getWindowNames() {
      return texture.getWindowConverter ().getWindowNames ();
   }
   
   /**
    * Sets the current interpolation window to use, based on a preset name
    * @param presetName name of the interpolation window
    */
   public void setWindow(String presetName) {
      texture.getWindowConverter ().setWindow (presetName);
   }
   
   @Override
   public void prerender(RenderList list) {
      super.prerender(list);
      texture.prerender ();
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
      idxs = mesh.createVertexIndices ();
      Vector2d tscale = texture.getTextureCoordinateScaling();
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
      
      return mesh;
   }   
   
   /**
    * @return the current pixel interpolator
    */
   public DicomPixelInterpolator getPixelInterpolator() {
      return texture.getWindowConverter ();
   }
   
   /**
    * @return range of valid window names
    */
   public StringRange getWindowRange() {
      return texture.getWindowConverter().getWindowRange();
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
      Point2d origin = new Point2d();
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

      texture.setWidths(widths);
      // update surface widths
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
    */
   public boolean getSpatialInterpolation() {
      return texture.getSpatialInterpolation();
   }
   
   /**
    * Sets whether to use spatial interpolation (tri-linear) or nearest neighbour
    * when computing voxels
    * @param set enable/disable spatial interpolation
    */
   public void setSpatialInterpolation(boolean set) {
      texture.setSpatialInterpolation(set);
   }
   
     
   /**
    * @return the current time index
    */
   public int getTimeIndex() {
      return texture.getTime ();
   }
   
   /**
    * Sets the current time index
    * 
    * @param idx time index
    */
   public void setTimeIndex(int idx) {
      if (idx < 0) {
         idx = 0;
      } else if (idx > myImage.getNumTimes()) {
         idx = myImage.getNumTimes()-1;
      }
      texture.setTime (idx);
   }
   
   /**
    * @return the number of time indices available in the DICOM image
    */
   public IntegerInterval getTimeIndexRange() {
      return new IntegerInterval(0, myImage.getNumTimes()-1);
   }

   @Override
   public void transformGeometry(
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      super.transformGeometry (gtr, context, flags);
      texture.setLocation (computeTTW (getPose()));
   }
   
   public RigidTransform3d getTextureLocation() {
      return texture.getLocation();
   }
   
   public void setTextureLocation (RigidTransform3d TTW) {
      texture.setLocation (TTW);
   }
   
}
