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

import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class AssistContextPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private TwoStatePreference mPreference;
    @Mock
    private AssistContextPreferenceController.SettingObserver mObserver;
    private Context mContext;
    private AssistContextPreferenceController mController;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mContext = RuntimeEnvironment.application;
        mController = new AssistContextPreferenceController(mContext, mLifecycle);
        ReflectionHelpers.setField(mController, "mSettingObserver", mObserver);
    }

    @Test
    public void isAvailable_hasAssistant_shouldReturnTrue() {
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ASSISTANT, "com.android.settings/assist");
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_hasNoAssistant_shouldReturnFalse() {
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ASSISTANT, "");
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void onResume_shouldUpdatePreference() {
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ASSISTANT, "com.android.settings/assist");
        mController.displayPreference(mScreen);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ASSIST_STRUCTURE_ENABLED, 1);

        mLifecycle.handleLifecycleEvent(ON_RESUME);
        verify(mObserver).register(any(ContentResolver.class), eq(true));
        verify(mPreference).setChecked(true);
    }
}
