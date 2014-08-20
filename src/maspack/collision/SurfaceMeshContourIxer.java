package maspack.collision;

import maspack.collision.MeshIntersectionPoint;
import maspack.geometry.BVNode;
import maspack.geometry.BVTree;
import maspack.geometry.Boundable;
import maspack.geometry.RobustPreds;
import maspack.geometry.Vertex3d;
import maspack.geometry.HalfEdge;
import maspack.geometry.Face;
import maspack.geometry.PolygonalMesh;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * An intersector which produces contours, similar to AJL's code.
 * 
 * This class will find intersection contours between two PolygonalMeshes.
 * First, their bounding volume hierarchies are tested for collisions in order
 * to create a map of Face-Face intersections.
 * An unordered list of all intersection points (MeshIntersectionPoints) is
 * generated from all Face-Face intersections.
 * Finally, MIPs are processed to generate one or more MeshIntersectionContours, 
 * each of which contains a single list of ordered MIPs, which may be open or closed.
 * 
 * @author andrew
 *
 */
public class SurfaceMeshContourIxer {
   public PolygonalMesh mesh0;
   public PolygonalMesh mesh1;
   
   // Avoid repeating edge/face collision checks (expensive)
   HashMap<EdgeFacePair, MeshIntersectionPoint> 
      mySavedEdgeFaceResults = new HashMap<EdgeFacePair, MeshIntersectionPoint>(); 
   
   protected boolean myHandleDegen = true; 
   
   
   /*
    * Each contour is a list of MeshIntersectionPoints, each of which represents
    * the intersection of a HalfEdge and a Face.
    */
   protected ArrayList<IntersectionContour> myContours = new ArrayList<IntersectionContour> ();

   public static long renderTime = -1;

   public SurfaceMeshContourIxer () {
   }
   
   /**
    * Call this method to change how this class handles degenerate intersection cases
    * that it detects. By default, it is true. If true, upon detection of a degenerate 
    * case, vertices of the two faces in question will be perturbed slightly, and the 
    * intersection code will attempt to run again, until all degenerate cases are resolved.
    * This behaviour might be undesirable, and in this case, you can call setHandleDegen(false) 
    * to disable it
    * 
    * @param handle true if degenerate cases should be handled by perturbation, false otherwise.
    */
   public void setHandleDegen (boolean handle) {
      myHandleDegen = handle;
   }
   
   public List<IntersectionContour> getContours () {
      return myContours;
   }
   
   /*
    * Main interface for collision -- called by CollisionPair. Returns a
    * ContactInfo populated with the results of the collision analysis. Returns
    * null if there is no collision. mesh0, mesh1 are the candidate colliding
    * surface meshes, which can be the same mesh.
    * 
    * isRigidBodyRigidBody should be specified as true only if mesh0 and mesh1
    * both represent rigid bodies. This controls the type of data returned in
    * ContactInfo.
    */
   public boolean findContours (PolygonalMesh mesh0, PolygonalMesh mesh1) {
      if (mesh0 != mesh1) {
         if (mesh0.canSelfIntersect)
            if (findContours (mesh0, mesh0))
               throw new RuntimeException ("self-intersecting mesh");
         if (mesh1.canSelfIntersect)
            if (findContours (mesh1, mesh1))
               throw new RuntimeException ("self-intersecting mesh");
      }

      this.mesh0 = mesh0;
      this.mesh1 = mesh1;
      mesh0.updateFaceNormals();
      mesh1.updateFaceNormals();
      if (!mesh0.isTriangular() | !mesh1.isTriangular())
         throw new IllegalArgumentException ("collision with non-triangular mesh");

      ArrayList<MeshIntersectionPoint> allMips = new ArrayList<MeshIntersectionPoint>();
      // Try getting allMips continuously until no DegenerateCases are caught
      while (true) {
         // Clear old saved results
         mySavedEdgeFaceResults.clear ();
         myContours.clear ();
         allMips.clear();
      
         BVTree bvh0 = mesh0.getBVTree();
         BVTree bvh1 = mesh1.getBVTree();
         ArrayList<BVNode> nodes0 = new ArrayList<BVNode>();
         ArrayList<BVNode> nodes1 = new ArrayList<BVNode>();
         bvh0.intersectTree (nodes0, nodes1, bvh1);

         try {
            getAllMIPs(allMips, nodes0, nodes1);
            break;
         } catch (DegenerateCaseException e) {
            System.out.println ("Caught a DegenerateCaseException, points perturbed and retrying");
            mesh0.notifyVertexPositionsModified ();
            mesh1.notifyVertexPositionsModified ();
            continue;
         }
      }
      
      boolean intersected;
      if (allMips.size() > 0) {
         intersected = true;
         buildContours (myContours, allMips);
      }
      else {
         intersected = false;
      }
      
      return intersected;
   }
   
   /**
    * Build a list of IntersectionContours, where each contour is an ordered list of 
    * MeshIntersectionPoints. return in contours
    * 
    * @param contours Initialized list of IntersectionContours (new contours added here)
    * @param mips List containing all MeshIntersectionPoints to use. This list is empty 
    *           by the end of this call if the method completed successfully.
    */
   protected void buildContours (
            List<IntersectionContour> contours, 
            ArrayList<MeshIntersectionPoint> mips) {
      while (mips.size() > 0) {
         try {
            IntersectionContour contour = getOneContour (mips);
            if (contour != null) {
               contours.add (contour);
            }
         } catch (DegenerateCaseException e) {
            if (myHandleDegen) {
               System.out.println("Caught a DegenerateCaseException while getting contours!");
               System.out.println(e.getMessage ());
               System.out.println("  This means it wasn't caught in getAllMips and we have some thinking to do.");
               System.out.println("  Please yell at Andrew Ho to do something about it.");
            } else {
               System.out.println ("Caught a DegenerateCaseException! Degeneracy handling is disabled.");
               System.out.println ("Use SurfaceMeshContourIxer.setHandleDegen(true) to perturb " +
               		"\n  vertices slightly to try to resolve this case.");
               mips.clear ();
               break;
            }
         }
      }
   }
   
   /**
    * From an non-empty list of MIPs, create a new MeshIntersectionContour and
    * return it. It might be open or closed. MIPs are removed from mips as they
    * are used.
    * 
    * @param mips List of possible MeshIntersectionPoints to check. This method
    *        deletes MIPs in mips as it goes along.
    * @return new MeshIntersectionContour, or <code>null</code>
    * if <code>mips</code> is empty
    */
   protected IntersectionContour getOneContour (
      ArrayList<MeshIntersectionPoint> mips) 

         throws DegenerateCaseException {
      if (mips.size() == 0) { 
          return null;
      }
      IntersectionContour contour = new IntersectionContour ();
      contour.myIsOpen = false;

      MeshIntersectionPoint firstMip = mips.get (0);
      MeshIntersectionPoint mip = firstMip;
      MeshIntersectionPoint nextMip = null;

      // Start the contour
      contour.add (mip);
      
      Face fPrev = null;
      while (nextMip != contour.get (0) || contour.size() < 2) {
         nextMip = null;
         // First search this edge's face's edges for intersection with mip.face
         Face thisEdgesFace;
         if (mip.edge.getFace () == fPrev) {
            if (mip.edge.opposite == null) {
               thisEdgesFace = null;
            } else {
               thisEdgesFace = mip.edge.opposite.getFace ();
            }
         } else {
            thisEdgesFace = mip.edge.getFace ();
         }
         
         /* 
          * We have found the edge of an open mesh. This contour is 
          * definitely open. Continue from contour.get(0) and go in 
          * opposite direction. If we already did that, then we're finished. 
          */
         if (thisEdgesFace == null) {
            if (contour.myIsOpen) {
               // We're finished, since we already did this
               break;
            }
            contour.myIsOpen = true;
            nextMip = firstMip;
            contour.reverse ();
            mip = nextMip;
            mips.remove (mip);
            
            // mip.edge.getFace was already checked in first pass, 
            // so we should use this as previous to switch directions.
            fPrev = mip.edge.getFace ();
            continue;
         }
         
         /* 
          * Now the meat of the algorithm. Search this edge's faces edges
          * for another match. If found, this is the next mip in the contour
          */
         for (int i=0; i<3; i++) {
            HalfEdge candEdge = thisEdgesFace.getEdge (i).getPrimary ();
            if (candEdge == mip.edge.getPrimary ()) {
               // We already know about this edge
               continue;
            }
            EdgeFacePair testPair = new EdgeFacePair (candEdge, mip.face);
            nextMip = mySavedEdgeFaceResults.get (testPair);
            if (nextMip == null) {
               continue;
            } else {
               fPrev = thisEdgesFace;
               break;
            }
         }
         
         /*
          * if nextMip is still null, the next mip must be on the other mesh's face
          */
         if (nextMip == null) {
            for (int i=0; i<3; i++) {
               HalfEdge candEdge = mip.face.getEdge (i).getPrimary ();
               EdgeFacePair testPair = new EdgeFacePair (candEdge, thisEdgesFace);
               nextMip = mySavedEdgeFaceResults.get (testPair);
               if (nextMip == null) {
                  continue;
               } else {
                  fPrev = mip.face;
                  break;
               }
            }
         }
         
         /*
          * If we still haven't found the next MIP at this point,
          * we're basically screwed. We should probably give up or something
          */
         if (nextMip == null) {
            System.out.println("mips.size(): " + mips.size());
            System.out.println("contour.size(): " + contour.size());
            throw new DegenerateCaseException ("Couldn't find next intersection point!");
         }
         
         mip = nextMip;
         contour.add (mip);
         if (!mips.remove (mip)) {
            // This shouldn't happen
            System.out.println("mips.size(): " + mips.size());
            System.out.println("contour.size(): " + contour.size());
            if (contour.myIsOpen) {
               System.out.println("Open contour");
            }
            throw new DegenerateCaseException ("Warning! nextMip wasn't in mips!!! aborting");
         }
      }

      return contour;
   }
   
   /**
    * Return an unordered list of MeshIntersectionPoints representing all
    * edge/face collisions between the lists of BVs nodes0 and nodes1.
    * 
    * New MeshIntersectionPoints are generated and appended to allMips.
    * All Faces are assumed to be triangular! Undetermined behaviour if not the case.
    * 
    * @param allMips Initialized ArrayList of MeshIntersectionPoints
    * @param nodes0 first list of BVs from BVTree.intersectTree
    * @param nodes1 second list of BVs from BVTree.intersectTree (same length as nodes0)
    * 
    * @throws DegenerateCaseException if a degenerate case is detected. 
    *           Points of those faces are perturbed before this exception is re-thrown 
    */
   protected void getAllMIPs (
         ArrayList<MeshIntersectionPoint> allMips, 
         ArrayList<BVNode> nodes0, ArrayList<BVNode> nodes1) 
         throws DegenerateCaseException {
      assert nodes0.size() == nodes1.size();
      
      for (int i=0; i<nodes0.size(); i++) {
         BVNode n0 = nodes0.get (i);
         BVNode n1 = nodes1.get (i);
         for (Boundable b0 : n0.getElements ()) {
            for (Boundable b1 : n1.getElements ()) {
               
               if (b0 instanceof Face && b1 instanceof Face) {
                  Face f0 = (Face) b0;
                  Face f1 = (Face) b1;
                  
                  int numFound = 0;
                  numFound += doFaceFaceCollision (allMips, f0, f1);
                  numFound += doFaceFaceCollision (allMips, f1, f0);
                  
                  if (myHandleDegen) {
                     if (numFound != 0 && numFound != 2) {
                        perturbVertices (f0);
                        perturbVertices (f1);
//                        System.out.println("Found " + numFound + " ixps");
                        throw new DegenerateCaseException();
                     }
                  }
               }
            }
         }
      }
   }
   
   /**
    * Perturb all vertices of a Face
    * 
    * @param f face whose vertices are to be perturbed
    */
   protected void perturbVertices (Face f) {
      double dx, dy, dz;
      double eps = 1e-16;
      for (Vertex3d v : f.getVertices ()) {
         dx = Math.random() * eps;
         dy = Math.random() * eps;
         dz = Math.random() * eps;
         v.pnt.add (dx, dy, dz);
      }
   }
   
   /**
    * Collide one triangular face with the second, but not the other way around.
    * 
    * @param allMips new MIPs are appended to this list
    * @param f0 The face which has its edges checked
    * @param f1 The face which has its face checked
    *  
    * @return the number of edge/face collisions detected in this call.
    */
   protected int doFaceFaceCollision (ArrayList<MeshIntersectionPoint> allMips, Face f0, Face f1) { 
      int numFound = 0;
      // Faces assumed to be triangular here!
      for (int eidx = 0; eidx<3; eidx++) {
         HalfEdge e = f0.getEdge (eidx).getPrimary ();
         EdgeFacePair pair = new EdgeFacePair(e,f1);
         
         // First check if we've already tested this edge/face pair
         if (mySavedEdgeFaceResults.containsKey (pair)) {
            if (mySavedEdgeFaceResults.get (pair) != null) {
               numFound++;
            }
            continue;
         }
         
         MeshIntersectionPoint mip = new MeshIntersectionPoint (); 
         boolean collided = robustIntersectionWithFace(e, f1, mip);
         if (collided) {
            mySavedEdgeFaceResults.put (pair, mip);
            allMips.add (mip);
            numFound++;
         } else {
            mySavedEdgeFaceResults.put (pair, null);
         }
      }
      
      return numFound;
   }
   
   /*
    * Test for intersection using adaptive exact arithmetic and SOS tiebreaking.
    */
   protected boolean robustIntersectionWithFace (
      HalfEdge he, Face face, MeshIntersectionPoint mip) {

      HalfEdge he0 = face.firstHalfEdge();
      Vertex3d v = he0.tail;
      if (v == he.head)
         return false;
      if (v == he.tail)
         return false;
      v = he0.head;
      if (v == he.head)
         return false;
      if (v == he.tail)
         return false;
      v = he0.getNext().head;
      if (v == he.head)
         return false;
      if (v == he.tail)
         return false;


      if (!RobustPreds.intersectEdgeFace (he, face, mip))
         return false;
      mip.edge = he;
      mip.face = face;
      return true;
   }

   /**
    * Static Inner Class for holding an Edge-Face pair.
    * This class will ALWAYS store the primary half-edge, no 
    * matter what is passed to the constructor. 
    * 
    * This class is immutable, and overrides equals/hashcode to
    * test if myEdge.equals(other.myEdge) and myFace.equals(other.myFace).
    * 
    * @author andrew
    *
    */
   public static class EdgeFacePair {
      protected HalfEdge myEdge;
      protected Face myFace;
      
      /**
       * Create an edge-face pair
       * @param e edge (primary or secondary, only primary is stored)
       * @param f face
       */
      public EdgeFacePair (HalfEdge e, Face f) {
         myEdge = e.getPrimary();
         myFace = f; 
      }
      
      public HalfEdge getEdge () {
         return myEdge;
      }
      public Face getFace () {
         return myFace;
      }
      
      @Override
      public int hashCode () {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((myEdge == null) ? 0 : myEdge.hashCode ());
         result = prime * result + ((myFace == null) ? 0 : myFace.hashCode ());
         return result;
      }

      @Override
      public boolean equals (Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass () != obj.getClass ())
            return false;
         EdgeFacePair other = (EdgeFacePair)obj;
         if (myEdge == null) {
            if (other.myEdge != null)
               return false;
         }
         else if (!myEdge.equals (other.myEdge))
            return false;
         if (myFace == null) {
            if (other.myFace != null)
               return false;
         }
         else if (!myFace.equals (other.myFace))
            return false;
         return true;
      }
   }
   
   public static class DegenerateCaseException extends Exception {

      public DegenerateCaseException () {
         super();
      }
      public DegenerateCaseException (String string) {
         super(string);
      }
   }
}
