package artisynth.core.mechmodels;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Deque;
import java.util.ArrayList;
import java.util.List;

import artisynth.core.util.ScanToken;
import artisynth.core.util.ObjectToken;
import artisynth.core.util.StringToken;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import maspack.matrix.Vector3d;
import maspack.util.ReaderTokenizer;
import maspack.util.NumberFormat;


import artisynth.core.modelbase.*;
import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.spatialmotion.RigidBodyConstraint.MotionType;
import maspack.properties.*;

/**
 * Applies a restoring force to a joint coordinate when its value falls outside
 * a specfied range. This is used to implement ``soft'' limits on joint
 * coordinates.
 *
 * <p>The applied force is nominally linear. If {@code x} and {@code v} are the
 * coordinate value and speed, then for an upper limit {@code ul} the force is
 * nominally given by
 * <pre>
 * f = - Ku (x - ul) - Du v
 * </pre>
 * where {@code Ku} and {@code Du} are the stiffness and damping coefficients
 * associated with the upper limit, while for a lower limit {@code ll} it is
 * <pre>
 * f = - Kl (ll - x) - Dl v
 * </pre>
 * where {@code Kl} and {@code Dl} are the coefficients for the lower
 * limit. However, each limit can also be associated with a transition region
 * {@code tau}. If {@code tau > 0}, then the restoring force is applied more
 * gradually in the intervals {@code [ul, ul+tau]} (for the upper limit) and
 * {@code [ll-tau, ll]} (for the lower limit).
 */
public class JointLimitForce extends ModelComponentBase
   implements ForceComponent {

   private static final double INF = Double.POSITIVE_INFINITY;

   private static double RTOD = 180/Math.PI;
   private static double DTOR = Math.PI/180;
   
   JointCoordinateHandle myCoord;

   public static final double DEFAULT_LIMIT = INF;
   public static final double DEFAULT_STIFFNESS = 0;
   public static final double DEFAULT_DAMPING = 0;
   public static final double DEFAULT_TRANSITION = 0;

   double myUpperLimit = DEFAULT_LIMIT;
   double myUpperStiffness = DEFAULT_STIFFNESS;
   double myUpperDamping = DEFAULT_DAMPING;
   double myUpperTransition = DEFAULT_TRANSITION;

   double myLowerLimit = -DEFAULT_LIMIT;
   double myLowerStiffness = DEFAULT_STIFFNESS;
   double myLowerDamping = DEFAULT_DAMPING;
   double myLowerTransition = DEFAULT_TRANSITION;
   
   public static PropertyList myProps =
      new PropertyList (JointLimitForce.class, ModelComponentBase.class);

   static {
      myProps.add (
         "upperLimit",
         "coordinate upper limit",
         DEFAULT_LIMIT);
      myProps.add (
         "upperStiffness",
         "upper limit stiffness",
         DEFAULT_STIFFNESS);
      myProps.add (
         "upperDamping",
         "upper limit damping (force/speed)",
         DEFAULT_DAMPING);
      myProps.add (
         "upperTransition",
         "upper limit transition region",
         DEFAULT_TRANSITION);
      myProps.add (
         "lowerLimit",
         "coordinate lower limit",
         -DEFAULT_LIMIT);
      myProps.add (
         "lowerStiffness",
         "lower limit stiffness)",
         DEFAULT_STIFFNESS);
      myProps.add (
         "lowerDamping",
         "lower limit damping (force/speed)",
         DEFAULT_DAMPING);
      myProps.add (
         "lowerTransition",
         "lower limit transition region",
         DEFAULT_TRANSITION);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Sets the upper limit for the joint coordinate, beyond which a restoring
    * force will be applied. The default value is {@code infinity} (no upper
    * limit).
    *
    * @param ul new upper coordinate limit,
    */
   public void setUpperLimit (double ul) {
      myUpperLimit = ul;
   }
   
   /**
    * Queries the upper limit for the joint coordinate.
    *
    * @return upper coordinate limit,
    */
   public double getUpperLimit () {
      return myUpperLimit;
   }
   
   /**
    * Sets the stiffness for the upper limit restoring force. The default value
    * is 0 (no stiffness force).
    *
    * @param k new upper limit stiffness,
    */
   public void setUpperStiffness (double k) {
      myUpperStiffness = k;
   }
   
   /**
    * Queries the stiffness for the upper limit restoring force.
    *
    * @return upper limit stiffness,
    */
   public double getUpperStiffness () {
      return myUpperStiffness;
   }

   /**
    * Sets the damping parameter for the upper limit restoring force. The
    * default value is 0 (no damping force).
    *
    * @param d new upper limit damping
    */
   public void setUpperDamping (double d) {
      myUpperDamping = d;
   }
   
   /**
    * Queries the damping parameter for the upper limit restoring force.
    *
    * @return upper limit damping
    */
   public double getUpperDamping () {
      return myUpperDamping;
   }
   
   /**
    * Sets the transition region size for the upper limit restoring force.  The
    * default valus is 0 (no transition region).
    *
    * @param tau new upper limit transition region
    */
   public void setUpperTransition (double tau) {
      myUpperTransition = tau;
   }
   
   /**
    * Queries the transition region size for the upper limit restoring force.
    *
    * @return upper limit transition region
    */
   public double getUpperTransition () {
      return myUpperTransition;
   }

   /**
    * Sets the lower limit for the joint coordinate, beyond which a restoring
    * force will be applied. The default value is {@code -infinity} (no lower
    * limit).
    *
    * @param ll new lower coordinate limit,
    */
   public void setLowerLimit (double ll) {
      myLowerLimit = ll;
   }
   
   /**
    * Queries the lower limit for the joint coordinate.
    *
    * @return lower coordinate limit,
    */
   public double getLowerLimit () {
      return myLowerLimit;
   }
   
   /**
    * Sets the stiffness for the lower limit restoring force. The default value
    * is 0 (no stiffness force).
    *
    * @param k new lower limit stiffness,
    */
   public void setLowerStiffness (double k) {
      myLowerStiffness = k;
   }
   
   /**
    * Queries the stiffness for the lower limit restoring force.
    *
    * @return lower limit stiffness,
    */
   public double getLowerStiffness () {
      return myLowerStiffness;
   }
   
   /**
    * Sets the damping parameter for the lower limit restoring force. The
    * default value is 0 (no damping force).
    *
    * @param d new lower limit damping
    */
   public void setLowerDamping (double d) {
      myLowerDamping = d;
   }
   
   /**
    * Queries the damping parameter for the lower limit restoring force.
    *
    * @return lower limit damping
    */
   public double getLowerDamping () {
      return myLowerDamping;
   }

   /**
    * Sets the transition region size for the lower limit restoring force.  The
    * default valus is 0 (no transition region).
    *
    * @param tau new lower limit transition region
    */
   public void setLowerTransition (double tau) {
      myLowerTransition = tau;
   }
   
   /**
    * Queries the transition region size for the lower limit restoring force.
    *
    * @return lower limit transition region
    */
   public double getLowerTransition () {
      return myLowerTransition;
   }

   public JointLimitForce() {
   }

   public JointLimitForce (JointCoordinateHandle ch) {
      myCoord = new JointCoordinateHandle(ch);
   }

   public JointLimitForce (String name, JointCoordinateHandle ch) {
      this (ch);
      setName (name);
   }

   /**
    * Set all the upper limit parameters.
    *
    * @param ul upper limit
    * @param k stiffness
    * @param d damping factor
    * @param tau transition region size
    */
   public void setUpper (
      double ul, double k, double d, double tau) {
      setUpperLimit (ul);
      setUpperStiffness (k);
      setUpperDamping (d);
      setUpperTransition (tau);
   }

   /**
    * Set all the lower limit parameters.
    *
    * @param ll upper limit
    * @param k stiffness
    * @param d damping factor
    * @param tau transition region size
    */
   public void setLower (
      double ll, double k, double d, double tau) {
      setLowerLimit (ll);
      setLowerStiffness (k);
      setLowerDamping (d);
      setLowerTransition (tau);
   }

   private double getLimitForce (double del, double k, double trans) {
      if (trans > 0) {
         if (del < trans) {
            double a = k/trans;
            return a*del*del/2;
         }
         else {
            return k*trans/2 + (del-trans)*k;
         }
      }
      else {
         return del*k;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void applyForces (double t) {
      double val = myCoord.getValue();
      double f = 0;

      if (val >= myUpperLimit) {
         double del = val-myUpperLimit;
         f = -getLimitForce (del, myUpperStiffness, myUpperTransition);
         if (myUpperDamping != 0) {
            double damping = myUpperDamping;
            if (myUpperStiffness != 0 && del < myUpperTransition) {
               damping *= Math.sqrt(del/myUpperTransition);
            }
            f -= damping*myCoord.getSpeed();
         }
         myCoord.applyForce (f);
      }
      else if (val <= myLowerLimit) {
         double del = myLowerLimit-val;
         f = getLimitForce (del, myLowerStiffness, myLowerTransition);
         if (myLowerDamping != 0) {
            double damping = myLowerDamping;
            if (myLowerStiffness != 0 && del < myLowerTransition) {
               damping *= Math.sqrt(del/myLowerTransition);
            }
            f -= damping*myCoord.getSpeed();
         }
         myCoord.applyForce (f);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addSolveBlocks (SparseNumberedBlockMatrix M) {
      if (myLowerDamping != 0 || myUpperDamping != 0) {
         myCoord.addSolveBlocks (M);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addPosJacobian (SparseNumberedBlockMatrix M, double s) {
   }

   /**
    * {@inheritDoc}
    */
   public void addVelJacobian (SparseNumberedBlockMatrix M, double s) {
      if (myLowerDamping != 0 || myUpperDamping != 0) {
         double damping;
         double val = myCoord.getValue();
         if (myUpperDamping != 0 && val >= myUpperLimit) {
            double del = val-myUpperLimit;
            damping = myUpperDamping;
            if (myUpperStiffness != 0 && del < myUpperTransition) {
               damping *= Math.sqrt(del/myUpperTransition);
            }
            // // scale to natural coords since Jacobian is in regular coords
            // damping *= myCoord.getNatScale();
            myCoord.addVelJacobian (M, -s*damping);
         }
         else if (myLowerDamping != 0 && val <= myLowerLimit) {
            double del = myLowerLimit-val;
            damping = myLowerDamping;
            if (myLowerStiffness != 0 && del < myLowerTransition) {
               damping *= Math.sqrt(del/myLowerTransition);
            }
            // // scale to natural coords since Jacobian is in regular coords
            // damping *= myCoord.getNatScale();
            myCoord.addVelJacobian (M, -s*damping);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public int getJacobianType() {
      return Matrix.SYMMETRIC;
   }

   /* --- begin I/O methods --- */

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.print ("coordinate=");
      myCoord.write (pw, ancestor);
      super.writeItems (pw, fmt, ancestor);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "coordinate")) {
         tokens.offer (new StringToken ("coordinate", rtok.lineno()));
         myCoord = new JointCoordinateHandle();
         myCoord.scan (rtok, tokens);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      if (postscanAttributeName (tokens, "coordinate")) {
         myCoord.postscan (tokens, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   /* --- end I/O methods --- */
}
