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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowSecureSettings;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class GesturesSettingsPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;
    @Mock
    private Preference mPreference;

    private GesturesSettingPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
        mController = new GesturesSettingPreferenceController(mActivity);
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

    @Test
    @Config(shadows = ShadowSecureSettings.class)
    public void updateState_assistSupported_shouldSetToAssistGestureStatus() {
        final FakeFeatureFactory featureFactory =
                (FakeFeatureFactory) FakeFeatureFactory.getFactory(mActivity);
        when(featureFactory.assistGestureFeatureProvider.isSupported(any(Context.class)))
                .thenReturn(true);
        when(featureFactory.assistGestureFeatureProvider.isSensorAvailable(any(Context.class)))
                .thenReturn(true);

        final ContentResolver cr = mActivity.getContentResolver();
        Settings.Secure.putInt(cr, Settings.Secure.ASSIST_GESTURE_ENABLED, 0);
        Settings.Secure.putInt(cr, Settings.Secure.ASSIST_GESTURE_SILENCE_ALERTS_ENABLED, 0);
        mController.updateState(mPreference);
        verify(mActivity).getText(R.string.language_input_gesture_summary_off);

        Settings.Secure.putInt(cr, Settings.Secure.ASSIST_GESTURE_ENABLED, 1);
        Settings.Secure.putInt(cr, Settings.Secure.ASSIST_GESTURE_SILENCE_ALERTS_ENABLED, 0);
        mController.updateState(mPreference);
        verify(mActivity).getText(R.string.language_input_gesture_summary_on_with_assist);

        Settings.Secure.putInt(cr, Settings.Secure.ASSIST_GESTURE_ENABLED, 0);
        Settings.Secure.putInt(cr, Settings.Secure.ASSIST_GESTURE_SILENCE_ALERTS_ENABLED, 1);
        mController.updateState(mPreference);
        verify(mActivity).getText(R.string.language_input_gesture_summary_on_non_assist);
    }

    @Test
    @Config(shadows = ShadowSecureSettings.class)
    public void updateState_sensorNotAvailable_shouldSetToEmptyStatus() {
        final FakeFeatureFactory featureFactory =
                (FakeFeatureFactory) FakeFeatureFactory.getFactory(mActivity);
        when(featureFactory.assistGestureFeatureProvider.isSensorAvailable(any(Context.class)))
                .thenReturn(false);

        mController.updateState(mPreference);
        verify(mPreference).setSummary("");
    }
}
