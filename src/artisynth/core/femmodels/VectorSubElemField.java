package artisynth.core.femmodels;

import java.io.*;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.FemElement.ElementClass;
import artisynth.core.util.*;
import artisynth.core.modelbase.FieldUtils.VectorFieldFunction;

import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.PropertyDesc.TypeCode;
import maspack.properties.PropertyDesc;
import maspack.render.*;

public class VectorSubElemField<T extends VectorObject<T>> 
   extends VectorFemField<T> {
  
   protected ArrayList<T[]> myValueArrays;
   protected ArrayList<T[]> myShellValueArrays;

   protected class SubElemFieldFunction 
      extends VectorFieldFunction<T> {

      public SubElemFieldFunction () {
      }

      public VectorSubElemField<T> getField() {
         return VectorSubElemField.this;
      }

      public T eval (FieldPoint def) {
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

   public VectorFieldFunction<T> createFieldFunction (boolean useRestPos) {
      return new SubElemFieldFunction();
   }

   protected void initValues () {
      myValueArrays = new ArrayList<T[]>();
      myShellValueArrays = new ArrayList<T[]>();
      updateValueLists();
      setRenderProps (createRenderProps());
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
   public VectorSubElemField (Class<T> type) {
      super (type);
   }

   public VectorSubElemField (
      Class<T> type, FemModel3d fem, T defaultValue) {
      super (type, fem, defaultValue);
      initValues ();
   }

   public VectorSubElemField (Class<T> type, FemModel3d fem)  {
      super (type, fem);
      initValues ();
   }

   public VectorSubElemField (String name, Class<T> type, FemModel3d fem)  {
      this (type, fem);
      setName (name);
   }

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

   public T getValue (int elemIdx, int subIdx) {
      T[] varray = myValueArrays.get(elemIdx);
      if (varray == null) {
         return myDefaultValue;
      }
      else {
         return varray[subIdx];
      }
   }

   public T getShellValue (int elemIdx, int subIdx) {
      T[] varray = myShellValueArrays.get(elemIdx);
      if (varray == null) {
         return myDefaultValue;
      }
      else {
         return varray[subIdx];
      }
   }

   public T getValue (FemElement3dBase elem, int subIdx) {
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         return getValue (elem.getNumber(), subIdx);
      }
      else {
         return getShellValue (elem.getNumber(), subIdx);
      }
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
      T[] values = myValueArrays.get (getElementIndex(elem));
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
      T value = createInstance();
      for (int i=0; i<elem.numNodes(); i++) {
         for (int j=0; j<npnts; j++) {
            double a = Mex[i*npnts+j];
            if (a != 0) {
               value.scaledAddObj (weights.get(i)*a, values[j]);
            }
         }
      }
      return value;
   }

   public void setValue (FemElement3dBase elem, int subIdx, T value) {
      String sizeErr = checkSize (value);
      if (sizeErr != null) {
         throw new IllegalArgumentException (
            "value for elem "+elem.getNumber()+", subIdx "+subIdx+": "+sizeErr);
      }
      int elemNum = elem.getNumber();
      if (subIdx >= elem.numAllIntegrationPoints()) {
         throw new IllegalArgumentException (
            "subIdx=" + subIdx + ", maximum value for element " + elem +
            " is " + (elem.numAllIntegrationPoints()-1));
      }
      ArrayList<T[]> valueArrays;
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         valueArrays = myValueArrays;
      }
      else {
         valueArrays = myShellValueArrays;
      }
      T[] varray = valueArrays.get(elemNum);
      if (varray == null) {
         varray = initValueArray (elem);
         valueArrays.set (elemNum, varray);
      }
      T storedValue = createInstance();
      storedValue.set (value);
      varray[subIdx] = storedValue;
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.print ("values=");
      writeValueArrays (
         pw, fmt, myValueArrays, 
         new ElementWritableTest (myFem.getElements()));
      pw.print ("shellValues=");
      writeValueArrays (
         pw, fmt, myShellValueArrays, 
         new ElementWritableTest (myFem.getShellElements()));
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "values")) {
         myValueArrays = new ArrayList<T[]>();
         scanValueArrays (rtok, myValueArrays);
         return true;
      }
      else if (scanAttributeName (rtok, "shellValues")) {
         myShellValueArrays = new ArrayList<T[]>();
         scanValueArrays (rtok, myShellValueArrays);
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
      checkValueArraysSizes (myValueArrays, ElementClass.VOLUMETRIC);
      checkValueArraysSizes (myShellValueArrays, ElementClass.SHELL);
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

   // build render object for rendering Vector3d values

   protected RenderObject buildRenderObject() {
      if (myRenderScale != 0 && hasThreeVectorValue()) {
         RenderObject robj = new RenderObject();
         robj.createLineGroup();
         Point3d pos = new Point3d();
         Vector3d vec = new Vector3d();
         for (int num=0; num<myValueArrays.size(); num++) {
            T[] vecs = (T[])myValueArrays.get(num);
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
         for (int num=0; num<myShellValueArrays.size(); num++) {
            T[] vecs = (T[])myShellValueArrays.get(num);
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


}
