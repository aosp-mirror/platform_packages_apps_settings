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

package com.android.settings.development.autofill;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.Log;
import android.view.autofill.AutofillManager;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public final class AutofillLoggingLevelPreferenceController
        extends DeveloperOptionsPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnDestroy {

    private static final String TAG = "AutofillLoggingLevelPreferenceController";
    private static final String AUTOFILL_LOGGING_LEVEL_KEY = "autofill_logging_level";

    private final String[] mListValues;
    private final String[] mListSummaries;
    private final AutofillDeveloperSettingsObserver mObserver;

    public AutofillLoggingLevelPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);

        Resources resources = context.getResources();
        mListValues = resources.getStringArray(R.array.autofill_logging_level_values);
        mListSummaries = resources.getStringArray(R.array.autofill_logging_level_entries);
        mObserver = new AutofillDeveloperSettingsObserver(mContext, () -> updateOptions());
        mObserver.register();

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onDestroy() {
        mObserver.unregister();
    }

    @Override
    public String getPreferenceKey() {
        return AUTOFILL_LOGGING_LEVEL_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        writeLevel(newValue);
        updateOptions();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateOptions();
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        writeLevel(null);
    }

    private void updateOptions() {
        if (mPreference == null) {
            // TODO: there should be a hook on AbstractPreferenceController where we could
            // unregister mObserver and avoid this check
            Log.v(TAG, "ignoring Settings update because UI is gone");
            return;
        }
        final int level = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AUTOFILL_LOGGING_LEVEL, AutofillManager.DEFAULT_LOGGING_LEVEL);

        final int index;
        if (level == AutofillManager.FLAG_ADD_CLIENT_DEBUG) {
            index = 1;
        } else if (level == AutofillManager.FLAG_ADD_CLIENT_VERBOSE) {
            index = 2;
        } else {
            index = 0;
        }
        final ListPreference listPreference = (ListPreference) mPreference;
        listPreference.setValue(mListValues[index]);
        listPreference.setSummary(mListSummaries[index]);
    }

    private void writeLevel(Object newValue) {
        int level = AutofillManager.NO_LOGGING;
        if (newValue instanceof String) {
            level = Integer.parseInt((String) newValue);
        }
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.AUTOFILL_LOGGING_LEVEL, level);
    }
}
