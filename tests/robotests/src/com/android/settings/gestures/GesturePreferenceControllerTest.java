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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.widget.VideoPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class GesturePreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)

    private TestPrefController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new TestPrefController(mContext, "testKey");
        mPreference = new Preference(RuntimeEnvironment.application);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mPreference.getKey())).thenReturn(mPreference);
    }

    @Test
    public void display_configIsTrue_shouldDisplay() {
        mController.mIsPrefAvailable = true;
        when(mScreen.findPreference(anyString())).thenReturn(mock(VideoPreference.class));

        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void display_configIsFalse_shouldNotDisplay() {
        mController.mIsPrefAvailable = false;

        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void onStart_shouldStartVideoPreference() {
        final VideoPreference videoPreference = mock(VideoPreference.class);
        when(mScreen.findPreference(mController.getVideoPrefKey())).thenReturn(videoPreference);
        mController.mIsPrefAvailable = true;
        mController.displayPreference(mScreen);

        mController.onStart();

        verify(videoPreference).onViewVisible();
    }

    @Test
    public void onStop_shouldStopVideoPreference() {
        final VideoPreference videoPreference = mock(VideoPreference.class);
        when(mScreen.findPreference(mController.getVideoPrefKey())).thenReturn(videoPreference);
        mController.mIsPrefAvailable = true;

        mController.displayPreference(mScreen);
        mController.onStop();

        verify(videoPreference).onViewInvisible();
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

    private class TestPrefController extends GesturePreferenceController {

        boolean mIsPrefAvailable;
        boolean mIsPrefEnabled;

        private TestPrefController(Context context, String key) {
            super(context, key);
        }

        @Override
        public int getAvailabilityStatus() {
            return mIsPrefAvailable ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
        }

        @Override
        protected String getVideoPrefKey() {
            return "videoKey";
        }

        @Override
        public boolean isChecked() {
            return mIsPrefEnabled;
        }

        @Override
        public boolean setChecked(boolean isChecked) {
            return false;
        }
    }
}
