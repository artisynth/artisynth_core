/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.awt.Color;
import java.util.ArrayList;

import maspack.geometry.BVFeatureQuery;
import maspack.geometry.MeshFactory;
import maspack.geometry.NagataInterpolator;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.render.HasRenderProps;
import maspack.render.IsRenderable;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.Shading;
import maspack.render.GL.GLViewer;

public class NagataDistanceTest implements IsRenderable, HasRenderProps {

   RenderProps myRenderProps = createRenderProps();
   ArrayList<Point3d> myPoints = new ArrayList<Point3d>();
   ArrayList<Point3d> myFacePoints = new ArrayList<Point3d>();
   ArrayList<Point3d> myNearPoints = new ArrayList<Point3d>();
   ArrayList<Double> myDistances = new ArrayList<Double>();

   Vector2d myCurveDir = new Vector2d (0.25, 0.75);
   Vector2d myCurvePos = new Vector2d (0.75, 0.25);
   double[] myCurveBounds = new double[2];

   PolygonalMesh myBaseMesh;
   PolygonalMesh myFineMesh;

   NagataInterpolator myInterp = new NagataInterpolator();

   public void computeNearestPoint (
      Point3d near, Vector3d normal, Point3d pos, boolean debug) {

      // //TriangleIntersector ti = new TriangleIntersector();
      BVFeatureQuery query = new BVFeatureQuery();
      // //OBBTree obbTree = myBaseMesh.getObbtree();

      // Vector2d svec = new Vector2d();  
      // query.nearestFaceToPoint (near, svec, myBaseMesh, pos);
      // //obbTree.nearestFace (pos, normal, near, svec, ti);

      // svec.x = svec.x + svec.y; // convert to eta, zeta
      // svec.y = svec.y;

      // myInterp.nearestPointOnFace (
      //    near, normal, face, myBaseMesh, svec, pos, 1e-8);
      
      myInterp.debug = debug;
      myInterp.nearestPointOnMesh (
        near, normal, myBaseMesh, pos, 1e-8, query);
      
      // //myInterp.distanceToCurve (near, myCurvePos, myCurveDir, pos, 1e-8);
         
      // myInterp.nearestPoint (near, normal, pos, eta, zeta, 1e-8);
   }


   public void createTriangleMesh() {
      myBaseMesh = new PolygonalMesh();
      myBaseMesh.addVertex (1, 0, 0);
      myBaseMesh.addVertex (-0.5, 0.5, 0.5);
      myBaseMesh.addVertex (-0.5, -0.5, 0.5);
      myBaseMesh.addFace (new int[] { 0, 1, 2 });


      ArrayList<Vector3d> normals = new ArrayList<Vector3d>();      

      Vector3d n0 = new Vector3d(0.5, 0, 1);
      Vector3d n1 = new Vector3d(-0.2,  0.4, 1);
      Vector3d n2 = new Vector3d(-0.2, -0.4, 1);

      n0 =
         new Vector3d( 0.994821588150, 0.0, -0.10163664570324002);
      n1 =
         new Vector3d( -0.39964089136, 0.759875873293, 0.512714165140);
      n2 =
         new Vector3d(-0.39964089136452324,-0.7598758732934454,0.51271416514066);

      normals.add (n0);
      normals.add (n1);
      normals.add (n2);

      myBaseMesh.setNormals (normals, new int[] {0, 1, 2});
   }

   public void createTetMesh() {
      myBaseMesh = new PolygonalMesh();
      myBaseMesh.addVertex (1, 0, 0);
      myBaseMesh.addVertex (-0.5, 0.5, 0.5);
      myBaseMesh.addVertex (-0.5, -0.5, 0.5);
      myBaseMesh.addVertex (-0.5, 0, -0.5);

      myBaseMesh.addFace (new int[] { 0, 1, 2 });
      myBaseMesh.addFace (new int[] { 0, 2, 3 });
      myBaseMesh.addFace (new int[] { 0, 3, 1 });
      myBaseMesh.addFace (new int[] { 3, 2, 1 });

      ArrayList<Vector3d> normals = myBaseMesh.getNormals();

      System.out.println ("normals 0: "+normals.get(0));
      System.out.println ("normals 1: "+normals.get(1));
      System.out.println ("normals 2: "+normals.get(2));
      
   }

   public NagataDistanceTest () {

      //createTriangleMesh();
      createTetMesh();

      myFineMesh = MeshFactory.createRefinedMesh (myBaseMesh, 10);

      double xmax =  1.5;
      double xmin = -1.0;
      double ymax =  1.0;
      double ymin = -1.0;
      int npnts = 11;

      double[] zvals = new double[] { 0.8 };

      //OBBTree obbTree = myBaseMesh.getObbtree();
      Point3d nearest = new Point3d();
      Vector3d normal = new Vector3d();
      Vector2d coords = new Vector2d();
      //TriangleIntersector ti = new TriangleIntersector();
      BVFeatureQuery query = new BVFeatureQuery();

      // Point3d p = new Point3d (.25, 0.0, 0.8);
      // myPoints.add (p);

      // myFacePoints.add (new Point3d(nearest));
      // computeNearestPoint (nearest, normal, p);            
      // myNearPoints.add (new Point3d(nearest));

      Vector3d del = new Vector3d();
      for (int i=0; i<npnts; i++) {
         for (int j=0; j<npnts; j++) {
            for (int k=0; k<zvals.length; k++) {
               double x = xmin + (i/(double)(npnts-1))*(xmax-xmin);
               double y = ymin + (j/(double)(npnts-1))*(ymax-ymin);
               double z = zvals[k];
               Point3d p = new Point3d (x, y, z);
               myPoints.add (p);

               // myInterp.nearestPointOnMesh (
               //    nearest, normal, myBaseMesh, p, 1e-8, ti);

               //query.nearestFaceToPoint (nearest, coords, myBaseMesh, p);
               //obbTree.nearestFace (p, normal, nearest, coords, ti);
               computeNearestPoint (nearest, normal, p, false);            
               myFacePoints.add (new Point3d(nearest));

               del.sub (p, nearest);
               double d = del.dot(normal);
               if (d < 0) {
                  if (i == 4 && j == 3) {
                     System.out.println ("fail at " + i + "," + j);
                  }
               }
               myDistances.add (d);
               myNearPoints.add (new Point3d(nearest));
            }
         }
      }
      RenderProps.setFaceStyle (myFineMesh, Renderer.FaceStyle.FRONT_AND_BACK);

      myInterp.setBoundsForCurve (myCurveBounds, myCurvePos, myCurveDir);
   }

   public void setRenderProps (RenderProps props) {
      myRenderProps = props.clone();
   }

   public RenderProps getRenderProps () {
      return myRenderProps;
   }

   public RenderProps createRenderProps() {
      RenderProps props = new RenderProps();
      props.setPointColor (Color.BLUE);
      props.setLineColor (Color.GREEN);
      props.setPointSize (2);
      return props;
   }

   public void prerender (RenderList list) {
   }

   public void render (Renderer renderer, int flags) {

      float[] coords0 = new float[3];
      float[] coords1 = new float[3];

      for (int i=0; i<myPoints.size(); i++) {
         myPoints.get(i).get(coords0);
         myNearPoints.get(i).get(coords1);
         renderer.drawPoint (myRenderProps, coords0, false);
         if (myDistances.get(i) < 0) {
            myRenderProps.setLineColor (Color.RED);
            renderer.drawLine (myRenderProps, coords0, coords1, false);
            myRenderProps.setLineColor (Color.GREEN);
         }
         else {
            renderer.drawLine (myRenderProps, coords0, coords1, false);
         }
      }

      // draw the curve
      Point3d pos = new Point3d();
      Point3d pos0 = new Point3d();
      int nsegs = 20;
      double xi0 = myCurveBounds[0];
      double xi1 = myCurveBounds[1];
      myInterp.interpolateVertex (pos0, myCurvePos.x, myCurvePos.y);
      myInterp.interpolateCurve (pos, xi0, pos0, myCurvePos, myCurveDir);
      renderer.setShading (Shading.NONE);
      renderer.beginDraw (DrawMode.LINE_STRIP);
      renderer.setColor (1f, 1f, 0f);
      renderer.addVertex (pos);
      for (int i=0; i<nsegs; i++) {
         double xi = xi0 + (i+1)*(xi1-xi0)/(double)nsegs;
         myInterp.interpolateCurve (pos, xi, pos0, myCurvePos, myCurveDir);
         renderer.addVertex (pos);
      }
      renderer.endDraw();
      renderer.setShading (Shading.FLAT);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      for (int i=0; i<myPoints.size(); i++) {
         myPoints.get(i).updateBounds (pmin, pmax);
      }
   }

   public int getRenderHints() {
      return 0;
   }

   public static void main (String[] args) {

      NagataDistanceTest tester = new NagataDistanceTest();
      ArrayList<PolygonalMesh> meshes = new ArrayList<PolygonalMesh>();

      //meshes.add (tester.myBaseMesh);
      meshes.add (tester.myFineMesh);
      MeshViewer frame = new MeshViewer (meshes, 640, 480);
      frame.addRenderable (tester);
      GLViewer viewer = frame.getViewer();
      viewer.autoFitOrtho ();
      frame.setVisible (true);

   }
}
