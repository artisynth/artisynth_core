package artisynth.core.mechmodels;

import java.util.*;

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

   public CollisionRenderer () {
      myRob = null;
   }

   private static int CONSTRAINT_GRP = 0;
   private static int SEGMENT_GRP = 1;
   private static int CONTOUR_GRP = 2;

   private void addPoint (RenderObject r, Point3d p) {
      r.addPoint (r.vertex ((float)p.x, (float)p.y, (float)p.z));
   }

   private void addLineSeg (RenderObject r, float[] p0, float[] p1) {
      int v0idx = r.vertex (p0[0], p0[1], p0[2]);
      int v1idx = r.vertex (p1[0], p1[1], p1[2]);
      r.addLine (v0idx, v1idx);
   }

   private void addLineSeg (
      RenderObject r, Point3d p0, Point3d p1) {

      int v0idx = r.vertex ((float)p0.x, (float)p0.y, (float)p0.z);
      int v1idx = r.vertex ((float)p1.x, (float)p1.y, (float)p1.z);
      r.addLine (v0idx, v1idx);
   }

   private void addConstraintRenderInfo (
      RenderObject r, Collection<ContactConstraint> cons, double nrmlLen) {

      Point3d p0 = new Point3d();
      Point3d p1 = new Point3d();
      for (ContactConstraint c : cons) {
         double lam = c.myLambda;
         p0.add (c.myCpnt0.myPoint, c.myCpnt1.myPoint);
         p0.scale (0.5);
         p1.scaledAdd (nrmlLen*(lam/maxlam), c.myNormal, p0);
         addLineSeg (r, p0, p1);
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
      RenderObject r = new RenderObject();

      r.clearAll();


      r.createLineGroup();     // constraints
      r.createLineGroup();     // segments
      r.createLineGroup();     // contours
      r.createPointGroup();    // contact info
      r.createTriangleGroup(); // intersection faces

      r.addNormal (0, 0, 0);   // create default dummy normal

      if (handler.myDrawConstraints) {
         r.lineGroup (CONSTRAINT_GRP);
         double nrmlLen = handler.getContactNormalLen();
         if (nrmlLen > 0) {
            addConstraintRenderInfo (
               r, handler.myBilaterals0.values(), nrmlLen);
            addConstraintRenderInfo (
               r, handler.myBilaterals1.values(), nrmlLen);
            addConstraintRenderInfo (
               r, handler.myUnilaterals, nrmlLen);
         }
      }
       
      if (handler.myLineSegments != null) {
         r.lineGroup (SEGMENT_GRP);         
         for (LineSeg seg : handler.myLineSegments) {
            addLineSeg (r, seg.coords0, seg.coords1);
         }
      }
      
      if (handler.myDrawIntersectionContours &&
          props.getEdgeWidth() > 0 &&
          handler.getLastContactInfo() != null) {

         r.lineGroup (CONTOUR_GRP);
         ContactInfo contactInfo = handler.getLastContactInfo();
         // offset lines
         if (contactInfo.contours != null) {
            for (MeshIntersectionContour contour : contactInfo.contours) {
               int vidx0 = r.numVertices();
               for (MeshIntersectionPoint p : contour) {
                  r.addVertex (
                     r.addPosition ((float)p.x, (float)p.y, (float)p.z));
               }
               int vidx1 = r.numVertices()-1;
               r.addLineLoop (vidx0, vidx1);
            }
         }
         else if (contactInfo.intersections != null){
            // use intersections to render lines
            for (TriTriIntersection tsect : contactInfo.intersections) {
               addLineSeg (r, tsect.points[0], tsect.points[1]);
            }
         }
      }

      if (handler.myDrawIntersectionPoints &&
          handler.getLastContactInfo() != null) {
         
         ContactInfo contactInfo = handler.getLastContactInfo();

         if (contactInfo.intersections != null) {
            for (TriTriIntersection tsect : contactInfo.intersections) {
               for (Point3d pnt : tsect.points) {
                  addPoint (r, pnt);
               }
            }
         }

         if (contactInfo.points0 != null) {
            for (ContactPenetratingPoint cpp : contactInfo.points0) {
               if (cpp.distance > 0) {
                  addPoint (r, cpp.vertex.getWorldPoint());
               }
            }
         }

         if (contactInfo.points1 != null) {
            for (ContactPenetratingPoint cpp : contactInfo.points1) {
               if (cpp.distance > 0) {
                  addPoint (r, cpp.vertex.getWorldPoint());
               }
            }
         }

         if (contactInfo.edgeEdgeContacts != null) {
            for (EdgeEdgeContact eec : contactInfo.edgeEdgeContacts) {
               addPoint (r, eec.point0);
               addPoint (r, eec.point1);
            }
         }
      }

      if (handler.myDrawIntersectionFaces &&
          handler.myFaceSegments != null) {

         for (FaceSeg seg : handler.myFaceSegments) {
            r.addNormal ((float)seg.nrm.x, (float)seg.nrm.y, (float)seg.nrm.z);
            Point3d p0 = seg.p0;
            Point3d p1 = seg.p1;
            Point3d p2 = seg.p2;
            int v0idx = r.vertex((float)p0.x, (float)p0.y, (float)p0.z);
            int v1idx = r.vertex((float)p1.x, (float)p1.y, (float)p1.z);
            int v2idx = r.vertex((float)p2.x, (float)p2.y, (float)p2.z);
            r.addTriangle (v0idx, v1idx, v2idx);     
         }
      }
      if (myRob != null) {
         myRob.dispose();
      }
      myRob = r;
   }

   private void drawLines (
      Renderer renderer, RenderObject r, RenderProps props) {
   
      LineStyle style = props.getLineStyle();
      Shading savedShading = renderer.setLineShading (props);
      renderer.setLineColoring (props, /*selected=*/false);
      switch (style) {
         case LINE: {
            int width = props.getLineWidth();
            if (width > 0) {
               //renderer.setLightingEnabled (false);
               //renderer.setColor (props.getLineColorArray(), /*selected=*/false);
               renderer.drawLines (r, LineStyle.LINE, width);
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
               //renderer.setLineLighting (props, /*selected=*/false);
               renderer.drawLines (r, style, rad);
               //renderer.setShadeModel(savedShading);
            }
            break;
         }
      }
      renderer.setShading(savedShading);
   }

   public void render (
      Renderer renderer, CollisionHandler handler, RenderProps props, int flags) {

      RenderObject r = myRob;

      if (r == null) {
         // XXX paranoid
         return;
      }
            
      if (r.numLines(CONSTRAINT_GRP) > 0) {
         r.lineGroup (CONSTRAINT_GRP);
         drawLines (renderer, r, props);
      }

      if (r.numLines(SEGMENT_GRP) > 0) {
         r.lineGroup (SEGMENT_GRP);
         drawLines (renderer, r, props);
      }

      if (r.numLines(CONTOUR_GRP) > 0) {
         int width = props.getEdgeWidth();
         if (width > 0) {
            r.lineGroup (CONTOUR_GRP);
            float[] rgb = props.getEdgeColorF();
            if (rgb == null) {
               rgb = props.getLineColorF();
            }
            renderer.setColor (rgb, false);
            Shading save = renderer.getShading();
            renderer.setShading (Shading.NONE);
            renderer.drawLines (r, LineStyle.LINE, width);
            renderer.setShading (save);
         }
      }
      
      if (r.numPoints() > 0) {
         PointStyle style = props.getPointStyle();
         double width = 0;
         Shading savedShading = null;
         renderer.setPointColoring (props, /*selected=*/false);
         if (style == PointStyle.POINT) {
            width = props.getPointSize();
            savedShading = renderer.setShading (Shading.NONE);
         }
         else {
            width = props.getPointRadius();
         }
         renderer.drawPoints (r, style, width);
         if (style == PointStyle.POINT) {
            renderer.setShading (savedShading);
         }
      }
      
      if (r.numTriangles() > 0) {
         Shading savedShadeModel = renderer.getShading();
         FaceStyle savedFaceStyle = renderer.getFaceStyle();

         renderer.setFaceColoring (props, /*selected=*/false);
         renderer.setFaceStyle (props.getFaceStyle());
         renderer.setShading (props.getShading());

         renderer.drawTriangles (r);

         renderer.setFaceStyle (savedFaceStyle);
         renderer.setShading (savedShadeModel);
      }
   }

}
