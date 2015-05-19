/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.applications;

import android.annotation.Nullable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/**
 * Settings screen to manage everything about assist.
 */
public class ManageAssist extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_CONTEXT = "context";

    private SwitchPreference mContextPref;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.manage_assist);
        mContextPref = (SwitchPreference) findPreference(KEY_CONTEXT);
        mContextPref.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ASSIST_STRUCTURE_ENABLED, 1) != 0);
        mContextPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView v = (TextView) LayoutInflater.from(view.getContext()).inflate(
                R.layout.manage_assist_footer, null);
        getListView().addFooterView(v);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATIONS_MANAGE_ASSIST;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mContextPref) {
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.ASSIST_STRUCTURE_ENABLED,
                    (boolean) newValue ? 1 : 0);
            return true;
        }
        return false;
    }
}
