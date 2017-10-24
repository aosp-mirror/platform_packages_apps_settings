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

package com.android.settings.notification;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class VolumeSeekBarPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private VolumeSeekBarPreference mPreference;
    @Mock
    private VolumeSeekBarPreference.Callback mCallback;

    private VolumeSeekBarPreferenceControllerTestable mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mScreen.findPreference(nullable(String.class))).thenReturn(mPreference);
        mController =
            new VolumeSeekBarPreferenceControllerTestable(mContext, mCallback);
    }

    @Test
    public void displayPreference_available_shouldUpdatePreference() {
        mController.displayPreference(mScreen);

        verify(mPreference).setCallback(mCallback);
        verify(mPreference).setStream(mController.AUDIO_STREAM);
        verify(mPreference).setMuteIcon(mController.MUTE_ICON);
    }

    @Test
    public void displayPreference_notAvailable_shouldNotUpdatePreference() {
        mController =
            new VolumeSeekBarPreferenceControllerTestable(mContext, mCallback, false);

        mController.displayPreference(mScreen);

        verify(mPreference, never()).setCallback(any(VolumeSeekBarPreference.Callback.class));
        verify(mPreference, never()).setStream(anyInt());
        verify(mPreference, never()).setMuteIcon(anyInt());
    }

    @Test
    public void onResume_shouldResumePreference() {
        mController.displayPreference(mScreen);

        mController.onResume();

        verify(mPreference).onActivityResume();
    }

    @Test
    public void onPause_shouldPausePreference() {
        mController.displayPreference(mScreen);

        mController.onPause();

        verify(mPreference).onActivityPause();
    }

    private class VolumeSeekBarPreferenceControllerTestable extends
        VolumeSeekBarPreferenceController {

        private final static int AUDIO_STREAM = 1;
        private final static int MUTE_ICON = 2;

        private boolean mAvailable;

        VolumeSeekBarPreferenceControllerTestable(Context context,
            VolumeSeekBarPreference.Callback callback) {
            this(context, callback, true);
        }

        VolumeSeekBarPreferenceControllerTestable(Context context,
            VolumeSeekBarPreference.Callback callback, boolean available) {
            super(context, callback, null);
            mAvailable = available;
        }

        @Override
        public String getPreferenceKey() {
            return null;
        }

        @Override
        public boolean handlePreferenceTreeClick(Preference preference) {
            return false;
        }

        @Override
        public boolean isAvailable() {
            return mAvailable;
        }

        @Override
        public int getAudioStream() {
            return AUDIO_STREAM;
        }

        @Override
        public int getMuteIcon() {
            return MUTE_ICON;
        }

        private void setAvailable(boolean available) {

        }
    }

}
