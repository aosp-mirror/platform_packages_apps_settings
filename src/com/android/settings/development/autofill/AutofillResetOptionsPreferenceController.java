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

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.autofill.AutofillManager;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public final class AutofillResetOptionsPreferenceController
        extends DeveloperOptionsPreferenceController
        implements PreferenceControllerMixin {

    private static final String AUTOFILL_RESET_OPTIONS_KEY = "autofill_reset_developer_options";

    public AutofillResetOptionsPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return AUTOFILL_RESET_OPTIONS_KEY;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(AUTOFILL_RESET_OPTIONS_KEY, preference.getKey())) {
            return false;
        }
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putInt(contentResolver, Settings.Global.AUTOFILL_LOGGING_LEVEL,
                AutofillManager.DEFAULT_LOGGING_LEVEL);
        Settings.Global.putInt(contentResolver, Settings.Global.AUTOFILL_MAX_PARTITIONS_SIZE,
                AutofillManager.DEFAULT_MAX_PARTITIONS_SIZE);
        Settings.Global.putInt(contentResolver, Settings.Global.AUTOFILL_MAX_VISIBLE_DATASETS, 0);
        Toast.makeText(mContext, R.string.autofill_reset_developer_options_complete,
                Toast.LENGTH_SHORT).show();
        return true;
    }
}
