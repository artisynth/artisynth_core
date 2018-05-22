#version 330

// fragment color output
out vec4 fragment_color;

// fragment colors from previous shader
in ColorData {
   vec4 diffuse;
} colorIn;

// main fragment shader
void main() {
   float r = max(colorIn.diffuse.r, 0)/10;
   float g = max(-colorIn.diffuse.r, 0)/10;
   fragment_color = vec4(r, g, 0, 1.0);
}