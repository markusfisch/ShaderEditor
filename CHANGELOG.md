# Change Log

## 2.16.0
* Add uniform for rotation vector sensor
* Alternatively calculate rotation matrix from rotation vector if necessary
* Fix using accelerometer sensor to calculate gravity

## 2.15.0
* Adds frame count uniform

## 2.14.0
* Adds import and export for shaders

## 2.13.0
* Adds a preference to auto save changes

## 2.12.0
* Adds a preference to disable syntax highlighting for large shaders

## 2.11.0
* New app icon
* Added adaptive app icon for Android 8.0
* Added FAQ menu item to answer common questions
* Added swirl gun example
* Shows a message for "Set wallpaper" when not set as live wallpaper

## 2.10.6
* Expose rotation and inclination matrix as uniforms
* Rename rotation uniform to gyroscope

## 2.10.5
* Fallback to old gravity and linear acceleration calculation if necessary
* Add uniform for geomagnetic inclination angle

## 2.10.4
* Fix remapping of orientation uniform
* Use system-calculated gravity and linear acceleration data

## 2.10.3
* Insert preprocessor directives after #version

## 2.10.2
* Remove non-ascii characters from pasted source code

## 2.10.1
* Match version declaration of vertex shader

## 2.10.0
* Try to acquire a GLES 3.0 context if available

## 2.9.1
* Fix broken backbuffer preset selection
* Fix crash when setting backbuffer parameters

## 2.9.0
* Choose any shader as the template for new shaders
* Remap orientation uniform when device is rotated
* Show shader name in settings summary too
* Fixed initializing camera

## 2.8.1
* Fixed clearing empty backbuffer
* Fixed floating action button icon color

## 2.8.0
* Set a texture for the initial backbuffer
* Replace already existing uniform statements when adding them
* Samples can have a lower quality setting

## 2.7.0
* Collection of sample shaders added
* Use front or back camera as texture
* New uniforms to rotate camera frame to match with device orientation
* Give shaders a name
* Use a low-pass filter on the orientation uniform

## 2.6.1
* Use previous defaults for sampler texture params
* Converted shader samples indenting style

## 2.6.0
* Support custom texture parameters for each sampler
* Removed laser lines shader

## 2.5.3
* Enable/disable listening for battery events in wallpaper service

## 2.5.2
* Fixed terminating wallpaper service after use
* Fixed reusing list items

## 2.5.1
* Added a splash screen to give feedback while loading
* Fixed input type of editor to allow GBoard swipe shortcuts
* Fixed loading of external images on Yotaphone 2

## 2.5.0
* Added orientation uniform exposing azimuth, pitch and roll
* Fixed rotation uniform
* Improved description for rotation and magnetic sensors
* Sorted preset uniforms by name for a better overview
* Double tap zooms/restores in texture viewer
* Reformatted to standard Android coding style
* Use left/right padding for editor only

## 2.4.6
* Set current shader as wallpaper shader

## 2.4.5
* New setting to open preview activity in new task

## 2.4.4
* Support Nougat's multi-window mode
* Removed superfluous ScrollView around EditText
* Update highlighting after setting an error line

## 2.4.3
* Made ftime's period definable in shader source

## 2.4.2
* New second and subsecond uniform, changed ftime
* Extra uniform to transfer fractional part of time
* Fixed date uniform, now updates properly every second

## 2.4.1
* Fixed inserting code from texture fragment

## 2.4.0
* Support for samplerCube
* Configurable tab width
* Deep Link for App Indexing
* Run lint and FindBugs when building for release
* Renamed project directory to app

## 2.3.1
* Added date/time and startRandom uniform
* Allow underscores and hyphens in texture names
* Show texture name in texture view
* Add READ_EXTERNAL_STORAGE permission for API < 19

## 2.3.0
* Exposure of light, pressure and proximity sensors
* Expose magnetic field sensor
* Fixed binding of multiple textures
* Create sensor listeners on demand

## 2.2.0
* Add uniforms by picking them form a list
* Moved quality spinner to the right/end
* Fixed monitoring battery level
* Drop glClear() to preserve pixel buffer
* Only show update wallpaper if set as wallpaper

## 2.1.0
* Change render resolution/quality
* Fixed inserting sampler2D statement

## 2.0.3
* Handle out of memory exception

## 2.0.2
* Better names for time constants and labels
* Additional sample shaders
* Put ImageView's behind translucent system bars

## 2.0.1
* Button to rotate source image for texture
* Intent filter for text/plain and image/\*

## 2.0.0
* Material Design refactoring
* Import and use textures from images on the device
* Update wallpaper shader from menu
* Wrap time variable
* Use highp where available
* Run shader in extra activity
* Support multiple touches
* Turn wallpaper black when battery is low
* Converted to gradle

## 1.6.3
* Bind texture again for drawing into framebuffer

## 1.6.2
* Show overflow icon in action bar for Lollipop

## 1.6.1
* Show source if it's hidden and there's an error

## 1.6.0
* Fixed y-flipped rendering when using a frame buffer
* Support for gyroscope and battery

## 1.5.0
* Remove trailing white space on save
* Improved auto indenting

## 1.4.0
* Implemented back buffer
* Preference to change update delay of editor
* Preference to set editor text size

## 1.3.0
* Set rate of sensor events

## 1.2.0
* Improved auto-indenting
* Exposed wallpaper offset

## 1.1.2
* Fixed dead-lock when no program is running

## 1.1.1
* Removed String.isEmpty()

## 1.1.0
* Show FPS in spinner label
* Added FPS gauge
* Added simple auto indenting
