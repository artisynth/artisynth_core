package maspack.collision;

import java.util.ArrayList;
import java.util.Collection;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;

public class AhoIntersectionContour extends ArrayList<IntersectionPoint> {
   boolean myIsOpen;

   public AhoIntersectionContour () {
      // TODO Auto-generated constructor stub
   }

//   public IntersectionContour (int initialCapacity) {
//      super (initialCapacity);
//      // TODO Auto-generated constructor stub
//   }
//
//   public IntersectionContour (Collection<? extends MeshIntersectionPoint> c) {
//      super (c);
//      // TODO Auto-generated constructor stub
//   }
//   
   /**
    * Reverse the order of elements in this contour in place
    */
   public void reverse () {
      if (size() == 0 || size() == 1) {
         return;
      }

      int mid;
      if (size() % 2 == 0) {
         mid = size() / 2;
      } else {
         mid = (size() - 1) / 2;
      }
      
      IntersectionPoint tmpMip;
      for (int i=0; i<mid; i++) {
         tmpMip = get(i);
         set (i, get(size()-1-i));
         set (size()-1-i, tmpMip);
      }
   }
   
   /**
    * Return the area of this (approximately planar) contour.
    * 
    * @return area
    */
   public double getArea () {
      /* Compute the centroid of all the points. */
      int pSize = size ();
      if (pSize < 3)
         return (0);
      Point3d centroid = new Point3d ();
      int N = size();
      if (!isOpen()) {
         N--;
      }
      for (int i=0; i<N; i++) {
         centroid.add (get(i));
      }
      centroid.scale (1.0d / N);
      Vector3d cp = new Vector3d ();
      Vector3d normalSum = new Vector3d ();
      Vector3d rLast = new Vector3d ();
      rLast.sub (get (pSize - 1), centroid);
      Vector3d r = new Vector3d ();
      for (int i = 0; i < pSize; i++) {
         r.sub (get (i), centroid);
         cp.cross (r, rLast);
         normalSum.add (cp);
         Vector3d rTemp = rLast;
         rLast = r;
         r = rTemp;
      }
      return normalSum.norm() * 0.5;
   }
   
   public void setOpen(boolean isOpen) {
      myIsOpen = isOpen;
   }
   
   public boolean isOpen() {
      return myIsOpen;
   }
}
