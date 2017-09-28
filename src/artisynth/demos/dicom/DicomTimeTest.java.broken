package artisynth.demos.dicom;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.renderables.DicomViewer;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.fileutil.FileManager;
import maspack.image.dicom.DicomImage;
import maspack.image.dicom.DicomImageDecoderImageMagick;
import maspack.image.dicom.DicomReader;

/**
 * DICOM image of the heart, with time
 *
 */
public class DicomTimeTest extends RootModel {

   String dicom_url = "http://www.osirix-viewer.com/datasets/DATA/MAGIX.zip";
   String dicom_root = "MAGIX/Cardiaque Cardiaque_standard (Adulte)/";
   String[] dicom_folders = new String[] {
       "Cir  CardiacCirc  3.0  B20f  0-90% RETARD_DECLECHEMENT 0 % - 10",
       "Cir  CardiacCirc  3.0  B20f  0-90% RETARD_DECLECHEMENT 10 % - 10",
       "Cir  CardiacCirc  3.0  B20f  0-90% RETARD_DECLECHEMENT 20 % - 10",
       "Cir  CardiacCirc  3.0  B20f  0-90% RETARD_DECLECHEMENT 30 % - 10",
       "Cir  CardiacCirc  3.0  B20f  0-90% RETARD_DECLECHEMENT 40 % - 10",
       "Cir  CardiacCirc  3.0  B20f  0-90% RETARD_DECLECHEMENT 50 % - 10",
       "Cir  CardiacCirc  3.0  B20f  0-90% RETARD_DECLECHEMENT 60 % - 10",
       "Cir  CardiacCirc  3.0  B20f  0-90% RETARD_DECLECHEMENT 70 % - 10",
       "Cir  CardiacCirc  3.0  B20f  0-90% RETARD_DECLECHEMENT 80 % - 10",
       "Cir  CardiacCirc  3.0  B20f  0-90% RETARD_DECLECHEMENT 90 % - 10"};
   
   DicomViewer dcp;
   
   public void build(String[] args) throws IOException {  

      // check for ImageMagick
      boolean hasImageMagick = DicomImageDecoderImageMagick.checkForImageMagick();
      if (!hasImageMagick) {
         throw new RuntimeException("This demo requires ImageMagick to be installed and available on the system PATH.");
      }
      
      // prepare utility for downloading DICOM sample
      String localDir = ArtisynthPath.getSrcRelativePath(this, "data/MAGIX");
      FileManager fileManager = new FileManager(localDir, "zip:" + dicom_url + "!/");
      fileManager.setConsoleProgressPrinting(true);
      fileManager.setOptions(FileManager.DOWNLOAD_ZIP); // download zip file first
      
      // download dicom data
      File dicomPath = fileManager.get(dicom_root); // extract all of the dicom root
      
      // 
      DicomImage im = null;
      try {
         DicomReader rs = new DicomReader();
         
         // load all dicom volume sequences, specifying temporal position
         for (int i=0; i<dicom_folders.length; i++) {
            File  file = new File(dicomPath, dicom_folders[i]);
            if (!file.exists()) {
               fileManager.get(dicom_root + dicom_folders[i]);
            }
            
            // restrict to files ending in .dcm
            im = rs.read(im, file.getAbsolutePath(), Pattern.compile(".*\\.dcm"), false, i);
         }
         System.out.println(im);
      } catch (IOException e) {
         e.printStackTrace();
         throw new RuntimeException("Failed to load image", e);
      }

      dcp = new DicomViewer("Beating Heart", im);
      addRenderable(dcp);
   }
   
   @Override
   public StepAdjustment advance(double t0, double t1, int flags) {
      
      // adjust time index in image
      int tidx = (int)(t0*10);
      tidx = tidx % (dcp.getImage().getNumTimes());
      dcp.setTimeIndex(tidx);
      
      return super.advance(t0, t1, flags);
   }

   @Override
   public void attach(DriverInterface driver) {
      super.attach(driver);
      getMainViewer().setBackgroundColor(Color.WHITE);
   }
   
   @Override
   public String getAbout() {
      return "Loads and displays a time-dependent DICOM image of the heart, which is automatically "
         + "downloaded from www.osirix-viewer.com.  ImageMagick is required to extract "
         + "and convert the image data to a usable form.";
   }
   
}
