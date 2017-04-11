package maspack.geometry;

import maspack.util.Clonable;
import maspack.util.InternalErrorException;

public class Vertex3dNode implements Clonable {

   Vertex3dNode next;
   Vertex3dNode prev;
   Vertex3d vtx;

   public Vertex3dNode (Vertex3d vtx) {
      this.vtx = vtx;
   }         

   public Vertex3dNode getNext() {
      return next;
   }

   public void setNext (Vertex3dNode next) {
      this.next = next;
   }

   public Vertex3dNode getPrev() {
      return prev;
   }

   public void setPrev (Vertex3dNode prev) {
      this.prev = prev;
   }

   public Vertex3d getVertex() {
      return vtx;
   }

   public void setVertex (Vertex3d vtx) {
      this.vtx = vtx;
   }

   public Vertex3dNode clone() {
      Vertex3dNode copy;
      try {
         copy = (Vertex3dNode)super.clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException (
            "Clone not supported for Vertex3dNode");
      }
      return copy;
   }

}
