package artisynth.core.mechmodels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import maspack.collision.ContactInfo;
import maspack.collision.ContactPlane;
import maspack.collision.EdgeEdgeContact;
import maspack.collision.IntersectionContour;
import maspack.collision.IntersectionPoint;
import maspack.collision.PenetratingPoint;
import maspack.collision.PenetrationRegion;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.TriTriIntersection;
import maspack.geometry.Vertex3d;
import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.BVFeatureQuery;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.LineStyle;
import maspack.render.Renderer.PointStyle;
import maspack.render.Renderer.Shading;
import artisynth.core.mechmodels.Collidable.Group;
import artisynth.core.mechmodels.CollisionBehavior.Method;
import artisynth.core.util.ScalarRange;

/**
 * Class to perform rendering for a CollisionHandlerX
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

   private void addLineSeg (
      RenderObject ro, Point3d p0, Vector3d dir, double len) {

      Point3d p1 = new Point3d(p0);
      p1.scaledAdd (len, dir);

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

//   private double getMaxLambda (Collection<ContactConstraint> cons, double max) {
//      for (ContactConstraint c : cons) {
//         double lam = c.myLambda;
//         if (lam > max) {
//            max = lam;
//         }
//      }
//      return max;
//   }

//   private double getMaxLambda (CollisionHandler handler) {
//      double max = 0;
//      max = getMaxLambda (handler.myBilaterals0.values(), max);
//      max = getMaxLambda (handler.myBilaterals1.values(), max);
//      max = getMaxLambda (handler.myUnilaterals, max);
//      return max;
//   }

   private double maxlam = 0.20;
//
//   private Vector3d getVec(float[] coords) {
//      return new Vector3d (coords[0], coords[1], coords[2]);
//   }

   private void maybeAddVertexFaceNormal (
      RenderObject ro, ContactConstraint cc, double normalLen) {

      Vertex3d[] vtxs1 = cc.myCpnt1.myVtxs;
      if (vtxs1.length == 3) {
         // then this is a vertex-face normal situation
         Vector3d nrml = new Vector3d();
         Vector3d v01 = new Vector3d();
         Vector3d v02 = new Vector3d();
         v01.sub (vtxs1[1].pnt, vtxs1[0].pnt); 
         v02.sub (vtxs1[2].pnt, vtxs1[0].pnt); 
         nrml.cross (v01, v02);
         nrml.normalize();
         MeshBase mesh = vtxs1[0].getMesh();
         if (!mesh.meshToWorldIsIdentity()) {
            nrml.transform (mesh.getMeshToWorld());
         }
         addLineSeg (ro, cc.myCpnt0.myPoint, nrml, normalLen);
      }
   }

   protected void findInsideFaces (
      Face face, BVFeatureQuery query, PolygonalMesh mesh, 
      ArrayList<Face> faces) {

      face.setVisited();
      Point3d pnt = new Point3d();
      HalfEdge he = face.firstHalfEdge();
      for (int i=0; i<3; i++) {
         if (he.opposite != null) {
            Face oFace = he.opposite.getFace();
            if (!oFace.isVisited()) {
               // check if inside
               oFace.computeWorldCentroid(pnt);

               boolean inside = query.isInsideOrientedMesh(mesh, pnt, -1);
               if (inside) {
                  faces.add(oFace);
                  findInsideFaces(oFace, query, mesh, faces);
               }
            }
         }
         he = he.getNext();
      }
   }

   protected void buildFaceSegments (
      RenderObject ro, CollisionHandler handler,
      ArrayList<TriTriIntersection> intersections) {

      BVFeatureQuery query = new BVFeatureQuery();

      PolygonalMesh mesh0 = handler.getCollidable(0).getCollisionMesh();
      PolygonalMesh mesh1 = handler.getCollidable(1).getCollisionMesh();

      ArrayList<Face> faces = new ArrayList<Face>();
      
      // mark faces as visited and add segments
      for (TriTriIntersection isect : intersections) {
         isect.face0.setVisited();
         isect.face1.setVisited();
         // add partials?
      }

      // mark interior faces and add segments
      for (TriTriIntersection isect : intersections) {
         if (isect.face0.getMesh() != mesh0) {
            findInsideFaces(isect.face0, query, mesh0, faces);
            findInsideFaces(isect.face1, query, mesh1, faces);
         } else {
            findInsideFaces(isect.face0, query, mesh1, faces);
            findInsideFaces(isect.face1, query, mesh0, faces);
         }
      }

      for (TriTriIntersection isect : intersections) {
         isect.face0.clearVisited();
         isect.face1.clearVisited();
      }

      // add faces to render object and clear visited flag
      Vector3d nrm = new Vector3d();
      Point3d p0 = new Point3d();
      Point3d p1 = new Point3d();
      Point3d p2 = new Point3d();
      for (Face face : faces) {
         face.clearVisited();
         face.getWorldNormal (nrm);
         ro.addNormal ((float)nrm.x, (float)nrm.y, (float)nrm.z);
         HalfEdge he = face.firstHalfEdge();
         he.head.getWorldPoint (p0);
         he = he.getNext();
         he.head.getWorldPoint (p1);
         he = he.getNext();
         he.head.getWorldPoint (p2);

         int v0idx = ro.vertex((float)p0.x, (float)p0.y, (float)p0.z);
         int v1idx = ro.vertex((float)p1.x, (float)p1.y, (float)p1.z);
         int v2idx = ro.vertex((float)p2.x, (float)p2.y, (float)p2.z);
         ro.addTriangle (v0idx, v1idx, v2idx);     
      }
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

      CollisionBehavior behav = handler.myBehavior;
      ContactInfo cinfo = handler.getLastContactInfo();

      if (behav.myDrawConstraints) {
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

      double normalLen = 0;
      if (behav.getDrawContactNormals()) {
         normalLen = handler.getContactNormalLen();
      }
      if (normalLen != 0 && cinfo != null) {
         ro.lineGroup (SEGMENT_GRP);
         Method method = handler.getMethod();
         if (method == Method.CONTOUR_REGION) {
            int numc = 0;
            for (ContactPlane region : cinfo.getContactPlanes()) {
               for (Point3d p : region.points) {
                  if (numc >= handler.myMaxUnilaterals) {
                     break;        
                  }
                  addLineSeg (ro, p, region.normal, normalLen);
               }
            }
         }
         else if (method != Method.INACTIVE) {
            for (ContactConstraint cc : handler.myBilaterals0.values()) {
               maybeAddVertexFaceNormal (ro, cc, normalLen);
            }
            for (ContactConstraint cc : handler.myBilaterals1.values()) {
               maybeAddVertexFaceNormal (ro, cc, normalLen);
            }
         }
      }
       
      if (behav.myDrawIntersectionContours &&
          props.getEdgeWidth() > 0 && cinfo != null) {

         ro.lineGroup (CONTOUR_GRP);
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

      if (behav.myDrawIntersectionPoints && cinfo != null) {
         
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

         if (behav.getMethod() == 
             CollisionBehavior.Method.VERTEX_EDGE_PENETRATION) {
            if (cinfo.getEdgeEdgeContacts() != null) {
               for (EdgeEdgeContact eec : cinfo.getEdgeEdgeContacts()) {
                  addPoint (ro, eec.point0);
                  addPoint (ro, eec.point1);
               }
            }
         }
      }

      if (behav.myDrawIntersectionFaces && cinfo != null) {
         ArrayList<TriTriIntersection> intersections = cinfo.getIntersections();
         if (intersections != null) {
            buildFaceSegments (ro, handler, intersections);
         }
      }

      RenderObject oldRob = myRob;
      myRob = ro;
//      if (oldRob != null) {
//         oldRob.dispose();
//      }

      RenderObject rd = null;
      if (behav.myDrawPenetrationDepth != -1 && cinfo != null) {

         int num = behav.myDrawPenetrationDepth;
         Collidable b0 = behav.getCollidable(0);
         CollidableBody h0 = handler.getCollidable(0);
         if (!(b0 instanceof Group)) {
            if (h0 != b0 && h0.getCollidableAncestor() != b0) {
               // then we want the *other* collidable body, so switch num
               num = (num == 0 ? 1 : 0);
            }
         }
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
//      if (oldRob != null) {
//         oldRob.dispose();
//      }
   }

   int getColorIndex (
      ScalarRange range,
      Vertex3d vtx, HashMap<Vertex3d,Double> depthMap) { 

      double depth = 0;
      Double value = depthMap.get(vtx);
      if (value != null) {
         depth = range.clip(value);
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
      ScalarRange range = handler.myBehavior.myPenetrationDepthRange;
      range.updateInterval (0, maxd);
      float[] rgb = new float[3];
      for (int i=0; i<256; i++) {
         handler.myManager.myColorMap.getRGB (i/255.0, rgb);
         rd.addColor (rgb);
      }

      Point3d wpnt = new Point3d();
      Vector3d wnrm = new Vector3d();
      for (PenetrationRegion region : regions) {
         for (Face face : region.getInsideFaces()) {
            HalfEdge he = face.firstHalfEdge();
            Vertex3d v0 = he.getHead();
            Vertex3d v1 = he.getNext().getHead();
            Vertex3d v2 = he.getTail();

            v0.getWorldPoint (wpnt);
            int pi0 = rd.addPosition (wpnt);
            v1.getWorldPoint (wpnt);
            int pi1 = rd.addPosition (wpnt);
            v2.getWorldPoint (wpnt);
            int pi2 = rd.addPosition (wpnt);

            int ci0 = getColorIndex (range, v0, depthMap);
            int ci1 = getColorIndex (range, v1, depthMap);
            int ci2 = getColorIndex (range, v2, depthMap);
            
            face.getWorldNormal (wnrm);
            int ni = rd.addNormal (wnrm);

            int v0idx = rd.addVertex (pi0, ni, ci0, -1);
            int v1idx = rd.addVertex (pi1, ni, ci1, -1);
            int v2idx = rd.addVertex (pi2, ni, ci2, -1);
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
