/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import maspack.matrix.*;
import maspack.properties.*;

public class GLClipPlane extends GLGridPlane {
   boolean myClippingEnabled = false;
   boolean mySlicingEnabled = false;
   double myOffset = 0.0;

   public static PropertyList myProps =
      new PropertyList (GLClipPlane.class, GLGridPlane.class);

   static {
      myProps.add (
         "slicing isSlicingEnabled setSlicingEnabled",
         "flag to show slice at plane", false);
      myProps.add ("offset", "distance along grid resolution", 0.0);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void setOffset (double off) {
      myOffset = off;
   }

   public double getOffset() {
      return myOffset;
   }

   public boolean setSlicingEnabled (boolean active) {
      
      if (mySlicingEnabled != active) {
         mySlicingEnabled = active;
         if (myViewer != null) {
            myViewer.repaint();
         }
      }
      return true;
   }

   public boolean isSlicingEnabled() {
      return mySlicingEnabled;
   }

   public boolean isClippingEnabled() {
      return myClippingEnabled;
   }

   public boolean setClippingEnabled (boolean active) {
      if (myClippingEnabled != active) {
         myClippingEnabled = active;
         if (myViewer != null) {
            myViewer.repaint();
         }
      }
      return myClippingEnabled;
   }

   /** 
    * Returns true if a specified point is clipped by this plane; i.e.,
    * if it lies in the half space defined by the positive z axis.
    *
    * @return true if a specified point is clipped
    */   
   public boolean isClipped (Point3d p) {
      double px = p.x - XGridToWorld.p.x;
      double py = p.y - XGridToWorld.p.y;
      double pz = p.z - XGridToWorld.p.z;
      RotationMatrix3d R = XGridToWorld.R;
      double z = R.m02*px + R.m12*py + R.m22*pz;
      return z > 0;
   }
   
   public int getClipPlaneValues(float[] vals, int offset, boolean flip) {
      RigidTransform3d X = XGridToWorld;
      if (flip) {
         vals[offset++] = (float)(X.R.m02);
         vals[offset++] = (float)(X.R.m12);
         vals[offset++] = (float)(X.R.m22);
         vals[offset++] = (float)(-X.R.m02 * X.p.x - X.R.m12 * X.p.y - X.R.m22 * X.p.z + myOffset);
      } else {
         vals[offset++] = (float)(-X.R.m02);
         vals[offset++] = (float)(-X.R.m12);
         vals[offset++] = (float)(-X.R.m22);
         vals[offset++] = (float)(X.R.m02 * X.p.x + X.R.m12 * X.p.y + X.R.m22 * X.p.z + myOffset);
      }
      return offset;
   }
}
