/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.nearby;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.nearby.NearbyManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

import java.util.Objects;

/**
 * Fragment with the top level fast pair settings.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class FastPairSettingsFragment extends SettingsPreferenceFragment {

    private static final String TAG = "FastPairSettingsFrag";

    private static final String SCAN_SWITCH_KEY = "fast_pair_scan_switch";
    private static final String SAVED_DEVICES_PREF_KEY = "saved_devices";

    private MainSwitchPreference mMainSwitchPreference;
    private OnMainSwitchChangeListener mMainSwitchChangeListener;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mMainSwitchPreference = Objects.requireNonNull(
                findPreference(SCAN_SWITCH_KEY));
        mMainSwitchChangeListener = (switchView, isChecked) ->
                NearbyManager.setFastPairScanEnabled(getContext(), isChecked);
        Preference savedDevicePref = Objects.requireNonNull(
                findPreference(SAVED_DEVICES_PREF_KEY));
        savedDevicePref.setOnPreferenceClickListener(preference -> {
            Intent savedDevicesIntent = getSavedDevicesIntent();
            if (savedDevicesIntent != null && getActivity() != null) {
                getActivity().startActivity(savedDevicesIntent);
            }
            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mMainSwitchPreference.addOnSwitchChangeListener(mMainSwitchChangeListener);
        mMainSwitchPreference.setChecked(NearbyManager.isFastPairScanEnabled(getContext()));
    }

    @Override
    public void onPause() {
        super.onPause();
        mMainSwitchPreference.removeOnSwitchChangeListener(mMainSwitchChangeListener);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.CONNECTION_DEVICE_ADVANCED_FAST_PAIR;
    }

    @Override
    public int getHelpResource() {
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.fast_pair_settings;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.fast_pair_settings);

    @Nullable
    private ComponentName getSavedDevicesComponent() {
        String savedDevicesComponent = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.NEARBY_FAST_PAIR_SETTINGS_DEVICES_COMPONENT);
        if (TextUtils.isEmpty(savedDevicesComponent)) {
            savedDevicesComponent = getString(
                com.android.internal.R.string.config_defaultNearbyFastPairSettingsDevicesComponent);
        }

        if (TextUtils.isEmpty(savedDevicesComponent)) {
            return null;
        }

        return ComponentName.unflattenFromString(savedDevicesComponent);
    }

    @Nullable
    private Intent getSavedDevicesIntent() {
        ComponentName componentName = getSavedDevicesComponent();
        if (componentName == null) {
            return null;
        }

        PackageManager pm = getPackageManager();
        Intent intent = getIntent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setComponent(componentName);

        final ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.GET_META_DATA);
        if (resolveInfo == null || resolveInfo.activityInfo == null) {
            Log.e(TAG, "Device-specified fast pair component (" + componentName
                    + ") not available");
            return null;
        }
        return intent;
    }
}
