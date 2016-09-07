package artisynth.core.mechmodels;

import java.util.*;
import java.awt.Color;

import maspack.collision.*;
import maspack.geometry.*;
import maspack.matrix.*;
import maspack.properties.*;
import maspack.render.*;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.LineStyle;
import maspack.render.Renderer.PointStyle;
import maspack.render.Renderer.Shading;
import maspack.util.*;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.mechmodels.MechSystem.FrictionInfo;
import artisynth.core.mechmodels.CollisionBehavior.Method;
import artisynth.core.mechmodels.CollisionHandler.LineSeg;
import artisynth.core.mechmodels.CollisionHandler.FaceSeg;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.util.TimeBase;

/**
 * Class to perform rendering for a CollisionHandler
 */
public class CollisionRenderer {

   RenderObject myRob;
   RenderObject myDepthRob;

   public CollisionRenderer () {
      myRob = null;
      myDepthRob = null;
   }

   private static int CONSTRAINT_GRP = 0;
   private static int SEGMENT_GRP = 1;
   private static int CONTOUR_GRP = 2;

   private void addPoint (RenderObject ro, Point3d p) {
      ro.addPoint (ro.vertex ((float)p.x, (float)p.y, (float)p.z));
   }

   private void addLineSeg (RenderObject ro, float[] p0, float[] p1) {
      int v0idx = ro.vertex (p0[0], p0[1], p0[2]);
      int v1idx = ro.vertex (p1[0], p1[1], p1[2]);
      ro.addLine (v0idx, v1idx);
   }

   private void addLineSeg (
      RenderObject ro, Point3d p0, Point3d p1) {

      int v0idx = ro.vertex ((float)p0.x, (float)p0.y, (float)p0.z);
      int v1idx = ro.vertex ((float)p1.x, (float)p1.y, (float)p1.z);
      ro.addLine (v0idx, v1idx);
   }

   private void addConstraintRenderInfo (
      RenderObject ro, Collection<ContactConstraint> cons, double nrmlLen) {

      Point3d p0 = new Point3d();
      Point3d p1 = new Point3d();
      for (ContactConstraint c : cons) {
         double lam = c.myLambda;
         p0.add (c.myCpnt0.myPoint, c.myCpnt1.myPoint);
         p0.scale (0.5);
         p1.scaledAdd (nrmlLen*(lam/maxlam), c.myNormal, p0);
         addLineSeg (ro, p0, p1);
      }
   }

   private double getMaxLambda (Collection<ContactConstraint> cons, double max) {
      for (ContactConstraint c : cons) {
         double lam = c.myLambda;
         if (lam > max) {
            max = lam;
         }
      }
      return max;
   }

   private double getMaxLambda (CollisionHandler handler) {
      double max = 0;
      max = getMaxLambda (handler.myBilaterals0.values(), max);
      max = getMaxLambda (handler.myBilaterals1.values(), max);
      max = getMaxLambda (handler.myUnilaterals, max);
      return max;
   }

   private double maxlam = 0.20;

   private Vector3d getVec(float[] coords) {
      return new Vector3d (coords[0], coords[1], coords[2]);
   }

   public void prerender (CollisionHandler handler, RenderProps props) {
      RenderObject ro = new RenderObject();

      ro.clearAll();

      ro.createLineGroup();     // constraints
      ro.createLineGroup();     // segments
      ro.createLineGroup();     // contours
      ro.createPointGroup();    // contact info
      ro.createTriangleGroup(); // intersection faces

      ro.addNormal (0, 0, 0);   // create default dummy normal

      if (handler.myDrawConstraints) {
         ro.lineGroup (CONSTRAINT_GRP);
         double nrmlLen = handler.getContactNormalLen();
         if (nrmlLen > 0) {
            addConstraintRenderInfo (
               ro, handler.myBilaterals0.values(), nrmlLen);
            addConstraintRenderInfo (
               ro, handler.myBilaterals1.values(), nrmlLen);
            addConstraintRenderInfo (
               ro, handler.myUnilaterals, nrmlLen);
         }
      }
       
      if (handler.myLineSegments != null) {
         ro.lineGroup (SEGMENT_GRP);         
         for (LineSeg seg : handler.myLineSegments) {
            addLineSeg (ro, seg.coords0, seg.coords1);
         }
      }
      
      if (handler.myDrawIntersectionContours &&
          props.getEdgeWidth() > 0 &&
          handler.getLastContactInfo() != null) {

         ro.lineGroup (CONTOUR_GRP);
         ContactInfo cinfo = handler.getLastContactInfo();
         // offset lines
         if (cinfo.getContours() != null) {
            for (IntersectionContour contour : cinfo.getContours()) {
               int vidx0 = ro.numVertices();
               for (IntersectionPoint p : contour) {
                  ro.addVertex (
                     ro.addPosition ((float)p.x, (float)p.y, (float)p.z));
               }
               int vidx1 = ro.numVertices()-1;
               ro.addLineLoop (vidx0, vidx1);
            }
         }
         else if (cinfo.getIntersections() != null){
            // use intersections to render lines
            for (TriTriIntersection tsect : cinfo.getIntersections()) {
               addLineSeg (ro, tsect.points[0], tsect.points[1]);
            }
         }
      }

      if (handler.myDrawIntersectionPoints &&
          handler.getLastContactInfo() != null) {
         
         ContactInfo cinfo = handler.getLastContactInfo();

         if (cinfo.getIntersections() != null) {
            for (TriTriIntersection tsect : cinfo.getIntersections()) {
               for (Point3d pnt : tsect.points) {
                  addPoint (ro, pnt);
               }
            }
         }

         for (PenetratingPoint cpp : cinfo.getPenetratingPoints0()) {
            if (cpp.distance > 0) {
               addPoint (ro, cpp.vertex.getWorldPoint());
            }
         }

         for (PenetratingPoint cpp : cinfo.getPenetratingPoints1()) {
            if (cpp.distance > 0) {
               addPoint (ro, cpp.vertex.getWorldPoint());
            }
         }

         if (handler.myMethod == 
             CollisionBehavior.Method.VERTEX_EDGE_PENETRATION) {
            if (cinfo.getEdgeEdgeContacts() != null) {
               for (EdgeEdgeContact eec : cinfo.getEdgeEdgeContacts()) {
                  addPoint (ro, eec.point0);
                  addPoint (ro, eec.point1);
               }
            }
         }
      }

      if (handler.myDrawIntersectionFaces &&
          handler.myFaceSegments != null) {

         for (FaceSeg seg : handler.myFaceSegments) {
            ro.addNormal ((float)seg.nrm.x, (float)seg.nrm.y, (float)seg.nrm.z);
            Point3d p0 = seg.p0;
            Point3d p1 = seg.p1;
            Point3d p2 = seg.p2;
            int v0idx = ro.vertex((float)p0.x, (float)p0.y, (float)p0.z);
            int v1idx = ro.vertex((float)p1.x, (float)p1.y, (float)p1.z);
            int v2idx = ro.vertex((float)p2.x, (float)p2.y, (float)p2.z);
            ro.addTriangle (v0idx, v1idx, v2idx);     
         }
      }

      RenderObject oldRob = myRob;
      myRob = ro;
      if (oldRob != null) {
         oldRob.dispose();
      }

      RenderObject rd = null;
      if (handler.myDrawMeshPenetration != -1 &&
          handler.getLastContactInfo() != null) {

         ContactInfo cinfo = handler.getLastContactInfo();
         int num = handler.myDrawMeshPenetration;
         ArrayList<PenetrationRegion> regions;
         ArrayList<PenetratingPoint> points;
         if (num == 0) {
            regions = cinfo.getPenetrationRegions0();
            points = cinfo.getPenetratingPoints0();
         }
         else {
            regions = cinfo.getPenetrationRegions1();
            points = cinfo.getPenetratingPoints1();
         }
         if (regions != null && regions.size() > 0) {
            rd = createPenetrationRenderObject (handler, points, regions);
         }
      }
      
      oldRob = myDepthRob;
      myDepthRob = rd;
      if (oldRob != null) {
         oldRob.dispose();
      }
   }

   int getColorIndex (
      CollisionHandler handler,
      Vertex3d vtx, HashMap<Vertex3d,Double> depthMap) { 

      DoubleInterval range = handler.myDrawPenetrationRange;
      double depth = 0;
      Double value = depthMap.get(vtx);
      if (value != null) {
         depth = range.clipToRange(value);
      }
      int idx = (int)(255*((depth-range.getLowerBound())/range.getRange()));
      return idx;
   }

   RenderObject createPenetrationRenderObject (
      CollisionHandler handler,
      ArrayList<PenetratingPoint> points, ArrayList<PenetrationRegion> regions) {
      
      RenderObject rd = new RenderObject();
      HashMap<Vertex3d,Double> depthMap = new HashMap<Vertex3d,Double>();
      double maxd = 0;
      for (PenetratingPoint pp : points) {
         depthMap.put (pp.vertex, pp.distance);
         if (pp.distance > maxd) {
            maxd = pp.distance;
         }
      }     
      switch (handler.myDrawPenetrationRanging) {
         case AUTO_FIT: {
            handler.myDrawPenetrationRange.set (0, maxd);
            break;
         }
         case AUTO_EXPAND: {
            if (maxd > handler.myDrawPenetrationRange.getUpperBound()) {
               handler.myDrawPenetrationRange.setUpperBound (maxd);
            }
            break;
         }
      }
      float[] rgb = new float[3];
      for (int i=0; i<256; i++) {
         handler.myColorMap.getRGB (i/255.0, rgb);
         rd.addColor (rgb);
      }

      for (PenetrationRegion region : regions) {
         for (Face face : region.getInsideFaces()) {
            HalfEdge he = face.firstHalfEdge();
            Vertex3d v0 = he.getHead();
            Vertex3d v1 = he.getNext().getHead();
            Vertex3d v2 = he.getTail();

            int pi0 = rd.addPosition (v0.pnt);
            int pi1 = rd.addPosition (v1.pnt);
            int pi2 = rd.addPosition (v2.pnt);

            int ci0 = getColorIndex (handler, v0, depthMap);
            int ci1 = getColorIndex (handler, v1, depthMap);
            int ci2 = getColorIndex (handler, v2, depthMap);

            int v0idx = rd.addVertex (pi0, -1, ci0, -1);
            int v1idx = rd.addVertex (pi1, -1, ci1, -1);
            int v2idx = rd.addVertex (pi2, -1, ci2, -1);
            rd.addTriangle (v0idx, v1idx, v2idx);
         }
      }
      return rd;
   }

   private void drawLines (
      Renderer renderer, RenderObject ro, RenderProps props) {
   
      LineStyle style = props.getLineStyle();
      Shading savedShading = renderer.setLineShading (props);
      renderer.setLineColoring (props, /*highlight=*/false);
      switch (style) {
         case LINE: {
            int width = props.getLineWidth();
            if (width > 0) {
               //renderer.setLightingEnabled (false);
               //renderer.setColor (props.getLineColorArray(), /*highlight=*/false);
               renderer.drawLines (ro, LineStyle.LINE, width);
               //renderer.setLightingEnabled (true);
            }
            break;
         }
            // do we need to handle the solid line case?
         case SPINDLE:
         case SOLID_ARROW:
         case CYLINDER: {
            double rad = props.getLineRadius();
            if (rad > 0) {
               //Shading savedShading = renderer.getShadeModel();
               //renderer.setLineLighting (props, /*highlight=*/false);
               renderer.drawLines (ro, style, rad);
               //renderer.setShadeModel(savedShading);
            }
            break;
         }
      }
      renderer.setShading(savedShading);
   }

   public void render (
      Renderer renderer, CollisionHandler handler, RenderProps props, int flags) {

      RenderObject ro = myRob;

      if (ro == null) {
         // XXX paranoid
         return;
      }
      if (ro.numLines(CONSTRAINT_GRP) > 0) {
         ro.lineGroup (CONSTRAINT_GRP);
         drawLines (renderer, ro, props);
      }

      if (ro.numLines(SEGMENT_GRP) > 0) {
         ro.lineGroup (SEGMENT_GRP);
         drawLines (renderer, ro, props);
      }

      if (ro.numLines(CONTOUR_GRP) > 0) {
         int width = props.getEdgeWidth();
         if (width > 0) {
            ro.lineGroup (CONTOUR_GRP);
            float[] rgb = props.getEdgeColorF();
            if (rgb == null) {
               rgb = props.getLineColorF();
            }
            renderer.setColor (rgb, /*highlight=*/false);
            Shading save = renderer.getShading();
            renderer.setShading (Shading.NONE);
            renderer.drawLines (ro, LineStyle.LINE, width);
            renderer.setShading (save);
         }
      }
      
      if (ro.numPoints() > 0) {
         PointStyle style = props.getPointStyle();
         double width = 0;
         Shading savedShading = null;
         renderer.setPointColoring (props, /*highlight=*/false);
         if (style == PointStyle.POINT) {
            width = props.getPointSize();
            savedShading = renderer.setShading (Shading.NONE);
         }
         else {
            width = props.getPointRadius();
         }
         renderer.drawPoints (ro, style, width);
         if (style == PointStyle.POINT) {
            renderer.setShading (savedShading);
         }
      }
      
      if (ro.numTriangles() > 0) {
         Shading savedShadeModel = renderer.getShading();
         FaceStyle savedFaceStyle = renderer.getFaceStyle();

         renderer.setFaceColoring (props, /*highlight=*/false);
         renderer.setFaceStyle (props.getFaceStyle());
         renderer.setShading (props.getShading());

         renderer.drawTriangles (ro);

         renderer.setFaceStyle (savedFaceStyle);
         renderer.setShading (savedShadeModel);
      }

      RenderObject rd = myDepthRob;

      if (rd != null && rd.numTriangles() > 0) {
         Shading savedShadeModel = renderer.getShading();
         FaceStyle savedFaceStyle = renderer.getFaceStyle();

         renderer.setFaceStyle (props.getFaceStyle());
         renderer.setShading (Shading.SMOOTH); //props.getShading());

         renderer.setDepthOffset (2);
         renderer.drawTriangles (rd);

         renderer.setFaceStyle (savedFaceStyle);
         renderer.setShading (savedShadeModel);
      }
   }

}
