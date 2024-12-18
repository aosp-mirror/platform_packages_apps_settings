/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.utils.StringUtil;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowRestrictedLockUtilsInternal.class})
public class FingerprintStatusPreferenceControllerTest {

    @Mock
    private LockPatternUtils mLockPatternUtils;
    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private UserManager mUm;
    @Mock
    private PackageManager mPackageManager;

    private FakeFeatureFactory mFeatureFactory;
    private Context mContext;
    private FingerprintStatusPreferenceController mController;
    private Preference mPreference;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)).thenReturn(true);
        ShadowApplication.getInstance().setSystemService(Context.FINGERPRINT_SERVICE,
                mFingerprintManager);
        ShadowApplication.getInstance().setSystemService(Context.USER_SERVICE, mUm);
        mPreference = new Preference(mContext);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mFeatureFactory.securityFeatureProvider.getLockPatternUtils(mContext))
                .thenReturn(mLockPatternUtils);
        when(mUm.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[] {1234});
        mController = new FingerprintStatusPreferenceController(mContext, mLifecycle);
    }

    @Test
    public void getAvailabilityStatus_noFingerprintManger_DISABLED() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_hasFingerprintManger_AVAILABLE() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void updateState_notSupported_shouldDoNothing() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateState_noFingerprint_shouldShowDefaultSummary() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.security_settings_fingerprint_preference_summary_none));
        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void updateState_hasFingerprint_shouldShowSummary() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.getEnrolledFingerprints(anyInt()))
                .thenReturn(Collections.singletonList(mock(Fingerprint.class)));
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(true);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo(StringUtil.getIcuPluralsString(mContext, 1,
                R.string.security_settings_fingerprint_preference_summary));
        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    @Ignore
    public void updateState_parentalConsentRequired_preferenceDisabled() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);

        RestrictedPreference restrictedPreference = mock(RestrictedPreference.class);
        RestrictedLockUtils.EnforcedAdmin admin = mock(RestrictedLockUtils.EnforcedAdmin.class);

        mController.mPreference = restrictedPreference;
        mController.updateStateInternal(admin);
        verify(restrictedPreference).setDisabledByAdmin(eq(admin));

        reset(admin);

        mController.updateStateInternal(null /* enforcedAdmin */);
        verify(restrictedPreference, never()).setDisabledByAdmin(any());
    }
}
