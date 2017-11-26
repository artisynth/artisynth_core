package artisynth.demos.tutorial;

import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.util.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.CollisionManager.*;
import artisynth.core.modelbase.*;
import artisynth.core.probes.WayPoint;
import artisynth.core.driver.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import maspack.render.*;

import java.awt.Color;
import java.io.*;

import javax.swing.*;

/**
 * Demo of jointed rigid bodies colliding with a base plate
 */
public class JointedCollide extends RigidBodyJoint {

   public void build (String[] args) {

      super.build (args);

      bodyB.setDynamic (true);  // allow bodyB to fall freely

      // create and add the inclined plane
      RigidBody base = RigidBody.createBox ("base", 25, 25, 2, 0.2);
      base.setPose (new RigidTransform3d (5, 0, 0, 0, 1, 0, -Math.PI/8));
      base.setDynamic (false);
      mech.addRigidBody (base);

       // turn on collisions
      CollisionBehavior behav = new CollisionBehavior (true, 0);

      //behav.setColliderType (ColliderType.SIGNED_DISTANCE);
      CollisionManager cm = mech.getCollisionManager();
      cm.setColliderType (ColliderType.SIGNED_DISTANCE);
     
      mech.setDefaultCollisionBehavior (behav);
      behav.setMethod (CollisionBehavior.Method.VERTEX_PENETRATION); 

     // turn on collisions
      //mech.setDefaultCollisionBehavior (true, 0.20);
      mech.setCollisionBehavior (bodyA, bodyB, false);
   }

}
