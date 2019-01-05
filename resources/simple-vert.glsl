#version 430

layout(location = 0) in vec3 positions;
layout(location = 1) in vec3 normals;
layout(location = 2) in vec2 uv;

uniform mat4 view_tr;
uniform mat4 model_tr;

out vec2 frag_uv;

void main(void)
{
  gl_Position = view_tr * model_tr * vec4(positions, 1);
  frag_uv = uv;
}
