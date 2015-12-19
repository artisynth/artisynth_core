package maspack.render.GL.GL3;

import javax.media.opengl.GL3;

/**
 * Basic attribute.  Note: it seems only "Arrays" are stored in VAOs, so it might be
 * better to always stick with a GL3Vertex
 * @author antonio
 *
 */
public abstract class GL3VertexAttribute {
   
   public static class AttributeInfo {
      private String name;
      private int index;
      public AttributeInfo(String name, int index) {
         this.name = name;
         this.index = index;
      }
      
      public String name() {
         return name;
      }
      
      public int index() {
         return index;
      }
      
      public String toString() {
         return name + " (location=" + index + ")";
      }
   }
   
   // XXX Assumes no (layout=) qualifier in GLSL, to allow consistent attribute
   //     indices across programs
   public static AttributeInfo VERTEX_POSITION = new AttributeInfo("vertex_position", 0);
   public static AttributeInfo VERTEX_NORMAL = new AttributeInfo("vertex_normal", 1);
   public static AttributeInfo VERTEX_COLOR = new AttributeInfo("vertex_color", 2);
   public static AttributeInfo VERTEX_TEXTURE = new AttributeInfo("vertex_texture", 3);
   
   public static AttributeInfo INSTANCE_SCALE = new AttributeInfo("instance_scale", 6);
   public static AttributeInfo INSTANCE_POSITION = new AttributeInfo("instance_position", 7);
   public static AttributeInfo INSTANCE_ORIENTATION = new AttributeInfo("instance_orientation", 8);
   public static AttributeInfo INSTANCE_AFFINE_MATRIX = new AttributeInfo("instance_affine_matrix", 6);
   public static AttributeInfo INSTANCE_NORMAL_MATRIX = new AttributeInfo("instance_normal_matrix", 10);
   public static AttributeInfo INSTANCE_COLOR = new AttributeInfo("instance_color", 4);
   public static AttributeInfo INSTANCE_TEXTURE = new AttributeInfo("instance_texture", 5);
   
   public static AttributeInfo LINE_RADIUS = new AttributeInfo("line_radius", 4);
   public static AttributeInfo LINE_BOTTOM_POSITION = new AttributeInfo("line_bottom_position", 5);
   public static AttributeInfo LINE_TOP_POSITION = new AttributeInfo("line_top_position", 6);
   public static AttributeInfo LINE_LENGTH_OFFSET = new AttributeInfo("line_length_offset", 7);
   public static AttributeInfo LINE_BOTTOM_COLOR = new AttributeInfo("line_bottom_color", 8);
   public static AttributeInfo LINE_TOP_COLOR = new AttributeInfo("line_top_color", 9);
   public static AttributeInfo LINE_BOTTOM_TEXTURE = new AttributeInfo("line_bottom_texture", 10);
   public static AttributeInfo LINE_TOP_TEXTURE = new AttributeInfo("line_top_texture", 11);
   
   private AttributeInfo attr;
   
   protected GL3VertexAttribute(AttributeInfo attr) {
      this.attr = attr;
   }
   
   public String getAttributeName() {
      return attr.name();
   }
   
   public int getAttributeIndex() {
      return attr.index();
   }
   
   /**
    * Bind attribute to current VAO
    * @param gl context
    */
   public abstract void bind(GL3 gl);
   
   public String toString() {
      return attr.toString();
   }
   
   /**
    * Check that underlying data is valid
    * @param gl
    * @return
    */
   public abstract boolean isValid();
  
}
