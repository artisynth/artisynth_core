package maspack.collision;


import maspack.geometry.*;
import maspack.matrix.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;

public class SignedDistanceCollider implements AbstractCollider {
   
   public PolygonalMesh mesh0;
   public PolygonalMesh mesh1;
   /*
    * Method getContacts returns this ContactInfo populated with all of the
    * information about the collision.
    */
   public ContactInfo contactInfo;
   
   public SignedDistanceCollider() {
   }
   
   // mesh0 is rigid/fixed, mesh1 is deformable.
   public ContactInfo getContacts (
      PolygonalMesh mesh0, PolygonalMesh mesh1) {
      
      this.mesh0 = mesh0;
      this.mesh1 = mesh1;
      contactInfo = new ContactInfo (mesh0, mesh1);
      SignedDistanceGrid grid0 = mesh0.getSignedDistanceGrid();
      if (grid0 == null) {
         Vector3d cellDivisions = new Vector3d (20.0, 20.0, 20.0);
         double gridMargin = 0.1;
         grid0 = mesh0.getSignedDistanceGrid (gridMargin, cellDivisions);
      }
      //************************************************************************
      Point3d meshMin = new Point3d();
      Point3d meshMax = new Point3d();
      Vector3d widths = new Vector3d();
      RigidTransform3d XOBBToWorld0 = 
         new RigidTransform3d (mesh0.getMeshToWorld());
      mesh0.getLocalBounds (meshMin, meshMax);
      XOBBToWorld0.mulXyz ((meshMin.x + meshMax.x) / 2.0, 
                           (meshMin.y + meshMax.y) / 2.0, 
                           (meshMin.z + meshMax.z) / 2.0);
      widths.sub (meshMax, meshMin);
      OBB obb0 = new OBB (widths, XOBBToWorld0);
      
      RigidTransform3d XOBBToWorld1 = 
         new RigidTransform3d (mesh1.getMeshToWorld());
      mesh1.getLocalBounds (meshMin, meshMax);
      XOBBToWorld1.mulXyz ((meshMin.x + meshMax.x) / 2.0, 
                           (meshMin.y + meshMax.y) / 2.0, 
                           (meshMin.z + meshMax.z) / 2.0);
      // XOBBToWorld1 should be identity.
      widths.sub (meshMax, meshMin);      
      OBB obb1 = new OBB (widths, XOBBToWorld1);
      if (BVBoxNodeTester.isDisjoint (obb0, obb1)) {
         return null;
      }
      //************************************************************************
      ArrayList<Vertex3d> mesh1Vertices = mesh1.getVertices ();
      Vector3d normal = new Vector3d();
      double distance;
      RigidTransform3d X1to0 = null;
      if (!mesh0.meshToWorldIsIdentity() ||
          !mesh1.meshToWorldIsIdentity()) {
         X1to0 = new RigidTransform3d();
         // Transform from deformable mesh to rigid mesh.
         X1to0.mulInverseLeft (mesh0.getMeshToWorld(), mesh1.getMeshToWorld());
      }
      Vertex3d vtx = new Vertex3d();
      for (Vertex3d v1 : mesh1Vertices) { // mesh1 is deformable
         vtx.pnt.set (v1.pnt);
         if (X1to0 != null)
            vtx.pnt.transform (X1to0);
         distance = grid0.getDistanceAndNormal (normal, vtx.pnt);
         if (distance <= 0) {
            if (!mesh0.meshToWorldIsIdentity())
               normal.transform (mesh0.getMeshToWorld());
            contactInfo.myPoints1.add (
               new PenetratingPoint (v1, normal, distance * -1.0));
         }
      }
      ContactInfo tmp = contactInfo;
      contactInfo = null;
      return tmp;
   }
   
//   /*
//    * The following code does nothing and is merely for compatibility with the
//    * old collision code.
//    */
//   public double getEpsilon() {
//      return 0;
//   }
//
//   public double getPointTolerance() {
//      return 0;      
//   }
//
//   public double getRegionTolerance() {
//      return 0;      
//   }
//
//   public void setPointTolerance (double d) {
//      
//   }
//
//   public void setRegionTolerance (double d) {
//      
//   }
}
