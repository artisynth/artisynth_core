package artisynth.core.opensim;

import java.io.File;

// myCF File filter
public class OpenSimFileFilter extends javax.swing.filechooser.FileFilter {

   public static class Utils {

      public final static String xml = "xml";
      public final static String osim = "osim";

      /*
       * Get the extension of a file.
       */  
      public static String getExtension(File f) {
         String ext = null;
         String s = f.getName();
         int i = s.lastIndexOf('.');

         if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
         }
         return ext;
      }
   }

   public boolean accept(File f) {
      if (f.isDirectory()) {
         return true;
      }

      String extension = Utils.getExtension(f);
      if (extension != null) {
         if (extension.equals(Utils.xml) || extension.equals(Utils.osim))  {
            return true;
         } else {
            return false;
         }
      }
      return false;
   }

   public String getTypeDescription(File f) {
      String extension = Utils.getExtension(f);
      String type = null;

      if (extension != null) {
         if (extension.equals(Utils.xml) ||
         extension.equals(Utils.osim)) {
            type = "OpenSim file";
         }
      }
      return type;
   }

   @Override
   public String getDescription () {
      return "OpenSim files (*.xml, *.osim)";
   }   
}