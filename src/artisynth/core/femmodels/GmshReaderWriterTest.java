/**
 * Copyright (c) 2024, by the Authors: John E Lloyd (UBC), Isaac McKay (USASK).
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Set;

import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.*;
import maspack.util.*;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.modelbase.ComponentList;

public class GmshReaderWriterTest extends FemReaderWriterTestBase {

   private boolean myWriteMshFiles;

   protected FemReaderBase createFemReader (Reader r) throws IOException {
      return new GmshReader (r);
   }

   protected FemWriterBase createFemWriter (PrintWriter pw) throws IOException {
      return new GmshWriter (pw);
   }   

   String myHexGridStr = String.join (
      "\n",
      "$MeshFormat",
      "4.1 0 8",
      "$EndMeshFormat",
      "$Entities",
      "0 0 0 1",
      "1 -0.5 -0.5 -0.5 0.5 0.5 0.5 0 0",
      "$EndEntities",
      "$Nodes",
      "1 12 1 12",
      "3 1 0 12",
      "1",
      "2",
      "3",
      "4",
      "5",
      "6",
      "7",
      "8",
      "9",
      "10",
      "11",
      "12",
      "-0.5 -0.5 -0.5",
      "0.5 -0.5 -0.5",
      "-0.5 0.5 -0.5",
      "0.5 0.5 -0.5",
      "-0.5 -0.5 0.0",
      "0.5 -0.5 0.0",
      "-0.5 0.5 0.0",
      "0.5 0.5 0.0",
      "-0.5 -0.5 0.5",
      "0.5 -0.5 0.5",
      "-0.5 0.5 0.5",
      "0.5 0.5 0.5",
      "$EndNodes",
      "$Elements",
      "1 2 1 2",
      "3 1 5 2",
      "1 5 7 8 6 1 3 4 2",
      "2 9 11 12 10 5 7 8 6",
      "$EndElements\n");

   // specifies a hex grid plus extra items that should be ignored
   String myHexGridPlusUnusedStr = String.join (
      "\n",
      "$MeshFormat",
      "4.1 0 8",
      "$EndMeshFormat",
      "$Entities",
      "0 0 1 1",
      "2 -1.5 -0.5 0.5 1.5 0.5 1.5 0 0",
      "1 -0.5 -0.5 -0.5 0.5 0.5 0.5 0 0",
      "$EndEntities",
      "$Nodes",
      "2 16 1 16",
      "3 1 0 12",
      "1",
      "2",
      "3",
      "4",
      "5",
      "6",
      "7",
      "8",
      "9",
      "10",
      "11",
      "12",
      "-0.5 -0.5 -0.5",
      "0.5 -0.5 -0.5",
      "-0.5 0.5 -0.5",
      "0.5 0.5 -0.5",
      "-0.5 -0.5 0.0",
      "0.5 -0.5 0.0",
      "-0.5 0.5 0.0",
      "0.5 0.5 0.0",
      "-0.5 -0.5 0.5",
      "0.5 -0.5 0.5",
      "-0.5 0.5 0.5",
      "0.5 0.5 0.5",
      "1 1 0 4", // ignore nodes: entity 1, dim 1, non parametric, 4 nodes
      "13",
      "14",
      "15",
      "16",
      "-0.5 -0.5 -0.5",
      "0.5 -0.5 -0.5",
      "-0.5 0.5 -0.5",
      "0.5 0.5 -0.5",
      "$EndNodes",
      "$Elements",
      "3 7 1 7",
      "3 1 5 2",
      "1 5 7 8 6 1 3 4 2",
      "2 9 11 12 10 5 7 8 6",
      "3 1 22 3", // ignore: dim=3, entity=1, type=22, 3 elements
      "5 5 7 8 6 1 2",    // these element defs are meaningless ...
      "6 9 11 12 10 5 6",
      "7 5 7 8 11 14 13",
      "1 1 30 2", // ignore: dim=1, entity=1, type=30, 2 elements
      "4 13 12 16",      // these element defs are meaningless ...
      "5 13 14 16",
      "$EndElements\n");

   public GmshReaderWriterTest (boolean writeFiles) {
      myWriteMshFiles = writeFiles;
   }

   public void test (FemModel3d fem) {
      try {
         int num0 = fem.getNode(0).getNumber();
         String out = writeToString (fem);
         FemModel3d femChk = readFromString (out, 0);
         checkNumbering (femChk, 0, 0);
         if (num0 != 10) {
            String outChk = writeToString (femChk);
            if (!outChk.equals (out)) {
               System.out.println ("OUTPUT:\n" + out);
               System.out.println ("CHECK:\n" + outChk);
               throw new TestException (
                  "write/read/write does not equal write");
            }
         }
         femChk = readFromString (out, FemReaderBase.PRESERVE_NUMBERING);
         int base = 1;
         if (num0 == 10) {
            base = fem.getOneBasedNodeElementNumbering() ? 10 : 11;
         }
         checkNumbering (femChk, base, base+fem.numElements());
         String outChk = writeToString (femChk);
         if (!outChk.equals (out)) {
            System.out.println ("OUTPUT:\n" + out);
            System.out.println ("CHECK:\n" + outChk);
            throw new TestException (
               "write/read/write does not equal write");
         }
         femChk = readFromString (out, FemReaderBase.PRESERVE_NUMBERING_BASE0);
         checkNumbering (femChk, base-1, base-1+fem.numElements());
         if (myWriteMshFiles && fem.getName() != null) {
            GmshWriter.write (fem.getName()+".msh", fem);
         }
      }
      catch (IOException e) {
         throw new TestException ("write/read failed", e);
      }
   }

   public void testWithUnusedItems() {
      FemModel3d fem;
      String output = null;
      try {
         fem = readFromString (
            myHexGridPlusUnusedStr, GmshReader.SUPPRESS_WARNINGS);
         output = writeToString (fem);
      }
      catch (IOException e) {
         throw new TestException ("Error reading/writing", e);
      }
      if (!output.equals (myHexGridStr)) {
         throw new TestException (
            "reading input with unused items creates different model");
      }
   }

   public void test() {
      for (FemModel3d fem : createTestFems()) {
         test (fem);
      }
      testWithUnusedItems();
   }

   public static void main (String[] args) throws IOException {
      boolean writeFiles = false;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-writeFiles")) {
            writeFiles = true;
         }
         else {
            System.out.println ("Arguments: [-writeFiles]");
            System.exit(1);
         }
      }
      
      GmshReaderWriterTest tester = new GmshReaderWriterTest(writeFiles);
      tester.runtest();
   }

}
