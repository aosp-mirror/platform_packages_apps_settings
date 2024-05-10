/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.password;

import static android.Manifest.permission.REQUEST_PASSWORD_COMPLEXITY;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_HIGH;

import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_REQUESTED_MIN_COMPLEXITY;

import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

import android.content.Intent;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.password.SetupChooseLockGeneric.SetupChooseLockGenericFragment;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowPasswordUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;

import com.google.android.setupdesign.GlifPreferenceLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowUserManager.class,
        ShadowUtils.class,
        ShadowLockPatternUtils.class,
})
public class SetupChooseLockGenericTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private FakeFeatureFactory mFakeFeatureFactory;
    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private FaceManager mFaceManager;

    @Before
    public void setUp() {
        ShadowUtils.setFingerprintManager(mFingerprintManager);
        ShadowUtils.setFaceManager(mFaceManager);
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();

        Settings.Global.putInt(application.getContentResolver(), Settings.Global.DEVICE_PROVISIONED,
                0);

        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
    }

    @After
    public void tearDown() {
        ShadowPasswordUtils.reset();
    }

    @Test
    public void setupChooseLockGenericPasswordComplexityExtraWithoutPermission() {
        Intent intent = new Intent("com.android.settings.SETUP_LOCK_SCREEN");
        intent.putExtra(EXTRA_IS_SETUP_FLOW, true);
        intent.putExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_HIGH);
        SetupChooseLockGeneric activity =
                Robolectric.buildActivity(SetupChooseLockGeneric.class, intent).create().get();

        assertThat(activity.isFinishing()).isTrue();
    }

    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void setupChooseLockGenericPasswordComplexityExtraWithPermission() {
        ShadowPasswordUtils.addGrantedPermission(REQUEST_PASSWORD_COMPLEXITY);

        Intent intent = new Intent("com.android.settings.SETUP_LOCK_SCREEN");
        intent.putExtra(EXTRA_IS_SETUP_FLOW, true);
        intent.putExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_HIGH);
        SetupChooseLockGeneric activity =
                Robolectric.buildActivity(SetupChooseLockGeneric.class, intent).create().get();

        assertThat(activity.isFinishing()).isFalse();
    }

    @Test
    public void setupChooseLockGenericUsingDescriptionTextOfGlifLayout() {
        SetupChooseLockGenericFragment fragment = getFragmentOfSetupChooseLockGeneric(true, true,
                false);
        GlifPreferenceLayout view = getViewOfSetupChooseLockGenericFragment(fragment);
        assertThat(TextUtils.isEmpty(view.getDescriptionText())).isFalse();
        assertThat(view.getDescriptionText().toString()).isEqualTo(fragment.loadDescriptionText());
    }

    @Test
    public void setupChooseLockGenericUsingDescriptionTextOfGlifLayoutForBiometric() {
        SetupChooseLockGenericFragment fragment = getFragmentOfSetupChooseLockGeneric(true, true,
                true);
        GlifPreferenceLayout view = getViewOfSetupChooseLockGenericFragment(fragment);
        assertThat(TextUtils.isEmpty(view.getDescriptionText())).isFalse();
        assertThat(view.getDescriptionText().toString()).isEqualTo(fragment.loadDescriptionText());
    }

    @Test
    public void updatePreferenceTextShowScreenLockAndShowFaceAndShowFingerprint() {
        when(mFakeFeatureFactory.mFaceFeatureProvider.isSetupWizardSupported(any())).thenReturn(
                true);
        SetupChooseLockGenericFragment fragment = getFragmentOfSetupChooseLockGeneric(true, true,
                true);

        final String supportFingerprint = capitalize(fragment.getResources().getString(
                R.string.security_settings_fingerprint));
        final String supportFace = capitalize(fragment.getResources().getString(
                R.string.keywords_face_settings));

        assertThat(fragment.getBiometricsPreferenceTitle(ScreenLockType.PIN)).contains(
                supportFingerprint);
        assertThat(fragment.getBiometricsPreferenceTitle(ScreenLockType.PIN)).contains(
                supportFace);
        assertThat(fragment.getBiometricsPreferenceTitle(ScreenLockType.PATTERN)).contains(
                supportFingerprint);
        assertThat(fragment.getBiometricsPreferenceTitle(ScreenLockType.PATTERN)).contains(
                supportFace);
        assertThat(fragment.getBiometricsPreferenceTitle(ScreenLockType.PASSWORD)).contains(
                supportFingerprint);
        assertThat(fragment.getBiometricsPreferenceTitle(ScreenLockType.PASSWORD)).contains(
                supportFace);
    }

    @Test
    public void updatePreferenceTextShowScreenLockAndShowFingerprint() {
        when(mFakeFeatureFactory.mFaceFeatureProvider.isSetupWizardSupported(any())).thenReturn(
                false);
        SetupChooseLockGenericFragment fragment = getFragmentOfSetupChooseLockGeneric(false, false,
                true);

        final String supportFingerprint = capitalize(fragment.getResources().getString(
                R.string.security_settings_fingerprint));
        final String supportFace = capitalize(fragment.getResources().getString(
                R.string.keywords_face_settings));

        assertThat(fragment.getBiometricsPreferenceTitle(ScreenLockType.PIN)).contains(
                supportFingerprint);
        assertThat(fragment.getBiometricsPreferenceTitle(ScreenLockType.PIN)).doesNotContain(
                supportFace);
        assertThat(fragment.getBiometricsPreferenceTitle(ScreenLockType.PATTERN)).contains(
                supportFingerprint);
        assertThat(fragment.getBiometricsPreferenceTitle(ScreenLockType.PATTERN)).doesNotContain(
                supportFace);
        assertThat(fragment.getBiometricsPreferenceTitle(ScreenLockType.PASSWORD)).contains(
                supportFingerprint);
        assertThat(fragment.getBiometricsPreferenceTitle(ScreenLockType.PASSWORD)).doesNotContain(
                supportFace);
    }

    private SetupChooseLockGenericFragment getFragmentOfSetupChooseLockGeneric(
            boolean forFingerprint, boolean forFace, boolean forBiometric) {
        ShadowPasswordUtils.addGrantedPermission(REQUEST_PASSWORD_COMPLEXITY);
        Intent intent = new Intent("com.android.settings.SETUP_LOCK_SCREEN");
        intent.putExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_HIGH);
        intent.putExtra(EXTRA_IS_SETUP_FLOW, true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, forFingerprint);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE, forFace);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_BIOMETRICS, forBiometric);

        SetupChooseLockGeneric activity = ActivityController.of(
                new SetupChooseLockGeneric(), intent).setup().get();

        List<Fragment> fragments = activity.getSupportFragmentManager().getFragments();
        assertThat(fragments).isNotNull();
        assertThat(fragments.size()).isEqualTo(1);
        assertThat(fragments.get(0)).isInstanceOf(SetupChooseLockGenericFragment.class);

        return (SetupChooseLockGenericFragment) fragments.get(0);
    }
    private GlifPreferenceLayout getViewOfSetupChooseLockGenericFragment(
            @NonNull SetupChooseLockGenericFragment fragment) {
        assertThat(fragment.getView()).isNotNull();
        assertThat(fragment.getView()).isInstanceOf(GlifPreferenceLayout.class);

        return (GlifPreferenceLayout) fragment.getView();
    }

    private static String capitalize(final String input) {
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }
}
