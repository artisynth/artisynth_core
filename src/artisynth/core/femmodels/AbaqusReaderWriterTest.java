/**
 * Copyright (c) 2023, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.IOException;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.StringWriter;
import java.util.*;

import maspack.util.ReaderTokenizer;
import maspack.util.TestException;
import maspack.util.UnitTest;
import maspack.matrix.Point3d;

public class AbaqusReaderWriterTest extends UnitTest {

   int RESET_NUMS = AbaqusReader.RESET_NUMBERING;
   int ZERO_BASED = AbaqusReader.ZERO_BASED_NUMBERING;
      
   public static final String testStr = 
"**\n" +
"**  Test string to simulate .inp file\n"+
"**  This does not necessarily define valid geometry\n"+
"**\n" +
"*NODE\n" +
"11, -33.7105933695, -6.46865879995, -8.62690343413\n" +
"12, -33.82468718284, -6.669215181303, -8.62487424552\n" +
"13, -33.94466720271, -6.866303577644, -8.622347712735\n" +
"14, -34.06828783568, -7.053331640251, -8.619087123595\n" +
"15, -34.19859551829, -7.235772215223, -8.61509587574\n" +
"16, -34.32022392704, -7.401900599696, -8.611885741885\n" +
"17, -34.438459599, -7.570424959081, -8.611084132708\n" +
"18, -34.53283707398, -7.711797030419, -8.612950166914\n" +
"19, -34.62780023398, -7.8527446489, -8.616602216708\n" +
"20, -34.70783219937, -7.966811225577, -8.620315203951\n" +
"21, -34.79028097545, -8.079150286521, -8.624211974944\n" +
"22, -34.86935257057, -8.183070561364, -8.627753538285\n" +
"23, -34.95048878604, -8.28539573827, -8.63076621151\n" +
"24, -35.0320496408, -8.382510214765, -8.632697948068\n" +
"25, -35.11603448872, -8.477555443719, -8.633445774633\n" +
"26, -35.20085751542, -8.57169764146, -8.63321410089\n" +
"27, -35.2856387847, -8.66586973353, -8.63221945718\n" +
"28, -35.29987642479, -8.639838667671, -8.787451801629\n" +
"29, -35.3342063523, -8.58797814264, -8.933135140266\n" +
"30, -35.40689923916, -8.492180003532, -9.105856353569\n" +
"31, -35.49547935666, -8.377797581872, -9.259050979896\n" +
"32, -35.61419245453, -8.220222479314, -9.430235553386\n" +
"33, -35.73257199798, -8.047834100238, -9.586825113175\n" +
"34, -35.84986947853, -7.839899224021, -9.743862565475\n" +
"35, -35.9476828926, -7.61748038359, -9.89440354171\n" +
"36, -35.74234328418, -7.483222595587, -9.899549828151\n" +
"37, -35.53896542223, -7.345980939318, -9.903526523803\n" +
"38, -35.33856730881, -7.204330871261, -9.9043364818\n" +
"39, -35.13992050223, -7.060296355087, -9.90240461243\n" +
"40, -34.9440100975, -6.912811867196, -9.894504906135\n" +
"41, -34.75276209125, -6.760217928099, -9.876079900496\n" +
"42, -34.56814438192, -6.602263878055, -9.842140951402\n" +
"43, -34.3916710656, -6.43842656654, -9.79519214072\n" +
"44, -34.23084836603, -6.481537891051, -9.632628351902\n" +
"45, -34.08244794532, -6.509174065377, -9.455483792775\n" +
"46, -33.95222171119, -6.522744711401, -9.263130396627\n" +
"47, -33.84318958321, -6.522055167922, -9.057683615456\n" +
"49, -33.76453216282, -6.504114978142, -8.845059867462\n" +
"50, -35.12797731414, -7.270989266375, -9.767723843882\n" +
"51, -35.11656345345, -7.481046479845, -9.632022662384\n" +
"52, -35.05789307219, -7.871247398923, -9.318550273127\n" +
"60, -1.0, -1.0, 1.0\n" +
"61, 1.0, -1.0, 1.0\n" +
"62, 1.0, 1.0, 1.0\n" +
"63, -1.0, 1.0, 1.0\n" +
"64, -1.0, -1.0, -1.0\n" +
"65, 1.0, -1.0, -1.0\n" +
"66, 1.0, 1.0, -1.0\n" +
"67, -1.0, 1.0, -1.0\n" +
"68, 0.0, -1.0, 1.0\n" +
"69, 1.0, 0.0, 1.0\n" +
"70, 0.0, 1.0, 1.0\n" +
"71, -1.0, 0.0, 1.0\n" +
"72, 0.0, -1.0, -1.0\n" +
"73, 1.0, 0.0, -1.0\n" +
"74, 0.0, 1.0, -1.0\n" +
"75, -1.0, 0.0, -1.0\n" +
"76, -1.0, -1.0, 0.0\n" +
"77, 1.0, -1.0, 0.0\n" +
"78, 1.0, 1.0, 0.0\n" +
"79, -1.0, 1.0, 0.0\n" +
"**\n" +
"*ELEMENT, TYPE=C3D4\n" +
" 4, 11, 13, 12, 14\n" +
" 5, 14, 15, 16, 17\n" +
" 6, 17, 19, 18, 20\n" +
"**\n" +
"**\n" +
"*ELEMENT, TYPE=C3D6\n" +
" 7, 12, 11, 13, 14, 15, 16\n" +
" 8, 40, 41, 42, 45, 44, 43\n" +
" 9, 18, 17, 19, 22, 21, 20\n" +
"**\n" +
"**\n" +
"*ELEMENT, TYPE=C3D8\n" +
"10, 11, 12, 13, 14, 15, 16, 17, 18\n" + 
"11, 14, 15, 16, 17, 21, 20, 19, 18\n" +
"12, 20, 19, 18, 17, 24, 23, 22, 21\n" +
"**\n" +
"**\n" +
"*ELEMENT, TYPE=C3D10\n" +
"13, 20, 22, 21, 23, 26, 25, 24, 27, 29, 28\n" +
"14, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39\n" +
"*ELEMENT, TYPE=C3D20\n" +
"15, 63, 62, 61, 60, 67, 66, 65, 64, 70, 69, 68, 71, 74, 73, 72, 75, 79, 78, 77, 76\n" +
"*ELEMENT, TYPE=S3\n" +
" 1, 11, 12, 13\n" + 
" 2, 14, 15, 16\n" +
" 3, 17, 18, 19\n" +
"*ELEMENT, TYPE=S4\n" +
"16, 11, 12, 13, 14\n" + 
"17, 14, 15, 16, 17\n" +
"*****\n";

   protected void checkSerialNumbering (FemModel3d fem) {
      for (int i=0; i<fem.numNodes(); i++) {
         int n = fem.getNode(i).getNumber();
         if (n != i) {
            throw new TestException ("node "+i+" has number "+n);
         }
      }
      for (int i=0; i<fem.numElements(); i++) {
         int n = fem.getElement(i).getNumber();
         if (n != i) {
            throw new TestException ("element "+i+" has number "+n);
         }
      }
      for (int i=0; i<fem.numShellElements(); i++) {
         int n = fem.getShellElement(i).getNumber();
         if (n != i) {
            throw new TestException (
               "shell element "+i+" has number "+n);
         }
      }
   }
   
   protected static void checkFileStrings (String input, String check) {

      String[] inputs = input.split ("\n");
      String[] checks = check.split ("\n");

      ArrayList<String> inputStrs = new ArrayList<>();
      for (int i=0; i<inputs.length; i++) {
         String si = inputs[i];
         if (!si.startsWith ("**")) {
            if (!si.startsWith ("*ST") && !si.startsWith ("*END")) {
               inputStrs.add (inputs[i]);
            }
         }
      }
      ArrayList<String> checkStrs = new ArrayList<>();
      for (int i=0; i<checks.length; i++) {
         String sc = checks[i];
         if (!sc.startsWith ("**")) {
            if (!sc.startsWith ("*ST") && !sc.startsWith ("*END")) {
               checkStrs.add (sc);
            }
         }
      }
      if (inputStrs.size() != checkStrs.size()) {
         throw new TestException (
            "input and check strings have different line counts: " +
            inputStrs.size() + " vs. " + checkStrs.size());
      }
      for (int i=0; i<inputStrs.size(); i++) {
         String si = inputStrs.get(i);
         String sc = checkStrs.get(i);
         if (!si.equals (sc)) {
            System.out.println ("input: " + si);
            System.out.println ("check: " + sc);
            throw new TestException (
               "input and check strings "+i+" differ");
         }
      }
   }

   void checkComponentNumbering (FemModel3d fem, FemModel3d chk, int off) {
      checkEquals (
         "numNodes", fem.numNodes(), chk.numNodes());
      checkEquals (
         "numElements", fem.numElements(), chk.numElements());
      checkEquals (
         "numShellElements", fem.numShellElements(), chk.numShellElements());
      
      for (int i=0; i<fem.numNodes(); i++) {
         int n = fem.getNode(i).getNumber();
         int c = chk.getNode(i).getNumber() + off;
         if (n != c) {
            throw new TestException (
               "node "+i+" has number "+n+", expecting "+c);
         }
      }
      for (int i=0; i<fem.numElements(); i++) {
         int n = fem.getElement(i).getNumber();
         int c = chk.getElement(i).getNumber() + off;
         if (n != c) {
            throw new TestException (
               "element "+i+" has number "+n+", expecting "+c);
         }
      }
      for (int i=0; i<fem.numShellElements(); i++) {
         int n = fem.getShellElement(i).getNumber();
         int c = chk.getShellElement(i).getNumber() + off;
         if (n != c) {
            throw new TestException (
               "shell element "+i+" has number "+n+", expecting "+c);
         }
      }
   }

   private FemElement3d addSolidElem (FemModel3d fem, int num, int... nidxs) {
      FemNode3d[] nodes = new FemNode3d[nidxs.length];
      for (int i=0; i<nodes.length; i++) {
         nodes[i] = fem.getNode (nidxs[i]);
      }
      FemElement3d elem = null;
      switch (nidxs.length) {
         case 4: {
            elem = new TetElement (nodes);
            break;
         }
         case 5: {
            elem = new PyramidElement (nodes);
            break;
         }
         case 6: {
            elem = new WedgeElement (nodes);
            break;
         }
         case 8: {
            elem = new HexElement (nodes);
            break;
         }
         case 10: {
            elem = new QuadtetElement (nodes);
            break;
         }
         case 20: {
            elem = new QuadhexElement (nodes);
            break;
         }
         default: {
            throw new TestException (
               "No element implemented for "+nodes.length+" nodes");
         }
      }
      fem.addNumberedElement (elem, num);
      return elem;      
   }

   private ShellElement3d addShellElem (
      FemModel3d fem, int num, double thickness, boolean membrane, int... nidxs) {
      FemNode3d[] nodes = new FemNode3d[nidxs.length];
      for (int i=0; i<nodes.length; i++) {
         nodes[i] = fem.getNode (nidxs[i]);
      }
      ShellElement3d elem = null;
      switch (nidxs.length) {
         case 3: {
            elem = new ShellTriElement (
               nodes[0], nodes[1], nodes[2], thickness, membrane);
            break;
         }
         case 4: {
            elem = new ShellQuadElement (
               nodes[0], nodes[1], nodes[2], nodes[3], thickness, membrane);
            break;
         }
         default: {
            throw new TestException (
               "No element implemented for "+nodes.length+" nodes");
         }
      }
      fem.addNumberedShellElement (elem, num);
      return elem;      
   }

   private FemModel3d createTestFem (int baseNum) {
      FemModel3d fem = new FemModel3d();
      int ndivs = 2;
      double width = 4.0;
      // create a regular grid of 27 nodes. These can be used to form any type
      // of currently supported element
      int num = (baseNum != -1 ? baseNum : 0);
      Point3d p = new Point3d();
      for (int k=0; k<=ndivs; k++) {
         for (int j=0; j<=ndivs; j++) {
            for (int i=0; i<=ndivs; i++) {
               p.x = width*(-0.5 + i/(double)ndivs);
               p.y = width*(-0.5 + j/(double)ndivs);
               p.z = width*(-0.5 + k/(double)ndivs);
               fem.addNumberedNode (new FemNode3d (p), num++);
            }
         }
      }

      num = (baseNum != -1 ? baseNum : 0);
      // we're careful to add elements in the same order as the writer will
      // write them out, to preserve the number - index mapping.

      // tets
      addSolidElem (fem, num++, 0, 9, 1, 3);
      addSolidElem (fem, num++, 1, 10, 2, 4);
      addSolidElem (fem, num++, 9, 18, 10, 12);

      // wedges
      addSolidElem (fem, num++, 0, 9, 1, 3, 12, 4);
      addSolidElem (fem, num++, 1, 10, 2, 4, 13, 5);
      addSolidElem (fem, num++, 9, 18, 10, 12, 21, 13);

      // hexes
      addSolidElem (fem, num++, 0, 1, 10, 9, 3, 4, 13, 12);
      addSolidElem (fem, num++, 0, 2, 20, 18, 6, 8, 26, 24);
      addSolidElem (fem, num++, 9, 10, 19, 18, 15, 16, 25, 24);

      // quadtet
      addSolidElem (fem, num++, 6, 8, 24, 0, 7, 16, 15, 3, 4, 12);

      // quadhex
      addSolidElem (
         fem, num++, 0, 2, 20, 18, 6, 8, 26, 24,
         1, 11, 19, 9, 7, 17, 25, 15, 3, 5, 23, 21);

      double thickness = 0.001;
      boolean MEMBRANE = true;
      boolean NO_MEMBRANE = false;

      if (baseNum == -1) {
         num = 0;
      }
      // tri membranes
      addShellElem (fem, num++, thickness, MEMBRANE, 0, 1, 9);
      addShellElem (fem, num++, thickness, MEMBRANE, 1, 2, 10);
      addShellElem (fem, num++, thickness, MEMBRANE, 9, 10, 18);

      // quad membranes
      addShellElem (fem, num++, thickness, MEMBRANE, 0, 1, 10, 9);
      addShellElem (fem, num++, thickness, MEMBRANE, 1, 2, 11, 10);
      addShellElem (fem, num++, thickness, MEMBRANE, 9, 10, 19, 18);

      // tri shells
      addShellElem (fem, num++, thickness, NO_MEMBRANE, 10, 11, 19);
      addShellElem (fem, num++, thickness, NO_MEMBRANE, 4, 5, 13);
      addShellElem (fem, num++, thickness, NO_MEMBRANE, 7, 8, 16);

      // quad shells
      addShellElem (fem, num++, thickness, NO_MEMBRANE, 10, 11, 19, 18);
      addShellElem (fem, num++, thickness, NO_MEMBRANE, 4, 5, 13, 12);
      addShellElem (fem, num++, thickness, NO_MEMBRANE, 7, 8, 16, 15);

      return fem;
   }

   private String writeToString (FemModel3d fem) throws IOException {
      StringWriter swriter = new StringWriter ();
      AbaqusWriter writer = new AbaqusWriter (new PrintWriter(swriter));
      writer.setSuppressWarnings (true);
      writer.writeFem (fem);
      writer.close();
      return swriter.toString();
   }

   private FemModel3d readFromString (
      String str, int opts) throws IOException {
      BufferedReader breader = new BufferedReader (new StringReader (str));
      AbaqusReader reader = new AbaqusReader (breader, null);
      reader.setSuppressWarnings (true);
      reader.setZeroBasedNumbering ((opts & ZERO_BASED) != 0);
      reader.setResetNumbering ((opts & RESET_NUMS) != 0);
      return reader.readFem (null);
   }

   public void test() throws IOException {

      FemModel3d fem1 = readFromString (testStr, 0);
      checkEquals ("numNodes", fem1.numNodes(), 61);
      checkEquals ("numElements", fem1.numElements(), 12);
      checkEquals ("numShellElements", fem1.numShellElements(), 5);

      FemModel3d femx = readFromString (testStr, RESET_NUMS);
      checkEquals ("numNodes", fem1.numNodes(), 61);
      checkEquals ("numElements", fem1.numElements(), 12);
      checkEquals ("numShellElements", fem1.numShellElements(), 5);
      checkSerialNumbering (femx);

      FemModel3d fem0 = readFromString (testStr, ZERO_BASED);
      checkComponentNumbering (fem1, fem0, 1);

      //System.out.println (writeToString (fem));

      checkFileStrings (testStr, writeToString (fem1));

      fem1 = createTestFem(1);
      String fem1Str = writeToString (fem1);
      FemModel3d chkFem = readFromString (fem1Str, 0);
      String chkStr = writeToString (chkFem);
      checkFileStrings (fem1Str, chkStr);
      // make sure numberings were preserved
      checkComponentNumbering (fem1, chkFem, 0);

      fem0 = createTestFem(-1);
      String fem0Str = writeToString (fem0);
      // fem0Str and fem1Str should be the same because the
      // writer will force one-based numbering
      checkFileStrings (fem1Str, fem0Str);
   }      

   public static void main (String args[]) {
      AbaqusReaderWriterTest tester = new AbaqusReaderWriterTest();
      tester.runtest();
   }
}
