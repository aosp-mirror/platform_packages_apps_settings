# Background info about tests
1. This test is ran in postsubmits at andoid-settings/robo_tests.gcl
2. It is important that this module stays somewhat small, if the test size grows
   too large, it will be likely that this suite breaks due to flakiness(which
   tends to happen with screenshot tests). In this case investigate splitting
   the module.

#  Running and updating screenshots.
1. For FingerprintEnrollIntroScreenshotTest.kt#testEnrollIntro
2. atest SettingsScreenshotRNGTests
3. There should be a file like com.android.settings.tests.screenshot.biometrics.fingerprint.fragment.FingerprintEnrollIntroScreenshotTest_testEnrollIntro_actual_robolectric_fp_enroll_intro.png_6245562387930305138.png
4. Place this screenshot in packages/apps/Settings/tests/screenshot/assets/robolectric/fp_enroll_intro.png
