PACKAGE = de.markusfisch.android.shadereditor
APK = ShaderEditor/build/outputs/apk/ShaderEditor-debug.apk

all: debug install start

debug:
	./gradlew assembleDebug

lint:
	./gradlew lintDebug

apk:
	./gradlew build

release:
	@./gradlew assembleRelease \
		-Pandroid.injected.signing.store.file=$(ANDROID_KEYFILE) \
		-Pandroid.injected.signing.store.password=$(ANDROID_STORE_PASSWORD) \
		-Pandroid.injected.signing.key.alias=$(ANDROID_KEY_ALIAS) \
		-Pandroid.injected.signing.key.password=$(ANDROID_KEY_PASSWORD)

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
