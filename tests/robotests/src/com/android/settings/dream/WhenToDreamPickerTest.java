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

package com.android.settings.dream;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.dream.DreamBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class WhenToDreamPickerTest {

    private WhenToDreamPicker mPicker;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DreamBackend mBackend;
    @Mock
    private UserManager mUserManager;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final Context context = spy(ApplicationProvider.getApplicationContext());

        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_dreamsEnabledOnBattery,
                true);

        when(context.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        FakeFeatureFactory.setupForTest();

        mPicker = spy(new WhenToDreamPicker());
        when(mPicker.getContext()).thenReturn(context);
        mPicker.onAttach(context);

        ReflectionHelpers.setField(mPicker, "mBackend", mBackend);
    }

    @Test
    public void getDefaultKeyReturnsCurrentWhenToDreamSetting() {
        when(mBackend.getWhenToDreamSetting()).thenReturn(DreamBackend.WHILE_CHARGING);
        assertThat(mPicker.getDefaultKey())
                .isEqualTo(DreamSettings.getKeyFromSetting(DreamBackend.WHILE_CHARGING));

        when(mBackend.getWhenToDreamSetting()).thenReturn(DreamBackend.WHILE_DOCKED);
        assertThat(mPicker.getDefaultKey())
                .isEqualTo(DreamSettings.getKeyFromSetting(DreamBackend.WHILE_DOCKED));

        when(mBackend.getWhenToDreamSetting()).thenReturn(DreamBackend.EITHER);
        assertThat(mPicker.getDefaultKey())
                .isEqualTo(DreamSettings.getKeyFromSetting(DreamBackend.EITHER));

        when(mBackend.getWhenToDreamSetting()).thenReturn(DreamBackend.NEVER);
        assertThat(mPicker.getDefaultKey())
                .isEqualTo(DreamSettings.getKeyFromSetting(DreamBackend.NEVER));
    }

    @Test
    public void setDreamWhileCharging() {
        final String key = DreamSettings.getKeyFromSetting(DreamBackend.WHILE_CHARGING);
        mPicker.setDefaultKey(key);
        verify(mBackend).setWhenToDream(DreamBackend.WHILE_CHARGING);
    }

    @Test
    public void setDreamWhileDocked() {
        final String key = DreamSettings.getKeyFromSetting(DreamBackend.WHILE_DOCKED);
        mPicker.setDefaultKey(key);
        verify(mBackend).setWhenToDream(DreamBackend.WHILE_DOCKED);
    }

    @Test
    public void setDreamWhileChargingOrDocked() {
        final String key = DreamSettings.getKeyFromSetting(DreamBackend.EITHER);
        mPicker.setDefaultKey(key);
        verify(mBackend).setWhenToDream(DreamBackend.EITHER);
    }

    @Test
    public void setDreamNever() {
        final String key = DreamSettings.getKeyFromSetting(DreamBackend.NEVER);
        mPicker.setDefaultKey(key);
        verify(mBackend).setWhenToDream(DreamBackend.NEVER);
    }
}
