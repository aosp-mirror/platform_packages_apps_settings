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

import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.CaptioningManager;

import com.android.internal.widget.SubtitleView;
import com.android.settings.R;
import com.android.settingslib.accessibility.AccessibilityUtils;

import com.google.common.annotations.VisibleForTesting;

import java.util.Locale;

/**
 * Helper class for caption.
 */
public class CaptionHelper {

    /* WebVtt specifies line height as 5.3% of the viewport height. */
    @VisibleForTesting
    static final float LINE_HEIGHT_RATIO = 0.0533f;

    private final Context mContext;
    private final CaptioningManager mCaptioningManager;

    public CaptionHelper(Context context) {
        mContext = context;
        mCaptioningManager = context.getSystemService(CaptioningManager.class);
    }

    /**
     * Sets the user's preferred captioning enabled state.
     *
     * @param enabled Whether to enable or disable captioning manager.
     */
    public void setEnabled(boolean enabled) {
        if (isEnabled() == enabled) {
            return;
        }

        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, enabled ? ON : OFF);
    }

    /**
     * Gets if the captioning manager is enabled.
     *
     * @return True if the captioning manager is enabled, false otherwise.
     */
    public boolean isEnabled() {
        return mCaptioningManager.isEnabled();
    }

    /**
     * Updates font style of captioning properties for preview screen.
     *
     * @param previewText preview text
     * @param previewWindow preview window
     * @param styleId font style id
     */
    public void applyCaptionProperties(SubtitleView previewText, View previewWindow,
            int styleId) {
        previewText.setStyle(styleId);

        final float fontScale = mCaptioningManager.getFontScale();
        if (previewWindow != null) {
            // Assume the viewport is clipped with a 16:9 aspect ratio.
            final float virtualHeight = Math.max(9 * previewWindow.getWidth(),
                    16 * previewWindow.getHeight()) / 16.0f;
            previewText.setTextSize(virtualHeight * LINE_HEIGHT_RATIO * fontScale);
        } else {
            final float textSize = mContext.getResources().getDimension(
                    R.dimen.caption_preview_text_size);
            previewText.setTextSize(textSize * fontScale);
        }

        final Locale locale = mCaptioningManager.getLocale();
        if (locale != null) {
            final CharSequence localizedText = AccessibilityUtils.getTextForLocale(
                    mContext, locale, R.string.captioning_preview_characters);
            previewText.setText(localizedText);
        } else {
            previewText.setText(R.string.captioning_preview_characters);
        }
    }
}
