/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import maspack.util.NumberFormat;
import artisynth.core.modelbase.ComponentList;

public class TetGenWriter implements FemWriter {

   private boolean myZeroIndexed = true;

   public boolean isZetIndexed() {
      return myZeroIndexed;
   }

   public void setZeroIndexed (boolean enable) {
      myZeroIndexed = enable;
   }

   public static void writeNodeFile (FemModel3d fem, String fileName) {
      writeNodeFile (fem, fileName, /*zeroIndexed=*/true);
   }
   
   public static void writeNodeFile (
      FemModel3d fem, String fileName, boolean zeroIndexed) {
      try {
         PrintWriter pw = new PrintWriter (new File (fileName));
         NumberFormat fmt = new NumberFormat ("%19g");
         
         ComponentList<FemNode3d> nodeList = fem.getNodes ();
         
         int idxOff = zeroIndexed ? 0 : 1;
         pw.println (nodeList.size() + " 3 0 0");
         for (FemNode3d n : nodeList) {
            pw.print (n.getNumber()+idxOff);
            pw.println (" " + n.getPosition().toString (fmt));
         }
         
         pw.close ();
      }
      catch (IOException e) {
         e.printStackTrace ();
      }
   }

   public static void writeElemFile (FemModel3d fem, String fileName) {
      writeElemFile (fem, fileName, /*zeroIndexed=*/true);
   }
   
   public static void writeElemFile (
      FemModel3d fem, String fileName, boolean zeroIndexed) {
      try {
         PrintWriter pw = new PrintWriter (new File (fileName));
         NumberFormat dfmt = new NumberFormat ("%6d");
         
         ComponentList<FemElement3d> elemList = fem.getElements ();
         
         pw.println (elemList.size() + " 4 0");
         
         int idxOff = zeroIndexed ? 0 : 1;
         for (FemElement3d e : elemList) {
            FemNode3d[] nodes = e.getNodes();
            int[] firstRow = new int[4];
            
            if (e instanceof TetElement) {
               firstRow[0] = nodes[0].getNumber()+idxOff;
               firstRow[1] = nodes[1].getNumber()+idxOff;
               firstRow[2] = nodes[2].getNumber()+idxOff;
               firstRow[3] = nodes[3].getNumber()+idxOff;
            }
            else {
               System.out.println ("Unknown element type: " + 
                  e.getClass().getName());
               continue;
            }
            
            pw.print ((e.getNumber()+idxOff) + " ");
            
            for (int i = 0; i < firstRow.length; i++) {
               pw.print (dfmt.format (firstRow[i]));
            }
            
            pw.println ();
         }
         
         pw.close ();
      }
      catch (FileNotFoundException e) {
         e.printStackTrace();
      }
   }
   
   private File myElemFile;
   private File myNodeFile;
   
   public TetGenWriter(File nodeFile, File elemFile) {
      myElemFile = elemFile;
      myNodeFile = nodeFile;
   }
   
   public TetGenWriter(String nodeFile, String elemFile) {
      myNodeFile = new File(nodeFile);
      myElemFile = new File(elemFile);
   }
   
   @Override
   public void writeFem(FemModel3d fem) throws IOException {
      writeNodeFile(fem, myNodeFile.getAbsolutePath(), myZeroIndexed);
      writeElemFile(fem, myElemFile.getAbsolutePath(), myZeroIndexed);
   }
}
      
