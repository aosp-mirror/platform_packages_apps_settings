/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.ToggleSwitch;

public abstract class ToggleFeaturePreferenceFragment extends SettingsPreferenceFragment {

    protected SwitchBar mSwitchBar;
    protected ToggleSwitch mToggleSwitch;

    protected String mPreferenceKey;

    protected CharSequence mSettingsTitle;
    protected Intent mSettingsIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int resId = getPreferenceScreenResId();
        if (resId <= 0) {
            PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(
                    getActivity());
            setPreferenceScreen(preferenceScreen);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SettingsActivity activity = (SettingsActivity) getActivity();
        mSwitchBar = activity.getSwitchBar();
        updateSwitchBarText(mSwitchBar);
        mToggleSwitch = mSwitchBar.getSwitch();

        onProcessArguments(getArguments());

        // Show the "Settings" menu as if it were a preference screen
        if (mSettingsTitle != null && mSettingsIntent != null) {
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            Preference settingsPref = new Preference(preferenceScreen.getContext());
            settingsPref.setTitle(mSettingsTitle);
            settingsPref.setIconSpaceReserved(true);
            settingsPref.setIntent(mSettingsIntent);
            preferenceScreen.addPreference(settingsPref);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        installActionBarToggleSwitch();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        removeActionBarToggleSwitch();
    }

    protected void updateSwitchBarText(SwitchBar switchBar) {
        // Implement this to provide meaningful text in switch bar
        switchBar.setSwitchBarText(R.string.accessibility_service_master_switch_title,
                R.string.accessibility_service_master_switch_title);
    }

    protected abstract void onPreferenceToggled(String preferenceKey, boolean enabled);

    protected void onInstallSwitchBarToggleSwitch() {
        // Implement this to set a checked listener.
    }

    protected void onRemoveSwitchBarToggleSwitch() {
        // Implement this to reset a checked listener.
    }

    private void installActionBarToggleSwitch() {
        mSwitchBar.show();
        onInstallSwitchBarToggleSwitch();
    }

    private void removeActionBarToggleSwitch() {
        mToggleSwitch.setOnBeforeCheckedChangeListener(null);
        onRemoveSwitchBarToggleSwitch();
        mSwitchBar.hide();
    }

    public void setTitle(String title) {
        getActivity().setTitle(title);
    }

    protected void onProcessArguments(Bundle arguments) {
        // Key.
        mPreferenceKey = arguments.getString(AccessibilitySettings.EXTRA_PREFERENCE_KEY);

        // Enabled.
        if (arguments.containsKey(AccessibilitySettings.EXTRA_CHECKED)) {
            final boolean enabled = arguments.getBoolean(AccessibilitySettings.EXTRA_CHECKED);
            mSwitchBar.setCheckedInternal(enabled);
        }

        // Title.
        if (arguments.containsKey(AccessibilitySettings.EXTRA_RESOLVE_INFO)) {
            ResolveInfo info = arguments.getParcelable(AccessibilitySettings.EXTRA_RESOLVE_INFO);
            getActivity().setTitle(info.loadLabel(getPackageManager()).toString());
        } else if (arguments.containsKey(AccessibilitySettings.EXTRA_TITLE)) {
            setTitle(arguments.getString(AccessibilitySettings.EXTRA_TITLE));
        }

        // Summary.
        if (arguments.containsKey(AccessibilitySettings.EXTRA_SUMMARY_RES)) {
            final int summary = arguments.getInt(AccessibilitySettings.EXTRA_SUMMARY_RES);
            mFooterPreferenceMixin.createFooterPreference().setTitle(summary);
        } else if (arguments.containsKey(AccessibilitySettings.EXTRA_SUMMARY)) {
            final CharSequence summary = arguments.getCharSequence(
                    AccessibilitySettings.EXTRA_SUMMARY);
            mFooterPreferenceMixin.createFooterPreference().setTitle(summary);
        }
    }
}
