package maspack.render.GL.GL2;

import maspack.render.RenderInstances.RenderInstancesIdentifier;
import maspack.render.RenderInstances.RenderInstancesVersion;
import maspack.render.RenderObject.RenderObjectIdentifier;

public class RenderInstancesKey extends RenderObjectKey {
   
   RenderInstancesIdentifier riv;
   public RenderInstancesKey(RenderInstancesIdentifier rinst, RenderObjectIdentifier robj, DrawType type, int gidx) {
      super(robj, type, gidx);
      this.riv = rinst;
   }
   
   public boolean isValid() {
      return super.isValid() && riv.isValid();
   }

   @Override
   public int hashCode() {
      return riv.getId()*31+super.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
         return false;
      }
      
      RenderInstancesKey other = (RenderInstancesKey)obj;
      if (riv != other.riv || !super.equals(other)) {
         return false;
      }
      return true;
   }
   
   

}
