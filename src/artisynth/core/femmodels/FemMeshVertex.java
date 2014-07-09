/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;

public class FemMeshVertex extends Vertex3d {
   protected FemNode myPnt;

   public FemMeshVertex( FemNode node, Point3d pos) {
      super(pos);
      myPnt = node;
   }
   
   public FemMeshVertex (FemNode p) {
      super (p.getPosition());
      myPnt = p;
   }

   public FemNode getPoint() {
      return myPnt;
   }
   
   public FemMeshVertex clone() {
      FemMeshVertex vtx = null;
      
      vtx = (FemMeshVertex)super.clone();
      vtx.myPnt = myPnt;
      
      return vtx;
   }
   
}
