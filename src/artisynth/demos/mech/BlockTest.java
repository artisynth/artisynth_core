package artisynth.demos.mech;

import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.util.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.modelbase.*;
import artisynth.core.probes.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import artisynth.core.driver.*;
import maspack.render.*;

import java.awt.Color;
import java.io.*;

import javax.swing.JFrame;

public class BlockTest extends RootModel {
   public static boolean debug = false;

   RigidBody myBlock;
   RigidBody myBase;
   private static boolean seeContacts = true;

   public void build (String[] args) {

      MechModel msmod = new MechModel ("msmod");
      msmod.setGravity (0, 0, -1);
      //msmod.setGravity (0, 0, 0);
      //msmod.setFrameDamping (1.0);
      //msmod.setRotaryDamping (4.0);
      //msmod.setPointDamping (1.0);
      //msmod.setPrintState ("%10.6f");

      myBlock = RigidBody.createBox ("block", 1, 0.5, 0.5, 1000);
      msmod.addRigidBody (myBlock);
      addModel (msmod);
      myBlock.setVelocity (0.0, 0, 0, 0.0, 0.5, 0.0);

      FrameMarker marker = new FrameMarker();
      msmod.addFrameMarker (marker, myBlock, new Point3d (0.5, -0.25, 0.25));
      RenderProps.setPointStyle (marker, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (marker, 0.05);

      myBase = RigidBody.createBox ("base", 2, 2, 0.25, 1000);
      myBase.setPose (new RigidTransform3d (0, 0, -1));
      myBase.setDynamic (false);
      msmod.addRigidBody (myBase);

      msmod.setDefaultCollisionBehavior (true, 0.2);
      CollisionManager cm = msmod.getCollisionManager();
      //cm.setReduceConstraints (true);
      //cm.setColliderType (CollisionManager.ColliderType.SIGNED_DISTANCE);


      if (seeContacts) {
         RenderProps.setFaceStyle (myBlock, Renderer.FaceStyle.NONE);
         RenderProps.setShading (myBlock, Renderer.Shading.NONE);
         RenderProps.setDrawEdges (myBlock, true);

         CollisionManager collisions = msmod.getCollisionManager();
         RenderProps.setVisible (collisions, true);
         RenderProps.setLineWidth (collisions, 3);      
         RenderProps.setLineColor (collisions, Color.RED);
         collisions.setDrawContactNormals (true);
      }

      ControlPanel panel = new ControlPanel();
      panel.addWidget (msmod, "integrator");
      panel.addWidget (myBlock, "position");
      panel.addWidget (msmod, "collisionManager:compliance");
      panel.addWidget (msmod, "collisionManager:damping");
      panel.addWidget (msmod, "collisionManager:acceleration");
      panel.addWidget (msmod, "penetrationTol");
      addControlPanel (panel);
      addBreakPoint (10);
      Main.getMain().arrangeControlPanels(this);
   }
}
