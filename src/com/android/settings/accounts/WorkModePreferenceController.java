/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.settings.accounts;

import android.content.Context;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.slices.SliceData;
import com.android.settingslib.widget.MainSwitchPreference;

import org.jetbrains.annotations.NotNull;


/** Controller for "Work apps" toggle that allows the user to enable/disable quiet mode. */
public class WorkModePreferenceController extends BasePreferenceController
        implements OnCheckedChangeListener, DefaultLifecycleObserver,
        ManagedProfileQuietModeEnabler.QuietModeChangeListener {

    private final ManagedProfileQuietModeEnabler mQuietModeEnabler;
    private MainSwitchPreference mPreference;

    public WorkModePreferenceController(Context context, String key) {
        super(context, key);
        mQuietModeEnabler = new ManagedProfileQuietModeEnabler(context, this);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.addOnSwitchChangeListener(this);
    }

    @Override
    public int getAvailabilityStatus() {
        return (mQuietModeEnabler.isAvailable()) ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public void onStart(@NotNull LifecycleOwner lifecycleOwner) {
        lifecycleOwner.getLifecycle().addObserver(mQuietModeEnabler);
    }

    @Override
    public void onStop(@NotNull LifecycleOwner lifecycleOwner) {
        lifecycleOwner.getLifecycle().removeObserver(mQuietModeEnabler);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mQuietModeEnabler.setQuietModeEnabled(!isChecked);
        if (android.app.admin.flags.Flags.quietModeCredentialBugFix()) {
            updateState(mPreference);
        }
    }

    @Override
    public final void updateState(Preference preference) {
        mPreference.updateStatus(!mQuietModeEnabler.isQuietModeEnabled());
    }

    @Override
    public void onQuietModeChanged() {
        updateState(mPreference);
    }

    @Override
    @SliceData.SliceType
    public int getSliceType() {
        return SliceData.SliceType.SWITCH;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accounts;
    }
}
