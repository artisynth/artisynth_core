package artisynth.core.femmodels;

import maspack.matrix.*;
import maspack.geometry.*;
import maspack.geometry.BVFeatureQuery.ObjectDistanceCalculator;

import artisynth.core.femmodels.FemModel.ElementFilter;

/**
 * Worker class to locate nearest elements within a bounding volume hierarchy.
 */
public class PointElem3dDistanceCalculator
   implements ObjectDistanceCalculator {

   Point3d myPnt;
   Point3d myNearest;
   FemElement3dBase myElement;
   double myDist;
   ElementFilter myFilter;
   TriangleIntersector myIntersector = new TriangleIntersector();

   public PointElem3dDistanceCalculator () {
      this (null);
   }

   public PointElem3dDistanceCalculator (ElementFilter filter) {
      if (myIntersector == null) {
         myIntersector = new TriangleIntersector();
      }
      myPnt = new Point3d();
      myNearest = new Point3d();
      myFilter = filter;
      reset();
   }

   @Override
   public void reset() {
      myElement = null;
      myDist = Double.POSITIVE_INFINITY;
   }

   public void setPoint (Point3d pnt, RigidTransform3d XBvhToWorld) {
      if (XBvhToWorld == RigidTransform3d.IDENTITY) {
         myPnt.set (pnt);
      }
      else {
         myPnt.inverseTransform (XBvhToWorld, pnt);
      }
   }          

   public void setPoint (Point3d pnt) {
      myPnt.set (pnt);
   }          

   public double nearestDistance (BVNode node) {
      return node.distanceToPoint (myPnt);
   }
               
   public double nearestDistance (Boundable e) {
      myElement = null;
      if (e instanceof FemElement3dBase) {
         FemElement3dBase elem = (FemElement3dBase)e;
         if (myFilter == null || myFilter.elementIsValid (elem)) {
            myElement = elem;
            myDist = Double.POSITIVE_INFINITY;
            // get the nearest distance for all the faces
            int[] tris = elem.getTriangulatedFaceIndices();
            FemNode3d[] nodes = elem.getNodes();
            Point3d p0, p1, p2;
            Point3d near = new Point3d();
            for (int i=0; i<tris.length/3; i++) {
               p0 = nodes[tris[i*3  ]].getPosition();
               p1 = nodes[tris[i*3+1]].getPosition();
               p2 = nodes[tris[i*3+2]].getPosition();
               double d = myIntersector.nearestpoint (
                  p0, p1, p2, myPnt, near, null);
               if (d < myDist) {
                  myDist = d;
                  myNearest.set (near);
               }
            }
            return myDist;
         }
      }
      return -1;
   }
      
   public Point3d nearestPoint() {
      return myNearest;
   }

   public FemElement3dBase nearestObject () {
      return myElement;
   }

   @Override
   public double nearestDistance() {
      return myDist;
   }
}
