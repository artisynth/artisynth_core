package artisynth.demos.mech;

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
import maspack.function.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class ConditionalMarkerDemo extends RootModel {

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      boolean wrapping = false;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-wrapping")) {
            wrapping = true;
         }
         else {
            System.out.println (
               "WARNING: unknown model argument "+args[i]);
         }
      }

      // set up a double pendulum
      double size = 0.5;
      double density = 1000.0;
      double cos45 = Math.sqrt(2)/2;
      
      mech.setInertialDamping (0.2);

      PolygonalMesh mesh = MeshFactory.createRoundedBox (
         size, size/3, size/3, 2, 1, 1, 12);
      RigidBody link0 = RigidBody.createFromMesh ("link0", mesh, density, 1.0);
      link0.setPose (
         new RigidTransform3d (cos45*size/2, 0, 0,  0, -Math.PI/4, 0));
      link0.setDynamic (false);
      mech.addRigidBody (link0);

      HingeJoint joint0 = new HingeJoint (
         link0, null, new Point3d (0, 0, cos45*size/2), new Vector3d (0, -1, 0));
      mech.addBodyConnector (joint0);

      mesh = MeshFactory.createRoundedBox (
         size, size/3, size/4, 2, 1, 1, 12);
      RigidBody link1 = RigidBody.createFromMesh ("link1", mesh, density, 1.0);
      link1.setPose (
         new RigidTransform3d (
            size*(cos45+0.5), 0, -cos45*size/2,  0, Math.PI/2, 0));
      mech.addRigidBody (link1);

      HingeJoint joint1 = new HingeJoint (
         link1, link0,
         new Point3d (cos45*size, 0, -cos45*size/2), new Vector3d (0, -1, 0));
      mech.addBodyConnector (joint1);

      // add main markers
      FrameMarker mkr0 = mech.addFrameMarker (
         link0, new Point3d (size/6, 0, 0.5*size));
      FrameMarker mkr1 = mech.addFrameMarker (
         link1, new Point3d (-size/6, 0, 0.5*size));

      // add wrap cylinder
      RigidCylinder cylinder = null;
      if (wrapping) {
         cylinder = new RigidCylinder ("wrap", size/5.9, size, 1000.0);
         cylinder.setDynamic (false);
         cylinder.setPose (
            new RigidTransform3d (
               cos45*size, 0, -cos45*size/2, 0, 0, Math.PI/2));
         mech.addRigidBody (cylinder);
      }
      
      link0.getDistanceGridComp().setMaxResolution(40);
      link1.getDistanceGridComp().setMaxResolution(40);

      // conditional points
      JointConditionalMarker[] cmkrs = new JointConditionalMarker[4];
      for (int i=0; i<cmkrs.length; i++) {
         double ang = i*Math.PI/4;
         double cos = Math.cos (ang);
         double sin = Math.sin (ang);
         cmkrs[i] = new JointConditionalMarker (
            link0,
            new Point3d (cos*size/6, 0, -size*(0.5+sin/6)),
            new JointCoordinateHandle (joint1, 0),
            new DoubleInterval (-240, -45-Math.toDegrees(ang)));
         mech.addFrameMarker (cmkrs[i]);
      }

      // create spring

      MultiPointSpring spr = new MultiPointSpring ("spr", 20, 2, 0);
      spr.setWrapKnotDensity (100);
      spr.addPoint (mkr0);
      spr.addPoint (cmkrs[0]);
      spr.addPoint (cmkrs[1]);
      spr.addPoint (cmkrs[2]);
      spr.addPoint (mkr1);
      if (wrapping) {
         spr.setAllSegmentsWrappable(0);
         spr.addWrappable (cylinder);
      }

      //spr.addWrappable (link0);
      //spr.addWrappable (link1);
      mech.addMultiPointSpring (spr);

      // set rendering properties
      joint0.setShaftLength (0.5*size); // draw shaft
      joint0.setShaftRadius (0.05*size);
      RenderProps.setFaceColor (joint0, Color.BLUE); // set colors
      joint1.setShaftLength (0.5*size); // draw shaft
      joint1.setShaftRadius (0.05*size);
      RenderProps.setFaceColor (joint1, Color.BLUE); // set colors
      RenderProps.setFaceColor (link0, new Color (0.8f, 0.8f, 1f));    
      RenderProps.setFaceColor (link1, new Color (0.8f, 0.8f, 1f));  

      RenderProps.setSphericalPoints (mech, 0.02, Color.WHITE);
      RenderProps.setCylindricalLines (mech, 0.01, Color.RED);
   }

}
