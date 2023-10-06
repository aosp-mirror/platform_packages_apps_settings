/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.security;

import static android.content.Context.FACE_SERVICE;
import static android.content.Context.FINGERPRINT_SERVICE;
import static android.content.pm.PackageManager.FEATURE_FACE;
import static android.content.pm.PackageManager.FEATURE_FINGERPRINT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Looper;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.TestUtils;
import com.android.settings.biometrics.combination.CombinedBiometricStatusPreferenceController;
import com.android.settings.biometrics.face.FaceStatusPreferenceController;
import com.android.settings.biometrics.fingerprint.FingerprintStatusPreferenceController;
import com.android.settings.safetycenter.SafetyCenterManagerWrapper;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.security.trustagent.TrustAgentManager;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SecuritySettingsTest {
    private Context mContext;
    private SecuritySettingsFeatureProvider mSecuritySettingsFeatureProvider;
    private SecuritySettings mSecuritySettings;
    private Preference mPreference;
    private RestrictedPreference mPreferenceFace;
    private RestrictedPreference mPreferenceFingerprint;
    private RestrictedPreference mPreferenceCombined;

    @Mock
    private TrustAgentManager mTrustAgentManager;
    @Mock
    private FaceManager mFaceManager;
    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;

    private PreferenceScreen mScreen;

    @Before
    @UiThreadTest
    public void setup() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        MockitoAnnotations.initMocks(this);
        SafetyCenterManagerWrapper.sInstance = mSafetyCenterManagerWrapper;
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(FEATURE_FACE)).thenReturn(true);
        when(mPackageManager.hasSystemFeature(FEATURE_FINGERPRINT)).thenReturn(true);
        doReturn(mFaceManager).when(mContext).getSystemService(FACE_SERVICE);
        doReturn(mFingerprintManager).when(mContext).getSystemService(FINGERPRINT_SERVICE);
        FakeFeatureFactory mFeatureFactory = FakeFeatureFactory.setupForTest();
        SecurityFeatureProvider mSecurityFeatureProvider =
                mFeatureFactory.getSecurityFeatureProvider();
        when(mSecurityFeatureProvider.getTrustAgentManager()).thenReturn(mTrustAgentManager);
        mSecuritySettingsFeatureProvider = mFeatureFactory.getSecuritySettingsFeatureProvider();
        mSecuritySettings = new SecuritySettings();

        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = new Preference(mContext);
        mPreference.setKey(SecuritySettings.SECURITY_CATEGORY);

        mPreferenceFace = new RestrictedPreference(mContext);
        mPreferenceFingerprint = new RestrictedPreference(mContext);
        mPreferenceCombined = new RestrictedPreference(mContext);

        mPreferenceFace.setKey(FaceStatusPreferenceController
                .KEY_FACE_SETTINGS);
        mPreferenceFingerprint.setKey(FingerprintStatusPreferenceController
                .KEY_FINGERPRINT_SETTINGS);
        mPreferenceCombined.setKey(CombinedBiometricStatusPreferenceController
                .KEY_BIOMETRIC_SETTINGS);

        mScreen.addPreference(mPreference);
        mScreen.addPreference(mPreferenceFace);
        mScreen.addPreference(mPreferenceFingerprint);
        mScreen.addPreference(mPreferenceCombined);
    }

    @Test
    public void noAlternativeFragmentAvailableAndSafetyCenterIsDisabled_pageIndexIncluded()
            throws Exception {
        when(mSecuritySettingsFeatureProvider.hasAlternativeSecuritySettingsFragment()).thenReturn(
                false);
        when(mSafetyCenterManagerWrapper.isEnabled(any())).thenReturn(false);
        BaseSearchIndexProvider indexProvider = SecuritySettings.SEARCH_INDEX_DATA_PROVIDER;

        List<String> allXmlKeys = TestUtils.getAllXmlKeys(mContext, indexProvider);
        List<String> nonIndexableKeys = indexProvider.getNonIndexableKeys(mContext);
        allXmlKeys.removeAll(nonIndexableKeys);

        assertThat(allXmlKeys).isNotEmpty();
    }

    @Test
    public void alternativeFragmentAvailable_pageIndexExcluded() throws Exception {
        when(mSecuritySettingsFeatureProvider.hasAlternativeSecuritySettingsFragment()).thenReturn(
                true);
        BaseSearchIndexProvider indexProvider = SecuritySettings.SEARCH_INDEX_DATA_PROVIDER;

        List<String> allXmlKeys = TestUtils.getAllXmlKeys(mContext, indexProvider);
        List<String> nonIndexableKeys = indexProvider.getNonIndexableKeys(mContext);
        allXmlKeys.removeAll(nonIndexableKeys);

        assertThat(allXmlKeys).isEmpty();
    }

    @Test
    @UiThreadTest
    public void preferenceController_containsFaceWhenAvailable() {
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        final List<AbstractPreferenceController> controllers =
                mSecuritySettings.createPreferenceControllers(mContext);

        assertThat(isFacePrefAvailable(controllers)).isTrue();
        assertThat(isFingerprintPrefAvailable(controllers)).isFalse();
        assertThat(isCombinedPrefAvailable(controllers)).isFalse();
    }

    @Test
    @UiThreadTest
    public void preferenceController_containsFingerprintWhenAvailable() {
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        final List<AbstractPreferenceController> controllers =
                mSecuritySettings.createPreferenceControllers(mContext);

        assertThat(isFacePrefAvailable(controllers)).isFalse();
        assertThat(isFingerprintPrefAvailable(controllers)).isTrue();
        assertThat(isCombinedPrefAvailable(controllers)).isFalse();
    }

    @Test
    @UiThreadTest
    public void preferenceController_containsCombinedBiometricWhenAvailable() {
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        final List<AbstractPreferenceController> controllers =
                mSecuritySettings.createPreferenceControllers(mContext);

        assertThat(isFacePrefAvailable(controllers)).isFalse();
        assertThat(isFingerprintPrefAvailable(controllers)).isFalse();
        assertThat(isCombinedPrefAvailable(controllers)).isTrue();
    }

    @Test
    @UiThreadTest
    public void preferenceLifecycle_faceShowsThenCombined() {
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        final List<AbstractPreferenceController> controllers =
                mSecuritySettings.createPreferenceControllers(mContext);

        FaceStatusPreferenceController mFaceStatusPreferenceController =
                getFaceStatusPreferenceController(controllers);

        FingerprintStatusPreferenceController mFingerprintStatusPreferenceController =
                getFingerprintStatusPreferenceController(controllers);

        CombinedBiometricStatusPreferenceController mCombinedStatusPreferenceController =
                getCombinedBiometricStatusPreferenceController(controllers);

        mFaceStatusPreferenceController.setPreferenceScreen(mScreen);
        mFingerprintStatusPreferenceController.setPreferenceScreen(mScreen);
        mCombinedStatusPreferenceController.setPreferenceScreen(mScreen);

        mSecuritySettings.getSettingsLifecycle().handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

        assertThat(mPreferenceFace.isVisible()).isTrue();
        assertThat(mPreferenceFingerprint.isVisible()).isFalse();
        assertThat(mPreferenceCombined.isVisible()).isFalse();

        mSecuritySettings.getSettingsLifecycle().handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        mSecuritySettings.getSettingsLifecycle().handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

        assertThat(mPreferenceFace.isVisible()).isFalse();
        assertThat(mPreferenceFingerprint.isVisible()).isFalse();
        assertThat(mPreferenceCombined.isVisible()).isTrue();
    }

    @Test
    @UiThreadTest
    public void preferenceLifecycle_fingerprintShowsThenCombined() {
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        final List<AbstractPreferenceController> controllers =
                mSecuritySettings.createPreferenceControllers(mContext);

        FaceStatusPreferenceController mFaceStatusPreferenceController =
                getFaceStatusPreferenceController(controllers);

        FingerprintStatusPreferenceController mFingerprintStatusPreferenceController =
                getFingerprintStatusPreferenceController(controllers);

        CombinedBiometricStatusPreferenceController mCombinedStatusPreferenceController =
                getCombinedBiometricStatusPreferenceController(controllers);

        mFaceStatusPreferenceController.setPreferenceScreen(mScreen);
        mFingerprintStatusPreferenceController.setPreferenceScreen(mScreen);
        mCombinedStatusPreferenceController.setPreferenceScreen(mScreen);

        mSecuritySettings.getSettingsLifecycle().handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

        assertThat(mPreferenceFace.isVisible()).isFalse();
        assertThat(mPreferenceFingerprint.isVisible()).isTrue();
        assertThat(mPreferenceCombined.isVisible()).isFalse();

        mSecuritySettings.getSettingsLifecycle().handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        mSecuritySettings.getSettingsLifecycle().handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

        assertThat(mPreferenceFace.isVisible()).isFalse();
        assertThat(mPreferenceFingerprint.isVisible()).isFalse();
        assertThat(mPreferenceCombined.isVisible()).isTrue();
    }

    @Test
    @UiThreadTest
    public void preferenceLifecycle_combinedShowsThenFace() {
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        final List<AbstractPreferenceController> controllers =
                mSecuritySettings.createPreferenceControllers(mContext);

        FaceStatusPreferenceController mFaceStatusPreferenceController =
                getFaceStatusPreferenceController(controllers);

        FingerprintStatusPreferenceController mFingerprintStatusPreferenceController =
                getFingerprintStatusPreferenceController(controllers);

        CombinedBiometricStatusPreferenceController mCombinedStatusPreferenceController =
                getCombinedBiometricStatusPreferenceController(controllers);

        mFaceStatusPreferenceController.setPreferenceScreen(mScreen);
        mFingerprintStatusPreferenceController.setPreferenceScreen(mScreen);
        mCombinedStatusPreferenceController.setPreferenceScreen(mScreen);

        mSecuritySettings.getSettingsLifecycle().handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

        assertThat(mPreferenceFace.isVisible()).isFalse();
        assertThat(mPreferenceFingerprint.isVisible()).isFalse();
        assertThat(mPreferenceCombined.isVisible()).isTrue();

        mSecuritySettings.getSettingsLifecycle().handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        mSecuritySettings.getSettingsLifecycle().handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

        assertThat(mPreferenceFace.isVisible()).isTrue();
        assertThat(mPreferenceFingerprint.isVisible()).isFalse();
        assertThat(mPreferenceCombined.isVisible()).isFalse();
    }

    @Test
    @UiThreadTest
    public void preferenceLifecycle_combinedShowsThenFingerprint() {
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        final List<AbstractPreferenceController> controllers =
                mSecuritySettings.createPreferenceControllers(mContext);

        FaceStatusPreferenceController mFaceStatusPreferenceController =
                getFaceStatusPreferenceController(controllers);

        FingerprintStatusPreferenceController mFingerprintStatusPreferenceController =
                getFingerprintStatusPreferenceController(controllers);

        CombinedBiometricStatusPreferenceController mCombinedStatusPreferenceController =
                getCombinedBiometricStatusPreferenceController(controllers);

        mFaceStatusPreferenceController.setPreferenceScreen(mScreen);
        mFingerprintStatusPreferenceController.setPreferenceScreen(mScreen);
        mCombinedStatusPreferenceController.setPreferenceScreen(mScreen);

        mSecuritySettings.getSettingsLifecycle().handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

        assertThat(mPreferenceFace.isVisible()).isFalse();
        assertThat(mPreferenceFingerprint.isVisible()).isFalse();
        assertThat(mPreferenceCombined.isVisible()).isTrue();

        mSecuritySettings.getSettingsLifecycle().handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        mSecuritySettings.getSettingsLifecycle().handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

        assertThat(mPreferenceFace.isVisible()).isFalse();
        assertThat(mPreferenceFingerprint.isVisible()).isTrue();
        assertThat(mPreferenceCombined.isVisible()).isFalse();
    }

    boolean isFacePrefAvailable(List<AbstractPreferenceController> controllers) {
        return controllers.stream().filter(
                controller -> controller instanceof FaceStatusPreferenceController
                        && controller.isAvailable()).count() == 1;
    }

    boolean isFingerprintPrefAvailable(List<AbstractPreferenceController> controllers) {
        return controllers.stream().filter(
                controller -> controller instanceof FingerprintStatusPreferenceController
                        && controller.isAvailable()).count() == 1;
    }

    boolean isCombinedPrefAvailable(List<AbstractPreferenceController> controllers) {
        return controllers.stream().filter(
                controller -> controller instanceof CombinedBiometricStatusPreferenceController
                        && controller.isAvailable()).count() == 1;
    }

    FaceStatusPreferenceController getFaceStatusPreferenceController(
            List<AbstractPreferenceController> controllers) {
        for (AbstractPreferenceController abstractPreferenceController: controllers) {
            if (abstractPreferenceController instanceof FaceStatusPreferenceController) {
                return (FaceStatusPreferenceController) abstractPreferenceController;
            }
        }
        return null;
    }

    FingerprintStatusPreferenceController getFingerprintStatusPreferenceController(
            List<AbstractPreferenceController> controllers) {
        for (AbstractPreferenceController abstractPreferenceController: controllers) {
            if (abstractPreferenceController instanceof FingerprintStatusPreferenceController) {
                return (FingerprintStatusPreferenceController) abstractPreferenceController;
            }
        }
        return null;
    }

    CombinedBiometricStatusPreferenceController getCombinedBiometricStatusPreferenceController(
            List<AbstractPreferenceController> controllers) {
        for (AbstractPreferenceController abstractPreferenceController: controllers) {
            if (abstractPreferenceController
                    instanceof CombinedBiometricStatusPreferenceController) {
                return (CombinedBiometricStatusPreferenceController) abstractPreferenceController;
            }
        }
        return null;
    }
}
