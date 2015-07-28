package maspack.render.GL.GL2;


public class DisplayListKey {

   private Object r;
   private int id;
   
   public DisplayListKey(Object r, int id) {
      this.r = r;
      this.id = id;
   }
   
   public Object getObject() {
      return r;
   }
   
   public int getID() {
      return id;
   }
   
   @Override
   public int hashCode() {
      // native hash code, makes immutable
      return 31*id+System.identityHashCode(r);
   }
   
   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
         return false;
      }
      DisplayListKey other = (DisplayListKey)obj;
      if (other.r != r || other.id != id) {
         return false;
      }
      return true;
   }
}
