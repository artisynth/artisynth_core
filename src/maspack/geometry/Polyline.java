/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;

public class Polyline {
   private Vertex3d[] myVtxs = new Vertex3d[0];
   private double[] myLenCoords = new double[0];
   private LineSegment[] mySegments = null;
   
   public int idx; // index into the face array

   public int myWorldCoordCnt = -1;

   /**
    * Creates an empty polyline with a specified index value.
    * 
    * @param idx
    * desired index value
    */
   public Polyline (int idx) {
      this.idx = idx;
   }
   
   /**
    * Creates a duplicate copy of a polyline
    */
   public Polyline(Polyline p) {
      this.idx = p.idx;
      Vertex3d[] ovtxs = p.getVertices();
      
      myVtxs = new Vertex3d[ovtxs.length];
      for (int i=0; i<myVtxs.length; i++) {
         myVtxs[i] = new Vertex3d(new Point3d(ovtxs[i].pnt), ovtxs[i].idx);
      }
      
      mySegments = null;
   }

   /**
    * Returns the index value for this polyline.
    * 
    * @return index value
    */
   public int getIndex() {
      return idx;
   }

   public int[] getVertexIndices() {
      int[] idxs = new int[myVtxs.length];
      for (int i=0; i<myVtxs.length; i++) {
         idxs[i] = myVtxs[i].getIndex();
      }
      return idxs;
   }

   /**
    * Creates a line a list of vertices.
    * 
    * @param vtxs
    * vertices to form the line
    * @param numVtxs
    * number of vertices
    */
   public void set (Vertex3d[] vtxs, int numVtxs) {
      myVtxs = new Vertex3d[numVtxs];
      for (int i=0; i<numVtxs; i++) {
         myVtxs[i] = vtxs[i];
      }
      mySegments = null;
   }

   /**
    * Computes the length of this line.
    */
   public double computeLength () {
      double len = 0;
      if (myVtxs.length > 1) {
         Point3d last = myVtxs[0].pnt;
         for (int i=1; i<myVtxs.length; i++) {
            len += last.distance(myVtxs[i].pnt);
            last = myVtxs[i].pnt;
         }
      }
      return len;
   }

   /**
    * Computes the length of this line and per-segment length info.
    */
   public double updateLengths () {
      double len = 0;
      if (myLenCoords.length != myVtxs.length) {
         myLenCoords = new double[myVtxs.length];
      }
      if (myVtxs.length > 1) {
         Point3d last = myVtxs[0].pnt;
         for (int i=1; i<myVtxs.length; i++) {
            double slen = last.distance(myVtxs[i].pnt); // segment length
            len += slen;
            last = myVtxs[i].pnt;
            myLenCoords[i] = len;
         }
      }
      for (int i = 0; i < myLenCoords.length; i++) {
         myLenCoords[i] /= len;
      }
      return len;
   }
   
   /**
    * Interpolate point along polyline 
    * 
    * @param s - normalized curve parameter in range [0-1]
    * @return point along polyline at s
    */
   public Point3d interpolatePosition(double s) {
      updateLengths ();
      Point3d pos = new Point3d();
      for (int i = 0; i < myLenCoords.length; i++) {
         double s0 = myLenCoords[i];
         double s1 = myLenCoords[i+1];
         if (s >=  s0 && s <= s1) {
            pos.interpolate (myVtxs[i].pnt, (s-s0)/(s1-s0), myVtxs[i+1].pnt);
            break;
         }
      }
      return pos;      
   }
   
   /**
    * Interpolate tangent vector to polyline at specified point 
    * 
    * @param s - normalized curve parameter in range [0-1]
    * @return tangent to polyline curve at s
    */
   public Vector3d interpolateTangent(double s) {
      updateLengths ();
      Vector3d tangent = new Vector3d ();
      for (int i = 0; i < myLenCoords.length; i++) {
         double s0 = myLenCoords[i];
         double s1 = myLenCoords[i+1];
         if (s >=  s0 && s <= s1) {
            if (s == s1 && i+1 != myLenCoords.length-1) { 
               // s is at vertex, take average tangent of adjacent segments
               // t = (v[i+1]-v[i])/(s1-s0)+(v[i+2]-v[i+1])/(s2-s1)
               double len1 = 1d/(s1-s0);
               double len2 = 1d/(myLenCoords[i+2]-s1);
               tangent.scaledAdd (len1, myVtxs[i+1].pnt);
               tangent.scaledAdd (-len1, myVtxs[i].pnt);
               tangent.scaledAdd (len2, myVtxs[i+2].pnt);
               tangent.scaledAdd (-len2, myVtxs[i+1].pnt);
               tangent.normalize ();
            } else {
               tangent.sub (myVtxs[i+1].pnt,myVtxs[i].pnt);
               tangent.normalize ();
            }
            break;
         }
      }
      return tangent;      
   }

   public void updateBounds (Point3d min, Point3d max) {
      for (int i=0; i<myVtxs.length; i++) {
         myVtxs[i].pnt.updateBounds (min, max);
      }
   }

   public int numVertices() {
      return myVtxs.length;
   }

   // public PolygonalMesh getMesh() {
   //    return he0.head.getMesh();
   // }

   /**
    * Do not replace individual vertices in this array
    * @return list of vertices
    */
   public Vertex3d[] getVertices() {
      return myVtxs;
   }

   public Vertex3d getVertex (int idx) {
      if (idx < 0 || idx >= myVtxs.length) {
         throw new IllegalArgumentException ("index " + idx + " out of bounds");
      }
      return myVtxs[idx];
   }
   
   /**
    * Creates a list of line segments
    * @return list of line segments
    */
   public LineSegment[] getSegments() {
      if (mySegments == null) {
         LineSegment[] segs = new LineSegment[numVertices()-1];
         for (int i=0; i<numVertices()-1; ++i) {
            segs[i] = new LineSegment(myVtxs[i], myVtxs[i+1]); 
         }
         mySegments = segs;
      }
      return mySegments;
   }
   
//   /**
//    * Returns a trimmed polyline array that lies within the sphere, null if
//    * the current polyline does not intersect the sphere.
//    * @param centre centre of the sphere of interest
//    * @param radius radius of sphere
//    * @return
//    */
//   public ArrayList<Polyline> getInsideSphere(Point3d centre, double radius) {
//      
//      double r2 = radius*radius;
//      
//      ArrayList<Polyline> segments = new ArrayList<Polyline>();
//      ArrayList<Vertex3d> segment = new ArrayList<Vertex3d>();
//      
//      boolean prevInside = false;
//      Point3d p = new Point3d();  // p2-c
//      Point3d dp = new Point3d(); // p1-p2
//      double a,b,c,d, lambda1, lambda2;     // for use with quadratic equation
//      int idx = 0;
//      
//      if (isInsideSphere(myVtxs[0], centre, radius)) {
//         segment.add(myVtxs[0]);
//         prevInside = true;
//      }
//      
//      for (int i=1; i<myVtxs.length; i++) {
//         
//         p.sub(myVtxs[i].getPosition(), centre);
//         dp.sub(myVtxs[i-1].getPosition(), myVtxs[i].getPosition());
//         
//         // find intersection with sphere
//         a = dp.normSquared();
//         b = 2*dp.dot(p);
//         c = p.normSquared()-r2;
//         
//         d = b*b-4*a*c;
//         if (d >=0) {
//            d = Math.sqrt(d);
//            lambda1=(-b+d)/(2*a);
//            lambda2=(-b-d)/(2*a);
//         } else {
//            lambda1 = Double.NaN;
//            lambda2 = Double.NaN;
//         }
//         
//         
//         // if it is inside, add to list
//         if (p.normSquared() <= r2) {
//            if (prevInside) {
//               segment.add(myVtxs[i]);
//            } else {
//               
//               if (lambda1 >=0 && lambda1 <=1) {
//                  p.scaledAdd(lambda1, dp, myVtxs[i].getPosition());
//               } else {
//                  p.scaledAdd(lambda2, dp, myVtxs[i].getPosition());
//               }
//               
//               // create and add start of segment
//               Vertex3d vtx = new Vertex3d(p);
//               segment.add(vtx);
//               segment.add(myVtxs[i]);
//            }            
//            prevInside = true;
//         } else {
//            // if previously inside, find single intersection
//            if (prevInside) {
//               
//               if (lambda1>=0 && lambda1<=1) {
//                  p.scaledAdd(lambda1, dp, myVtxs[i].getPosition());
//               } else {
//                  p.scaledAdd(lambda2, dp, myVtxs[i].getPosition());
//               }
//               Vertex3d vtx = new Vertex3d(p);
//               segment.add(vtx);
//               
//            } else {
//               
//               // check if passes through sphere
//               if (d >= 0) {
//                  if (lambda1 >=0 && lambda1 <=1 && lambda2>=0 && lambda2<=1) {
//                     p.scaledAdd(lambda1, dp, myVtxs[i].getPosition());
//                     Vertex3d vtx = new Vertex3d(p);
//                     segment.add(vtx);
//                     p.scaledAdd(lambda2, dp, myVtxs[i].getPosition());
//                     vtx = new Vertex3d(p);
//                     segment.add(vtx);
//                  }
//                  
//               }  // done checking if crossed sphere             
//            } // done checking if we were previously inside sphere
//            
//            // check if we need to complete the segment
//            if (segment.size()>0) {
//               Polyline newLine = new Polyline(segments.size());
//               newLine.set(segment.toArray(new Vertex3d[1]), segment.size());
//               segments.add(newLine);
//               
//               segment = new ArrayList<Vertex3d>();
//            }
//            
//            prevInside = false;
//         } // done checking if we are currently in sphere
//         
//      } // done looping through vertices
//
//      
//      // check if we need to complete the final segment
//      if (segment.size()>0) {
//         Polyline newLine = new Polyline(segments.size());
//         newLine.set(segment.toArray(new Vertex3d[1]), segment.size());
//         segments.add(newLine);
//      }
//      
//      return segments;
//      
//   }
//   
//   public boolean isInsideSphere(Vertex3d v, Point3d centre, double radius) {
//      
//      Point3d p = new Point3d();
//      p.sub(v.getPosition(), centre);
//      if (p.normSquared() <= radius*radius) {
//         return true;
//      }
//      return false;
//      
//   }
   
   
   public static void main (String[] args) {
      System.out.println ("PolyLine test...");
      Point3d[] pnts = new Point3d[] {
                                      new Point3d(0,0,0),
                                      new Point3d(0,1,0),
                                      new Point3d(8,1,0),
                                      new Point3d(8,0,0)
      };
      
      PolylineMesh mesh = new PolylineMesh ();
      for (Point3d pnt : pnts)
         mesh.addVertex (pnt);
      Polyline line = mesh.addLine (mesh.getVertices ().toArray (new Vertex3d[0]));
      
      double l = line.computeLength ();
      double s = 0.9;
      Point3d p = line.interpolatePosition (s);
      Vector3d v = line.interpolateTangent (s);
      System.out.println ("len="+l+
         ", pnt("+s+")=["+p.toString ("%2.1f")+"]"+
         ", tangent("+s+")=["+v.toString ("%2.1f")+"]");
   }

}
