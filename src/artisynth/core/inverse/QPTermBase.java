package artisynth.core.inverse;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;
import maspack.properties.HasProperties;
import maspack.properties.Property;
import maspack.properties.PropertyList;

public abstract class QPTermBase implements QPTerm, HasProperties {
   protected MatrixNd Q = new MatrixNd(); //Quadratic term
   protected VectorNd P = new VectorNd(); //Proportional term
   
   protected double myWeight;
   protected int mySize;

   public static final double defaultWeight = 1;
   
   public static PropertyList myProps = new PropertyList (QPTermBase.class);

   static {
      myProps.add ("weight * *", "weighting factor for this optimization term", 1);
   }
      
   public QPTermBase() {
      this(defaultWeight);
   }
   
   public QPTermBase(double weight) {
      myWeight = weight;
   }
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public Property getProperty(String pathName) {
      return PropertyList.getProperty(pathName, this);
   }
   
   public void setWeight(double w) {
      myWeight = w;
   }
   
   public double getWeight() {
      return myWeight;
   }
   
   public int getSize() {
      return mySize;
   }

   @Override
   public void setSize (int size) {
      Q.setSize (size, size);
      P.setSize (size);
      mySize = size;
   }
   
   /**
    * Recompute the cost terms
    */
   protected abstract void compute(double t0, double t1);
   
   @Override
   public void getQP (MatrixNd Q, VectorNd P, double t0, double t1) {
      compute(t0,t1);
      Q.add(this.Q);
      P.add(this.P);
   }
   
}
