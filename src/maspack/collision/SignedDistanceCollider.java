package maspack.collision;

import maspack.geometry.*;
import maspack.matrix.*;
import java.util.ArrayList;
import maspack.util.ArraySupport;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;

public class SignedDistanceCollider implements AbstractCollider {
   
   public SignedDistanceCollider() {
   }

   private boolean boundingBoxesDisjoint (
      PolygonalMesh mesh0, PolygonalMesh mesh1) {
      
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
      return BVBoxNodeTester.isDisjoint (obb0, obb1);
   }


   public ContactInfo getContacts (
      PolygonalMesh mesh0, PolygonalMesh mesh1) {

      DistanceGrid grid0 = mesh0.getSignedDistanceGrid();
      if (grid0 == null) {
         Vector3i cellDivisions = new Vector3i (20, 20, 20);
         double gridMargin = 0.1;
         grid0 = mesh0.getSignedDistanceGrid (gridMargin, cellDivisions);
      }     
      return getContacts (mesh0, grid0, mesh1, null);
   }
   
   // mesh0 is rigid/fixed, mesh1 is deformable.
   public ContactInfo getContacts (
      PolygonalMesh mesh0, DistanceGrid grid0,
      PolygonalMesh mesh1, DistanceGrid grid1) {

      if (boundingBoxesDisjoint (mesh0, mesh1)) {
         return null;
      }
      ContactInfo cinfo = new ContactInfo (mesh0, mesh1);
      cinfo.myPoints0 = new ArrayList<PenetratingPoint>();
      cinfo.myPoints1 = new ArrayList<PenetratingPoint>();
      if (grid0 != null) {
         findPenetratingPoints (cinfo.myPoints1, mesh0, grid0, mesh1);
      }
      if (grid1 != null) {
         findPenetratingPoints (cinfo.myPoints0, mesh1, grid1, mesh0);
      }
      if (cinfo.myPoints0.size() == 0 && cinfo.myPoints1.size() == 0) {
         return null;
      }
      else {
         return cinfo;
      }
   }
   
   private void findPenetratingPoints (
      ArrayList<PenetratingPoint> points,
      PolygonalMesh mesh0, DistanceGrid grid0, PolygonalMesh mesh1) {

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
      Point3d vpnt = new Point3d();
      for (Vertex3d v1 : mesh1Vertices) { // mesh1 is deformable
         vpnt.set (v1.pnt);
         if (X1to0 != null) {
            vpnt.transform (X1to0);
         }
         distance = grid0.getLocalDistanceAndNormal (normal, vpnt);
         if (distance <= 0) {
            if (!mesh0.meshToWorldIsIdentity()) {
               normal.transform (mesh0.getMeshToWorld());
               vpnt.transform (mesh0.getMeshToWorld());
            }
            points.add (
               new PenetratingPoint (v1, vpnt, normal, -distance));
         }
      }
   }
}
