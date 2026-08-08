/**
 * Copyright (c) 2020, by the Authors: Fabien Péan
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.demos.mech;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.DistanceConstraint;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;

public class DistanceConstraintDemo extends RootModel {
   MechModel root = new MechModel("root");

   @Override
   public void build(String[] args) throws IOException {
      super.build(args);
      addModel(root);

      root.setGravity(0, 0, -10);
      root.setIntegrator(Integrator.ConstrainedBackwardEuler);
      
      RigidBody rb1 = RigidBody.createIcosahedralSphere("rb1", 1, 1, 2);
      rb1.transformPose(new RigidTransform3d(-0.5, 0, 2));
      root.addRigidBody(rb1);

      Point p0 = new Point(new Point3d(3,0,3));
      root.addPoint(p0);
      
      Point p1 = new Point(new Point3d(0,0,2));
      root.addPoint(p1);
      root.attachPoint(p1, rb1);
      RenderProps.setSphericalPoints(p1, 0.1, Color.green);
      
      Point p2 = new Point(new Point3d(0,0,0));
      root.addPoint(p2);
      RenderProps.setSphericalPoints(p2, 0.1, Color.green);
      
      AxialSpring as = new AxialSpring(1e1, 0, 0);
      as.setPoints (p0, p1);
      as.setRestLengthFromPoints();
      root.addAxialSpring(as);
      
      DistanceConstraint c = new DistanceConstraint(rb1, p2);
      root.addConstrainer(c);
   }
}
