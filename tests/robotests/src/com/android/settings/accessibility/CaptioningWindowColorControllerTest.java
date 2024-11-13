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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.accessibility.CaptioningManager;
import android.view.accessibility.CaptioningManager.CaptionStyle;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCaptioningManager;

/** Tests for {@link CaptioningWindowColorController}. */
@RunWith(RobolectricTestRunner.class)
public class CaptioningWindowColorControllerTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock
    private PreferenceScreen mScreen;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private CaptioningWindowColorController mController;
    private ColorPreference mPreference;
    private ShadowCaptioningManager mShadowCaptioningManager;
    private CaptionHelper mCaptionHelper;

    @Before
    public void setUp() {
        mCaptionHelper = new CaptionHelper(mContext);
        mController = new CaptioningWindowColorController(
                mContext, "captioning_window_color", mCaptionHelper);
        final AttributeSet attributeSet = Robolectric.buildAttributeSet().build();
        mPreference = new ColorPreference(mContext, attributeSet);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        CaptioningManager captioningManager = mContext.getSystemService(CaptioningManager.class);
        mShadowCaptioningManager = Shadow.extract(captioningManager);
    }

    @Test
    @DisableFlags(Flags.FLAG_FIX_A11Y_SETTINGS_SEARCH)
    public void getAvailabilityStatus_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_FIX_A11Y_SETTINGS_SEARCH)
    public void getAvailabilityStatus_customCaption_shouldReturnAvailable() {
        mCaptionHelper.setRawUserStyle(CaptionStyle.PRESET_CUSTOM);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_FIX_A11Y_SETTINGS_SEARCH)
    public void getAvailabilityStatus_noCustom_shouldReturnUnsearchable() {
        mCaptionHelper.setRawUserStyle(0);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getSummary_defaultValue_shouldReturnNone() {
        mController.displayPreference(mScreen);

        assertThat(mPreference.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.color_none));
    }

    @Test
    public void getSummary_redValue_shouldReturnRed() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_WINDOW_COLOR, 0xFFFF0000);

        mController.displayPreference(mScreen);

        assertThat(mPreference.getSummary().toString()).isEqualTo("Red");
    }

    @Test
    public void setRedValue_shouldReturnRed() {
        mController.displayPreference(mScreen);

        mPreference.setValue(0xFFFF0000);

        assertThat(mPreference.getSummary().toString()).isEqualTo("Red");
    }

    @Test
    public void setNoneColorValue_shouldNotHaveColor() {
        final CaptionHelper captionHelper = new CaptionHelper(mContext);
        captionHelper.setWindowColor(0xFFFF0000);
        mController.displayPreference(mScreen);

        mPreference.setValue(0x00FFFFFF);

        assertThat(CaptionStyle.hasColor(captionHelper.getWindowColor())).isFalse();
    }

    @Test
    public void setRedValueFromNoneValue_halfOpacityRedColor_shouldReturnExpectedColor() {
        final CaptionHelper captionHelper = new CaptionHelper(mContext);
        captionHelper.setWindowColor(0x80FF0000);
        mController.displayPreference(mScreen);
        mPreference.setValue(0x00FFFFFF);

        mPreference.setValue(0xFFFF0000);

        assertThat(captionHelper.getWindowColor()).isEqualTo(0x80FF0000);
    }

    @Test
    public void onValueChanged_shouldSetCaptionEnabled() {
        Settings.Secure.putInt(
            mContext.getContentResolver(), Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, OFF);
        mController.displayPreference(mScreen);

        mController.onValueChanged(mPreference, 0xFFFF0000);

        final boolean isCaptionEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, OFF) == ON;
        assertThat(isCaptionEnabled).isTrue();
    }
}
