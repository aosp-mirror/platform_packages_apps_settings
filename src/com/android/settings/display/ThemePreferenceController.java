/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.ACTION_THEME;

import android.app.AlertDialog;
import android.app.UiModeManager;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.FeatureFactory;

import libcore.util.Objects;

public class ThemePreferenceController extends PreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_THEME = "theme";

    private final UiModeManager mUiModeManager;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public ThemePreferenceController(Context context) {
        super(context);
        mUiModeManager = context.getSystemService(UiModeManager.class);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_THEME;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_THEME.equals(preference.getKey())) {
            mMetricsFeatureProvider.action(mContext, ACTION_THEME);
        }
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        ListPreference pref = (ListPreference) preference;
        String[] options = mUiModeManager.getAvailableThemes();
        for (int i = 0; i < options.length; i++) {
            options[i] = nullToDefault(options[i]);
        }
        pref.setEntries(options);
        pref.setEntryValues(options);
        String theme = mUiModeManager.getTheme();
        if (theme == null) {
            theme = mContext.getString(R.string.default_theme);
        }
        pref.setValue(nullToDefault(theme));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (Objects.equal(newValue, mUiModeManager.getTheme())) {
            return true;
        }
        // TODO: STOPSHIP Don't require reboot and remove this prompt.
        OnClickListener onConfirm = (d, i) -> {
            mUiModeManager.setTheme(defaultToNull((String) newValue));
            ((ListPreference) preference).setValue((String) newValue);
        };
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.change_theme_reboot)
                .setPositiveButton(com.android.internal.R.string.global_action_restart, onConfirm)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        return false;
    }

    @Override
    public boolean isAvailable() {
        String[] themes = mUiModeManager.getAvailableThemes();
        return themes != null && themes.length > 1;
    }

    private String nullToDefault(String input) {
        if (input == null) {
            return mContext.getString(R.string.default_theme);
        }
        return input;
    }

    private String defaultToNull(String input) {
        if (mContext.getString(R.string.default_theme).equals(input)) {
            return null;
        }
        return input;
    }
}
