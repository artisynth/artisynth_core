package artisynth.core.materials;

import java.util.ArrayList;
import java.io.*;

import artisynth.core.mechmodels.CollisionResponse;
import artisynth.core.mechmodels.ContactForceBehavior;
import artisynth.core.mechmodels.ContactPoint;
import artisynth.core.mechmodels.CollisionHandler;
import maspack.collision.*;
import maspack.geometry.*;
import maspack.util.*;
import maspack.matrix.Vector3d;

/**
 * Implementation of non-linear elastic foundation compliance model from:
 * Bei, Y., & Fregly, B. J. (2004). Multibody dynamic simulation of knee 
 * contact mechanics. Medical engineering & physics, 26(9), 777-789.
 * 
 * @author stavness
 */
public class LinearElasticContact
   implements ContactForceBehavior, Scannable {
   
   double tol = 1e-10; // tolerance on distance < thickness
   double myPoissonRatio;
   double myYoungsModulus;
   double myThickness;
   double myDamping;
   double K;
   CollisionResponse response = null;

   /**
    * Need no-args constructor for scanning
    */
   public LinearElasticContact () {
   }

   public LinearElasticContact (double youngsModulus, 
      double poissionRatio, double damping, double thickness) {
//      handler = h;
      myThickness = thickness;
      myYoungsModulus = youngsModulus;
      myDamping = damping;
      myPoissonRatio = poissionRatio;
      updateStiffness ();
   }

   public void updateStiffness() {
      K = -(1-myPoissonRatio)*myYoungsModulus/((1+myPoissonRatio)*(1-2*myPoissonRatio));      
   }
   
   public void setResponse(CollisionResponse resp) {
      response = resp;
   }
   
   @Override
   public void computeResponse (
      double[] fres, double dist, ContactPoint cpnt1, ContactPoint cpnt2,
      Vector3d normal, double area) {
      

      // XXX assume cpnt1 is always interpenetrating vertex
      //double area = 0.001d;

      
      
      if (area==-1) {
         System.err.println (
            "EFContact: no region supplied; must use AJL_CONTOUR collisions");
      }
      
      if (myThickness+dist<tol) { // XXX how best to handle penetration larger than thickness?
         dist = tol-myThickness;
      }
      
      fres[0] = -K*Math.log (1+dist/myThickness)*area; // force (distance is negative)
      fres[1] = -(myThickness+dist)/(K*area); // compliance
      fres[2] = myDamping;
      
   //   System.out.printf("d=%1.2e, area=%1.2e, [ %8.8f, %4.2f, %4.2f ]\n",dist,area,fres[0],fres[1],fres[2]);
   }

   public double getPoissonRatio () {
      return myPoissonRatio;
   }

   public void setPoissonRatio (double poissionRatio) {
      myPoissonRatio = poissionRatio;
      updateStiffness ();
   }

   public double getYoungsModulus () {
      return myYoungsModulus;
   }

   public void setYoungsModulus (double youngsModulus) {
      this.myYoungsModulus = youngsModulus;
      updateStiffness ();
   }

   public double getThickness () {
      return myThickness;
   }

   public void setThickness (double thickness) {
      this.myThickness = thickness;
   }

   public double getDamping () {
      return myDamping;
   }

   public void setDamping (double damping) {
      this.myDamping = damping;
   }
   
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (rtok.tokenIsWord()) {
            if (rtok.sval.equals ("PoissonsRatio")) {
               rtok.scanToken ('=');
               myPoissonRatio = rtok.scanNumber();
            }
            else if (rtok.sval.equals ("YoungsModulus")) {
               rtok.scanToken ('=');
               myYoungsModulus = rtok.scanNumber();
            }
            else if (rtok.sval.equals ("thickness")) {
               rtok.scanToken ('=');
               myThickness = rtok.scanNumber();
            }
            else if (rtok.sval.equals ("damping")) {
               rtok.scanToken ('=');
               myDamping = rtok.scanNumber();
            }
            else {
               throw new IOException (
                  "Error scanning " + getClass().getName() +
                  ": unexpected attribute name: " + rtok);
            }
         }
         else {
            throw new IOException (
               "Error scanning " + getClass().getName() +
               ": unexpected token: " + rtok);
         }
      }
      updateStiffness ();
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {

      IndentingPrintWriter.printOpening (pw, "[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      pw.println ("YoungsModulus=" + fmt.format(myYoungsModulus));
      pw.println ("PoissonsRatio=" + fmt.format(myPoissonRatio));
      pw.println ("thickness=" + fmt.format(myThickness));
      pw.println ("damping=" + fmt.format(myDamping));
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");     
   }

   public boolean isWritable() {
      return true;
   }
   
   public LinearElasticContact clone() 
      throws CloneNotSupportedException {
      return (LinearElasticContact)super.clone();
   }
   
}
