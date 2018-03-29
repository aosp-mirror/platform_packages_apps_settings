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

package com.android.settings.dashboard.conditional;


import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.media.AudioManager;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowAudioManager;
import com.android.settings.testutils.shadow.ShadowNotificationManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowNotificationManager.class, ShadowAudioManager.class})
public class RingerMutedConditionTest {
    private static final String TAG = "RingerMutedConditionTest";
    @Mock
    private ConditionManager mConditionManager;

    private Context mContext;
    private ShadowNotificationManager mNotificationManager;
    private ShadowAudioManager mAudioManager;
    private RingerMutedCondition mCondition;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mAudioManager = Shadow.extract(mContext.getSystemService(Context.AUDIO_SERVICE));
        mNotificationManager = Shadow.extract(
                mContext.getSystemService(Context.NOTIFICATION_SERVICE));
        when(mConditionManager.getContext()).thenReturn(mContext);
        mCondition = spy(new RingerMutedCondition(mConditionManager));
    }

    @Test
    public void verifyText() {
        assertThat(mCondition.getTitle()).isEqualTo(
                mContext.getText(R.string.condition_device_muted_title));
        assertThat(mCondition.getSummary()).isEqualTo(
                mContext.getText(R.string.condition_device_muted_summary));
        assertThat(mCondition.getActions()[0]).isEqualTo(
                mContext.getText(R.string.condition_device_muted_action_turn_on_sound));
    }

    @Test
    public void refreshState_zenModeOn_shouldNotActivate() {
        mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT);
        mNotificationManager.setZenMode(Settings.Global.ZEN_MODE_NO_INTERRUPTIONS, null, TAG);

        mCondition.refreshState();

        verify(mCondition).setActive(false);
    }

    @Test
    public void refreshState_zenModeOff_shouldActivate() {
        mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT);
        mNotificationManager.setZenMode(Settings.Global.ZEN_MODE_OFF, null, TAG);

        mCondition.refreshState();

        verify(mCondition).setActive(true);
    }
}
