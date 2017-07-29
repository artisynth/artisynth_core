package maspack.render.GL.GL3;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import maspack.matrix.AffineTransform2dBase;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix4d;
import maspack.matrix.Plane;
import maspack.matrix.RigidTransform2d;
import maspack.matrix.RigidTransform3d;
import maspack.render.Light;
import maspack.render.Material;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.Shading;
import maspack.render.GL.GLClipPlane;
import maspack.render.GL.GLProgramInfo;
import maspack.render.GL.GLProgramInfo.RenderingMode;
import maspack.render.GL.GLShaderProgram;
import maspack.render.GL.GLTexture;
import maspack.render.GL.GL3.GLSLGenerator.StringIntPair;

public class GL3ProgramManager {

   public static boolean debug = true;

   HashMap<String,Integer> attributeLocationMap;
   HashMap<String,Integer> textureLocationMap;
   HashMap<String,Integer> uniformBlockLocationMap;
   HashMap<String,UniformBufferObject> uniformBufferObjectMap;

   HashMap<Object, GLShaderProgram> keyToProgramMap;

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
      attributeLocationMap = new HashMap<>();
      textureLocationMap = new HashMap<> ();
      uniformBlockLocationMap = new HashMap<> ();
      uniformBufferObjectMap = new HashMap<>();

      // make sure all attributes from the GLSLGenerator are numbered
      for (StringIntPair attr : GLSLGenerator.ATTRIBUTES) {
         addSharedAttribute (attr.getString (), attr.getInt ()); 
      }
      for (StringIntPair tex : GLSLGenerator.TEXTURES) {
         addSharedTexture (tex.getString (), tex.getInt ()); 
      }
      for (StringIntPair ublock : GLSLGenerator.UNIFORM_BLOCKS) {
         addSharedUniformBlock (ublock.getString (), ublock.getInt ()); 
      }
   }

   public void addSharedAttribute(String attribute, int location) {
      attributeLocationMap.put (attribute, location);
   }

   public void addSharedTexture(String textureName, int location) {
      textureLocationMap.put (textureName, location);
   }

   public void addSharedUniformBlock(String blockName, int location) {
      uniformBlockLocationMap.put (blockName, location);
   }

   public void addSharedUniformBufferObject(UniformBufferObject ubo) {
      uniformBufferObjectMap.put (ubo.getBlockName (), ubo);
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
      for (Entry<Object, GLShaderProgram> entry : keyToProgramMap.entrySet()) {
         int progId = entry.getValue().getId ();
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

   private GLShaderProgram createProgram(GL3 gl, GLProgramInfo key) {
      // generate source
      String[] shaders = GLSLGenerator.getShaderScripts(key);
      //      Logger logger = Logger.getSystemLogger();
      //      logger.info(shaders[0]);
      //      logger.info(shaders[1]);
      return createProgram(gl, shaders);
   }

   private GLShaderProgram createProgram(GL3 gl, String[] shaders) {

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
         // shaders = GLSLGenerator.getShaderScripts(key);
         throw new RuntimeException("Fragment shader compilation failed.\n" + shaders[1]);
      }

      int progId = gl.glCreateProgram();
      gl.glAttachShader(progId, vs);
      gl.glAttachShader(progId, fs);

      // bind attributes
      for (Entry<String,Integer> attribute : attributeLocationMap.entrySet ()) {
         if (shaders[0].contains (attribute.getKey ())) {
            gl.glBindAttribLocation (progId, attribute.getValue (), attribute.getKey ());
         }
      }

      gl.glLinkProgram(progId);
      success = glCheckProgramLinking(gl, progId);
      if (!success) {
         throw new RuntimeException("Program failed to link.\n");
      }

      GLShaderProgram prog = new GLShaderProgram (progId);
      bindUBOs (gl, prog);
      prog.use (gl);
      bindTextures(gl, prog);

      // clean up shaders... will be deleted when program is complete
      gl.glDetachShader(progId, vs);
      gl.glDeleteShader(vs);
      gl.glDetachShader(progId, fs);
      gl.glDeleteShader(fs);

      return prog;
   }

   public int getSharedAttributeLocation(String name) {
      Integer loc = attributeLocationMap.get (name);
      if (loc == null) {
         loc = -1;
      }
      return loc;
   }

   public int getSharedTextureLocation(String name) {
      Integer loc = textureLocationMap.get (name);
      if (loc == null) {
         loc = -1;
      }
      return loc;
   }

   public int getUniformBlockLocation(String name) {
      Integer loc = uniformBlockLocationMap.get (name);
      if (loc == null) {
         loc = -1;
      }
      return loc;
   }

   private void bindUBOs(GL3 gl, GLShaderProgram prog) {

      // generate UBOs if not exist
      // matrices
      if (matricesUBO == null) {
         matricesUBO = MatricesUBO.generate(gl, prog.getId());
      }
      matricesUBO.bindLocation(gl, prog.getId(), getUniformBlockLocation (matricesUBO.getBlockName()));

      // materials
      if (materialsUBO == null) {
         materialsUBO = MaterialsUBO.generate(gl, prog.getId());
      }
      materialsUBO.bindLocation(gl, prog.getId (), getUniformBlockLocation (materialsUBO.getBlockName()));

      // lights
      if (numLights > 0 ) { //&& key.getShading() != Shading.NONE) {
         if (lightsUBO == null) {
            lightsUBO = LightsUBO.generate (gl, prog.getId (), numLights);
         }
         lightsUBO.bindLocation(gl, prog.getId (), getUniformBlockLocation (lightsUBO.getBlockName()));
      }

      // clip planes
      if (numClipPlanes > 0) {
         if (clipsUBO == null) {
            clipsUBO = ClipPlanesUBO.generate(gl, prog.getId (), numClipPlanes);
         }
         clipsUBO.bindLocation(gl, prog.getId (), getUniformBlockLocation (clipsUBO.getBlockName()));
      }

      for (UniformBufferObject ubo : uniformBufferObjectMap.values ()) {
         ubo.bindLocation (gl, prog.getId (), getUniformBlockLocation (ubo.getBlockName ()));
      }
   }

   private void bindTextures(GL3 gl, GLShaderProgram prog) {
      for (Entry<String,Integer> tex : textureLocationMap.entrySet ()) {
         setUniform (gl, prog, tex.getKey (), tex.getValue ());
      }
   }

   private boolean glCheckShaderCompilation (GL3 gl, int shader) {
      
      int[] buff = new int[2];
      gl.glGetShaderiv(shader, GL3.GL_COMPILE_STATUS, buff, 0);
      gl.glGetShaderiv(shader, GL3.GL_INFO_LOG_LENGTH, buff, 1);
      if(buff[1] > 1) {
         int maxLength = buff[1];
         byte[] log = new byte[maxLength];
         int[] logLength = new int[] {maxLength};
         gl.glGetShaderInfoLog(shader, maxLength, logLength, 0, log, 0);

         String err = new String(log);
         if (err.length() > 0 && (int)err.charAt(0) != 0) {
            System.err.println(err);
         }
         if (buff[0] == GL3.GL_FALSE) {
            gl.glDeleteShader(shader);
            return false;
         }
      }

      return true;
   }
   
   private boolean glCheckProgramLinking (GL3 gl, int program) {
      
      int[] buff = new int[2];
      gl.glGetProgramiv(program, GL3.GL_LINK_STATUS, buff, 0);
      gl.glGetProgramiv(program, GL3.GL_INFO_LOG_LENGTH, buff, 1);
      if(buff[1] > 1) {
         int maxLength = buff[1];
         byte[] log = new byte[maxLength];
         int[] logLength = new int[] {maxLength};
         gl.glGetProgramInfoLog(program, maxLength, logLength, 0, log, 0);

         String err = new String(log);
         if (err.length() > 0 && (int)err.charAt(0) != 0) {
            System.err.println(err);
         }
         if (buff[0] == GL3.GL_FALSE) {
            gl.glDeleteProgram (program);
            return false;
         }
      }

      return true;
   }
   
   public GLShaderProgram getSelectionProgram(GL3 gl, GLProgramInfo info) {
      GLProgramInfo select = new GLProgramInfo();  // basic flat program
      
      // use rounded points by default
      select.setSelecting (true);
      RenderingMode mode = info.getMode ();
      if (mode == RenderingMode.POINTS) {
         select.setRoundPointsEnabled (info.hasRoundPoints ());
      }
      select.setMode (mode);
      select.setNumLights (0);
      select.setNumClipPlanes (numClipPlanes);
      
      // disable everything else
      select.setVertexColorsEnabled (false);
      select.setVertexNormalsEnabled (false);
      select.setVertexTexturesEnabled (false);
      select.setInstanceColorsEnabled (false);
      select.setLineColorsEnabled (false);
      select.setShading (Shading.NONE);
      
      return getProgram(gl, select);
   }

   public GLShaderProgram getProgram(GL3 gl, GLProgramInfo key) {
      GLShaderProgram prog = keyToProgramMap.get(key);
      if (prog == null) {
         prog = createAndBindProgram(gl, key);
      }
      return prog;
   }

   public GLShaderProgram getProgram(GL3 gl, Object key, String[] shaders) {
      GLShaderProgram prog = keyToProgramMap.get(key);
      if (prog == null) {
         prog = createAndBindProgram(gl, key, shaders);
      }
      return prog;
   }

   static String readFile(String path, Charset encoding) {
      String out = null;
      try {
         byte[] encoded = Files.readAllBytes(Paths.get(path));
         out = new String(encoded, encoding);
      }
      catch (IOException e) {
      }
      return out;
   }

   public GLShaderProgram getProgram(GL3 gl, Object key) {
      return keyToProgramMap.get(key);
   }
   
   public GLShaderProgram getProgram(GL3 gl, Object key, File[] shaders) {
      GLShaderProgram prog = keyToProgramMap.get(key);
      if (prog == null) {
         String[] str = new String[shaders.length];
         for (int i=0; i<shaders.length; ++i) {
            str[i] = readFile(shaders[i].getAbsolutePath (), Charset.defaultCharset());
         }
         prog = createAndBindProgram(gl, key, str);
      }
      return prog;
   }

   private GLShaderProgram createAndBindProgram(GL3 gl, GLProgramInfo key) {
      GLShaderProgram prog = createProgram(gl, key);
      keyToProgramMap.put(key.clone (), prog);  // immutable copy of key into map
      return prog;
   }

   /**
    * Unsure that the supplied key is either a protected copy or immutable,
    * as it will be placed in a map
    * @param gl
    * @param key
    * @param shaders
    * @return shader program
    */
   private GLShaderProgram createAndBindProgram(GL3 gl, Object key, String[] shaders) {
      GLShaderProgram prog = createProgram(gl, shaders);
      keyToProgramMap.put(key, prog);
      return prog;
   }


   /**
    * Creates a program containing all uniforms, for binding purposes
    * @param gl
    */
   protected void createDefaultProgram(GL3 gl) {
      GLProgramInfo info = new GLProgramInfo();
      info.setNumLights (numLights);
      info.setNumClipPlanes (numClipPlanes);
      info.setMode (RenderingMode.DEFAULT);
      info.setShading (Shading.SMOOTH);
      info.setVertexNormalsEnabled (true);
      info.setVertexColorsEnabled (true);
      info.setVertexTexturesEnabled (true);
      info.setVertexColorMixing (ColorMixing.REPLACE);
      info.setTextureColorMixing (ColorMixing.MODULATE);
      createAndBindProgram(gl, info);
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
      setMatrices(gl, projection, view, model, modelNormal, RigidTransform2d.IDENTITY);
   }

   public void setMatrices(GL3 gl, Matrix4d projection, RigidTransform3d view, 
      AffineTransform3dBase model, Matrix3d modelNormal,
      AffineTransform2dBase texture) {
      matricesUBO.updateMatrices(gl, projection, view, model, 
         modelNormal, texture);
   }

   public void setMaterials(GL3 gl, Material frontMaterial, Material backMaterial) {
      materialsUBO.setMaterials(gl, frontMaterial, backMaterial);
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

   public void setUniform(GL3 gl, GLShaderProgram prog, String name, int val) {
      int loc = prog.getUniformLocation (gl, name);
      if (loc >= 0) {
         gl.glUniform1i (loc, val);
      }
   }

   public void setUniform(GL3 gl, GLShaderProgram prog, String name, float val) {
      int loc = prog.getUniformLocation (gl, name);
      if (loc >= 0) {
         gl.glUniform1f (loc, val);
      }
   }

   public void setUniform4f(GL3 gl, GLShaderProgram prog, String name, float[] v) {
      int loc = prog.getUniformLocation (gl, name);
      if (loc >= 0) {
         gl.glUniform4fv (loc, 1, v, 0);
      }
   }
   
   public void setSelectionColor(GL3 gl, GLShaderProgram prog, float[] rgba) {
      int loc = prog.getUniformLocation (gl, "selection_color");
      if (loc >= 0) {
         gl.glUniform4fv (loc, 1, rgba, 0);
      }
   }

   public void setLights(GL3 gl, List<Light> lights, float intensityScale, RigidTransform3d viewMatrix) {
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

   public void bindTexture(GL3 gl, String name, GLTexture tex) {
      int loc = getSharedTextureLocation (name);
      if (loc >= 0) {
         gl.glActiveTexture (GL.GL_TEXTURE0+loc);
         tex.bind (gl);
      }
   }
   
   public void activateTexture(GL3 gl, String name) {
      int loc = getSharedTextureLocation (name);
      if (loc >= 0) {
         gl.glActiveTexture (GL.GL_TEXTURE0+loc);
      }
   }

   public void unbindTexture(GL3 gl, String name, GLTexture tex) {
      int loc = getSharedTextureLocation (name);
      if (loc >= 0) {
         gl.glActiveTexture (GL.GL_TEXTURE0+loc);
         tex.unbind (gl);
      }
   }

}
