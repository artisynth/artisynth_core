package artisynth.demos.dicom;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import artisynth.core.renderables.DicomViewer;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.fileutil.FileManager;
import maspack.util.PathFinder;

public class DicomTest extends RootModel {

   // Dicom file name and URL from which to load it
   String dicom_file = "MR-MONO2-8-16x-heart";
   String dicom_url =
      "http://www.artisynth.org/files/data/dicom/MR-MONO2-8-16x-heart.gz";
   
   public void build(String[] args) throws IOException {
      
      // cache image in a local directory 'data' beneath Java source
      String localDir = PathFinder.getSourceRelativePath(
         this, "data/MONO2_HEART");
      // create a file manager to get the file and download it if necessary
      FileManager fileManager = new FileManager(localDir, "gz:"+dicom_url+"!/");
      fileManager.setConsoleProgressPrinting(true);
      fileManager.setOptions(FileManager.DOWNLOAD_ZIP); // download zip file first

      // get the file from local directory, downloading first if needed
      File dicomPath = fileManager.get(dicom_file);
      
      // create a DicomViewer for the file
      DicomViewer dcp = new DicomViewer("Heart", dicomPath.getAbsolutePath(), 
         null, /*check subdirectories*/false);
      
      addRenderable(dcp); // add it to root model's list of renderable
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
