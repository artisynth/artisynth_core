package maspack.render.GL.GL3;

import maspack.render.RenderProps.Shading;
import maspack.render.GL.GL3.GLSLInfo.InstancedRendering;
import maspack.render.GL.GL3.GLSLInfo.ColorInterpolation;


public class GLSLGenerator {

   static String VERSION_STRING = "#version 330";
   static String UNIFORM_LAYOUT = "std140";  // std140 or "shared"

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
   //    vec4 ambient;   // diffuse-mixing factor is ambient.a
   //    vec4 specular;  // shininess is specular.a
   //    vec4 emission;  
   // };
   //
   // // light source info, in camera space
   // struct LightSource {
   //    vec4 diffuse;
   //    vec4 ambient;
   //    vec4 specular;
   //    vec4 position;     
   //    vec4 direction;    // (dir, spot cutoff)
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
   // uniform sampler2D texture0;
   //
   // // per-object materials
   // layout (std140) uniform Materials {
   //    Material front_material;
   //    Material back_material;
   // };

   //==========================================================
   // Program Inputs:
   //==========================================================
   // // vertex inputs
   // in vec3 vertex_position;
   // in vec3 vertex_normal;
   // in vec4 vertex_color;
   // 
   // // texture info
   // in vec2 vertex_texture;
   // 
   // // point instance information
   // in float instance_scale;
   // in vec3  instance_position;
   // in vec4  instance_orientation;  // quaternion
   // in vec4  instance_color;
   // in vec2  instance_texture;
   // 
   // // affine instance information
   // in mat4 instance_affine_matrix;
   // in mat4 instance_normal_matrix;  // inverse transpose
   //
   // // line instance information
   // in float line_radius;
   // in vec4  line_length_offset;     // (length, length_flag, offset, offset_flag), flag specifies distance subtracted from segment length  
   // in vec3  line_bottom_position;
   // in vec3  line_top_position;
   // in vec4  line_bottom_color;
   // in vec4  line_top_color;
   // in vec2  line_bottom_texture;
   // in vec2  line_top_texture;

   public static String[] getShaderScripts(GLSLInfo info) {

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

   private static void buildVertexShader(StringBuilder[] sbs, GLSLInfo info) {
      buildVertexShaderHeader(sbs[SHADER_GLOBALS], info);
      buildVertexShaderFunctions(sbs[SHADER_FUNCTIONS], info);
      buildVertexShaderMain(sbs[SHADER_MAIN], info);
   }

   private static void buildVertexShaderHeader(StringBuilder hb, GLSLInfo info) {
      addVertexInfo(hb, info);
      addVertexLighting(hb, info.getShading(), info.numLights());
      addVertexClipping(hb, info.numClipPlanes());
   }

   private static void buildVertexShaderFunctions(StringBuilder fb, GLSLInfo info) {
      if (info.getShading() == Shading.FLAT || info.getShading() == Shading.GOURAUD) {
         addBlinnPhong(fb);
      }

      if (info.getColorInterpolation() == ColorInterpolation.HSV) {
         addRGBtoHSV(fb);
      }

      switch (info.getInstancedRendering()) {
         case LINES: {
            addRodriguesLineRotation(fb);
            break;
         }
         case FRAMES: {
            addQuaternionRotation(fb);
            break;
         }
         case NONE:
         case POINTS:
         case AFFINES:
            // nothing required
            break;
      }

   }

   private static void buildVertexShaderMain(StringBuilder mb, GLSLInfo info) {

      appendln(mb, "// main vertex shader");
      appendln(mb, "void main() {");
      appendln(mb);
      appendln(mb, "   vec3 position;");
      if (info.hasVertexNormals() && info.getShading() != Shading.NONE) {
         appendln(mb, "   vec3 normal;");
      }
      appendln(mb);

      InstancedRendering instanced = info.getInstancedRendering();
      
      // transform vertex using instance info
      switch (instanced) {
         case AFFINES:
            appendln(mb, "   // instance vertex, affine transform");
            appendln(mb, "   position = (instance_affine_matrix *  vec4(vertex_position, 1.0) ).xyz;");
            if (info.hasVertexNormals() && info.getShading() != Shading.NONE) {
               appendln(mb, "   normal = (instance_normal_matrix *  vec4(vertex_normal, 0.0) ).xyz;");
            }
            appendln(mb);
            break;
         case FRAMES:
            appendln(mb, "   // instance vertex, scale-rotate-translate");
            appendln(mb, "   position = qrot(instance_orientation, (instance_scale * vertex_position)) + instance_position;");
            if (info.hasVertexNormals() && info.getShading() != Shading.NONE) {
               appendln(mb, "   normal = qrot(instance_orientation, vertex_normal);");
            }
            appendln(mb);
            break;
         case LINES:
            appendln(mb, "   // instance vertex, scale radially, rotate/translate");
            appendln(mb, "   vec3  u = line_top_position-line_bottom_position;");
            appendln(mb, "   float line_length = length(u);");
            appendln(mb, "   u = normalize(u);");
            if (info.hasLineLengthOffset()) {
               appendln(mb, "   // scale and offet line");
               appendln(mb, "   float line_offset = mix(line_length_offset.z, line_length-line_length_offset.z, line_length_offset.w);");
               appendln(mb, "   line_length = mix(line_length_offset.x, line_length-line_length_offset.x, line_length_offset.y);");
            }
            appendln(mb, "   position = vec3(line_radius*vertex_position.xy, line_length*vertex_position.z);");
            appendln(mb, "   position = line_bottom_position + linerot(u, position);");
            if (info.hasLineLengthOffset()) {
               appendln(mb, "   position += u*line_offset;  // offset along line");
            }
            if (info.hasVertexNormals() && info.getShading() != Shading.NONE) {
               appendln(mb, "   normal = vec3( vertex_normal.xy*line_length, vertex_normal.z*line_radius);");
               appendln(mb, "   normal = linerot(u, normal);");
            }
            appendln(mb);
            break;
         case POINTS:
            appendln(mb, "   // instance vertex, scale-translate");
            appendln(mb, "   position = instance_scale * vertex_position + instance_position;");
            if (info.hasVertexNormals() && info.getShading() != Shading.NONE) {
               appendln(mb, "   normal = vertex_normal;");
            }
            break;
         case NONE:
            appendln(mb, "   position = vertex_position;");
            if (info.hasVertexNormals() && info.getShading() != Shading.NONE) {
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
      if (cinterp != ColorInterpolation.NONE) {
         switch (instanced) {
            case POINTS:
            case FRAMES:
            case AFFINES:
               if (info.hasInstanceColors()) {
                  if (cinterp == ColorInterpolation.HSV) {
                     appendln(mb, "   fragmentColorOut.color = rgba2hsva(instance_color);");
                  } else {
                     appendln(mb, "   fragmentColorOut.color = instance_color;");
                  }
               } else if (info.hasVertexColors()) {
                  if (cinterp == ColorInterpolation.HSV) {
                     appendln(mb, "   fragmentColorOut.color = rgba2hsva(vertex_color);");
                  } else {
                     appendln(mb, "   fragmentColorOut.color = vertex_color;");
                  }
               }
               break;
            case LINES:
               if (info.hasLineColors()) {
                  if (cinterp == ColorInterpolation.HSV) {
                     appendln(mb, "   // interpolate color based on line");
                     appendln(mb, "   fragmentColorOut.color  = mix(rgba2hsva(line_bottom_color), rgba2hsva(line_top_color), vertex_position.z);");
                  } else {
                     appendln(mb, "   fragmentColorOut.color = instance_color;");
                  }
               } else if (info.hasVertexColors()) {
                  if (cinterp == ColorInterpolation.HSV) {
                     appendln(mb, "   fragmentColorOut.color = rgba2hsva(vertex_color);");
                  } else {
                     appendln(mb, "   fragmentColorOut.color = vertex_color;");
                  }
               }
               break;
            case NONE:
               if (info.hasVertexColors()) {
                  if (cinterp == ColorInterpolation.HSV) {
                     appendln(mb, "   fragmentColorOut.color = rgba2hsva(vertex_color);");
                  } else {
                     appendln(mb, "   fragmentColorOut.color = vertex_color;");
                  }
               }
               break;
         }
      }
      
      // do lighting computations
      switch (info.getShading()) {
         case FLAT:
         case GOURAUD:
            if (info.numLights() > 0) {
               appendln(mb, "   // per-vertex lighting computations");
               appendln(mb, "   // compute camera position/normal");
               appendln(mb, "   vec4 camera_position = vm_matrix * vec4(position, 1.0);");
               if (info.hasVertexNormals()) {
                  appendln(mb, "   vec4 camera_normal = normal_matrix * vec4(normal, 0.0);");
               } else {
                  appendln(mb, "   vec4 camera_normal = -camera_position;");
               }
               appendln(mb, "   vec3 nfront = normalize(camera_normal.xyz);");
               appendln(mb, "   vec3 nback = -nfront;");
               appendln(mb, "   vec3 e = normalize(-camera_position.xyz);");
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
               appendln(mb, "      vec3  light_to_vertex = vec3(camera_position-light[i].position);");
               appendln(mb, "      float lightdist = length(light_to_vertex);");
               appendln(mb, "      light_to_vertex = normalize(light_to_vertex);");                  
               appendln(mb, "      // determine direction either from point or direction using direction indicator");
               appendln(mb, "      vec3 light_direction = mix(light[i].direction.xyz, light_to_vertex, light[i].position.w);");
               appendln(mb, "      ");
               appendln(mb, "      float spotatt = 1.0;  // spot attentuation initially zero");
               appendln(mb, "      float coslimit = light[i].direction.w;");
               appendln(mb, "      if (coslimit > -1) {");
               appendln(mb, "         // check angle");
               appendln(mb, "         float coslight = dot(light_direction, light_to_vertex);");
               appendln(mb, "         // cosine range, rescaled to [0,1)");
               appendln(mb, "         float ccut = max((coslight-coslimit), 0)/(1-coslimit+1e-10);");
               appendln(mb, "         // radial distance cut-off");
               appendln(mb, "         float rcut = 1-lightdist*lightdist*(1-coslight*coslight)/(coslimit*coslimit+1e-10);");
               appendln(mb, "         // choose between angle and radial");
               appendln(mb, "         spotatt = max(mix(rcut, ccut, light[i].position.w), 0);");
               appendln(mb, "         spotatt = pow(spotatt, light[i].attenuation.w);");
               appendln(mb, "      }");
               appendln(mb, "      ");
               appendln(mb, "      // distance attenuation doesn't affect directional lights");
               appendln(mb, "      float att = mix(1.0, 1.0 / (light[i].attenuation.x + light[i].attenuation.y*lightdist +");
               appendln(mb, "         light[i].attenuation.z*lightdist*lightdist), light[i].position.w);");
               appendln(mb, "      att *= spotatt;  // combine into a single attenuation parameter");
               appendln(mb, "      ");
               appendln(mb, "      vec2 ds = blinnPhongCoeffs( nfront, -light_direction, e, front_material.specular.a);");
               appendln(mb, "      fldiff += intensity_scale*att*ds.x*light[i].diffuse.rgb;");
               appendln(mb, "      flspec += intensity_scale*att*ds.y*light[i].specular.rgb;");
               appendln(mb, "      flambi += intensity_scale*light[i].ambient.rgb;");
               appendln(mb);
               appendln(mb, "      ds = blinnPhongCoeffs( nback, -light_direction, e, back_material.specular.a);");
               appendln(mb, "      bldiff += intensity_scale*att*ds.x*light[i].diffuse.rgb;");
               appendln(mb, "      blspec += intensity_scale*att*ds.y*light[i].specular.rgb;");
               appendln(mb, "      blambi += intensity_scale*light[i].ambient.rgb;");
               appendln(mb, "   }");
               appendln(mb, "   ");
               appendln(mb, "   // accumulate");
               appendln(mb, "   fragmentLightOut.front_diffuse  = fldiff;");
               appendln(mb, "   fragmentLightOut.front_ambient  = flambi;");
               appendln(mb, "   fragmentLightOut.front_specular = flspec;");
               appendln(mb, "   fragmentLightOut.back_diffuse   = bldiff;");
               appendln(mb, "   fragmentLightOut.back_ambient   = blambi;");
               appendln(mb, "   fragmentLightOut.back_specular  = blspec;");
               appendln(mb);
            }
            break;
         case PHONG:
            // forward along direction information for vertices
            if (info.numLights() > 0) {
               appendln(mb, "   // per-fragment lighting info, vertex normal and eye directions");
               appendln(mb, "   vec4 camera_position = vm_matrix * vec4(position, 1.0);");
               if (info.hasVertexNormals()) {
                  appendln(mb, "   vec4 camera_normal = normal_matrix * vec4(normal, 0.0);");
               } else {
                  appendln(mb, "   vec4 camera_normal = -camera_position;");
               }
               appendln(mb, "   fragmentDirOut.normal = camera_normal.xyz;");
               appendln(mb, "   fragmentDirOut.to_eye = -camera_position.xyz;");
               appendln(mb, "");
            }
            break;
         case NONE:
            break;

      }
      
      // textures
      switch (info.getInstancedRendering()) {
         case POINTS:
         case FRAMES:
         case AFFINES:
            // replace texture with instance texture
            if (info.hasInstanceTextures()) {
               appendln(mb, "   // use instance texture coordinates");
               appendln(mb, "   textureOut.texcoord = instance_texture;");
               appendln(mb);
            } else  if (info.hasVertexTextures()) {
               appendln(mb, "   // forward vertex texture coordinates");
               appendln(mb, "   textureOut.texcoord = vertex_texture;");
               appendln(mb);
            }
            break;
         case LINES:
            if (info.hasLineTextures()) {
               appendln(mb, "   // compute line-based texture coordinate");
               appendln(mb, "   textureOut.texcoord = mix(line_bottom_texture, line_top_texture, vertex_position.z);");
               appendln(mb);
            } else if (info.hasVertexTextures()) {
               appendln(mb, "   // forward vertex texture coordinates");
               appendln(mb, "   textureOut.texcoord = vertex_texture;");
               appendln(mb);
            }
            break;
         case NONE:
            if (info.hasVertexTextures()) {
               appendln(mb, "   // forward vertex texture coordinates");
               appendln(mb, "   textureOut.texcoord = vertex_texture;");
               appendln(mb);
            }
            break;
      }
      
      if (info.numClipPlanes() > 0) {
         appendln(mb, "   // clipping planes, in world coordinates");
         appendln(mb, "   for (int i=0; i<"+ info.numClipPlanes() + "; ++i) {");
         appendln(mb, "      gl_ClipDistance[i] = dot(m_matrix * vec4(position, 1.0), clip_plane[i].plane);");
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
      appendln(hb, "};");
      appendln(hb);
   }

   private static void addVertexInfo(StringBuilder hb, GLSLInfo info) {
      addPVMInput(hb);
      addVertexInputs(hb, info);
      addVertexOutputs(hb, info);
   }

   private static void addVertexInputs(StringBuilder hb, GLSLInfo info) {
      
      appendln(hb, "// vertex inputs");
      appendln(hb, "in vec3 vertex_position;");
      if (info.hasVertexNormals() && info.getShading() != Shading.NONE) {
         appendln(hb, "in vec3 vertex_normal;");
      }
      if (info.hasVertexColors() && info.getColorInterpolation() != ColorInterpolation.NONE) {
         appendln(hb, "in vec4 vertex_color;");
      }
      if (info.hasVertexTextures()) {
         appendln(hb, "in vec2 vertex_texture;");
      }
      
      switch (info.getInstancedRendering()) {
         case POINTS:
            appendln(hb);
            appendln(hb, "// instance inputs");
            appendln(hb, "in float instance_scale;");
            appendln(hb, "in vec3  instance_position;"); 
            if (info.hasInstanceColors() && info.getColorInterpolation() != ColorInterpolation.NONE) {
               appendln(hb, "in vec4  instance_color;");
            }
            if (info.hasInstanceTextures()) {
               appendln(hb, "in vec2  instance_texture;");
            }
            break;
         case FRAMES:
            appendln(hb);
            appendln(hb, "// instance inputs");
            appendln(hb, "in float instance_scale;");
            appendln(hb, "in vec3  instance_position;");
            appendln(hb, "in vec4  instance_orientation;"); 
            if (info.hasInstanceColors() && info.getColorInterpolation() != ColorInterpolation.NONE) {
               appendln(hb, "in vec4  instance_color;");
            }
            if (info.hasInstanceTextures()) {
               appendln(hb, "in vec2  instance_texture;");
            }
            break;
         case AFFINES:
            appendln(hb);
            appendln(hb, "// instance inputs");
            appendln(hb, "in float instance_scale;");
            appendln(hb, "in vec3  instance_affine_matrix;");
            appendln(hb, "in vec4  instance_orientation;"); 
            if (info.hasInstanceColors() && info.getColorInterpolation() != ColorInterpolation.NONE) {
               appendln(hb, "in vec4  instance_color;");
            }
            if (info.hasInstanceTextures()) {
               appendln(hb, "in vec2  instance_texture;");
            }
            break;
         case LINES:
            appendln(hb);
            appendln(hb, "// line instance inputs");
            appendln(hb, "in float line_radius;");
            appendln(hb, "in vec3  line_bottom_position;");
            appendln(hb, "in vec3  line_top_position;"); 
            if (info.hasLineLengthOffset()) {
               appendln(hb, "in vec4  line_length_offset;");
            }
            if (info.hasInstanceColors() && info.getColorInterpolation() != ColorInterpolation.NONE) {
               appendln(hb, "in vec4  line_bottom_color;");
               appendln(hb, "in vec4  line_top_color;");
            }
            if (info.hasInstanceTextures()) {
               appendln(hb, "in vec2  line_bottom_texture;");
               appendln(hb, "in vec2  line_top_texture;");
            }
            break;
         case NONE:
            break;
      }
      appendln(hb);
   }

   private static void addVertexOutputs(StringBuilder hb, GLSLInfo info) {
      
      InstancedRendering instanced = info.getInstancedRendering();
      boolean hasColors = (info.getColorInterpolation() != ColorInterpolation.NONE)
                          && (info.hasVertexColors()
                             || ((instanced == InstancedRendering.POINTS 
                                  || instanced == InstancedRendering.FRAMES
                                  || instanced == InstancedRendering.AFFINES) && info.hasInstanceColors())
                             || (instanced == InstancedRendering.LINES && info.hasLineColors() ) );
      
      boolean hasTextures = info.hasVertexTextures()
                            || ((instanced == InstancedRendering.POINTS 
                                  || instanced == InstancedRendering.FRAMES
                                  || instanced == InstancedRendering.AFFINES) && info.hasInstanceTextures())
                            || (instanced == InstancedRendering.LINES && info.hasLineTextures() );
      
      if (hasColors) {
         appendln(hb, "// per-fragment color info");
         appendln(hb, "out FragmentColorData {");
         appendln(hb, "   vec4 color;");
         appendln(hb, "} fragmentColorOut;");
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

      if ( (shading == Shading.FLAT || shading == Shading.GOURAUD) && (nLights > 0) ) {
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
               appendln(hb,"   flat vec3 front_diffuse;  ");
               appendln(hb,"   flat vec3 front_ambient;  ");
               appendln(hb,"   flat vec3 front_specular;  ");
               appendln(hb,"   flat vec3 back_diffuse;  ");
               appendln(hb,"   flat vec3 back_ambient;  ");
               appendln(hb,"   flat vec3 back_specular;  ");
               appendln(hb,"} fragmentLightOut;");
               appendln(hb);
               break;
            case GOURAUD:
               appendln(hb,"// light colors");
               appendln(hb,"// required for modulation with vertex colors or texture");
               appendln(hb,"out LightColorData {");
               appendln(hb,"   vec3 front_diffuse;  ");
               appendln(hb,"   vec3 front_ambient;  ");
               appendln(hb,"   vec3 front_specular;  ");
               appendln(hb,"   vec3 back_diffuse;  ");
               appendln(hb,"   vec3 back_ambient;  ");
               appendln(hb,"   vec3 back_specular;  ");
               appendln(hb,"} fragmentLightOut;");
               appendln(hb);
               break;
            case PHONG:
                  appendln(hb,"// directions for per-fragment lighting");
                  appendln(hb,"out FragmentDirData {");
                  appendln(hb, "   vec3 normal;");
                  appendln(hb, "   vec3 to_eye;");               
                  appendln(hb,"} fragmentDirOut;");
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
      appendln(hb, "   vec4 ambient;   // diffuse-mixing factor is ambient.a");
      appendln(hb, "   vec4 specular;  // shininess is specular.a");
      appendln(hb, "   vec4 emission;  ");
      appendln(hb, "};");
   }

   private static void appendLightSourceStruct(StringBuilder hb) {
      appendln(hb, "// light source info, in camera space");
      appendln(hb, "struct LightSource {");
      appendln(hb, "   vec4 diffuse;");
      appendln(hb, "   vec4 ambient;");
      appendln(hb, "   vec4 specular;");
      appendln(hb, "   vec4 position;        // (pos, point indicator)");
      appendln(hb, "   vec4 direction;       // (dir, spot cutoff), cutoff==0 -> point");
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
      //      appendln(fb, "// Blinn-Phong lighting equation");
      //      appendln(fb, "void blinnPhong(in vec3 ndir, in vec3 ldir, in vec3 edir, ");
      //      appendln(fb, "   in vec4 ldiff, in vec4 lambi, in vec4 lspec,");
      //      appendln(fb, "   in vec4 mdiff, in vec4 mambi, in vec4 mspec,");
      //      appendln(fb, "   out vec3 diff, out vec3 ambi, out vec3 spec) {");
      //      appendln(fb, "   ");
      //      appendln(fb, "   float intensity = max(dot(ndir,ldir), 0.0);");
      //      appendln(fb, "   ");
      //      appendln(fb, "   spec = vec3(0.0);");
      //      appendln(fb, "   diff = vec3(0.0);");
      //      appendln(fb, "   if (intensity > 0) {");
      //      appendln(fb, "      // compute the half vector");
      //      appendln(fb, "      vec3 h = normalize(ldir + edir);");
      //      appendln(fb, "      float intSpec = max(dot(h,ndir), 0.0);");
      //      appendln(fb, "      spec = mspec.rgb * lspec.rgb * pow(intSpec, mspec.a);");
      //      appendln(fb, "      diff = mdiff.rgb * ldiff.rgb * intensity;");
      //      appendln(fb, "   }");
      //      appendln(fb, "   // ambient color or fraction of diffuse");
      //      appendln(fb, "   ambi = mix(mambi.rgb * lambi.rgb, mdiff.rgb * lambi.rgb, mambi.a);");
      //      appendln(fb, "}");
      //      appendln(fb);
      appendln(fb, "// Blinn-Phong lighting equation coefficients, (diffuse, specular)");
      appendln(fb, "vec2 blinnPhongCoeffs(in vec3 ndir, in vec3 ldir, in vec3 edir, in float specPower) {");
      appendln(fb, "   ");
      appendln(fb, "   float intensity = max(dot(ndir,ldir), 0.0);");
      appendln(fb, "   ");
      appendln(fb, "   if (intensity > 0) {");
      appendln(fb, "      // compute the half vector");
      appendln(fb, "      vec3 h = normalize(ldir + edir);");
      appendln(fb, "      return vec2(intensity,  pow(max(dot(h,ndir), 0.0),specPower));");
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

   private static void addRodriguesLineRotation(StringBuilder fb) {
      // Rodriguez-based line rotation, rotate pos so that 
      // z-axis maps to u, assuming u is a unit vector
      appendln(fb, "vec3 linerot(in vec3 u, in vec3 pos) {");
      appendln(fb, "   vec3 w = vec3(u.y, -u.x, 0);");
      appendln(fb, "   vec3 wx = vec3(-u.x*pos.z, -u.y*pos.z, u.x*pos.x+u.y*pos.y);");
      appendln(fb, "   w = (u.z*pos - wx + (u.y*pos.x-u.x*pos.y)/(1+u.z)*w);");
      appendln(fb, "   return w;");
      appendln(fb, "}");
      appendln(fb);
   }

   private static void addRGBtoHSV(StringBuilder fb) {
      //      appendln(fb, "// rgb to hsv conversion");
      //      appendln(fb, "vec3 rgb2hsv( in vec3 c ) {");
      //      appendln(fb, "   vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);");
      //      appendln(fb, "   vec4 p = c.g < c.b ? vec4(c.bg, K.wz) : vec4(c.gb, K.xy);");
      //      appendln(fb, "   vec4 q = c.r < p.x ? vec4(p.xyw, c.r) : vec4(c.r, p.yzx);");
      //      appendln(fb, "   float d = q.x - min(q.w, q.y);");
      //      appendln(fb, "   float e = 1.0e-10;");
      //      appendln(fb, "   return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);");
      //      appendln(fb, "}");
      //      appendln(fb);
      appendln(fb, "// rgba to hsva conversion");
      appendln(fb, "vec3 rgba2hsva( in vec4 c ) {");
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
      //      appendln(fb, "// hsv to rgb conversion");
      //      appendln(fb, "vec3 hsv2rgb( in vec3 c ) {");
      //      appendln(fb, "   vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);");
      //      appendln(fb, "   vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);");
      //      appendln(fb, "   return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);");
      //      appendln(fb, "}");
      //      appendln(fb);
      appendln(fb, "// hsva to rgba conversion");
      appendln(fb, "vec3 hsva2rgba( in vec4 c ) {");
      appendln(fb, "   vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);");
      appendln(fb, "   vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);");
      appendln(fb, "   return vec4(c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y), c.a);");
      appendln(fb, "}");
      appendln(fb);
   }

   private static void buildFragmentShader(StringBuilder[] sbs, GLSLInfo info) {
      buildFragmentShaderHeader(sbs[SHADER_GLOBALS], info);
      buildFragmentShaderFunctions(sbs[SHADER_FUNCTIONS], info);
      buildFragmentShaderMain(sbs[SHADER_MAIN], info);
   }

   private static void buildFragmentShaderHeader(StringBuilder hb, GLSLInfo info) {
      addFragmentInfo(hb, info);
      addFragmentLighting(hb, info.getShading(), info.numLights());
   }

   private static void addFragmentInfo(StringBuilder hb, GLSLInfo info) {
      
      appendln(hb, "// fragment color output");
      appendln(hb, "out vec4 fragment_color;");
      appendln(hb);
      
      InstancedRendering instanced = info.getInstancedRendering();
      boolean hasColors = (info.getColorInterpolation() != ColorInterpolation.NONE)
         && (info.hasVertexColors()
            || ((instanced == InstancedRendering.POINTS 
                 || instanced == InstancedRendering.FRAMES
                 || instanced == InstancedRendering.AFFINES) && info.hasInstanceColors())
            || (instanced == InstancedRendering.LINES && info.hasLineColors() ) );

      boolean hasTextures = info.hasVertexTextures()
           || ((instanced == InstancedRendering.POINTS 
                 || instanced == InstancedRendering.FRAMES
                 || instanced == InstancedRendering.AFFINES) && info.hasInstanceTextures())
           || (instanced == InstancedRendering.LINES && info.hasLineTextures() );
      
      if (hasColors) {
         appendln(hb, "// fragment colors from vertex shader");
         appendln(hb, "in FragmentColorData {");
         appendln(hb, "   vec4 color;");
         appendln(hb, "} fragmentColorIn;");
         appendln(hb);
      }
      
      if (hasTextures) {
         appendln(hb, "// texture info");
         appendln(hb, "uniform sampler2D texture0;");
         appendln(hb, "in TextureData {");
         appendln(hb, "   vec2 texcoord;");
         appendln(hb, "} textureIn;");
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

      if ( (shading == Shading.PHONG) && (nLights > 0) ) {
         // we need all light info
         appendLightSourceStruct(hb);
         appendln(hb,"layout ("+UNIFORM_LAYOUT+") uniform Lights {");
         appendln(hb,"   LightSource light[" + nLights + "];");
         appendln(hb,"   float intensity_scale;  // for HDR->LDR, initialized to one");
         appendln(hb,"};");
         appendln(hb);
      }
   }

   private static void addFragmentLightingInputs(StringBuilder hb, Shading shading,
      int nLights) {
      
      switch (shading) {
         case FLAT:
            appendln(hb,"// light reflectance color");
            appendln(hb,"// required for modulation with vertex colors or texture");
            appendln(hb,"in LightColorData {");
            appendln(hb,"   flat vec3 front_diffuse;  ");
            appendln(hb,"   flat vec3 front_ambient;  ");
            appendln(hb,"   flat vec3 front_specular;  ");
            appendln(hb,"   flat vec3 back_diffuse;  ");
            appendln(hb,"   flat vec3 back_ambient;  ");
            appendln(hb,"   flat vec3 back_specular;  ");
            appendln(hb,"} fragmentLightIn;");
            appendln(hb);
            break;
         case GOURAUD:
            appendln(hb,"// light reflectance color");
            appendln(hb,"// required for modulation with vertex colors or texture");
            appendln(hb,"in LightColorData {");
            appendln(hb,"   vec3 front_diffuse;  ");
            appendln(hb,"   vec3 front_ambient;  ");
            appendln(hb,"   vec3 front_specular;  ");
            appendln(hb,"   vec3 back_diffuse;  ");
            appendln(hb,"   vec3 back_ambient;  ");
            appendln(hb,"   vec3 back_specular;  ");
            appendln(hb,"} fragmentLightIn;");
            appendln(hb);
            break;
         case PHONG:
            if (nLights > 0) {
               appendln(hb,"// directions for per-fragment lighting");
               appendln(hb,"in FragmentDirData {");
               appendln(hb, "   vec3 normal;");
               appendln(hb, "   vec3 to_eye;");               
               appendln(hb,"} fragmentDirIn;");
               appendln(hb);
            }
            break;
         case NONE:
            break;
      }
   }

   private static void buildFragmentShaderFunctions(StringBuilder fb, GLSLInfo info) {

      if (info.getShading() == Shading.PHONG) {
         addBlinnPhong(fb);
      }

      if (info.getColorInterpolation() == ColorInterpolation.HSV) {
         addHSVtoRGB(fb);
      }

   }

   private static void buildFragmentShaderMain(StringBuilder mb, GLSLInfo info) {

      appendln(mb, "// main fragment shader");
      appendln(mb, "void main() {");
      appendln(mb, "");
      
      // do lighting computations
      if (info.getShading() != Shading.NONE) {
         appendln(mb, "   vec3 diffuse, ambient, specular, emission;");
         appendln(mb, "   Material material;");
         switch (info.getShading()) {
            case FLAT:
            case GOURAUD:
               if (info.numLights() > 0) {
                  appendln(mb, "   // imported per-vertex lighting");
                  appendln(mb, "   if( gl_FrontFacing ) {");
                  appendln(mb, "      diffuse  = fragmentLightIn.front_diffuse;");
                  appendln(mb, "      ambient  = fragmentLightIn.front_ambient;");
                  appendln(mb, "      specular = fragmentLightIn.front_specular;");
                  appendln(mb, "      material = front_material;");
                  appendln(mb, "   } else {");
                  appendln(mb, "      diffuse  = fragmentLightIn.back_diffuse;");
                  appendln(mb, "      ambient  = fragmentLightIn.back_ambient;");
                  appendln(mb, "      specular = fragmentLightIn.back_specular;");
                  appendln(mb, "      material = back_material;");
                  appendln(mb, "   }");
                  appendln(mb);
               } else {
                  appendln(mb, "   if( gl_FrontFacing ) {");
                  appendln(mb, "      material = front_material;");
                  appendln(mb, "   } else {");
                  appendln(mb, "      material = back_material;");
                  appendln(mb, "   }");
                  appendln(mb, "   diffuse  = vec3(0.0);");
                  appendln(mb, "   ambient  = vec3(0.0);");
                  appendln(mb, "   specular = vec3(0.0);");
                  appendln(mb);
               }
               break;
            case PHONG:
               if ( info.numLights() > 0) {
                  appendln(mb, "   // fragment normal and eye location for lighting");
                  appendln(mb, "   vec3 n = normalize(fragmentDirIn.normal);");
                  appendln(mb, "   vec3 e = normalize(fragmentDirIn.to_eye);");
                  appendln(mb, "   ");
                  appendln(mb, "   // choose material based on face orientation");
                  appendln(mb, "   if (gl_FrontFacing) {");
                  appendln(mb, "      material = front_material;");
                  appendln(mb, "   } else {");
                  appendln(mb, "      material = back_material;");
                  appendln(mb, "      n = -n;  // flip fragment normal");
                  appendln(mb, "   }");
                  appendln(mb, "   ");
                  appendln(mb, "   // per-fragment lighting computations");
                  appendln(mb, "   for (int i=0; i<" + info.numLights() + "; ++i) {");
                  appendln(mb, "      vec3 light_to_vertex = -fragmentDirIn.to_eye-light[i].position.xyz;");
                  appendln(mb, "      float lightdist = length(light_to_vertex);");
                  appendln(mb, "      light_to_vertex = normalize(light_to_vertex);");                  
                  appendln(mb, "      // determine direction either from point or direction using direction indicator");
                  appendln(mb, "      vec3 light_direction = mix(light[i].direction.xyz, light_to_vertex, light[i].position.w);");
                  appendln(mb, "      ");
                  appendln(mb, "      float spotatt = 1.0;  // spot attentuation initially zero");
                  appendln(mb, "      float coslimit = light[i].direction.w;");
                  appendln(mb, "      if (coslimit > -1) {");
                  appendln(mb, "         // check angle");
                  appendln(mb, "         float coslight = dot(light_direction, light_to_vertex);");
                  appendln(mb, "         // cosine range, rescaled to [0,1)");
                  appendln(mb, "         float ccut = max((coslight-coslimit), 0)/(1-coslimit+1e-10);");
                  appendln(mb, "         // radial distance cut-off");
                  appendln(mb, "         float rcut = 1-lightdist*lightdist*(1-coslight*coslight)/(coslimit*coslimit+1e-10);");
                  appendln(mb, "         // choose between angle and radial");
                  appendln(mb, "         spotatt = max(mix(rcut, ccut, light[i].position.w), 0);");
                  appendln(mb, "         spotatt = pow(spotatt, light[i].attenuation.w);");
                  appendln(mb, "      }");
                  appendln(mb, "      ");
                  appendln(mb, "      // distance attenuation doesn't affect directional lights");
                  appendln(mb, "      float att = mix(1.0, 1.0 / (light[i].attenuation.x + light[i].attenuation.y*lightdist +");
                  appendln(mb, "         light[i].attenuation.z*lightdist*lightdist), light[i].position.w);");
                  appendln(mb, "      att *= spotatt;  // combine into a single attenuation parameter");
                  appendln(mb, "      ");
                  appendln(mb, "      vec2 ds = blinnPhongCoeffs( n, -light_direction, e, material.specular.a);");
                  appendln(mb, "      diffuse  += intensity_scale*att*ds.x*light[i].diffuse.rgb;");
                  appendln(mb, "      specular += intensity_scale*att*ds.y*light[i].specular.rgb;");
                  appendln(mb, "      ambient  += intensity_scale*light[i].ambient.rgb;");
                  //                  appendln(mb, "      diffuse  = max(att*ds.x*light[i].diffuse.rgb, diffuse);");
                  //                  appendln(mb, "      specular = max(att*ds.y*light[i].specular.rgb, specular);");
                  //                  appendln(mb, "      ambient  = max(light[i].ambient.rgb, ambient);");
                  appendln(mb, "      ");
                  appendln(mb, "   }");
                  appendln(mb, "   ");
               } else {
                  appendln(mb, "   if( gl_FrontFacing ) {");
                  appendln(mb, "      material = front_material;");
                  appendln(mb, "   } else {");
                  appendln(mb, "      material = back_material;");
                  appendln(mb, "   }");
                  appendln(mb, "   diffuse  = vec3(0.0);");
                  appendln(mb, "   ambient  = vec3(0.0);");
                  appendln(mb, "   specular = vec3(0.0);");
                  appendln(mb);
               }
               break;
            case NONE:
               break;
         }
      }
      
      // combine colors
      InstancedRendering instanced = info.getInstancedRendering();
      ColorInterpolation cinterp = info.getColorInterpolation();
      boolean hasColors = (cinterp != ColorInterpolation.NONE)
         && (info.hasVertexColors()
            || ((instanced == InstancedRendering.POINTS 
                 || instanced == InstancedRendering.FRAMES
                 || instanced == InstancedRendering.AFFINES) && info.hasInstanceColors())
            || (instanced == InstancedRendering.LINES && info.hasLineColors() ) );

      boolean hasTextures = info.hasVertexTextures()
              || ((instanced == InstancedRendering.POINTS 
                    || instanced == InstancedRendering.FRAMES
                    || instanced == InstancedRendering.AFFINES) && info.hasInstanceTextures())
              || (instanced == InstancedRendering.LINES && info.hasLineTextures() );

      appendln(mb, "   // combine colors");
      appendln(mb, "   vec4 color;");
      if (info.getShading() == Shading.NONE) {
         // use material color directly
         if (hasColors) {
            if (cinterp == ColorInterpolation.HSV) {
               appendln(mb, "   color = hsva2rgba(fragmentColorIn.color);");
            } else {
               appendln(mb, "   color = fragmentColorIn.color;");
            }
         } else {
            appendln(mb, "   if( gl_FrontFacing ) {");
            appendln(mb, "      color = front_material.diffuse;");
            appendln(mb, "   } else {");
            appendln(mb, "      color = back_material.diffuse;");
            appendln(mb, "   }");
         }
      } else {
         if (hasColors) {
            // replace diffuse
            if (cinterp == ColorInterpolation.HSV) {
               appendln(mb, "   vec4 diffuse_color = hsva2rgba(fragmentColorIn.color);");
            } else {
               appendln(mb, "   vec4 diffuse_color = fragmentColorIn.color;");
            }
         } else {
            appendln(mb, "   vec4 diffuse_color = material.diffuse;");
         }
         appendln(mb, "   diffuse  = diffuse_color.rgb*diffuse;");
         appendln(mb, "   specular = material.specular.rgb*specular;");
         appendln(mb, "   // mix ambient and diffuse based on ambient weighting");
         appendln(mb, "   ambient  = mix(material.ambient.rgb, diffuse_color.rgb, material.ambient.a)*ambient;");
         appendln(mb, "   emission = material.emission.rgb;");
         appendln(mb, "   color = vec4(max(diffuse+specular+emission, ambient), diffuse_color.a);");
      }
      
      if (hasTextures) {
         appendln(mb, "   // grab texture color and modulate");
         appendln(mb, "   vec4 texture_color = texture( texture0, textureIn.texcoord );");
         appendln(mb, "   color *= texture_color;");
      }
      //      appendln(mb, "   // gamma correction");
      //      appendln(mb, "   vec3 gamma = vec3(1.0/2.2);");
      //      appendln(mb, "   color = vec4(pow(color.rgb, gamma), color.a);");
      appendln(mb, "   fragment_color = clamp(color, 0, 1);");

      appendln(mb, "}");

   }

   public static void main(String[] args) {

      GLSLInfo proginfo = new GLSLInfo(1, 0, Shading.NONE, ColorInterpolation.RGB, 
         false, false, false, InstancedRendering.LINES,
            false, false, false, false, false);
      String[] shaders = getShaderScripts(proginfo);

      System.out.println("Vertex Shader: ");
      System.out.println(shaders[VERTEX_SHADER]);

      System.out.println("Fragment Shader: ");
      System.out.println(shaders[FRAGMENT_SHADER]);
   }


}
