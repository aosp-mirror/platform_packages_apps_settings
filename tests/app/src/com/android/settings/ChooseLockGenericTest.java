/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;

import android.text.format.DateUtils;
import android.view.KeyEvent;

import com.android.settings.R;

import java.util.Collection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link ChooseLockGenericTest}
 *
 * m SettingsTests &&
 * adb install \
 * -r -g  ${ANDROID_PRODUCT_OUT}/data/app/SettingsTests/SettingsTests.apk &&
 * adb shell am instrument -e class com.android.settings.ChooseLockGenericTest \
 * -w com.android.settings.tests/android.support.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class ChooseLockGenericTest {
    private static final long TIMEOUT = 5 * DateUtils.SECOND_IN_MILLIS;
    private static final Intent PHISHING_ATTACK_INTENT = new Intent()
            .putExtra("confirm_credentials", false)
            .putExtra("password_confirmed", true);

    private UiDevice mDevice;
    private Context mTargetContext;
    private String mSettingPackage;
    private PackageManager mPackageManager;
    @Rule
    public ActivityTestRule<ChooseLockGeneric> mChooseLockGenericActivityRule =
            new ActivityTestRule<>(
                    ChooseLockGeneric.class,
                    true /* enable touch at launch */,
                    false /* don't launch at every test */);

    @Before
    public void setUp() throws Exception {
        mDevice = UiDevice.getInstance(getInstrumentation());
        mTargetContext = getInstrumentation().getTargetContext();
        mSettingPackage = mTargetContext.getPackageName();
        mPackageManager = mTargetContext.getPackageManager();

        setPassword();
    }

    @After
    public void tearDown() throws Exception {
        clearPassword();
    }

    @Test
    public void testConfirmLockPasswordShown_deviceWithPassword() throws Exception, Throwable {
        // GIVEN a PIN password is set on this device at set up.
        // WHEN ChooseLockGeneric is launched with no extras.
        mChooseLockGenericActivityRule.launchActivity(null /* No extras */);
        // THEN ConfirmLockPassword.InternalActivity is shown.
        assertThat(getCurrentActivity()).isInstanceOf(ConfirmLockPassword.InternalActivity.class);
    }

    @Test
    public void testConfirmLockPasswordShown_deviceWithPassword_phishingAttack()
            throws Exception, Throwable {
        // GIVEN a PIN password is set on this device at set up.
        // WHEN ChooseLockGeneric is launched with extras to by-pass lock password confirmation.
        mChooseLockGenericActivityRule.launchActivity(PHISHING_ATTACK_INTENT);
        // THEN ConfirmLockPassword.InternalActivity is still shown.
        assertThat(getCurrentActivity()).isInstanceOf(ConfirmLockPassword.InternalActivity.class);
    }

    private Activity getCurrentActivity() throws Throwable {
        getInstrumentation().waitForIdleSync();
        final Activity[] activity = new Activity[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                Collection<Activity> activities = ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED);
                activity[0] = activities.iterator().next();
            }
        });
        return activity[0];
    }

    private void launchNewPassword() throws Exception {
        Intent newPasswordIntent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD)
                .setPackage(mSettingPackage)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getInstrumentation().getContext().startActivity(newPasswordIntent);
        mDevice.waitForIdle();
    }

    /** Sets a PIN password, 12345, for testing. */
    private void setPassword() throws Exception {
        launchNewPassword();

        if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            // Set "lock_none", but it actually means we don't want to enroll a fingerprint.
            UiObject view = new UiObject(
                    new UiSelector().resourceId(mSettingPackage + ":id/lock_none"));
            assertTrue("lock_none", view.waitForExists(TIMEOUT));
            view.click();
            mDevice.waitForIdle();
        }

        // Pick PIN from the option list
        UiObject view = new UiObject(new UiSelector()
                .resourceId(mSettingPackage + ":id/lock_pin"));
        assertTrue("lock_pin", view.waitForExists(TIMEOUT));
        view.click();
        mDevice.waitForIdle();

        // Ignore any interstitial options
        view = new UiObject(new UiSelector()
                .resourceId(mSettingPackage + ":id/encrypt_dont_require_password"));
        if (view.waitForExists(TIMEOUT)) {
            view.click();
            mDevice.waitForIdle();
        }

        // Yes, we really want to
        view = new UiObject(new UiSelector()
                .resourceId(mSettingPackage + ":id/next_button"));
        if (view.waitForExists(TIMEOUT)) {
            view.click();
            mDevice.waitForIdle();
        }

        // Set our PIN
        view = new UiObject(new UiSelector()
                .resourceId(mSettingPackage + ":id/password_entry"));
        assertTrue("password_entry", view.waitForExists(TIMEOUT));

        // Enter it twice to confirm
        enterTestPin();
        enterTestPin();

        mDevice.pressBack();
    }

    /** Clears the previous set PIN password. */
    private void clearPassword() throws Exception {
        launchNewPassword();

        // Enter current PIN
        UiObject view = new UiObject(
                new UiSelector().resourceId(mSettingPackage + ":id/password_entry"));
        if (!view.waitForExists(TIMEOUT)) {
            // Odd, maybe there is a crash dialog showing; try dismissing it
            mDevice.pressBack();
            mDevice.waitForIdle();

            assertTrue("password_entry", view.waitForExists(TIMEOUT));
        }

        enterTestPin();

        // Set back to "none"
        view = new UiObject(new UiSelector().resourceId(mSettingPackage + ":id/lock_none"));
        assertTrue("lock_none", view.waitForExists(TIMEOUT));
        view.click();
        mDevice.waitForIdle();

        // Yes, we really want "none" if prompted again
        view = new UiObject(new UiSelector().resourceId(mSettingPackage + ":id/lock_none"));
        if (view.waitForExists(TIMEOUT)) {
            view.click();
            mDevice.waitForIdle();
        }

        // Yes, we really want to
        view = new UiObject(new UiSelector()
                .resourceId("android:id/button1"));
        if (view.waitForExists(TIMEOUT)) {
            view.click();
            mDevice.waitForIdle();
        }

        mDevice.pressBack();
    }

    private void enterTestPin() throws Exception {
        mDevice.waitForIdle();
        mDevice.pressKeyCode(KeyEvent.KEYCODE_1);
        mDevice.pressKeyCode(KeyEvent.KEYCODE_2);
        mDevice.pressKeyCode(KeyEvent.KEYCODE_3);
        mDevice.pressKeyCode(KeyEvent.KEYCODE_4);
        mDevice.pressKeyCode(KeyEvent.KEYCODE_5);
        mDevice.waitForIdle();
        mDevice.pressEnter();
        mDevice.waitForIdle();
    }
}
