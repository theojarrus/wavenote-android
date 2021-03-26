# Wavenote for Android
[![Version](https://img.shields.io/badge/version-3.5-blue)](https://play.google.com/store/apps/details?id=com.theost.wavenote)

Wavenote - notepad for musicians. Based on [Simplenote](https://simplenote.com).

## How to Configure

* Clone repository.
```shell
git clone https://github.com/theo-jedi/wavenote-android.git
cd wavenote-android
```

* Import into Android Studio using the Gradle build option. You may need to create a `local.properties` file with the absolute path to the Android SDK. Sample `local.properties`:
```
sdk.dir=/Applications/Android Studio.app/sdk
```

* Install debug build with Android Studio or command line with:
```shell
./gradlew installDebug
```

* Create a new account in order to use a development build. Logging in with an existing Simplenote account won't work.

## Android Wear

To properly install the wear app, run `./gradlew assembleRelease` to package up the app and then `adb install` with the generated .apk to the host device.

If you want to debug the Wear app, simply connect the device to adb and then run the `Wear` project from Android Studio.
