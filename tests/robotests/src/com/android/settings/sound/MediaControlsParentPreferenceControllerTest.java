/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.sound;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class MediaControlsParentPreferenceControllerTest {

    private static final String KEY = "media_controls_summary";

    private Context mContext;
    private int mOriginalQs;
    private int mOriginalResume;
    private ContentResolver mContentResolver;
    private MediaControlsParentPreferenceController mController;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mContentResolver = mContext.getContentResolver();
        mOriginalQs = Settings.Global.getInt(mContentResolver,
                Settings.Global.SHOW_MEDIA_ON_QUICK_SETTINGS, 1);
        mOriginalResume = Settings.Secure.getInt(mContentResolver,
                Settings.Secure.MEDIA_CONTROLS_RESUME, 1);
        mController = new MediaControlsParentPreferenceController(mContext, KEY);
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(mContentResolver, Settings.Global.SHOW_MEDIA_ON_QUICK_SETTINGS,
                mOriginalQs);
        Settings.Secure.putInt(mContentResolver, Settings.Secure.MEDIA_CONTROLS_RESUME,
                mOriginalResume);
    }

    @Test
    public void getAvailability_returnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getSummary_isOn_showPlayer() {
        Settings.Global.putInt(mContentResolver, Settings.Global.SHOW_MEDIA_ON_QUICK_SETTINGS, 1);
        Settings.Secure.putInt(mContentResolver, Settings.Secure.MEDIA_CONTROLS_RESUME, 1);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getString(R.string.media_controls_show_player));
    }

    @Test
    public void getSummary_isOff_hidePlayer() {
        Settings.Global.putInt(mContentResolver, Settings.Global.SHOW_MEDIA_ON_QUICK_SETTINGS, 1);
        Settings.Secure.putInt(mContentResolver, Settings.Secure.MEDIA_CONTROLS_RESUME, 0);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getString(R.string.media_controls_hide_player));
    }
}
