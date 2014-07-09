package maspack.fileutil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class FileGrabberExamples {

   private static String host = "http://www.magic.ubc.ca/artisynth/files/";
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
      FileGrabber grabber = new FileGrabber();
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
               FileGrabber.staticGet(local+example2_file, host+example1_file));
         
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
      FileGrabber grabber =
         new FileGrabber (local+example3_folder, host + example3_folder);

      try {
         for (int i=0; i<example3_files.length; i++) {
            FileReader reader =
               new FileReader (grabber.get (example3_files[i], FileGrabber.CHECK_HASH));
            
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
      FileGrabber grabber =
         new FileGrabber(example5_lib_dir, host + example5_lib_dir);

      int options = 0;
      if (updateRequested) {
         // select this if we want to try to get the latest version of the file
         options = FileGrabber.CHECK_HASH;
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
      FileGrabber fg = new FileGrabber("tmp", "http://www.ece.ubc.ca.err");
      File f;
      
      try {
         f = fg.getRemote("mesh.elem");
      } catch (FileGrabberException e) {
         System.err.println(e.getMessage());
      }
      
      fg.setRemoteSource("http://www.ece.ubc.ca/~antonios/files");
      try {
         f = fg.getRemote("fake.file");
      } catch (FileGrabberException e) {
         System.err.println(e.getMessage());
      }

      try {
         String fhash = fg.getLocalHash("fake.file");
         System.out.println("Hash: " + fhash);
      } catch (FileGrabberException e) {
         System.err.println(e.getMessage());
      }
      
      try {
         String fhash = fg.getRemoteHash("mesh.elem");
         f = fg.getRemote("mesh.elem"); 
         System.out.println("Hash: " + fhash + ", " + fg.getLocalHash(f));
      } catch (FileGrabberException e) {
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
