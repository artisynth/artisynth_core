/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.*;

import maspack.matrix.*;
import maspack.util.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

import java.io.*;

public abstract class PointAttachment extends DynamicAttachment 
   implements CopyableComponent {

   protected Point myPoint;

   public Point getSlave() {
      return myPoint;
   }

   public Point getPoint() {
      return myPoint;
   }

   public int getSlaveSolveIndex() {
      if (myPoint != null) {
         return myPoint.getSolveIndex();
      }
      else {
         return -1;
      }
   }
   
   
   /**
    * Distributes an external force applied at a particular point 
    * to all masters
    * @param pnt point at which to apply the force (to be used to compute a wrench)
    * @param s force scale factor
    * @param f force vector to apply
    */
   public abstract void addScaledExternalForce(Point3d pnt, double s, Vector3d f);

   protected double getMassForPoint (Object mass) {
      double m = 0;
      if (mass == null) {
         if (myPoint instanceof Particle) {
            m = ((Particle)myPoint).getMass();
         }
      }
      else if (mass instanceof Number) {
         m = ((Number)mass).doubleValue();
      }
      else {
         throw new IllegalArgumentException (
            "mass type "+mass.getClass()+" inconsitent with particle");
      }
      return m;
   }

    public void writeItems (
       PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      
      super.writeItems (pw, fmt, ancestor);
      if (myPoint != null) {
         pw.print ("point=");
         pw.println (ComponentUtils.getWritePathName (
                        ancestor, myPoint));
      }
   }

  protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();      
      if (scanAndStoreReference (rtok, "point", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "point")) {
         myPoint = postscanReference (tokens, Point.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   @Override
   public PointAttachment copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      PointAttachment a = (PointAttachment)super.copy (flags, copyMap);

      // EDIT: for FrameMarker.copy() can eventually lead here with copyMap=null, Sanchez (Nov 30, 2011)
      if (copyMap != null) {
         if (copyMap.get (myPoint) == null) {
            System.out.println ("not here: " + myPoint);
         }
      }
      
      if (myPoint != null) {
         a.myPoint =
            (Point)ComponentUtils.maybeCopy (flags, copyMap, myPoint);
      }
      return a;
   }

   public abstract void computePosState (Vector3d pos);

   /**
    * {@inheritDoc}
    */
   public boolean isDuplicatable() {
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      return false;
   }

}
