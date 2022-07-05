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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.CaptioningManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.SubtitleView;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.accessibility.AccessibilityUtils;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.LayoutPreference;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Controller that shows the caption locale summary. */
public class CaptionPreviewPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    @VisibleForTesting
    static final List<String> CAPTIONING_FEATURE_KEYS = Arrays.asList(
            Settings.Secure.ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR,
            Settings.Secure.ACCESSIBILITY_CAPTIONING_BACKGROUND_COLOR,
            Settings.Secure.ACCESSIBILITY_CAPTIONING_WINDOW_COLOR,
            Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_COLOR,
            Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET,
            Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_TYPE,
            Settings.Secure.ACCESSIBILITY_CAPTIONING_TYPEFACE,
            Settings.Secure.ACCESSIBILITY_CAPTIONING_FONT_SCALE
    );
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    @VisibleForTesting
    AccessibilitySettingsContentObserver mSettingsContentObserver;
    private CaptioningManager mCaptioningManager;
    private CaptionHelper mCaptionHelper;
    private LayoutPreference mPreference;
    private SubtitleView mPreviewText;
    private View mPreviewWindow;
    private View mPreviewViewport;

    public CaptionPreviewPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mCaptioningManager = context.getSystemService(CaptioningManager.class);
        mCaptionHelper = new CaptionHelper(context);
        mSettingsContentObserver = new AccessibilitySettingsContentObserver(mHandler);
        mSettingsContentObserver.registerKeysToObserverCallback(CAPTIONING_FEATURE_KEYS,
                key -> refreshPreviewText());
    }

    @Override
    public void onStart() {
        mSettingsContentObserver.register(mContext.getContentResolver());
    }

    @Override
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mSettingsContentObserver);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mPreviewText = mPreference.findViewById(R.id.preview_text);
        mPreviewWindow = mPreference.findViewById(R.id.preview_window);
        mPreviewViewport = mPreference.findViewById(R.id.preview_viewport);
        mPreviewViewport.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right,
                    int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if ((oldRight - oldLeft) != (right - left)) {
                    // Remove the listener once the callback is triggered.
                    mPreviewViewport.removeOnLayoutChangeListener(this);
                    mHandler.post(() -> refreshPreviewText());
                }
            }
        });
    }

    private void refreshPreviewText() {
        if (mPreviewText != null) {
            final int styleId = mCaptioningManager.getRawUserStyle();
            mCaptionHelper.applyCaptionProperties(mPreviewText, mPreviewViewport, styleId);

            final Locale locale = mCaptioningManager.getLocale();
            if (locale != null) {
                final CharSequence localizedText = AccessibilityUtils.getTextForLocale(
                        mContext, locale, R.string.captioning_preview_text);
                mPreviewText.setText(localizedText);
            } else {
                mPreviewText.setText(R.string.captioning_preview_text);
            }

            final CaptioningManager.CaptionStyle style = mCaptioningManager.getUserStyle();
            if (style.hasWindowColor()) {
                mPreviewWindow.setBackgroundColor(style.windowColor);
            } else {
                final CaptioningManager.CaptionStyle defStyle =
                        CaptioningManager.CaptionStyle.DEFAULT;
                mPreviewWindow.setBackgroundColor(defStyle.windowColor);
            }
        }
    }
}
