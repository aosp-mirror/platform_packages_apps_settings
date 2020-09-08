/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.settings.development.graphicsdriver;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

import dalvik.system.VMRuntime;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller of global switch to enable Game Driver for all Apps.
 */
public class GraphicsDriverEnableForAllAppsPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener,
                   GraphicsDriverContentObserver.OnGraphicsDriverContentChangedListener,
                   LifecycleObserver, OnStart, OnStop {

    public static final int GAME_DRIVER_DEFAULT = 0;
    public static final int GAME_DRIVER_ALL_APPS = 1;
    public static final int GAME_DRIVER_PRERELEASE_ALL_APPS = 2;
    public static final int GAME_DRIVER_OFF = 3;
    public static final String PROPERTY_GFX_DRIVER_GAME = "ro.gfx.driver.0";
    public static final String PROPERTY_GFX_DRIVER_PRERELEASE = "ro.gfx.driver.1";

    private final Context mContext;
    private final ContentResolver mContentResolver;
    private final String mPreferenceDefault;
    private final String mPreferenceGameDriver;
    private final String mPreferencePrereleaseDriver;
    @VisibleForTesting
    CharSequence[] mEntryList;
    @VisibleForTesting
    GraphicsDriverContentObserver mGraphicsDriverContentObserver;

    private ListPreference mPreference;

    public GraphicsDriverEnableForAllAppsPreferenceController(Context context, String key) {
        super(context, key);
        mContext = context;
        mContentResolver = context.getContentResolver();

        final Resources resources = context.getResources();
        mPreferenceDefault = resources.getString(R.string.graphics_driver_app_preference_default);
        mPreferenceGameDriver =
                resources.getString(R.string.graphics_driver_app_preference_game_driver);
        mPreferencePrereleaseDriver =
                resources.getString(R.string.graphics_driver_app_preference_prerelease_driver);
        mEntryList = constructEntryList(mContext, false);
        mGraphicsDriverContentObserver =
                new GraphicsDriverContentObserver(new Handler(Looper.getMainLooper()), this);
    }

    @Override
    public int getAvailabilityStatus() {
        return DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(mContext)
                        && (Settings.Global.getInt(mContentResolver,
                                    Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT)
                                != GAME_DRIVER_OFF)
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.setEntries(mEntryList);
        mPreference.setEntryValues(mEntryList);
        mPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onStart() {
        mGraphicsDriverContentObserver.register(mContentResolver);
    }

    @Override
    public void onStop() {
        mGraphicsDriverContentObserver.unregister(mContentResolver);
    }

    @Override
    public void updateState(Preference preference) {
        final ListPreference listPref = (ListPreference) preference;
        listPref.setVisible(isAvailable());
        final int currentChoice = Settings.Global.getInt(
                mContentResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT);
        if (currentChoice == GAME_DRIVER_ALL_APPS) {
            listPref.setValue(mPreferenceGameDriver);
            listPref.setSummary(mPreferenceGameDriver);
        } else if (currentChoice == GAME_DRIVER_PRERELEASE_ALL_APPS) {
            listPref.setValue(mPreferencePrereleaseDriver);
            listPref.setSummary(mPreferencePrereleaseDriver);
        } else {
            listPref.setValue(mPreferenceDefault);
            listPref.setSummary(mPreferenceDefault);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ListPreference listPref = (ListPreference) preference;
        final String value = newValue.toString();
        final int currentChoice = Settings.Global.getInt(
                mContentResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT);
        final int userChoice;
        if (value.equals(mPreferenceGameDriver)) {
            userChoice = GAME_DRIVER_ALL_APPS;
        } else if (value.equals(mPreferencePrereleaseDriver)) {
            userChoice = GAME_DRIVER_PRERELEASE_ALL_APPS;
        } else {
            userChoice = GAME_DRIVER_DEFAULT;
        }
        listPref.setValue(value);
        listPref.setSummary(value);

        if (userChoice != currentChoice) {
            Settings.Global.putInt(
                    mContentResolver, Settings.Global.GAME_DRIVER_ALL_APPS, userChoice);
        }

        return true;
    }

    @Override
    public void onGraphicsDriverContentChanged() {
        updateState(mPreference);
    }

    /**
     * Constructs and returns a list of graphics driver choices.
     */
    public static CharSequence[] constructEntryList(Context context, boolean withSystem) {
        final Resources resources = context.getResources();
        final String prereleaseDriverPackageName =
                SystemProperties.get(PROPERTY_GFX_DRIVER_PRERELEASE);
        final String gameDriverPackageName = SystemProperties.get(PROPERTY_GFX_DRIVER_GAME);

        List<CharSequence> entryList = new ArrayList<>();
        entryList.add(resources.getString(R.string.graphics_driver_app_preference_default));
        final PackageManager pm = context.getPackageManager();
        if (!TextUtils.isEmpty(prereleaseDriverPackageName)
                && hasDriverPackage(pm, prereleaseDriverPackageName)) {
            entryList.add(resources.getString(
                    R.string.graphics_driver_app_preference_prerelease_driver));
        }
        if (!TextUtils.isEmpty(gameDriverPackageName)
                && hasDriverPackage(pm, gameDriverPackageName)) {
            entryList.add(resources.getString(R.string.graphics_driver_app_preference_game_driver));
        }
        if (withSystem) {
            entryList.add(resources.getString(R.string.graphics_driver_app_preference_system));
        }
        CharSequence[] filteredEntryList = new CharSequence[entryList.size()];
        filteredEntryList = entryList.toArray(filteredEntryList);
        return filteredEntryList;
    }

    private static boolean hasDriverPackage(PackageManager pm, String driverPackageName) {
        final ApplicationInfo driverAppInfo;
        try {
            driverAppInfo = pm.getApplicationInfo(driverPackageName,
                    PackageManager.MATCH_SYSTEM_ONLY);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        if (driverAppInfo.targetSdkVersion < Build.VERSION_CODES.O) {
            return false;
        }
        final String abi = chooseAbi(driverAppInfo);
        if (abi == null) {
            return false;
        }
        return true;
    }

    private static String chooseAbi(ApplicationInfo ai) {
        final String isa = VMRuntime.getCurrentInstructionSet();
        if (ai.primaryCpuAbi != null
                && isa.equals(VMRuntime.getInstructionSet(ai.primaryCpuAbi))) {
            return ai.primaryCpuAbi;
        }
        if (ai.secondaryCpuAbi != null
                && isa.equals(VMRuntime.getInstructionSet(ai.secondaryCpuAbi))) {
            return ai.secondaryCpuAbi;
        }
        return null;
    }
}
