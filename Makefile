PACKAGE = de.markusfisch.android.shadereditor
APK = ShaderEditor/build/outputs/apk/ShaderEditor-debug.apk

all: debug install start

debug:
	./gradlew assembleDebug

lint:
	./gradlew lintDebug

apk:
	./gradlew build

infer:
	infer -- ./gradlew build

install:
	adb $(TARGET) install -rk $(APK)

start:
	adb $(TARGET) shell 'am start -n $(PACKAGE)/.activity.MainActivity'

uninstall:
	adb $(TARGET) uninstall $(PACKAGE)

images:
	svg/update.sh

clean:
	./gradlew clean
