package maspack.function;

import java.util.*;
import java.io.*;
import maspack.util.*;
import maspack.matrix.*;

/**
 * Implements a linear function of n arguments defined by a set of
 * n+1 coefficients c:
 *<pre> 
 * y = sum c[i]*in[i] + c[n]
 *</pre>
 */
public class LinearFunctionNx1 implements Diff1FunctionNx1, Scannable {

   private double[] myC;
   private int myInputSize;

   /**
    * Creates an empty LinearFunctionNx1
    */
   public LinearFunctionNx1 () {
      myC = new double[0];
   }
   
   /**
    * Creates a new LinearFunctionNx1 with the specified coefficients.
    *
    * @param coefs function coefficents (will be copied)
    */
   public LinearFunctionNx1 (double[] coefs) {
      setCoefficients (coefs);
   }

   /**
    * Creates a new LinearFunctionNx1 with the specified coefficients.
    *
    * @param coefs function coefficents (will be copied)
    */
   public LinearFunctionNx1 (VectorNd coefs) {
      myC = new double[coefs.size()];
      coefs.get (myC);
   }

   /**
    * Creates a new LinearFunctionNx1 with a single argument
    * defined by
    * <pre>
    * y = m*in[0] + b
    * </pre>
    *
    * @param m slope of the function
    * @param b intercept of the function
    */
   public LinearFunctionNx1 (double m, double b) {
      setCoefficients (m, b);
   }

   /**
    * Sets this function to one with a single argument defined by
    * <pre>
    * y = m*in[0] + b
    * </pre>
    *
    * @param m slope of the function
    * @param b intercept of the function
    */
   public void setCoefficients (double m, double b) {
      setCoefficients (new double[] {m, b});
   }

   /**
    * Sets the coefficients of this function.
    *
    * @param coefs function coefficents (will be copied)
    */
   public void setCoefficients (double[] coefs) {
      myC = Arrays.copyOf (coefs, coefs.length);
      myInputSize = Math.max (0, myC.length-1);
   }

   /**
    * Returns the coefficients of this function.
    *
    * @return function coefficents (should not be modified)
    */
   public double[] getCoefficients() {
      return myC;
   }

   /**
    * {@inheritDoc}
    */
   public int inputSize() {
      return myInputSize;
   }


   /**
    * {@inheritDoc}
    */
   public double eval (VectorNd in) {
      if (in.size() != myInputSize) {
         throw new IllegalArgumentException (
            "argument 'in' has size "+in.size()+" vs. "+myInputSize);
      }
      double val = 0;
      int i;
      for (i=0; i<myInputSize; i++) {
         val += myC[i]*in.get(i);
      }
      if (i < myC.length) {
         val += myC[i];
      }
      return val;
   }

   /**
    * {@inheritDoc}
    */
   public double eval (VectorNd deriv, VectorNd in) {
      if (in.size() != myInputSize) {
         throw new IllegalArgumentException (
            "argument 'in' has size "+in.size()+" vs. "+myInputSize);
      }
      if (deriv != null) {
         if (deriv.size() != myInputSize) {
            deriv.setSize (myInputSize);
         }
         for (int i=0; i<myInputSize; i++) {
            deriv.set (i, myC[i]);
         }
      }
      double val = 0;
      int i;
      for (i=0; i<myInputSize; i++) {
         val += myC[i]*in.get(i);
      }
      if (i < myC.length) {
         val += myC[i];
      }
      return val;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isWritable() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      rtok.scanToken ('[');
      DynamicDoubleArray values = new DynamicDoubleArray();
      while (rtok.nextToken() != ']') {
         if (!rtok.tokenIsNumber()) {
            throw new IOException ("Numeric value expected, "+rtok);
         }
         values.add (rtok.nval);
      }
      myC = Arrays.copyOf (values.getArray(), values.size());
      myInputSize = Math.max (0, myC.length-1);
   }

   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      pw.print ("[ ");
      for (int i=0; i<myC.length; i++) {
         pw.print (fmt.format(myC[i])+" ");
      }
      pw.println ("]");
   }

   /**
    * Returns {@code true} if {@code fxn} is equal to this function.
    */
   public boolean equals (LinearFunctionNx1 fxn) {
      if (myC.length != fxn.myC.length) {
         return false;
      }
      for (int i=0; i<myC.length; i++) {
         if (myC[i] != fxn.myC[i]) {
            return false;
         }
      }
      return true;      
   }

   @Override
   public LinearFunctionNx1 clone ()  {
      LinearFunctionNx1 fxn;
      try {
         fxn = (LinearFunctionNx1)super.clone();
      }
      catch (Exception e) {
         throw new InternalErrorException ("clone not supported for "+this);
      }
      if (myC != null) {
         fxn.setCoefficients (myC);
      }
      return fxn;
   }     

}
