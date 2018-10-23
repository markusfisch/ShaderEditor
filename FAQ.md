# Frequently Asked Questions

## How to use GLES 3.0?

Simply declare the GLES version in the first line of the shader:

	#version 300 es

GLES 3.0 must be available on your device for this work.

## How many FPS should I get to set my shader as live wallpaper?

Some devices limit GPU usage to consume less power when not plugged in.
Always check the performance with the soft keyboard hidden and the power
cord off. A shader should make at least around 30 fps to not slow down
the UI.

## How much battery will a live wallpaper take?

A live wallpaper should only consume battery when you see it.
So it generally depends on how often and how long you look at it.
With normal use, you shouldn't note a difference in battery consumption.

## What if errors are not highlighted?

Unfortunately error information is disabled on some devices (e.g. Huawei
Ideos X3, Asus Transformer). Error highlighting/reporting is not possible
on these devices.

## `Set wallpaper` doesn't work

You need to set Shader Editor as live wallpaper first. To do this, go to
(the system) Settings, Display, Wallpaper, Live Wallpapers and choose Shader
Editor as your live wallpaper. As soon as Shader Wallpaper is your live
wallpaper, you can change what shader is running within the app with those
menu entries ("Set as wallpaper" and "Update wallpaper" if the current shader
is the chosen wallpaper shader).

## Why can't both cameras be opened at the same time?

This is a system limitation. No app can open both cameras at the same time.

## `time` uniform loses accuracy / framerate drops over time

Most probably it's because `time` is just a float of medium precision
(unfortunately the default for fragment shaders).
Either try to specify high precision (what may not be available on your
hardware):

```glsl
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
```

Or use the `second`/`subsecond`/`ftime` uniforms instead.
See issue [#10](https://github.com/markusfisch/ShaderEditor/issues/10#issuecomment-160463706) for details.

## Where can I learn more about Shaders?

### Basics

* [The Book of Shaders](http://thebookofshaders.com/)
* [An Introduction to Shaders](https://aerotwist.com/tutorials/an-introduction-to-shaders-part-1/)

### Advanced

* [The OpenGL® ES Shading Language](https://www.khronos.org/files/opengles_shading_language.pdf)
* [OpenGL API documentation](http://docs.gl/)
* [GLSL Programming](https://en.wikibooks.org/wiki/GLSL_Programming)
* [Best Practices for Shaders](https://developer.apple.com/library/content/documentation/3DDrawing/Conceptual/OpenGLES_ProgrammingGuide/BestPracticesforShaders/BestPracticesforShaders.html)
