package artisynth.core.fields;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.*;

import artisynth.core.fields.ScalarFieldUtils.ScalarVertexFunction;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemElement3dBase;
import artisynth.core.femmodels.FemElement3dList;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.FemElement.ElementClass;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.FemFieldPoint;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.mechmodels.*;
import artisynth.core.util.ScanToken;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.MatrixNd;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.ArraySupport;
import maspack.util.DynamicDoubleArray;
import maspack.util.DynamicBooleanArray;
import maspack.util.IndentingPrintWriter;
import maspack.util.*;
import maspack.render.*;
import maspack.geometry.*;
import maspack.properties.*;
import maspack.render.Renderer.*;

/**
 * A scalar field defined over an FEM model, using values set at the element
 * integration points. Values at other points are obtained by interpolation
 * within the elements nearest to those points. Values at elements for which no
 * explicit values have been set are given by the field's <i>default
 * value</i>.
 *
 * <p> For a given element {@code elem}, values should be specified for
 * <i>all</i> integration points, as returned by {@link
 * FemElement3dBase#getAllIntegrationPoints}. This includes the regular
 * integration points, as well as the <i>warping</i> point, which is located at
 * the element center and is used by corotated linear materials. Integration
 * point indices should be in the range {@code 0} to {@link
 * FemElement3dBase#numAllIntegrationPoints} - 1.
 */
public class ScalarSubElemField extends ScalarFemField {
   
   protected ArrayList<double[]> myValues;      // values at each volume element
   protected ArrayList<boolean[]> myValset;     // is volume elem value set?
   protected ArrayList<double[]> myShellValues; // values at each shell element
   protected ArrayList<boolean[]> myShellValset;// is shell elem value set?
   
   public static boolean DEFAULT_VOLUME_ELEMS_VISIBLE = true;
   protected boolean myVolumeElemsVisible = DEFAULT_VOLUME_ELEMS_VISIBLE;

   public static boolean DEFAULT_SHELL_ELEMS_VISIBLE = true;
   protected boolean myShellElemsVisible = DEFAULT_SHELL_ELEMS_VISIBLE;

   public static PropertyList myProps =
      new PropertyList (ScalarSubElemField.class, ScalarFemField.class);

   static {
      myProps.add (
         "volumeElemsVisible", "field is visible for volume elements",
         DEFAULT_VOLUME_ELEMS_VISIBLE);
      myProps.add (
         "shellElemsVisible", "field is visible for shell elements",
         DEFAULT_SHELL_ELEMS_VISIBLE);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }   

   public Range getVisualizationRange() {
      return new EnumRange<Visualization>(
         Visualization.class, new Visualization[] {
            Visualization.POINT, Visualization.SURFACE, Visualization.OFF });
   }

   public boolean getVolumeElemsVisible () {
      return myVolumeElemsVisible;
   }

   public void setVolumeElemsVisible (boolean visible) {
      myVolumeElemsVisible = visible;
   }

   public boolean getShellElemsVisible () {
      return myShellElemsVisible;
   }

   public void setShellElemsVisible (boolean visible) {
      myShellElemsVisible = visible;
   }

   protected void initValues () {
      myValues = new ArrayList<double[]>();
      myValset = new ArrayList<boolean[]>();
      myShellValues = new ArrayList<double[]>();
      myShellValset = new ArrayList<boolean[]>();
      updateValueLists();
   }

   protected void updateValueLists() {
      resizeArrayList (
         myValues, myFem.getElements().getNumberLimit());
      resizeArrayList (
         myValset, myFem.getElements().getNumberLimit());
      resizeArrayList (
         myShellValues, myFem.getShellElements().getNumberLimit());
      resizeArrayList (
         myShellValset, myFem.getShellElements().getNumberLimit());
   }

   void updateValueRange (DoubleInterval range) {
      for (int i=0; i<myValset.size(); i++) {
         boolean[] valueset = myValset.get(i);
         if (valueset != null) {
            double[] values = myValues.get(i);
            for (int j=0; j<valueset.length; j++) {
               if (valueset[j]) {
                  range.updateBounds (values[j]);
               }
            }
         }
      }
      for (int i=0; i<myShellValset.size(); i++) {
         boolean[] valueset = myShellValset.get(i);
         if (valueset != null) {
            double[] values = myShellValues.get(i);
            for (int j=0; j<valueset.length; j++) {
               if (valueset[j]) {
                  range.updateBounds (values[j]);
               }
            }
         }
      }
   }

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public ScalarSubElemField () {
   }

   /**
    * Constructs a field for a given FEM model, with a default value of 0.
    *
    * @param fem FEM model over which the field is defined
    */
   public ScalarSubElemField (FemModel3d fem)  {
      super (fem);
      initValues ();
   }

   /**
    * Constructs a field for a given FEM model and default value.
    * 
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for integration points which don't have
    * explicitly set values
    */
   public ScalarSubElemField (
      FemModel3d fem, double defaultValue) {
      super (fem, defaultValue);
      initValues ();
   }

   /**
    * Constructs a named field for a given FEM model, with a default value of
    * 0.
    * 
    * @param name name of the field
    * @param fem FEM model over which the field is defined
    */
   public ScalarSubElemField (String name, FemModel3d fem) {
      this (fem);
      setName (name);
   }

   /**
    * Constructs a named field for a given FEM model and default value.
    *
    * @param name name of the field
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for integration points which don't have
    * explicitly set values
    */
   public ScalarSubElemField (
      String name, FemModel3d fem, double defaultValue) {
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

   private void checkElemNum (int elemNum) {
      int maxNum = myFem.getElements().getNumberLimit();
      if (elemNum >= maxNum) {
         throw new IllegalArgumentException (
            "elemNum="+elemNum+", max elem num is "+maxNum);
      }
      else if (myValues.size() < maxNum) {
         updateValueLists();
      }
   }

   private void checkShellElemNum (int elemNum) {
      int maxNum = myFem.getShellElements().getNumberLimit();
      if (elemNum >= maxNum) {
         throw new IllegalArgumentException (
            "elemNum="+elemNum+", max elem num is "+maxNum);
      }
      else if (myShellValues.size() < maxNum) {
         updateValueLists();
      }
   }

   private void checkSubIndex (boolean[] valueset, int subIdx) {
      if (subIdx >= valueset.length) {
         throw new IllegalArgumentException (
            "subIdx=" + subIdx +
            ", maximum value for element is " + (valueset.length-1));
      }
   }

   /**
    * Returns the value at an integration point of a volumetric
    * element. The default value is returned if a value has not been explicitly
    * set for that point.  The element is specified by its number; element
    * numbers are used instead of indices as they are more persistent if the
    * FEM model is modified.
    * 
    * @param elemNum volumetric element number
    * @param subIdx integration point index, in the range {@code 0} to {@code
    * n-1}, where {@code n} is the value returned by the element's {@link
    * FemElement3dBase#numAllIntegrationPoints} method
    * @return value at the integration point
    */
   public double getElementValue (int elemNum, int subIdx) {
      checkElemNum (elemNum);
      boolean [] valueset = myValset.get(elemNum);
      if (valueset == null) {
         return myDefaultValue;
      }
      else {
         if (subIdx == -1) {
            subIdx = 0;
         }
         checkSubIndex (valueset, subIdx);
         if (valueset[subIdx]) {
            return myValues.get(elemNum)[subIdx];
         }
         else {
            return myDefaultValue;
         }
      }
   }

   /**
    * Returns the value at an integration point of a shell element. The default
    * value is returned if a value has not been explicitly set for that point.
    * The element is specified by its number; element numbers are used instead
    * of indices as they are more persistent if the FEM model is modified.
    * 
    * @param elemNum volumetric element number
    * @param subIdx integration point index, in the range {@code 0} to {@code
    * n-1}, where {@code n} is the value returned by the element's {@link
    * FemElement3dBase#numAllIntegrationPoints} method
    * @return value at the integration point
    */
   public double getShellElementValue (int elemNum, int subIdx) {
      checkShellElemNum (elemNum);
      boolean [] valueset = myShellValset.get(elemNum);
      if (valueset == null) {
         return myDefaultValue;
      }
      else {
         if (subIdx == -1) {
            subIdx = 0;
         }
         checkSubIndex (valueset, subIdx);
         if (valueset[subIdx]) {
            return myShellValues.get(elemNum)[subIdx];
         }
         else {
            return myDefaultValue;
         }
      }
   }

   /**
    * Returns the value at an integration point of an element (either
    * volumetric or shell). The default value is returned if a value has not
    * been explicitly set for that point.
    *
    * @param elem element containing the integration point
    * @param subIdx integration point index, in the range {@code 0} to {@code
    * n-1}, where {@code n} is the value returned by the element's {@link
    * FemElement3dBase#numAllIntegrationPoints} method
    * @return value at the integration point
    */
   public double getValue (FemElement3dBase elem, int subIdx) {
      checkElementBelongsToFem (elem);
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         return getElementValue (elem.getNumber(), subIdx);
      }
      else {
         return getShellElementValue (elem.getNumber(), subIdx);
      }
   }

   /**
    * {@inheritDoc}
    */
   public double getValue (FemFieldPoint fp) {
      if (fp.getElementType() == 0) {
         return getElementValue (
            fp.getElementNumber(), fp.getElementSubIndex());
      }
      else {
         return getShellElementValue (
            fp.getElementNumber(), fp.getElementSubIndex());
      }
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
      double[] values;
      boolean[] valueset;
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         values = myValues.get (elem.getNumber());
         valueset = myValset.get (elem.getNumber());
      }
      else {
         values = myShellValues.get (elem.getNumber());
         valueset = myShellValset.get (elem.getNumber());
      }
      if (valueset == null) {
         return myDefaultValue;
      }
      VectorNd weights = new VectorNd(elem.numNodes());
      elem.getMarkerCoordinates (weights, null, loc, /*checkInside=*/false);
      // nodal extrapolation matrix maps integration point values to nodes
      MatrixNd E = elem.getNodalExtrapolationMatrix();
      int npnts = E.colSize();
      double[] Ebuf = E.getBuffer();
      double value = 0;
      for (int i=0; i<elem.numNodes(); i++) {
         for (int j=0; j<npnts; j++) {
            double a = Ebuf[i*npnts+j];
            if (a != 0) {
               double val = (valueset[j] ? values[j] : myDefaultValue);
               value += weights.get(i)*a*val;
            }
         }
      }
      return value;
   }

   /**
    * Sets the value at an integration point of an element (either volumetric
    * or shell).
    * 
    * @param elem element containing the integration point
    * @param subIdx integration point index, in the range {@code 0} to {@code
    * n-1}, where {@code n} is the value returned by the element's {@link
    * FemElement3dBase#numAllIntegrationPoints} method
    * @param value new value for the integration point
    */
   public void setValue (FemElement3dBase elem, int subIdx, double value) {
      checkElementBelongsToFem (elem);
      int elemNum = elem.getNumber();
      double[] varray;
      boolean[] valueset;
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         checkElemNum (elemNum);
         varray = myValues.get(elemNum);
         valueset = myValset.get(elemNum);
         if (varray == null) {
            int numi = elem.numAllIntegrationPoints();
            varray = new double[numi];
            valueset = new boolean[numi];
            myValues.set (elemNum, varray);
            myValset.set (elemNum, valueset);
         }
      }
      else {
         checkShellElemNum (elemNum);
         varray = myShellValues.get(elemNum);
         valueset = myShellValset.get(elemNum);
         if (varray == null) {
            int numi = elem.numAllIntegrationPoints();
            varray = new double[numi];
            valueset = new boolean[numi];
            myShellValues.set (elemNum, varray);
            myShellValset.set (elemNum, valueset);
         }
      }
      checkSubIndex (valueset, subIdx);
      varray[subIdx] = value;
      valueset[subIdx] = true;
      notifyValuesChanged();
   }

   /**
    * Queries whether a value has been seen at an integration point of an
    * element (either volumetric or shell).
    * 
    * @param elem element containing the integration point
    * @param subIdx integration point index, in the range {@code 0} to {@code
    * n-1}, where {@code n} is the value returned by the element's {@link
    * FemElement3dBase#numAllIntegrationPoints} method
    * @return {@code true} if a value has been set at the integration point
    */
   public boolean isValueSet (FemElement3dBase elem, int subIdx) {
      checkElementBelongsToFem (elem);
      int elemNum = elem.getNumber();
      boolean[] valueset;
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         checkElemNum (elemNum);
         valueset = myValset.get(elemNum);
      }
      else {
         checkShellElemNum (elemNum);
         valueset = myShellValset.get(elemNum);
      }
      if (valueset == null) {
         return false;
      }
      return valueset[subIdx];
   }
   
   /**
    * Clears the value at an integration of an element (either volumetric or
    * shell). After this call, the point will be associated with the default
    * value.
    * 
    * @param elem element containing the integration point
    * @param subIdx integration point index, in the range {@code 0} to {@code
    * n-1}, where {@code n} is the value returned by the element's {@link
    * FemElement3dBase#numAllIntegrationPoints} method
    */
   public void clearValue (FemElement3dBase elem, int subIdx) {
      checkElementBelongsToFem (elem);
      int elemNum = elem.getNumber();
      ArrayList<double[]> valueArrays;
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         checkElemNum (elemNum);
         boolean[] valueset = myValset.get(elemNum);
         if (valueset != null) {
            checkSubIndex (valueset, subIdx);            
            valueset[subIdx] = false;
            if (allUnset (valueset)) {
               myValues.set (elemNum, null);
               myValset.set (elemNum, null);
            }
         }
      }
      else {
         checkShellElemNum (elemNum);
         boolean[] valueset = myShellValset.get(elemNum);
         if (valueset != null) {
            checkSubIndex (valueset, subIdx);            
            valueset[subIdx] = false;
            if (allUnset (valueset)) {
               myShellValues.set (elemNum, null);
               myShellValset.set (elemNum, null);
            }
         }
      }
      notifyValuesChanged();
   }
   
   /**
    * {@inheritDoc}
    */
   public void clearAllValues() {
      for (int i=0; i<myValues.size(); i++) {
         myValues.set (i, null);
         myValset.set (i, null);
      }
      for (int i=0; i<myShellValues.size(); i++) {
         myShellValues.set (i, null);
         myShellValset.set (i, null);
      }
      notifyValuesChanged();
   }   

   /* ---- Begin I/O methods ---- */

   private void writeScalarValueArrays (
      PrintWriter pw, NumberFormat fmt,
      ArrayList<double[]> valueArrays, ArrayList<boolean[]> valueSetArrays,
      WritableTest writableTest)
      throws IOException {

      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (int num=0; num<valueArrays.size(); num++) {
         boolean[] valueset = valueSetArrays.get(num);
         if (valueset == null || !writableTest.isWritable(num)) {
            pw.println ("null");
         }
         else {
            double[] varray = valueArrays.get(num);
            pw.print ("[ ");
            for (int k=0; k<varray.length; k++) {
               if (valueset[k]) {
                  pw.print (fmt.format(varray[k])+" ");
               }
               else {
                  pw.print ("null ");
               }
            }
            pw.println ("]");
         }
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }
 
   private void scanScalarValueArrays (
      ReaderTokenizer rtok,
      ArrayList<double[]> valueArrays, ArrayList<boolean[]> valueSetArrays)
      throws IOException {

      DynamicDoubleArray scannedValues = new DynamicDoubleArray();
      DynamicBooleanArray scannedValueset = new DynamicBooleanArray();
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (rtok.tokenIsWord() && rtok.sval.equals ("null")) {
            valueArrays.add (null);
            valueSetArrays.add (null);
         }
         else {
            if (rtok.ttype != '[') {
               throw new IOException ("Expecting token '[', got "+rtok);
            }
            scannedValues.clear();
            scannedValueset.clear();
            do {
               rtok.nextToken();
               if (rtok.tokenIsWord ("null")) {
                  scannedValues.add (0);
                  scannedValueset.add (false);
               }
               else if (rtok.tokenIsNumber()) {
                  scannedValues.add (rtok.nval);
                  scannedValueset.add (true);
               }
               else if (rtok.ttype != ']') {
                  throw new IOException (
                     "Expecting number or 'null', got "+rtok);
               }
            }
            while (rtok.ttype != ']');
            boolean[] valueset = scannedValueset.toArray();
            if (!allUnset(valueset)) {
               valueArrays.add (scannedValues.toArray());
               valueSetArrays.add (valueset);
            }
            else {
               valueArrays.add (null);
               valueSetArrays.add (null);
            }
         }
      }
   }

   private void checkScalarValueArraysSizes (
      ArrayList<double[]> valueArrays, ElementClass eclass) throws IOException {

      FemElement3dList<?> elems;
      if (eclass == ElementClass.VOLUMETRIC) {
         elems = myFem.getElements();
      }
      else {
         elems = myFem.getShellElements();
      }
      for (int i=0; i<valueArrays.size(); i++) {
         double[] varray = valueArrays.get(i);
         if (varray != null) {
            FemElement3dBase elem = elems.getByNumber(i);
            if (elem == null) {
               throw new IOException (
                  "Values defined for nonexistent "+elemName(eclass,i));
            }
            int npnts = elem.numAllIntegrationPoints();
            if (varray.length != npnts) {
               throw new IOException (
                  "Number of values ("+varray.length+") for "+elemName(eclass,i)+
                  " does not equal number of integration points ("+npnts+")");
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.print ("values=");
      writeScalarValueArrays (
         pw, fmt, myValues, myValset,
         new ElementWritableTest (myFem.getElements()));
      pw.print ("shellValues=");
      writeScalarValueArrays (
         pw, fmt, myShellValues, myShellValset,
         new ElementWritableTest (myFem.getShellElements()));
   }

   /**
    * {@inheritDoc}
    */
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "values")) {
         myValues = new ArrayList<double[]>();
         myValset = new ArrayList<boolean[]>();
         scanScalarValueArrays (rtok, myValues, myValset);
         return true;
      }
      else if (scanAttributeName (rtok, "shellValues")) {
         myShellValues = new ArrayList<double[]>();
         myShellValset = new ArrayList<boolean[]>();
         scanScalarValueArrays (rtok, myShellValues, myShellValset);
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
      // sanity check on number of values in each array
      checkScalarValueArraysSizes (myValues, ElementClass.VOLUMETRIC);
      checkScalarValueArraysSizes (myShellValues, ElementClass.SHELL);
   }

   /* ---- Begin edit methods ---- */   

   /**
    * {@inheritDoc}
    */
   public void getSoftReferences (List<ModelComponent> refs) {
      super.getSoftReferences (refs);
      for (int i=0; i<myValues.size(); i++) {
         if (myValues.get(i) != null) {
            refs.add (myFem.getElements().getByNumber(i));
         }
      }
      for (int i=0; i<myShellValues.size(); i++) {
         if (myShellValues.get(i) != null) {
            refs.add (myFem.getShellElements().getByNumber(i));
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      super.updateReferences (undo, undoInfo);
      if (undo) {
         restoreReferencedValues (myValues, undoInfo);
         restoreReferencedValues (myShellValues, undoInfo);
      }
      else {
         removeUnreferencedValues (
            myValues, 
            new ElementReferencedTest (myFem.getElements()), undoInfo);
         removeUnreferencedValues (
            myShellValues, 
            new ElementReferencedTest (myFem.getShellElements()), undoInfo);
      }
   }

   /* ---- equality methods --- */
   
   private boolean valueSetArraysEqual (
      double[] values0, boolean[] valset0, 
      double[] values1, boolean[] valset1) {
      
      if ((values0 != null) != (valset0 != null)) {
         throw new IllegalArgumentException (
            "values0 and valset0 have different non-null status");
      }
      if ((values1 != null) != (valset1 != null)) {
         throw new IllegalArgumentException (
            "values1 and valset1 have different non-null status");
      }
      if (values0 != null && values0.length != valset0.length) {
         throw new IllegalArgumentException (
            "values0 and valset0 have different lengths");
      }
      if (values1 != null && values1.length != valset1.length) {
         throw new IllegalArgumentException (
            "values1 and valset1 have different lengths");
      }
      if ((values0 != null) != (values1 != null)) {
         return false;
      }
      if (values0 == null) {
         return true;
      }
      for (int i=0; i<values0.length; i++) {
         if (valset0[i] != valset1[i]) {
            return false;
         }
         else if (valset0[i] && values0[i] != values0[i]) {
            return false;
         }
      }
      return true;
   }
   
   private boolean valueSetArraysEqual (
      ArrayList<double[]> values0, ArrayList<boolean[]> valset0, 
      ArrayList<double[]> values1, ArrayList<boolean[]> valset1) {

      if (values0.size() != valset0.size()) {
         throw new IllegalArgumentException (
            "values0 and valset0 have different sizes");         
      }
      if (values1.size() != valset1.size()) {
         throw new IllegalArgumentException (
            "values1 and valset1 have different sizes");         
      }
      if (values0.size() != values1.size()) {
         return false;
      }
      for (int i=0; i<values0.size(); i++) {
         if (!valueSetArraysEqual (
            values0.get(i), valset0.get(i), values1.get(i), valset1.get(i))) {
            return false;
         }
      }
      return true;      
   }

   /**
    * Returns {@code true} if this field is functionally equal to another field.
    * Intended mainly for testing and debugging.
    */
   public boolean equals (ScalarSubElemField field) {
     if (!super.equals (field)) {
         return false;
      }
      if (!valueSetArraysEqual (
             myValues, myValset, 
             field.myValues, field.myValset)) {
         return false;
      }
      if (!valueSetArraysEqual (
             myShellValues, myShellValset,
             field.myShellValues, field.myShellValset)) {
         return false;
      }
      return true;
    }

   // --- rendering interface ----

   /**
    * Add points every regular integration point in the elements of elist.
    */
   private int addElemPoints (
      RenderObject rob, FemElement3dList<? extends FemElement3dBase> elist,
      DoubleInterval range, int vidx) {

      Point3d pos = new Point3d();
      for (FemElement3dBase elem : elist) {
         IntegrationPoint3d [] ipnts = elem.getAllIntegrationPoints() ;
         for (int k=0; k<ipnts.length; k++) {
            ipnts[k].computePosition (pos, elem);
            rob.addPosition (pos);
            int cidx = getColorIndex (getValue(elem, k), range);
            rob.addVertex (vidx, -1, cidx, -1);
            rob.addPoint (vidx);
            vidx++;
         }   
      }
      return vidx;
   }

   protected RenderObject buildPointRenderObject(DoubleInterval range) {
      RenderObject rob = new RenderObject();
      rob.createPointGroup();
      ScalarFieldUtils.addColors (rob, myColorMap);
      int vidx = 0;
      if (getVolumeElemsVisible()) {
         vidx = addElemPoints (rob, myFem.getElements(), range, vidx);
      }
      if (getShellElemsVisible()) {
         vidx = addElemPoints (rob, myFem.getShellElements(), range, vidx);
      }
      return rob;
   }

   private double getVertexValue (PointAttachment pa, double[] valueAtNodes) {
      PointList<FemNode3d> nodes = myFem.getNodes();
      if (pa instanceof PointParticleAttachment) {
         // XXX is this checking needed? Can we assume getParticle() is a
         // FemNode3d and that the node belongs to the right FEM?
         Particle p = ((PointParticleAttachment)pa).getParticle();
         if (p instanceof FemNode3d) {
            FemNode3d node = (FemNode3d)p;
            if (node.getGrandParent() == myFem) {
               return valueAtNodes[nodes.indexOf(node)];
            }
         }
         return myDefaultValue;
      }
      else if (pa instanceof PointFem3dAttachment) {
         PointFem3dAttachment pfa = (PointFem3dAttachment)pa;
         double[] wgts = pfa.getCoordinates().getBuffer();
         double value = 0;
         int k = 0;
         for (FemNode n : pfa.getNodes()) {
            // XXX is this checking needed? Can we assume the node is a
            // FemNode3d that it belongs to the right FEM?
            if (n instanceof FemNode3d && n.getGrandParent() == myFem) {
               value += wgts[k++]*valueAtNodes[nodes.indexOf((FemNode3d)n)];
            }
            else {
               value += wgts[k++]*myDefaultValue;
            }
         }
         return value;
      }
      else {
         return myDefaultValue;
      }
   }

   /**
    * Find the values at every node.
    */
   private double[] getValuesAtNodes() {
      double[] values = new double[myFem.numNodes()];
      PointList<FemNode3d> allNodes = myFem.getNodes();
      if (getVolumeElemsVisible()) {
         for (FemElement3d elem : myFem.getElements()) {
            double[] nodalExtrapMat = elem.getNodalAveragingMatrix().getBuffer();
            FemNode3d[] enodes = elem.getNodes();
            int numIpnts = elem.numIntegrationPoints();
            for (int k=0; k<numIpnts; k++) {
               for (int i=0; i<enodes.length; i++) {
                  int nadjacent = enodes[i].numAdjacentVolumeElements();
                  if (getShellElemsVisible()) {
                     nadjacent += enodes[i].numAdjacentShellElements();
                  }
                  int nidx = allNodes.indexOf(enodes[i]);
                  double a = nodalExtrapMat[i*numIpnts + k];
                  if (a != 0) {
                     values[nidx] += a*getValue(elem,k)/(double)nadjacent;
                  }
               }
            }
         }
      }
      if (getShellElemsVisible()) {
         for (ShellElement3d elem : myFem.getShellElements()) {
            double[] nodalExtrapMat = elem.getNodalAveragingMatrix().getBuffer();
            FemNode3d[] enodes = elem.getNodes();
            int numIpnts = elem.numIntegrationPoints();
            for (int k=0; k<numIpnts; k++) {
               for (int i=0; i<enodes.length; i++) {
                  int nadjacent = enodes[i].numAdjacentShellElements();
                  if (getVolumeElemsVisible()) {
                     nadjacent += enodes[i].numAdjacentVolumeElements();
                  }
                  int nidx = allNodes.indexOf(enodes[i]);
                  double a = nodalExtrapMat[i*numIpnts + k];
                  if (a != 0) {
                     values[nidx] += a*getValue(elem,k)/(double)nadjacent;
                  }
               }
            }
         }
      }
      return values;
   }

   protected ScalarVertexFunction getVertexFunction () {
      double[] values = getValuesAtNodes();
      return ((mcomp,vtx) -> getVertexValue (
                 ((FemMeshComp)mcomp).getVertexAttachment(vtx), values));
   }

}
