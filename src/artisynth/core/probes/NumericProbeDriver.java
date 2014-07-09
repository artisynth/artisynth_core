/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.util.*;
import org.python.util.*;
import org.python.core.*;

import artisynth.core.util.JythonInit;
import maspack.properties.*;
import maspack.matrix.*;

/**
 * Encapsulates the code used to map inputs to properties for a
 * NumericInputProbe, or properties to outputs for a NumericOutputProbe.
 */
public class NumericProbeDriver {
   String myExpression = "";
   double[] myValues = new double[0];
   // LinkedHashMap<String,NumericProbeVariable> myVariables;
   String myVariable;
   PyCode myCode;
   NumericConverter myConverter;

   public NumericProbeDriver() {
      // myVariables = new LinkedHashMap<String,NumericProbeVariable>();
      myExpression = "";
      myVariable = null;
      myValues = new double[0];
      myCode = null;
      myConverter = null;
   }

   public NumericProbeDriver (NumericProbeDriver driver) {
      myExpression = driver.myExpression;
      myValues = new double[driver.myValues.length];
      myVariable = driver.myVariable;
      myCode = driver.myCode;
      if (driver.myConverter != null) {
         myConverter = new NumericConverter (driver.myConverter);
      }
      else {
         myConverter = null;
      }
   }

   private void setOutputSize (int size) {
      if (size <= 0) {
         throw new IllegalArgumentException (
            "output size must be greater than 0");
      }
      myValues = new double[size];
   }

   public int getOutputSize() {
      return myValues.length;
   }

   public String getExpression() {
      return myExpression;
   }

   public void setInvalid() {
      myExpression = "";
      myVariable = null;
   }

   public boolean isValid() {
      return !myExpression.equals ("");
   }

   public String getSingleVariable() {
      return myVariable;
   }

   private String extractSingleVariable (String expr) {
      int idx = 0;
      while (idx < expr.length() && Character.isWhitespace (expr.charAt (idx))) {
         idx++;
      }
      if (idx == expr.length()) {
         return null;
      }
      int idx0 = idx;
      while (idx < expr.length() &&
             !Character.isWhitespace (expr.charAt (idx))) {
         idx++;
      }
      String varname = expr.substring (idx0, idx);
      if (NumericProbeBase.isValidVariableName (varname)) {
         return varname;
      }
      else {
         return null;
      }
   }

   public void compileJythonExpression (
      String expr, HashMap<String,NumericProbeVariable> variables) {
      PyCode code = null;
      CompileMode myCmode = CompileMode.getMode ("eval");
      CompilerFlags myCflags = new CompilerFlags();
      JythonInit.init();
      try {
         code = (PyTableCode)Py.compile_command_flags (
            expr, "<input>", myCmode, myCflags, true);
         //code = (PyTableCode)__builtin__.compile (expr, "<>", "eval");
      }
      catch (Exception e) {
         throw new IllegalArgumentException ("error parsing expression");
      }
      PyStringMap locals = JythonInit.getArtisynthLocals().copy();
      for (Map.Entry<String,NumericProbeVariable> entry : variables.entrySet()) {
         NumericProbeVariable var = entry.getValue();
         locals.__setitem__ (
            new PyString (entry.getKey()), Py.java2py (var.getValue()));
      }
      locals.__setitem__ ("t", Py.java2py (1));
      Object res = null;
      try {
         res = Py.tojava (Py.runCode (code, locals, locals), Object.class);
      }
      catch (Exception e) {
         e.printStackTrace();
         throw new IllegalArgumentException ("error evaluating expression");
      }
      NumericConverter conv = null;
      try {
         conv = new NumericConverter (res);
      }
      catch (Exception e) {
         throw new IllegalArgumentException (
            "expression does not return a numeric result");
      }
      setOutputSize (conv.getDimension());
      myCode = code;
      myConverter = conv;
      myExpression = expr;
      myVariable = null;
   }

   public void setExpression (
      String expr, HashMap<String,NumericProbeVariable> variables) {
      String varname = extractSingleVariable (expr);
      if (varname != null) {
         NumericProbeVariable var = variables.get (varname);
         if (var == null) {
            throw new IllegalArgumentException ("variable '" + varname
            + "' not found");
         }
         setOutputSize (var.getDimension());
         myVariable = varname;
         myExpression = varname;
         myCode = null;
         myConverter = null;
      }
      else {
         if (!JythonInit.jythonIsAvailable()) {
            throw new IllegalArgumentException (
               "jython expressions not available on this host");
         }
         compileJythonExpression (expr, variables);
      }
   }

   private int indexOfVariableName (String expr, String vname, int idx) {
      int startIdx = expr.indexOf (vname, idx);
      if (startIdx == -1) {
         return -1;
      }
      if (startIdx > idx) { // check previous character
         int prevc = expr.charAt (startIdx - 1);
         if (Character.isJavaIdentifierPart (prevc)) {
            return -1;
         }
      }
      if (startIdx + vname.length() < expr.length()) {
         // check following character
         int nextc = expr.charAt (startIdx + vname.length());
         if (Character.isJavaIdentifierPart (nextc)) {
            return -1;
         }
      }
      return startIdx;
   }

   private boolean replaceVariable (String oldname, String newname) {
      StringBuilder buf = null;
      int oldIdx = 0;
      int newIdx = 0;
      int nameIdx;
      while ((nameIdx = indexOfVariableName (myExpression, oldname, oldIdx)) != -1) {
         if (buf == null) {
            buf = new StringBuilder (512);
         }
         for (int idx = oldIdx; idx < nameIdx; idx++) {
            buf.append (myExpression.charAt (idx));
         }
         buf.append (newname);
         newIdx += newname.length() + (nameIdx - oldIdx);
         oldIdx += oldname.length() + (nameIdx - oldIdx);
      }
      if (buf != null) {
         myExpression = buf.toString();
         return true;
      }
      else {
         return false;
      }
   }

   public boolean usesVariable (String vname) {
      if (myVariable != null) {
         return myVariable.equals (vname);
      }
      else {
         return indexOfVariableName (myExpression, vname, 0) != -1;
      }
   }

   public boolean usesJythonExpression() {
      return myCode != null;
   }

   public boolean renameVariable (String oldname, String newname) {
      if (myVariable != null) {
         if (myVariable.equals (oldname)) {
            myVariable = newname;
            myExpression = newname;
            return true;
         }
      }
      else {
         return replaceVariable (oldname, newname);
      }
      return false;
   }

   public double[] eval (
      HashMap<String,NumericProbeVariable> variables, PyStringMap locals) {
      if (myVariable != null) {
         NumericProbeVariable var = variables.get (myVariable);
         if (var != null) {
            var.getValues (myValues);
         }
         return myValues;
      }
      else if (myCode != null) {
         Object res =
            Py.tojava (Py.runCode (myCode, locals, locals), Object.class);
         return myConverter.objectToArray (res);
      }
      else {
         return null;
      }
   }

   public String toString() {
      if (myExpression == "") {
         return null;
      }
      else {
         return "\"" + myExpression + "\"";
      }
   }
}
