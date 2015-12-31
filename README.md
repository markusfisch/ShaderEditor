Shader Editor
=============

Create and edit [GLSL](https://en.wikipedia.org/wiki/GLSL) shaders on
your Android phone or tablet and use them as live wallpaper.

[![Download from Google Play](http://developer.android.com/images/brand/en_generic_rgb_wo_45.png)](https://play.google.com/store/apps/details?id=de.markusfisch.android.shadereditor)
[![Shader Editor on fdroid.org](https://f-droid.org/wiki/images/0/0f/F-Droid-button_smaller.png)](https://f-droid.org/repository/browse/?fdfilter=Shader+Editor&fdid=de.markusfisch.android.shadereditor)

Features
--------

* Live preview in background or on an extra screen
* Syntax highlighting
* Error highlighting
* FPS display
* Use any shader as __live wallpaper__
* Exposure of __accelerometer sensors__
* Support for __wallpaper offset__
* Exposure of __battery level__
* Supports __multiple touches__
* Supports multiple render resolutions
* Previous rendered frame in __backbuffer__ texture
* Import and use __arbitrary textures__
* Disables rendering when battery is low

Tips
----

### How many FPS should I get to set my shader as live wallpaper?

Some devices limit GPU usage to consume less power when not plugged in.
Always check the performance with the soft keyboard hidden and the power
cord off. A shader should make at least around 30 fps to not slow down
the UI.

### How much battery will a live wallpaper take?

A live wallpaper should only consume battery when you see it.
So it generally depends on how often and how long you look at it.
With normal use, you shouldn't note a difference in battery consumption.

### Errors are not highlighted

Unfortunately error information is disabled on some devices (e.g. Huawei
Ideos X3, Asus Transformer). Error highlighting/reporting is not possible
on these devices.
