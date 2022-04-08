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

package com.android.settings.password;

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static androidx.test.InstrumentationRegistry.getTargetContext;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.AppTask;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;
import android.text.format.DateUtils;
import android.view.WindowManager;

import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.List;

/**
 * Tests for {@link ChooseLockGenericTest}
 *
 * m SettingsTests &&
 * adb install \
 * -r -g  ${ANDROID_PRODUCT_OUT}/data/app/SettingsTests/SettingsTests.apk &&
 * adb shell am instrument -e class com.android.settings.password.ChooseLockGenericTest \
 * -w com.android.settings.tests/androidx.test.runner.AndroidJUnitRunner
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
    }

    @Test
    public void testConfirmLockPasswordShown_deviceWithPassword() throws Throwable {
        setPassword();
        try {
            // GIVEN a PIN password is set on this device at set up.
            // WHEN ChooseLockGeneric is launched with no extras.
            mChooseLockGenericActivityRule.launchActivity(null /* No extras */);
            // THEN ConfirmLockPassword.InternalActivity is shown.
            final Activity activity = getCurrentActivity();
            assertThat(isSecureWindow(activity)).isTrue();
            assertThat(activity)
                    .isInstanceOf(ConfirmLockPassword.InternalActivity.class);
        } finally {
            finishAllAppTasks();
            mDevice.waitForIdle();
            clearPassword();
        }
    }

    @Test
    public void testConfirmLockPasswordShown_deviceWithPassword_phishingAttack() throws Throwable {
        setPassword();
        try {
            // GIVEN a PIN password is set on this device at set up.
            // WHEN ChooseLockGeneric is launched with extras to by-pass lock password confirmation.
            mChooseLockGenericActivityRule.launchActivity(PHISHING_ATTACK_INTENT);
            // THEN ConfirmLockPassword.InternalActivity is still shown.
            final Activity activity = getCurrentActivity();
            assertThat(isSecureWindow(activity)).isTrue();
            assertThat(activity)
                    .isInstanceOf(ConfirmLockPassword.InternalActivity.class);
        } finally {
            finishAllAppTasks();
            mDevice.waitForIdle();
            clearPassword();
        }
    }

    @Test
    public void testForFingerprint_inflateLayout() {
        mChooseLockGenericActivityRule.launchActivity(new Intent()
                .putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, true));

        assertThat(mChooseLockGenericActivityRule.getActivity().isResumed()).isTrue();
    }

    private Activity getCurrentActivity() throws Throwable {
        getInstrumentation().waitForIdleSync();
        final Activity[] activity = new Activity[1];
        getInstrumentation().runOnMainSync(() -> {
            Collection<Activity> activities = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED);
            activity[0] = activities.iterator().next();
        });
        return activity[0];
    }

    /** Sets a PIN password, 12345, for testing. */
    private void setPassword() throws Exception {
        Intent newPasswordIntent = new Intent(getTargetContext(), ChooseLockGeneric.class)
                .putExtra(LockPatternUtils.PASSWORD_TYPE_KEY,
                        DevicePolicyManager.PASSWORD_QUALITY_NUMERIC)
                .putExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD,
                        LockscreenCredential.createPin("12345"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getInstrumentation().getContext().startActivity(newPasswordIntent);
        mDevice.waitForIdle();


        // Ignore any interstitial options
        UiObject view = new UiObject(new UiSelector()
                .resourceId(mSettingPackage + ":id/encrypt_dont_require_password"));
        if (view.waitForExists(TIMEOUT)) {
            view.click();
            mDevice.waitForIdle();
        }

        // Set our PIN
        view = new UiObject(new UiSelector()
                .resourceId(mSettingPackage + ":id/password_entry"));
        assertTrue("password_entry", view.waitForExists(TIMEOUT));

        // Enter it twice to confirm
        enterTestPin(view);
        enterTestPin(view);

        // Dismiss notifications setting
        view = new UiObject(new UiSelector()
                .resourceId(mSettingPackage + ":id/redaction_done_button"));
        if (view.waitForExists(TIMEOUT)) {
            view.click();
            mDevice.waitForIdle();
        }

        mDevice.pressBack();

        assertThat(getTargetContext().getSystemService(KeyguardManager.class).isDeviceSecure())
                .isTrue();
    }

    /** Clears the previous set PIN password. */
    private void clearPassword() throws Exception {
        Intent newPasswordIntent = new Intent(getTargetContext(), ChooseLockGeneric.class)
                .putExtra(LockPatternUtils.PASSWORD_TYPE_KEY,
                        DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        getInstrumentation().getContext().startActivity(newPasswordIntent);
        mDevice.waitForIdle();

        // Enter current PIN
        UiObject view = new UiObject(
                new UiSelector().resourceId(mSettingPackage + ":id/password_entry"));
        if (!view.waitForExists(TIMEOUT)) {
            // Odd, maybe there is a crash dialog showing; try dismissing it
            mDevice.pressBack();
            mDevice.waitForIdle();

            assertTrue("password_entry", view.waitForExists(TIMEOUT));
        }

        enterTestPin(view);

        mDevice.pressBack();

        assertThat(getTargetContext().getSystemService(KeyguardManager.class).isDeviceSecure())
                .isFalse();
    }

    private void finishAllAppTasks() {
        final ActivityManager activityManager =
                getTargetContext().getSystemService(ActivityManager.class);
        final List<AppTask> appTasks = activityManager.getAppTasks();
        for (ActivityManager.AppTask task : appTasks) {
            task.finishAndRemoveTask();
        }
    }

    private void enterTestPin(UiObject view) throws Exception {
        mDevice.waitForIdle();
        view.setText("12345");
        mDevice.pressEnter();
        mDevice.waitForIdle();
    }

    private boolean isSecureWindow(Activity activity) {
        return (activity.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_SECURE)
                != 0;
    }
}
