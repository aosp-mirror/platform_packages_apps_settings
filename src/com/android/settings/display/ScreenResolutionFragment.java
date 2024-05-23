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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.instrumentation.SettingsStatsLog;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.display.DisplayDensityUtils;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Preference fragment used for switch screen resolution */
@SearchIndexable
public class ScreenResolutionFragment extends RadioButtonPickerFragment {
    private static final String TAG = "ScreenResolution";

    private Resources mResources;
    private static final String SCREEN_RESOLUTION = "user_selected_resolution";
    private static final String SCREEN_RESOLUTION_KEY = "screen_resolution";
    private Display mDefaultDisplay;
    private String[] mScreenResolutionOptions;
    private Set<Point> mResolutions;
    private String[] mScreenResolutionSummaries;

    private IllustrationPreference mImagePreference;
    private DisplayObserver mDisplayObserver;
    private AccessibilityManager mAccessibilityManager;

    private int mHighWidth;
    private int mFullWidth;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mDefaultDisplay =
                context.getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);
        mAccessibilityManager = context.getSystemService(AccessibilityManager.class);
        mResources = context.getResources();
        mScreenResolutionOptions =
                mResources.getStringArray(R.array.config_screen_resolution_options_strings);
        mImagePreference = new IllustrationPreference(context);
        mDisplayObserver = new DisplayObserver(context);
        ScreenResolutionController controller =
                new ScreenResolutionController(context, SCREEN_RESOLUTION_KEY);
        mResolutions = controller.getAllSupportedResolutions();
        mHighWidth = controller.getHighWidth();
        mFullWidth = controller.getFullWidth();
        Log.i(TAG, "mHighWidth:" + mHighWidth + "mFullWidth:" + mFullWidth);
        mScreenResolutionSummaries =
                new String[] {
                    mHighWidth + " x " + controller.getHighHeight(),
                    mFullWidth + " x " + controller.getFullHeight()
                };
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
        footerPreference.setLayoutResource(
                com.android.settingslib.widget.preference.footer.R.layout.preference_footer);
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
        Display.Mode mode = getPreferMode(width);

        mDisplayObserver.startObserve();

        /** For store settings globally. */
        /** TODO(b/259797244): Remove this once the atom is fully populated. */
        Settings.System.putString(
                getContext().getContentResolver(),
                SCREEN_RESOLUTION,
                mode.getPhysicalWidth() + "x" + mode.getPhysicalHeight());

        try {
            /** Apply the resolution change. */
            Log.i(TAG, "setUserPreferredDisplayMode: " + mode);
            mDefaultDisplay.setUserPreferredDisplayMode(mode);
        } catch (Exception e) {
            Log.e(TAG, "setUserPreferredDisplayMode() failed", e);
            return;
        }

        /** Send the atom after resolution changed successfully. */
        SettingsStatsLog.write(
                SettingsStatsLog.USER_SELECTED_RESOLUTION,
                mDefaultDisplay.getUniqueId().hashCode(),
                mode.getPhysicalWidth(),
                mode.getPhysicalHeight());
    }

    /** Get the key corresponding to the resolution. */
    @VisibleForTesting
    String getKeyForResolution(int width) {
        return width == mHighWidth
                ? mScreenResolutionOptions[ScreenResolutionController.HIGHRESOLUTION_IDX]
                : width == mFullWidth
                        ? mScreenResolutionOptions[ScreenResolutionController.FULLRESOLUTION_IDX]
                        : null;
    }

    /** Get the width corresponding to the resolution key. */
    int getWidthForResoluitonKey(String key) {
        return mScreenResolutionOptions[ScreenResolutionController.HIGHRESOLUTION_IDX].equals(key)
                ? mHighWidth
                : mScreenResolutionOptions[ScreenResolutionController.FULLRESOLUTION_IDX].equals(
                    key)
                ? mFullWidth : -1;
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

        if (mAccessibilityManager.isEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain();
            event.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
            event.getText().add(mResources.getString(R.string.screen_resolution_selected_a11y));
            mAccessibilityManager.sendAccessibilityEvent(event);
        }

        super.onRadioButtonClicked(selected);
    }

    /** Update the resolution image according display mode. */
    private void updateIllustrationImage(IllustrationPreference preference) {
        String key = getDefaultKey();

        if (TextUtils.equals(
                mScreenResolutionOptions[ScreenResolutionController.HIGHRESOLUTION_IDX], key)) {
            preference.setLottieAnimationResId(R.drawable.screen_resolution_high);
        } else if (TextUtils.equals(
                mScreenResolutionOptions[ScreenResolutionController.FULLRESOLUTION_IDX], key)) {
            preference.setLottieAnimationResId(R.drawable.screen_resolution_full);
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
                            new ScreenResolutionController(context, SCREEN_RESOLUTION_KEY);
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
            final int currentIndex = density.getCurrentIndexForDefaultDisplay();
            final int defaultDensity = density.getDefaultDensityForDefaultDisplay();

            if (density.getDefaultDisplayDensityValues()[mCurrentIndex]
                    == density.getDefaultDensityForDefaultDisplay()) {
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
            /* If current density is the same as a default density of other resolutions,
             * then mCurrentIndex may be out of boundary.
             */
            if (density.getDefaultDisplayDensityValues().length <= mCurrentIndex) {
                mCurrentIndex = density.getCurrentIndexForDefaultDisplay();
            }
            if (density.getDefaultDisplayDensityValues()[mCurrentIndex]
                    != density.getDefaultDensityForDefaultDisplay()) {
                density.setForcedDisplayDensity(mCurrentIndex);
            }

            mDefaultDensity = density.getDefaultDensityForDefaultDisplay();
        }

        private boolean isDensityChanged() {
            final DisplayDensityUtils density = new DisplayDensityUtils(mContext);
            if (density.getDefaultDensityForDefaultDisplay() == mDefaultDensity) {
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

            Log.i(TAG,
                    "resolution changed from " + mPreviousWidth.get() + " to " + getCurrentWidth());
            return true;
        }
    }
}
