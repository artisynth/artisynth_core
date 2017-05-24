package maspack.geometry.io;

import java.io.*;
import java.util.*;

import maspack.util.*;
import maspack.geometry.*;
import maspack.matrix.*;

import maspack.geometry.io.MeshWriter.DataFormat;
import maspack.geometry.io.PlyWriter.DataType;

public class PlyReaderWriterTest extends UnitTest {
   
   private static double EPS = 1e-8;

   PolygonalMesh myBox =
      MeshFactory.createQuadBox (1, 2, 3, Point3d.ZERO, 1, 1, 1);

   public PlyReaderWriterTest() {
   }

   void test (
      MeshBase mesh, DataFormat dataFmt, DataType floatType, boolean writeNrms)
      throws IOException {


      ByteArrayOutputStream os = new ByteArrayOutputStream();
      PlyWriter writer = new PlyWriter(os);
      
      writer.setDataFormat (dataFmt);
      writer.setFloatType (floatType);
      if (writeNrms) {
         writer.setWriteNormals (1);
      } else {
         // XXX explicitly disable, otherwise for box with hard edges, normals will be written anyways
         writer.setWriteNormals(0);
      }
      writer.writeMesh (mesh);

      byte[] bytes = os.toByteArray();
      //System.out.println (new String(os.toByteArray()));
      ByteArrayInputStream is = new ByteArrayInputStream (bytes);
      PlyReader reader = new PlyReader(is);
      MeshBase check = null;
      if (mesh instanceof PolygonalMesh) {
         check = reader.readMesh (new PolygonalMesh());
      }
      else if (mesh instanceof PointMesh) {
         check = reader.readMesh (new PointMesh());
      }
      double eps = 0;
      if (dataFmt == DataFormat.ASCII) {
         eps = EPS;
      }
      else if (floatType == DataType.FLOAT) {
         eps = 1e-6;
      }
      if (!check.epsilonEquals (mesh, eps)) {
         throw new TestException ("Read mesh does not equal written mesh");
      }
   }

   void test (MeshBase mesh, boolean writeNrms) throws IOException {
      test (mesh, DataFormat.ASCII, DataType.FLOAT, writeNrms);
      test (mesh, DataFormat.BINARY_LITTLE_ENDIAN, DataType.FLOAT, writeNrms);
      test (mesh, DataFormat.BINARY_BIG_ENDIAN, DataType.FLOAT, writeNrms);
      test (mesh, DataFormat.ASCII, DataType.DOUBLE, writeNrms);
      test (mesh, DataFormat.BINARY_LITTLE_ENDIAN, DataType.DOUBLE, writeNrms);
      test (mesh, DataFormat.BINARY_BIG_ENDIAN, DataType.DOUBLE, writeNrms);
   }

   public void test() {
      try {
         PolygonalMesh box =
            MeshFactory.createQuadBox (1, 2, 3, Point3d.ZERO, 1, 1, 1);
         PolygonalMesh sphere =
            MeshFactory.createSphere (3.0, 12);

         test (box, false);
         // test (box, true);
         // test (sphere, true);
         test (sphere, false);

         PointMesh pmesh = MeshFactory.createRandomPointMesh (10, 12.0);
         test (pmesh, false);

         // add normals to the point mesh
         ArrayList<Vector3d> nrms = new ArrayList<Vector3d>();
         int[] idxs = new int[pmesh.numVertices()];
         for (int i=0; i<pmesh.numVertices(); i++) {
            Vector3d nrm = new Vector3d();
            nrm.setRandom();
            nrms.add (nrm);
            idxs[i] = i;
         }
         pmesh.setNormals(nrms, idxs);
         test (pmesh, true);
      }
      catch (IOException e) {
         throw new TestException ("Unexpected IOException: " + e);
      }
   }

   public static void main (String[] args) {
      PlyReaderWriterTest tester = new PlyReaderWriterTest();

      try {
         tester.test();
      }
      catch (Exception e) {
         e.printStackTrace(); 
         System.exit(1); 
      }
      System.out.println ("\nPassed\n");      
   }
}
