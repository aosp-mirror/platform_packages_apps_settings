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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.CaptioningManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.internal.widget.SubtitleView;
import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.Locale;

/** Tests for {@link CaptionHelper}. */
@RunWith(RobolectricTestRunner.class)
public class CaptionHelperTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private CaptioningManager mCaptioningManager;
    @Mock
    private SubtitleView mSubtitleView;
    @Mock
    private View mPreviewWindow;
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private CaptionHelper mCaptionHelper;

    @Before
    public void setUp() {
        when(mContext.getSystemService(CaptioningManager.class)).thenReturn(mCaptioningManager);
        mCaptionHelper = new CaptionHelper(mContext);
    }

    @Test
    public void applyCaptionProperties_verifyAction() {
        final float fontScale = 1.0f;
        when(mCaptioningManager.getFontScale()).thenReturn(fontScale);
        final int windowSize = 100;
        when(mPreviewWindow.getWidth()).thenReturn(windowSize);
        when(mPreviewWindow.getHeight()).thenReturn(windowSize);
        final float textSize = CaptionHelper.LINE_HEIGHT_RATIO * windowSize * fontScale;

        mCaptionHelper.applyCaptionProperties(mSubtitleView, mPreviewWindow, /* styleId= */ 0);

        verify(mSubtitleView).setTextSize(textSize);
        verify(mSubtitleView).setText(R.string.captioning_preview_characters);
    }

    @Test
    public void applyCaptionProperties_withoutPreviewWindow_verifyAction() {
        final float fontScale = 1.0f;
        when(mCaptioningManager.getFontScale()).thenReturn(fontScale);
        final float textSize = mContext.getResources().getDimension(
                R.dimen.caption_preview_text_size) * fontScale;

        mCaptionHelper.applyCaptionProperties(mSubtitleView, /* PreviewWindow= */ null,
                /* styleId= */ 0);

        verify(mSubtitleView).setTextSize(textSize);
        verify(mSubtitleView).setText(R.string.captioning_preview_characters);
    }

    @Test
    public void applyCaptionProperties_localeUS_verifyAction() {
        when(mCaptioningManager.getLocale()).thenReturn(Locale.US);
        final String text = mContext.getString(R.string.captioning_preview_characters);

        mCaptionHelper.applyCaptionProperties(mSubtitleView, /* PreviewWindow= */ null,
                /* styleId= */ 0);

        verify(mSubtitleView).setText(text);
    }

    @Test
    public void enableCaptioningManager_shouldSetCaptionEnabled() {
        when(mCaptioningManager.isEnabled()).thenReturn(false);

        mCaptionHelper.setEnabled(true);

        final boolean isCaptionEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, OFF) == ON;
        assertThat(isCaptionEnabled).isTrue();
    }

    @Test
    public void disableCaptioningManager_shouldSetCaptionDisabled() {
        when(mCaptioningManager.isEnabled()).thenReturn(true);

        mCaptionHelper.setEnabled(false);

        final boolean isCaptionEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, OFF) == ON;
        assertThat(isCaptionEnabled).isFalse();
    }
}
