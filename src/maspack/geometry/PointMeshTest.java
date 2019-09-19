package maspack.geometry;

import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.*;

/**
 * Test class for PointMesh
 */
public class PointMeshTest extends MeshTestBase {
   
   protected PointMesh createNewMesh() {
      return new PointMesh();
   }

   public void test() {
      testWriteRead();
   }

   private void testWriteRead() {
      PointMesh mesh = MeshFactory.createRandomPointMesh(35, 2.0);

      testWriteRead (mesh, ".obj", true);
      testWriteRead (mesh, ".obj");
      testWriteRead (mesh, ".ply");
      // XYZ files don't work at the moment because (a) they don't encode
      // whether or not normals are present, and (b) the xyzb file stores data
      // as float and not double.
      //testWriteRead (mesh, ".xyz");
      //testWriteRead (mesh, ".xyzb");
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      PointMeshTest tester = new PointMeshTest();
      tester.runtest();
   }

}
