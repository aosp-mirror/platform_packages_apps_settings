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

package com.android.settings.gestures;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class GesturesSettingsPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;

    private GesturesSettingPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(mock(DevicePolicyManager.class)).when(mActivity)
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        FakeFeatureFactory.setupForTest();
        mController = new GesturesSettingPreferenceController(mActivity, "test_key");
    }

    @Test
    public void isAvailable_hasGesture_shouldReturnTrue() {
        final List<AbstractPreferenceController> mControllers = new ArrayList<>();
        mControllers.add(new AbstractPreferenceController(RuntimeEnvironment.application) {
            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public String getPreferenceKey() {
                return "test_key";
            }
        });
        ReflectionHelpers.setField(mController, "mGestureControllers", mControllers);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void isAvailable_noGesture_shouldReturnFalse() {
        ReflectionHelpers.setField(mController, "mGestureControllers",
                new ArrayList<AbstractPreferenceController>());

        assertThat(mController.isAvailable()).isFalse();
    }
}
