// Isaac McKay - 2024 - Much of the code is copied from JointLimitDemo.java

package artisynth.demos.mech;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.CollisionBehavior.Method;
import artisynth.core.mechmodels.CollisionManager.ColliderType;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.materials.*;
import artisynth.core.probes.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.function.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

final class NewtonBall extends RigidBody {

   Particle anchor1;
   Particle anchor2;
   
   public NewtonBall(
      double length, double density, Vector3d v,
      MechModel mech, boolean isRaised) {
      super();
      double anchorSpread = length/2;
      double relSize = length/5;
      super.addMesh (MeshFactory.createSphere (relSize, 40, 20), true, true);
      super.setDensity(density);
      super.transformGeometry (AffineTransform3d.createScaling (length));
      if (isRaised) {
         this.transformPose (new RigidTransform3d(v.x + length, v.y, v.z
            + length, 0, -Math.toRadians (90), 0));
      } else {
         this.transformPose (new RigidTransform3d(v.x, v.y, v.z, 0, 0, 0));
      }
      

      FrameMarker socket = mech.addFrameMarker (this, new Point3d(0, 0, relSize));
      anchor1 = new Particle (
         0, new Point3d (v.x, v.y + anchorSpread, v.z + length));
      mech.addParticle (anchor1);
      anchor1.setDynamic (false);
      anchor2 = new Particle (
         0, new Point3d (v.x, v.y - anchorSpread, v.z + length));
      mech.addParticle (anchor2);
      anchor2.setDynamic (false);

      AxialSpring s1 = new AxialSpring(null, anchor1.distance (socket));
      AxialSpring s2 = new AxialSpring(null, anchor2.distance (socket));
      s1.setMaterial (new LinearAxialMaterial(1000000, density * 5));
      s2.setMaterial (new LinearAxialMaterial(1000000, density * 5));

      mech.attachAxialSpring (anchor1, socket, s1);
      mech.attachAxialSpring (anchor2, socket, s2);

      RenderProps.setShininess(this, 100);
      RenderProps.setShading(this, Shading.SMOOTH);
      
      mech.addRigidBody (this);
   }
   
   public Particle getAnchor1() {
      return anchor1;
   }
   
   public Particle getAnchor2() {
      return anchor2;
   }
}

final class Cradle {
   private LinkedList<NewtonBall> balls;
   private MechModel mech;
   private Particle topFrontLeft;
   private Particle topFrontRight;
   private Particle bottomFrontLeft;
   private Particle bottomFrontRight;
   private Particle topBackLeft;
   private Particle topBackRight;
   private Particle bottomBackLeft;
   private Particle bottomBackRight;
   
   public Cradle(int numBalls, MechModel m) {
      mech = m;
      balls = new LinkedList<>();
      double acc = 0.0;
      double density = 1000; // arbitrary
      double size = 1;
      balls.add (new NewtonBall(size, density,
         new Vector3d(acc, 0, 0), m, true));
      for (int i = 0; i < numBalls - 1; i++) {
         acc -= 2 * size/5;
         balls.add (new NewtonBall(size, density,
            new Vector3d(acc, 0, 0), m, false));
      }
      
      // making visual representation of the frame that
      // the balls are anchored to
      topFrontLeft = balls.getFirst ().getAnchor2 ();
      topFrontRight = balls.getFirst ().getAnchor1 ();
      bottomFrontLeft = new Particle(null, 0.0,
         topFrontLeft.getPosition ().x,
         topFrontLeft.getPosition ().y,
         topFrontLeft.getPosition ().z - 1.5);
      bottomFrontLeft.setDynamic (false);
      bottomFrontLeft.setRenderProps (mech.getRenderProps ());
      bottomFrontRight = new Particle(null, 0.0,
         topFrontRight.getPosition ().x,
         topFrontRight.getPosition ().y,
         topFrontRight.getPosition ().z - 1.5);
      bottomFrontRight.setDynamic (false);
      bottomFrontRight.setRenderProps (mech.getRenderProps ());
      mech.add (bottomFrontLeft);
      mech.add (bottomFrontRight);
      topBackLeft = balls.getLast ().getAnchor2 ();
      topBackRight = balls.getLast ().getAnchor1 ();
      bottomBackLeft = new Particle(null, 0.0,
         topBackLeft.getPosition ().x,
         topBackLeft.getPosition ().y,
         topBackLeft.getPosition ().z - 1.5);
      bottomBackLeft.setDynamic (false);
      bottomBackLeft.setRenderProps (mech.getRenderProps ());
      bottomBackRight = new Particle(null, 0.0,
         topBackRight.getPosition ().x,
         topBackRight.getPosition ().y,
         topBackRight.getPosition ().z - 1.5);
      bottomBackRight.setDynamic (false);
      bottomBackRight.setRenderProps (mech.getRenderProps ());
      mech.add (bottomBackLeft);
      mech.add (bottomBackRight);
      
      // the following is just boilerplate for drawing the cradle frame
      LinkedList<AxialSpring> frame = new LinkedList<>();
      frame.add (new AxialSpring(null,
         topFrontLeft.getPosition ().x - topBackLeft.getPosition ().x));
      frame.get (0).setPoints (topFrontLeft, topBackLeft);
      frame.add (new AxialSpring(null,
         topFrontRight.getPosition ().x - topBackRight.getPosition().x ));
      frame.get (1).setPoints (topFrontRight, topBackRight);
      
      frame.add (new AxialSpring(null,
         topFrontLeft.getPosition ().z - bottomFrontLeft.getPosition ().z));
      frame.get (2).setPoints (topFrontLeft, bottomFrontLeft);
      frame.add (new AxialSpring(null,
         topFrontRight.getPosition ().z - bottomFrontRight.getPosition().z));
      frame.get (3).setPoints (topFrontRight, bottomFrontRight);
      
      frame.add (new AxialSpring(null,
         topBackLeft.getPosition ().z - bottomBackLeft.getPosition ().z));
      frame.get (4).setPoints (topBackLeft, bottomBackLeft);
      frame.add (new AxialSpring(null,
         topBackRight.getPosition ().z - bottomBackRight.getPosition().z));
      frame.get (5).setPoints (topBackRight, bottomBackRight);
      
      frame.add (new AxialSpring(null,
         bottomFrontRight.getPosition ().y -
         bottomFrontLeft.getPosition ().y));
      frame.get (6).setPoints (bottomFrontRight, bottomFrontLeft);
      frame.add (new AxialSpring(null,
         bottomBackRight.getPosition ().y -
         bottomBackLeft.getPosition().x ));
      frame.get (7).setPoints (bottomBackRight, bottomBackLeft);  
      
      for (AxialSpring a: frame) {
         mech.add (a);
      }
   }
}

public class NewtonsCradle extends RootModel {

   public static final double DTOR = Math.PI/180;
   public static final double RTOD = 180/Math.PI;

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
//      mech.getCollisionManager ().setCompliance (0.0005);
      CollisionBehavior behav = new CollisionBehavior(true, 0.1);
      behav.setMethod (Method.VERTEX_PENETRATION);
      LinearElasticContact efc =
         new LinearElasticContact(1000000.0, 0.4, 0.5, 0.1);
      behav.setForceBehavior (efc);
      mech.setDefaultCollisionBehavior (behav);
      
      //mech.setIntegrator (Integrator.RungeKutta4);
      mech.setIntegrator (Integrator.ForwardEuler);
      addModel (mech);

      mech.setRotaryDamping (3.0);
      mech.setInertialDamping (0.25);
      setMaxStepSize (0.001);
//      mech.getCollisionManager ().setCompliance (0.0005);

      Color darkGreen = new Color (0.3f, 0.4f, 0.3f);
      Color grayBlack = new Color (0.3f, 0.3f, 0.3f);
      Color darkRed = new Color (0.75f, 0f, 0f);

      RenderProps.setCylindricalLines(mech, 0.053, darkGreen);
      RenderProps.setCylindricalLines(mech.axialSprings(), 0.02, grayBlack);
      RenderProps.setShininess(mech.axialSprings(), 200);

      RenderProps.setSphericalPoints (mech, 0.05, darkGreen);
      RenderProps.setPointColor (mech.frameMarkers(), grayBlack);
      RenderProps.setShininess (mech.frameMarkers(), 200);
      RenderProps.setFaceColor (mech, darkRed);
      RenderProps.setSpecular (mech, new Color (1f, 1f, 0.8f));
      
      Cradle c = new Cradle(5, mech);
   }
}
