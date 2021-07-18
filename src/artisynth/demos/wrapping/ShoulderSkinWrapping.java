package artisynth.demos.wrapping;

import java.io.IOException;

import artisynth.core.femmodels.SkinMeshBody;
import artisynth.core.femmodels.SkinMeshBody.FrameBlending;
import artisynth.core.materials.SimpleAxialMuscle;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MultiPointMuscle;
import artisynth.core.mechmodels.RigidBody;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;

public class ShoulderSkinWrapping extends ShoulderWrapping {
   
   int skinwrapping_num_viapoints = 3;

   public void build (String[] args) throws IOException {
     super.build (args);
     
     
   }
 
   @Override
   public void addSupraspinatus () {
      
      Point3d pos = new Point3d();
      Point3d pos_skin = new Point3d();
      RigidBody humerus = mech.rigidBodies ().get ("humerus");
      RigidBody scapula = mech.rigidBodies ().get ("scapula");      
      PolygonalMesh humerusMesh = humerus.getSurfaceMesh ();
      Vector3d vec = new Vector3d ();
      
   // create a SkinMeshBody and use it to create "skinned" muscle via points
      SkinMeshBody skinBody = new SkinMeshBody();
      skinBody.addMasterBody (mech.rigidBodies ().get ("scapula"));
      skinBody.addMasterBody (mech.rigidBodies ().get ("humerus"));
      skinBody.setFrameBlending (FrameBlending.DUAL_QUATERNION_LINEAR);
      mech.addMeshBody (skinBody);
      
      for (int i = 0; i < supraspinatus_num_fibers; i++) {
         double alpha = i/(double)supraspinatus_num_fibers;
         pos.scale (alpha, supraspinatus_origin_anterior);
         pos.scaledAdd (1-alpha, supraspinatus_origin_posterior);
         FrameMarker origin = mech.addFrameMarkerWorld (scapula, pos);

         pos.scale (alpha, supraspinatus_insertion_anterior);
         pos.scaledAdd (1-alpha, supraspinatus_insertion_posterior);
         humerusMesh.distanceToPoint (pos, pos);
         FrameMarker insertion = mech.addFrameMarkerWorld (humerus, pos);

         MultiPointMuscle muscle = new MultiPointMuscle ("muscle"+i);
         muscle.setMaterial (new SimpleAxialMuscle (0, 0, 10.0));
         muscle.addPoint (origin);

         VectorNd weights = new VectorNd(2);
         double s_offset = 0.8; // where along the strant to start adding via points
         for (int j=0; j<skinwrapping_num_viapoints; j++) {
            double s = (j+1)/(double)(skinwrapping_num_viapoints+1);

            double s_location = s_offset + (1-s_offset)*s; // location of via points along strand
            pos_skin.combine (
               s_location, insertion.getPosition(), (1-s_location), origin.getPosition());
            // project point to surface if it is inside the humerus
            if (humerusMesh.pointIsInside (pos_skin) == 1) {
               humerusMesh.distanceToPoint (pos_skin, pos_skin);
            }
            weights.set (0, 1-s); // scapula weight
            weights.set (1, s); // humerus weight
            muscle.addPoint (
               skinBody.addMarker (null, new Point3d (pos_skin), weights));
         }

         // //muscle.addPoint (skinBody.addMarker (new Point3d (pos_skin)));
         // for (int j = 1; j <= (skinwrapping_num_viapoints-1)/2+1; j++) {
         //    double b =
         //       (double)j * 1/((skinwrapping_num_viapoints-1)/2 + 1);
         //    vec.sub (pos, origin.getPosition ());
         //    vec.scale (b);
         //    pos_skin.add (origin.getPosition (), vec);
         //    muscle.addPoint (skinBody.addMarker (new Point3d (pos_skin)));
         // }
            
         // for (int j = 0; j < (skinwrapping_num_viapoints-1); j++) {
         //    double b =
         //       (double)j * 1/((skinwrapping_num_viapoints-1)/2 + 1);
         //    vec.sub (insertion.getPosition (), pos);
         //    vec.scale (b);
         //    pos_skin.add (pos, vec);
         //    muscle.addPoint (skinBody.addMarker (new Point3d (pos_skin)));
         // }

         muscle.addPoint (insertion);
        
         System.out.println ("updating wrap segments ...");
         //muscle.updateWrapSegments();
         mech.addMultiPointSpring (muscle);
      }      
   }

 
}
