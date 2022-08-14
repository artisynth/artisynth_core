package artisynth.core.femmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import artisynth.core.femmodels.FemElement.ElementClass;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.FieldPoint;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.util.ScanToken;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.MatrixNd;
import maspack.matrix.VectorObject;
import maspack.render.RenderObject;
import maspack.util.InternalErrorException;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.properties.PropertyDesc;

/**
 * A vector field defined over an FEM model, using values set at the element
 * integration points. Values at other points are obtained by interpolation
 * within the elements nearest to those points. Values at elements for which no
 * explicit values have been set are given by the field's <i>default
 * value</i>. Vectors are of type {@code T}, which must be an instance of
 * {@link VectorObject}.
 *
 * <p> For a given element {@code elem}, values should be specified for
 * <i>all</i> integration points, as returned by {@link
 * FemElement3dBase#getAllIntegrationPoints}. This includes the regular
 * integration points, as well as the <i>warping</i> point, which is located at
 * the element center and is used by corotated linear materials. Integration
 * point indices should be in the range {@code 0} to {@link
 * FemElement3dBase#numAllIntegrationPoints} - 1.
 */
public class VectorSubElemField<T extends VectorObject<T>> 
   extends VectorFemField<T> {
  
   protected ArrayList<T[]> myValues;
   protected ArrayList<T[]> myShellValues;

   protected void initValues () {
      myValues = new ArrayList<T[]>();
      myShellValues = new ArrayList<T[]>();
      updateValueLists();
      setRenderProps (createRenderProps());
   }

   protected void updateValueLists() {
      resizeArrayList (
         myValues, myFem.getElements().getNumberLimit());
      resizeArrayList (
         myShellValues, myFem.getShellElements().getNumberLimit());
   }
   
   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public VectorSubElemField (Class<T> type) {
      super (type);
   }

   /**
    * Constructs a field for a given FEM model, with a default value of 0.
    *
    * @param fem FEM model over which the field is defined
    */
   public VectorSubElemField (Class<T> type, FemModel3d fem)  {
      super (type, fem);
      initValues ();
   }

   /**
    * Constructs a field for a given FEM model and default value.
    * 
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for integration points which don't have
    * explicitly set values
    */
   public VectorSubElemField (
      Class<T> type, FemModel3d fem, T defaultValue) {
      super (type, fem, defaultValue);
      initValues ();
   }

   /**
    * Constructs a named field for a given FEM model, with a default value of
    * 0.
    * 
    * @param name name of the field
    * @param fem FEM model over which the field is defined
    */
   public VectorSubElemField (String name, Class<T> type, FemModel3d fem)  {
      this (type, fem);
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
   public VectorSubElemField (
      String name, Class<T> type, FemModel3d fem, T defaultValue) {
      this(type, fem, defaultValue);
      setName (name);
   }

   protected T[] createArray (int len) {
      return (T[])(new VectorObject[len]);      
   }

   protected T[] initValueArray (FemElement3dBase elem) {
      T[] varray = createArray(elem.numAllIntegrationPoints());
      for (int i=0; i<varray.length; i++) {
         varray[i] = myDefaultValue;
      }
      return varray;
   }

   private void checkElemNum (int elemNum) {
      if (elemNum >= myValues.size()) {
         throw new IllegalArgumentException (
            "elemNum="+elemNum+
            ", max elem num is "+ myFem.getElements().getNumberLimit());
      }
   }

   private void checkShellElemNum (int elemNum) {
      if (elemNum >= myShellValues.size()) {
         throw new IllegalArgumentException (
            "elemNum="+elemNum+
            ", max elem num is "+ myFem.getShellElements().getNumberLimit());
      }
   }

   private void checkSubIndex (T[] valueArray, int subIdx) {
      if (subIdx >= valueArray.length) {
         throw new IllegalArgumentException (
            "subIdx=" + subIdx +
            ", maximum value for element is " + (valueArray.length-1));
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
   public T getElementValue (int elemNum, int subIdx) {
      checkElemNum (elemNum);
      T[] varray = myValues.get(elemNum);
      if (varray == null) {
         return myDefaultValue;
      }
      else {
         if (subIdx == -1) {
            subIdx = 0;
         }
         checkSubIndex (varray, subIdx);         
         return varray[subIdx] != null ? varray[subIdx] : myDefaultValue;
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
   public T getShellElementValue (int elemNum, int subIdx) {
      checkShellElemNum (elemNum);
      T[] varray = myShellValues.get(elemNum);
      if (varray == null) {
         return myDefaultValue;
      }
      else {
         if (subIdx == -1) {
            subIdx = 0;
         }
         checkSubIndex (varray, subIdx);
         return varray[subIdx] != null ? varray[subIdx] : myDefaultValue;
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
   public T getValue (FemElement3dBase elem, int subIdx) {
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
   public T getValue (Point3d pos) {
      Point3d loc = new Point3d();
      FemElement3dBase elem = myFem.findNearestElement (loc, pos);
      if (elem == null) {
         // shouldn't happen, but just in case
         return myDefaultValue;
      }
      // TODO: if loc != pnt, then we are outside the element and we may want
      // to handle this differently - like by returning the default value.
      T[] values;
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         values = myValues.get (elem.getNumber());
      }
      else {
         values = myShellValues.get (elem.getNumber());
      }      
      if (values == null) {
         return myDefaultValue;
      }
      VectorNd weights = new VectorNd(elem.numNodes());
      elem.getMarkerCoordinates (weights, null, loc, /*checkInside=*/false);
      // nodal extrapolation matrix maps integration point values to nodes
      MatrixNd E = elem.getNodalExtrapolationMatrix();
      int npnts = E.colSize();
      double[] Ebuf = E.getBuffer();
      T value = createTypeInstance();
      for (int i=0; i<elem.numNodes(); i++) {
         for (int j=0; j<npnts; j++) {
            double a = Ebuf[i*npnts+j];
            if (a != 0) {
               T val = (values[j] != null ? values[j] : myDefaultValue);
               value.scaledAddObj (weights.get(i)*a, val);
            }
         }
      }
      return value;
   }

   /**
    * {@inheritDoc}
    */
   public T getValue (FieldPoint fp) {
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
    * Sets the value at an integration point of an element (either volumetric
    * or shell).
    * 
    * @param elem element containing the integration point
    * @param subIdx integration point index, in the range {@code 0} to {@code
    * n-1}, where {@code n} is the value returned by the element's {@link
    * FemElement3dBase#numAllIntegrationPoints} method
    * @param value new value for the integration point
    */
   public void setValue (FemElement3dBase elem, int subIdx, T value) {
      checkElementBelongsToFem (elem);
      String sizeErr = checkSize (value);
      if (sizeErr != null) {
         throw new IllegalArgumentException (
            "value for elem "+elem.getNumber()+", subIdx "+subIdx+": "+sizeErr);
      }
      int elemNum = elem.getNumber();
      ArrayList<T[]> valueArrays;
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         checkElemNum (elemNum);         
         valueArrays = myValues;
      }
      else {
         checkShellElemNum (elemNum);         
         valueArrays = myShellValues;
      }
      T[] varray = valueArrays.get(elemNum);
      if (varray == null) {
         varray = createArray (elem.numAllIntegrationPoints());
         valueArrays.set (elemNum, varray);
      }
      T storedValue = createTypeInstance();
      storedValue.set (value);
      checkSubIndex (varray, subIdx);
      varray[subIdx] = storedValue;
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
      T[] values;
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         checkElemNum (elemNum);
         values = myValues.get (elemNum);
      }
      else {
         checkShellElemNum (elemNum);
         values = myShellValues.get (elemNum);
      }
      if (values == null) {
         return false;
      }
      else {
         checkSubIndex (values, subIdx);
         return values[subIdx] != null;
      }
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
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         checkElemNum (elemNum);
         T[] values = myValues.get (elemNum);
         if (values != null) {
            checkSubIndex (values, subIdx);
            values[subIdx] = null;
            if (allValuesNull (values)) {
               myValues.set (elemNum, null);
            }
         }
      }
      else {
         checkShellElemNum (elemNum);
         T[] values = myShellValues.get (elemNum);
         if (values != null) {
            checkSubIndex (values, subIdx);
            values[subIdx] = null;
            if (allValuesNull (values)) {
               myShellValues.set (elemNum, null);
            }
         }
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public void clearAllValues() {
      for (int i=0; i<myValues.size(); i++) {
         myValues.set (i, null);
      }
      for (int i=0; i<myShellValues.size(); i++) {
         myShellValues.set (i, null);
      }
   }

   /* ---- Begin I/O methods ---- */

   protected <S extends VectorObject<S>> void writeValueArrays (
      PrintWriter pw, NumberFormat fmt,
      ArrayList<S[]> valueArrays, WritableTest writableTest) throws IOException {

      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (int num=0; num<valueArrays.size(); num++) {
         S[] varray = valueArrays.get(num);
         if (varray == null || !writableTest.isWritable(num)) {
            pw.println ("null");
         }
         else {
            pw.print ("[ ");
            IndentingPrintWriter.addIndentation (pw, 2);
            for (int k=0; k<varray.length; k++) {
               writeValue (pw, fmt, varray[k]);
            }
            IndentingPrintWriter.addIndentation (pw, -2);
            pw.println ("]");
         }
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }
 
   protected <S extends VectorObject<S>> void scanValueArrays (
      ReaderTokenizer rtok, ArrayList<S[]> valueArrays) throws IOException {
      ArrayList<S> scannedValues = new ArrayList<>();
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (rtok.tokenIsWord() && rtok.sval.equals ("null")) {
            valueArrays.add (null);
         }
         else {
            if (rtok.ttype != '[') {
               throw new IOException ("Expecting token '[', got "+rtok);
            }
            scannedValues.clear();
            while (rtok.nextToken() != ']') {
               S value;
               if (rtok.tokenIsWord() && rtok.sval.equals ("null")) {
                  value = null;
               }
               else {
                  rtok.pushBack();
                  value = (S)PropertyDesc.scanValue (
                     rtok, myValueType, myTypeParameter);
               }
               scannedValues.add (value);              
            }
            S[] values = (S[])scannedValues.toArray(new VectorObject[0]);
            if (allValuesNull (values)) {
               valueArrays.add (null);
            }
            else {
               valueArrays.add (values);
            }
         }
      }
   }

   protected <T> void checkValueArraysSizes (
      ArrayList<T[]> valueArrays, ElementClass eclass) throws IOException {

      FemElement3dList<?> elems;
      if (eclass == ElementClass.VOLUMETRIC) {
         elems = myFem.getElements();
      }
      else {
         elems = myFem.getShellElements();
      }
      for (int i=0; i<valueArrays.size(); i++) {
         T[] varray = valueArrays.get(i);
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
      writeValueArrays (
         pw, fmt, myValues, 
         new ElementWritableTest (myFem.getElements()));
      pw.print ("shellValues=");
      writeValueArrays (
         pw, fmt, myShellValues, 
         new ElementWritableTest (myFem.getShellElements()));
   }

   /**
    * {@inheritDoc}
    */
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "values")) {
         myValues = new ArrayList<T[]>();
         scanValueArrays (rtok, myValues);
         return true;
      }
      else if (scanAttributeName (rtok, "shellValues")) {
         myShellValues = new ArrayList<T[]>();
         scanValueArrays (rtok, myShellValues);
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
      checkValueArraysSizes (myValues, ElementClass.VOLUMETRIC);
      checkValueArraysSizes (myShellValues, ElementClass.SHELL);
   }

   /* ---- Begin edit methods ---- */

   /**
    * {@inheritDoc}
    */
   public void getSoftReferences (List<ModelComponent> refs) {
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

   // build render object for rendering Vector3d values

   protected RenderObject buildRenderObject() {
      if (myRenderScale != 0 && hasThreeVectorValue()) {
         RenderObject robj = new RenderObject();
         robj.createLineGroup();
         Point3d pos = new Point3d();
         Vector3d vec = new Vector3d();
         for (int num=0; num<myValues.size(); num++) {
            T[] vecs = (T[])myValues.get(num);
            if (vecs != null) {
               FemElement3d e = myFem.getElements().getByNumber(num);
               IntegrationPoint3d[] ipnts = e.getAllIntegrationPoints();
               for (int k=0; k<vecs.length; k++) {
                  if (getThreeVectorValue (vec, vecs[k])) {
                     ipnts[k].computePosition (pos, e.getNodes());
                     addLineSegment (robj, pos, vec);
                  }
               }
            }
         }
         for (int num=0; num<myShellValues.size(); num++) {
            T[] vecs = (T[])myShellValues.get(num);
            if (vecs != null) {
               ShellElement3d e = myFem.getShellElements().getByNumber(num);
               IntegrationPoint3d[] ipnts = e.getAllIntegrationPoints();
               for (int k=0; k<vecs.length; k++) {
                  if (getThreeVectorValue (vec, vecs[k])) {
                     ipnts[k].computePosition (pos, e.getNodes());
                     addLineSegment (robj, pos, vec);
                  }
               }
            }
         }
         return robj;
      }
      else {
         return null;
      }
   }
   
   /* ---- equality methods --- */

   private <S extends VectorObject<S>> boolean vectorArrayListEquals (
      ArrayList<S[]> list0, ArrayList<S[]> list1) {

      if (list0.size() != list1.size()) {
         return false;
      }
      for (int i=0; i<list0.size(); i++) {
         if (!vectorArrayEquals (list0.get(i), list1.get(i))) {
            return false;
         }
      }
      return true;
   }

   public boolean equals (VectorSubElemField<T> field) {
      return (
         super.equals (field) &&
         vectorArrayListEquals (myValues, field.myValues) &&
         vectorArrayListEquals (myShellValues, field.myShellValues));
   }

}
