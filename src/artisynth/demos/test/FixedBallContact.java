package artisynth.demos.test;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

/**
 * Demo showing an FEM falling onto an inclinded plate, in which FEM nodes are
 * attached to the plate as soon as they make contact with it.
 */
public class FixedBallContact extends RootModel {

   // References to the FEM model, MechModel and plate are needed by the
   // overrides of the advance and set/getState methods.
   FemModel3d myFem;
   MechModel myMech;
   RigidBody myPlate;

   double inclineAng = Math.toRadians(30); // incline angle
   
   public void build (String[] args) {
      myMech = new MechModel ("mech");
      addModel (myMech);
      
      // create a FEM sphere
      myFem = FemFactory.createIcosahedralSphere (
         null, 1.0, /*ndivs=*/2, 1.0);
      myFem.transformGeometry (new RigidTransform3d (0, 0, 1.5));
      myMech.addModel (myFem);

      // create the inclined plane onto which the ball will fall
      myPlate = RigidBody.createBox (null, 3.0, 3.0, 0.25, 1000.0);
      myPlate.setPose (
         new RigidTransform3d (0, 0, -0.125, 0, -inclineAng, 0));
      myPlate.setDynamic (false);
      myMech.addRigidBody (myPlate);

      // render properties: make nodes visibe as spheres
      RenderProps.setSphericalPoints (myFem, 0.02, Color.GREEN);

      addWayPoint (0.5);
      addWayPoint (1.0);
   }

   /**
    * Override the root model state to be a composite state consisting of two
    * sub-states: (1) the underlying root model state, and (2) a NumericState
    * containing information about which FEM nodes are attached to the plate.
    *
    * <p>The attachment state consists of:
    *
    * <p>an integer giving the number of attachments
    *
    * <p>for each attachment,the index of the node, and its plate relative
    * location
    */
   @Override
   public CompositeState createState (ComponentState prevState) {
      CompositeState state = new CompositeState();
      state.addState (super.createState (prevState)); // underlying root state
      state.addState (new NumericState()); // attachment state
      return state;
   }

   /**
    * Override the root model's getInitialState() method to get both the
    * initial root state and the initial attachment state (which has zero
    * attachments).
    */
   @Override
   public void getInitialState (
      ComponentState state, ComponentState prevstate) {
      // unpack root and attachment states
      CompositeState cs = (CompositeState)state;
      ComponentState rootState = cs.getState(0);
      NumericState astate = (NumericState)cs.getState(1);

      // call getInitialState() for underlying root model
      if (prevstate == null) {
         super.getInitialState (rootState, null);
      }
      else {
         super.getInitialState (
            rootState, ((CompositeState)prevstate).getState(0));
      }
      // set attachment state to specify no attachments:
      astate.zput (0);
   }

   /**
    * Override the root model's getState() method to get both the root state
    * and the current attachment state.
    */
   @Override
   public void getState (ComponentState state) {
      // unpack root and attachment states
      CompositeState cs = (CompositeState)state;
      ComponentState rootState = cs.getState(0);
      NumericState astate = (NumericState)cs.getState(1);

      super.getState (rootState); // get root state

      // seacrh all nodes and find those attached to the plate:
      ArrayList<FemNode3d> attached = new ArrayList<>();      
      for (DynamicAttachment a : myMech.attachments()) {
         if (a.getSlave() instanceof FemNode3d) {
            FemNode3d node = (FemNode3d)a.getSlave();
            if (node.getGrandParent() == myFem) {
               attached.add (node);
            }
         }
      }
      astate.zput (attached.size()); // save number of attachments
      for (FemNode3d node : attached) {
         // save info for each attachment
         astate.zput (node.getNumber());
         // save location for exact numerical repeatability
         PointFrameAttachment a = (PointFrameAttachment)node.getAttachment();
         astate.dput (a.getLocation());
      }
   }

   /**
    * Override the root model's setState() method to set both the root state
    * and restore node attachments as per the attachment state.
    */
   @Override
   public void setState (ComponentState state) {
      // unpack root and attachment states
      CompositeState cs = (CompositeState)state;
      ComponentState rootState = cs.getState(0);
      NumericState astate = (NumericState)cs.getState(1);

      super.setState (rootState); // set root state

      astate.resetOffsets(); // rewind attachment state to beginning

      // first detach all nodes that are connected to the plate
      for (FemNode3d node : myFem.getNodes()) {
         if (node.isAttached() &&
             node.getAttachment().getMasters()[0] == myPlate) {
            myMech.detachPoint (node);
         }
      }
      // then (re)attach all nodes specified in the attachment state
      int nattached = astate.zget(); // get number of attachments
      Point3d loc = new Point3d();
      for (int i=0; i<nattached; i++) {
         // restore each attachment
         FemNode3d node = myFem.getNodeByNumber(astate.zget());
         astate.dget (loc);
         myMech.addAttachment (new PointFrameAttachment (myPlate, node, loc));
      }
   }

   /**
    * Override of the advance method that attachs FEM nodes to the plate
    * when the make contact with it.
    */
   @Override
   public StepAdjustment advance (double t0, double t1, int flags) {
      // when a node of the FEM falls below the plate plane, attach ot
      for (FemNode3d node : myFem.getNodes()) {
         Point3d pos = node.getPosition();
         if (pos.z < Math.sin(inclineAng)*pos.x && !node.isAttached()) {
            myMech.attachPoint (node, myPlate);
         }
      }
      return super.advance (t0, t1, flags);
   }

}
