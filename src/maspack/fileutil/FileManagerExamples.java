/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.fileutil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class FileManagerExamples {

   private static String host = "http://www.artisynth.org/files/";
   private static String local = "tmp/";
   
   private static String example1_file = "lib/argparser.jar";
   private static String example2_file = "lib/jass.jar";
   
   private static String example3_folder = "lib/Windows64";
   private static String[] example3_files = {"jogl.dll", "jogl_awt.dll", "jogl_cg.dll"};
   
   private static String example5_lib = "looks.1.1.jar";
   private static String example5_lib_dir = "lib";
      
   public static void example1() {
      
      System.out.println("Running example1...");
      
      // get a single big file
      FileManager grabber = new FileManager();
      try {
         File file = grabber.get (local+example1_file, host+example1_file);
         FileReader reader = new FileReader (file);
         
         // read a character
         System.out.println( file.getPath() + "[0]: " + reader.read() );
         reader.close();
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      
   }
   
   public static void example2() {
      
      System.out.println("Running example2...");
      
      // get a single big file
      try {
         FileReader reader = 
            new FileReader( 
               FileManager.staticGet(local+example2_file, host+example1_file));
         
         // read a character
         System.out.println( local+example2_file + "[0]: " + reader.read() );
         reader.close();
      }
      catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   public static void example3() {
      
      System.out.println("Running example3...");
      
      // get a bunch of files:
      FileManager grabber =
         new FileManager (local+example3_folder, host + example3_folder);

      try {
         for (int i=0; i<example3_files.length; i++) {
            FileReader reader =
               new FileReader (grabber.get (example3_files[i], FileManager.CHECK_HASH));
            
            System.out.println( example3_files[i]+ "[0]: " + reader.read() );
            reader.close();
         }
      }
      catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   public static void example4() {
      // download a directory (in actuality, will probably be a 'component')
   }
   
   
   public static void example5(boolean updateRequested) {
      
      System.out.println("Running example5...");
      
      // get a library file. Names of needed library files obtained from a config
      // file; e.g., $ARTISYNTH_HOME/lib/LIB_MANIFEST
      FileManager grabber =
         new FileManager(example5_lib_dir, host + example5_lib_dir);

      int options = 0;
      if (updateRequested) {
         // select this if we want to try to get the latest version of the file
         options = FileManager.CHECK_HASH;
      }
      try {
         File libFile = grabber.get (example5_lib, options);
         System.out.println("Successfully loaded library " + libFile.toString());
      }
      catch (Exception e) {
         System.out.println (
            "Error: library "+example5_lib+" not present and cannot be loaded: "+e);
      }
   }
   
   public static void errorChecks() {
      
      System.out.println("Running errorChecks ...");
      
      // no internet
      FileManager fg = new FileManager("tmp", "http://www.ece.ubc.ca.err");
      File f;
      
      try {
         f = fg.getRemote("mesh.elem");
      } catch (FileTransferException e) {
         System.err.println(e.getMessage());
      }
      
      fg.setRemoteSource("http://www.ece.ubc.ca/~antonios/files");
      try {
         f = fg.getRemote("fake.file");
      } catch (FileTransferException e) {
         System.err.println(e.getMessage());
      }

      try {
         String fhash = fg.getLocalHash("fake.file");
         System.out.println("Hash: " + fhash);
      } catch (FileTransferException e) {
         System.err.println(e.getMessage());
      }
      
      try {
         String fhash = fg.getRemoteHash("mesh.elem");
         f = fg.getRemote("mesh.elem"); 
         System.out.println("Hash: " + fhash + ", " + fg.getLocalHash(f));
      } catch (FileTransferException e) {
         System.err.println(e.getMessage());
      }
      
      
   }
   
   public static void main(String args[]) {
//      example1();
//      example2();
//      example3();
//      // example4();
//      example5(true);
      errorChecks();
   }
   
}
