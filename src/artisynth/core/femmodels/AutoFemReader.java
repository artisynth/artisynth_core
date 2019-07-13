/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.File;
import java.io.IOException;

/**
 * Convenience routines for reading any FE file format
 * @author Antonio
 */
public class AutoFemReader implements FemReader {

   private File myFile;
   
   public enum FemFileType {
      ABAQUS,
      ANSYS,
      TETGEN
   }
   
   public AutoFemReader(File file) {
      myFile = file;
   }
   
   public AutoFemReader(String filename) {
      myFile = new File(filename);
   }
   
   @Override
   public FemModel3d readFem(FemModel3d fem) throws IOException {
      read(fem, myFile.getAbsolutePath(), null);
      return fem;
   }
   
   /**
    * Attempts to read a FEM file, filling in the model "model".  If {@code == null}, a new model is created.
    * @param model the model to assemble
    * @param fileName the file to read.  For multi-file formats like ANSYS and TETGEN, supply the element file name,
    *        or else it will try to find the corresponding ".ele" or ".elem" file to distinguish between them
    * @param type the file format (from {@code ABAQUS, ANSYS, TETGEN})
    * @return The created model
    * @throws IOException if there is a read error
    */
   public static FemModel3d read(FemModel3d model, String fileName, FemFileType type) throws IOException {
      
      if (model == null) {
         model = new FemModel3d();
      }
      
      if (type == null) {
         type = detectType(fileName);
      }
      
      if (type == null) {
         throw new IOException("Cannot determine file type for '" + fileName + "'");
      }
      
      String fileNameNoExtension = removeExtension(fileName);
      String nodeFileName = null;
      String elemFileName = null;
      switch(type) {
         case ABAQUS:
            AbaqusReader.read(model, fileName, 1, /*options=*/0);
            break;
         case ANSYS:
            nodeFileName = fileNameNoExtension + ".node";
            elemFileName = fileNameNoExtension + ".elem";
            AnsysReader.read(model, nodeFileName, elemFileName, 1, null, 0);
            break;
         case TETGEN:
            nodeFileName = fileNameNoExtension + ".node";
            elemFileName = fileNameNoExtension + ".ele";
            TetGenReader.read(model, 1, nodeFileName, elemFileName, null);
            break;
         default:
            break;
        
      }

      return model;
      
   }
   
   private static String removeExtension(String fileName) {
      File f = new File(fileName);
      String fname = f.getName();
      int idx = fname.lastIndexOf('.');
      if (idx > 0) {
         fname = fname.substring(0, idx);
      }
      fname = f.getParentFile().getAbsolutePath() + File.separator + fname;
      return fname;
   }
   
   private static String getFileExtension(String fileName) {
      File f = new File(fileName);
      String fname = f.getName();
      int idx = fname.lastIndexOf('.');
      if (idx > 0 && idx < fname.length()-1) {
         fname = fname.substring(idx+1, fname.length());
      } else {
         fname = "";
      }
      return fname;
   }
   
   /**
    * Currently only detects type by file extension.  For Abaqus use .inp,
    * Ansys .elem, and Tetgen .ele.  If a .node is supplied, the method will
    * try to find a corresponding .ele or .elem file.
    * 
    * @param fileName name of the model file
    * @return type of the file
    */
   public static FemFileType detectType(String fileName) {
      
      String ext = getFileExtension(fileName);
      ext = ext.toLowerCase();
      
      if ("inp".equals(ext)) {
         return FemFileType.ABAQUS;
      } else if ("elem".equals(ext)) {
         return FemFileType.ANSYS;
      } else if ("ele".equals(ext)) {
         return FemFileType.TETGEN;
      } else if ("node".equals(ext)) {
         // check for existing "ele" or "elem" file
         String baseName = removeExtension(fileName);
         String elemName = baseName + ".ele";
         File elemFile = new File(elemName);
         if (elemFile.exists()) {
            return FemFileType.TETGEN;
         }
          
         elemName = baseName + ".elem";
         elemFile = new File(elemName);
         if (elemFile.exists()) {
            return FemFileType.ANSYS;
         }
      }
      
      return null;
      
   }
   
}
