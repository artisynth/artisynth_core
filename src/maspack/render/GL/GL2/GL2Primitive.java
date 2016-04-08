package maspack.render.GL.GL2;

public class GL2Primitive extends GL2Object {
   
   public static final int DEFAULT_RESOLUTION = 64;
   public static final boolean DEFAULT_CAPPED = true;
   
   public enum PrimitiveType {
      SPHERE,
      CYLINDER,
      SPINDLE,
      CONE,
      CUBE,
   }
   
   public static class PrimitiveKey {
      PrimitiveType type;
      int resolution;
      boolean capped;
      
      public PrimitiveKey(PrimitiveType type) {
         this(type, DEFAULT_RESOLUTION, DEFAULT_CAPPED);
      }
      
      public PrimitiveKey(PrimitiveType type, int resolution) {
         this(type, resolution, DEFAULT_CAPPED);
      }
      
      public PrimitiveKey(PrimitiveType type, int resolution, boolean capped) {
         this.type = type;
         switch(type) {
            case CONE:
            case CYLINDER:
               this.resolution = resolution;
               this.capped = capped;
               break;
            case SPHERE:
            case SPINDLE:
               this.resolution = resolution;
               this.capped = false;
               break;
            case CUBE:
               this.capped = false;
               this.resolution = 0;
               break;
         }
      }
      
      public PrimitiveType getType() {
         return type;
      }
      
      public int getResolution() {
         return resolution;
      }
      
      public boolean isCapped() {
         return capped;
      }
      
      @Override
      public int hashCode () {
         return resolution*31+type.hashCode ()*17 + (capped ? 0 : 2037);
      }
      
      @Override
      public boolean equals (Object obj) {
         if (obj == null || !obj.getClass ().equals (this.getClass ())) {
            return false;
         }
         PrimitiveKey other = (PrimitiveKey)obj;
         return (other.type == this.type && other.resolution == this.resolution && other.capped == this.capped);
      }
      
   }
   
   PrimitiveKey key;
   
   public GL2Primitive(PrimitiveKey key, GL2DisplayList list) {
      super(list);
      this.key = key;
   }
   
   public PrimitiveType getType() {
      return key.getType();
   }
   
   public int getResolution() {
      return key.getResolution ();
   }
   
   public boolean isCapped() {
      return key.isCapped();
   }
   
   public PrimitiveKey getKey() {
      return key;
   }
   
   @Override
   public GL2Primitive acquire () {
      return (GL2Primitive)super.acquire ();
   }

}
