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

package com.android.settings.deviceinfo.firmwareversion;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.DeviceInfoUtils;
import androidx.preference.Preference;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class KernelVersionPreferenceController extends BasePreferenceController {

    private static final String KEY_KERNEL_VERSION = "kernel_version";
    private static final String FILENAME_PROC_VERSION = "/proc/version";
    private static final String LOG_TAG = "KernelVersionPreferenceController";

    public KernelVersionPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return DeviceInfoUtils.getFormattedKernelVersion(mContext);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_KERNEL_VERSION;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), KEY_KERNEL_VERSION)) {
            return false;
        }
        preference.setSummary(getFullKernelVersion());
        return false;
    }

    private String getFullKernelVersion() {
        String procVersionStr;
        try {
            procVersionStr = readLine(FILENAME_PROC_VERSION);
            return procVersionStr;
        } catch (IOException e) {
            Log.e(LOG_TAG,
            "IO Exception when getting kernel version for Device Info screen", e);
            return "Unavailable";
        }
    }

    /**
     * Reads a line from the specified file.
     * @param filename the file to read from
     * @return the first line, if any.
     * @throws IOException if the file couldn't be read
     */
    private static String readLine(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }
}
