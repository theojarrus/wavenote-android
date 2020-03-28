# Wavenote for Android
[![Version](https://img.shields.io/badge/version-2.4-blue.svg)](https://github.com/fedor-jedi/wavenote-android/releases/tag/2.4)

Wavenote - notepad for musicians. Based on [Simplenote](https://simplenote.com).

## How to Configure

* Clone repository.
```shell
git clone https://github.com/fedor-jedi/wavenote-android.git
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

* Create a new account in order to use a development build. Logging in with an existing Wavenote account won't work. Use the account for **testing purposes only** as all note data will be periodically cleared out on the server.

_Note: Wavenote API features such as sharing and publishing will not work with development builds._

## Android Wear

To properly install the wear app, run `./gradlew assembleRelease` to package up the app and then `adb install` with the generated .apk to the host device.

If you want to debug the Wear app, simply connect the device to adb and then run the `Wear` project from Android Studio.
