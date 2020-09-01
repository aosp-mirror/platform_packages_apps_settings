/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

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
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
public class PowerMenuPrivacyPreferenceControllerTest {

    private static final String TEST_KEY = "test_key";
    private static final String SETTING_KEY = Settings.Secure.POWER_MENU_LOCKED_SHOW_CONTENT;
    private static final String CARDS_AVAILABLE_KEY =
            Settings.Secure.GLOBAL_ACTIONS_PANEL_AVAILABLE;
    private static final String CARDS_ENABLED_KEY = Settings.Secure.GLOBAL_ACTIONS_PANEL_ENABLED;
    private static final String CONTROLS_ENABLED_KEY = Settings.Secure.CONTROLS_ENABLED;

    private Context mContext;
    private ContentResolver mContentResolver;
    private ShadowPackageManager mShadowPackageManager;
    private PowerMenuPrivacyPreferenceController mController;

    @Mock
    private Preference mPreference;
    @Mock
    private LockPatternUtils mLockPatternUtils;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mShadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());

        mContentResolver = mContext.getContentResolver();
        FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        when(featureFactory.securityFeatureProvider.getLockPatternUtils(mContext))
                .thenReturn(mLockPatternUtils);
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);

        mController = new PowerMenuPrivacyPreferenceController(mContext, TEST_KEY);
    }

    @Test
    public void isChecked_POWER_MENU_LOCKED_SHOW_CONTENTIs1_returnTrue() {
        Settings.Secure.putInt(mContentResolver, SETTING_KEY, 1);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_POWER_MENU_LOCKED_SHOW_CONTENTIs0_returnFalse() {
        Settings.Secure.putInt(mContentResolver, SETTING_KEY, 0);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_POWER_MENU_LOCKED_SHOW_CONTENTIsNotSet_returnFalse() {
        Settings.Secure.putString(mContentResolver, SETTING_KEY, null);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_true_POWER_MENU_LOCKED_SHOW_CONTENTIsNot0() {
        mController.setChecked(true);

        assertThat(Settings.Secure.getInt(mContentResolver, SETTING_KEY, 0)).isNotEqualTo(0);
    }

    @Test
    public void setChecked_false_POWER_MENU_LOCKED_SHOW_CONTENTIs0() {
        mController.setChecked(false);

        assertThat(Settings.Secure.getInt(mContentResolver, SETTING_KEY, 0)).isEqualTo(0);
    }

    @Test
    public void getSummary_notSecureLock_isPower_menu_privacy_not_secureString() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(false);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getText(R.string.power_menu_privacy_not_secure));
    }

    @Test
    public void getSummary_cardsControlsAvailable_isPower_menu_privacy_showString() {
        Settings.Secure.putInt(mContentResolver, CARDS_AVAILABLE_KEY, 1);
        mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_CONTROLS, true);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getText(R.string.power_menu_privacy_show));
    }

    @Test
    public void
        getSummary_cardsUnavailableControlsAvailable_isPower_menu_privacy_show_controlsString() {
        Settings.Secure.putInt(mContentResolver, CARDS_AVAILABLE_KEY, 0);
        mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_CONTROLS, true);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getText(R.string.power_menu_privacy_show_controls));
    }

    @Test
    public void
        getSummary_cardsAvailableControlsUnavailable_isPower_menu_privacy_show_cardsString() {
        Settings.Secure.putInt(mContentResolver, CARDS_AVAILABLE_KEY, 1);
        mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_CONTROLS, false);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getText(R.string.power_menu_privacy_show_cards));
    }

    @Test
    public void updateState_onPreferenceRefreshed_preferenceEnabledAndSummaryChanged() {
        mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_CONTROLS, true);
        Settings.Secure.putInt(mContentResolver, CARDS_AVAILABLE_KEY, 1);

        mController.updateState(mPreference);

        verify(mPreference).setEnabled(anyBoolean());
        verify(mPreference, atLeastOnce()).setSummary(mController.getSummary());
    }

    @Test
    public void getAvailabilityStatus_allOnNotSecure_returnsDisabled() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(false);

        mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_CONTROLS, true);
        Settings.Secure.putInt(mContentResolver, CARDS_AVAILABLE_KEY, 1);
        Settings.Secure.putInt(mContentResolver, CARDS_ENABLED_KEY, 1);
        Settings.Secure.putInt(mContentResolver, CONTROLS_ENABLED_KEY, 1);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatus_controlsDeletedSecure_retursAvailable() {
        Settings.Secure.putString(mContentResolver, CONTROLS_ENABLED_KEY, null);
        mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_CONTROLS, true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }
}
