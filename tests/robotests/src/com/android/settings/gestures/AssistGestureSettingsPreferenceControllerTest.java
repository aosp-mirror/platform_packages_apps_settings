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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.provider.Settings.Secure;

import com.android.settings.TestConfig;
import com.android.settings.search.InlinePayload;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowSecureSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
    manifest = TestConfig.MANIFEST_PATH,
    sdk = TestConfig.SDK_VERSION,
    shadows = ShadowSecureSettings.class
)
public class AssistGestureSettingsPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private FakeFeatureFactory mFactory;
    private AssistGestureSettingsPreferenceController mController;

    private static final String KEY_ASSIST = "gesture_assist";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mController = new AssistGestureSettingsPreferenceController(mContext, null, KEY_ASSIST,
                false);
    }

    @Test
    public void isAvailable_whenSupported_shouldReturnTrue() {
        mController.mAssistOnly = false;
        when(mFactory.assistGestureFeatureProvider.isSensorAvailable(mContext)).thenReturn(true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_whenUnsupported_shouldReturnFalse() {
        when(mFactory.assistGestureFeatureProvider.isSupported(mContext)).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testPreferenceController_ProperResultPayloadType() {
        final Context context = RuntimeEnvironment.application;
        AssistGestureSettingsPreferenceController controller =
                new AssistGestureSettingsPreferenceController(context, null /* lifecycle */,
                        KEY_ASSIST, false /* assistOnly */);
        ResultPayload payload = controller.getResultPayload();
        assertThat(payload).isInstanceOf(InlineSwitchPayload.class);
    }

    @Test
    public void testSetValue_updatesCorrectly() {
        int newValue = 1;
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putInt(resolver, Secure.ASSIST_GESTURE_ENABLED, 0);

        ((InlinePayload) mController.getResultPayload()).setValue(mContext, newValue);
        int updatedValue = Settings.Secure.getInt(resolver, Secure.ASSIST_GESTURE_ENABLED, -1);

        assertThat(updatedValue).isEqualTo(newValue);
    }

    @Test
    public void testGetValue_correctValueReturned() {
        int currentValue = 1;
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putInt(resolver, Settings.Secure.ASSIST_GESTURE_ENABLED, currentValue);

        int newValue = ((InlinePayload) mController.getResultPayload()).getValue(mContext);

        assertThat(newValue).isEqualTo(currentValue);
    }
}

