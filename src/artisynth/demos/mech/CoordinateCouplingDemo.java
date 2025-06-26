package artisynth.demos.mech;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.gui.*;
import artisynth.core.probes.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.function.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class CoordinateCouplingDemo extends RootModel {

   HingeJoint myJoint0;
   HingeJoint myJoint1;
   CoordinateSetter myCsetter;

   public static PropertyList myProps =
      new PropertyList (CoordinateCouplingDemo.class, RootModel.class);

   static {
      myProps.add ("theta0", "angle for joint 0", 0);
      myProps.add ("theta1", "angle for joint 1", 0);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getTheta0() {
      if (myJoint0 != null) {
         return myJoint0.getTheta();
      }
      else {
         return 0;
      }
   }

   public void setTheta0 (double the) {
      myCsetter.setCoordinate (myJoint0, 0, Math.toRadians(the));
   }

   public double getTheta1() {
      if (myJoint1 != null) {
         return myJoint1.getTheta();
      }
      else {
         return 0;
      }
   }

   public void setTheta1 (double the) {
      myCsetter.setCoordinate (myJoint1, 0, Math.toRadians(the));
   }

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

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
      mech.addRigidBody (link0);

      myJoint0 = new HingeJoint (
         link0, null, new Point3d (0, 0, cos45*size/2), new Vector3d (0, -1, 0));
      mech.addBodyConnector (myJoint0);

      mesh = MeshFactory.createRoundedBox (
         size, size/3, size/4, 2, 1, 1, 12);
      RigidBody link1 = RigidBody.createFromMesh ("link1", mesh, density, 1.0);
      link1.setPose (
         new RigidTransform3d (
            size*(cos45+0.5), 0, -cos45*size/2,  0, Math.PI/2, 0));
      mech.addRigidBody (link1);

      myJoint1 = new HingeJoint (
         link1, link0,
         new Point3d (cos45*size, 0, -cos45*size/2), new Vector3d (0, -1, 0));
      mech.addBodyConnector (myJoint1);

      // now add coordinate coupling
      ArrayList<JointCoordinateHandle> handles =
         new ArrayList<JointCoordinateHandle>();
      handles.add (new JointCoordinateHandle (myJoint1, 0));
      handles.add (new JointCoordinateHandle (myJoint0, 0));
      LinearFunction1x1 fxn = new LinearFunction1x1 (0.5, 0);
      JointCoordinateCoupling jcc =
         new JointCoordinateCoupling (handles, fxn);
      mech.addConstrainer (jcc);

      // coordinate setter for setting joint angles
      myCsetter = new CoordinateSetter(mech);

      // set rendering properties
      myJoint0.setShaftLength (0.5*size); // draw shaft
      myJoint0.setShaftRadius (0.05*size);
      RenderProps.setFaceColor (myJoint0, Color.BLUE); // set colors
      myJoint1.setShaftLength (0.5*size); // draw shaft
      myJoint1.setShaftRadius (0.05*size);
      RenderProps.setFaceColor (myJoint1, Color.BLUE); // set colors
      RenderProps.setFaceColor (link0, new Color (0.5f, 1f, 1f));    
      RenderProps.setFaceColor (link1, new Color (0.5f, 1f, 1f));    

      ControlPanel panel = new ControlPanel();
      panel.addWidget (this, "theta0", -180, 180);
      panel.addWidget (this, "theta1", -180, 180);
      addControlPanel (panel);
   }

   public void postscanInitialize() {
      MechModel mech = (MechModel)findComponent ("models/mech");
      myJoint0 = (HingeJoint)mech.bodyConnectors().get(0);
      myJoint1 = (HingeJoint)mech.bodyConnectors().get(1);
      myCsetter = new CoordinateSetter(mech);
   }

}
