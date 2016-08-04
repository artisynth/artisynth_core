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
in vec4 vertex_color;

// per-vertex color info
out ColorData {
   vec4 diffuse;
} colorOut;

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
out LightColorData {
   flat vec3 front_ambient;  
   flat vec3 front_diffuse;  
   flat vec3 front_specular;  
   flat vec3 back_ambient;  
   flat vec3 back_diffuse;  
   flat vec3 back_specular;  
} lightOut;

// Blinn-Phong lighting equation coefficients, (diffuse, specular)
vec2 blinnPhongCoeffs(in vec3 ndir, in vec3 ldir, in vec3 edir, in float specExponent) {
   
   float intensity = max(dot(ndir,ldir), 0.0);
   
   if (intensity > 0) {
      // compute the half vector
      vec3 h = normalize(ldir + edir);
      float shine = (specExponent == 0 ? 1 :pow(max(dot(h,ndir), 0.0), specExponent));
      return vec2(intensity, shine);
   }
   return vec2(0.0);
}

// main vertex shader
void main() {

   vec3 position;  // transformed vertex position
   vec3 normal;  // transformed vertex normal

   position = vertex_position;
   normal = vertex_normal;
   // vertex output
   gl_Position = pvm_matrix * vec4(position, 1.0);

   colorOut.diffuse = vertex_color;

   // per-vertex lighting computations
   // compute camera position/normal
   vec4 camera_position = vm_matrix * vec4(position, 1.0);
   vec4 camera_normal = normal_matrix * vec4(normal, 0.0);
   vec3 nfront = normalize(camera_normal.xyz);
   vec3 nback = -nfront;
   vec3 eye = normalize(-camera_position.xyz);
   
   // accumulated light colors
   vec3 fldiff = vec3(0.0);  // front
   vec3 flambi = vec3(0.0);
   vec3 flspec = vec3(0.0);
   vec3 bldiff = vec3(0.0);  // back
   vec3 blambi = vec3(0.0);
   vec3 blspec = vec3(0.0);
   
   // lights
   for (int i=0; i<3; ++i) {
      vec3  light_to_vertex = camera_position.xyz-light[i].position.xyz;
      float lightdist = length(light_to_vertex);
      light_to_vertex = light_to_vertex/lightdist;
      vec3 light_direction = normalize(light[i].direction.xyz);
      
      float spotatt = 1.0;  // spot attentuation initially zero if non-spotlight
      float coslimit = light[i].direction.w;
      if (coslimit > 0) {
         // check angle
         float coslight = dot(light_direction, light_to_vertex);
         if (coslight < coslimit) {
            spotatt = 0;
         } else {
            spotatt = pow(coslight, light[i].attenuation.w);
         }
      }
      
      // distance attenuation doesn't affect directional lights
      float att = mix(1.0, 1.0 / (light[i].attenuation.x + light[i].attenuation.y*lightdist +
         light[i].attenuation.z*lightdist*lightdist), light[i].position.w);
      att *= spotatt;  // combine into a single attenuation parameter
      
      // determine direction either from point or direction using direction indicator
      light_direction = mix(light_direction, light_to_vertex, light[i].position.w);
      vec2 ds = blinnPhongCoeffs( nfront, -light_direction, eye, front_material.specular.a);
      flambi += intensity_scale*light[i].ambient.rgb;
      fldiff += intensity_scale*att*ds.x*light[i].diffuse.rgb;
      flspec += intensity_scale*att*ds.y*light[i].specular.rgb;

      ds = blinnPhongCoeffs( nback, -light_direction, eye, back_material.specular.a);
      blambi += intensity_scale*light[i].ambient.rgb;
      bldiff += intensity_scale*att*ds.x*light[i].diffuse.rgb;
      blspec += intensity_scale*att*ds.y*light[i].specular.rgb;
   }
   
   // accumulate
   lightOut.front_ambient  = flambi;
   lightOut.front_diffuse  = fldiff;
   lightOut.front_specular = flspec;
   lightOut.back_ambient   = blambi;
   lightOut.back_diffuse   = bldiff;
   lightOut.back_specular  = blspec;

}