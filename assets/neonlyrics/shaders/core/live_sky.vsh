#version 330
#extension GL_ARB_separate_shader_objects : require

// 26.3 (Renderpearl): core shaders cannot use #moj_import, so the uniform blocks are
// declared inline with the exact layout Minecraft binds them with.
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

uniform sampler2D Sampler0;

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;

layout(location = 0) out vec3 skyDir;
layout(location = 1) out vec3 skyInfo;

float sky_u16(int i, bool second) {
    vec4 p = texelFetch(Sampler0, ivec2(i, 0), 0);
    vec2 b = second ? p.ba : p.rg;
    return (floor(b.x * 255.0 + 0.5) * 256.0 + floor(b.y * 255.0 + 0.5)) / 65535.0 * 4.0 - 2.0;
}

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    // Position ist schon mit der Pose gedreht - zurueck in Welt-Achsen (Inverse steht in Pixel 8..12)
    mat3 inv = mat3(
        sky_u16(8, false), sky_u16(8, true), sky_u16(9, false),
        sky_u16(9, true), sky_u16(10, false), sky_u16(10, true),
        sky_u16(11, false), sky_u16(11, true), sky_u16(12, false));
    skyDir = inv * Position;
    // x = Himmelszeit, y = Helligkeit, z = Deckkraft
    skyInfo = vec3(UV0.x, Color.r * 2.0, Color.a);
}
