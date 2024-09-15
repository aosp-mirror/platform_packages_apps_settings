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

package com.android.settings.connecteddevice.audiosharing;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsCategoryController;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settings.widget.SettingsMainSwitchBar;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowFragment.class})
public class AudioSharingDashboardFragmentTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private SettingsActivity mActivity;
    @Mock private SettingsMainSwitchBar mSwitchBar;
    @Mock private AudioSharingDeviceVolumeGroupController mVolumeGroupController;
    @Mock private AudioSharingCallAudioPreferenceController mCallAudioController;
    @Mock private AudioSharingPlaySoundPreferenceController mPlaySoundController;
    @Mock private AudioStreamsCategoryController mStreamsCategoryController;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private AudioSharingDashboardFragment mFragment;

    @Before
    public void setUp() {
        mFragment = new AudioSharingDashboardFragment();
    }

    @Test
    public void getPreferenceScreenResId_returnsCorrectXml() {
        assertThat(mFragment.getPreferenceScreenResId())
                .isEqualTo(R.xml.bluetooth_le_audio_sharing);
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(mFragment.getLogTag()).isEqualTo("AudioSharingDashboardFrag");
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(SettingsEnums.AUDIO_SHARING_SETTINGS);
    }

    @Test
    public void getHelpResource_returnsCorrectResource() {
        assertThat(mFragment.getHelpResource()).isEqualTo(R.string.help_url_audio_sharing);
    }

    @Test
    public void onActivityCreated_showSwitchBar() {
        doReturn(mSwitchBar).when(mActivity).getSwitchBar();
        mFragment = spy(new AudioSharingDashboardFragment());
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(mContext).when(mFragment).getContext();
        mFragment.onAttach(mContext);
        mFragment.onActivityCreated(new Bundle());
        verify(mSwitchBar).show();
    }

    @Test
    public void onAudioSharingStateChanged_updateVisibilityForControllers() {
        mFragment.setControllers(
                mVolumeGroupController,
                mCallAudioController,
                mPlaySoundController,
                mStreamsCategoryController);
        mFragment.onAudioSharingStateChanged();
        verify(mVolumeGroupController).updateVisibility();
        verify(mCallAudioController).updateVisibility();
        verify(mPlaySoundController).updateVisibility();
        verify(mStreamsCategoryController).updateVisibility();
    }

    @Test
    public void onAudioSharingProfilesConnected_registerCallbacksForVolumeGroupController() {
        mFragment.setControllers(
                mVolumeGroupController,
                mCallAudioController,
                mPlaySoundController,
                mStreamsCategoryController);
        mFragment.onAudioSharingProfilesConnected();
        verify(mVolumeGroupController).onAudioSharingProfilesConnected();
    }
}
