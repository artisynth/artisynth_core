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
import artisynth.core.femmodels.AnsysCdbWriter.EBlockKey;

public class AnsysCdbReaderWriterTest extends FemReaderWriterTestBase {

   private boolean myWriteMshFiles;

   private EBlockKey myEBlockKey = EBlockKey.SOLID;
   private boolean myUseEtBlock = false;

   protected FemReaderBase createFemReader (Reader r) throws IOException {
      return new AnsysCdbReader (r);
   }

   protected FemWriterBase createFemWriter (PrintWriter pw) throws IOException {
      AnsysCdbWriter writer = new AnsysCdbWriter(pw);
      writer.setEBlockKey (myEBlockKey);
      writer.setUseEtBlock (myUseEtBlock);
      return writer;
   }   

   public AnsysCdbReaderWriterTest (boolean writeFiles) {
      myWriteMshFiles = writeFiles;
      createTestFems();
   }

   // tests for parsing format strings

   void testParseFormat (String str, int... chkLengths) {
      int[] lengths = AnsysCdbReader.parseFormat(str);
      if (lengths == null && chkLengths != null) {
         throw new TestException (
            "parseFormat: could not parse '"+str+"'");
      }
      else if (lengths != null && chkLengths == null) {
         throw new TestException (
            "parseFormat: unexpectedly parsed '"+str+"'");
      }
      if (lengths != null) {
         if (!ArraySupport.equals (lengths, chkLengths)) {
            throw new TestException (
               "parseFormat: expected "+new VectorNi(chkLengths)+
               ", got "+new VectorNi(lengths));
         }
      }
   }

   void testParseFormat() {
      testParseFormat ("", null);
      testParseFormat (" ", null);
      testParseFormat (" ( ", null);
      testParseFormat ("xx( ", null);
      testParseFormat ("(6i7", null);
      testParseFormat ("6i7)", null);
      testParseFormat ("(6i)", null);
      testParseFormat ("(6i)", null);
      testParseFormat ("(i)", null);
      testParseFormat ("(6i,i5)", null);
      testParseFormat ("(4i6)", 6, 6, 6, 6);
      testParseFormat (" (4i6) ", 6, 6, 6, 6);
      testParseFormat ("( 4i6 ) ", 6, 6, 6, 6);
      testParseFormat ("(3i6,2e12.5e2 ) ", 6, 6, 6, 12, 12);
      testParseFormat ("( 2i7,2I2, 4a9 ) ", 7, 7, 2, 2, 9, 9, 9, 9);
      testParseFormat ("( 2f6.4 , 2A4) ", 6, 6, 4, 4);
      testParseFormat (
         "(10I11, 12e8.2e2) ",
         11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 
         8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8);
   }

   // tests for parsing element numbers

   void testParseElementNumber(String name, int chk) {
      checkEquals (
         "parseElementNumber", AnsysCdbReader.parseElementNumber(name), chk);
   }

   void testParseElementNumber() {
      testParseElementNumber ("a", -1);
      testParseElementNumber ("", -1);
      testParseElementNumber ("SOLID", -1);
      testParseElementNumber ("SOLID0", 0);
      testParseElementNumber ("SOLID124", 124);
      testParseElementNumber ("shell7", 7);
      testParseElementNumber ("7", 7);
      testParseElementNumber ("181", 181);
   }      

   public void test (FemModel3d fem) {
      try {
         int num0 = fem.getNode(0).getNumber();
         String out = writeToString (fem);
         FemModel3d femNew = readFromString (out, 0);
         checkNumbering (femNew, 0, 0);
         if (num0 != 10) {
            String outNew = writeToString (femNew);
            checkOutputs (
               "write/read/write does not equal write", outNew, out);
         }
         femNew = readFromString (out, FemReaderBase.PRESERVE_NUMBERING);
         int base = 1;
         if (num0 == 10) {
            base = fem.getOneBasedNodeElementNumbering() ? 10 : 11;
         }
         checkNumbering (femNew, base, base+fem.numElements());
         String outNew = writeToString (femNew);
         checkOutputs (
            "write/read/write does not equal write", outNew, out);
         femNew = readFromString (out, FemReaderBase.PRESERVE_NUMBERING_BASE0);
         checkNumbering (femNew, base-1, base-1+fem.numElements());
         if (myWriteMshFiles && fem.getName() != null) {
            AnsysCdbWriter.write (fem.getName()+".msh", fem);
         }
      }
      catch (IOException e) {
         throw new TestException ("write/read failed", e);
      }
   }

   void writeTestFiles () throws IOException {

      myEBlockKey = EBlockKey.SOLID;
      myUseEtBlock = false;
      writeToFile ("allElems.cdb", myTestFems.get("allElems"));
      writeToFile ("special.cdb", createSpecial());
      myUseEtBlock = true;
      writeToFile ("allElemsEtBlock.cdb", myTestFems.get("allElems"));
      myUseEtBlock = false;
      myEBlockKey = EBlockKey.COMPACT;
      writeToFile ("allElemsCompact.cdb", myTestFems.get("allElems"));
      myEBlockKey = EBlockKey.SOLID;
      writeToFile ("allElemsOffset10.cdb", myTestFems.get("allElemsOffset10"));
   }

   protected FemModel3d createSpecial () {
      double widthX = 3.0;
      double widthY = 1.0;
      double widthZ = 1.0;
      int numY = 1;
      int numZ = 1;

      FemModel3d fem1 = createMixedElemGrid (
         widthX, widthY, widthZ, 4, numY, numZ, SHELL_SURFACE);
      FemModel3d fem0 = createMixedElemGrid (
         widthX, widthY, widthZ, 3, numY, numZ, QUAD);

      fem0.transformGeometry (new RigidTransform3d (0, 0, widthZ));
      fem1.transformGeometry (new RigidTransform3d (0, 0, -widthZ));
      //FemFactory.addFem (fem0, fem1);

      FemModel3d fem2 = FemFactory.createShellTriGrid (
         null, widthX, widthY, 2, 2, 0.01, /*membrane*/true);
      fem2.transformGeometry (new RigidTransform3d (0, 0, widthZ/4));
      //FemFactory.addFem (fem0, fem2);

      FemModel3d fem3 = FemFactory.createShellQuadGrid (
         null, widthX, widthY, 2, 2, 0.01, /*membrane*/false);
      fem3.transformGeometry (new RigidTransform3d (0, 0, -widthZ/4));
      //FemFactory.addFem (fem0, fem3);

      return fem0;
   }


   public void test() {
      testParseFormat();
      testParseElementNumber();
      for (FemModel3d fem : myTestFems.values()) {
         test (fem);
      }
      myUseEtBlock = true;
      for (FemModel3d fem : myTestFems.values()) {
         test (fem);
      }
      myEBlockKey = EBlockKey.COMPACT;
      for (FemModel3d fem : myTestFems.values()) {
         test (fem);
      }
      myUseEtBlock = false;
      myEBlockKey = EBlockKey.BLANK;
      for (FemModel3d fem : myTestFems.values()) {
         test (fem);
      }
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
      
      AnsysCdbReaderWriterTest tester = new AnsysCdbReaderWriterTest(writeFiles);
      tester.runtest();
      tester.writeTestFiles();
   }

}
