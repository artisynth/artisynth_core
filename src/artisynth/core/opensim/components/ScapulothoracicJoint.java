/**
 * Copyright (c) 2023, by the Authors: John E Lloyd (UBC), Ian Stavness (USask)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 * 
 * Implementation of Ajay Seth's Scapulothoracic Joint from OpenSim: 
 * Seth A, Matias R, Veloso AP and Delp SL. A biomechanical model of the 
 * scapulothoracic joint to accurately capture scapular kinematics during 
 * shoulder movements. PLoS ONE. (2015)
 */
package artisynth.core.opensim.components;

import java.util.ArrayList;

import artisynth.core.mechmodels.EllipsoidJoint;
import artisynth.core.mechmodels.RigidBody;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.util.DoubleInterval;

public class ScapulothoracicJoint extends JointBase {
   
   protected static final double RTOD = 180.0/Math.PI; 
   protected static final double DTOR = Math.PI/180.0; 

   // ellipsoid representing the thorax surface
   Vector3d thoracic_ellipsoid_radii_x_y_z = new Vector3d();
 
   // winging axis origin in the scapula plane (tangent to the thoracic surface).
   Vector2d scapula_winging_axis_origin = new Vector2d(); 
   
   // winging axis orientation (in radians) in the scapula plane (tangent to the thoracic surface).
   double scapula_winging_axis_direction; 

   public Vector3d getThoracicEllipsoidRadii () {
      return thoracic_ellipsoid_radii_x_y_z;
   }

   public void setThoracicEllipsoidRadii (
      Vector3d thoracic_ellipsoid_radii_x_y_z) {
      this.thoracic_ellipsoid_radii_x_y_z = thoracic_ellipsoid_radii_x_y_z;
   }

   public Vector2d getScapulaWingingAxisOrigin () {
      return scapula_winging_axis_origin;
   }

   public void setScapulaWingingAxisOrigin (
      double x, double y) {
      this.scapula_winging_axis_origin.set (x, y);
   }

   public double getScapulaWingingAxisDirection () {
      return scapula_winging_axis_direction;
   }

   public void setScapulaWingingAxisDirection (
      double scapula_winging_axis_direction) {
      this.scapula_winging_axis_direction = scapula_winging_axis_direction;
   }

   @Override
   public EllipsoidJoint createJoint (
      RigidBody parent, RigidTransform3d TJP, RigidBody child, RigidTransform3d TJC) {
      
      RigidBody childRB = child;
      RigidBody parentRB = parent;

      // Create ellipsoid coordinate frame (the joint's D frame). 
      // From Seth's implementation notes: Ellipsoid rotated Pi/2 w.r.t. parent
      //  (i.e. Thorax) so that abduction and elevation are positive.

      TJP.mulRotZ (Math.PI/2);
     
      // swizzle radii defined in thorax frame to ellipsoid frame    
      double[] radii = new double[3];
      radii[0] = thoracic_ellipsoid_radii_x_y_z.y;
      radii[1] = thoracic_ellipsoid_radii_x_y_z.x;
      radii[2] = thoracic_ellipsoid_radii_x_y_z.z;
      
      // TODO: set scapula_winging axis origin/direction. For now, assuming
      // scapula_winging is the local y-axis of the joint's C frame.
     
     EllipsoidJoint joint = new EllipsoidJoint (childRB, TJC, parentRB, TJP, radii[0], radii[1], radii[2]);

     // set joint ranges for the joint coordinate properties
     ArrayList<Coordinate> cs = getCoordinateArray ();

     DoubleInterval xrange = cs.get (0).getRange ();
     xrange.scale (RTOD);
     joint.setXRange (xrange);

     DoubleInterval yrange = cs.get (1).getRange ();
     yrange.scale (RTOD);
     joint.setYRange (yrange);
     
     DoubleInterval thetarange = cs.get (2).getRange ();
     thetarange.scale (RTOD);
     joint.setThetaRange (thetarange);
     
     DoubleInterval phirange = cs.get (3).getRange ();
     phirange.scale (RTOD);
     joint.setPhiRange (phirange);
     
     // TODO: set default coordinate values?
     for (Coordinate c : cs) {
        c.setDefaultValue (0);
     }
     
     return joint;
   }
}
