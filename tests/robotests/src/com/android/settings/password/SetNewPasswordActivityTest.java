/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.Manifest.permission.REQUEST_PASSWORD_COMPLEXITY;
import static android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PARENT_PROFILE_PASSWORD;
import static android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PASSWORD;
import static android.app.admin.DevicePolicyManager.EXTRA_PASSWORD_COMPLEXITY;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_HIGH;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_NONE;

import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CALLER_APP_NAME;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_DEVICE_PASSWORD_REQUIREMENT_ONLY;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_IS_CALLING_APP_ADMIN;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_REQUESTED_MIN_COMPLEXITY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowPasswordUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import com.google.android.setupcompat.util.WizardManagerHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
public class SetNewPasswordActivityTest {

    private static final String APP_LABEL = "label";
    private static final String PKG_NAME = "packageName";

    @Mock
    private MetricsFeatureProvider mockMetricsProvider;
    private int mProvisioned;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory fakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mockMetricsProvider = fakeFeatureFactory.getMetricsFeatureProvider();
        when(mockMetricsProvider.getAttribution(any())).thenReturn(SettingsEnums.PAGE_UNKNOWN);

        mProvisioned = Settings.Global.getInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0);
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, mProvisioned);
        ShadowPasswordUtils.reset();
    }

    @Test
    @Ignore("b/295325503")
    public void testChooseLockGeneric() {
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class).get();
        activity.launchChooseLock(new Bundle());
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent intent = getLaunchChooseLockIntent(shadowActivity);

        assertThat(intent.getComponent())
                .isEqualTo(new ComponentName(activity, ChooseLockGeneric.class));
    }

    @Test
    public void testSetupChooseLockGeneric() {
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0);
        Intent intent = new Intent(ACTION_SET_NEW_PASSWORD);
        intent.putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true);
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class, intent).create().get();
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        Intent nextIntent = shadowActivity.getNextStartedActivityForResult().intent;
        assertThat(nextIntent.getComponent())
                .isEqualTo(new ComponentName(activity, SetupChooseLockGeneric.class));
    }

    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void testLaunchChooseLock_setNewPasswordExtraWithoutPermission() {
        ShadowPasswordUtils.setCallingAppLabel(APP_LABEL);
        ShadowPasswordUtils.setCallingAppPackageName(PKG_NAME);
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);

        Intent intent = new Intent(ACTION_SET_NEW_PASSWORD);
        intent.putExtra(EXTRA_PASSWORD_COMPLEXITY, PASSWORD_COMPLEXITY_HIGH);
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class, intent).create().get();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        assertThat(shadowActivity.getNextStartedActivityForResult()).isNull();
        verify(mockMetricsProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                SettingsEnums.ACTION_SET_NEW_PASSWORD,
                SettingsEnums.SET_NEW_PASSWORD_ACTIVITY,
                PKG_NAME,
                PASSWORD_COMPLEXITY_HIGH);
    }

    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void testLaunchChooseLock_setNewPasswordExtraWithPermission() {
        ShadowPasswordUtils.setCallingAppLabel(APP_LABEL);
        ShadowPasswordUtils.setCallingAppPackageName(PKG_NAME);
        ShadowPasswordUtils.addGrantedPermission(REQUEST_PASSWORD_COMPLEXITY);
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);

        Intent intent = new Intent(ACTION_SET_NEW_PASSWORD);
        intent.putExtra(EXTRA_PASSWORD_COMPLEXITY, PASSWORD_COMPLEXITY_HIGH);
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class, intent).create().get();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent actualIntent = getLaunchChooseLockIntent(shadowActivity);
        assertThat(actualIntent.getAction()).isEqualTo(ACTION_SET_NEW_PASSWORD);
        assertThat(actualIntent.hasExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY)).isTrue();
        assertThat(actualIntent.getIntExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_NONE))
                .isEqualTo(PASSWORD_COMPLEXITY_HIGH);
        assertThat(actualIntent.hasExtra(EXTRA_KEY_CALLER_APP_NAME)).isTrue();
        assertThat(actualIntent.getStringExtra(EXTRA_KEY_CALLER_APP_NAME)).isEqualTo(APP_LABEL);
        verify(mockMetricsProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                SettingsEnums.ACTION_SET_NEW_PASSWORD,
                SettingsEnums.SET_NEW_PASSWORD_ACTIVITY,
                PKG_NAME,
                PASSWORD_COMPLEXITY_HIGH);
    }

    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void testLaunchChooseLock_setNewPasswordExtraInvalidValue() {
        ShadowPasswordUtils.setCallingAppLabel(APP_LABEL);
        ShadowPasswordUtils.setCallingAppPackageName(PKG_NAME);
        ShadowPasswordUtils.addGrantedPermission(REQUEST_PASSWORD_COMPLEXITY);
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);

        Intent intent = new Intent(ACTION_SET_NEW_PASSWORD);
        intent.putExtra(EXTRA_PASSWORD_COMPLEXITY, -1);
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class, intent).create().get();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent actualIntent = getLaunchChooseLockIntent(shadowActivity);
        assertThat(actualIntent.getAction()).isEqualTo(ACTION_SET_NEW_PASSWORD);
        assertThat(actualIntent.hasExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY)).isFalse();
        assertThat(actualIntent.hasExtra(EXTRA_KEY_CALLER_APP_NAME)).isTrue();
        assertThat(actualIntent.getStringExtra(EXTRA_KEY_CALLER_APP_NAME)).isEqualTo(APP_LABEL);
        verify(mockMetricsProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                SettingsEnums.ACTION_SET_NEW_PASSWORD,
                SettingsEnums.SET_NEW_PASSWORD_ACTIVITY,
                PKG_NAME,
                -1);
    }

    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void testLaunchChooseLock_setNewPasswordExtraNoneComplexity() {
        ShadowPasswordUtils.setCallingAppLabel(APP_LABEL);
        ShadowPasswordUtils.setCallingAppPackageName(PKG_NAME);
        ShadowPasswordUtils.addGrantedPermission(REQUEST_PASSWORD_COMPLEXITY);
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);

        Intent intent = new Intent(ACTION_SET_NEW_PASSWORD);
        intent.putExtra(EXTRA_PASSWORD_COMPLEXITY, PASSWORD_COMPLEXITY_NONE);
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class, intent).create().get();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent actualIntent = getLaunchChooseLockIntent(shadowActivity);
        assertThat(actualIntent.getAction()).isEqualTo(ACTION_SET_NEW_PASSWORD);
        assertThat(actualIntent.hasExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY)).isFalse();
        assertThat(actualIntent.hasExtra(EXTRA_KEY_CALLER_APP_NAME)).isTrue();
        assertThat(actualIntent.getStringExtra(EXTRA_KEY_CALLER_APP_NAME)).isEqualTo(APP_LABEL);
        verify(mockMetricsProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                SettingsEnums.ACTION_SET_NEW_PASSWORD,
                SettingsEnums.SET_NEW_PASSWORD_ACTIVITY,
                PKG_NAME,
                PASSWORD_COMPLEXITY_NONE);
    }

    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void testLaunchChooseLock_setNewPasswordWithoutExtra() {
        ShadowPasswordUtils.setCallingAppLabel(APP_LABEL);
        ShadowPasswordUtils.setCallingAppPackageName(PKG_NAME);
        ShadowPasswordUtils.addGrantedPermission(REQUEST_PASSWORD_COMPLEXITY);
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);

        Intent intent = new Intent(ACTION_SET_NEW_PASSWORD);
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class, intent).create().get();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent actualIntent = getLaunchChooseLockIntent(shadowActivity);
        assertThat(actualIntent.getAction()).isEqualTo(ACTION_SET_NEW_PASSWORD);
        assertThat(actualIntent.hasExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY)).isFalse();
        assertThat(actualIntent.hasExtra(EXTRA_KEY_CALLER_APP_NAME)).isTrue();
        assertThat(actualIntent.getStringExtra(EXTRA_KEY_CALLER_APP_NAME)).isEqualTo(APP_LABEL);
        verify(mockMetricsProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                SettingsEnums.ACTION_SET_NEW_PASSWORD,
                SettingsEnums.SET_NEW_PASSWORD_ACTIVITY,
                PKG_NAME,
                Integer.MIN_VALUE);
    }

    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void testLaunchChooseLock_setNewParentProfilePasswordExtraWithPermission() {
        ShadowPasswordUtils.setCallingAppLabel(APP_LABEL);
        ShadowPasswordUtils.setCallingAppPackageName(PKG_NAME);
        ShadowPasswordUtils.addGrantedPermission(REQUEST_PASSWORD_COMPLEXITY);
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);

        Intent intent = new Intent(ACTION_SET_NEW_PARENT_PROFILE_PASSWORD);
        intent.putExtra(EXTRA_PASSWORD_COMPLEXITY, PASSWORD_COMPLEXITY_HIGH);
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class, intent).create().get();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent actualIntent = getLaunchChooseLockIntent(shadowActivity);
        assertThat(actualIntent.getAction()).isEqualTo(ACTION_SET_NEW_PARENT_PROFILE_PASSWORD);
        assertThat(actualIntent.hasExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY)).isFalse();
        assertThat(actualIntent.hasExtra(EXTRA_KEY_CALLER_APP_NAME)).isTrue();
        assertThat(actualIntent.getStringExtra(EXTRA_KEY_CALLER_APP_NAME)).isEqualTo(APP_LABEL);
        verify(mockMetricsProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                SettingsEnums.ACTION_SET_NEW_PARENT_PROFILE_PASSWORD,
                SettingsEnums.SET_NEW_PASSWORD_ACTIVITY,
                PKG_NAME,
                PASSWORD_COMPLEXITY_HIGH);
    }

    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void testLaunchChooseLock_setNewParentProfilePasswordWithoutExtra() {
        ShadowPasswordUtils.setCallingAppLabel(APP_LABEL);
        ShadowPasswordUtils.setCallingAppPackageName(PKG_NAME);
        ShadowPasswordUtils.addGrantedPermission(REQUEST_PASSWORD_COMPLEXITY);
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);

        Intent intent = new Intent(ACTION_SET_NEW_PARENT_PROFILE_PASSWORD);
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class, intent).create().get();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent actualIntent = getLaunchChooseLockIntent(shadowActivity);
        assertThat(actualIntent.getAction()).isEqualTo(ACTION_SET_NEW_PARENT_PROFILE_PASSWORD);
        assertThat(actualIntent.hasExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY)).isFalse();
        assertThat(actualIntent.getBooleanExtra(
                EXTRA_KEY_DEVICE_PASSWORD_REQUIREMENT_ONLY, false)).isFalse();
        assertThat(actualIntent.hasExtra(EXTRA_KEY_CALLER_APP_NAME)).isTrue();
        assertThat(actualIntent.getStringExtra(EXTRA_KEY_CALLER_APP_NAME)).isEqualTo(APP_LABEL);
        verify(mockMetricsProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                SettingsEnums.ACTION_SET_NEW_PARENT_PROFILE_PASSWORD,
                SettingsEnums.SET_NEW_PASSWORD_ACTIVITY,
                PKG_NAME,
                Integer.MIN_VALUE);
    }
    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void launchChooseLock_setNewParentProfilePassword_DevicePasswordRequirementExtra() {
        ShadowPasswordUtils.setCallingAppPackageName(PKG_NAME);
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);

        Intent intent = new Intent(ACTION_SET_NEW_PARENT_PROFILE_PASSWORD)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_PASSWORD_REQUIREMENT_ONLY, true);
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class, intent).create().get();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent actualIntent = getLaunchChooseLockIntent(shadowActivity);

        assertThat(actualIntent.getBooleanExtra(
                EXTRA_KEY_DEVICE_PASSWORD_REQUIREMENT_ONLY, false)).isTrue();
        verify(mockMetricsProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                SettingsEnums.ACTION_SET_NEW_PARENT_PROFILE_PASSWORD,
                SettingsEnums.SET_NEW_PASSWORD_ACTIVITY,
                PKG_NAME,
                Integer.MIN_VALUE | (1 << 30));
    }

    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void launchChooseLock_callingAppIsAdmin_setsAdminExtra() {
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class).get();
        DevicePolicyManager devicePolicyManager =
                (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        Shadows.shadowOf(devicePolicyManager).setActiveAdmin(buildTestComponentName(PKG_NAME));
        ShadowPasswordUtils.setCallingAppPackageName(PKG_NAME);

        activity.launchChooseLock(new Bundle());

        Intent intent = getLaunchChooseLockIntent(Shadows.shadowOf(activity));
        assertThat(intent.hasExtra(EXTRA_KEY_IS_CALLING_APP_ADMIN)).isTrue();
    }

    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void launchChooseLock_callingAppIsNotAdmin_doesNotSetAdminExtra() {
        SetNewPasswordActivity activity =
                Robolectric.buildActivity(SetNewPasswordActivity.class).get();
        DevicePolicyManager devicePolicyManager =
                (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        Shadows.shadowOf(devicePolicyManager)
                .setActiveAdmin(buildTestComponentName("other_pkg_name"));
        ShadowPasswordUtils.setCallingAppPackageName(PKG_NAME);

        activity.launchChooseLock(new Bundle());

        Intent intent = getLaunchChooseLockIntent(Shadows.shadowOf(activity));
        assertThat(intent.hasExtra(EXTRA_KEY_IS_CALLING_APP_ADMIN)).isFalse();
    }

    private ComponentName buildTestComponentName(String packageName) {
        return new ComponentName(packageName, "clazz");
    }

    private Intent getLaunchChooseLockIntent(ShadowActivity shadowActivity) {
        return shadowActivity.getNextStartedActivityForResult().intent;
    }
}
