/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Deque;

import maspack.geometry.PolygonalMesh;
import maspack.matrix.Matrix3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.Matrix6x1;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.Renderer;
import maspack.util.InternalErrorException;
import maspack.util.ReaderTokenizer;
import maspack.util.NumberFormat;
import artisynth.core.femmodels.IntegrationData3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.DeformedPointBase;
import artisynth.core.util.ScanToken;

public class BeamBody extends DeformableBody {

   double myLen;

   private static double DEFAULT_STIFFNESS = 1000.0;

   double myStiffness = DEFAULT_STIFFNESS;

   public static PropertyList myProps =
      new PropertyList (BeamBody.class, DeformableBody.class);

   static {
      myProps.add (
         "stiffness", "beam stiffness", DEFAULT_STIFFNESS);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getStiffness () {
      return myStiffness;
   }

   public void setStiffness (double E) {
      myStiffness = E;
      invalidateStiffness();
   }

   @Override public <T extends FemMaterial> T setMaterial (T mat) {
      T newMat = super.setMaterial (mat);
      invalidateStiffness();
      return newMat;
   }

   public BeamBody () {
   }

   public BeamBody (
      PolygonalMesh mesh, double density, double len, double E) {

      setDensity (density);
      setMesh (mesh, /*meshFileName=*/null);
      myLen = len;
      setStiffness (E);
   }

   public int numElasticCoords() {
      //return 8;
      return 6;
   }

   public void updateStiffnessMatrix() {
      if (!myStiffnessValidP) {
         computeStiffnessMatrix();
      }
   }

   // private static Point3d[] myIntegrationPoints = new Point3d[] {
   //    new Point3d (-Math.sqrt(3/5.0), 0, 0), 
   //    new Point3d (0, 0, 0), 
   //    new Point3d (Math.sqrt(3/5.0), 0, 0)
   // };

   private static IntegrationPoint3d[] myIntegrationPoints = new IntegrationPoint3d[] {
      new IntegrationPoint3d (0, 0,  -Math.sqrt(3/5.0), 0, 0, /*weight=*/5.0/9.0), 
      new IntegrationPoint3d (0, 0,  0, 0, 0,                 /*weight=*/8.0/9.0), 
      new IntegrationPoint3d (0, 0,  Math.sqrt(3/5.0), 0, 0,  /*weight=*/5.0/9.0)
   };

   private void computeBFromDShape (Matrix6x1 B, Matrix3d DS) {
      B.m00 = DS.m00;
      B.m10 = DS.m11;
      B.m20 = DS.m22;
      B.m30 = DS.m01 + DS.m10;
      B.m40 = DS.m12 + DS.m21;
      B.m50 = DS.m02 + DS.m20;
   }

   public void computeStiffnessFromIntegration () {
      int numc = numElasticCoords();
      Matrix6d D = new Matrix6d();
      Matrix3d DS = new Matrix3d();
      Matrix6x1 Bi = new Matrix6x1();
      Matrix6x1 Bj = new Matrix6x1();
      Matrix6x1 Bx = new Matrix6x1();

      myStiffnessMatrix.setZero();

      for (int k=0; k<myIntegrationPoints.length; k++) {

         DeformedPointBase def = new DeformedPointBase();
         IntegrationPoint3d pt = myIntegrationPoints[k];
         IntegrationData3d dt = new IntegrationData3d ();
         //pt.setF (Matrix3d.IDENTITY);
         def.setF (Matrix3d.IDENTITY);
         // get the tangent at the rest position
         Matrix3d Q = Matrix3d.IDENTITY;
         //myMaterial.computeTangent (D, pt.getStress(), pt, dt, null);
         //myMaterial.computeTangent (D, SymmetricMatrix3d.ZERO, def, Q, null);
         SymmetricMatrix3d sigma = new SymmetricMatrix3d();
         myMaterial.computeStressAndTangent (sigma, D, def, Q, 0.0, null);
         double dl = (myLen/2)*pt.getWeight();

         for (int i=0; i<numc; i++) {
            getDShape (DS, i, pt.getCoords());
            computeBFromDShape (Bi, DS);
            for (int j=0; j<numc; j++) {
               getDShape (DS, j, pt.getCoords());
               computeBFromDShape (Bj, DS);
               Bx.mul (D, Bj);
               myStiffnessMatrix.add (i, j, dl*Bi.dot(Bx));
            }
         }
      }
   }

   public void computeStiffnessMatrix() {
      myStiffnessMatrix.setZero();

      // Matrix4d Ksub = new Matrix4d();

      // double lsqr = myLen*myLen;
      // double l = myLen;
      // double E = myStiffness;

      // Ksub.set (new double[] {
      //       12, 6*l, -12, 6*l, 
      //       6*l, 4*lsqr, -6*l, 2*lsqr,
      //       -12, -6*l, 12, -6*l,
      //       6*l, 2*lsqr, -6*l, 4*lsqr });

      // Ksub.scale (myStiffness/(lsqr*l));

      // // Ksub.setDiagonal (new double[] { E, E, E, E });
      // // Ksub.set (new double[] {
      // //       E, 0, -E/2, 0, 
      // //       0, E, 0, -E/2, 
      // //       -E/2, 0, E, 0,
      // //       0, -E/2, 0, E });

      // myStiffnessMatrix.setSubMatrix (0, 0, Ksub);
      // myStiffnessMatrix.setSubMatrix (4, 4, Ksub);

      // System.out.println ("K0=\n" + myStiffnessMatrix.toString ("%10.5f"));
      computeStiffnessFromIntegration();
      myStiffnessValidP = true;
   }

   private static double sqr (double x) {
      return x*x;
   }

   public void getShape (Vector3d shp, int i, Vector3d pos0) {

      double xi = 2*pos0.x/myLen;
      shp.setZero();
      
      switch (i) {
         // case 0: shp.y = 0.25*sqr(1-xi)*(2+xi); return;
         // case 1: shp.y = 0.25*sqr(1+xi)*(2-xi); return;
         // case 2: shp.y = 0.125*myLen*sqr(1-xi)*(1+xi); return;
         // case 3: shp.y = -0.125*myLen*sqr(1+xi)*(1-xi); return;

         // case 4: shp.z = 0.25*sqr(1-xi)*(2+xi); return;
         // case 5: shp.z = 0.25*sqr(1+xi)*(2-xi); return;
         // case 6: shp.z = 0.125*myLen*sqr(1-xi)*(1+xi); return;
         // case 7: shp.z = -0.125*myLen*sqr(1+xi)*(1-xi); return;

         case 0: shp.y = 0.25*sqr(1-xi)*(2+xi)-0.5; return;
         case 1: shp.y = 0.125*myLen*sqr(1-xi)*(1+xi); return;
         case 2: shp.y = -0.125*myLen*sqr(1+xi)*(1-xi); return;

         case 3: shp.z = 0.25*sqr(1-xi)*(2+xi)-0.5; return;
         case 4: shp.z = 0.125*myLen*sqr(1-xi)*(1+xi); return;
         case 5: shp.z = -0.125*myLen*sqr(1+xi)*(1-xi); return;

         default: {
            throw new InternalErrorException (
               "shape function index "+i+" exceeds "+(numElasticCoords()-1));
         }
      }
      
   }

   public void getDShape (Matrix3d Dshp, int i, Vector3d pos0) {

      double xi = 2*pos0.x/myLen;
      Dshp.setZero();

      switch (i) {
         // case 0: Dshp.m10 = 1.5*(xi*xi-1)/myLen; return;
         // case 1: Dshp.m10 = 1.5*(1-xi*xi)/myLen; return;
         // case 2: Dshp.m10 = 0.25*(3*xi*xi-2*xi-1); return;
         // case 3: Dshp.m10 = 0.25*(3*xi*xi+2*xi-1); return;

         // case 4: Dshp.m20 = 1.5*(xi*xi-1)/myLen; return;
         // case 5: Dshp.m20 = 1.5*(1-xi*xi)/myLen; return;
         // case 6: Dshp.m20 = 0.25*(3*xi*xi-2*xi-1); return;
         // case 7: Dshp.m20 = 0.25*(3*xi*xi+2*xi-1); return;

         case 0: Dshp.m10 = 1.5*(xi*xi-1)/myLen; return;
         case 1: Dshp.m10 = 0.25*(3*xi*xi-2*xi-1); return;
         case 2: Dshp.m10 = 0.25*(3*xi*xi+2*xi-1); return;

         case 3: Dshp.m20 = 1.5*(xi*xi-1)/myLen; return;
         case 4: Dshp.m20 = 0.25*(3*xi*xi-2*xi-1); return;
         case 5: Dshp.m20 = 0.25*(3*xi*xi+2*xi-1); return;

         default: {
            throw new InternalErrorException (
               "shape function index "+i+" exceeds "+(numElasticCoords()-1));
         }
      }
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "length")) {
         double len = rtok.scanNumber();
         myLen = len;
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }   


   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.println ("length=" + fmt.format (myLen));
      super.writeItems (pw, fmt, ancestor);
   }
    

}

