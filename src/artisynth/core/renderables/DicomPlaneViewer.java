/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package artisynth.core.renderables;

import java.awt.Color;

import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import maspack.geometry.GeometryTransformer;
import maspack.image.dicom.DicomImage;
import maspack.image.dicom.DicomPixelInterpolator;
import maspack.image.dicom.DicomPlaneTextureContent;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Point2d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.ColorMapProps;
import maspack.render.LineRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;
import maspack.util.IntegerInterval;
import maspack.util.StringRange;

public class DicomPlaneViewer extends RenderableComponentBase implements TransformableGeometry {

   DicomImage myImage;
   DicomPlaneTextureContent texture;

   public static PropertyList myProps = new PropertyList(
      DicomPlaneViewer.class, RenderableComponentBase.class);
   
   static {
      myProps.add(
         "renderProps * *", "render properties for this component",
         createDefaultRenderProps());
      myProps.add("location * *", "coordinate transform for centre of plane", 
         new RigidTransform3d(RigidTransform3d.IDENTITY));
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
   
   RenderObject robj;
   boolean robjValid;
   RigidTransform3d location;
   Vector2d widths;
   
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
      robj = null;
      robjValid = false;
      
      location = loc.copy();
      this.widths = widths.clone();
      
      myImage = image;
      texture = new DicomPlaneTextureContent (image, loc, widths);
      myRenderProps.getColorMap ().setContent (texture);
      
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
      maybeUpdateRenderObject();
   }
   
   protected RenderObject buildRenderObject() {
      RenderObject robj = new RenderObject();
      
      float[][] coords = {{-0.5f, -0.5f}, {-0.5f, 0.5f}, {0.5f, 0.5f}, {0.5f, -0.5f}};
      
      // xy-slice
      Point2d[] texcoords = texture.getTextureCoordinates ();
      robj.addNormal (0, 0, 1);
      for (int i=0; i<4; ++i) {
         robj.addPosition (coords[i][0], coords[i][1], 0);
         robj.addTextureCoord (texcoords[i]);
         robj.addVertex ();
      }
      robj.createTriangleGroup ();
      robj.addTriangle (0, 1, 2);
      robj.addTriangle (0, 2, 3);
      
      return robj;
   }
   
   private boolean maybeUpdateRenderObject() {
      
      boolean modified = false;
      if (robj == null) {
         robj = buildRenderObject ();
         modified = true;
      }
      robjValid = true;
      return modified;
   }
   
   
   @Override
   public synchronized void render(Renderer renderer, int flags) {
      
      RenderProps rprops = getRenderProps();
      
      renderer.pushModelMatrix ();
      
      // adjust for widths and location
      renderer.mulModelMatrix(location);
      AffineTransform3d scaling = new AffineTransform3d();
      scaling.applyScaling(widths.x, widths.y, 1);
      renderer.mulModelMatrix(scaling);
     
      ColorMapProps oldColorMap = renderer.setColorMap (rprops.getColorMap ());
      FaceStyle oldFaceStyle = renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK);
      Shading oldShading = renderer.setShading (rprops.getShading ());
      
      if (!renderer.isSelecting()) {
         renderer.setFaceColoring (rprops, isSelected());
      }      
      
      
      renderer.drawTriangles (robj, 0);
      
      renderer.setShading (oldShading);
      renderer.setFaceStyle (oldFaceStyle);
      renderer.setColorMap (oldColorMap);
      
      renderer.popModelMatrix ();
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
   public void updateBounds(Vector3d pmin, Vector3d pmax) {
      super.updateBounds(pmin, pmax);
      
      // update from corners
      float[][] coords = {{-0.5f, -0.5f}, {-0.5f, 0.5f}, {0.5f, 0.5f}, {0.5f, -0.5f}};
      for (int i=0; i<coords.length; ++i) {
         Point3d p = new Point3d(widths.x*coords[i][0], widths.y*coords[i][1],0);
         p.transform(location);
         p.updateBounds(pmin, pmax);
      }
      
   }
   
   @Override
   public boolean isSelectable() {
      return true;
   }
   
   /**
    * @return a copy of the plane's current centre location/orientation
    */
   public RigidTransform3d getLocation() {
      return location.copy();
   }
   
   /**
    * Sets the current location of the center of the plane
    *
    * @param trans plane location transform
    */
   public void setLocation(RigidTransform3d trans) {
      texture.setLocation(trans);
      location.set(trans);
   }
   
   /**
    * Sets the widths of the plane
    * @param widths plane widths
    */
   public void setSize(Vector2d size) {
      texture.setWidths(size);
      this.widths.set(size);
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
   public void transformGeometry(AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   @Override
   public void transformGeometry(
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      gtr.transform(location);
      texture.setLocation(location);
   }

   @Override
   public void addTransformableDependencies(
      TransformGeometryContext context, int flags) {
      // nothing
   }
   
   
}
