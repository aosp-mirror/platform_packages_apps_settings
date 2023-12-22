/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.applications.appcompat;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_16_9;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_3_2;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_4_3;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_APP_DEFAULT;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_DISPLAY_SIZE;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_SPLIT_SCREEN;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_UNSET;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.ActionButtonsPreference;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.ArrayList;
import java.util.List;

/**
 * App specific activity to show aspect ratio overrides
 */
public class UserAspectRatioDetails extends AppInfoBase implements
        RadioWithImagePreference.OnClickListener {
    private static final String TAG = UserAspectRatioDetails.class.getSimpleName();

    private static final String KEY_HEADER_SUMMARY = "app_aspect_ratio_summary";
    private static final String KEY_HEADER_BUTTONS = "header_view";

    private static final String KEY_PREF_HALF_SCREEN = "half_screen_pref";
    private static final String KEY_PREF_DISPLAY_SIZE = "display_size_pref";
    private static final String KEY_PREF_16_9 = "16_9_pref";
    private static final String KEY_PREF_4_3 = "4_3_pref";
    @VisibleForTesting
    static final String KEY_PREF_FULLSCREEN = "fullscreen_pref";
    @VisibleForTesting
    static final String KEY_PREF_DEFAULT = "app_default_pref";
    @VisibleForTesting
    static final String KEY_PREF_3_2 = "3_2_pref";

    @VisibleForTesting
    @NonNull String mSelectedKey = KEY_PREF_DEFAULT;

    /** Radio button preference key mapped to {@link PackageManager.UserMinAspectRatio} value */
    @VisibleForTesting
    final BiMap<String, Integer> mKeyToAspectRatioMap = HashBiMap.create();

    private final List<RadioWithImagePreference> mAspectRatioPreferences = new ArrayList<>();

    @NonNull private UserAspectRatioManager mUserAspectRatioManager;
    private boolean mIsOverrideToFullscreenEnabled;

    @Override
    public void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUserAspectRatioManager = new UserAspectRatioManager(getContext());
        mIsOverrideToFullscreenEnabled = getAspectRatioManager()
                .isOverrideToFullscreenEnabled(mPackageName, mUserId);

        initPreferences();
        try {
            final int userAspectRatio = getAspectRatioManager()
                    .getUserMinAspectRatioValue(mPackageName, mUserId);
            mSelectedKey = getSelectedKey(userAspectRatio);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get user min aspect ratio");
        }
        refreshUi();
    }

    @Override
    public void onRadioButtonClicked(@NonNull RadioWithImagePreference selected) {
        final String selectedKey = selected.getKey();
        if (mSelectedKey.equals(selectedKey)) {
            return;
        }
        final int userAspectRatio = getSelectedUserMinAspectRatio(selectedKey);
        try {
            getAspectRatioManager().setUserMinAspectRatio(mPackageName, mUserId, userAspectRatio);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to set user min aspect ratio");
            return;
        }
        logActionMetrics(selectedKey, mSelectedKey);
        // Only update to selected aspect ratio if nothing goes wrong
        mSelectedKey = selectedKey;
        updateAllPreferences(mSelectedKey);
        Log.d(TAG, "Killing application process " + mPackageName);
        try {
            final IActivityManager am = ActivityManager.getService();
            am.stopAppForUser(mPackageName, mUserId);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to stop application " + mPackageName);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.USER_ASPECT_RATIO_APP_INFO_SETTINGS;
    }

    @Override
    protected boolean refreshUi() {
        if (mPackageInfo == null || mPackageInfo.applicationInfo == null) {
            return false;
        }
        updateAllPreferences(mSelectedKey);
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    private void launchApplication() {
        Intent launchIntent = mPm.getLaunchIntentForPackage(mPackageName)
                .addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP);
        if (launchIntent != null) {
            getContext().startActivityAsUser(launchIntent, new UserHandle(mUserId));
        }
    }

    @PackageManager.UserMinAspectRatio
    @VisibleForTesting
    int getSelectedUserMinAspectRatio(@NonNull String selectedKey) {
        final int appDefault = mKeyToAspectRatioMap
                .getOrDefault(KEY_PREF_DEFAULT, USER_MIN_ASPECT_RATIO_UNSET);
        return mKeyToAspectRatioMap.getOrDefault(selectedKey, appDefault);
    }

    @NonNull
    private String getSelectedKey(@PackageManager.UserMinAspectRatio int userMinAspectRatio) {
        final String appDefault = mKeyToAspectRatioMap.inverse()
                .getOrDefault(USER_MIN_ASPECT_RATIO_UNSET, KEY_PREF_DEFAULT);

        if (userMinAspectRatio == USER_MIN_ASPECT_RATIO_UNSET && mIsOverrideToFullscreenEnabled) {
            // Pre-select fullscreen option if device manufacturer has overridden app to fullscreen
            userMinAspectRatio = USER_MIN_ASPECT_RATIO_FULLSCREEN;
        }
        return mKeyToAspectRatioMap.inverse().getOrDefault(userMinAspectRatio, appDefault);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Preference pref = EntityHeaderController
                .newInstance(getActivity(), this, null /* header */)
                .setIcon(Utils.getBadgedIcon(getContext(), mPackageInfo.applicationInfo))
                .setLabel(mPackageInfo.applicationInfo.loadLabel(mPm))
                .setIsInstantApp(AppUtils.isInstant(mPackageInfo.applicationInfo))
                .setPackageName(mPackageName)
                .setUid(mPackageInfo.applicationInfo.uid)
                .setHasAppInfoLink(true)
                .setButtonActions(EntityHeaderController.ActionType.ACTION_NONE,
                        EntityHeaderController.ActionType.ACTION_NONE)
                .done(getPrefContext());

        getPreferenceScreen().addPreference(pref);
    }

    private void initPreferences() {
        addPreferencesFromResource(R.xml.user_aspect_ratio_details);

        final String summary = getContext().getResources().getString(
                R.string.aspect_ratio_main_summary, Build.MODEL);
        findPreference(KEY_HEADER_SUMMARY).setTitle(summary);

        ((ActionButtonsPreference) findPreference(KEY_HEADER_BUTTONS))
                .setButton1Text(R.string.launch_instant_app)
                .setButton1Icon(R.drawable.ic_settings_open)
                .setButton1OnClickListener(v -> launchApplication());

        if (mIsOverrideToFullscreenEnabled) {
            addPreference(KEY_PREF_DEFAULT, USER_MIN_ASPECT_RATIO_APP_DEFAULT);
        } else {
            addPreference(KEY_PREF_DEFAULT, USER_MIN_ASPECT_RATIO_UNSET);
        }
        addPreference(KEY_PREF_FULLSCREEN, USER_MIN_ASPECT_RATIO_FULLSCREEN);
        addPreference(KEY_PREF_DISPLAY_SIZE, USER_MIN_ASPECT_RATIO_DISPLAY_SIZE);
        addPreference(KEY_PREF_HALF_SCREEN, USER_MIN_ASPECT_RATIO_SPLIT_SCREEN);
        addPreference(KEY_PREF_16_9, USER_MIN_ASPECT_RATIO_16_9);
        addPreference(KEY_PREF_4_3, USER_MIN_ASPECT_RATIO_4_3);
        addPreference(KEY_PREF_3_2, USER_MIN_ASPECT_RATIO_3_2);
    }

    private void addPreference(@NonNull String key,
            @PackageManager.UserMinAspectRatio int aspectRatio) {
        final RadioWithImagePreference pref = findPreference(key);
        if (pref == null) {
            return;
        }
        if (!getAspectRatioManager().hasAspectRatioOption(aspectRatio, mPackageName)) {
            pref.setVisible(false);
            return;
        }
        pref.setTitle(mUserAspectRatioManager.getAccessibleEntry(aspectRatio, mPackageName));
        pref.setOnClickListener(this);
        mKeyToAspectRatioMap.put(key, aspectRatio);
        mAspectRatioPreferences.add(pref);
    }

    private void updateAllPreferences(@NonNull String selectedKey) {
        for (RadioWithImagePreference pref : mAspectRatioPreferences) {
            pref.setChecked(selectedKey.equals(pref.getKey()));
        }
    }

    private void logActionMetrics(@NonNull String selectedKey, @NonNull String unselectedKey) {
        final MetricsFeatureProvider metricsFeatureProvider =
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        final int attribution = metricsFeatureProvider.getAttribution(getActivity());
        metricsFeatureProvider.action(
                attribution,
                getUnselectedAspectRatioAction(unselectedKey),
                getMetricsCategory(),
                mPackageName,
                mUserId
        );
        metricsFeatureProvider.action(
                attribution,
                getSelectedAspectRatioAction(selectedKey),
                getMetricsCategory(),
                mPackageName,
                mUserId
        );
    }

    private static int getSelectedAspectRatioAction(@NonNull String selectedKey) {
        switch (selectedKey) {
            case KEY_PREF_DEFAULT:
                return SettingsEnums.ACTION_USER_ASPECT_RATIO_APP_DEFAULT_SELECTED;
            case KEY_PREF_FULLSCREEN:
                return SettingsEnums.ACTION_USER_ASPECT_RATIO_FULL_SCREEN_SELECTED;
            case KEY_PREF_HALF_SCREEN:
                return SettingsEnums.ACTION_USER_ASPECT_RATIO_HALF_SCREEN_SELECTED;
            case KEY_PREF_4_3:
                return SettingsEnums.ACTION_USER_ASPECT_RATIO_4_3_SELECTED;
            case KEY_PREF_16_9:
                return SettingsEnums.ACTION_USER_ASPECT_RATIO_16_9_SELECTED;
            case KEY_PREF_3_2:
                return SettingsEnums.ACTION_USER_ASPECT_RATIO_3_2_SELECTED;
            case KEY_PREF_DISPLAY_SIZE:
                return SettingsEnums.ACTION_USER_ASPECT_RATIO_DISPLAY_SIZE_SELECTED;
            default:
                return SettingsEnums.ACTION_UNKNOWN;
        }
    }

    private static int getUnselectedAspectRatioAction(@NonNull String unselectedKey) {
        switch (unselectedKey) {
            case KEY_PREF_DEFAULT:
                return SettingsEnums.ACTION_USER_ASPECT_RATIO_APP_DEFAULT_UNSELECTED;
            case KEY_PREF_FULLSCREEN:
                return SettingsEnums.ACTION_USER_ASPECT_RATIO_FULL_SCREEN_UNSELECTED;
            case KEY_PREF_HALF_SCREEN:
                return SettingsEnums.ACTION_USER_ASPECT_RATIO_HALF_SCREEN_UNSELECTED;
            case KEY_PREF_4_3:
                return SettingsEnums.ACTION_USER_ASPECT_RATIO_4_3_UNSELECTED;
            case KEY_PREF_16_9:
                return SettingsEnums.ACTION_USER_ASPECT_RATIO_16_9_UNSELECTED;
            case KEY_PREF_3_2:
                return SettingsEnums.ACTION_USER_ASPECT_RATIO_3_2_UNSELECTED;
            case KEY_PREF_DISPLAY_SIZE:
                return SettingsEnums.ACTION_USER_ASPECT_RATIO_DISPLAY_SIZE_UNSELECTED;
            default:
                return SettingsEnums.ACTION_UNKNOWN;
        }
    }

    @VisibleForTesting
    UserAspectRatioManager getAspectRatioManager() {
        return mUserAspectRatioManager;
    }
}
