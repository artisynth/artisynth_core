package artisynth.demos.opensim;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.awt.Color;

import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.opensim.OpenSimParser;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.render.*;
import maspack.util.PathFinder;
import maspack.geometry.*;

import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.SurfaceRender;

/**
 * Modified version of OpenSimArm26 that replaces the humerus with an FEM
 * model, modifying the joints, wrap objects and muscle points accordingly.
 */
public class OpenSimFemHumerus extends OpenSimArm26 {

   double frameAttachDist = 0.005;

   public void build (String[] args) throws IOException {
      // create OpenSimArm26
      super.build (args);

      // find the original humerus bone
      RigidBody humerus = myParser.findBody ("r_humerus");

      // create a FEM model for the humerus, based on a TetGen construction of
      // a higher resolution surface mesh
      PolygonalMesh mesh = new PolygonalMesh (
         getSourceRelativePath ("osim/Geometry/humerus_rv_highres.obj"));
      FemModel3d fem = FemFactory.createFromMesh (
         null, mesh, /*quality*/2);
      fem.setMaterial (new LinearMaterial (1e9, 0.49));
      fem.setParticleDamping (1.0);
      // match the density to the original humerus
      fem.setDensity (humerus.getDensity());
      fem.setName ("humerus");
      // transform the model to match current humerus pose
      fem.transformGeometry (humerus.getPose());
      // enable surface rending, turn off element rendering, change color
      fem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setVisible (fem.getElements(), false);
      RenderProps.setDrawEdges (fem, true);
      RenderProps.setFaceColor (fem, new Color (0.8f, 0.8f, 1f));
      RenderProps.setLineColor (fem, new Color (0.6f, 0.6f, 1f));
      myMech.addModel (fem);

      // replace humerus in the shoulder and elbow joints
      JointBase shoulder = myParser.findJoint ("r_shoulder");
      JointBase elbow = myParser.findJoint ("r_elbow");
      shoulder.setBodyA (
         fem, createFrameAttachment (null, fem, shoulder.getCurrentTCW()));
      elbow.setBodyB (
         fem, createFrameAttachment (null, fem, elbow.getCurrentTDW()));
      // add range limits for the elbow
      elbow.setCoordinateRange (0, new DoubleInterval(0, Math.PI));

      // Attach three frames to top, middle, and bottom of the humerus FEM.
      // These will serve as attacment frames for the wrap objects, or for
      // humerus-based muscle points that are not origins or insertions.
      Frame[] femFrames = new Frame[3];
      // use shoulder joint frame for the top frame
      RigidTransform3d TFW = new RigidTransform3d (shoulder.getCurrentTDW());
      femFrames[0] = addFemFrame (myMech, fem, "top", TFW);
      TFW.mulXyz (0, -0.14, -0.005);
      femFrames[1] = addFemFrame (myMech, fem, "mid", TFW);
      // use elbow joint frame for the bottom frame
      femFrames[2] = addFemFrame (myMech, fem, "bot", elbow.getCurrentTDW());

      // reattach each humerus wrap object to the nearest FEM frame
      for (WrapComponent wobj : myParser.getWrapObjects()) {
         // remove object and it's attachment from OpenSim hierarchy
         myParser.removeWrapObject(wobj);
         RigidBody wbody = (RigidBody)wobj;
         // place it in the default 'rigidBodies' container, and create an
         // attachment for it
         myMech.addRigidBody (wbody);
         Frame frame = nearestFrame (femFrames, wbody.getPose().p);
         myMech.addAttachment (new FrameFrameAttachment (wbody, frame));
      }

      // replace the muscle points that are attached to the humerus with ones
      // that are attached to the FEM
      for (PointSpringBase muscle : myParser.getMusclesAndSprings()) {
         for (int i=0; i<muscle.numPoints(); i++) {
            // can assume default OpenSim muscle points are FrameMarkers
            FrameMarker mkr = (FrameMarker)muscle.getPoint(i);
            if (mkr.getFrame() == humerus) {
               // marker is attached to the humerus
               PointAttachable attachBody;
               if (i == 0 || i == muscle.numPoints()-1) {
                  // origin or insertion - attach directly to FEM
                  attachBody = fem;
               }
               else {
                  // attach to the nearest frame
                  attachBody = nearestFrame (femFrames, mkr.getPosition());
               }
               // replace the old point with a new one attached to 'attachBody'
               myParser.replaceMusclePoint (muscle, mkr, attachBody);
            }
         }
      }
      // reassign epicondyle marker to the lowest FEM frame
      Marker mkr = myParser.findMarker ("r_humerus_epicondyle");
      myParser.replaceMarker (mkr, femFrames[2]);
      // remove the humerus, which is now attached to nothing
      myParser.getBodySet().remove (humerus);
   }

   /**
    * Find the frame whose origin is nearest to a given point or vector.
    */
   Frame nearestFrame (Frame[] frames, Vector3d vec) {
      Frame nearest = null;
      double minDist = Double.POSITIVE_INFINITY;
      for (Frame frame : frames) {
         double dist = frame.getPose().p.distance(vec);
         if (dist < minDist) {
            nearest = frame;
            minDist = dist;
         }
      }
      return nearest;
   }
   
   /**
    * Create a frame attachment inside a FEM model. For this application, we
    * support the attachment using the FEM nodes that are within
    * 'frameAttachDist' of the frame's z axis.
    */
   FrameAttachment createFrameAttachment (
      Frame frame, FemModel3d fem, RigidTransform3d TFW) {
      ArrayList<FemNode3d> nodes = new ArrayList<>();
      Vector3d u = new Vector3d();
      TFW.R.getColumn (2, u);
      Line line = new Line (new Point3d(TFW.p), u);
      for (FemNode3d n : fem.getNodes()) {
         if (line.distance(n.getPosition()) <= frameAttachDist) {
            nodes.add (n);
         }
      }
      FrameFem3dAttachment attachment = new FrameFem3dAttachment (frame);
      attachment.setFromNodes (TFW, nodes.toArray(new FemNode3d[0]));
      return attachment;
   }

   /**
    * Create a frame with a given name, and attach it to the FEM at
    * the pose specified by TFW.
    */
   private Frame addFemFrame (
      MechModel mech, FemModel3d fem, String name, RigidTransform3d TFW) {

      Frame frame = new Frame ();      
      frame.setName (name);
      frame.setAxisLength (0.05); // make the axes of the frame visible
      mech.addFrame (frame);
      mech.addAttachment (createFrameAttachment (frame, fem, TFW));
      return frame;
   }
}
