/**
 * Copyright (c) 2014, by the Authors: Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.moviemaker;

import java.io.File;
import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.protocol.DataSource;
import javax.media.datasink.*;
import java.util.Vector;

import maspack.util.InternalErrorException;

/**
 * Merged the tracks from different inputs and generate a QuickTime file
 * with the all the merged tracks.
 */
public class Merge implements ControllerListener, DataSinkListener {

   Vector sourcesURLs = new Vector(1);
   Processor [] processors = null;
   String outputFile = null;
   String videoEncoding = "JPEG";
   String audioEncoding = "LINEAR";
   String outputType = FileTypeDescriptor.QUICKTIME;
   DataSource [] dataOutputs = null;
   DataSource merger = null;
   DataSource outputDataSource;
   Processor outputProcessor;
   ProcessorModel outputPM;
   DataSink outputDataSink;
   MediaLocator outputLocator;
   boolean done = false;
    
   VideoFormat videoFormat = null;
   AudioFormat audioFormat = null;
    
   public Merge(String [] args) {
      parseArgs(args);
      if (sourcesURLs.size() < 2) {
         System.err.println("Need at least two source URLs");
         showUsage();
      } else {
         doMerge();

      }
   }

   private void doMerge() {
      int i = 0;
      processors = new Processor[sourcesURLs.size()];
      dataOutputs = new DataSource[sourcesURLs.size()];
	
      for (i = 0; i < sourcesURLs.size(); i++) {
         String source = (String) sourcesURLs.elementAt(i);
         MediaLocator ml = new MediaLocator(source);
         ProcessorModel pm = new MyPM(ml);
         try {
            processors[i] = Manager.createRealizedProcessor(pm);
            dataOutputs[i] = processors[i].getDataOutput();
            processors[i].start();
         } catch (Exception e) {
            throw new InternalErrorException (
               "Failed to create a processor: " + e);
         }
      }

      // Merge the data sources from the individual processors
      try {
         merger = Manager.createMergingDataSource(dataOutputs);
         merger.connect();
         merger.start();
      } catch (Exception e) {
         throw new InternalErrorException (
            "Failed to merge data sources: " + e);
      }
      if (merger == null) {
         throw new InternalErrorException (
            "Failed to merge data sources");
      }
      /*
        try {
        Player p = Manager.createPlayer(merger);
        new com.sun.media.ui.PlayerWindow(p);
        } catch (Exception e) {
        System.err.println("Failed to create player " + e);
        }
      */
	
      // Create the output processor
      ProcessorModel outputPM = new MyPMOut(merger);

      try {
         outputProcessor = Manager.createRealizedProcessor(outputPM);
         outputDataSource = outputProcessor.getDataOutput();
      } catch (Exception e) {
         throw new InternalErrorException (
            "Failed to create output processor: " + e);
      }

      try {
         outputLocator = new MediaLocator(outputFile);
         outputDataSink = Manager.createDataSink(outputDataSource,
                                                 outputLocator);
         outputDataSink.open();
      } catch (Exception e) {
         throw new InternalErrorException (
            "Failed to create output DataSink: " + e);
      }

      outputProcessor.addControllerListener(this);
      outputDataSink.addDataSinkListener(this);
      //                System.err.println("Merging...");
      try {
         outputDataSink.start();
         outputProcessor.start();
      } catch (Exception e) {
         throw new InternalErrorException (
            "Failed to start file writing: " + e);
      }
      int count = 0;
	
      while (!done) {
         try {
            Thread.currentThread().sleep(100);
         } catch (InterruptedException ie) {
         }
	    
         if (outputProcessor != null &&
             (int)(outputProcessor.getMediaTime().getSeconds()) > count) {
            System.err.print(".");
            count = (int)(outputProcessor.getMediaTime().getSeconds());
         }
	    
      }

      if (outputDataSink != null) {
         outputDataSink.close();
      }
      synchronized (this) {
         if (outputProcessor != null) {
            outputProcessor.close();
         }
      }
      //                System.err.println("Done!");
   }

   public void controllerUpdate(ControllerEvent ce) {
      if (ce instanceof EndOfMediaEvent) {
         synchronized (this) {
            outputProcessor.close();
            outputProcessor = null;
         }
      }
   }

   public void dataSinkUpdate(DataSinkEvent dse) {
      if (dse instanceof EndOfStreamEvent) {
         done = true;
      } else if (dse instanceof DataSinkErrorEvent) {
         done = true;
      }
   }

   class MyPM extends ProcessorModel {

      MediaLocator inputLocator;
	
      public MyPM(MediaLocator inputLocator) {
         this.inputLocator = inputLocator;
      }
	
      public ContentDescriptor getContentDescriptor() {
         return new ContentDescriptor(ContentDescriptor.RAW);
      }

      public DataSource getInputDataSource() {
         return null;
      }

      public MediaLocator getInputLocator() {
         return inputLocator;
      }

      public Format getOutputTrackFormat(int index) {
         return null;
      }

      public int getTrackCount(int n) {
         return n;
      }

      public boolean isFormatAcceptable(int index, Format format) {
         if (videoFormat == null) {
            videoFormat = new VideoFormat(videoEncoding);
         }
         if (audioFormat == null) {
            audioFormat = new AudioFormat(audioEncoding);
         }
         if (format.matches(videoFormat) || format.matches(audioFormat))
            return true;
         else
            return false;
      }
   }
    
   class MyPMOut extends ProcessorModel {

      DataSource inputDataSource;
	
      public MyPMOut(DataSource inputDataSource) {
         this.inputDataSource = inputDataSource;
      }
	
      public ContentDescriptor getContentDescriptor() {
         return new FileTypeDescriptor(outputType);
      }

      public DataSource getInputDataSource() {
         return inputDataSource;
      }

      public MediaLocator getInputLocator() {
         return null;
      }

      public Format getOutputTrackFormat(int index) {
         return null;
      }

      public int getTrackCount(int n) {
         return n;
      }

      public boolean isFormatAcceptable(int index, Format format) {
         if (videoFormat == null) {
            videoFormat = new VideoFormat(videoEncoding);
         }
         if (audioFormat == null) {
            audioFormat = new AudioFormat(audioEncoding);
         }
         if (format.matches(videoFormat) || format.matches(audioFormat))
            return true;
         else
            return false;
      }
   }

   private void showUsage() {
      System.err.println("Usage: Merge <url1> <url2> [<url3> ... ] [-o <out URL>] [-v <video_encoding>] [-a <audio_encoding>] [-t <content_type>]");
   }
    
   private void parseArgs(String [] args) {
      int i = 0;
      while (i < args.length) {
         if (args[i].equals("-h")) {
            showUsage();
         } else if (args[i].equals("-o")) {
            i++;
            outputFile = args[i];
         } else if (args[i].equals("-t")) {
            i++;
            outputType = args[i];
         } else if (args[i].equals("-v")) {
            i++;
            videoEncoding = args[i];
         } else if (args[i].equals("-a")) {
            i++;
            audioEncoding = args[i];
         } else {
            sourcesURLs.addElement(args[i]);
         }
         i++;
      }

      if (outputFile == null) {
         outputFile = "file:" + System.getProperty("user.dir") + 
            File.separator + "merged.mov";
      }
   }
    
   public static void main(String [] args) {
      new Merge(args);
      System.exit(0);
   }
}
