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

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.service.quickaccesswallet.QuickAccessWalletClient;

import androidx.preference.Preference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WalletPrivacyPreferenceControllerTest {

    private static final String TEST_KEY = "test_key";
    private static final String SETTING_KEY = Settings.Secure.LOCKSCREEN_SHOW_WALLET;

    private Context mContext;
    private ContentResolver mContentResolver;
    private WalletPrivacyPreferenceController mController;

    @Mock
    private Preference mPreference;
    @Mock
    private LockPatternUtils mLockPatternUtils;
    @Mock
    private QuickAccessWalletClient mClient;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mContentResolver = mContext.getContentResolver();
        FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        when(featureFactory.securityFeatureProvider.getLockPatternUtils(mContext))
                .thenReturn(mLockPatternUtils);
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);

        when(mClient.isWalletServiceAvailable()).thenReturn(true);
        mController = new WalletPrivacyPreferenceController(mContext, TEST_KEY) {
            @Override
            QuickAccessWalletClient initWalletClient() {
                return mClient;
            }
        };
    }

    @Test
    public void isChecked_SettingIs1_returnTrue() {
        Settings.Secure.putInt(mContentResolver, SETTING_KEY, 1);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_SettingIs0_returnFalse() {
        Settings.Secure.putInt(mContentResolver, SETTING_KEY, 0);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_SettingIsNotSet_returnFalse() {
        Settings.Secure.putString(mContentResolver, SETTING_KEY, null);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_true_SettingIsNot0() {
        mController.setChecked(true);

        assertThat(Settings.Secure.getInt(mContentResolver, SETTING_KEY, 0)).isNotEqualTo(0);
    }

    @Test
    public void setChecked_false_SettingIs0() {
        mController.setChecked(false);

        assertThat(Settings.Secure.getInt(mContentResolver, SETTING_KEY, 0)).isEqualTo(0);
    }

    @Test
    public void getSummary_notSecureLock_lockscreen_privacy_not_secureString() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(false);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getText(R.string.lockscreen_privacy_not_secure));
    }

    @Test
    public void getSummary_isSecure_lockscreen_privacy_showString() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getText(R.string.lockscreen_privacy_wallet_summary));
    }

    @Test
    public void updateState_onPreferenceRefreshed_preferenceEnabledAndSummaryChanged() {
        mController.updateState(mPreference);

        verify(mPreference).setEnabled(anyBoolean());
        verify(mPreference, atLeastOnce()).setSummary(mController.getSummary());
    }

    @Test
    public void getAvailabilityStatus_noServiceAndIsSecure_returnsUnsupported() {
        when(mClient.isWalletServiceAvailable()).thenReturn(false);
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_hasServiceButNotSecure_returnsDisabled() {
        when(mClient.isWalletServiceAvailable()).thenReturn(true);
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatus_hasServiceAndIsSecure_returnsAvailable() {
        when(mClient.isWalletServiceAvailable()).thenReturn(true);
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }
}
