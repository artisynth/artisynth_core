package artisynth.core.fields;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;

import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.mechmodels.*;
import artisynth.core.util.ScanToken;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorObject;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * A vector field, for vectors of type {@link VectorNd}, defined over a
 * triangular polygonal mesh, using values set at the mesh's faces. Values at
 * other points are obtained by finding the faces nearest to those
 * points. Values at faces for which no explicit value has been set are
 * associated with the field's <i>default value</i>. Since values are assumed
 * to be constant over a given face, this field is not continuous. The {@code
 * VectorNd} values must be of a fixed size as specified in the field's
 * constructor.
 */
public class VectorNdFaceField extends VectorFaceField<VectorNd> {

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
   public VectorNdFaceField () {
      super (VectorNd.class);
   }
   
   /**
    * Constructs a field for a given mesh, with a default value of 0.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    *
    * @param vecSize size of the field's {@code VectorNd} values
    * @param mcomp component containing the mesh associated with the field
    */   
   public VectorNdFaceField (int vecSize, MeshComponent mcomp) {
      super (VectorNd.class, mcomp);
      initSize (vecSize);
      initValues();
   }

   /**
    * Constructs a field for a given mesh and default value.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    * 
    * @param vecSize size of the field's {@code VectorNd} values
    * @param mcomp component containing the mesh associated with the field
    * @param defaultValue default value for faces which don't have
    * explicitly set values
    */
   public VectorNdFaceField (
      int vecSize, MeshComponent mcomp, VectorNd defaultValue) {
      super (VectorNd.class, mcomp, defaultValue);
      initSize (vecSize);
      initValues();
   }

   /**
    * Constructs a named field for a given mesh, with a default value of 0.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    * 
    * @param name name of the field
    * @param vecSize size of the field's {@code VectorNd} values
    * @param mcomp component containing the mesh associated with the field
    */
   public VectorNdFaceField (String name, int vecSize, MeshComponent mcomp) {
      this (vecSize, mcomp);
      setName (name);
   }

   /**
    * Constructs a named field for a given mesh and default value.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    *
    * @param name name of the field
    * @param vecSize size of the field's {@code VectorNd} values
    * @param mcomp component containing the mesh associated with the field
    * @param defaultValue default value for faces which don't have
    * explicitly set values
    */
   public VectorNdFaceField (
      String name, int vecSize, MeshComponent mcomp, VectorNd defaultValue) {
      this (vecSize, mcomp, defaultValue);
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
}
