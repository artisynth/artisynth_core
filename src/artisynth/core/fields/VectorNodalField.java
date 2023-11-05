package artisynth.core.fields;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import artisynth.core.femmodels.FemElement3dBase;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.FemFieldPoint;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.util.ScanToken;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorObject;
import maspack.render.RenderObject;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * A vector field defined over an FEM model, using values set at the
 * nodes. Values at other points are obtained by nodal interpolation on the
 * elements nearest to those points. Values at nodes for which no explicit
 * value has been set are given by the field's <i>default value</i>. Vectors
 * are of type {@code T}, which must be an instance of {@link VectorObject}.
 */
public class VectorNodalField<T extends VectorObject<T>> 
   extends VectorFemField<T> {

   ArrayList<T> myValues;
   protected ArrayList<T[]> myVolumetricValues;   
   protected ArrayList<T[]> myShellValues;   

   protected void initValues() {
      myValues = new ArrayList<>();
      updateValueLists();
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
   
   /**
    * Constructs a field for a given FEM model, with a default value of 0.
    *
    * @param type class type of the {@link VectorObject} describing the field's
    * @param fem FEM model over which the field is defined
    */
   public VectorNodalField (Class<T> type, FemModel3d fem) {
      super (type, fem);
      initValues();
   }

   /**
    * Constructs a field for a given FEM model and default value.
    * 
    * @param type class type of the {@link VectorObject} describing the field's
    * values
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for nodes which don't have
    * explicitly set values
    */
   public VectorNodalField (Class<T> type, FemModel3d fem, T defaultValue) {
      super (type, fem, defaultValue);
      initValues();
   }

   /**
    * Constructs a named field for a given FEM model, with a default value of 0.
    * 
    * @param name name of the field
    * @param type class type of the {@link VectorObject} describing the field's
    * values
    * @param fem FEM model over which the field is defined
    */
   public VectorNodalField (String name, Class<T> type, FemModel3d fem) {
      this (type, fem);
      setName (name);
   }

   /**
    * Constructs a named field for a given FEM model and default value.
    *
    * @param name name of the field
    * @param type class type of the {@link VectorObject} describing the field's
    * values
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for nodes which don't have
    * explicitly set values
    */
   public VectorNodalField (
      String name, Class<T> type, FemModel3d fem, T defaultValue) {
      this (type, fem, defaultValue);
      setName (name);
   }

   private void checkNodeNum (int nodeNum) {
      int maxNum = myFem.getNodes().getNumberLimit();
      if (nodeNum >= maxNum) {
         throw new IllegalArgumentException (
            "nodeNum="+nodeNum+", max node num is "+ maxNum);
      }
      else if (myValues.size() < maxNum) {
         updateValueLists();
      }
   }

   /**
    * Returns the value at the node specified by the given number. The default
    * value is returned if a value has not been explicitly set for that node.
    * Node numbers are used instead of indices as they are more persistent if
    * the FEM model is modified.
    * 
    * @param nodeNum node number
    * @return value at the node
    */
   public T getValue (int nodeNum) {
      checkNodeNum (nodeNum);
      T value = myValues.get (nodeNum);
      if (value == null) {
         return myDefaultValue;
      }
      else {
         return value;
      }
   }

   /**
    * Returns the value at a node. The default value is returned if a value
    * has not been explicitly set for that node.
    *
    * @param node node for which the value is requested
    * @return value at the node
    */
   public T getValue (FemNode3d node) {
      checkNodeBelongsToFem (node);
      return getValue (node.getNumber());
   }

   /**
    * {@inheritDoc}
    */
   public T getValue (FemFieldPoint fp) {
      return getCachedValue (
         fp.getElementType(), fp.getElementNumber(), fp.getElementSubIndex());
   }
   
   /**
    * {@inheritDoc}
    */
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

   /**
    * Sets the value at a node.
    * 
    * @param node node for which the value is to be set
    * @param value new value for the node
    */
   public void setValue (FemNode3d node, T value) {
      checkNodeBelongsToFem (node);
      String sizeErr = checkSize (value);
      if (sizeErr != null) {
         throw new IllegalArgumentException (
            "value for node "+node.getNumber()+": "+sizeErr);
      }
      int nodeNum = node.getNumber();
      checkNodeNum (nodeNum);
      T storedValue = createTypeInstance();
      storedValue.set (value);
      myValues.set (nodeNum, storedValue);
   }

   /**
    * Queries whether a value has been seen at a given node.
    * 
    * @param node node being queried
    * @return {@code true} if a value has been set at the node
    */
   public boolean isValueSet (FemNode3d node) {
      checkNodeBelongsToFem (node);
      int nodeNum = node.getNumber();
      checkNodeNum (nodeNum);      
      return myValues.get (nodeNum) != null;
   }

   /**
    * Clears the value at a given node. After this call, the node will be
    * associated with the default value.
    * 
    * @param node node whose value is to be cleared
    */
   public void clearValue (FemNode3d node) {
      checkNodeBelongsToFem (node);
      int nodeNum = node.getNumber();
      checkNodeNum (nodeNum);
      myValues.set (nodeNum, null);
   }
   
   /**
    * {@inheritDoc}
    */
   public void clearAllValues() {
      for (int i=0; i<myValues.size(); i++) {
         myValues.set (i, null);
      }
   }

   public T getValue (int[] nodeNums, double[] weights) {
      T vec = createTypeInstance();
      for (int i=0; i<nodeNums.length; i++) {
         vec.scaledAddObj (weights[i], getValue(nodeNums[i]));
      }
      return vec;
   }

   protected T[] initValueArray (int elemType, int elemIdx) {
      FemElement3dBase elem;
      if (elemType == 0) {
         elem = myFem.getElementByNumber (elemIdx);
      }
      else {
         elem = myFem.getShellElementByNumber (elemIdx);
      }
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

   ArrayList<T[]> initCacheArray (int maxvals) {
      ArrayList<T[]> array = new ArrayList<>(maxvals);
      for (int i=0; i<maxvals; i++) {
         array.add (null);
      }
      return array;
   }
   
   void initializeCache() {
      myVolumetricValues = 
         initCacheArray (myFem.getElements().getNumberLimit());
      myShellValues = 
         initCacheArray (myFem.getShellElements().getNumberLimit());
   }

   protected T getCachedValue (int elemType, int elemIdx, int subIdx) {
      if (myVolumetricValues == null) {
         initializeCache();
      }
      T[] varray;
      if (elemType == 0) {
         varray = myVolumetricValues.get(elemIdx);
         if (varray == null) {
            varray = initValueArray (elemType, elemIdx);
            myVolumetricValues.set (elemIdx, varray);
         }
      }
      else {
         varray = myShellValues.get(elemIdx);
         if (varray == null) {
            varray = initValueArray (elemType, elemIdx);
            myShellValues.set (elemIdx, varray);
         }
      }      
      if (subIdx == -1) {
         subIdx = 0;
      }
      return varray[subIdx];
   }

   /* ---- Begin I/O methods ---- */

   /**
    * {@inheritDoc}
    */
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.println ("values=");
      writeValues (pw, fmt, myValues, new NodeWritableTest(myFem.getNodes()));
   }

   /**
    * {@inheritDoc}
    */
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

   /**
    * {@inheritDoc}
    */
   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      updateValueLists();
   }

   /* ---- Begin edit methods ---- */

   /**
    * {@inheritDoc}
    */
   public void getSoftReferences (List<ModelComponent> refs) {
      for (int i=0; i<myValues.size(); i++) {
         if (myValues.get(i) != null) {
            refs.add (myFem.getNodes().getByNumber(i));
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      if (undo) {
         restoreReferencedValues (myValues, undoInfo);
      }
      else {
         removeUnreferencedValues (
            myValues, new NodeReferencedTest (myFem.getNodes()), undoInfo);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void clearCacheIfNecessary() {
      myVolumetricValues = null;
      myShellValues = null;
   }
   
   // build render object for rendering Vector3d values

   protected RenderObject buildRenderObject() {
      if (myRenderScale != 0 && hasThreeVectorValue()) {
         RenderObject robj = new RenderObject();
         robj.createLineGroup();
         Point3d pos = new Point3d();
         Vector3d vec = new Vector3d();
         for (int num=0; num<myValues.size(); num++) {
            if (myValues.get(num).getThreeVectorValue(vec)) {
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

   /**
    * Returns {@code true} if this field is functionally equal to another field.
    * Intended mainly for testing and debugging.
    */
   public boolean equals (VectorNodalField<T> field) {
     return (
        super.equals (field) && 
        vectorListEquals (myValues, field.myValues));
   }

}
