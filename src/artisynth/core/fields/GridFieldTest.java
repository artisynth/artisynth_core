package artisynth.core.fields;

import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.modelbase.ScanTest;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;

public class GridFieldTest extends FieldTestBase {

   private double getCenterCellTestValue (
      InterpolatingGridBase grid, int i, int j, int k) {
      double value = 0;
      value += grid.xyzIndicesToVertex (new Vector3i(i,j,k));
      value += grid.xyzIndicesToVertex (new Vector3i(i,j,k+1));
      value += grid.xyzIndicesToVertex (new Vector3i(i,j+1,k));
      value += grid.xyzIndicesToVertex (new Vector3i(i,j+1,k+1));
      
      value += grid.xyzIndicesToVertex (new Vector3i(i+1,j,k));
      value += grid.xyzIndicesToVertex (new Vector3i(i+1,j,k+1));
      value += grid.xyzIndicesToVertex (new Vector3i(i+1,j+1,k));
      value += grid.xyzIndicesToVertex (new Vector3i(i+1,j+1,k+1));      
      return value/8;
   }

   double getCheckTol (GridFieldBase field) {
      if (field.getUseLocalValuesForField() &&
          field.getGrid().getCenterAndOrientation().equals (
             RigidTransform3d.IDENTITY)) {
         return 0;
      }
      else {
         return 1e-10;
      }
   }

   public void checkScalarGridField (ScalarGridField field) {
      ScalarGrid grid = field.getGrid();
      double tol = getCheckTol (field);
      for (int i=0; i<field.numVertices(); i++) {
         double valueChk = i;
         checkEquals (
            "value at vertex "+i, field.getVertexValue (i), valueChk);
         checkEquals (
            "value at vertex pos "+i,
            field.getValue(field.getVertexPosition(i)), valueChk, tol);
      }
      Vector3i res = grid.getResolution();
      Vector3d widths = grid.getWidths();
      for (int i=0; i<res.x; i++) {
         for (int j=0; j<res.y; j++) {
            for (int k=0; k<res.z; k++) {
               double valueChk = getCenterCellTestValue (grid, i, j, k);
               Vector3d pos0 = field.getVertexPosition (i, j, k);
               Vector3d pos1 = field.getVertexPosition (i+1,j+1,k+1);
               Point3d cent = new Point3d();
               cent.add (pos0, pos1);
               cent.scale (0.5);
               checkEquals (
                     "value at centroid "+i,
                     field.getValue(cent), valueChk, tol);
            }
         }
      }
   }

   public <T extends VectorObject<T>> void checkVectorGridField (
      VectorGridField<T> field) {
      VectorGrid<T> grid = field.getGrid();      
      double tol = getCheckTol (field);
      for (int i=0; i<field.numVertices(); i++) {
         T valueChk = grid.createTypeInstance();
         setValue (valueChk, i);
         T value = field.getVertexValue (i);
         if (!value.epsilonEquals (valueChk, 0)) {
            throw new TestException (
               "value at vertex "+i+" = " + value +", expected "+valueChk);
         }
         value = field.getValue(field.getVertexPosition(i));
         if (!value.epsilonEquals (valueChk, tol)) {
            throw new TestException (
               "value at vertex pos "+i+" = " + value +", expected "+valueChk);
         }
      }

      Vector3i res = grid.getResolution();
      Vector3d widths = grid.getWidths();
      T valueChk = grid.createTypeInstance();
      for (int i=0; i<res.x; i++) {
         for (int j=0; j<res.y; j++) {
            for (int k=0; k<res.z; k++) {
               double val = getCenterCellTestValue (grid, i, j, k);
               setValue (valueChk, val);
               Vector3d pos0 = field.getVertexPosition (i, j, k);
               Vector3d pos1 = field.getVertexPosition (i+1,j+1,k+1);
               Point3d cent = new Point3d();
               cent.add (pos0, pos1);
               cent.scale (0.5);
               T value = field.getValue(cent);
               if (!value.epsilonEquals (valueChk, tol)) {
                  throw new TestException (
                     "value at centrid "+i+" = " + value +", expected "+valueChk);
               }
            }
         }
      }
   }

   public <T extends VectorObject<T>> void testScalarGridField (
      MechModel mech, String name, ScalarGrid sgrid) {

      ScalarGridField sgfield = new ScalarGridField (name, sgrid);
      for (int i=0; i<sgfield.numVertices(); i++) {
         sgfield.setVertexValue (i, i);
      }
      mech.addField (sgfield);

      checkScalarGridField (sgfield);      
      
      ScalarGridField sgfieldChk =
         (ScalarGridField)ScanTest.testScanAndWrite (sgfield, mech, "%g");
      checkScalarGridField (sgfieldChk);      

      RigidTransform3d TLW = new RigidTransform3d();
      TLW.setRandom();
      sgrid.setLocalToWorld (TLW);
      sgfield.setUseLocalValuesForField (false);

      checkScalarGridField (sgfield); 
      
      sgfieldChk =
         (ScalarGridField)ScanTest.testScanAndWrite (sgfield, mech, "%g");
      checkScalarGridField (sgfieldChk);      
   }

   public <T extends VectorObject<T>> void testVectorGridField (
      MechModel mech, String name, VectorGrid<T> vgrid) {

      VectorGridField<T> vgfield =
         new VectorGridField<T> (name, vgrid);
      for (int i=0; i<vgfield.numVertices(); i++) {
         T value = vgrid.createTypeInstance();
         setValue (value, i);
         vgfield.setVertexValue (i, value);
      }
      mech.addField (vgfield);

      checkVectorGridField (vgfield);      
      VectorGridField<Vector3d> vgfieldChk =
         (VectorGridField<Vector3d>)ScanTest.testScanAndWrite (
            vgfield, mech, "%g");
      checkVectorGridField (vgfieldChk);      

      RigidTransform3d TLW = new RigidTransform3d();
      TLW.setRandom();
      vgrid.setLocalToWorld (TLW);
      vgfield.setUseLocalValuesForField (false);

      checkVectorGridField (vgfield);      
      vgfieldChk = (VectorGridField<Vector3d>)ScanTest.testScanAndWrite (
         vgfield, mech, "%g");
      checkVectorGridField (vgfieldChk);      
   }

   public void test() {
      PolygonalMesh mesh = MeshFactory.createBox (1.0, 2.0, 3.0);
      MechModel mech = new MechModel();

      // === scalar grid field ===

      Vector3i res = new Vector3i (2, 3, 4);
      Vector3d widths = new Vector3d (2.0, 2.0, 3.0);

      RigidTransform3d TCL = new RigidTransform3d();
      TCL.setRandom();

      testScalarGridField (
         mech, "sfield1", new ScalarGrid (widths, res));
      testScalarGridField (
         mech, "sfield2", new ScalarGrid (widths, res, TCL));

      // === vector grid field ===

      testVectorGridField (
         mech, "vfield1",
         new VectorGrid<Vector3d> (Vector3d.class, widths, res));
      testVectorGridField (
         mech, "vfield2",
         new VectorGrid<Vector3d> (Vector3d.class, widths, res, TCL));
      testVectorGridField (
         mech, "vfield3",
         new VectorGrid<Matrix3d> (Matrix3d.class, widths, res));
      testVectorGridField (
         mech, "vfield4",
         new VectorGrid<Matrix3d> (Matrix3d.class, widths, res, TCL));
      testVectorGridField (
         mech, "vfield5",
         new VectorNdGrid (5, widths, res));
      testVectorGridField (
         mech, "vfield6",
         new VectorNdGrid (5, widths, res, TCL));
      testVectorGridField (
         mech, "vfield7",
         new MatrixNdGrid (2, 2, widths, res));
      testVectorGridField (
         mech, "vfield8",
         new MatrixNdGrid (2, 2, widths, res, TCL));
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      GridFieldTest tester = new GridFieldTest();
      tester.runtest();
   }

}
