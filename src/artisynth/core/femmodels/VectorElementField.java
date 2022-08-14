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
import maspack.matrix.VectorObject;
import maspack.render.RenderObject;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * A vector field defined over an FEM model, using values set at the
 * elements.  Values at other points are obtained by finding the elements
 * nearest to those points. Values at element for which no explicit value has
 * been set are given by the field's <i>default value</i>. Since values are
 * assumed to be constant over a given element, this field is not continuous.
 * Vectors are of type {@code T}, which must be an instance of {@link
 * VectorObject}.
 */
public class VectorElementField<T extends VectorObject<T>> 
   extends VectorFemField<T> {

   protected ArrayList<T> myValues;
   protected ArrayList<T> myShellValues;

   protected void initValues () {
      myValues = new ArrayList<T>();
      myShellValues = new ArrayList<T>();
      updateValueLists();
      setRenderProps (createRenderProps());
   }

   protected void updateValueLists() {
      resizeArrayList (myValues, myFem.getElements().getNumberLimit());
      resizeArrayList (myShellValues, myFem.getShellElements().getNumberLimit());
   }

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public VectorElementField (Class<T> type) {
      super (type);
   }

   /**
    * Constructs a field for a given FEM model, with a default value of 0.
    *
    * @param type class type of the {@link VectorObject} describing the field's
    * @param fem FEM model over which the field is defined
    */
   public VectorElementField (Class<T> type, FemModel3d fem) {
      super (type, fem, null);
      initValues ();
   }

   /**
    * Constructs a field for a given FEM model and default value.
    * 
    * @param type class type of the {@link VectorObject} describing the field's
    * values
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for elements which don't have
    * explicitly set values
    */
   public VectorElementField (Class<T> type, FemModel3d fem, T defaultValue) {
      super (type, fem, defaultValue);
      initValues ();
   }

  /**
    * Constructs a named field for a given FEM model, with a default value of 0.
    * 
    * @param name name of the field
    * @param type class type of the {@link VectorObject} describing the field's
    * values
    * @param fem FEM model over which the field is defined
    */
   public VectorElementField (String name, Class<T> type, FemModel3d fem) {
      this (type, fem, null);
      setName (name);
   }

   /**
    * Constructs a named field for a given FEM model and default value.
    *
    * @param name name of the field
    * @param type class type of the {@link VectorObject} describing the field's
    * values
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for elements which don't have
    * explicitly set values
    */
   public VectorElementField (
      String name, Class<T> type, FemModel3d fem, T defaultValue) {
      this (type, fem, defaultValue);
      setName (name);
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

   /**
    * Returns the value at the volumetric element specified by the given
    * number. The default value is returned if a value has not been explicitly
    * set for that element.  Element numbers are used instead of indices as
    * they are more persistent if the FEM model is modified.
    * 
    * @param elemNum volumetric element number
    * @return value at the element
    */
   public T getElementValue (int elemNum) {
      checkElemNum (elemNum);
      T value = myValues.get (elemNum);
      if (value == null) {
         return myDefaultValue;
      }
      else {
         return value;
      }
   }

   /**
    * Returns the value at the shell element specified by the given number. The
    * default value is returned if a value has not been explicitly set for that
    * element.  Element numbers are used instead of indices as they are more
    * persistent if the FEM model is modified.
    * 
    * @param elemNum shell element number
    * @return value at the element
    */
   public T getShellElementValue (int elemNum) {
      checkShellElemNum (elemNum);
      T value = myShellValues.get (elemNum);
      if (value == null) {
         return myDefaultValue;
      }
      else {
         return value;
      }
   }

   /**
    * Returns the value at an element (either volumetric or shell). The default
    * value is returned if a value has not been explicitly set for that
    * element.
    *
    * @param elem element for which the value is requested
    * @return value at the element
    */
   public T getValue (FemElement3dBase elem) {
      checkElementBelongsToFem (elem);
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         return getElementValue (elem.getNumber());
      }
      else {
         return getShellElementValue (elem.getNumber());
      }
   }

   /**
    * {@inheritDoc}
    */
   public T getValue (FieldPoint fp) {
      if (fp.getElementType() == 0) {
         return getElementValue (fp.getElementNumber());
      }
      else {
         return getShellElementValue (fp.getElementNumber());
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
      return getValue (elem);
   }

   /**
    * Sets the value at an element (either volumetric or shell).
    * 
    * @param elem element for which the value is to be set
    * @param value new value for the element
    */
   public void setValue (FemElement3dBase elem, T value) {
      checkElementBelongsToFem (elem);
      String sizeErr = checkSize (value);
      if (sizeErr != null) {
         throw new IllegalArgumentException (
            "value for elem "+elem.getNumber()+": "+sizeErr);
      }
      int elemNum = elem.getNumber();      
      T storedValue = createTypeInstance();
      storedValue.set (value);
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         checkElemNum (elemNum);
         myValues.set (elemNum, storedValue);
      }
      else {
         checkShellElemNum (elemNum);
         myShellValues.set (elemNum, storedValue);
      }
   }

   /**
    * Queries whether a value has been seen at a given element (either
    * volumetric or shell).
    * 
    * @param elem element being queried
    * @return {@code true} if a value has been set at the element
    */
   public boolean isValueSet (FemElement3dBase elem) {
      checkElementBelongsToFem (elem);
      int elemNum = elem.getNumber();
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         checkElemNum (elemNum);
         return myValues.get (elemNum) != null;
      }
      else {
         checkShellElemNum (elemNum);
         return myShellValues.get (elemNum) != null;
      }
   }
   
   /**
    * Clears the value at a given element (either volumetric or shell). After
    * this call, the element will be associated with the default value.
    * 
    * @param elem element whose value is to be cleared
    */
   public void clearValue (FemElement3dBase elem) {
      checkElementBelongsToFem (elem);
      int elemNum = elem.getNumber();
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         checkElemNum (elemNum);
         myValues.set (elemNum, null);
      }
      else {
         checkShellElemNum (elemNum);
         myShellValues.set (elemNum, null);
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public void clearAllValues() {
      for (int i=0; i<myValues.size(); i++){
         myValues.set (i, null);
      }
      for (int i=0; i<myShellValues.size(); i++){
         myShellValues.set (i, null);
      }
   }

   /* ---- Begin I/O methods ---- */

   /**
    * {@inheritDoc}
    */
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.print ("values=");
      writeValues (
         pw, fmt, myValues, 
         new ElementWritableTest(myFem.getElements()));
      pw.print ("shellValues=");
      writeValues (
         pw, fmt, myShellValues, 
         new ElementWritableTest(myFem.getShellElements()));
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
      else if (scanAttributeName (rtok, "shellValues")) {
         myShellValues = new ArrayList<T>();
         scanValues (rtok, myShellValues);
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
            myValues, new ElementReferencedTest (myFem.getElements()), 
            undoInfo);
         removeUnreferencedValues (
            myShellValues, new ElementReferencedTest (myFem.getShellElements()), 
            undoInfo);
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
            if (getThreeVectorValue (vec, myValues.get(num))) {
               FemElement3d e = myFem.getElements().getByNumber(num);
               e.computeCentroid (pos);
               addLineSegment (robj, pos, vec);
            }
         }
         for (int num=0; num<myShellValues.size(); num++) {
            if (getThreeVectorValue (vec, myShellValues.get(num))) {
               ShellElement3d e = myFem.getShellElements().getByNumber(num);
               e.computeCentroid (pos);
               addLineSegment (robj, pos, vec);
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
   public boolean equals (VectorElementField<T> field) {
      return (
         super.equals (field) &&
         vectorListEquals (myValues, field.myValues) &&
         vectorListEquals (myShellValues, field.myShellValues));
   }

}


