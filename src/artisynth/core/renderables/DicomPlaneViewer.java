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
   
   Vector2d widths;

   public static PropertyList myProps = new PropertyList(
      DicomPlaneViewer.class, TexturePlaneBase.class);
   
   static {
      myProps.add("size * *", "plane size", null);
      // myProps.add("resolution * *", "x and y resolution of image texture", null);
      myProps.add("timeIndex * *", "time coordinate", 0);
      myProps.add("spatialInterpolation * *", "trilinearly interpolate between voxels", false);
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
    * @param loc location/orientation of center of image plane
    * @param widths widths of image plane
    */
   public DicomPlaneViewer(String name, DicomImage image, RigidTransform3d loc, Vector2d widths) {
      super();
      init(name, image, loc, widths);
   }
   
   /**
    * Creates a new image-plane viewer widget, with supplied DICOM image.  The
    * name of the component becomes the image name
    * 
    * @param image DICOM image data
    * @param loc location/orientation of center of image plane
    * @param widths widths of image plane
    */
   public DicomPlaneViewer(DicomImage image, RigidTransform3d loc, Vector2d widths) {
      this(image.getTitle(), image, loc, widths);
   }
   
   private void init(String name, DicomImage image, RigidTransform3d loc, Vector2d widths) {
      setName(ModelComponentBase.makeValidName(name));
      myRenderProps = createRenderProps();
      
      this.widths = widths.clone();
      
      myImage = image;
      texture = new DicomPlaneTextureContent (image, loc, widths);
      myRenderProps.getColorMap ().setContent (texture);
      
      imageMesh = buildImageMesh ();
      updateImageWidths (widths);
      imageMeshInfo = new MeshInfo();
      imageMeshInfo.set (imageMesh);
      imageMesh.setMeshToWorld (getPose());
      
      setPose(loc);
      
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
   protected PolygonalMesh getImageMesh () {
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
   
   protected PolygonalMesh buildImageMesh() {
      
      PolygonalMesh mesh = new PolygonalMesh();
      
      float[][] coords = {{-0.5f, -0.5f}, {-0.5f, 0.5f}, {0.5f, 0.5f}, {0.5f, -0.5f}};
      
      // xy-slice
      Point2d[] texcoords = texture.getTextureCoordinates ();
       
      for (int i=0; i<4; ++i) {
         mesh.addVertex (coords[i][0], coords[i][1], 0);
      }
      mesh.addFace (new int[]{0,1,2});
      mesh.addFace (new int[]{0,2,3});
      
      // add normals
      int[] idx = mesh.createFeatureIndices ();
      for (int i=0; i<idx.length; ++i) {
         idx[i] = 0;
      }
      ArrayList<Vector3d> normals = new ArrayList<>(1);
      normals.add (new Vector3d(0,0,1));
      mesh.setNormals (normals, idx);
      
      // add texture coordinates
      idx = mesh.createVertexIndices ();
      ArrayList<Vector3d> tcoords = new ArrayList<>();
      for (int i=0; i<4; ++i) {
         tcoords.add (new Vector3d(texcoords[i].x, texcoords[i].y, 0));
      }
      mesh.setTextureCoords (tcoords, idx);
      
      return mesh;
   }   
   
//   @Override
//   public synchronized void render(Renderer renderer, int flags) {
//      
//      RenderProps rprops = getRenderProps();
//      
//      renderer.pushModelMatrix ();
//      
//      // adjust for widths and location
//      renderer.mulModelMatrix(getPose());
//      AffineTransform3d scaling = new AffineTransform3d();
//      scaling.applyScaling(widths.x, widths.y, 1);
//      renderer.mulModelMatrix(scaling);
//     
//      ColorMapProps oldColorMap = renderer.setColorMap (rprops.getColorMap ());
//      FaceStyle oldFaceStyle = renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK);
//      Shading oldShading = renderer.setShading (rprops.getShading ());
//      
//      if (!renderer.isSelecting()) {
//         renderer.setFaceColoring (rprops, isSelected());
//      }      
//      
//      
//      renderer.drawTriangles (robj, 0);
//      
//      renderer.setShading (oldShading);
//      renderer.setFaceStyle (oldFaceStyle);
//      renderer.setColorMap (oldColorMap);
//      
//      renderer.popModelMatrix ();
//   }
   
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
   
   protected void updateImageWidths(Vector2d width) {
      float[][] coords = {{-0.5f, -0.5f}, {-0.5f, 0.5f}, {0.5f, 0.5f}, {0.5f, -0.5f}};
      for (int i=0; i<4; ++i) {
         imageMesh.getVertex (i).setPosition (new Point3d(coords[i][0]*width.x, coords[i][1]*width.y, 0));
      }
      imageMesh.notifyVertexPositionsModified ();
   }
   
   /**
    * Sets the widths of the plane
    * @param widths plane widths
    */
   public void setSize(Vector2d widths) {
      texture.setWidths(widths);
      // update surface widths
      this.widths.set(widths);
      updateImageWidths(widths);
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
      texture.setLocation(getPose());
   }
   
}
