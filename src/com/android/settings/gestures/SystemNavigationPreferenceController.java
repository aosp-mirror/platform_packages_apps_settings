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

import static android.os.UserHandle.USER_CURRENT;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON_OVERLAY;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.om.IOverlayManager;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.widget.RadioButtonPreference;

public abstract class SystemNavigationPreferenceController extends GesturePreferenceController
        implements RadioButtonPreference.OnClickListener {

    private static final String ACTION_QUICKSTEP = "android.intent.action.QUICKSTEP_SERVICE";
    private static final String PREF_KEY_VIDEO = "gesture_swipe_up_video";

    private static final String[] RADIO_BUTTONS_IN_GROUP = {
            SystemNavigationLegacyPreferenceController.PREF_KEY_LEGACY,
            SystemNavigationSwipeUpPreferenceController.PREF_KEY_SWIPE_UP,
            SystemNavigationEdgeToEdgePreferenceController.PREF_KEY_EDGE_TO_EDGE,
    };

    protected final IOverlayManager mOverlayManager;
    protected PreferenceScreen mPreferenceScreen;

    public SystemNavigationPreferenceController(Context context, IOverlayManager overlayManager,
            String key) {
        super(context, key);
        mOverlayManager = overlayManager;
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

    /**
     * Enables the specified overlay package.
     */
    static void setNavBarInteractionMode(IOverlayManager overlayManager, String overlayPackage) {
        try {
            overlayManager.setEnabledExclusiveInCategory(overlayPackage, USER_CURRENT);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    static boolean isSwipeUpEnabled(Context context) {
        if (isEdgeToEdgeEnabled(context)) {
            return false;
        }
        return NAV_BAR_MODE_2BUTTON == context.getResources().getInteger(
                com.android.internal.R.integer.config_navBarInteractionMode);
    }

    static boolean isEdgeToEdgeEnabled(Context context) {
        return NAV_BAR_MODE_GESTURAL == context.getResources().getInteger(
                com.android.internal.R.integer.config_navBarInteractionMode);
    }
}
