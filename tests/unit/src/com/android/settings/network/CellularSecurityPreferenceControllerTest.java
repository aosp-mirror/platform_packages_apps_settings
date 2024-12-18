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

package com.android.settings.network;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public final class CellularSecurityPreferenceControllerTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private TelephonyManager mTelephonyManager;
    private Preference mPreference;
    private PreferenceScreen mPreferenceScreen;

    private static final String PREF_KEY = "cellular_security_pref_controller_test";
    private Context mContext;
    private CellularSecurityPreferenceController mController;

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

        doNothing().when(mContext).startActivity(any(Intent.class));

        mController = new CellularSecurityPreferenceController(mContext, PREF_KEY);

        mPreference = spy(new Preference(mContext));
        mPreference.setKey(PREF_KEY);
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mPreference);
    }

    @Test
    public void getAvailabilityStatus_hardwareSupported_shouldReturnTrue() {
        // Enable telephony API flags for testing
        enableFlags(true);

        // Hardware support is enabled
        doReturn(true).when(mTelephonyManager).isNullCipherNotificationsEnabled();
        doReturn(true).when(mTelephonyManager)
              .isCellularIdentifierDisclosureNotificationsEnabled();
        doReturn(true).when(mTelephonyManager).isNullCipherAndIntegrityPreferenceEnabled();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);

        // Disable null cipher toggle API, should still be available
        doThrow(new UnsupportedOperationException("test")).when(mTelephonyManager)
              .isNullCipherAndIntegrityPreferenceEnabled();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);

        // Enable null cipher toggle API, disable notifications API, should still be available
        doReturn(true).when(mTelephonyManager).isNullCipherAndIntegrityPreferenceEnabled();
        doThrow(new UnsupportedOperationException("test")).when(mTelephonyManager)
              .isNullCipherNotificationsEnabled();
        doThrow(new UnsupportedOperationException("test")).when(mTelephonyManager)
              .isCellularIdentifierDisclosureNotificationsEnabled();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noHardwareSupport_shouldReturnFalse() {
        // Enable telephony API flags for testing
        enableFlags(true);

        // Hardware support is disabled
        doThrow(new UnsupportedOperationException("test")).when(mTelephonyManager)
              .isNullCipherNotificationsEnabled();
        doThrow(new UnsupportedOperationException("test")).when(mTelephonyManager)
              .isCellularIdentifierDisclosureNotificationsEnabled();
        doThrow(new UnsupportedOperationException("test")).when(mTelephonyManager)
              .isNullCipherAndIntegrityPreferenceEnabled();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_flagsDisabled_shouldReturnFalse() {
        // Both flags disabled
        enableFlags(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);

        // One flag is disabled
        mSetFlagsRule.disableFlags(
                Flags.FLAG_ENABLE_IDENTIFIER_DISCLOSURE_TRANSPARENCY_UNSOL_EVENTS);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void handlePreferenceTreeClick_safetyCenterSupported_shouldRedirectToSafetyCenter() {
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

        boolean prefHandled = mController.handlePreferenceTreeClick(mPreference);

        assertThat(prefHandled).isTrue();
        verify(mContext).startActivity(intentCaptor.capture());
        assertThat(intentCaptor.getValue().getAction()).isEqualTo(Intent.ACTION_SAFETY_CENTER);
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
