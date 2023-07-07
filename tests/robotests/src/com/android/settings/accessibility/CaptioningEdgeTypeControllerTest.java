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
import android.util.AttributeSet;
import android.view.accessibility.CaptioningManager;
import android.view.accessibility.CaptioningManager.CaptionStyle;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

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

/** Tests for {@link CaptioningEdgeTypeController}. */
@RunWith(RobolectricTestRunner.class)
public class CaptioningEdgeTypeControllerTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private PreferenceScreen mScreen;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private CaptioningEdgeTypeController mController;
    private EdgeTypePreference mPreference;
    private ShadowCaptioningManager mShadowCaptioningManager;

    @Before
    public void setUp() {
        mController = new CaptioningEdgeTypeController(mContext, "captioning_edge_type");
        final AttributeSet attributeSet = Robolectric.buildAttributeSet().build();
        mPreference = new EdgeTypePreference(mContext, attributeSet);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        CaptioningManager captioningManager = mContext.getSystemService(CaptioningManager.class);
        mShadowCaptioningManager = Shadow.extract(captioningManager);
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getSummary_defaultValue_shouldReturnNone() {
        mController.displayPreference(mScreen);

        assertThat(mPreference.getSummary().toString()).isEqualTo("None");
    }

    @Test
    public void getSummary_outlineValue_shouldReturnOutline() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_TYPE, CaptionStyle.EDGE_TYPE_OUTLINE);

        mController.displayPreference(mScreen);

        assertThat(mPreference.getSummary().toString()).isEqualTo("Outline");
    }

    @Test
    public void setOutlineValue_shouldReturnOutline() {
        mController.displayPreference(mScreen);

        mPreference.setValue(CaptionStyle.EDGE_TYPE_OUTLINE);

        assertThat(mPreference.getSummary().toString()).isEqualTo("Outline");
    }

    @Test
    public void onValueChanged_shouldSetCaptionEnabled() {
        mShadowCaptioningManager.setEnabled(false);
        mController.displayPreference(mScreen);

        mController.onValueChanged(mPreference, CaptionStyle.EDGE_TYPE_OUTLINE);

        final boolean isCaptionEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, OFF) == ON;
        assertThat(isCaptionEnabled).isTrue();
    }
}
