package artisynth.core.renderables;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;

import org.w3c.dom.NodeList;

import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MeshInfo;
import artisynth.core.mechmodels.PointList;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.RenderableComponentList;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Vector2d;
import maspack.properties.PropertyList;
import maspack.render.ColorMapProps;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;
import maspack.render.TextureContentFile;

public class ImageViewer extends TexturePlaneBase {

   MeshInfo imageMeshInfo;
   PolygonalMesh imageMesh;
   File imageFile;

   public static PropertyList myProps = new PropertyList (ImageViewer.class, TexturePlaneBase.class);
   static {
      myProps.add ("file", "image file", null);
   }
   
   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public ImageViewer(File imageFile) {
      this(ModelComponentBase.makeValidName (imageFile.getName ()), imageFile);
   }
   
   public ImageViewer() {
      RenderProps props = createRenderProps ();
      props.setFaceColor (Color.WHITE);
      props.setFaceStyle (FaceStyle.FRONT_AND_BACK);
      setRenderProps (props);
   }

   public ImageViewer(String name, File imageFile) {
      this();
      setName (name);
      try {
         loadImage(imageFile);
      }
      catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   public File getFile() {
      return imageFile;
   }
   
   public void setFile(File file) {
      imageFile = file;
      try {
         loadImage (imageFile);
      }
      catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   private static float getFloatValue(final IIOMetadataNode dimension, final String elementName) {
      NodeList pixelSizes = dimension.getElementsByTagName(elementName);
      IIOMetadataNode pixelSize = pixelSizes.getLength() > 0 ? (IIOMetadataNode) pixelSizes.item(0) : null;
      return pixelSize != null ? Float.parseFloat(pixelSize.getAttribute("value")) : -1;
  }
   
   public Vector2d getImageSize(File imageFile) throws IOException {
      
      ImageInputStream stream = ImageIO.createImageInputStream(imageFile);
      Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

      Vector2d size = new Vector2d(-1,-1);
      
      if (readers.hasNext()) {
          ImageReader reader = readers.next();
          reader.setInput(stream);

          IIOMetadata metadata = reader.getImageMetadata(0);
          IIOMetadataNode standardTree = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
          IIOMetadataNode dimension = (IIOMetadataNode) standardTree.getElementsByTagName("Dimension").item(0);
          float horizontalPixelSizeMM = getFloatValue(dimension, "HorizontalPixelSize");
          float verticalPixelSizeMM = getFloatValue(dimension, "VerticalPixelSize");
          int iw = reader.getWidth (0);
          int ih = reader.getHeight (0);
          size.set (iw*horizontalPixelSizeMM/1000, ih*verticalPixelSizeMM/1000);
          // System.out.println ("Size: " + (iw*horizontalPixelSizeMM/1000) + " x " + (ih*verticalPixelSizeMM/1000) + " m^2");
      }
      else {
          System.err.printf("Could not read %s\n", imageFile);
      }
      
      return size;

   }
   
   protected void loadImage(File imageFile) throws IOException {
      this.imageFile = imageFile;
      
      if (imageFile != null) {
         TextureContentFile imageTexture = new TextureContentFile (imageFile.getAbsolutePath ());
         int height = imageTexture.getHeight ();
         int width = imageTexture.getWidth ();
         
         BufferedImage image = imageTexture.getImage ();
         Vector2d imageSize = getImageSize(imageFile);
         
         ColorMapProps cmprops = new ColorMapProps (imageTexture);
         cmprops.setEnabled (true);
         RenderProps props = getRenderProps ();
         props.setColorMap (cmprops);
   
         // load image, generate plane
         imageMesh = MeshFactory.createRectangle (imageSize.x, imageSize.y, 1, 1, true, true);

         System.out.println ("Size: " + imageSize.x + " x " + imageSize.y);
      } else {
         // empty rectangle
         imageMesh = MeshFactory.createRectangle (1, 1, 1, 1, true, true);
      }
      
      imageMeshInfo = new MeshInfo();
      imageMeshInfo.set (imageMesh);
      imageMesh.setMeshToWorld (getPose());
   }

   @Override
   protected PolygonalMesh getImageMesh () {
      return imageMesh;
   }
   
   @Override
   protected MeshInfo getImageMeshInfo () {
      return imageMeshInfo;
   }
   

}
