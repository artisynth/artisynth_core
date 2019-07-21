/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.File;
import java.awt.Color;

import maspack.render.*;
import maspack.render.color.ColorUtils;
import maspack.render.Renderer.PointStyle;
import maspack.render.GL.GLViewer;
import maspack.render.GL.GLViewerFrame;
import maspack.util.*;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;

/**
 * This is a test class for SignedDistanceGrid.
 * Many of the tests are visual, so if the user wants to see where the test is
 * failing, the test should be run without any other tests. The grid will then
 * be rendered a different colour where the test has failed.
 * 
 * 
 * @author Bruce Haines, bruce DOT a DOT haines AT gmail.com
 */
public class DistanceGridTester {
   
   private GLViewerFrame frame;
   private PolygonalMesh m;
   private DistanceGrid g;
   private GLViewer viewer;

   static boolean useGraphics = false;

   public DistanceGridTester() {
   }
   
   public DistanceGridTester (PolygonalMesh mesh, 
                                  DistanceGrid grid,
                                  boolean render) {
      m = mesh;
      g = grid;
      if (render) {
         frame = new GLViewerFrame ("GridViewer", 1000, 1000);
         viewer = frame.getViewer();
         RenderProps.setPointStyle (g, PointStyle.SPHERE);
         RenderProps.setLineWidth (g, 2);
         viewer.addRenderable (m);
         viewer.addRenderable (g);
         viewer.setBackgroundColor (0, 0, 0);
         frame.setVisible (true);
      }
   }
   
   public static void main (String[] args) {
      
      PolygonalMesh boxMesh = MeshFactory.createBox (4.0, 4.0, 4.0);
      Vector3i cellDivisions = new Vector3i (25, 25, 25);
      
      RenderProps meshRenderProps = new RenderProps();
      meshRenderProps.setFaceStyle (Renderer.FaceStyle.BACK);
      boxMesh.setRenderProps (meshRenderProps);
      
//      DistanceGrid boxGrid = 
//         new DistanceGrid (boxMesh, 0.1, cellDivisions);
//      SignedDistanceGridTest test1 = 
//         new SignedDistanceGridTest (boxMesh, boxGrid, true);
      
      //test1.bruteForcePhiTest();
      //test1.colourTest();
      //test1.normalCalcTest();
      //test1.visualInsideOutsideTest();
      //test1.normalCalcTest ();
      //boxMesh = MeshFactory.createTriangularSphere(2.0, 16);
      PolygonalMesh mesh = null;

      if (false) {
         try {
            mesh =
               new PolygonalMesh (
                  new File (
                     PathFinder.expand (
                        "${srcdir DistanceGrid}/sampleData/molar1.2.obj")));
            mesh.scale (3.0);
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
      if (false) {
         mesh = MeshFactory.createSphere (1.0, 12);
         mesh.scale (2.0, 1.5, 1.0);
      }
      if (true) {
         mesh = MeshFactory.createPointedCylinder (1.2, 1.2, 0.3, 4);
      }
      
      mesh.setRenderProps (meshRenderProps);
      DistanceGrid grid;

      if (false) {
         FunctionTimer timer = new FunctionTimer();
         int cnt = 20;
         timer.start();
         for (int i=0; i<cnt; i++) {
            grid = new DistanceGrid (
               mesh.getFaces(), 0.1, cellDivisions, /*signed=*/true);
         }
         timer.stop();
         System.out.println ("grid compute time=" + timer.result(cnt));
      }

      grid =
         new DistanceGrid (
            mesh.getFaces(), 0.1, new Vector3i(10,10,5), /*signed=*/true);
      DistanceGridTester test = 
         new DistanceGridTester (mesh, grid, useGraphics);
         
      test.bruteForcePhiTest();
      test.normalCalcTest();
      test.checkPhiIsSet ();
      //test.checkCellArray ();
      
      test.visualInsideOutsideTest();

      if (useGraphics) {
         test.viewer.rerender();
      }
      //checkCellArray ();
      //checkSignOfPhi ();
      //bruteForcePhiTest ();
      //checkPhiIsSet ();
      //interpolationTest ();
   }
   
   /**
    * This test offers a visual inspection for the sign of each vertex. It 
    * changes the colours of the grid vertices based on their sign. Inside 
    * vertices are blue, outside are red.
    */
   private void visualInsideOutsideTest() {
      double phi[] = g.getVertexDistances ();
      for (int i = 0; i < phi.length; i++) {
         if (phi[i] < 0) {
            g.setVertexColor (i, Color.BLUE);
         }
         else
            g.setVertexColor (i, Color.RED);
      }
      System.out.println("visualInsideOutsideTest complete.");
   }
  
   // /**
   //  * This test is a basic verification of SignedDistanceGrid.gridCellArray[].
   //  * We're making sure that it matches phi[]. This is a trivial but important
   //  * assurance because the visual inspection tests rely on gridCellArray to 
   //  * match phi. 
   //  */
   // public void checkCellArray () {
   //    // go through all of phi and compare with gridCellArray.
   //    double myPhi[] = g.getDistances ();
   //    for (int i = 0; i < myPhi.length; i++) {         
   //       if (myPhi[i] != this.g.getGridCell(i).getDistance()) {
   //          throw new TestException("cellArray distance does not match phi");
   //       }
   //    }
   //    System.out.println ("Cell Array matches Phi, Test passed.");
   // }
   
   /**
    * This test checks the sign of each point in a rectangular grid. We 
    * manually check that every point inside the mesh boundaries has a negative
    * distance, and every point outside has a positive distance.
    * Warning: this method only works for rectangular prisms.
    */
   private void checkSignOfPhi() {
      double myPhi[] = g.getVertexDistances ();
      Vector3d min = new Vector3d();
      Vector3d max = new Vector3d();
      m.getLocalBounds (min, max);
      for (int i = 0; i < myPhi.length; i++) {
//         int z = i / (gridSize[0] * gridSize[1]);
//         int y = (i - z * gridSize[0] * gridSize[1]) / gridSize[0];
//         int x = i % gridSize[0];
         // translate to mesh coordinates.
         Vector3d coords = new Vector3d();
         g.getLocalVertexCoords(coords, g.vertexToXyzIndices (new Vector3i(), i));
         // If our point lies inside all the boundaries
         if ((coords.x > min.x && coords.x < max.x) &&
             (coords.y > min.y && coords.y < max.y) &&
             (coords.z > min.z && coords.z < max.z)) {
            if (myPhi[i] > 0) {
               // myPhi should be < 0
               throw new TestException(
                  "Phi is positive when it should be negative");
            }
         }
         else if ((coords.x < min.x || coords.x > max.x) ||
                  (coords.y < min.y || coords.y > max.y) ||
                  (coords.z < min.z || coords.z > max.z)) {
            if (myPhi[i] < 0) {
               // myPhi should be > 0
               throw new TestException(
                  "Phi is negative when it should be positive");
            }
         }
      }
   }
   
   /**
    * This test verifies the normal calculations. For every point, we find the
    * nearest point to the triangle, subtract the points, and normalize the
    * resulting vector. There is usually quite a bit of error, especially with
    * finer resolution meshes. The error calculation is done per axis, and 
    * should perhaps be represented in a better way in the future.
    */
   private void normalCalcTest() {
      Vector3d normal = new Vector3d();
      double myPhi[] = g.getVertexDistances ();
      double epsilon = 1e-8;
      Point3d closestPoint = new Point3d();
      Point3d meshpnt = new Point3d();
      double errorCount = 0;
      Vector3d errorSum = new Vector3d();
      Vector3d errorAverage = new Vector3d();
      Vector3d errorMax = new Vector3d();
      Vector3d errorThis = new Vector3d();
      
      for (int i = 0; i < myPhi.length; i++) {
         g.getLocalVertexCoords (meshpnt, g.vertexToXyzIndices (new Vector3i(), i));
         double dist = 
            g.getLocalDistanceAndNormal (
            normal,
            meshpnt.x,
            meshpnt.y,
            meshpnt.z);
         Face face = (Face)g.getClosestFeature(i);
         face.nearestPoint (closestPoint, meshpnt);
         //closestPoint.sub (meshpnt);
         meshpnt.sub (closestPoint);
         meshpnt.absolute ();
         meshpnt.normalize ();
         normal.absolute ();
         //normal.scale (dist);
         double myErr = meshpnt.distance (normal);
         
         if (myErr > epsilon) {
            //g.setGridPointColor (i, Color.WHITE);
            errorThis.x = Math.abs(normal.x - meshpnt.x) / meshpnt.x * 100.0; 
            errorThis.y = Math.abs(normal.y - meshpnt.y) / meshpnt.y * 100.0; 
            errorThis.z = Math.abs(normal.z - meshpnt.z) / meshpnt.z * 100.0; 
            if (errorThis.x > errorMax.x) {
               errorMax.x = errorThis.x;
            }
            if (errorThis.y > errorMax.y) {
               errorMax.y = errorThis.y;
            }
            if (errorThis.z > errorMax.z) {
               errorMax.z = errorThis.z;
            }
            errorSum.add (errorThis);
            errorCount++;
         }
      }
      if (errorCount != 0) {
         errorAverage.x = errorSum.x / errorCount;
         errorAverage.y = errorSum.y / errorCount;
         errorAverage.z = errorSum.z / errorCount;
      }
      System.out.println ("Average error of normals: " + errorAverage);
      System.out.println ("Max error of normals: " + errorMax);
   }
   
   /**
    * This test takes every entry in phi, gets the mesh coordinates for that
    * entry, and calculates the distance to all of the faces in the mesh, 
    * comparing it with the one stored. A smaller distance is an indication of 
    * failure.
    * Rendering: 
    * Green - Pass
    * Blue - The calculated closestFace is correct, but distance calculated is 
    * wrong (face.nearestPoint is likely the culprit).
    * Red - The calculated closestFace is incorrect, and the actual closest face
    * is adjacent.
    * White - Fail, closestFace is not even adjacent to the actual closest face.
    */
   private void bruteForcePhiTest () {
      
      double myPhi[] = g.getVertexDistances ();
      
      Point3d closestPoint = new Point3d();
      Point3d myPoint = new Point3d ();
      int badVertexCount = 0;
      double maxError = 0;
      double avgError = 0;
      boolean vertexIsBad = false;
      
      for (int i = 0; i < myPhi.length; i++) {
         vertexIsBad = false;
         // Extract out the grid coordinates.
         Vector3i gidxs = new Vector3i();
         // Get the distance from currentPoint to the face.
         g.getLocalVertexCoords (myPoint, g.vertexToXyzIndices (gidxs, i));
         for (int j = 0; j < m.numFaces(); j++) {
            Face face = m.getFace(j);
            face.nearestPoint (closestPoint, myPoint);
            double distance = myPoint.distance (closestPoint);
            double distanceInPhi = myPhi[i];
            // Test to see if we've found a smaller distance
            if (Math.abs (distance) < Math.abs (distanceInPhi)){
               g.setVertexColor (i, Color.WHITE);
               if (!vertexIsBad) {
                  badVertexCount++;
                  vertexIsBad = true;
               }
               int calculatedFaceIndex = 
                  ((Face)g.getClosestFeature(i)).getIndex();
               // I'm trusting 'distance' over myPhi. This is because it is an
               // exhaustive search, and should be as accurate as the method
               // used to calculate the closest point, whereas myPhi is 
               // calculated using a sweep method, which will mismatch 
               // the closest face.
               double tempError = 
                  (Math.abs (distanceInPhi) - Math.abs (distance)) / 
                   Math.abs (distance) * 100.0f;
               avgError += tempError;
               if (tempError > maxError) {
                  maxError = tempError;
               }
               if (calculatedFaceIndex == j) {
                  g.setVertexColor (i, Color.BLUE);
               }
               else {
                  // We want to test if they at least share a vertex.
                  Face calculatedFace = m.getFace(calculatedFaceIndex);
                  HalfEdge he, he0;
                  Vector3d calculatedFaceVertices[] = new Vector3d[3];
                  he0 = calculatedFace.firstHalfEdge();
                  he = he0;
                  calculatedFaceVertices[0] = new Vector3d(he.head.pnt);
                  he = he.getNext();
                  calculatedFaceVertices[1] = new Vector3d(he.head.pnt);
                  he = he.getNext();
                  calculatedFaceVertices[2] = new Vector3d(he.head.pnt);
                  
                  Vector3d faceVertices[] = new Vector3d[3];
                  he0 = face.firstHalfEdge();
                  he = he0;
                  faceVertices[0] = new Vector3d(he.head.pnt);
                  he = he.getNext();
                  faceVertices[1] = new Vector3d(he.head.pnt);
                  he = he.getNext();
                  faceVertices[2] = new Vector3d(he.head.pnt);

                  boolean areFacesAdjacent = false;
                  
                  for (int p = 0; p < 2; p++) {
                     for (int q = 0; q < 2; q++) {
                        if (faceVertices[q].equals(calculatedFaceVertices[p])) {
                           areFacesAdjacent = true;
                           g.setVertexColor (i, Color.RED);
                        }
                     }
                  }
               }
            }
         }
      }
      avgError /= myPhi.length;
      System.out.println ("Number of incorrect vertices: " + badVertexCount);
      System.out.println ("Max error of phi is: " + maxError + "%");
      System.out.println ("Average error of phi is: " + avgError + "%");
      System.out.println ("Brute Force Distance Test Complete.");
   }
   
   /** 
    * This test checks that every value in phi is less than half the width of
    * the parallelpiped that is formed by the grid.
    * 
    */
   private void checkPhiIsSet () {
      double myPhi[] = g.getVertexDistances ();
      
      Vector3i resolution = g.getResolution();
      Vector3d cellSize = g.getCellWidths ();
      double maxDist = Math.sqrt (resolution.x * cellSize.x *
                                  resolution.x * cellSize.x +
                                  resolution.y * cellSize.y *
                                  resolution.y * cellSize.y +
                                  resolution.z * cellSize.z *
                                  resolution.z * cellSize.z);
      
      for (int i = 0; i < myPhi.length; i++) {
         if (myPhi[i] >= maxDist || myPhi[i] <= -maxDist) {
            
            throw new TestException (
               "phi at index " + i + " has not been set." );
         }
      }
      System.out.println ("Phi has been initialized. Test Passed.");
   }
   
   /** 
    * This test changes the colour of each vertex based on 
    * distance from the mesh surface. This is a visual test of the integrity
    * of the signed distance field.
    *
    **/
   private void colourTest () {
      
      double myPhi[] = g.getVertexDistances ();
      Vector3i resolution = g.getResolution();
      Vector3d cellSize = g.getCellWidths ();
      
      for (int i = 0; i < myPhi.length; i++) {
         // Extract out the grid coordinates.
         Vector3i gidxs = new Vector3i();
         g.vertexToXyzIndices (gidxs, i);
         double maxDist = Math.sqrt (
                          resolution.x * cellSize.x *
                          resolution.x * cellSize.x +
                          resolution.y * cellSize.y *
                          resolution.y * cellSize.y +
                          resolution.z * cellSize.z *
                          resolution.z * cellSize.z) / 3.0f;
         float h = (float)(Math.abs (g.getVertexDistance (gidxs)) / maxDist);
         float RGB[] = new float[3];
         float HSV[] = new float[3];
         HSV[0] = h;
         HSV[1] = 1.0f;
         HSV[2] = 1.0f;
         ColorUtils.HSVtoRGB (HSV, RGB);
         g.setVertexColor (i, new Color(RGB[0], RGB[1], RGB[2]));
      }
      System.out.println("Colour Test complete.");
   }
   
}
