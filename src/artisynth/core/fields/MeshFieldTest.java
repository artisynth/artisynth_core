package artisynth.core.fields;

import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.ScanTest;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;

public class MeshFieldTest extends FieldTestBase {

   int defaultSpacing = 4; // spacing for default values  

   public void checkScalarVertexField (ScalarVertexField field) {
      PolygonalMesh mesh = (PolygonalMesh)field.getMesh();
      for (int i=0; i<mesh.numVertices(); i++) {
         Vertex3d vtx = mesh.getVertex(i);
         double valueChk;
         if (field.isValueSet (vtx)) {
            valueChk = i;
         }
         else {
            valueChk = field.getDefaultValue();
         }
         checkEquals (
            "value at vertex "+i, field.getValue (i), valueChk);
         checkEquals (
            "value at vertex pos "+i,
            field.getValue(vtx.getPosition()), valueChk, 1e-10);
      }
      // check bad index exception 
      checkForIllegalArgumentException (
         () -> field.getValue (mesh.numVertices()));
      Point3d cent = new Point3d();
      for (int i=0; i<mesh.numFaces(); i++) {
         Face face = mesh.getFace(i);
         int[] idxs = face.getVertexIndices();
         double valueChk = 0;
         for (int k=0; k<3; k++) {
            valueChk += field.getValue (idxs[k]);
         }
         valueChk /= 3;
         face.computeCentroid (cent);
         checkEquals (
            "value at centroid "+i, field.getValue(cent), valueChk, 1e-10);
      }
   }

   public void checkScalarFaceField (ScalarFaceField field) {
      PolygonalMesh mesh = (PolygonalMesh)field.getMesh();
      Point3d cent = new Point3d();
      for (int i=0; i<mesh.numFaces(); i++) {
         Face face = mesh.getFace(i);
         double valueChk;
         if (field.isValueSet (face)) {
            valueChk = i;
         }
         else {
            valueChk = field.getDefaultValue();
         }
         face.computeCentroid (cent);
         checkEquals (
            "value at face "+i, field.getValue (i), valueChk);
         checkEquals (
            "value at face centroid "+i,
            field.getValue(cent), valueChk, 1e-10);
      }
      // check bad index exception 
      checkForIllegalArgumentException (
         () -> field.getValue (mesh.numFaces()));
   }

   public <T extends VectorObject<T>> void checkVectorVertexField (
      VectorVertexField<T> field) {
      PolygonalMesh mesh = (PolygonalMesh)field.getMesh();
      for (int i=0; i<mesh.numVertices(); i++) {
         Vertex3d vtx = mesh.getVertex(i);
         T valueChk;
         if (field.isValueSet (vtx)) {
            valueChk = field.createTypeInstance();
            setValue (valueChk, i);
         }
         else {
            valueChk = field.getDefaultValue();
         }
         T value = field.getValue(i);
         if (!value.epsilonEquals (valueChk, 0)) {
            throw new TestException (
               "value at vertex "+i+" = " + value + ", expecting "+valueChk);
         }
         value = field.getValue(vtx.getPosition());
         if (!value.epsilonEquals (valueChk, 1e-10)) {
            throw new TestException (
               "value at vertex pos "+i+" = " + value + ", expecting "+valueChk);
         }
      }
      // check bad index exception 
      checkForIllegalArgumentException (
         () -> field.getValue (mesh.numVertices()));
      Point3d cent = new Point3d();
      for (int i=0; i<mesh.numFaces(); i++) {
         Face face = mesh.getFace(i);
         int[] idxs = face.getVertexIndices();
         T valueChk = field.createTypeInstance();
         for (int k=0; k<3; k++) {
            valueChk.scaledAddObj (1.0, field.getValue (idxs[k]));
         }
         valueChk.scaleObj (1/3.0);
         face.computeCentroid (cent);
         T value = field.getValue(cent);
         if (!value.epsilonEquals (valueChk, 1e-10)) {
            throw new TestException (
               "value at centroid "+i+" = " + value + ", expecting "+valueChk);
         }
      }
   }

   public <T extends VectorObject<T>> void checkVectorFaceField (
      VectorFaceField<T> field) {
      PolygonalMesh mesh = (PolygonalMesh)field.getMesh();
      Point3d cent = new Point3d();
      for (int i=0; i<mesh.numFaces(); i++) {
         Face face = mesh.getFace(i);
         face.computeCentroid (cent);
         T valueChk;
         if (field.isValueSet (face)) {
            valueChk = field.createTypeInstance();
            setValue (valueChk, i);
         }
         else {
            valueChk = field.getDefaultValue();
         }
         T value = field.getValue(i);
         if (!value.epsilonEquals (valueChk, 0)) {
            throw new TestException (
               "value at face "+i+" = " + value + ", expecting "+valueChk);
         }
         value = field.getValue(cent);
         if (!value.epsilonEquals (valueChk, 1e-10)) {
            throw new TestException (
               "value at face centroid "+i+" = "+value+", expecting "+valueChk);
         }
      }
      // check bad index exception 
      checkForIllegalArgumentException (
         () -> field.getValue (mesh.numFaces()));
   }

   public <T extends VectorObject<T>> void testVectorVertexField (
      MechModel mech, VectorVertexField<T> field) {

      PolygonalMesh mesh = (PolygonalMesh)field.getMesh();

      for (int i=0; i<mesh.numVertices(); i++) {
         Vertex3d vtx = mesh.getVertex(i);
         T value = field.createTypeInstance();
         setValue (value, i);
         field.setValue (vtx, value);
         checkEquals ("isValueSet", field.isValueSet(vtx), true);
         if (i%defaultSpacing == 0) {
            field.clearValue (vtx);
            checkEquals ("isValueSet", field.isValueSet(vtx), false);
         }
      }
      T value = field.createTypeInstance();
      setValue (value, 100);
      field.setDefaultValue (value);
      checkVectorVertexField (field);      

      // check that we catch illegal vertices
      checkForIllegalArgumentException (
         () -> field.getValue (new Vertex3d()));         
      checkForIllegalArgumentException (
         () -> field.setValue (new Vertex3d(), field.createTypeInstance()));
      checkForIllegalArgumentException (
         () -> field.isValueSet (new Vertex3d()));         
      checkForIllegalArgumentException (
         () -> field.clearValue (new Vertex3d()));         

      VectorVertexField<Vector3d> fieldChk =
         (VectorVertexField<Vector3d>)ScanTest.testScanAndWrite (
            field, mech, "%g");
      checkVectorVertexField (fieldChk);      

      // clear values and check again
      field.clearAllValues();
      for (Vertex3d vtx : mesh.getVertices()) {
         checkEquals ("valueIsSet ", field.isValueSet(vtx), false);
      }
      checkVectorVertexField (field);      
      fieldChk = (VectorVertexField<Vector3d>)ScanTest.testScanAndWrite (
         field, mech, "%g");
      checkVectorVertexField (fieldChk);      
   }

   public <T extends VectorObject<T>> void testVectorFaceField (
      MechModel mech, VectorFaceField<T> field) {

      PolygonalMesh mesh = (PolygonalMesh)field.getMesh();

      for (int i=0; i<mesh.numFaces(); i++) {
         Face face = mesh.getFace(i);         
         T value = field.createTypeInstance();
         setValue (value, i);
         field.setValue (face, value);
         checkEquals ("isValueSet", field.isValueSet(face), true);
         if (i%defaultSpacing == 0) {         
            field.clearValue (face);
            checkEquals ("isValueSet", field.isValueSet(face), false);
         }
      }
      T value = field.createTypeInstance();
      setValue (value, 100);
      field.setDefaultValue (value);
      checkVectorFaceField (field);      

      // check that we catch illegal faces
      checkForIllegalArgumentException (
         () -> field.getValue (new Face(0)));         
      checkForIllegalArgumentException (
         () -> field.setValue (new Face(0), field.createTypeInstance()));         
      checkForIllegalArgumentException (
         () -> field.isValueSet (new Face(0)));         
      checkForIllegalArgumentException (
         () -> field.clearValue (new Face(0)));         
         
      VectorFaceField<Vector3d> fieldChk =
         (VectorFaceField<Vector3d>)ScanTest.testScanAndWrite (
            field, mech, "%g");
      checkVectorFaceField (fieldChk);      

      // clear values and check again
      field.clearAllValues();
      for (Face face : mesh.getFaces()) {
         checkEquals ("valueIsSet ", field.isValueSet(face), false);
      }
      fieldChk = (VectorFaceField)ScanTest.testScanAndWrite (
         field, mech, "%g");
      checkVectorFaceField (fieldChk);      
   }

   public void testWithChangingMesh() {
      PolygonalMesh mesh = MeshFactory.createBox (0.5, 0.5, 0.5);
      FixedMeshBody meshBody = new FixedMeshBody ("mesh", mesh);

      ScalarVertexField svfield =
         new ScalarVertexField ("svfield", meshBody);
      ScalarFaceField sffield =
         new ScalarFaceField ("sffield", meshBody);
      VectorVertexField<Vector3d> vvfield =
         new VectorVertexField ("vvfield", Vector3d.class, meshBody);
      VectorFaceField<Vector3d> vffield =
         new VectorFaceField ("vffield", Vector3d.class, meshBody);

      // assign field values for original mesh
      for (int i=0; i<mesh.numVertices(); i++) {
         Vertex3d vtx = mesh.getVertex(i);
         svfield.setValue (vtx, i);
         vvfield.setValue (vtx, new Vector3d (i, i, i));
      }
      for (int i=0; i<mesh.numFaces(); i++) {
         Face face = mesh.getFace(i);
         sffield.setValue (face, i);
         vffield.setValue (face, new Vector3d (i, i, i));
      }

      int oldNumVerts = mesh.numVertices();
      int oldNumFaces = mesh.numFaces();

      // now grow the mesh
      PolygonalMesh meshx = MeshFactory.createBox (1.0, 1.0, 1.0);
      mesh.addMesh (meshx);

      // check values for new mesh, and assign values for now verts/faces
      for (int i=0; i<mesh.numVertices(); i++) {
         Vertex3d vtx = mesh.getVertex(i);
         double schk = i;
         Vector3d vchk = new Vector3d (i, i, i);
         if (i >= oldNumVerts) {
            checkEquals ("valueIsSet ", svfield.isValueSet(vtx), false);
            checkEquals ("valueIsSet ", vvfield.isValueSet(vtx), false);
            svfield.setValue (vtx, schk);
            vvfield.setValue (vtx, vchk);
         }
         checkEquals ("valueIsSet ", svfield.isValueSet(vtx), true);
         checkEquals ("valueIsSet ", vvfield.isValueSet(vtx), true);
         checkEquals ("value at vertex "+i, svfield.getValue(vtx), schk);
         checkEquals ("value at vertex "+i, vvfield.getValue(vtx), vchk);
      }

      for (int i=0; i<mesh.numFaces(); i++) {
         Face face = mesh.getFace(i);
         double schk = i;
         Vector3d vchk = new Vector3d (i, i, i);
         if (i >= oldNumFaces) {
            checkEquals ("valueIsSet ", sffield.isValueSet(face), false);
            checkEquals ("valueIsSet ", vffield.isValueSet(face), false);
            sffield.setValue (face, schk);
            vffield.setValue (face, vchk);
         }
         checkEquals ("valueIsSet ", sffield.isValueSet(face), true);
         checkEquals ("valueIsSet ", vffield.isValueSet(face), true);
         checkEquals ("value at vertex "+i, sffield.getValue(face), schk);
         checkEquals ("value at vertex "+i, vffield.getValue(face), vchk);
      }

   }

   public void test() {
      testWithFixedMesh();
      testWithChangingMesh();
   }

   public void testWithFixedMesh() {
      PolygonalMesh mesh = MeshFactory.createBox (1.0, 2.0, 3.0);
      MechModel mech = new MechModel();
      FixedMeshBody meshBody = new FixedMeshBody ("mesh", mesh);
      mech.addMeshBody (meshBody);

      // === scalar vertex field ===

      ScalarVertexField svfield =
         new ScalarVertexField ("svfield", meshBody);
      for (int i=0; i<mesh.numVertices(); i++) {
         Vertex3d vtx = mesh.getVertex(i);
         svfield.setValue (vtx, i);
         checkEquals ("isValueSet", svfield.isValueSet(vtx), true);
         if (i%defaultSpacing == 0) {
            svfield.clearValue (vtx);
            checkEquals ("isValueSet", svfield.isValueSet(vtx), false);
         }
      }
      svfield.setDefaultValue (100);
      checkScalarVertexField (svfield);      

      // check that we catch illegal vertices
      checkForIllegalArgumentException (
         () -> svfield.getValue (new Vertex3d()));         
      checkForIllegalArgumentException (
         () -> svfield.setValue (new Vertex3d(), 0));         
      checkForIllegalArgumentException (
         () -> svfield.isValueSet (new Vertex3d()));         
      checkForIllegalArgumentException (
         () -> svfield.clearValue (new Vertex3d()));         

      ScalarVertexField svfieldChk =
         (ScalarVertexField)ScanTest.testScanAndWrite (svfield, mech, "%g");
      checkScalarVertexField (svfieldChk);      

      // clear values and check again
      svfield.clearAllValues();
      for (Vertex3d vtx : mesh.getVertices()) {
         checkEquals ("valueIsSet ", svfield.isValueSet(vtx), false);
      }
      checkScalarVertexField (svfield);      
      svfieldChk =
         (ScalarVertexField)ScanTest.testScanAndWrite (svfield, mech, "%g");
      checkScalarVertexField (svfieldChk);      

      // === vector vertex field ===

      testVectorVertexField (
         mech, new VectorVertexField<Vector3d> (
            "vvfield", Vector3d.class, meshBody));
      testVectorVertexField (
         mech, new VectorVertexField<Matrix3d> (
            "vvfield", Matrix3d.class, meshBody));
      testVectorVertexField (
         mech, new VectorNdVertexField (
            "vvfield", 5, meshBody));
      testVectorVertexField (
         mech, new MatrixNdVertexField (
            "vvfield", 2, 3, meshBody));

      // === scalar face field ===

      ScalarFaceField sffield =
         new ScalarFaceField ("sffield", meshBody);
      for (int i=0; i<mesh.numFaces(); i++) {
         Face face = mesh.getFace(i);
         sffield.setValue (face, i);
         checkEquals ("isValueSet", sffield.isValueSet(face), true);
         if (i%defaultSpacing == 0) {         
            sffield.clearValue (face);
            checkEquals ("isValueSet", sffield.isValueSet(face), false);
         }
      }
      sffield.setDefaultValue (100);
      checkScalarFaceField (sffield);      
      
      // check that we catch illegal faces
      checkForIllegalArgumentException (
         () -> sffield.getValue (new Face(0)));         
      checkForIllegalArgumentException (
         () -> sffield.setValue (new Face(0), 0));         
      checkForIllegalArgumentException (
         () -> sffield.isValueSet (new Face(0)));         
      checkForIllegalArgumentException (
         () -> sffield.clearValue (new Face(0)));         

      ScalarFaceField sffieldChk =
         (ScalarFaceField)ScanTest.testScanAndWrite (sffield, mech, "%g");
      checkScalarFaceField (sffieldChk);      

      // clear values and check again
      sffield.clearAllValues();
      for (Face face : mesh.getFaces()) {
         checkEquals ("valueIsSet ", sffield.isValueSet(face), false);
      }
      sffieldChk = (ScalarFaceField)ScanTest.testScanAndWrite (
         sffield, mech, "%g");
      checkScalarFaceField (sffieldChk);      

      // vector face field
      testVectorFaceField (
         mech, new VectorFaceField<Vector3d> (
            "vffield", Vector3d.class, meshBody));
      testVectorFaceField (
         mech, new VectorFaceField<Matrix3d> (
            "vffield", Matrix3d.class, meshBody));
      testVectorFaceField (
         mech, new VectorNdFaceField (
            "vffield", 5, meshBody));
      testVectorFaceField (
         mech, new MatrixNdFaceField (
            "vffield", 2, 3, meshBody));

   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      MeshFieldTest tester = new MeshFieldTest();
      tester.runtest();
   }

}
