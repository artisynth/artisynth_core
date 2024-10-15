/**
 * Copyright (c) 2024, by the Authors: John E Lloyd (UBC).
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.*;
import java.io.*;
import maspack.util.*;
import maspack.matrix.*;

import artisynth.core.modelbase.ComponentList;

/**
 * Base class for FEM reader/writer test code.
 */
public abstract class FemReaderWriterTestBase extends UnitTest {

   protected static int QUAD = 0x01;
   protected static int SHELL_SURFACE = 0x02;

   protected abstract FemReaderBase createFemReader (Reader r) throws IOException;

   protected abstract FemWriterBase createFemWriter (
      PrintWriter pw) throws IOException;

   protected HashMap<String,FemModel3d> myTestFems = new LinkedHashMap<>();

   /**
    * Check file outputs, written as strings, line by line.
    */
   protected void checkOutputs (
      String msg, String out, String chk) {
      
      String[] outs = out.split ("\n");
      String[] chks = chk.split ("\n");
      for (int i=0; i<Math.min(outs.length, chks.length); i++) {
         if (!outs[i].equals (chks[i])) {
            throw new TestException (
               msg + ", line "+(i+1)+", expected:\n" +
               chks[i] + "\ngot:\n" + outs[i]);
         }
      }
      if (outs.length != chks.length) {
         throw new TestException (
            msg + ": output has "+outs.length+
            " lines, expected "+chks.length);
      }
   }

   /**
    * Checks the component numbering within a fem
    */
   void checkNumbering (
      FemModel3d fem, int base, int shellBase) {

      for (int i=0; i<fem.numNodes(); i++) {
         FemNode3d node = fem.getNode(i);
         if (node.getNumber() != base+i) {
            throw new TestException (
               "Node "+i+" has number "+node.getNumber()+
               "; expected "+(base+i));
         }
      }
      for (int i=0; i<fem.numElements(); i++) {
         FemElement3d elem = fem.getElement(i);
         if (elem.getNumber() != base+i) {
            throw new TestException (
               "Element "+i+" has number "+elem.getNumber()+
               "; expected "+(base+i));
         }
      }
      for (int i=0; i<fem.numShellElements(); i++) {
         ShellElement3d elem = fem.getShellElement(i);
         if (elem.getNumber() != shellBase+i) {
            throw new TestException (
               "Shell element "+i+" has number "+elem.getNumber()+
               "; expected "+(shellBase+i));
         }
      }
   }

   protected FemModel3d createAllElemGrid (
      double widthX, double widthY, double widthZ,
      int numY, int numZ, boolean oneBased, int numoff) {

      FemModel3d fem0 = createMixedElemGrid (
         widthX, widthY, widthZ, 4, numY, numZ, SHELL_SURFACE);
      FemModel3d fem1 = createMixedElemGrid (
         widthX, widthY, widthZ, 4, numY, numZ, QUAD);

      fem0.transformGeometry (new RigidTransform3d (0, 0, widthZ));
      fem1.transformGeometry (new RigidTransform3d (0, 0, -widthZ));
      FemFactory.addFem (fem0, fem1);

      FemModel3d fem2 = FemFactory.createShellTriGrid (
         null, widthX, widthY, 2, 2, 0.01, /*membrane*/true);
      fem2.transformGeometry (new RigidTransform3d (0, 0, widthZ/4));
      FemFactory.addFem (fem0, fem2);

      FemModel3d fem3 = FemFactory.createShellQuadGrid (
         null, widthX, widthY, 2, 2, 0.01, /*membrane*/false);
      fem3.transformGeometry (new RigidTransform3d (0, 0, -widthZ/4));
      FemFactory.addFem (fem0, fem3);

      if (oneBased) {
         fem0.setOneBasedNodeElementNumbering (true);
      }
      if (numoff > 0) {
         addNumberOffset (fem0, oneBased ? numoff-1 : numoff);
      }
      return fem0;
   }

   protected void addNumberOffset (FemModel3d fem, int off) {
      fem.getNodes().incrementNumbering(off);
      fem.getElements().incrementNumbering(off);
      fem.getShellElements().incrementNumbering(off);
   }

   protected FemModel3d createSeparateShellVolumeFem (
      boolean oneBased, int numoff) {

      FemModel3d fem0 = FemFactory.createShellTriGrid (
         null, 2.0, 2.0, 2, 2, 0.01, /*membrane*/true);

      FemModel3d fem1 = FemFactory.createHexGrid (null, 1.0, 1.0, 1.0, 2, 2, 2);
      fem1.transformGeometry (new RigidTransform3d (0, 0, -1));
      FemFactory.addFem (fem0, fem1);
      if (oneBased) {
         fem0.setOneBasedNodeElementNumbering (true);
      }
      if (numoff > 0) {
         addNumberOffset (fem0, oneBased ? numoff-1 : numoff);
      }
      return fem0;
   }

   protected FemModel3d createOverlapShellVolumeFem (
      boolean oneBased, int numoff) {

      FemModel3d fem0 = createMixedElemGrid (
         2.0, 1.0, 1.0, 2, 1, 1, SHELL_SURFACE);

      FemModel3d fem1 = FemFactory.createShellQuadGrid (
         null, 1.0, 1.0, 2, 2, 0.01, /*membrane*/false);
      fem1.transformGeometry (new RigidTransform3d (0, 0, -1));
      FemFactory.addFem (fem0, fem1);
      if (oneBased) {
         fem0.setOneBasedNodeElementNumbering (true);
      }
      if (numoff > 0) {
         addNumberOffset (fem0, oneBased ? numoff-1 : numoff);
      }
      return fem0;
   }

   protected FemModel3d createMixedElemGrid (
      double widthX, double widthY, double widthZ,
      int numX, int numY, int numZ, int flags) {

      FemModel3d fem = new FemModel3d();

      FemFactory.createGridNodes(fem, widthX, widthY, widthZ, numX, numY, numZ);
      // System.out.println("num nodes: "+myNodes.size());
      // create all the elements
      ComponentList<FemNode3d> nodes = fem.getNodes();

      int wk = (numX + 1) * (numY + 1);
      int wj = (numX + 1);
      for (int i = 0; i < numX; i++) {
         for (int j = 0; j < numY; j++) {
            for (int k = 0; k < numZ; k++) {
               // get node numbers corresponding to a hex node
               FemNode3d n0 = nodes.get((k + 1) * wk + j * wj + i);
               FemNode3d n1 = nodes.get((k + 1) * wk + j * wj + i + 1);
               FemNode3d n2 = nodes.get((k + 1) * wk + (j + 1) * wj + i + 1);
               FemNode3d n3 = nodes.get((k + 1) * wk + (j + 1) * wj + i);
               FemNode3d n4 = nodes.get(k * wk + j * wj + i);
               FemNode3d n5 = nodes.get(k * wk + j * wj + i + 1);
               FemNode3d n6 = nodes.get(k * wk + (j + 1) * wj + i + 1);
               FemNode3d n7 = nodes.get(k * wk + (j + 1) * wj + i);

               if (i%4 == 0) {
                  HexElement e =
                     new HexElement(n0, n1, n2, n3, n4, n5, n6, n7);
                  e.setParity((i + j + k) % 2 == 0 ? 1 : 0);
                  fem.addElement (e);
               }
               else if (i%4 == 1) {
                  Tetrahedralizer tetzer = new Tetrahedralizer();
                  TetElement[] elems =
                     tetzer.subdivideHex (
                        n0, n1, n2, n3, n4, n5, n6, n7,
                        /* allOnX= */(i + j + k) % 2 != 0);
                  for (FemElement3d e : elems) {
                     fem.addElement(e);
                  }
               }
               else if (i%4 == 2) {
                  WedgeElement e1 = new WedgeElement(n0, n1, n4, n3, n2, n7);
                  WedgeElement e2 = new WedgeElement(n1, n5, n4, n2, n6, n7);

                  fem.addElement(e1);
                  fem.addElement(e2);
               }
               else if (i%4 == 3) {
                  int evenCode = 0;
                  if ((i % 2) == 0) {
                     evenCode |= 0x1;
                  }
                  if ((j % 2) == 0) {
                     evenCode |= 0x2;
                  }
                  if ((k % 2) == 0) {
                     evenCode |= 0x4;
                  }
                  PyramidElement[] elems =
                     FemFactory.divideCubeIntoPyramids (
                        n0, n1, n2, n3, n4, n5, n6, n7, evenCode);
                  for (FemElement3d e : elems) {
                     fem.addElement(e);
                  }
               }
            }
         }
      }
      FemFactory.setGridEdgesHard(fem, widthX, widthY, widthZ);
      if ((flags & QUAD) != 0) {
         fem = FemFactory.createQuadraticModel (null, fem);
      }
      if ((flags & SHELL_SURFACE) != 0) {
         for (int i = 0; i < numX; i++) {
            // put quads along the top/bottom
            for (int k = 0; k <= numZ; k += numZ) {
               for (int j = 0; j < numY; j++) {
                  FemNode3d n0 = nodes.get(k * wk + j * wj + i);
                  FemNode3d n1 = nodes.get(k * wk + j * wj + i + 1);
                  FemNode3d n2 = nodes.get(k * wk + (j + 1) * wj + i + 1);
                  FemNode3d n3 = nodes.get(k * wk + (j + 1) * wj + i);
                  ShellQuadElement e;
                  if (k == 0) {
                     e = new ShellQuadElement (
                        n0, n3, n2, n1, 0.01, /*membrane*/false);
                  }
                  else {
                     e = new ShellQuadElement (
                        n0, n1, n2, n3, 0.01, /*membrane*/false);
                  }
                  fem.addShellElement (e);                  
               }
            }
            // put tris along the sidea
            for (int j = 0; j <= numY; j += numY) {
               for (int k = 0; k < numZ; k++) {
                  FemNode3d n0 = nodes.get(k * wk + j * wj + i);
                  FemNode3d n1 = nodes.get(k * wk + j * wj + i + 1);
                  FemNode3d n2 = nodes.get((k + 1) * wk + j * wj + i + 1);
                  FemNode3d n3 = nodes.get((k + 1) * wk + j * wj + i);
                  ShellTriElement e0;
                  ShellTriElement e1;
                  if (j == 0) {
                     e0 = new ShellTriElement (
                        n0, n1, n2, 0.01, /*membrane*/false);
                     e1 = new ShellTriElement (
                        n0, n2, n3, 0.01, /*membrane*/false);
                  }
                  else {
                     e0 = new ShellTriElement (
                        n0, n2, n1, 0.01, /*membrane*/false);
                     e1 = new ShellTriElement (
                        n0, n3, n2, 0.01, /*membrane*/false);
                  }
                  fem.addShellElement (e0);                  
                  fem.addShellElement (e1);
               }
            }
         }
      }
      return fem;
   }

   String writeToString (FemModel3d fem) throws IOException {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter (sw);
      FemWriterBase femWriter = createFemWriter (pw);
      femWriter.writeFem (fem);
      femWriter.close();
      return sw.toString();
   }

   void writeToFile (String fileName, FemModel3d fem) throws IOException {
      PrintWriter pw =
         new PrintWriter (new BufferedWriter (new FileWriter (fileName)));
      FemWriterBase femWriter = createFemWriter (pw);
      femWriter.writeFem (fem);
      femWriter.close();
   }

   private int countLines (String str) {
      int num = 1;
      for (int i=0; i<str.length(); i++) {
         if (str.charAt(i) == '\n') {
            num++;
         }
      }
      return num;
   }

   FemModel3d readFromString (String str, int flags) throws IOException {
      FemModel3d fem = new FemModel3d();
      FemReaderBase femReader = createFemReader (new StringReader (str));
      femReader.setOptionsFromFlags (flags);
      femReader.readFem (fem);
      if (femReader.myLineCnt != countLines(str)) {
         throw new TestException (
            "Reader line count is off: expected "+countLines(str)+
            ", got "+femReader.myLineCnt);
      }
      femReader.close();
      return fem;
   }

   protected void addTestFem (
      String name, FemModel3d fem) {
      fem.setName (name);
      myTestFems.put (name, fem);
   }

   protected ArrayList<FemModel3d> createTestFems() {
      ArrayList<FemModel3d> fems = new ArrayList<>();

      addTestFem (
         "wedgeCylinder",
         FemFactory.createWedgeCylinder (null, 1, 1, 3, 3));
      addTestFem (
         "pyramidGrid",
         FemFactory.createPyramidGrid (null, 1, 1, 1, 2, 2, 2));
      addTestFem (
         "hexCylinder",
         FemFactory.createConformalHexCylinder (null, 1, 1, 2, 2));
      addTestFem (
         "tetCylinder",
         FemFactory.createTetCylinder (null, 1, 1, 2, 2));
      addTestFem (
         "quadtetCylinder",
         FemFactory.createQuadtetCylinder (null, 1, 1, 2, 2));
      addTestFem (
         "hexGrid",
         FemFactory.createHexGrid (null, 1.0, 1.0, 1.0, 2, 2, 2));
      addTestFem (
         "quadwedgeGrid",
         FemFactory.createQuadwedgeGrid (null, 1.0, 1.0, 1.0, 2, 2, 2));
      addTestFem (
         "quadhexGrid",
         FemFactory.createQuadhexGrid (null, 1.0, 1.0, 1.0, 2, 2, 2));
      addTestFem (
         "quadpyramidGrid",
         FemFactory.createQuadpyramidGrid (null, 1.0, 1.0, 1.0, 2, 2, 2));
      // shell elements
      addTestFem (
         "shellTriMembrane",
         FemFactory.createShellTriGrid (
            null, 2.0, 2.0, 2, 2, 0.01, /*membrane*/true));
      addTestFem (
         "shellQuadMembrane",
         FemFactory.createShellQuadGrid (
            null, 2.0, 2.0, 2, 2, 0.01, /*membrane*/true));
      addTestFem (
         "shellTriGrid",
         FemFactory.createShellTriGrid (
            null, 2.0, 2.0, 2, 2, 0.01, /*membrane*/false));
      addTestFem (
         "shellQuadGrid",
         FemFactory.createShellQuadGrid (
            null, 2.0, 2.0, 2, 2, 0.01, /*membrane*/false));
      // mixed elements
      addTestFem (
         "hexWedgeCylinder",
         FemFactory.createHexWedgeCylinder (null, 2.0, 1.0, 6, 2, 2));
      addTestFem (
         "mixedElemGrid",
         createMixedElemGrid (3.0, 1.0, 1.0, 4, 2, 2, /*flags*/0));
      addTestFem (
         "mixedQuadelemGrid",
         createMixedElemGrid (3.0, 1.0, 1.0, 4, 2, 2, /*flags*/QUAD));
      // mixed elements and shells
      addTestFem (
         "mixedElemGridSurface",
         createMixedElemGrid (
            3.0, 1.0, 1.0, 4, 2, 2, /*flags*/SHELL_SURFACE)); 
      addTestFem (
         "sepShellVol",
         createSeparateShellVolumeFem (/*oneBased*/false, /*numoff*/0));
      addTestFem (
         "sepShellVolBase1",
         createSeparateShellVolumeFem (/*oneBased*/true, /*numoff*/0));
      addTestFem (
         "sepShellVolOff10",
         createSeparateShellVolumeFem (/*oneBased*/false, /*numoff*/10));
      addTestFem (
         "sepShellVolBase1Off10",
         createSeparateShellVolumeFem (/*oneBased*/true, /*numoff*/10));
      addTestFem (
         "overlapShellVol",
         createOverlapShellVolumeFem (/*oneBased*/false, /*numoff*/0));
      addTestFem (
         "overlapShellVolBase1",
         createOverlapShellVolumeFem (/*oneBased*/true, /*numoff*/0));
      addTestFem (
         "overlapShellVolOff10",
         createOverlapShellVolumeFem (/*oneBased*/false, /*numoff*/10));
      addTestFem (
         "overlapShellVolBase1Off10",
         createOverlapShellVolumeFem (/*oneBased*/true, /*numoff*/10));
      addTestFem (
         "allElems",
         createAllElemGrid (3.0, 1.0, 1.0, 2, 2,/*oneBased*/false, /*numoff*/0));
      addTestFem (
         "allElemsBase1",
         createAllElemGrid (3.0, 1.0, 1.0, 2, 2,/*oneBased*/true, /*numoff*/0));
      addTestFem (
         "allElemsOffset10",
         createAllElemGrid (3.0, 1.0, 1.0, 2, 2,/*oneBased*/false,/*numoff*/10));
      addTestFem (
         "allElemsBase1Off10",
         createAllElemGrid (3.0, 1.0, 1.0, 2, 2,/*oneBased*/true, /*numoff*/10));

      return fems;
   }
}
