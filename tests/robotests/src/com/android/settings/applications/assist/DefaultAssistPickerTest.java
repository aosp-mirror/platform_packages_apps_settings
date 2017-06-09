/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.assist;


import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DefaultAssistPickerTest {

    private static final ComponentName TEST_ASSIST =
            new ComponentName("com.android.settings", "assist");

    private Context mContext;
    private DefaultAssistPicker mPicker;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPicker = spy(new DefaultAssistPicker());
        mPicker.onAttach(mContext);
        doReturn(mContext).when(mPicker).getContext();
    }

    @Test
    public void setDefaultAppKey_shouldUpdateDefaultAssist() {
        final List<DefaultAssistPicker.Info> assistants = new ArrayList<>();
        assistants.add(new DefaultAssistPicker.Info(TEST_ASSIST));
        ReflectionHelpers.setField(mPicker, "mAvailableAssistants", assistants);
        mPicker.setDefaultKey(TEST_ASSIST.flattenToString());

        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ASSISTANT))
                .isEqualTo(TEST_ASSIST.flattenToString());
        assertThat(mPicker.getDefaultKey())
                .isEqualTo(TEST_ASSIST.flattenToString());
    }

    @Test
    public void setDefaultAppKey_noAvaialbleAssit_shouldClearDefaultAssist() {
        final List<DefaultAssistPicker.Info> assistants = new ArrayList<>();
        ReflectionHelpers.setField(mPicker, "mAvailableAssistants", assistants);
        mPicker.setDefaultKey(TEST_ASSIST.flattenToString());

        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ASSISTANT))
                .isEmpty();
        assertThat(mPicker.getDefaultKey())
                .isNull();
    }

    @Test
    public void setDefaultAppKeyToNull_shouldClearDefaultAssist() {
        final List<DefaultAssistPicker.Info> assistants = new ArrayList<>();
        assistants.add(new DefaultAssistPicker.Info(TEST_ASSIST));
        ReflectionHelpers.setField(mPicker, "mAvailableAssistants", assistants);
        mPicker.setDefaultKey(null);

        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ASSISTANT))
                .isEmpty();
        assertThat(mPicker.getDefaultKey())
                .isNull();
    }
}
