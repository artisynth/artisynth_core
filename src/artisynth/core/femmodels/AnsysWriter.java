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
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import maspack.util.NumberFormat;
import maspack.matrix.Point3d;

public class AnsysWriter implements FemWriter {
   
   public static void writeNodeFile (FemModel3d fem, String fileName) {
      try {
         PrintWriter pw = new PrintWriter (new File (fileName));
         writeNodeFile (fem, pw);
      }
      catch (FileNotFoundException e) {
         e.printStackTrace();
      }
   }

   
   public static void writeNodeFile (FemModel3d fem, PrintWriter nodeWriter) {
      NumberFormat fmt1 = new NumberFormat ("%8i");
      NumberFormat fmt2 = new NumberFormat ("%19.13G");
      
      int offset = (fem.getByNumber (0) == null) ? 0 : 1;

      for (FemNode3d n : fem.getNodes()) {
         nodeWriter.print (fmt1.format (n.getNumber () + offset));
         nodeWriter.println (" " + fmt2.format (n.getPosition().x) +
            " " + fmt2.format (n.getPosition().y) +
            " " + fmt2.format (n.getPosition().z));
      }

      nodeWriter.close ();
   }
   
   public static void writeMarkerFile (FemModel3d fem, String fileName) {
      NumberFormat fmt1 = new NumberFormat ("%8i");
      NumberFormat fmt2 = new NumberFormat ("%20.13G");
      
      try {
         PrintWriter pw = new PrintWriter (new File (fileName));
         
         HashMap<Integer, Point3d> markerMap = new HashMap<Integer, Point3d> ();
         
         for (FemMarker m : fem.markers()) {
            markerMap.put (m.getNumber (), m.getPosition ());
         }
//         for (FemElement3d e : fem.getElements ()) {
//            for (FemMarker m : fem.getMarkersInElement (e)) {
//               markerMap.put (m.getNumber (), m.getPosition ());
//            }
//         }
         
         SortedSet<Integer> sortedIds = new TreeSet<Integer> (markerMap.keySet ());
         Iterator<Integer> it = sortedIds.iterator();

         while (it.hasNext()) {
            int markerId = it.next ();
            Point3d pos = markerMap.get (markerId);
            pw.println (fmt1.format (markerId) + " " + pos.toString (fmt2));
         }
         
         pw.close ();
      }
      catch (IOException e) {
         e.printStackTrace ();
      }
   }
   
   public static void writeElemFile (FemModel3d fem, String fileName) {
      try {
         PrintWriter pw = new PrintWriter (new File (fileName));
         writeElemFile (fem, pw);
      }
      catch (FileNotFoundException e) {
         e.printStackTrace();
      }
   }
   
   public static void writeElemFile (FemModel3d fem, PrintWriter elemWriter) {
      NumberFormat dfmt = new NumberFormat ("%6d");

      int nodeOffset = (fem.getByNumber (0) == null) ? 0 : 1;
      int elemOffset = (fem.getElementByNumber (0) == null) ? 0 : 1;

      for (FemElement3d e : fem.getElements()) {
         FemNode3d[] nodes = e.getNodes();
         int[] firstRow = new int[13];
         int[] secondRow = null;

         if (e instanceof TetElement) {
            firstRow[0] = nodes[0].getNumber () + nodeOffset;
            firstRow[1] = nodes[1].getNumber () + nodeOffset;
            firstRow[2] = nodes[2].getNumber () + nodeOffset;
            firstRow[3] = nodes[3].getNumber () + nodeOffset;
            firstRow[4] = 0;
            firstRow[5] = 0;
            firstRow[6] = 0;
            firstRow[7] = 0;
         }
         else if (e instanceof PyramidElement) {
             firstRow[0] = nodes[0].getNumber () + nodeOffset;
             firstRow[1] = nodes[1].getNumber () + nodeOffset;
             firstRow[2] = nodes[2].getNumber () + nodeOffset;
             firstRow[3] = nodes[3].getNumber () + nodeOffset;
             firstRow[4] = nodes[4].getNumber () + nodeOffset;
             firstRow[5] = nodes[4].getNumber () + nodeOffset;
             firstRow[6] = nodes[4].getNumber () + nodeOffset;
             firstRow[7] = nodes[4].getNumber () + nodeOffset;
         }
         else if (e instanceof WedgeElement) {
            firstRow[0] = nodes[0].getNumber () + nodeOffset;
            firstRow[1] = nodes[1].getNumber () + nodeOffset;
            firstRow[2] = nodes[2].getNumber () + nodeOffset;
            firstRow[3] = nodes[2].getNumber () + nodeOffset;
            firstRow[4] = nodes[3].getNumber () + nodeOffset;
            firstRow[5] = nodes[4].getNumber () + nodeOffset;
            firstRow[6] = nodes[5].getNumber () + nodeOffset;
            firstRow[7] = nodes[5].getNumber () + nodeOffset;
         }
         else if (e instanceof HexElement) {
            firstRow[0] = nodes[3].getNumber () + nodeOffset;
            firstRow[1] = nodes[2].getNumber () + nodeOffset;
            firstRow[2] = nodes[1].getNumber () + nodeOffset;
            firstRow[3] = nodes[0].getNumber () + nodeOffset;
            firstRow[4] = nodes[7].getNumber () + nodeOffset;
            firstRow[5] = nodes[6].getNumber () + nodeOffset;
            firstRow[6] = nodes[5].getNumber () + nodeOffset;
            firstRow[7] = nodes[4].getNumber () + nodeOffset;
         }
         else if (e instanceof QuadtetElement) {
            firstRow[0] = nodes[0].getNumber () + nodeOffset;
            firstRow[1] = nodes[1].getNumber () + nodeOffset;
            firstRow[2] = nodes[2].getNumber () + nodeOffset;
            firstRow[3] = nodes[3].getNumber () + nodeOffset;
            firstRow[4] = nodes[4].getNumber () + nodeOffset;
            firstRow[5] = nodes[5].getNumber () + nodeOffset;
            firstRow[6] = nodes[6].getNumber () + nodeOffset;
            firstRow[7] = nodes[7].getNumber () + nodeOffset;

            secondRow = new int[2];
            secondRow[0] = nodes[8].getNumber () + nodeOffset;
            secondRow[1] = nodes[9].getNumber () + nodeOffset;
         }
         else if (e instanceof QuadhexElement) {
            firstRow[0] = nodes[3].getNumber () + nodeOffset;
            firstRow[1] = nodes[2].getNumber () + nodeOffset;
            firstRow[2] = nodes[1].getNumber () + nodeOffset;
            firstRow[3] = nodes[0].getNumber () + nodeOffset;
            firstRow[4] = nodes[7].getNumber () + nodeOffset;
            firstRow[5] = nodes[6].getNumber () + nodeOffset;
            firstRow[6] = nodes[5].getNumber () + nodeOffset;
            firstRow[7] = nodes[4].getNumber () + nodeOffset;

            secondRow = new int[12];
            secondRow[0] = nodes[10].getNumber () + nodeOffset;
            secondRow[1] = nodes[9].getNumber () + nodeOffset;
            secondRow[2] = nodes[8].getNumber () + nodeOffset;
            secondRow[3] = nodes[11].getNumber () + nodeOffset;
            secondRow[4] = nodes[14].getNumber () + nodeOffset;
            secondRow[5] = nodes[13].getNumber () + nodeOffset;
            secondRow[6] = nodes[12].getNumber () + nodeOffset;
            secondRow[7] = nodes[15].getNumber () + nodeOffset;
            secondRow[8] = nodes[19].getNumber () + nodeOffset;
            secondRow[9] = nodes[18].getNumber () + nodeOffset;
            secondRow[10] = nodes[17].getNumber () + nodeOffset;
            secondRow[11] = nodes[16].getNumber () + nodeOffset;
         }
         else {
            System.out.println ("Ignoring unknown element type: " + 
               e.getClass().getName());
            continue;
         }

         int[] elemAttr = fem.ansysElemProps.get (e);

         if (elemAttr != null) {
            for (int i = 8; i < 13; i++) {
               firstRow[i] = elemAttr[i - 8];
            }
         }
         else {
            for (int i = 8; i < 12; i++) {
               firstRow[i] = 1;
            }
            firstRow[12] = 0;
         }

         for (int i = 0; i < firstRow.length; i++) {
            elemWriter.print (dfmt.format (firstRow[i]));
         }

         int elemNum = e.getNumber () + elemOffset;

         elemWriter.println ("     " + elemNum);

         if ( secondRow != null ) {
            for (int j = 0; j < secondRow.length; j++) {
               elemWriter.print (dfmt.format (secondRow[j]));
            }
            elemWriter.println ();

            secondRow = null;
         }
      }

      elemWriter.close ();
   }
   
   private File myElemFile;
   private File myNodeFile;
   
   public AnsysWriter(File nodeFile, File elemFile) {
      myElemFile = elemFile;
      myNodeFile = nodeFile;
   }
   
   public AnsysWriter(String nodeFile, String elemFile) {
      myNodeFile = new File(nodeFile);
      myElemFile = new File(elemFile);
   }
   
   
   
   @Override
   public void writeFem(FemModel3d fem) throws IOException {
      writeNodeFile(fem, myNodeFile.getAbsolutePath());
      writeElemFile(fem, myElemFile.getAbsolutePath());
   }
   
}