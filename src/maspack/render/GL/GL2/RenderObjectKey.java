package maspack.render.GL.GL2;

import maspack.render.RenderObject;
import maspack.render.RenderObject.RenderObjectIdentifier;
import maspack.render.RenderObject.RenderObjectState;

public class RenderObjectKey {
   
   public enum DrawType {
      POINTS,
      LINES,
      TRIANGLES,
      VERTICES
   }
   
   RenderObjectIdentifier rId;
   int posSet;
   int nrmSet;
   int clrSet;
   int texSet;
   DrawType type;
   int groupIdx;
   
   public RenderObjectKey(RenderObject robj, DrawType type) {
      this.rId = robj.getIdentifier();
      this.posSet = robj.getPositionSetIdx();
      this.nrmSet = robj.getNormalSetIdx();
      this.clrSet = robj.getColorSetIdx();
      this.texSet = robj.getTextureCoordSetIdx();
      this.type = type;
      switch (type) {
         case LINES:
            this.groupIdx = robj.getLineGroupIdx();
            break;
         case POINTS:
            this.groupIdx = robj.getPointGroupIdx();
            break;
         case TRIANGLES:
            this.groupIdx = robj.getTriangleGroupIdx();
            break;
         case VERTICES:
            break;
         default:
            break;
      }
   }
   
   public RenderObjectKey(RenderObjectIdentifier rId, RenderObjectState rState, DrawType type) {
      this.rId = rId;
      this.posSet = rState.getPositionSetIdx();
      this.nrmSet = rState.getNormalSetIdx();
      this.clrSet = rState.getColorSetIdx();
      this.texSet = rState.getTextureCoordSetIdx();
      this.type = type;
      switch (type) {
         case LINES:
            this.groupIdx = rState.getLineGroupIdx();
            break;
         case POINTS:
            this.groupIdx = rState.getPointGroupIdx();
            break;
         case TRIANGLES:
            this.groupIdx = rState.getTriangleGroupIdx();
            break;
         case VERTICES:
            break;
         default:
            break;
      }
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + System.identityHashCode(rId);
      result = prime*result + posSet+1;
      result = prime*result + nrmSet+1;
      result = prime*result + clrSet+1;
      result = prime*result + texSet+1;
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
      if (rId != other.rId
         || posSet != other.posSet || nrmSet != other.nrmSet 
         || clrSet != other.clrSet || texSet != other.texSet
         || type != other.type || groupIdx != other.groupIdx) {
         return false;
      }
      return true;
   }
   
   
   
}
