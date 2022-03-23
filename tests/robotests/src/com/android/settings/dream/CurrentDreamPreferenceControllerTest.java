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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;

import com.android.settings.widget.GearPreference;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.dream.DreamBackend.DreamInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class CurrentDreamPreferenceControllerTest {

    private CurrentDreamPreferenceController mController;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DreamBackend mBackend;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private DreamInfo mDreamInfo;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mController = new CurrentDreamPreferenceController(mContext, "test");
        ReflectionHelpers.setField(mController, "mBackend", mBackend);
    }

    @Test
    public void isDisabledIfNoDreamsAvailable() {
        when(mBackend.getDreamInfos()).thenReturn(new ArrayList<>(0));

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isEnabledIfDreamsAvailable() {
        when(mBackend.getDreamInfos()).thenReturn(Collections.singletonList(mDreamInfo));

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void gearShowsIfActiveDreamInfoHasOptions() {
        mDreamInfo.settingsComponentName = mock(ComponentName.class);
        mDreamInfo.isActive = true;

        when(mBackend.getDreamInfos()).thenReturn(Collections.singletonList(mDreamInfo));

        GearPreference mockPref = mock(GearPreference.class);
        ArgumentCaptor<GearPreference.OnGearClickListener> captor =
                ArgumentCaptor.forClass(GearPreference.OnGearClickListener.class);

        // verify that updateState sets a non-null gear click listener
        mController.updateState(mockPref);
        verify(mockPref).setOnGearClickListener(captor.capture());
        captor.getAllValues().forEach(listener -> assertThat(listener).isNotNull());
    }

    @Test
    public void gearHidesIfActiveDreamInfoHasNoOptions() {
        mDreamInfo.settingsComponentName = null;
        mDreamInfo.isActive = true;

        when(mBackend.getDreamInfos()).thenReturn(Collections.singletonList(mDreamInfo));

        GearPreference mockPref = mock(GearPreference.class);
        ArgumentCaptor<GearPreference.OnGearClickListener> captor =
                ArgumentCaptor.forClass(GearPreference.OnGearClickListener.class);

        // setting a null onGearClickListener removes the gear from view
        mController.updateState(mockPref);
        verify(mockPref).setOnGearClickListener(captor.capture());
        captor.getAllValues().forEach(listener -> assertThat(listener).isNull());
    }
}
