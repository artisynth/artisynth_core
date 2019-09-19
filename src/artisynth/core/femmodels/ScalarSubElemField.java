package artisynth.core.femmodels;

import java.io.*;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.FemElement.ElementClass;
import artisynth.core.util.*;
import artisynth.core.modelbase.FieldUtils.ScalarFieldFunction;

import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.PropertyDesc.TypeCode;
import maspack.properties.PropertyDesc;

public class ScalarSubElemField extends ScalarFemField {
   
   protected ArrayList<double[]> myValueArrays;
   protected ArrayList<double[]> myShellValueArrays;

   protected class SubElemFieldFunction 
      extends ScalarFieldFunction {

      public SubElemFieldFunction () {
      }

      public ScalarSubElemField getField() {
         return ScalarSubElemField.this;
      }

      public double eval (FieldPoint def) {
         if (def.getElementType() == 0) {
            return getValue (
               def.getElementNumber(), def.getElementSubIndex());
         }
         else {
            return getShellValue (
               def.getElementNumber(), def.getElementSubIndex());
         }
      }
   }

   public ScalarFieldFunction createFieldFunction (boolean useRestPos) {
      return new SubElemFieldFunction();
   }

   protected void initValues () {
      myValueArrays = new ArrayList<double[]>();
      myShellValueArrays = new ArrayList<double[]>();
      updateValueLists();
   }

   protected void updateValueLists() {
      resizeArrayList (
         myValueArrays, myFem.getElements().getNumberLimit());
      resizeArrayList (
         myShellValueArrays, myFem.getShellElements().getNumberLimit());
   }

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public ScalarSubElemField () {
   }

   public ScalarSubElemField (
      FemModel3d fem, double defaultValue) {
      super (fem, defaultValue);
      initValues ();
   }

   public ScalarSubElemField (FemModel3d fem)  {
      super (fem);
      initValues ();
   }

   public ScalarSubElemField (String name, FemModel3d fem) {
      this (fem);
      setName (name);
   }

   public ScalarSubElemField (String name, FemModel3d fem, double defaultValue) {
      this (fem, defaultValue);
      setName (name);
   }

   protected double[] initValueArray (FemElement3dBase elem) {
      double[] varray = new double[elem.numAllIntegrationPoints()];
      for (int i=0; i<varray.length; i++) {
         varray[i] = myDefaultValue;
      }
      return varray;
   }

   public double getValue (int elemIdx, int subIdx) {
      double[] varray = myValueArrays.get(elemIdx);
      if (varray == null) {
         return myDefaultValue;
      }
      else {
         return varray[subIdx];
      }
   }

   public double getShellValue (int elemIdx, int subIdx) {
      double[] varray = myShellValueArrays.get(elemIdx);
      if (varray == null) {
         return myDefaultValue;
      }
      else {
         return varray[subIdx];
      }
   }

   public double getValue (FemElement3dBase elem, int subIdx) {
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         return getValue (elem.getNumber(), subIdx);
      }
      else {
         return getShellValue (elem.getNumber(), subIdx);
      }
   }

   public void setValue (FemElement3dBase elem, int subIdx, double value) {
      int elemNum = elem.getNumber();
      if (subIdx >= elem.numAllIntegrationPoints()) {
         throw new IllegalArgumentException (
            "subIdx=" + subIdx + ", maximum value for element " + elem +
            " is " + (elem.numAllIntegrationPoints()-1));
      }
      ArrayList<double[]> valueArrays;
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         valueArrays = myValueArrays;
      }
      else {
         valueArrays = myShellValueArrays;
      }
      double[] varray = valueArrays.get(elemNum);
      if (varray == null) {
         varray = initValueArray (elem);
         valueArrays.set (elemNum, varray);
      }
      varray[subIdx] = value;
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.print ("values=");
      writeScalarValueArrays (
         pw, fmt, myValueArrays, 
         new ElementWritableTest (myFem.getElements()));
      pw.print ("shellValues=");
      writeScalarValueArrays (
         pw, fmt, myShellValueArrays, 
         new ElementWritableTest (myFem.getShellElements()));
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "values")) {
         myValueArrays = new ArrayList<double[]>();
         scanScalarValueArrays (rtok, myValueArrays);
         return true;
      }
      else if (scanAttributeName (rtok, "shellValues")) {
         myShellValueArrays = new ArrayList<double[]>();
         scanScalarValueArrays (rtok, myShellValueArrays);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);      
   }

   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      updateValueLists();
      // sanity check on number of values in each array
      checkScalarValueArraysSizes (myValueArrays, ElementClass.VOLUMETRIC);
      checkScalarValueArraysSizes (myShellValueArrays, ElementClass.SHELL);
   }

   public void getSoftReferences (List<ModelComponent> refs) {
      for (int i=0; i<myValueArrays.size(); i++) {
         if (myValueArrays.get(i) != null) {
            refs.add (myFem.getElements().getByNumber(i));
         }
      }
      for (int i=0; i<myShellValueArrays.size(); i++) {
         if (myShellValueArrays.get(i) != null) {
            refs.add (myFem.getShellElements().getByNumber(i));
         }
      }
   }

   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      if (undo) {
         restoreReferencedValues (myValueArrays, undoInfo);
         restoreReferencedValues (myShellValueArrays, undoInfo);
      }
      else {
         removeUnreferencedValues (
            myValueArrays, 
            new ElementReferencedTest (myFem.getElements()), undoInfo);
         removeUnreferencedValues (
            myShellValueArrays, 
            new ElementReferencedTest (myFem.getShellElements()), undoInfo);
      }
   }

   public double getValue (Point3d pos) {
      Point3d loc = new Point3d();
      FemElement3dBase elem = myFem.findNearestElement (loc, pos);
      if (elem == null) {
         // shouldn't happen, but just in case
         return myDefaultValue;
      }
      // TODO: if loc != pnt, then we are outside the element and we may want
      // to handle this differently - like by returning the default value.
      double[] values = myValueArrays.get (getElementIndex(elem));
      if (values == null) {
         return myDefaultValue;
      }
      int npnts = elem.numAllIntegrationPoints();
      if (values.length != npnts) {
         throw new InternalErrorException (
            "Number of values for "+elemName(elem)+" is "+values.length+
            ", but number of integration points is "+npnts);
      }
      VectorNd weights = new VectorNd(elem.numNodes());
      elem.getMarkerCoordinates (weights, null, loc, /*checkInside=*/false);
      // nodal extrapolation matrix maps integration point values to nodes
      double[] Mex = elem.getNodalExtrapolationMatrix().getBuffer();
      double value = 0;
      for (int i=0; i<elem.numNodes(); i++) {
         for (int j=0; j<npnts; j++) {
            double a = Mex[i*npnts+j];
            if (a != 0) {
               value += weights.get(i)*a*values[j];
            }
         }
      }
      return value;
   }

}
