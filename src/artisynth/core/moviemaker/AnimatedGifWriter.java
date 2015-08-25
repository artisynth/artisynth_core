/**
 * Copyright (c) 2014, by the Authors: Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.moviemaker;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import argparser.DoubleHolder;
import argparser.IntHolder;
import argparser.ArgParser;

/**
 * Creates an animated Gif.
 * Code adapted from discussions on the Oracle Forums:
 * https://forums.oracle.com/thread/1264385
 * 
 * @author Antonio
 *
 */
public class AnimatedGifWriter {

  
   /**
    * Configures the metadata for a single frame
    * @param meta metadata for frame
    * @param delay time delay in seconds between frames
    * @param playCount number of times to play, 0 for infinite loop
    * @param imageIndex index of frame
    * @throws IIOInvalidTreeException if cannot create metadata
    */
   private static void configure(IIOMetadata meta, double delay, 
      int playCount, int imageIndex) throws IIOInvalidTreeException {

      String metaFormat = meta.getNativeMetadataFormatName();
      if (!"javax_imageio_gif_image_1.0".equals(metaFormat)) {
         throw new IllegalArgumentException(
            "Unfamiliar gif metadata format: " + metaFormat);
      }

      IIOMetadataNode root = (IIOMetadataNode)meta.getAsTree(metaFormat);

      //Find the GraphicControlExtension node
      IIOMetadataNode child = (IIOMetadataNode)root.getFirstChild();
      while (child != null) {
         if ("GraphicControlExtension".equals(child.getNodeName())) {
            break;
         }
         child = (IIOMetadataNode)child.getNextSibling();
      }

      // set the delay time
      IIOMetadataNode gce = (IIOMetadataNode) child;
      gce.setAttribute("userDelay", "FALSE");
      gce.setAttribute("delayTime", ""+((int)(delay*100)) );

      //only the first node needs the ApplicationExtensions node
      if (imageIndex == 0) {
         IIOMetadataNode aes =
            new IIOMetadataNode("ApplicationExtensions");
         IIOMetadataNode ae =
            new IIOMetadataNode("ApplicationExtension");
         ae.setAttribute("applicationID", "NETSCAPE");
         ae.setAttribute("authenticationCode", "2.0");

         // last two bytes is an unsigned short (little endian) that
         // indicates the the number of times to loop.
         // 0 means loop forever.
         byte[] uo = new byte[]{
            0x1, (byte)(playCount & 0xFF), (byte)((playCount >> 8) & 0xFF)
         };
         ae.setUserObject(uo);
         aes.appendChild(ae);
         root.appendChild(aes);
      }

      meta.setFromTree(metaFormat, root);

   }

   /**
    * Writes a sequence of images to an animated GIF
    * @param file output file
    * @param frames input image sequence
    * @param delayTime time between frames (s)
    * @param loopCount number of times to loop (-1 for infinite)
    * @throws IOException if cannot write to the output file
    */
   public static void write(File file, BufferedImage[] frames, 
      double delayTime, int loopCount) throws IOException {
      write(file, Arrays.asList(frames), delayTime, loopCount);
   }
   
   /**
    * Writes a sequence of images to an animated GIF
    * @param file output file
    * @param frames input image sequence
    * @param delayTime time between frames (s)
    * @param loopCount number of times to loop (-1 for infinite)
    * @throws IOException if cannot write to the output file
    */
   public static void write(File file, List<? extends BufferedImage> frames, 
      double delayTime, int loopCount ) throws IOException {

      ImageWriter iw;
      try {
         iw = ImageIO.getImageWritersByFormatName("gif").next();
      } catch (Exception e) {
         throw new IOException("Cannot write GIF format", e);
      }
      
      ImageOutputStream ios = ImageIO.createImageOutputStream(file);
      iw.setOutput(ios);
      iw.prepareWriteSequence(null);

      int count = loopCount + 1;
      if (count < 0) {
         count = 0;
      }
      
      for (int i = 0; i < frames.size(); i++) {
         BufferedImage src = frames.get(i);
         ImageWriteParam iwp = iw.getDefaultWriteParam();
         IIOMetadata metadata = iw.getDefaultImageMetadata(
            new ImageTypeSpecifier(src), iwp);
         configure(metadata, delayTime, count, i);
         IIOImage ii = new IIOImage(src, null, metadata);
         iw.writeToSequence(ii, null);
      }
      iw.endWriteSequence();
      ios.close();

   }

   /**
    * Writes a sequence of images to an animated GIF
    * @param out output file
    * @param fileNames list of input frame file names (any supported format)
    * @param delayTime time between frames (s)
    * @param loopCount number of times to loop (-1 for infinite)
    * @throws IOException if cannot read or write files
    */
   public static void write(File out, String[] fileNames, 
      double delayTime, int loopCount) throws IOException {
      
      BufferedImage[] frames = new BufferedImage[fileNames.length];
      for (int ii = 0; ii < frames.length; ii++) {
        frames[ii] = ImageIO.read(new File(fileNames[ii]));
      }
      write(out, frames, delayTime, loopCount);
      
   }

   /**
    * Parses a string containing options, fills in the delay and loop
    * values
    * @param args list of arguments
    * @param delayHolder output delay time (s)
    * @param loopHolder output loop count
    */
   public static void parseArgs(String args, DoubleHolder delayHolder, 
      IntHolder loopHolder) {
      parseArgs(args.split(" "), delayHolder, loopHolder);
   }
   
   /**
    * Parses options, fills in the delay and loop values
    * @param args list of arguments
    * @param delayHolder output delay time (s)
    * @param loopHolder output loop count
    */
   public static String[] parseArgs(String[] args, DoubleHolder delayHolder, 
      IntHolder loopHolder) {
      
      ArgParser parser = new ArgParser("java java.core.moviemaker.AnimatedGifWriter [options] "
         + "<list of input files> < output file>");
      DoubleHolder fpsHolder = new DoubleHolder(0);
      
      parser.addOption ("-help %h #prints help message", null);
      parser.addOption("-delay %f #delay between frames (s)", delayHolder);
      parser.addOption("-fps %f #frames per second", fpsHolder);
      parser.addOption("-loop %d #number of times to play (-1 for infinite)", loopHolder);
   
      String[] left = parser.matchAllArgs(args, 0, 0);
      
      // favour fps
      if (fpsHolder.value > 0) {
         delayHolder.value = 1.0/fpsHolder.value;
      }
      
      return left;
   }

   /**
    * Main function
    * Usage: java artisynth.core.moviemaker.AnimatedGifWriter [options] &lt;list of input files&gt; &lt;output file&gt;
    */
   public static void main(String[] args) throws Exception {
      
      DoubleHolder delayHolder = new DoubleHolder(0);
      IntHolder loopHolder = new IntHolder(1);
      String[] left = parseArgs(args, delayHolder, loopHolder);
      if (left == null || left.length < 2) {
         return;
      }
      
      ArrayList<BufferedImage> imageList = new ArrayList<BufferedImage>(left.length);
      for (int i=0; i<left.length-1; i++ ) {
         try {
            BufferedImage image = ImageIO.read(new File(left[i]));
            imageList.add(image);
         } catch (Exception e) {
            System.err.println("Cannot read " + left[i] + ", ignoring");
         }
      }
      
      String outputFile = left[left.length-1];
      
      if (imageList.size() > 0) {
         write(new File(outputFile), imageList, delayHolder.value, loopHolder.value);
      }
      
   }
}