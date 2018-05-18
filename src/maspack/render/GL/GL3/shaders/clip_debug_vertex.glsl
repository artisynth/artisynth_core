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

// per-vertex color info
out ColorData {
   vec4 diffuse;
} colorOut;

// clipping planes
struct ClipPlane {
   vec4 plane;           // ax+by+cz+d=0
};
layout (std140) uniform ClipPlanes {
   ClipPlane clip_plane[4];
};
out float gl_ClipDistance[4];

// main vertex shader
void main() {

   vec3 position = vertex_position;
   // vertex output
   gl_Position = pvm_matrix * vec4(position, 1.0);

   // clipping planes, in world coordinates
   vec4 wpos = m_matrix * vec4(position, 1.0);
   for (int i=0; i<4; ++i) {
      // gl_ClipDistance[i] = 1.0;
      gl_ClipDistance[i] = dot(wpos, clip_plane[i].plane);
   }
   colorOut.diffuse.r = dot(wpos, clip_plane[0].plane);
   colorOut.diffuse.g = dot(wpos, clip_plane[1].plane);
   colorOut.diffuse.b = dot(wpos, clip_plane[2].plane);
   colorOut.diffuse.a = dot(wpos, clip_plane[3].plane);

}