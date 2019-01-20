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
package com.android.settings.connecteddevice;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.bluetooth.BluetoothDeviceRenamePreferenceController;
import com.android.settings.bluetooth.BluetoothSwitchPreferenceController;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBarController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.FooterPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated screen for allowing the user to toggle bluetooth which displays relevant information to
 * the user based on related settings such as bluetooth scanning.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL)
public class BluetoothDashboardFragment extends DashboardFragment {

    private static final String TAG = "BluetoothDashboardFrag";
    public static final String KEY_BLUETOOTH_SCREEN = "bluetooth_switchbar_screen";

    private FooterPreference mFooterPreference;
    private SwitchBar mSwitchBar;
    private BluetoothSwitchPreferenceController mController;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BLUETOOTH_FRAGMENT;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_bluetooth_screen;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_screen;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mFooterPreference = mFooterPreferenceMixin.createFooterPreference();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(BluetoothDeviceRenamePreferenceController.class).setFragment(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SettingsActivity activity = (SettingsActivity) getActivity();
        mSwitchBar = activity.getSwitchBar();
        mController = new BluetoothSwitchPreferenceController(activity,
                new SwitchBarController(mSwitchBar), mFooterPreference);
        Lifecycle lifecycle = getSettingsLifecycle();
        if (lifecycle != null) {
            lifecycle.addObserver(mController);
        }
    }
    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                        boolean enabled) {
                    final List<SearchIndexableRaw> result = new ArrayList<>();

                    // Add the activity title
                    final SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = context.getString(R.string.bluetooth_settings_title);
                    data.screenTitle = context.getString(R.string.bluetooth_settings_title);
                    data.keywords = context.getString(R.string.keywords_bluetooth_settings);
                    data.key = KEY_BLUETOOTH_SCREEN;
                    result.add(data);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    BluetoothManager manager =
                            (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
                    if (manager != null) {
                        BluetoothAdapter adapter = manager.getAdapter();
                        final int status = adapter != null
                                ? TogglePreferenceController.AVAILABLE
                                : TogglePreferenceController.UNSUPPORTED_ON_DEVICE;
                        if (status != TogglePreferenceController.AVAILABLE) {
                            keys.add(KEY_BLUETOOTH_SCREEN);
                        }
                    }

                    return keys;
                }
            };
}
