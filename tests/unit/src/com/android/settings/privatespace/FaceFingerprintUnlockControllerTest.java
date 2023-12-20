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

package com.android.settings.privatespace;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.privatespace.onelock.FaceFingerprintUnlockController;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class FaceFingerprintUnlockControllerTest {
    @Mock private Context mContext;
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock Lifecycle mLifecycle;
    @Mock LockPatternUtils mLockPatternUtils;

    private Preference mPreference;
    private FaceFingerprintUnlockController mFaceFingerprintUnlockController;

    /** Required setup before a test. */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        final String preferenceKey = "private_space_biometrics";

        mPreference = new Preference(ApplicationProvider.getApplicationContext());
        mPreference.setKey(preferenceKey);

        final FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        when(featureFactory.securityFeatureProvider.getLockPatternUtils(mContext))
                .thenReturn(mLockPatternUtils);

        mFaceFingerprintUnlockController =
                new FaceFingerprintUnlockController(mContext, mLifecycle);
    }

    /** Tests that the controller is always available. */
    @Test
    public void getAvailabilityStatus_whenFlagsEnabled_returnsAvailable() {
        mSetFlagsRule.enableFlags(
                android.os.Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_ENABLE_BIOMETRICS_TO_UNLOCK_PRIVATE_SPACE);

        assertThat(mFaceFingerprintUnlockController.isAvailable()).isEqualTo(true);
    }

    /** Tests that the controller is not available when Biometrics flag is not enabled. */
    @Test
    public void getAvailabilityStatus_whenBiometricFlagDisabled_returnsFalse() {
        mSetFlagsRule.enableFlags(android.os.Flags.FLAG_ALLOW_PRIVATE_PROFILE);
        mSetFlagsRule.disableFlags(
                android.multiuser.Flags.FLAG_ENABLE_BIOMETRICS_TO_UNLOCK_PRIVATE_SPACE);

        assertThat(mFaceFingerprintUnlockController.isAvailable()).isEqualTo(false);
    }

    /** Tests that the controller is not available when private feature flag is not enabled. */
    @Test
    public void getAvailabilityStatus_whenPrivateFlagDisabled_returnsFalse() {
        mSetFlagsRule.disableFlags(android.os.Flags.FLAG_ALLOW_PRIVATE_PROFILE);
        mSetFlagsRule.enableFlags(
                android.multiuser.Flags.FLAG_ENABLE_BIOMETRICS_TO_UNLOCK_PRIVATE_SPACE);

        assertThat(mFaceFingerprintUnlockController.isAvailable()).isEqualTo(false);
    }

    /** Tests that preference is disabled and summary says same as device lock. */
    @Test
    public void getSummary_whenScreenLock() {
        doReturn(false).when(mLockPatternUtils).isSeparateProfileChallengeEnabled(anyInt());
        mSetFlagsRule.enableFlags(
                android.os.Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_ENABLE_BIOMETRICS_TO_UNLOCK_PRIVATE_SPACE);

        mFaceFingerprintUnlockController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getSummary().toString()).isEqualTo("Same as device screen lock");
    }

    /** Tests that preference is enabled and summary is not same as device lock. */
    @Test
    public void getSummary_whenSeparateProfileLock() {
        doReturn(true).when(mLockPatternUtils).isSeparateProfileChallengeEnabled(anyInt());
        mSetFlagsRule.enableFlags(
                android.os.Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_ENABLE_BIOMETRICS_TO_UNLOCK_PRIVATE_SPACE);

        mFaceFingerprintUnlockController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.getSummary().toString()).isNotEqualTo("Same as device screen lock");
    }
}
