/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.virtual;

import static com.android.settings.spa.app.appinfo.AppInfoSettingsProvider.startAppInfoSettings;

import android.app.Application;
import android.content.Context;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.Utils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.widget.AppPreference;

import java.util.Objects;

/** This class adds the details about the virtual device companion app. */
public class VirtualDeviceDetailsCompanionAppController extends BasePreferenceController {

    private static final String KEY_VIRTUAL_DEVICE_COMPANION_APP = "virtual_device_companion_app";

    @Nullable
    private PreferenceFragmentCompat mFragment;
    @Nullable
    private String mPackageName;

    public VirtualDeviceDetailsCompanionAppController(@NonNull Context context) {
        super(context, KEY_VIRTUAL_DEVICE_COMPANION_APP);
    }

    /** One-time initialization when the controller is first created. */
    void init(@NonNull PreferenceFragmentCompat fragment, @NonNull String packageName) {
        mFragment = fragment;
        mPackageName = packageName;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        ApplicationsState applicationsState = ApplicationsState.getInstance(
                (Application) mContext.getApplicationContext());
        final ApplicationsState.AppEntry appEntry = applicationsState.getEntry(
                mPackageName, UserHandle.myUserId());

        final AppPreference preference = screen.findPreference(getPreferenceKey());

        preference.setTitle(appEntry.label);
        preference.setIcon(Utils.getBadgedIcon(mContext, appEntry.info));
        preference.setOnPreferenceClickListener(pref -> {
            startAppInfoSettings(Objects.requireNonNull(mPackageName), appEntry.info.uid,
                    Objects.requireNonNull(mFragment), /* request= */ 1001,
                    getMetricsCategory());
            return true;
        });
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
