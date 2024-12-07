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

package com.android.settings.network.tether;

import static android.net.ConnectivityManager.TETHERING_BLUETOOTH;
import static android.net.ConnectivityManager.TETHERING_USB;
import static android.net.TetheringManager.TETHERING_ETHERNET;

import static com.android.settings.wifi.WifiUtils.canShowWifiHotspot;
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
import android.net.IpConfiguration;
import android.net.TetheringManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.RestrictedDashboardFragment;
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
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

// LINT.IfChange
/**
 * Displays preferences for Tethering.
 */
@SearchIndexable
public class TetherSettings extends RestrictedDashboardFragment
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

    @VisibleForTesting
    RestrictedSwitchPreference mUsbTether;
    @VisibleForTesting
    TwoStatePreference mBluetoothTether;
    @VisibleForTesting
    TwoStatePreference mEthernetTether;

    private BroadcastReceiver mTetherChangeReceiver;
    private BroadcastReceiver mBluetoothStateReceiver;

    private String[] mBluetoothRegexs;
    private AtomicReference<BluetoothPan> mBluetoothPan = new AtomicReference<>();

    private Handler mHandler = new Handler();
    private OnStartTetheringCallback mStartTetheringCallback;
    private ConnectivityManager mCm;
    private EthernetManager mEm;
    private EthernetListener mEthernetListener;
    private final HashSet<String> mAvailableInterfaces = new HashSet<>();

    @VisibleForTesting
    WifiTetherPreferenceController mWifiTetherPreferenceController;

    private boolean mUsbConnected;
    private boolean mMassStorageActive;

    private boolean mBluetoothEnableForTether;

    private DataSaverBackend mDataSaverBackend;
    private boolean mDataSaverEnabled;
    @VisibleForTesting
    Preference mDataSaverFooter;

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
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.tether_prefs;
    }

    @SuppressWarnings("NullAway")
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setIfOnlyAvailableForAdmins(true);
        if (isUiRestricted()) {
            return;
        }

        mContext = getContext();
        mDataSaverBackend = new DataSaverBackend(mContext);
        mDataSaverEnabled = mDataSaverBackend.isDataSaverEnabled();
        mDataSaverFooter = findPreference(KEY_DATA_SAVER_FOOTER);

        setupTetherPreference();
        setupViewModel();

        final Activity activity = getActivity();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(activity.getApplicationContext(), mProfileServiceListener,
                    BluetoothProfile.PAN);
        }
        if (mBluetoothStateReceiver == null) {
            mBluetoothStateReceiver = new BluetoothStateReceiver();
            mContext.registerReceiver(
                    mBluetoothStateReceiver,
                    new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        }

        setTopIntroPreferenceTitle();

        mDataSaverBackend.addListener(this);

        mCm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // Some devices do not have available EthernetManager. In that case getSystemService will
        // return null.
        mEm = mContext.getSystemService(EthernetManager.class);

        mUsbRegexs = mTm.getTetherableUsbRegexs();
        mBluetoothRegexs = mTm.getTetherableBluetoothRegexs();

        final boolean usbAvailable = mUsbRegexs.length != 0;
        final boolean bluetoothAvailable = adapter != null && mBluetoothRegexs.length != 0;
        final boolean ethernetAvailable = (mEm != null);

        if (!usbAvailable || Utils.isMonkeyRunning()) {
            getPreferenceScreen().removePreference(mUsbTether);
        }

        mWifiTetherPreferenceController.displayPreference(getPreferenceScreen());

        if (!isCatalystEnabled()) {
            if (!bluetoothAvailable) {
                mBluetoothTether.setVisible(false);
            } else {
                BluetoothPan pan = mBluetoothPan.get();
                if (pan != null && pan.isTetheringOn()) {
                    mBluetoothTether.setChecked(true);
                } else {
                    mBluetoothTether.setChecked(false);
                }
            }
        }

        if (!ethernetAvailable) getPreferenceScreen().removePreference(mEthernetTether);
        // Set initial state based on Data Saver mode.
        onDataSaverChanged(mDataSaverBackend.isDataSaverEnabled());
    }

    @VisibleForTesting
    void setupViewModel() {
        TetheringManagerModel model = new ViewModelProvider(this).get(TetheringManagerModel.class);
        mWifiTetherPreferenceController =
                new WifiTetherPreferenceController(getContext(), getSettingsLifecycle(), model);
        mTm = model.getTetheringManager();
        model.getTetheredInterfaces().observe(this, this::onTetheredInterfacesChanged);
    }

    @Override
    public void onDestroy() {
        if (isUiRestricted()) {
            super.onDestroy();
            return;
        }

        mDataSaverBackend.remListener(this);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothProfile profile = mBluetoothPan.getAndSet(null);
        if (profile != null && adapter != null) {
            adapter.closeProfileProxy(BluetoothProfile.PAN, profile);
        }
        if (mBluetoothStateReceiver != null) {
            mContext.unregisterReceiver(mBluetoothStateReceiver);
            mBluetoothStateReceiver = null;
        }

        super.onDestroy();
    }

    @VisibleForTesting
    void setupTetherPreference() {
        mUsbTether = (RestrictedSwitchPreference) findPreference(KEY_USB_TETHER_SETTINGS);
        mBluetoothTether = (TwoStatePreference) findPreference(KEY_ENABLE_BLUETOOTH_TETHERING);
        mEthernetTether = (TwoStatePreference) findPreference(KEY_ENABLE_ETHERNET_TETHERING);
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        mDataSaverEnabled = isDataSaving;
        mWifiTetherPreferenceController.setDataSaverEnabled(mDataSaverEnabled);
        mUsbTether.setEnabled(!mDataSaverEnabled);
        if (!isCatalystEnabled()) {
            mBluetoothTether.setEnabled(!mDataSaverEnabled);
        }
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

    private class BluetoothStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.i(TAG, "onReceive: action: " + action);

            if (TextUtils.equals(action, BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state =
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                Log.i(TAG, "onReceive: state: " + BluetoothAdapter.nameForState(state));
                final BluetoothProfile profile = mBluetoothPan.get();
                switch(state) {
                    case BluetoothAdapter.STATE_ON:
                        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                        if (profile == null && adapter != null) {
                            adapter.getProfileProxy(mContext, mProfileServiceListener,
                                    BluetoothProfile.PAN);
                        }
                        break;
                }
            }
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

        if (isUiRestricted()) {
            if (!isUiRestrictedByOnlyAdmin()) {
                getEmptyTextView().setText(
                        com.android.settingslib.R.string.tethering_settings_not_available);
            }
            getPreferenceScreen().removeAll();
            return;
        }


        mStartTetheringCallback = new OnStartTetheringCallback(this);

        mMassStorageActive = Environment.MEDIA_SHARED.equals(Environment.getExternalStorageState());
        registerReceiver();

        mEthernetListener = new EthernetListener(this);
        if (mEm != null) {
            mEm.addInterfaceStateListener(mContext.getApplicationContext().getMainExecutor(),
                    mEthernetListener);
        }

        updateUsbState();
        updateBluetoothAndEthernetState();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (isUiRestricted()) {
            return;
        }
        getActivity().unregisterReceiver(mTetherChangeReceiver);
        if (mEm != null) {
            mEm.removeInterfaceStateListener(mEthernetListener);
        }
        mTetherChangeReceiver = null;
        mStartTetheringCallback = null;
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
            final RestrictedLockUtils.EnforcedAdmin enforcedAdmin =
                    checkIfUsbDataSignalingIsDisabled(mContext, UserHandle.myUserId());
            if (enforcedAdmin != null) {
                mUsbTether.setDisabledByAdmin(enforcedAdmin);
            }
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
        if (isCatalystEnabled()) return;

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
            if (mAvailableInterfaces.contains(s)) isAvailable = true;
        }

        for (String s : tethered) {
            if (mAvailableInterfaces.contains(s)) isTethered = true;
        }

        if (DEBUG) {
            Log.d(TAG, "updateEthernetState() isAvailable : " + isAvailable
                    + ", isTethered : " + isTethered);
        }

        if (isTethered) {
            mEthernetTether.setEnabled(!mDataSaverEnabled);
            mEthernetTether.setChecked(true);
        } else if (mAvailableInterfaces.size() > 0) {
            mEthernetTether.setEnabled(!mDataSaverEnabled);
            mEthernetTether.setChecked(false);
        } else {
            mEthernetTether.setEnabled(false);
            mEthernetTether.setChecked(false);
        }
    }

    private void startTethering(int choice) {
        if (choice == TETHERING_BLUETOOTH && !isCatalystEnabled()) {
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
        } else if (preference == mBluetoothTether && !isCatalystEnabled()) {
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
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    if (mBluetoothPan.get() == null) {
                        mBluetoothPan.set((BluetoothPan) proxy);
                        updateBluetoothState();
                    }
                }

                @Override
                public void onServiceDisconnected(int profile) { /* Do nothing */ }
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
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    final TetheringManager tm =
                            context.getSystemService(TetheringManager.class);

                    if (!TetherUtil.isTetherAvailable(context)) {
                        keys.add(KEY_TETHER_PREFS_SCREEN);
                    }

                    if (!canShowWifiHotspot(context) || !TetherUtil.isTetherAvailable(context)) {
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

                    final EthernetManager em =
                            context.getSystemService(EthernetManager.class);
                    final boolean ethernetAvailable = (em != null);
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

    protected void onTetheredInterfacesChanged(List<String> interfaces) {
        Log.d(TAG, "onTetheredInterfacesChanged() interfaces : " + interfaces.toString());
        final String[] tethered = interfaces.toArray(new String[interfaces.size()]);
        updateUsbState(tethered);
        updateBluetoothAndEthernetState(tethered);
    }

    private static final class EthernetListener implements EthernetManager.InterfaceStateListener {
        final WeakReference<TetherSettings> mTetherSettings;

        EthernetListener(TetherSettings settings) {
            mTetherSettings = new WeakReference<>(settings);
        }

        @Override
        public void onInterfaceStateChanged(@NonNull String iface, int state, int role,
                @NonNull IpConfiguration configuration) {
            final TetherSettings tetherSettings = mTetherSettings.get();
            if (tetherSettings == null) {
                return;
            }
            tetherSettings.onInterfaceStateChanged(iface, state, role, configuration);
        }
    }

    void onInterfaceStateChanged(@NonNull String iface, int state, int role,
            @NonNull IpConfiguration configuration) {
        if (state == EthernetManager.STATE_LINK_UP) {
            mAvailableInterfaces.add(iface);
        } else {
            mAvailableInterfaces.remove(iface);
        }
        updateBluetoothAndEthernetState();
    }

    @Override
    public @Nullable String getPreferenceScreenBindingKey(@NonNull Context context) {
        return TetherScreen.KEY;
    }
}
// LINT.ThenChange(BluetoothTetherSwitchPreference.kt)
