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
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.util.DoubleInterval;

public class ScapulothoracicJoint extends JointBase {

   public static boolean myOpenSimCompatible = true;
   
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

   public RigidTransform3d getJointTransformInChild() {
      RigidTransform3d TJC = super. getJointTransformInChild();
      TJC.mulRotZ (Math.PI/2);
      return TJC;
   }

   /**
    * Joint pose relative to parent body (OpenSim 3)
    * @return joint pose
    */
   public RigidTransform3d getJointTransformInParent() {
      RigidTransform3d TJP = super. getJointTransformInParent();
      TJP.mulRotZ (Math.PI/2);
      return TJP;
   }

   public BodyAndTransform findParentBodyAndTransform (
      ModelComponentMap componentMap) {
      BodyAndTransform bat = super.findParentBodyAndTransform(componentMap);
      bat.transform.mulRotZ (Math.PI/2);
      return bat;
   }

   public BodyAndTransform findChildBodyAndTransform (
      ModelComponentMap componentMap) {
      BodyAndTransform bat = super.findChildBodyAndTransform(componentMap);
      bat.transform.mulRotZ (Math.PI/2);
      return bat;
   }

   @Override
   public EllipsoidJoint createJoint (
      RigidBody parent, RigidTransform3d TJP, RigidBody child, RigidTransform3d TJC) {
      
      RigidBody childRB = child;
      RigidBody parentRB = parent;

      // Create ellipsoid coordinate frame (the joint's D frame). 
      // From Seth's implementation notes: Ellipsoid rotated Pi/2 w.r.t. parent
      //  (i.e. Thorax) so that abduction and elevation are positive.

      // RigidTransform3d RotZ = new RigidTransform3d();
      // RotZ.R.setRotZ (Math.PI/2);

      // System.out.println ("RotZ=\n" + RotZ.toString("%12.8f"));

      // System.out.println ("TJP=\n" + TJP.toString("%12.8f"));
      // System.out.println ("TJC=\n" + TJC.toString("%12.8f"));
      
      // TJP = new RigidTransform3d (TJP);
      // TJP.mulRotZ (Math.PI/2);
      // //TJP.mul (RotZ);
      // TJC = new RigidTransform3d (TJC);
      // //TJC.mulInverseLeft (RotZ, TJC);
      // TJC.mulRotZ (Math.PI/2);
      // //TJC.mul (RotZ);
     
      // swizzle radii defined in thorax frame to ellipsoid frame    
      double[] radii = new double[3];
      radii[0] = thoracic_ellipsoid_radii_x_y_z.y;
      radii[1] = thoracic_ellipsoid_radii_x_y_z.x;
      radii[2] = thoracic_ellipsoid_radii_x_y_z.z;

      // radii[0] = thoracic_ellipsoid_radii_x_y_z.x;
      // radii[1] = thoracic_ellipsoid_radii_x_y_z.y;
      // radii[2] = thoracic_ellipsoid_radii_x_y_z.z;
      
      // TODO: set scapula_winging axis origin/direction. For now, assuming
      // scapula_winging is the local y-axis of the joint's C frame.
     
     EllipsoidJoint joint = new EllipsoidJoint (
        childRB, TJC, parentRB, TJP, 
        radii[0], radii[1], radii[2],
        scapula_winging_axis_direction, 
        myOpenSimCompatible);
     joint.setName (getName());
     joint.setCoordinateName (0, "scapula_abduction");
     joint.setCoordinateName (1, "scapula_elevation");
     joint.setCoordinateName (2, "scapula_upward_rot");
     joint.setCoordinateName (3, "scapula_winging");

     boolean setCoords = true;
     // set joint ranges for the joint coordinate properties
     ArrayList<Coordinate> cs = getCoordinateArray ();

     DoubleInterval xrange = cs.get (0).getRange ();
     xrange.scale (RTOD);
     joint.setLongitudeRange (xrange);
     if (setCoords) {
        joint.setLongitude (RTOD*cs.get(0).getDefaultValue());
     }
     

     DoubleInterval yrange = cs.get (1).getRange ();
     yrange.scale (RTOD);
     joint.setLatitudeRange (yrange);
     if (setCoords) {
        joint.setLatitude (RTOD*cs.get(1).getDefaultValue());
     }
     
     
     DoubleInterval thetarange = cs.get (2).getRange ();
     thetarange.scale (RTOD);
     joint.setThetaRange (thetarange);
     if (setCoords) {
        joint.setTheta (RTOD*cs.get(2).getDefaultValue());
     }
     
     
     DoubleInterval phirange = cs.get (3).getRange ();
     phirange.scale (RTOD);
     joint.setPhiRange (phirange);
     if (setCoords) {
        joint.setPhi (RTOD*cs.get(3).getDefaultValue());
     }
     

     System.out.println (
        "Stored TCD\n" + joint.getStoredTCD().toString("%12.8f"));     
     
     // TODO: set default coordinate values?
     for (Coordinate c : cs) {
        c.setDefaultValue (0);
     }
     
     return joint;
   }
}
