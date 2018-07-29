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
import android.provider.Settings.SettingNotFoundException;

final class AutofillTestingHelper {
    private final ContentResolver mResolver;

    public AutofillTestingHelper(Context context) {
        mResolver = context.getContentResolver();
    }

    public void setLoggingLevel(int max) {
        setGlobal(Settings.Global.AUTOFILL_LOGGING_LEVEL, max);
    }

    public void setMaxPartitionsSize(int max) {
        setGlobal(Settings.Global.AUTOFILL_MAX_PARTITIONS_SIZE, max);
    }

    public void setMaxVisibleDatasets(int level) {
        setGlobal(Settings.Global.AUTOFILL_MAX_VISIBLE_DATASETS, level);
    }

    public int getLoggingLevel() throws SettingNotFoundException {
        return getGlobal(Settings.Global.AUTOFILL_LOGGING_LEVEL);
    }

    public int getMaxPartitionsSize() throws SettingNotFoundException {
        return getGlobal(Settings.Global.AUTOFILL_MAX_PARTITIONS_SIZE);
    }

    public int getMaxVisibleDatasets() throws SettingNotFoundException {
        return getGlobal(Settings.Global.AUTOFILL_MAX_VISIBLE_DATASETS);
    }

    private void setGlobal(String key, int value) {
        Settings.Global.putInt(mResolver, key, value);
    }

    private int getGlobal(String key) throws SettingNotFoundException {
        return Settings.Global.getInt(mResolver, key);
    }
}
