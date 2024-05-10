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

package com.android.settings.security.screenlock;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.Context;

import androidx.preference.SwitchPreference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.security.trustagent.TrustAgentManager;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PowerButtonInstantLockPreferenceControllerTest {

    private static final int TEST_USER_ID = 0;

    @Mock
    private LockPatternUtils mLockPatternUtils;
    @Mock
    private TrustAgentManager mTrustAgentManager;

    private Context mContext;
    private PowerButtonInstantLockPreferenceController mController;
    private SwitchPreference mPreference;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mFeatureFactory.securityFeatureProvider.getTrustAgentManager())
                .thenReturn(mTrustAgentManager);

        mPreference = new SwitchPreference(mContext);
        mController = new PowerButtonInstantLockPreferenceController(
                mContext, TEST_USER_ID, mLockPatternUtils);
    }

    @Test
    public void isAvailable_lockSetToPattern_shouldReturnTrue() {
        when(mLockPatternUtils.isSecure(TEST_USER_ID)).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(TEST_USER_ID))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_lockSetToPin_shouldReturnTrue() {
        when(mLockPatternUtils.isSecure(TEST_USER_ID)).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(TEST_USER_ID))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_lockSetToPassword_shouldReturnTrue() {
        when(mLockPatternUtils.isSecure(TEST_USER_ID)).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(TEST_USER_ID))
                .thenReturn(DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_lockSetToNone_shouldReturnFalse() {
        when(mLockPatternUtils.isSecure(TEST_USER_ID)).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void updateState_shouldSetPref() {
        final String fakeTrustAgent = "trust_agent";
        when(mTrustAgentManager.getActiveTrustAgentLabel(mContext, mLockPatternUtils))
                .thenReturn(fakeTrustAgent);
        when(mLockPatternUtils.getPowerButtonInstantlyLocks(TEST_USER_ID)).thenReturn(true);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
        assertThat(mPreference.getSummary()).isEqualTo(mContext.getString(
                R.string.lockpattern_settings_power_button_instantly_locks_summary,
                fakeTrustAgent));

        when(mTrustAgentManager.getActiveTrustAgentLabel(mContext, mLockPatternUtils))
                .thenReturn(null);
        when(mLockPatternUtils.getPowerButtonInstantlyLocks(TEST_USER_ID)).thenReturn(false);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();
        assertThat(mPreference.getSummary()).isEqualTo(mContext.getString(
                com.android.settingslib.R.string.summary_empty));
    }

    @Test
    public void onPreferenceChange_shouldUpdateLockPatternUtils() {
        mController.onPreferenceChange(mPreference, true /* newValue */);

        verify(mLockPatternUtils).setPowerButtonInstantlyLocks(true, TEST_USER_ID);
    }
}
