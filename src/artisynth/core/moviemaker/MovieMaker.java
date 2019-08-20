/**
 * Copyright (c) 2014, by the Authors: Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.moviemaker;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.imageio.ImageIO;

import argparser.DoubleHolder;
import argparser.IntHolder;
import artisynth.core.driver.Main;
import artisynth.core.workspace.RootModel;
import maspack.render.GL.FrameBufferObject;
import maspack.render.GL.GLViewer;
import maspack.util.NumberFormat;
import maspack.util.StreamGobbler;
import maspack.widgets.GuiUtils;

public class MovieMaker {   

   private String myFormat = "png";
   private String myMethodName = INTERNAL_METHOD;
   private String dataPath; // where data is written
   private int frameCounter; // counts frames rendered
   private int lastFrameCount; // last number of frames recorded
   //private double frameRate = 31.25; // frame rate in 1/s (31.25 would be better)
   private double frameRate = 50; // frame rate in 1/s (31.25 would be better)
   private Rectangle movieArea;
   private Dimension viewerResize;
   private int aasamples = FrameBufferObject.defaultMultiSamples;
   private boolean alwaysOnTop = true;

   private double audioRate = 44100;
   private boolean audioNormalized = false;
   private double mySpeed = 1.0;
   private boolean isGrabbing = false;
   private boolean audioToFile = false;
   private boolean audioToText = false;

   private String audioFileName = null;

   private MovieMakerDialog myDialog;
   private LinkedHashMap<String, Method> myMethodMap;
   private NumberFormat myFileNameFmt = new NumberFormat ("frame%05d");

   private Robot robot;
   private GLViewer myViewer;
   private int grabMode;

   private static String myMencoderCmd =
      "mencoder mf://frame*.$FMT -mf fps=$FPS:type=$FMT -ovc lavc -lavcopts " +
         "vcodec=mpeg4:vrc_buf_size=1835:vrc_maxrate=4900:vbitrate=2500 " + 
         "-ffourcc DX50 -oac copy -o $OUT.avi";

   // ffmpeg set up to use the h264 codec
   // Edit: Sanchez, March 28, 2013
   // Updated command to reflect changes in most recent version, removed audio
   //    
   private static String myFfmpegCmd =
      // "ffmpeg -y -f image2 -r $FPS -i frame%05d.$FMT -c:v libx264 -r $FPS $OUT.mov";
   //      "ffmpeg  -i frame%05d.$FMT -r $FPS -vcodec libx264 -vpre medium " +
   //      "-crf 18 -aq 100 -ar 22050 $OUT.mov";
      "ffmpeg -y -f image2 -r $FPS -i frame%05d.$FMT -pix_fmt yuv420p -crf 20 -preset veryslow -vcodec libx264 $OUT.mov";
   // ffmpeg command for use on SuSE Linux at UBC CS: No preset, no high
   // quality vcodec, but at least it works:
   // ffmpeg -i frame%05d.$FMT -r $FPS -aq 100 -ar 22050 foo.mov

   private static String myAvconvCmd = 
      "avconv -r $FPS -i frame%05d.$FMT -c:v libx264 $OUT.mp4";

   private static String myGifOptions = "-loop 0 -fps $FPS";

   // for Mac OS X, Runtime exec require absolute path to binary executable
   private static final String mencoderAbsPath = "/usr/local/bin/";
   private static final String myMencoderCmdOsX =
      mencoderAbsPath + myMencoderCmd; 

   public static final String INTERNAL_METHOD = "internal";
   public static final String MENCODER_METHOD = "mencoder";
   public static final String MENCODER_OSX_METHOD = "mencoder_osx";
   public static final String FFMPEG_METHOD = "ffmpeg";
   public static final String ANIMATED_GIF_METHOD = "animated_gif";
   public static final String AVCONV_METHOD = "avconv";

   public static final int OFFSCREEN_MODE = 1;
   public static final int ONSCREEN_MODE = 2;

   public MovieMaker (GLViewer viewer) throws Exception {
      robot = new Robot();

      myMethodMap = new LinkedHashMap<String, Method>();
      myMethodMap.put (INTERNAL_METHOD, new Method (INTERNAL_METHOD, "jpg"));
      myMethodMap.put (MENCODER_METHOD, new Method (myMencoderCmd, "png"));
      myMethodMap.put (MENCODER_OSX_METHOD, new Method (myMencoderCmdOsX, "png"));
      myMethodMap.put (FFMPEG_METHOD, new Method (myFfmpegCmd, "png"));
      myMethodMap.put (ANIMATED_GIF_METHOD, new Method (myGifOptions, "png"));
      myMethodMap.put (AVCONV_METHOD, new Method (myAvconvCmd, "png"));

      myViewer = viewer;
   }

   /**
    * Returns the HashMap of method names mapping to method information.
    */
   public HashMap<String, Method> getMethodMap() {
      return myMethodMap;
   }

   /**
    * Sets movie making method. Must be one of: "internal", "mencoder", 
    * "mencoder_osx", "ffmpeg", "animated_gif" or "avconv".
    */
   public void setMethod (String methodName) {
      if (myMethodMap.get(methodName) == null) { 
         throw new IllegalArgumentException ("Unknown method: " + methodName);
      }

      myMethodName = methodName;
   }

   /**
    * Sets movie making method. Must be one of: "internal", "mencoder", 
    * "mencoder_osx", "ffmpeg", "animated_gif" or "avconv".
    */
   public void setMethod (String methodName, String methodCmd, String imgFmt) {
      myMethodName = methodName;
      setFormat (imgFmt);
      myMethodMap.put (methodName, new Method(methodCmd, imgFmt));
   }
   
   public void addMethod(String methodName, String methodCmd, String imgFmt) {
      myMethodMap.put (methodName, new Method(methodCmd, imgFmt));
   }
   
   /**
    * Returns current movie making method name.
    */
   public String getMethod () {
      return myMethodName;
   }

   /**
    * Returns the output format.
    */
   public String getFormat() {
      return myFormat;
   }

   /**
    * Sets the output format. Useful ones include "jpg" and "bmp". 
    * ImageIO.getWriterFormatNames() gives a complete list.
    */
   public void setFormat (String fmt) {
      myFormat = fmt;
   }

   /**
    * Returns the absolute file path of the frame specified by int n.
    */
   public String getFrameFileName (int n) {
      return (dataPath + File.separator + 
         myFileNameFmt.format (n) + "." + myFormat);
   }

   public void resetFrameCounter() {
      frameCounter = 0;
   }

   /**
    * Grabs rectangle and writes to disk.
    */
   public synchronized void grab () throws Exception {
      frameCounter++;
      Main.getMain().getLogger().info("capturing frame " + frameCounter);
      if (grabMode == MovieMaker.ONSCREEN_MODE) {
         BufferedImage img = robot.createScreenCapture (movieArea);
         File file = new File (getFrameFileName (frameCounter));
         ImageIO.write (img, myFormat, file);
      }
      else {
         myViewer.setupScreenShot (viewerResize.width, viewerResize.height,
            aasamples, new File (getFrameFileName (frameCounter)), myFormat);
         myViewer.repaint();
         
         // XXX Note: we need to wait for grab to be complete, otherwise
         // model can advance before frame capture is complete, leading to
         // incorrect rendering at the supplied time.
         //
         // Bit of a hack here. We wait for the repaint to take effect
         // unless a stop request is pending, because if a stop request
         // is pending that means the GUI thread is likely waiting for
         // said stop request, in which case it won't be able to do the
         // paint and hence clear the grab. This still isn't completely
         // robust, because we still get timeouts ...
         int cnt = 0;
         while (myViewer.grabPending() && 
            // XXX wait for 10 seconds regardless of stop pending
            // !Main.getMain().getScheduler().stopRequestPending() &&   
            cnt++ < 500) {
            try {
               Thread.sleep(20);
               Thread.yield();  // yield to other threads
            } catch (Exception e){
            }
         }
         
         if (myViewer.grabPending()) {
            Main.getMain().getLogger().warn("deadlock timeout");
         }
         myViewer.repaint();
      }
   }

   /**
    * Grabs rectangle and writes to disk right now, forcing a rerender 
    * (as opposed to waiting for next render cycle).
    */
   public synchronized void forceGrab () throws Exception {
      frameCounter++;
      Main.getMain().getLogger().info ("frame " + frameCounter);
      if (grabMode == MovieMaker.ONSCREEN_MODE) {
         BufferedImage img = robot.createScreenCapture (movieArea);
         File file = new File (getFrameFileName (frameCounter));
         ImageIO.write (img, myFormat, file);
      }
      else {
         myViewer.setupScreenShot (viewerResize.width, viewerResize.height,
            aasamples, new File (getFrameFileName (frameCounter)), myFormat);
         myViewer.rerender();
         myViewer.paint();
      }
   }

   private String getFilenameExtension(String filename) {
      String extension = null;
      int n = filename.length();
      if (n >= 4 && filename.substring(n-4).startsWith("."))
         extension = filename.substring(n-3);
      return extension;
   }

   private static Rectangle getViewerBounds(GLViewer viewer) {
      Rectangle area = new Rectangle();
      area.width = viewer.getCanvas().getWidth();
      area.height = viewer.getCanvas().getHeight();
      area.x = viewer.getCanvas().getLocationOnScreen().x;
      area.y = viewer.getCanvas().getLocationOnScreen().y;
      return area;
   }

   public synchronized void grabScreenShot (String filename) {
      String format = getFilenameExtension(filename);
      if (format == null) {
         format = "png";
      }
      try {
         ImageIO.write((new Robot()).
            createScreenCapture(getViewerBounds(myViewer)), 
            format, new File(filename));
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Grabs rectangle and writes to disk.
    */
   public synchronized void grab (String filename) throws Exception {
      Main.getMain().getLogger().info ("grab");

      if (!filename.toLowerCase().endsWith (myFormat)) {
         filename = filename + "." + myFormat;
      }

      if (grabMode == MovieMaker.ONSCREEN_MODE) {
         BufferedImage img = robot.createScreenCapture (movieArea);
         File file = new File (filename);

         ImageIO.write (img, myFormat, file);
      }
      else {
         myViewer.setupScreenShot (viewerResize.width, viewerResize.height, 
            new File (filename), myFormat);
         myViewer.repaint ();
      }
   }

   /**
    * Writes movie information file.
    */
   public int close () throws Exception {
      if (frameCounter > 0) { 
         String fn = dataPath + "/info.txt";

         BufferedWriter bw = new BufferedWriter (new FileWriter (fn));
         bw.write ("Number of frames: " + String.valueOf (frameCounter));
         bw.newLine ();
         bw.write ("Width: " + String.valueOf (
            (viewerResize == null) ? movieArea.width : viewerResize.width));
         bw.newLine(); 
         bw.write ("Height: " + String.valueOf (
            (viewerResize == null) ? movieArea.height : viewerResize.height));
         bw.newLine();
         bw.write ("Frame rate: " + String.valueOf (frameRate));
         bw.flush();
         bw.close();

         lastFrameCount = frameCounter;
         frameCounter = 0;
      }
      else { 
         lastFrameCount = 0;
         frameCounter = 0;
      }

      return lastFrameCount;
   }

   /**
    * Renders captured data to a movie with the specified file name.
    */
   public void render (String fn) throws Exception { 
      if (lastFrameCount == 0) { 
         Main.getMain().getLogger().info ("No frames grabbed, no movie to make");
         return;
      }
      
      // wait for all frames to be done writing
      myViewer.awaitScreenShotCompletion();

      Method method = myMethodMap.get (myMethodName);
      if (myMethodName.equals (INTERNAL_METHOD)) { 
         String frameFileNames[] = new String[lastFrameCount];

         for (int i = 1; i <= lastFrameCount; i++) {
            frameFileNames[i - 1] = getFrameFileName (i);
         }

         new MakeMovieFromData (frameFileNames, dataPath, fn+".mov");
      }
      else if (myMethodName.equals(ANIMATED_GIF_METHOD)) {
         String opts = method.command;
         opts = opts.replace ("$FPS", "" + frameRate);

         String frameFileNames[] = new String[lastFrameCount];
         for (int i = 1; i <= lastFrameCount; i++) {
            frameFileNames[i - 1] = getFrameFileName (i);
         }

         File outFile = new File(dataPath, fn+ ".gif");

         DoubleHolder delayHolder = new DoubleHolder(0);
         IntHolder loopHolder = new IntHolder(0);
         AnimatedGifWriter.parseArgs(opts, delayHolder, loopHolder);
         AnimatedGifWriter.write(outFile, frameFileNames, delayHolder.value, loopHolder.value);

      }
      else { // Custom method, execute the specified command in data directory.
         String cmd = method.command;
         cmd = cmd.replace ("$FPS", "" + frameRate);
         cmd = cmd.replace ("$FMT", myFormat);
         
         String[] cmdArray = cmd.split ("\\s+");

         // Substitute $OUT later because file name might contain white space
         String finalCmd = "";
         for (int i = 0; i < cmdArray.length; i++) {
            cmdArray[i] = cmdArray[i].replace ("$OUT", fn);
            finalCmd = finalCmd + " " + cmdArray[i];
         }

         // Map<String, String> env = System.getenv();
         //       File dataFile = new File(dataPath);
         //       Process proc = Runtime.getRuntime().exec (
         //          cmdArray, /*env=*/null, null);

         Main.getMain().getLogger().info ("Executing " + finalCmd);
         ProcessBuilder procBuild = new ProcessBuilder(cmdArray);
         procBuild.directory(new File(dataPath));
         Process proc = procBuild.start();

         StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), System.err, ">>");
         // any output?
         StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), System.out, ">>");

         // start gobblers
         outputGobbler.start();
         errorGobbler.start();

         int exitVal = proc.waitFor ();

         if (exitVal != 0) { 
            Main.getMain().getLogger().error (
               "\nMovie creation failed with exit value: " + exitVal + "\n");
         }
      }
   }

   /**
    * Saves the first image file in the same directory as the movie.
    */
   public void saveFirstFrame (String fn) {
      if (lastFrameCount == 0) {
         return; 
      }

      File firstFrame = new File (getFrameFileName (1));
      File posterImage = new File (dataPath + File.separator + fn + "." + myFormat);

      Main.getMain().getLogger().info("Copying " + firstFrame.getName () + 
         " to " + posterImage.getName ());

      try {
         if (!firstFrame.exists ()) {
            throw new Exception ("First frame image file does not exist");
         }

         FileInputStream source = new FileInputStream (firstFrame);
         FileOutputStream destination = new FileOutputStream (posterImage);
         if (source == null || destination == null) {
            source.close();
            throw new Exception ("File streams null");
         }

         byte[] buf = new byte[1024];
         while (true) { 
            int bytes_read = source.read (buf);
            if (bytes_read == -1) {
               break;
            }

            destination.write (buf);
         }

         source.close ();
         destination.close ();
      }
      catch (Exception e) {
         e.printStackTrace ();
      }
   }

   /**
    * Remove the image files created in the specified directory after a movie
    * has been made.
    */
   public void clean () {
      for (int i = 1; i <= lastFrameCount; i++) {
         File tmpFile = new File (getFrameFileName (i));
         tmpFile.delete ();
      }
   }

   /**
    * Returns the audio sampling rate in Hz.
    */
   public double getAudioSampleRate () {
      return audioRate;
   }

   /**
    * Sets the audio sampling rate in Hz. Default rate is 44100Hz.
    */
   public void setAudioSampleRate (double rate) {
      audioRate = rate;
   }

   /**
    * Returns true if audio must be normalized.
    */
   public boolean isAudioNormalized () { 
      return audioNormalized;
   }

   /**
    * Sets whether audio must be normalized.
    */
   public void setAudioNormalized (boolean normalize) {
      audioNormalized = normalize;
   }

   /**
    * Returns the video speed.
    */
   public double getSpeed () {
      return mySpeed;
   }

   /**
    * Sets the video speed. A speed of 2 will create a movie 
    * that runs twice as fast as real time. The default speed is 1.
    */
   public void setSpeed (double s) {
      mySpeed = s;
   }

   /**
    * Returns true if currently grabbing frames.
    */
   public boolean isGrabbing () { 
      return isGrabbing;
   }

   /**
    * Sets whether the MovieMaker is currently grabbing frames.
    */
   public void setGrabbing (boolean grabbing) { 
      isGrabbing = grabbing;
   }

   /**
    * Returns true if currently rendering audio to a file.
    */
   public boolean isRenderingAudioToFile() {
      return audioToFile;
   }

   /**
    * Sets whether the MovieMaker is currently rendering audio to a file.
    */
   public void setRenderingAudioToFile (boolean rendering) {
      audioToFile = rendering;
   }

   /**
    * Returns true if currently rendering audio to text.
    */
   public boolean isRenderingAudioToText () {
      return audioToText;
   }

   /**
    * Sets whether the MovieMaker is currently rendering audio to text.
    */
   public void setRenderingAudioToText (boolean rendering) {
      audioToText = rendering;
   }

   /**
    * Returns the area defining the movie region.
    */
   public Rectangle getCaptureArea() {
      return movieArea;
   }

   /**
    * Sets the rectangle defining the movie region and what dimensions
    * to capture the movie to; resizeDim should only be set if rendering
    * from a GL canvas, otherwise null.
    */
   public void setCaptureArea (Rectangle rect, Dimension resizeDim, boolean viewerMode) { 
      movieArea = (rect == null) ? new Rectangle() : new Rectangle (rect);
      viewerResize = resizeDim;

      if (!viewerMode) {
         viewerResize = null;
      }

      grabMode = (viewerResize != null ) ? OFFSCREEN_MODE : ONSCREEN_MODE;
   }

   public void setAntiAliasingSamples(int s) {
      aasamples = s;
   }

   public int getGrabMode () {
      return grabMode;
   }

   /**
    * Returns the path name of the current audio file.
    */
   public String getAudioFileName () {
      return audioFileName;
   }

   /**
    * Sets the path name for the audio save file.
    */
   public void setAudioFileName (String fileName) {
      audioFileName = fileName;
   }

   /**
    * Returns the current frame rate in Hz.
    */
   public double getFrameRate () { 
      return frameRate;
   }

   /**
    * Sets the current frame rate in Hz.
    */
   public void setFrameRate (double rate) {
      frameRate = rate;
   }

   /**
    * Returns the path name of the directory where capture and movie
    * files are to be stored.
    */
   public String getDataPath ()
   { 
      return dataPath;
   }

   /**
    * Sets the path name of the the directory where capture and movie
    * files are to be stored.
    */
   public void setDataPath (String path) {
      dataPath = path;
   }

   /**
    * Displays MovieMakerDialog to the right of the specified window.
    */
   public void showDialog (Window win) {
      if (myDialog == null) {
         myDialog = new MovieMakerDialog (this, Main.getMain());        
      }

      GuiUtils.locateRight(myDialog, win);
      myDialog.setVisible (true);
   }

   /**
    * Hides MovieMakerDialog.
    */
   public void closeDialog () {
      myDialog.setVisible (false);  
   }

   /**
    * Update MovieMakerDialog for new model name and audio capabilities.
    */
   public void updateForNewRootModel (String modelName, RootModel root) {
      if (myDialog != null) { 
         myDialog.updateForNewRootModel (modelName, root);         
      }
   }
   
   public boolean isAlwaysOnTop() {
      return alwaysOnTop;
   }
   
   public void setAlwaysOnTop(boolean set) {
      alwaysOnTop = set;
   }

   /**
    * Information about a particular movie making method.
    */
   class Method {
      public String command;
      public String frameFileFormat;

      public Method (String cmd, String fmt) {
         command = cmd;
         frameFileFormat = fmt;
      }
   }
   
   
}
