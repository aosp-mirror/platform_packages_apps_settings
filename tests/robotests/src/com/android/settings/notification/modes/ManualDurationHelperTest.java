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

package com.android.settings.notification.modes;

import static com.google.common.truth.Truth.assertThat;

import android.app.Flags;
import android.content.ContentResolver;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ManualDurationHelperTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private ContentResolver mContentResolver;

    private ManualDurationHelper mHelper;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mContentResolver = RuntimeEnvironment.application.getContentResolver();

        mHelper = new ManualDurationHelper(mContext);
    }

    @Test
    public void getDurationSummary_durationForever() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ZEN_DURATION,
                Settings.Secure.ZEN_DURATION_FOREVER);
        assertThat(mHelper.getSummary()).isEqualTo(
                mContext.getString(R.string.zen_mode_duration_summary_forever));
    }

    @Test
    public void getDurationSummary_durationPrompt() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ZEN_DURATION,
                Settings.Secure.ZEN_DURATION_PROMPT);
        assertThat(mHelper.getSummary()).isEqualTo(
                mContext.getString(R.string.zen_mode_duration_summary_always_prompt));
    }

    @Test
    public void getDurationSummary_durationCustom() {
        // minutes
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ZEN_DURATION, 45);
        assertThat(mHelper.getSummary()).isEqualTo("45 minutes");

        // hours
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ZEN_DURATION, 300);
        assertThat(mHelper.getSummary()).isEqualTo("5 hours");
    }

}
