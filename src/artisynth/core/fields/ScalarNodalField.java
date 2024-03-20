package artisynth.core.fields;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import artisynth.core.femmodels.FemCutPlane;
import artisynth.core.femmodels.FemElement3dBase;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.femmodels.PointFem3dAttachment;
import artisynth.core.fields.ScalarFieldUtils.ScalarVertexFunction;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.PointParticleAttachment;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.FemFieldPoint;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.util.ScanToken;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.render.RenderObject;
import maspack.util.DoubleInterval;
import maspack.util.DynamicBooleanArray;
import maspack.util.DynamicDoubleArray;
import maspack.util.EnumRange;
import maspack.util.NumberFormat;
import maspack.util.Range;
import maspack.util.ReaderTokenizer;

/**
 * A scalar field defined over an FEM model, using values set at the
 * nodes. Values at other points are obtained by nodal interpolation on the
 * elements nearest to those points. Values at nodes for which no explicit
 * value has been set are given by the field's <i>default value</i>.
 */
public class ScalarNodalField extends ScalarFemField {

   DynamicDoubleArray myValues;  // values at each node
   DynamicBooleanArray myValset; // whether value is set at each node 

   // cached values
   protected ArrayList<double[]> myVolumetricValues;  
   protected ArrayList<double[]> myShellValues;  

   public Range getVisualizationRange() {
      return new EnumRange<Visualization>(
         Visualization.class, new Visualization[] {
            Visualization.POINT, Visualization.SURFACE, Visualization.OFF });
   }

   protected void initValues() {
      myValues = new DynamicDoubleArray();
      myValset = new DynamicBooleanArray();
      updateValueLists();
   }

   protected void updateValueLists() {
      int size = myFem.getNodes().getNumberLimit();
      myValues.resize (size);
      myValset.resize (size);
   }

   void updateValueRange (DoubleInterval range) {
      for (int i=0; i<myValset.size(); i++) {
         if (myValset.get(i)) {
            range.updateBounds (myValues.get(i));
         }
      }
   }

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public ScalarNodalField () {
   }
   
   /**
    * Constructs a field for a given FEM model, with a default value of 0.
    *
    * @param fem FEM model over which the field is defined
    */
   public ScalarNodalField (FemModel3d fem) {
      super (fem);
      initValues();
   }

   /**
    * Constructs a field for a given FEM model and default value.
    * 
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for nodes which don't have
    * explicitly set values
    */
   public ScalarNodalField (FemModel3d fem, double defaultValue) {
      super (fem, defaultValue);
      initValues();
   }

   /**
    * Constructs a named field for a given FEM model, with a default value of
    * 0.
    * 
    * @param name name of the field
    * @param fem FEM model over which the field is defined
    */
   public ScalarNodalField (String name, FemModel3d fem) {
      this (fem);
      setName (name);
   }

   /**
    * Constructs a named field for a given FEM model and default value.
    *
    * @param name name of the field
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for nodes which don't have
    * explicitly set values
    */
   public ScalarNodalField (String name, FemModel3d fem, double defaultValue) {
      this (fem, defaultValue);
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
   public double getValue (int nodeNum) {
      checkNodeNum (nodeNum);
      if (myValset.get(nodeNum)) {
         return myValues.get (nodeNum);
      }
      else {
         return myDefaultValue;
      }
   }

   /**
    * Returns the value at a node. The default value is returned if a value
    * has not been explicitly set for that node.
    *
    * @param node node for which the value is requested
    * @return value at the node
    */
   public double getValue (FemNode3d node) {
      checkNodeBelongsToFem (node);
      return getValue (node.getNumber());
   }

   /**
    * {@inheritDoc}
    */
   public double getValue (FemFieldPoint fp) {
      return getCachedValue (
         fp.getElementType(), fp.getElementNumber(), fp.getElementSubIndex());
   }

   /**
    * {@inheritDoc}
    */  
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
      Vector3d posx = new Vector3d();
      for (int i=0; i<nodeNums.length; i++) {
         nodeNums[i] = nodes[i].getNumber();
         posx.scaledAdd (weights.get(i), nodes[i].getPosition());
      }
      return getValue (nodeNums, weights.getBuffer());
   }
   
   /**
    * Sets the value at a node.
    * 
    * @param node node for which the value is to be set
    * @param value new value for the node
    */
   public void setValue (FemNode3d node, double value) {
      checkNodeBelongsToFem (node);
      int nodeNum = node.getNumber();
      checkNodeNum (nodeNum);      
      myValues.set (nodeNum, value);
      myValset.set (nodeNum, true);
      notifyValuesChanged();
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
      return myValset.get (nodeNum);
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
      myValset.set (nodeNum, false);
      notifyValuesChanged();
   }
   
   /**
    * {@inheritDoc}
    */
   public void clearAllValues() {
      for (int i=0; i<myValset.size(); i++) {
         myValset.set (i, false);
      }
      notifyValuesChanged();
   }   

   public double getValue (int[] nodeNums, double[] weights) {
      double value = 0;
      for (int i=0; i<nodeNums.length; i++) {
         value += weights[i]*getValue (nodeNums[i]);
      }
      return value;
   }

   protected double[] initValueArray (int elemType, int elemIdx) {
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

   ArrayList<double[]> initCacheArray (int maxvals) {
      ArrayList<double[]> array = new ArrayList<>(maxvals);
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

   protected double getCachedValue (int elemType, int elemIdx, int subIdx) {
      if (myVolumetricValues == null) {
         initializeCache();
      }
      double[] varray;
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
      writeValues (
         pw, fmt, myValues, myValset, 
         new NodeWritableTest(myFem.getNodes()));
   }

   /**
    * {@inheritDoc}
    */  
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "values")) {
         myValues = new DynamicDoubleArray();
         myValset = new DynamicBooleanArray();
         scanValues (rtok, myValues, myValset);
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
      super.getSoftReferences (refs);
      for (int i=0; i<myValues.size(); i++) {
         if (myValset.get(i)) {
            refs.add (myFem.getNodes().getByNumber(i));
         }
      }
   }

   /**
    * {@inheritDoc}
    */  
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      super.updateReferences (undo, undoInfo);
      if (undo) {
         restoreReferencedValues (myValues, myValset, undoInfo);
      }
      else {
         removeUnreferencedValues (
            myValues, myValset, 
            new NodeReferencedTest (myFem.getNodes()), undoInfo);
      }
   }

   /**
    * {@inheritDoc}
    */  
   public void clearCacheIfNecessary() {
      myVolumetricValues = null;
      myShellValues = null;
   }

   /**
    * Returns {@code true} if this field is functionally equal to another field.
    * Intended mainly for testing and debugging.
    */
   public boolean equals (ScalarNodalField field) {
      if (!super.equals (field)) {
         return false;
      }
      if (!valueSetArraysEqual (
             myValues, myValset, field.myValues, field.myValset)) {
         return false;
      }
      return true;
   }

   // --- rendering interface ----

   protected RenderObject buildPointRenderObject(DoubleInterval range) {
      RenderObject rob = new RenderObject();

      rob.createPointGroup();
      ScalarFieldUtils.addColors (rob, myColorMap);
      int vidx = 0;
      for (FemNode3d n : myFem.getNodes()) {
         rob.addPosition (n.getPosition());
         int cidx = getColorIndex (getValue(n), range);
         rob.addVertex (vidx, -1, cidx, -1);
         rob.addPoint (vidx);
         vidx++;
      }     
      return rob;
   }

   private double getVertexValue (PointAttachment pa) {
      if (pa instanceof PointParticleAttachment) {
         // XXX is this checking needed? Can we assume getParticle() is a
         // FemNode3d and that the node belongs to the right FEM?
         Particle p = ((PointParticleAttachment)pa).getParticle();
         if (p instanceof FemNode3d) {
            FemNode3d node = (FemNode3d)p;
            if (node.getGrandParent() == myFem) {
               return getValue(node);
            }
         }
         return myDefaultValue;
      }
      else if (pa instanceof PointFem3dAttachment) {
         PointFem3dAttachment pfa = (PointFem3dAttachment)pa;
         double[] wgts = pfa.getCoordinates().getBuffer();
         return getVertexValue (pfa.getNodes(), wgts);
      }
      else {
         return myDefaultValue;
      }
   }
   
   private double getVertexValue (FemNode[] nodes, double[] wgts) {
      double value = 0;
      int k = 0;
      for (FemNode n : nodes) {
         // XXX is this checking needed? Can we assume the node is a
         // FemNode3d that it belongs to the right FEM?
         if (n instanceof FemNode3d && n.getGrandParent() == myFem) {
            value += wgts[k++]*getValue ((FemNode3d)n);
         }
         else {
            value += wgts[k++]*myDefaultValue;
         }
      }
      return value;     
   }

   protected ScalarVertexFunction getVertexFunction () {
      return (
         (mcomp, vtx) -> {
            if (mcomp instanceof FemCutPlane) {
               return getValue (vtx.getWorldPoint());
            }
            else if (mcomp instanceof FemMeshComp) {
               return getVertexValue (
                  ((FemMeshComp)mcomp).getVertexAttachment(vtx));
            }
            else {
               return 0;
            }
         });         
   }

 
}
