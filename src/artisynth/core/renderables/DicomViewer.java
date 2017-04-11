/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package artisynth.core.renderables;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.RenderableComponentBase;
import maspack.image.dicom.DicomImage;
import maspack.image.dicom.DicomPixelInterpolator;
import maspack.image.dicom.DicomReader;
import maspack.image.dicom.DicomTextureContent;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Point2d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
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

public class DicomViewer extends RenderableComponentBase {

   DicomImage myImage;
   DicomTextureContent texture;
   
   static int XY_IDX = 0;
   static int XZ_IDX = 1;
   static int YZ_IDX = 2;

   public static PropertyList myProps = new PropertyList(
      DicomViewer.class, RenderableComponentBase.class);
   
   static {
      myProps.add(
         "renderProps * *", "render properties for this component",
         createDefaultRenderProps());
      myProps.add("x * *", "coordinate for YZ plane", 0.5, "[0,1]");
      myProps.add("y * *", "coordinate for XZ plane", 0.5, "[0,1]");
      myProps.add("z * *", "coordinate for XY plane", 0.5, "[0,1]");
      myProps.add("timeIndex * *", "time coordinate", 0);
      myProps.add("transform * *", "additional transform to apply to image",
         new AffineTransform3d(RigidTransform3d.IDENTITY) );
      myProps.add("drawYZ * *", "draw XY plane", true);
      myProps.add("drawXZ * *", "draw XY plane", true);
      myProps.add("drawXY * *", "draw XY plane", true);
      myProps.add("drawBox * *", "draw image box", true);
      myProps.addReadOnly("pixelInterpolator", "pixel converter");
   }
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public static RenderProps createDefaultRenderProps() {
      return new LineRenderProps();
   }
   
   AffineTransform3d myTransform;
   AffineTransform3d myRenderTransform;
   
   boolean drawBox;
   boolean drawSlice[];
   
   RenderObject robj;
   boolean robjValid;

   /**
    * Creates a new viewer widget, with supplied name and DICOM image
    * 
    * @param name widget name
    * @param image DICOM image data
    */
   public DicomViewer(String name, DicomImage image) {
      super();
      init(name, image);
   }
   
   /**
    * Creates a new viewer widget, with supplied DICOM image.  The
    * name of the component becomes the image name
    * 
    * @param image DICOM image data
    */
   public DicomViewer(DicomImage image) {
      this(image.getTitle(), image);
   }
   
   private void init(String name, DicomImage image) {
      setName(ModelComponentBase.makeValidName(name));
      myRenderProps = createRenderProps();
      robj = null;
      robjValid = false;
      
      myTransform = new AffineTransform3d(RigidTransform3d.IDENTITY);
      drawBox = true;
      drawSlice = new boolean[]{true, true, true};
      
      setImage(image);
      setSliceCoordinates(0.5, 0.5, 0.5);
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
    * Reads DICOM files into a 3D (+time) image
    * @param name name of the viewer component
    * @param imagePath directory containing DICOM files
    * @param filePattern pattern for accepting/rejecting contained files.  The pattern is applied
    * to the absolute file names of all files contained in the imagePath
    * @param checkSubdirs recursively check sub-folders for further DICOM files
    */
   public DicomViewer(String name, String imagePath, Pattern filePattern, boolean checkSubdirs) {
      DicomImage im = null;
      try {
         DicomReader rs = new DicomReader();
         im = rs.read(im, imagePath, filePattern, checkSubdirs);
      } catch (IOException ioe) {
         throw new RuntimeException(ioe);
      } catch(Exception e) {
         throw new RuntimeException("Failed to read dicom images in " + imagePath, e);
      }
      if (im == null) {
         throw new RuntimeException("No image data loaded");
      }
      init(name, im);
   }
   
   /**
    * Reads DICOM files into a 3D (+time) image
    * @param name name of the viewer component
    * @param files list of DICOM files to load
    */
   public DicomViewer(String name, List<File> files) {
      DicomImage im = null;
      try {
         DicomReader rs = new DicomReader();
         im = rs.read(im, files);
      } catch(Exception e) {
         throw new RuntimeException("Failed to read dicom images", e);
      }
      if (im == null) {
         throw new RuntimeException("No image data loaded");
      }
      init(name, im);
   }

   /**
    * Reads DICOM files into a 3D (+time) image
    * @param name name of the viewer component
    * @param imagePath directory containing DICOM files
    * @param filePattern pattern for accepting/rejecting contained files.  The pattern is applied
    * to the absolute file names of all files contained in the imagePath
    */
   public DicomViewer(String name, String imagePath, Pattern filePattern) {
      this(name, imagePath, filePattern, false);
   }
   
   /**
    * Reads DICOM files into a 3D (+time) image
    * @param name name of the viewer component
    * @param imagePath directory containing DICOM files
    */
   public DicomViewer(String name, String imagePath) {
      this(name, imagePath, null);
   }
   
   /**
    * Reads DICOM files into a 3D (+time) image
    * @param name name of the viewer component
    * @param imagePath directory containing DICOM files
    * @param filePattern pattern for accepting/rejecting contained files.  The pattern is applied
    * to the absolute file names of all files contained in the imagePath
    * @param checkSubdirs recursively check sub-folders for further DICOM files
    */
   public DicomViewer(String name, File imagePath, Pattern filePattern, boolean checkSubdirs) {
      this(name, imagePath.getAbsolutePath(), filePattern, checkSubdirs);
   }

   /**
    * Reads DICOM files into a 3D (+time) image
    * @param name name of the viewer component
    * @param imagePath directory containing DICOM files
    * @param filePattern pattern for accepting/rejecting contained files.  The pattern is applied
    * to the absolute file names of all files contained in the imagePath
    */
   public DicomViewer(String name, File imagePath, Pattern filePattern) {
      this(name, imagePath, filePattern, false);
   }
   
   /**
    * Reads DICOM files into a 3D (+time) image
    * @param name name of the viewer component
    * @param imagePath directory containing DICOM files
    */
   public DicomViewer(String name, File imagePath) {
      this(name, imagePath, null);
   }
   
   /**
    * @return the DICOM image being displayed
    */
   public DicomImage getImage() {
      return myImage;
   }
   
   /**
    * Sets the DICOM image to display
    * @param image
    */
   private void setImage(DicomImage image) {
      myImage = image;
      texture = new DicomTextureContent (image);
      myRenderProps.getColorMap ().setContent (texture);
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
   
   /**
    * Sets the normalized slice coordinates to display
    * @param x in [0,1], sets the 'x' slice
    * @param y in [0,1], sets the 'y' slice
    * @param z in [0,1], sets the 'z' slice
    */
   public void setSliceCoordinates(double x, double y, double z) {
      
      if (x < 0) {
         x = 0;
      } else if (x > 1) {
         x = 1;
      }
      
      if (y < 0) {
         y = 0;
      } else if (y > 1) {
         y = 1;
      }
      
      if (z < 0) {
         z = 0;
      } else if (z > 1) {
         z = 1;
      }
      
      setX(x);
      setY(y);
      setZ(z);
   }
   
   /**
    * Sets the normalized slice coordinates to display
    * 
    * @param coords normalized slice coordinates
    * @see DicomViewer#setSliceCoordinates(double, double, double)
    */
   public void setSliceCoordinates(Vector3d coords) {
      setSliceCoordinates(coords.x, coords.y, coords.z);
   }
   
   protected void updateRenderTransform() {
      if (myRenderTransform == null) {
         myRenderTransform = new AffineTransform3d();
      }
      myRenderTransform.set(myImage.getVoxelTransform());
      
      // shift by half pixel
      Vector3d shift = new Vector3d(-0.5,-0.5,-0.5);
      shift.transform(myRenderTransform);
      myRenderTransform.p.add(shift);
      // scale for full image
      myRenderTransform.applyScaling(myImage.getNumCols(), myImage.getNumRows(), myImage.getNumSlices());
      // append myTransform
      myRenderTransform.mul(myTransform, myRenderTransform);
   }
   
   @Override
   public void prerender(RenderList list) {
      super.prerender(list);
      texture.prerender ();
      maybeUpdateRenderObject();
      updateRenderTransform();
   }
   
   protected RenderObject buildRenderObject() {
      RenderObject robj = new RenderObject();
      
      float x = (float)getX();
      float y = (float)getY();
      float z = (float)getZ();
      
      float[][] coords = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
      
      // xy-slice
      Point2d[] texcoords = texture.getTextureCoordinates (DicomTextureContent.COL_ROW_PLANE);
      robj.addNormal (0, 0, 1);
      for (int i=0; i<4; ++i) {
         robj.addPosition (coords[i][0], coords[i][1], z);
         robj.addTextureCoord (texcoords[i]);
         robj.addVertex ();
      }
      
      // xz-slice
      texcoords = texture.getTextureCoordinates (DicomTextureContent.COL_SLICE_PLANE);
      robj.addNormal (0, 1, 0);
      for (int i=0; i<4; ++i) {
         robj.addPosition (coords[i][0], y, coords[i][1]);
         robj.addTextureCoord (texcoords[i]);
         robj.addVertex ();
      }
      
      // yz-slice
      texcoords = texture.getTextureCoordinates (DicomTextureContent.ROW_SLICE_PLANE);
      robj.addNormal (1, 0, 0);
      for (int i=0; i<4; ++i) {
         robj.addPosition (x, coords[i][0], coords[i][1]);
         robj.addTextureCoord (texcoords[i]);
         robj.addVertex ();
      }
      
      // three planes
      for (int i=0; i<3; ++i) {
         robj.createTriangleGroup ();
         int baseIdx = 4*i;
         robj.addTriangle (baseIdx, baseIdx+1, baseIdx+2);
         robj.addTriangle (baseIdx, baseIdx+2, baseIdx+3);
      }
      
      // box coordinates
      int vidx = robj.vertex (0,0,0);
      robj.vertex (0,1,0);
      robj.vertex (1,1,0);
      robj.vertex (1,0,0);
      robj.vertex (0,0,1);
      robj.vertex (0,1,1);
      robj.vertex (1,1,1);
      robj.vertex (1,0,1);
      
      final int[][] edges = {{0,1},{1,2},{2,3},{3,0},{0,4},{1,5},
                             {4,5},{5,6},{6,7},{7,4},{2,6},{3,7}};
      
      for (int[] edge : edges) {
         robj.addLine (edge[0]+vidx, edge[1]+vidx);
      }
      return robj;
   }
   
   /**
    * Sets a 3D transform to apply to the image
    * 
    * @param trans 3D transform
    */
   public void setTransform(AffineTransform3dBase trans) {
      myTransform.set(trans);
   }
   
   /**
    * Sets a 3D transform to apply to the image.  Required for property.
    * 
    * @param trans 3D transform
    */
   public void setTransform(AffineTransform3d trans) {
      myTransform.set(trans);
   }
   
   private boolean maybeUpdateRenderObject() {
      
      boolean modified = false;
      if (robj == null) {
         robj = buildRenderObject ();
         modified = true;
      } else if (!robjValid) {
     
         float x = (float)getX();
         float y = (float)getY();
         float z = (float)getZ();
         
         float[][] coords = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
         
         // xy-slice
         for (int i=0; i<4; ++i) {
            robj.setPosition (i, coords[i][0], coords[i][1], z);
         }
         
         // xz-slice
         for (int i=0; i<4; ++i) {
            robj.setPosition (i+4, coords[i][0], y, coords[i][1]);
         }
         
         // yz-slice
         for (int i=0; i<4; ++i) {
            robj.setPosition (i+8, x, coords[i][0], coords[i][1]);
         }
         
         modified = true;
      }
      robjValid = true;
      return modified;
   }
   
   /**
    * @return the 3D transform applied to the image.  By default,
    * this is the voxel-to-world transform
    */
   public AffineTransform3d getTransform() {
      return myTransform;
   }
   
   @Override
   public synchronized void render(Renderer renderer, int flags) {
      
      RenderProps rprops = getRenderProps();
      
      renderer.pushModelMatrix ();
      renderer.setModelMatrix (myRenderTransform);
     
      if (drawBox) {
         // draw box
         Shading savedShading = renderer.setShading (Shading.NONE);
         renderer.setLineColoring (rprops, isSelected());
         renderer.drawLines (robj, 0);
         renderer.setShading (savedShading);
      }
      
      ColorMapProps oldColorMap = renderer.setColorMap (rprops.getColorMap ());
      FaceStyle oldFaceStyle = renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK);
      Shading oldShading = renderer.setShading (rprops.getShading ());
      
      if (!renderer.isSelecting()) {
         renderer.setFaceColoring (rprops, isSelected());
      }      
      
      for (int i=0; i<3; i++) {
         if (drawSlice[i]) {      
            renderer.drawTriangles (robj, i);
         }
      }
      
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
      maybeUpdateRenderObject();
      updateRenderTransform();
      
      // update from corners
      for (int x=0; x<2; ++x) {
         for (int y=0; y<2; ++y) {
            for (int z=0; z<2; ++z) {
               Point3d p = new Point3d(x,y,z);
               p.transform (myRenderTransform);
               p.updateBounds (pmin, pmax);         
            }
         }
      }
      
   }
   
   @Override
   public boolean isSelectable() {
      return true;
   }
   
   /**
    * @return the current normalized 'x' coordinate
    */
   public double getX() {
      return texture.getX ();
   }
   
   /**
    * Sets the current normalized 'x' coordinate
    *
    * @param x normalized 'x' coordinate
    */
   public void setX(double x) {
      texture.setX (x);
      robjValid = false;
   }
   
   /**
    * 
    * @return the current normalized 'y' coordinate
    */
   public double getY() {
      return texture.getY ();
   }
   
   /**
    * Sets the current normalized 'y' coordinate
    * @param y normalized 'y' coordinate
    */
   public void setY(double y) {
      texture.setY (y);
      robjValid = false;
   }
   
   /**
    * @return the current normalized 'z' coordinate
    */
   public double getZ() {
      return texture.getZ ();
   }
   
   /**
    * Sets the current normalized 'z' coordinate
    * @param z normalized 'z' coordinate
    */
   public void setZ(double z) {
      texture.setZ (z);
      robjValid = false;
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
   
   /**
    * @return whether or not to draw the YZ plane (at the current 'x' coordinate)
    */
   public boolean getDrawYZ() {
      return drawSlice[YZ_IDX];
   }
   
   /**
    * Sets whether or not to draw the YZ plane (at the current 'x' coordinate
    * 
    * @param enable if <code>true</code>, enables drawing of YZ plane
    */
   public void setDrawYZ(boolean enable) {
      drawSlice[YZ_IDX] = enable;
   }
   
   /**
    * @return whether or not to draw the XZ plane (at the current 'y' coordinate)
    */
   public boolean getDrawXZ() {
      return drawSlice[XZ_IDX];
   }

   /**
    * Sets whether or not to draw the XZ plane (at the current 'y' coordinate)
    * 
    * @param enable if <code>true</code>, enables drawing of XZ plane
    */
   public void setDrawXZ(boolean enable) {
      drawSlice[XZ_IDX] = enable;
   }
   
   /**
    * @return whether or not to draw the XY plane (at the current 'z' coordinate)
    */
   public boolean getDrawXY() {
      return drawSlice[XY_IDX];
   }
   
   /**
    * Sets whether or not to draw the XY plane (at the current 'z' coordinate)
    * @param enable if <code>true</code>, enables drawing of XY plane
    */
   public void setDrawXY(boolean enable) {
      drawSlice[XY_IDX] = enable;
   }
   
   /**
    * @return whether or not to draw the 3D bounding box outline
    */
   public boolean getDrawBox() {
      return drawBox;
   }
   
   /**
    * Sets whether or not to draw the 3D bounding box outline
    * @param enable if <code>true</code>, enables drawing of the 3D bounding box
    */
   public void setDrawBox(boolean enable) {
      drawBox = enable;
   }
}
