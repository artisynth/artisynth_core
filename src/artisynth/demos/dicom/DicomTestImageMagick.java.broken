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
import maspack.image.dicom.DicomImageDecoderImageMagick;

/**
 * Dicom image of the wrist, using ImageMagick to decode
 *
 */
public class DicomTestImageMagick extends RootModel {

   String dicom_url = "http://www.osirix-viewer.com/datasets/DATA/WRIX.zip";
   String dicom_folder = "WRIX/WRIX/WRIST RIGHT/T1 TSE COR RT. - 4";
   
   public void build(String[] args) throws IOException {
      
      // check for ImageMagick
      boolean hasImageMagick = DicomImageDecoderImageMagick.checkForImageMagick();
      if (!hasImageMagick) {
         throw new RuntimeException("This demo requires ImageMagick to be "
            + "installed and available on the system PATH.");
      }
      
      // grab remote zip file with DICOM data
      String localDir = ArtisynthPath.getSrcRelativePath(this, "data/WRIX");
      FileManager fileManager = new FileManager(localDir, "zip:" + dicom_url + "!/");
      fileManager.setConsoleProgressPrinting(true);
      fileManager.setOptions(FileManager.DOWNLOAD_ZIP); // download zip file first
      // download dicom image
      File dicomPath = fileManager.get(dicom_folder);
      
      // restrict to files ending in .dcm
      Pattern dcmPattern = Pattern.compile(".*\\.dcm");
      
      // add DicomViewer
      DicomViewer dcp = new DicomViewer("Wrist", dicomPath, dcmPattern);
      addRenderable(dcp);
      
   }

   @Override
   public void attach(DriverInterface driver) {
      super.attach(driver);
      getMainViewer().setBackgroundColor(Color.WHITE);
   }
   
   @Override
   public String getAbout() {
      return "Loads and displays a DICOM image of the wrist, which is automatically "
         + "downloaded from www.osirix-viewer.com.  ImageMagick is required to "
         + "extract and convert the image data to a usable form.";
   }
   
   
}
