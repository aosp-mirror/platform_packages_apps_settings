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

import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableForAllAppsPreferenceController.UPDATABLE_DRIVER_DEFAULT;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableForAllAppsPreferenceController.UPDATABLE_DRIVER_OFF;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableForAllAppsPreferenceController.UPDATABLE_DRIVER_PRERELEASE_ALL_APPS;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableForAllAppsPreferenceController.UPDATABLE_DRIVER_PRODUCTION_ALL_APPS;

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
 * Controller of global switch bar used to fully turn off updatable driver.
 */
public class GraphicsDriverGlobalSwitchBarController
        implements SwitchWidgetController.OnSwitchChangeListener,
                   GraphicsDriverContentObserver.OnGraphicsDriverContentChangedListener,
                   LifecycleObserver, OnStart, OnStop {

    private final ContentResolver mContentResolver;
    private final SwitchWidgetController mSwitchWidgetController;
    @VisibleForTesting
    GraphicsDriverContentObserver mGraphicsDriverContentObserver;

    GraphicsDriverGlobalSwitchBarController(
            Context context, SwitchWidgetController switchWidgetController) {
        mContentResolver = context.getContentResolver();
        mGraphicsDriverContentObserver =
                new GraphicsDriverContentObserver(new Handler(Looper.getMainLooper()), this);
        mSwitchWidgetController = switchWidgetController;
        mSwitchWidgetController.setEnabled(
                DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context));
        mSwitchWidgetController.setChecked(
                Settings.Global.getInt(
                        mContentResolver, Settings.Global.UPDATABLE_DRIVER_ALL_APPS,
                        UPDATABLE_DRIVER_DEFAULT)
                != UPDATABLE_DRIVER_OFF);
        mSwitchWidgetController.setListener(this);
    }

    @Override
    public void onStart() {
        mSwitchWidgetController.startListening();
        mGraphicsDriverContentObserver.register(mContentResolver);
    }

    @Override
    public void onStop() {
        mSwitchWidgetController.stopListening();
        mGraphicsDriverContentObserver.unregister(mContentResolver);
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        final int graphicsDriverGlobalOption = Settings.Global.getInt(
                mContentResolver, Settings.Global.UPDATABLE_DRIVER_ALL_APPS,
                UPDATABLE_DRIVER_DEFAULT);

        if (isChecked
                && (graphicsDriverGlobalOption == UPDATABLE_DRIVER_DEFAULT
                        || graphicsDriverGlobalOption == UPDATABLE_DRIVER_PRODUCTION_ALL_APPS
                        || graphicsDriverGlobalOption == UPDATABLE_DRIVER_PRERELEASE_ALL_APPS)) {
            return true;
        }

        if (!isChecked && graphicsDriverGlobalOption == UPDATABLE_DRIVER_OFF) {
            return true;
        }

        Settings.Global.putInt(mContentResolver, Settings.Global.UPDATABLE_DRIVER_ALL_APPS,
                isChecked ? UPDATABLE_DRIVER_DEFAULT : UPDATABLE_DRIVER_OFF);

        return true;
    }

    @Override
    public void onGraphicsDriverContentChanged() {
        mSwitchWidgetController.setChecked(
                Settings.Global.getInt(
                        mContentResolver, Settings.Global.UPDATABLE_DRIVER_ALL_APPS,
                        UPDATABLE_DRIVER_DEFAULT)
                != UPDATABLE_DRIVER_OFF);
    }
}
