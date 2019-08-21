package maspack.collision;

import java.util.*;

import maspack.collision.SurfaceMeshIntersector.RegionType;
import maspack.geometry.Face;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.TriTriIntersection;
import maspack.geometry.Vertex3d;
import maspack.geometry.*;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector2d;
import maspack.util.DataBuffer;

/**
 * This class returns information describing contact between two polygonal
 * meshes, as determined by the 
 * {@link AbstractCollider#getContacts getContacts} method of 
 * {@link AbstractCollider}. This information includes the interpenetrating
 * vertices of mesh, and the contact regions formed from matching 
 * interpenetration zones. Contact information produced by
 * {@link SurfaceMeshCollider} also includes the intersection
 * contours and interpenetration regions on each mesh.
 * 
 * <p>Contact information can be used to produce dynamic constraints
 * for handling the contact.
 */
public class ContactInfo {

   // Triangle intersections. Produced by MeshCollider only
   ArrayList<TriTriIntersection> myIntersections =
      new ArrayList<TriTriIntersection>();

   // the colliding meshes
   PolygonalMesh myMesh0 = null;
   PolygonalMesh myMesh1 = null;

   // interpenetration regions - produced by SurfaceMeshCollider only,
   // based on the contours
   ArrayList<PenetrationRegion> myRegions0 = null;
   RegionType myRegionsType0;
   ArrayList<PenetrationRegion> myRegions1 = null;
   RegionType myRegionsType1;

   // intersection contours - produced by SurfaceMeshCollider only
   ArrayList<IntersectionContour> myContours = null;

   // the interpenetrating points for each mesh
   ArrayList<PenetratingPoint> myPoints0 = null;
   ArrayList<PenetratingPoint> myPoints1 = null;

   // edge-edge contacts - produced by SurfaceMeshCollider only
   ArrayList<EdgeEdgeContact> myEdgeEdgeContacts = null;

   // contact regions with normals facing mesh 0. Computed on-demand
   // from either penetration regions or triangle intersections
   ArrayList<ContactPlane> myContactPlanes = null;

   double myPointTol = 1e-4;
   double myContactPlaneTol = 1e-2;

   public ContactInfo (PolygonalMesh m0, PolygonalMesh m1) {
      myMesh0 = m0;
      myMesh1 = m1;
   }

   /**
    * Returns the first or second mesh associated with this contact
    * information, as indicated by <code>meshNum</code>, which should be either
    * 0 or 1.
    *
    * @param meshNum number of the requested mesh (0 or 1)
    * @return mesh indicated by <code>meshNum</code>
    */
   
   public PolygonalMesh getMesh (int meshNum) { 
      if (meshNum == 0) {
         return myMesh0;
      }
      else if (meshNum == 1) {
         return myMesh1;
      }
      else {
         throw new IndexOutOfBoundsException ("meshNum must be 0 or 1");
      }
  }

   /**
    * Sets the nearest point tolerance used to compute contact planes.
    * 
    * @param tol nearest point tolerance
    */
   public void setPointTol (double tol) {
      myPointTol = tol;
   }
   
   /**
    * Returns the nearest point tolerance used to compute contact planes.
    * 
    * @return nearest point tolerance for contact planes
    */
   public double getPointTol() {
      return myPointTol;
   }
   
   /**
    * Sets the tolerance used to compute contact planes.
    * 
    * @param tol contact plane tolerance
    */
   public void setContactPlaneTol (double tol) {
      myContactPlaneTol = tol;
   }
   
   /**
    * Returns the tolerance used to compute contact planes.
    * 
    * @return contact plane tolerance
    */
   public double getContactPlaneTol() {
      return myContactPlaneTol;
   }


   /**
    * Returns the contact penetration regions associated with the mesh
    * indicated by <code>meshNum</code> (which should be 0 or 1).  Each region
    * represents a connected subset of the indicated mesh that is either
    * "inside" or "outside" the other mesh. If the collider that produced this
    * contact information is not capable of computing penetration regions,
    * or if no regions were requested, <code>null</code> is returned. 
    * At present, only {@link SurfaceMeshCollider} can 
    * compute penetration regions.
    *
    * @param meshNum number of the indicated mesh (0 or 1)
    * @return list of the contact penetration regions for the indicated mesh,
    * or <code>null</code> if this information is not available.
    * Should not
    * be modified.
    */
   public ArrayList<PenetrationRegion> getRegions (int meshNum) {
      if (meshNum == 0) {
         return myRegions0;
      }
      else if (meshNum == 1) {
         return myRegions1;
      }
      else {
         throw new IndexOutOfBoundsException ("meshNum must be 0 or 1");
      }
   }

   /**
    * Returns which type of penetration regions (inside, outside, or none) have
    * been computed for the mesh indicated by <code>meshNum</code> (which
    * should be 0 or 1). The regions themselves are returned by {@link
    * #getRegions}.  If the collider that produced this contact
    * information is not capable of computing penetration regions,
    * or if no regions were requested for the mesh in question,
    * {@link RegionType#NONE} is returned. At present, only {@link
    * SurfaceMeshCollider} can compute penetration regions.
    *
    * @param meshNum number of the indicated mesh (0 or 1)
    * @return which type of penetration regions have been computed for
    * the indicated mesh, or <code>Regions.NONE</code> if there are none.
    */
   public RegionType getRegionsType (int meshNum) {
      if (meshNum == 0) {
         return myRegionsType0;
      }
      else if (meshNum == 1) {
         return myRegionsType1;
      }
      else {
         throw new IndexOutOfBoundsException ("meshNum must be 0 or 1");
      }
   }

//   /**
//    * Returns whether contact penetration regions associated with the mesh
//    * indicated by <code>meshNum</code> (which should be 0 or 1) are "inside" or
//    * "outside" the other mesh. The regions themselves are returned by {@link
//    * #getPenetrationRegions}.  If the collider that produced this contact
//    * information is not capable of computing penetration regions,
//    * <code>false</code> is returned. At present, only {@link
//    * SurfaceMeshCollider} can compute penetration regions.
//    *
//    * @param meshNum number of the indicated mesh (0 or 1)
//    * @return <code>true</code> if regions for the indicated mesh are inside
//    * the other mesh, or <code>false</code> if they are outside or if this
//    * information is not available. 
//    */
//   public boolean penetrationRegionsAreInside (int meshNum) {
//      if (meshNum == 0) {
//         return myRegions0Inside;
//      }
//      else if (meshNum == 1) {
//         return myRegions1Inside;
//      }
//      else {
//         throw new IndexOutOfBoundsException ("meshNum must be 0 or 1");
//      }
//   }
//   
   /**
    * Returns the intersection contours associated with this contact.  If the
    * collider that produced this contact information is not capable of
    * computing intersection contours, <code>null</code> is returned. At
    * present, only {@link SurfaceMeshCollider} can compute contours.
    * 
    * @return list of intersection contours, or <code>null</code> if
    * they are not available. Should not be modified.
    */
   public ArrayList<IntersectionContour> getContours() {
      return myContours;
   }

   /**
    * Returns the number of intersection contours associated with this contact.
    * If the collider that produced this contact information is not capable of
    * computing intersection contours, <code>0</code> is returned. At
    * present, only {@link SurfaceMeshCollider} can compute contours.
    * 
    * @return number of intersection contours, or <code>0</code> if
    * they are not available.
    */  
   public int numContours() {
      return myContours == null ? 0 : myContours.size();
   }  
   
   /**
    * Sets the contours of this ContactInfo to a deep copy of a specified
    * list of contours.
    * 
    * @param contours contours to copy
    */
   void setContours (ArrayList<IntersectionContour> contours) {
      myContours = new ArrayList<IntersectionContour>(contours.size());
      for (IntersectionContour c : contours) {
         myContours.add (c.copy());
      }
   }

   /**
    * Directly set the penetrating points for the indicated mesh.  The value is
    * set by reference, so the specified array {@code points} is not
    * copied. For internal use only.
    */
   public void setPenetratingPoints (
      ArrayList<PenetratingPoint> points, int meshNum) {
      if (meshNum == 0) {
         myPoints0 = points;
      }
      else if (meshNum == 1) {
         myPoints1 = points;
      }
      else {
         throw new IndexOutOfBoundsException ("meshNum must be 0 or 1");
      }
   }   
   /**
    * Returns the penetrating points of the mesh indicated by
    * <code>meshNum</code> (which should be 0 or 1). These represent all the
    * vertices of the indicated mesh that are "inside" the other mesh, along
    * with the corresponding nearest face on the other mesh.  The penetrating
    * points can be used to produce vertex-based contact constraints.
    *
    * <p>This method computes the penetration points on demand and then caches
    * the result.
    *
    * @param meshNum number of the indicated mesh (0 or 1)
    * @return list of penetrating points of the indicated mesh with respect
    * to the other mesh. Should not be modified.
    */
   public ArrayList<PenetratingPoint> getPenetratingPoints (int meshNum) {
      if (meshNum == 0) {
         if (myPoints0 == null) {
            if (myRegionsType0 == RegionType.INSIDE) {
               myPoints0 = new ArrayList<PenetratingPoint>();
               for (PenetrationRegion r : myRegions0) {
                  SurfaceMeshCollider.collideVerticesWithFaces (
                     myPoints0, r, myMesh1);
               } 
               Collections.sort (
                  myPoints0, new PenetratingPoint.IndexComparator());      
            }
            else {
               myPoints0 = computePenetratingPoints (myMesh0, myMesh1);
            }
         }
         return myPoints0;
      }
      else if (meshNum == 1) {
         if (myPoints1 == null) {
            if (myRegionsType1 == RegionType.INSIDE) {
               myPoints1 = new ArrayList<PenetratingPoint>();
               for (PenetrationRegion r : myRegions1) {
                  SurfaceMeshCollider.collideVerticesWithFaces (
                     myPoints1, r, myMesh0);
               }
               Collections.sort (
                  myPoints1, new PenetratingPoint.IndexComparator());        
            }
            else {
               myPoints1 = computePenetratingPoints (myMesh1, myMesh0);
            }
         }
         return myPoints1;
      }
      else {
         throw new IndexOutOfBoundsException ("meshNum must be 0 or 1");
      }
   }
   
   ArrayList<PenetratingPoint> computePenetratingPoints (
      PolygonalMesh mesh0, PolygonalMesh mesh1) {
      
      BVFeatureQuery query = new BVFeatureQuery();
      Point3d wpnt = new Point3d();
      Point3d nearest = new Point3d();
      Vector2d uv = new Vector2d();
      Vector3d disp = new Vector3d();
      
      ArrayList<PenetratingPoint> points = 
         new ArrayList<PenetratingPoint>();
      for (Vertex3d vtx : mesh0.getVertices()) {
         // John Lloyd, Jan 3, 2014: rewrote to use isInsideOrientedMesh()
         // to determine if a vertex is inside another mesh. Previous code
         // would not always work and broke when the BVTree code was
         // refactored.
         vtx.getWorldPoint (wpnt);
         if (query.isInsideOrientedMesh (mesh1, wpnt, -1)) {
            Face f = query.getFaceForInsideOrientedTest (nearest, uv);
            mesh1.transformToWorld (nearest);
            disp.sub (nearest, wpnt);
            points.add (new PenetratingPoint (
               vtx, f, uv, nearest, disp, /*region=*/null));
         }
      }
      return points;
   }

   /**
    * Returns a set of contact planes for this contact. These are formed by
    * identifying matching interpenetration zones between the meshes, and then
    * fitting a plane to them, computing a reduced set of contact points
    * projected into that plane, and estimating the interpenetration depth.
    *
    * <p>Contact planes are typically used for handling rigid body contact,
    * and their computation depends on the collider used for determining the
    * contact information. If no suitable penetration zones are found between
    * the meshes, no contact planes will be produced.
    *
    * <p>The method computes contact planes on demand and then caches
    * the result.
    *
    * @return list of contact planes determined for this contact information.  
    * Should not be modified.
    */
   public ArrayList<ContactPlane> getContactPlanes() {
      if (myContactPlanes == null) {
         myContactPlanes = computeContactPlanes();
      }
      return myContactPlanes;
   }

   ArrayList<ContactPlane> computeContactPlanes() {
      ArrayList<ContactPlane> cplanes = new ArrayList<ContactPlane>();
      if (myRegionsType0 == RegionType.INSIDE &&
          myRegionsType1 == RegionType.INSIDE) {
         // then create from penetration regions
         HashMap<PenetrationRegion,PenetrationRegion> matchingRegions =
            findMatchingRegions();
         for (PenetrationRegion r0 : matchingRegions.keySet()) {
            PenetrationRegion r1 = matchingRegions.get(r0);
            ContactPlane cp = new ContactPlane();
            if (cp.build (r0, r1, myMesh0, myPointTol)) {
               cplanes.add (cp);
            }
         }        
      }
      else if (myIntersections != null) {
         // create for triangle-triangle intersections
         MeshCollider.createContactPlanes (cplanes, myIntersections, myContactPlaneTol);
         for (ContactPlane cp : cplanes) {
            MeshCollider.getContactPlaneInfo (
               cp, myMesh0, myMesh1, myPointTol);
         }       
      }
      return cplanes;
   }

   /**
    * Finds the penetration regions on each mesh that correspond to each other,
    * in terms of sharing the same intersection contours.  If the collider that
    * produced this contact information is not capable of computing penetration
    * regions, <code>null</code> is returned. At present, only {@link
    * SurfaceMeshCollider} can compute penetration regions.
    *
    * @return matching penetration regions, or <code>null</code> if penetration
    * regions are not available.
    */
   public HashMap<PenetrationRegion,PenetrationRegion> findMatchingRegions() {
      if (myRegions0 != null) {
         HashSet<PenetrationRegion> unmatched1 = 
            new HashSet<PenetrationRegion>();
         HashMap<PenetrationRegion,PenetrationRegion> matchingRegions = 
            new HashMap<PenetrationRegion,PenetrationRegion>();
         unmatched1.addAll (myRegions1);
         for (PenetrationRegion r0 : myRegions0) {
            PenetrationRegion found = null;
            for (PenetrationRegion r1 : myRegions1) {
               if (r0.myContours.equals(r1.myContours)) {
                  found = r1;
               }
            }
            if (found != null) {
               matchingRegions.put (r0, found);
               myRegions1.remove (found);
            }
         }
         return matchingRegions;
      }
      else {
         return null;
      }
   }      


   /**
    * Directly set edge-edge contacts for the contact. The values are * set by
    * reference, so the specified array {@code contacts} is not copied. For
    * internal use only.
    */
   public void setEdgeEdgeContacts (ArrayList<EdgeEdgeContact> contacts) {
      myEdgeEdgeContacts = contacts;
   }

   /**
    * Returns the edge-edge contacts for this contact. If the collider that
    * produced this contact information is not capable of computing edge-edge
    * contacts, <code>null</code> is returned. At present, only {@link
    * SurfaceMeshCollider} can compute edge-edge contacts.
    *
    * <p>The method computes the contacts on demand and then caches the result.
    * 
    * @return list of edge-edge contacts, or <code>null</code> if this
    * information is not available. Should not be modified.
    */
   public ArrayList<EdgeEdgeContact> getEdgeEdgeContacts() {
      if (myEdgeEdgeContacts == null) {
         if (myRegionsType0 == RegionType.INSIDE &&
             myRegionsType1 == RegionType.INSIDE) {
            SurfaceMeshCollider collider = new SurfaceMeshCollider();
            myEdgeEdgeContacts = collider.findEdgeEdgeContacts (this);
         }
      }
      return myEdgeEdgeContacts;
   }
   
   /**
    * Returns the triangle intersections for this contact.  If the collider
    * that produced this contact information does not compute individual
    * triangle intersections, <code>null</code> is returned.  At present, only
    * {@link MeshCollider} produces triangle intersections.
    * 
    * @return list of triangle intersections, or <code>null</code> if
    * unavailable.
    */
   public ArrayList<TriTriIntersection> getIntersections() {
      return myIntersections;
   }

   public int numContourPoints() {
      int nump = 0;
      if (myContours != null) {
         for (IntersectionContour c : myContours) {
            nump += c.size();
         }
      }
      return nump;
   }

   public void getContourState (
      DataBuffer state, ArrayList<IntersectionContour> contours) {
      if (contours == null) {
         state.zput (-1);
         return;
      }
      else {
         state.zput (contours.size());
         for (IntersectionContour contour : contours) {
            contour.getState (state, myMesh0, myMesh1);
         }
      }
   }

   public ArrayList<IntersectionContour> setContourState (DataBuffer state) {
      ArrayList<IntersectionContour> contours = null;
      int numc = state.zget();
      if (numc != -1) {
         contours = new ArrayList<IntersectionContour>();
         for (int i=0; i<numc; i++) {
            IntersectionContour contour = new IntersectionContour();
            contour.setState (state, myMesh0, myMesh1);
            contours.add (contour);
         }
      }
      return contours;
   }

   public void getState (DataBuffer state) {
      TriTriIntersection.getState (state, myIntersections);
      getContourState (state, myContours);
      PenetratingPoint.getState (state, myPoints0);
      PenetratingPoint.getState (state, myPoints1);
      EdgeEdgeContact.getState (state, myEdgeEdgeContacts);
   }

   public void setState (DataBuffer state) {
      myIntersections = TriTriIntersection.setStateArray (
         state, myMesh0, myMesh1);
      myContours = setContourState (state);
      myPoints0 = PenetratingPoint.setState (state, myMesh0, myMesh1);
      myPoints1 = PenetratingPoint.setState (state, myMesh1, myMesh0);
      myEdgeEdgeContacts =
         EdgeEdgeContact.setStateArray (state, myMesh0, myMesh1);
   }

   public boolean equals (ContactInfo cinfo, StringBuilder msg) {
      if (myMesh0 != cinfo.myMesh0) {
         if (msg != null) msg.append ("mesh0 differs\n");
         return false;
      }
      if (myMesh1 != cinfo.myMesh1) {
         if (msg != null) msg.append ("mesh1 differs\n");
         return false;
      }
      if ((myIntersections == null) != (cinfo.myIntersections == null)) {
         if (msg != null) msg.append ("intersections != null differs\n");
         return false;
      }
      if (myIntersections != null) {
         if (myIntersections.size() != cinfo.myIntersections.size()) {
            if (msg != null) msg.append ("intersections size differs\n");
            return false;
         }
         for (int i=0; i<myIntersections.size(); i++) {
            if (!myIntersections.get(i).equals (
               cinfo.myIntersections.get(i), msg)) {
               if (msg != null) {
                  msg.append ("TriTriIntersection "+i+" differs\n");
               }
               return false;
            }
         }
      }
      if ((myContours == null) != (cinfo.myContours == null)) {
         if (msg != null) msg.append ("countours != null differs\n");
         return false;
      }
      if (myContours != null) {
         if (myContours.size() != cinfo.myContours.size()) {
            if (msg != null) msg.append ("contours size differs\n");
            return false;
         }
         for (int i=0; i<myContours.size(); i++) {
            if (!myContours.get(i).equals (cinfo.myContours.get(i), msg)) {
               if (msg != null) {
                  msg.append ("IntersectionContour "+i+" differs\n");
               }
               return false;
            }
         }
      }
      if (!PenetratingPoint.equals (myPoints0, cinfo.myPoints0, msg)) {
         if (msg != null) msg.append ("points0 differs\n");
         return false;
      }
      if (!PenetratingPoint.equals (myPoints1, cinfo.myPoints1, msg)) {
         if (msg != null) msg.append ("points1 differs\n");
         return false;
      }
      if (!EdgeEdgeContact.equals (
             myEdgeEdgeContacts, cinfo.myEdgeEdgeContacts, msg)) {
         if (msg != null) msg.append ("edgeEdgeContacts differs\n");
         return false;
      }
      
      return true;      
   }
}
