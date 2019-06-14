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

import static com.android.settings.development.gamedriver.GameDriverEnableForAllAppsPreferenceController.GAME_DRIVER_ALL_APPS;
import static com.android.settings.development.gamedriver.GameDriverEnableForAllAppsPreferenceController.GAME_DRIVER_DEFAULT;
import static com.android.settings.development.gamedriver.GameDriverEnableForAllAppsPreferenceController.GAME_DRIVER_OFF;
import static com.android.settings.development.gamedriver.GameDriverEnableForAllAppsPreferenceController.GAME_DRIVER_PRERELEASE_ALL_APPS;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;

import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

/**
 * Controller of global switch bar used to fully turn off Game Driver.
 */
public class GameDriverGlobalSwitchBarController
        implements SwitchWidgetController.OnSwitchChangeListener,
                   GameDriverContentObserver.OnGameDriverContentChangedListener, LifecycleObserver,
                   OnStart, OnStop {

    private final Context mContext;
    private final ContentResolver mContentResolver;
    @VisibleForTesting
    SwitchWidgetController mSwitchWidgetController;
    @VisibleForTesting
    GameDriverContentObserver mGameDriverContentObserver;

    GameDriverGlobalSwitchBarController(
            Context context, SwitchWidgetController switchWidgetController) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mGameDriverContentObserver =
                new GameDriverContentObserver(new Handler(Looper.getMainLooper()), this);
        mSwitchWidgetController = switchWidgetController;
        mSwitchWidgetController.setEnabled(
                DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context));
        mSwitchWidgetController.setChecked(
                Settings.Global.getInt(
                        mContentResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT)
                != GAME_DRIVER_OFF);
        mSwitchWidgetController.setListener(this);
    }

    @Override
    public void onStart() {
        mSwitchWidgetController.startListening();
        mGameDriverContentObserver.register(mContentResolver);
    }

    @Override
    public void onStop() {
        mSwitchWidgetController.stopListening();
        mGameDriverContentObserver.unregister(mContentResolver);
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        final int gameDriver = Settings.Global.getInt(
                mContentResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT);

        if (isChecked
                && (gameDriver == GAME_DRIVER_DEFAULT || gameDriver == GAME_DRIVER_ALL_APPS
                        || gameDriver == GAME_DRIVER_PRERELEASE_ALL_APPS)) {
            return true;
        }

        if (!isChecked && gameDriver == GAME_DRIVER_OFF) {
            return true;
        }

        Settings.Global.putInt(mContentResolver, Settings.Global.GAME_DRIVER_ALL_APPS,
                isChecked ? GAME_DRIVER_DEFAULT : GAME_DRIVER_OFF);

        return true;
    }

    @Override
    public void onGameDriverContentChanged() {
        mSwitchWidgetController.setChecked(
                Settings.Global.getInt(
                        mContentResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT)
                != GAME_DRIVER_OFF);
    }
}
