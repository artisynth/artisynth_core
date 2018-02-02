package maspack.render.GL.GL3;

import maspack.render.Renderer.ColorInterpolation;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.Shading;
import maspack.render.GL.GLProgramInfo;
import maspack.render.GL.GLProgramInfo.RenderingMode;


public class GLSLGenerator {

   static String VERSION_STRING = "#version 330";
   static String UNIFORM_LAYOUT = "std140";  // std140 or "shared"

   public static class StringIntPair {
      String str;
      int i;
      public StringIntPair(String str, int i) {
         this.str = str;
         this.i = i;
      }
      public String getString() {
         return str;
      }
      public int getInt() {
         return i;
      }
   }
   
   /**
    * Set of built-in attributes, with a hint for binding locations 
    */
   public static StringIntPair[] ATTRIBUTES = {
      // vertices
      new StringIntPair ("vertex_position", 0),
      new StringIntPair ("vertex_normal", 1),
      new StringIntPair ("vertex_color", 2),
      new StringIntPair ("vertex_texcoord", 3),
      // instance (point, rigid, affine)
      new StringIntPair ("instance_scale", 4),
      new StringIntPair ("instance_color", 5),
      // instance (point, rigid)
      new StringIntPair ("instance_position", 6),
      // instance (rigid)
      new StringIntPair ("instance_orientiation", 7),
      // instance (affine)
      new StringIntPair ("instance_affine_matrix", 6),
      new StringIntPair ("instance_normal_matrix", 10),
      // instance (line)
      new StringIntPair ("line_radius", 4),
      new StringIntPair ("line_bottom_position", 5),
      new StringIntPair ("line_top_position", 6),
      new StringIntPair ("line_bottom_color", 7),
      new StringIntPair ("line_top_color", 8),
      new StringIntPair ("line_bottom_scale_offset", 9),
      new StringIntPair ("line_top_scale_offset", 10)
   };
   
   /**
    * Set of built-in textures, with a hint for binding locations 
    */
   public static final StringIntPair[] TEXTURES = {
      new StringIntPair("color_map", 0),
      new StringIntPair("normal_map", 1),
      new StringIntPair("bump_map", 2)
   };
   
   public static final StringIntPair[] UNIFORMS = {
      new StringIntPair("selection_color", 0)
   };
   
   public static final StringIntPair[] UNIFORM_BLOCKS = {
       new StringIntPair("Materials", 0),
       new StringIntPair("Matrices", 1),
       new StringIntPair("Lights", 2),
       new StringIntPair("ClipPlanes", 3)
   };
   
   private static final int VERTEX_SHADER = 0;
   private static final int FRAGMENT_SHADER = 1;
   private static final int SHADER_GLOBALS = 0;
   private static final int SHADER_FUNCTIONS = 1;
   private static final int SHADER_MAIN = 2;

   //==========================================================
   // Program Structures
   //==========================================================
   // // material properties
   // struct Material {
   //    vec4 diffuse;   // alpha is diffuse.a
   //    vec4 specular;  // shininess is specular.a
   //    vec4 emission;  
   //    vec4 power;     // scale factor for ambient/diffuse/specular/emission
   // };
   //
   // // light source info, in camera space
   // struct LightSource {
   //    vec4 diffuse;
   //    vec4 ambient;
   //    vec4 specular;
   //    vec4 position;     // position.w indicates point vs directional 
   //    vec4 direction;    // (dir, spot cutoff[>-1 -> spotlight])
   //    vec4 attenuation;  // (spot exponent, constant, linear, quadratic)
   // };
   //
   // struct ClipPlane {
   //    vec4 plane;        // ax+by+cz+d=0
   // };
   //==========================================================
   // Program Uniforms:
   //==========================================================
   //----------------------------------------------------------   
   // Common
   //----------------------------------------------------------
   // // PVM matrices
   // layout (std140) uniform Matrices {
   //    mat4 pvm_matrix;     // model to screen
   //    mat4 vm_matrix;      // model to viewer
   //    mat4 m_matrix;       // model to world
   //    mat4 normal_matrix;  // model to viewer normal
   //    mat4 texture_matrix; // texture coordinates scale
   // };
   //
   // // lights
   // layout (std140) uniform Lights {
   //    LightSource light[?];
   //    float intensity_scale; // for HDR->LDR, initialized to one
   // };
   //
   // // clipping planes
   // layout (std140) uniform ClipPlanes {
   //    ClipPlane clip_plane[?];    // v.x*x+v.y*y+v.z*z+v.w=0
   // };
   // 
   //----------------------------------------------------------   
   // Per-object
   //----------------------------------------------------------
   // // texture info
   // uniform sampler2D color_map;
   // uniform sampler2D normal_map;
   // uniform normal_scale normal_map; // normal perturbation scale
   // uniform sampler2D bump_map;
   // uniform bump_scale;  // bump scale
   //
   // // per-object materials
   // layout (std140) uniform Materials {
   //    Material front_material;
   //    Material back_material;
   // };
   // 
   // // per-object selection color
   // uniform vec4 selection_color;

   //==========================================================
   // Program Inputs:
   //==========================================================
   // // vertex inputs
   // in vec3 vertex_position;
   // in vec3 vertex_normal;
   // in vec4 vertex_color;
   // 
   // // texture info
   // in vec2 vertex_texcoord;
   // 
   // // normal info
   // 
   // // point instance information
   // in vec3  instance_position;
   // in vec4  instance_color;
   // in float instance_scale;
   // in vec4  instance_orientation;   // quaternion
   // 
   // // affine instance information
   // in mat4 instance_affine_matrix;
   // in mat4 instance_normal_matrix;  // inverse transpose
   //
   // // line instance information
   // in float line_radius
   // in vec3  line_bottom_position;
   // in vec3  line_top_position;
   // in vec4  line_bottom_color;
   // in vec4  line_top_color;
   // in vec4  line_bottom_scale_offset; // (radius, boff, toff, w): scales s.t. v=normalize(line_top_position-line_bottom_position)
   // in vec4  line_top_scale_offset;    //     pos = (bottom + boff*v)*(1-w) + (top + toff*v)*w

   public static String[] getShaderScripts(GLProgramInfo info) {

      StringBuilder[][] shaders = new StringBuilder[2][3];
      for (int i=0; i<shaders.length; ++i) {
         for (int j=0; j<shaders[i].length; ++j) {
            shaders[i][j] = new StringBuilder();
         }
      }

      // matrices to vertex shader
      buildVertexShader(shaders[VERTEX_SHADER], info);
      buildFragmentShader(shaders[FRAGMENT_SHADER], info);


      // gather into shaders
      String[] out = new String[2];
      for (int i=0; i<shaders.length; ++i) {
         out[i] = VERSION_STRING + "\n\n"
         + shaders[i][SHADER_GLOBALS].toString()
         + shaders[i][SHADER_FUNCTIONS].toString()
         + shaders[i][SHADER_MAIN].toString();
      }
      
      return out;

   }

   private static void buildVertexShader(StringBuilder[] sbs, GLProgramInfo info) {
      buildVertexShaderHeader(sbs[SHADER_GLOBALS], info);
      buildVertexShaderFunctions(sbs[SHADER_FUNCTIONS], info);
      buildVertexShaderMain(sbs[SHADER_MAIN], info);
   }

   private static void buildVertexShaderHeader(StringBuilder hb, GLProgramInfo info) {
      addVertexInfo(hb, info);
      if (!info.isSelecting ()) {
         addVertexLighting(hb, info.getShading(), info.numLights());
      }
      addVertexClipping(hb, info.numClipPlanes());
   }

   private static void buildVertexShaderFunctions(StringBuilder fb, GLProgramInfo info) {
      if (info.getShading() == Shading.FLAT /*|| info.getShading() == Shading.GOURAUD*/) {
         addBlinnPhong(fb);
      }

      if (info.getColorInterpolation() == ColorInterpolation.HSV) {
         addRGBtoHSV(fb);
      }

      switch (info.getMode()) {
         case INSTANCED_LINES: {
            addRodriguesRotation(fb);
            break;
         }
         case INSTANCED_FRAMES: {
            addQuaternionRotation(fb);
            break;
         }
         case DEFAULT:
         case POINTS:
         case INSTANCED_POINTS:
         case INSTANCED_AFFINES:
         default:
            // nothing required
            break;
      }

   }
   
   /**
    * Determines if we should add texture properties to the shader
    * @param info program info
    * @return true if texture content should be added
    */
   private static boolean hasTextures(GLProgramInfo info) {
      return !info.isSelecting () && info.hasVertexTextures() && info.hasTextureMap ();
   }

   private static void buildVertexShaderMain(StringBuilder mb, GLProgramInfo info) {

      appendln(mb, "// main vertex shader");
      appendln(mb, "void main() {");
      appendln(mb);
      appendln(mb, "   vec3 position;  // transformed vertex position");
      if (info.hasVertexNormals() && info.getShading() != Shading.NONE) {
         appendln(mb, "   vec3 normal;  // transformed vertex normal");
      }
      appendln(mb);

      RenderingMode mode = info.getMode();
      boolean computeNormals = !info.isSelecting () && info.hasVertexNormals() && info.getShading() != Shading.NONE;
      
      // transform vertex using instance info
      switch (mode) {
         case INSTANCED_AFFINES:
            appendln(mb, "   // instance vertex, affine transform");
            appendln(mb, "   position = (instance_affine_matrix *  vec4(instance_scale * vertex_position, 1.0) ).xyz;");
            if (computeNormals) {
               appendln(mb, "   normal = (instance_normal_matrix *  vec4(vertex_normal, 0.0) ).xyz;");
            }
            appendln(mb);
            break;
         case INSTANCED_FRAMES:
            appendln(mb, "   // instance vertex, scale-rotate-translate");
            appendln(mb, "   position = qrot(instance_orientation, (instance_scale * vertex_position)) + instance_position;");
            if (computeNormals) {
               appendln(mb, "   normal = qrot(instance_orientation, vertex_normal);");
            }
            appendln(mb);
            break;
         case INSTANCED_LINES:
            appendln(mb, "   // instance vertex, scale radially, rotate/translate");
            appendln(mb, "   vec3  u = line_top_position-line_bottom_position;");
            appendln(mb, "   float line_length = length(u);  // target length");
            appendln(mb, "   u = u/line_length;              // target direction vector");           
            if (info.hasLineScaleOffset()) {
               appendln(mb, "   float line_rad = line_radius*mix(line_bottom_scale_offset.r, line_top_scale_offset.r, vertex_position.z);  // adjust radius");
               appendln(mb, "   float line_offset = mix(line_bottom_scale_offset.y, line_length+line_bottom_scale_offset.z, line_bottom_scale_offset.w);");
               appendln(mb, "   float line_top = mix(line_top_scale_offset.y, line_length+line_top_scale_offset.z, line_top_scale_offset.w);");
               appendln(mb, "   line_length = line_top-line_offset;");
            } else {
               appendln(mb, "   float line_rad = line_radius;  // radius length");   
            }
            appendln(mb);
            appendln(mb, "   // transform position");
            appendln(mb, "   position = vec3(line_rad*vertex_position.xy, line_length*vertex_position.z);");
            appendln(mb, "   position = line_bottom_position + zrot(u, position);");
            if (info.hasLineScaleOffset ()) {
               appendln(mb, "   position = position + line_offset*u;");
            }
            
            if (computeNormals) {
               appendln(mb, "   // transform normal");
               appendln(mb, "   normal = vec3( vertex_normal.xy*line_length, vertex_normal.z*line_radius);");
               appendln(mb, "   normal = zrot(u, normal);");
               if (info.hasLineScaleOffset ()) {
                  appendln(mb, "   float rdiff = line_bottom_scale_offset.r-line_top_scale_offset.r;");
                  appendln(mb, "   float cost = 1.0/sqrt(rdiff*rdiff+1);");
                  appendln(mb, "   float sint = rdiff*cost;");
                  appendln(mb, "   vec3 trot = vec3(vertex_position.y, -vertex_position.x, 0);");
                  appendln(mb, "   normal = rodrigues(normal, trot, sint, cost);");
               }
            }
            appendln(mb);
            break;
         case INSTANCED_POINTS:
            appendln(mb, "   // instance vertex, scale-translate");
            appendln(mb, "   position = instance_scale * vertex_position + instance_position;");
            if (!info.isSelecting () && info.hasVertexNormals() && info.getShading() != Shading.NONE) {
               appendln(mb, "   normal = vertex_normal;");
            }
            break;
         case DEFAULT:
         case POINTS:
            appendln(mb, "   position = vertex_position;");
            if (computeNormals) {
               appendln(mb, "   normal = vertex_normal;");
            }
            break;
      }

      // at this point, position and normal should be correct, compute output position
      appendln(mb, "   // vertex output");
      appendln(mb, "   gl_Position = pvm_matrix * vec4(position, 1.0);");
      appendln(mb);

      // vertex colors
      ColorInterpolation cinterp = info.getColorInterpolation();
      if (!info.isSelecting () && info.getVertexColorMixing () != ColorMixing.NONE) {
         switch (mode) {
            case INSTANCED_POINTS:
            case INSTANCED_FRAMES:
            case INSTANCED_AFFINES:
               if (info.hasInstanceColors()) {
                  if (cinterp == ColorInterpolation.HSV) {
                     appendln(mb, "   colorOut.diffuse = rgba2hsva(instance_color);");
                  } else {
                     appendln(mb, "   colorOut.diffuse = instance_color;");
                  }
                  appendln(mb);
               } else if (info.hasVertexColors()) {
                  if (cinterp == ColorInterpolation.HSV) {
                     appendln(mb, "   colorOut.diffuse = rgba2hsva(vertex_color);");
                  } else {
                     appendln(mb, "   colorOut.diffuse = vertex_color;");
                  }
                  appendln(mb);
               }
               break;
            case INSTANCED_LINES:
               if (info.hasLineColors()) {
                  appendln(mb, "   // interpolate color based on line");
                  appendln(mb, "   float cz = vertex_position.z;");
                  if (info.hasLineScaleOffset()) {
                     appendln(mb, "   // interpolate as fraction of bottom-to-top");
                     appendln(mb, "   cz = (cz*line_length+line_offset)/(line_length+line_offset);");
                  }
                  if (cinterp == ColorInterpolation.HSV) {
                     appendln(mb, "   colorOut.diffuse = mix(rgba2hsva(line_bottom_color), rgba2hsva(line_top_color), cz);");
                  } else {
                     appendln(mb, "   colorOut.diffuse = mix(line_bottom_color, line_top_color, cz);");
                  }
                  appendln(mb);
               } else if (info.hasVertexColors()) {
                  if (cinterp == ColorInterpolation.HSV) {
                     appendln(mb, "   colorOut.diffuse = rgba2hsva(vertex_color);");
                  } else {
                     appendln(mb, "   colorOut.diffuse = vertex_color;");
                  }
                  appendln(mb);
               }
               break;
            case DEFAULT:
            case POINTS:
               if (info.hasVertexColors()) {
                  if (cinterp == ColorInterpolation.HSV) {
                     appendln(mb, "   colorOut.diffuse = rgba2hsva(vertex_color);");
                  } else {
                     appendln(mb, "   colorOut.diffuse = vertex_color;");
                  }
                  appendln(mb);
               }
               break;
         }
      }
      
      // do lighting computations
      if (!info.isSelecting ()) {
         switch (info.getShading()) {
            case FLAT:
            //case GOURAUD:
               if (info.numLights() > 0) {
                  appendln(mb, "   // per-vertex lighting computations");
                  appendln(mb, "   // compute camera position/normal");
                  appendln(mb, "   vec4 camera_position = vm_matrix * vec4(position, 1.0);");
                  if (info.hasVertexNormals()) {
                     appendln(mb, "   vec4 camera_normal = normal_matrix * vec4(normal, 0.0);");
                  } else {
                     appendln(mb, "   vec4 camera_normal = -camera_position;  // assume pointed at camera");
                  }
                  appendln(mb, "   vec3 nfront = normalize(camera_normal.xyz);");
                  appendln(mb, "   vec3 nback = -nfront;");
                  appendln(mb, "   vec3 eye = normalize(-camera_position.xyz);");
                  appendln(mb, "   ");
                  appendln(mb, "   // accumulated light colors");
                  appendln(mb, "   vec3 fldiff = vec3(0.0);  // front");
                  appendln(mb, "   vec3 flambi = vec3(0.0);");
                  appendln(mb, "   vec3 flspec = vec3(0.0);");
                  appendln(mb, "   vec3 bldiff = vec3(0.0);  // back");
                  appendln(mb, "   vec3 blambi = vec3(0.0);");
                  appendln(mb, "   vec3 blspec = vec3(0.0);");
                  appendln(mb, "   ");
                  appendln(mb, "   // lights");
                  appendln(mb, "   for (int i=0; i<" + info.numLights() + "; ++i) {");
                  appendln(mb, "      vec3  light_to_vertex = camera_position.xyz-light[i].position.xyz;");
                  appendln(mb, "      float lightdist = length(light_to_vertex);");
                  appendln(mb, "      light_to_vertex = light_to_vertex/lightdist;");                  
                  appendln(mb, "      vec3 light_direction = normalize(light[i].direction.xyz);");
                  appendln(mb, "      ");
                  appendln(mb, "      float spotatt = 1.0;  // spot attentuation initially zero if non-spotlight");
                  appendln(mb, "      float coslimit = light[i].direction.w;");
                  appendln(mb, "      if (coslimit > 0) {");
                  appendln(mb, "         // check angle");
                  appendln(mb, "         float coslight = dot(light_direction, light_to_vertex);");
                  appendln(mb, "         if (coslight < coslimit) {");
                  appendln(mb, "            spotatt = 0;");
                  appendln(mb, "         } else {");
                  appendln(mb, "            spotatt = pow(coslight, light[i].attenuation.w);");
                  appendln(mb, "         }");
                  appendln(mb, "      }");
                  appendln(mb, "      ");
                  appendln(mb, "      // distance attenuation doesn't affect directional lights");
                  appendln(mb, "      float att = mix(1.0, 1.0 / (light[i].attenuation.x + light[i].attenuation.y*lightdist +");
                  appendln(mb, "         light[i].attenuation.z*lightdist*lightdist), light[i].position.w);");
                  appendln(mb, "      att *= spotatt;  // combine into a single attenuation parameter");
                  appendln(mb, "      ");
                  appendln(mb, "      // determine direction either from point or direction using direction indicator");
                  appendln(mb, "      light_direction = mix(light_direction, light_to_vertex, light[i].position.w);");
                  appendln(mb, "      vec2 ds = blinnPhongCoeffs( nfront, -light_direction, eye, front_material.specular.a);");
                  appendln(mb, "      flambi += intensity_scale*light[i].ambient.rgb;");
                  appendln(mb, "      fldiff += intensity_scale*att*ds.x*light[i].diffuse.rgb;");
                  appendln(mb, "      flspec += intensity_scale*att*ds.y*light[i].specular.rgb;");
                  appendln(mb);
                  appendln(mb, "      ds = blinnPhongCoeffs( nback, -light_direction, eye, back_material.specular.a);");
                  appendln(mb, "      blambi += intensity_scale*light[i].ambient.rgb;");
                  appendln(mb, "      bldiff += intensity_scale*att*ds.x*light[i].diffuse.rgb;");
                  appendln(mb, "      blspec += intensity_scale*att*ds.y*light[i].specular.rgb;");
                  appendln(mb, "   }");
                  appendln(mb, "   ");
                  appendln(mb, "   // accumulate");
                  appendln(mb, "   lightOut.front_ambient  = flambi;");
                  appendln(mb, "   lightOut.front_diffuse  = fldiff;");
                  appendln(mb, "   lightOut.front_specular = flspec;");
                  appendln(mb, "   lightOut.back_ambient   = blambi;");
                  appendln(mb, "   lightOut.back_diffuse   = bldiff;");
                  appendln(mb, "   lightOut.back_specular  = blspec;");
                  appendln(mb);
               }
               break;
            case SMOOTH:
            case METAL:
               // forward along direction information for vertices
               if (info.numLights() > 0) {
                  appendln(mb, "   // per-fragment lighting info, vertex normal and eye directions");
                  appendln(mb, "   vec4 camera_position = vm_matrix * vec4(position, 1.0);");
                  if (info.hasVertexNormals()) {
                     appendln(mb, "   vec4 camera_normal = normal_matrix * vec4(normal, 0.0);");
                  } else {
                     appendln(mb, "   vec4 camera_normal = -camera_position;  // assume pointed at camera");
                  }
                  appendln(mb, "   surfOut.normal = normalize(camera_normal.xyz);");
                  appendln(mb, "   surfOut.to_eye = -camera_position.xyz;");
                  appendln(mb, "");
               }
               break;
            case NONE:
               break;
   
         }
      }
      
      // textures
      boolean hasTextures = hasTextures(info); 
      if (hasTextures) {
         appendln(mb, "   // forward vertex texture coordinates");
         appendln(mb, "   textureOut.texcoord = (texture_matrix*vec4(vertex_texcoord, 0, 1)).xy;");
         appendln(mb);
      }
      
      
      if (info.numClipPlanes() > 0) {
         appendln(mb, "   // clipping planes, in world coordinates");
         appendln(mb, "   vec4 world_position = m_matrix * vec4(position, 1.0);");
         appendln(mb, "   for (int i=0; i<"+ info.numClipPlanes() + "; ++i) {");
         appendln(mb, "      gl_ClipDistance[i] = dot(world_position, clip_plane[i].plane);");
         appendln(mb, "   }");
         appendln(mb);
      }

      appendln(mb, "}");

   }

   private static void appendln(StringBuilder sb) {
      sb.append('\n');
   }

   private static void appendln(StringBuilder sb, String s) {
      sb.append(s);
      appendln(sb);
   }

   private static void addPVMInput(StringBuilder hb) {
      appendln(hb, "// PVM matrices");
      appendln(hb, "layout ("+UNIFORM_LAYOUT+") uniform Matrices {");
      appendln(hb, "   mat4 pvm_matrix;     // model to screen");
      appendln(hb, "   mat4 vm_matrix;      // model to viewer");
      appendln(hb, "   mat4 m_matrix;       // model to world");
      appendln(hb, "   mat4 normal_matrix;  // model to viewer normal");
      appendln(hb, "   mat4 texture_matrix; // texture coordinate transform");
      appendln(hb, "};");
      appendln(hb);
   }

   private static void addVertexInfo(StringBuilder hb, GLProgramInfo info) {
      addPVMInput(hb);
      addVertexInputs(hb, info);
      addVertexOutputs(hb, info);
   }

   private static void addVertexInputs(StringBuilder hb, GLProgramInfo info) {
      
      appendln(hb, "// vertex inputs");
      appendln(hb, "in vec3 vertex_position;");
      if (!info.isSelecting () && info.hasVertexNormals() && info.getShading() != Shading.NONE) {
         appendln(hb, "in vec3 vertex_normal;");
      }
      if (!info.isSelecting () && info.hasVertexColors() && info.getVertexColorMixing () != ColorMixing.NONE) {
         appendln(hb, "in vec4 vertex_color;");
      }
      if (!info.isSelecting () && info.hasVertexTextures() && info.hasTextureMap ()) {
         appendln(hb, "in vec2 vertex_texcoord;");
      }
      
      switch (info.getMode()) {
         case INSTANCED_POINTS:
            appendln(hb);
            appendln(hb, "// instance inputs");
            appendln(hb, "in float instance_scale;");
            appendln(hb, "in vec3  instance_position;"); 
            if (!info.isSelecting () && info.hasInstanceColors() && info.getVertexColorMixing () != ColorMixing.NONE) {
               appendln(hb, "in vec4  instance_color;");
            }
            break;
         case INSTANCED_FRAMES:
            appendln(hb);
            appendln(hb, "// instance inputs");
            appendln(hb, "in vec3  instance_position;");
            if (!info.isSelecting () && info.hasInstanceColors() && info.getVertexColorMixing () != ColorMixing.NONE) {
               appendln(hb, "in vec4  instance_color;");
            }
            appendln(hb, "in float instance_scale;");
            appendln(hb, "in vec4  instance_orientation;");
            break;
         case INSTANCED_AFFINES:
            appendln(hb);
            appendln(hb, "// instance inputs");
            appendln(hb, "in float instance_scale;");
            appendln(hb, "in mat4  instance_affine_matrix;");
            appendln(hb, "in mat4  instance_normal_matrix;"); 
            if (!info.isSelecting () && info.hasInstanceColors() && info.getVertexColorMixing () != ColorMixing.NONE) {
               appendln(hb, "in vec4  instance_color;");
            }
            break;
         case INSTANCED_LINES:
            appendln(hb);
            appendln(hb, "// line instance inputs");
            appendln(hb, "in float line_radius;");
            appendln(hb, "in vec3  line_bottom_position;");
            appendln(hb, "in vec3  line_top_position;"); 
            if (info.hasLineScaleOffset()) {
               appendln(hb, "in vec4  line_bottom_scale_offset;");
               appendln(hb, "in vec4  line_top_scale_offset;");
            }
            if (!info.isSelecting () && info.hasLineColors() && info.getVertexColorMixing () != ColorMixing.NONE) {
               appendln(hb, "in vec4  line_bottom_color;");
               appendln(hb, "in vec4  line_top_color;");
            }
            break;
         case DEFAULT:
         case POINTS:
            break;
      }
      appendln(hb);
   }

   private static void addVertexOutputs(StringBuilder hb, GLProgramInfo info) {
      
      RenderingMode instanced = info.getMode();
      boolean hasColors = (!info.isSelecting ()) 
                          && (info.getVertexColorMixing () != ColorMixing.NONE)
                          && (info.hasVertexColors()
                             || ((instanced == RenderingMode.INSTANCED_POINTS 
                                  || instanced == RenderingMode.INSTANCED_FRAMES
                                  || instanced == RenderingMode.INSTANCED_AFFINES) && info.hasInstanceColors())
                             || (instanced == RenderingMode.INSTANCED_LINES && info.hasLineColors() ) );
      
      boolean hasTextures = hasTextures(info);
      
      if (hasColors) {
         appendln(hb, "// per-vertex color info");
         appendln(hb, "out ColorData {");
         if (info.getColorInterpolation () == ColorInterpolation.NONE) {
            appendln(hb, "   flat vec4 diffuse;");
         } else {
            appendln(hb, "   vec4 diffuse;");   
         }
         appendln(hb, "} colorOut;");
         appendln(hb);
      }
      
      if (hasTextures) {
         appendln(hb, "// per-vertex texture info");
         appendln(hb, "out TextureData {");
         appendln(hb, "   vec2 texcoord;");
         appendln(hb, "} textureOut;");
         appendln(hb);
      }
   }

   private static void addVertexLighting(StringBuilder hb, 
      Shading shading, int nLights) {
      addVertexLightingUniforms(hb, shading, nLights);
      addVertexLightingOutputs(hb, shading, nLights);
   }

   private static void addVertexLightingUniforms(StringBuilder hb, 
      Shading shading, int nLights) {

      if ( (shading == Shading.FLAT /*|| shading == Shading.GOURAUD*/) && (nLights > 0) ) {
         // we need material info to compute per-vertex colors
         appendMaterialStruct(hb);
         appendln(hb, "// per-object materials");
         appendln(hb, "layout ("+UNIFORM_LAYOUT+") uniform Materials {");
         appendln(hb, "   Material front_material;");
         appendln(hb, "   Material back_material;");
         appendln(hb, "};");
         appendln(hb);

         // we need all light info
         appendLightSourceStruct(hb);
         appendln(hb,"layout ("+UNIFORM_LAYOUT+") uniform Lights {");
         appendln(hb,"   LightSource light[" + nLights + "];");
         appendln(hb,"   float intensity_scale;  // for HDR->LDR, initialized to one");
         appendln(hb,"};");
         appendln(hb);
      }
   }

   private static void addVertexLightingOutputs(StringBuilder hb, Shading shading, 
      int nLights ) {
      // Phong    -> output necessary directions
      // Gouraud, Flat -> per-vertex front and back color
      // None     -> no output (grabbed from uniform in fragment shader)
      if (nLights > 0) {
         switch (shading) {
            case FLAT:
               appendln(hb,"// light colors");
               appendln(hb,"// required for modulation with vertex colors or texture");
               appendln(hb,"out LightColorData {");
               appendln(hb,"   flat vec3 front_ambient;  ");
               appendln(hb,"   flat vec3 front_diffuse;  ");
               appendln(hb,"   flat vec3 front_specular;  ");
               appendln(hb,"   flat vec3 back_ambient;  ");
               appendln(hb,"   flat vec3 back_diffuse;  ");
               appendln(hb,"   flat vec3 back_specular;  ");
               appendln(hb,"} lightOut;");
               appendln(hb);
               break;
//            case GOURAUD:
//               appendln(hb,"// light colors");
//               appendln(hb,"// required for modulation with vertex colors or texture");
//               appendln(hb,"out LightColorData {");
//               appendln(hb,"   vec3 front_ambient;  ");
//               appendln(hb,"   vec3 front_diffuse;  ");
//               appendln(hb,"   vec3 front_specular;  ");
//               appendln(hb,"   vec3 back_ambient;  ");
//               appendln(hb,"   vec3 back_diffuse;  ");
//               appendln(hb,"   vec3 back_specular;  ");
//               appendln(hb,"} lightOut;");
//               appendln(hb);
//               break;
            case SMOOTH:
            case METAL:
                  appendln(hb,"// directions for per-fragment lighting");
                  appendln(hb,"out SurfaceData {");
                  appendln(hb, "   vec3 normal;");
                  appendln(hb, "   vec3 to_eye;");               
                  appendln(hb,"} surfOut;");
                  appendln(hb);
               break;
            case NONE:
               break;
         }
      }
   }

   private static void addVertexClipping(StringBuilder hb, int nClipPlanes) {
      if (nClipPlanes > 0) {
         appendClipPlaneStruct(hb);
         appendln(hb,"layout ("+UNIFORM_LAYOUT+") uniform ClipPlanes {");
         appendln(hb,"   ClipPlane clip_plane[" + nClipPlanes + "];");
         appendln(hb,"};");
         appendln(hb,"out float gl_ClipDistance[" + nClipPlanes + "];");
         appendln(hb);
      }
   }

   private static void appendMaterialStruct(StringBuilder hb) {
      appendln(hb, "// material properties");
      appendln(hb, "struct Material {");
      appendln(hb, "   vec4 diffuse;   // alpha is diffuse.a");
      appendln(hb, "   vec4 specular;  // shininess is specular.a");
      appendln(hb, "   vec4 emission;  ");
      appendln(hb, "   vec4 power;     // scales for ambient/diffuse/specular/emission");
      appendln(hb, "};");
   }

   private static void appendLightSourceStruct(StringBuilder hb) {
      appendln(hb, "// light source info, in camera space");
      appendln(hb, "struct LightSource {");
      appendln(hb, "   vec4 ambient;");
      appendln(hb, "   vec4 diffuse;");
      appendln(hb, "   vec4 specular;");
      appendln(hb, "   vec4 position;        // (pos, point indicator)");
      appendln(hb, "   vec4 direction;       // (dir, spot cutoff), cutoff < -1 -> point");
      appendln(hb, "   vec4 attenuation;     // (exponent, constant, linear, quadratic)");
      appendln(hb, "};");
   }
   
   private static void appendClipPlaneStruct(StringBuilder hb) {
      appendln(hb, "// clipping planes");
      appendln(hb, "struct ClipPlane {");
      appendln(hb, "   vec4 plane;           // ax+by+cz+d=0");
      appendln(hb, "};");
   }

   private static void addBlinnPhong(StringBuilder fb) {
      appendln(fb, "// Blinn-Phong lighting equation coefficients, (diffuse, specular)");
      appendln(fb, "vec2 blinnPhongCoeffs(in vec3 ndir, in vec3 ldir, in vec3 edir, in float specExponent) {");
      appendln(fb, "   ");
      appendln(fb, "   float intensity = max(dot(ndir,ldir), 0.0);");
      appendln(fb, "   ");
      appendln(fb, "   if (intensity > 0) {");
      appendln(fb, "      // compute the half vector");
      appendln(fb, "      vec3 h = normalize(ldir + edir);");
      appendln(fb, "      float shine = (specExponent == 0 ? 1 :pow(max(dot(h,ndir), 0.0), specExponent));");
      appendln(fb, "      return vec2(intensity, shine);");
      appendln(fb, "   }");
      appendln(fb, "   return vec2(0.0);");
      appendln(fb, "}");
      appendln(fb);
   }

   private static void addQuaternionRotation(StringBuilder fb) {
      appendln(fb, "// quaternion rotation");
      appendln(fb, "vec3 qrot( in vec4 q, in vec3 v ) {");
      appendln(fb, "   return v + 2.0*cross(cross(v, q.xyz ) + q.w*v, q.xyz);");
      appendln(fb, "}");
      appendln(fb);
   }

   private static void addRodriguesRotation(StringBuilder fb) {
      // Rodriguez-based line rotation, rotate pos so that 
      // z-axis maps to u, assuming u is a unit vector
      appendln(fb, "// z-axis gets rotated to u, v gets transformed accordingly");
      appendln(fb, "vec3 zrot(in vec3 u, in vec3 v) {");
      appendln(fb, "   // w = cross(u,z)");
      appendln(fb, "   vec3 w = vec3(u.y, -u.x, 0);");
      appendln(fb, "   float sint = sqrt(u.x*u.x+u.y*u.y);");
      appendln(fb, "   w = (sint == 0 ? vec3(1,0,0) : w/sint);");
      appendln(fb, "   // rodriguez rotation formula");
      appendln(fb, "   w = u.z*v - cross(w, v)*sint + dot(w, v)*(1-u.z)*w;");
      appendln(fb, "   return w;");
      appendln(fb, "}");
      appendln(fb);
      appendln(fb, "// rotate u about vector v, with provided sin/cosine of angle");
      appendln(fb, "vec3 rodrigues(in vec3 u, in vec3 v, in float sint, in float cost) {");
      appendln(fb, "   float d = length(v);");
      appendln(fb, "   vec3 w = (d == 0 ? u : u*cost + cross(v, u)*sint/d + dot(v, u)*(1-cost)/(d*d)*v);");
      appendln(fb, "   return w;");
      appendln(fb, "}");
      appendln(fb);
   }

   private static void addRGBtoHSV(StringBuilder fb) {
      appendln(fb, "// rgba to hsva conversion");
      appendln(fb, "vec4 rgba2hsva( in vec4 c ) {");
      appendln(fb, "   vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);");
      appendln(fb, "   vec4 p = c.g < c.b ? vec4(c.bg, K.wz) : vec4(c.gb, K.xy);");
      appendln(fb, "   vec4 q = c.r < p.x ? vec4(p.xyw, c.r) : vec4(c.r, p.yzx);");
      appendln(fb, "   float d = q.x - min(q.w, q.y);");
      appendln(fb, "   float e = 1.0e-10;");
      appendln(fb, "   return vec4(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x, c.a);");
      appendln(fb, "}");
      appendln(fb);
   }

   private static void addHSVtoRGB(StringBuilder fb) {
      appendln(fb, "// hsva to rgba conversion");
      appendln(fb, "vec4 hsva2rgba( in vec4 c ) {");
      appendln(fb, "   vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);");
      appendln(fb, "   vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);");
      appendln(fb, "   return vec4(c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y), c.a);");
      appendln(fb, "}");
      appendln(fb);
   }

   private static void buildFragmentShader(StringBuilder[] sbs, GLProgramInfo info) {
      buildFragmentShaderHeader(sbs[SHADER_GLOBALS], info);
      buildFragmentShaderFunctions(sbs[SHADER_FUNCTIONS], info);
      buildFragmentShaderMain(sbs[SHADER_MAIN], info);
   }

   private static void buildFragmentShaderHeader(StringBuilder hb, GLProgramInfo info) {
      addFragmentInfo(hb, info);
      if (!info.isSelecting ()) {
         addFragmentLighting(hb, info.getShading(), info.numLights());
      }
   }

   private static void addFragmentInfo(StringBuilder hb, GLProgramInfo info) {
      
      appendln(hb, "// fragment color output");
      appendln(hb, "out vec4 fragment_color;");
      appendln(hb);
      
      if (info.isSelecting ()) {
         appendln(hb, "uniform vec4 selection_color;  // fragment color for selection");
         appendln(hb);
      }
      
      RenderingMode instanced = info.getMode();
      boolean hasColors = !info.isSelecting () 
         && (info.getVertexColorMixing () != ColorMixing.NONE)
         && (info.hasVertexColors()
            || ((instanced == RenderingMode.INSTANCED_POINTS 
                 || instanced == RenderingMode.INSTANCED_FRAMES
                 || instanced == RenderingMode.INSTANCED_AFFINES) && info.hasInstanceColors())
            || (instanced == RenderingMode.INSTANCED_LINES && info.hasLineColors() ) );

      boolean hasTextures = hasTextures(info);
      
      if (hasColors) {
         appendln(hb, "// fragment colors from previous shader");
         appendln(hb, "in ColorData {");
         if (info.getColorInterpolation () == ColorInterpolation.NONE) {
            appendln(hb, "   flat vec4 diffuse;");
         } else {
            appendln(hb, "   vec4 diffuse;");   
         }
         appendln(hb, "} colorIn;");
         appendln(hb);
      }
      
      if (hasTextures) {
         appendln(hb, "// texture info");
         appendln(hb, "in TextureData {");
         appendln(hb, "   vec2 texcoord;");
         appendln(hb, "} textureIn;");
         
         if (info.hasColorMap ()) {
            appendln(hb, "uniform sampler2D color_map;");
         }
         
         // only allow normal/bump mapping in per fragment lighting
         if (hasFragmentLighting (info.getShading ())) {
            if (info.hasNormalMap ()) {
               appendln(hb, "uniform sampler2D normal_map;");
               appendln(hb, "uniform float normal_scale;");
            }
            if (info.hasBumpMap ()) {
               appendln(hb, "uniform sampler2D bump_map;");
               appendln(hb, "uniform float bump_scale;");
            }
         }
         appendln(hb);
      }      
   }

   private static void addFragmentLighting(StringBuilder hb, 
      Shading shading, int nLights) {
      addFragmentLightingUniforms(hb, shading, nLights);
      addFragmentLightingInputs(hb, shading, nLights);
   }

   private static void addFragmentLightingUniforms(StringBuilder hb, 
      Shading shading, int nLights) {

      appendMaterialStruct(hb);
      appendln(hb, "// per-object materials");
      appendln(hb, "layout ("+UNIFORM_LAYOUT+") uniform Materials {");
      appendln(hb, "   Material front_material;");
      appendln(hb, "   Material back_material;");
      appendln(hb, "};");
      appendln(hb);

      if (nLights > 0) {
         switch (shading) {
            case SMOOTH:
            case METAL: {
               // we need all light info
               appendLightSourceStruct(hb);
               appendln(hb,"layout ("+UNIFORM_LAYOUT+") uniform Lights {");
               appendln(hb,"   LightSource light[" + nLights + "];");
               appendln(hb,"   float intensity_scale;  // for HDR->LDR, initialized to one");
               appendln(hb,"};");
               appendln(hb);  
               break;
            }
            case NONE:
            case FLAT:
               break;
         }
      }
   }

   private static void addFragmentLightingInputs(StringBuilder hb, Shading shading,
      int nLights) {
      
      switch (shading) {
         case FLAT:
            appendln(hb,"// light reflectance color");
            appendln(hb,"// required for modulation with vertex colors or texture");
            appendln(hb,"in LightColorData {");
            appendln(hb,"   flat vec3 front_ambient;  ");
            appendln(hb,"   flat vec3 front_diffuse;  ");
            appendln(hb,"   flat vec3 front_specular;  ");
            appendln(hb,"   flat vec3 back_ambient;  ");
            appendln(hb,"   flat vec3 back_diffuse;  ");
            appendln(hb,"   flat vec3 back_specular;  ");
            appendln(hb,"} lightIn;");
            appendln(hb);
            break;
//         case GOURAUD:
//            appendln(hb,"// light reflectance color");
//            appendln(hb,"// required for modulation with vertex colors or texture");
//            appendln(hb,"in LightColorData {");
//            appendln(hb,"   vec3 front_ambient;  ");
//            appendln(hb,"   vec3 front_diffuse;  ");
//            appendln(hb,"   vec3 front_specular;  ");
//            appendln(hb,"   vec3 back_ambient;  ");
//            appendln(hb,"   vec3 back_diffuse;  ");
//            appendln(hb,"   vec3 back_specular;  ");
//            appendln(hb,"} lightIn;");
//            appendln(hb);
//            break;
         case SMOOTH:
         case METAL:
            if (nLights > 0) {
               appendln(hb,"// directions for per-fragment lighting");
               appendln(hb,"in SurfaceData {");
               appendln(hb, "   vec3 normal;");
               appendln(hb, "   vec3 to_eye;");               
               appendln(hb,"} surfIn;");
               appendln(hb);
            }
            break;
         case NONE:
            break;
      }
   }
   
   private static void addBumpPerturbation(StringBuilder fb) {
      appendln(fb, "// Bump mapping");
      appendln(fb, "// Derivative of height w.r.t. screen using forward-differences");
      appendln(fb, "vec2 computedHdxy() {");
      appendln(fb, "   vec2 tdx = dFdx(textureIn.texcoord);");
      appendln(fb, "   vec2 tdy = dFdy(textureIn.texcoord);");
      appendln(fb, "   float hll = bump_scale*texture(bump_map, textureIn.texcoord).x;");
      appendln(fb, "   float dBx = bump_scale*texture(bump_map, textureIn.texcoord+tdx).x-hll;");
      appendln(fb, "   float dBy = bump_scale*texture(bump_map, textureIn.texcoord+tdy).x-hll;");
      appendln(fb, "   return vec2(dBx, dBy);");
      appendln(fb, "}");
      appendln(fb, "vec3 perturbNormalBump(in vec3 pos, in vec3 nrm, in vec2 dB) {");
      appendln(fb, "   vec3 dx = dFdx(pos);");
      appendln(fb, "   vec3 dy = dFdy(pos);");
      appendln(fb, "   vec3 r1 = cross(dy, nrm);");
      appendln(fb, "   vec3 r2 = cross(nrm, dx);");
      appendln(fb, "   float fdet = dot(dx, r1);");
      appendln(fb, "   vec3 grad = sign(fdet)*(dB.x*r1 + dB.y*r2);");
      appendln(fb, "   return normalize( abs(fdet)*nrm - grad);");
      appendln(fb, "}");
      appendln(fb);
   }

   private static void addNormalPerturbation(StringBuilder fb) {
      appendln(fb, "// Normal mapping");
      appendln(fb, "mat3 cotangent_frame( in vec3 N, in vec3 p, in vec2 uv ) {");
      appendln(fb, "    // get edge vectors of the pixel triangle");
      appendln(fb, "    vec3 dp1 = dFdx( p );");
      appendln(fb, "    vec3 dp2 = dFdy( p );");
      appendln(fb, "    vec2 duv1 = dFdx( uv );");
      appendln(fb, "    vec2 duv2 = dFdy( uv );");
      appendln(fb); 
      appendln(fb, "    // solve the linear system");
      appendln(fb, "    vec3 dp2perp = cross( dp2, N );");
      appendln(fb, "    vec3 dp1perp = cross( N, dp1 );");
      appendln(fb, "    vec3 T = dp2perp * duv1.x + dp1perp * duv2.x;");
      appendln(fb, "    vec3 B = dp2perp * duv1.y + dp1perp * duv2.y;");
      appendln(fb); 
      appendln(fb, "    // construct a scale-invariant frame ");
      appendln(fb, "    float invmax = inversesqrt( max( dot(T,T), dot(B,B) ) );");
      appendln(fb, "    return mat3( T * invmax, B * invmax, N );");
      appendln(fb, "}");
      appendln(fb, "vec3 perturbNormal(in vec3 eye, in vec3 nrm ) {");
      appendln(fb, "   vec3 map = texture(normal_map, textureIn.texcoord).xyz;");
      appendln(fb, "   map.xy = normal_scale*map.xy;");
      appendln(fb, "   mat3 TBN = cotangent_frame(nrm, -eye, textureIn.texcoord);");
      appendln(fb, "   return normalize(TBN*map);");
      appendln(fb, "}");
      appendln(fb);
      
   }
   
   private static boolean hasFragmentLighting(Shading shading) {
      switch (shading) {
         case FLAT:
         case NONE:
            return false;
         case SMOOTH:
         case METAL:
            return true;
      }
      return true;
   }
   
   private static void buildFragmentShaderFunctions(StringBuilder fb, GLProgramInfo info) {

      if (!info.isSelecting ()) {
         if (hasFragmentLighting (info.getShading ())) {
            addBlinnPhong(fb);
         }

         if (info.getColorInterpolation() == ColorInterpolation.HSV) {
            addHSVtoRGB(fb);
         }

         if (hasFragmentLighting (info.getShading ()) && info.hasNormalMap ()) {
            addNormalPerturbation(fb);
         }

         if (hasFragmentLighting (info.getShading ()) && info.hasBumpMap ()) {
            addBumpPerturbation(fb);
         }
      }

   }
   
   private static void buildFragmentShaderMain(StringBuilder mb, GLProgramInfo info) {

      RenderingMode mode = info.getMode();
      ColorInterpolation cinterp = info.getColorInterpolation();
      boolean hasTextures = hasTextures(info);
      
      appendln(mb, "// main fragment shader");
      appendln(mb, "void main() {");
      appendln(mb, "");
      
      // points
      if (mode == RenderingMode.POINTS && info.hasRoundPoints()) {
         appendln(mb, "   vec2 point_texcoord = 2.0*gl_PointCoord-1.0;");
         appendln(mb, "   float point_dist = dot(point_texcoord, point_texcoord);");
         appendln(mb, "   if ( point_dist > 1) {");
         appendln(mb, "      discard;");
         appendln(mb, "   }");
      }
      
      // do lighting computations
      boolean lights = false;
      if (!info.isSelecting ()) {
         if (info.getShading() != Shading.NONE && info.numLights() > 0) {
            
            lights = true;
            appendln(mb, "   vec3 ambient = vec3(0.0);");
            appendln(mb, "   vec3 diffuse = vec3(0.0);");
            appendln(mb, "   vec3 specular = vec3(0.0);");
            appendln(mb, "   vec3 emission = vec3(0.0);");
            appendln(mb, "   Material material;");
            switch (info.getShading()) {
               case FLAT:
               //case GOURAUD:
                     appendln(mb, "   // imported per-vertex lighting");
                     appendln(mb, "   if( gl_FrontFacing ) {");
                     appendln(mb, "      ambient  = lightIn.front_ambient;");
                     appendln(mb, "      diffuse  = lightIn.front_diffuse;");
                     appendln(mb, "      specular = lightIn.front_specular;");
                     appendln(mb, "      material = front_material;");
                     appendln(mb, "   } else {");
                     appendln(mb, "      diffuse  = lightIn.back_diffuse;");
                     appendln(mb, "      ambient  = lightIn.back_ambient;");
                     appendln(mb, "      specular = lightIn.back_specular;");
                     appendln(mb, "      material = back_material;");
                     appendln(mb, "   }");
                     appendln(mb);
                     break;
               case SMOOTH:
               case METAL:
                     appendln(mb, "   // fragment normal and eye location for lighting");
                     
                     if (!info.hasVertexNormals ()) {
                        appendln(mb, "   // compute normal using derivatives");
                        appendln(mb, "   vec3 deyedx = dFdx(surfIn.to_eye);");
                        appendln(mb, "   vec3 deyedy = dFdy(surfIn.to_eye);");
                        appendln(mb, "   vec3 normal = normalize( cross( deyedx, deyedy ) );");
                     } else {
                        appendln(mb, "   vec3 normal = normalize(surfIn.normal);");
                     }
                     appendln(mb, "   vec3 eye = normalize(surfIn.to_eye);");
                     appendln(mb, "   ");
                     if (hasTextures) {
                        if (info.hasNormalMap()) {
                           appendln(mb, "   // normal-map purturbation");
                           appendln(mb, "   normal = perturbNormal(-surfIn.to_eye, normal);");
                           appendln(mb, "   ");
                        }
                        if (info.hasBumpMap()) {
                           appendln(mb, "   // bump-map normal purturbation");
                           appendln(mb, "   normal = perturbNormalBump(-surfIn.to_eye, normal, computedHdxy());");
                           appendln(mb, "   ");
                        }
                     }
                     appendln(mb, "   // choose material based on face orientation");
                     appendln(mb, "   if (gl_FrontFacing) {");
                     appendln(mb, "      material = front_material;");
                     appendln(mb, "   } else {");
                     appendln(mb, "      material = back_material;");
                     appendln(mb, "      normal = -normal;  // flip fragment normal");
                     appendln(mb, "   }");
                     appendln(mb, "   ");
                     
                     appendln(mb, "   // per-fragment lighting computations");
                     appendln(mb, "   for (int i=0; i<" + info.numLights() + "; ++i) {");
                     appendln(mb, "      vec3 light_to_vertex = -surfIn.to_eye-light[i].position.xyz;");
                     appendln(mb, "      float lightdist = length(light_to_vertex);");
                     appendln(mb, "      light_to_vertex = normalize(light_to_vertex);");                  
                     appendln(mb, "      // determine direction either from point or direction using direction indicator");
                     appendln(mb, "      vec3 light_direction = normalize(light[i].direction.xyz);");
                     appendln(mb, "      ");
                     appendln(mb, "      float spotatt = 1.0;  // spot attentuation initially zero if non-spotlight");
                     appendln(mb, "      float coslimit = light[i].direction.w;");
                     appendln(mb, "      if (coslimit > 0) {");
                     appendln(mb, "         // check angle");
                     appendln(mb, "         float coslight = dot(light_direction, light_to_vertex);");
                     appendln(mb, "         if (coslight < coslimit) {");
                     appendln(mb, "            spotatt = 0;");
                     appendln(mb, "         } else {");
                     appendln(mb, "            spotatt = pow(coslight, light[i].attenuation.w);");
                     appendln(mb, "         }");
                     appendln(mb, "      }");
                     appendln(mb, "      ");
                     appendln(mb, "      // distance attenuation doesn't affect directional lights");
                     appendln(mb, "      float att = mix(1.0, 1.0 / (light[i].attenuation.x + light[i].attenuation.y*lightdist +");
                     appendln(mb, "         light[i].attenuation.z*lightdist*lightdist), light[i].position.w);");
                     appendln(mb, "      att *= spotatt;  // combine into a single attenuation parameter");
                     appendln(mb, "      ");
                     appendln(mb, "      // position vs directional light");
                     appendln(mb, "      light_direction = mix(light_direction, light_to_vertex, light[i].position.w);");
                     appendln(mb, "      vec2 ds = blinnPhongCoeffs( normal, -light_direction, eye, material.specular.a);");
                     appendln(mb, "      ambient  += intensity_scale*light[i].ambient.rgb;");
                     appendln(mb, "      diffuse  += intensity_scale*att*ds.x*light[i].diffuse.rgb;");
                     appendln(mb, "      specular += intensity_scale*att*ds.y*light[i].specular.rgb;");
                     appendln(mb, "      ");
                     appendln(mb, "   }");
                     appendln(mb, "   ");
                  break;
               case NONE:
                  // never here
                  break;
            }
         } else {
            appendln(mb, "   // material to use;");
            appendln(mb, "   Material material;");
            appendln(mb, "   if( gl_FrontFacing ) {");
            appendln(mb, "      material = front_material;");
            appendln(mb, "   } else {");
            appendln(mb, "      material = back_material;");
            appendln(mb, "   }");
         }
         
         // combine colors
         boolean hasFragmentColors = (info.getVertexColorMixing () != ColorMixing.NONE)
            && (info.hasVertexColors()
               || ((mode == RenderingMode.INSTANCED_POINTS 
                    || mode == RenderingMode.INSTANCED_FRAMES
                    || mode == RenderingMode.INSTANCED_AFFINES) && info.hasInstanceColors())
               || (mode == RenderingMode.INSTANCED_LINES && info.hasLineColors() ) );

         appendln(mb, "   // compute final color, starting with material");
         appendln(mb, "   vec4 fdiffuse = material.diffuse;");
         appendln(mb, "   vec3 fspecular = material.specular.rgb;");
         appendln(mb, "   vec3 femission = material.emission.rgb;");
         appendln(mb);
         
         if (hasFragmentColors) {
            // mix colors
            appendln(mb, "   // incoming vertex color");
            if (cinterp == ColorInterpolation.HSV) {
               appendln(mb, "   vec4 vcolor = hsva2rgba(colorIn.diffuse);");
            } else {
               appendln(mb, "   vec4 vcolor = colorIn.diffuse;");
            }
            appendln(mb, "   // mix");
            ColorMixing cmix = info.getVertexColorMixing();
            switch (cmix) {
               case DECAL:
                  if (info.isMixVertexColorDiffuse ()) {
                     appendln(mb, "   fdiffuse  = vec4(mix(fdiffuse.rgb,vcolor.rgb,vcolor.a), fdiffuse.a); // decal");
                  }
                  if (info.isMixVertexColorSpecular ()) {
                     appendln(mb, "   fspecular = mix(fspecular,vcolor.rgb,vcolor.a); // decal");
                  }
                  if (info.isMixVertexColorEmission ()) {
                     appendln(mb, "   femission = mix(femission,vcolor.rgb,vcolor.a); // decal");
                  }
                  break;
               case MODULATE:
                  if (info.isMixVertexColorDiffuse ()) {
                     appendln(mb, "   fdiffuse  = fdiffuse*vcolor;      // modulate");
                  }
                  if (info.isMixVertexColorSpecular ()) {
                     appendln(mb, "   fspecular = fspecular*vcolor.rgb; // modulate");
                  }
                  if (info.isMixVertexColorEmission ()) {
                     appendln(mb, "   femission = femission*vcolor.rgb; // modulate");
                  }
                  break;
               case REPLACE:
                  if (info.isMixVertexColorDiffuse ()) {
                     appendln(mb, "   fdiffuse  = vcolor;     // replace");
                  }
                  if (info.isMixVertexColorSpecular ()) {
                     appendln(mb, "   fspecular = vcolor.rgb; // replace");
                  }
                  if (info.isMixVertexColorEmission ()) {
                     appendln(mb, "   femission = vcolor.rgb; // replace");
                  }
                  break;
               case NONE:
                  appendln(mb, "   // vcolor ignored");
               default:
            }
         }

         if (hasTextures && info.hasColorMap ()) {
            appendln(mb, "   // grab texture color and mix");
            appendln(mb, "   vec4 texture_color = texture( color_map, textureIn.texcoord );");
            ColorMixing cmix = info.getTextureColorMixing();
            switch (cmix) {
               case DECAL:
                  if (info.isMixTextureColorDiffuse ()) {
                     appendln(mb, "   fdiffuse  = vec4(mix(fdiffuse.rgb,texture_color.rgb,texture_color.a), fdiffuse.a); // decal");
                  }
                  if (info.isMixTextureColorSpecular ()) {
                     appendln(mb, "   fspecular = mix(fspecular,texture_color.rgb,texture_color.a); // decal");
                  }
                  if (info.isMixTextureColorEmission ()) {
                     appendln(mb, "   femission = mix(femission,texture_color.rgb,texture_color.a); // decal");
                  }
                  break;
               case MODULATE:
                  if (info.isMixTextureColorDiffuse ()) {
                     appendln(mb, "   fdiffuse  = fdiffuse*texture_color;      // modulate");
                  }
                  if (info.isMixTextureColorSpecular ()) {
                     appendln(mb, "   fspecular = fspecular*texture_color.rgb; // modulate");
                  }
                  if (info.isMixTextureColorEmission ()) {
                     appendln(mb, "   femission = femission*texture_color.rgb; // modulate");
                  }
                  break;
               case REPLACE:
                  if (info.isMixTextureColorDiffuse ()) {
                     appendln(mb, "   fdiffuse  = texture_color;     // replace");
                  }
                  if (info.isMixTextureColorSpecular ()) {
                     appendln(mb, "   fspecular = texture_color.rgb; // replace");
                  }
                  if (info.isMixTextureColorEmission ()) {
                     appendln(mb, "   femission = texture_color.rgb; // replace");
                  }
                  break;
               case NONE:
                  appendln(mb, "   // texture_color ignored");
                  break;
               default:
            }
            appendln(mb);
         }
      }
      
      
      // lighting
      if (info.isSelecting ()) {
         appendln(mb, "   fragment_color = selection_color;");
      } else if (lights) {
         appendln(mb, "   // apply lighting");
         appendln(mb, "   ambient  = fdiffuse.rgb*ambient*material.power.x;");
         appendln(mb, "   diffuse  = fdiffuse.rgb*diffuse*material.power.y;");
         appendln(mb, "   specular = fspecular*specular*material.power.z;");
         appendln(mb, "   emission = femission*material.power.w;  // emission only material-related");
         appendln(mb, "   fragment_color = vec4(max(diffuse+specular+emission, ambient), fdiffuse.a);");
      } else {
         appendln(mb, "   fragment_color = fdiffuse;");
      }
      appendln(mb);
      
      //      appendln(mb, "   // gamma correction");
      //      appendln(mb, "   vec3 gamma = vec3(1.0/2.2);");
      //      appendln(mb, "   fragment_color = vec4(pow(fragment_color.rgb, gamma), fragment_color.a);");

      appendln(mb, "}");

   }

   public static void main(String[] args) {
      
      GLProgramInfo info = new GLProgramInfo ();
      info.setNumLights(1);
      info.setShading (Shading.SMOOTH);
      
      String[] shaders = getShaderScripts(info);

      System.out.println("Vertex Shader: ");
      System.out.println(shaders[VERTEX_SHADER]);

      System.out.println("Fragment Shader: ");
      System.out.println(shaders[FRAGMENT_SHADER]);
   }

}
