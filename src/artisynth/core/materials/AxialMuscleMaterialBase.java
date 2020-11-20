package artisynth.core.materials;

import java.io.*;
import java.util.*;

import maspack.interpolation.CubicHermiteSpline1d;

import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;
import artisynth.core.modelbase.*;
import maspack.properties.*;
import maspack.util.*;

/**
 * Base class for point-to-point muscle materials that contains optional
 * attributes for defining force-length curves.
 */
public abstract class AxialMuscleMaterialBase extends AxialMaterial {

   CubicHermiteSpline1d myActiveForceLengthCurve;
   CubicHermiteSpline1d myPassiveForceLengthCurve;
   CubicHermiteSpline1d myTendonForceLengthCurve;
   CubicHermiteSpline1d myForceVelocityCurve;

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

}
