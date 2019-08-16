package artisynth.core.femmodels;

import java.io.*;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import artisynth.core.modelbase.FieldUtils.VectorFieldFunction;

import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.*;

public class VectorNdNodalField extends VectorNodalField<VectorNd> {

   protected int myVecSize;

   private void initSize (int vsize) {
      if (vsize <= 0) {
         throw new IllegalArgumentException (
            "specified vector size is " + vsize + ", must be > 0");
      }
      myVecSize = vsize;
   }

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

   @Override
   protected VectorNd createInstance () {
      return new VectorNd (myVecSize);
   }
   
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
   
   public VectorNdNodalField (int vecSize, FemModel3d fem) {
      super (VectorNd.class); 
      initSize (vecSize);
      initFemAndDefaultValue (fem, null);
      initValues();
   }

   public VectorNdNodalField (
      int vecSize, FemModel3d fem, VectorNd defaultValue) {
      super (VectorNd.class);
      initSize (vecSize);
      initFemAndDefaultValue (fem, defaultValue);
      initValues();
   }

   public VectorNdNodalField (String name, int vecSize, FemModel3d fem) {
      this (vecSize, fem);
      setName (name);
   }

   public VectorNdNodalField (
      String name, int vecSize, FemModel3d fem, VectorNd defaultValue) {
      this (vecSize, fem, defaultValue);
      setName (name);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.println ("size=" + myVecSize);
      super.writeItems (pw, fmt, ancestor);
   }

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
