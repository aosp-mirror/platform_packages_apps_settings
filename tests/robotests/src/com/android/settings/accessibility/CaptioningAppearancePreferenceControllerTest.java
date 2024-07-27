/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.CaptioningManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCaptioningManager;

/** Tests for {@link CaptioningAppearancePreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class CaptioningAppearancePreferenceControllerTest {

    private static final String TEST_KEY = "test_key";
    private static final int DEFAULT_PRESET_INDEX = 1;
    private static final int DEFAULT_FONT_SCALE_INDEX = 2;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private CaptioningAppearancePreferenceController mController;
    private ShadowCaptioningManager mShadowCaptioningManager;

    @Before
    public void setUp() {
        CaptioningManager captioningManager = mContext.getSystemService(CaptioningManager.class);
        mShadowCaptioningManager = Shadow.extract(captioningManager);
        mController = new CaptioningAppearancePreferenceController(mContext, TEST_KEY);
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getSummary_noScale_shouldReturnDefaultSummary() {
        final String expectedSummary =
                getSummaryCombo(DEFAULT_FONT_SCALE_INDEX, DEFAULT_PRESET_INDEX);
        assertThat(mController.getSummary().toString()).isEqualTo(expectedSummary);
    }

    @Test
    public void getSummary_smallestScale_shouldReturnExpectedSummary() {
        Settings.Secure.putFloat(mContext.getContentResolver(),
            Settings.Secure.ACCESSIBILITY_CAPTIONING_FONT_SCALE, 0.25f);
        shadowOf(Looper.getMainLooper()).idle();

        final String expectedSummary =
                getSummaryCombo(/* fontScaleIndex= */ 0, DEFAULT_PRESET_INDEX);
        assertThat(mController.getSummary().toString()).isEqualTo(expectedSummary);
    }

    @Test
    public void getSummary_smallScale_shouldReturnExpectedSummary() {
        Settings.Secure.putFloat(mContext.getContentResolver(),
            Settings.Secure.ACCESSIBILITY_CAPTIONING_FONT_SCALE, 0.5f);
        shadowOf(Looper.getMainLooper()).idle();

        final String expectedSummary =
                getSummaryCombo(/* fontScaleIndex= */ 1, DEFAULT_PRESET_INDEX);
        assertThat(mController.getSummary().toString()).isEqualTo(expectedSummary);
    }

    @Test
    public void getSummary_mediumScale_shouldReturnExpectedSummary() {
        Settings.Secure.putFloat(mContext.getContentResolver(),
            Settings.Secure.ACCESSIBILITY_CAPTIONING_FONT_SCALE, 1.0f);
        shadowOf(Looper.getMainLooper()).idle();

        final String expectedSummary =
                getSummaryCombo(/* fontScaleIndex= */ 2, DEFAULT_PRESET_INDEX);
        assertThat(mController.getSummary().toString()).isEqualTo(expectedSummary);
    }

    @Test
    public void getSummary_largeScale_shouldReturnExpectedSummary() {
        Settings.Secure.putFloat(mContext.getContentResolver(),
            Settings.Secure.ACCESSIBILITY_CAPTIONING_FONT_SCALE, 1.5f);
        shadowOf(Looper.getMainLooper()).idle();

        final String expectedSummary =
                getSummaryCombo(/* fontScaleIndex= */ 3, DEFAULT_PRESET_INDEX);
        assertThat(mController.getSummary().toString()).isEqualTo(expectedSummary);
    }

    @Test
    public void getSummary_largestScale_shouldReturnExpectedSummary() {
        Settings.Secure.putFloat(mContext.getContentResolver(),
            Settings.Secure.ACCESSIBILITY_CAPTIONING_FONT_SCALE, 2.0f);
        shadowOf(Looper.getMainLooper()).idle();

        final String expectedSummary =
                getSummaryCombo(/* fontScaleIndex= */ 4, DEFAULT_PRESET_INDEX);
        assertThat(mController.getSummary().toString()).isEqualTo(expectedSummary);
    }

    @Test
    public void getSummary_setByAppPreset_shouldReturnExpectedSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET, 4);

        final String expectedSummary =
                getSummaryCombo(DEFAULT_FONT_SCALE_INDEX, /* presetIndex= */ 0);
        assertThat(mController.getSummary().toString()).isEqualTo(expectedSummary);
    }

    @Test
    public void getSummary_whiteOnBlackPreset_shouldReturnExpectedSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET, 0);

        final String expectedSummary =
                getSummaryCombo(DEFAULT_FONT_SCALE_INDEX, /* presetIndex= */ 1);
        assertThat(mController.getSummary().toString()).isEqualTo(expectedSummary);
    }

    @Test
    public void getSummary_blackOnWhitePreset_shouldReturnExpectedSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET, 1);

        final String expectedSummary =
                getSummaryCombo(DEFAULT_FONT_SCALE_INDEX, /* presetIndex= */ 2);
        assertThat(mController.getSummary().toString()).isEqualTo(expectedSummary);
    }

    @Test
    public void getSummary_yellowOnBlackPreset_shouldReturnExpectedSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET, 2);

        final String expectedSummary =
                getSummaryCombo(DEFAULT_FONT_SCALE_INDEX, /* presetIndex= */ 3);
        assertThat(mController.getSummary().toString()).isEqualTo(expectedSummary);
    }

    @Test
    public void getSummary_yellowOnBluePreset_shouldReturnExpectedSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET, 3);

        final String expectedSummary =
                getSummaryCombo(DEFAULT_FONT_SCALE_INDEX, /* presetIndex= */ 4);
        assertThat(mController.getSummary().toString()).isEqualTo(expectedSummary);
    }

    @Test
    public void getSummary_customPreset_shouldReturnExpectedSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET, -1);

        final String expectedSummary =
                getSummaryCombo(DEFAULT_FONT_SCALE_INDEX, /* presetIndex= */ 5);
        assertThat(mController.getSummary().toString()).isEqualTo(expectedSummary);
    }

    private String getSummaryCombo(int fontScaleIndex, int presetIndex) {
        final String[] fontScaleArray = mContext.getResources().getStringArray(
                R.array.captioning_font_size_selector_titles);
        final String[] presetArray = mContext.getResources().getStringArray(
                R.array.captioning_preset_selector_titles);
        return mContext.getString(
                com.android.settingslib.R.string.preference_summary_default_combination,
                fontScaleArray[fontScaleIndex], presetArray[presetIndex]);
    }
}
