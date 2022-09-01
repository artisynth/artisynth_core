package artisynth.core.modelbase;

import maspack.matrix.Point3d;
import maspack.geometry.Vertex3d;

/**
 * Provides information about a point on a mesh that can be used to evaluate
 * the value of a {@link FieldComponent} at that point.  The point's position
 * is assumed to be determined by a weighted combination of vertex positions.
 */
public interface MeshFieldPoint {

   /**
    * Returns the current spatial position.
    */
   public Point3d getPosition();

   /**
    * Returns the vertices whose positions determine the point position.
    */
   public Vertex3d[] getVertices();

   /**
    * Returns the number of vertices returned by {@link #getVertices}.
    */
   public int numVertices();

   /**
    * Returns the weights used to combine the vertex positions to obtain
    * the point's position.
    */
   public double[] getWeights();

}
