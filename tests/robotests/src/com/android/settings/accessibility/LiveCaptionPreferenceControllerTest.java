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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class LiveCaptionPreferenceControllerTest {

    private Context mContext;
    private LiveCaptionPreferenceController mController;
    private Preference mLiveCaptionPreference;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new LiveCaptionPreferenceController(mContext, "test_key");
        mLiveCaptionPreference = new Preference(mContext);
        mLiveCaptionPreference.setSummary(R.string.live_caption_summary);
    }

    @Test
    public void getAvailabilityStatus_canResolveIntent_shouldReturnAvailable() {
        final ShadowPackageManager pm = Shadows.shadowOf(mContext.getPackageManager());
        pm.addResolveInfoForIntent(LiveCaptionPreferenceController.LIVE_CAPTION_INTENT,
                new ResolveInfo());

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noResolveIntent_shouldReturnUnavailable() {
        final ShadowPackageManager pm = Shadows.shadowOf(mContext.getPackageManager());
        pm.setResolveInfosForIntent(LiveCaptionPreferenceController.LIVE_CAPTION_INTENT,
                Collections.emptyList());

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void updateState_liveCaptionEnabled_subtextShowsOnSummary() {
        setLiveCaptionEnabled(true);

        mController.updateState(mLiveCaptionPreference);

        assertThat(mLiveCaptionPreference.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.live_caption_summary)
        );
    }

    @Test
    public void updateState_liveCaptionDisabled_subtextShowsOffSummary() {
        setLiveCaptionEnabled(false);

        mController.updateState(mLiveCaptionPreference);

        assertThat(mLiveCaptionPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.live_caption_summary)
        );
    }

    private void setLiveCaptionEnabled(boolean enabled) {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.ODI_CAPTIONS_ENABLED,
                enabled ? AccessibilityUtil.State.ON: AccessibilityUtil.State.OFF);
    }
}