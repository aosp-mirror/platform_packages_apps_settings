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
 * limitations under the License.
 */

package com.android.settings.gestures;

import static android.provider.Settings.Secure.VOLUME_HUSH_GESTURE;
import static android.provider.Settings.Secure.VOLUME_HUSH_MUTE;
import static android.provider.Settings.Secure.VOLUME_HUSH_OFF;
import static android.provider.Settings.Secure.VOLUME_HUSH_VIBRATE;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.Assert.assertEquals;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.R;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProviderImpl;
import com.android.settings.search.InlinePayload;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class PreventRingingPreferenceControllerTest {

    private static final String KEY_PICK_UP = "gesture_prevent_ringing";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    private PreventRingingPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new PreventRingingPreferenceController(mContext, KEY_PICK_UP);
    }

    @Test
    public void testIsAvailable_configIsTrue_shouldReturnTrue() {
        when(mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_volumeHushGestureEnabled)).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testIsAvailable_configIsFalse_shouldReturnFalse() {
        when(mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_volumeHushGestureEnabled)).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testGetSummary_mute() {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                Settings.Secure.VOLUME_HUSH_MUTE);
        assertEquals(mContext.getString(R.string.prevent_ringing_option_mute_summary),
                mController.getSummary());
    }

    @Test
    public void testGetSummary_vibrate() {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                Settings.Secure.VOLUME_HUSH_VIBRATE);
        assertEquals(mContext.getString(R.string.prevent_ringing_option_vibrate_summary),
                mController.getSummary());
    }
    @Test
    public void testGetSummary_other() {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                7);
        assertEquals(mContext.getString(R.string.prevent_ringing_option_none_summary),
                mController.getSummary());
    }

    @Test
    public void testUpdateState_mute() {
        ListPreference pref = mock(ListPreference.class);
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                Settings.Secure.VOLUME_HUSH_MUTE);
        mController.updateState(pref);
        verify(pref).setValue(String.valueOf(Settings.Secure.VOLUME_HUSH_MUTE));
    }

    @Test
    public void testUpdateState_vibrate() {
        ListPreference pref = mock(ListPreference.class);
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                Settings.Secure.VOLUME_HUSH_VIBRATE);
        mController.updateState(pref);
        verify(pref).setValue(String.valueOf(Settings.Secure.VOLUME_HUSH_VIBRATE));
    }

    @Test
    public void testUpdateState_other() {
        ListPreference pref = mock(ListPreference.class);
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                7);
        mController.updateState(pref);
        verify(pref).setValue(String.valueOf(Settings.Secure.VOLUME_HUSH_OFF));
    }

    @Test
    public void testUpdateState_parentPage() {
        Preference pref = mock(Preference.class);
        // verify no exception
        mController.updateState(pref);
    }

    @Test
    public void testOnPreferenceChange() {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.VOLUME_HUSH_GESTURE,
                7);

        mController.onPreferenceChange(mock(Preference.class), String.valueOf(VOLUME_HUSH_MUTE));

        assertEquals(VOLUME_HUSH_MUTE, Settings.Secure.getInt(mContext.getContentResolver(),
                VOLUME_HUSH_GESTURE, VOLUME_HUSH_OFF));
    }
}
