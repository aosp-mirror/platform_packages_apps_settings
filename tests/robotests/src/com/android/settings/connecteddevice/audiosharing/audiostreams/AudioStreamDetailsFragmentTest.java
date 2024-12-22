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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamDetailsFragment.BROADCAST_ID_ARG;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamDetailsFragment.BROADCAST_NAME_ARG;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AudioStreamDetailsFragmentTest {
    @Rule public final MockitoRule mocks = MockitoJUnit.rule();
    private static final String BROADCAST_NAME = "name";
    private static final int BROADCAST_ID = 1;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private AudioStreamHeaderController mHeaderController;
    @Mock private AudioStreamButtonController mButtonController;
    private TestFragment mFragment;

    @Before
    public void setUp() {
        mFragment = spy(new TestFragment());
        doReturn(mHeaderController).when(mFragment).use(AudioStreamHeaderController.class);
        doReturn(mButtonController).when(mFragment).use(AudioStreamButtonController.class);
    }

    @Test
    public void getPreferenceScreenResId_returnsCorrectXml() {
        assertThat(mFragment.getPreferenceScreenResId())
                .isEqualTo(R.xml.bluetooth_le_audio_stream_details_fragment);
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(mFragment.getLogTag()).isEqualTo(AudioStreamDetailsFragment.TAG);
    }

    @Test
    public void getMetricsCategory_returnsCorrectEnum() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(SettingsEnums.AUDIO_STREAM_DETAIL);
    }

    @Test
    public void onAttach_getArguments() {
        Bundle bundle = new Bundle();
        bundle.putString(BROADCAST_NAME_ARG, BROADCAST_NAME);
        bundle.putInt(BROADCAST_ID_ARG, BROADCAST_ID);
        mFragment.setArguments(bundle);

        mFragment.onAttach(mContext);

        verify(mButtonController).init(BROADCAST_ID);
        verify(mHeaderController).init(mFragment, BROADCAST_NAME, BROADCAST_ID);
    }

    public static class TestFragment extends AudioStreamDetailsFragment {
        @Override
        protected <T extends AbstractPreferenceController> T use(Class<T> clazz) {
            return super.use(clazz);
        }
    }
}
