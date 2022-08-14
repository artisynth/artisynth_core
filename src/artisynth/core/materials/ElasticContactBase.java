package artisynth.core.materials;

import artisynth.core.fields.ScalarMeshField;
import artisynth.core.modelbase.ContactPoint;
import artisynth.core.modelbase.FunctionPropertyList;
import artisynth.core.modelbase.ScalarFieldComponent;
import maspack.geometry.MeshBase;
import maspack.matrix.Vector3d;

/**
 * Base class for elastic foundation contact implementations.
 */
public abstract class ElasticContactBase extends ContactForceBehavior {

   /**
    * Describes how the method {@link #computeDamping} computes the
    * net damping {@code d} from the damping factor {@code df}.
    */
   public enum DampingMethod {
      /**
       * Damping is set directly to the damping factor {@code df}.
       */
      DIRECT,

      /**
       * Damping is set to {@code -force * df}, where {@code force} is the
       * contact force computed by the {@link #computeResponse} method and
       * {@code df} is the damping factor. This produces a Hunt and
       * Crossley-like damping similar to that used in OpenSim (see Sherman,
       * Seth and Delp, "Simbody: multibody dynamics for biomedical research",
       * 2011 Symposium on Human Body Dynamics).
       */
      FORCE
   };

   protected static double DEFAULT_THICKNESS = 1;
   double myThickness = DEFAULT_THICKNESS;
   ScalarFieldComponent myThicknessField = null;

   protected static double DEFAULT_MIN_THICKNESS_RATIO = 0.01;
   double myMinThicknessRatio = DEFAULT_MIN_THICKNESS_RATIO;
   
   protected static double DEFAULT_DAMPING_FACTOR = 1000;
   double myDampingFactor = DEFAULT_DAMPING_FACTOR;

   protected static DampingMethod DEFAULT_DAMPING_METHOD = DampingMethod.DIRECT;
   DampingMethod myDampingMethod = DEFAULT_DAMPING_METHOD;

   protected static boolean DEFAULT_USE_LOG_DISTANCE = true;
   boolean myUseLogDistance = DEFAULT_USE_LOG_DISTANCE;

   protected static boolean DEFAULT_USE_LOCAL_CONTACT_AREA = true;
   boolean myUseLocalContactArea = DEFAULT_USE_LOCAL_CONTACT_AREA;

   public static FunctionPropertyList myProps =
   new FunctionPropertyList (ElasticContactBase.class, MaterialBase.class);

   public FunctionPropertyList getAllPropertyInfo() {
      return myProps;
   }

   static {
      myProps.addWithField (
         "thickness", "foundation layer thickness", DEFAULT_THICKNESS);
      myProps.add (
         "minThicknessRatio",
         "penetration threshold past which contact force becomes linear",
         DEFAULT_MIN_THICKNESS_RATIO);
      myProps.add (
         "dampingFactor",
         "factor used to compute the damping", DEFAULT_DAMPING_FACTOR);
      myProps.add (
         "dampingMethod", 
         "describes how damping is computed from the damping factor",
         DEFAULT_DAMPING_METHOD);
      myProps.add (
         "useLogDistance", "generate force using log(1-d/thickness)",
         DEFAULT_USE_LOG_DISTANCE);
      myProps.add (
         "useLocalContactArea",
         "compute contact area locally to each vertex",
         DEFAULT_USE_LOCAL_CONTACT_AREA);
   }

   /**
    * Need no-args constructor for scanning
    */
   public ElasticContactBase () {
   }

   /**
    * Queries the thickness of the foundation layer.
    *
    * @return foundation layer thickness
    */
   public double getThickness () {
      return myThickness;
   }

   /**
    * Sets the thickness of the foundation layer.
    * 
    * @param h new foundation layer thickness
    */
   public void setThickness (double h) {
      this.myThickness = h;
   }

   /**
    * Internal method to find the foundation layer thickness. If the property
    * is bound to a field, the method first checks to see if it is a mesh field
    * associated with the meshes of either {@code cpnt0} or {@code cpnt1}, and
    * if it is, it extracts the value using the vertex information of the
    * contact point.  Otherwise, the value is extracted from the field using a
    * general point query. If the property is not bound to a field, then the
    * fixed property value is returned.
    */
   protected double getThickness (ContactPoint cpnt0, ContactPoint cpnt1) {
      if (myThicknessField == null) {
         return getThickness();
      }
      else {
         return getScalarFieldValue (myThicknessField, cpnt0, cpnt1);
      }
   }

   /**
    * Queries the field, if any, associated with the foundation thickness.
    *
    * @return thickness field, or {@code null}
    */
   public ScalarFieldComponent getThicknessField() {
      return myThicknessField;
   }

   /**
    * Binds the foundation thickness to a field, or unbinds it if {@code field}
    * is {@code null}.
    *
    * @param field field to bind to, or {@code null}
    */
   public void setThicknessField (ScalarFieldComponent field) {
      myThicknessField = field;
   }

   /**
    * Queries the minimum thickness ratio for this material. See
    * {@link #setMinThicknessRatio}.
    * 
    * @return minimum thickness ratio
    */
   public double getMinThicknessRatio() {
      return myMinThicknessRatio;
   }
   
   /**
    * Sets the minimum thickness ratio for this material.  If contact forces
    * are being generated using log distances, and penetration into the
    * foundation layer exceeds {@code h*(1-r)}, then the contact force revert
    * to a linear formulation, using the stiffness value at the threshold
    * point. This ensures that restoring forces are still defined even for
    * penetrations through the foundation layer.
    * 
    * @param r new minimum thickness ratio
    */
   public void setMinThicknessRatio (double r) {
      myMinThicknessRatio = r;
   }

   /**
    * Queries the damping factor for this material. See {@link
    * #setDampingFactor}.
    * 
    * @return damping factor for this material
    */
   public double getDampingFactor () {
      return myDampingFactor;
   }

   /**
    * Sets the damping factor for this material. This is this constant
    * multiplier used when computing the damping force.  See {@link
    * DampingMethod}.
    * 
    * @param df new damping factor
    */
   public void setDampingFactor (double df) {
      this.myDampingFactor = df;
   }

   /**
    * Queries the damping method for this material. See {@link
    * #setDampingMethod}.
    * 
    * @return damping method for this material
    */
   public DampingMethod getDampingMethod () {
      return myDampingMethod;
   }

   /**
    * Sets the damping method for this material, which determines how damping
    * forces are compute. See {@link DampingMethod}.
    * 
    * @param method new damping method
    */
   public void setDampingMethod (DampingMethod method) {
      this.myDampingMethod = method;
   }

   /**
    * Queries whether log distances are used for computing contact
    * forces. See {@link #setUseLogDistance}.
    *
    * @return {@code true} if log distances are used for computing contact
    * forces
    */
   public boolean getUseLogDistance() {
      return myUseLogDistance;
   }

   /**
    * Sets whether log distances are enabled for computing contact forces.  If
    * enabled, then contact forces increase according to
    * <pre>
    *   ln (1 + d/h)
    * </pre>
    * where {@code d} is the (negatively valued) penetration distance,
    * and {@code h} is the foundation layer thickness. This ensures
    * that forces increase non-linearly to prevent penetration of
    * the foundation layer.
    * 
    * @param enable if {@code true}, enables using log distances to compute
    * contact forces
    */
   public void setUseLogDistance (boolean enable) {
      myUseLogDistance = enable;
   }

   /**
    * Queries whether local contact areas are used for computing
    * contact pressure. See {@link #setUseLocalContactArea}.
    * 
    * @return {@code true} if local contact areas are used for
    * computing pressure
    */
   public boolean getUseLocalContactArea() {
      return myUseLocalContactArea;
   }

   /**
    * Queries whether local contact areas are used for computing contact
    * pressure. If enabled, then within {@link #computeResponse}, the system
    * will attempt to determine the area around each contact using local vertex
    * information for that contact. Otherwise, it will use the value of the
    * {@code contactArea} argument.
    * 
    * @param enable if {@code true}, enables using local contact areas for
    * computing pressure
    */
   public void setUseLocalContactArea (boolean enable) {
      myUseLocalContactArea = enable;
   }

   /**
    * {@inheritDoc}
    */
   public ElasticContactBase clone() {
      return (ElasticContactBase)super.clone();
   }

   /**
    * Computes a damping term in the manner prescribed by the damping method.
    * This method is available for used inside a subclass implementation of
    * {@link #computeResponse} to compute the current damping term stored in
    * {@code fres[2]}.
    *
    * @param force contact force, as computed by {@link #computeResponse} and
    * stored in {@code fres[0]}.

    * @param compliance contact compliance, as computed by {@link
    * #computeResponse} and stored in {@code fres[1]}.
    */
   protected double computeDamping (double force, double compliance) {
      
      switch (myDampingMethod) {
         case DIRECT: {
            return myDampingFactor;
         }
         case FORCE: {
            return -myDampingFactor*force;
         }
         default: {
            throw new UnsupportedOperationException (
               "Unimplemented damping method " + myDampingMethod);
         }
      }
   }

   /**
    * Convenience method to estimate the area of a contact, given the first
    * contact point, the contact normal, the value of the {@code contactArea}
    * argument to [@link #computeResponse}, and the current setting of the
    * property {@code useLocalContactArea}.
    */
   protected double getContactArea (
      ContactPoint cpnt, Vector3d normal, double contactArea) {
     
      double area;
      if (myUseLocalContactArea) {
         area = cpnt.computeContactArea (normal);
         if (area == -1) {
            throw new IllegalArgumentException (
               "LinearElasticContact: no vertex associated with contact point."+
               "Make sure the contact method is set to VERTEX_PENETRATION.");
         }
      }
      else {
         if (contactArea == -1) {
            throw new IllegalArgumentException (
               "LinearElasticContact: contactArea undefined. Make sure the "+
               "collider type is set to AJL_CONTOUR.");
         }
         area = contactArea;
      }
      return area;
   }
   
   protected double getScalarFieldValue (
      ScalarFieldComponent field, ContactPoint cpnt0, ContactPoint cpnt1) {
      if (field instanceof ScalarMeshField) {
         MeshBase mesh = ((ScalarMeshField)field).getMesh();
         if (cpnt0.getMesh() == mesh) {
            return field.getValue (cpnt0);
         }
         else if (cpnt1.getMesh() == mesh) {
            return field.getValue (cpnt1);
         }
      }
      return field.getValue (cpnt0.getPosition());
   }
   
}
