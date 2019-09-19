package artisynth.core.femmodels;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Deque;

import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import artisynth.core.modelbase.FieldUtils.ScalarFieldFunction;

import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.*;

public class ScalarNodalField extends ScalarFemField {

   DynamicDoubleArray myValues;
   DynamicBooleanArray myValuesSet;
   protected ArrayList<double[]> myValueArrays;  

   protected class NodalFieldFunction 
      extends ScalarFieldFunction {

      public NodalFieldFunction () {
      }

      public ScalarNodalField getField() {
         return ScalarNodalField.this;
      }

      public double eval (FieldPoint def) {
         return getCachedValue (
            def.getElementNumber(), def.getElementSubIndex());
      }
   }

   public ScalarFieldFunction createFieldFunction (boolean useRestPos) {
      return new NodalFieldFunction();
   }

   protected void initValues() {
      myValues = new DynamicDoubleArray();
      myValuesSet = new DynamicBooleanArray();
      updateValueLists();
   }

   protected void updateValueLists() {
      int size = myFem.getNodes().getNumberLimit();
      myValues.resize (size);
      myValuesSet.resize (size);
   }

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public ScalarNodalField () {
   }
   
   public ScalarNodalField (FemModel3d fem) {
      super (fem);
      initValues();
   }

   public ScalarNodalField (FemModel3d fem, double defaultValue) {
      super (fem, defaultValue);
      initValues();
   }

   public ScalarNodalField (String name, FemModel3d fem) {
      this (fem);
      setName (name);
   }

   public ScalarNodalField (String name, FemModel3d fem, double defaultValue) {
      this (fem, defaultValue);
      setName (name);
   }

   public double getValue (int nodeNum) {
      if (myValuesSet.get(nodeNum)) {
         return myValues.get (nodeNum);
      }
      else {
         return myDefaultValue;
      }
   }

   public double getValue (FemNode3d node) {
      return getValue (node.getNumber());
   }

   public void setValue (FemNode3d node, double value) {
      int nodeNum = node.getNumber();
      myValues.set (nodeNum, value);
      myValuesSet.set (nodeNum, true);
   }

   public double getValue (int[] nodeNums, double[] weights) {
      double value = 0;
      for (int i=0; i<nodeNums.length; i++) {
         value += weights[i]*getValue (nodeNums[i]);
      }
      return value;
   }

   protected double[] initValueArray (int elemIdx) {
      FemElement3dBase elem = getElementAtIndex (elemIdx);

      FemNode3d[] nodes = (FemNode3d[])elem.getNodes();
      int[] nodeNums = new int[nodes.length];
      double[] weights = new double[nodes.length];
      for (int i=0; i<nodes.length; i++) {
         nodeNums[i] = nodes[i].getNumber();
      }
      IntegrationPoint3d[] ipnts = elem.getAllIntegrationPoints();
      double[] varray = new double[ipnts.length];
      for (int k=0; k<ipnts.length; k++) {
         VectorNd N = ipnts[k].getShapeWeights();
         for (int i=0; i<nodes.length; i++) {
            weights[i] = N.get(i);
         }
         varray[k] = getValue (nodeNums, weights);
      }
      return varray;
   }

   void initializeCache() {
      myShellIndexOffset = myFem.getElements().getNumberLimit();
      int maxelems =
         myShellIndexOffset + myFem.getShellElements().getNumberLimit();
      myValueArrays = new ArrayList<double[]>(maxelems);
      for (int i=0; i<maxelems; i++) {
         myValueArrays.add (null);
      }
   }

   protected double getCachedValue (int elemIdx, int subIdx) {
      if (myValueArrays == null) {
         initializeCache();
      }
      double[] varray = myValueArrays.get(elemIdx);
      if (varray == null) {
         varray = initValueArray (elemIdx);
         myValueArrays.set (elemIdx, varray);
      }
      return varray[subIdx];
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
      VectorNd weights = new VectorNd(elem.numNodes());
      elem.getMarkerCoordinates (weights, null, loc, /*checkInside=*/false);
      FemNode3d[] nodes = elem.getNodes();
      int[] nodeNums = new int[nodes.length];
      for (int i=0; i<nodeNums.length; i++) {
         nodeNums[i] = nodes[i].getNumber();
      }
      return getValue (nodeNums, weights.getBuffer());
   }
   
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.println ("values=");
      writeValues (
         pw, fmt, myValues, myValuesSet, 
         new NodeWritableTest(myFem.getNodes()));
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "values")) {
         myValues = new DynamicDoubleArray();
         myValuesSet = new DynamicBooleanArray();
         scanValues (rtok, myValues, myValuesSet);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);      
   }

   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      updateValueLists();
   }

   public void getSoftReferences (List<ModelComponent> refs) {
      for (int i=0; i<myValues.size(); i++) {
         if (myValuesSet.get(i)) {
            refs.add (myFem.getNodes().getByNumber(i));
         }
      }
   }

   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      if (undo) {
         restoreReferencedValues (myValues, myValuesSet, undoInfo);
      }
      else {
         removeUnreferencedValues (
            myValues, myValuesSet, 
            new NodeReferencedTest (myFem.getNodes()), undoInfo);
      }
   }

   public void clearCacheIfNecessary() {
      myValueArrays = null;
   }
   
}
