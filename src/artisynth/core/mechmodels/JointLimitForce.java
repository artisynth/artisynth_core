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
         "coordinate upper limit (deg for rotary joints)",
         DEFAULT_LIMIT);
      myProps.add (
         "upperStiffness",
         "upper limit stiffness (force/deg for rotary joints)",
         DEFAULT_STIFFNESS);
      myProps.add (
         "upperDamping",
         "upper limit damping (force/speed)",
         DEFAULT_DAMPING);
      myProps.add (
         "upperTransition",
         "upper limit transition region (deg for rotary joints)",
         DEFAULT_TRANSITION);
      myProps.add (
         "lowerLimit",
         "coordinate lower limit (deg for rotary joints)",
         -DEFAULT_LIMIT);
      myProps.add (
         "lowerStiffness",
         "lower limit stiffness (force/deg for rotary joints)",
         DEFAULT_STIFFNESS);
      myProps.add (
         "lowerDamping",
         "lower limit damping (force/speed)",
         DEFAULT_DAMPING);
      myProps.add (
         "lowerTransition",
         "lower limit transition region (deg for rotary joints)",
         DEFAULT_TRANSITION);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void setUpperLimit (double lim) {
      myUpperLimit = lim;
   }
   
   public double getUpperLimit () {
      return myUpperLimit;
   }
   
   public void setUpperStiffness (double k) {
      myUpperStiffness = k;
   }
   
   public double getUpperStiffness () {
      return myUpperStiffness;
   }
   
   public void setLowerLimit (double lim) {
      myLowerLimit = lim;
   }
   
   public double getLowerLimit () {
      return myLowerLimit;
   }
   
   public void setLowerStiffness (double k) {
      myLowerStiffness = k;
   }
   
   public double getLowerStiffness () {
      return myLowerStiffness;
   }
   
   public void setUpperDamping (double d) {
      myUpperDamping = d;
   }
   
   public double getUpperDamping () {
      return myUpperDamping;
   }
   
   public void setLowerDamping (double d) {
      myLowerDamping = d;
   }
   
   public double getLowerDamping () {
      return myLowerDamping;
   }
   
   public void setUpperTransition (double d) {
      myUpperTransition = d;
   }
   
   public double getUpperTransition () {
      return myUpperTransition;
   }

   public void setLowerTransition (double d) {
      myLowerTransition = d;
   }
   
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

   public void setUpper (
      double limit, double stiffness, double damping, double transition) {
      setUpperLimit (limit);
      setUpperStiffness (stiffness);
      setUpperDamping (damping);
      setUpperTransition (transition);
   }

   public void setLower (
      double limit, double stiffness, double damping, double transition) {
      setLowerLimit (limit);
      setLowerStiffness (stiffness);
      setLowerDamping (damping);
      setLowerTransition (transition);
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
      double val = myCoord.getNatValue();
      double f = 0;

      if (val >= myUpperLimit) {
         double del = val-myUpperLimit;
         f = -getLimitForce (del, myUpperStiffness, myUpperTransition);
         if (myUpperDamping != 0) {
            double damping = myUpperDamping;
            if (myUpperStiffness != 0 && del < myUpperTransition) {
               damping *= Math.sqrt(del/myUpperTransition);
            }
            f -= damping*myCoord.getNatSpeed();
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
            f -= damping*myCoord.getNatSpeed();
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
         double val = myCoord.getNatValue();
         if (myUpperDamping != 0 && val >= myUpperLimit) {
            double del = val-myUpperLimit;
            damping = myUpperDamping;
            if (myUpperStiffness != 0 && del < myUpperTransition) {
               damping *= Math.sqrt(del/myUpperTransition);
            }
            // scale to natural coords since Jacobian is in regular coords
            damping *= myCoord.getNatScale();
            myCoord.addVelJacobian (M, -s*damping);
         }
         else if (myLowerDamping != 0 && val <= myLowerLimit) {
            double del = myLowerLimit-val;
            damping = myLowerDamping;
            if (myLowerStiffness != 0 && del < myLowerTransition) {
               damping *= Math.sqrt(del/myLowerTransition);
            }
            // scale to natural coords since Jacobian is in regular coords
            damping *= myCoord.getNatScale();
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

   // public static JointLimitForce create (
   //    String name, CoordinateLimitForce clf, OpenSimObject ref,
   //    ModelComponentMap componentMap) {

   //    JointLimitForce jlf = new JointLimitForce (name);
   //    jlf.myCoord = CoordinateHandle.create (clf.coordinate, ref, componentMap);
   //    if (jlf.myCoord == null) {
   //       return null;
   //    }
   //    MotionType type = jlf.myCoord.getMotionType();
   //    double scale = 1.0;
   //    if (jlf.myCoord.getMotionType() == MotionType.ROTARY) {
   //       scale = Math.PI/180.0;
   //    }
   //    double kupper = clf.upper_stiffness/scale;
   //    double klower = clf.lower_stiffness/scale;
   //    double dupper = clf.damping/scale;
   //    double dlower = dupper;

   //    // adjust upper and lower damping to reflect proper critcal damping
   //    if (kupper != 0 && klower != 0) {
   //       if (kupper > klower) {
   //          dlower *= Math.sqrt(klower/kupper);
   //       }
   //       else if (klower > kupper) {
   //          dupper *= Math.sqrt(kupper/klower);            
   //       }
   //    }
   //    jlf.setUpperLimit (scale*clf.upper_limit);
   //    jlf.setUpperStiffness (kupper);
   //    jlf.setUpperDamping (dupper);
   //    jlf.setLowerLimit (scale*clf.lower_limit);
   //    jlf.setLowerStiffness (klower);
   //    jlf.setLowerDamping (dlower);
   //    jlf.setTransition (scale*clf.transition);
   //    return jlf;
   // }
   
}
