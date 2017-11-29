/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.SearchIndexableResource;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.bluetooth.BluetoothMasterSwitchPreferenceController;
import com.android.settings.bluetooth.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.deviceinfo.UsbBackend;
import com.android.settings.nfc.NfcPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.drawer.CategoryKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This fragment contains all the advanced connection preferences(i.e, Bluetooth, NFC, USB..)
 */
public class AdvancedConnectedDeviceDashboardFragment extends DashboardFragment {

    private static final String TAG = "AdvancedConnectedDeviceFrag";
    private UsbModePreferenceController mUsbPrefController;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CONNECTION_DEVICE_ADVANCED;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_connected_devices;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.connected_devices_advanced;
    }

    @Override
    public String getCategoryKey() {
        //TODO(b/69926683): remove this method and change DashboardFragmentRegistry directly for P
        return CategoryKey.CATEGORY_DEVICE;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final Lifecycle lifecycle = getLifecycle();
        final NfcPreferenceController nfcPreferenceController =
                new NfcPreferenceController(context);
        lifecycle.addObserver(nfcPreferenceController);
        controllers.add(nfcPreferenceController);
        mUsbPrefController = new UsbModePreferenceController(context, new UsbBackend(context));
        lifecycle.addObserver(mUsbPrefController);
        controllers.add(mUsbPrefController);
        final BluetoothMasterSwitchPreferenceController bluetoothPreferenceController =
                new BluetoothMasterSwitchPreferenceController(
                        context, Utils.getLocalBtManager(context), this,
                        (SettingsActivity) getActivity());
        lifecycle.addObserver(bluetoothPreferenceController);
        controllers.add(bluetoothPreferenceController);

        SmsMirroringFeatureProvider smsMirroringFeatureProvider =
                FeatureFactory.getFactory(context).getSmsMirroringFeatureProvider();
        AbstractPreferenceController smsMirroringController =
                smsMirroringFeatureProvider.getController(context);
        controllers.add(smsMirroringController);
        return controllers;
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.connected_devices_advanced;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    PackageManager pm = context.getPackageManager();
                    if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
                        keys.add(NfcPreferenceController.KEY_TOGGLE_NFC);
                        keys.add(NfcPreferenceController.KEY_ANDROID_BEAM_SETTINGS);
                    }
                    keys.add(BluetoothMasterSwitchPreferenceController.KEY_TOGGLE_BLUETOOTH);

                    SmsMirroringFeatureProvider smsMirroringFeatureProvider =
                            FeatureFactory.getFactory(context).getSmsMirroringFeatureProvider();
                    SmsMirroringPreferenceController smsMirroringController =
                            smsMirroringFeatureProvider.getController(context);
                    smsMirroringController.updateNonIndexableKeys(keys);

                    return keys;
                }
            };
}
