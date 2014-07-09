/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import maspack.matrix.*;

/**
 * Contains information related to the input or output channels of a probe.
 */
public class NumericProbeVariable {
   // String myName;
   double[] myValues;

   public NumericProbeVariable (int dimension) {
      // myName = null;
      myValues = new double[dimension];
   }

   public NumericProbeVariable (NumericProbeVariable var) {
      myValues = new double[var.getDimension()];
   }

   // public void setName (String name)
   // {
   // myName = name;
   // }

   // public String getName()
   // {
   // return myName;
   // }

   public void setDimension (int d) {
      if (d <= 0) {
         throw new IllegalArgumentException ("size must be greater than 0");
      }
      myValues = new double[d];
   }

   public int getDimension() {
      return myValues.length;
   }

   public String toString() {
      return "NumericProbeVariable(" + myValues.length + ")";
   }

   public double[] getValues() {
      return myValues;
   }

   public void getValues (double[] vals) {
      for (int i = 0; i < myValues.length; i++) {
         vals[i] = myValues[i];
      }
   }

   public Object getValue() {
      if (myValues.length == 1) {
         return new Double (myValues[0]);
      }
      else {
         return new VectorNd (myValues);
      }
   }

   // public void setValues (Object obj)
   // {

   // for (int i=0; i<myValues.length; i++)
   // { myValues[i] = vals[i];
   // }
   // }

   public void setValues (double[] buf) {
      setValues (buf, 0);
   }

   public void setValues (double[] buf, int offset) {
      if (buf.length + offset < myValues.length) {
         throw new IllegalArgumentException ("insufficient input data");
      }
      for (int i = 0; i < myValues.length; i++) {
         myValues[i] = buf[offset + i];
      }
   }

}
