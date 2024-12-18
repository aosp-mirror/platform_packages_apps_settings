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

import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.EXTERNAL_DISPLAY_HELP_URL;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.DISPLAY_ID_ARG;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.EXTERNAL_DISPLAY_NOT_FOUND_RESOURCE;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.isDisplayAllowed;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.isUseDisplaySettingEnabled;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.isResolutionSettingEnabled;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.isRotationSettingEnabled;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragmentBase;
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.DisplayListener;
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.Injector;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.TwoTargetPreference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The Settings screen for External Displays configuration and connection management.
 */
public class ExternalDisplayPreferenceFragment extends SettingsPreferenceFragmentBase {
    static final int EXTERNAL_DISPLAY_SETTINGS_RESOURCE = R.xml.external_display_settings;
    static final String DISPLAYS_LIST_PREFERENCE_KEY = "displays_list_preference";
    static final String EXTERNAL_DISPLAY_USE_PREFERENCE_KEY = "external_display_use_preference";
    static final String EXTERNAL_DISPLAY_ROTATION_KEY = "external_display_rotation";
    static final String EXTERNAL_DISPLAY_RESOLUTION_PREFERENCE_KEY = "external_display_resolution";
    static final int EXTERNAL_DISPLAY_CHANGE_RESOLUTION_FOOTER_RESOURCE =
            R.string.external_display_change_resolution_footer_title;
    static final int EXTERNAL_DISPLAY_LANDSCAPE_DRAWABLE =
            R.drawable.external_display_mirror_landscape;
    static final int EXTERNAL_DISPLAY_TITLE_RESOURCE =
            R.string.external_display_settings_title;
    static final int EXTERNAL_DISPLAY_USE_TITLE_RESOURCE =
            R.string.external_display_use_title;
    static final int EXTERNAL_DISPLAY_NOT_FOUND_FOOTER_RESOURCE =
            R.string.external_display_not_found_footer_title;
    static final int EXTERNAL_DISPLAY_PORTRAIT_DRAWABLE =
            R.drawable.external_display_mirror_portrait;
    static final int EXTERNAL_DISPLAY_ROTATION_TITLE_RESOURCE =
            R.string.external_display_rotation;
    static final int EXTERNAL_DISPLAY_RESOLUTION_TITLE_RESOURCE =
            R.string.external_display_resolution_settings_title;
    @VisibleForTesting
    static final String PREVIOUSLY_SHOWN_LIST_KEY = "mPreviouslyShownListOfDisplays";
    private boolean mStarted;
    @Nullable
    private MainSwitchPreference mUseDisplayPref;
    @Nullable
    private IllustrationPreference mImagePreference;
    @Nullable
    private Preference mResolutionPreference;
    @Nullable
    private ListPreference mRotationPref;
    @Nullable
    private FooterPreference mFooterPreference;
    @Nullable
    private PreferenceCategory mDisplaysPreference;
    @Nullable
    private Injector mInjector;
    @Nullable
    private String[] mRotationEntries;
    @Nullable
    private String[] mRotationEntriesValues;
    @NonNull
    private final Runnable mUpdateRunnable = this::update;
    private final DisplayListener mListener = new DisplayListener() {
        @Override
        public void update(int displayId) {
            scheduleUpdate();
        }
    };
    private boolean mPreviouslyShownListOfDisplays;

    public ExternalDisplayPreferenceFragment() {}

    @VisibleForTesting
    ExternalDisplayPreferenceFragment(@NonNull Injector injector) {
        mInjector = injector;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY;
    }

    @Override
    public int getHelpResource() {
        return EXTERNAL_DISPLAY_HELP_URL;
    }

    @Override
    public void onSaveInstanceStateCallback(@NonNull Bundle outState) {
        outState.putSerializable(PREVIOUSLY_SHOWN_LIST_KEY,
                (Serializable) mPreviouslyShownListOfDisplays);
    }

    @Override
    public void onCreateCallback(@Nullable Bundle icicle) {
        if (mInjector == null) {
            mInjector = new Injector(getPrefContext());
        }
        addPreferencesFromResource(EXTERNAL_DISPLAY_SETTINGS_RESOURCE);
    }

    @Override
    public void onActivityCreatedCallback(@Nullable Bundle savedInstanceState) {
        restoreState(savedInstanceState);
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

    /**
     * @return id of the preference.
     */
    @Override
    protected int getPreferenceScreenResId() {
        return EXTERNAL_DISPLAY_SETTINGS_RESOURCE;
    }

    @VisibleForTesting
    protected void launchResolutionSelector(@NonNull final Context context, final int displayId) {
        final Bundle args = new Bundle();
        args.putInt(DISPLAY_ID_ARG, displayId);
        new SubSettingLauncher(context)
                .setDestination(ResolutionPreferenceFragment.class.getName())
                .setArguments(args)
                .setSourceMetricsCategory(getMetricsCategory()).launch();
    }

    @VisibleForTesting
    protected void launchDisplaySettings(final int displayId) {
        final Bundle args = new Bundle();
        var context = getPrefContext();
        args.putInt(DISPLAY_ID_ARG, displayId);
        new SubSettingLauncher(context)
                .setDestination(this.getClass().getName())
                .setArguments(args)
                .setSourceMetricsCategory(getMetricsCategory()).launch();
    }

    /**
     * Returns the preference for the footer.
     */
    @NonNull
    @VisibleForTesting
    FooterPreference getFooterPreference(@NonNull Context context) {
        if (mFooterPreference == null) {
            mFooterPreference = new FooterPreference(context);
            mFooterPreference.setPersistent(false);
        }
        return mFooterPreference;
    }

    @NonNull
    @VisibleForTesting
    ListPreference getRotationPreference(@NonNull Context context) {
        if (mRotationPref == null) {
            mRotationPref = new ListPreference(context);
            mRotationPref.setPersistent(false);
        }
        return mRotationPref;
    }

    @NonNull
    @VisibleForTesting
    Preference getResolutionPreference(@NonNull Context context) {
        if (mResolutionPreference == null) {
            mResolutionPreference = new Preference(context);
            mResolutionPreference.setPersistent(false);
        }
        return mResolutionPreference;
    }

    @NonNull
    @VisibleForTesting
    MainSwitchPreference getUseDisplayPreference(@NonNull Context context) {
        if (mUseDisplayPref == null) {
            mUseDisplayPref = new MainSwitchPreference(context);
            mUseDisplayPref.setPersistent(false);
        }
        return mUseDisplayPref;
    }

    @NonNull
    @VisibleForTesting
    IllustrationPreference getIllustrationPreference(@NonNull Context context) {
        if (mImagePreference == null) {
            mImagePreference = new IllustrationPreference(context);
            mImagePreference.setPersistent(false);
        }
        return mImagePreference;
    }

    /**
     * @return return display id argument of this settings page.
     */
    @VisibleForTesting
    protected int getDisplayIdArg() {
        var args = getArguments();
        return args != null ? args.getInt(DISPLAY_ID_ARG, INVALID_DISPLAY) : INVALID_DISPLAY;
    }

    @NonNull
    private PreferenceCategory getDisplaysListPreference(@NonNull Context context) {
        if (mDisplaysPreference == null) {
            mDisplaysPreference = new PreferenceCategory(context);
            mDisplaysPreference.setPersistent(false);
        }
        return mDisplaysPreference;
    }

    private void restoreState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        mPreviouslyShownListOfDisplays = Boolean.TRUE.equals(savedInstanceState.getSerializable(
                PREVIOUSLY_SHOWN_LIST_KEY, Boolean.class));
    }

    private void update() {
        final var screen = getPreferenceScreen();
        if (screen == null || mInjector == null || mInjector.getContext() == null) {
            return;
        }
        screen.removeAll();
        updateScreenForDisplayId(getDisplayIdArg(), screen, mInjector.getContext());
    }

    private void updateScreenForDisplayId(final int displayId,
            @NonNull final PreferenceScreen screen, @NonNull Context context) {
        final var displaysToShow = getDisplaysToShow(displayId);
        if (displaysToShow.isEmpty() && displayId == INVALID_DISPLAY) {
            showTextWhenNoDisplaysToShow(screen, context);
        } else if (displaysToShow.size() == 1
                && ((displayId == INVALID_DISPLAY && !mPreviouslyShownListOfDisplays)
                        || displaysToShow.get(0).getDisplayId() == displayId)) {
            showDisplaySettings(displaysToShow.get(0), screen, context);
        } else if (displayId == INVALID_DISPLAY) {
            // If ever shown a list of displays - keep showing it for consistency after
            // disconnecting one of the displays, and only one display is left.
            mPreviouslyShownListOfDisplays = true;
            showDisplaysList(displaysToShow, screen, context);
        }
        updateSettingsTitle(displaysToShow, displayId);
    }

    private void updateSettingsTitle(@NonNull final List<Display> displaysToShow, int displayId) {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        if (displaysToShow.size() == 1 && displaysToShow.get(0).getDisplayId() == displayId) {
            var displayName = displaysToShow.get(0).getName();
            if (!displayName.isEmpty()) {
                activity.setTitle(displayName.substring(0, Math.min(displayName.length(), 40)));
                return;
            }
        }
        activity.setTitle(EXTERNAL_DISPLAY_TITLE_RESOURCE);
    }

    private void showTextWhenNoDisplaysToShow(@NonNull final PreferenceScreen screen,
            @NonNull Context context) {
        if (isUseDisplaySettingEnabled(mInjector)) {
            screen.addPreference(updateUseDisplayPreferenceNoDisplaysFound(context));
        }
        screen.addPreference(updateFooterPreference(context,
                EXTERNAL_DISPLAY_NOT_FOUND_FOOTER_RESOURCE));
    }

    private void showDisplaySettings(@NonNull Display display, @NonNull PreferenceScreen screen,
            @NonNull Context context) {
        final var isEnabled = mInjector != null && mInjector.isDisplayEnabled(display);
        if (isUseDisplaySettingEnabled(mInjector)) {
            screen.addPreference(updateUseDisplayPreference(context, display, isEnabled));
        }
        if (!isEnabled) {
            // Skip all other settings
            return;
        }
        final var displayRotation = getDisplayRotation(display.getDisplayId());
        screen.addPreference(updateIllustrationImage(context, displayRotation));
        screen.addPreference(updateResolutionPreference(context, display));
        screen.addPreference(updateRotationPreference(context, display, displayRotation));
        if (isResolutionSettingEnabled(mInjector)) {
            screen.addPreference(updateFooterPreference(context,
                    EXTERNAL_DISPLAY_CHANGE_RESOLUTION_FOOTER_RESOURCE));
        }
    }

    private void showDisplaysList(@NonNull List<Display> displaysToShow,
            @NonNull PreferenceScreen screen, @NonNull Context context) {
        var pref = getDisplaysListPreference(context);
        pref.setKey(DISPLAYS_LIST_PREFERENCE_KEY);
        pref.removeAll();
        if (!displaysToShow.isEmpty()) {
            screen.addPreference(pref);
        }
        for (var display : displaysToShow) {
            pref.addPreference(new DisplayPreference(context, display));
        }
    }

    private List<Display> getDisplaysToShow(int displayIdToShow) {
        if (mInjector == null) {
            return List.of();
        }
        if (displayIdToShow != INVALID_DISPLAY) {
            var display = mInjector.getDisplay(displayIdToShow);
            if (display != null && isDisplayAllowed(display, mInjector)) {
                return List.of(display);
            }
        }
        var displaysToShow = new ArrayList<Display>();
        for (var display : mInjector.getAllDisplays()) {
            if (display != null && isDisplayAllowed(display, mInjector)) {
                displaysToShow.add(display);
            }
        }
        return displaysToShow;
    }

    private Preference updateUseDisplayPreferenceNoDisplaysFound(@NonNull Context context) {
        final var pref = getUseDisplayPreference(context);
        pref.setKey(EXTERNAL_DISPLAY_USE_PREFERENCE_KEY);
        pref.setTitle(EXTERNAL_DISPLAY_USE_TITLE_RESOURCE);
        pref.setChecked(false);
        pref.setEnabled(false);
        pref.setOnPreferenceChangeListener(null);
        return pref;
    }

    private Preference updateUseDisplayPreference(@NonNull final Context context,
            @NonNull final Display display, boolean isEnabled) {
        final var pref = getUseDisplayPreference(context);
        pref.setKey(EXTERNAL_DISPLAY_USE_PREFERENCE_KEY);
        pref.setTitle(EXTERNAL_DISPLAY_USE_TITLE_RESOURCE);
        pref.setChecked(isEnabled);
        pref.setEnabled(true);
        pref.setOnPreferenceChangeListener((p, newValue) -> {
            writePreferenceClickMetric(p);
            final boolean result;
            if (mInjector == null) {
                return false;
            }
            if ((Boolean) newValue) {
                result = mInjector.enableConnectedDisplay(display.getDisplayId());
            } else {
                result = mInjector.disableConnectedDisplay(display.getDisplayId());
            }
            if (result) {
                pref.setChecked((Boolean) newValue);
            }
            return result;
        });
        return pref;
    }

    private Preference updateIllustrationImage(@NonNull final Context context,
            final int displayRotation) {
        var pref = getIllustrationPreference(context);
        if (displayRotation % 2 == 0) {
            pref.setLottieAnimationResId(EXTERNAL_DISPLAY_PORTRAIT_DRAWABLE);
        } else {
            pref.setLottieAnimationResId(EXTERNAL_DISPLAY_LANDSCAPE_DRAWABLE);
        }
        return pref;
    }

    private Preference updateFooterPreference(@NonNull final Context context, final int title) {
        var pref = getFooterPreference(context);
        pref.setTitle(title);
        return pref;
    }

    private Preference updateRotationPreference(@NonNull final Context context,
            @NonNull final Display display, final int displayRotation) {
        var pref = getRotationPreference(context);
        pref.setKey(EXTERNAL_DISPLAY_ROTATION_KEY);
        pref.setTitle(EXTERNAL_DISPLAY_ROTATION_TITLE_RESOURCE);
        if (mRotationEntries == null || mRotationEntriesValues == null) {
            mRotationEntries = new String[] {
                    context.getString(R.string.external_display_standard_rotation),
                    context.getString(R.string.external_display_rotation_90),
                    context.getString(R.string.external_display_rotation_180),
                    context.getString(R.string.external_display_rotation_270)};
            mRotationEntriesValues = new String[] {"0", "1", "2", "3"};
        }
        pref.setEntries(mRotationEntries);
        pref.setEntryValues(mRotationEntriesValues);
        pref.setValueIndex(displayRotation);
        pref.setSummary(mRotationEntries[displayRotation]);
        pref.setOnPreferenceChangeListener((p, newValue) -> {
            writePreferenceClickMetric(p);
            var rotation = Integer.parseInt((String) newValue);
            var displayId = display.getDisplayId();
            if (mInjector == null || !mInjector.freezeDisplayRotation(displayId, rotation)) {
                return false;
            }
            pref.setValueIndex(rotation);
            return true;
        });
        pref.setEnabled(isRotationSettingEnabled(mInjector));
        return pref;
    }

    private Preference updateResolutionPreference(@NonNull final Context context,
            @NonNull final Display display) {
        var pref = getResolutionPreference(context);
        pref.setKey(EXTERNAL_DISPLAY_RESOLUTION_PREFERENCE_KEY);
        pref.setTitle(EXTERNAL_DISPLAY_RESOLUTION_TITLE_RESOURCE);
        pref.setSummary(display.getMode().getPhysicalWidth() + " x "
                + display.getMode().getPhysicalHeight());
        pref.setOnPreferenceClickListener((Preference p) -> {
            writePreferenceClickMetric(p);
            launchResolutionSelector(context, display.getDisplayId());
            return true;
        });
        pref.setEnabled(isResolutionSettingEnabled(mInjector));
        return pref;
    }

    private int getDisplayRotation(int displayId) {
        if (mInjector == null) {
            return 0;
        }
        return Math.min(3, Math.max(0, mInjector.getDisplayUserRotation(displayId)));
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

    @VisibleForTesting
    class DisplayPreference extends TwoTargetPreference
            implements Preference.OnPreferenceClickListener {
        private final int mDisplayId;

        DisplayPreference(@NonNull final Context context, @NonNull final Display display) {
            super(context);
            mDisplayId = display.getDisplayId();
            setPersistent(false);
            setKey("display_id_" + mDisplayId);
            setTitle(display.getName());
            setSummary(display.getMode().getPhysicalWidth() + " x "
                               + display.getMode().getPhysicalHeight());
            setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(@NonNull Preference preference) {
            launchDisplaySettings(mDisplayId);
            writePreferenceClickMetric(preference);
            return true;
        }
    }
}
