package artisynth.core.inverse;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Deque;

import artisynth.core.modelbase.*;
import artisynth.core.util.ScanToken;
import maspack.matrix.VectorNd;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.util.ReaderTokenizer;
import maspack.util.NumberFormat;

/**
 * Base implementation for a QPTerm
 */
public abstract class QPTermBase extends ModelComponentBase 
   implements QPTerm {
   
   protected double myWeight;
   protected boolean myInternalP;

   public static final double DEFAULT_WEIGHT = 1;
   
   public static final boolean DEFAULT_ENABLED = true;
   protected boolean enabled = DEFAULT_ENABLED;
   
   public static PropertyList myProps =
      new PropertyList (QPTermBase.class, ModelComponentBase.class);

   static {
      myProps.add (
         "enabled isEnabled setEnabled",
         "enable this term", DEFAULT_ENABLED);
      myProps.add (
         "weight", "weighting factor for this optimization term", 1);
   }
      
   public QPTermBase() {
      this(DEFAULT_WEIGHT);
   }
   
   public QPTermBase(double weight) {
      myWeight = weight;
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Property getProperty(String pathName) {
      return PropertyList.getProperty(pathName, this);
   }
   
   /**
    * Sets the weight for this QPTerm.
    *
    * @param w weight for the term 
    */
   public void setWeight(double w) {
      myWeight = w;
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public double getWeight() {
      return myWeight;
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isEnabled () {
      return enabled;
   }

   /**
    * {@inheritDoc}
    */
   public void setEnabled (boolean enabled) {
      this.enabled = enabled;
   }

   /**
    * Sets a flag indicating that this term is <i>internal</i> to the tracking
    * controller, and should not be directly added or removed by the
    * application using methods such as {@link
    * TrackingController#addConstraintTerm}, {@link
    * TrackingController#removeConstraintTerm}, {@link
    * TrackingController#addCostTerm}, or {@link
    * TrackingController#removeCostTerm}.
    *
    * @param internal if {@code true}, specifies that this term is internal
    */
   public void setInternal (boolean internal) {
      myInternalP = internal;
   }
   
   /**
    * Queries whether this term is <i>internal</i> to the controller,
    * as described for {@link #setInternal}.
    * 
    * @return {@code true} if this term is internal
    */
   public boolean isInternal () {
      return myInternalP;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public TrackingController getController() {
      if (getParent() instanceof TrackingController) {
         return (TrackingController)getParent();
      }
      else {
         return null;
      }
   }
   
   /**
    * @inheritDoc
    */
   @Override
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      if (myInternalP) {
         pw.println ("internal=true");
      }
      super.writeItems (pw, fmt, ancestor);
   }
   
   /**
    * @inheritDoc
    */
   @Override
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "internal")) {
         myInternalP = rtok.scanBoolean();
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   /**
    * Computes the element-wise product of v1 and v2 and places the
    * result in out.
    */
   protected void mulElements (VectorNd out, VectorNd v1, VectorNd v2) {
      assert(v1.size() == v2.size() && v2.size() == out.size());
      
      double [] v1buff = v1.getBuffer();
      double [] v2buff = v2.getBuffer();
      double [] outbuff = out.getBuffer();
      
      for (int i=0; i<v1.size(); i++) {
         outbuff[i] = v1buff[i]*v2buff[i];
      }      
   }

}
