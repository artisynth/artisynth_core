package artisynth.core.inverse;

import java.io.*;
import java.util.*;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.util.*;
import maspack.properties.*;
import maspack.util.*;

/**
 * Wrapper component used to store the excitation components used by the
 * inverse solver. Contains the upper and lower excitation bounds.
 */
public class ExciterComp extends WeightedReferenceComp<ExcitationComponent> {

   private static DoubleInterval DEFAULT_EXCITATION_BOUNDS =
      new DoubleInterval ("[0,1])");
   private DoubleInterval myExcitationBounds =
      new DoubleInterval(DEFAULT_EXCITATION_BOUNDS);
   private PropertyMode myExcitationBoundsMode = PropertyMode.Inherited;

   public static PropertyList myProps = new PropertyList (
      ExciterComp.class, WeightedReferenceComp.class);

   static {
      myProps.addReadOnly (
         "excitation", "current excitation value for the excitation component");
      myProps.addInheritable (
         "excitationBounds", "bounds for the computed excitations",
         DEFAULT_EXCITATION_BOUNDS);
   }

   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public DoubleInterval getExcitationBounds () {
      return myExcitationBounds;
   }

   public void setExcitationBounds (DoubleInterval bounds) {
      myExcitationBounds.set (bounds);
      myExcitationBoundsMode = PropertyUtils.propagateValue (
         this, "excitationBounds", bounds, myExcitationBoundsMode);
   }

   public PropertyMode getExcitationBoundsMode() {
      return myExcitationBoundsMode;
   }

   public void setExcitationBoundsMode (PropertyMode mode) {
      myExcitationBoundsMode =
         PropertyUtils.setModeAndUpdate (
            this, "excitationBounds", myExcitationBoundsMode, mode);
   }

   public ExciterComp () {
      super();
   }

   public ExciterComp (ExcitationComponent ex, double weight) {
      super (ex, weight);
   }

   public ExcitationComponent getExciter() {
      return getReference();
   }

   public void setExcitation (double e) {
      getReference().setExcitation (e);
   }

   public double getExcitation () {
      return getReference().getExcitation();
   }
}
