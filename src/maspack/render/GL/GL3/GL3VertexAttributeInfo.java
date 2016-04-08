package maspack.render.GL.GL3;

public class GL3VertexAttributeInfo {
   
   String name;
   int location;
   
   public GL3VertexAttributeInfo(String name, int location) {
      this.name = name;
      this.location = location;
   }

   public String getName() {
      return name;
   }
   
   public void setName(String s) {
      name = s;
   }
   
   public int getLocation() {
      return location;
   }
   
   public void setLocation(int l) {
      location = l;
   }
   
   @Override
   public String toString () {
      return name;
   }
}
