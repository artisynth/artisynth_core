/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.io.File;
import java.util.LinkedList;

import maspack.geometry.Face;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.HalfEdge;
import maspack.geometry.SignedDistanceGrid;
import maspack.render.*;
import maspack.render.Renderer.DrawMode;
import maspack.render.color.ColorUtils;
import maspack.render.GL.GLViewer;
import maspack.util.*;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;

/**
 * This is a test class for SignedDistanceGrid.
 * Many of the tests are visual, so if the user wants to see where the test is
 * failing, the test should be run without any other tests. The grid will then
 * be rendered a different colour where the test has failed.
 * 
 * 
 * @author Bruce Haines, bruce DOT a DOT haines AT gmail.com
 */
public class SignedDistanceGridTest {
   
   private SignedDistanceGridViewer frame;
   private PolygonalMesh m;
   private SignedDistanceGrid g;
   private GLViewer viewer;

   public SignedDistanceGridTest() {
   }
   
   public SignedDistanceGridTest (PolygonalMesh mesh, 
                                  SignedDistanceGrid grid,
                                  boolean render) {
      m = mesh;
      g = grid;
      if (render) {
         frame = new SignedDistanceGridViewer (1000, 1000);
         viewer = frame.getViewer();
         viewer.addRenderable (m);
         viewer.addRenderable (g);
         for (int i = 0; i < g.getPhi().length; i++) {
            frame.addCell (g.getGridCell (i));
         }
         viewer.setBackgroundColor (0, 0, 0);
         frame.setVisible (true);
      }
   }
   
   public static void main (String[] args) {
      
      PolygonalMesh boxMesh = MeshFactory.createBox (4.0, 4.0, 4.0);
      Vector3d cellDivisions = new Vector3d (25.0, 25.0, 25.0);
      
      RenderProps meshRenderProps = new RenderProps();
      meshRenderProps.setFaceStyle (Renderer.FaceStyle.BACK);
      boxMesh.setRenderProps (meshRenderProps);
      
//      SignedDistanceGrid boxGrid = 
//         new SignedDistanceGrid (boxMesh, 0.1, cellDivisions);
//      SignedDistanceGridTest test1 = 
//         new SignedDistanceGridTest (boxMesh, boxGrid, true);
      
      //test1.bruteForcePhiTest();
      //test1.colourTest();
      //test1.normalCalcTest();
      //test1.visualInsideOutsideTest();
      //test1.normalCalcTest ();
      //boxMesh = MeshFactory.createTriangularSphere(2.0, 16);
      try {
         PolygonalMesh molarMesh =
            new PolygonalMesh (
               new File (
                  PathFinder.expand (
                     "${srcdir SignedDistanceGrid}/sampleData/molar1.2.obj")));
         molarMesh.scale (3.0);
         molarMesh.setRenderProps (meshRenderProps);
         SignedDistanceGrid molarGrid = 
            new SignedDistanceGrid (molarMesh, 0.1, cellDivisions);
         SignedDistanceGridTest molarTest = 
            new SignedDistanceGridTest (molarMesh, molarGrid, true);
         
         molarTest.bruteForcePhiTest();
         //molarTest.visualInsideOutsideTest();
         molarTest.normalCalcTest();
         molarTest.checkPhiIsSet ();
         molarTest.checkCellArray ();
      }
      catch (Exception e) {
         e.printStackTrace();
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
      double myPhi[] = g.getPhi ();
      for (int i = 0; i < myPhi.length; i++) {
         if (g.gridCellArray[i].getDistance () < 0) {
            g.gridCellArray[i].setColour (0, 0, 1);
         }
         else
            g.gridCellArray[i].setColour (1, 0, 0);
      }
      System.out.println("visualInsideOutsideTest complete.");
   }
  
   /**
    * This test is a basic verification of SignedDistanceGrid.gridCellArray[].
    * We're making sure that it matches phi[]. This is a trivial but important
    * assurance because the visual inspection tests rely on gridCellArray to 
    * match phi. 
    */
   public void checkCellArray () {
      // go through all of phi and compare with gridCellArray.
      double myPhi[] = g.getPhi ();
      for (int i = 0; i < myPhi.length; i++) {         
         if (myPhi[i] != this.g.getGridCell(i).getDistance()) {
            throw new TestException("cellArray distance does not match phi");
         }
      }
      System.out.println ("Cell Array matches Phi, Test passed.");
   }
   
   /**
    * This test checks the sign of each point in a rectangular grid. We 
    * manually check that every point inside the mesh boundaries has a negative
    * distance, and every point outside has a positive distance.
    * Warning: this method only works for rectangular prisms.
    */
   private void checkSignOfPhi() {
      double myPhi[] = g.getPhi ();
      int gridSize[] = g.getGridSize ();
      Vector3d min = g.getMeshMin();
      Vector3d max = g.getMeshMax();
      for (int i = 0; i < myPhi.length; i++) {
         int z = i / (gridSize[0] * gridSize[1]);
         int y = (i - z * gridSize[0] * gridSize[1]) / gridSize[0];
         int x = i % gridSize[0];
         // translate to mesh coordinates.
         double currentPoint[] = new double[3];
         currentPoint = g.getMeshCoordinatesFromGrid(x, y, z);
         // If our point lies inside all the boundaries
         if ((currentPoint[0] > min.x && currentPoint[0] < max.x) &&
             (currentPoint[1] > min.y && currentPoint[1] < max.y) &&
             (currentPoint[2] > min.z && currentPoint[2] < max.z)) {
            if (myPhi[i] > 0) {
               // myPhi should be < 0
               throw new TestException(
                  "Phi is positive when it should be negative");
            }
         }
         else if ((currentPoint[0] < min.x || currentPoint[0] > max.x) ||
                  (currentPoint[1] < min.y || currentPoint[1] > max.y) ||
                  (currentPoint[2] < min.z || currentPoint[2] > max.z)) {
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
      int myClosestFace[] = g.getClosestFace ();
      Face myFaces[] = g.getFaces ();
      double myPhi[] = g.getPhi ();
      int gridSize[] = g.getGridSize ();
      double epsilon = 1e-8;
      Point3d closestPoint = new Point3d();
      Point3d meshpnt = new Point3d();
      double errorCount = 0;
      Vector3d errorSum = new Vector3d();
      Vector3d errorAverage = new Vector3d();
      Vector3d errorMax = new Vector3d();
      Vector3d errorThis = new Vector3d();
      
      for (int i = 0; i < myPhi.length; i++) {
         int z = i / (gridSize[0] * gridSize[1]);
         int y = (i - z * gridSize[0] * gridSize[1]) / gridSize[0];
         int x = i % gridSize[0];
         g.getMeshCoordinatesFromGrid (meshpnt, x, y, z);
         double dist = 
            g.getDistanceAndNormal (
            normal,
            meshpnt.x,
            meshpnt.y,
            meshpnt.z);
         Face face = myFaces [myClosestFace [i]];
         face.nearestPoint (closestPoint, meshpnt);
         //closestPoint.sub (meshpnt);
         meshpnt.sub (closestPoint);
         meshpnt.absolute ();
         meshpnt.normalize ();
         normal.absolute ();
         //normal.scale (dist);
         double myErr = meshpnt.distance (normal);
         
         if (myErr > epsilon) {
            //g.getGridCell (i).setColour (1, 1, 1); // paint it white.
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
      
      Face myFaces[] = g.getFaces ();
      int closestFace[] = g.getClosestFace();
      double myPhi[] = g.getPhi ();
      int gridSize[] = g.getGridSize ();
      
      Point3d closestPoint = new Point3d();
      Point3d myPoint = new Point3d ();
      int badVertexCount = 0;
      double maxError = 0;
      double avgError = 0;
      boolean vertexIsBad = false;
      
      for (int i = 0; i < myPhi.length; i++) {
         vertexIsBad = false;
         // Extract out the grid coordinates.
         int z = i / (gridSize[0] * gridSize[1]);
         int y = (i - z * gridSize[0] * gridSize[1]) / gridSize[0];
         int x = i % gridSize[0];
         // Get the distance from currentPoint to the face.
         g.getMeshCoordinatesFromGrid (myPoint, x, y, z);
         long sum = 0;
         long time = 0;
         double average = 0;
         double epsilon = 1e-3;
         for (int j = 0; j < myFaces.length; j++) {
            Face face = myFaces [j];
            face.nearestPoint (closestPoint, myPoint);
            double distance = myPoint.distance (closestPoint);
            double distanceInPhi = myPhi[i];
            // Test to see if we've found a smaller distance
            if (Math.abs (distance) < Math.abs (distanceInPhi)){
               g.getGridCell(i).setColour (1, 1, 1);
               if (!vertexIsBad) {
                  badVertexCount++;
                  vertexIsBad = true;
               }
               int calculatedFaceIndex = g.getClosestFace (i);
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
                  g.gridCellArray[i].setColour (0, 0, 1);
               }
               else {
                  // We want to test if they at least share a vertex.
                  Face calculatedFace = myFaces [calculatedFaceIndex];
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
                           g.gridCellArray[i].setColour (1, 0, 0);
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
      double myPhi[] = g.getPhi ();
      
      int gridSize[] = g.getGridSize ();
      Vector3d cellSize = g.getGridCellSize ();
      double maxDist = Math.sqrt ((double)(gridSize[0] - 1 ) * cellSize.x *
                                  (double)(gridSize[0] - 1 ) * cellSize.x +
                                  (double)(gridSize[1] - 1 ) * cellSize.y *
                                  (double)(gridSize[1] - 1 ) * cellSize.y +
                                  (double)(gridSize[2] - 1 ) * cellSize.z *
                                  (double)(gridSize[2] - 1 ) * cellSize.z);
      
      for (int i = 0; i < myPhi.length; i++) {
         if (myPhi[i] >= maxDist || myPhi[i] <= -maxDist) {
            
            throw new TestException (
               "phi at index " + i + " has not been set." );
         }
      }
      System.out.println ("Phi has been initialized. Test Passed.");
   }
   
   /** 
    * This class was created so that we can render grid points onto the grid,
    * without messing up the organized data structure of the SignedDistanceGrid.
    * 
    */
   class testCell implements IsSelectable {
      public Vector3d myVertex;   // In mesh coordinates
      public Vector3d normal;
      public double d;
      protected float[] pointColour = new float[] { 0.0f, 1.0f, 1.0f };
      private boolean isSelected;
      
      public testCell() {
         normal = new Vector3d();
         //pointColour[0] = 0;
         //pointColour[1] = 1;
         //pointColour[2] = 0;
      }
      
      // Meant to take points in mesh coordinates
      public testCell (SignedDistanceGrid myGrid, double x, double y, double z) {
         this();
         myVertex = new Vector3d (x, y, z);
         setDistance (myGrid.getDistanceAndNormal (normal, myVertex));
      }
      
      // Meant to take points in mesh coordinates
      public testCell (SignedDistanceGrid myGrid, Vector3d vertex) {
         this();
         myVertex = new Vector3d (vertex);
         setDistance (myGrid.getDistanceAndNormal (normal, myVertex));
      }
      
      public void setDistance (double distance) {
         if (distance < 0.0) {
            // Inside dots are blue
            pointColour[0] = 0.0f;
            pointColour[1] = 0.0f;
            pointColour[2] = 1.0f;
         }
         else {
         // Inside dots are red
            pointColour[0] = 1.0f;
            pointColour[1] = 0.0f;
            pointColour[2] = 0.0f;
         }
         d = distance;
      }       
      
      public void prerender (RenderList list) {
         
      }
      public void render (Renderer renderer, int flags) {
         
         // gl.glEnable (GL2.GL_POINT_SMOOTH);
         renderer.setPointSize (3);
         renderer.setColor (pointColour[0], pointColour[1], pointColour[2]);
         
         renderer.drawPoint (myVertex);
         Vector3d offset = new Vector3d();
         offset.scaledAdd (0.1, normal, myVertex);
         // Get the normal to the surface and render it.
         //Vector3d normal = new Vector3d();
         renderer.setLineWidth (1);
         renderer.drawLine (myVertex, offset);
      }
      
      public void getSelection (LinkedList<Object> list, int qid) {
      }
      
      public boolean isSelectable() {
         return true;
      }
      public int numSelectionQueriesNeeded() {
         return -1;
      }

      public boolean isSelected () {
         return isSelected;
      }
      public int getRenderHints() {
         return 0;
      }
      public void setColour (float r, float g, float b) {
         pointColour[0] = r;
         pointColour[1] = g;
         pointColour[2] = b;
      }
      public void updateBounds (Point3d pmin, Point3d pmax) {
         
      }
   }
   
   /**
    * This test creates a series of extra cells in the grid. They are created
    * between other grid cells, and their distances/normals have been 
    * interpolated. 
    * Warning: This should only be run with rectangular meshes.
    * Two basic tests verify interpolation for the distance and normal in each
    * direction.
    * Selecting one of these cells will display the interpolated
    * distance, which can then be compared with hand calculations.
    * 
    */
   private void interpolationTest () {      
      //Face myFaces[] = g.getFaces ();
      //int closestFace[] = g.getClosestFace();
      //double myPhi[] = g.getPhi ();
      int gridSize[] = g.getGridSize ();
      //Vector3d min = g.getMin ();
      Vector3d cellSize = g.getGridCellSize ();
      
      testCell a1 = new testCell (g, -2.0,  0.0,  0.0);
      testCell b1 = new testCell (g,  0.0, -2.0,  0.0);
      testCell c1 = new testCell (g,  0.0,  0.0, -2.0);
      
      testCell a2 = new testCell (g, -1.0, 0.0, 0.0);
      testCell b2 = new testCell (g, 0.0, -1.0, 0.0);
      testCell c2 = new testCell (g, 0.0, 0.0, -1.0);
      
      testCell d1 = new testCell (g, 0.0, 0.0, 0.0);
      testCell d2 = new testCell (g, 0.05, 0.05, 0.0);
      testCell d3 = new testCell (g, 0.1, 0.1, 0.0);
      testCell d4 = new testCell (g, 0.15, 0.15, 0.0);
      testCell d5 = new testCell (g, 0.2, 0.2, 0.0);
      testCell d6 = new testCell (g, 0.25, 0.25, 0.0);
      testCell d7 = new testCell (g, 0.3, 0.3, 0.0);
      testCell d8 = new testCell (g, 0.35, 0.35, 0.0);
      testCell d9 = new testCell (g, 0.4, 0.4, 0.0);
      
      viewer.addRenderable (a1);
      viewer.addRenderable (b1);
      viewer.addRenderable (c1);
      viewer.addRenderable (d1);
      
      viewer.addRenderable (a2);
      viewer.addRenderable (b2);
      viewer.addRenderable (c2);
      viewer.addRenderable (d2);
      
      viewer.addRenderable (d3);
      viewer.addRenderable (d4);
      viewer.addRenderable (d5);
      viewer.addRenderable (d6);
      viewer.addRenderable (d7);
      viewer.addRenderable (d8);
      viewer.addRenderable (d9);
      
      double epsilon = 0.000001;
      double d2d1 = d2.d - d1.d;
      double d3d2 = d3.d - d2.d;
      double d4d3 = d4.d - d3.d;
      double d5d4 = d5.d - d4.d;
      double d6d5 = d6.d - d5.d;
      double d7d6 = d7.d - d6.d;
      double d8d7 = d8.d - d7.d;
      double d9d8 = d9.d - d8.d;
      
      // Verify interpolated distances.
      if (Math.abs(a2.d - (a1.d - 1.0)) > epsilon) {
         //
         System.out.println ("Interpolation has failed along the x axis.");
      }
      if (Math.abs(b2.d - (b1.d - 1.0)) > epsilon) {
         //
         System.out.println ("Interpolation has failed along the y axis.");
      }
      if (Math.abs(c2.d - (c1.d - 1.0)) > epsilon) {
         //
         System.out.println ("Interpolation has failed along the z axis.");
      }
      /*if (Math.abs(d2.d - (d1.d - 1.732)) > epsilon) {
         //
         System.out.println ("Interpolation has failed.");
      }*/
      
      // Verify interpolated normals.
      Vector3d zeros = new Vector3d (0, 0, 0);
      a1.normal.sub (a2.normal);
      if (a1.normal.distance (zeros) > epsilon) {
         //
         System.out.println (
            "Interpolation of the normal has failed along the x axis");
      }
      b1.normal.sub (b2.normal);
      if (b1.normal.distance (zeros) > epsilon) {
         //
         System.out.println (
            "Interpolation of the normal has failed along the y axis");
      }
      c1.normal.sub (c2.normal);
      if (c1.normal.distance (zeros) > epsilon) {
         //
         System.out.println (
            "Interpolation of the normal has failed along the z axis");
      }
      
      // Draw a half circle in the grid.
      testCell circle[] = new testCell[100];
      double radius = 0.895;
      for (int i = 0; i < circle.length; i++) {
         double xval = (double)i / ((double)circle.length / 2.0) * radius - radius;
         double yval = Math.sqrt (radius*radius - xval*xval);
         circle[i] = new testCell (g, xval, yval, 0.0);
         viewer.addRenderable (circle[i]);
         
         double maxDist = Math.sqrt ((gridSize[0] - 1 ) * cellSize.x *
            (double)(gridSize[0] - 1 ) * cellSize.x +
            (double)(gridSize[1] - 1 ) * cellSize.y *
            (double)(gridSize[1] - 1 ) * cellSize.y +
            (double)(gridSize[2] - 1 ) * cellSize.z *
            (double)(gridSize[2] - 1 ) * cellSize.z) / 3.0f;
         
         float h = (float)(Math.abs (circle[i].d) / maxDist);
         float RGB[] = new float[3];
         float HSV[] = new float[3];
         HSV[0] = h;
         HSV[1] = 1.0f;
         HSV[2] = 1.0f;
         ColorUtils.HSVtoRGB (HSV, RGB);
         circle[i].setColour (RGB[0], RGB[1], RGB[2]);
         
      }
      System.out.println ("");
   }
      
   /** 
    * This test changes the colour of each vertex based on 
    * distance from the mesh surface. This is a visual test of the integrity
    * of the signed distance field.
    *
    **/
   private void colourTest () {
      
      double myPhi[] = g.getPhi ();
      int gridSize[] = g.getGridSize ();
      Vector3d cellSize = g.getGridCellSize ();
      
      for (int i = 0; i < myPhi.length; i++) {
         // Extract out the grid coordinates.
         int z = i / (gridSize[0] * gridSize[1]);
         int y = (i - z * gridSize[0] * gridSize[1]) / gridSize[0];
         int x = i % gridSize[0];
         double maxDist = Math.sqrt ((gridSize[0] - 1 ) * cellSize.x *
                          (double)(gridSize[0] - 1 ) * cellSize.x +
                          (double)(gridSize[1] - 1 ) * cellSize.y *
                          (double)(gridSize[1] - 1 ) * cellSize.y +
                          (double)(gridSize[2] - 1 ) * cellSize.z *
                          (double)(gridSize[2] - 1 ) * cellSize.z) / 3.0f;
         float h = (float)(Math.abs (g.getPhiAtPoint (x, y, z)) / maxDist);
         float RGB[] = new float[3];
         float HSV[] = new float[3];
         HSV[0] = h;
         HSV[1] = 1.0f;
         HSV[2] = 1.0f;
         ColorUtils.HSVtoRGB (HSV, RGB);
         g.gridCellArray[i].setColour (RGB[0], RGB[1], RGB[2]);
      }
      System.out.println("Colour Test complete.");
   }
   
}
