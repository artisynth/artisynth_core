#version 330

// PVM matrices
layout (std140) uniform Matrices {
   mat4 pvm_matrix;     // model to screen
   mat4 vm_matrix;      // model to viewer
   mat4 m_matrix;       // model to world
   mat4 normal_matrix;  // model to viewer normal
   mat4 texture_matrix; // texture coordinate transform
};

// vertex inputs
in vec3 vertex_position;
in vec3 vertex_normal;

// material properties
struct Material {
   vec4 diffuse;   // alpha is diffuse.a
   vec4 specular;  // shininess is specular.a
   vec4 emission;  
   vec4 power;     // scales for ambient/diffuse/specular/emission
};
// per-object materials
layout (std140) uniform Materials {
   Material front_material;
   Material back_material;
};

// light source info, in camera space
struct LightSource {
   vec4 ambient;
   vec4 diffuse;
   vec4 specular;
   vec4 position;        // (pos, point indicator)
   vec4 direction;       // (dir, spot cutoff), cutoff < -1 -> point
   vec4 attenuation;     // (exponent, constant, linear, quadratic)
};
layout (std140) uniform Lights {
   LightSource light[3];
   float intensity_scale;  // for HDR->LDR, initialized to one
};

// light colors
// required for modulation with vertex colors or texture
flat out LightColorData {
   vec3 front_diffuse;  
} lightOut;

// main vertex shader
void main() {

   vec3 position;  // transformed vertex position
   vec3 normal;  // transformed vertex normal

   position = vertex_position;
   normal = vertex_normal;
   // vertex output
   gl_Position = pvm_matrix * vec4(position, 1.0);

   // per-vertex lighting computations
   // compute camera position/normal
   vec4 camera_position = vm_matrix * vec4(position, 1.0);
   vec4 camera_normal = normal_matrix * vec4(normal, 0.0);
   vec3 nfront = normalize(camera_normal.xyz);
   vec3 eye = normalize(-camera_position.xyz);
   
   // accumulated light colors
   vec3 fldiff = vec3(0.0);  // front
   
   // lights
   for (int i=0; i<3; ++i) {
      vec3  light_to_vertex = camera_position.xyz-light[i].position.xyz;
      float lightdist = length(light_to_vertex);
      light_to_vertex = light_to_vertex/lightdist;
      vec3 light_direction = normalize(light[i].direction.xyz);

      // determine direction either from point or direction using direction 
      // indicator
      light_direction = 
          mix(light_direction, light_to_vertex, light[i].position.w);

      float intensity = max(dot(nfront, -light_direction), 0.0);
      fldiff += intensity_scale*intensity*light[i].diffuse.rgb;
   }
   
   // accumulate
   lightOut.front_diffuse  = fldiff;

}
