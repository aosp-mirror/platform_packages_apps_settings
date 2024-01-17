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

package com.android.settings.bluetooth;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.utils.ThreadUtils;

import dagger.internal.Preconditions;

import java.util.List;

public class BluetoothDetailsExtraOptionsController extends BluetoothDetailsController {

    private static final String KEY_BLUETOOTH_EXTRA_OPTIONS = "bt_extra_options";

    @VisibleForTesting @Nullable
    PreferenceCategory mOptionsContainer;
    @Nullable PreferenceScreen mPreferenceScreen;

    public BluetoothDetailsExtraOptionsController(
            Context context,
            PreferenceFragmentCompat fragment,
            CachedBluetoothDevice device,
            Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BLUETOOTH_EXTRA_OPTIONS;
    }

    @Override
    protected void init(PreferenceScreen screen) {
        mPreferenceScreen = screen;
        mOptionsContainer = screen.findPreference(getPreferenceKey());
        refresh();
    }

    @Override
    protected void refresh() {
        ThreadUtils.postOnBackgroundThread(
                () -> {
                    List<Preference> options =
                            FeatureFactory.getFeatureFactory()
                                    .getBluetoothFeatureProvider()
                                    .getBluetoothExtraOptions(mContext, mCachedDevice);
                    ThreadUtils.postOnMainThread(
                            () -> {
                                if (mOptionsContainer != null) {
                                    mOptionsContainer.removeAll();
                                    for (Preference option : options) {
                                        mOptionsContainer.addPreference(option);
                                    }
                                    setVisible(
                                            Preconditions.checkNotNull(mPreferenceScreen),
                                            getPreferenceKey(),
                                            !options.isEmpty());
                                }
                            });
                });
    }
}
