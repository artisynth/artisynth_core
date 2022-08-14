package artisynth.core.fields;

import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.modelbase.ScanTest;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;

public class FieldTestBase extends UnitTest {

   void setValue (VectorObject vobj, double val) {
      if (vobj instanceof Vector) {
         Vector vec = (Vector)vobj;
         for (int i=0; i<vec.size(); i++) {
            vec.set (i, val);
         }
      }
      else if (vobj instanceof Matrix) {
         Matrix mat = (Matrix)vobj;
         MatrixNd M = new MatrixNd (mat.rowSize(), mat.colSize());
         for (int i=0; i<mat.rowSize(); i++) {
            for (int j=0; j<mat.colSize(); j++) {
               M.set (i, j, val);
            }
         }
         mat.set (M);
      }
      else {
         throw new InternalErrorException (
            "Unimplemented type: " + vobj.getClass());
      }
   }
}
   
