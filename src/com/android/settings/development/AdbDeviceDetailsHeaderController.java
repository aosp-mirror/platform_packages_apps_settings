/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.settings.development;

import android.content.Context;
import android.debug.PairDevice;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.widget.LayoutPreference;

/**
 * Controller for logic pertaining to displaying adb device information for the
 * {@link AdbDeviceDetailsFragment}.
 */
public class AdbDeviceDetailsHeaderController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver {

    private static final String TAG = "AdbDeviceDetailsHeader";

    @VisibleForTesting
    static final String KEY_HEADER = "adb_device_header";

    private PairDevice mPairedDevice;
    private final Fragment mFragment;
    private EntityHeaderController mEntityHeaderController;

    public AdbDeviceDetailsHeaderController(
            PairDevice pairedDevice,
            Context context,
            Fragment fragment) {
        super(context);

        mPairedDevice = pairedDevice;
        mFragment = fragment;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_HEADER;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        setupEntityHeader(screen);
    }

    private void setupEntityHeader(PreferenceScreen screen) {
        LayoutPreference headerPref = (LayoutPreference) screen.findPreference(KEY_HEADER);
        mEntityHeaderController =
                EntityHeaderController.newInstance(
                        mFragment.getActivity(), mFragment,
                        headerPref.findViewById(R.id.entity_header));

        mEntityHeaderController
                .setIcon(mContext.getDrawable(com.android.internal.R.drawable.ic_bt_laptop))
                .setLabel(mPairedDevice.getDeviceName())
                .done(mFragment.getActivity(), true);
    }
}

