/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.*;

public class AttachedPoint {
   Point myPnt;
   Point3d myBodPos;

   public AttachedPoint (Point pnt, Point3d bodPos) {
      myPnt = pnt;
      myBodPos = new Point3d (bodPos);
   }
}
