/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;

public class CubaturePoint3d extends Point3d {

   private static final long serialVersionUID = 11L;
   public double w;
   
   
   public CubaturePoint3d() {
      super(0,0,0);
      setWeight(0);
   }
   
   public CubaturePoint3d(double x, double y, double z, double w) {
      super(x,y,z);
      setWeight(w);
   }
   public CubaturePoint3d(Point3d pos, double w) {
      super(pos);
      setWeight(w);
   }
   
   public void setWeight(double w) {
      this.w = w;
   }
   
   public double getWeight() {
      return w;
   }
  
   public void transform (AffineTransform3dBase X) {
      super.transform(X);
      w = w*X.determinant();
   }

   /**
    * {@inheritDoc}
    */
   public void transform (AffineTransform3dBase X, Vector3d p1) {
      super.transform(X, p1);
      w = w*X.determinant();
   }

   /**
    * {@inheritDoc}
    */
   public void inverseTransform (AffineTransform3dBase X) {
      super.inverseTransform(X);
      w = w/X.determinant();
   }

   /**
    * {@inheritDoc}
    */
   public void inverseTransform (AffineTransform3dBase X, Vector3d p1) {
      super.inverseTransform(X, p1);
      w = w/X.determinant();
   }
   
   /**
    * {@inheritDoc}
    */
   public Point3d scale (double s) {
      super.scale(s);
      w = w*s*s*s;
      return this;
   }
   
   /**
    * {@inheritDoc} 
    */
   public Point3d scale (double sx, double sy, double sz) {
      super.scale(sx,sy,sz);
      w = w*sx*sy*sz;
      return this;
   }
   
   
}
