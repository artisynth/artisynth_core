package maspack.dicom;

import java.io.File;
import java.util.regex.Pattern;

import javax.media.opengl.GL2;

import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.PropertyChangeEvent;
import artisynth.core.modelbase.PropertyChangeListener;
import artisynth.core.modelbase.RenderableComponentBase;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.GLRenderer;
import maspack.render.LineRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Texture;
import maspack.render.TextureLoader;
import maspack.util.IntegerInterval;

public class DicomViewer extends RenderableComponentBase implements PropertyChangeListener {

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
   
   TextureLoader textureLoader = null;
   
   AffineTransform3d renderTransform;
   Point3d[] boxRenderCoords;
   Point3d[][] sliceRenderCoords;

   public DicomViewer(String name, DicomImage image) {
      super();
      init(name, image);
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
      
      // add other windows
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
   
   public DicomViewer(DicomImage image) {
      this(image.title, image);
   }
   
   public DicomViewer(String name, String imagePath, Pattern filePattern, boolean checkSubdirs) {
      DicomImage im = null;
      try {
         DicomReader rs = new DicomReader();
         im = rs.read(im, imagePath, filePattern, checkSubdirs);
      } catch(Exception e) {
         throw new RuntimeException("Failed to read dicom images in " + imagePath, e);
      }
      init(name, im);
   }

   public DicomViewer(String name, String imagePath, Pattern filePattern) {
      this(name, imagePath, filePattern, false);
   }
   
   public DicomViewer(String name, String imagePath) {
      this(name, imagePath, null);
   }
   
   public DicomViewer(String name, File imagePath, Pattern filePattern, boolean checkSubdirs) {
      this(name, imagePath.getAbsolutePath(), filePattern, checkSubdirs);
   }

   public DicomViewer(String name, File imagePath, Pattern filePattern) {
      this(name, imagePath, filePattern, false);
   }
   
   public DicomViewer(String name, File imagePath) {
      this(name, imagePath, null);
   }
   
   public DicomImage getImage() {
      return myImage;
   }
   
   public void setImage(DicomImage image) {
      myImage = image;
      if (textureLoader != null) {
         textureLoader.clearAllTextures();
         textureLoader = null;
      }
      numPixels[0] = myImage.getNumCols();
      numPixels[1] = myImage.getNumRows();
      numPixels[2] = myImage.getNumSlices();
   }
 
   public int numWindows() {
      //return myImage.get
      if (converter instanceof DicomWindowPixelConverter) {
         DicomWindowPixelConverter windowc = (DicomWindowPixelConverter)converter;
         return windowc.numWindows();
      }
      return 0;
   }
   
   public String[] getWindowNames() {
      //return myImage.get
      if (converter instanceof DicomWindowPixelConverter) {
         DicomWindowPixelConverter windowc = (DicomWindowPixelConverter)converter;
         return windowc.getWindowNames();
      }
      return null;
   }
   
   public void setWindow(String presetName) {
      //return myImage.get
      if (converter instanceof DicomWindowPixelConverter) {
         DicomWindowPixelConverter windowc = (DicomWindowPixelConverter)converter;
         windowc.setWindow(presetName);
      }
   }
   
   public void setSliceCoordinates(double s, double t, double u) {
      
      if (s < 0) {
         s = 0;
      } else if (s > 1) {
         s = 1;
      }
      
      if (t < 0) {
         t = 0;
      } else if (t > 1) {
         t = 1;
      }
      
      if (u < 0) {
         u = 0;
      } else if (u > 1) {
         u = 1;
      }
  
      sliceCoords.set(s, t, u);
      
      for (int i=0; i<3; i++) {
         int newId = (int)(Math.round(sliceCoords.get(i)*(numPixels[i]-1)));
         if (newId != sliceCoordIdxs[i]) {
            clearTexture(i);
            sliceCoordIdxs[i] = newId;
         }
      }
   }
   
   public void setSliceCoordinates(Vector3d coords) {
      setSliceCoordinates(coords.x, coords.y, coords.z);
   }
   
   @Override
   public void prerender(RenderList list) {
      super.prerender(list);
      updateRenderCoords();
   }
   
   private void refreshTextures() {
      
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
                           
                           textureLoader.getTexture(textureIds[i], image, ny, nz, src, dst);
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
                           textureLoader.getTexture(textureIds[i], image, ny, nz, src, dst);
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
                           
                           textureLoader.getTexture(textureIds[i], image, nx, nz, src, dst);
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
                           textureLoader.getTexture(textureIds[i], image, nx, nz, src, dst);
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
                           
                           textureLoader.getTexture(textureIds[i], image, nx, ny, src, dst);
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
                           textureLoader.getTexture(textureIds[i], image, nx, ny, src, dst);
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

   public void setTransform(AffineTransform3d trans) {
      myTransform.set(trans);
   }
   
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
   
   public AffineTransform3d getTransform() {
      return myTransform;
   }
   
   @Override
   public synchronized void render(GLRenderer renderer, int flags) {
      
      GL2 gl = renderer.getGL2();
      if (textureLoader == null) {
         textureLoader = new TextureLoader(gl);
      }
      refreshTextures();
      
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
         gl.glTexEnvf( GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE );
         gl.glTexParameteri (GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
         gl.glTexParameteri (GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
         gl.glTexParameteri (
            GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
         gl.glTexParameteri (
            GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);

         float alpha = (float)rprops.getAlpha();
         gl.glColor4f(1f, 1f, 1f, alpha);
      }      
      
      byte[] savedCullFaceEnabled= new byte[1];
      gl.glGetBooleanv (GL2.GL_CULL_FACE, savedCullFaceEnabled, 0);
      gl.glDisable (GL2.GL_CULL_FACE);
      
      for (int i=0; i<3; i++) {
         if (drawSlice[i]) {
            
            if (!renderer.isSelecting()) {
               Texture tex = textureLoader.getTextureByName(textureIds[i]);
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
   
   public void setPixelConverter(DicomPixelConverter converter) {
      if (this.converter != null) {
         this.converter.setPropertyHost(null);
      }
      this.converter = converter;
      converter.setPropertyHost(this);
      
      // clear textures
      clearTextures();
   }
   
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
   
   public double getX() {
      return sliceCoords.x;
   }
   
   public void setX(double x) {
      setSliceCoordinates(x, sliceCoords.y, sliceCoords.z);
   }
   
   public double getY() {
      return sliceCoords.y;
   }
   
   public void setY(double y) {
      setSliceCoordinates(sliceCoords.x, y, sliceCoords.z);
   }
   
   public double getZ() {
      return sliceCoords.z;
   }
   
   public void setZ(double z) {
      setSliceCoordinates(sliceCoords.x, sliceCoords.y, z);
   }
   
   public int getTimeIndex() {
      return timeIdx;
   }
   
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
   
   public IntegerInterval getTimeIndexRange() {
      return new IntegerInterval(0, myImage.getNumTimes()-1);
   }
   
   public boolean getDrawYZ() {
      return drawSlice[0];
   }
   
   public void setDrawYZ(boolean set) {
      drawSlice[0] = set;
   }
   
   public boolean getDrawXZ() {
      return drawSlice[1];
   }
   
   public void setDrawXZ(boolean set) {
      drawSlice[1] = set;
   }
   
   public boolean getDrawXY() {
      return drawSlice[2];
   }
   
   public void setDrawXY(boolean set) {
      drawSlice[2] = set;
   }
   
   public boolean getDrawBox() {
      return drawBox;
   }
   
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
