package artisynth.demos.test;

import java.awt.Color;
import java.util.*;
import java.io.*;

import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.ControllerBase;
import artisynth.core.modelbase.NameChangeEvent;
import maspack.collision.AbstractCollider;
import maspack.collision.ContactInfo;
import maspack.collision.ContactPlane;
import maspack.collision.MeshCollider;
import maspack.collision.SurfaceMeshIntersector;
import maspack.collision.SurfaceMeshIntersector.RegionType;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.MeshBase;
import maspack.geometry.TriTriIntersection;
import maspack.geometry.Vertex3d;
import maspack.geometry.LineSegment;
import maspack.collision.*;
import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.RigidTransform3d;
import maspack.properties.PropertyList;
import maspack.render.Renderer;
import maspack.render.Renderer.*;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.color.ColorMap;
import maspack.render.color.HueColorMap;
import maspack.util.DoubleHolder;
import maspack.util.ArraySupport;
import maspack.util.ArraySort;
import maspack.util.DoubleInterval;
import maspack.util.RandomGenerator;
import maspack.util.NumberFormat;
import maspack.util.FunctionTimer;
import maspack.util.InternalErrorException;

public class IntersectionTester extends ControllerBase {

   private SurfaceMeshIntersector myIntersector;
   private boolean myContoursOnly;
   private SurfaceMeshIntersector.CSG myCSGOperation = SurfaceMeshIntersector.CSG.NONE;
   private boolean myRenderCSGMesh = false;
   private PolygonalMesh myMesh0;
   private PolygonalMesh myMesh1;
   private ContactInfo myContactInfo;
   private ArrayList<IntersectionContour> myContours;
   private PolygonalMesh myCSGMesh;
   private int myRecursiveCSG = -1;

   static abstract class RandomTransform {

      public abstract RigidTransform3d nextTransform();
   }

   static class RandomTransform2d extends RandomTransform {

      double myRange;

      public RandomTransform2d (double range) {
         myRange = range;
      }

      public RigidTransform3d nextTransform() {
         double x = RandomGenerator.nextDouble (-myRange, myRange);
         double y = RandomGenerator.nextDouble (-myRange, myRange);
         double ang = RandomGenerator.nextDouble (-Math.PI, Math.PI);
         RigidTransform3d T = new RigidTransform3d (x, y, 0, ang, 0, 0);
         return T;
      }
   }

   private RigidTransform3d myTMW2Base;

   private DoubleInterval myZPerturb0 = null;
   private DoubleInterval myZPerturb1 = null;

   public boolean getRenderCSGMesh() {
      return myRenderCSGMesh;
   }

   public void setRenderCSGMesh (boolean enable) {
      if (enable != myRenderCSGMesh) {
         myRenderCSGMesh = enable;
      }
   }

   public SurfaceMeshIntersector.CSG getCSGOperation() {
      return myCSGOperation;
   }

   public void setCSGOperation (SurfaceMeshIntersector.CSG op) {
      if (op != myCSGOperation) {
         myCSGOperation = op;
         updateCSGMesh();
      }
   }

   public void setRecursiveCSG (int meshNum) {
      myRecursiveCSG = meshNum;      
   }

   public int getRecursiveCSG () {
      return myRecursiveCSG;
   }

   public void setZPerturb0 (double min, double max) {
      if (min == 0 && max == 0) {
         myZPerturb0 = null;
      }
      else {
         myZPerturb0 = new DoubleInterval (min, max);
      }
   }

   public void setZPerturb1 (double min, double max) {
      if (min == 0 && max == 0) {
         myZPerturb1 = null;
      }
      else {
         myZPerturb1 = new DoubleInterval (min, max);
      }
   }

   public boolean isInterior (Vertex3d v) {
      Iterator<HalfEdge> it = v.getIncidentHalfEdges();
      while (it.hasNext()) {
         HalfEdge he = it.next();
         if (he.opposite == null) {
            return false;
         }
      }
      return true;
   }

   private double EPS = 1e-14;
   
   private boolean debug = false;
   
   // public IntersectionTester (String name, RigidBody c1, RigidBody c2, double M) {
   //    this(c1, c2, M);
   //    setName(name);
   //    notifyParentOfChange(new NameChangeEvent(this, null));
   // }
   
   public IntersectionTester (
      PolygonalMesh mesh0, PolygonalMesh mesh1, double maxPenetrationDistance) {
      super ();
      myMesh0 = mesh0;
      myMesh1 = mesh1;
      myIntersector = new SurfaceMeshIntersector();
      myContactInfo = null;
      myContours = null;
      setRenderProps (createRenderProps ());
   }

   public void setMesh (PolygonalMesh mesh, int meshNum) {
      if (meshNum == 0) {
         myMesh0 = mesh;
      }
      else if (meshNum == 1) {
         myMesh1 = mesh;
      }
   }

   //protected RenderProps myRenderProps = null;

   public static PropertyList myProps = new PropertyList (
      IntersectionTester.class, ControllerBase.class);

   static private RenderProps defaultRenderProps = new RenderProps ();

   static {
      myProps.add (
         "renderProps * *", "render properties for this constraint",
         defaultRenderProps);
      myProps.add (
         "renderCSGMesh", "controls whether CSG mesh is rendered",
         false);
      myProps.add (
         "CSGOperation", "controls which CSG mesh is created, if any",
         SurfaceMeshIntersector.CSG.NONE);
   }

   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public boolean getContoursOnly() {
      return myContoursOnly;
   }      

   public void setContoursOnly (boolean enable) {
      myContoursOnly = enable;
   }

   public PolygonalMesh getCSGMesh() {
      return myCSGMesh;
   }

   @Override
   public void apply (double t0, double t1) {
      // TODO Auto-generated method stub

   }

   private String formatPoint (String pfx, NumberFormat fmt, Point3d p) {
      return (pfx+fmt.format(p.x)+", "+fmt.format(p.y)+", "+fmt.format(p.z)+",");
   }

   class CPPVertexComparator implements Comparator<PenetratingPoint> {
      public int compare (
         PenetratingPoint p0, PenetratingPoint p1) {
         
         if (p0.vertex.getIndex() < p1.vertex.getIndex()) {
            return -1;
         }
         else if (p0.vertex.getIndex() == p1.vertex.getIndex()) {
            return 0;
         }
         else {
            return 1;
         }
      }
   }
   
   void updateCSGMesh() {
      PolygonalMesh csgMesh = null;
      try {
         switch (myCSGOperation) {
            case INTERSECTION: {
               csgMesh = myIntersector.findIntersection (myMesh0, myMesh1);
               break;
            }
            case UNION: {
               csgMesh = myIntersector.findUnion (myMesh0, myMesh1);
               break;
            }
            case DIFFERENCE01: {
               csgMesh = myIntersector.findDifference01 (myMesh0, myMesh1);
               break;
            }
            case DIFFERENCE10: {
               csgMesh = myIntersector.findDifference10 (myMesh0, myMesh1);
               break;
            }
         }
      }
      catch (InternalErrorException e) {
         System.out.println (e);
         try {
            SurfaceMeshIntersectorTest.writeProblem (
               "csgop.obj", myMesh0, myMesh1, null, myCSGOperation);
         }
         catch (IOException ioe) {
            ioe.printStackTrace();
         }
      }
      myCSGMesh = csgMesh;
   }
   
   @Override
   public void prerender (RenderList list) {

      super.prerender (list);
      
      if (myZPerturb0 != null) {
         for (Vertex3d v : myMesh0.getVertices()) {
            if (isInterior (v)) {
               v.pnt.z = RandomGenerator.nextDouble (
                  myZPerturb0.getLowerBound(), myZPerturb0.getUpperBound());
            }
         }
         myMesh0.notifyVertexPositionsModified();
         myMesh0.updateFaceNormals();
      }
      
      if (myZPerturb1 != null) {
         for (Vertex3d v : myMesh1.getVertices()) {
            if (isInterior (v)) {
               v.pnt.z = RandomGenerator.nextDouble (
                  myZPerturb1.getLowerBound(), myZPerturb1.getUpperBound());
            }
         }
         myMesh1.notifyVertexPositionsModified();
         myMesh1.updateFaceNormals();
      }

      PolygonalMesh csgMesh = null;
      FunctionTimer timer = new FunctionTimer();
      if (myContoursOnly) {
         timer.start();
         System.out.println ("finding contours");
         myContours = myIntersector.findContours(myMesh0, myMesh1);
         System.out.println ("num contours=" + myContours.size());
         timer.stop();
         if (myContours.size() == 0) {
            return;
         }
      }
      else {
         timer.start();
         try {
            if (myCSGOperation == SurfaceMeshIntersector.CSG.INTERSECTION) {
               myContactInfo = myIntersector.findContoursAndRegions (
                  myMesh0, RegionType.INSIDE, myMesh1, RegionType.INSIDE);
               csgMesh = myIntersector.createCSGMesh (myContactInfo);
            }
            else if (myCSGOperation == SurfaceMeshIntersector.CSG.UNION) {
               myContactInfo = myIntersector.findContoursAndRegions (
                  myMesh0, RegionType.OUTSIDE, myMesh1, RegionType.OUTSIDE);
               csgMesh = myIntersector.createCSGMesh (myContactInfo);
            }
            else if (myCSGOperation == SurfaceMeshIntersector.CSG.DIFFERENCE01) {
               myContactInfo = myIntersector.findContoursAndRegions (
                  myMesh0, RegionType.OUTSIDE, myMesh1, RegionType.INSIDE);
               csgMesh = myIntersector.createCSGMesh (myContactInfo);
            }
            else if (myCSGOperation == SurfaceMeshIntersector.CSG.DIFFERENCE10) {
               myContactInfo = myIntersector.findContoursAndRegions (
                  myMesh0, RegionType.INSIDE, myMesh1, RegionType.OUTSIDE);
               csgMesh = myIntersector.createCSGMesh (myContactInfo);
            }
            else {
               myContactInfo = myIntersector.findContoursAndRegions (
                  myMesh0, RegionType.INSIDE, myMesh1, RegionType.INSIDE);
            }
         }
         catch (InternalErrorException e) {
            System.out.println (e);
            try {
               SurfaceMeshIntersectorTest.writeProblem (
                  "csgop.obj", myMesh0, myMesh1, null, myCSGOperation);
            }
            catch (IOException ioe) {
               ioe.printStackTrace();
            }
         }
         timer.stop();
         if (myContactInfo == null) {
            myContours = null;
            return;
         }
         myContours = myContactInfo.getContours();

         ArrayList<PenetratingPoint> points0 = 
            myContactInfo.getPenetratingPoints(0);
         // if (points0 != null) {
         //    System.out.println ("num verts0= " + points0.size());
         // }
         // else {
         //    System.out.println ("num verts0= null");
         // }
         
         ArrayList<PenetratingPoint> points1 = 
            myContactInfo.getPenetratingPoints(1);      
         // if (points1 != null) {
         //    System.out.println ("num verts1= " + points1.size());
         // }
         // else {
         //    System.out.println ("num verts1= null");
         // }

         ArrayList<PenetrationRegion> regions;

         if (false) {
            
            System.out.println ("Regions 0:");
            regions = myContactInfo.getRegions(0);
            for (int i=0; i<regions.size(); i++) {
               PenetrationRegion r = regions.get(i);
               int[] faceIdxs =
                  SurfaceMeshIntersectorTest.getFaceIndices (r.getFaces());
               ArraySort.sort (faceIdxs);
               int[] vtxIdxs =
                  SurfaceMeshIntersectorTest.getVertexIndices (r);
               ArraySort.sort (vtxIdxs);
               System.out.println (
                  " "+i+" area=" + r.getArea() + " faces=[ " +
                  ArraySupport.toString(faceIdxs) + " vtxs=[ " +
                  ArraySupport.toString(vtxIdxs) + "]");
            }
            System.out.println ("Regions 1:");
            regions = myContactInfo.getRegions(1);
            for (int i=0; i<regions.size(); i++) {
               PenetrationRegion r = regions.get(i);
               int[] faceIdxs =
                  SurfaceMeshIntersectorTest.getFaceIndices (r.getFaces());
               ArraySort.sort (faceIdxs);
               int[] vtxIdxs =
                  SurfaceMeshIntersectorTest.getVertexIndices (r);
               ArraySort.sort (vtxIdxs);
               System.out.println (
                  " "+i+" area=" + r.getArea() + " faces=[ " +
                  ArraySupport.toString(faceIdxs) + " vtxs=[ " +
                  ArraySupport.toString(vtxIdxs) + "]");
            }

            System.out.println ("contours:" );
            RigidTransform3d T = new RigidTransform3d();
            ArrayList<IntersectionContour> contours = myContactInfo.getContours();
            for (int k=0; k<contours.size(); k++) {
               IntersectionContour c = contours.get(k);
               c.printCornerPoints (
                  "contour "+k+" closed="+c.isClosed(), "%20.16f", T);
            }
         }
         

         if (false) {
            String pfx = "         ";
            System.out.print (pfx+"new int[] {");
            int k = 0;
            Collections.sort (points0, new CPPVertexComparator());
            for (PenetratingPoint cpp : points0) {
               System.out.print ((k++ > 0 ? ", " : " ") + cpp.vertex.getIndex());
            }
            System.out.println (" },");
            System.out.print (pfx+"new int[] {");
            k = 0;
            Collections.sort (points1, new CPPVertexComparator());
            for (PenetratingPoint cpp : points1) {
               System.out.print ((k++ > 0 ? ", " : " ") + cpp.vertex.getIndex());
            }
            System.out.println (" },");
         }

         if (myRenderCSGMesh && csgMesh != null) {
            csgMesh.prerender (myRenderProps);
            myCSGMesh = csgMesh;
         }
         else {
            myCSGMesh = null;
         }

      }
      //System.out.println ("time=" + timer.result(1));
   }
   
   @Override
   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);

      ArrayList<IntersectionContour> contours = myContours;
      if (contours != null) {
         renderer.setLineWidth (3);
         renderer.setColor (Color.RED);
         for (IntersectionContour c : contours) {
            if (c.isClosed()) {
               renderer.beginDraw (DrawMode.LINE_LOOP);
            }
            else {
               renderer.beginDraw (DrawMode.LINE_STRIP);
            }
            for (IntersectionPoint p : c) {
               renderer.addVertex ((float)p.x, (float)p.y, (float)p.z);
            }
            renderer.endDraw();
         }           
      }
      PolygonalMesh csgMesh = myCSGMesh;
      if (myRenderCSGMesh && csgMesh != null) {
         csgMesh.render (renderer, myRenderProps, flags);
      }
   }

   private boolean edgeOnMesh (HalfEdge edge, PolygonalMesh mesh) {
      return edge.getHead().getMesh() == mesh;
   }

   private Face getOppositeFace (HalfEdge he) {
      if (he.opposite != null) {
         return he.opposite.getFace();
      }
      else {
         return null;
      }
   }

   private Face findSegmentFace (
      IntersectionPoint p0, IntersectionPoint p1, PolygonalMesh mesh) {
      if (edgeOnMesh (p0.edge, mesh)) {
         if (edgeOnMesh (p1.edge, mesh)) {
            if (p0.edge.getFace() == p1.edge.getFace()) {
               return p0.edge.getFace();
            }
            else if (p0.edge.getFace() == getOppositeFace (p1.edge)) {
               return p0.edge.getFace();
            }
            else if (getOppositeFace (p0.edge) ==  p1.edge.getFace()) {
               return p1.edge.getFace();
            }
            else if (getOppositeFace (p0.edge) == getOppositeFace (p1.edge)) {
               return getOppositeFace (p0.edge);
            }
            else {
               System.out.println ("NO COMMON Face edge/edge");
            }
         }
         else {
            if (p1.face == p0.edge.getFace() || 
                p1.face == getOppositeFace (p0.edge)) {
               return p1.face;
            }
            else {
               System.out.println ("NO COMMON Face edge/face");
            }
         }
      }
      else {
         if (edgeOnMesh (p1.edge, mesh)) {
            if (p0.face == p1.edge.getFace() || 
                p0.face == getOppositeFace (p1.edge)) {
               return p0.face;
            }
            else {
               System.out.println ("NO COMMON Face face/edge");
            }
         }
         else {
            if (p0.face == p1.face) {
               return p0.face;
            }
            else {
               System.out.println ("NO COMMON Face face/face");               
            }
         }
      }
      return null;
   }

   private boolean faceIsPenetrating (Face face, HashSet<Vertex3d> vertices) {
      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he = he0;
      do {
         if (!vertices.contains(he.getHead())) {
            return false;
         }
         he = he.getNext();
      }
      while (he != he0);
      return true;
   }

   private Face[] findPenetratingFaces (
      ArrayList<PenetratingPoint> points) {

      HashSet<Vertex3d> vertices = new HashSet<Vertex3d>();
      for (PenetratingPoint p : points) {
         vertices.add (p.vertex);
      }
      HashSet<Face> faces = new HashSet<Face>();
      for (PenetratingPoint p : points) {
         Vertex3d vtx = p.vertex;
         Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
         while (it.hasNext()) {
            HalfEdge he = it.next();
            Face face = he.getFace();
            if (!faces.contains(face) && faceIsPenetrating (face, vertices)) {
               faces.add (face);
            }
         }
      }
      return faces.toArray (new Face[0]);
   }

   double computePenetratingFaceArea (ArrayList<PenetratingPoint> points) {

      double area = 0;
      HashSet<Vertex3d> vertices = new HashSet<Vertex3d>();
      for (PenetratingPoint p : points) {
         vertices.add (p.vertex);
      }
      HashSet<Face> faces = new HashSet<Face>();
      for (PenetratingPoint p : points) {
         Vertex3d vtx = p.vertex;
         Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
         while (it.hasNext()) {
            HalfEdge he = it.next();
            Face face = he.getFace();
            if (!faces.contains(face) && faceIsPenetrating (face, vertices)) {
               area += face.computeArea();
               System.out.println (" adding face "+face.getIndex()+" "+area);
               faces.add (face);
            }
         }
      }
      return area;
   }

   private class FacePoints {
      IntersectionPoint first;
      IntersectionPoint last;
   }

   /**
    * Return the edge of a face that contains an intersection point.
    */
   private HalfEdge getPointEdge (IntersectionPoint pa, Face face) {
      PolygonalMesh mesh = (PolygonalMesh)face.getMesh();
      if (edgeOnMesh (pa.edge, mesh)) {
         if (pa.edge.getFace() == face) {
            return pa.edge;
         }
         else if (getOppositeFace(pa.edge) == face) {
            return pa.edge.opposite;
         }
         else {
            throw new InternalErrorException (
               "Face edge not found for point " + pa);
         }
      }
      else {
         // convert pa to mesh local coordinates
         Point3d paLoc = new Point3d(pa);
         paLoc.inverseTransform (mesh.getMeshToWorld());
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         HalfEdge heMin = null;
         double dmin = Double.POSITIVE_INFINITY;
         do {
            double d = LineSegment.distance (
               he.getHead().pnt, he.getTail().pnt, paLoc);
            if (d < dmin) {
               heMin = he;
               dmin = d;
            }
            he = he.getNext();
         }
         while (he != he0);
         return heMin;         
      }
   }

   /**
    * Returns true if the countour is oriented so that it travels
    * clockwise with respect to the orientation of the specified mesh.
    */
   boolean contourIsClockwise (
      IntersectionContour contour,
      PolygonalMesh mesh1, PolygonalMesh mesh2) {

      int csize = contour.size();

      // find the largest contour segment
      Vector3d dir = new Vector3d();
      Vector3d xprod = new Vector3d();

      double dmax = 0;
      boolean clockwise = true;
      for (int i=0; i<csize; i++) {
         IntersectionPoint pa = contour.get((i)%csize);
         IntersectionPoint pb = contour.get((i+1)%csize);

         Face face1 = findSegmentFace (pa, pb, mesh1);         
         Face face2 = findSegmentFace (pa, pb, mesh2);         

         dir.sub (pb, pa);
         xprod.cross (face2.getWorldNormal(), dir);
         double dot = face1.getWorldNormal().dot(xprod);
         //System.out.println (" dot=" + dot);
         if (Math.abs(dot) > dmax) {
            dmax = Math.abs(dot);
            clockwise = (dot < 0);
         }
      }
      return clockwise;
   }

   private void getWorldPoint (Point3d pnt, Vertex3d vtx) {
      MeshBase mesh = vtx.getMesh();
      if (!mesh.meshToWorldIsIdentity()) {
         pnt.transform (mesh.getMeshToWorld(), vtx.pnt);
      }
      else {
         pnt.set (vtx.pnt);
      }
   }

   @Override
   public RenderProps createRenderProps () {
      RenderProps props = super.createRenderProps ();
      props.setFaceStyle (FaceStyle.FRONT);
      return props;
   }

   @Override
   public boolean isSelectable () {
      return true;
   }
}
