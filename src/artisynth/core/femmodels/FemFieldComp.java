package artisynth.core.femmodels;

import java.io.*;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;

import java.util.ArrayList;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.PointList;
import artisynth.core.femmodels.FemElement.ElementClass;
import artisynth.core.util.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.render.*;
import maspack.properties.PropertyDesc.TypeCode;
import maspack.properties.PropertyDesc;

public abstract class FemFieldComp
   extends ModelComponentBase implements FieldComponent {
   
   protected int myShellIndexOffset;
   protected FemModel3d myFem;
   protected RenderProps myRenderProps;

   protected void setFem (FemModel3d fem) {
      myFem = fem;
      myShellIndexOffset = fem.getElements().getNumberLimit();
   }

   protected int getElementIndex (FemElement3dBase elem) {
      int idx = elem.getNumber();
      if (elem.getElementClass() != ElementClass.VOLUMETRIC) {
         idx += myShellIndexOffset;
      }
      return idx;
   }

   protected FemElement3dBase getElementAtIndex (int elemIdx) {
      if (elemIdx >= myShellIndexOffset) {
         return myFem.getShellElementByNumber (elemIdx-myShellIndexOffset);
      }
      else {
         return myFem.getElementByNumber (elemIdx);
      }
   }

   protected String elemName (FemElement3dBase elem) {
      if (elem.getElementClass() != ElementClass.VOLUMETRIC) {
         return "shell element "+elem.getNumber();
      }
      else {
         return "volumetric element "+elem.getNumber();
      }
   }

   protected void writeValues (
      PrintWriter pw, NumberFormat fmt, DynamicDoubleArray values, 
      DynamicBooleanArray valuesSet, WritableTest writableTest)
      throws IOException {

      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (int num=0; num<values.size(); num++) {
         if (!valuesSet.get(num) || !writableTest.isWritable(num)) {
            pw.println ("null");
         }
         else {
            pw.println (fmt.format (values.get(num)));
         }
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }
 
   protected void writeScalarValueArrays (
      PrintWriter pw, NumberFormat fmt,
      ArrayList<double[]> valueArrays, WritableTest writableTest)
      throws IOException {

      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (int num=0; num<valueArrays.size(); num++) {
         double[] varray = valueArrays.get(num);
         if (varray == null || !writableTest.isWritable(num)) {
            pw.println ("null");
         }
         else {
            pw.print ("[ ");
            for (int k=0; k<varray.length; k++) {
               pw.print (fmt.format(varray[k])+" ");
            }
            pw.println ("]");
         }
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }
 
   protected void scanValues (
      ReaderTokenizer rtok,
      DynamicDoubleArray values, DynamicBooleanArray valuesSet)
      throws IOException {

      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (rtok.tokenIsWord() && rtok.sval.equals ("null")) {
            values.add (0);
            valuesSet.add (false);
         }
         else if (rtok.tokenIsNumber()) {
            values.add (rtok.nval);
            valuesSet.add (true);
         }
         else {
            throw new IOException ("Expecting number or 'null', got "+rtok);
         }
      }
   }

   protected void scanScalarValueArrays (
      ReaderTokenizer rtok, ArrayList<double[]> valueArrays)
      throws IOException {

      ArrayList<Double> values = new ArrayList<>();
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (rtok.tokenIsWord() && rtok.sval.equals ("null")) {
            valueArrays.add (null);
         }
         else {
            if (rtok.ttype != '[') {
               throw new IOException ("Expecting token '[', got "+rtok);
            }
            values.clear();
            while (rtok.nextToken() != ']') {
               rtok.pushBack();
               values.add (rtok.scanNumber());
            }
            valueArrays.add (ArraySupport.toDoubleArray(values));
         }
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "fem", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);      
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.println ("fem=" + ComponentUtils.getWritePathName (ancestor, myFem));
   }

   private String elemName (ElementClass eclass, int num) {
      if (eclass == ElementClass.VOLUMETRIC) {
         return "volumetric element number "+num;
      }
      else {
         return "shell element number"+num;
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

   protected void checkScalarValueArraysSizes (
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

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "fem")) {
         FemModel3d fem = 
            ScanWriteUtils.postscanReference (
               tokens, FemModel3d.class, ancestor);
         setFem (fem);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected interface ReferenceTest {
      boolean isReferenced (int num);
   }

   protected interface WritableTest {
      boolean isWritable (int num);
   }

   protected class ElementReferencedTest implements ReferenceTest {
      FemElement3dList<? extends FemElement3dBase> myList;

      ElementReferencedTest (FemElement3dList<? extends FemElement3dBase> list) {
         myList = list;
      }

      public boolean isReferenced (int num) {
         return myList.getByNumber (num) != null;
      }      
   }

   protected class NodeReferencedTest implements ReferenceTest {
      PointList<FemNode3d> myList;

      NodeReferencedTest (PointList<FemNode3d> list) {
         myList = list;
      }

      public boolean isReferenced (int num) {
         return myList.getByNumber (num) != null;
      }      
   }

   protected class ElementWritableTest implements WritableTest {
      FemElement3dList<? extends FemElement3dBase> myList;

      ElementWritableTest (FemElement3dList<? extends FemElement3dBase> list) {
         myList = list;
      }

      public boolean isWritable (int num) {
         FemElement3dBase e = myList.getByNumber(num);
         return e != null && e.isWritable();
      }      
   }

   protected class NodeWritableTest implements WritableTest {
      PointList<FemNode3d> myList;

      NodeWritableTest (PointList<FemNode3d> list) {
         myList = list;
      }

      public boolean isWritable (int num) {
         FemNode3d n = myList.getByNumber(num);
         return n != null && n.isWritable();
      }      
   }

   protected static class NumDoublePair {
      int myNum;
      double myValue;

      NumDoublePair (int num, double value) {
         myNum = num;
         myValue = value;
      }
   }
   
   protected void removeUnreferencedValues (
      DynamicDoubleArray values, DynamicBooleanArray valuesSet,
      ReferenceTest test, Deque<Object> undoInfo) {

      ArrayList<NumDoublePair> removedValues = new ArrayList<>();
      for (int i=0; i<values.size(); i++) {
         if (valuesSet.get(i)) {
            if (!test.isReferenced(i)) {
               removedValues.add (new NumDoublePair (i, values.get(i)));
               valuesSet.set (i, false);
               System.out.println ("removed double value at " + i);
            }
         }
      }
      if (removedValues.size() > 0) {
         undoInfo.addLast (removedValues);
      }
      else {
         undoInfo.addLast (NULL_OBJ);
      }     
   }

   protected void restoreReferencedValues (
      DynamicDoubleArray values, DynamicBooleanArray valuesSet,
      Deque<Object> undoInfo) {

      Object obj = undoInfo.removeFirst();
      if (obj != NULL_OBJ) {
         for (NumDoublePair pair : (ArrayList<NumDoublePair>)obj) {
            values.set (pair.myNum, pair.myValue);
            valuesSet.set (pair.myNum, true);
            System.out.println ("restored double value at " + pair.myNum);
         }
      }
   }

   protected static class NumValuePair<T> {
      int myNum;
      T myValue;

      NumValuePair (int num, T value) {
         myNum = num;
         myValue = value;
      }
   }

   protected <T> void removeUnreferencedValues (
      ArrayList<T> values, ReferenceTest test, Deque<Object> undoInfo) {

      ArrayList<NumValuePair<T>> removedValues = new ArrayList<>();
      for (int i=0; i<values.size(); i++) {
         if (values.get(i) != null) {
            if (!test.isReferenced(i)) {
               removedValues.add (new NumValuePair<T> (i, values.get(i)));
               values.set (i, null);
               System.out.println ("removed value at " + i);
            }
         }
      }
      if (removedValues.size() > 0) {
         undoInfo.addLast (removedValues);
      }
      else {
         undoInfo.addLast (NULL_OBJ);
      }     
   }

   protected <T> void restoreReferencedValues (
      ArrayList<T> values, Deque<Object> undoInfo) {

      Object obj = undoInfo.removeFirst();
      if (obj != NULL_OBJ) {
         for (NumValuePair<T> pair : (ArrayList<NumValuePair<T>>)obj) {
            values.set (pair.myNum, pair.myValue);
            System.out.println ("restored value at " + pair.myNum);
         }
      }
   }
 
   public void clearCacheIfNecessary() {
   }

   protected void updateValueLists() {
   }

   protected <T> void resizeArrayList (
      ArrayList<T> list, int newsize) {
      while (list.size() < newsize) {
         list.add (null);
      }
      while (list.size() > newsize) {
         list.remove (list.size()-1);
      }
   }

   /* --- Begin partial implemetation of Renderable --- */

   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   public void setRenderProps (RenderProps props) {
      myRenderProps = RenderableComponentBase.updateRenderProps (
         this, myRenderProps, props);
   }

   public int getRenderHints() {
      int code = 0;
      if (myRenderProps != null && myRenderProps.isTransparent()) {
         code |= IsRenderable.TRANSPARENT;
      }
      return code;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return true;
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   public int numSelectionQueriesNeeded() {
      return -1;
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
   }

   public RenderProps createRenderProps() {
      return RenderProps.createRenderProps (this);
   }

   /* --- End partial implemetation of Renderable --- */

}
