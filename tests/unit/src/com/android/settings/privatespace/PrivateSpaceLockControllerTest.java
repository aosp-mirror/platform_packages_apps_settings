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

import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PASSWORD;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PATTERN;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PIN;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Flags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.privatespace.onelock.PrivateSpaceLockController;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class PrivateSpaceLockControllerTest {
    @Mock
    private Context mContext;
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock SettingsPreferenceFragment mSettingsPreferenceFragment;
    @Mock
    LockPatternUtils mLockPatternUtils;

    private Preference mPreference;
    private PrivateSpaceLockController mPrivateSpaceLockController;

    /** Required setup before a test. */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        final String preferenceKey = "unlock_set_or_change_private_lock";

        mPreference = new Preference(ApplicationProvider.getApplicationContext());
        mPreference.setKey(preferenceKey);

        final FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        when(featureFactory.securityFeatureProvider.getLockPatternUtils(mContext))
                .thenReturn(mLockPatternUtils);

        mPrivateSpaceLockController = new PrivateSpaceLockController(mContext,
                mSettingsPreferenceFragment);
    }

    /** Tests that the controller is always available. */
    @Test
    public void getAvailabilityStatus_returnsAvailable() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);

        assertThat(mPrivateSpaceLockController.isAvailable()).isEqualTo(true);
    }

    /** Tests that preference is disabled and summary says same as device lock. */
    @Test
    public void getSummary_whenScreenLock() {
        doReturn(false).when(mLockPatternUtils).isSeparateProfileChallengeEnabled(anyInt());
        mSetFlagsRule.enableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);

        mPrivateSpaceLockController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getSummary().toString()).isEqualTo("Same as device screen lock");
    }

    /** Tests that preference is enabled and summary is Pattern. */
    @Test
    public void getSummary_whenProfileLockPattern() {
        doReturn(true)
                .when(mLockPatternUtils).isSeparateProfileChallengeEnabled(anyInt());
        doReturn(CREDENTIAL_TYPE_PATTERN)
                .when(mLockPatternUtils).getCredentialTypeForUser(anyInt());
        mSetFlagsRule.enableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);

        mPrivateSpaceLockController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.getSummary().toString()).isEqualTo("Pattern");
    }

    /** Tests that preference is enabled and summary is Pin. */
    @Test
    public void getSummary_whenProfileLockPin() {
        doReturn(true).when(mLockPatternUtils).isSeparateProfileChallengeEnabled(anyInt());
        doReturn(CREDENTIAL_TYPE_PIN).when(mLockPatternUtils).getCredentialTypeForUser(anyInt());
        mSetFlagsRule.enableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);

        mPrivateSpaceLockController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.getSummary().toString()).isEqualTo("PIN");
    }

    /** Tests that preference is enabled and summary is Password. */
    @Test
    public void getSummary_whenProfileLockPassword() {
        doReturn(true)
                .when(mLockPatternUtils).isSeparateProfileChallengeEnabled(anyInt());
        doReturn(CREDENTIAL_TYPE_PASSWORD)
                .when(mLockPatternUtils).getCredentialTypeForUser(anyInt());
        mSetFlagsRule.enableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);

        mPrivateSpaceLockController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.getSummary().toString()).isEqualTo("Password");
    }
}
