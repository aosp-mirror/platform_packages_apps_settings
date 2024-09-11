/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.datetime;

import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TimeFeedbackPreferenceControllerTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = spy(Robolectric.setupActivity(Activity.class));
    }

    @Test
    public void emptyIntentUri_controllerNotAvailable() {
        String emptyIntentUri = "";
        TimeFeedbackPreferenceController controller =
                new TimeFeedbackPreferenceController(mContext, "test_key", emptyIntentUri);
        assertThat(controller.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void clickPreference() {
        Preference preference = new Preference(mContext);

        String intentUri =
                "intent:#Intent;"
                        + "action=com.android.settings.test.LAUNCH_USER_FEEDBACK;"
                        + "package=com.android.settings.test.target;"
                        + "end";
        TimeFeedbackPreferenceController controller =
                new TimeFeedbackPreferenceController(mContext, "test_key", intentUri);

        // Click a preference that's not controlled by this controller.
        preference.setKey("fake_key");
        assertThat(controller.handlePreferenceTreeClick(preference)).isFalse();

        // Check for startActivity() call.
        verify(mContext, never()).startActivity(any());

        // Click a preference controlled by this controller.
        preference.setKey(controller.getPreferenceKey());
        assertThat(controller.handlePreferenceTreeClick(preference)).isTrue();

        // Check for startActivity() call.
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(intentCaptor.capture());
        Intent actualIntent = intentCaptor.getValue();
        assertThat(actualIntent.getAction()).isEqualTo(
                "com.android.settings.test.LAUNCH_USER_FEEDBACK");
        assertThat(actualIntent.getPackage()).isEqualTo("com.android.settings.test.target");
    }
}
