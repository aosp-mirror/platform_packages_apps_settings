/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link CaptioningPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class CaptioningPreferenceControllerTest {

    private Context mContext;
    private CaptioningPreferenceController mController;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new CaptioningPreferenceController(mContext, "captioning_pref");
    }

    @Test
    public void getAvailabilityStatus_byDefault_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getSummary_enabledCaptions_shouldReturnOnSummary() {
        setCaptioningEnabled(true);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getText(R.string.show_captions_enabled));
    }

    @Test
    public void getSummary_disabledCaptions_shouldReturnOffSummary() {
        setCaptioningEnabled(false);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getText(R.string.show_captions_disabled));
    }

    private void setCaptioningEnabled(boolean enabled) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, enabled ? ON : OFF);
    }
}
