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
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

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
    public static final int GAME_DRIVER_OFF = 2;

    private final Context mContext;
    private final ContentResolver mContentResolver;
    @VisibleForTesting
    GameDriverContentObserver mGameDriverContentObserver;

    private SwitchPreference mPreference;

    public GameDriverEnableForAllAppsPreferenceController(Context context, String key) {
        super(context, key);
        mContext = context;
        mContentResolver = context.getContentResolver();
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
        mPreference = (SwitchPreference) screen.findPreference(getPreferenceKey());
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
        final SwitchPreference switchPreference = (SwitchPreference) preference;
        switchPreference.setVisible(isAvailable());
        switchPreference.setChecked(
                Settings.Global.getInt(
                        mContentResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT)
                == GAME_DRIVER_ALL_APPS);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isChecked = (boolean) newValue;
        final int gameDriver = Settings.Global.getInt(
                mContentResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT);

        if (isChecked && gameDriver == GAME_DRIVER_ALL_APPS) {
            return true;
        }

        if (!isChecked && (gameDriver == GAME_DRIVER_DEFAULT || gameDriver == GAME_DRIVER_OFF)) {
            return true;
        }

        Settings.Global.putInt(mContentResolver, Settings.Global.GAME_DRIVER_ALL_APPS,
                isChecked ? GAME_DRIVER_ALL_APPS : GAME_DRIVER_DEFAULT);

        return true;
    }

    @Override
    public void onGameDriverContentChanged() {
        updateState(mPreference);
    }
}
