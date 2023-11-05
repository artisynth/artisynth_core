package artisynth.core.fields;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.List;

import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemElementRenderer;
import artisynth.core.femmodels.FemElement.ElementClass;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.FemFieldPoint;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.util.ScanToken;
import maspack.matrix.Point3d;
import maspack.util.DynamicBooleanArray;
import maspack.util.DynamicDoubleArray;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.*;
import maspack.properties.*;
import maspack.render.Renderer.*;
import maspack.render.*;

/**
 * A scalar field defined over an FEM model, using values set at the
 * elements. Values at other points are obtained by finding the elements
 * nearest to those points. Values at elements for which no explicit value has
 * been set are given by the field's <i>default value</i>. Since values are
 * assumed to be constant over a given element, this field is not continuous.
 */
public class ScalarElementField extends ScalarFemField {

   protected DynamicDoubleArray myValues;      // values at volumetic elements
   protected DynamicBooleanArray myValset;     // is volumetric value set?
   protected DynamicDoubleArray myShellValues; // values at shell elements
   protected DynamicBooleanArray myShellValset;// is shell value set?

   public static double DEFAULT_ELEMENT_WIDGET_SIZE = 0.75;
   protected double myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE = 0.75;

   public static boolean DEFAULT_VOLUME_ELEMS_VISIBLE = true;
   protected boolean myVolumeElemsVisible = DEFAULT_VOLUME_ELEMS_VISIBLE;

   public static boolean DEFAULT_SHELL_ELEMS_VISIBLE = true;
   protected boolean myShellElemsVisible = DEFAULT_SHELL_ELEMS_VISIBLE;

   public static PropertyList myProps =
      new PropertyList (ScalarElementField.class, ScalarFemField.class);

   static {
      myProps.add (
         "elementWidgetSize", "widget size for rendering elements",
         DEFAULT_ELEMENT_WIDGET_SIZE, "[0,1]");
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
            Visualization.POINT, Visualization.ELEMENT, Visualization.OFF });
   }

   public double getElementWidgetSize () {
      return myElementWidgetSize;
   }

   public void setElementWidgetSize (double size) {
      myElementWidgetSize = size;
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
      myValues = new DynamicDoubleArray();
      myValset = new DynamicBooleanArray();
      myShellValues = new DynamicDoubleArray();
      myShellValset = new DynamicBooleanArray();
      updateValueLists();
   }

   protected void updateValueLists() {
      int maxElements = myFem.getElements().getNumberLimit();
      int maxShellElements = myFem.getShellElements().getNumberLimit();
      myValues.resize (maxElements);
      myValset.resize (maxElements);
      myShellValues.resize (maxShellElements);
      myShellValset.resize (maxShellElements);
   }

   void updateValueRange (DoubleInterval range) {
      for (int i=0; i<myValset.size(); i++) {
         if (myValset.get(i)) {
            range.updateBounds (myValues.get(i));
         }
      }
      for (int i=0; i<myShellValset.size(); i++) {
         if (myShellValset.get(i)) {
            range.updateBounds (myShellValues.get(i));         
         }
      }
   }

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public ScalarElementField () {
   }

   /**
    * Constructs a field for a given FEM model, with a default value of 0.
    *
    * @param fem FEM model over which the field is defined
    */
   public ScalarElementField (FemModel3d fem) {
      super (fem, 0);
      initValues ();
   }

   /**
    * Constructs a field for a given FEM model and default value.
    * 
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for elements which don't have
    * explicitly set values
    */
   public ScalarElementField (FemModel3d fem, double defaultValue) {
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
   public ScalarElementField (String name, FemModel3d fem) {
      this (fem);
      setName (name);
   }

   /**
    * Constructs a named field for a given FEM model and default value.
    *
    * @param name name of the field
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for elements which don't have
    * explicitly set values
    */
   public ScalarElementField (
      String name, FemModel3d fem, double defaultValue) {
      this (fem, defaultValue);
      setName (name);
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

   /**
    * Returns the value at the volumetric element specified by the given
    * number. The default value is returned if a value has not been explicitly
    * set for that element.  Element numbers are used instead of indices as
    * they are more persistent if the FEM model is modified.
    * 
    * @param elemNum volumetric element number
    * @return value at the element
    */
   public double getElementValue (int elemNum) {
      checkElemNum (elemNum);
      if (myValset.get(elemNum)) {
         return myValues.get(elemNum);
      }
      else {
         return myDefaultValue;
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
   public double getShellElementValue (int elemNum) {
      checkShellElemNum (elemNum);
      if (myShellValset.get(elemNum)) {
         return myShellValues.get(elemNum);
      }
      else {
         return myDefaultValue;
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
   public double getValue (FemElement3dBase elem) {
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
   public double getValue (FemFieldPoint fp) {
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
   public double getValue (Point3d pos) {
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
   public void setValue (FemElement3dBase elem, double value) {
      checkElementBelongsToFem (elem);
      int elemNum = elem.getNumber();
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         checkElemNum (elemNum);
         myValues.set (elemNum, value);
         myValset.set (elemNum, true);
      }
      else {
         checkShellElemNum (elemNum);
         myShellValues.set (elemNum, value);
         myShellValset.set (elemNum, true);
      }
      notifyValuesChanged();
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
         return myValset.get (elemNum);
      }
      else {
         checkShellElemNum (elemNum);
         return myShellValset.get (elemNum);
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
         myValset.set (elemNum, false);
      }
      else {
         checkShellElemNum (elemNum);
         myShellValset.set (elemNum, false);
      }
      notifyValuesChanged();
   }
   
   /**
    * {@inheritDoc}
    */
   public void clearAllValues() {
      for (int i=0; i<myValset.size(); i++) {
         myValset.set (i, false);
      }
      for (int i=0; i<myShellValset.size(); i++) {
         myShellValset.set (i, false);
      }
      notifyValuesChanged();
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
         pw, fmt, myValues, myValset, 
         new ElementWritableTest(myFem.getElements()));
      pw.print ("shellValues=");
      writeValues (
         pw, fmt, myShellValues, myShellValset, 
         new ElementWritableTest(myFem.getShellElements()));
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
      else if (scanAttributeName (rtok, "shellValues")) {
         myShellValues = new DynamicDoubleArray();
         myShellValset = new DynamicBooleanArray();
         scanValues (rtok, myShellValues, myShellValset);
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
            refs.add (myFem.getElements().getByNumber(i));
         }
      }
      for (int i=0; i<myShellValues.size(); i++) {
         if (myShellValset.get(i)) {
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
         restoreReferencedValues (myValues, myValset, undoInfo);
         restoreReferencedValues (myShellValues, myShellValset, undoInfo);
      }
      else {
         removeUnreferencedValues (
            myValues, myValset,
            new ElementReferencedTest(myFem.getElements()), undoInfo);
         removeUnreferencedValues (
            myShellValues, myShellValset,
            new ElementReferencedTest(myFem.getShellElements()), undoInfo);
      }
   }

   /**
    * Returns {@code true} if this field is functionally equal to another field.
    * Intended mainly for testing and debugging.
    */
   public boolean equals (ScalarElementField field) {
      if (!super.equals (field)) {
         return false;
      }
      if (!valueSetArraysEqual (
             myValues, myValset, field.myValues, field.myValset)) {
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
    * Add element widgets for every element in elist.
    */
   private void addElemWidgets (
      RenderObject rob, int gid,
      FemElement3dList<? extends FemElement3dBase> elist, DoubleInterval range) {

      rob.triangleGroup (gid);
      for (FemElement3dBase elem : elist) {
         int cidx = getColorIndex (getValue(elem), range);
         rob.setCurrentColor (cidx);
         FemElementRenderer.addWidgetFaces (
            rob, null, elem, getElementWidgetSize());
      }     
      FemElementRenderer.updateWidgetNormals (rob, gid);
   }

   protected RenderObject buildElementRenderObject (DoubleInterval range) {
      RenderObject rob = new RenderObject();
      rob.createTriangleGroup(); // for volume widgets
      rob.createTriangleGroup(); // for shell widgets
      ScalarFieldUtils.addColors (rob, myColorMap);
      if (getVolumeElemsVisible()) {
         addElemWidgets (rob, 0, myFem.getElements(), range);
      }
      if (getShellElemsVisible()) {
         addElemWidgets (rob, 1, myFem.getShellElements(), range);
      }
      return rob;
   }

   /**
    * Add points for every elements in elist.
    */
   private int addElemPoints (
      RenderObject rob, FemElement3dList<? extends FemElement3dBase> elist,
      DoubleInterval range, int vidx) {

      Point3d cent = new Point3d();
      for (FemElement3dBase elem : elist) {
         elem.computeCentroid (cent);
         rob.addPosition (cent);
         int cidx = getColorIndex (getValue(elem), range);
         rob.addVertex (vidx, -1, cidx, -1);
         rob.addPoint (vidx);
         vidx++;
      }
      return vidx;
   }

   protected RenderObject buildPointRenderObject (DoubleInterval range) {
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

}
