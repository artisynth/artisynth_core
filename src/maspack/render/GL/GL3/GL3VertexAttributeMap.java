package maspack.render.GL.GL3;

import java.util.HashMap;

public class GL3VertexAttributeMap {

   GL3VertexAttributeInfo VERTEX_POSITION;
   GL3VertexAttributeInfo VERTEX_NORMAL;
   GL3VertexAttributeInfo VERTEX_COLOR;
   GL3VertexAttributeInfo VERTEX_TEXCOORD;
   
   HashMap<String,GL3VertexAttributeInfo> attribMap;
   
   public GL3VertexAttributeMap(GL3VertexAttributeInfo position, GL3VertexAttributeInfo normal,
      GL3VertexAttributeInfo color, GL3VertexAttributeInfo texcoord) {
      this.VERTEX_POSITION = position;
      this.VERTEX_NORMAL = normal;
      this.VERTEX_COLOR = color;
      this.VERTEX_TEXCOORD = texcoord;
      attribMap = new HashMap<> ();
      attribMap.put (VERTEX_POSITION.getName (), VERTEX_POSITION);
      attribMap.put (VERTEX_NORMAL.getName (), VERTEX_NORMAL);
      attribMap.put (VERTEX_COLOR.getName (), VERTEX_COLOR);
      attribMap.put (VERTEX_TEXCOORD.getName (), VERTEX_TEXCOORD);
      
   }
   
   public GL3VertexAttributeInfo getPosition() {
      return VERTEX_POSITION;
   }
   
   public GL3VertexAttributeInfo getNormal() {
      return VERTEX_NORMAL;
   }
   
   public GL3VertexAttributeInfo getColor() {
      return VERTEX_COLOR;
   }
   
   public GL3VertexAttributeInfo getTexcoord() {
      return VERTEX_TEXCOORD;
   }
   
   public void add(GL3VertexAttributeInfo info) {
      put(info.getName (), info);
   }
   
   public void put(String name, GL3VertexAttributeInfo info) {
      attribMap.put (name, info);
   }
   
   public GL3VertexAttributeInfo get(String name) {
      return attribMap.get (name);
   }
   
}
