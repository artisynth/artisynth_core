#version 330

// fragment color output
out vec4 fragment_color;

// directions for per-fragment lighting
in DirectionData {
   vec3 normal;
   vec3 to_eye;
} dirIn;

// main fragment shader
void main() {   
   fragment_color = vec4(dirIn.to_eye, 1.0);
   //fragment_color = vec4(1.0, 1.0, 1.0, 1.0);
}
