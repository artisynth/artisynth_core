package artisynth.demos.fem;

import java.awt.Color;
import java.io.*;

import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.IncompMethod;
import artisynth.core.materials.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;

import artisynth.core.probes.*;
import maspack.matrix.*;
import maspack.render.*;

import java.util.*;

public class HexBeam3d extends FemBeam3d {

   private class Vibrator extends MonitorBase {

      List<FemNode3d> myNodes;
      double myA;
      double myFreq;
      
      public Vibrator (List<FemNode3d> nodes, double a, double freq) {
         myNodes = nodes;
         myA = a;
         myFreq = freq;
      }
      
      public void apply (double t0, double t1) {
         Vector3d vel = new Vector3d();
         Point3d pos = new Point3d();
         double off = myA*Math.sin (2*Math.PI*t1*myFreq);
         vel.z = myA*2*Math.PI*myFreq*Math.cos (2*Math.PI*t1*myFreq);
         for (FemNode3d n : myNodes) {
            if (!n.isDynamic()) {
               pos.set (n.getRestPosition());
               pos.z += off;
               n.setTargetPosition (pos);
               n.setTargetVelocity (vel);
            }
         }
      }
   }

   public void printStresses (String fileName) {

      try {
         PrintWriter pw =
            new PrintWriter (new BufferedWriter (new FileWriter (fileName)));
         for (FemNode3d n : myFemMod.getNodes()) {
            Point3d pos = n.getPosition();
            SymmetricMatrix3d sig = n.getStress();
            pw.printf (
               "%5d %10.6f %10.6f %10.6f ", n.getNumber(), pos.x, pos.y, pos.z);
            pw.printf (
               " %11.3f %11.3f %11.3f %11.3f %11.3f %11.3f\n",
               sig.m00, sig.m11, sig.m22, sig.m01, sig.m02, sig.m12);
         }
         pw.close();
      }
      catch (IOException e) {
         System.out.println ("Error opening or writing to file " + fileName);
      }
   }

   public void build (String[] args) {

      // NORMAL:
      super.build ("hex", 1.0, 0.2, 4, 2, /*options=*/0);
      //super.build ("hex", 30.0, 30.0, 10, 10, /*options=*/0);
      //super (name, "hex", 1.0, 0.2, 10, 5, 0);
      myFemMod.setSurfaceRendering (FemModel3d.SurfaceRender.None);

      LinearMaterial lmat = new LinearMaterial (100000, 0.33);
      myFemMod.setMaterial (lmat);
      
      RenderProps.setLineColor (myFemMod.getElements().get(12), Color.RED);

      // System.out.println ("DGT=\n" + DGT.toString ("%11.8f"));
      //myFemMod.addMarker (new FemMarker(0.4, -0.05, 0.05));

      //    addMarker (-0.1, 0.05, 2);
   //    addMarker ( 0, 0, 2);
       addMarker ( 0.1, -0.05, 2);
   //    addMarker ( 2, -0.05, 0);
   }

   private void addMarker (double x, double y, double z) {
      FemMarker mkr = myFemMod.addMarker (new Point3d (x, y, z), true);
      RenderProps.setPointColor (mkr, Color.RED);
   }

   public void build (String string, double d, double e, int i,
      int j, int k) {
      super.build(string,d,e,i,j,k);
      myFemMod.setMaterial (
         new MooneyRivlinMaterial (50000.0, 0, 0, 0, 0, 5000000.0));
      myFemMod.setIncompressible (FemModel.IncompMethod.AUTO);
   }

   public StepAdjustment advance (double t0, double t1, int flags) {
      // SolveMatrixTest tester = new SolveMatrixTest();
      // System.out.println ("error=" + tester.testStiffness (myMechMod, 1e-8));

      return super.advance (t0, t1, flags);
   }

//    public void advance (long t0, long t1) {
//       super.advance(t0, t1);
//       FemModel3d femMod = (FemModel3d)findComponent ("models/mech/models/fem");
//       SparseBlockMatrix S = femMod.getActiveStiffnessMatrix();
//       System.out.println ("S=" + S.rowSize()+"x"+S.colSize());
//    }
   
}

