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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserManager;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.dream.DreamBackend.DreamInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class CurrentDreamPickerTest {

    private static String COMPONENT_KEY = "mocked_component_name_string";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DreamBackend mBackend;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;
    @Mock
    private UserManager mUserManager;
    private CurrentDreamPicker mPicker;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mActivity.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        FakeFeatureFactory.setupForTest();

        mPicker = new CurrentDreamPicker();
        mPicker.onAttach(mActivity);

        ReflectionHelpers.setField(mPicker, "mBackend", mBackend);
    }

    @Test
    public void getDefaultShouldReturnActiveDream() {
        ComponentName mockComponentName = mock(ComponentName.class);
        when(mockComponentName.flattenToString()).thenReturn(COMPONENT_KEY);
        when(mBackend.getActiveDream()).thenReturn(mockComponentName);

        assertThat(mPicker.getDefaultKey()).isEqualTo(COMPONENT_KEY);
    }

    @Test
    public void setDefaultShouldUpdateActiveDream() {
        DreamInfo mockInfo = mock(DreamInfo.class);
        ComponentName mockName = mock(ComponentName.class);

        mockInfo.componentName = mockName;
        when(mockName.flattenToString()).thenReturn(COMPONENT_KEY);
        when(mBackend.getDreamInfos()).thenReturn(Collections.singletonList(mockInfo));

        mPicker.setDefaultKey(COMPONENT_KEY);

        verify(mBackend).setActiveDream(mockName);
    }
}
