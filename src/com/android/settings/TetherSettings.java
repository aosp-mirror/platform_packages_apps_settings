/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import static android.net.ConnectivityManager.TETHERING_BLUETOOTH;
import static android.net.ConnectivityManager.TETHERING_USB;
import static android.net.TetheringManager.TETHERING_ETHERNET;

import static com.android.settingslib.RestrictedLockUtilsInternal.checkIfUsbDataSignalingIsDisabled;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.EthernetManager;
import android.net.TetheringManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.FeatureFlags;
import com.android.settings.datausage.DataSaverBackend;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.wifi.tether.WifiTetherPreferenceController;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.TetherUtil;
import com.android.settingslib.search.SearchIndexable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/*
 * Displays preferences for Tethering.
 */
@SearchIndexable
public class TetherSettings extends RestrictedSettingsFragment
        implements DataSaverBackend.Listener {

    @VisibleForTesting
    static final String KEY_TETHER_PREFS_SCREEN = "tether_prefs_screen";
    @VisibleForTesting
    static final String KEY_WIFI_TETHER = "wifi_tether";
    @VisibleForTesting
    static final String KEY_USB_TETHER_SETTINGS = "usb_tether_settings";
    @VisibleForTesting
    static final String KEY_ENABLE_BLUETOOTH_TETHERING = "enable_bluetooth_tethering";
    private static final String KEY_ENABLE_ETHERNET_TETHERING = "enable_ethernet_tethering";
    private static final String KEY_DATA_SAVER_FOOTER = "disabled_on_data_saver";
    @VisibleForTesting
    static final String KEY_TETHER_PREFS_TOP_INTRO = "tether_prefs_top_intro";

    private static final String TAG = "TetheringSettings";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private RestrictedSwitchPreference mUsbTether;

    private SwitchPreference mBluetoothTether;

    private SwitchPreference mEthernetTether;

    private BroadcastReceiver mTetherChangeReceiver;

    private String[] mBluetoothRegexs;
    private String mEthernetRegex;
    private AtomicReference<BluetoothPan> mBluetoothPan = new AtomicReference<>();

    private Handler mHandler = new Handler();
    private OnStartTetheringCallback mStartTetheringCallback;
    private ConnectivityManager mCm;
    private EthernetManager mEm;
    private TetheringEventCallback mTetheringEventCallback;
    private EthernetListener mEthernetListener;

    private WifiTetherPreferenceController mWifiTetherPreferenceController;

    private boolean mUsbConnected;
    private boolean mMassStorageActive;

    private boolean mBluetoothEnableForTether;
    private boolean mUnavailable;

    private DataSaverBackend mDataSaverBackend;
    private boolean mDataSaverEnabled;
    private Preference mDataSaverFooter;

    @VisibleForTesting
    String[] mUsbRegexs;
    @VisibleForTesting
    Context mContext;
    @VisibleForTesting
    TetheringManager mTm;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TETHER;
    }

    public TetherSettings() {
        super(UserManager.DISALLOW_CONFIG_TETHERING);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mWifiTetherPreferenceController =
                new WifiTetherPreferenceController(context, getSettingsLifecycle());
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.tether_prefs);
        mContext = getContext();
        mDataSaverBackend = new DataSaverBackend(mContext);
        mDataSaverEnabled = mDataSaverBackend.isDataSaverEnabled();
        mDataSaverFooter = findPreference(KEY_DATA_SAVER_FOOTER);

        setIfOnlyAvailableForAdmins(true);
        if (isUiRestricted()) {
            mUnavailable = true;
            getPreferenceScreen().removeAll();
            return;
        }

        final Activity activity = getActivity();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(activity.getApplicationContext(), mProfileServiceListener,
                    BluetoothProfile.PAN);
        }

        setupTetherPreference();
        setTopIntroPreferenceTitle();

        mDataSaverBackend.addListener(this);

        mCm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mEm = (EthernetManager) getSystemService(Context.ETHERNET_SERVICE);
        mTm = (TetheringManager) getSystemService(Context.TETHERING_SERVICE);

        mUsbRegexs = mTm.getTetherableUsbRegexs();
        mBluetoothRegexs = mTm.getTetherableBluetoothRegexs();
        mEthernetRegex = mContext.getResources().getString(
                com.android.internal.R.string.config_ethernet_iface_regex);

        final boolean usbAvailable = mUsbRegexs.length != 0;
        final boolean bluetoothAvailable = adapter != null && mBluetoothRegexs.length != 0;
        final boolean ethernetAvailable = !TextUtils.isEmpty(mEthernetRegex);

        if (!usbAvailable || Utils.isMonkeyRunning()) {
            getPreferenceScreen().removePreference(mUsbTether);
        }

        mWifiTetherPreferenceController.displayPreference(getPreferenceScreen());

        if (!bluetoothAvailable) {
            getPreferenceScreen().removePreference(mBluetoothTether);
        } else {
            BluetoothPan pan = mBluetoothPan.get();
            if (pan != null && pan.isTetheringOn()) {
                mBluetoothTether.setChecked(true);
            } else {
                mBluetoothTether.setChecked(false);
            }
        }
        if (!ethernetAvailable) getPreferenceScreen().removePreference(mEthernetTether);
        // Set initial state based on Data Saver mode.
        onDataSaverChanged(mDataSaverBackend.isDataSaverEnabled());
    }

    @Override
    public void onDestroy() {
        mDataSaverBackend.remListener(this);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothProfile profile = mBluetoothPan.getAndSet(null);
        if (profile != null && adapter != null) {
            adapter.closeProfileProxy(BluetoothProfile.PAN, profile);
        }

        super.onDestroy();
    }

    @VisibleForTesting
    void setupTetherPreference() {
        mUsbTether = (RestrictedSwitchPreference) findPreference(KEY_USB_TETHER_SETTINGS);
        mBluetoothTether = (SwitchPreference) findPreference(KEY_ENABLE_BLUETOOTH_TETHERING);
        mEthernetTether = (SwitchPreference) findPreference(KEY_ENABLE_ETHERNET_TETHERING);
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        mDataSaverEnabled = isDataSaving;
        mUsbTether.setEnabled(!mDataSaverEnabled);
        mBluetoothTether.setEnabled(!mDataSaverEnabled);
        mEthernetTether.setEnabled(!mDataSaverEnabled);
        mDataSaverFooter.setVisible(mDataSaverEnabled);
    }

    @Override
    public void onAllowlistStatusChanged(int uid, boolean isAllowlisted) {
    }

    @Override
    public void onDenylistStatusChanged(int uid, boolean isDenylisted)  {
    }

    @VisibleForTesting
    void setTopIntroPreferenceTitle() {
        final Preference topIntroPreference = findPreference(KEY_TETHER_PREFS_TOP_INTRO);
        final WifiManager wifiManager = mContext.getSystemService(WifiManager.class);
        if (wifiManager.isStaApConcurrencySupported()) {
            topIntroPreference.setTitle(R.string.tethering_footer_info_sta_ap_concurrency);
        } else {
            topIntroPreference.setTitle(R.string.tethering_footer_info);
        }
    }

    private class TetherChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            if (DEBUG) {
                Log.d(TAG, "onReceive() action : " + action);
            }
            // TODO(b/194961339): Stop using ACTION_TETHER_STATE_CHANGED and use
            //  mTetheringEventCallback instead.
            if (action.equals(TetheringManager.ACTION_TETHER_STATE_CHANGED)) {
                // TODO - this should understand the interface types
                ArrayList<String> available = intent.getStringArrayListExtra(
                        TetheringManager.EXTRA_AVAILABLE_TETHER);
                ArrayList<String> active = intent.getStringArrayListExtra(
                        TetheringManager.EXTRA_ACTIVE_TETHER);
                updateBluetoothState();
                updateEthernetState(available.toArray(new String[available.size()]),
                        active.toArray(new String[active.size()]));
            } else if (action.equals(Intent.ACTION_MEDIA_SHARED)) {
                mMassStorageActive = true;
                updateBluetoothAndEthernetState();
                updateUsbPreference();
            } else if (action.equals(Intent.ACTION_MEDIA_UNSHARED)) {
                mMassStorageActive = false;
                updateBluetoothAndEthernetState();
                updateUsbPreference();
            } else if (action.equals(UsbManager.ACTION_USB_STATE)) {
                mUsbConnected = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);
                updateBluetoothAndEthernetState();
                updateUsbPreference();
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                if (mBluetoothEnableForTether) {
                    switch (intent
                            .getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        case BluetoothAdapter.STATE_ON:
                            startTethering(TETHERING_BLUETOOTH);
                            mBluetoothEnableForTether = false;
                            break;

                        case BluetoothAdapter.STATE_OFF:
                        case BluetoothAdapter.ERROR:
                            mBluetoothEnableForTether = false;
                            break;

                        default:
                            // ignore transition states
                    }
                }
                updateBluetoothAndEthernetState();
            } else if (action.equals(BluetoothPan.ACTION_TETHERING_STATE_CHANGED)) {
                updateBluetoothAndEthernetState();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mUnavailable) {
            if (!isUiRestrictedByOnlyAdmin()) {
                getEmptyTextView().setText(R.string.tethering_settings_not_available);
            }
            getPreferenceScreen().removeAll();
            return;
        }


        mStartTetheringCallback = new OnStartTetheringCallback(this);
        mTetheringEventCallback = new TetheringEventCallback();
        mTm.registerTetheringEventCallback(r -> mHandler.post(r), mTetheringEventCallback);

        mMassStorageActive = Environment.MEDIA_SHARED.equals(Environment.getExternalStorageState());
        registerReceiver();

        mEthernetListener = new EthernetListener();
        if (mEm != null)
            mEm.addListener(mEthernetListener, r -> mHandler.post(r));

        updateUsbState();
        updateBluetoothAndEthernetState();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mUnavailable) {
            return;
        }
        getActivity().unregisterReceiver(mTetherChangeReceiver);
        mTm.unregisterTetheringEventCallback(mTetheringEventCallback);
        if (mEm != null)
            mEm.removeListener(mEthernetListener);
        mTetherChangeReceiver = null;
        mStartTetheringCallback = null;
        mTetheringEventCallback = null;
        mEthernetListener = null;
    }

    @VisibleForTesting
    void registerReceiver() {
        final Activity activity = getActivity();

        mTetherChangeReceiver = new TetherChangeReceiver();
        IntentFilter filter = new IntentFilter(TetheringManager.ACTION_TETHER_STATE_CHANGED);
        final Intent intent = activity.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_STATE);
        activity.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_SHARED);
        filter.addAction(Intent.ACTION_MEDIA_UNSHARED);
        filter.addDataScheme("file");
        activity.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothPan.ACTION_TETHERING_STATE_CHANGED);
        activity.registerReceiver(mTetherChangeReceiver, filter);

        if (intent != null) mTetherChangeReceiver.onReceive(activity, intent);
    }

    // TODO(b/194961339): Separate the updateBluetoothAndEthernetState() to two methods,
    //  updateBluetoothAndEthernetState() and updateBluetoothAndEthernetPreference().
    //  Because we should update the state when only receiving tethering
    //  state changes and update preference when usb or media share changed.
    private void updateBluetoothAndEthernetState() {
        String[] tethered = mTm.getTetheredIfaces();
        updateBluetoothAndEthernetState(tethered);
    }

    private void updateBluetoothAndEthernetState(String[] tethered) {
        String[] available = mTm.getTetherableIfaces();
        updateBluetoothState();
        updateEthernetState(available, tethered);
    }

    private void updateUsbState() {
        String[] tethered = mTm.getTetheredIfaces();
        updateUsbState(tethered);
    }

    @VisibleForTesting
    void updateUsbState(String[] tethered) {
        boolean usbTethered = false;
        for (String s : tethered) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) usbTethered = true;
            }
        }
        if (DEBUG) {
            Log.d(TAG, "updateUsbState() mUsbConnected : " + mUsbConnected
                    + ", mMassStorageActive : " + mMassStorageActive
                    + ", usbTethered : " + usbTethered);
        }
        if (usbTethered) {
            mUsbTether.setEnabled(!mDataSaverEnabled);
            mUsbTether.setChecked(true);
            mUsbTether.setDisabledByAdmin(
                    checkIfUsbDataSignalingIsDisabled(mContext, UserHandle.myUserId()));
        } else {
            mUsbTether.setChecked(false);
            updateUsbPreference();
        }
    }

    private void updateUsbPreference() {
        boolean usbAvailable = mUsbConnected && !mMassStorageActive;
        final RestrictedLockUtils.EnforcedAdmin enforcedAdmin =
                checkIfUsbDataSignalingIsDisabled(mContext, UserHandle.myUserId());

        if (enforcedAdmin != null) {
            mUsbTether.setDisabledByAdmin(enforcedAdmin);
        } else if (usbAvailable) {
            mUsbTether.setEnabled(!mDataSaverEnabled);
        } else {
            mUsbTether.setEnabled(false);
        }
    }

    @VisibleForTesting
    int getBluetoothState() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return BluetoothAdapter.ERROR;
        }
        return adapter.getState();
    }

    @VisibleForTesting
    boolean isBluetoothTetheringOn() {
        final BluetoothPan bluetoothPan = mBluetoothPan.get();
        return bluetoothPan != null && bluetoothPan.isTetheringOn();
    }

    private void updateBluetoothState() {
        final int btState = getBluetoothState();
        if (DEBUG) {
            Log.d(TAG, "updateBluetoothState() btState : " + btState);
        }
        if (btState == BluetoothAdapter.ERROR) {
            Log.w(TAG, "updateBluetoothState() Bluetooth state is error!");
            return;
        }

        if (btState == BluetoothAdapter.STATE_TURNING_OFF) {
            mBluetoothTether.setEnabled(false);
        } else if (btState == BluetoothAdapter.STATE_TURNING_ON) {
            mBluetoothTether.setEnabled(false);
        } else {
            if (btState == BluetoothAdapter.STATE_ON && isBluetoothTetheringOn()) {
                mBluetoothTether.setChecked(true);
                mBluetoothTether.setEnabled(!mDataSaverEnabled);
            } else {
                mBluetoothTether.setEnabled(!mDataSaverEnabled);
                mBluetoothTether.setChecked(false);
            }
        }
    }

    @VisibleForTesting
    void updateEthernetState(String[] available, String[] tethered) {
        boolean isAvailable = false;
        boolean isTethered = false;

        for (String s : available) {
            if (s.matches(mEthernetRegex)) isAvailable = true;
        }

        for (String s : tethered) {
            if (s.matches(mEthernetRegex)) isTethered = true;
        }

        if (DEBUG) {
            Log.d(TAG, "updateEthernetState() isAvailable : " + isAvailable
                    + ", isTethered : " + isTethered);
        }

        if (isTethered) {
            mEthernetTether.setEnabled(!mDataSaverEnabled);
            mEthernetTether.setChecked(true);
        } else if (isAvailable || (mEm != null && mEm.isAvailable())) {
            mEthernetTether.setEnabled(!mDataSaverEnabled);
            mEthernetTether.setChecked(false);
        } else {
            mEthernetTether.setEnabled(false);
            mEthernetTether.setChecked(false);
        }
    }

    private void startTethering(int choice) {
        if (choice == TETHERING_BLUETOOTH) {
            // Turn on Bluetooth first.
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter.getState() == BluetoothAdapter.STATE_OFF) {
                mBluetoothEnableForTether = true;
                adapter.enable();
                mBluetoothTether.setEnabled(false);
                return;
            }
        }

        mCm.startTethering(choice, true, mStartTetheringCallback, mHandler);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mUsbTether) {
            if (mUsbTether.isChecked()) {
                startTethering(TETHERING_USB);
            } else {
                mCm.stopTethering(TETHERING_USB);
            }
        } else if (preference == mBluetoothTether) {
            if (mBluetoothTether.isChecked()) {
                startTethering(TETHERING_BLUETOOTH);
            } else {
                mCm.stopTethering(TETHERING_BLUETOOTH);
            }
        } else if (preference == mEthernetTether) {
            if (mEthernetTether.isChecked()) {
                startTethering(TETHERING_ETHERNET);
            } else {
                mCm.stopTethering(TETHERING_ETHERNET);
            }
        }

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_tether;
    }

    private BluetoothProfile.ServiceListener mProfileServiceListener =
            new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            mBluetoothPan.set((BluetoothPan) proxy);
        }
        public void onServiceDisconnected(int profile) {
            mBluetoothPan.set(null);
        }
    };

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.tether_prefs;
                    return Arrays.asList(sir);
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return !FeatureFlagUtils.isEnabled(context, FeatureFlags.TETHER_ALL_IN_ONE);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    final TetheringManager tm =
                            context.getSystemService(TetheringManager.class);

                    if (!TetherUtil.isTetherAvailable(context)) {
                        keys.add(KEY_TETHER_PREFS_SCREEN);
                        keys.add(KEY_WIFI_TETHER);
                    }

                    final boolean usbAvailable =
                            tm.getTetherableUsbRegexs().length != 0;
                    if (!usbAvailable || Utils.isMonkeyRunning()) {
                        keys.add(KEY_USB_TETHER_SETTINGS);
                    }

                    final boolean bluetoothAvailable =
                            tm.getTetherableBluetoothRegexs().length != 0;
                    if (!bluetoothAvailable) {
                        keys.add(KEY_ENABLE_BLUETOOTH_TETHERING);
                    }

                    final boolean ethernetAvailable = !TextUtils.isEmpty(
                            context.getResources().getString(
                                    com.android.internal.R.string.config_ethernet_iface_regex));
                    if (!ethernetAvailable) {
                        keys.add(KEY_ENABLE_ETHERNET_TETHERING);
                    }
                    return keys;
                }
    };

    private static final class OnStartTetheringCallback extends
            ConnectivityManager.OnStartTetheringCallback {
        final WeakReference<TetherSettings> mTetherSettings;

        OnStartTetheringCallback(TetherSettings settings) {
            mTetherSettings = new WeakReference<>(settings);
        }

        @Override
        public void onTetheringStarted() {
            update();
        }

        @Override
        public void onTetheringFailed() {
            update();
        }

        private void update() {
            TetherSettings settings = mTetherSettings.get();
            if (settings != null) {
                settings.updateBluetoothAndEthernetState();
            }
        }
    }

    private final class TetheringEventCallback implements TetheringManager.TetheringEventCallback {
        @Override
        public void onTetheredInterfacesChanged(List<String> interfaces) {
            Log.d(TAG, "onTetheredInterfacesChanged() interfaces : " + interfaces.toString());
            String[] tethered = interfaces.toArray(new String[interfaces.size()]);
            updateUsbState(tethered);
            updateBluetoothAndEthernetState(tethered);
        }
    }

    private final class EthernetListener implements EthernetManager.Listener {
        public void onAvailabilityChanged(String iface, boolean isAvailable) {
            mHandler.post(() -> updateBluetoothAndEthernetState());
        }
    }
}
