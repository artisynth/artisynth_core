package artisynth.core.femmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;

import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.util.ScanToken;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorObject;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * A vector field, for vectors of type {@link VectorNd}, defined over an FEM
 * model, using values set at the nodes. Values at other points are obtained by
 * nodal interpolation on the elements nearest to those points. Values at nodes
 * for which no explicit value has been set are given by the field's <i>default
 * value</i>. The {@code VectorNd} values must be a of a fixed size as
 * specified in the field's constructor.
 */
public class VectorNdNodalField extends VectorNodalField<VectorNd> {

   protected int myVecSize;

   private void initSize (int vsize) {
      if (vsize <= 0) {
         throw new IllegalArgumentException (
            "specified vector size is " + vsize + ", must be > 0");
      }
      myVecSize = vsize;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected String checkSize (VectorNd value) {
      if (value.size() != myVecSize) {
         return ("size "+value.size()+
                 " incompatible with field size of "+myVecSize);
      }
      else {
         return null;
      }
   } 

   /**
    * {@inheritDoc}
    */
   @Override
   public VectorNd createTypeInstance () {
      return new VectorNd (myVecSize);
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean hasParameterizedType() {
      return false;
   }   

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public VectorNdNodalField () {
      super (VectorNd.class);
   }
   
   /**
    * Constructs a field for a given FEM model, with a default value of 0.
    *
    * @param vecSize size of the field's {@code VectorNd} values
    * @param fem FEM model over which the field is defined
    */
   public VectorNdNodalField (int vecSize, FemModel3d fem) {
      super (VectorNd.class); 
      initSize (vecSize);
      initFemAndDefaultValue (fem, null);
      initValues();
   }

   /**
    * Constructs a field for a given FEM model and default value.
    * 
    * @param vecSize size of the field's {@code VectorNd} values
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for nodes which don't have
    * explicitly set values
    */
   public VectorNdNodalField (
      int vecSize, FemModel3d fem, VectorNd defaultValue) {
      super (VectorNd.class);
      initSize (vecSize);
      initFemAndDefaultValue (fem, defaultValue);
      initValues();
   }

   /**
    * Constructs a named field for a given FEM model, with a default value of 0.
    * 
    * @param name name of the field
    * @param vecSize size of the field's {@code VectorNd} values
    * @param fem FEM model over which the field is defined
    */
   public VectorNdNodalField (String name, int vecSize, FemModel3d fem) {
      this (vecSize, fem);
      setName (name);
   }

   /**
    * Constructs a named field for a given FEM model and default value.
    *
    * @param name name of the field
    * @param vecSize size of the field's {@code VectorNd} values
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for nodes which don't have
    * explicitly set values
    */
   public VectorNdNodalField (
      String name, int vecSize, FemModel3d fem, VectorNd defaultValue) {
      this (vecSize, fem, defaultValue);
      setName (name);
   }

   /**
    * {@inheritDoc}
    */
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.println ("size=" + myVecSize);
      super.writeItems (pw, fmt, ancestor);
   }

   /**
    * {@inheritDoc}
    */
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "size")) {
         initSize (rtok.scanInteger());
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);      
   } 

   protected boolean hasThreeVectorValue() {
      return myVecSize == 3;
   }

   // Converts, if possible, a VectorObject value to a three-vector.
   protected boolean getThreeVectorValue (Vector3d vec, VectorObject vobj) {
      if (myVecSize == 3) {
         double[] vbuf = ((VectorNd)vobj).getBuffer();
         vec.set (vbuf[0], vbuf[1], vbuf[2]);
         return true;
      }
      else {
         return false;
      }
   }


}
