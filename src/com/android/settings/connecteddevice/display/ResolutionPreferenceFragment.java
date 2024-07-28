/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.display;

import static android.view.Display.INVALID_DISPLAY;

import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.DISPLAY_ID_ARG;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.EXTERNAL_DISPLAY_HELP_URL;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.EXTERNAL_DISPLAY_NOT_FOUND_RESOURCE;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.isDisplayAllowed;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.Display.Mode;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.internal.util.ToBooleanFunction;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragmentBase;
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.DisplayListener;
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.Injector;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.HashSet;

public class ResolutionPreferenceFragment extends SettingsPreferenceFragmentBase {
    private static final String TAG = "ResolutionPreference";
    static final int DEFAULT_LOW_REFRESH_RATE = 60;
    static final String MORE_OPTIONS_KEY = "more_options";
    static final String TOP_OPTIONS_KEY = "top_options";
    static final int MORE_OPTIONS_TITLE_RESOURCE =
            R.string.external_display_more_options_title;
    static final int EXTERNAL_DISPLAY_RESOLUTION_SETTINGS_RESOURCE =
            R.xml.external_display_resolution_settings;
    static final String DISPLAY_MODE_LIMIT_OVERRIDE_PROP = "persist.sys.com.android.server.display"
            + ".feature.flags.enable_mode_limit_for_external_display-override";
    @Nullable
    private Injector mInjector;
    @Nullable
    private PreferenceCategory mTopOptionsPreference;
    @Nullable
    private PreferenceCategory mMoreOptionsPreference;
    private boolean mStarted;
    private final HashSet<String> mResolutionPreferences = new HashSet<>();
    private int mExternalDisplayPeakWidth;
    private int mExternalDisplayPeakHeight;
    private int mExternalDisplayPeakRefreshRate;
    private boolean mRefreshRateSynchronizationEnabled;
    private boolean mMoreOptionsExpanded;
    private final Runnable mUpdateRunnable = this::update;
    private final DisplayListener mListener = new DisplayListener() {
        @Override
        public void update(int displayId) {
            scheduleUpdate();
        }
    };

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY;
    }

    @Override
    public int getHelpResource() {
        return EXTERNAL_DISPLAY_HELP_URL;
    }

    @Override
    public void onCreateCallback(@Nullable Bundle icicle) {
        if (mInjector == null) {
            mInjector = new Injector(getPrefContext());
        }
        addPreferencesFromResource(EXTERNAL_DISPLAY_RESOLUTION_SETTINGS_RESOURCE);
        updateDisplayModeLimits(mInjector.getContext());
    }

    @Override
    public void onActivityCreatedCallback(@Nullable Bundle savedInstanceState) {
        View view = getView();
        TextView emptyView = null;
        if (view != null) {
            emptyView = (TextView) view.findViewById(android.R.id.empty);
        }
        if (emptyView != null) {
            emptyView.setText(EXTERNAL_DISPLAY_NOT_FOUND_RESOURCE);
            setEmptyView(emptyView);
        }
    }

    @Override
    public void onStartCallback() {
        mStarted = true;
        if (mInjector == null) {
            return;
        }
        mInjector.registerDisplayListener(mListener);
        scheduleUpdate();
    }

    @Override
    public void onStopCallback() {
        mStarted = false;
        if (mInjector == null) {
            return;
        }
        mInjector.unregisterDisplayListener(mListener);
        unscheduleUpdate();
    }

    public ResolutionPreferenceFragment() {}

    @VisibleForTesting
    ResolutionPreferenceFragment(@NonNull Injector injector) {
        mInjector = injector;
    }

    @VisibleForTesting
    protected int getDisplayIdArg() {
        var args = getArguments();
        return args != null ? args.getInt(DISPLAY_ID_ARG, INVALID_DISPLAY) : INVALID_DISPLAY;
    }

    @VisibleForTesting
    @NonNull
    protected Resources getResources(@NonNull Context context) {
        return context.getResources();
    }

    private void update() {
        final PreferenceScreen screen = getPreferenceScreen();
        if (screen == null || mInjector == null) {
            return;
        }
        var context = mInjector.getContext();
        if (context == null) {
            return;
        }
        var display = mInjector.getDisplay(getDisplayIdArg());
        if (display == null || !isDisplayAllowed(display, mInjector)) {
            screen.removeAll();
            mTopOptionsPreference = null;
            mMoreOptionsPreference = null;
            return;
        }
        mResolutionPreferences.clear();
        var remainingModes = addModePreferences(context,
                getTopPreference(context, screen),
                display.getSupportedModes(), this::isTopMode, display);
        addRemainingPreferences(context,
                getMorePreference(context, screen),
                display, remainingModes.first, remainingModes.second);
    }

    private PreferenceCategory getTopPreference(@NonNull Context context,
            @NonNull PreferenceScreen screen) {
        if (mTopOptionsPreference == null) {
            mTopOptionsPreference = new PreferenceCategory(context);
            mTopOptionsPreference.setPersistent(false);
            mTopOptionsPreference.setKey(TOP_OPTIONS_KEY);
            screen.addPreference(mTopOptionsPreference);
        } else {
            mTopOptionsPreference.removeAll();
        }
        return mTopOptionsPreference;
    }

    private PreferenceCategory getMorePreference(@NonNull Context context,
            @NonNull PreferenceScreen screen) {
        if (mMoreOptionsPreference == null) {
            mMoreOptionsPreference = new PreferenceCategory(context);
            mMoreOptionsPreference.setPersistent(false);
            mMoreOptionsPreference.setTitle(MORE_OPTIONS_TITLE_RESOURCE);
            mMoreOptionsPreference.setOnExpandButtonClickListener(() -> {
                mMoreOptionsExpanded = true;
            });
            mMoreOptionsPreference.setKey(MORE_OPTIONS_KEY);
            screen.addPreference(mMoreOptionsPreference);
        } else {
            mMoreOptionsPreference.removeAll();
        }
        return mMoreOptionsPreference;
    }

    private void addRemainingPreferences(@NonNull Context context,
            @NonNull PreferenceCategory group, @NonNull Display display,
            boolean isSelectedModeFound, @NonNull Mode[] moreModes) {
        if (moreModes.length == 0) {
            return;
        }
        mMoreOptionsExpanded |= !isSelectedModeFound;
        group.setInitialExpandedChildrenCount(mMoreOptionsExpanded ? Integer.MAX_VALUE : 0);
        addModePreferences(context, group, moreModes, /*checkMode=*/ null, display);
    }

    private Pair<Boolean, Mode[]> addModePreferences(@NonNull Context context,
            @NonNull PreferenceGroup group,
            @NonNull Mode[] modes,
            @Nullable ToBooleanFunction<Mode> checkMode,
            @NonNull Display display) {
        Display.Mode curMode = display.getMode();
        var currentResolution = curMode.getPhysicalWidth() + "x" + curMode.getPhysicalHeight();
        var rotatedResolution = curMode.getPhysicalHeight() + "x" + curMode.getPhysicalWidth();
        var skippedModes = new ArrayList<Mode>();
        var isAnyOfModesSelected = false;
        for (var mode : modes) {
            var modeStr = mode.getPhysicalWidth() + "x" + mode.getPhysicalHeight();
            SelectorWithWidgetPreference pref = group.findPreference(modeStr);
            if (pref != null) {
                continue;
            }
            if (checkMode != null && !checkMode.apply(mode)) {
                skippedModes.add(mode);
                continue;
            }
            var isCurrentMode =
                    currentResolution.equals(modeStr) || rotatedResolution.equals(modeStr);
            if (!isCurrentMode && !isAllowedMode(mode)) {
                continue;
            }
            if (mResolutionPreferences.contains(modeStr)) {
                // Added to "Top modes" already.
                continue;
            }
            mResolutionPreferences.add(modeStr);
            pref = new SelectorWithWidgetPreference(context);
            pref.setPersistent(false);
            pref.setKey(modeStr);
            pref.setTitle(mode.getPhysicalWidth() + " x " + mode.getPhysicalHeight());
            pref.setSingleLineTitle(true);
            pref.setOnClickListener(preference -> onDisplayModeClicked(preference, display));
            pref.setChecked(isCurrentMode);
            isAnyOfModesSelected |= isCurrentMode;
            group.addPreference(pref);
        }
        return new Pair<>(isAnyOfModesSelected, skippedModes.toArray(Mode.EMPTY_ARRAY));
    }

    private boolean isTopMode(@NonNull Mode mode) {
        return mTopOptionsPreference != null
                && mTopOptionsPreference.getPreferenceCount() < 3;
    }

    private boolean isAllowedMode(@NonNull Mode mode) {
        if (mRefreshRateSynchronizationEnabled
                && (mode.getRefreshRate() < DEFAULT_LOW_REFRESH_RATE - 1
                        || mode.getRefreshRate() > DEFAULT_LOW_REFRESH_RATE + 1)) {
            Log.d(TAG, mode + " refresh rate is out of synchronization range");
            return false;
        }
        if (mExternalDisplayPeakHeight > 0
                && mode.getPhysicalHeight() > mExternalDisplayPeakHeight) {
            Log.d(TAG, mode + " height is above the allowed limit");
            return false;
        }
        if (mExternalDisplayPeakWidth > 0
                && mode.getPhysicalWidth() > mExternalDisplayPeakWidth) {
            Log.d(TAG, mode + " width is above the allowed limit");
            return false;
        }
        if (mExternalDisplayPeakRefreshRate > 0
                && mode.getRefreshRate() > mExternalDisplayPeakRefreshRate) {
            Log.d(TAG, mode + " refresh rate is above the allowed limit");
            return false;
        }
        return true;
    }

    private void scheduleUpdate() {
        if (mInjector == null || !mStarted) {
            return;
        }
        unscheduleUpdate();
        mInjector.getHandler().post(mUpdateRunnable);
    }

    private void unscheduleUpdate() {
        if (mInjector == null || !mStarted) {
            return;
        }
        mInjector.getHandler().removeCallbacks(mUpdateRunnable);
    }

    private void onDisplayModeClicked(@NonNull SelectorWithWidgetPreference preference,
            @NonNull Display display) {
        if (mInjector == null) {
            return;
        }
        String[] modeResolution = preference.getKey().split("x");
        int width = Integer.parseInt(modeResolution[0]);
        int height = Integer.parseInt(modeResolution[1]);
        for (var mode : display.getSupportedModes()) {
            if (mode.getPhysicalWidth() == width && mode.getPhysicalHeight() == height
                        && isAllowedMode(mode)) {
                mInjector.setUserPreferredDisplayMode(display.getDisplayId(), mode);
                return;
            }
        }
    }

    private boolean isDisplayResolutionLimitEnabled() {
        if (mInjector == null) {
            return false;
        }
        var flagOverride = mInjector.getSystemProperty(DISPLAY_MODE_LIMIT_OVERRIDE_PROP);
        var isOverrideEnabled = "true".equals(flagOverride);
        var isOverrideEnabledOrNotSet = !"false".equals(flagOverride);
        return (mInjector.isModeLimitForExternalDisplayEnabled() && isOverrideEnabledOrNotSet)
                || isOverrideEnabled;
    }

    private void updateDisplayModeLimits(@Nullable Context context) {
        if (context == null) {
            return;
        }
        mExternalDisplayPeakRefreshRate = getResources(context).getInteger(
                    com.android.internal.R.integer.config_externalDisplayPeakRefreshRate);
        if (isDisplayResolutionLimitEnabled()) {
            mExternalDisplayPeakWidth = getResources(context).getInteger(
                    com.android.internal.R.integer.config_externalDisplayPeakWidth);
            mExternalDisplayPeakHeight = getResources(context).getInteger(
                    com.android.internal.R.integer.config_externalDisplayPeakHeight);
        }
        mRefreshRateSynchronizationEnabled = getResources(context).getBoolean(
                    com.android.internal.R.bool.config_refreshRateSynchronizationEnabled);
        Log.d(TAG, "mExternalDisplayPeakRefreshRate=" + mExternalDisplayPeakRefreshRate);
        Log.d(TAG, "mExternalDisplayPeakWidth=" + mExternalDisplayPeakWidth);
        Log.d(TAG, "mExternalDisplayPeakHeight=" + mExternalDisplayPeakHeight);
        Log.d(TAG, "mRefreshRateSynchronizationEnabled=" + mRefreshRateSynchronizationEnabled);
    }
}
