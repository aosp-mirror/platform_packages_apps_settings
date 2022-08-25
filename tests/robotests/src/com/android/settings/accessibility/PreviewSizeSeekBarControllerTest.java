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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowInteractionJankMonitor;
import com.android.settings.widget.LabeledSeekBarPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Tests for {@link PreviewSizeSeekBarController}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowInteractionJankMonitor.class})
public class PreviewSizeSeekBarControllerTest {
    private static final String FONT_SIZE_KEY = "font_size";
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private PreviewSizeSeekBarController mSeekBarController;
    private FontSizeData mFontSizeData;
    private LabeledSeekBarPreference mSeekBarPreference;

    @Mock
    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFontSizeData = new FontSizeData(mContext);

        mSeekBarController =
                new PreviewSizeSeekBarController(mContext, FONT_SIZE_KEY, mFontSizeData);

        mSeekBarPreference = spy(new LabeledSeekBarPreference(mContext, /* attrs= */ null));
        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mSeekBarPreference);
    }

    @Test
    public void initMax_matchResult() {
        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mSeekBarPreference);

        mSeekBarController.displayPreference(mPreferenceScreen);

        assertThat(mSeekBarPreference.getMax()).isEqualTo(
                mFontSizeData.getValues().size() - 1);
    }

    @Test
    public void initProgress_matchResult() {
        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mSeekBarPreference);

        mSeekBarController.displayPreference(mPreferenceScreen);

        verify(mSeekBarPreference).setProgress(mFontSizeData.getInitialIndex());
    }

    @Test
    public void resetToDefaultState_matchResult() {
        final int defaultProgress =
                mFontSizeData.getValues().indexOf(mFontSizeData.getDefaultValue());
        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mSeekBarPreference);

        mSeekBarController.displayPreference(mPreferenceScreen);
        mSeekBarPreference.setProgress(defaultProgress + 1);
        mSeekBarController.resetState();

        assertThat(mSeekBarPreference.getProgress()).isEqualTo(defaultProgress);
    }
}
