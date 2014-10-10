/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import artisynth.core.modelbase.ComponentList;
import maspack.util.NumberFormat;

public class UCDWriter extends FemWriterBase {

   public static void write(FemModel3d fem, String fileName) {
      try {
         PrintWriter writer = new PrintWriter(fileName);
         write(fem, writer);
         writer.close();
      } catch (IOException e) {}

   }

   public static void write(FemModel3d fem, PrintWriter writer) {

      NumberFormat fmt = new NumberFormat("%10g");
      NumberFormat dfmt = new NumberFormat("%6d");

      ComponentList<FemNode3d> nodeList = fem.getNodes();
      ComponentList<FemElement3d> elementList = fem.getElements();

      writer.println(nodeList.size() + " " + elementList.size() + " 0 0 0");

      for (FemNode3d n : nodeList) {
         writer.print(n.getNumber());
         writer.println(" " + n.getPosition().toString(fmt));
      }

      for (FemElement3d e : elementList) {
         FemNode3d[] nodes = e.getNodes();
         int[] elemNodes = null;
         String elemType;

         if (e instanceof TetElement) {
            elemNodes = new int[4];
            elemNodes[0] = nodes[0].getNumber();
            elemNodes[1] = nodes[1].getNumber();
            elemNodes[2] = nodes[2].getNumber();
            elemNodes[3] = nodes[3].getNumber();
            elemType = "tet";
         } else if (e instanceof HexElement) {
            elemNodes = new int[8];
            elemNodes[0] = nodes[1].getNumber();
            elemNodes[1] = nodes[2].getNumber();
            elemNodes[2] = nodes[3].getNumber();
            elemNodes[3] = nodes[4].getNumber();
            elemNodes[4] = nodes[5].getNumber();
            elemNodes[5] = nodes[6].getNumber();
            elemNodes[6] = nodes[7].getNumber();
            elemNodes[7] = nodes[8].getNumber();
            elemType = "hex";
         } else {
            System.out.println("Unknown element type: "
               + e.getClass().getName());
            continue;
         }

         writer.print(e.getNumber() + " 0 " + elemType + " ");

         for (int i = 0; i < elemNodes.length; i++) {
            writer.print(dfmt.format(elemNodes[i]));
         }

         writer.println();
      }

   }

   protected UCDWriter(OutputStream os) {
      myOstream = os;
   }

   protected UCDWriter(File file) throws IOException {
      this(new FileOutputStream(file));
      myFile = file;
   }

   protected UCDWriter(String fileName) throws IOException {
      this(new File(fileName));
   }

   @Override
   public void writeFem(FemModel3d fem) throws IOException {
      write(fem, new PrintWriter(myOstream));
   }
}
