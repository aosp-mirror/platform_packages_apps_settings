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

package com.android.settings.notification;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings.Global;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ChargingSoundPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Activity mActivity;
    @Mock
    private ContentResolver mContentResolver;
    @Mock
    private SoundSettings mSetting;
    @Mock
    private Context mContext;

    private ChargingSoundPreferenceController mController;
    private SwitchPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mSetting.getActivity()).thenReturn(mActivity);
        when(mActivity.getContentResolver()).thenReturn(mContentResolver);
        mPreference = new SwitchPreference(ShadowApplication.getInstance().getApplicationContext());
        mController = new ChargingSoundPreferenceController(mContext, mSetting, null);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        doReturn(mScreen).when(mSetting).getPreferenceScreen();
    }

    @Test
    public void isAvailable_isAlwaysTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void displayPreference_chargingSoundEnabled_shouldCheckedPreference() {
        Global.putInt(mContentResolver, Global.CHARGING_SOUNDS_ENABLED, 1);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void displayPreference_chargingSoundDisabled_shouldUncheckedPreference() {
        Global.putInt(mContentResolver, Global.CHARGING_SOUNDS_ENABLED, 0);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void onPreferenceChanged_preferenceChecked_shouldEnabledChargingSound() {
        mController.displayPreference(mScreen);

        mPreference.getOnPreferenceChangeListener().onPreferenceChange(mPreference, true);

        assertThat(Global.getInt(mContentResolver, Global.CHARGING_SOUNDS_ENABLED, 1))
            .isEqualTo(1);
    }

    @Test
    public void onPreferenceChanged_preferenceUnchecked_shouldDisabledChargingSound() {
        mController.displayPreference(mScreen);

        mPreference.getOnPreferenceChangeListener().onPreferenceChange(mPreference, false);

        assertThat(Global.getInt(mContentResolver, Global.CHARGING_SOUNDS_ENABLED, 1))
            .isEqualTo(0);
    }
}
