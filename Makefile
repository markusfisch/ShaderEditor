PACKAGE = de.markusfisch.android.shadereditor
APK = ShaderEditor/build/outputs/apk/ShaderEditor-debug.apk

all: apk install start

apk:
	./gradlew build

install:
	adb $(TARGET) install -rk $(APK)

start:
	adb shell 'am start -n $(PACKAGE)/.activity.MainActivity'

uninstall:
	adb uninstall $(PACKAGE)

images:
	svg/update.sh

clean:
	./gradlew clean
