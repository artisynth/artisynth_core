/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.IOException;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.materials.AxialMuscleMaterial;
import artisynth.core.util.ArtisynthPath;


public class PointToPointMuscle extends FemMuscleModel {
   public static String fempath =
      ArtisynthPath.getSrcRelativePath (PointToPointMuscle.class, "meshes/");

   public static PropertyList myProps =
      new PropertyList (PointToPointMuscle.class, FemMuscleModel.class);

   static {
      myProps.remove ("excitation");
      myProps.add ("excitation * *", "Muscle excitation", 0, "[0,1] NW");
      myProps.add ("maxForce * *", "Max Muscle Force N", 0, "[0,10000000] NW");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getExcitation() {
      if (numActivations() < 1) {
         return 0;   // no muscle
      }
      MuscleBundle bundle = getMuscleBundles().get(0);
      return bundle.getExcitation();
   }

   public void setExcitation (double excitation) {
      if (numActivations() < 1) {
         return;  // no bundles
      }
      MuscleBundle bundle = getMuscleBundles().get(0);
      bundle.setExcitation (excitation);
   }

   static int[] strand0 =
      new int[] { 60, 17, 16, 14, 22, 21, 42, 41, 47, 48, 45, 61 };
   static int[] strand1 =
      new int[] { 60, 15, 12, 13, 20, 4, 39, 40, 46, 44, 1, 61 };
   static int[] strand2 =
      new int[] { 60, 30, 24, 25, 5, 3, 52, 53, 59, 2, 0, 61 };
   static int[] strand3 =
      new int[] { 60, 29, 28, 26, 32, 31, 55, 54, 57, 58, 56, 61 };
   static int[] strand4 =
      new int[] { 60, 27, 19, 10, 23, 7, 43, 37, 50, 51, 34, 61 };
   static int[] strand5 =
      new int[] { 60, 18, 11, 9, 8, 6, 38, 36, 49, 35, 33, 61 };
   static int firstNode = 60, lastNode = 61;
   
   public static final int[] originAttachmentNodes =
      new int[] { 29, 27, 15, 17, 30, 60, 18 };
   public static final int[] insertionAttachmentNodes =
      new int[] { 33, 0, 61, 34, 1, 56, 45 };

   
   public PointToPointMuscle() {
      super();

      //bundle = new MuscleBundle();
      //bundle.setFibresActive (true);
   }

   public PointToPointMuscle (
      String name, double density, double scale, String meshName,
      boolean addMuscles) throws IOException {
      super (name);

      TetGenReader.read (
         this, density, fempath+meshName+".1.node", fempath+meshName+".1.ele",
         new Vector3d (scale, scale, scale));

      MuscleBundle bundle = new MuscleBundle();
      bundle.setFibresActive (true);

      if (addMuscles) {
         addStrand (bundle, strand0);
         addStrand (bundle, strand1);
         addStrand (bundle, strand2);
         addStrand (bundle, strand3);
         addStrand (bundle, strand4);
         addStrand (bundle, strand5);
         addMuscleBundle (bundle);
      }
      setMaxForce (1.0);
   }

   private void addStrand (MuscleBundle bundle, int[] strand) {
      for (int s = 4; s < strand.length - 3; s++) {
         Muscle fibre = new Muscle();
         fibre.setConstantMuscleMaterial(1, 1);
         fibre.setFirstPoint (getNode (strand[s - 1]));
         fibre.setSecondPoint (getNode (strand[s]));
         bundle.addFibre (fibre);
      }
   }

   public FemNode3d getFirstNode() {
      return getNode (firstNode);
   }

   public FemNode3d getLastNode() {
      return getNode (lastNode);
   }

   public double getMaxForce() {
      if (numActivations() < 1) {
         return 0;   // no muscle
      }
      MuscleBundle bundle = getMuscleBundles().get(0);
      Muscle mus = bundle.getFibres().get(0);
      if (mus.getMaterial() instanceof AxialMuscleMaterial) {
         return ((AxialMuscleMaterial)mus.getMaterial()).getMaxForce();
      }
      else {
         return 0;
      }
   }

   public void setMaxForce (double maxForce) {
      if (numActivations() < 1) {
         return;   // no muscle
      }
      MuscleBundle bundle = getMuscleBundles().get(0);
      bundle.setMaxForce (maxForce);
   }
}
