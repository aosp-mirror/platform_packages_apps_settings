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
 * limitations under the License
 */

package com.android.settings.backup;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowPrivacySettingsUtils.class})
public class ConfigureAccountPreferenceControllerTest {
    private Context mContext;
    private ConfigureAccountPreferenceController mController;
    private PrivacySettingsConfigData mPSCD;
    private Preference mPreference;
    private String mTestSummary;

    @Mock
    private Intent mIntent;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPSCD = PrivacySettingsConfigData.getInstance();
        mController = new ConfigureAccountPreferenceController(mContext,
                PrivacySettingsUtils.CONFIGURE_ACCOUNT);
        mPreference = new Preference(mContext);
        mTestSummary = "Summary";
    }

    @After
    public void tearDown() {
        ShadowPrivacySettingsUtils.reset();
    }

    @Test
    public void updateState_backupEnabled_hadConfigIntent_prefShouldBeEnabled() {
        mPSCD.setBackupEnabled(true);
        mPSCD.setBackupGray(false);
        mPSCD.setConfigIntent(mIntent);

        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void
    updateState_backupEnabled_hadConfigIntent_nullConfigSummary_prefDisplayDefaultSummary() {
        mPSCD.setBackupEnabled(true);
        mPSCD.setBackupGray(false);
        mPSCD.setConfigIntent(mIntent);
        mPSCD.setConfigSummary(null);

        mController.updateState(mPreference);
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.backup_configure_account_default_summary));
    }

    @Test
    public void
    updateState_backupEnabled_hadConfigIntent_hasConfigSummary_prefDisplayConfigSummary() {
        mPSCD.setBackupEnabled(true);
        mPSCD.setBackupGray(false);
        mPSCD.setConfigIntent(mIntent);
        mPSCD.setConfigSummary(mTestSummary);

        mController.updateState(mPreference);
        assertThat(mPreference.getSummary()).isEqualTo(mTestSummary);
    }

    @Test
    public void getAvailabilityStatus_isAdmiUser_isnotInvisibleKey_shouldBeAvailable() {
        ShadowPrivacySettingsUtils.setIsAdminUser(true);
        ShadowPrivacySettingsUtils.setIsInvisibleKey(false);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_isnotAdmiUser_shouldBeDisabledForUser() {
        ShadowPrivacySettingsUtils.setIsAdminUser(false);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_isAdmiUser_isInvisibleKey_shouldBeDisabledUnsupported() {
        ShadowPrivacySettingsUtils.setIsAdminUser(true);
        ShadowPrivacySettingsUtils.setIsInvisibleKey(true);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }
}
