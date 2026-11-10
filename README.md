# BEDO Smart Factory

Android monitor for the Smart Factory Integrated Production Line.

Open this repository in Android Studio with JDK 17 or newer and Android SDK 35. Use Gradle 8.9, then run the `app` configuration or `gradle :app:assembleDebug`.

Demo mode opens by default. To test a module, join its TH-Module Wi-Fi network in Android Settings and switch Demo off. The app reads temperature and humidity from `http://192.168.4.1/data` while the screen is active. The firmware schema is isolated in `Telemetry.kt`.
