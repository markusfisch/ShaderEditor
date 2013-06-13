Shader Editor
=============

Create and edit [GLSL](https://en.wikipedia.org/wiki/GLSL) shaders on your Android phone or tablet and use them as live wallpaper.

[![Google Play](http://developer.android.com/images/brand/en_generic_rgb_wo_45.png)](https://play.google.com/store/apps/details?id=de.markusfisch.android.shadereditor)

Features
--------

* Live preview
* Syntax highlighting
* Error highlighting
* FPS display
* Use any shader as _live wallpaper_
* Exposure of _accelerometer sensor_

Tips
----

### How many FPS should I get to set my shader as live wallpaper?

Some devices do limit the GPU to consume less power when not plugged in. That's why you should always check your FPS in full screen (use the eye icon in the upper left) with the power cord off. Your shader should make at least 30 FPS or so to not slow down the UI. Low resolution displays will produce better FPS as there's just a lot less to calculate.

### How much battery will a live wallpaper take?

A live wallpaper should only consume battery when you see it. So it generally depends on how often and how long you look at it. With normal use, you shouldn't note a difference in battery consumption.

### Errors are not highlighted

Unfortunately error information is disabled on some devices (e.g. Huawei Ideos X3). Error highlighting/reporting is not available on these devices.
