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

import static com.android.settings.security.EncryptionStatusPreferenceController.PREF_KEY_ENCRYPTION_DETAIL_PAGE;
import static com.android.settings.security.EncryptionStatusPreferenceController.PREF_KEY_ENCRYPTION_SECURITY_PAGE;
import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.UserManager;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = ShadowLockPatternUtils.class)
public class EncryptionStatusPreferenceControllerTest {

    private Context mContext;
    private EncryptionStatusPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController =
            new EncryptionStatusPreferenceController(mContext, PREF_KEY_ENCRYPTION_DETAIL_PAGE);
        mPreference = new Preference(mContext);
    }

    @Test
    public void isAvailable_admin_true() {
        UserManager userManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        Shadows.shadowOf(userManager).setIsAdminUser(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_notAdmin_false() {
        UserManager userManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        Shadows.shadowOf(userManager).setIsAdminUser(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isAvailable_notVisible_false() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isAvailable_notVisible_butNotDetailPage_true() {
        mController = new EncryptionStatusPreferenceController(mContext,
                PREF_KEY_ENCRYPTION_SECURITY_PAGE);

        UserManager userManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        Shadows.shadowOf(userManager).setIsAdminUser(true);
        assertThat(mController.isAvailable()).isTrue();
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

        final CharSequence summary = mContext.getText(R.string.decryption_settings_summary);
        assertThat(mPreference.getSummary()).isEqualTo(summary);
        assertThat(mController.getPreferenceKey()).isNotEqualTo(PREF_KEY_ENCRYPTION_SECURITY_PAGE);
        assertThat(mPreference.getFragment()).isEqualTo(CryptKeeperSettings.class.getName());
    }

    @Test
    public void updateSummary_unencrypted_securityPage_shouldNotHaveEncryptionFragment() {
        mController =
            new EncryptionStatusPreferenceController(mContext, PREF_KEY_ENCRYPTION_SECURITY_PAGE);
        ShadowLockPatternUtils.setDeviceEncryptionEnabled(false);

        mController.updateState(mPreference);

        final CharSequence summary = mContext.getText(R.string.decryption_settings_summary);
        assertThat(mPreference.getSummary()).isEqualTo(summary);

        assertThat(mPreference.getFragment()).isNotEqualTo(CryptKeeperSettings.class.getName());
    }
}
