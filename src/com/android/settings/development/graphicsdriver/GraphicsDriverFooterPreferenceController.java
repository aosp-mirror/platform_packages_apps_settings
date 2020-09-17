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

import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableForAllAppsPreferenceController.GAME_DRIVER_DEFAULT;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableForAllAppsPreferenceController.GAME_DRIVER_OFF;

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
 * Controller of footer preference for Graphics Driver Preferences dashboard.
 */
public class GraphicsDriverFooterPreferenceController extends BasePreferenceController
        implements GraphicsDriverContentObserver.OnGraphicsDriverContentChangedListener,
        LifecycleObserver, OnStart, OnStop {

    private final ContentResolver mContentResolver;
    @VisibleForTesting
    GraphicsDriverContentObserver mGraphicsDriverContentObserver;

    private FooterPreference mPreference;

    public GraphicsDriverFooterPreferenceController(Context context, String key) {
        super(context, key);
        mContentResolver = context.getContentResolver();
        mGraphicsDriverContentObserver =
                new GraphicsDriverContentObserver(new Handler(Looper.getMainLooper()), this);
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
        mGraphicsDriverContentObserver.register(mContentResolver);
    }

    @Override
    public void onStop() {
        mGraphicsDriverContentObserver.unregister(mContentResolver);
    }

    @Override
    public void updateState(Preference preference) {
        final FooterPreference footerPref = (FooterPreference) preference;
        footerPref.setVisible(isAvailable());
    }

    @Override
    public void onGraphicsDriverContentChanged() {
        updateState(mPreference);
    }
}
