package artisynth.core.inverse;

import java.util.ArrayList;

import artisynth.core.modelbase.*;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;
import maspack.util.*;
import maspack.properties.*;

/**
 * Constraint term to bound excitation values, using bounds specified by the
 * ExciterComps stored in the controller.
 *
 * @author Teun
 */
public class BoundsTerm extends QPConstraintTermBase {

   public static PropertyList myProps =
      new PropertyList (BoundsTerm.class, QPTermBase.class);

   static {
      // remove weight property since it is not used by this term
      myProps.remove ("weight");
   }

   public BoundsTerm() {
      super();
   }

   public BoundsTerm (String name) {
      setName (name);
   }
  
   /**
    * {@inheritDoc}
    */
   @Override
   public int numConstraints (int qpsize) {
      //myRowSize specifies the number of rows added manually
      //to this blocks of mySize are added for each bound
      int result = 0;
      TrackingController controller = getController();
      if (controller != null) {
         for (ExciterComp ec : controller.myExciters) {
            DoubleInterval bounds = ec.getExcitationBounds();
            if (bounds.getLowerBound() > Double.NEGATIVE_INFINITY) {
               result++;
            }
            if (bounds.getUpperBound() < Double.POSITIVE_INFINITY) {
               result++;
            }
         }
      }
      return result;
   }
  
   /**
    * {@inheritDoc}
    */
   @Override
   public int getTerm (MatrixNd A, VectorNd b, int rowoff, double t0, double t1) {

      TrackingController controller = getController();
      if (controller == null) {
         return rowoff;
      }
      
      int numex = controller.myExciters.size();

      // get lower bounds first, then upper bounds, just to ensure numeric
      // repeatability with legacy code
      for (int i=0; i<numex; i++) {
         DoubleInterval bounds =
            controller.myExciters.get(i).getExcitationBounds();
         double lower = bounds.getLowerBound();
         if (lower > Double.NEGATIVE_INFINITY) {
            for (int j=0; j<numex; j++) {
               if (j == i) {
                  A.set (rowoff, j, 1.0); // x >= lb
               }
               else {
                  A.set (rowoff, j, 0.0);
               }
            }
            b.set (rowoff++, lower);
         }
      }
      for (int i=0; i<numex; i++) {
         DoubleInterval bounds =
            controller.myExciters.get(i).getExcitationBounds();
         double upper = bounds.getUpperBound();
         if (upper < Double.POSITIVE_INFINITY) {
            for (int j=0; j<numex; j++) {
               if (j == i) {
                  A.set (rowoff, j, -1.0); // -x >= -ub
               }
               else {
                  A.set (rowoff, j, 0.0);
               }
            }
            b.set (rowoff++, -upper);
         }
      }

      if (controller.getComputeIncrementally()) {
         for (int i=0; i<numex; i++) {
            double ex = controller.getExcitation(i);
            b.add (i, -ex);
            b.add (i+numex, ex);
         }
      }
      return rowoff;

   }
}
