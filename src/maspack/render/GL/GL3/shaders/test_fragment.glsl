#version 330

// fragment color output
out vec4 fragment_color;

// fragment colors from previous shader
in ColorData {
   vec4 diffuse;
} colorIn;

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

// light reflectance color
// required for modulation with vertex colors or texture
in LightColorData {
   flat vec3 front_ambient;  
   flat vec3 front_diffuse;  
   flat vec3 front_specular;  
   flat vec3 back_ambient;  
   flat vec3 back_diffuse;  
   flat vec3 back_specular;  
} lightIn;

// main fragment shader
void main() {

   vec3 ambient, diffuse, specular, emission;
   Material material;
   // imported per-vertex lighting
   if( gl_FrontFacing ) {
      ambient  = lightIn.front_ambient;
      diffuse  = lightIn.front_diffuse;
      specular = lightIn.front_specular;
      material = front_material;
   } else {
      diffuse  = lightIn.back_diffuse;
      ambient  = lightIn.back_ambient;
      specular = lightIn.back_specular;
      material = back_material;
   }

   // compute final color, starting with material
   vec4 fdiffuse = material.diffuse;
   vec3 fspecular = material.specular.rgb;
   vec3 femission = material.emission.rgb;

   // incoming vertex color
   vec4 vcolor = colorIn.diffuse;
   // mix
   fdiffuse  = vcolor;     // replace
   femission = vcolor.rgb; // replace
   // apply lighting
   ambient  = fdiffuse.rgb*ambient*material.power.x;
   diffuse  = fdiffuse.rgb*diffuse*material.power.y;
   specular = fspecular*specular*material.power.z;
   emission = femission*material.power.w;
   fragment_color = vec4(max(emission, ambient), fdiffuse.a);

}