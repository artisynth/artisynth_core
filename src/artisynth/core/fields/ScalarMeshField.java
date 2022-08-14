package artisynth.core.fields;

import java.io.*;
import java.util.ArrayList;
import java.util.Deque;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.util.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.PropertyDesc.TypeCode;
import maspack.properties.PropertyDesc;

/**
 * Base class for scalar field defined over a mesh.
 */
public abstract class ScalarMeshField
   extends MeshFieldComp implements ScalarFieldComponent {

   double myDefaultValue = 0;   

   public ScalarMeshField () {
   }

   public ScalarMeshField (MeshComponent mcomp) {
      myDefaultValue = 0;
      setMeshComp (mcomp);
   }

   public ScalarMeshField (MeshComponent mcomp, double defaultValue) {
      myDefaultValue = defaultValue;
      setMeshComp (mcomp);
   }

   /**
    * {@inheritDoc}
    */
   public double getValue (FieldPoint fp) {
      return getValue(fp.getSpatialPos());
   }

   /**
    * {@inheritDoc}
    */  
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "defaultValue")) {
         myDefaultValue = rtok.scanNumber();
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);      
   }

   /**
    * {@inheritDoc}
    */   
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.println ("defaultValue=" + fmt.format(myDefaultValue));
   }

   /**
    * {@inheritDoc}
    */   
   public abstract double getValue (Point3d pos);
   
   /**
    * Clear all values defined for the features (e.g., vertices, faces)
    * associated with this field. After this call, the field will have a
    * uniform value defined by its {@code defaultValue}.
    */
   public abstract void clearAllValues();

   /**
    * Returns the default value for this field. See {@link #setDefaultValue}.
    *
    * @return default value for this field
    */
   public double getDefaultValue() {
      return myDefaultValue;
   }

   /**
    * Sets the default value for this field. Default values are used at
    * features (e.g., vertices, faces) for which values have not been
    * explicitly specified.
    * 
    * @param value new default value for this field
    */
   public void setDefaultValue (double value) {
      myDefaultValue = value;
   }
}
