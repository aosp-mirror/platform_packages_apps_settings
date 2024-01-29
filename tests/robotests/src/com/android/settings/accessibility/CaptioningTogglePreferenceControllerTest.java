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
import android.provider.Settings;
import android.view.accessibility.CaptioningManager;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.widget.SettingsMainSwitchPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCaptioningManager;

/** Tests for {@link CaptioningTogglePreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class CaptioningTogglePreferenceControllerTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private PreferenceScreen mScreen;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private CaptioningTogglePreferenceController mController;
    private SettingsMainSwitchPreference mSwitchPreference;
    private ShadowCaptioningManager mShadowCaptioningManager;

    @Before
    public void setUp() {
        mController = new CaptioningTogglePreferenceController(mContext,
                "captioning_preference_switch");
        mSwitchPreference = new SettingsMainSwitchPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mSwitchPreference);
        CaptioningManager captioningManager = mContext.getSystemService(CaptioningManager.class);
        mShadowCaptioningManager = Shadow.extract(captioningManager);
    }

    @After
    public void tearDown() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, OFF);
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void displayPreference_captionEnabled_shouldSetChecked() {
        mShadowCaptioningManager.setEnabled(true);

        mController.displayPreference(mScreen);

        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void displayPreference_captionDisabled_shouldSetUnchecked() {
        mShadowCaptioningManager.setEnabled(false);

        mController.displayPreference(mScreen);

        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void performClick_captionEnabled_shouldSetCaptionDisabled() {
        mShadowCaptioningManager.setEnabled(true);
        mController.displayPreference(mScreen);

        mSwitchPreference.performClick();

        assertThat(mSwitchPreference.isChecked()).isFalse();
        assertThat(isCaptionEnabled()).isFalse();
    }

    @Test
    public void performClick_captionDisabled_shouldSetCaptionEnabled() {
        mShadowCaptioningManager.setEnabled(false);
        mController.displayPreference(mScreen);

        mSwitchPreference.performClick();

        assertThat(mSwitchPreference.isChecked()).isTrue();
        assertThat(isCaptionEnabled()).isTrue();
    }

    @Test
    public void setChecked_switchChecked_shouldSetCaptionEnabled() {
        mController.displayPreference(mScreen);

        mController.setChecked(/* isChecked= */ true);

        assertThat(isCaptionEnabled()).isTrue();
    }

    @Test
    public void setChecked_switchUnchecked_shouldSetCaptionDisabled() {
        mController.displayPreference(mScreen);

        mController.setChecked(/* isChecked= */ false);

        assertThat(isCaptionEnabled()).isFalse();
    }

    @Test
    public void onSwitchChanged_switchChecked_shouldSetCaptionEnabled() {
        mController.displayPreference(mScreen);

        mController.onCheckedChanged(/* buttonView= */ null, /* isChecked= */ true);

        assertThat(isCaptionEnabled()).isTrue();
    }

    @Test
    public void onSwitchChanged_switchUnchecked_shouldSetCaptionDisabled() {
        mController.displayPreference(mScreen);

        mController.onCheckedChanged(/* buttonView= */ null, /* isChecked= */ false);

        assertThat(isCaptionEnabled()).isFalse();
    }

    private boolean isCaptionEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, OFF) == ON;
    }
}
