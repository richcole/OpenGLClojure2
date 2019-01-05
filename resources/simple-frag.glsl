#version 430

in vec2 frag_uv;
out vec4 color;
uniform sampler2D tex;

void main(void)
{
  color = texture(tex, frag_uv);
}
