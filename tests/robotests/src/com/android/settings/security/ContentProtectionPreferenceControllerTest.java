/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.security;

import static android.view.contentprotection.flags.Flags.FLAG_SETTING_UI_ENABLED;
import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class ContentProtectionPreferenceControllerTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private ContentProtectionPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new ContentProtectionPreferenceController(mContext, "key");
        mPreference = new Preference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void isAvailable_flagSettingUiDisabled_isFalse() {
        mSetFlagsRule.disableFlags(FLAG_SETTING_UI_ENABLED);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_flagSettingUiEnabled_isTrue() {
        mSetFlagsRule.enableFlags(FLAG_SETTING_UI_ENABLED);
        assertThat(mController.isAvailable()).isTrue();
    }
}
