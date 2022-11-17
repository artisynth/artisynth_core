package artisynth.demos.test;

import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;
import maspack.util.*;
import maspack.collision.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.CollisionManager.ColliderType;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.modelbase.*;
import artisynth.core.probes.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import artisynth.core.driver.*;
import artisynth.core.renderables.ColorBar;
import maspack.render.*;
import maspack.render.color.*;

import java.awt.Color;
import java.io.*;

import javax.swing.JFrame;

public class IntersectionTest extends RootModel {

   RigidBody myBody0;
   RigidBody myBody1;
   double myDensity = 1000;

   String myDataDir = PathFinder.findSourceDir(this)+"/data";

   RigidBody createBody (
      MechModel mech, String name, PolygonalMesh mesh) {

      RigidBody body = RigidBody.createFromMesh (name, mesh, myDensity, 1.0);
      if (body != null) {
         mech.addRigidBody (body);
         RenderProps.setDrawEdges (body, true);
         RenderProps.setFaceStyle (body, FaceStyle.NONE);
         RenderProps.setShading (body, Shading.NONE);
      }
      return body;
   }


   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      CollisionManager cm = mech.getCollisionManager();
      cm.setColliderType (ColliderType.AJL_CONTOUR);

      PolygonalMesh mesh0 = null;
      PolygonalMesh mesh1 = null;

      if (false) {
         mesh0 = MeshFactory.createBox (
            5.0, 3.0, 1.0, Point3d.ZERO, 1, 1, 1); 
         mesh1 = MeshFactory.createSkylineMesh (
            3.0, 1.0, 1.0,  3, 1, "1 1");
         mesh1.transform (new RigidTransform3d (0.0, 0.0, -0.5));
      }
      
      if (false) {
         mesh0 = MeshFactory.createBox (
            3.0, 3.0, 1.0, Point3d.ZERO, 3, 3, 1); 
         mesh1 = MeshFactory.createBox (
            1.0, 1.0, 3.0, Point3d.ZERO, 1, 1, 3); 
      }
      
      if (true) {
         mesh0 = MeshFactory.createBox (
            1.0, 3.0, 1.0, Point3d.ZERO, 1, 3, 1); 
         mesh1 = MeshFactory.createBox (
            1.0, 1.0, 3.0, Point3d.ZERO, 1, 1, 3); 
      }
      
      myBody0 = createBody (mech, "body0", mesh0);
      myBody1 = createBody (mech, "body1", mesh1);

      RenderProps.setFaceStyle (myBody0, FaceStyle.NONE);
      RenderProps.setShading (myBody0, Shading.NONE);
      RenderProps.setDrawEdges (myBody0, true);

      myBody0.setDynamic (false);
      myBody1.setDynamic (false);

      CollisionBehavior behav = new CollisionBehavior (true, 0);
      behav.setMethod (CollisionBehavior.Method.INACTIVE);
      mech.setCollisionBehavior (myBody0, myBody1, behav);

      cm.setDrawIntersectionContours(true);
      RenderProps.setVisible (cm, true);
      RenderProps.setEdgeWidth (cm, 3);     
      RenderProps.setEdgeColor (cm, new Color (1f, 0, 0));
      //RenderProps.setFaceStyle (cm, FaceStyle.FRONT_AND_BACK);

      behav.setMethod (CollisionBehavior.Method.INACTIVE);

      setDefaultViewOrientation (new AxisAngle (1, 0, 0, 0));
      //RenderProps.setShading (mech, Shading.NONE);

   }
   
}
