package artisynth.core.mechmodels;

import java.util.*;
import maspack.collision.*;
import maspack.geometry.*;
import maspack.matrix.*;
import maspack.util.*;

/**
 * Collects the collision response characteristics for a specific pair of
 * collidables.
 */
public class CollisionResponse extends CollisionComponent {

   ArrayList<CollisionHandler> myHandlers;

   public CollisionResponse () {
      myHandlers = new ArrayList<CollisionHandler> ();
   }

   CollisionResponse (Collidable c0, Collidable c1) {
      myHandlers = new ArrayList<CollisionHandler> ();
      if (c0 instanceof Collidable.Group) {
         throw new IllegalArgumentException (
            "First collidable must not be a generic collidable");
      }
      setCollidablePair (new CollidablePair (c0, c1));
   }

   void collectHandlers (CollisionHandlerTable table) {
      table.collectHandlers (myHandlers, myPair);
   }

   /**
    * Returns the collidable in a CollisionHandler that is associated with the
    * collidable <code>target</code>
    */
   private CollidableBody getBodyForCollidable (
      Collidable target, CollisionHandler ch) {

      int bidx = ch.getBodyIndex (target);
      if (bidx == -1) {
         throw new InternalErrorException (
            "Handler does not a collidable associated with this response");
      }
      return ch.getCollidable(bidx);
   }

   /**
    * Queries if the collidables associated with this response are in
    * contact.
    *
    * @return <code>true</code> if the collidables are in contact.
    */
   public boolean inContact () {
      return myHandlers.size () == 0;
   }

   /**
    * Returns a map specifying the contact forces acting on all the deformable
    * bodies associated with either the first or second collidable (as indicated
    * by <code> cidx</code>).
    *
    * <p>
    * The map gives the most recently computed forces acting on each vertex of
    * the collision meshes of the deformable bodies (this is the same mesh
    * returned by {@link CollidableBody#getCollisionMesh}). Vertices for which
    * no forces were computed do not appear in the map.
    *
    * <p>
    * Contact forces are those that arise in order to prevent further
    * interpenetration between <code>colA</code> and <code>colB</code>. They do
    * <i>not</i> include forces that are computed to separate
    * <code>colA</code> and <code>colB</code> when they initially come into
    * contact.
    * @param cidx collidable index - 0 for first, 1 for second
    *
    * @return map giving the contact forces acting on the deformable bodies in
    * this response.
    */
   public Map<Vertex3d,Vector3d> getContactForces (int cidx) {
      LinkedHashMap<Vertex3d,Vector3d> map =
         new LinkedHashMap<Vertex3d,Vector3d> ();
      for (CollisionHandler ch : myHandlers) {
         ch.getContactForces (
            map, getBodyForCollidable (myPair.get(cidx), ch));
      }
      return map;
   }

   /**
    * Returns a map specifying the contact pressures acting on all the
    * deformable bodies associated with either the first or second collidable
    * (as indicated by <code> cidx</code>).
    *
    * <p> The map gives the most recently computed pressures acting on each
    * vertex of the collision meshes of the deformable bodies (this is the same
    * mesh returned by {@link CollidableBody#getCollisionMesh}). Vertices for
    * which no forces were computed do not appear in the map.
    *
    * <p> This method works by first calling {@link #getContactForces} to
    * obtain the vertex forces, and then converting these to pressures by
    * dividing the force magnitude at each vertex by 1/3 of the area of all the
    * faces surrounding that vertex.
    *
    * @return map giving the contact pressures acting on the deformable bodies
    * in this response.
    */
   public Map<Vertex3d,Double> getContactPressures (int cidx) {
      Map<Vertex3d,Vector3d> fmap = getContactForces (cidx);
      
      // convert forces into pressures:
      
      // set of faces for which planar area has been updated:
      HashSet<Face> faces = new HashSet<>();
      // pressure map to be completed:
      LinkedHashMap<Vertex3d,Double> pmap = new LinkedHashMap<>();

      for (Map.Entry<Vertex3d,Vector3d> entry : fmap.entrySet()) {
         Vertex3d vertex = entry.getKey();
         double force = entry.getValue().norm();
         // Pressure at the vertex is related to force at the vertex
         // by the formula
         // 
         //    force = 1/3 * pressure * adjacentFaceArea
         //
         double adjacentFaceArea = 0;
         Iterator<HalfEdge> it = vertex.getIncidentHalfEdges();
         while (it.hasNext()) {
            HalfEdge he = it.next();
            Face face = he.getFace();
            if (!faces.contains(face)) {
               // update planar area for the face
               face.computeNormal();
               faces.add (face);
            }
            adjacentFaceArea += face.getPlanarArea();
         }
         double pressure = 3*force/adjacentFaceArea;
         pmap.put (vertex, pressure);              
      }
      return pmap;
   }

   /**
    * Returns the PenetrationRegions on all bodies associated with either the
    * first or second collidable (as indicated by <code>cidx</code>) resulting
    * from contact with the other collidable. In order for penetration regions
    * to be available, collisions must be performed with the collider type set
    * to {@link CollisionManager.ColliderType#AJL_CONTOUR
    * ColliderType.AJL_CONTOUR}. If penetrations regions are not available,
    * <code>null</code> is returned.
    *
    * @param cidx
    * collidable index - 0 for first, 1 for second
    * @return penetration regions for the indicated collidable
    */
   public ArrayList<PenetrationRegion> getPenetrationRegions (int cidx) {

      Collidable target = myPair.get (cidx);
      ArrayList<PenetrationRegion> regions =
         new ArrayList<PenetrationRegion> ();

      for (CollisionHandler ch : myHandlers) {
         ContactInfo cinfo = ch.myLastContactInfo;
         ArrayList<PenetrationRegion> localRegions;
         if (getBodyForCollidable (target, ch) == ch.getCollidable(0)) {
            localRegions = cinfo.getRegions(0);
         }
         else {
            localRegions = cinfo.getRegions(1);
         }
         if (localRegions == null) {
            // regions not available
            return null;
         }
         else {
            regions.addAll (localRegions);
         }
      }
      return regions;
   }

   /**
    * Returns the total collision contact area associated with either the first
    * or second collidable (as indicated by <code> cidx</code>). In order for
    * this to be determined, collisions must be performed with the collider type
    * set to {@link CollisionManager.ColliderType#AJL_CONTOUR
    * ColliderType.AJL_CONTOUR} . If this is not the case, -1 is returned.
    * 
    * @param cidx
    * collidable index - 0 for first, 1 for second
    */
   public double getContactArea (int cidx) {

      Collidable target = myPair.get (cidx);
      double area = 0;
      for (CollisionHandler ch : myHandlers) {
         ContactInfo cinfo = ch.myLastContactInfo;
         ArrayList<PenetrationRegion> localRegions;
         if (getBodyForCollidable (target, ch) == ch.getCollidable(0)) {
            localRegions = cinfo.getRegions(0);
         }
         else {
            localRegions = cinfo.getRegions(1);
         }
         if (localRegions == null) {
            // regions not available
            return -1;
         }
         for (PenetrationRegion r : localRegions) {
            area += r.getArea ();
         }
      }
      return area;
   }

   void clearHandlers () {
      myHandlers.clear ();
   }

   void addHandler (CollisionHandler ch) {
      myHandlers.add (ch);
   }

   /**
    * Returns the CollisionHandlers for all currently active collisions
    * associated with the collidables of this response.
    * 
    * <p>Each collision handler is associated with two collidable bodies. 
    * However, the pairwise ordering of these bodies may not correspond
    * to the ordering of the collidables associated with this
    * response. In other words, the first and second bodies the
    * handler may be associated with the second and first collidables.
    * To test this, one may use the method {@link 
    * CollisionHandler#getBodyIndex} to query whether one of this
    * response's collidables is associated with the first or second
    * handler body. For example, to test the first collidable, one
    * would call
    * <pre>
    *   if (handler.getBodyIndex(getCollidable(0)) == 0) {
    *      // pairwise ordering corresponds
    *   }
    *   else {
    *      // pairwise ordering is reversed
    *   }
    * </pre>
    *
    * @return collision handlers for all the target bodies. Should not be
    * modified.
    */
   public ArrayList<CollisionHandler> getHandlers() {
      return myHandlers;
   }

}
