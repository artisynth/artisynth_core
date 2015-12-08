package artisynth.core.femmodels;

import maspack.util.*;
import maspack.matrix.*;

public class NaturalCoordsComputeTest extends UnitTest {

   FemElement3d myElem;

   public NaturalCoordsComputeTest() {

      double[] nodeCoords = new double[]
         {
            -1, -1,  -0.5, 
             1, -1,  -1.5,
            -1,  1,  -1,
             1,  1,  -1,
            -1, -1,   1, 
             1, -1,   2,
            -1, 0.5,   1,
             1,  1,   1,
         };

      FemNode3d[] nodes = new FemNode3d[8];
      for (int i=0; i<8; i++) {
         nodes[i] = new FemNode3d (
            nodeCoords[i*3], nodeCoords[i*3+1], nodeCoords[i*3+2]);
      }
      myElem = 
         new HexElement (nodes[4], nodes[5], nodes[7], nodes[6], 
                         nodes[0], nodes[1], nodes[3], nodes[2]);

   }

   public Point3d getRandomPoint (double r) {
      Point3d pos = new Point3d();
      do {
         pos.setRandom (-r, r);
      }
      while (pos.norm() > r);
      return pos;
   }

   void docheck (Point3d pos) {
      
      Vector3d coords = new Vector3d();
      Vector3d ccheck = new Vector3d();
      Vector3d error = new Vector3d();

      int numIters;
      int chkIters;

      numIters = myElem.getNaturalCoordinates (coords, pos, 100);
      //System.out.println ("coords=" + coords);
      if (numIters >= 0) {
         totalNumIters += numIters;
      }
      chkIters = myElem.getNaturalCoordinatesStd (ccheck, pos, 100);
      //System.out.println ("ccheck=" + ccheck);
      if (chkIters >= 0) {
         totalChkIters += chkIters;
      }
      error.sub (ccheck, coords);
      boolean printInfo = false;
      if (numIters < 0 && chkIters >= 0) {
         System.out.println ("FAILED but check did not");
         printInfo = true;
      }
      else if (numIters >= 0 && chkIters < 0) {
         System.out.println ("CHECK FAILED");
         printInfo = true;
      }
      else if (numIters >= 0 && chkIters >= 0 && error.norm() > 1e-8) {
         System.out.println ("ERROR=" + error.norm());         
         printInfo = true;
      }
      if (printInfo) {
         System.out.println ("pos=" + pos);
         System.out.println ("coords=" + coords);
         System.out.println ("ccheck=" + ccheck);
      }
   }

   int totalNumIters = 0;
   int totalChkIters = 0;

   public void test() {

      docheck (new Point3d (
                  -2.2233029745908697,-1.8443261764617684,-0.3979159197510569));
      
      int testcnt = 10000;
      double radius = 4.0;
      for (int k=0; k<testcnt; k++) {
         Point3d pos = getRandomPoint (radius);
         docheck (pos);
      }
      System.out.println (
         "average iterations: " + (totalNumIters/(double)testcnt));
      System.out.println (
         "average check iterations: " + (totalChkIters/(double)testcnt));
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      NaturalCoordsComputeTest tester = new NaturalCoordsComputeTest();
      tester.test();
   }
}
