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

package com.android.settings.security;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION_O,
        shadows = {
                ShadowUserManager.class,
                ShadowLockPatternUtils.class
        })
public class EncryptionStatusPreferenceControllerTest {

    private Context mContext;
    private EncryptionStatusPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new EncryptionStatusPreferenceController(mContext);
        mPreference = new Preference(mContext);
    }

    @Test
    public void isAvailable_admin_true() {
        ShadowUserManager.getShadow().setIsAdminUser(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_notAdmin_false() {
        ShadowUserManager.getShadow().setIsAdminUser(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void updateSummary_encrypted_shouldSayEncrypted() {
        ShadowLockPatternUtils.setDeviceEncryptionEnabled(true);

        mController.updateState(mPreference);

        assertThat(mPreference.getFragment()).isNull();
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getText(R.string.crypt_keeper_encrypted_summary));
    }

    @Test
    public void updateSummary_unencrypted_shouldHasEncryptionFragment() {
        ShadowLockPatternUtils.setDeviceEncryptionEnabled(false);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getText(R.string.summary_placeholder));
        assertThat(mPreference.getFragment()).isEqualTo(CryptKeeperSettings.class.getName());
    }
}
