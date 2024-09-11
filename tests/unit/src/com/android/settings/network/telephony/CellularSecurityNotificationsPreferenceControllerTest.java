/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.network.telephony;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Build;
import android.platform.test.flag.junit.SetFlagsRule;
import android.safetycenter.SafetyCenterManager;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.internal.telephony.flags.Flags;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class CellularSecurityNotificationsPreferenceControllerTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private TelephonyManager mTelephonyManager;
    private Preference mPreference;
    private PreferenceScreen mPreferenceScreen;

    private static final String PREF_KEY = "cellular_security_notifications_pref_controller_test";
    private Context mContext;
    private CellularSecurityNotificationsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // Tests must be skipped if these conditions aren't met as they cannot be mocked
        Assume.assumeTrue(Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU);
        SafetyCenterManager mSafetyCenterManager = InstrumentationRegistry.getInstrumentation()
                .getContext().getSystemService(SafetyCenterManager.class);
        Assume.assumeTrue(mSafetyCenterManager != null);
        Assume.assumeTrue(mSafetyCenterManager.isSafetyCenterEnabled());

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);

        mController = new CellularSecurityNotificationsPreferenceController(mContext, PREF_KEY);

        mPreference = spy(new Preference(mContext));
        mPreference.setKey(PREF_KEY);
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mPreference);
    }

    @Test
    public void getAvailabilityStatus_hardwareSupported_shouldReturnTrue() {
        // All flags enabled
        enableFlags(true);

        // Hardware support is enabled
        doReturn(true).when(mTelephonyManager).isNullCipherNotificationsEnabled();
        doReturn(true).when(mTelephonyManager)
              .isCellularIdentifierDisclosureNotificationsEnabled();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noHardwareSupport_shouldReturnFalse() {
        // All flags enabled
        enableFlags(true);

        // Hardware support is disabled
        doThrow(new UnsupportedOperationException("test")).when(mTelephonyManager)
              .isNullCipherNotificationsEnabled();
        doThrow(new UnsupportedOperationException("test")).when(mTelephonyManager)
              .isCellularIdentifierDisclosureNotificationsEnabled();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_flagsDisabled_shouldReturnFalse() {
        // All flags disabled
        enableFlags(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);

        // One flag is disabled
        enableFlags(true);
        mSetFlagsRule.disableFlags(
                Flags.FLAG_ENABLE_IDENTIFIER_DISCLOSURE_TRANSPARENCY_UNSOL_EVENTS);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void setChecked_flagsDisabled_shouldReturnFalse() {
        // Flags disabled
        enableFlags(false);

        // Hardware support is enabled
        doNothing().when(mTelephonyManager).setNullCipherNotificationsEnabled(true);
        doNothing().when(mTelephonyManager)
              .setEnableCellularIdentifierDisclosureNotifications(true);
        assertThat(mController.setChecked(false)).isFalse();
        assertThat(mController.setChecked(true)).isFalse();

        // Enable 1 flag, make sure it still fails
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_MODEM_CIPHER_TRANSPARENCY_UNSOL_EVENTS);
        assertThat(mController.setChecked(false)).isFalse();
        assertThat(mController.setChecked(true)).isFalse();
    }

    @Test
    public void setChecked_hardwareDisabled_shouldReturnFalse() {
        // Flags disabled
        enableFlags(false);

        // Hardware support is enabled
        doNothing().when(mTelephonyManager).setNullCipherNotificationsEnabled(true);
        doNothing().when(mTelephonyManager)
                .setEnableCellularIdentifierDisclosureNotifications(true);
        assertThat(mController.setChecked(true)).isFalse();

        // Hardware support is enabled, called with false
        doNothing().when(mTelephonyManager).setNullCipherNotificationsEnabled(false);
        doNothing().when(mTelephonyManager)
                .setEnableCellularIdentifierDisclosureNotifications(false);
        assertThat(mController.setChecked(false)).isFalse();
    }

    @Test
    public void setChecked_shouldReturnTrue() {
        enableFlags(true);

        // Hardware support is enabled, enabling the feature
        doNothing().when(mTelephonyManager).setNullCipherNotificationsEnabled(true);
        doNothing().when(mTelephonyManager)
              .setEnableCellularIdentifierDisclosureNotifications(true);
        assertThat(mController.setChecked(true)).isTrue();

        // Hardware support is enabled, disabling the feature
        doNothing().when(mTelephonyManager).setNullCipherNotificationsEnabled(false);
        doNothing().when(mTelephonyManager)
              .setEnableCellularIdentifierDisclosureNotifications(false);
        assertThat(mController.setChecked(false)).isTrue();
    }

    @Test
    public void isChecked_shouldReturnTrue() {
        // Hardware support is enabled
        doReturn(true).when(mTelephonyManager).isNullCipherNotificationsEnabled();
        doReturn(true).when(mTelephonyManager)
              .isCellularIdentifierDisclosureNotificationsEnabled();
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_flagsDisabled_shouldReturnFalse() {
        enableFlags(false);

        // Hardware support is enabled
        doReturn(true).when(mTelephonyManager).isNullCipherNotificationsEnabled();
        doReturn(true).when(mTelephonyManager)
              .isCellularIdentifierDisclosureNotificationsEnabled();

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_hardwareUnsupported_shouldReturnFalse() {
        enableFlags(true);

        // Hardware support is disabled
        doThrow(new UnsupportedOperationException("test")).when(mTelephonyManager)
              .isNullCipherNotificationsEnabled();
        doThrow(new UnsupportedOperationException("test")).when(mTelephonyManager)
              .isCellularIdentifierDisclosureNotificationsEnabled();

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_notificationsDisabled_shouldReturnFalse() {
        enableFlags(true);

        // Hardware support is enabled, but APIs are disabled
        doReturn(false).when(mTelephonyManager).isNullCipherNotificationsEnabled();
        doReturn(false).when(mTelephonyManager)
              .isCellularIdentifierDisclosureNotificationsEnabled();
        assertThat(mController.isChecked()).isFalse();

        // Enable 1 API, should still return false
        doReturn(true).when(mTelephonyManager).isNullCipherNotificationsEnabled();
        assertThat(mController.isChecked()).isFalse();
    }

    private void enableFlags(boolean enabled) {
        if (enabled) {
            mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_MODEM_CIPHER_TRANSPARENCY_UNSOL_EVENTS);
            mSetFlagsRule.enableFlags(
                    Flags.FLAG_ENABLE_IDENTIFIER_DISCLOSURE_TRANSPARENCY_UNSOL_EVENTS);
            mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_MODEM_CIPHER_TRANSPARENCY);
            mSetFlagsRule.enableFlags(
                    Flags.FLAG_ENABLE_IDENTIFIER_DISCLOSURE_TRANSPARENCY);
        } else {
            mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_MODEM_CIPHER_TRANSPARENCY_UNSOL_EVENTS);
            mSetFlagsRule.disableFlags(
                    Flags.FLAG_ENABLE_IDENTIFIER_DISCLOSURE_TRANSPARENCY_UNSOL_EVENTS);
            mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_MODEM_CIPHER_TRANSPARENCY);
            mSetFlagsRule.disableFlags(
                    Flags.FLAG_ENABLE_IDENTIFIER_DISCLOSURE_TRANSPARENCY);
        }
    }
}
