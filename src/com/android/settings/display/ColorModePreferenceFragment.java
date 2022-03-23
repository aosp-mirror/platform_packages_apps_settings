/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import static android.hardware.display.ColorDisplayManager.COLOR_MODE_AUTOMATIC;
import static android.hardware.display.ColorDisplayManager.COLOR_MODE_BOOSTED;
import static android.hardware.display.ColorDisplayManager.COLOR_MODE_NATURAL;
import static android.hardware.display.ColorDisplayManager.COLOR_MODE_SATURATED;
import static android.hardware.display.ColorDisplayManager.VENDOR_COLOR_MODE_RANGE_MAX;
import static android.hardware.display.ColorDisplayManager.VENDOR_COLOR_MODE_RANGE_MIN;

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.hardware.display.ColorDisplayManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings.Secure;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.LayoutPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
@SearchIndexable
public class ColorModePreferenceFragment extends RadioButtonPickerFragment {

    private static final String KEY_COLOR_MODE_PREFIX = "color_mode_";

    private static final int COLOR_MODE_FALLBACK = COLOR_MODE_NATURAL;

    private ContentObserver mContentObserver;
    private ColorDisplayManager mColorDisplayManager;
    private Resources mResources;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mColorDisplayManager = context.getSystemService(ColorDisplayManager.class);
        mResources = context.getResources();

        final ContentResolver cr = context.getContentResolver();
        mContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                if (ColorDisplayManager.areAccessibilityTransformsEnabled(getContext())) {
                    // Color modes are not configurable when Accessibility transforms are enabled.
                    // Close this fragment in that case.
                    getActivity().finish();
                }
            }
        };
        cr.registerContentObserver(
                Secure.getUriFor(Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED),
                false /* notifyForDescendants */, mContentObserver, mUserId);
        cr.registerContentObserver(
                Secure.getUriFor(Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED),
                false /* notifyForDescendants */, mContentObserver, mUserId);
    }

    @Override
    public void onDetach() {
        if (mContentObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(mContentObserver);
            mContentObserver = null;
        }
        super.onDetach();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.color_mode_settings;
    }

    @VisibleForTesting
    void configureAndInstallPreview(LayoutPreference preview, PreferenceScreen screen) {
        preview.setSelectable(false);
        screen.addPreference(preview);
    }

    @Override
    protected void addStaticPreferences(PreferenceScreen screen) {
        final LayoutPreference preview = new LayoutPreference(screen.getContext(),
                R.layout.color_mode_preview);
        configureAndInstallPreview(preview, screen);
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final Map<Integer, String> colorModesToSummaries =
                ColorModeUtils.getColorModeMapping(mResources);
        final List<ColorModeCandidateInfo> candidates = new ArrayList<>();
        for (int colorMode : mResources.getIntArray(
                com.android.internal.R.array.config_availableColorModes)) {
            candidates.add(new ColorModeCandidateInfo(
                    colorModesToSummaries.get(colorMode),
                    getKeyForColorMode(colorMode),
                    true /* enabled */));
        }
        return candidates;
    }

    @Override
    protected String getDefaultKey() {
        final int colorMode = getColorMode();
        if (isValidColorMode(colorMode)) {
            return getKeyForColorMode(colorMode);
        }
        return getKeyForColorMode(COLOR_MODE_FALLBACK);
    }

    @Override
    protected boolean setDefaultKey(String key) {
        int colorMode = Integer.parseInt(key.substring(key.lastIndexOf("_") + 1));
        if (isValidColorMode(colorMode)) {
            setColorMode(colorMode);
        }
        return true;
    }

    /**
     * Wraps ColorDisplayManager#getColorMode for substitution in testing.
     */
    @VisibleForTesting
    public int getColorMode() {
        return mColorDisplayManager.getColorMode();
    }

    /**
     * Wraps ColorDisplayManager#setColorMode for substitution in testing.
     */
    @VisibleForTesting
    public void setColorMode(int colorMode) {
        mColorDisplayManager.setColorMode(colorMode);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.COLOR_MODE_SETTINGS;
    }

    @VisibleForTesting
    String getKeyForColorMode(int colorMode) {
        return KEY_COLOR_MODE_PREFIX + colorMode;
    }

    private boolean isValidColorMode(int colorMode) {
        return colorMode == COLOR_MODE_NATURAL
                || colorMode == COLOR_MODE_BOOSTED
                || colorMode == COLOR_MODE_SATURATED
                || colorMode == COLOR_MODE_AUTOMATIC
                || (colorMode >= VENDOR_COLOR_MODE_RANGE_MIN
                && colorMode <= VENDOR_COLOR_MODE_RANGE_MAX);
    }

    @VisibleForTesting
    static class ColorModeCandidateInfo extends CandidateInfo {
        private final CharSequence mLabel;
        private final String mKey;

        ColorModeCandidateInfo(CharSequence label, String key, boolean enabled) {
            super(enabled);
            mLabel = label;
            mKey = key;
        }

        @Override
        public CharSequence loadLabel() {
            return mLabel;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return mKey;
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.color_mode_settings) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    final int[] availableColorModes = context.getResources().getIntArray(
                            com.android.internal.R.array.config_availableColorModes);
                    return availableColorModes != null && availableColorModes.length > 0
                            && !ColorDisplayManager.areAccessibilityTransformsEnabled(context);
                }
            };
}
