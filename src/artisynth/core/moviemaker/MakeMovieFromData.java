/**
 * Copyright (c) 2014, by the Authors: Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.moviemaker;

import java.io.*;
import java.awt.Dimension;
import java.util.*;
import javax.media.*;
import javax.media.control.*;
import javax.media.protocol.*;
import javax.media.datasink.*;
import javax.media.format.VideoFormat;

/**
   Take data in a given directory, in format is dictated by MovieMaker class, and make a movie out of it.
   Just create the object and it will do it.
*/

public class MakeMovieFromData implements ControllerListener, DataSinkListener {
   private double frameRate; 
   private int width,height; // frame sizes
   private int nFrames;

   /**
      Create frame writer
      @param dataPath absolute path where data is stored
      @param fileName filename for movie (without .mov extension)
   */   
   public MakeMovieFromData(String[] frameFileNames, String dataPath, 
      String fileName) throws Exception {

      String fileInfo = dataPath + "/info.txt";
      
      BufferedReader in = new BufferedReader (new FileReader (fileInfo));
      
      String line = in.readLine ();
      nFrames = Integer.parseInt (line.substring (line.lastIndexOf (' ') + 1));
      line = in.readLine ();
      width = Integer.parseInt (line.substring (line.lastIndexOf (' ') + 1));
      line = in.readLine ();
      height = Integer.parseInt (line.substring (line.lastIndexOf (' ') + 1));
      line = in.readLine ();
      frameRate = Double.parseDouble (line.substring (line.lastIndexOf (' ') + 1));
      
      MediaLocator oml;
      if ((oml = createMediaLocator("file:"+dataPath + "/" + fileName)) == null) {
         System.err.println("Cannot build media locator from: " + fileName);
         in.close();
         throw new Exception("Movie write failed: Can't build Medialocator");
      }
      System.out.println ("output file = " + fileName);
      
      Vector<String> inputFiles = new Vector<String> ();
      for (int i = 0; i < nFrames; i++) {
         inputFiles.addElement (frameFileNames[i]);
      }
      
      doIt (width, height, (int) frameRate, inputFiles, oml);
      in.close();
   }


   public boolean doIt(int width, int height, int frameRate, 
      Vector<String> inFiles, MediaLocator outML) {
      ImageDataSource ids = new ImageDataSource(width, height, frameRate, inFiles);
      System.out.println("out MediaLocator="+outML);

      Processor p;

      try {
         System.err.println("- create processor for the image datasource ...");
         p = Manager.createProcessor(ids);
      } catch (Exception e) {
         System.err.println("Yikes!  Cannot create a processor from the data source.");
         return false;
      }

      p.addControllerListener(this);

      // Put the Processor into configured state so we can set
      // some processing options on the processor.
      p.configure();
      if (!waitForState(p, Processor.Configured)) {
         System.err.println("Failed to configure the processor.");
         return false;
      }

      // Set the output content descriptor to QuickTime. 
      p.setContentDescriptor(new ContentDescriptor(FileTypeDescriptor.QUICKTIME));

      // Query for the processor for supported formats.
      // Then set it on the processor.
      TrackControl tcs[] = p.getTrackControls();
      Format f[] = tcs[0].getSupportedFormats();
      if (f == null || f.length <= 0) {
         System.err.println("The mux does not support the input format: " + tcs[0].getFormat());
         return false;
      }
      for (int i=0; i<f.length; i++)
      { System.out.println ("supported formats: " + f[i]); 
      }

      tcs[0].setFormat(f[0]);

      System.err.println("Setting the track format to: " + f[0]);

      // We are done with programming the processor.  Let's just
      // realize it.
      p.realize();
      if (!waitForState(p, Controller.Realized)) {
         System.err.println("Failed to realize the processor.");
         return false;
      }

      // Now, we'll need to create a DataSink.
      DataSink dsink;
      if ((dsink = createDataSink(p, outML)) == null) {
         System.err.println("Failed to create a DataSink for the given output MediaLocator: " + outML);
         return false;
      }

      dsink.addDataSinkListener(this);
      fileDone = false;

      System.err.println("start processing...");

      // OK, we can now start the actual transcoding.
      try {
         p.start();
         dsink.start();
      } catch (IOException e) {
         System.err.println("IO error during processing");
         return false;
      }

      // Wait for EndOfStream event.
      waitForFileDone();

      // Cleanup.
      try {
         dsink.close();
      } catch (Exception e) {}
      p.removeControllerListener(this);

      System.err.println("...done processing.");

      return true;
   }


   /**
    * Create the DataSink.
    */
   DataSink createDataSink(Processor p, MediaLocator outML) {

     DataSource ds;

     if ((ds = p.getDataOutput()) == null) {
       System.err.println("Something is really wrong: the processor does not have an output DataSource");
       return null;
     }

     DataSink dsink;

     try {
       //System.err.println("- create DataSink for: " + outML);
       dsink = Manager.createDataSink(ds, outML);
       dsink.open();
     } catch (Exception e) {
       System.err.println("Cannot create the DataSink: " + e);
       return null;
     }

     return dsink;
   }


   Object waitSync = new Object();
   boolean stateTransitionOK = true;

   /**
    * Block until the processor has transitioned to the given state.
    * Return false if the transition failed.
    */
   boolean waitForState(Processor p, int state) {
     synchronized (waitSync) {
       try {
         while (p.getState() < state && stateTransitionOK)
            waitSync.wait();
       } catch (Exception e) {}
     }
     return stateTransitionOK;
   }


   /**
    * Controller Listener.
    */
   public void controllerUpdate(ControllerEvent evt) {

     if (evt instanceof ConfigureCompleteEvent ||
         evt instanceof RealizeCompleteEvent ||
         evt instanceof PrefetchCompleteEvent) {
       synchronized (waitSync) {
         stateTransitionOK = true;
         waitSync.notifyAll();
       }
     } else if (evt instanceof ResourceUnavailableEvent) {
       synchronized (waitSync) {
         stateTransitionOK = false;
         waitSync.notifyAll();
       }
     } else if (evt instanceof EndOfMediaEvent) {
       evt.getSourceController().stop();
       evt.getSourceController().close();
     }
   }


   Object waitFileSync = new Object();
   boolean fileDone = false;
   boolean fileSuccess = true;

   /**
    * Block until file writing is done. 
    */
   boolean waitForFileDone() {
     synchronized (waitFileSync) {
       try {
         while (!fileDone)
            waitFileSync.wait();
       } catch (Exception e) {}
     }
     return fileSuccess;
   }


   /**
    * Event handler for the file writer.
    */
   public void dataSinkUpdate(DataSinkEvent evt) {

     if (evt instanceof EndOfStreamEvent) {
       synchronized (waitFileSync) {
         fileDone = true;
         waitFileSync.notifyAll();
       }
     } else if (evt instanceof DataSinkErrorEvent) {
       synchronized (waitFileSync) {
         fileDone = true;
         fileSuccess = false;
         waitFileSync.notifyAll();
       }
     }
   }



   /**
    * Create a media locator from the given string.
    */
   static MediaLocator createMediaLocator(String url) {

     MediaLocator ml;

     if (url.indexOf(":") > 0 && (ml = new MediaLocator(url)) != null)
        return ml;

     if (url.startsWith(File.separator)) {
       if ((ml = new MediaLocator("file:" + url)) != null) {
          return ml;
       }
     } else {
       String file = "file:" + System.getProperty("user.dir") + File.separator + url;
       if ((ml = new MediaLocator(file)) != null) {
          return ml;
       }
     }

     return null;
   }


   ///////////////////////////////////////////////
   //
   // Inner classes.
   ///////////////////////////////////////////////


   /**
    * A DataSource to read from a list of image files and
    * turn that into a stream of JMF buffers.
    * The DataSource is not seekable or positionable.
    */
   class ImageDataSource extends PullBufferDataSource {

     ImageSourceStream streams[];

     ImageDataSource(int width, int height, int frameRate, Vector<String> images) {
       streams = new ImageSourceStream[1];
       streams[0] = new ImageSourceStream(width, height, frameRate, images);
     }

     public void setLocator(MediaLocator source) {
     }

     public MediaLocator getLocator() {
       return null;
     }

     /**
      * Content type is of RAW since we are sending buffers of video
      * frames without a container format.
      */
     public String getContentType() {
       return ContentDescriptor.RAW;
     }

     public void connect() {
     }

     public void disconnect() {
     }

     public void start() {
     }

     public void stop() {
     }

     /**
      * Return the ImageSourceStreams.
      */
     public PullBufferStream[] getStreams() {
       return streams;
     }

     /**
      * We could have derived the duration from the number of
      * frames and frame rate.  But for the purpose of this program,
      * it's not necessary.
      */
     public Time getDuration() {
       return DURATION_UNKNOWN;
     }

     public Object[] getControls() {
       return new Object[0];
     }

     public Object getControl(String type) {
       return null;
     }
    }


   /**
    * The source stream to go along with ImageDataSource.
    */
   class ImageSourceStream implements PullBufferStream {

     Vector<String> images;
     int width, height;
     VideoFormat format;

     int nextImage = 0;	// index of the next image to be read.
     boolean ended = false;

     public ImageSourceStream(int width, int height, int frameRate, Vector<String> images) {
       this.width = width;
       this.height = height;
       this.images = images;

       format = new VideoFormat(VideoFormat.JPEG,
                                new Dimension(width, height),
                                Format.NOT_SPECIFIED,
                                Format.byteArray,
                                (float)frameRate);
     }

     /**
      * We should never need to block assuming data are read from files.
      */
     public boolean willReadBlock() {
       return false;
     }

     /**
      * This is called from the Processor to read a frame worth
      * of video data.
      */
     public void read(Buffer buf) throws IOException {

       // Check if we've finished all the frames.
       if (nextImage >= images.size()) {
         // We are done.  Set EndOfMedia.
         //System.err.println("Done reading all images.");
         buf.setEOM(true);
         buf.setOffset(0);
         buf.setLength(0);
         ended = true;
         return;
       }

       String imageFile = images.elementAt(nextImage);
       nextImage++;

       //System.err.println("  - reading image file: " + imageFile);

       // Open a random access file for the next image. 
       RandomAccessFile raFile;
       raFile = new RandomAccessFile(imageFile, "r");

       byte data[] = null;

       // Check the input buffer type & size.

       if (buf.getData() instanceof byte[])
          data = (byte[])buf.getData();

       // Check to see the given buffer is big enough for the frame.
       if (data == null || data.length < raFile.length()) {
         data = new byte[(int)raFile.length()];
         buf.setData(data);
       }

       // Read the entire image from the file.
       raFile.readFully(data, 0, (int)raFile.length());

       //System.err.println("    read " + raFile.length() + " bytes.");

       buf.setOffset(0);
       buf.setLength((int)raFile.length());
       buf.setFormat(format);
       buf.setFlags(buf.getFlags() | Buffer.FLAG_KEY_FRAME);

       // Close the random access file.
       raFile.close();
     }

     /**
      * Return the format of each video frame. 
      */
     public Format getFormat() {
       return format;
     }

     public ContentDescriptor getContentDescriptor() {
       return new ContentDescriptor(ContentDescriptor.RAW);
     }

     public long getContentLength() {
       return 0;
     }

     public boolean endOfStream() {
       return ended;
     }

     public Object[] getControls() {
       return new Object[0];
     }

     public Object getControl(String type) {
       return null;
     }
    }
        
        
}
