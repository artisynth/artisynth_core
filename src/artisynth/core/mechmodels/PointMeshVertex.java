/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.geometry.*;

public class PointMeshVertex extends Vertex3d {
   protected Point myPnt;

   public PointMeshVertex (Point p) {
      super (p.myState.pos);
      myPnt = p;
   }

   public Point getPoint() {
      return myPnt;
   }
   
   @Override
   public Vertex3d clone() {
      PointMeshVertex vtx = (PointMeshVertex)super.clone();
      vtx.myPnt = myPnt;
      return vtx;
   }
   
}
