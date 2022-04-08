/**
 * Copyright (C) 2017 The Android Open Source Project
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.deletionhelper;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.text.format.Formatter;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.Utils;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * Handles the wall of text which appears below the options in the Storage Management settings drill
 * down.
 */
public class AutomaticStorageManagerDescriptionPreferenceController
        extends AbstractPreferenceController implements PreferenceControllerMixin {
    private static final String KEY_FREED = "freed_bytes";

    public AutomaticStorageManagerDescriptionPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_FREED;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        Preference preference = screen.findPreference(getPreferenceKey());
        final Context context = preference.getContext();
        ContentResolver cr = context.getContentResolver();
        long freedBytes =
                Settings.Secure.getLong(
                        cr, Settings.Secure.AUTOMATIC_STORAGE_MANAGER_BYTES_CLEARED, 0);
        long lastRunMillis =
                Settings.Secure.getLong(cr, Settings.Secure.AUTOMATIC_STORAGE_MANAGER_LAST_RUN, 0);
        if (freedBytes == 0 || lastRunMillis == 0 || !Utils.isStorageManagerEnabled(context)) {
            preference.setSummary(R.string.automatic_storage_manager_text);
        } else {
            preference.setSummary(
                    context.getString(
                            R.string.automatic_storage_manager_freed_bytes,
                            Formatter.formatFileSize(context, freedBytes),
                            DateUtils.formatDateTime(
                                    context, lastRunMillis, DateUtils.FORMAT_SHOW_DATE)));
        }
    }
}
