/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.io.File;

import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.ModelComponent;
import maspack.render.RenderProps;
import maspack.render.ColorMapProps;

public class ImageProbe extends InputProbe {
   protected RigidBody imagePlane;
   protected String imageBasename;
   protected String imageFileExt;
   protected File imageDirectory;
   protected double frameRate;
   protected String fileNameFormat;

   public ImageProbe() {
      super();
   }

   public ImageProbe (
      ModelComponent e, RigidBody plane, String directoryName, 
      String fileBasename, String fileExtension, double rate, double startTime,
      double stopTime, double ratio) {
      super (e);

      imagePlane = plane;
      imageDirectory = new File (directoryName);
      if (!imageDirectory.exists() || !imageDirectory.isDirectory())
         imageDirectory = null;
      // imageFileExt = fileName.substring (fileName.lastIndexOf ('.'),
      // fileName.length());
      // imageBasename = fileName.substring (0, fileName.lastIndexOf ('.'));
      imageFileExt = fileExtension;
      imageBasename = fileBasename;
      frameRate = rate;
      setStartStopTimes (startTime, stopTime);
      fileNameFormat = createFileNameFormat (rate, stopTime-startTime);

      RenderProps props = imagePlane.getRenderProps();
      // props.setFaceColor (Color.white);
      // props.setFaceStyle(Faces.FRONT_AND_BACK);
      ColorMapProps tprops = props.getColorMap();
      if (tprops == null)
         tprops = new ColorMapProps();
      tprops.setEnabled (true);
      props.setColorMap (tprops);
      imagePlane.setRenderProps (props);

      setImage (0);

   }

   @Override
   public void apply (double t) {
      super.apply (t);
      setImage (t);
   }

   public RigidBody getImagePlane() {
      return imagePlane;
   }

   public void setImage (double t) {
      if (imageBasename == null || imageDirectory == null)
         return;

      int frameNum = (int)(t * frameRate) + 1;
      String filename =
         String.format (fileNameFormat, imageBasename, frameNum, imageFileExt);
      ColorMapProps tprops = imagePlane.getRenderProps().getColorMap();
      tprops.setFileName (imageDirectory.getAbsolutePath() + "/" + filename);
      imagePlane.getRenderProps().setColorMap (tprops);
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

}
