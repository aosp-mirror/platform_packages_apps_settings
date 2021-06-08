/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.notification.zen;

import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.notification.SettingsEnableZenModeDialog;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.LayoutPreference;

public class ZenModeButtonPreferenceController extends AbstractZenModePreferenceController
        implements PreferenceControllerMixin {

    public static final String KEY = "zen_mode_toggle";

    private static final String TAG = "EnableZenModeButton";
    private final FragmentManager mFragment;

    // DND can also be toggled from QS. If DND wasn't toggled by this preference, don't
    // reroute focus.
    private boolean mRefocusButton = false;
    private Button mZenButtonOn;
    private Button mZenButtonOff;

    public ZenModeButtonPreferenceController(Context context, Lifecycle lifecycle, FragmentManager
            fragment) {
        super(context, KEY, lifecycle);
        mFragment = fragment;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        if (null == mZenButtonOn) {
            mZenButtonOn = ((LayoutPreference) preference)
                    .findViewById(R.id.zen_mode_settings_turn_on_button);
            updateZenButtonOnClickListener(preference);
        }

        if (null == mZenButtonOff) {
            mZenButtonOff = ((LayoutPreference) preference)
                    .findViewById(R.id.zen_mode_settings_turn_off_button);
            mZenButtonOff.setOnClickListener(v -> {
                mRefocusButton = true;
                writeMetrics(preference, false);
                mBackend.setZenMode(Settings.Global.ZEN_MODE_OFF);
            });
        }

        updatePreference(preference);
    }

    private void updatePreference(Preference preference) {
        switch (getZenMode()) {
            case Settings.Global.ZEN_MODE_ALARMS:
            case Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
                mZenButtonOff.setVisibility(View.VISIBLE);
                mZenButtonOn.setVisibility(View.GONE);
                if (mRefocusButton) {
                    mRefocusButton = false;
                    mZenButtonOff.sendAccessibilityEvent(TYPE_VIEW_FOCUSED);
                }
                break;
            case Settings.Global.ZEN_MODE_OFF:
            default:
                mZenButtonOff.setVisibility(View.GONE);
                updateZenButtonOnClickListener(preference);
                mZenButtonOn.setVisibility(View.VISIBLE);
                if (mRefocusButton) {
                    mRefocusButton = false;
                    mZenButtonOn.sendAccessibilityEvent(TYPE_VIEW_FOCUSED);
                }
        }
    }

    private void updateZenButtonOnClickListener(Preference preference) {
        mZenButtonOn.setOnClickListener(v -> {
            mRefocusButton = true;
            writeMetrics(preference, true);
            int zenDuration = getZenDuration();
            switch (zenDuration) {
                case Settings.Secure.ZEN_DURATION_PROMPT:
                    new SettingsEnableZenModeDialog().show(mFragment, TAG);
                    break;
                case Settings.Secure.ZEN_DURATION_FOREVER:
                    mBackend.setZenMode(Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS);
                    break;
                default:
                    mBackend.setZenModeForDuration(zenDuration);
            }
        });
    }

    private void writeMetrics(Preference preference, boolean buttonOn) {
        mMetricsFeatureProvider.logClickedPreference(preference,
                preference.getExtras().getInt(DashboardFragment.CATEGORY));
        mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_ZEN_TOGGLE_DND_BUTTON,
                buttonOn);
    }
}