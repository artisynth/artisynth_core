/**
 * Copyright (c) 2025, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
/* -------------------------------------------------------------------------- *
 *                                                                            *
 * Portions of this code were adapted from the OpenSim classes                *
 * SmoothSegmentedFunctionFactory and SegmentedQuinticBezierToolkit, written  *
 * by Matthew Millard and distributed under the following copywrite:          *
 *                                                                            *
 * The OpenSim API is a toolkit for musculoskeletal modeling and simulation.  *
 * See http://opensim.stanford.edu and the NOTICE file for more information.  *
 * OpenSim is developed at Stanford University and supported by the US        *
 * National Institutes of Health (U54 GM072970, R24 HD065690) and by DARPA    *
 * through the Warrior Web program.                                           *
 *                                                                            *
 * Copyright (c) 2005-2017 Stanford University and the Authors                *
 * Author(s): Matthew Millard                                                 *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may    *
 * not use this file except in compliance with the License. You may obtain a  *
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0.         *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 * -------------------------------------------------------------------------- */
package artisynth.core.materials;

import java.io.*;
import java.util.*;

import artisynth.core.modelbase.HasNumericState;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;
import artisynth.core.modelbase.*;
import maspack.util.DataBuffer;
import maspack.util.*;
import maspack.properties.*;
import maspack.numerics.*;
import maspack.matrix.*;
import maspack.function.*;
import maspack.interpolation.CubicHermiteSpline1d;
import maspack.interpolation.CubicHermiteSpline1d.Knot;

/**
 * Implements the Millard2012EquilibriumMuscle from OpenSim. One should consult
 * the OpenSim API documentation for a detailed explanation of the muscle
 * model's parameters and behavior. The names of this class's muscle parameter
 * properties should match the OpenSim equivalents closely enough to be
 * unambiguous.
 *
 * <p>Equilbrium mus1cle models contain a muscle component and tendon component
 * in series. The muscle and tendon lengths sum to the combined length {@code
 * l} that is supplied the the method {@link #computeF}, which computes the
 * muacle tension, while the equilibrum condition implies that the tension from
 * the muscle and tendon components must both equal the value returned by
 * {@link #computeF}. To implement this, we store the muscle length as a state
 * variable which is updated at each time step to ensure that the equilbrium
 * condition is satisfied. We do this differently from OpenSim: rather than
 * solving for and then integrating the muscle velocity, the velocity inferred
 * by dividing the cnange in muscle length by the time step size.  This avoids
 * the singularity that arises in the OpenSim method at zero activation, and so
 * removes the need for always ensuring that the activation is non-zero.
 *
 * <p>As with the OpenSim model, we also a support, via the {@code rigidTendon}
 * property, a mode where the tendon is considered to be rigid (via the {@code
 * rigidTendon} property), which simplifies the computation in situations where
 * this approximation is acceptable.
 */
public class Millard2012AxialMuscle extends EquilibriumAxialMuscle {

   // Force length curves and velocity curve

   CubicHermiteSpline1d myActiveForceLengthCurve;
   CubicHermiteSpline1d myPassiveForceLengthCurve;
   CubicHermiteSpline1d myTendonForceLengthCurve;
   CubicHermiteSpline1d myForceVelocityCurve;

   // from OpenSim Millard2012EquilibriumMuscle:

   public static double DEFAULT_MAX_PENNATION_ANGLE = Math.acos(0.1);
   protected double myMaxPennationAngle = DEFAULT_MAX_PENNATION_ANGLE;

   public static double DEFAULT_DEFAULT_MIN_FLC_VALUE = 0.0;
   protected static double myDefaultMinFlcValue = DEFAULT_DEFAULT_MIN_FLC_VALUE;

   double myMinCos; // minimum cosine of the pennation angle

   public static PropertyList myProps =
      new PropertyList(Millard2012AxialMuscle.class,
                       EquilibriumAxialMuscle.class);

   // default force curves
   protected static CubicHermiteSpline1d myDefaultActiveForceLengthCurve;
   protected static CubicHermiteSpline1d myDefaultPassiveForceLengthCurve;
   protected static CubicHermiteSpline1d myDefaultForceVelocityCurve;
   protected static CubicHermiteSpline1d myDefaultTendonForceLengthCurve;

   static {
      myProps.add (
         "maxPennationAngle",
         "maximum pennation angle", DEFAULT_MAX_PENNATION_ANGLE);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public Millard2012AxialMuscle() {
      super();
      setActiveForceLengthCurve (getDefaultActiveForceLengthCurve());
      setPassiveForceLengthCurve (getDefaultPassiveForceLengthCurve());
      setForceVelocityCurve (getDefaultForceVelocityCurve());
      setTendonForceLengthCurve (getDefaultTendonForceLengthCurve());
      
      myMinCos = Math.cos(DEFAULT_MAX_PENNATION_ANGLE);
      myMuscleLength = DEFAULT_MUSCLE_LENGTH;
      myMuscleLengthPrev = DEFAULT_MUSCLE_LENGTH;
      myH = -1;
   }

   public Millard2012AxialMuscle (
      double fmax, double lopt,
      double tslack, double optPenAng) {
      
      this();
      setMaxIsoForce (fmax);
      setOptFibreLength (lopt);
      setTendonSlackLength (tslack);
      setOptPennationAngle (optPenAng);
      myMuscleLength = myOptFibreLength*Math.cos(optPenAng);
      myMuscleLengthPrev = myMuscleLength;
   }

   /**
    * {@inheritDoc}
    */
   protected double computeActiveForceLength (double ln) {
      return myActiveForceLengthCurve.evalY (ln);
   }

   /**
    * {@inheritDoc}
    */
   protected double computeActiveForceLengthDeriv (double ln) {
      return myActiveForceLengthCurve.evalDy (ln);
   }
   
   /**
    * {@inheritDoc}
    */
   protected double computePassiveForceLength (double ln) {
      return myPassiveForceLengthCurve.evalY (ln);
   }

   /**
    * {@inheritDoc}
    */
   protected double computePassiveForceLengthDeriv (double ln) {
      return myPassiveForceLengthCurve.evalDy (ln);
   }

   /**
    * {@inheritDoc}
    */
   protected double computeForceVelocity (double vn, double a) {
      if (vn < -1) {
         return 0;
      }
      else {
         return myForceVelocityCurve.evalY (vn);
      }
   }

   /**
    * {@inheritDoc}
    */
   protected double computeForceVelocityDeriv (double vn, double a) {
      if (vn < -1) {
         return 0;
      }
      else {
         return myForceVelocityCurve.evalDy (vn);
      }
   }

   /**
    * {@inheritDoc}
    */
   protected double computeTendonForce (double tln) {
      return myTendonForceLengthCurve.evalY (tln);
   }

   /**
    * {@inheritDoc}
    */
   protected double computeTendonForceDeriv (double tln) {
      return myTendonForceLengthCurve.evalDy (tln);
   }

   public static CubicHermiteSpline1d getDefaultActiveForceLengthCurve() {
      if (myDefaultActiveForceLengthCurve == null) {
         myDefaultActiveForceLengthCurve =
            createActiveForceLengthCurve (
               0.4441, 0.73, 1.8123, 0.8616, getDefaultMinFlcValue());
      }
      return myDefaultActiveForceLengthCurve;
   }

   public static CubicHermiteSpline1d getDefaultPassiveForceLengthCurve() {
      if (myDefaultPassiveForceLengthCurve == null) {
         double strainAtZeroForce = 0.0;
         double strainAtOneNormForce = 0.7;
         myDefaultPassiveForceLengthCurve =
            createPassiveForceLengthCurve (
               strainAtZeroForce, strainAtOneNormForce, 0.2,
               2.0/(strainAtOneNormForce-strainAtZeroForce), 0.75);
      }
      return myDefaultPassiveForceLengthCurve;
   }

   public static CubicHermiteSpline1d getDefaultForceVelocityCurve() {
      if (myDefaultForceVelocityCurve == null) {
         myDefaultForceVelocityCurve =
            createForceVelocityCurve (
               0.01, 0.25, 5.0, 0.01, 0.15, 1.4, 0.6, 0.9);
      }
      return myDefaultForceVelocityCurve;
   }

   public static CubicHermiteSpline1d getDefaultTendonForceLengthCurve() {
      if (myDefaultTendonForceLengthCurve == null) {
         double strainAtOneNormForce = 0.049;
         myDefaultTendonForceLengthCurve =
            createTendonForceLengthCurve (
               strainAtOneNormForce, 1.375/strainAtOneNormForce, 2.0/3.0, 0.5);
      }
      return myDefaultTendonForceLengthCurve;
   }

   public double getMaxPennationAngle() {
      return myMaxPennationAngle;
   }

   public void setMaxPennationAngle (double ang) {
      myMaxPennationAngle = ang;
      myMinCos = Math.cos(ang);
   }

   public static double getDefaultMinFlcValue() {
      return myDefaultMinFlcValue;
   }

   public static void setDefaultMinFlcValue (double min) {
      myDefaultMinFlcValue = min;
      myDefaultActiveForceLengthCurve = null;
   }

   static int nsame = 0;

   static int ninterval = 0;
   static int nsolved = 0;
   static int nclippedL = 0;
   static int nclipped0 = 0;
   static int maxiters = 0;
   static int nshort = 0;

   static int nreduce;

   double getMaxTendonStiffness() {
      return
         getTendonForceLengthCurve().getLastKnot().getDy()/myTendonSlackLength;
   }

   double getMaxPassiveStiffness() {
      return getPassiveForceLengthCurve().getLastKnot().getDy()/myOptFibreLength;
   }

   // force length and force velocity curves methods

   /**
    * Queries the active force length curve for this material.  Returns {@code
    * null} if this curve has not been set.
    *
    * @return current active force length curve
    */
   public CubicHermiteSpline1d getActiveForceLengthCurve() {
      return myActiveForceLengthCurve;
   }

   /**
    * Sets the active force length curve for this material, or removes it if
    * {@code curve} is set to {@code null}. Any specified curve is copied
    * internally.
    *
    * @param curve new active force length curve
    */
   public void setActiveForceLengthCurve (CubicHermiteSpline1d curve) {
      if (curve != null) {
         myActiveForceLengthCurve = new CubicHermiteSpline1d (curve);
      }
      else {
         myActiveForceLengthCurve = null;
      }
   }

   /**
    * Queries the passive force length curve for this material.  Returns {@code
    * null} if this curve has not been set.
    *
    * @return current passive force length curve
    */
   public CubicHermiteSpline1d getPassiveForceLengthCurve() {
      return myPassiveForceLengthCurve;
   }

   /**
    * Sets the passive force length curve for this material, or removes it if
    * {@code curve} is set to {@code null}. Any specified curve is copied
    * internally.
    *
    * @param curve new passive force length curve
    */
   public void setPassiveForceLengthCurve (CubicHermiteSpline1d curve) {
      if (curve != null) {
         myPassiveForceLengthCurve = new CubicHermiteSpline1d (curve);
      }
      else {
         myPassiveForceLengthCurve = null;
      }
   }

   /**
    * Queries the tendon force length curve for this material.  Returns {@code
    * null} if this curve has not been set.
    *
    * @return current tendon force length curve
    */
   public CubicHermiteSpline1d getTendonForceLengthCurve() {
      return myTendonForceLengthCurve;
   }

   /**
    * Sets the tendon force length curve for this material, or removes it if
    * {@code curve} is set to {@code null}. Any specified curve is copied
    * internally.
    *
    * @param curve new tendon force length curve
    */
   public void setTendonForceLengthCurve (CubicHermiteSpline1d curve) {
      if (curve != null) {
         myTendonForceLengthCurve = new CubicHermiteSpline1d (curve);
      }
      else {
         myTendonForceLengthCurve = null;
      }
   }

   /**
    * Queries the force velocity curve for this material.  Returns {@code
    * null} if this curve has not been set.
    *
    * @return current force velocity curve
    */
   public CubicHermiteSpline1d getForceVelocityCurve() {
      return myForceVelocityCurve;
   }

   /**
    * Sets the force velocity curve for this material, or removes it if
    * {@code curve} is set to {@code null}. Any specified curve is copied
    * internally.
    *
    * @param curve new force velocity curve
    */
   public void setForceVelocityCurve (CubicHermiteSpline1d curve) {
      if (curve != null) {
         myForceVelocityCurve = new CubicHermiteSpline1d (curve);
      }
      else {
         myForceVelocityCurve = null;
      }
   }

   /**
    * Creates an active force length curve with specified parameters.  Adapted
    * from code by Matthew Millard in the OpenSim class
    * SmoothSegmentedFunctionFactory.
    */
   public static CubicHermiteSpline1d createActiveForceLengthCurve (
      double minActiveNormFiberLength,
      double transitionNormFiberLength,
      double maxActiveNormFiberLength,
      double shallowAscendingSlope,
      double minimumValue) {

      if (minActiveNormFiberLength <= 0) {
         throw new IllegalArgumentException (
            "minActiveNormFiberLength must be > 0");
      }
      if (transitionNormFiberLength >= 1) {
         throw new IllegalArgumentException (
            "transitionNormFiberLength must be < 1");
      }
      if (minActiveNormFiberLength >= transitionNormFiberLength) {
         throw new IllegalArgumentException (
            "minActiveNormFiberLength must be < transitionNormFiberLength");
      }
      if (maxActiveNormFiberLength <= 1) {
         throw new IllegalArgumentException (
            "maxActiveNormFiberLength must be > 1");
      }
      if (shallowAscendingSlope < 0) {
         throw new IllegalArgumentException (
            "shallowAscendingSlope must be >= 0");
      }
      if (shallowAscendingSlope >= 1/(1-transitionNormFiberLength)) {
         throw new IllegalArgumentException (
            "shallowAscendingSlope must be < 1/(1-transitionNormFiberLength)");
      }
      if (minimumValue < 0) {
         throw new IllegalArgumentException (
            "minimumValue must be >= 0");
      }
      
      CubicHermiteSpline1d curve = new CubicHermiteSpline1d();

      // start point
      curve.addKnot (minActiveNormFiberLength, minimumValue, 0.0);

      // trans point
      double xt = transitionNormFiberLength;
      curve.addKnot (
         xt, 1.0 - shallowAscendingSlope*(1.0-xt), shallowAscendingSlope);

      // max point
      curve.addKnot (1.0, 1.0, 0.0);

      // end point
      curve.addKnot (maxActiveNormFiberLength, minimumValue, 0);
      return curve;
   }

   /**
    * Check for an inflexion point within the curve interval specified by the
    * {@code idx}-th knot, and if one is found, try to remove it by adding an
    * additional knot the interval.
    */
   static void addAntiInflexionKnot (CubicHermiteSpline1d curve, int idx0) {

      Knot knot0 = curve.getKnot(idx0);
      Knot knot1 = curve.getKnot(idx0+1);

      // unpack the x, y and dy parameters at each knot. 
      double x0 = knot0.getX();
      double x1 = knot1.getX();
      double h = x1-x0; // length of the interval
      double y0 = knot0.getY();
      double dy0 = knot0.getDy();
      double y1 = knot1.getY();
      double dy1 = knot1.getDy();

      // upack the a3 and a2 cubic coefficients for knot0. These are related
      // the initial acceleration y''(0) and (constant) jerk value y'''(x) by
      //
      // y''(0) = 2 a2
      // y'''(x) = 6 a3
      //
      // Note: each knot's cubic and associated coefficients are defined with
      // respect to the shifted interval [0 x1-x0].
      double a2 = knot0.getA2();
      double a3 = knot0.getA3();

      // see if there is an inflexion point in the interval.  This will be true
      // if y''(x) has a zero. If it does not, there is no need to do anything.
      if (a3 != 0) {
         double chk = -a2/(3*a3);
         if (chk <= 0 && chk >= h) {
            // no inflexion
            return;
         }
      }
      if (dy1-dy0 == 0) {
         // can't remove inflexion if there is no end point change y'(x)
         return;
      }
      // Try to remove the inflexion by adding an extra knot at x = tau in the
      // interval [0, h], and replace the straight line defining y''(x) with a
      // piecewise linear version with curvature values c0, ctau and c1
      // defined at x = {0, tau, h}.
      if (Math.abs(dy0) > Math.abs(dy1)) {
         // forward direction: curvature at the beginning and set c1 = 0.
         // See if solutuin is feasible by solving for tau with ctau = 0.
         double tau = 3*(dy1*h-(y1-y0))/(dy1-dy0);
         if (tau <= 0 || tau >= h) {
            // can't do it - no solution
            return;
         }
         // If feasible, reduce tau by 2/3 to get a soother curve:
         tau = 2*tau/3;
         // Now backsolve for c0 and ctau:
         Vector2d cvec = new Vector2d();
         Vector2d b = new Vector2d (y1-y0+(dy1-dy0)*tau - h*dy1, dy1-dy0);
         Matrix2d M = new Matrix2d (
            tau*tau/3, (2*tau-h)*h/6, tau/2, h/2);
         double d = Matrix2d.solve (M, b, cvec);
         double c0 = cvec.x;
         double ctau = cvec.y;
         double ytau = y0 + dy0*tau + (c0/3 + ctau/6)*tau*tau;
         double dytau = dy0 + (c0 + ctau)*tau/2;
         curve.addKnot (x0+tau, ytau, dytau);
      }
      else {
         // other direction: apply curvature at interval end and set c0 = 0.
         double tau = 3*((y1-y0)-dy0*h)/(dy1-dy0);
         if (tau <= 0 || tau >= h) {
            // can't do it - no solution
            return;
         }
         // reduce tau by 2/3 to get a soother curve:
         tau = 2*tau/3;
         //  backsolve for c1 and ctau:
         Vector2d cvec = new Vector2d();
         Vector2d b = new Vector2d (y1-y0-(dy1-dy0)*tau - h*dy0, dy1-dy0);
         Matrix2d M = new Matrix2d (
            -tau*tau/3, -(2*tau-h)*h/6, tau/2, h/2);
         double d = Matrix2d.solve (M, b, cvec);
         double ctau = cvec.y;
         double ytau = y0 + dy0*(h-tau) + ctau*(h-tau)*(h-tau)/6;
         double dytau = dy0 + ctau*(h-tau)/2;
         curve.addKnot (x0+h-tau, ytau, dytau);
      }
   }

   /**
    * Creates a force velocity curve with specified parameters.  Adapted
    * from code by Matthew Millard in the OpenSim class
    * SmoothSegmentedFunctionFactory.
    */
   public static CubicHermiteSpline1d createForceVelocityCurve (
      double concentricSlopeAtVmax,
      double concentricSlopeNearVmax,
      double isometricSlope,
      double eccentricSlopeAtVmax, 
      double eccentricSlopeNearVmax,
      double maxEccentricVelocityForceMultiplier,
      double concentricCurviness,
      double eccentricCurviness) {

      // check conditions
      if (concentricSlopeAtVmax < 0 || concentricSlopeAtVmax >= 1) {
         throw new IllegalArgumentException (
            "concentricSlopeAtVmax must be in the range [0,1)");
      }
      if (concentricSlopeNearVmax <= concentricSlopeAtVmax ||
          concentricSlopeNearVmax >= isometricSlope) {
         throw new IllegalArgumentException (
            "concentricSlopeNearVmax must be > concentricSlopeAtVmax "+
            "and < isometricSlope");
      }
      if (isometricSlope <= 1) {
         throw new IllegalArgumentException (
            "isometricSlope must be > 1");
      }
      if (eccentricSlopeNearVmax >= isometricSlope ||
          eccentricSlopeNearVmax <= eccentricSlopeAtVmax) {
         throw new IllegalArgumentException (
            "eccentricSlopeNearVmax must be < isometricSlope "+
            "and > eccentricSlopeAtVmax");
      }
      if (isometricSlope <= maxEccentricVelocityForceMultiplier-1) {
         throw new IllegalArgumentException (
            "isometricSlope must exceed (maxEccentricVelocityForceMultiplier-1)");
      }
      if (eccentricSlopeAtVmax < 0) {
         throw new IllegalArgumentException (
            "eccentricSlopeAtVmax must be >= 0");
      }
      if (eccentricSlopeAtVmax >= maxEccentricVelocityForceMultiplier-1) {
         throw new IllegalArgumentException (
            "eccentricSlopeAtVmax must be less than " +
            "(maxEccentricVelocityForceMultiplier-1)");
      }
      if (maxEccentricVelocityForceMultiplier <= 1) {
         throw new IllegalArgumentException (
            "maxEccentricVelocityForceMultiplier must be > 1");
      }
      if (concentricCurviness < 0 || concentricCurviness > 1) {
         throw new IllegalArgumentException (
            "concentricCurviness must be in the range [0,1]");
      }
      if (eccentricCurviness < 0 || eccentricCurviness > 1) {
         throw new IllegalArgumentException (
            "eccentricCurviness must be in the range [0,1]");
      }
      // need to make end points have small non-zero slopes to ensure curve is
      // invertible.

      CubicHermiteSpline1d curve = new CubicHermiteSpline1d();

      // start point at C
      double xC = -1.0;
      double yC = 0.0;
      double dydxC = concentricSlopeAtVmax;
      curve.addKnot (xC, yC, dydxC);

      // point near C
      double xNearC = -0.9;
      double dydxNearC = concentricSlopeNearVmax;
      double yNearC = yC + 0.5*dydxNearC*(xNearC-xC) + 0.5*dydxC*(xNearC-xC);
      curve.addKnot (xNearC, yNearC, dydxNearC);

      // ISO mid point
      curve.addKnot (0.0, 1.0, isometricSlope);

      // end point E
      double xE = 1.0;
      double yE = maxEccentricVelocityForceMultiplier;
      double dydxE = eccentricSlopeAtVmax;
      curve.addKnot (xE, yE, dydxE);

      // point near E
      double xNearE = 0.9;
      double dydxNearE = eccentricSlopeNearVmax;
      double yNearE =
         yE + 0.5*dydxNearE*(xNearE-xE) + 0.5*dydxE*(xNearE-xE);
      curve.addKnot (xNearE, yNearE, dydxNearE);

      // add extra knot to remove inflection between near C and ISO
      addAntiInflexionKnot (curve, 1);
      // add extra knot to remove inflection between near ISO and near E
      addAntiInflexionKnot (curve, 3);

      return curve;
   }

   /**
    * Creates a tendon force length curve with specified parameters.  Adapted
    * from code by Matthew Millard in the OpenSim class
    * SmoothSegmentedFunctionFactory.
    */
   public static CubicHermiteSpline1d createTendonForceLengthCurve (
      double strainAtOneNormForce,
      double stiffnessAtOneNormForce,
      double normForceAtToeEnd,
      double curviness) {

      if (strainAtOneNormForce <= 0) {
         throw new IllegalArgumentException (
            "strainAtOneNormForce is " +strainAtOneNormForce+"; must be > 0");
      }
      if (stiffnessAtOneNormForce <= 1/strainAtOneNormForce) {
         throw new IllegalArgumentException (
            "stiffnessAtOneNormForce (" + stiffnessAtOneNormForce +
            ") must be > 1/strainAtOneNormForce (" + 1/strainAtOneNormForce +")");
      }
      if (normForceAtToeEnd <= 0 || normForceAtToeEnd >= 1) {
         throw new IllegalArgumentException (
            "normForceAtToeEnd (" +normForceAtToeEnd + 
            ") must be in the range (0,1)");
      }
      if (curviness < 0 || curviness > 1) {
         throw new IllegalArgumentException (
            "curviness ("+curviness+") must be in the range [0,1]");
      }

      double eIso = strainAtOneNormForce;
      double kIso = stiffnessAtOneNormForce;
      double fToe = normForceAtToeEnd;

      // In the original Millard code, the curve is defined by two quintic
      // Bezier segments, between the points [x0,xToeCtrl] and [xToeCtrl,xToe].
      // We have found that it is sufficient to define these segments using
      // cubic Hermite splines. For completeness, we also add an additional
      // linear segment, with slope kIso, between [xToe,xIso].

      // start point
      double x0 = 1.0;
      double y0 = 0;
      double dydx0 = 0;

      // final ISO point
      double xIso = 1.0 + eIso;
      double yIso = 1.0;

      // point where the curved section becomes linear
      double yToe = fToe;
      double xToe = (yToe-1)/kIso + xIso;

      //To limit the 2nd derivative of the toe region the line it tends to
      //has to intersect the x axis to the right of the origin
      double xFoot = 1.0+(xToe-1.0)/10.0;
      double yFoot = 0;

      //Compute the location of the corner formed by the average slope of the
      //toe and the slope of the linear section
      double yToeMid = yToe*0.5;
      double xToeMid = (yToeMid-yIso)/kIso + xIso;
      double dydxToeMid = (yToeMid-yFoot)/(xToeMid-xFoot);

      //Compute the location of the control point to the left of the corner
      double xToeCtrl = xFoot + 0.5*(xToeMid-xFoot); 
      double yToeCtrl = yFoot + dydxToeMid*(xToeCtrl-xFoot);

      // create curve and add segments.
      CubicHermiteSpline1d curve = new CubicHermiteSpline1d();

      curve.addKnot (x0, y0, dydx0);
      curve.addKnot (xToeCtrl, yToeCtrl, dydxToeMid);
      curve.addKnot (xToe, yToe, kIso);
      curve.addKnot (xIso, yIso, kIso);
      
      return curve;
   }

   public static CubicHermiteSpline1d createTendonForceLengthCurveOld (
      double strainAtOneNormForce,
      double stiffnessAtOneNormForce,
      double normForceAtToeEnd,
      double curviness) {

      if (strainAtOneNormForce <= 0) {
         throw new IllegalArgumentException (
            "strainAtOneNormForce is " +strainAtOneNormForce+"; must be > 0");
      }
      if (stiffnessAtOneNormForce <= 1/strainAtOneNormForce) {
         throw new IllegalArgumentException (
            "stiffnessAtOneNormForce (" + stiffnessAtOneNormForce +
            ") must be > 1/strainAtOneNormForce (" + 1/strainAtOneNormForce +")");
      }
      if (normForceAtToeEnd <= 0 || normForceAtToeEnd >= 1) {
         throw new IllegalArgumentException (
            "normForceAtToeEnd (" +normForceAtToeEnd + 
            ") must be in the range (0,1)");
      }
      if (curviness < 0 || curviness > 1) {
         throw new IllegalArgumentException (
            "curviness ("+curviness+") must be in the range [0,1]");
      }

      double eIso = strainAtOneNormForce;
      double kIso = stiffnessAtOneNormForce;
      double fToe = normForceAtToeEnd;

      CubicHermiteSpline1d curve = new CubicHermiteSpline1d();

      // start point
      curve.addKnot (1.0, 0.0, 0.0);

      // ISO point
      double xIso = 1.0 + strainAtOneNormForce;
      curve.addKnot (xIso, 1.0, stiffnessAtOneNormForce);
      
      // point between start and ISO where the curved section becomes linear
      curve.addKnot ((fToe-1)/kIso + xIso, fToe, stiffnessAtOneNormForce);
      
      return curve;
   }

   /**
    * Creates a passive force length curve with specified parameters.  Adapted
    * from code by Matthew Millard in the OpenSim class
    * SmoothSegmentedFunctionFactory.
    */
   public static CubicHermiteSpline1d createPassiveForceLengthCurve (
      double strainAtZeroForce,
      double strainAtOneNormForce,
      double stiffnessAtLowForce,
      double stiffnessAtOneNormForce, 
      double curviness) {

      if (strainAtZeroForce >= strainAtOneNormForce) {
         throw new IllegalArgumentException (
            "strainAtZeroForce ("+strainAtZeroForce+
            ") must be < strainAtOneNormForce ("+strainAtOneNormForce+")");
      }
      if (stiffnessAtOneNormForce <= 1/(strainAtOneNormForce-strainAtZeroForce)) {
         throw new IllegalArgumentException (
            "stiffnessAtOneNormForce ("+stiffnessAtOneNormForce+
            ") must be > 1/(strainAtOneNormForce-strainAtZeroForce) ("+
            1/(strainAtOneNormForce-strainAtZeroForce) + ")");
      }
      if (stiffnessAtLowForce <= 0) {
         throw new IllegalArgumentException (
            "stiffnessAtLowForce ("+stiffnessAtLowForce+") must be > 0");
      }
      if (stiffnessAtLowForce >= stiffnessAtOneNormForce) {
         throw new IllegalArgumentException (
            "stiffnessAtLowForce ("+stiffnessAtLowForce+
            ") must be < stiffnessAtOneNormForce ("+stiffnessAtOneNormForce+")");
      }
      if (curviness < 0 || curviness > 1) {
         throw new IllegalArgumentException (
            "curviness ("+curviness+") must be in the range [0,1]");
      }
      
      double eZero = strainAtZeroForce;
      double eIso = strainAtOneNormForce;
      double kLow = stiffnessAtLowForce;
      double kIso = stiffnessAtOneNormForce;

      // start point
      double xStart = 1.0 + eZero;

      // ISO point
      double xIso = 1.0 + eIso;
      double yIso = 1.0;

      // intermediate knot between start and ISO
      double deltaX = Math.min(0.1*(1.0/kIso), 0.1*(xIso-xStart));
      double xLow = xStart + deltaX;
      double xfoot = xStart + 0.5*(xLow-xStart);
      
      double yLow = kLow*(xLow-xfoot);

      ArrayList<Point2d> cpnts = new ArrayList<>();

      //Compute the Quintic Bezier control points
      double c = 0.1 + 0.8*curviness; // scale curviness
      cpnts.addAll (
         calcQuinticBezierCornerControlPoints(
            xStart, /*yStart*/0, 0, xLow, yLow, kLow,c));
    
      cpnts.addAll (
         calcQuinticBezierCornerControlPoints(
            xLow, yLow, kLow, xIso, yIso, kIso, c));     

      // create cubic curve with segments corresponding to the two quintic
      // segments:
      CubicHermiteSpline1d curve = new CubicHermiteSpline1d();
      Knot knot0 = curve.addKnot (xStart, 0.0, 0.0);
      Knot knot1 = curve.addKnot (xIso, 1.0, stiffnessAtOneNormForce);
      Knot knot2 = curve.addKnot (xLow, yLow, kLow);

      // add extra knots into the cubic segment at the point of greatest
      // error with respect to the the quintic segment:
      for (int seg=0; seg<2; seg++) {
         Knot knotA = (seg==0 ? knot0 : knot1);
         // find u and point with max error wrt quintic segment
         int nsamps = 20; 
         double uMaxErr = 0;
         double maxErr = -1;
         Point2d qMaxErr = null;
         for (int i=1; i<=nsamps; i++) {
            double u = i/(double)(nsamps+1);
            Point2d q =
               calcQuinticBezierCurveVal (u, cpnts, seg);
            double erry = curve.eval (q.x);
            double err = Math.abs(q.y - curve.eval (q.x));
            if (err > maxErr) {
               maxErr = err;
               qMaxErr = q;
               uMaxErr = u;
            }
         }
         // find dydx at point of max error, and use this to insert and
         // additional knot within the segment:
         double dydx = calcQuinticBezierCurveDydx (
            uMaxErr, cpnts, seg);
         Knot knotB = curve.addKnot (qMaxErr.x, qMaxErr.y, dydx);
         // check that the two new segments do not contain inflexion points
         if (curve.segmentContainsInflexion (knotA) ||
             curve.segmentContainsInflexion (knotB)) {
            System.out.println (
               "WARNING: computed passive force length curve contains inflexion");
         }
      }
      
      return curve;
   }

   // Quintic bezier curve methods found in the OpenSim class
   // SegmentedQuinticBezierToolkit:

   /**
    * Calculates control points for a quintic bezier segment with specified
    * boundary conditions and curviness. Code by Matthew Millard.
    */
   static List<Point2d> calcQuinticBezierCornerControlPoints (
      double x0, double y0, double dydx0,
      double x1, double y1, double dydx1, double curviness) {

      double EPS = 1e-16;

      ArrayList<Point2d> pnts = new ArrayList<>(6);      

      //1. Calculate the location where the two lines intersect
      // (x-x0)*dydx0 + y0 = (x-x1)*dydx1 + y1
      //   x*(dydx0-dydx1) = y1-y0-x1*dydx1+x0*dydx0
      //                 x = (y1-y0-x1*dydx1+x0*dydx0)/(dydx0-dydx1);

      double xC = 0;
      double yC = 0;
      double rootEPS = Math.sqrt(EPS);
      if(Math.abs(dydx0-dydx1) > rootEPS) {
         xC = (y1-y0-x1*dydx1+x0*dydx0)/(dydx0-dydx1);    
      }
      else {
         xC = (x1+x0)/2;
      }

      yC = (xC-x1)*dydx1 + y1;
      //Check to make sure that the inputs are consistent with a corner, and will
      //not produce an 's' shaped section. To check this we compute the sides of
      //a triangle that is formed by the two points that the user entered, and 
      //also the intersection of the 2 lines the user entered. If the distance
      //between the two points the user entered is larger than the distance from
      //either point to the intersection location, this function will generate a
      //'C' shaped curve. If this is not true, an 'S' shaped curve will result, 
      //and this function should not be used.

      double xCx0 = (xC-x0);
      double yCy0 = (yC-y0);
      double xCx1 = (xC-x1);
      double yCy1 = (yC-y1);
      double x0x1 = (x1-x0);
      double y0y1 = (y1-y0);

      double a = xCx0*xCx0 + yCy0*yCy0;
      double b = xCx1*xCx1 + yCy1*yCy1;
      double c = x0x1*x0x1 + y0y1*y0y1;

      if (! ((c > a) && (c > b))) {
         //This error message needs to be better.
         throw new IllegalArgumentException (
            "The intersection point for the two lines defined by the input "+
            "parameters must be consistent with a C shaped corner.");
      }
      
      //Start point
      pnts.add (new Point2d(x0, y0));
    
      //Original code - leads to 2 localized corners

      Point2d pnt = new Point2d (x0+curviness*(xC-x0), y0+curviness*(yC-y0));
      pnts.add (pnt);
      pnts.add (new Point2d(pnt));
      pnt = new Point2d (x1+curviness*(xC-x1), y1+curviness*(yC-y1));
      pnts.add (pnt);
      pnts.add (new Point2d(pnt));
      
      //End point
      pnts.add (new Point2d(x1, y1));

      return pnts;
   }

   /**
    * Calculates the point value for a parmeter value u on a quintic bezier
    * segment. The segment is specified by 6 control points within the list
    * {@code cpnts}, starting at the index {@code segNum*6}. Code by Matthew
    * Millard.
    */
   static Point2d calcQuinticBezierCurveVal(
      double u, List<Point2d> cpnts, int segNum) {
      if (u < 0 || u > 1) {
         throw new IllegalArgumentException (
            "argument u is "+u+"; must be between 0.0 and 1.0");
      }
      int k = segNum*6;
      if (cpnts.size() - k < 6) {
         throw new IllegalArgumentException (
            "cpnts must have at least 6 points beyond segNum*6");
      }

      //Compute the Bezier point

      double u5 = 1;
      double u4 = u;
      double u3 = u4*u;
      double u2 = u3*u;
      double u1 = u2*u;
      double u0 = u1*u;

      //See lines 1-6 of MuscleCurveCodeOpt_20120210
      double t2 = u1 * 0.5e1;
      double t3 = u2 * 0.10e2;
      double t4 = u3 * 0.10e2;
      double t5 = u4 * 0.5e1;
      double t9 = u0 * 0.5e1;
      double t10 = u1 * 0.20e2;
      double t11 = u2 * 0.30e2;
      double t15 = u0 * 0.10e2;
      Point2d pnt = new Point2d();
      pnt.scale (u0*(-0.1e1) + t2 - t3 + t4 - t5 + u5*0.1e1, cpnts.get(k++));
      pnt.scaledAdd (t9 - t10 + t11 + u3 * (-0.20e2) + t5, cpnts.get(k++));
      pnt.scaledAdd (-t15 + u1 * 0.30e2 - t11 + t4, cpnts.get(k++));
      pnt.scaledAdd (t15 - t10 + t3, cpnts.get(k++));
      pnt.scaledAdd (-t9 + t2, cpnts.get(k++));
      pnt.scaledAdd (u0*0.1e1, cpnts.get(k++));
      return pnt;
   }

   /**
    * Calculates the dydx value for a parmeter value u on a quintic bezier
    * segment. The segment is specified by 6 control points within the list
    * {@code cpnts}, starting at the index {@code segNum*6}. Code by Matthew
    * Millard.
    */
   static double calcQuinticBezierCurveDydx (
      double u, List<Point2d> cpnts, int segNum) {

      if (u < 0 || u > 1) {
         throw new IllegalArgumentException (
            "argument u is "+u+"; must be between 0.0 and 1.0");
      }
      int k = segNum*6;
      if (cpnts.size() - k < 6) {
         throw new IllegalArgumentException (
            "cpnts must have at least 6 points beyond segNum*6");
      }

      double t1 = u*u;//u ^ 2;
      double t2 = t1*t1;//t1 ^ 2;
      double t4 = t1 * u;
      double t5 = t4 * 0.20e2;
      double t6 = t1 * 0.30e2;
      double t7 = u * 0.20e2;
      double t10 = t2 * 0.25e2;
      double t11 = t4 * 0.80e2;
      double t12 = t1 * 0.90e2;
      double t16 = t2 * 0.50e2;
      Point2d du = new Point2d();
      du.scale (t2 * (-0.5e1) + t5 - t6 + t7 - 0.5e1, cpnts.get(k++));
      du.scaledAdd (t10 - t11 + t12 + u * (-0.40e2) + 0.5e1, cpnts.get(k++));
      du.scaledAdd (-t16 + t4 * 0.120e3 - t12 + t7, cpnts.get(k++));
      du.scaledAdd (t16 - t11 + t6, cpnts.get(k++));
      du.scaledAdd (-t10 + t5, cpnts.get(k++));
      du.scaledAdd (t2 * 0.5e1, cpnts.get(k++));

      // dydx = dydu/dxdu;
      return du.y/du.x;
   }

   // Serialization methods

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      if (myActiveForceLengthCurve != null) {
         pw.print ("activeForceLengthCurve=");
         myActiveForceLengthCurve.write (pw, fmt, ancestor);
      }
      if (myPassiveForceLengthCurve != null) {
         pw.print ("passiveForceLengthCurve=");
         myPassiveForceLengthCurve.write (pw, fmt, ancestor);
      }
      if (myTendonForceLengthCurve != null) {
         pw.print ("tendonForceLengthCurve=");
         myTendonForceLengthCurve.write (pw, fmt, ancestor);
      }
      if (myForceVelocityCurve != null) {
         pw.print ("forceVelocityCurve=");
         myForceVelocityCurve.write (pw, fmt, ancestor);
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      // if keyword is a property name, try scanning that
      rtok.nextToken();
      if (ScanWriteUtils.scanAttributeName (
             rtok, "activeForceLengthCurve")) {
         myActiveForceLengthCurve = new CubicHermiteSpline1d();
         myActiveForceLengthCurve.scan (rtok, null);
         return true;
      }
      else if (ScanWriteUtils.scanAttributeName (
                  rtok, "passiveForceLengthCurve")) {
         myPassiveForceLengthCurve = new CubicHermiteSpline1d();
         myPassiveForceLengthCurve.scan (rtok, null);
         return true;
      }
      else if (ScanWriteUtils.scanAttributeName (
                  rtok, "tendonForceLengthCurve")) {
         myTendonForceLengthCurve = new CubicHermiteSpline1d();
         myTendonForceLengthCurve.scan (rtok, null);
         return true;
      }
      else if (ScanWriteUtils.scanAttributeName (
                  rtok, "forceVelocityCurve")) {
         myForceVelocityCurve = new CubicHermiteSpline1d();
         myForceVelocityCurve.scan (rtok, null);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   private CubicHermiteSpline1d maybeCopy (CubicHermiteSpline1d curve) {
      if (curve != null) {
         curve = new CubicHermiteSpline1d (curve);
      }
      return curve;
   }

   public Millard2012AxialMuscle clone() {
      Millard2012AxialMuscle mat = (Millard2012AxialMuscle)super.clone();

      mat.myActiveForceLengthCurve = maybeCopy (myActiveForceLengthCurve);
      mat.myPassiveForceLengthCurve = maybeCopy (myPassiveForceLengthCurve);
      mat.myTendonForceLengthCurve = maybeCopy (myTendonForceLengthCurve);
      mat.myForceVelocityCurve = maybeCopy (myForceVelocityCurve);
      mat.myLengthValid = false;

      return mat;
   }

   /**
    * Compute the (very approximate) equivalent SimpleAxialMuscle material, and
    * returns the corresponding rest length. The {@code stiffness} property for
    * the SimpleAxialMuscle is set to the equivalent serial stiffness of the
    * passive and tendon forces (as determined from their final linear
    * behavior). The {@code unilateral} property is also set to {@code true}
    * and the {@code blendInterval} is set to an appropriate value.
    *
    * <p>This can provide a useful alternative material when the Millard model
    * is unstable.
    */
   public double getSimpleMuscle (SimpleAxialMuscle linmat) {
      // linear portions of the passive and tendon curves are defined
      // by their last knots.
      Knot lastKnotP = myPassiveForceLengthCurve.getLastKnot();
      Knot lastKnotT = myTendonForceLengthCurve.getLastKnot();

      double maxF = getMaxIsoForce();
      double lopt = getOptFibreLength();
      double T = getTendonSlackLength();
      double Vmax = getMaxContractionVelocity();
      double H = myHeight;
      double cos = Math.cos(getOptPennationAngle());

      double l0 = cos*lopt + T; // rest length
      double tau = lastKnotP.getX() - 1; // transition region

      double Kp = lastKnotP.getDy(); // max normalized passive stiffness
      double Kt = lastKnotT.getDy(); // max normalized tendon stiffness

      // convert Kp to a passive stiffness Km:
      double Km = sqr(cos)*Kp/lopt; 
      // remove length normalization on Kt:
      Kt = Kt/T;

      double Ke = maxF*(Km*Kt)/(Km+Kt); // effective non-normalized stiffness
      linmat.setStiffness (Ke);
      linmat.setUnilateral (true);
      linmat.setMaxForce (maxF);
      linmat.setBlendInterval (tau);

      linmat.setDamping (0); //maxF*sqr(cos)*getFibreDamping()/(lopt*Vmax));

      return l0;
   }


   public static void main (String[] args) throws IOException {

      Millard2012AxialMuscle m = new Millard2012AxialMuscle();

      // m.writeActiveForceLengthCurve ("MillardAFLC.txt", 0, 2, 400, "%g");
      // m.writePassiveForceLengthCurve ("MillardPFLC.txt", 0, 2, 400, "%g");
      // m.writeTendonForceLengthCurve ("MillardTFLC.txt", 0.9, 1.1, 100, "%g");
      // m.writeForceVelocityCurve ("MillardFVC.txt", 1, -1, 1, 400, "%g");

      CubicHermiteSpline1d pflc0 =
         createPassiveForceLengthCurve (0, 0.7, 0.2, 2.85714, 0.75);

      CubicHermiteSpline1d pflcx =
         createPassiveForceLengthCurve (0, 0.6405, 0.0751000, 6.31630, 0.75);

      CubicHermiteSpline1d tflcA =
         createTendonForceLengthCurve (0.0330000, 37.3920, 0.225500, 0.75);

      CubicHermiteSpline1d tflcB =
         createTendonForceLengthCurve (0.0490000, 28.0612, 0.666667, 0.75);

      try {
         PrintWriter pw = new PrintWriter (new FileWriter ("pflcx.txt"));
         pflcx.write (pw, new NumberFormat("%g"), null);
         pw.close();

         pw = new PrintWriter (new FileWriter ("pflc0.txt"));
         pflc0.write (pw, new NumberFormat("%g"), null);
         pw.close();

         pw = new PrintWriter (new FileWriter ("tflcA.txt"));
         tflcA.write (pw, new NumberFormat("%g"), null);
         pw.close();

         pw = new PrintWriter (new FileWriter ("tflcB.txt"));
         tflcB.write (pw, new NumberFormat("%g"), null);
         pw.close();
      }
      catch (Exception e) {
      }
   }

}
