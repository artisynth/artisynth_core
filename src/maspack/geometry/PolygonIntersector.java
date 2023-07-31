package maspack.geometry;

import java.util.*;

import maspack.matrix.*;
import maspack.util.*;
import com.seisw.util.geom.*;

/**
 * Utility class to intersect 2D and 3D polygons. It uses a Java implementation
 * of General Polygon Clipper called GPCJ2.
 *
 * <p> GPCJ2 is an adaptation of the GPCJ library, that is itself a Java port
 * of the GPC library. It was developed from GPCJ by Christian Lutz:
 * https://github.com/ChristianLutz/gpcj
 *
 * <p>GPC (General Polygon Clipper) was written by Alan Murta
 * http://www.cs.man.ac.uk/~toby/gpc/
 *
 * <p>GPCJ (General Polygon Clipper for Java) was written by Daniel
 * Bridenbecker and extended by David Legland:
 * http://web.archive.org/web/20090213122910/http://www.seisw.com/GPCJ/GPCJ.html
 *
 * <p>Original licence for GPC:
 *
 * <p>The SEI Software Open Source License, Version 1.0
 *
 * <p> Copyright (c) 2004, Solution Engineering, Inc.  All rights reserved.
 *
 * <p>Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * <ol>
 *
 * <li> Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer </li>
 *
 * <li> The end-user documentation included with the redistribution, if any,
 * must include the following acknowledgment: "This product includes software
 * developed by the Solution Engineering, Inc. (http://www.seisw.com/)."
 * Alternately, this acknowledgment may appear in the software itself, if and
 * wherever such third-party acknowledgments normally appear.</li>
 *
 * <li>The name "Solution Engineering" must not be used to endorse or promote
 * products derived from this software without prior written permission. For
 * written permission, please contact admin@seisw.com </li>
 *
 * </ol>
 * 
 * <p>    
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL SOLUTION ENGINEERING, INC. OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
public class PolygonIntersector {

   /**
    * Internal method for testing and debugging
    */
   private void printPoly (String name, Poly poly) {
      System.out.println ("num inner=" + poly.getNumInnerPoly());
      for (int i=0; i<poly.getNumInnerPoly(); i++) {
         Poly inner = poly.getInnerPoly(i);
         System.out.println ("  nump: " + inner.getNumPoints());
         for (int k=0; k<inner.getNumPoints(); k++) {
            System.out.println ("   "+inner.getX(k) + " " + poly.getY(k));
         }
      }
   }

   /**
    * Create a PolyDefault (used by GPCJ) from a Polygon2d.
    *
    * @param poly2d 2D polygon
    * @return GPCJ polygon
    */
   private PolyDefault createPoly (Polygon2d poly2d) {
      PolyDefault poly = new PolyDefault();
      Vertex2d vtx = poly2d.firstVertex;
      if (vtx != null) {
         do {
            poly.add (vtx.pnt.x, vtx.pnt.y);
            vtx = vtx.next;
         }
         while (vtx != poly2d.firstVertex);
      }
      return poly;
   }

   /**
    * Create a Polygon2d from a GPCJ Poly. Only the main polygon is used.
    */
   private Polygon2d createPolygon2d (Poly poly) {
      Polygon2d poly2d = new Polygon2d();
      for (int i=0; i<poly.getNumPoints(); i++) {
         poly2d.addVertex (poly.getX(i), poly.getY(i));
      }
      return poly2d;
   }

   /**
    * Create a PolyDefault (used by GPCJ) from a Polygon3d projected
    * into a plane.
    *
    * @param poly3d 3D polygon
    * @param TPW transform from plane to world, used for the projection
    * @return GPCJ polygon
    */
   private PolyDefault createPoly (Polygon3d poly3d, RigidTransform3d TPW) {
      PolyDefault poly = new PolyDefault();
      PolygonVertex3d vtx = poly3d.firstVertex;
      if (vtx != null) {
         Point3d pnt2d = new Point3d(); // vertex projection into the plane
         do {
            pnt2d.inverseTransform (TPW, vtx.pnt);
            poly.add (pnt2d.x, pnt2d.y);
            vtx = vtx.next;
         }
         while (vtx != poly3d.firstVertex);
      }
      return poly;
   }

   /**
    * Create a Polygon3d from a GPCJ Poly. Only the main polygon is used.
    */
   private Polygon3d createPolygon3d (Poly poly, RigidTransform3d TPW) {
      Polygon3d poly3d = new Polygon3d();
      Point3d pnt3d = new Point3d();
      for (int i=0; i<poly.getNumPoints(); i++) {
         pnt3d.set (poly.getX(i), poly.getY(i), 0);
         pnt3d.transform (TPW);
         poly3d.appendVertex (new PolygonVertex3d (pnt3d));
      }
      return poly3d;
   }

   /**
    * Intersects two 2D polygons and returns an array of the results.
    *
    * @param poly0 first polygon to intersect
    * @param poly1 second polygon to intersect
    * @return results of polygon intersection
    */
   public Polygon2d[] intersectPolygons (Polygon2d poly0, Polygon2d poly1) {
      PolyDefault p0 = createPoly (poly0);
      PolyDefault p1 = createPoly (poly1);
      Poly isect = p0.intersection (p1);
      Polygon2d[] result = new Polygon2d[isect.getNumInnerPoly()];
      for (int i=0; i<result.length; i++) {
         result[i] = createPolygon2d (isect.getInnerPoly(i));
      }
      return result;
   }

   /**
    * Intersects two 3D polygons in a given plane and returns an array of the
    * results.
    *
    * @param poly0 first polygon to intersect
    * @param poly1 second polygon to intersect
    * @param TPW transform from plane to world, used for the projection
    * @return results of polygon intersection
    */
   public Polygon3d[] intersectPolygons (
      Polygon3d poly0, Polygon3d poly1, RigidTransform3d TPW) {
      PolyDefault p0 = createPoly (poly0, TPW);
      PolyDefault p1 = createPoly (poly1, TPW);
      //printPoly ("poly0", p0);
      //printPoly ("poly1", p1);
      Poly isect = p0.intersection (p1);
      Polygon3d[] result = new Polygon3d[isect.getNumInnerPoly()];
      double a = 0;
      for (int i=0; i<result.length; i++) {
         result[i] = createPolygon3d (isect.getInnerPoly(i), TPW);
         a += isect.getInnerPoly(i).getArea();
      }
      return result;
   }

   /**
    * Intersects two 2D polygons and returns an array of the results.
    *
    * @param poly0 first polygon to intersect
    * @param poly1 second polygon to intersect
    * @return results of polygon intersection
    */
   public static Polygon2d[] intersect (Polygon2d poly0, Polygon2d poly1) {
      PolygonIntersector pi = new PolygonIntersector();
      return pi.intersectPolygons (poly0, poly1);
   }

   /**
    * Intersects two 3D polygons in a given plane and returns an array of the
    * results.
    *
    * @param poly0 first polygon to intersect
    * @param poly1 second polygon to intersect
    * @param TPW transform from plane to world, used for the projection
    * @return results of polygon intersection
    */
   public static Polygon3d[] intersect (
      Polygon3d poly0, Polygon3d poly1, RigidTransform3d TPW) {
      PolygonIntersector pi = new PolygonIntersector();
      return pi.intersectPolygons (poly0, poly1, TPW);
   }

}
