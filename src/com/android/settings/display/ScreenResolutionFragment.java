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

package com.android.settings.display;

import static com.android.settings.display.ScreenResolutionController.FHD_WIDTH;
import static com.android.settings.display.ScreenResolutionController.QHD_WIDTH;

import android.annotation.Nullable;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Display;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.display.DisplayDensityUtils;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Preference fragment used for switch screen resolution */
@SearchIndexable
public class ScreenResolutionFragment extends RadioButtonPickerFragment {
    private static final String TAG = "ScreenResolution";

    private Resources mResources;
    private static final int FHD_INDEX = 0;
    private static final int QHD_INDEX = 1;
    private static final String RESOLUTION_METRIC_SETTING_KEY = "user_selected_resolution";
    private Display mDefaultDisplay;
    private String[] mScreenResolutionOptions;
    private Set<Point> mResolutions;
    private String[] mScreenResolutionSummaries;

    private IllustrationPreference mImagePreference;
    private DisplayObserver mDisplayObserver;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mDefaultDisplay =
                context.getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);
        mResources = context.getResources();
        mScreenResolutionOptions =
                mResources.getStringArray(R.array.config_screen_resolution_options_strings);
        mScreenResolutionSummaries =
                mResources.getStringArray(R.array.config_screen_resolution_summaries_strings);
        mResolutions = getAllSupportedResolution();
        mImagePreference = new IllustrationPreference(context);
        mDisplayObserver = new DisplayObserver(context);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.screen_resolution_settings;
    }

    @Override
    protected void addStaticPreferences(PreferenceScreen screen) {
        updateIllustrationImage(mImagePreference);
        screen.addPreference(mImagePreference);

        final FooterPreference footerPreference = new FooterPreference(screen.getContext());
        footerPreference.setTitle(R.string.screen_resolution_footer);
        footerPreference.setSelectable(false);
        footerPreference.setLayoutResource(R.layout.preference_footer);
        screen.addPreference(footerPreference);
    }

    @Override
    public void bindPreferenceExtra(
            SelectorWithWidgetPreference pref,
            String key,
            CandidateInfo info,
            String defaultKey,
            String systemDefaultKey) {
        final ScreenResolutionCandidateInfo candidateInfo = (ScreenResolutionCandidateInfo) info;
        final CharSequence summary = candidateInfo.loadSummary();
        if (summary != null) pref.setSummary(summary);
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final List<ScreenResolutionCandidateInfo> candidates = new ArrayList<>();

        for (int i = 0; i < mScreenResolutionOptions.length; i++) {
            candidates.add(
                    new ScreenResolutionCandidateInfo(
                            mScreenResolutionOptions[i],
                            mScreenResolutionSummaries[i],
                            mScreenResolutionOptions[i],
                            true /* enabled */));
        }

        return candidates;
    }

    /** Get all supported resolutions on the device. */
    private Set<Point> getAllSupportedResolution() {
        Set<Point> resolutions = new HashSet<>();
        for (Display.Mode mode : mDefaultDisplay.getSupportedModes()) {
            resolutions.add(new Point(mode.getPhysicalWidth(), mode.getPhysicalHeight()));
        }

        return resolutions;
    }

    /** Get prefer display mode. */
    private Display.Mode getPreferMode(int width) {
        for (Point resolution : mResolutions) {
            if (resolution.x == width) {
                return new Display.Mode(
                        resolution.x, resolution.y, getDisplayMode().getRefreshRate());
            }
        }

        return getDisplayMode();
    }

    /** Get current display mode. */
    @VisibleForTesting
    public Display.Mode getDisplayMode() {
        return mDefaultDisplay.getMode();
    }

    /** Using display manager to set the display mode. */
    @VisibleForTesting
    public void setDisplayMode(final int width) {
        mDisplayObserver.startObserve();

        /** For store settings globally. */
        /** TODO(b/238061217): Moving to an atom with the same string */
        Settings.System.putString(
                getContext().getContentResolver(),
                RESOLUTION_METRIC_SETTING_KEY,
                getPreferMode(width).getPhysicalWidth()
                        + "x"
                        + getPreferMode(width).getPhysicalHeight());

        /** Apply the resolution change. */
        mDefaultDisplay.setUserPreferredDisplayMode(getPreferMode(width));
    }

    /** Get the key corresponding to the resolution. */
    @VisibleForTesting
    String getKeyForResolution(int width) {
        return width == FHD_WIDTH
                ? mScreenResolutionOptions[FHD_INDEX]
                : width == QHD_WIDTH ? mScreenResolutionOptions[QHD_INDEX] : null;
    }

    /** Get the width corresponding to the resolution key. */
    int getWidthForResoluitonKey(String key) {
        return mScreenResolutionOptions[FHD_INDEX].equals(key)
                ? FHD_WIDTH
                : mScreenResolutionOptions[QHD_INDEX].equals(key) ? QHD_WIDTH : -1;
    }

    @Override
    protected String getDefaultKey() {
        int physicalWidth = getDisplayMode().getPhysicalWidth();

        return getKeyForResolution(physicalWidth);
    }

    @Override
    protected boolean setDefaultKey(final String key) {
        int width = getWidthForResoluitonKey(key);
        if (width < 0) {
            return false;
        }

        setDisplayMode(width);
        updateIllustrationImage(mImagePreference);

        return true;
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference selected) {
        String selectedKey = selected.getKey();
        int selectedWidth = getWidthForResoluitonKey(selectedKey);
        if (!mDisplayObserver.setPendingResolutionChange(selectedWidth)) {
            return;
        }
        super.onRadioButtonClicked(selected);
    }

    /** Update the resolution image according display mode. */
    private void updateIllustrationImage(IllustrationPreference preference) {
        String key = getDefaultKey();

        if (TextUtils.equals(mScreenResolutionOptions[FHD_INDEX], key)) {
            preference.setLottieAnimationResId(R.drawable.screen_resolution_1080p);
        } else if (TextUtils.equals(mScreenResolutionOptions[QHD_INDEX], key)) {
            preference.setLottieAnimationResId(R.drawable.screen_resolution_1440p);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SCREEN_RESOLUTION;
    }

    /** This is an extension of the CandidateInfo class, which adds summary information. */
    public static class ScreenResolutionCandidateInfo extends CandidateInfo {
        private final CharSequence mLabel;
        private final CharSequence mSummary;
        private final String mKey;

        ScreenResolutionCandidateInfo(
                CharSequence label, CharSequence summary, String key, boolean enabled) {
            super(enabled);
            mLabel = label;
            mSummary = summary;
            mKey = key;
        }

        @Override
        public CharSequence loadLabel() {
            return mLabel;
        }

        /** It is the summary for radio options. */
        public CharSequence loadSummary() {
            return mSummary;
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
            new BaseSearchIndexProvider(R.xml.screen_resolution_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    ScreenResolutionController mController =
                            new ScreenResolutionController(context, "fragment");
                    return mController.checkSupportedResolutions();
                }
            };

    private static final class DisplayObserver implements DisplayManager.DisplayListener {
        private final @Nullable Context mContext;
        private int mDefaultDensity;
        private int mCurrentIndex;
        private AtomicInteger mPreviousWidth = new AtomicInteger(-1);

        DisplayObserver(Context context) {
            mContext = context;
        }

        public void startObserve() {
            if (mContext == null) {
                return;
            }

            final DisplayDensityUtils density = new DisplayDensityUtils(mContext);
            final int currentIndex = density.getCurrentIndex();
            final int defaultDensity = density.getDefaultDensity();

            if (density.getValues()[mCurrentIndex] == density.getDefaultDensity()) {
                return;
            }

            mDefaultDensity = defaultDensity;
            mCurrentIndex = currentIndex;
            final DisplayManager dm = mContext.getSystemService(DisplayManager.class);
            dm.registerDisplayListener(this, null);
        }

        public void stopObserve() {
            if (mContext == null) {
                return;
            }

            final DisplayManager dm = mContext.getSystemService(DisplayManager.class);
            dm.unregisterDisplayListener(this);
        }

        @Override
        public void onDisplayAdded(int displayId) {}

        @Override
        public void onDisplayRemoved(int displayId) {}

        @Override
        public void onDisplayChanged(int displayId) {
            if (displayId != Display.DEFAULT_DISPLAY) {
                return;
            }

            if (!isDensityChanged() || !isResolutionChangeApplied()) {
                return;
            }

            restoreDensity();
            stopObserve();
        }

        private void restoreDensity() {
            final DisplayDensityUtils density = new DisplayDensityUtils(mContext);
            if (density.getValues()[mCurrentIndex] != density.getDefaultDensity()) {
                DisplayDensityUtils.setForcedDisplayDensity(
                        Display.DEFAULT_DISPLAY, density.getValues()[mCurrentIndex]);
            }

            mDefaultDensity = density.getDefaultDensity();
        }

        private boolean isDensityChanged() {
            final DisplayDensityUtils density = new DisplayDensityUtils(mContext);
            if (density.getDefaultDensity() == mDefaultDensity) {
                return false;
            }

            return true;
        }

        private int getCurrentWidth() {
            final DisplayManager dm = mContext.getSystemService(DisplayManager.class);
            return dm.getDisplay(Display.DEFAULT_DISPLAY).getMode().getPhysicalWidth();
        }

        private boolean setPendingResolutionChange(int selectedWidth) {
            int currentWidth = getCurrentWidth();

            if (selectedWidth == currentWidth) {
                return false;
            }
            if (mPreviousWidth.get() != -1 && !isResolutionChangeApplied()) {
                return false;
            }

            mPreviousWidth.set(currentWidth);

            return true;
        }

        private boolean isResolutionChangeApplied() {
            if (mPreviousWidth.get() == getCurrentWidth()) {
                return false;
            }

            return true;
        }
    }
}
