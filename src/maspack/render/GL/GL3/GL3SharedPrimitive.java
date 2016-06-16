package maspack.render.GL.GL3;

public class GL3SharedPrimitive extends GL3SharedObject {

   public static enum PrimitiveType {
      AXES,
      CONE,
      CUBE,
      CYLINDER,
      SPHERE,
      SPINDLE
   }
   
   public static abstract class PrimitiveKey {
      @Override
      public int hashCode () {
         final int prime = 31;
         return prime + ((type == null) ? 0 : type.hashCode ());
      }

      @Override
      public boolean equals (Object obj) {
         if (this == obj) {
            return true;
         }
         if (obj == null) {
            return false;
         }
         if (obj instanceof PrimitiveKey) {
            return false;
         }
         PrimitiveKey other = (PrimitiveKey)obj;
         if (type != other.type) {
            return false;
         }
         return true;
      }

      PrimitiveType type;
      public PrimitiveKey (PrimitiveType type) {
         this.type = type;
      }
      
      public PrimitiveType getType() {
         return type;
      }
      
   }
   
   public static class AxesKey extends PrimitiveKey {
      boolean x;
      boolean y;
      boolean z;
      public AxesKey(boolean x, boolean y, boolean z) {
         super(PrimitiveType.AXES);
         this.x = x;
         this.y = y;
         this.z = z;
      }
      public boolean hasX() {
         return x;
      }
      public boolean hasY() {
         return y;
      }
      public boolean hasZ() {
         return z;
      }
      public int numAxes() {
         return ( (x ? 1 : 0) + (y ? 1 : 0) + (z ? 1 : 0) );
      }
      @Override
      public int hashCode() {
         return ( (x ? 1 : 0) + (y ? 2 : 0) + (z ? 4 : 0) );
      }
      
      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if ( (obj == null) || (getClass() != obj.getClass()) ) {
            return false;
         }
         AxesKey other = (AxesKey)obj;
         return equals(other.x, other.y, other.z);
      }
      
      public boolean equals(boolean x, boolean y, boolean z) {
         if ( (this.x != x) || (this.y != y) || (this.z != z) ) {
            return false;
         }
         return true;
      }
   }
   
   public static class SphereKey extends PrimitiveKey {
      private int slices;
      private int levels;
      public SphereKey(int slices, int levels) {
         super(PrimitiveType.SPHERE);
         this.slices = slices;
         this.levels = levels;
      }
      public int getSlices() {
         return slices;
      }
      public int getLevels() {
         return levels;
      }
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + levels;
         result = prime * result + slices;
         return result;
      }
      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if ( (obj == null) || (getClass() != obj.getClass())) {
            return false;
         }
         SphereKey other = (SphereKey)obj;
         return equals(other.slices, other.levels);
      }
      
      public boolean equals(int slices, int levels) {
         if ((this.levels != levels) || (this.slices != slices)) {
            return false;
         }
         return true;
      }
   }
   
   public static class SpindleKey extends PrimitiveKey {
      private int slices;
      private int levels;
      public SpindleKey(int slices, int levels) {
         super(PrimitiveType.SPINDLE);
         this.slices = slices;
         this.levels = levels;
      }
      public int getSlices() {
         return slices;
      }
      public int getLevels() {
         return levels;
      }
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + levels;
         result = prime * result + slices;
         return result;
      }
      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if ( (obj == null) || (getClass() != obj.getClass())) {
            return false;
         }
         SpindleKey other = (SpindleKey)obj;
         return equals(other.slices, other.levels);
      }
      
      public boolean equals(int slices, int levels) {
         if ((this.levels != levels) || (this.slices != slices)) {
            return false;
         }
         return true;
      }
   }
   
   public static class CylinderKey extends PrimitiveKey {
      private int slices;
      boolean capped;
      public CylinderKey(int slices, boolean capped) {
         super(PrimitiveType.CYLINDER);
         this.slices = slices;
         this.capped = capped;
      }
      public int getSlices() {
         return slices;
      }
      public boolean isCapped() {
         return capped;
      }
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + (capped ? 1231 : 1237);
         result = prime * result + slices;
         return result;
      }
      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if ( (obj == null) ||  (getClass() != obj.getClass())) {
            return false;
         }
         CylinderKey other = (CylinderKey)obj;
         return equals(other.slices, other.capped);
      }
      
      public boolean equals(int slices, boolean capped) {
         if ((this.capped != capped) || (this.slices != slices)) {
            return false;
         }
         return true;
      }
   }
   
   public static class ConeKey extends PrimitiveKey {
      private int slices;
      boolean capped;
      public ConeKey(int slices, boolean capped) {
         super(PrimitiveType.CONE);
         this.slices = slices;
         this.capped = capped;
      }
      public int getSlices() {
         return slices;
      }
      public boolean isCapped() {
         return capped;
      }
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + (capped ? 1231 : 1237);
         result = prime * result + slices;
         return result;
      }
      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if ( (obj == null) ||  (getClass() != obj.getClass())) {
            return false;
         }
         ConeKey other = (ConeKey)obj;
         return equals(other.slices, other.capped);
      }
      
      public boolean equals(int slices, boolean capped) {
         if ((this.capped != capped) || (this.slices != slices)) {
            return false;
         }
         return true;
      }
   }
   
   public static class CubeKey extends PrimitiveKey {
      public CubeKey() {
         super(PrimitiveType.CUBE);
      };
      
      @Override
      public int hashCode () {
         return super.hashCode ();
      }
      @Override
      public boolean equals (Object obj) {
         if (obj == null) {
            return false;
         }
         if (obj.getClass () != this.getClass ()) {
            return false;
         }
         return true;
      }
   }
   
   PrimitiveKey key;
   public GL3SharedPrimitive(PrimitiveKey key, GL3VertexAttributeArray[] attributes, 
      GL3ElementAttributeArray elements, VertexBufferObject[] vbos, 
      IndexBufferObject ibo, int glMode) {
      super(attributes, elements, vbos, ibo, glMode);
      this.key = key;
   }
   
   @Override
   public GL3SharedPrimitive acquire () {
      return (GL3SharedPrimitive)super.acquire ();
   }
   
   public boolean matches(PrimitiveKey key) {
      if (this.key.equals (key)) {
         return true;
      }
      return false;
   }
   
   public PrimitiveKey getKey() {
      return key;
   }
   
}
