/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.ArrayList;

import maspack.util.NumberFormat;

public class AffineOrthogonalTest {

   
   public static void doSkewTest() {
      
      double [][] pnts = {{-1,-1,-1}, {-1,1,-1}, {-1,1,1}, {-1,-1,1}, {1,-1,-1}, {1,1,-1}, {1,1,1}, {1,-1,1} };
      
      Vector3d skewVec = new Vector3d(2,1,2);
      Vector3d axis = new Vector3d(1,1,0);
      double angle = 1.7;
      Vector3d translation = new Vector3d(5,2,3);
      Vector3d scaling = new Vector3d(3,4,1);
      
      
      Matrix3d skew = new Matrix3d( 1, -skewVec.z, skewVec.y,
                             skewVec.z, 1, -skewVec.x,
                             -skewVec.y, skewVec.x, 1);
      RotationMatrix3d R = new RotationMatrix3d();
      R.setAxisAngle(new AxisAngle(axis, angle));
      
      AffineTransform3d trans = new AffineTransform3d();
      trans.setRotation(R);
      trans.A.mul(skew);
      trans.applyScaling(scaling.x, scaling.y, scaling.z);
      trans.setTranslation(translation);
      
      NumberFormat fmt = new NumberFormat("%.5f");
      Vector3d.setDefaultFormat(fmt.toString());
      Matrix3d.setDefaultFormat(fmt.toString());
      
      ArrayList<Point3d> p = new ArrayList<Point3d>();
      ArrayList<Point3d> q = new ArrayList<Point3d>();
      
      
      for (int i=0; i<pnts.length; i++) {
         Point3d v = new Point3d(pnts[i]);
         Point3d w = new Point3d(pnts[i]);
         w.transform(trans);
         
         p.add(w);
         q.add(v);

      }
      
      System.out.println("Original: \n" + trans);
      
      AffineTransform3d transb = new AffineTransform3d();
      transb.fitOrthogonal(p, q);
      System.out.println("Computed: \n"+transb);
      
      AffineTransform3d transc = new AffineTransform3d();
      transc.fitOrthogonal(p, q, 1e-15);
      System.out.println("Iterated: \n"+transc);
      
      // decompose:
      Vector3d translationb = transb.p;
      Matrix3d Rb = new Matrix3d(transb.A);
      Vector3d scaleb = new Vector3d();
      Vector3d tmp  = new Vector3d();
      
      for (int i=0; i<3; i++) {
         Rb.getColumn(i, tmp);
         double s = tmp.norm();
         if (tmp.get(i)<0) {
            s = -s;
         }
         tmp.scale(1.0/s);
         scaleb.set(i, s);
         Rb.setColumn(i, tmp);
      }
      
      RotationMatrix3d Rb2 = new RotationMatrix3d();
      Rb2.setSubMatrix(0, 0, Rb);
      AxisAngle aab = new AxisAngle(Rb2);
      
      System.out.println("Translation: " + translationb);
      System.out.println("Rotation: " + aab.axis + ", " + fmt.format(aab.angle));
      System.out.println("Scaling: " + scaleb);
      
      double err = 0;
      double errOrig = 0;
      double errIter = 0;
      
      for (int i=0; i<q.size(); i++) {
         
         Vector3d diff = new Vector3d(q.get(i));
         diff.sub(p.get(i));
         errOrig += diff.normSquared();
         
         diff.set(q.get(i));
         diff.transform(transb);
         diff.sub(p.get(i));
         err += diff.normSquared();
         
         diff.set(q.get(i));
         diff.transform(transc);
         diff.sub(p.get(i));
         errIter += diff.normSquared();
      }
      
      System.out.println("P = [");
      for (int i=0; i<q.size(); i++) {
         System.out.println(p.get(i));
      }
      System.out.println("];");
      
      System.out.println("Q = [");
      for (int i=0; i<q.size(); i++) {
         Point3d pnt = new Point3d(q.get(i));
         pnt.transform(transb);
         System.out.println(pnt);
      }
      System.out.println("];");
      
      System.out.println();
      System.out.println("Original error = " + errOrig);
      System.out.println("Error = " + err);
      System.out.println("Iterated Error = " + errIter);
      
   }
   
   public static void doTest() {
      
      double [][] pnts = {{-1,-1,-1}, {-1,1,-1}, {-1,1,1}, {-1,-1,1}, {1,-1,-1}, {1,1,-1}, {1,1,1}, {1,-1,1} };
      
      NumberFormat fmt = new NumberFormat("%.5f");
      Vector3d.setDefaultFormat(fmt.toString());
      Matrix3d.setDefaultFormat(fmt.toString());
      
      ArrayList<Point3d> p = new ArrayList<Point3d>();
      ArrayList<Point3d> q = new ArrayList<Point3d>();
      
      AffineTransform3d transf = new AffineTransform3d();
      
      Vector3d axis = new Vector3d(1,1,0);
      double angle = 1.7;
      Vector3d translation = new Vector3d(5,2,3);
      Vector3d scaling = new Vector3d(-13,-2,-7);
      
      axis.normalize();
      transf.setRotation(new AxisAngle(axis, angle));
      transf.setTranslation(translation);
      transf.applyScaling(scaling.x, scaling.y, scaling.z);
      
      for (int i=0; i<pnts.length; i++) {
         Point3d v = new Point3d(pnts[i]);
         Point3d w = new Point3d(pnts[i]);
         w.transform(transf);
         
         p.add(w);
         q.add(v);
      }
      
      AffineTransform3d transb = new AffineTransform3d();
      transb.fitOrthogonal(p, q);
      
      // decompose:
      Vector3d translationb = transb.p;
      Matrix3d Rb = new Matrix3d(transb.A);
      Vector3d scaleb = new Vector3d();
      Vector3d tmp  = new Vector3d();
      
      for (int i=0; i<3; i++) {
         Rb.getColumn(i, tmp);
         double s = tmp.norm();
         tmp.scale(1.0/s);
         scaleb.set(i, s);
         Rb.setColumn(i, tmp);
      }
      // check if we have a flip instead of rotation
      double Rbdet = Rb.determinant();
      if (Rbdet < 0 ) {
         Rbdet = -Rbdet;
         Rb.getColumn(2, tmp);
         tmp.scale(-1);
         scaleb.set(2, -scaleb.get(2));
         Rb.setColumn(2, tmp);
      }
      
      RotationMatrix3d Rb2 = new RotationMatrix3d();
      Rb2.setSubMatrix(0, 0, Rb);
      AxisAngle aab = new AxisAngle(Rb2);
      
      System.out.println("Translation: \t" + translationb + " \t/ " + translation);
      System.out.println("Rotation: \t" + aab.axis + ", " + fmt.format(aab.angle) + " \t/ " + axis + ", " + angle);
      System.out.println("Scaling: \t" + scaleb + " \t/ " + scaling);
      
      // re-check based on new info
      AffineTransform3d transc = new AffineTransform3d();
      transc.setRotation(aab);
      transc.setTranslation(translationb);
      transc.applyScaling(scaleb.x, scaleb.y, scaleb.z);
      
      AffineTransform3d transd = new AffineTransform3d();
      transd.fitOrthogonal(p, q, 1e-15);
      
      System.out.println("Original: \n"+transf);
      System.out.println("Computed: \n"+transb);
      System.out.println("Recomputed transform:\n" + transc);
      System.out.println("Iterated transform:\n" + transd);      

   }
   
   public static void main(String[] args) {
      
      //doTest();
      doSkewTest();
      
   }
}
