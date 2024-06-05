### Fingerprint Settings Enrollment

#### Entry Points (To FingerprintEnrollment)

* FingerprintSettings (which launches the below intent)
* Intent -> ".biometrics.fingerprint2.ui.enrollment.activity.FingerprintEnrollmentV2Activity")

#### General Architecture

The code should follow the MVVM architecture.

**In addition, one activity (FingerprintEnrollmentV2Activity) should**

* Control a list of fragments which correspond to enrollment steps
* Be responsible for navigation events between fragments
* Be responsible for navigation events to other activities if need be (
  ConfirmDeviceCredentialActivity)
* Be the controller of the viewmodels

#### Style

* Please use [kfmt](https://plugins.jetbrains.com/plugin/14912-ktfmt)

