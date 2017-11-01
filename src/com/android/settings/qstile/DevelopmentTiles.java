/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.settings.qstile;

import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.view.IWindowManager;
import android.view.ThreadedRenderer;
import android.view.View;
import android.view.WindowManagerGlobal;

import com.android.internal.app.LocalePicker;
import com.android.settings.development.DevelopmentSettings;

public abstract class DevelopmentTiles extends TileService {

    protected abstract boolean isEnabled();

    protected abstract void setIsEnabled(boolean isEnabled);

    @Override
    public void onStartListening() {
        super.onStartListening();
        refresh();
    }

    public void refresh() {
        getQsTile().setState(isEnabled() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        getQsTile().updateTile();
    }

    @Override
    public void onClick() {
        setIsEnabled(getQsTile().getState() == Tile.STATE_INACTIVE);
        new DevelopmentSettings.SystemPropPoker().execute(); // Settings app magic
        refresh();
    }

    /**
     * Tile to control the "Show layout bounds" developer setting
     */
    public static class ShowLayout extends DevelopmentTiles {

        @Override
        protected boolean isEnabled() {
            return SystemProperties.getBoolean(View.DEBUG_LAYOUT_PROPERTY, false);
        }

        @Override
        protected void setIsEnabled(boolean isEnabled) {
            SystemProperties.set(View.DEBUG_LAYOUT_PROPERTY, isEnabled ? "true" : "false");
        }
    }

    /**
     * Tile to control the "GPU profiling" developer setting
     */
    public static class GPUProfiling extends DevelopmentTiles {

        @Override
        protected boolean isEnabled() {
            final String value = SystemProperties.get(ThreadedRenderer.PROFILE_PROPERTY);
            return value.equals("visual_bars");
        }

        @Override
        protected void setIsEnabled(boolean isEnabled) {
            SystemProperties.set(ThreadedRenderer.PROFILE_PROPERTY, isEnabled ? "visual_bars" : "");
        }
    }

    /**
     * Tile to control the "Force RTL" developer setting
     */
    public static class ForceRTL extends DevelopmentTiles {

        @Override
        protected boolean isEnabled() {
            return Settings.Global.getInt(
                    getContentResolver(), Settings.Global.DEVELOPMENT_FORCE_RTL, 0) != 0;
        }

        @Override
        protected void setIsEnabled(boolean isEnabled) {
            Settings.Global.putInt(
                    getContentResolver(), Settings.Global.DEVELOPMENT_FORCE_RTL, isEnabled ? 1 : 0);
            SystemProperties.set(Settings.Global.DEVELOPMENT_FORCE_RTL, isEnabled ? "1" : "0");
            LocalePicker.updateLocales(getResources().getConfiguration().getLocales());
        }
    }

    /**
     * Tile to control the "Animation speed" developer setting
     */
    public static class AnimationSpeed extends DevelopmentTiles {

        @Override
        protected boolean isEnabled() {
            IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
            try {
                return wm.getAnimationScale(0) != 1;
            } catch (RemoteException e) { }
            return false;
        }

        @Override
        protected void setIsEnabled(boolean isEnabled) {
            IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
            float scale = isEnabled ? 10 : 1;
            try {
                wm.setAnimationScale(0, scale);
                wm.setAnimationScale(1, scale);
                wm.setAnimationScale(2, scale);
            } catch (RemoteException e) { }
        }
    }
}