/**
 * copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.NumberFormat;
import maspack.util.RandomGenerator;

/**
 * Dual Number with Double values, d = a + e*b, where e*e = 0;
 * @author antonio
 *
 */
public class DualScalar {

   private double a;
   private double b;
   
   /**
    * Constructs dual number a+e*b
    */
   public DualScalar(double a, double b) {
      set(a,b);
   }
   
   public DualScalar(DualScalar d) {
      set(d);
   }
   
   /**
    * Assigns to dual number a+e*b
    */
   public void set(double a, double b) {
      this.a = a;
      this.b = b;
   }
   
   /**
    * Copies values from d
    */
   public void set(DualScalar d) {
      this.a = d.a;
      this.b = d.b;
   }
   
   /**
    * Copies values to d
    */
   public void get(DualScalar d) {
      d.a = this.a;
      d.b = this.b;
   }
   
   /**
    * Assigns to dual number v[0]+e*v[1]
    */
   public void set(double[] v) {
      this.a = v[0];
      this.b = v[1];
   }
   
   /**
    * Fills in dual values v[0]+e*v[1]
    */
   public void get(double [] v) {
      v[0] = a;
      v[1] = b;
   }
   
   /**
    * Adds dual numbers d1+d2 and places this result in this dual number
    */
   public void add(DualScalar d1, DualScalar d2) {
      this.a = d1.a + d2.a;
      this.b = d1.b + d2.b;
   }
   
   /**
    * Adds the supplied dual number to this
    */
   public void add(DualScalar d ) {
      add(this,d);
   }
   
   /**
    * Subtracts d2 from d1 and places the result in this dual number
    */
   public void sub(DualScalar d1, DualScalar d2) {
      this.a = d1.a - d2.a;
      this.b = d1.b - d2.b;
   }
   
   /**
    * Subtracts d from this dual number
    */
   public void sub(DualScalar d) {
      sub(this,d);
   }
   
   /**
    * Multiplies dual numbers d1*d2 and places the result in this
    */
   public void mul(DualScalar d1, DualScalar d2) {
      double a = d1.a*d2.a;
      double b = d1.a*d2.b + d1.b*d2.a;   
      this.a = a;
      this.b = b;
   }
   
   /**
    * Multiplies by the supplied dual number
    */
   public void mul(DualScalar d) {
      mul(this,d);
   }
   
   /**
    * Divides dual numbers d1/d2 and places the result in this
    */
   public void div(DualScalar d1, DualScalar d2) {
      double a = d1.a/d2.a;
      double b = (d1.b*d2.a-d1.a*d2.b)/d2.a/d2.a;
      this.a = a;
      this.b = b;
   }
   
   /**
    * Divides by the supplied dual number
    */
   public void div(DualScalar d) {
      div(this, d);
   }

   /**
    * Assigns this dual number to the conjugate of d: (a+e*b)' = a-e*b 
    */
   public void conjugate(DualScalar d) {
      this.a = d.a;
      this.b = -d.b;
   }
   
   /**
    * Conjugates the current dual number (a+e*b)' = a-e*b
    */
   public void conjugate() {
      this.b = -this.b;
   }
   
   /**
    * Magnitude of |a+e*b| = |a|
    */
   public double norm() {
      return Math.abs(a);
   }
   
   /**
    * Computes d^(1/2) and places the result in this. 
    */
   public void sqrt(DualScalar d) {
      double a = Math.sqrt(d.a);
      this.b = d.b/(2*a);
      this.a = a;
   }
   
   /**
    * Computes sqrt(this) 
    */
   public void sqrt() {
      sqrt(this);
   }
   
   /**
    * Computes d^e and places the result in this
    */
   public void pow(DualScalar d, DualScalar e) {
      double a = Math.pow(d.a, e.a);
      this.b = d.b/d.a*e.a*a + e.b*a*Math.log(d.a);
      this.a = a;
   }
   
   /**
    * Computes (this)^e
    */
   public void pow(DualScalar e) {
      pow(this,e);
   }
   
   /**
    * Computes 1/d and places this result in this
    */
   public void invert(DualScalar d) {
      this.a = 1/d.a;
      this.b = -d.b*this.a*this.a;
   }
   
   public void invert() {
      invert(this);
   }
   
   public double getReal() {
      return a;
   }
   
   public double getDual() {
      return b;
   }
   
   public String toString () {
      return ("("+a+" "+b+")");
   }

   public String toString (String fmtStr) {
      NumberFormat fmt = new NumberFormat();
      return ("("+fmt.format(a)+" "+fmt.format(b)+")");
   }
}
