package artisynth.demos.dicom;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import artisynth.core.renderables.DicomViewer;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.fileutil.FileManager;

/**
 * DICOM image of the brain, raw encoding, using REGEX to limit files
 */
public class DicomTest extends RootModel {

   String dicom_url = "http://barre.nom.fr/medical/samples/files/MR-MONO2-8-16x-heart.gz";
   String dicom_file = "/MR-MONO2-8-16x-heart";
   
   public void build(String[] args) throws IOException {
      
      // download the BRAINIX dicom data if it does not already exist
      String localDir = ArtisynthPath.getSrcRelativePath(this, "data/MONO2_HEART");
      FileManager fileManager = new FileManager(localDir, "gz:" + dicom_url + "!/");
      fileManager.setConsoleProgressPrinting(true);
      fileManager.setOptions(FileManager.DOWNLOAD_ZIP); // download zip file first
      File dicomPath = fileManager.get(dicom_file);     // extract
      
     
      DicomViewer dcp = new DicomViewer("Heart", dicomPath.getAbsolutePath(), 
         null, /*check subdirectories*/ false);
      
      addRenderable(dcp);
      
   }

   @Override
   public void attach(DriverInterface driver) {
      super.attach(driver);
      
      getMainViewer().setBackgroundColor(Color.WHITE);
   }
   
   @Override
   public String getAbout() {
      return "Loads and displays a DICOM image of the heart";
   }
   
}
