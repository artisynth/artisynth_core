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

public class JointLimitDemo extends RootModel {

   public static final double DTOR = Math.PI/180;
   public static final double RTOD = 180/Math.PI;

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // set up a hinge joint
      double size = 0.5;
      double density = 1000.0;
      
      mech.setInertialDamping (0.2);

      PolygonalMesh mesh = MeshFactory.createRoundedBox (
         size, size/5, size/5, 2, 1, 1, 12);
      RigidBody link0 = RigidBody.createFromMesh ("link0", mesh, density, 1.0);
      link0.setPose (
         new RigidTransform3d (0, 0, -size/2,  0, 0, 0));
      mech.addRigidBody (link0);

      HingeJoint joint0 = new HingeJoint (
         link0, null, new Point3d (0, 0, 0), new Vector3d (0, -1, 0));
      mech.addBodyConnector (joint0);
      joint0.setTheta (90.0);

      // now add joint limit
      JointCoordinateHandle handle =
         new JointCoordinateHandle (joint0, 0);
      JointLimitForce jlf = new JointLimitForce ("jlf", handle);
      jlf.setLower (DTOR*0, RTOD*100, RTOD*0.5, DTOR*10);
      mech.addForceEffector (jlf);
      // handles.add (new JointCoordinateHandle (joint1, 0));
      // handles.add (new JointCoordinateHandle (joint0, 0));
      // LinearFunction1x1 fxn = new LinearFunction1x1 (0.5, 0);
      // JointJointLimit jcc =
      //    new JointJointLimit (handles, fxn);
      // mech.addConstrainer (jcc);

       // set rendering properties
      joint0.setShaftLength (0.5*size); // draw shaft
      joint0.setShaftRadius (0.05*size);
      RenderProps.setFaceColor (joint0, Color.BLUE); // set colors
      RenderProps.setFaceColor (link0, new Color (0.5f, 1f, 1f));    
   }

}
