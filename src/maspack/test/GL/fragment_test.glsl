#version 330

// fragment color output
out vec4 fragment_color;

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
   flat vec3 front_diffuse;  
} lightIn;

// main fragment shader
void main() {

   vec3 diffuse = vec3(0.0);
   Material material = front_material;

   diffuse  = lightIn.front_diffuse;

   // compute final color, starting with material
   vec4 fdiffuse = material.diffuse;

   // apply lighting
   diffuse  = fdiffuse.rgb*diffuse*material.power.y;
   fragment_color = vec4(diffuse, 1.0);

}
