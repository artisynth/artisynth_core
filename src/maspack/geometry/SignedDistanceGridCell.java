/**
 * Copyright (c) 2014, by the Authors: Bruce Haines (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.LinkedList;

import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.render.IsSelectable;
import maspack.render.Renderer;
import maspack.render.RenderList;

// This is a class to make grid points renderable and selectable.


public class SignedDistanceGridCell implements IsSelectable {

   private Vector3i pidxs = new Vector3i();
   private double distance;
   private int myIndex;
   private SignedDistanceGrid myGrid;
   private boolean isSelected;
   protected float[] pointColour = new float[] { 0, 1, 0 };
   protected float[] selectedColour = new float[] { 0, 1, 0 };
   protected int myPointSize = 5;
   
   public SignedDistanceGridCell() {
   }
  
   public SignedDistanceGridCell (int idx, SignedDistanceGrid grid) {
      myGrid = grid;
      setIndex (idx);
   }
   
   public void setVertex (int x, int y, int z) {
      Vector3i res = myGrid.getResolution();
      pidxs.x = x;
      pidxs.y = y;
      pidxs.z = z;
      myIndex = x + y*(res.x+1) + z*(res.x+1)*(res.y+1);
   }
   
   public void setDistance (double d) {
      distance = d;
   } 
   
   public double getDistance () {
      return distance;
   }
   
   public void setIndex (int index) {
      myIndex = index;
      myGrid.vertexToXyzIndices (pidxs, index);
   }
   
   public void prerender (RenderList list) {
   }
   
   public void render (Renderer renderer, int flags) {
      
      Vector3d vertexCoords = new Vector3d();
      myGrid.getLocalVertexCoords (vertexCoords, pidxs);
      
      renderer.setPointSize (3);
      renderer.setColor (pointColour);
      renderer.drawPoint (vertexCoords.x, vertexCoords.y, vertexCoords.z);

      Vector3d normal = new Vector3d();
      normal = myGrid.getLocalVertexNormal (pidxs.x, pidxs.y, pidxs.z);
      
      renderer.setLineWidth (1);  // Render the normal.
      renderer.drawLine (
         vertexCoords.x,
         vertexCoords.y,
         vertexCoords.z,
         vertexCoords.x+normal.x*0.1,
         vertexCoords.y+normal.y*0.1,
         vertexCoords.z+normal.z*0.1);
   }
   
//   public void handleSelection (LinkedList<IsRenderable> pathlist, int[] namestack, int idx) {
//	   pathlist.add (this);
//      System.out.println (
//         "Index: " + myIndex +
//         " | Distance: " + distance +
//         " | ClosestFace: " + myGrid.getClosestFace (myIndex));
//   }
//   
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
   
   public void selectPoint (boolean selected) {
      isSelected = selected;
   }
    
   public Vector3i getPointIndices() {
      return pidxs;
   }
   
   public int getRenderHints() {
      return 0;
   }
   
   public void setColour (float r, float g, float b) {
      pointColour[0] = r;
      pointColour[1] = g;
      pointColour[2] = b;
   }
   
   public void updateBounds (Vector3d pmin, Vector3d pmax) {
   }
   
}
