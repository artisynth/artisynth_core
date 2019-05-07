package artisynth.core.mechmodels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Iterator;

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
import maspack.matrix.Vector2d;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorInterpolation;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.LineStyle;
import maspack.render.Renderer.PointStyle;
import maspack.render.Renderer.Shading;
import artisynth.core.mechmodels.Collidable.Group;
import artisynth.core.mechmodels.CollisionBehavior.Method;
import artisynth.core.mechmodels.CollisionBehavior.ColorMapType;
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
   private static int FORCE_GRP = 3;

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
         System.out.println ("  " + nrmlLen*(lam/maxlam));
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

   private double maxlam = 20;
//
//   private Vector3d getVec(float[] coords) {
//      return new Vector3d (coords[0], coords[1], coords[2]);
//   }

   private void maybeAddVertexFaceNormal (
      RenderObject ro, ContactConstraint cc, double normalLen) {

      addLineSeg (ro, cc.myCpnt0.myPoint, cc.myNormal, normalLen);
      // Vertex3d[] vtxs1 = cc.myCpnt1.myVtxs;
      // if (vtxs1.length == 3) {
      //    // then this is a vertex-face normal situation
      //    Vector3d nrml = new Vector3d();
      //    Vector3d v01 = new Vector3d();
      //    Vector3d v02 = new Vector3d();
      //    v01.sub (vtxs1[1].pnt, vtxs1[0].pnt); 
      //    v02.sub (vtxs1[2].pnt, vtxs1[0].pnt); 
      //    nrml.cross (v01, v02);
      //    nrml.normalize();
      //    MeshBase mesh = vtxs1[0].getMesh();
      //    if (!mesh.meshToWorldIsIdentity()) {
      //       nrml.transform (mesh.getMeshToWorld());
      //    }
      //    addLineSeg (ro, cc.myCpnt0.myPoint, nrml, normalLen);
      // }
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
      ro.createLineGroup();     // forces
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
            for (ContactConstraint cc : handler.myUnilaterals) {
               addLineSeg (ro, cc.myCpnt0.myPoint, cc.myNormal, normalLen);
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

      double forceScale = 0;
      if (behav.getDrawContactForces()) {
         forceScale = handler.getContactForceLenScale();
      }
      if (forceScale != 0 && cinfo != null) {
         ro.lineGroup (FORCE_GRP);
         Method method = handler.getMethod();
         if (method == Method.CONTOUR_REGION) {
            int numc = 0;
            for (ContactConstraint cc : handler.myPrevUnilaterals) {
               double len = forceScale*cc.myLambda;
               if (len != 0) {
                  addLineSeg (ro, cc.myCpnt0.myPoint, cc.myNormal, len);
               }
            }
         }
         else if (method != Method.INACTIVE) {
            for (ContactConstraint cc : handler.myBilaterals0.values()) {
               double len = forceScale*cc.myLambda;
               if (len != 0) {
                  addLineSeg (ro, cc.myCpnt0.myPoint, cc.myNormal, len);
               }
            }
            for (ContactConstraint cc : handler.myBilaterals1.values()) {
               double len = forceScale*cc.myLambda;
               if (len != 0) {
                  addLineSeg (ro, cc.myCpnt0.myPoint, cc.myNormal, len);
               }
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

         for (PenetratingPoint cpp : cinfo.getPenetratingPoints(0)) {
            if (cpp.distance > 0) {
               addPoint (ro, cpp.vertex.getWorldPoint());
            }
         }

         for (PenetratingPoint cpp : cinfo.getPenetratingPoints(1)) {
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

      RenderObject rd = null;
      if (behav.myDrawColorMap != ColorMapType.NONE && cinfo != null) {

         int num = behav.myColorMapCollidableNum;
         Collidable b0 = behav.getCollidable(0);
         CollidableBody h0 = handler.getCollidable(0);
         if (!(b0 instanceof Group)) {
            if (h0 != b0 && h0.getCollidableAncestor() != b0) {
               // then we want the *other* collidable body, so switch num
               num = (num == 0 ? 1 : 0);
            }
         }
         HashSet<Face> faces = new HashSet<Face>();
         HashMap<Vertex3d,Double> valueMap =
            createValueMap (faces, cinfo, handler, behav, num);

         if (faces.size() > 0) {
            double minv = 0;
            double maxv = 0;
            for (Double d : valueMap.values()) {
               if (d > maxv) {
                  maxv = d;
               }
               else if (d < minv) {
                  minv = d;
               }
            }
            rd = createPenetrationRenderObject (
               handler, valueMap, minv, maxv, faces);
         }
      }
      
      oldRob = myDepthRob;
      myDepthRob = rd;
   }

   protected void storeVertexForces (
      HashMap<Vertex3d,Double> valueMap,
      Vertex3d[] vtxs, double[] wgts, double lam) {

      if (vtxs != null) {
         for (int i=0; i<vtxs.length; i++) {
            double weightedLam = lam*wgts[i];
            Double prevLam = valueMap.get (vtxs[i]);
            if (prevLam != null) {
               weightedLam += prevLam;
            }
            valueMap.put (vtxs[i], weightedLam);                     
         }
      }
   }

   private boolean containsMeshVertices (ContactPoint cp, PolygonalMesh mesh) {
      return (cp.numVertices() > 0 && cp.getVertices()[0].getMesh() == mesh);
   }

   protected void storeVertexForces (
      HashMap<Vertex3d,Double> valueMap,
      ContactConstraint cc, PolygonalMesh mesh) {

      Vertex3d[] vtxs = null;
      double[] wgts = null;

      // check cpnt0 and cpnt1 for vertices belonging to the mesh
      if (containsMeshVertices (cc.myCpnt0, mesh)) {
         vtxs = cc.myCpnt0.getVertices();
         wgts = cc.myCpnt0.getWeights();
      }
      else if (containsMeshVertices (cc.myCpnt1, mesh)) {
         vtxs = cc.myCpnt1.getVertices();
         wgts = cc.myCpnt1.getWeights();
      }
      else {
         // have to find the face and vertices directly. Assume 
         // that we can use the position of cpnt0
         BVFeatureQuery query = new BVFeatureQuery();
         Vector2d uv = new Vector2d();
         Point3d nearPnt = new Point3d();
         Face face = query.getNearestFaceToPoint (
            nearPnt, uv, mesh, cc.myCpnt0.getPoint());
         if (face != null) {
            vtxs = face.getVertices();
            wgts = new double[] {1-uv.x-uv.y, uv.x, uv.y};
         }
      }
      // check vtxs == null just in case query.getNearestFaceToPoint failed
      // for some reason
      if (vtxs != null) {
         for (int i=0; i<vtxs.length; i++) {
            double lam = cc.myLambda*wgts[i];
            Double prevLam = valueMap.get (vtxs[i]);
            if (prevLam != null) {
               lam += prevLam;
            }
            valueMap.put (vtxs[i], lam);                     
         }
      }
   }

   HashMap<Vertex3d,Double> createValueMap (
      HashSet<Face> faces, ContactInfo cinfo,
      CollisionHandler handler, CollisionBehavior behav,
      int num) {
      
      HashMap<Vertex3d,Double> valueMap = new HashMap<Vertex3d,Double>();
      if (behav.myDrawColorMap == ColorMapType.PENETRATION_DEPTH) {
         ArrayList<PenetratingPoint> points;
         for (PenetratingPoint pp : cinfo.getPenetratingPoints (num)) {
            valueMap.put (pp.vertex, pp.distance);
         }
         for (Vertex3d vertex : valueMap.keySet()) {
            Iterator<HalfEdge> it = vertex.getIncidentHalfEdges();
            while (it.hasNext()) {
               HalfEdge he = it.next();
               Face face = he.getFace();
               if (!faces.contains(face)) {
                  faces.add (face);
               }
            }
         }
      }
      else if (behav.myDrawColorMap == ColorMapType.CONTACT_PRESSURE) {
         PolygonalMesh mesh;
         if (num == 0) {
            mesh = handler.myCollidable0.getCollisionMesh();
         }
         else {
            mesh = handler.myCollidable1.getCollisionMesh();
         }
         
         for (ContactConstraint cc : handler.myBilaterals0.values()) {
            if (cc.myLambda > 0) {
               storeVertexForces (valueMap, cc, mesh);
            }
         }
         for (ContactConstraint cc : handler.myBilaterals1.values()) {
            if (cc.myLambda > 0) {
               storeVertexForces (valueMap, cc, mesh);
            }
         }
         for (ContactConstraint cc : handler.myPrevUnilaterals) {
            if (cc.myLambda > 0) {
               storeVertexForces (valueMap, cc, mesh);
            }
         }
         for (Map.Entry<Vertex3d,Double> entry : valueMap.entrySet()) {
            // convert forces to pressures
            Vertex3d vertex = entry.getKey();
            double lam = entry.getValue();
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
            double pressure = 3*lam/adjacentFaceArea;
            valueMap.put (vertex, pressure);              
         }
      }
      return valueMap;
   }            

   int getColorIndex (
      ScalarRange range,
      Vertex3d vtx, HashMap<Vertex3d,Double> valueMap) { 

      double depth = 0;
      Double value = valueMap.get(vtx);
      if (value != null) {
         depth = range.clip(value);
      }
      int idx = (int)(255*((depth-range.getLowerBound())/range.getRange()));
      return idx;
   }

   RenderObject createPenetrationRenderObject (
      CollisionHandler handler, HashMap<Vertex3d,Double> valueMap,
      double minv, double maxv, HashSet<Face> faces) {
      
      RenderObject rd = new RenderObject();
      ScalarRange range = handler.myBehavior.myColorMapRange;
      range.updateInterval (minv, maxv);
      float[] rgb = new float[3];
      for (int i=0; i<256; i++) {
         handler.myManager.myColorMap.getRGB (i/255.0, rgb);
         rd.addColor (rgb);
      }

      Point3d wpnt = new Point3d();
      Vector3d wnrm = new Vector3d();

      for (Face face : faces) {
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

         int ci0 = getColorIndex (range, v0, valueMap);
         int ci1 = getColorIndex (range, v1, valueMap);
         int ci2 = getColorIndex (range, v2, valueMap);
            
         face.getWorldNormal (wnrm);
         int ni = rd.addNormal (wnrm);

         int v0idx = rd.addVertex (pi0, ni, ci0, -1);
         int v1idx = rd.addVertex (pi1, ni, ci1, -1);
         int v2idx = rd.addVertex (pi2, ni, ci2, -1);
         rd.addTriangle (v0idx, v1idx, v2idx);
      }
      return rd;
   }

   private void drawLines (
      Renderer renderer, RenderObject ro, RenderProps props, int width) {

      Shading savedShading = renderer.setLineShading (props);
      LineStyle style = props.getLineStyle();
      switch (style) {
         case LINE: {
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
         renderer.setLineColoring (props, /*highlight=*/false);
         drawLines (renderer, ro, props, props.getLineWidth());
      }

      if (ro.numLines(FORCE_GRP) > 0) {
         ro.lineGroup (FORCE_GRP);
         renderer.setEdgeColoring (props, /*highlight=*/false);
         drawLines (renderer, ro, props, props.getEdgeWidth());
      }

      if (ro.numLines(SEGMENT_GRP) > 0) {
         ro.lineGroup (SEGMENT_GRP);
         renderer.setLineColoring (props, /*highlight=*/false);
         drawLines (renderer, ro, props, props.getLineWidth());
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
         ColorInterpolation savedColorInterp = null;
         ColorInterpolation interp =
            handler.myBehavior.getColorMapInterpolation();
         if (interp != ColorInterpolation.RGB) {
            savedColorInterp = renderer.setColorInterpolation (interp);
         }
            
         renderer.setFaceStyle (props.getFaceStyle());
         renderer.setShading (Shading.SMOOTH); //props.getShading());

         renderer.setDepthOffset (2);
         renderer.drawTriangles (rd);

         if (savedColorInterp != null) {
            renderer.setColorInterpolation (savedColorInterp);
         }
         renderer.setFaceStyle (savedFaceStyle);
         renderer.setShading (savedShadeModel);
      }
   }

}
