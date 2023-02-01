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

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.CaptioningManager;
import android.view.accessibility.CaptioningManager.CaptionStyle;

import com.android.internal.widget.SubtitleView;
import com.android.settings.R;
import com.android.settingslib.accessibility.AccessibilityUtils;

import com.google.common.annotations.VisibleForTesting;

import java.util.Locale;

/** Helper class for caption. */
public class CaptionHelper {

    /* WebVtt specifies line height as 5.3% of the viewport height. */
    @VisibleForTesting
    static final float LINE_HEIGHT_RATIO = 0.0533f;

    private final Context mContext;
    private final ContentResolver mContentResolver;
    private final CaptioningManager mCaptioningManager;

    public CaptionHelper(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
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
                    R.dimen.captioning_preview_text_size);
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

    /**
     * Sets the user's preferred captioning background color.
     *
     * @param color The captioning background color
     */
    public void setBackgroundColor(int color) {
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_CAPTIONING_BACKGROUND_COLOR, color);
    }

    /** Returns the captioning background color.*/
    public int getBackgroundColor() {
        final CaptionStyle attrs = CaptionStyle.getCustomStyle(mContentResolver);
        return attrs.hasBackgroundColor() ? attrs.backgroundColor : CaptionStyle.COLOR_UNSPECIFIED;
    }

    /**
     * Sets the user's preferred captioning foreground color.
     *
     * @param color The captioning foreground color
     */
    public void setForegroundColor(int color) {
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR, color);
    }

    /** Returns the captioning foreground color.*/
    public int getForegroundColor() {
        final CaptionStyle attrs = CaptionStyle.getCustomStyle(mContentResolver);
        return attrs.hasForegroundColor() ? attrs.foregroundColor : CaptionStyle.COLOR_UNSPECIFIED;
    }

    /**
     * Sets the user's preferred captioning window color.
     *
     * @param color The captioning window color
     */
    public void setWindowColor(int color) {
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_CAPTIONING_WINDOW_COLOR, color);
    }

    /** Returns the captioning window color.*/
    public int getWindowColor() {
        final CaptionStyle attrs = CaptionStyle.getCustomStyle(mContentResolver);
        return attrs.hasWindowColor() ? attrs.windowColor : CaptionStyle.COLOR_UNSPECIFIED;
    }

    /**
     * Sets the user's preferred captioning edge color.
     *
     * @param color The captioning edge color
     */
    public void setEdgeColor(int color) {
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_COLOR, color);
    }

    /** Returns the captioning edge color.*/
    public int getEdgeColor() {
        final CaptionStyle attrs = CaptionStyle.getCustomStyle(mContentResolver);
        return attrs.edgeColor;
    }

    /**
     * Sets the user's preferred captioning edge type.
     *
     * @param type The captioning edge type
     */
    public void setEdgeType(int type) {
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_TYPE, type);
    }

    /** Returns the captioning edge type.*/
    public int getEdgeType() {
        final CaptionStyle attrs = CaptionStyle.getCustomStyle(mContentResolver);
        return attrs.edgeType;
    }

    /**
     * Sets the captioning raw user style.
     *
     * @param type The captioning raw user style
     */
    public void setRawUserStyle(int type) {
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET, type);
    }

    /** Returns the captioning raw preset number.*/
    public int getRawUserStyle() {
        return mCaptioningManager.getRawUserStyle();
    }

    /** Returns the captioning visual properties.*/
    public CaptionStyle getUserStyle() {
        return mCaptioningManager.getUserStyle();
    }

    /** Returns the captioning locale language.*/
    public Locale getLocale() {
        return mCaptioningManager.getLocale();
    }
}
