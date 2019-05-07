package maspack.geometry;

/**
 * Indicates how mass is distributed across the features of a geometric object
 * for purposes of computing its inertia.
 */
public enum MassDistribution {

   /**
    * Mass is distributed uniformly across the volume of the object
    */
   VOLUME,

   /**
    * Mass is distributed uniformly across the area (or faces) of the object
    */
   AREA,

   /**
    * Mass is distributed uniformly across the length (or edges) of the object
    */
   LENGTH,

   /**
    * Mass is distributed uniformly across the points of the object
    */
   POINT,

   /**
    * Mass is distributed uniformly according to a default appropriate to the
    * object
    */
   DEFAULT
}
