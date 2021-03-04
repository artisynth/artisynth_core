/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import maspack.matrix.*;

/**
 * Provides utility routines for Renderable objects.
 */
public class RenderableUtils {
   private static double inf = Double.POSITIVE_INFINITY;

   /**
    * Returns true if a Renderable is visible.
    */
   public static boolean isVisible (HasRenderProps renderable) {
      RenderProps props = renderable.getRenderProps();
      return props != null && props.isVisible();
   }

   /**
    * Sets the RenderProps of a Renderable object to a copy of its existing
    * RenderProps. If the object does not currently have any RenderProps, (i.e.,
    * {@link Renderable#getRenderProps getRenderProps} returns <code>null</code>),
    * then this method does nothing.
    * 
    * <p>
    * This routine is used to prevent side-effects when the existing RenderProps
    * are shared by more than one object.
    * 
    * @param r
    * Renderable objects whose RenderProps are to be cloned.
    */
   public static void cloneRenderProps (Renderable r) {
      RenderProps props = r.getRenderProps();
      if (props != null) {
         RenderProps newProps = r.createRenderProps();
         newProps.set (props);
         r.setRenderProps (newProps);
      }
   }

   /**
    * Returns a "radius" for a Renderable. This is done by computing the
    * distance from the center to the vertices of the bpunding box.
    */
   public static double getRadius (IsRenderable r) {
      Point3d min = new Point3d (inf, inf, inf);
      Point3d max = new Point3d (-inf, -inf, -inf);
      r.updateBounds (min, max);
      if (min.x != inf) {
         Vector3d diff = new Vector3d();
         diff.sub (max, min);
         return diff.norm() / 2;
      }
      else {
         return 0;
      }
   }

   /**
    * Returns a "radius" and a center for a Renderable.
    */
   public static double getRadiusAndCenter (Point3d center, IsRenderable r) {
      Point3d min = new Point3d (inf, inf, inf);
      Point3d max = new Point3d (-inf, -inf, -inf);
      r.updateBounds (min, max);
      center.add (min, max);
      center.scale (0.5);
      if (min.x != inf) {
         Vector3d diff = new Vector3d();
         diff.sub (max, min);
         return diff.norm() / 2;
      }
      else {
         return 0;
      }
   }

   /**
    * Computes the bounds for a Renderable.
    * 
    * @param r
    * Renderable to compute bounds for
    * @param min
    * minimum coordinate values (optional)
    * @param max
    * maximum coordinate values (optional)
    */
   public static void getBounds (IsRenderable r, Point3d min, Point3d max) {
      if (min == null) {
         min = new Point3d();
      }
      if (max == null) {
         max = new Point3d();
      }
      min.set (inf, inf, inf);
      max.set (-inf, -inf, -inf);
      r.updateBounds (min, max);
   }

   public static void updateBounds (Vector3d vmin, Vector3d vmax, float[] vals) {
      double x = vals[0];
      double y = vals[1];
      double z = vals[2];
      if (x > vmax.x) {
         vmax.x = x;
      }
      if (x < vmin.x) {
         vmin.x = x;
      }
      if (y > vmax.y) {
         vmax.y = y;
      }
      if (y < vmin.y) {
         vmin.y = y;
      }
      if (z > vmax.z) {
         vmax.z = z;
      }
      if (z < vmin.z) {
         vmin.z = z;
      }
   }

   /**
    * Update bounds to account for a sphere with a given width and radius.
    */
   public static void updateSphereBounds (
      Vector3d vmin, Vector3d vmax, Vector3d center, double radius) {

      double x = center.x;
      double y = center.y;
      double z = center.z;
      if (x - radius < vmin.x) {
         vmin.x = x - radius;
      }
      if (x + radius > vmax.x) {
         vmax.x = x + radius;
      }
      if (y - radius < vmin.y) {
         vmin.y = y - radius;
      }
      if (y + radius > vmax.y) {
         vmax.y = y + radius;
      }
      if (z - radius < vmin.z) {
         vmin.z = z - radius;
      }
      if (z + radius > vmax.z) {
         vmax.z = z + radius;
      }
   }

   /**
    * Update bounds to account for the coordinate frame TFW drawn with length
    * len.
    */
   public static void updateFrameBounds (
      Vector3d vmin, Vector3d vmax, RigidTransform3d TFW, double len) {

      TFW.p.updateBounds (vmin, vmax);
      RotationMatrix3d R = TFW.R;
      Vector3d axisTip = new Vector3d();

      // x axis
      axisTip.set (R.m00, R.m10, R.m20);
      axisTip.scale (len);
      axisTip.add (TFW.p);
      axisTip.updateBounds (vmin, vmax);

      // y axis
      axisTip.set (R.m01, R.m11, R.m21);
      axisTip.scale (len);
      axisTip.add (TFW.p);
      axisTip.updateBounds (vmin, vmax);
      
      // z axis
      axisTip.set (R.m02, R.m12, R.m22);
      axisTip.scale (len);
      axisTip.add (TFW.p);
      axisTip.updateBounds (vmin, vmax);
   }

}
