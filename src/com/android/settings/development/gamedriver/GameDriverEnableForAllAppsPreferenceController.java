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

package com.android.settings.development.gamedriver;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

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

/**
 * Controller of global switch to enable Game Driver for all Apps.
 */
public class GameDriverEnableForAllAppsPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener,
                   GameDriverContentObserver.OnGameDriverContentChangedListener, LifecycleObserver,
                   OnStart, OnStop {

    public static final int GAME_DRIVER_DEFAULT = 0;
    public static final int GAME_DRIVER_ALL_APPS = 1;
    public static final int GAME_DRIVER_PRERELEASE_ALL_APPS = 2;
    public static final int GAME_DRIVER_OFF = 3;

    private final Context mContext;
    private final ContentResolver mContentResolver;
    private final String mPreferenceDefault;
    private final String mPreferenceGameDriver;
    private final String mPreferencePrereleaseDriver;
    @VisibleForTesting
    GameDriverContentObserver mGameDriverContentObserver;

    private ListPreference mPreference;

    public GameDriverEnableForAllAppsPreferenceController(Context context, String key) {
        super(context, key);
        mContext = context;
        mContentResolver = context.getContentResolver();

        final Resources resources = context.getResources();
        mPreferenceDefault = resources.getString(R.string.game_driver_app_preference_default);
        mPreferenceGameDriver =
                resources.getString(R.string.game_driver_app_preference_game_driver);
        mPreferencePrereleaseDriver =
                resources.getString(R.string.game_driver_app_preference_prerelease_driver);
        mGameDriverContentObserver =
                new GameDriverContentObserver(new Handler(Looper.getMainLooper()), this);
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
        mPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onStart() {
        mGameDriverContentObserver.register(mContentResolver);
    }

    @Override
    public void onStop() {
        mGameDriverContentObserver.unregister(mContentResolver);
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
    public void onGameDriverContentChanged() {
        updateState(mPreference);
    }
}
