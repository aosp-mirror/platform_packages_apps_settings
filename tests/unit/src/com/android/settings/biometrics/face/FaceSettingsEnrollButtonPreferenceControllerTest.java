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

package com.android.settings.biometrics.face;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.widget.Button;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class FaceSettingsEnrollButtonPreferenceControllerTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private Context mContext;
    @Mock
    private Button mButton;
    @Mock
    private FaceSettingsEnrollButtonPreferenceController.Listener mListener;

    private FaceSettingsEnrollButtonPreferenceController mController;

    @Before
    public void setUp() {
        mController = new FaceSettingsEnrollButtonPreferenceController(mContext);
        mController.setListener(mListener);
    }

    @Test
    public void isSliceable_returnFalse() {
        assertThat(mController.isSliceable()).isFalse();
    }

    @Test
    public void testOnClick() {
        mController.onClick(mButton);

        assertThat(mController.isClicked()).isTrue();
        verify(mListener).onStartEnrolling(any());
    }
}
