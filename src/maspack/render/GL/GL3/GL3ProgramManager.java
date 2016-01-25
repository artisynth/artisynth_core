package maspack.render.GL.GL3;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.media.opengl.GL3;

import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix4d;
import maspack.matrix.Plane;
import maspack.matrix.RigidTransform3d;
import maspack.render.Material;
import maspack.render.RenderProps.Shading;
import maspack.render.GL.GLClipPlane;
import maspack.render.GL.GLLight;
import maspack.render.Renderer.ColorInterpolation;
import maspack.render.GL.GL3.GLSLInfo.InstancedRendering;

public class GL3ProgramManager {
   
   static final int UBO_MATRIX_LOCATION = 0;
   static final int UBO_MATERIALS_LOCATION = 1;
   static final int UBO_LIGHTS_LOCATION = 2;
   static final int UBO_CLIPS_LOCATION = 3;

   HashMap<GLSLInfo, Integer> keyToProgramMap;

   // Uniform buffer objects for holding most common info
   MatricesUBO matricesUBO = null;
   MaterialsUBO materialsUBO = null;
   LightsUBO lightsUBO = null;
   ClipPlanesUBO clipsUBO = null;

   private int numLights;
   private int numClipPlanes;

   public GL3ProgramManager() {
      keyToProgramMap = new HashMap<>();
      numLights = 0;
      numClipPlanes = 0;
   }
   
   private void clearMatrices(GL3 gl) {
      if (matricesUBO != null) {
         matricesUBO.dispose(gl);
         matricesUBO = null;
      }
   }
   
   private void clearMaterials(GL3 gl) {
      if (materialsUBO != null) {
         materialsUBO.dispose(gl);
         materialsUBO = null;
      }
   }
   
   private void clearLights(GL3 gl) {
      if (lightsUBO != null) {
         lightsUBO.dispose(gl);
         lightsUBO = null;
      }
   }
   
   private void clearClips(GL3 gl) {
      if (clipsUBO != null) {
         clipsUBO.dispose(gl);
         clipsUBO = null;
      }
   }

   private void clearUBOs(GL3 gl) {
      clearMatrices(gl);
      clearMaterials(gl);
      clearLights(gl);
      clearClips(gl);
   }

   private void clearPrograms(GL3 gl) {
      // delete all programs (will trigger a re-create)
      for (Entry<GLSLInfo, Integer> entry : keyToProgramMap.entrySet()) {
         int progId = entry.getValue().intValue();
         gl.glDeleteProgram(progId);
      }
      keyToProgramMap.clear();
   }

   private void clearAll(GL3 gl) {
      clearUBOs(gl);
      clearPrograms(gl);
   }
   
   protected void setNumLights(GL3 gl, int nLights) {
      if (nLights != numLights) {
         numLights = nLights;
         clearAll(gl);
      }
   }

   protected void setNumClipPlanes(GL3 gl, int nClips) {
      if (nClips != numClipPlanes) {
         numClipPlanes = nClips;
         clearAll(gl);
      }
   }
  
   private int createProgram(GL3 gl, GLSLInfo key) {

      // generate source
      String[] shaders = GLSLGenerator.getShaderScripts(key);

      int vs = gl.glCreateShader(GL3.GL_VERTEX_SHADER);
      gl.glShaderSource(vs,  1,  new String[]{shaders[0]}, null, 0);
      gl.glCompileShader(vs);
      boolean success =  glCheckShaderCompilation(gl, vs);
      if (!success) {
         throw new RuntimeException("Vertex shader compilation failed.\n" + shaders[0]);
      }

      int fs = gl.glCreateShader(GL3.GL_FRAGMENT_SHADER);
      gl.glShaderSource(fs, 1, new String[]{shaders[1]}, null, 0);
      gl.glCompileShader(fs);
      success =  glCheckShaderCompilation(gl, fs);
      if (!success) {
         throw new RuntimeException("Fragment shader compilation failed.\n" + shaders[1]);
      }

      int prog = gl.glCreateProgram();
      gl.glAttachShader(prog, vs);
      
      // bind locations XXX at some point, might need to query instead
      gl.glBindAttribLocation(prog, GL3VertexAttribute.VERTEX_POSITION.index(), GL3VertexAttribute.VERTEX_POSITION.name());
      if (key.hasVertexNormals() && key.getShading() != Shading.NONE) {
         gl.glBindAttribLocation(prog, GL3VertexAttribute.VERTEX_NORMAL.index(), GL3VertexAttribute.VERTEX_NORMAL.name());
      }
      if (key.hasVertexColors() && key.getColorInterpolation() != ColorInterpolation.NONE) {
         gl.glBindAttribLocation(prog, GL3VertexAttribute.VERTEX_COLOR.index(), GL3VertexAttribute.VERTEX_COLOR.name());  
      }
      if (key.hasVertexTextures()) {
         gl.glBindAttribLocation(prog, GL3VertexAttribute.VERTEX_TEXTURE.index(), GL3VertexAttribute.VERTEX_TEXTURE.name());  
      }
      
      // required
      switch (key.getInstancedRendering()) {
         case POINTS:
            gl.glBindAttribLocation(prog, GL3VertexAttribute.INSTANCE_SCALE.index(), GL3VertexAttribute.INSTANCE_SCALE.name());
            gl.glBindAttribLocation(prog, GL3VertexAttribute.INSTANCE_POSITION.index(), GL3VertexAttribute.INSTANCE_POSITION.name());
            break;
         case FRAMES:
            gl.glBindAttribLocation(prog, GL3VertexAttribute.INSTANCE_SCALE.index(), GL3VertexAttribute.INSTANCE_SCALE.name());
            gl.glBindAttribLocation(prog, GL3VertexAttribute.INSTANCE_POSITION.index(), GL3VertexAttribute.INSTANCE_POSITION.name());
            gl.glBindAttribLocation(prog, GL3VertexAttribute.INSTANCE_ORIENTATION.index(), GL3VertexAttribute.INSTANCE_ORIENTATION.name());
            break;
         case AFFINES:
            gl.glBindAttribLocation(prog, GL3VertexAttribute.INSTANCE_AFFINE_MATRIX.index(), GL3VertexAttribute.INSTANCE_AFFINE_MATRIX.name());
            gl.glBindAttribLocation(prog, GL3VertexAttribute.INSTANCE_NORMAL_MATRIX.index(), GL3VertexAttribute.INSTANCE_NORMAL_MATRIX.name());
            break;
         case LINES:
            gl.glBindAttribLocation(prog, GL3VertexAttribute.LINE_RADIUS.index(), GL3VertexAttribute.LINE_RADIUS.name());
            gl.glBindAttribLocation(prog, GL3VertexAttribute.LINE_BOTTOM_POSITION.index(), GL3VertexAttribute.LINE_BOTTOM_POSITION.name());
            gl.glBindAttribLocation(prog, GL3VertexAttribute.LINE_TOP_POSITION.index(), GL3VertexAttribute.LINE_TOP_POSITION.name());
            break;
         case NONE:
            break;
      }
      
      // optional
      switch (key.getInstancedRendering()) {
         case POINTS:
         case FRAMES:
         case AFFINES:
            if (key.getColorInterpolation() != ColorInterpolation.NONE && key.hasInstanceColors()) {
               gl.glBindAttribLocation(prog, GL3VertexAttribute.INSTANCE_COLOR.index(), GL3VertexAttribute.INSTANCE_COLOR.name());
            }
            if (key.hasInstanceTextures()) {
               gl.glBindAttribLocation(prog, GL3VertexAttribute.INSTANCE_TEXTURE.index(), GL3VertexAttribute.INSTANCE_TEXTURE.name());
            }
            break;
         case LINES:
            if (key.hasLineLengthOffset()) {
               gl.glBindAttribLocation(prog, GL3VertexAttribute.LINE_LENGTH_OFFSET.index(), GL3VertexAttribute.LINE_LENGTH_OFFSET.name());
            }
            if (key.getColorInterpolation() != ColorInterpolation.NONE && key.hasLineColors()) {
               gl.glBindAttribLocation(prog, GL3VertexAttribute.LINE_BOTTOM_COLOR.index(), GL3VertexAttribute.LINE_BOTTOM_COLOR.name());
               gl.glBindAttribLocation(prog, GL3VertexAttribute.LINE_TOP_COLOR.index(), GL3VertexAttribute.LINE_TOP_COLOR.name());
            }
            if (key.hasLineTextures()) {
               gl.glBindAttribLocation(prog, GL3VertexAttribute.LINE_BOTTOM_TEXTURE.index(), GL3VertexAttribute.LINE_BOTTOM_TEXTURE.name());
               gl.glBindAttribLocation(prog, GL3VertexAttribute.LINE_TOP_TEXTURE.index(), GL3VertexAttribute.LINE_TOP_TEXTURE.name());
            }
            break;
         case NONE:
            break;
      }
      
      gl.glAttachShader(prog, fs);
      gl.glLinkProgram(prog);

      // clean up shaders... will be deleted when program is complete
      gl.glDetachShader(prog, vs);
      gl.glDeleteShader(vs);
      gl.glDetachShader(prog, fs);
      gl.glDeleteShader(fs);
      
      //      System.out.println("Program: " + prog);
      //      System.out.println(shaders[0]);
      //      System.out.println(shaders[1]);
      
      return prog;
   }
   
   public void bindUBOs(GL3 gl, int prog, GLSLInfo key) {
      
      // generate UBOs if not exist
      // matrices
      if (matricesUBO == null) {
         matricesUBO = new MatricesUBO(gl, prog);
      }
      matricesUBO.bindLocation(gl, prog, UBO_MATRIX_LOCATION);
      
      // materials
      if (materialsUBO == null) {
         materialsUBO = new MaterialsUBO(gl, prog);
      }
      materialsUBO.bindLocation(gl, prog, UBO_MATERIALS_LOCATION);
      
      // lights
      if (numLights > 0 && key.getShading() != Shading.NONE) {
         if (lightsUBO == null) {
            lightsUBO = new LightsUBO(gl, prog, numLights);
         }
         lightsUBO.bindLocation(gl, prog, UBO_LIGHTS_LOCATION);
      }
      
      // clip planes
      if (numClipPlanes > 0) {
         if (clipsUBO == null) {
            clipsUBO = new ClipPlanesUBO(gl, prog, numClipPlanes);
         }
         clipsUBO.bindLocation(gl, prog, UBO_CLIPS_LOCATION);
      }
   }
   
   boolean glCheckShaderCompilation (GL3 gl, int shader) {
      int[] buff = new int[2];
      gl.glGetShaderiv(shader, GL3.GL_COMPILE_STATUS, buff, 0);
      if(buff[0] == GL3.GL_FALSE) {
         gl.glGetShaderiv(shader, GL3.GL_INFO_LOG_LENGTH, buff, 0);
         int maxLength = buff[0];
         byte[] log = new byte[maxLength];
         int[] logLength = new int[] {maxLength};
         gl.glGetShaderInfoLog(shader, maxLength, logLength, 0, log,0);

         // Provide the infolog in whatever manor you deem best.
         String err = new String(log);
         System.err.println(err);

         gl.glDeleteShader(shader); // Don't leak the shader.
         return false;
      }

      return true;
   }

   public int getProgram(GL3 gl, GLSLInfo key) {
      Integer progId = keyToProgramMap.get(key);
      if (progId == null) {
         progId = createAndBindProgram(gl, key);
      }
      return progId.intValue();
   }
   
   private int createAndBindProgram(GL3 gl, GLSLInfo key) {
      int progId = createProgram(gl, key);
      bindUBOs(gl, progId, key);
      keyToProgramMap.put(key, progId);
      return progId;
   }
   
   /**
    * Creates a program containing all uniforms, for binding purposes
    * @param gl
    */
   protected void createDefaultProgram(GL3 gl) {
      GLSLInfo key = new GLSLInfo(numLights, numClipPlanes, Shading.PHONG, 
         ColorInterpolation.NONE, true, false, false, 
         InstancedRendering.NONE, false, false, false, false, false);
      createAndBindProgram(gl, key);
   }

   public void dispose(GL3 gl) {
      clearAll(gl);
   }
   
   public void init(GL3 gl, int nLights, int nClipPlanes) {
      setNumLights(gl, nLights);
      setNumClipPlanes(gl, nClipPlanes);
      createDefaultProgram(gl);
   }
   
   public void reconfigure(GL3 gl, int nLights, int nClipPlanes) {
      if (nLights != numLights || nClipPlanes != numClipPlanes) {
         init(gl, nLights, nClipPlanes);
      }
   }
   
   public int numLights() {
      return numLights;
   }
   
   public int numClipPlanes() {
      return numClipPlanes;
   }
   
   public void setMatrices(GL3 gl, Matrix4d projection, RigidTransform3d view, 
      AffineTransform3dBase model, Matrix3d modelNormal) {
      matricesUBO.updateMatrices(gl, projection, view, model, modelNormal);
   }
   
   public void setMaterials(GL3 gl, Material frontMaterial, Material backMaterial) {
      materialsUBO.updateMaterials(gl, frontMaterial, backMaterial);
   }
   
   public void setMaterials(GL3 gl, Material frontMaterial, float[] frontDiffuse, 
      Material backMaterial, float[] backDiffuse) {
      materialsUBO.updateMaterials(gl, frontMaterial, frontDiffuse, 
         backMaterial, backDiffuse);
   }
   
   public void setMaterialDiffuse(GL3 gl, float[] frontRgba, float[] backRgba) {
      materialsUBO.updateColor(gl, frontRgba, MaterialsUBO.FRONT_DIFFUSE);
      materialsUBO.updateColor(gl, backRgba, MaterialsUBO.BACK_DIFFUSE);
   }
   
   public void setLights(GL3 gl, List<GLLight> lights, float intensityScale, RigidTransform3d viewMatrix) {
      lightsUBO.updateLights(gl, lights, intensityScale, viewMatrix);
   }
   
   public void setClipPlanes(GL3 gl, Plane[] clips) {
      clipsUBO.updateClipPlanes(gl, clips);
   }
   
   public int setClipPlanes(GL3 gl, GLClipPlane[] clips) {
      return clipsUBO.updateClipPlanes(gl, clips);
   }
   
   public int setClipPlanes(GL3 gl, List<GLClipPlane> clips) {
      return clipsUBO.updateClipPlanes(gl, clips);
   }
   
   

}
