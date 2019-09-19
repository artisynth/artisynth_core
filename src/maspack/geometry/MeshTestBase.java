package maspack.geometry;

import java.util.*;
import java.io.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.SpatialInertia;
import maspack.geometry.Vertex3d;

public abstract class MeshTestBase extends UnitTest {
   
   protected abstract MeshBase createNewMesh();

   void testWriteRead (MeshBase mesh, String suffix) {
      testWriteRead (mesh, suffix, false);
   }

   void testWriteRead (
      MeshBase mesh, String suffix, boolean zeroIndexed) {
      String filePath =
         PathFinder.getSourceRelativePath (this, "_meshWRTest"+suffix);
      try {
         if (zeroIndexed) {
            mesh.write (new File (filePath), "%g", true);
         }
         else {
            mesh.write (new File (filePath));
         }
      }
      catch (IOException e) {
         throw new TestException ("Error writing file: "+ e);
      }
      MeshBase check = createNewMesh();
      try {
         if (zeroIndexed) {
            check.read (new File (filePath), true);
         }
         else {
            check.read (new File (filePath));
         }
      }
      catch (IOException e) {
         throw new TestException ("Error reading file: "+ e);
      }
      if (!mesh.epsilonEquals (check, 0)) {
         throw new TestException ("Read mesh not identical to written mesh");
      }
      (new File (filePath)).delete();
   }
}
      
