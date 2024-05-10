/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.HearingAidCompatibilityPreferenceController.HAC_DISABLED;
import static com.android.settings.accessibility.HearingAidCompatibilityPreferenceController.HAC_ENABLED;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link HearingAidCompatibilityPreferenceControllerTest}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowAudioManager.class,
})
public class HearingAidCompatibilityPreferenceControllerTest {

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private ContentResolver mContentResolver;

    private TelephonyManager mTelephonyManager;
    private AudioManager mAudioManager;
    private final SwitchPreference mPreference = new SwitchPreference(mContext);
    private HearingAidCompatibilityPreferenceController mController;

    @Before
    public void setUp() {
        mTelephonyManager = spy(mContext.getSystemService(TelephonyManager.class));
        mAudioManager = spy(mContext.getSystemService(AudioManager.class));
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(AudioManager.class)).thenReturn(mAudioManager);
        mController = new HearingAidCompatibilityPreferenceController(mContext,
                "hearing_aid_compatibility");
    }

    @Test
    public void getAvailabilityStatus_HacSupported_shouldReturnAvailable() {
        doReturn(true).when(mTelephonyManager).isHearingAidCompatibilitySupported();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_HacNotSupported_shouldReturnUnsupported() {
        doReturn(false).when(mTelephonyManager).isHearingAidCompatibilitySupported();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }


    @Test
    public void isChecked_enabledHac_shouldReturnTrue() {
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.HEARING_AID,
                HAC_ENABLED);
        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void isChecked_disabledHac_shouldReturnFalse() {
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.HEARING_AID,
                HAC_DISABLED);
        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void setChecked_enabled_shouldEnableHac() {
        mController.setChecked(true);

        assertThat(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HEARING_AID, HAC_DISABLED)).isEqualTo(HAC_ENABLED);
        verify(mAudioManager).setParameters("HACSetting=ON;");
    }

    @Test
    public void setChecked_disabled_shouldDisableHac() {
        mController.setChecked(false);

        assertThat(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HEARING_AID, HAC_DISABLED)).isEqualTo(HAC_DISABLED);
        verify(mAudioManager).setParameters("HACSetting=OFF;");
    }

}
