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

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenModesBackend;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ManualDurationPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private ContentResolver mContentResolver;

    @Mock
    private ZenModesBackend mBackend;

    @Mock
    private Fragment mParent;

    private ManualDurationPreferenceController mPrefController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        mPrefController = new ManualDurationPreferenceController(mContext, "key", mParent,
                mBackend);
    }

    @Test
    public void testIsAvailable_onlyForManualDnd() {
        assertThat(mPrefController.isAvailable(TestModeBuilder.EXAMPLE)).isFalse();
        assertThat(mPrefController.isAvailable(TestModeBuilder.MANUAL_DND_ACTIVE)).isTrue();
        assertThat(mPrefController.isAvailable(TestModeBuilder.MANUAL_DND_INACTIVE)).isTrue();
    }

    @Test
    public void testUpdateState_durationSummary() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ZEN_DURATION,
                45 /* minutes */);

        Preference pref = new Preference(mContext);
        mPrefController.updateState(pref, TestModeBuilder.EXAMPLE);

        assertThat(pref.getSummary().toString()).contains("45");
    }
}
