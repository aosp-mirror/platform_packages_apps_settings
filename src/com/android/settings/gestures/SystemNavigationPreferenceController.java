/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.gestures;

import static android.os.UserHandle.USER_SYSTEM;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.om.IOverlayManager;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.widget.RadioButtonPreference;

public abstract class SystemNavigationPreferenceController extends GesturePreferenceController
        implements RadioButtonPreference.OnClickListener {

    private static final int OFF = 0;
    private static final int ON = 1;

    private static final String HIDE_BACK_BUTTON = "quickstepcontroller_hideback";
    private static final String HIDE_HOME_BUTTON = "quickstepcontroller_hidehome";
    private static final String HIDE_NAVBAR_DIVIDER = "hide_navigationbar_divider";
    private static final String SHOW_HANDLE = "quickstepcontroller_showhandle";
    private static final String ENABLE_CLICK_THROUGH = "quickstepcontroller_clickthrough";
    private static final String ENABLE_LAUNCHER_SWIPE_TO_HOME = "SWIPE_HOME";
    private static final String ENABLE_COLOR_ADAPT_FOR_HANDLE = "navbar_color_adapt_enable";
    private static final String ENABLE_ASSISTANT_GESTURE = "ENABLE_ASSISTANT_GESTURE";
    private static final String PROTOTYPE_ENABLED = "prototype_enabled";

    private static final int EDGE_SENSITIVITY_WIDTH = 32;
    private static final String EDGE_SENSITIVITY_KEY = "quickstepcontroller_edge_width_sensitivity";

    private static final String GESTURES_MATCH_MAP_OFF = "000000";
    private static final String GESTURES_MATCH_MAP_ON = "071133";
    private static final String GESTURES_MATCH_MAP_KEY = "quickstepcontroller_gesture_match_map";

    private static final String OVERLAY_NAVBAR_TYPE_INSET =
            "com.android.internal.experiment.navbar.type.inset";
    private static final String OVERLAY_NAVBAR_TYPE_FLOATING =
            "com.android.internal.experiment.navbar.type.floating";

    private static final String ACTION_QUICKSTEP = "android.intent.action.QUICKSTEP_SERVICE";
    private static final String PREF_KEY_VIDEO = "gesture_swipe_up_video";

    private static final String[] RADIO_BUTTONS_IN_GROUP = {
            SystemNavigationLegacyPreferenceController.PREF_KEY_LEGACY,
            SystemNavigationSwipeUpPreferenceController.PREF_KEY_SWIPE_UP,
            SystemNavigationEdgeToEdgePreferenceController.PREF_KEY_EDGE_TO_EDGE,
    };

    protected PreferenceScreen mPreferenceScreen;

    public SystemNavigationPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return isGestureAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceScreen = screen;

        Preference preference = screen.findPreference(getPreferenceKey());
        if (preference != null && preference instanceof RadioButtonPreference) {
            RadioButtonPreference radioPreference = (RadioButtonPreference) preference;
            radioPreference.setOnClickListener(this);
            radioPreference.setAppendixVisibility(View.GONE);
        }
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (!isChecked || mPreferenceScreen == null) {
            return false;
        }
        Preference preference = mPreferenceScreen.findPreference(getPreferenceKey());
        if (preference != null && preference instanceof RadioButtonPreference) {
            onRadioButtonClicked((RadioButtonPreference) preference);
        }
        return true;
    }

    @Override
    public CharSequence getSummary() {
        if (isEdgeToEdgeEnabled(mContext)) {
            return mContext.getText(R.string.edge_to_edge_navigation_title);
        } else if (isSwipeUpEnabled(mContext)) {
            return mContext.getText(R.string.swipe_up_to_switch_apps_title);
        } else {
            return mContext.getText(R.string.legacy_navigation_title);
        }
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    static boolean isGestureAvailable(Context context) {
        if (!context.getResources().getBoolean(
                com.android.internal.R.bool.config_swipe_up_gesture_setting_available)) {
            return false;
        }

        final ComponentName recentsComponentName = ComponentName.unflattenFromString(
                context.getString(com.android.internal.R.string.config_recentsComponentName));
        if (recentsComponentName == null) {
            return false;
        }
        final Intent quickStepIntent = new Intent(ACTION_QUICKSTEP)
                .setPackage(recentsComponentName.getPackageName());
        if (context.getPackageManager().resolveService(quickStepIntent,
                PackageManager.MATCH_SYSTEM_ONLY) == null) {
            return false;
        }
        return true;
    }

    static void selectRadioButtonInGroup(String preferenceKey, PreferenceScreen screen) {
        if (screen == null) {
            return;
        }
        for (String key : RADIO_BUTTONS_IN_GROUP) {
            ((RadioButtonPreference) screen.findPreference(key)).setChecked(
                    TextUtils.equals(key, preferenceKey));
        }
    }

    static void setEdgeToEdgeGestureEnabled(Context context, boolean enable) {
        // TODO(b/127366543): replace all of this with a single switch
        setBooleanGlobalSetting(context, HIDE_BACK_BUTTON, enable);
        setBooleanGlobalSetting(context, HIDE_HOME_BUTTON, enable);
        setBooleanGlobalSetting(context, HIDE_NAVBAR_DIVIDER, enable);
        setBooleanGlobalSetting(context, SHOW_HANDLE, enable);
        setBooleanGlobalSetting(context, ENABLE_CLICK_THROUGH, enable);
        setBooleanGlobalSetting(context, ENABLE_LAUNCHER_SWIPE_TO_HOME, enable);
        setBooleanGlobalSetting(context, ENABLE_COLOR_ADAPT_FOR_HANDLE, enable);
        setBooleanGlobalSetting(context, ENABLE_ASSISTANT_GESTURE, enable);
        setBooleanGlobalSetting(context, PROTOTYPE_ENABLED, enable);
        Settings.Global.putInt(context.getContentResolver(), EDGE_SENSITIVITY_KEY,
                EDGE_SENSITIVITY_WIDTH);
        Settings.Global.putString(context.getContentResolver(), GESTURES_MATCH_MAP_KEY,
                enable ? GESTURES_MATCH_MAP_ON : GESTURES_MATCH_MAP_OFF);

        IOverlayManager overlayManager = IOverlayManager.Stub
                .asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));
        if (overlayManager != null) {
            try {
                overlayManager.setEnabled(OVERLAY_NAVBAR_TYPE_FLOATING, false, USER_SYSTEM);
                overlayManager.setEnabled(OVERLAY_NAVBAR_TYPE_INSET, enable, USER_SYSTEM);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    static void setBooleanGlobalSetting(Context context, String name, boolean flag) {
        Settings.Global.putInt(context.getContentResolver(), name, flag ? ON : OFF);
    }

    static void setSwipeUpEnabled(Context context, boolean enabled) {
        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.SWIPE_UP_TO_SWITCH_APPS_ENABLED, enabled ? ON : OFF);
    }

    static boolean isSwipeUpEnabled(Context context) {
        if (isEdgeToEdgeEnabled(context)) {
            return false;
        }
        final int defaultSwipeUpValue = context.getResources()
                .getBoolean(com.android.internal.R.bool.config_swipe_up_gesture_default) ? ON : OFF;
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.SWIPE_UP_TO_SWITCH_APPS_ENABLED, defaultSwipeUpValue) == ON;
    }

    static boolean isEdgeToEdgeEnabled(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), PROTOTYPE_ENABLED, OFF) == ON;
    }
}
