/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import maspack.matrix.*;
import maspack.properties.*;

public class GLClipPlane extends GLGridPlane {
   int myFrontGlClipPlane = -1;
   int myBackGlClipPlane = -1;
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

   public int getFrontGlClipPlane() {
      return myFrontGlClipPlane;
   }

   public int getBackGlClipPlane() {
      return myBackGlClipPlane;
   }

   public void setOffset (double off) {
      myOffset = off;
   }

   public double getOffset() {
      return myOffset;
   }

   public boolean setSlicingEnabled (boolean active) {
      if (mySlicingEnabled != active) {
         if (active) {
            if (myViewer == null) {
               mySlicingEnabled = false;
               return false;
            }
            if (myClippingEnabled) {
               myBackGlClipPlane = myViewer.allocGlClipPlane();
               if (myBackGlClipPlane == -1) {
                  mySlicingEnabled = false;
                  return false;
               }
            }
         }
         else {
            if (myBackGlClipPlane != -1) {
               myViewer.freeGlClipPlane (myBackGlClipPlane);
               myBackGlClipPlane = -1;
            }
         }
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
         if (active) {
            if (myViewer == null) {
               myClippingEnabled = false;
               return false;
            }
            myFrontGlClipPlane = myViewer.allocGlClipPlane();
            if (myFrontGlClipPlane == -1) {
               myClippingEnabled = false;
               return false;
            }
            if (isSlicingEnabled()) {
               myBackGlClipPlane = myViewer.allocGlClipPlane();
               if (myBackGlClipPlane == -1) {
                  mySlicingEnabled = false;
                  return false;
               }
            }
            else {
               myBackGlClipPlane = -1;
            }
         }
         else {
            if (myFrontGlClipPlane != -1) {
               myViewer.freeGlClipPlane (myFrontGlClipPlane);
               myFrontGlClipPlane = -1;
            }
            if (myBackGlClipPlane != -1) {
               myViewer.freeGlClipPlane (myBackGlClipPlane);
               myBackGlClipPlane = -1;
            }
         }
         myClippingEnabled = active;
         if (myViewer != null) {
            myViewer.repaint();
         }
      }
      return myClippingEnabled;
   }

   public void releaseClipPlanes() {
      if (myFrontGlClipPlane != -1) {
         myViewer.freeGlClipPlane (myFrontGlClipPlane);
      }
      if (myBackGlClipPlane != -1) {
         myViewer.freeGlClipPlane (myBackGlClipPlane);
      }
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
}
