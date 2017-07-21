/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.widget.VideoPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class GesturePreferenceControllerTest {


    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Lifecycle mLifecycle;
    private TestPrefController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new TestPrefController(mContext, mLifecycle);
    }

    @Test
    public void display_configIsTrue_shouldDisplay() {
        mController.mIsPrefAvailable = true;
        when(mScreen.findPreference(anyString())).thenReturn(mock(VideoPreference.class));

        mController.displayPreference(mScreen);

        verify(mScreen, never()).removePreference(any(Preference.class));
    }

    @Test
    public void display_configIsFalse_shouldNotDisplay() {
        mController.mIsPrefAvailable = false;
        final Preference preference = mock(Preference.class);
        when(mScreen.getPreferenceCount()).thenReturn(1);
        when(mScreen.getPreference(0)).thenReturn(preference);
        when(preference.getKey()).thenReturn(mController.getPreferenceKey());

        mController.displayPreference(mScreen);

        verify(mScreen).removePreference(any(Preference.class));
    }

    @Test
    public void onResume_shouldStartVideoPreferenceWithVideoPauseState() {
        final VideoPreference videoPreference = mock(VideoPreference.class);
        when(mScreen.findPreference(mController.getVideoPrefKey())).thenReturn(videoPreference);
        mController.mIsPrefAvailable = true;

        mController.displayPreference(mScreen);
        final Bundle savedState = new Bundle();

        mController.onCreate(null);
        mController.onResume();
        verify(videoPreference).onViewVisible(false);

        reset(videoPreference);
        savedState.putBoolean(mController.KEY_VIDEO_PAUSED, true);
        mController.onCreate(savedState);
        mController.onResume();
        verify(videoPreference).onViewVisible(true);

        reset(videoPreference);
        savedState.putBoolean(mController.KEY_VIDEO_PAUSED, false);
        mController.onCreate(savedState);
        mController.onResume();
        verify(videoPreference).onViewVisible(false);
    }

    @Test
    public void onPause_shouldStopVideoPreference() {
        final VideoPreference videoPreference = mock(VideoPreference.class);
        when(mScreen.findPreference(mController.getVideoPrefKey())).thenReturn(videoPreference);
        mController.mIsPrefAvailable = true;

        mController.displayPreference(mScreen);
        mController.onPause();

        verify(videoPreference).onViewInvisible();
    }

    @Test
    public void onPause_shouldUpdateVideoPauseState() {
        final VideoPreference videoPreference = mock(VideoPreference.class);
        when(mScreen.findPreference(mController.getVideoPrefKey())).thenReturn(videoPreference);
        mController.mIsPrefAvailable = true;
        mController.displayPreference(mScreen);

        when(videoPreference.isVideoPaused()).thenReturn(true);
        mController.onPause();
        assertThat(mController.mVideoPaused).isTrue();

        when(videoPreference.isVideoPaused()).thenReturn(false);
        mController.onPause();
        assertThat(mController.mVideoPaused).isFalse();
    }

    @Test
    public void onSaveInstanceState_shouldSaveVideoPauseState() {
        final Bundle outState = mock(Bundle.class);

        mController.mVideoPaused = true;
        mController.onSaveInstanceState(outState);
        verify(outState).putBoolean(mController.KEY_VIDEO_PAUSED, true);

        mController.mVideoPaused = false;
        mController.onSaveInstanceState(outState);
        verify(outState).putBoolean(mController.KEY_VIDEO_PAUSED, false);
    }

    @Test
    public void updateState_preferenceSetCheckedWhenSettingIsOn() {
        // Mock a TwoStatePreference
        final TwoStatePreference preference = mock(TwoStatePreference.class);
        // Set the setting to be enabled.
        mController.mIsPrefEnabled = true;
        // Run through updateState
        mController.updateState(preference);

        // Verify pref is checked (as setting is enabled).
        verify(preference).setChecked(true);
    }

    @Test
    public void updateState_preferenceSetUncheckedWhenSettingIsOff() {
        // Mock a TwoStatePreference
        final TwoStatePreference preference = mock(TwoStatePreference.class);
        // Set the setting to be disabled.
        mController.mIsPrefEnabled = false;

        // Run through updateState
        mController.updateState(preference);

        // Verify pref is unchecked (as setting is disabled).
        verify(preference).setChecked(false);
    }

    @Test
    public void updateState_notTwoStatePreference_setSummary() {
        // Mock a regular preference
        final Preference preference = mock(Preference.class);
        // Set the setting to be disabled.
        mController.mIsPrefEnabled = false;

        // Run through updateState
        mController.updateState(preference);

        // Verify summary is set to off (as setting is disabled).
        verify(preference).setSummary(com.android.settings.R.string.gesture_setting_off);
    }

    private class TestPrefController extends GesturePreferenceController {

        boolean mIsPrefAvailable;
        boolean mIsPrefEnabled;

        public TestPrefController(Context context,
                Lifecycle lifecycle) {
            super(context, lifecycle);
        }

        @Override
        public boolean isAvailable() {
            return mIsPrefAvailable;
        }

        @Override
        public String getPreferenceKey() {
            return "testKey";
        }

        @Override
        protected String getVideoPrefKey() {
            return "videoKey";
        }

        @Override
        protected boolean isSwitchPrefEnabled() {
            return mIsPrefEnabled;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            return false;
        }
    }
}
