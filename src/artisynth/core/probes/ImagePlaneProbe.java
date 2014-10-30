/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.awt.Color;
import java.io.File;
import java.util.LinkedList;

import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.GLRenderable;
import maspack.render.GLRenderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.RenderProps.Faces;
import maspack.render.TextureProps;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.util.TransformableGeometry;

public class ImagePlaneProbe extends InputProbe implements RenderableComponent,
TransformableGeometry {
   protected RenderProps myRenderProps = null;
   protected PolygonalMesh planeMesh;
   protected String imageBasename;
   protected String imageFileExt;
   protected File imageDirectory;
   protected double frameRate;
   protected String fileNameFormat;

   public static final RigidTransform3d sagittalProjection =
      new RigidTransform3d (new Vector3d(), new AxisAngle (
         1, 0, 0, Math.PI / 2));

   public ImagePlaneProbe (
      ModelComponent e, String directoryName, String fileName, 
      double rate, double startTime, double stopTime, double ratio) {

      super (e);
      double r = getRadius (e);
      planeMesh = MeshFactory.createPlane (r, r);
      planeMesh.transform (sagittalProjection);
      imageDirectory = new File (directoryName);
      if (!imageDirectory.exists() || !imageDirectory.isDirectory())
         imageDirectory = null;
      imageFileExt =
         fileName.substring (fileName.lastIndexOf ('.'), fileName.length());
      imageBasename = fileName.substring (0, fileName.lastIndexOf ('.'));
      frameRate = rate;
      setStartStopTimes (startTime, stopTime);
      fileNameFormat = createFileNameFormat (rate, stopTime - startTime);

      RenderProps props = createRenderProps();
      props.setFaceColor (Color.white);
      props.setFaceStyle (Faces.FRONT_AND_BACK);
      TextureProps tprops = props.getTextureProps();
      if (tprops == null)
         tprops = new TextureProps();
      tprops.setEnabled (true);
      props.setTextureProps (tprops);
      setRenderProps (props);

      setImage (0);
   }

   public static PropertyList myProps =
      new PropertyList (ImagePlaneProbe.class, InputProbe.class);

   static {
      myProps.add ("renderProps * *", "render properties", null);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   @Override
   public void apply (double t) {
      super.apply (t);
      setImage (t);
   }

   public PolygonalMesh getMesh() {
      return planeMesh;
   }

   public void setImage (double t) {
      if (imageBasename == null || imageDirectory == null)
         return;

      int frameNum = (int)(t * frameRate) + 1;
      String filename =
         String.format (fileNameFormat, imageBasename, frameNum, imageFileExt);
      TextureProps tprops = myRenderProps.getTextureProps();
      tprops.setFileName (imageDirectory.getAbsolutePath() + "/" + filename);
      tprops.setTexture (null);
      myRenderProps.setTextureProps (tprops);
   }

   public RenderProps createRenderProps() {
      return RenderProps.createMeshProps (this);
   }

   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   public void setRenderProps (RenderProps props) {
      myRenderProps =
         RenderableComponentBase.updateRenderProps (this, myRenderProps, props);
   }

   public void transformGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {
      transformGeometry (X);
   }

   public void transformGeometry (AffineTransform3dBase X) {
      planeMesh.transform (X);
      myRenderProps.clearMeshDisplayList();
   }

   public boolean isSelectable() {
      return true;
   }

   public int numSelectionQueriesNeeded() {
      return -1;
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   public int getRenderHints() {
      int code = 0;
      if (myRenderProps != null && myRenderProps.getAlpha() != 1) {
         code |= TRANSLUCENT;
      }
      return code;
   }

   public void prerender (RenderList list) {
      // drawMesh is called in render()
      // list.addIfVisible (imagePlane);
   }

   public void render (GLRenderer renderer, int flags) {
      planeMesh.render (
         renderer, myRenderProps, isSelected() ? GLRenderer.SELECTED : 0);
      //renderer.drawMesh (
      //   myRenderProps, planeMesh, isSelected() ? GLRenderer.SELECTED : 0);
   }

   public void updateBounds (Point3d pmin, Point3d pmax) {
      planeMesh.updateBounds (pmin, pmax);
   }

   private String createFileNameFormat (double rate, double duration) {
      int numDigits = (int)Math.log10 (Math.ceil (rate * duration)) + 1;
      switch (numDigits) {
         case 1:
            return "%s%01d%s";
         case 2:
            return "%s%02d%s";
         case 3:
            return "%s%03d%s";
         case 4:
            return "%s%04d%s";
         default:
            System.err.println ("too many frames - "
            + (int)Math.ceil (rate * duration));
            return null;
      }

   }

   /**
    * Returns the radius of a renderable object.
    */
   private static final double inf = Double.POSITIVE_INFINITY;

   public double getRadius (ModelComponent e) {
      double radius = 1.0;
      if (e instanceof GLRenderable) {
         GLRenderable r = (GLRenderable)e;
         Point3d max = new Point3d (-inf, -inf, -inf);
         Point3d min = new Point3d (inf, inf, inf);
         r.updateBounds (min, max);
         if (max.x != -inf) { // then the renderable provided bounds, so use to
                              // compute radius
            Vector3d vdiag = new Vector3d();
            vdiag.sub (max, min);
            radius = vdiag.norm() / 2;
         }
      }
      return radius;
   }

}
