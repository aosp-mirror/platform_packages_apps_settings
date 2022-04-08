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

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
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

@SuppressWarnings("WeakerAccess")
@SearchIndexable
public class ColorModePreferenceFragment extends RadioButtonPickerFragment {

    @VisibleForTesting
    static final String KEY_COLOR_MODE_NATURAL = "color_mode_natural";
    @VisibleForTesting
    static final String KEY_COLOR_MODE_BOOSTED = "color_mode_boosted";
    @VisibleForTesting
    static final String KEY_COLOR_MODE_SATURATED = "color_mode_saturated";
    @VisibleForTesting
    static final String KEY_COLOR_MODE_AUTOMATIC = "color_mode_automatic";

    private ContentObserver mContentObserver;
    private ColorDisplayManager mColorDisplayManager;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mColorDisplayManager = context.getSystemService(ColorDisplayManager.class);

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
        super.onDetach();
        if (mContentObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(mContentObserver);
            mContentObserver = null;
        }
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
        final Context c = getContext();
        final int[] availableColorModes = c.getResources().getIntArray(
                com.android.internal.R.array.config_availableColorModes);

        List<ColorModeCandidateInfo> candidates = new ArrayList<>();
        if (availableColorModes != null) {
            for (int colorMode : availableColorModes) {
                if (colorMode == ColorDisplayManager.COLOR_MODE_NATURAL) {
                    candidates.add(new ColorModeCandidateInfo(
                                c.getText(R.string.color_mode_option_natural),
                                KEY_COLOR_MODE_NATURAL, true /* enabled */));
                } else if (colorMode == ColorDisplayManager.COLOR_MODE_BOOSTED) {
                    candidates.add(new ColorModeCandidateInfo(
                                c.getText(R.string.color_mode_option_boosted),
                                KEY_COLOR_MODE_BOOSTED, true /* enabled */));
                } else if (colorMode == ColorDisplayManager.COLOR_MODE_SATURATED) {
                    candidates.add(new ColorModeCandidateInfo(
                                c.getText(R.string.color_mode_option_saturated),
                                KEY_COLOR_MODE_SATURATED, true /* enabled */));
                } else if (colorMode == ColorDisplayManager.COLOR_MODE_AUTOMATIC) {
                    candidates.add(new ColorModeCandidateInfo(
                                c.getText(R.string.color_mode_option_automatic),
                                KEY_COLOR_MODE_AUTOMATIC, true /* enabled */));
                }
            }
        }
        return candidates;
    }

    @Override
    protected String getDefaultKey() {
        final int colorMode = mColorDisplayManager.getColorMode();
        if (colorMode == ColorDisplayManager.COLOR_MODE_AUTOMATIC) {
            return KEY_COLOR_MODE_AUTOMATIC;
        } else if (colorMode == ColorDisplayManager.COLOR_MODE_SATURATED) {
            return KEY_COLOR_MODE_SATURATED;
        } else if (colorMode == ColorDisplayManager.COLOR_MODE_BOOSTED) {
            return KEY_COLOR_MODE_BOOSTED;
        }
        return KEY_COLOR_MODE_NATURAL;
    }

    @Override
    protected boolean setDefaultKey(String key) {
        switch (key) {
            case KEY_COLOR_MODE_NATURAL:
                mColorDisplayManager.setColorMode(ColorDisplayManager.COLOR_MODE_NATURAL);
                break;
            case KEY_COLOR_MODE_BOOSTED:
                mColorDisplayManager.setColorMode(ColorDisplayManager.COLOR_MODE_BOOSTED);
                break;
            case KEY_COLOR_MODE_SATURATED:
                mColorDisplayManager.setColorMode(ColorDisplayManager.COLOR_MODE_SATURATED);
                break;
            case KEY_COLOR_MODE_AUTOMATIC:
                mColorDisplayManager.setColorMode(ColorDisplayManager.COLOR_MODE_AUTOMATIC);
                break;
        }
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.COLOR_MODE_SETTINGS;
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
