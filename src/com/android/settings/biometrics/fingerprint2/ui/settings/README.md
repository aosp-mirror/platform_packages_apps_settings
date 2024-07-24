### Fingerprint Settings Enrollment

#### Entry Point (For Fingerprint Settings)

* [SecuritySettings] (https://cs.android.com/android/platform/superproject/+/master:packages/apps/Settings/src/com/android/settings/security/SecuritySettings.java;l=40?q=SecuritySettings)

#### General Architecture

The code should follow the MVVM architecture.

The FingerprintSettingsV2Fragment is responsible for most of the heavy lifting. It should coordinate
navigation events, maintain the viewmodels, and launch new activities if need be.

#### Style

* Please use [kfmt](https://plugins.jetbrains.com/plugin/14912-ktfmt)

