package artisynth.core.femmodels;

import java.io.*;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import artisynth.core.modelbase.FieldUtils.VectorFieldFunction;

import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.*;
import maspack.render.*;

public class VectorNodalField<T extends VectorObject<T>> 
   extends VectorFemField<T> {

   ArrayList<T> myValues;
   protected ArrayList<T[]> myValueArrays;   

   protected class NodalFieldFunction 
      extends VectorFieldFunction<T> {

      public NodalFieldFunction () {
      }

      public VectorNodalField<T> getField() {
         return VectorNodalField.this;
      }

      public T eval (FieldPoint def) {
         return getCachedValue (
            def.getElementNumber(), def.getElementSubIndex());
      }
   }

   public VectorFieldFunction<T> createFieldFunction (boolean useRestPos) {
      return new NodalFieldFunction();
   }

   protected void initValues() {
      myValues = new ArrayList<>();
      updateValueLists();
      setRenderProps (createRenderProps());
   }

   protected void updateValueLists() {
      resizeArrayList (myValues, myFem.getNodes().getNumberLimit());
   }
   
   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public VectorNodalField (Class<T> type) {
      super (type);
   }
   
   public VectorNodalField (Class<T> type, FemModel3d fem) {
      super (type, fem);
      initValues();
   }

   public VectorNodalField (Class<T> type, FemModel3d fem, T defaultValue) {
      super (type, fem, defaultValue);
      initValues();
   }

   public VectorNodalField (String name, Class<T> type, FemModel3d fem) {
      this (type, fem);
      setName (name);
   }

   public VectorNodalField (
      String name, Class<T> type, FemModel3d fem, T defaultValue) {
      this (type, fem, defaultValue);
      setName (name);
   }

   public T getValue (int nodeNum) {
      T value = myValues.get (nodeNum);
      if (value == null) {
         return myDefaultValue;
      }
      else {
         return value;
      }
   }

   public T getValue (FemNode3d node) {
      return getValue (node.getNumber());
   }

   public void setValue (FemNode3d node, T value) {
      String sizeErr = checkSize (value);
      if (sizeErr != null) {
         throw new IllegalArgumentException (
            "value for node "+node.getNumber()+": "+sizeErr);
      }
      T storedValue = createInstance();
      storedValue.set (value);
      myValues.set (node.getNumber(), storedValue);
   }

   public T getValue (int[] nodeNums, double[] weights) {
      T vec = createInstance();
      for (int i=0; i<nodeNums.length; i++) {
         vec.scaledAddObj (weights[i], getValue(nodeNums[i]));
      }
      return vec;
   }

   protected T[] initValueArray (int elemIdx) {
      FemElement3dBase elem = getElementAtIndex (elemIdx);

      FemNode3d[] nodes = (FemNode3d[])elem.getNodes();
      int[] nodeNums = new int[nodes.length];
      double[] weights = new double[nodes.length];
      for (int i=0; i<nodes.length; i++) {
         nodeNums[i] = nodes[i].getNumber();
      }
      IntegrationPoint3d[] ipnts = elem.getAllIntegrationPoints();
      T[] varray = (T[])(new Object[ipnts.length]);
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
      myValueArrays = new ArrayList<T[]>(maxelems);
      for (int i=0; i<maxelems; i++) {
         myValueArrays.add (null);
      }
   }

   protected T getCachedValue (int elemIdx, int subIdx) {
      if (myValueArrays == null) {
         initializeCache();
      }
      T[] varray = myValueArrays.get(elemIdx);
      if (varray == null) {
         varray = initValueArray (elemIdx);
         myValueArrays.set (elemIdx, varray);
      }
      return varray[subIdx];
   }

   public T getValue (Point3d pos) {
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
      writeValues (pw, fmt, myValues, new NodeWritableTest(myFem.getNodes()));
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "values")) {
         myValues = new ArrayList<T>();
         scanValues (rtok, myValues);
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
         if (myValues.get(i) != null) {
            refs.add (myFem.getNodes().getByNumber(i));
         }
      }
   }

   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      if (undo) {
         restoreReferencedValues (myValues, undoInfo);
      }
      else {
         removeUnreferencedValues (
            myValues, new NodeReferencedTest (myFem.getNodes()), undoInfo);
      }
   }

   public void clearCacheIfNecessary() {
      myValueArrays = null;
   }
   
   // build render object for rendering Vector3d values

   protected RenderObject buildRenderObject() {
      if (myRenderScale != 0 && hasThreeVectorValue()) {
         RenderObject robj = new RenderObject();
         robj.createLineGroup();
         Point3d pos = new Point3d();
         Vector3d vec = new Vector3d();
         for (int num=0; num<myValues.size(); num++) {
            if (getThreeVectorValue (vec, myValues.get(num))) {
               FemNode3d n = myFem.getNodes().getByNumber(num);
               addLineSegment (robj, n.getPosition(), vec);
            }
         }
         return robj;
      }
      else {
         return null;
      }
   }

}
