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

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.SystemProperties;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.view.ThreadedRenderer;
import android.view.View;
import com.android.settings.DevelopmentSettings;

public class DevelopmentTiles {
    // List of components that need to be enabled when developer tools are turned on
    static final Class[] TILE_CLASSES = new Class[] {
            ShowLayout.class,
            GPUProfiling.class,
    };
    public static void setTilesEnabled(Context context, boolean enable) {
        final PackageManager pm = context.getPackageManager();
        for (Class cls : TILE_CLASSES) {
            pm.setComponentEnabledSetting(new ComponentName(context, cls),
                    enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                           : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                    PackageManager.DONT_KILL_APP);
        }
    }

    /**
     * Tile to control the "Show layout bounds" developer setting
     */
    public static class ShowLayout extends TileService {
        @Override
        public void onStartListening() {
            super.onStartListening();
            refresh();
        }

        public void refresh() {
            final boolean enabled = SystemProperties.getBoolean(View.DEBUG_LAYOUT_PROPERTY, false);
            getQsTile().setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            getQsTile().updateTile();
        }

        @Override
        public void onClick() {
            SystemProperties.set(View.DEBUG_LAYOUT_PROPERTY,
                    getQsTile().getState() == Tile.STATE_INACTIVE ? "true" : "false");
            new DevelopmentSettings.SystemPropPoker().execute(); // Settings app magic
            refresh();
        }
    }

    /**
     * Tile to control the "GPU profiling" developer setting
     */
    public static class GPUProfiling extends TileService {
        @Override
        public void onStartListening() {
            super.onStartListening();
            refresh();
        }

        public void refresh() {
            final String value = SystemProperties.get(ThreadedRenderer.PROFILE_PROPERTY);
            getQsTile().setState(value.equals("visual_bars")
                    ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            getQsTile().updateTile();
        }

        @Override
        public void onClick() {
            SystemProperties.set(ThreadedRenderer.PROFILE_PROPERTY,
                    getQsTile().getState() == Tile.STATE_INACTIVE ? "visual_bars" : "");
            new DevelopmentSettings.SystemPropPoker().execute(); // Settings app magic
            refresh();
        }
    }
}