---
group: "camera"
order: 1
name: "Back Camera"
desc: "Use the live camera feed as a shader texture."
video: "media/sample-camera.webm"
poster: "media/sample-camera.webp"
links:
  - label: "Khronos — OES_EGL_image_external"
    href: "https://registry.khronos.org/OpenGL/extensions/OES/OES_EGL_image_external.txt"
  - label: "Android — CameraX overview"
    href: "https://developer.android.com/media/camera/camerax"
code: |-
  #ifdef GL_FRAGMENT_PRECISION_HIGH
  precision highp float;
  #else
  precision mediump float;
  #endif
  
  uniform vec2 resolution;
  uniform vec2 cameraAddent;
  uniform mat2 cameraOrientation;
  uniform samplerExternalOES cameraBack;
  
  void main(void) {
  	vec2 uv = gl_FragCoord.xy / resolution.xy;
  	vec2 st = cameraAddent + uv * cameraOrientation;
  
  	gl_FragColor = vec4(
  		texture2D(cameraBack, st).rgb,
  		1.0);
  }
---
ShaderEditor can stream the device camera directly into your shader as a texture. The back camera appears as `cameraBack` (front as `cameraFront`). These use `samplerExternalOES` instead of `sampler2D` because Android camera frames are external GL textures.

The catch: the camera's orientation doesn't always match the screen. ShaderEditor provides `cameraOrientation` (a mat2 rotation matrix) and `cameraAddent` (a vec2 offset) to correct the mapping. The formula `cameraAddent + uv * cameraOrientation` handles portrait/landscape and front/back differences automatically.

**How to use:** Add the camera uniform via menu → "Add uniform" → scroll down to "cameraBack" (needs camera permission).

**Try this:** Convert to grayscale: `vec3 c = texture2D(cameraBack, st).rgb; float g = dot(c, vec3(0.299, 0.587, 0.114));`. Or edge-detect by sampling neighboring pixels and comparing differences.