package artisynth.demos.fem;

import java.io.*;

import artisynth.core.util.*;
import artisynth.core.femmodels.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.LinearMaterial;
import maspack.matrix.*;
import maspack.geometry.*;

public class SelfCollision extends FemBeam3d {

   private static double EPS = 1e-8;

   private void makeNodesCircularInYZ (double x, double r) {
      Point3d pos = new Point3d();
      for (FemNode3d node : myFemMod.getNodes()) {
         node.getPosition(pos);
         if (Math.abs(pos.x-x) < EPS) {
            double r0 = Math.sqrt(pos.z*pos.z + pos.y*pos.y);
            if (r0 > EPS) {
               pos.y *= r/r0;
               pos.z *= r/r0;
               node.setPosition(pos);
            }
         }
      }
   }

   public void build (String[] args) {
      build ("hex", 1.0, 0.1, 10, 2, NO_FIXED_NODES);

      try {
         myFemMod.addMeshComp (
            myFemMod.scanMesh (
               ArtisynthIO.newReaderTokenizer (
                  new StringReader (rightMeshString))));
         myFemMod.addMeshComp (
            myFemMod.scanMesh (
               ArtisynthIO.newReaderTokenizer (
                  new StringReader (leftMeshString))));
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit(1);
      }

      myMechMod.setCollisionBehavior (myFemMod, myFemMod, true);
      myFemMod.setMaterial (new LinearMaterial (40000, 0.2));

      // flair the ends so collision works properly
      makeNodesCircularInYZ (-0.5, 0.03);
      makeNodesCircularInYZ (-0.4, 0.07);
      makeNodesCircularInYZ (-0.3, 0.05);
      makeNodesCircularInYZ ( 0.3, 0.05);
      makeNodesCircularInYZ ( 0.4, 0.07);
      makeNodesCircularInYZ ( 0.5, 0.03);
      myFemMod.resetRestPosition();

      int[] fixedNodes = new int[] { 5, 38, 71, 82, 93, 60, 27, 16, 49};
      for (int i=0; i<fixedNodes.length; i++) {
         myFemMod.getNodes().getByNumber(fixedNodes[i]).setDynamic (false);
      }
      myFemMod.updateSlavePos();
   }

   String rightMeshString =
      "[ f 9 8 19\n"+
      "  f 9 19 20\n"+
      "  f 8 41 52\n"+
      "  f 8 52 19\n"+
      "  f 41 8 9\n"+
      "  f 41 9 42\n"+
      "  f 74 75 86\n"+
      "  f 74 86 85\n"+
      "  f 41 74 85\n"+
      "  f 41 85 52\n"+
      "  f 74 41 42\n"+
      "  f 74 42 75\n"+
      "  f 20 19 30\n"+
      "  f 20 30 31\n"+
      "  f 19 52 63\n"+
      "  f 19 63 30\n"+
      "  f 63 64 31\n"+
      "  f 63 31 30\n"+
      "  f 85 86 97\n"+
      "  f 85 97 96\n"+
      "  f 52 85 96\n"+
      "  f 52 96 63\n"+
      "  f 96 97 64\n"+
      "  f 96 64 63\n"+
      "  f 43 10 21\n"+
      "  f 43 21 54\n"+
      "  f 10 9 20\n"+
      "  f 10 20 21\n"+
      "  f 42 9 10\n"+
      "  f 42 10 43\n"+
      "  f 75 76 87\n"+
      "  f 75 87 86\n"+
      "  f 76 43 54\n"+
      "  f 76 54 87\n"+
      "  f 75 42 43\n"+
      "  f 75 43 76\n"+
      "  f 54 21 32\n"+
      "  f 54 32 65\n"+
      "  f 21 20 31\n"+
      "  f 21 31 32\n"+
      "  f 64 65 32\n"+
      "  f 64 32 31\n"+
      "  f 86 87 98\n"+
      "  f 86 98 97\n"+
      "  f 87 54 65\n"+
      "  f 87 65 98\n"+
      "  f 97 98 65\n"+
      "  f 97 65 64\n"+
      "]";

   String leftMeshString = 
      "[ f 1 0 11\n"+
      "  f 1 11 12\n"+
      "  f 0 33 44\n"+
      "  f 0 44 11\n"+
      "  f 33 0 1\n"+
      "  f 33 1 34\n"+
      "  f 66 67 78\n"+
      "  f 66 78 77\n"+
      "  f 33 66 77\n"+
      "  f 33 77 44\n"+
      "  f 66 33 34\n"+
      "  f 66 34 67\n"+
      "  f 12 11 22\n"+
      "  f 12 22 23\n"+
      "  f 11 44 55\n"+
      "  f 11 55 22\n"+
      "  f 55 56 23\n"+
      "  f 55 23 22\n"+
      "  f 77 78 89\n"+
      "  f 77 89 88\n"+
      "  f 44 77 88\n"+
      "  f 44 88 55\n"+
      "  f 88 89 56\n"+
      "  f 88 56 55\n"+
      "  f 35 2 13\n"+
      "  f 35 13 46\n"+
      "  f 2 1 12\n"+
      "  f 2 12 13\n"+
      "  f 34 1 2\n"+
      "  f 34 2 35\n"+
      "  f 67 68 79\n"+
      "  f 67 79 78\n"+
      "  f 68 35 46\n"+
      "  f 68 46 79\n"+
      "  f 67 34 35\n"+
      "  f 67 35 68\n"+
      "  f 46 13 24\n"+
      "  f 46 24 57\n"+
      "  f 13 12 23\n"+
      "  f 13 23 24\n"+
      "  f 56 57 24\n"+
      "  f 56 24 23\n"+
      "  f 78 79 90\n"+
      "  f 78 90 89\n"+
      "  f 79 46 57\n"+
      "  f 79 57 90\n"+
      "  f 89 90 57\n"+
      "  f 89 57 56\n"+
      "]";
}

