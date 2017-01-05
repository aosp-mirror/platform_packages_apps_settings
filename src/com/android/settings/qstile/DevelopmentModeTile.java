/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.qstile;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
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
import com.android.settings.DevelopmentSettings;

import java.util.Map;

public class DevelopmentModeTile extends TileService {

    static final String SHARED_PREFERENCES_NAME = "development_mode_tile_settings";

    private static final String SHOW_TOUCHES_KEY = "show_touches";
    private static final String POINTER_LOCATION_KEY = "pointer_location";
    private static final String DEBUG_LAYOUT_KEY = "debug_layout";
    private static final String FORCE_RTL_LAYOUT_KEY = "force_rtl_layout_all_locales";
    private static final String WINDOW_ANIMATION_SCALE_KEY = "window_animation_scale";
    private static final String TRANSITION_ANIMATION_SCALE_KEY = "transition_animation_scale";
    private static final String ANIMATOR_DURATION_SCALE_KEY = "animator_duration_scale";
    private static final String SHOW_HW_SCREEN_UPDATES_KEY = "show_hw_screen_udpates";
    private static final String SHOW_HW_LAYERS_UPDATES_KEY = "show_hw_layers_udpates";
    private static final String DEBUG_HW_OVERDRAW_KEY = "debug_hw_overdraw";
    private static final String TRACK_FRAME_TIME_KEY = "track_frame_time";

    private DevModeProperties mProps = new DevModeProperties();

    @Override
    public void onStartListening() {
        super.onStartListening();
        refresh();
    }

    public void refresh() {
        mProps.refreshState(this);
        getQsTile().setState(mProps.isSet ? (mProps.allMatch
            ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE) : Tile.STATE_UNAVAILABLE);
        getQsTile().updateTile();
    }

    @Override
    public void onClick() {
        if (getQsTile().getState() == Tile.STATE_UNAVAILABLE) {
            startActivityAndCollapse(new Intent(this, DevelopmentTileConfigActivity.class));
            return;
        }

        boolean active = getQsTile().getState() == Tile.STATE_INACTIVE;
        Map<String, ?> values =
                getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE).getAll();
        ContentResolver cr = getContentResolver();
        for (Property prop : mProps.mSysProps) {
            Object expected = values.get(prop.prefKey);
            String value = active && !prop.isDefault(expected) ? expected.toString() : "false";
            SystemProperties.set(prop.key, value);
        }
        for (Property prop : mProps.mSysSettings) {
            boolean expectedTrue = active && !prop.isDefault(values.get(prop.prefKey));
            Settings.System.putInt(cr, prop.key, expectedTrue ? 1 : 0);
        }

        boolean expectedGlobPropTrue = active &&
                !mProps.mGlobProp.isDefault(values.get(mProps.mGlobProp.prefKey));
        Settings.Global.putInt(cr, mProps.mGlobProp.key, expectedGlobPropTrue ? 1 : 0);
        SystemProperties.set(mProps.mGlobProp.key, expectedGlobPropTrue ? "1" : "0");
        LocalePicker.updateLocales(getResources().getConfiguration().getLocales());

        IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
        try {
            // Update the various animation scale values to expected values or 1. mProps.mAnimScales
            // is an ordered array, where the index corresponds to the individual property.
            for (int i = 0; i < mProps.mAnimScales.length; i++) {
                Object expected = values.get(mProps.mAnimScales[i]);
                float expectedFloat = active && expected != null ?
                        Float.parseFloat(expected.toString()) : 1;
                wm.setAnimationScale(i, expectedFloat);
            }
        } catch (RemoteException e) { }

        new DevelopmentSettings.SystemPropPoker().execute(); // Settings app magic
        refresh();
    }

    static class DevModeProperties {

        private final Property[] mSysProps = new Property[] {
                new Property(View.DEBUG_LAYOUT_PROPERTY, DEBUG_LAYOUT_KEY),
                new Property(ThreadedRenderer.DEBUG_DIRTY_REGIONS_PROPERTY,
                        SHOW_HW_SCREEN_UPDATES_KEY),
                new Property(ThreadedRenderer.DEBUG_SHOW_LAYERS_UPDATES_PROPERTY,
                        SHOW_HW_LAYERS_UPDATES_KEY),
                new Property(ThreadedRenderer.DEBUG_OVERDRAW_PROPERTY, DEBUG_HW_OVERDRAW_KEY),
                new Property(ThreadedRenderer.PROFILE_PROPERTY, TRACK_FRAME_TIME_KEY),
        };

        private final Property[] mSysSettings = new Property[] {
                new Property(Settings.System.SHOW_TOUCHES, SHOW_TOUCHES_KEY),
                new Property(Settings.System.POINTER_LOCATION, POINTER_LOCATION_KEY),
        };

        private final Property mGlobProp =
                new Property(Settings.Global.DEVELOPMENT_FORCE_RTL, FORCE_RTL_LAYOUT_KEY);

        private final String[] mAnimScales = new String[] {
                WINDOW_ANIMATION_SCALE_KEY,
                TRANSITION_ANIMATION_SCALE_KEY,
                ANIMATOR_DURATION_SCALE_KEY
        };

        /**
         * True is the values of all the properties corresponds to the expected values. Updated when
         * {@link #refreshState(Context)} is called.
         */
        public boolean allMatch;
        /**
         * True is at least one property has a non-default expected value. Updated when
         * {@link #refreshState(Context)} is called. Not that if all properties have default
         * expected value, then active and non-active state will be the same.
         */
        public boolean isSet;

        public void refreshState(Context context) {
            Map<String, ?> values =
                    context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE).getAll();
            allMatch = true;
            // True if there is at least one non-default value.
            isSet = false;

            for (Property prop : mSysProps) {
                Object expected = values.get(prop.prefKey);
                String actual = SystemProperties.get(prop.key);
                allMatch &= prop.isDefault(expected)
                    ? prop.isDefault(actual) : expected.toString().equals(actual);
                isSet |= !prop.isDefault(expected);
            }

            ContentResolver cr = context.getContentResolver();
            for (Property prop : mSysSettings) {
                boolean expectedTrue = !prop.isDefault(values.get(prop.prefKey));
                isSet |= expectedTrue;
                allMatch &= expectedTrue == (Settings.System.getInt(cr, prop.key, 0) != 0);
            }

            boolean expectedGlopPropTrue = !mGlobProp.isDefault(values.get(mGlobProp.prefKey));
            isSet |= expectedGlopPropTrue;
            allMatch &= expectedGlopPropTrue == (Settings.Global.getInt(cr, mGlobProp.key, 0) != 0);

            IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
            try {
                for (int i = 0; i < mAnimScales.length; i++) {
                    Object expected = values.get(mAnimScales[i]);
                    float expectedFloat = expected == null
                        ? 1 : Float.parseFloat(expected.toString());
                    isSet |= expectedFloat != 1;
                    allMatch &= expectedFloat == wm.getAnimationScale(i);
                }
            } catch (RemoteException e) { }
        }
    }

    private static class Property {
        final String key;
        final String prefKey;

        Property(String key, String prefKey) {
            this.key = key;
            this.prefKey = prefKey;
        }

        boolean isDefault(Object value) {
            if (value == null) {
                return true;
            }
            String str = value.toString();
            return str.equals("") || str.equals("false");
        }
    }
}
