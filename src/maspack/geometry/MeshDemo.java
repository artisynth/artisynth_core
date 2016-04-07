/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;
import maspack.render.RenderProps;
import maspack.render.GL.GLViewerFrame;

import java.awt.Color;

/**
 * Demonstration class which illustrates how to build a PolygonalMesh using its
 * <code>addVertex</code> and <code>addFace</code> methods.
 */
public class MeshDemo {
   public static void main (String[] args) {
      // create a simple box
      double wx = 9;
      double wy = 4;
      double wz = 1;
      PolygonalMesh myMesh = new PolygonalMesh();

      myMesh.addVertex (new Point3d (wx / 2, wy / 2, wz / 2));
      myMesh.addVertex (new Point3d (wx / 2, wy / 2, -wz / 2));
      myMesh.addVertex (new Point3d (-wx / 2, wy / 2, -wz / 2));
      myMesh.addVertex (new Point3d (-wx / 2, wy / 2, wz / 2));

      myMesh.addVertex (new Point3d (wx / 2, -wy / 2, wz / 2));
      myMesh.addVertex (new Point3d (wx / 2, -wy / 2, -wz / 2));
      myMesh.addVertex (new Point3d (-wx / 2, -wy / 2, -wz / 2));
      myMesh.addVertex (new Point3d (-wx / 2, -wy / 2, wz / 2));

      myMesh.addFace (new int[] { 0, 1, 2, 3 });
      myMesh.addFace (new int[] { 0, 3, 7, 4 });
      myMesh.addFace (new int[] { 1, 0, 4, 5 });
      myMesh.addFace (new int[] { 2, 1, 5, 6 });
      myMesh.addFace (new int[] { 3, 2, 6, 7 });
      myMesh.addFace (new int[] { 4, 7, 6, 5 });

      RenderProps props = myMesh.createRenderProps();
      props.setDrawEdges (true);
      props.setFaceColor (new Color (0.93f, 0.8f, 0.063f)); // gold
      myMesh.setRenderProps (props);

      GLViewerFrame frame = new GLViewerFrame ("MeshDemo", 400, 400);
      frame.getViewer().addRenderable (myMesh);
      frame.getViewer().autoFitPerspective ();

      frame.setVisible (true);
   }
}
