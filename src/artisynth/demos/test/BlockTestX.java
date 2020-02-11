package artisynth.demos.test;

import java.awt.Color;

import artisynth.core.driver.Main;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.CollisionBehavior;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.ContactForceBehavior;
import artisynth.core.mechmodels.ContactPoint;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.util.InternalErrorException;

public class BlockTestX extends RootModel {
   public static boolean debug = false;

   RigidBody myBlock;
   RigidBody myBase;
   private static boolean seeContacts = true;

   private class ContactForce implements ContactForceBehavior {
      public void computeResponse (
         double[] fres, double dist, ContactPoint cpnt1, ContactPoint cpnt2, 
         Vector3d nrml, double regionArea) {

         double c = 0.001;

         fres[0] = dist/c;
         fres[1] = c;
         fres[2] = 0.01;
      }
      
      public ContactForce clone() throws CloneNotSupportedException {
         return (ContactForce)super.clone();
      }
   }

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

      if (seeContacts) {
         RenderProps.setFaceStyle (myBlock, Renderer.FaceStyle.NONE);
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
      panel.addWidget (msmod, "collisionManager:collisionCompliance");
      panel.addWidget (msmod, "collisionManager:collisionDamping");
      addControlPanel (panel);
      addBreakPoint (10);
      Main.getMain().arrangeControlPanels(this);

      CollisionManager cm = msmod.getCollisionManager();
      //cm.setForceBehavior (myBase, myBlock, new ContactForce());
      CollisionBehavior behav = msmod.setCollisionBehavior (
         myBase, myBlock, true, 0.2);
      behav.setForceBehavior (new ContactForce());
      //cm.setForceBehavior (new ContactForce());
      
   }
}
