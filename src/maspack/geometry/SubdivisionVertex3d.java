package maspack.geometry;

import maspack.matrix.Vector3d;

/**
 * Holds a reference to the face and barycentric coordinates used to generate
 * this vertice.
 * 
 * @author elliote
 * 
 */
public class SubdivisionVertex3d extends Vertex3d {
   public Face f = null;
   public double u, v;
   Vector3d normal = new Vector3d();

   public SubdivisionVertex3d (Face face, double iu, double iv) {
      super();
      f = face;
      u = iu;
      v = iv;
   }

   public boolean computeNormal (Vector3d nrm) {
      nrm.set (normal);
      return true;
   }
   
   public SubdivisionVertex3d clone() {
      SubdivisionVertex3d vtx = null;
      
      vtx = (SubdivisionVertex3d)super.clone();
      vtx.u = u;
      vtx.v = v;
      vtx.f = f;
      
      return vtx;
   }
}
