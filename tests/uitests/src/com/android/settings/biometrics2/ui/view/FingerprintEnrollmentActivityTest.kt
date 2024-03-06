/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.biometrics2.ui.view

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.FEATURE_FINGERPRINT
import android.hardware.fingerprint.FingerprintManager
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal
import android.hardware.fingerprint.IFingerprintAuthenticatorsRegisteredCallback
import android.os.UserHandle
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.android.internal.widget.LockPatternChecker
import com.android.internal.widget.LockPatternUtils
import com.android.internal.widget.LockscreenCredential
import com.android.internal.widget.VerifyCredentialResponse
import com.android.settings.biometrics2.utils.LockScreenUtil
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@Ignore
@RunWith(AndroidJUnit4::class)
class FingerprintEnrollmentActivityTest {

    private val context: Context by lazy {
        InstrumentationRegistry.getInstrumentation().context
    }

    private val fingerprintManager: FingerprintManager by lazy {
        context.getSystemService(FingerprintManager::class.java)!!
    }

    private var fingerprintPropCallbackLaunched = false
    private var canAssumeUdfps = false
    private var canAssumeSfps = false
    private var enrollingPageTitle: String = ""
    private var runAsLandscape = false

    private val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Before
    @Throws(InterruptedException::class)
    fun setUp() {
        // Stop every test if it is not a fingerprint device
        Assume.assumeTrue(context.packageManager.hasSystemFeature(FEATURE_FINGERPRINT))

        fingerprintPropCallbackLaunched = false
        fingerprintManager.addAuthenticatorsRegisteredCallback(
            object : IFingerprintAuthenticatorsRegisteredCallback.Stub() {
                override fun onAllAuthenticatorsRegistered(
                    list: List<FingerprintSensorPropertiesInternal>
                ) {
                    fingerprintPropCallbackLaunched = true
                    assertThat(list).isNotNull()
                    assertThat(list).isNotEmpty()
                    val prop = list[0]
                    canAssumeUdfps = prop.isAnyUdfpsType
                    canAssumeSfps = prop.isAnySidefpsType
                    enrollingPageTitle = if (canAssumeUdfps) {
                        UDFPS_ENROLLING_TITLE
                    } else if (canAssumeSfps) {
                        SFPS_ENROLLING_TITLE
                    } else {
                        RFPS_ENROLLING_TITLE
                    }
                }
            })
        var i: Long = 0
        while (i < IDLE_TIMEOUT && !fingerprintPropCallbackLaunched) {
            Thread.sleep(100L)
            i += 100L
        }
        assertThat(fingerprintPropCallbackLaunched).isTrue()
        device.pressHome()

        // Stop settings before performing test
        try {
            device.executeShellCommand("am force-stop $SETTINGS_PACKAGE_NAME")
        } catch (e: IOException) {
            Log.e(TAG, "Fail to stop settings app", e)
        }
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        runAsLandscape = false
        setDeviceOrientation()

        LockScreenUtil.resetLockscreen(TEST_PIN)
        device.pressHome()
    }

    @Test
    fun testIntroChooseLock() {
        setDeviceOrientation()
        val intent = newActivityIntent(false)
        context.startActivity(intent)
        assertThat(
            device.wait(
                Until.hasObject(By.text("Choose your backup screen lock method")),
                IDLE_TIMEOUT
            )
        ).isTrue()
    }

    @Test
    fun testIntroChooseLock_runAslandscape() {
        runAsLandscape = true
        testIntroChooseLock()
    }

    private fun verifyIntroPage() {
        device.waitForIdle()
        run {
            var i: Long = 0
            while (i < IDLE_TIMEOUT) {
                if (device.wait(Until.hasObject(By.text("More")), 50L)) {
                    break
                } else if (device.wait(Until.hasObject(By.text("I agree")), 50L)) {
                    break
                }
                i += 100L
            }
        }

        // Click more btn at most twice and the introduction should stay in the last page
        var moreBtn: UiObject2? = null
        var i = 0
        val more = if (runAsLandscape) 5 else 2
        while (i < more && device.findObject(By.text("More")).also { moreBtn = it } != null) {
            moreBtn!!.click()
            device.waitForIdle()
            device.wait(Until.hasObject(By.text("More")), IDLE_TIMEOUT)
            ++i
        }
        assertThat(device.wait(Until.hasObject(By.text("No thanks")), IDLE_TIMEOUT)).isTrue()
        assertThat(device.wait(Until.hasObject(By.text("I agree")), IDLE_TIMEOUT)).isTrue()
    }

    @Test
    fun testIntroWithGkPwHandle_withUdfps_clickStart() {
        Assume.assumeTrue(canAssumeUdfps)

        setDeviceOrientation()
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true)
        launchIntroWithGkPwHandle(false)

        // Intro page
        verifyIntroPage()
        val agreeBtn = device.findObject(By.text("I agree"))
        assertThat(agreeBtn).isNotNull()
        agreeBtn.click()

        // FindUdfps page
        assertThat(device.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue()
        val lottie = device.findObject(
            By.res(SETTINGS_PACKAGE_NAME, "illustration_lottie")
        )
        assertThat(lottie).isNotNull()
        assertThat(lottie.isClickable).isTrue()
        val startBtn = device.findObject(By.text("Start"))
        assertThat(startBtn.isClickable).isTrue()
        startBtn.click()

        // Enrolling page
        assertThat(device.wait(Until.hasObject(By.text(enrollingPageTitle)), IDLE_TIMEOUT)).isTrue()
    }

    @Test
    fun testIntroWithGkPwHandle_withUdfps_clickStart_runAslandscape() {
        runAsLandscape = true
        testIntroWithGkPwHandle_withUdfps_clickStart()
    }

    @Test
    fun testIntroWithGkPwHandle_withUdfps_clickLottie() {
        Assume.assumeTrue(canAssumeUdfps)

        setDeviceOrientation()
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true)
        launchIntroWithGkPwHandle(false)

        // Intro page
        verifyIntroPage()
        val agreeBtn = device.findObject(By.text("I agree"))
        assertThat(agreeBtn).isNotNull()
        agreeBtn.click()

        // FindUdfps page
        assertThat(device.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue()
        val lottie = device.findObject(By.res(SETTINGS_PACKAGE_NAME, "illustration_lottie"))
        assertThat(lottie).isNotNull()
        assertThat(lottie.isClickable).isTrue()
        val startBtn = device.findObject(By.text("Start"))
        assertThat(startBtn.isClickable).isTrue()
        lottie.click()

        // Enrolling page
        assertThat(device.wait(Until.hasObject(By.text(enrollingPageTitle)), IDLE_TIMEOUT)).isTrue()
    }

    @Test
    fun testIntroWithGkPwHandle_withUdfps_clickLottie_runAslandscape() {
        runAsLandscape = true
        testIntroWithGkPwHandle_withUdfps_clickLottie()
    }

    @Test
    fun testIntroWithGkPwHandle_withSfps() {
        Assume.assumeTrue(canAssumeSfps)

        setDeviceOrientation()
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true)
        launchIntroWithGkPwHandle(false)

        // Intro page
        verifyIntroPage()
        val agreeBtn = device.findObject(By.text("I agree"))
        assertThat(agreeBtn).isNotNull()
        agreeBtn.click()

        // FindSfps page
        assertThat(device.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue()
        val lottie = device.findObject(
            By.res(SETTINGS_PACKAGE_NAME,"illustration_lottie")
        )
        assertThat(lottie).isNotNull()

        // We don't have view which can be clicked to run to next page, stop at here.
    }

    @Test
    fun testIntroWithGkPwHandle_withSfps_runAslandscape() {
        runAsLandscape = true
        testIntroWithGkPwHandle_withSfps()
    }

    @Test
    fun testIntroWithGkPwHandle_withRfps() {
        Assume.assumeFalse(canAssumeUdfps || canAssumeSfps)

        setDeviceOrientation()
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true)
        launchIntroWithGkPwHandle(false)

        // Intro page
        verifyIntroPage()
        val agreeBtn = device.findObject(By.text("I agree"))
        assertThat(agreeBtn).isNotNull()
        agreeBtn.click()

        // FindRfps page
        assertThat(device.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue()
        val lottie = device.findObject(
            By.res(SETTINGS_PACKAGE_NAME, "illustration_lottie")
        )
        if (lottie == null) {
            // FindSfps page shall have an animation view if no lottie view
            assertThat(
                device.findObject(
                    By.res(SETTINGS_PACKAGE_NAME, "fingerprint_sensor_location_animation")
                )
            ).isNotNull()
        }
    }

    @Test
    fun testIntroWithGkPwHandle_withRfps_runAslandscape() {
        runAsLandscape = true
        testIntroWithGkPwHandle_withRfps()
    }

    @Test
    fun testIntroWithGkPwHandle_clickNoThanksInIntroPage() {
        setDeviceOrientation()
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true)
        launchIntroWithGkPwHandle(false)

        // Intro page
        verifyIntroPage()
        val noThanksBtn = device.findObject(By.text("No thanks"))
        assertThat(noThanksBtn).isNotNull()
        noThanksBtn.click()

        // Back to home
        device.waitForWindowUpdate(SETTINGS_PACKAGE_NAME, IDLE_TIMEOUT)
        assertThat(device.findObject(By.text("No thanks"))).isNull()
    }

    @Test
    fun testIntroWithGkPwHandle_clickNoThanksInIntroPage_runAslandscape() {
        runAsLandscape = true
        testIntroWithGkPwHandle_clickNoThanksInIntroPage()
    }

    @Test
    fun testIntroWithGkPwHandle_clickSkipInFindSensor() {
        setDeviceOrientation()
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true)
        launchIntroWithGkPwHandle(false)

        // Intro page
        verifyIntroPage()
        val agreeBtn = device.findObject(By.text("I agree"))
        assertThat(agreeBtn).isNotNull()
        agreeBtn.click()

        // FindSensor page
        assertThat(device.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue()
        val doItLaterBtn = device.findObject(By.text(DO_IT_LATER))
        assertThat(doItLaterBtn).isNotNull()
        assertThat(doItLaterBtn.isClickable).isTrue()
        doItLaterBtn.click()

        // Back to home
        device.waitForWindowUpdate(SETTINGS_PACKAGE_NAME, IDLE_TIMEOUT)
        assertThat(device.findObject(By.text(DO_IT_LATER))).isNull()
    }

    @Test
    fun testIntroWithGkPwHandle_clickSkipInFindSensor_runAslandscape() {
        runAsLandscape = true
        testIntroWithGkPwHandle_clickSkipInFindSensor()
    }

    @Test
    fun testIntroWithGkPwHandle_clickSkipAnywayInFindFpsDialog_whenIsSuw() {
        setDeviceOrientation()
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true)
        launchIntroWithGkPwHandle(true)

        // Intro page
        verifyIntroPage()
        val agreeBtn = device.findObject(By.text("I agree"))
        assertThat(agreeBtn).isNotNull()
        agreeBtn.click()

        // FindSensor page
        assertThat(device.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue()
        val doItLaterBtn = device.findObject(By.text(DO_IT_LATER))
        assertThat(doItLaterBtn).isNotNull()
        assertThat(doItLaterBtn.isClickable).isTrue()
        doItLaterBtn.click()

        // SkipSetupFindFpsDialog
        assertThat(device.wait(Until.hasObject(By.text("Skip fingerprint?")), IDLE_TIMEOUT)).isTrue()
        val skipAnywayBtn = device.findObject(By.text("Skip anyway"))
        assertThat(skipAnywayBtn).isNotNull()
        assertThat(skipAnywayBtn.isClickable).isTrue()
        skipAnywayBtn.click()

        // Back to home
        device.waitForWindowUpdate(SETTINGS_PACKAGE_NAME, IDLE_TIMEOUT)
        assertThat(device.findObject(By.text("Skip anyway"))).isNull()
        assertThat(device.findObject(By.text(DO_IT_LATER))).isNull()
    }

    @Test
    fun testIntroWithGkPwHandle_clickSkipAnywayInFindFpsDialog_whenIsSuw_runAslandscape() {
        runAsLandscape = true
        testIntroWithGkPwHandle_clickSkipAnywayInFindFpsDialog_whenIsSuw()
    }

    @Test
    fun testIntroWithGkPwHandle_clickGoBackInFindFpsDialog_whenIsSuw() {
        setDeviceOrientation()
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true)
        launchIntroWithGkPwHandle(true)

        // Intro page
        verifyIntroPage()
        val agreeBtn = device.findObject(By.text("I agree"))
        assertThat(agreeBtn).isNotNull()
        agreeBtn.click()

        // FindSensor page
        assertThat(device.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue()
        val doItLaterBtn = device.findObject(By.text(DO_IT_LATER))
        assertThat(doItLaterBtn).isNotNull()
        assertThat(doItLaterBtn.isClickable).isTrue()
        doItLaterBtn.click()

        // SkipSetupFindFpsDialog
        assertThat(device.wait(Until.hasObject(By.text("Skip fingerprint?")), IDLE_TIMEOUT)).isTrue()
        val goBackBtn = device.findObject(By.text("Go back"))
        assertThat(goBackBtn).isNotNull()
        assertThat(goBackBtn.isClickable).isTrue()
        goBackBtn.click()

        // FindSensor page again
        assertThat(device.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue()
    }

    @Test
    fun testIntroWithGkPwHandle_clickGoBackInFindFpsDialog_whenIsSuw_runAslandscape() {
        runAsLandscape = true
        testIntroWithGkPwHandle_clickGoBackInFindFpsDialog_whenIsSuw()
    }

    @Test
    fun testIntroCheckPin() {
        setDeviceOrientation()
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true)
        val intent = newActivityIntent(false)
        context.startActivity(intent)
        assertThat(
            device.wait(
                Until.hasObject(By.text("Enter your device PIN to continue")),
                IDLE_TIMEOUT
            )
        ).isTrue()
    }

    @Test
    fun testEnrollingWithGkPwHandle() {
        setDeviceOrientation()
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true)
        launchEnrollingWithGkPwHandle()

        // Enrolling screen
        device.waitForIdle()
        assertThat(device.wait(Until.hasObject(By.text(enrollingPageTitle)), IDLE_TIMEOUT)).isTrue()
    }

    @Test
    fun testEnrollingWithGkPwHandle_runAslandscape() {
        runAsLandscape = true
        testEnrollingWithGkPwHandle()
    }

    @Test
    fun testEnrollingIconTouchDialog_withSfps() {
        Assume.assumeTrue(canAssumeSfps)

        setDeviceOrientation()
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true)
        launchEnrollingWithGkPwHandle()

        // Enrolling screen
        device.waitForIdle()
        assertThat(device.wait(Until.hasObject(By.text(enrollingPageTitle)), IDLE_TIMEOUT)).isTrue()
        val lottie = device.findObject(
            By.res(SETTINGS_PACKAGE_NAME, "illustration_lottie")
        )
        assertThat(lottie).isNotNull()
        lottie.click()
        lottie.click()
        lottie.click()

        // IconTouchDialog
        device.waitForIdle()
        assertThat(
            device.wait(
                Until.hasObject(By.text("Touch the sensor instead")),
                IDLE_TIMEOUT
            )
        )
            .isTrue()
        val okButton = device.findObject(By.text("OK"))
        assertThat(okButton).isNotNull()
        okButton.click()

        // Enrolling screen again
        device.waitForIdle()
        assertThat(device.wait(Until.hasObject(By.text(enrollingPageTitle)), IDLE_TIMEOUT)).isTrue()
    }

    @Test
    fun testEnrollingIconTouchDialog_withSfps_runAslandscape() {
        runAsLandscape = true
        testEnrollingIconTouchDialog_withSfps()
    }

    @Test
    fun testEnrollingIconTouchDialog_withRfps() {
        Assume.assumeFalse(canAssumeUdfps || canAssumeSfps)

        setDeviceOrientation()
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true)
        launchEnrollingWithGkPwHandle()

        // Enrolling screen
        device.waitForIdle()
        assertThat(device.wait(Until.hasObject(By.text(enrollingPageTitle)), IDLE_TIMEOUT)).isTrue()
        val lottie = device.findObject(
            By.res(SETTINGS_PACKAGE_NAME, "fingerprint_progress_bar")
        )
        assertThat(lottie).isNotNull()
        lottie.click()
        lottie.click()
        lottie.click()

        // IconTouchDialog
        device.waitForIdle()
        assertThat(
            device.wait(
                Until.hasObject(By.text("Whoops, that\u2019s not the sensor")),
                IDLE_TIMEOUT
            )
        ).isTrue()
        val okButton = device.findObject(By.text("OK"))
        assertThat(okButton).isNotNull()
        okButton.click()

        // Enrolling screen again
        device.waitForIdle()
        assertThat(device.wait(Until.hasObject(By.text(enrollingPageTitle)), IDLE_TIMEOUT)).isTrue()
    }

    @Test
    fun testEnrollingIconTouchDialog_withRfps_runAslandscape() {
        runAsLandscape = true
        testEnrollingIconTouchDialog_withRfps()
    }

    @Test
    fun testFindUdfpsWithGkPwHandle_clickStart() {
        Assume.assumeTrue(canAssumeUdfps)

        setDeviceOrientation()
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true)
        launchFindSensorWithGkPwHandle()

        // FindUdfps page
        assertThat(device.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue()
        val lottie = device.findObject(
            By.res(SETTINGS_PACKAGE_NAME, "illustration_lottie")
        )
        assertThat(lottie).isNotNull()
        assertThat(lottie.isClickable).isTrue()
        val startBtn = device.findObject(By.text("Start"))
        assertThat(startBtn.isClickable).isTrue()
        startBtn.click()

        // Enrolling page
        assertThat(device.wait(Until.hasObject(By.text(enrollingPageTitle)), IDLE_TIMEOUT)).isTrue()
    }

    @Test
    fun testFindUdfpsWithGkPwHandle_clickStart_runAslandscape() {
        runAsLandscape = true
        testFindUdfpsWithGkPwHandle_clickStart()
    }

    @Test
    fun testFindUdfpsLandscapeWithGkPwHandle_clickStartThenBack() {
        Assume.assumeTrue(canAssumeUdfps)

        setDeviceOrientation()
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true)
        launchFindSensorWithGkPwHandle()

        // FindUdfps page (portrait)
        assertThat(device.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue()

        // rotate device
        if (runAsLandscape) {
            device.setOrientationPortrait()
        } else {
            device.setOrientationLandscape()
        }
        device.waitForIdle()

        // FindUdfps page (landscape)
        assertThat(device.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue()
        val lottie = device.findObject(
            By.res(SETTINGS_PACKAGE_NAME, "illustration_lottie")
        )
        assertThat(lottie).isNotNull()
        assertThat(lottie.isClickable).isTrue()
        val startBtn = device.findObject(By.text("Start"))
        assertThat(startBtn.isClickable).isTrue()
        startBtn.click()

        // Enrolling page
        assertThat(device.wait(Until.hasObject(By.text(enrollingPageTitle)), IDLE_TIMEOUT)).isTrue()

        // Press back
        device.pressBack()
        device.waitForIdle()

        // FindUdfps page (landscape-again)
        assertThat(device.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue()
    }

    @Test
    fun testFindUdfpsLandscapeWithGkPwHandle_clickStartThenBack_runAslandscape() {
        runAsLandscape = true
        testFindUdfpsLandscapeWithGkPwHandle_clickStartThenBack()
    }

    @Test
    fun testFindUdfpsWithGkPwHandle_clickLottie() {
        Assume.assumeTrue(canAssumeUdfps)

        setDeviceOrientation()
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true)
        launchFindSensorWithGkPwHandle()

        // FindUdfps page
        assertThat(device.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue()
        val lottie = device.findObject(
            By.res(SETTINGS_PACKAGE_NAME, "illustration_lottie")
        )
        assertThat(lottie).isNotNull()
        assertThat(lottie.isClickable).isTrue()
        val startBtn = device.findObject(By.text("Start"))
        assertThat(startBtn.isClickable).isTrue()
        lottie.click()

        // Enrolling page
        assertThat(device.wait(Until.hasObject(By.text(enrollingPageTitle)), IDLE_TIMEOUT)).isTrue()
    }

    @Test
    fun testFindUdfpsWithGkPwHandle_clickLottie_runAslandscape() {
        runAsLandscape = true
        testFindUdfpsWithGkPwHandle_clickLottie()
    }

    @Test
    fun testFindSfpsWithGkPwHandle() {
        Assume.assumeTrue(canAssumeSfps)

        setDeviceOrientation()
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true)
        launchFindSensorWithGkPwHandle()

        // FindSfps page
        assertThat(device.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue()
        val lottie = device.findObject(
            By.res(SETTINGS_PACKAGE_NAME, "illustration_lottie")
        )
        assertThat(lottie).isNotNull()

        // We don't have view which can be clicked to run to next page, stop at here.
    }

    @Test
    fun testFindSfpsWithGkPwHandle_runAslandscape() {
        runAsLandscape = true
        testFindSfpsWithGkPwHandle()
    }

    @Test
    fun testFindRfpsWithGkPwHandle() {
        Assume.assumeFalse(canAssumeUdfps || canAssumeSfps)

        setDeviceOrientation()
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true)
        launchFindSensorWithGkPwHandle()

        // FindRfps page
        assertThat(device.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue()
        val lottie = device.findObject(
            By.res(
                SETTINGS_PACKAGE_NAME,
                "illustration_lottie"
            )
        )
        if (lottie == null) {
            // FindSfps page shall have an animation view if no lottie view
            assertThat(
                device.findObject(
                    By.res(
                        SETTINGS_PACKAGE_NAME,
                        "fingerprint_sensor_location_animation"
                    )
                )
            ).isNotNull()
        }
    }

    @Test
    fun testFindRfpsWithGkPwHandle_runAslandscape() {
        runAsLandscape = true
        testFindRfpsWithGkPwHandle()
    }

    @Test
    fun testFindSensorWithGkPwHandle_clickSkipInFindSensor() {
        setDeviceOrientation()
        LockScreenUtil.setLockscreen(LockScreenUtil.LockscreenType.PIN, TEST_PIN, true)
        launchFindSensorWithGkPwHandle()

        // FindSensor page
        assertThat(device.wait(Until.hasObject(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue()
        val doItLaterBtn = device.findObject(By.text(DO_IT_LATER))
        assertThat(doItLaterBtn).isNotNull()
        assertThat(doItLaterBtn.isClickable).isTrue()
        doItLaterBtn.click()

        // Back to home
        device.waitForWindowUpdate(SETTINGS_PACKAGE_NAME, IDLE_TIMEOUT)
        assertThat(device.wait(Until.gone(By.text(DO_IT_LATER)), IDLE_TIMEOUT)).isTrue()
    }

    @Test
    fun testFindSensorWithGkPwHandle_clickSkipInFindSensor_runAslandscape() {
        runAsLandscape = true
        testFindSensorWithGkPwHandle_clickSkipInFindSensor()
    }

    private fun launchIntroWithGkPwHandle(isSuw: Boolean) {
        val lockPatternUtils = LockPatternUtils(context)
        val lockscreenCredential = LockscreenCredential.createPin(TEST_PIN)
        val userId = UserHandle.myUserId()
        val onVerifyCallback =
            LockPatternChecker.OnVerifyCallback { response: VerifyCredentialResponse, _: Int ->
                val intent = newActivityIntent(isSuw)
                intent.putExtra(EXTRA_KEY_GK_PW_HANDLE, response.gatekeeperPasswordHandle)
                context.startActivity(intent)
            }
        LockPatternChecker.verifyCredential(
            lockPatternUtils, lockscreenCredential,
            userId, LockPatternUtils.VERIFY_FLAG_REQUEST_GK_PW_HANDLE, onVerifyCallback
        )
    }

    private fun launchFindSensorWithGkPwHandle() {
        val lockPatternUtils = LockPatternUtils(context)
        val lockscreenCredential = LockscreenCredential.createPin(TEST_PIN)
        val userId = UserHandle.myUserId()
        val onVerifyCallback =
            LockPatternChecker.OnVerifyCallback { response: VerifyCredentialResponse, _: Int ->
                val intent = newActivityIntent(false)
                intent.putExtra(EXTRA_SKIP_INTRO, true)
                intent.putExtra(EXTRA_KEY_GK_PW_HANDLE, response.gatekeeperPasswordHandle)
                context.startActivity(intent)
            }
        LockPatternChecker.verifyCredential(
            lockPatternUtils, lockscreenCredential,
            userId, LockPatternUtils.VERIFY_FLAG_REQUEST_GK_PW_HANDLE, onVerifyCallback
        )
    }

    private fun launchEnrollingWithGkPwHandle() {
        val lockPatternUtils = LockPatternUtils(context)
        val lockscreenCredential = LockscreenCredential.createPin(TEST_PIN)
        val userId = UserHandle.myUserId()
        val onVerifyCallback =
            LockPatternChecker.OnVerifyCallback { response: VerifyCredentialResponse, _: Int ->
                val intent = newActivityIntent(false)
                intent.putExtra(EXTRA_SKIP_FIND_SENSOR, true)
                intent.putExtra(EXTRA_KEY_GK_PW_HANDLE, response.gatekeeperPasswordHandle)
                context.startActivity(intent)
            }
        LockPatternChecker.verifyCredential(
            lockPatternUtils, lockscreenCredential,
            userId, LockPatternUtils.VERIFY_FLAG_REQUEST_GK_PW_HANDLE, onVerifyCallback
        )
    }

    private fun newActivityIntent(isSuw: Boolean): Intent {
        val intent = Intent()
        intent.setClassName(
            SETTINGS_PACKAGE_NAME,
            if (isSuw) SUW_ACTIVITY_CLASS_NAME else ACTIVITY_CLASS_NAME
        )
        if (isSuw) {
            intent.putExtra(EXTRA_IS_SETUP_FLOW, true)
        }
        intent.putExtra(EXTRA_PAGE_TRANSITION_TYPE, 1)
        intent.putExtra(Intent.EXTRA_USER_ID, context.userId)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    private fun setDeviceOrientation() {
        if (runAsLandscape) {
            device.setOrientationLandscape()
        } else {
            device.setOrientationPortrait()
        }
        device.waitForIdle()
    }

    companion object {
        private const val TAG = "FingerprintEnrollmentActivityTest"
        const val SETTINGS_PACKAGE_NAME = "com.android.settings"
        private const val ACTIVITY_CLASS_NAME =
            "com.android.settings.biometrics2.ui.view.FingerprintEnrollmentActivity"
        private const val SUW_ACTIVITY_CLASS_NAME = "$ACTIVITY_CLASS_NAME\$SetupActivity"
        private const val EXTRA_IS_SETUP_FLOW = "isSetupFlow"
        private const val EXTRA_SKIP_INTRO = "skip_intro"
        private const val EXTRA_SKIP_FIND_SENSOR = "skip_find_sensor"
        private const val EXTRA_PAGE_TRANSITION_TYPE = "page_transition_type"
        private const val EXTRA_KEY_GK_PW_HANDLE = "gk_pw_handle"
        private const val TEST_PIN = "1234"
        private const val DO_IT_LATER = "Do it later"
        private const val UDFPS_ENROLLING_TITLE = "Touch & hold the fingerprint sensor"
        private const val SFPS_ENROLLING_TITLE =
            "Lift, then touch. Move your finger slightly each time."
        private const val RFPS_ENROLLING_TITLE = "Lift, then touch again"
        private const val IDLE_TIMEOUT = 10000L
    }
}
