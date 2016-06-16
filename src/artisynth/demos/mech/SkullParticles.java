package artisynth.demos.mech;

import maspack.render.*;
import maspack.matrix.*;
import maspack.geometry.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import artisynth.core.driver.*;

import java.awt.Color;
import java.io.*;

//import artisynth.core.mechmodels.DynamicMechComponent.Activity;

public class SkullParticles extends RootModel {

   public void build (String[] args) {

      MechModel mech = new MechModel ("mech");
      mech.setGravity (0, 0, -9.8);

      RenderProps.setPointStyle (mech, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (mech, 0.01);
      RenderProps.setPointColor (mech, Color.GRAY);

      RenderProps.setLineStyle (mech, Renderer.LineStyle.SPINDLE);
      RenderProps.setLineRadius (mech, 0.003);
      RenderProps.setLineColor (mech, Color.BLUE);

      String path =
         ArtisynthPath.getSrcRelativePath (
            SkullParticles.class, "geometry/skull.obj");

      PolygonalMesh mesh = null;
      try {
         mesh = new PolygonalMesh (new File(path));
      }
      catch (Exception e) {
         e.printStackTrace(); 
         System.exit(1); 
      }
      RigidBody skull = RigidBody.createFromMesh (
         "skull", mesh, /*density=*/1800, /*scale=*/0.001);
      skull.setPose (new RigidTransform3d (0, 0, 0.05, -Math.PI/2, 0, Math.PI/2));

      Particle p0 = new Particle (1, 0.1, 0, 0.2);
      Particle p1 = new Particle (1, .15, 0, 0);
      Particle p2 = new Particle (1, -.15, 0, 0);

      p0.setDynamic (false);
      //p1.setDynamic (false);

      FrameMarker mkr0 = new FrameMarker (0, 0.121, 0);
      FrameMarker mkr1 = new FrameMarker (0, 0, -0.05);
      FrameMarker mkr2 = new FrameMarker (0, 0, 0.05);
      //FrameMarker mkr0 = new FrameMarker (0, 0.0718, -0.0602);
      //FrameMarker mkr1 = new FrameMarker (0, 0.0718, 0.0602);

      //RenderProps.setPointColor (mkr0, Color.RED);
      //RenderProps.setPointColor (mkr1, Color.RED);
      //RenderProps.setPointColor (mkr2, Color.RED);
      //RenderProps.setPointColor (mkr1, Color.RED);
      
      AxialSpring spr0 = new AxialSpring (500, 0, 0);
      AxialSpring spr1 = new AxialSpring (100, 0, 0);
      AxialSpring spr2 = new AxialSpring (100, 0, 0);

      mech.addRigidBody (skull);
      mech.setFrameDamping (2);
      mech.setPointDamping (1);

      mech.addParticle (p0);
      mech.addParticle (p1);
      mech.addParticle (p2);
      //mech.addParticle (p1);

      mech.addFrameMarker (mkr0, skull, null);
      mech.addFrameMarker (mkr1, skull, null);
      mech.addFrameMarker (mkr2, skull, null);
      RenderProps.setPointRadius (mkr0, 0.001);
      RenderProps.setPointRadius (mkr1, 0.001);
      RenderProps.setPointRadius (mkr2, 0.001);
      //mech.addFrameMarker (mkr1, skull, null);

      mech.attachAxialSpring (mkr0, p0, spr0);
      mech.attachAxialSpring (mkr1, p1, spr1);
      mech.attachAxialSpring (mkr2, p2, spr2);
      //mech.attachAxialSpring (mkr1, p1, spr1);

      addModel (mech);
   }

   public void attach (DriverInterface driver) {
      setViewerCenter (new Point3d (0.0658571, 0.00139115, 0.028269));
      setViewerEye (new Point3d (0.0658571, -0.699483, 0.028269));
      // System.out.println();
   }
   

}
