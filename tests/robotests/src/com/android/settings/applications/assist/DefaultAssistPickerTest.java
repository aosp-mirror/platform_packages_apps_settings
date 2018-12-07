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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivityManager;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class DefaultAssistPickerTest {

    private static ComponentName sTestAssist;

    @BeforeClass
    public static void beforeClass() {
        sTestAssist = new ComponentName("com.android.settings", "assist");
    }

    private Context mContext;
    private DefaultAssistPicker mPicker;
    private ShadowActivityManager mShadowActivityManager;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mShadowActivityManager = Shadow.extract(mContext.getSystemService(ActivityManager.class));
        mPicker = spy(new DefaultAssistPicker());
        mPicker.onAttach(mContext);
        doReturn(mContext).when(mPicker).getContext();
    }

    @Test
    public void setDefaultAppKey_shouldUpdateDefaultAssist() {
        final List<DefaultAssistPicker.Info> assistants = new ArrayList<>();
        assistants.add(new DefaultAssistPicker.Info(sTestAssist));
        ReflectionHelpers.setField(mPicker, "mAvailableAssistants", assistants);
        mPicker.setDefaultKey(sTestAssist.flattenToString());

        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ASSISTANT))
                .isEqualTo(sTestAssist.flattenToString());
        assertThat(mPicker.getDefaultKey()).isEqualTo(sTestAssist.flattenToString());
    }

    @Test
    public void setDefaultAppKey_noAvailableAssist_shouldClearDefaultAssist() {
        final List<DefaultAssistPicker.Info> assistants = new ArrayList<>();
        ReflectionHelpers.setField(mPicker, "mAvailableAssistants", assistants);
        mPicker.setDefaultKey(sTestAssist.flattenToString());

        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ASSISTANT))
                .isEmpty();
        assertThat(mPicker.getDefaultKey()).isNull();
    }

    @Test
    public void setDefaultAppKeyToNull_shouldClearDefaultAssist() {
        final List<DefaultAssistPicker.Info> assistants = new ArrayList<>();
        assistants.add(new DefaultAssistPicker.Info(sTestAssist));
        ReflectionHelpers.setField(mPicker, "mAvailableAssistants", assistants);
        mPicker.setDefaultKey(null);

        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ASSISTANT))
                .isEmpty();
        assertThat(mPicker.getDefaultKey()).isNull();
    }

    @Test
    public void addAssistService_lowRamDevice_shouldDoNothing() {
        mShadowActivityManager.setIsLowRamDevice(true);
        mPicker.addAssistServices();

        assertThat(mPicker.mAvailableAssistants).hasSize(0);
    }
}
