package artisynth.demos.fem;

import java.awt.Point;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JSeparator;

import maspack.render.RenderProps;
import maspack.widgets.DoubleFieldSlider;
import artisynth.core.driver.Main;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.MuscleBundle;
import artisynth.core.gui.ControlPanel;
import artisynth.core.gui.FemControlPanel;
import artisynth.core.gui.NumericProbePanel;
import artisynth.core.mechmodels.MuscleExciter;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.util.ArtisynthIO;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;

public class SpongeDemo extends RootModel {
   SpongeModel sponge;

   // boolean kinematic = false;

   public void build (String[] args) throws IOException {

      sponge = new SpongeModel ("sponge");
      addModel (sponge);

      MuscleExciter exciter = new MuscleExciter ("gang");
      exciter.addTarget (sponge.getMuscleBundles().get (0), 1.0);
      exciter.addTarget (sponge.getMuscleBundles().get (1), 1.0);
      exciter.addTarget (sponge.getMuscleBundles().get (2), 1.0);
      sponge.addMuscleExciter (exciter);

      MuscleExciter all = new MuscleExciter ("all");
      for (MuscleBundle b : sponge.getMuscleBundles()) {
         all.addTarget (b, 1.0);
      }
      sponge.addMuscleExciter (all);

      sponge.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setAlpha (sponge, 0.5);

      // loadProbes();

      // build solve matrix to set solve indices
      // SparseBlockMatrix M = sponge.getSolveMatrix();
      // System.out.printf("M size = (%d, %d), nonzero = %d\n", M.rowSize(),
      // M.colSize(), M.numNonZeroVals()); // + J.toString("%8.2e"));
      // SparseBlockMatrix J = sponge.updateActJacobian(1.0);
      // System.out.printf("J size = (%d, %d), nonzero = %d\n", J.rowSize(),
      // J.colSize(), J.numNonZeroVals()); // + J.toString("%8.2e"));
      // System.out.println("J = \n" + J.toString("%8.2e"));

      // testActJacobian();
      // printSparseJacobian();
      addControlPanel();
   }

   @Override
   public void initialize (double t) {
      super.initialize (t);
      // sponge.getMuscleList().get(0).setExcitation(0.2);
      // sponge.getMuscleList().get(1).setExcitation(0.1);
   }

   public void loadProbes() {
      // set default working directory to repository location
      File workingDir = new File (
         ArtisynthPath.getSrcRelativePath (
            this, "data/sponge"));
      ArtisynthPath.setWorkingDir (workingDir);

      System.out.println ("Set working directory to "
      + ArtisynthPath.getWorkingDir().getAbsolutePath());

      String probeFileFullPath =
         ArtisynthPath.getWorkingDir().getPath() + "/sponge.probes";
      System.out.println ("Loading Probes from File: " + probeFileFullPath);

      try {
         scanProbes (
            ArtisynthIO.newReaderTokenizer (probeFileFullPath));
      }
      catch (Exception e) {
         System.out.println ("Error reading probe file");
         e.printStackTrace();
      }
   }

   protected void addControls (ControlPanel panel) {
      FemControlPanel.addMuscleControls (panel, sponge, sponge);
      panel.addWidget (sponge, "profile");

      if (sponge.getMuscleBundles().size() > 0) {
         ComponentList<MuscleBundle> muscles =
            ((SpongeModel)sponge).getMuscleBundles();
         for (int i = 0; i < muscles.size(); ++i) {
            // sponge.getMuscleList().get(i).setName(String.valueOf(i));
            DoubleFieldSlider slider =
               (DoubleFieldSlider)panel.addWidget (
                  "excitation [N]", sponge, "bundles/" + i + ":excitation", 0,
                  1);
            slider.setRoundingTolerance (0.001);
            int idx = i % NumericProbePanel.colorList.length;
            slider.getLabel().setForeground (NumericProbePanel.colorList[idx]);
         }
      }
      panel.addWidget (new JSeparator());
      panel.addWidget ("gang 0-2", sponge, "exciters/gang:excitation");
      panel.addWidget ("all muscles", sponge, "exciters/all:excitation");

   }

   ControlPanel panel;

   public void addControlPanel () {
      panel = new ControlPanel ("options", "LiveUpdate");
      panel.setScrollable (true);
      addControls (panel);
      addControlPanel (panel);
   }

   public void attach (DriverInterface driver) {
      super.attach (driver);

      sponge = (SpongeModel)findComponent ("models/sponge");
      // set workspace
      // ArtisynthPath.setWorkingDir(ArtisynthPath.getHomeRelativeFile(
      // "muscleData", "."));
      ArtisynthPath.setWorkingDir (ArtisynthPath.getHomeRelativeFile (".", "."));
      System.out.println ("working dir = " + ArtisynthPath.getWorkingDirPath());

      // NumericOutputProbe volumeProbe = new NumericOutputProbe(sponge,
      // "volume",
      // "volume.txt", sponge.getMaxStepSizeSec());
      // volumeProbe.setStartStopTimes (0, TimeBase.secondsToTicks (30.0));
      // volumeProbe.setDefaultDisplayRange (0.01, 0.018);
      //      
      // addOutputProbe(volumeProbe);

      loadProbes();

      // try
      // {
      //
      // {
      // loadProbes();
      // }
      // // NumericOutputProbe collector = new NumericOutputProbe(tissue,
      // // "state", "femState.txt", 0.01);
      // // collector.setDefaultDisplayRange(-1,1);
      // // collector.setStopTime(TimeBase.secondsToTicks(10));
      // // addOutputProbe(collector);
      // }
      // catch (IOException e)
      // {
      // System.err.println("unable to load muscleProbes.txt");
      // // try
      // // {
      // // NumericOutputProbe collector = new NumericOutputProbe(tissue,
      // // "state", "femState.txt", 0.01);
      // // collector.setDefaultDisplayRange(-1,1);
      // // collector.setStopTime(TimeBase.secondsToTicks(10));
      // //
      // // addOutputProbe(collector);
      // // }
      // // catch (Exception ee)
      // // {
      // // ee.printStackTrace();
      // // }
      // }

   }

   public void detach (DriverInterface driver) {
      super.detach (driver);

      if (panel != null) {
         panel.dispose();
         panel = null;
      }
   }

   /**
    * {@inheritDoc}
    */
   public String getAbout() {
      return "Simple Muscle Tissue";
   }

   // public void testActJacobian()
   // {
   //      
   // int n_x = sponge.getActivePosStateSize();
   // int n_u = sponge.numActivations();
   // VectorNd act = new VectorNd(n_u);
   // VectorNd f0 = new VectorNd(n_x);
   // VectorNd f1 = new VectorNd(n_x);
   //      
   // act.set(0,0.2);
   // act.set(1,0.1);
   // // act.set(2,0.1);
   // // act.set(3,0.4);
   // act.scale(2);
   // sponge.setActivations(act,0);
   //
   // // compute forces through mech model
   // f0.setZero();
   // sponge.setActiveVelState(f0, 0, true);
   // sponge.setGravity(0.0);
   // // sponge.setYoungsModulus(0.0); //stiffness
   // // sponge.setStiffnessDamping(0.0);
   //      
   // sponge.getActiveForces(f0,0.0,0,true);
   //      
   // SparseBlockMatrix J = sponge.updateActJacobian(1.0);
   // // System.out.printf("J = (%d, %d)\n", J.rowSize(), J.colSize());
   // // System.out.printf("a = (%d)\n", act.size());
   // // System.out.printf("f = (%d)\n", f1.size());
   // f1.mul(J,act);
   //      
   // double[] buf0 = f0.getBuffer();
   // double[] buf1 = f1.getBuffer();
   // double tol = 1e-6;
   // for (int i = 0; i < n_x; i++)
   // {
   // if (Math.abs(buf0[i]-buf1[i]) > tol)
   // {
   // System.out.printf("* ");
   // }
   // System.out.printf(" [%e ~ %e] = %e\n",buf0[i],buf1[i], buf0[i]-buf1[i]);
   // }
   // }

   // public void printSparseJacobian()
   // {
   // SparseBlockMatrix posJac = sponge.getSolveMatrix();
   // posJac.setZero();
   // sponge.addPosJacobian(posJac, 1.0);
   // SparseBlockMatrix J = sponge.updateActJacobian(1.0);
   //
   // int numPosJac = posJac.numNonZeroVals();
   // int[] rowOffs = new int[posJac.rowSize()];
   // int[] posRows = new int[numPosJac];
   // int[] posCols = new int[numPosJac];
   // double[] vals = new double[numPosJac];
   // posJac.getCRSIndices(posCols, rowOffs, Partition.Full);
   // getRowIndices(rowOffs, posRows, numPosJac, posJac.rowSize());
   // posJac.getCRSValues(vals, Partition.Full);
   // System.out.println("M = \n" + J.toString("%8.2e"));
   // System.out.println("M sparse = \n");
   // for (int i = 0; i < numPosJac; i++)
   // {
   // System.out.printf("(%d, %d) = %8.2e\n", posRows[i], posCols[i],
   // vals[i]);
   // }
   //    
   //    
   // int nele = J.numNonZeroVals();
   // int n = J.rowSize();
   // int[] cols = new int[nele];
   // rowOffs = new int[n];
   // int[] rows = new int[nele];
   // vals = new double[nele];
   //
   // J.getCRSIndices(cols, rowOffs, Partition.Full);
   // getRowIndices(rowOffs, rows, nele, n);
   // J.getCRSValues(vals, Partition.Full);
   //      
   // System.out.println("J = \n" + J.toString("%8.2e"));
   // System.out.println("J sparse = \n");
   // for (int i=0; i<nele; i++)
   // { System.out.printf("(%d, %d) = %8.2e\n", rows[i], cols[i], vals[i]);
   // }
   //      
   // }

   private void getRowIndices (
      int[] offs, int[] rows, int numNonZero, int rowSize) {
      if (offs == null || rows == null)
         return;
      if (rows.length < numNonZero)
         return;
      int idx = 0;
      for (int i = 1; i < rowSize; i++) {
         int numInRow = offs[i] - offs[i - 1];
         for (int c = 0; c < numInRow; c++)
            rows[idx++] = i - 1;
      }
      while (idx < numNonZero)
         rows[idx++] = rowSize - 1;
   }

}
