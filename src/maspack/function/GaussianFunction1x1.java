/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

public class GaussianFunction1x1 implements SISOFunction {

   private double m;
   private double s;
   private double s2x2;
   private double a;
   
   public GaussianFunction1x1(double mean, double sd) {
      setMean(mean);
      setSD(sd);
      setScaleFactor(1);
   }
   
   public void setMean(double mean) {
      m = mean;
   }
   public double getMean() {
      return m;
   }
   public double getSD() {
      return s;
   }
   public void setSD(double sd) {
      s = sd;
      s2x2 = s*s*2;
   }
   
   public void setScaleFactor(double a) {
      this.a = a;
   }
   
   public double getScaleFactor() {
      return a;
   }
   
   public double eval(double x) {
      return a*Math.exp( -(x-m)*(x-m)/(s2x2) );
   }
   
}
