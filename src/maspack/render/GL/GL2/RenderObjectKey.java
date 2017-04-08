package maspack.render.GL.GL2;

import maspack.render.RenderKey;
import maspack.render.RenderObject.RenderObjectIdentifier;

public class RenderObjectKey implements RenderKey {
   
   public enum DrawType {
      POINTS,
      LINES,
      TRIANGLES,
      VERTICES
   }
   
   RenderObjectIdentifier rId;
   DrawType type;
   int groupIdx;
   
   public RenderObjectKey(RenderObjectIdentifier rId, DrawType type, int gidx) {
      this.rId = rId;
      this.type = type;
      this.groupIdx = gidx;
   }
   
   public boolean isValid() {
      return !rId.isDisposed ();
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + rId.getId();
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      result = prime * result + groupIdx;
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
         return false;
      }
      
      RenderObjectKey other = (RenderObjectKey)obj;
      if (rId != other.rId || type != other.type || groupIdx != other.groupIdx) {
         return false;
      }
      return true;
   }
   
   
   
}
