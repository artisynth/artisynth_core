#version 330

// vertex inputs
layout(location = 0) in vec3 vertex_position;

// main vertex shader
void main() {

   // vertex output
   gl_Position = vec4(vertex_position, 1.0);

}