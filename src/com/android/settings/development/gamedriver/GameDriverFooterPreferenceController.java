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

import static com.android.settings.development.gamedriver.GameDriverEnableForAllAppsPreferenceController.GAME_DRIVER_DEFAULT;
import static com.android.settings.development.gamedriver.GameDriverEnableForAllAppsPreferenceController.GAME_DRIVER_OFF;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.FooterPreference;

/**
 * Controller of footer preference for Game Driver.
 */
public class GameDriverFooterPreferenceController extends BasePreferenceController
        implements GameDriverContentObserver.OnGameDriverContentChangedListener, LifecycleObserver,
                   OnStart, OnStop {

    private final ContentResolver mContentResolver;
    @VisibleForTesting
    GameDriverContentObserver mGameDriverContentObserver;

    private FooterPreference mPreference;

    public GameDriverFooterPreferenceController(Context context) {
        super(context, FooterPreference.KEY_FOOTER);
        mContentResolver = context.getContentResolver();
        mGameDriverContentObserver =
                new GameDriverContentObserver(new Handler(Looper.getMainLooper()), this);
    }

    @Override
    public int getAvailabilityStatus() {
        return Settings.Global.getInt(
                       mContentResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT)
                        == GAME_DRIVER_OFF
                ? AVAILABLE_UNSEARCHABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
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
        preference.setVisible(isAvailable());
    }

    @Override
    public void onGameDriverContentChanged() {
        updateState(mPreference);
    }
}
