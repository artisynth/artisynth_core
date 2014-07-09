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

public class FallingSkull extends RootModel {

   public FallingSkull() {
      super (null);
   }

   public FallingSkull (String name) {
      this();
      setName (name);

      MechModel mech = new MechModel ("mech");
      mech.setGravity (0, 0, -9.8);

      RenderProps.setPointStyle (mech, RenderProps.PointStyle.SPHERE);
      RenderProps.setPointRadius (mech, 0.005);
      RenderProps.setPointColor (mech, Color.GRAY);

      RenderProps.setLineStyle (mech, RenderProps.LineStyle.ELLIPSOID);
      RenderProps.setLineRadius (mech, 0.003);
      RenderProps.setLineColor (mech, Color.BLUE);

      String path =
         ArtisynthPath.getSrcRelativePath (
            FallingSkull.class, "geometry/skull.obj");

      
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
      Particle p1 = new Particle (1, -0.2, 0, 0.2);

      p0.setDynamic (false);
      p1.setDynamic (false);

      FrameMarker mkr0 = new FrameMarker (0, 0.121, 0);
      //FrameMarker mkr0 = new FrameMarker (0, 0.0718, -0.0602);
      //FrameMarker mkr1 = new FrameMarker (0, 0.0718, 0.0602);

      RenderProps.setPointColor (mkr0, Color.RED);
      //RenderProps.setPointColor (mkr1, Color.RED);

      
      AxialSpring spr0 = new AxialSpring (200, 0, 0);
      AxialSpring spr1 = new AxialSpring (100, 0, 0);

      mech.addRigidBody (skull);
      mech.setFrameDamping (5);

      mech.addParticle (p0);
      //mech.addParticle (p1);

      mech.addFrameMarker (mkr0, skull, null);
      //mech.addFrameMarker (mkr1, skull, null);

      mech.attachAxialSpring (mkr0, p0, spr0);
      //mech.attachAxialSpring (mkr1, p1, spr1);



      addModel (mech);
   }

   public void attach (DriverInterface driver) {
      setViewerCenter (new Point3d (.0974827, 0.0178521, 0.0));
      setViewerEye (new Point3d (.24, -0.562918, 0.011512));
      // System.out.println();
   }
   

}
