package maspack.render.GL.GL3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.media.opengl.GL3;

import maspack.render.Material;

public class MaterialsUBO extends UniformBufferObject {
   
   // // material properties
   // struct Material {
   //    vec4 diffuse;   // alpha is diffuse.a
   //    vec4 ambient;   // diffuse-mixing factor is ambient.a
   //    vec4 specular;  // shininess is specular.a
   //    vec4 emission;  
   // };
   
   static final String[] MATERIALS_ATTRIBUTES = { 
      "front_material.diffuse",
      "front_material.ambient",
      "front_material.specular",
      "front_material.emission",
      "back_material.diffuse",
      "back_material.ambient",
      "back_material.specular",
      "back_material.emission"
   };
   
   static final String BLOCK_NAME = "Materials";
   static final int FRONT_DIFFUSE = 0;
   static final int FRONT_AMBIENT = 1;
   static final int FRONT_SPECULAR = 2;
   static final int FRONT_EMISSION = 3;
   static final int BACK_DIFFUSE = 4;
   static final int BACK_AMBIENT = 5;
   static final int BACK_SPECULAR = 6;
   static final int BACK_EMISSION = 7;
   
   int foffsets[];
   int fsize;
   
   public MaterialsUBO(GL3 gl, int progId) {
      super(gl, progId, BLOCK_NAME, MATERIALS_ATTRIBUTES, GL3.GL_DYNAMIC_DRAW);
      foffsets = new int[offsets.length];
      for (int i=0; i<offsets.length; ++i) {
         foffsets[i] = offsets[i]/Float.BYTES;
      }
      fsize = getSize()/Float.BYTES;
   }
   
   public void updateMaterials(GL3 gl, Material frontMaterial, Material backMaterial) {
      
      // if no back material, use same as front
      if (backMaterial == null) {
         backMaterial = frontMaterial;
      }
      
      float[] materialbuff = new float[fsize];
      
      int offset;
      
      offset = foffsets[FRONT_DIFFUSE];
      copy(materialbuff, offset, frontMaterial.getDiffuse(), 4); // alpha already stored in diffuse
      offset = foffsets[FRONT_AMBIENT];
      copy(materialbuff, offset, frontMaterial.getAmbient(), 3);
      materialbuff[offset+3] = frontMaterial.getAmbienceCoefficient();
      offset = foffsets[FRONT_SPECULAR];
      copy(materialbuff, offset, frontMaterial.getSpecular(), 3);
      materialbuff[offset+3] = frontMaterial.getShininess();
      offset = foffsets[FRONT_EMISSION];
      copy(materialbuff, offset, frontMaterial.getEmission(), 4); 
      
      offset = foffsets[BACK_DIFFUSE];
      copy(materialbuff, offset, backMaterial.getDiffuse(), 4); // alpha already stored in diffuse
      offset = foffsets[BACK_AMBIENT];
      copy(materialbuff, offset, backMaterial.getAmbient(), 3);
      materialbuff[offset+3] = backMaterial.getAmbienceCoefficient();
      offset = foffsets[BACK_SPECULAR];
      copy(materialbuff, offset, backMaterial.getSpecular(), 3);
      materialbuff[offset+3] = backMaterial.getShininess();
      offset = foffsets[BACK_EMISSION];
      copy(materialbuff, offset, backMaterial.getEmission(), 4);
      
      FloatBuffer fb = FloatBuffer.wrap(materialbuff);
      update(gl, fb);

   }
   
   public void updateMaterials(GL3 gl, Material frontMaterial, float[] frontDiffuse, 
      Material backMaterial, float[] backDiffuse) {
      
      // if no back material, use same as front
      if (backMaterial == null) {
         backMaterial = frontMaterial;
      }
      
      float[] materialbuff = new float[fsize];
      
      int offset;
      
      if (frontDiffuse == null) {
         frontDiffuse = frontMaterial.getDiffuse();
      } else if (frontDiffuse.length == 3) {
         float[] diff = new float[4];
         diff[0] = frontDiffuse[0];
         diff[1] = frontDiffuse[1];
         diff[2] = frontDiffuse[2];
         diff[3] = frontMaterial.getDiffuse()[3];
         frontDiffuse = diff;
      }
      
      if (backDiffuse == null) {
         backDiffuse = backMaterial.getDiffuse();
      } else if (backDiffuse.length == 3) {
         float[] diff = new float[4];
         diff[0] = backDiffuse[0];
         diff[1] = backDiffuse[1];
         diff[2] = backDiffuse[2];
         diff[3] = backMaterial.getDiffuse()[3];
         backDiffuse = diff;
      }
      
      offset = foffsets[FRONT_DIFFUSE];
      copy(materialbuff, offset, frontDiffuse, 4); // alpha already stored in diffuse
      offset = foffsets[FRONT_AMBIENT];
      copy(materialbuff, offset, frontMaterial.getAmbient(), 3);
      materialbuff[offset+3] = frontMaterial.getAmbienceCoefficient();
      offset = foffsets[FRONT_SPECULAR];
      copy(materialbuff, offset, frontMaterial.getSpecular(), 3);
      materialbuff[offset+3] = frontMaterial.getShininess();
      offset = foffsets[FRONT_EMISSION];
      copy(materialbuff, offset, frontMaterial.getEmission(), 4); 
      
      offset = foffsets[BACK_DIFFUSE];
      copy(materialbuff, offset, backDiffuse, 4); // alpha already stored in diffuse
      offset = foffsets[BACK_AMBIENT];
      copy(materialbuff, offset, backMaterial.getAmbient(), 3);
      materialbuff[offset+3] = backMaterial.getAmbienceCoefficient();
      offset = foffsets[BACK_SPECULAR];
      copy(materialbuff, offset, backMaterial.getSpecular(), 3);
      materialbuff[offset+3] = backMaterial.getShininess();
      offset = foffsets[BACK_EMISSION];
      copy(materialbuff, offset, backMaterial.getEmission(), 4);
      
      FloatBuffer fb = FloatBuffer.wrap(materialbuff);
      update(gl, fb);

   }
   
   public void updateColor(GL3 gl, ByteBuffer buff, int cidx) {
      update(gl, buff, offsets[cidx]);
   }
   
   public void updateColor(GL3 gl, float[] rgba, int cidx) {
      ByteBuffer buff = ByteBuffer.allocateDirect(4*Float.BYTES);
      buff.order(ByteOrder.nativeOrder());
      for (int i=0; i<3; ++i) {
         buff.putFloat(rgba[i]);
      }
      if (rgba.length > 3) {
         buff.putFloat(rgba[3]);
      } else {
         buff.putFloat(1.0f);
      }
      buff.rewind(); // rewind to beginning
      updateColor(gl, buff, cidx);
   }
   
   private static void copy(float[] out, int offset, float[] in, int len) {
      int idx = offset;
      for (int i = 0; i < len; ++i) {
         out[idx++] = in[i];
      }
   }
   

}
