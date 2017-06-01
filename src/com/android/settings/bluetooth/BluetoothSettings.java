/*
 * Copyright (C) 2011 The Android Open Source Project
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

import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.text.Spannable;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.LinkifyUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.location.ScanningSettings;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.GearPreference;
import com.android.settings.widget.SummaryUpdater.OnSummaryChangeListener;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBarController;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.FooterPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * BluetoothSettings is the Settings screen for Bluetooth configuration and
 * connection management.
 */
public class BluetoothSettings extends DeviceListPreferenceFragment implements Indexable {
    private static final String TAG = "BluetoothSettings";
    private static final int PAIRED_DEVICE_ORDER = 1;
    private static final int PAIRING_PREF_ORDER = 2;

    @VisibleForTesting
    static final String KEY_PAIRED_DEVICES = "paired_devices";
    @VisibleForTesting
    static final String KEY_FOOTER_PREF = "footer_preference";

    @VisibleForTesting
    PreferenceGroup mPairedDevicesCategory;
    @VisibleForTesting
    FooterPreference mFooterPreference;
    private Preference mPairingPreference;
    private BluetoothEnabler mBluetoothEnabler;
    private AlwaysDiscoverable mAlwaysDiscoverable;

    private SwitchBar mSwitchBar;

    private BluetoothDeviceNamePreferenceController mDeviceNamePrefController;
    @VisibleForTesting
    BluetoothPairingPreferenceController mPairingPrefController;

    // For Search
    @VisibleForTesting
    static final String DATA_KEY_REFERENCE = "main_toggle_bluetooth";

    public BluetoothSettings() {
        super(DISALLOW_CONFIG_BLUETOOTH);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.BLUETOOTH;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();
        mSwitchBar = activity.getSwitchBar();

        mBluetoothEnabler = new BluetoothEnabler(activity, new SwitchBarController(mSwitchBar),
                mMetricsFeatureProvider, Utils.getLocalBtManager(activity),
                MetricsEvent.ACTION_BLUETOOTH_TOGGLE);
        mBluetoothEnabler.setupSwitchController();
        if (mLocalAdapter != null) {
            mAlwaysDiscoverable = new AlwaysDiscoverable(getContext(), mLocalAdapter);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mBluetoothEnabler.teardownSwitchController();
    }

    @Override
    void initPreferencesFromPreferenceScreen() {
        mPairingPreference = mPairingPrefController.createBluetoothPairingPreference(
                PAIRING_PREF_ORDER);
        mFooterPreference = (FooterPreference) findPreference(KEY_FOOTER_PREF);
        mPairedDevicesCategory = (PreferenceGroup) findPreference(KEY_PAIRED_DEVICES);
    }

    @Override
    public void onStart() {
        // resume BluetoothEnabler before calling super.onStart() so we don't get
        // any onDeviceAdded() callbacks before setting up view in updateContent()
        if (mBluetoothEnabler != null) {
            mBluetoothEnabler.resume(getActivity());
        }
        super.onStart();
        // Always show paired devices regardless whether user-friendly name exists
        mShowDevicesWithoutNames = true;
        if (isUiRestricted()) {
            getPreferenceScreen().removeAll();
            if (!isUiRestrictedByOnlyAdmin()) {
                getEmptyTextView().setText(R.string.bluetooth_empty_list_user_restricted);
            }
            return;
        }

        if (mLocalAdapter != null) {
            updateContent(mLocalAdapter.getBluetoothState());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mBluetoothEnabler != null) {
            mBluetoothEnabler.pause();
        }

        // Make the device only visible to connected devices.
        if (mAlwaysDiscoverable != null) {
            mAlwaysDiscoverable.stop();
        }

        if (isUiRestricted()) {
            return;
        }
    }

    @Override
    public String getDeviceListKey() {
        return KEY_PAIRED_DEVICES;
    }

    private void updateContent(int bluetoothState) {
        int messageId = 0;

        switch (bluetoothState) {
            case BluetoothAdapter.STATE_ON:
                displayEmptyMessage(false);
                mDevicePreferenceMap.clear();

                if (isUiRestricted()) {
                    messageId = R.string.bluetooth_empty_list_user_restricted;
                    break;
                }

                addDeviceCategory(mPairedDevicesCategory,
                        R.string.bluetooth_preference_paired_devices,
                        BluetoothDeviceFilter.BONDED_DEVICE_FILTER, true);
                mPairedDevicesCategory.addPreference(mPairingPreference);
                updateFooterPreference(mFooterPreference);

                if (mAlwaysDiscoverable != null) {
                    mAlwaysDiscoverable.start();
                }
                return; // not break

            case BluetoothAdapter.STATE_TURNING_OFF:
                messageId = R.string.bluetooth_turning_off;
                mLocalAdapter.stopScanning();
                break;

            case BluetoothAdapter.STATE_OFF:
                setOffMessage();
                if (isUiRestricted()) {
                    messageId = R.string.bluetooth_empty_list_user_restricted;
                }
                break;

            case BluetoothAdapter.STATE_TURNING_ON:
                messageId = R.string.bluetooth_turning_on;
                break;
        }

        displayEmptyMessage(true);
        if (messageId != 0) {
            getEmptyTextView().setText(messageId);
        }
    }

    private void setOffMessage() {
        final TextView emptyView = getEmptyTextView();
        if (emptyView == null) {
            return;
        }
        final CharSequence briefText = getText(R.string.bluetooth_empty_list_bluetooth_off);

        final ContentResolver resolver = getActivity().getContentResolver();
        final boolean bleScanningMode = Settings.Global.getInt(
                resolver, Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 0) == 1;

        if (!bleScanningMode) {
            // Show only the brief text if the scanning mode has been turned off.
            emptyView.setText(briefText, TextView.BufferType.SPANNABLE);
        } else {
            final StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append(briefText);
            contentBuilder.append("\n\n");
            contentBuilder.append(getText(R.string.ble_scan_notify_text));
            LinkifyUtils.linkify(emptyView, contentBuilder, new LinkifyUtils.OnClickListener() {
                @Override
                public void onClick() {
                    final SettingsActivity activity =
                            (SettingsActivity) BluetoothSettings.this.getActivity();
                    activity.startPreferencePanel(BluetoothSettings.this,
                            ScanningSettings.class.getName(), null,
                            R.string.location_scanning_screen_title, null, null, 0);
                }
            });
        }
        setTextSpan(emptyView.getText(), briefText);
    }

    @VisibleForTesting
    void displayEmptyMessage(boolean display) {
        final Activity activity = getActivity();
        activity.findViewById(android.R.id.list_container).setVisibility(
                display ? View.INVISIBLE : View.VISIBLE);
        activity.findViewById(android.R.id.empty).setVisibility(
                display ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        super.onBluetoothStateChanged(bluetoothState);
        updateContent(bluetoothState);
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        updateContent(mLocalAdapter.getBluetoothState());
    }

    @VisibleForTesting
    void setTextSpan(CharSequence text, CharSequence briefText) {
        if (text instanceof Spannable) {
            Spannable boldSpan = (Spannable) text;
            boldSpan.setSpan(
                    new TextAppearanceSpan(getActivity(), android.R.style.TextAppearance_Medium), 0,
                    briefText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    @VisibleForTesting
    void setLocalBluetoothAdapter(LocalBluetoothAdapter localAdapter) {
        mLocalAdapter = localAdapter;
    }

    private final GearPreference.OnGearClickListener mDeviceProfilesListener = pref -> {
        // User clicked on advanced options icon for a device in the list
        if (!(pref instanceof BluetoothDevicePreference)) {
            Log.w(TAG, "onClick() called for other View: " + pref);
            return;
        }
        final CachedBluetoothDevice device =
                ((BluetoothDevicePreference) pref).getBluetoothDevice();
        if (device == null) {
            Log.w(TAG, "No BT device attached with this pref: " + pref);
            return;
        }
        final Bundle args = new Bundle();
        Context context = getActivity();
        boolean useDetailPage = FeatureFactory.getFactory(context).getBluetoothFeatureProvider(
                context).isDeviceDetailPageEnabled();
        if (!useDetailPage) {
            // Old version - uses a dialog.
            args.putString(DeviceProfilesSettings.ARG_DEVICE_ADDRESS,
                    device.getDevice().getAddress());
            final DeviceProfilesSettings profileSettings = new DeviceProfilesSettings();
            profileSettings.setArguments(args);
            profileSettings.show(getFragmentManager(),
                    DeviceProfilesSettings.class.getSimpleName());
        } else {
            // New version - uses a separate screen.
            args.putString(BluetoothDeviceDetailsFragment.KEY_DEVICE_ADDRESS,
                    device.getDevice().getAddress());
            final SettingsActivity activity =
                    (SettingsActivity) BluetoothSettings.this.getActivity();
            activity.startPreferencePanel(this,
                    BluetoothDeviceDetailsFragment.class.getName(), args,
                    R.string.device_details_title, null, null, 0);
        }
    };

    /**
     * Add a listener, which enables the advanced settings icon.
     *
     * @param preference the newly added preference
     */
    @Override
    void initDevicePreference(BluetoothDevicePreference preference) {
        preference.setOrder(PAIRED_DEVICE_ORDER);
        CachedBluetoothDevice cachedDevice = preference.getCachedDevice();
        if (cachedDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            // Only paired device have an associated advanced settings screen
            preference.setOnGearClickListener(mDeviceProfilesListener);
        }
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_bluetooth;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_settings;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final Lifecycle lifecycle = getLifecycle();
        mDeviceNamePrefController = new BluetoothDeviceNamePreferenceController(context, lifecycle);
        mPairingPrefController = new BluetoothPairingPreferenceController(context, this,
                (SettingsActivity) getActivity());
        controllers.add(mDeviceNamePrefController);
        controllers.add(mPairingPrefController);
        controllers.add(new BluetoothFilesPreferenceController(context));
        controllers.add(new BluetoothDeviceRenamePreferenceController(context, this, lifecycle));

        return controllers;
    }

    @VisibleForTesting
    static class SummaryProvider implements SummaryLoader.SummaryProvider, OnSummaryChangeListener {

        private final LocalBluetoothManager mBluetoothManager;
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        @VisibleForTesting
        BluetoothSummaryUpdater mSummaryUpdater;

        public SummaryProvider(Context context, SummaryLoader summaryLoader,
                LocalBluetoothManager bluetoothManager) {
            mBluetoothManager = bluetoothManager;
            mContext = context;
            mSummaryLoader = summaryLoader;
            mSummaryUpdater = new BluetoothSummaryUpdater(mContext, this, mBluetoothManager);
        }

        @Override
        public void setListening(boolean listening) {
            mSummaryUpdater.register(listening);
        }

        @Override
        public void onSummaryChanged(String summary) {
            if (mSummaryLoader != null) {
                mSummaryLoader.setSummary(this, summary);
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                SummaryLoader summaryLoader) {

            return new SummaryProvider(activity, summaryLoader, Utils.getLocalBtManager(activity));
        }
    };

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                        boolean enabled) {

                    final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();

                    final Resources res = context.getResources();

                    // Add fragment title
                    SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = res.getString(R.string.bluetooth_settings);
                    data.screenTitle = res.getString(R.string.bluetooth_settings);
                    data.key = DATA_KEY_REFERENCE;
                    result.add(data);

                    // Removed paired bluetooth device indexing. See BluetoothSettingsObsolete.java.
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    if (!FeatureFactory.getFactory(context).getBluetoothFeatureProvider(
                            context).isPairingPageEnabled()) {
                        keys.add(DATA_KEY_REFERENCE);
                    }
                    return keys;
                }
            };
}
