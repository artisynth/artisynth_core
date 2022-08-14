package artisynth.core.femmodels;

import java.io.*;
import java.util.ArrayList;
import java.util.Deque;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.PropertyDesc.TypeCode;
import maspack.properties.PropertyDesc;

/**
 * Base class for scalar field defined over an FEM model.
 */
public abstract class ScalarFemField
   extends FemFieldComp implements ScalarFieldComponent {

   double myDefaultValue = 0;   

   public ScalarFemField () {
   }

   public ScalarFemField (FemModel3d fem) {
      myDefaultValue = 0;
      setFem (fem);
   }

   public ScalarFemField (FemModel3d fem, double defaultValue) {
      myDefaultValue = defaultValue;
      setFem (fem);
   }

   /**
    * {@inheritDoc}
    */
   public double getValue (MeshFieldPoint fp) {
      return getValue(fp.getPosition());
   }
   
   /* ---- Begin I/O methods ---- */

   protected void writeValues (
      PrintWriter pw, NumberFormat fmt, DynamicDoubleArray values, 
      DynamicBooleanArray valuesSet, WritableTest writableTest)
      throws IOException {

      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (int num=0; num<values.size(); num++) {
         if (!valuesSet.get(num) || !writableTest.isWritable(num)) {
            pw.println ("null");
         }
         else {
            pw.println (fmt.format (values.get(num)));
         }
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }
 
   protected void scanValues (
      ReaderTokenizer rtok,
      DynamicDoubleArray values, DynamicBooleanArray valuesSet)
      throws IOException {

      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (rtok.tokenIsWord() && rtok.sval.equals ("null")) {
            values.add (0);
            valuesSet.add (false);
         }
         else if (rtok.tokenIsNumber()) {
            values.add (rtok.nval);
            valuesSet.add (true);
         }
         else {
            throw new IOException ("Expecting number or 'null', got "+rtok);
         }
      }
   }

   /**
    * {@inheritDoc}
    */  
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "defaultValue")) {
         myDefaultValue = rtok.scanNumber();
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);      
   }

   /**
    * {@inheritDoc}
    */  
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.println ("defaultValue=" + fmt.format(myDefaultValue));
   }

   /**
    * {@inheritDoc}
    */  
   public abstract double getValue (Point3d pos);
   
   /**
    * Clear all values defined for the features (e.g., nodes, elements)
    * associated with this field. After this call, the field will have a
    * uniform value defined by its {@code defaultValue}.
    */
   public abstract void clearAllValues();

   /**
    * Returns the default value for this field. See {@link #setDefaultValue}.
    *
    * @return default value for this field
    */
   public double getDefaultValue() {
      return myDefaultValue;
   }

   /**
    * Sets the default value for this field. Default values are used at
    * features (e.g., nodes, elements) for which values have not been
    * explicitly specified.
    * 
    * @param value new default value for this field
    */
   public void setDefaultValue (double value) {
      myDefaultValue = value;
   }
   
   /**
    * Checks if two values/valset pairings are equal. Each pairing is described
    * by a double dynamic array giving the value, and a boolean dynamic array
    * indicated if the value is actually set. When comparing the pairs, if a
    * value is not set for some index {@code i}, then the actual values at
    * {@code i} are ignored.
    */
   protected boolean valueSetArraysEqual (
      DynamicDoubleArray values0, DynamicBooleanArray valset0, 
      DynamicDoubleArray values1, DynamicBooleanArray valset1) {

      if (values0.size() != valset0.size()) {
         throw new IllegalArgumentException (
            "values0 and valset0 have different sizes");
      }
      if (values1.size() != valset1.size()) {
         throw new IllegalArgumentException (
            "values1 and valset1 have different sizes");
      }
      if (valset0.size() != valset1.size()) {
         return false;
      }
      for (int i=0; i<values0.size(); i++) {
         if (valset0.get(i) != valset1.get(i) ||
             (valset0.get(i) && values0.get(i) != values1.get(i))) {
            return false;
         }
      }
      return true;      
   }

   /**
    * Returns {@code true} if this field is functionally equal to another field.
    * Intended mainly for testing and debugging.
    */
   public boolean equals (ScalarFemField field) {
      return (
         super.equals (field) &&
         myDefaultValue == field.getDefaultValue());
   }

}
