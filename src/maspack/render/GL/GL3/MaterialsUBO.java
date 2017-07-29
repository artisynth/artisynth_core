package maspack.render.GL.GL3;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL3;

import maspack.render.Material;
import maspack.render.GL.GLSupport;

public class MaterialsUBO extends UniformBufferObject {
   
   // // material properties
   // struct Material {
   //    vec4 diffuse;   // alpha is diffuse.a
   //    vec4 specular;  // shininess is specular.a
   //    vec4 emission;  
   //    vec4 power;
   // };
   
   static final String[] MATERIALS_ATTRIBUTES = { 
      "front_material.diffuse",
      "front_material.specular",
      "front_material.emission",
      "front_material.power",
      "back_material.diffuse",
      "back_material.specular",
      "back_material.emission",
      "back_material.power"
   };
   
   static final String BLOCK_NAME = "Materials";
   static final int FRONT_DIFFUSE = 0;
   static final int FRONT_SPECULAR = 1;
   static final int FRONT_EMISSION = 2;
   static final int FRONT_POWER = 3;
   static final int BACK_DIFFUSE = 4;
   static final int BACK_SPECULAR = 5;
   static final int BACK_EMISSION = 6;
   static final int BACK_POWER = 7;
   
   private MaterialsUBO(GL3 gl, int progId) {
      super(gl, progId, BLOCK_NAME, MATERIALS_ATTRIBUTES, GL3.GL_DYNAMIC_DRAW);
   }
   
   public void setMaterials(GL3 gl, Material frontMaterial, Material backMaterial) {
      
      // if no back material, use same as front
      if (backMaterial == null) {
         backMaterial = frontMaterial;
      }
      
      // everything except scale
      ByteBuffer buff = getBuffer();
      
      buff.position (getByteOffset(FRONT_DIFFUSE));
      putFloat(buff, frontMaterial.getDiffuse(), 4); // alpha already stored in diffuse
      buff.position (getByteOffset(FRONT_SPECULAR));
      putFloat(buff, frontMaterial.getSpecular(), 3);
      buff.putFloat (frontMaterial.getShininess());
      buff.position (getByteOffset(FRONT_EMISSION));
      putFloat (buff, frontMaterial.getEmission(), 4); 
      buff.position (getByteOffset(FRONT_POWER));
      putFloat (buff, frontMaterial.getPower(), 4);
      
      buff.position (getByteOffset(BACK_DIFFUSE));
      putFloat(buff, backMaterial.getDiffuse(), 4); // alpha already stored in diffuse
      buff.position (getByteOffset(BACK_SPECULAR));
      putFloat(buff, backMaterial.getSpecular(), 3);
      buff.putFloat (backMaterial.getShininess());
      buff.position (getByteOffset(BACK_EMISSION));
      putFloat (buff, backMaterial.getEmission(), 4); 
      buff.position (getByteOffset(BACK_POWER));
      putFloat (buff, backMaterial.getPower(), 4);
      
      int len = buff.position();
      buff.flip ();
      update(gl, buff, 0, len);

   }
   
   public void updateMaterials(GL3 gl, Material frontMaterial, Material backMaterial) {
      
      // if no back material, use same as front
      if (backMaterial == null) {
         backMaterial = frontMaterial;
      }
      
      // everything except scale
      ByteBuffer buff = getBuffer();
      
      buff.position (getByteOffset(FRONT_DIFFUSE));
      putFloat(buff, frontMaterial.getDiffuse(), 4); // alpha already stored in diffuse
      buff.position (getByteOffset(FRONT_SPECULAR));
      putFloat(buff, frontMaterial.getSpecular(), 3);
      buff.putFloat (frontMaterial.getShininess());
      buff.position (getByteOffset(FRONT_EMISSION));
      putFloat (buff, frontMaterial.getEmission(), 4); 
      buff.position (getByteOffset(FRONT_POWER));
      putFloat (buff, frontMaterial.getPower(), 4);
      
      buff.position (getByteOffset(BACK_DIFFUSE));
      putFloat(buff, backMaterial.getDiffuse(), 4); // alpha already stored in diffuse
      buff.position (getByteOffset(BACK_SPECULAR));
      putFloat(buff, backMaterial.getSpecular(), 3);
      buff.putFloat (backMaterial.getShininess());
      buff.position (getByteOffset(BACK_EMISSION));
      putFloat (buff, backMaterial.getEmission(), 4); 
      buff.position (getByteOffset(BACK_POWER));
      putFloat (buff, backMaterial.getPower(), 4);
      
      int len = buff.position();
      buff.flip ();
      update(gl, buff, 0, len);

   }
   
   public void updateMaterials(GL3 gl, Material frontMaterial, float[] frontDiffuse, 
      Material backMaterial, float[] backDiffuse) {
      
      // if no back material, use same as front
      if (backMaterial == null) {
         backMaterial = frontMaterial;
      }
      
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
      
      // everything except scale
      ByteBuffer buff = getBuffer();
      
      buff.position (getByteOffset(FRONT_DIFFUSE));
      putFloat(buff, frontDiffuse, 4); // alpha already stored in diffuse
      buff.position (getByteOffset(FRONT_SPECULAR));
      putFloat(buff, frontMaterial.getSpecular(), 3);
      buff.putFloat (frontMaterial.getShininess());
      buff.position (getByteOffset(FRONT_EMISSION));
      putFloat (buff, frontMaterial.getEmission(), 4); 
      buff.position (getByteOffset(FRONT_POWER));
      putFloat (buff, frontMaterial.getPower(), 4);
      
      buff.position (getByteOffset(BACK_DIFFUSE));
      putFloat(buff, backDiffuse, 4); // alpha already stored in diffuse
      buff.position (getByteOffset(BACK_SPECULAR));
      putFloat(buff, backMaterial.getSpecular(), 3);
      buff.putFloat (backMaterial.getShininess());
      buff.position (getByteOffset(BACK_EMISSION));
      putFloat (buff, backMaterial.getEmission(), 4); 
      buff.position (getByteOffset(BACK_POWER));
      putFloat (buff, backMaterial.getPower(), 4);
      
      int len = buff.position();
      buff.flip ();
      update(gl, buff, 0, len);
   }
   
   public void updateColor(GL3 gl, ByteBuffer buff, int cidx) {
      update(gl, buff, getByteOffset(cidx), 4*GLSupport.FLOAT_SIZE);
   }
   
   public void updatePower(GL3 gl, float[] p) {
      ByteBuffer buff = mapBuffer (gl, GL3.GL_WRITE_ONLY);
      buff.position (getByteOffset (FRONT_POWER));
      putFloat (buff, p, 4);
      buff.position (getByteOffset (BACK_POWER));
      putFloat (buff, p, 4);
   }
   
   public void updateColor(GL3 gl, float[] rgba, int cidx) {
      ByteBuffer buff = getBuffer();
      for (int i=0; i<3; ++i) {
         buff.putFloat(rgba[i]);
      }
      if (rgba.length > 3) {
         buff.putFloat(rgba[3]);
      } else {
         buff.putFloat(1.0f);
      }
      buff.flip(); // rewind to beginning
      updateColor(gl, buff, cidx);
   }
   
   private static void putFloat(ByteBuffer buff, float[] in, int len) {
      for (int i=0; i<len; ++i) {
         buff.putFloat (in[i]);
      }
   }
   
   @Override
   public MaterialsUBO acquire () {
      return (MaterialsUBO)super.acquire ();
   }
   
   public static MaterialsUBO generate(GL3 gl, int progId) {
      return new MaterialsUBO(gl, progId);
   }

}
