#version 330

// PVM matrices
layout (std140) uniform Matrices {
   mat4 pvm_matrix;     // model to screen
   mat4 vm_matrix;      // model to viewer
   mat4 m_matrix;       // model to world
   mat4 normal_matrix;  // model to viewer normal
};

// vertex inputs
in vec3 vertex_position;
in vec3 vertex_normal;

// directions for per-fragment lighting
out DirectionData {
   vec3 normal;
   vec3 to_eye;
} dirOut;

// main vertex shader
void main() {

   vec3 position;  // transformed vertex position
   vec3 normal;  // transformed vertex normal

   position = vertex_position;
   normal = vertex_normal;
   // vertex output
   gl_Position = pvm_matrix * vec4(position, 1.0);

   // per-fragment lighting info, vertex normal and eye directions
   vec4 camera_position = vm_matrix * vec4(position, 1.0);
   vec4 camera_normal = normal_matrix * vec4(normal, 0.0);
   dirOut.normal = normalize(camera_normal.xyz);
   dirOut.to_eye = -camera_position.xyz;

}
