/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package artisynth.core.renderables;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.PropertyChangeEvent;
import artisynth.core.modelbase.PropertyChangeListener;
import artisynth.core.modelbase.RenderableComponentBase;
import maspack.dicom.DicomHeader;
import maspack.dicom.DicomImage;
import maspack.dicom.DicomPixelConverter;
import maspack.dicom.DicomReader;
import maspack.dicom.DicomTag;
import maspack.dicom.DicomWindowPixelConverter;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.LineRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.GL.GLTexture;
import maspack.render.GL.GLTextureLoader;
import maspack.render.GL.GL2.GL2Viewer;
import maspack.util.IntegerInterval;

public class DicomViewer extends RenderableComponentBase 
   implements PropertyChangeListener {

   DicomImage myImage;
   DicomPixelConverter converter = null;

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
      myProps.add("pixelConverter", "pixel converter", new DicomWindowPixelConverter());
   }
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public static RenderProps createDefaultRenderProps() {
      return new LineRenderProps();
   }
   
   private static final String[] textureIds = {"yz", "xz", "xy"};
   
   Vector3d sliceCoords;
   int[] sliceCoordIdxs;
   Vector3d pixelSize;
   int[] numPixels;
   int timeIdx;
   
   AffineTransform3d myTransform;
   boolean drawBox;
   boolean drawSlice[];
   
   GLTextureLoader textureLoader = null;
   
   AffineTransform3d renderTransform;
   Point3d[] boxRenderCoords;
   Point3d[][] sliceRenderCoords;

   /**
    * Creates a new viewer widget, with supplied name and DICOM image
    * @param name
    * @param image
    */
   public DicomViewer(String name, DicomImage image) {
      super();
      init(name, image);
   }
   
   /**
    * Creates a new viewer widget, with supplied DICOM image.  The
    * name of the component becomes the image name
    * @param image
    */
   public DicomViewer(DicomImage image) {
      this(image.getTitle(), image);
   }
   
   private void init(String name, DicomImage image) {
      myRenderProps = createRenderProps();
      
      timeIdx = 0;
      sliceCoords = new Vector3d();
      sliceCoordIdxs = new int[]{-1,-1,-1};
      pixelSize = new Vector3d();
      numPixels = new int[3];
      myTransform = new AffineTransform3d(RigidTransform3d.IDENTITY);
      drawBox = true;
      drawSlice = new boolean[]{true, true, true};
      
      setName(ModelComponentBase.makeValidName(name));
      setImage(image);
      setSliceCoordinates(0.5, 0.5, 0.5);
      
      // Pixel converter
      DicomWindowPixelConverter window = new DicomWindowPixelConverter();
      // Full dynamic range
      int maxIntensity = myImage.getMaxIntensity();
      int minIntensity = myImage.getMinIntensity();
      int c = (maxIntensity+minIntensity) >> 1;
      int diff = maxIntensity-minIntensity;
      window.addWindowPreset("FULL DYNAMIC", c, diff);
      window.setWindow("FULL DYNAMIC");
      
      // add other windows, loaded from first slice
      DicomHeader header = myImage.getSlice(0).getHeader();
      double[] windowCenters = header.getMultiDecimalValue(DicomTag.WINDOW_CENTER);
      if (windowCenters != null) {
         int numWindows = windowCenters.length;
         double[] windowWidths = header.getMultiDecimalValue(DicomTag.WINDOW_WIDTH);
         String[] windowNames = header.getMultiStringValue(DicomTag.WINDOW_CENTER_AND_WIDTH_EXPLANATION);
         
         for (int i=0; i<numWindows; i++) {
            String wname;
            if (windowNames != null) {
               wname = windowNames[i];
            } else {
               wname = "WINDOW" + i;
            }
            window.addWindowPreset(wname, (int)windowCenters[i], (int)windowWidths[i]);
         }
      }
      
      setPixelConverter(window);
   }
   
   @Override
   public RenderProps createRenderProps() {
      return RenderProps.createLineProps(this);
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
   public void setImage(DicomImage image) {
      myImage = image;
      if (textureLoader != null) {
         textureLoader.clearAllTextures();
      }
      numPixels[0] = myImage.getNumCols();
      numPixels[1] = myImage.getNumRows();
      numPixels[2] = myImage.getNumSlices();
   }
 
   /**
    * @return the number of interpolation windows available in the DICOM image
    */
   public int numWindows() {
      //return myImage.get
      if (converter instanceof DicomWindowPixelConverter) {
         DicomWindowPixelConverter windowc = (DicomWindowPixelConverter)converter;
         return windowc.numWindows();
      }
      return 0;
   }
   
   /**
    * @return the names of all possible interpolation windows available in the DICOM image
    */
   public String[] getWindowNames() {
      //return myImage.get
      if (converter instanceof DicomWindowPixelConverter) {
         DicomWindowPixelConverter windowc = (DicomWindowPixelConverter)converter;
         return windowc.getWindowNames();
      }
      return null;
   }
   
   /**
    * Sets the current interpolation window to use, based on a preset name
    * @param presetName name of the interpolation window
    */
   public void setWindow(String presetName) {
      //return myImage.get
      if (converter instanceof DicomWindowPixelConverter) {
         DicomWindowPixelConverter windowc = (DicomWindowPixelConverter)converter;
         windowc.setWindow(presetName);
      }
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
  
      sliceCoords.set(x, y, z);
      
      for (int i=0; i<3; i++) {
         int newId = (int)(Math.round(sliceCoords.get(i)*(numPixels[i]-1)));
         if (newId != sliceCoordIdxs[i]) {
            clearTexture(i);
            sliceCoordIdxs[i] = newId;
         }
      }
   }
   
   /**
    * Sets the normalized slice coordinates to display
    * @param coords
    * @see DicomViewer#setSliceCoordinates(double, double, double)
    */
   public void setSliceCoordinates(Vector3d coords) {
      setSliceCoordinates(coords.x, coords.y, coords.z);
   }
   
   @Override
   public void prerender(RenderList list) {
      super.prerender(list);
      updateRenderCoords();
   }
   
   private void refreshTextures(GL gl) {
      
      if (textureLoader != null) {
         for (int i=0; i<3; i++) {
            if (!textureLoader.isTextureValid(textureIds[i])) {
               int nx = myImage.getNumCols();
               int ny = myImage.getNumRows();
               int nz = myImage.getNumSlices();
               switch (i) {
                  case 0: {
                     // YZ
                     switch (myImage.getPixelType()) {
                        case BYTE:
                        case SHORT:
                        {
                           // grayscale
                           byte[] image = new byte[ny*nz];
                           myImage.getPixelsByte(
                              sliceCoordIdxs[0], 0, 0, 
                              1, 1, 1, 
                              1, ny, nz, 
                              timeIdx, image, converter);
                           
                           int src = GL2.GL_RED;
                           int dst = GL2.GL_LUMINANCE;
                           int max = 0;
                           for (int j=0; j<image.length; j++) {
                              int val = 0xFF & image[j];
                              if (val > max) {
                                 max = val;
                              }
                           }
                           
                           textureLoader.getTexture(gl, textureIds[i], GL.GL_TEXTURE_2D, image, ny, nz, src, dst);
                           break;
                        }
                        case RGB: {
                           // colour
                           byte[] image = new byte[3*ny*nz];
                           myImage.getPixelsRGB(
                              sliceCoordIdxs[0], 0, 0, 
                              1, 1, 1, 1,
                              ny, nz,  
                              timeIdx,
                              image, converter);
                           
                           int src = GL2.GL_RGB;
                           int dst = GL2.GL_RGB;
                           textureLoader.getTexture(gl, textureIds[i], GL.GL_TEXTURE_2D, image, ny, nz, src, dst);
                           break;
                        }
                     }   
                     
                     break;
                  }
                  case 1: {
                     // XZ
                     switch (myImage.getPixelType()) {
                        case BYTE:
                        case SHORT:
                        {
                           // grayscale
                           byte[] image = new byte[nx*nz];
                           myImage.getPixelsByte(
                              0, sliceCoordIdxs[1], 0, 
                              1, 1, 1, 
                              nx, 1, nz,  
                              timeIdx,
                              image, converter);
                           
                           int src = GL2.GL_RED;
                           int dst = GL2.GL_LUMINANCE;
                           
                           int max = 0;
                           for (int j=0; j<image.length; j++) {
                              int val = 0xFF & image[j];
                              if (val > max) {
                                 max = val;
                              }
                           }
                           
                           textureLoader.getTexture(gl, textureIds[i], GL.GL_TEXTURE_2D, image, nx, nz, src, dst);
                           break;
                        }
                        case RGB: {
                           // colour
                           byte[] image = new byte[3*nx*nz];
                           myImage.getPixelsRGB(
                              0, sliceCoordIdxs[1], 0, 
                              1, 1, 1, 
                              nx, 1, nz, 
                              timeIdx, image, converter);
                           
                           int src = GL2.GL_RGB;
                           int dst = GL2.GL_RGB;
                           textureLoader.getTexture(gl, textureIds[i], GL.GL_TEXTURE_2D, image, nx, nz, src, dst);
                           break;
                        }
                     }   
                     break;
                  }
                  case 2: {
                     // XY
                     switch (myImage.getPixelType()) {
                        case BYTE:
                        case SHORT:
                        {
                           // grayscale
                           byte[] image = new byte[nx*ny];
                           myImage.getPixelsByte(
                              0, 0, sliceCoordIdxs[2], 
                              1, 1, 1, 
                              nx, ny, 1,  
                              timeIdx, image, converter);
                           
                           int src = GL2.GL_RED;
                           int dst = GL2.GL_LUMINANCE;
                           
                           int max = 0;
                           for (int j=0; j<image.length; j++) {
                              int val = 0xFF & image[j];
                              if (val > max) {
                                 max = val;
                              }
                           }
                           
                           textureLoader.getTexture(gl, textureIds[i], GL.GL_TEXTURE_2D, image, nx, ny, src, dst);
                           break;
                        }
                        case RGB: {
                           // colour
                           byte[] image = new byte[3*nx*ny];
                           myImage.getPixelsRGB(
                              0, 0, sliceCoordIdxs[2], 
                              1, 1, 1, 
                              nx, ny, 1,  
                              timeIdx, image, converter);
                           
                           int src = GL2.GL_RGB;
                           int dst = GL2.GL_RGB;
                           textureLoader.getTexture(gl, textureIds[i], GL.GL_TEXTURE_2D, image, nx, ny, src, dst);
                           break;
                        }
                     }   
                     break;
                  }
               }
            }
         }
      }
   }

   /**
    * Sets a 3D transform to apply to the image
    * @param trans
    */
   public void setTransform(AffineTransform3d trans) {
      myTransform.set(trans);
   }
   
   /**
    * Sets a 3D transform to apply to the image
    * @param trans
    */
   public void setTransform(AffineTransform3dBase trans) {
      myTransform.set(trans);
   }
   
   private void updateRenderCoords() {
      
      if (renderTransform == null) {
         renderTransform = new AffineTransform3d();
      }
      
      renderTransform.set(myImage.getPixelTransform());
      
      // shift by half pixel
      Vector3d shift = new Vector3d(-0.5,-0.5,-0.5);
      shift.transform(renderTransform);
      renderTransform.p.add(shift);
      
      // scale for full image
      renderTransform.applyScaling(myImage.getNumCols(), myImage.getNumRows(), myImage.getNumSlices());
      
      // append myTransform
      renderTransform.mul(myTransform, renderTransform);
      
      // box coordinates
      if (boxRenderCoords == null) {
         boxRenderCoords = new Point3d[8];
         for (int i=0; i<boxRenderCoords.length; i++) {
            boxRenderCoords[i] = new Point3d();
         }
      }
      boxRenderCoords[0].set(0, 0, 0);
      boxRenderCoords[1].set(1, 0, 0);
      boxRenderCoords[2].set(1, 1, 0);
      boxRenderCoords[3].set(0, 1, 0);
      boxRenderCoords[4].set(0, 0, 1);
      boxRenderCoords[5].set(1, 0, 1);
      boxRenderCoords[6].set(1, 1, 1);
      boxRenderCoords[7].set(0, 1, 1);
      for (int i=0; i<boxRenderCoords.length; i++) {
         boxRenderCoords[i].transform(renderTransform);
      }
      
      if (sliceRenderCoords == null) {
         sliceRenderCoords = new Point3d[3][4];
         for (int i=0; i<sliceRenderCoords.length; i++) {
            for (int j=0; j<sliceRenderCoords[i].length; j++) {
               sliceRenderCoords[i][j] = new Point3d();
            }
         }
      }
      sliceRenderCoords[0][0].set(sliceCoords.x, 0, 0);
      sliceRenderCoords[0][1].set(sliceCoords.x, 1, 0);
      sliceRenderCoords[0][2].set(sliceCoords.x, 1, 1);
      sliceRenderCoords[0][3].set(sliceCoords.x, 0, 1);
      sliceRenderCoords[1][0].set(0, sliceCoords.y, 0);
      sliceRenderCoords[1][1].set(1, sliceCoords.y, 0);
      sliceRenderCoords[1][2].set(1, sliceCoords.y, 1);
      sliceRenderCoords[1][3].set(0, sliceCoords.y, 1);
      sliceRenderCoords[2][0].set(0, 0, sliceCoords.z);
      sliceRenderCoords[2][1].set(1, 0, sliceCoords.z);
      sliceRenderCoords[2][2].set(1, 1, sliceCoords.z);
      sliceRenderCoords[2][3].set(0, 1, sliceCoords.z);
      for (int i=0; i<sliceRenderCoords.length; i++) {
         for (int j=0; j<sliceRenderCoords[i].length; j++) {
            sliceRenderCoords[i][j].transform(renderTransform);
         }
      }
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

      if (!(renderer instanceof GL2Viewer)) {
         return;
      }
      GL2Viewer viewer = (GL2Viewer)renderer;
      
      GL2 gl = viewer.getGL2();
      if (textureLoader == null) {
         textureLoader = new GLTextureLoader();
      }
      refreshTextures(gl);
      
      RenderProps rprops = getRenderProps();
      
      if (drawBox) {
         // draw box
         float[] coords0 = new float[3];
         float[] coords1 = new float[3];
         
         final int[][] edges = {{0,1},{1,2},{2,3},{3,0},{0,4},{1,5},
                                {4,5},{5,6},{6,7},{7,4},{2,6},{3,7}};
         
         for (int i=0; i<edges.length; i++) {
            boxRenderCoords[edges[i][0]].get(coords0);
            boxRenderCoords[edges[i][1]].get(coords1);
            renderer.drawLine(rprops, coords0, coords1, isSelected());
         }
      }   
      
      if (!renderer.isSelecting()) {
         
         // texture settings
         gl.glDisable(GL2.GL_LIGHTING);  
         gl.glEnable(GL2.GL_TEXTURE_2D);
         
         //   select modulate to mix texture with color for shading
         gl.glTexEnvf (
            GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE );
         gl.glTexParameteri (
            GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
         gl.glTexParameteri (
            GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
         gl.glTexParameteri (
            GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
         gl.glTexParameteri (
            GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);

         float alpha = (float)rprops.getAlpha();
         gl.glColor4f(1f, 1f, 1f, alpha);
      }      
      
      byte[] savedCullFaceEnabled= new byte[1];
      gl.glGetBooleanv (GL2.GL_CULL_FACE, savedCullFaceEnabled, 0);
      gl.glDisable (GL2.GL_CULL_FACE);
      
      for (int i=0; i<3; i++) {
         if (drawSlice[i]) {
            
            if (!renderer.isSelecting()) {
               GLTexture tex = textureLoader.getTextureByName(textureIds[i]);
               tex.bind(gl);
            }
            
            gl.glBegin(GL2.GL_QUADS);
            gl.glTexCoord2d(0, 0);
            gl.glVertex3d(sliceRenderCoords[i][0].x, sliceRenderCoords[i][0].y, sliceRenderCoords[i][0].z);
            gl.glTexCoord2d(1, 0);
            gl.glVertex3d(sliceRenderCoords[i][1].x, sliceRenderCoords[i][1].y, sliceRenderCoords[i][1].z);
            gl.glTexCoord2d(1, 1);
            gl.glVertex3d(sliceRenderCoords[i][2].x, sliceRenderCoords[i][2].y, sliceRenderCoords[i][2].z);
            gl.glTexCoord2d(0, 1);
            gl.glVertex3d(sliceRenderCoords[i][3].x, sliceRenderCoords[i][3].y, sliceRenderCoords[i][3].z);
            gl.glEnd();
         }
      }
      
      // restore draw settings
      if (savedCullFaceEnabled[0] != 0) {
         gl.glEnable (GL2.GL_CULL_FACE);
      }
      
      if (!renderer.isSelecting()) {
         gl.glDisable(GL2.GL_TEXTURE_2D);
         gl.glEnable(GL2.GL_LIGHTING);
      }

   }
   
   /**
    * Sets the converter to use for interpolating pixels from raw form to a form suitable
    * for display
    * @param converter
    */
   public void setPixelConverter(DicomPixelConverter converter) {
      if (this.converter != null) {
         this.converter.setPropertyHost(null);
      }
      this.converter = converter;
      converter.setPropertyHost(this);
      
      // clear textures
      clearTextures();
   }
   
   /**
    * @return the current pixel interpolator
    * @see DicomViewer#setPixelConverter(DicomPixelConverter)
    */
   public DicomPixelConverter getPixelConverter() {
      return converter;
   }
   
   @Override
   public void updateBounds(Point3d pmin, Point3d pmax) {
      super.updateBounds(pmin, pmax);
      updateRenderCoords();
      // update from corners
      for (Point3d pnt : boxRenderCoords) {
         pnt.updateBounds(pmin, pmax);
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
      return sliceCoords.x;
   }
   
   /**
    * Sets the current normalized 'x' coordinate
    * @param x
    */
   public void setX(double x) {
      setSliceCoordinates(x, sliceCoords.y, sliceCoords.z);
   }
   
   /**
    * 
    * @return the current normalized 'y' coordinate
    */
   public double getY() {
      return sliceCoords.y;
   }
   
   /**
    * Sets the current normalized 'y' coordinate
    * @param y
    */
   public void setY(double y) {
      setSliceCoordinates(sliceCoords.x, y, sliceCoords.z);
   }
   
   /**
    * @return the current normalized 'z' coordinate
    */
   public double getZ() {
      return sliceCoords.z;
   }
   
   /**
    * Sets the current normalized 'z' coordinate
    * @param z
    */
   public void setZ(double z) {
      setSliceCoordinates(sliceCoords.x, sliceCoords.y, z);
   }
   
   /**
    * @return the current time index
    */
   public int getTimeIndex() {
      return timeIdx;
   }
   
   /**
    * Sets the current time index
    * @param idx
    */
   public void setTimeIndex(int idx) {
      if (idx < 0) {
         idx = 0;
      } else if (idx > myImage.getNumTimes()) {
         idx = myImage.getNumTimes()-1;
      }
      if (timeIdx != idx) {
         clearTextures();
         timeIdx = idx;
      }
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
      return drawSlice[0];
   }
   
   /**
    * Sets whether or not to draw the YZ plane (at the current 'x' coordinate)
    * @param set
    */
   public void setDrawYZ(boolean set) {
      drawSlice[0] = set;
   }
   
   /**
    * @return whether or not to draw the XZ plane (at the current 'y' coordinate)
    */
   public boolean getDrawXZ() {
      return drawSlice[1];
   }

   /**
    * Sets whether or not to draw the XZ plane (at the current 'y' coordinate)
    * @param set
    */
   public void setDrawXZ(boolean set) {
      drawSlice[1] = set;
   }
   
   /**
    * @return whether or not to draw the XY plane (at the current 'z' coordinate)
    */
   public boolean getDrawXY() {
      return drawSlice[2];
   }
   
   /**
    * Sets whether or not to draw the XY plane (at the current 'z' coordinate)
    * @param set
    */
   public void setDrawXY(boolean set) {
      drawSlice[2] = set;
   }
   
   /**
    * @return whether or not to draw the 3D bounding box outline
    */
   public boolean getDrawBox() {
      return drawBox;
   }
   
   /**
    * Sets whether or not to draw the 3D bounding box outline
    * @param set
    */
   public void setDrawBox(boolean set) {
      drawBox = set;
   }
   
   private synchronized void clearTextures() {
      // invalidate textures
      if (textureLoader != null) {
         textureLoader.clearAllTextures();
      }
   }
   
   private synchronized void clearTexture(int idx) {
      if (textureLoader != null) {
         textureLoader.clearTexture(textureIds[idx]);
      }
   }

   @Override
   public void propertyChanged(PropertyChangeEvent e) {
      if (e.getHost() == this.converter) {
         clearTextures();
      }
   }
}
