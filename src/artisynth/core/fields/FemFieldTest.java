package artisynth.core.fields;

import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.ScanTest;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemElement.ElementClass;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;

public class FemFieldTest extends FieldTestBase {

   int defaultSpacing = 5; // spacing for default values

   private double[] getRandomCoords (FemElement3dBase elem) {
      Vector3d ncoords = new Vector3d();
      if (elem instanceof HexElement || elem instanceof PyramidElement) {
         ncoords.x = RandomGenerator.nextDouble (-1, 1);
         ncoords.y = RandomGenerator.nextDouble (-1, 1);
         ncoords.z = RandomGenerator.nextDouble (-1, 1);
      }
      else if (elem instanceof WedgeElement) {
         double[] coords = RandomGenerator.randomUnityPartition (3);
         ncoords.x = coords[1];
         ncoords.y = coords[2];
         ncoords.z = RandomGenerator.nextDouble (-1, 1);
      }
      else if (elem instanceof TetElement) {
         double[] coords = RandomGenerator.randomUnityPartition (4);
         ncoords.x = coords[1];
         ncoords.y = coords[2];
         ncoords.z = coords[3];
      }
      else if (elem instanceof ShellQuadElement) {
         ncoords.x = RandomGenerator.nextDouble (-1, 1);
         ncoords.y = RandomGenerator.nextDouble (-1, 1);
         ncoords.z = 0;
      }
      else if (elem instanceof ShellTriElement) {
         double[] coords = RandomGenerator.randomUnityPartition (3);
         ncoords.x = coords[1];
         ncoords.y = coords[2];
         ncoords.z = 0;
      }
      
      double[] wgts = new double[elem.numNodes()];
      for (int i=0; i<wgts.length; i++) {
         wgts[i] = elem.getN (i, ncoords);
      }
      return wgts;
   }

   Point3d getPosInElement (FemElement3dBase elem, double[] wgts) {
      Point3d pos = new Point3d();
      for (int k=0; k<wgts.length; k++) {
         FemNode3d node = elem.getNodes()[k];
         pos.scaledAdd (wgts[k], node.getPosition());
      }
      return pos;
   }
   
   public void testScalarValueInElem (
      String name, FemElement3dBase elem, ScalarNodalField field) {
      double[] wgts = getRandomCoords (elem);
      double valueChk = 0;
      Point3d pos = getPosInElement (elem, wgts);
      for (int k=0; k<wgts.length; k++) {
         valueChk += wgts[k]*field.getValue(elem.getNodes()[k]);
      }
      checkEquals (
         "value at "+name+" pos", field.getValue(pos), valueChk, 1e-10);
   }

   public <T extends VectorObject<T>> void testVectorValueInElem (
      String name, FemElement3dBase elem, VectorNodalField<T> field) {

      double[] wgts = getRandomCoords (elem);
      T valueChk = field.createTypeInstance();
      Point3d pos = getPosInElement (elem, wgts);
      for (int k=0; k<wgts.length; k++) {
         valueChk.scaledAddObj (wgts[k], field.getValue(elem.getNodes()[k]));
      }
      T value = field.getValue(pos);
      if (!value.epsilonEquals (valueChk, 1e-10)) {
         throw new TestException (
            "value at "+name+" pos = "+value+", expected "+valueChk);
      }
   }

   public void checkScalarNodalField (ScalarNodalField field) {
      FemModel3d fem = field.getFemModel();
      for (int i=0; i<fem.numNodes(); i++) {
         FemNode3d node = fem.getNode(i);
         double valueChk;
         if (field.isValueSet (node)) {
            valueChk = i;
         }
         else {
            valueChk = field.getDefaultValue();
         }
         checkEquals (
            "value at node "+i, field.getValue (node.getNumber()), valueChk);
         checkEquals (
            "value at node pos "+i,
            field.getValue(node.getPosition()), valueChk, 1e-10);
      }
      // check bad number exception 
      if (fem.getOneBasedNodeElementNumbering()) {
         checkForIllegalArgumentException (()->field.getValue (fem.numNodes()+1));
      }
      else {
         checkForIllegalArgumentException (()->field.getValue (fem.numNodes()));
      }

      for (int i=0; i<fem.numElements(); i++) {
         testScalarValueInElem ("elem "+i, fem.getElement(i), field);
      }
      for (int i=0; i<fem.numShellElements(); i++) {
         testScalarValueInElem ("shell elem "+i, fem.getShellElement(i), field);
      }
   }

   public void checkScalarElemValue (
      String name, FemElement3dBase elem, int idx, ScalarElementField field) {
      double[] wgts = getRandomCoords (elem);
      Point3d pos = getPosInElement (elem, wgts);
      double valueChk;
      if (field.isValueSet (elem)) {
         valueChk = idx;
      }
      else {
         valueChk = field.getDefaultValue();
      }
      checkEquals (
         "value at "+name, field.getValue (elem), valueChk);
      checkEquals (
         "value at point in "+name,
         field.getValue(pos), valueChk, 1e-10);
   }

   public void checkScalarElementField (ScalarElementField field) {
      FemModel3d fem = field.getFemModel();
      for (int i=0; i<fem.numElements(); i++) {
         checkScalarElemValue (
            "elem" + i, fem.getElement(i), i, field);
      }
      for (int i=0; i<fem.numShellElements(); i++) {
         checkScalarElemValue (
            "shell elem " + i, fem.getShellElement(i), i, field);
      }
      // check bad number exception 
      if (fem.getOneBasedNodeElementNumbering()) {
         checkForIllegalArgumentException (
            () -> field.getElementValue (fem.numElements()+1));
         checkForIllegalArgumentException (
            () -> field.getShellElementValue (fem.numShellElements()+1));
      }
      else {
         checkForIllegalArgumentException (
            () -> field.getElementValue (fem.numElements()));
         checkForIllegalArgumentException (
            () -> field.getShellElementValue (fem.numShellElements()));
      }
   }

   public void checkScalarSubElemValues (
      String name, FemElement3dBase elem, ScalarSubElemField field) {
      double[] wgts = getRandomCoords (elem);
      Point3d pos = getPosInElement (elem, wgts);
      double valueChk;
      IntegrationPoint3d[] ipnts = elem.getAllIntegrationPoints();
      int numRegPnts = elem.numIntegrationPoints();
      for (int j=0; j<ipnts.length; j++) {
         if (field.isValueSet (elem, j)) {
            valueChk = getSubElemTestValue (elem, j);
         }
         else {
            valueChk = field.getDefaultValue();
         }
         checkEquals (
            "value at "+name+", "+j, field.getValue (elem, j), valueChk);
          if (j < numRegPnts) {
            ipnts[j].computePosition (pos, elem.getNodes());
            double value = field.getValue(pos);
            checkEquals ("value at pos for "+name+", "+j,
                         field.getValue (pos), valueChk, 1e-10);
         }        
      }
   }

   public <T extends VectorObject<T>> void checkVectorSubElemValues (
      String name, FemElement3dBase elem, VectorSubElemField<T> field) {
      double[] wgts = getRandomCoords (elem);
      Point3d pos = getPosInElement (elem, wgts);
      T valueChk;
      IntegrationPoint3d[] ipnts = elem.getAllIntegrationPoints();
      int numRegPnts = elem.numIntegrationPoints();
      for (int j=0; j<ipnts.length; j++) {
         if (field.isValueSet (elem, j)) {
            valueChk = field.createTypeInstance();
            setValue (valueChk, getSubElemTestValue (elem, j));
         }
         else {
            valueChk = field.getDefaultValue();
         }
         T value = field.getValue (elem, j);
         if (!value.epsilonEquals (valueChk, 0)) {
            throw new TestException (
               "value at "+name+", "+j+" = " + value + ", expecting "+valueChk);
         }
         if (j < numRegPnts) {
            ipnts[j].computePosition (pos, elem.getNodes());
            value = field.getValue(pos);
            if (!value.epsilonEquals (valueChk, 1e-10)) {
               throw new TestException (
                  "value at pos for "+name+", "+j+" = " + value +
                  ", expecting "+valueChk);                  
            }
         }
      }
      // checkEquals (
      //    "value at point in "+name,
      //    field.getValue(pos), valueChk, 1e-10);
   }

   public void checkScalarSubElemField (ScalarSubElemField field) {
      FemModel3d fem = field.getFemModel();
      for (int i=0; i<fem.numElements(); i++) {
         checkScalarSubElemValues (
            "elem" + i, fem.getElement(i), field);
      }
      for (int i=0; i<fem.numShellElements(); i++) {
         checkScalarSubElemValues (
            "shell elem " + i, fem.getShellElement(i), field);
      }
      // check bad number exception 
      if (fem.getOneBasedNodeElementNumbering()) {
         checkForIllegalArgumentException (
            () -> field.getElementValue (fem.numElements()+1, 0));
         checkForIllegalArgumentException (
            () -> field.getShellElementValue (fem.numShellElements()+1, 0));
      }
      else {
         checkForIllegalArgumentException (
            () -> field.getElementValue (fem.numElements(), 0));
         checkForIllegalArgumentException (
            () -> field.getShellElementValue (fem.numShellElements(), 0));
      }
   }

   public <T extends VectorObject<T>> void checkVectorNodalField (
      VectorNodalField<T> field) {
      FemModel3d fem = field.getFemModel();
      for (int i=0; i<fem.numNodes(); i++) {
         FemNode3d node = fem.getNode(i);
         T valueChk;
         if (field.isValueSet (node)) {
            valueChk = field.createTypeInstance();
            setValue (valueChk, i);
         }
         else {
            valueChk = field.getDefaultValue();
         }
         T value = field.getValue(node.getNumber());
         if (!value.epsilonEquals (valueChk, 0)) {
            throw new TestException (
               "value at node "+i+" = " + value + ", expecting "+valueChk);
         }
         value = field.getValue(node.getPosition());
         if (!value.epsilonEquals (valueChk, 1e-10)) {
            throw new TestException (
               "value at node pos "+i+" = " + value + ", expecting "+valueChk);
         }
      }
      // check bad number exception 
      if (fem.getOneBasedNodeElementNumbering()) {
         checkForIllegalArgumentException (()->field.getValue (fem.numNodes()+1));
      }
      else {
         checkForIllegalArgumentException (()->field.getValue (fem.numNodes()));
      }

      for (int i=0; i<fem.numElements(); i++) {
         testVectorValueInElem ("elem "+i, fem.getElement(i), field);
      }
      for (int i=0; i<fem.numShellElements(); i++) {
         testVectorValueInElem ("shell elem "+i, fem.getShellElement(i), field);
      }
   }

   public <T extends VectorObject<T>> void checkVectorElemValue (
      String name, FemElement3dBase elem, int idx, VectorElementField<T> field) {

      double[] wgts = getRandomCoords (elem);
      T valueChk;
      if (field.isValueSet (elem)) {
         valueChk = field.createTypeInstance();
         setValue (valueChk, idx);
      }
      else {
         valueChk = field.getDefaultValue();
      }
      T value = field.getValue(elem);
      if (!value.epsilonEquals (valueChk, 0)) {
         throw new TestException (
            "value at "+name+" = " + value + ", expecting "+valueChk);
      }
      Point3d pos = getPosInElement (elem, wgts);
      value = field.getValue(pos);
      if (!value.epsilonEquals (valueChk, 1e-10)) {
         throw new TestException (
            "value at "+name+" pos = " + value + ", expecting "+valueChk);
      }
   }   

   public <T extends VectorObject<T>> void checkVectorElementField (
      VectorElementField<T> field) {
      FemModel3d fem = field.getFemModel();
      for (int i=0; i<fem.numElements(); i++) {
         checkVectorElemValue ("elem "+i, fem.getElement(i), i, field);
      }
      for (int i=0; i<fem.numShellElements(); i++) {
         checkVectorElemValue ("shell elem "+i, fem.getShellElement(i), i, field);
      }
      // check bad number exception 
      if (fem.getOneBasedNodeElementNumbering()) {
         checkForIllegalArgumentException (
            () -> field.getElementValue (fem.numElements()+1));
         checkForIllegalArgumentException (
            () -> field.getShellElementValue (fem.numShellElements()+1));
      }
      else {
         checkForIllegalArgumentException (
            () -> field.getElementValue (fem.numElements()));
         checkForIllegalArgumentException (
            () -> field.getShellElementValue (fem.numShellElements()));
      }
   }

   public <T extends VectorObject<T>> void checkVectorSubElemField (
      VectorSubElemField<T> field) {
      FemModel3d fem = field.getFemModel();
      for (int i=0; i<fem.numElements(); i++) {
         checkVectorSubElemValues (
            "elem "+i, fem.getElement(i), field);
      }
      for (int i=0; i<fem.numShellElements(); i++) {
         checkVectorSubElemValues (
            "shell elem "+i, fem.getShellElement(i), field);
      }
      // check bad number exception 
      if (fem.getOneBasedNodeElementNumbering()) {
         checkForIllegalArgumentException (
            () -> field.getElementValue (fem.numElements()+1, 0));
         checkForIllegalArgumentException (
            () -> field.getShellElementValue (fem.numShellElements()+1, 0));
      }
      else {
         checkForIllegalArgumentException (
            () -> field.getElementValue (fem.numElements(), 0));
         checkForIllegalArgumentException (
            () -> field.getShellElementValue (fem.numShellElements(), 0));
      }
   }

   void testScalarNodalField (MechModel mech, ScalarNodalField field) {
      FemModel3d fem = field.getFemModel();
      for (int i=0; i<fem.numNodes(); i++) {
         FemNode3d node = fem.getNode(i);
         field.setValue (node, i);
         checkEquals ("isValueSet", field.isValueSet(node), true);
         if (i%defaultSpacing == 0) {
            field.clearValue (node);
            checkEquals ("isValueSet", field.isValueSet(node), false);
         }
      }
      field.setDefaultValue (100);
      checkScalarNodalField (field);      
      
      // check that we catch illegal nodes
      checkForIllegalArgumentException (
         () -> field.getValue (new FemNode3d()));         
      checkForIllegalArgumentException (
         () -> field.setValue (new FemNode3d(), 0));         
      checkForIllegalArgumentException (
         () -> field.isValueSet (new FemNode3d()));         
      checkForIllegalArgumentException (
         () -> field.clearValue (new FemNode3d()));         

      ScalarNodalField fieldChk =
         (ScalarNodalField)ScanTest.testScanAndWrite (field, mech, "%g");
      checkScalarNodalField (fieldChk); 
      if (!field.equals (fieldChk)) {
         throw new TestException ("scanned field not equal to scanned field");
      }
      
      // clear values and check again
      field.clearAllValues();
      for (FemNode3d node : fem.getNodes()) {
         checkEquals ("valueIsSet ", field.isValueSet(node), false);
      }
      checkScalarNodalField (field);      
      fieldChk =
         (ScalarNodalField)ScanTest.testScanAndWrite (field, mech, "%g");
      checkScalarNodalField (fieldChk);         
      if (!field.equals (fieldChk)) {
         throw new TestException ("field not equal to scanned field");
      }
   }

   <T extends VectorObject<T>> void testVectorNodalField (
      MechModel mech, VectorNodalField<T> field) {
      
      FemModel3d fem = field.getFemModel();
      for (int i=0; i<fem.numNodes(); i++) {
         FemNode3d node = fem.getNode(i);
         T value = field.createTypeInstance();
         setValue (value, i);
         field.setValue (node, value);
         checkEquals ("isValueSet", field.isValueSet(node), true);
         if (i%defaultSpacing == 0) {
            field.clearValue (node);
            checkEquals ("isValueSet", field.isValueSet(node), false);
         }
      }
      T value = field.createTypeInstance();
      setValue (value, 100);
      field.setDefaultValue (value);
      checkVectorNodalField (field);      

      // check that we catch illegal nodes
      checkForIllegalArgumentException (
         () -> field.getValue (new FemNode3d()));         
      checkForIllegalArgumentException (
         () -> field.setValue (new FemNode3d(), field.createTypeInstance()));
      checkForIllegalArgumentException (
         () -> field.isValueSet (new FemNode3d()));         
      checkForIllegalArgumentException (
         () -> field.clearValue (new FemNode3d()));         
      
      VectorNodalField<T> fieldChk =
         (VectorNodalField<T>)ScanTest.testScanAndWrite (
            field, mech, "%g");
      checkVectorNodalField (fieldChk);      
      if (!field.equals (fieldChk)) {
         throw new TestException ("field not equal to scanned field");
      }

      // clear values and check again
      field.clearAllValues();
      for (FemNode3d node : fem.getNodes()) {
         checkEquals ("valueIsSet ", field.isValueSet(node), false);
      }
      checkVectorNodalField (field);      
      fieldChk =
         (VectorNodalField)ScanTest.testScanAndWrite (field, mech, "%g");
      checkVectorNodalField (fieldChk);         
      if (!field.equals (fieldChk)) {
         throw new TestException ("field not equal to scanned field");
      }
   }

   void testScalarElementField (MechModel mech, ScalarElementField field) {
      FemModel3d fem = field.getFemModel();
      for (int i=0; i<fem.numElements(); i++) {
         FemElement3d elem = fem.getElement(i);
         field.setValue (elem, i);
         checkEquals ("isValueSet", field.isValueSet(elem), true);
         if (i%defaultSpacing == 0) {
            field.clearValue (elem);
            checkEquals ("isValueSet", field.isValueSet(elem), false);
         }
      }
      for (int i=0; i<fem.numShellElements(); i++) {
         ShellElement3d elem = fem.getShellElement(i);
         field.setValue (elem, i);
         checkEquals ("isValueSet", field.isValueSet(elem), true);
         if (i%defaultSpacing == 0) {
            field.clearValue (elem);
            checkEquals ("isValueSet", field.isValueSet(elem), false);
         }
      }
      field.setDefaultValue (100);
      checkScalarElementField (field);      

      // check that we catch illegal elements
      checkForIllegalArgumentException (
         () -> field.getValue (new TetElement()));         
      checkForIllegalArgumentException (
         () -> field.setValue (new TetElement(), 0));
      checkForIllegalArgumentException (
         () -> field.isValueSet (new TetElement()));         
      checkForIllegalArgumentException (
         () -> field.clearValue (new TetElement()));         
      
      ScalarElementField fieldChk =
         (ScalarElementField)ScanTest.testScanAndWrite (field, mech, "%g");
      checkScalarElementField (fieldChk);      
      if (!field.equals (fieldChk)) {
         throw new TestException ("field not equal to scanned field");
      }

      // clear values and check again
      field.clearAllValues();
      for (FemElement3d elem : fem.getElements()) {
         checkEquals ("valueIsSet ", field.isValueSet(elem), false);
      }
      checkScalarElementField (field);      
      fieldChk =
         (ScalarElementField)ScanTest.testScanAndWrite (field, mech, "%g");
      checkScalarElementField (fieldChk);         
      if (!field.equals (fieldChk)) {
         throw new TestException ("field not equal to scanned field");
      }
   }

   <T extends VectorObject<T>> void testVectorElementField (
      MechModel mech, VectorElementField<T> field) {

      FemModel3d fem = field.getFemModel();
      for (int i=0; i<fem.numElements(); i++) {
         FemElement3d elem = fem.getElement(i);
         T value = field.createTypeInstance();
         setValue (value, i);
         field.setValue (elem, value);
         checkEquals ("isValueSet", field.isValueSet(elem), true);
         if (i%defaultSpacing == 0) {
            field.clearValue (elem);
            checkEquals ("isValueSet", field.isValueSet(elem), false);
         }
      }
      for (int i=0; i<fem.numShellElements(); i++) {
         ShellElement3d elem = fem.getShellElement(i);
         T value = field.createTypeInstance();
         setValue (value, i);
         field.setValue (elem, value);
         checkEquals ("isValueSet", field.isValueSet(elem), true);
         if (i%defaultSpacing == 0) {
            field.clearValue (elem);
            checkEquals ("isValueSet", field.isValueSet(elem), false);
         }
      }
      T value = field.createTypeInstance();
      setValue (value, 100);
      field.setDefaultValue (value);
      checkVectorElementField (field);      
      
      // check that we catch illegal elements
      checkForIllegalArgumentException (
         () -> field.getValue (new TetElement()));         
      checkForIllegalArgumentException (
         () -> field.setValue (new TetElement(), field.createTypeInstance()));
      checkForIllegalArgumentException (
         () -> field.isValueSet (new TetElement()));         
      checkForIllegalArgumentException (
         () -> field.clearValue (new TetElement()));         

      VectorElementField<T> fieldChk =
         (VectorElementField<T>)ScanTest.testScanAndWrite (
            field, mech, "%g");
      checkVectorElementField (fieldChk);      
      if (!field.equals (fieldChk)) {
         throw new TestException ("field not equal to scanned field");
      }

      // clear values and check again
      field.clearAllValues();
      for (FemElement3d elem : fem.getElements()) {
         checkEquals ("valueIsSet ", field.isValueSet(elem), false);
      }
      checkVectorElementField (field);      
      fieldChk =
         (VectorElementField)ScanTest.testScanAndWrite (field, mech, "%g");
      checkVectorElementField (fieldChk);
      if (!field.equals (fieldChk)) {
         throw new TestException ("field not equal to scanned field");
      }
   }         

   double getSubElemTestValue (FemElement3dBase elem, int j) {
      // get a scalar test value for the j-th integration point of a given
      // element.
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         return elem.getNumber()*10+j;
      }
      else {
         int numRegIpnts = elem.numIntegrationPoints();
         int numLayers = numRegIpnts/elem.numNodes();
         // for shells, number of regular integration points exceeds the number
         // of nodes, with 2 or 3 per layer.
         if (j < numRegIpnts) {
            // Use % numLayers to ensure sure values are the same on each
            // layer, so test results will work out.
            return elem.getNumber()*10 + j%numLayers; 
         }
         else {
            return elem.getNumber()*10+j;
         }
      }
   }

   /**
    * Decide whether to clear a sub element test value. Need to do this so that
    * all integration points on the same layer are cleared, so that test
    * results will work out.
    */
   boolean clearSubElemValue (ShellElement3d elem, int j) {
      int numRegIpnts = elem.numIntegrationPoints();
      int numLayers = numRegIpnts/elem.numNodes();
      if (elem.getNumber() % 2 == 0) {
         // do this with every other element
         return false;
      }
      if (j < numRegIpnts) {
         // ipnts correspond to node 1
         if (j%numLayers == 1) {
            return true;
         }
      }
      return false;
   }

   void testScalarSubElemField (MechModel mech, ScalarSubElemField field) {
      FemModel3d fem = field.getFemModel();
      for (int i=0; i<fem.numElements(); i++) {
         FemElement3d elem = fem.getElement(i);
         IntegrationPoint3d[] ipnts = elem.getAllIntegrationPoints();
         for (int j=0; j<ipnts.length; j++) {
            field.setValue (elem, j, getSubElemTestValue (elem, j));
            checkEquals ("isValueSet", field.isValueSet(elem, j), true);
            if ((j+1)%defaultSpacing == 0) {
               field.clearValue (elem, j);
               checkEquals ("isValueSet", field.isValueSet(elem, j), false);
            }
         }
         if (i%defaultSpacing == 0) {
            for (int j=0; j<ipnts.length; j++) {
               field.clearValue (elem, j);
               checkEquals ("isValueSet", field.isValueSet(elem, j), false);
            }
         }
      }
      for (int i=0; i<fem.numShellElements(); i++) {
         ShellElement3d elem = fem.getShellElement(i);
         IntegrationPoint3d[] ipnts = elem.getAllIntegrationPoints();
         for (int j=0; j<ipnts.length; j++) {
            double value = getSubElemTestValue (elem, j);
            field.setValue (elem, j, value);
            checkEquals ("isValueSet", field.isValueSet(elem, j), true);
            if (clearSubElemValue (elem, j)) {
               field.clearValue (elem, j);
               checkEquals ("isValueSet", field.isValueSet(elem, j), false);
            }
         }
         if (i%defaultSpacing == 0) {
            for (int j=0; j<ipnts.length; j++) {
               field.clearValue (elem, j);
               checkEquals ("isValueSet", field.isValueSet(elem, j), false);
            }
         }
      }
      field.setDefaultValue (100);
      checkScalarSubElemField (field);      

      // check that we catch illegal elements
      checkForIllegalArgumentException (
         () -> field.getValue (new TetElement(), 0));         
      checkForIllegalArgumentException (
         () -> field.setValue (new TetElement(), 0, 0));
      checkForIllegalArgumentException (
         () -> field.isValueSet (new TetElement(), 0));         
      checkForIllegalArgumentException (
         () -> field.clearValue (new TetElement(), 0));         

      ScalarSubElemField fieldChk =
         (ScalarSubElemField)ScanTest.testScanAndWrite (field, mech, "%g");
      checkScalarSubElemField (fieldChk);      
      if (!field.equals (fieldChk)) {
         throw new TestException ("field not equal to scanned field");
      }

      // clear values and check again
      field.clearAllValues();
      for (FemElement3d elem : fem.getElements()) {
         for (int j=0; j<elem.numAllIntegrationPoints(); j++) {
            checkEquals ("isValueSet ", field.isValueSet(elem, j), false);
         }
      }
      for (ShellElement3d elem : fem.getShellElements()) {
         for (int j=0; j<elem.numAllIntegrationPoints(); j++) {
            checkEquals ("isValueSet ", field.isValueSet(elem, j), false);
         }
      }
      checkScalarSubElemField (field);      
      fieldChk =
         (ScalarSubElemField)ScanTest.testScanAndWrite (field, mech, "%g");
      checkScalarSubElemField (fieldChk);         
      if (!field.equals (fieldChk)) {
         throw new TestException ("field not equal to scanned field");
      }
   }

   <T extends VectorObject<T>> void testVectorSubElemField (
      MechModel mech, VectorSubElemField<T> field) {

      FemModel3d fem = field.getFemModel();
      for (int i=0; i<fem.numElements(); i++) {
         FemElement3d elem = fem.getElement(i);
         IntegrationPoint3d[] ipnts = elem.getAllIntegrationPoints();
         for (int j=0; j<ipnts.length; j++) {
            T value = field.createTypeInstance();
            setValue (value, getSubElemTestValue (elem, j));
            field.setValue (elem, j, value);
            checkEquals ("isValueSet", field.isValueSet(elem, j), true);
            if ((j+1)%defaultSpacing == 0) {
               field.clearValue (elem, j);
               checkEquals ("isValueSet", field.isValueSet(elem, j), false);
            }
         }
         if (i%defaultSpacing == 0) {
            for (int j=0; j<ipnts.length; j++) {
               field.clearValue (elem, j);
               checkEquals ("isValueSet", field.isValueSet(elem, j), false);
            }
         }
      }
      for (int i=0; i<fem.numShellElements(); i++) {
         ShellElement3d elem = fem.getShellElement(i);
         IntegrationPoint3d[] ipnts = elem.getAllIntegrationPoints();
         for (int j=0; j<ipnts.length; j++) {
            T value = field.createTypeInstance();
            setValue (value, getSubElemTestValue (elem, j));
            field.setValue (elem, j, value);
            checkEquals ("isValueSet", field.isValueSet(elem, j), true);
            if (clearSubElemValue (elem, j)) {
               field.clearValue (elem, j);
               checkEquals ("isValueSet", field.isValueSet(elem, j), false);
            }
         }
         if (i%defaultSpacing == 0) {
            for (int j=0; j<ipnts.length; j++) {
               field.clearValue (elem, j);
               checkEquals ("isValueSet", field.isValueSet(elem, j), false);
            }
         }
      }
      T value = field.createTypeInstance();
      setValue (value, 100);
      field.setDefaultValue (value);
      checkVectorSubElemField (field);      

      // check that we catch illegal elements
      checkForIllegalArgumentException (
         () -> field.getValue (new TetElement(), 0));         
      checkForIllegalArgumentException (
         () -> field.setValue (new TetElement(), 0, field.createTypeInstance()));
      checkForIllegalArgumentException (
         () -> field.isValueSet (new TetElement(), 0));         
      checkForIllegalArgumentException (
         () -> field.clearValue (new TetElement(), 0));         
      
      VectorSubElemField<T> fieldChk =
         (VectorSubElemField<T>)ScanTest.testScanAndWrite (
            field, mech, "%g");
      checkVectorSubElemField (fieldChk);      
      if (!field.equals (fieldChk)) {
         throw new TestException ("field not equal to scanned field");
      }

      // clear values and check again
      field.clearAllValues();
      for (FemElement3d elem : fem.getElements()) {
         for (int j=0; j<elem.numAllIntegrationPoints(); j++) {
            checkEquals ("isValueSet ", field.isValueSet(elem, j), false);
         }
      }
      for (ShellElement3d elem : fem.getShellElements()) {
         for (int j=0; j<elem.numAllIntegrationPoints(); j++) {
            checkEquals ("isValueSet ", field.isValueSet(elem, j), false);
         }
      }
      checkVectorSubElemField (field);      
      fieldChk =
         (VectorSubElemField)ScanTest.testScanAndWrite (field, mech, "%g");
      checkVectorSubElemField (fieldChk);         
      if (!field.equals (fieldChk)) {
         throw new TestException ("field not equal to scanned field");
      }
   }         

   /**
    * Create a FEM model that contains all linear volumetric and shell
    * elements.
    */
   FemModel3d createFem() {
      FemModel3d fem = new FemModel3d ("fem");
      FemFactory.createHexGrid (fem, 1.0, 1.0, 0.5, 2, 2, 1);

      FemModel3d xfem = FemFactory.createTetGrid (null, 1.0, 1.0, 0.5, 2, 2, 1);
      xfem.transformGeometry (new RigidTransform3d (0, 0, -0.5));
      FemFactory.addFem (fem, xfem);

      xfem = FemFactory.createWedgeGrid (null, 1.0, 1.0, 0.5, 2, 2, 1);
      xfem.transformGeometry (new RigidTransform3d (0, 0, -1.0));
      FemFactory.addFem (fem, xfem);      
      
      xfem = FemFactory.createPyramidGrid (null, 1.0, 1.0, 0.5, 2, 2, 1);
      xfem.transformGeometry (new RigidTransform3d (0, 0, -1.5));
      FemFactory.addFem (fem, xfem);      

      xfem = FemFactory.createShellTriGrid (null, 1.0, 1.0, 2, 2, 0.01, false);
      xfem.transformGeometry (new RigidTransform3d (0, 0, 0.5));
      FemFactory.addFem (fem, xfem);      

      xfem = FemFactory.createShellQuadGrid (null, 1.0, 1.0, 2, 2, 0.01, false);
      xfem.transformGeometry (new RigidTransform3d (0, 0, -2.0));
      FemFactory.addFem (fem, xfem);      
      return fem;
   }

   public void testWithChangingFem() {
      FemModel3d fem = new FemModel3d ("fem");
      FemFactory.createHexGrid (fem, 0.5, 0.5, 0.5, 1, 1, 1);

      ScalarNodalField snfield = new ScalarNodalField (fem);
      VectorNodalField<Vector3d> vnfield =
         new VectorNodalField (Vector3d.class, fem);
      ScalarElementField sefield = new ScalarElementField (fem);
      VectorElementField<Vector3d> vefield =
         new VectorElementField (Vector3d.class, fem);
      ScalarSubElemField ssfield = new ScalarSubElemField (fem);
      VectorSubElemField<Vector3d> vsfield =
         new VectorSubElemField (Vector3d.class, fem);

      // assign field values for original mesh
      for (int i=0; i<fem.numNodes(); i++) {
         FemNode3d node = fem.getNode(i);
         snfield.setValue (node, i);
         vnfield.setValue (node, new Vector3d(i,i,i));
      }
      for (int i=0; i<fem.numElements(); i++) {
         FemElement3d elem = fem.getElement(i);
         sefield.setValue (elem, i);
         vefield.setValue (elem, new Vector3d(i,i,i));
         int nipnts = elem.numAllIntegrationPoints();
         for (int j=0; j<nipnts; j++) {
            double val = i*10 + j;
            ssfield.setValue (elem, j, val);
            vsfield.setValue (elem, j, new Vector3d(val,val,val));
         }
      }

      int oldNumNodes = fem.numNodes();
      int oldNumElems = fem.numElements();

      // add nodea and elems to the FEM
      FemModel3d xfem =
         FemFactory.createHexGrid (null, 1.0, 1.0, 1.0, 1, 1, 1);
      FemFactory.addFem (fem, xfem);
      xfem = FemFactory.createShellTriGrid (null, 1.0, 1.0, 2, 2, 0.01, false);
      FemFactory.addFem (fem, xfem);      


      // check values for the enlarged FEM, assigning values for new
      // nodes/elems
      for (int i=0; i<fem.numNodes(); i++) {
         FemNode3d node = fem.getNode(i);
         double schk = i;
         Vector3d vchk = new Vector3d (i, i, i);
         if (i >= oldNumNodes) {
            checkEquals ("valueIsSet ", snfield.isValueSet(node), false);
            checkEquals ("valueIsSet ", vnfield.isValueSet(node), false);
            snfield.setValue (node, schk);
            vnfield.setValue (node, vchk);
         }
         checkEquals ("valueIsSet ", snfield.isValueSet(node), true);
         checkEquals ("valueIsSet ", vnfield.isValueSet(node), true);
         checkEquals ("value at vertex "+i, snfield.getValue(node), schk);
         checkEquals ("value at vertex "+i, vnfield.getValue(node), vchk);
      }

      for (int i=0; i<fem.numElements(); i++) {
         FemElement3d elem = fem.getElement(i);
         double schk = i;
         Vector3d vchk = new Vector3d (i, i, i);
         int nipnts = elem.numAllIntegrationPoints();
         if (i >= oldNumElems) {
            checkEquals ("valueIsSet ", sefield.isValueSet(elem), false);
            checkEquals ("valueIsSet ", vefield.isValueSet(elem), false);
            sefield.setValue (elem, schk);
            vefield.setValue (elem, vchk);
            for (int j=0; j<nipnts; j++) {
               double val = i*10 + j;
               checkEquals ("valueIsSet ", ssfield.isValueSet(elem, j), false);
               checkEquals ("valueIsSet ", vsfield.isValueSet(elem, j), false);
               ssfield.setValue (elem, j, val);
               vsfield.setValue (elem, j, new Vector3d(val,val,val));
            }
         }
         checkEquals ("valueIsSet ", sefield.isValueSet(elem), true);
         checkEquals ("valueIsSet ", vefield.isValueSet(elem), true);
         checkEquals ("value at elem "+i, sefield.getValue(elem), schk);
         checkEquals ("value at elem "+i, vefield.getValue(elem), vchk);
         for (int j=0; j<nipnts; j++) {
            double val = i*10 + j;
            checkEquals ("valueIsSet ", ssfield.isValueSet(elem, j), true);
            checkEquals ("valueIsSet ", vsfield.isValueSet(elem, j), true);
            checkEquals (
               "value at subelem "+i+","+j, ssfield.getValue (elem, j), val);
            checkEquals (
               "value at subelem "+i+","+j, vsfield.getValue (elem, j),
               new Vector3d (val,val,val));
         }
      }

      for (int i=0; i<fem.numShellElements(); i++) {
         ShellElement3d elem = fem.getShellElement(i);
         double schk = i;
         Vector3d vchk = new Vector3d (i, i, i);
         int nipnts = elem.numAllIntegrationPoints();
         checkEquals ("valueIsSet ", sefield.isValueSet(elem), false);
         checkEquals ("valueIsSet ", vefield.isValueSet(elem), false);
         sefield.setValue (elem, schk);
         vefield.setValue (elem, vchk);
         checkEquals ("valueIsSet ", sefield.isValueSet(elem), true);
         checkEquals ("valueIsSet ", vefield.isValueSet(elem), true);
         checkEquals ("value at shell elem "+i, sefield.getValue(elem), schk);
         checkEquals ("value at shell elem "+i, vefield.getValue(elem), vchk);
         for (int j=0; j<nipnts; j++) {
            schk = i*10 + j;
            vchk = new Vector3d (schk, schk, schk);
            checkEquals ("valueIsSet ", ssfield.isValueSet(elem, j), false);
            checkEquals ("valueIsSet ", vsfield.isValueSet(elem, j), false);
            ssfield.setValue (elem, j, schk);
            vsfield.setValue (elem, j, vchk);
            checkEquals ("valueIsSet ", ssfield.isValueSet(elem, j), true);
            checkEquals ("valueIsSet ", vsfield.isValueSet(elem, j), true);
            checkEquals (
               "value at subelem "+i+","+j, ssfield.getValue (elem, j), schk);
            checkEquals (
               "value at subelem "+i+","+j, vsfield.getValue (elem, j), vchk);
         }
      }
   }

   public void testWithFixedFem (FemModel3d fem) {
      MechModel mech = new MechModel();
      mech.addModel (fem);

      // === scalar nodal field ===

      testScalarNodalField (mech, new ScalarNodalField ("snfield", fem));

      // === vector nodal field ===

      testVectorNodalField (
         mech, new VectorNodalField<Vector3d> ("vnfield", Vector3d.class, fem));
      testVectorNodalField (
         mech, new VectorNodalField<Matrix3d> ("vnfield", Matrix3d.class, fem));
      testVectorNodalField (
         mech, new VectorNdNodalField ("vnfield", 5, fem));
      testVectorNodalField (
         mech, new MatrixNdNodalField ("vnfield", 2, 3, fem));

      // === scalar element field ===
      
      testScalarElementField (mech, new ScalarElementField ("sefield", fem));

      // === vector element field ===

      testVectorElementField (
         mech, new VectorElementField<Vector3d> ("vefield", Vector3d.class, fem));
      testVectorElementField (
         mech, new VectorElementField<Matrix3d> ("vefield", Matrix3d.class, fem));
      testVectorElementField (
         mech, new VectorNdElementField ("vefield", 5, fem));
      testVectorElementField (
         mech, new MatrixNdElementField ("vefield", 2, 3, fem));

      // === scalar sub element field ===
      
      testScalarSubElemField (mech, new ScalarSubElemField ("subfield", fem));

      testVectorSubElemField (
         mech, new VectorSubElemField<Vector3d> ("vefield", Vector3d.class, fem));
      testVectorSubElemField (
         mech, new VectorSubElemField<Matrix3d> ("vefield", Matrix3d.class, fem));
      testVectorSubElemField (
         mech, new VectorNdSubElemField ("vefield", 5, fem));
      testVectorSubElemField (
         mech, new MatrixNdSubElemField ("vefield", 2, 3, fem));

   }

   public void test() {
      FemModel3d fem = createFem();
      testWithFixedFem (fem);
      fem.setOneBasedNodeElementNumbering (true);
      testWithFixedFem (fem);
      testWithChangingFem();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      FemFieldTest tester = new FemFieldTest();
      tester.runtest();
   }

}
