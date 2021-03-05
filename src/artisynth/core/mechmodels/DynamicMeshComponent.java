package artisynth.core.mechmodels;

import maspack.geometry.Vertex3d;

/**
 * Mesh component that is driven by dynamics
 */
public abstract class DynamicMeshComponent extends MeshComponent {
   
   public DynamicMeshComponent() {
      super();
   }
   
   public DynamicMeshComponent(String name) {
      super(name);
   }
   
   /**
    * Retrieves (or creates) a vertex attachment component associated with
    * vertex vidx, describing how its motion is driven by the underlying
    * physical system.
    * 
    * @param vidx vertex index
    * 
    * @return PointAttachment structure
    */
   public abstract PointAttachment getVertexAttachment(int vidx);
   
   /**
    * Retrieves (or creates) a vertex attachment component associated with
    * vertex vtx, describing how its motion is driver by the underlying
    * physical system.
    * 
    * @param vtx vertex
    * @return PointAttachment structure
    */
   public abstract PointAttachment getVertexAttachment(Vertex3d vtx);

}
