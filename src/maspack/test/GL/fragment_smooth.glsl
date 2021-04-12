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

// directions for per-fragment lighting
in SurfaceData {
   vec3 normal;
   vec3 to_eye;
} surfIn;

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

// main fragment shader
void main() {

   vec3 ambient = vec3(0.0);
   vec3 diffuse = vec3(0.0);
   vec3 specular = vec3(0.0);
   vec3 emission = vec3(0.0);
   Material material;
   // fragment normal and eye location for lighting
   vec3 normal = normalize(surfIn.normal);
   vec3 eye = normalize(surfIn.to_eye);
   
   // choose material based on face orientation
   if (gl_FrontFacing) {
      material = front_material;
   } else {
      material = back_material;
      normal = -normal;  // flip fragment normal
   }
   
   // per-fragment lighting computations
   for (int i=0; i<3; ++i) {
      vec3 light_to_vertex = -surfIn.to_eye-light[i].position.xyz;
      float lightdist = length(light_to_vertex);
      light_to_vertex = normalize(light_to_vertex);
      // determine direction either from point or direction using direction indicator
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
      
      // position vs directional light
      light_direction = mix(light_direction, light_to_vertex, light[i].position.w);
      vec2 ds = blinnPhongCoeffs( normal, -light_direction, eye, material.specular.a);
      ambient  += intensity_scale*light[i].ambient.rgb;
      diffuse  += intensity_scale*att*ds.x*light[i].diffuse.rgb;
      specular += intensity_scale*att*ds.y*light[i].specular.rgb;
      
   }
   
   // compute final color, starting with material
   vec4 fdiffuse = material.diffuse;
   vec3 fspecular = material.specular.rgb;
   vec3 femission = material.emission.rgb;

   // apply lighting
   ambient  = fdiffuse.rgb*ambient*material.power.x;
   diffuse  = fdiffuse.rgb*diffuse*material.power.y;
   specular = fspecular*specular*material.power.z;
   emission = femission*material.power.w;  // emission only material-related
   fragment_color = vec4(max(diffuse+specular+emission, ambient), fdiffuse.a);

}
