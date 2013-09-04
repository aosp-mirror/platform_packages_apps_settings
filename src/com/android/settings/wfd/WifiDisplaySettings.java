/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.settings.wfd;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplay;
import android.hardware.display.WifiDisplayStatus;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.WpsInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.Html;
import android.util.Slog;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.ProgressCategory;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/**
 * The Settings screen for WifiDisplay configuration and connection management.
 */
public final class WifiDisplaySettings extends SettingsPreferenceFragment {
    private static final String TAG = "WifiDisplaySettings";
    private static final boolean DEBUG = false;

    private static final int MENU_ID_SCAN = Menu.FIRST;

    private DisplayManager mDisplayManager;

    private boolean mWifiDisplayOnSetting;
    private WifiDisplayStatus mWifiDisplayStatus;

    private PreferenceGroup mPairedDevicesCategory;
    private ProgressCategory mAvailableDevicesCategory;

    private TextView mEmptyView;

    private Switch mActionBarSwitch;

    /* certification */
    private boolean mWifiDisplayCertificationOn;
    private WifiP2pManager mWifiP2pManager;
    private Channel mWifiP2pChannel;
    private PreferenceGroup mCertCategory;
    private boolean mListen;
    private boolean mAutoGO;
    private int mWpsConfig = WpsInfo.INVALID;
    private int mListenChannel;
    private int mOperatingChannel;

    public WifiDisplaySettings() {
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mDisplayManager = (DisplayManager)getActivity().getSystemService(Context.DISPLAY_SERVICE);
        mWifiP2pManager = (WifiP2pManager)getActivity().getSystemService(Context.WIFI_P2P_SERVICE);
        mWifiP2pChannel = mWifiP2pManager.initialize(getActivity(), Looper.getMainLooper(), null);

        addPreferencesFromResource(R.xml.wifi_display_settings);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();
        mActionBarSwitch = new Switch(activity);
        mActionBarSwitch.setOnCheckedChangeListener(mSwitchOnCheckedChangedListener);

        final int padding = activity.getResources().getDimensionPixelSize(
                R.dimen.action_bar_switch_padding);
        mActionBarSwitch.setPaddingRelative(0, 0, padding, 0);
        activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(mActionBarSwitch,
                new ActionBar.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.END));

        mEmptyView = (TextView) getView().findViewById(android.R.id.empty);
        getListView().setEmptyView(mEmptyView);

        update();

        if (mWifiDisplayStatus.getFeatureState() == WifiDisplayStatus.FEATURE_STATE_UNAVAILABLE) {
            activity.finish();
        }
    }

    @Override
    public void onDestroyView() {
        getActivity().getActionBar().setCustomView(null);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();

        Context context = getActivity();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DisplayManager.ACTION_WIFI_DISPLAY_STATUS_CHANGED);
        context.registerReceiver(mReceiver, filter);

        getContentResolver().registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.WIFI_DISPLAY_ON), false, mSettingsObserver);
        getContentResolver().registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.WIFI_DISPLAY_CERTIFICATION_ON), false, mSettingsObserver);
        getContentResolver().registerContentObserver(Settings.Global.getUriFor(
            Settings.Global.WIFI_DISPLAY_WPS_CONFIG), false, mSettingsObserver);

        mDisplayManager.scanWifiDisplays();

        update();
    }

    @Override
    public void onPause() {
        super.onPause();

        Context context = getActivity();
        context.unregisterReceiver(mReceiver);

        getContentResolver().unregisterContentObserver(mSettingsObserver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.add(Menu.NONE, MENU_ID_SCAN, 0,
                mWifiDisplayStatus.getScanState() == WifiDisplayStatus.SCAN_STATE_SCANNING ?
                        R.string.wifi_display_searching_for_devices :
                                R.string.wifi_display_search_for_devices);
        item.setEnabled(mWifiDisplayStatus.getFeatureState() == WifiDisplayStatus.FEATURE_STATE_ON
                && mWifiDisplayStatus.getScanState() == WifiDisplayStatus.SCAN_STATE_NOT_SCANNING);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_SCAN:
                if (mWifiDisplayStatus.getFeatureState() == WifiDisplayStatus.FEATURE_STATE_ON) {
                    mDisplayManager.scanWifiDisplays();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference instanceof WifiDisplayPreference) {
            WifiDisplayPreference p = (WifiDisplayPreference)preference;
            WifiDisplay display = p.getDisplay();

            if (display.equals(mWifiDisplayStatus.getActiveDisplay())) {
                showDisconnectDialog(display);
            } else if (display.canConnect()){
                mDisplayManager.connectWifiDisplay(display.getDeviceAddress());
            }
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void update() {
        mWifiDisplayOnSetting = Settings.Global.getInt(getContentResolver(),
                Settings.Global.WIFI_DISPLAY_ON, 0) != 0;
        mWifiDisplayCertificationOn = Settings.Global.getInt(getContentResolver(),
                Settings.Global.WIFI_DISPLAY_CERTIFICATION_ON, 0) != 0;
        mWpsConfig = Settings.Global.getInt(getContentResolver(),
            Settings.Global.WIFI_DISPLAY_WPS_CONFIG, WpsInfo.INVALID);
        mWifiDisplayStatus = mDisplayManager.getWifiDisplayStatus();

        applyState();
    }

    private void applyState() {
        final int featureState = mWifiDisplayStatus.getFeatureState();
        mActionBarSwitch.setEnabled(featureState != WifiDisplayStatus.FEATURE_STATE_DISABLED);
        mActionBarSwitch.setChecked(mWifiDisplayOnSetting);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();

        if (featureState == WifiDisplayStatus.FEATURE_STATE_ON) {
            final WifiDisplay[] displays = mWifiDisplayStatus.getDisplays();

            if (mWifiDisplayCertificationOn) {
                buildCertificationMenu(preferenceScreen);
            }

            if (mPairedDevicesCategory == null) {
                mPairedDevicesCategory = new PreferenceCategory(getActivity());
                mPairedDevicesCategory.setTitle(R.string.wifi_display_paired_devices);
            } else {
                mPairedDevicesCategory.removeAll();
            }
            preferenceScreen.addPreference(mPairedDevicesCategory);

            if (mAvailableDevicesCategory == null) {
                mAvailableDevicesCategory = new ProgressCategory(getActivity(), null,
                        R.string.wifi_display_no_devices_found);
                mAvailableDevicesCategory.setTitle(R.string.wifi_display_available_devices);
            } else {
                mAvailableDevicesCategory.removeAll();
            }
            preferenceScreen.addPreference(mAvailableDevicesCategory);

            for (WifiDisplay d : displays) {
                if (d.isRemembered()) {
                    mPairedDevicesCategory.addPreference(createWifiDisplayPreference(d));
                } else if (d.isAvailable()){
                    mAvailableDevicesCategory.addPreference(createWifiDisplayPreference(d));
                }
            }
            if (mPairedDevicesCategory.getPreferenceCount() == 0) {
                preferenceScreen.removePreference(mPairedDevicesCategory);
            }
            if (mWifiDisplayStatus.getScanState() == WifiDisplayStatus.SCAN_STATE_SCANNING) {
                mAvailableDevicesCategory.setProgress(true);
            } else {
                mAvailableDevicesCategory.setProgress(false);
            }
        } else {
            mEmptyView.setText(featureState == WifiDisplayStatus.FEATURE_STATE_OFF ?
                    R.string.wifi_display_settings_empty_list_wifi_display_off :
                            R.string.wifi_display_settings_empty_list_wifi_display_disabled);
        }

        getActivity().invalidateOptionsMenu();
    }

    private void buildCertificationMenu(final PreferenceScreen preferenceScreen) {
        if (mCertCategory == null) {
            mCertCategory = new PreferenceCategory(getActivity());
            mCertCategory.setTitle(R.string.wifi_display_certification_heading);
        } else {
            mCertCategory.removeAll();
        }
        preferenceScreen.addPreference(mCertCategory);

        // display session info if there is an active p2p session
        if (!mWifiDisplayStatus.getSessionInfo().getGroupId().isEmpty()) {
            Preference p = new Preference(getActivity());
            p.setTitle(R.string.wifi_display_session_info);
            p.setSummary(mWifiDisplayStatus.getSessionInfo().toString());
            mCertCategory.addPreference(p);

            // show buttons for Pause/Resume when a WFD session is established
            if (mWifiDisplayStatus.getSessionInfo().getSessionId() != 0) {
                mCertCategory.addPreference(new Preference(getActivity()) {
                    @Override
                    public View getView(View convertView, ViewGroup parent) {
                        final View v;
                        if (convertView == null) {
                            LayoutInflater li = (LayoutInflater) getActivity().
                                    getSystemService(Service.LAYOUT_INFLATER_SERVICE);
                            v = li.inflate(R.layout.two_buttons_panel, null);
                        } else {
                            v = convertView;
                        }

                        Button b = (Button)v.findViewById(R.id.left_button);
                        b.setText(R.string.wifi_display_pause);
                        b.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mDisplayManager.pauseWifiDisplay();
                            }
                        });

                        b = (Button)v.findViewById(R.id.right_button);
                        b.setText(R.string.wifi_display_resume);
                        b.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mDisplayManager.resumeWifiDisplay();
                            }
                        });

                        return v;
                    }
                });
            }
        }

        // switch for Listen Mode
        CheckBoxPreference cbp = new CheckBoxPreference(getActivity()) {
            @Override
            protected void onClick() {
                mListen = !mListen;
                setListenMode(mListen);
                setChecked(mListen);
            }
        };
        cbp.setTitle(R.string.wifi_display_listen_mode);
        cbp.setChecked(mListen);
        mCertCategory.addPreference(cbp);

        // switch for Autonomous GO
        cbp = new CheckBoxPreference(getActivity()) {
            @Override
            protected void onClick() {
                mAutoGO = !mAutoGO;
                if (mAutoGO) {
                    startAutoGO();
                } else {
                    stopAutoGO();
                }
                setChecked(mAutoGO);
            }
        };
        cbp.setTitle(R.string.wifi_display_autonomous_go);
        cbp.setChecked(mAutoGO);
        mCertCategory.addPreference(cbp);

        // Drop down list for choosing WPS method (PBC/KEYPAD/DISPLAY)
        ListPreference lp = new ListPreference(getActivity()) {
            @Override
            protected void onDialogClosed(boolean positiveResult) {
                super.onDialogClosed(positiveResult);
                if (positiveResult) {
                    mWpsConfig = Integer.parseInt(getValue());
                    setSummary("%1$s");
                    getActivity().invalidateOptionsMenu();
                    Settings.Global.putInt(getActivity().getContentResolver(),
                            Settings.Global.WIFI_DISPLAY_WPS_CONFIG, mWpsConfig);
                }
            }
        };
        mWpsConfig = Settings.Global.getInt(getActivity().getContentResolver(),
                Settings.Global.WIFI_DISPLAY_WPS_CONFIG, WpsInfo.INVALID);
        String[] wpsEntries = { "Default", "PBC", "KEYPAD", "DISPLAY" };
        String[] wpsValues = {
            "" + WpsInfo.INVALID,
            "" + WpsInfo.PBC,
            "" + WpsInfo.KEYPAD,
            "" + WpsInfo.DISPLAY };
        lp.setTitle(R.string.wifi_display_wps_config);
        lp.setEntries(wpsEntries);
        lp.setEntryValues(wpsValues);
        lp.setValue("" + mWpsConfig);
        lp.setSummary("%1$s");
        mCertCategory.addPreference(lp);

        // Drop down list for choosing listen channel
        lp = new ListPreference(getActivity()) {
            @Override
            protected void onDialogClosed(boolean positiveResult) {
                super.onDialogClosed(positiveResult);
                if (positiveResult) {
                    mListenChannel = Integer.parseInt(getValue());
                    setSummary("%1$s");
                    getActivity().invalidateOptionsMenu();
                    setWifiP2pChannels(mListenChannel, mOperatingChannel);
                }
            }
        };
        String[] lcEntries = { "Auto", "1", "6", "11" };
        String[] lcValues = { "0", "1", "6", "11" };
        lp.setTitle(R.string.wifi_display_listen_channel);
        lp.setEntries(lcEntries);
        lp.setEntryValues(lcValues);
        lp.setValue("" + mListenChannel);
        lp.setSummary("%1$s");
        mCertCategory.addPreference(lp);

        // Drop down list for choosing operating channel
        lp = new ListPreference(getActivity()) {
            @Override
            protected void onDialogClosed(boolean positiveResult) {
                super.onDialogClosed(positiveResult);
                if (positiveResult) {
                    mOperatingChannel = Integer.parseInt(getValue());
                    setSummary("%1$s");
                    getActivity().invalidateOptionsMenu();
                    setWifiP2pChannels(mListenChannel, mOperatingChannel);
                }
            }
        };
        String[] ocEntries = { "Auto", "1", "6", "11", "36" };
        String[] ocValues = { "0", "1", "6", "11", "36" };
        lp.setTitle(R.string.wifi_display_operating_channel);
        lp.setEntries(ocEntries);
        lp.setEntryValues(ocValues);
        lp.setValue("" + mOperatingChannel);
        lp.setSummary("%1$s");
        mCertCategory.addPreference(lp);
    }

    private void startAutoGO() {
        if (DEBUG) {
            Slog.d(TAG, "Starting Autonomous GO...");
        }
        mWifiP2pManager.createGroup(mWifiP2pChannel, new ActionListener() {
            @Override
            public void onSuccess() {
                if (DEBUG) {
                    Slog.d(TAG, "Successfully started AutoGO.");
                }
            }

            @Override
            public void onFailure(int reason) {
                Slog.e(TAG, "Failed to start AutoGO with reason " + reason + ".");
            }
        });
    }

    private void stopAutoGO() {
        if (DEBUG) {
            Slog.d(TAG, "Stopping Autonomous GO...");
        }
        mWifiP2pManager.removeGroup(mWifiP2pChannel, new ActionListener() {
            @Override
            public void onSuccess() {
                if (DEBUG) {
                    Slog.d(TAG, "Successfully stopped AutoGO.");
                }
            }

            @Override
            public void onFailure(int reason) {
                Slog.e(TAG, "Failed to stop AutoGO with reason " + reason + ".");
            }
        });
    }

    private void setListenMode(final boolean enable) {
        if (DEBUG) {
            Slog.d(TAG, "Setting listen mode to: " + enable);
        }
        mWifiP2pManager.listen(mWifiP2pChannel, enable, new ActionListener() {
            @Override
            public void onSuccess() {
                if (DEBUG) {
                    Slog.d(TAG, "Successfully " + (enable ? "entered" : "exited")
                            +" listen mode.");
                }
            }

            @Override
            public void onFailure(int reason) {
                Slog.e(TAG, "Failed to " + (enable ? "entered" : "exited")
                        +" listen mode with reason " + reason + ".");
            }
        });
    }

    private void setWifiP2pChannels(final int lc, final int oc) {
        if (DEBUG) {
            Slog.d(TAG, "Setting wifi p2p channel: lc=" + lc + ", oc=" + oc);
        }
        mWifiP2pManager.setWifiP2pChannels(mWifiP2pChannel,
                lc, oc, new ActionListener() {
            @Override
            public void onSuccess() {
                if (DEBUG) {
                    Slog.d(TAG, "Successfully set wifi p2p channels.");
                }
            }

            @Override
            public void onFailure(int reason) {
                Slog.e(TAG, "Failed to set wifi p2p channels with reason " + reason + ".");
            }
        });
    }

    private Preference createWifiDisplayPreference(final WifiDisplay d) {
        WifiDisplayPreference p = new WifiDisplayPreference(getActivity(), d);
        if (d.equals(mWifiDisplayStatus.getActiveDisplay())) {
            switch (mWifiDisplayStatus.getActiveDisplayState()) {
                case WifiDisplayStatus.DISPLAY_STATE_CONNECTED:
                    p.setSummary(R.string.wifi_display_status_connected);
                    break;
                case WifiDisplayStatus.DISPLAY_STATE_CONNECTING:
                    p.setSummary(R.string.wifi_display_status_connecting);
                    break;
            }
        } else if (d.isAvailable()) {
            if (!d.canConnect()) {
                p.setSummary(R.string.wifi_display_status_in_use);
                p.setEnabled(false);
            } else if (d.isRemembered()) {
                p.setSummary(R.string.wifi_display_status_available);
            }
        }
        if (d.isRemembered()) {
            p.setWidgetLayoutResource(R.layout.wifi_display_preference);
        }
        return p;
    }

    private void showDisconnectDialog(final WifiDisplay display) {
        DialogInterface.OnClickListener ok = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (display.equals(mWifiDisplayStatus.getActiveDisplay())) {
                    mDisplayManager.disconnectWifiDisplay();
                }
            }
        };

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setCancelable(true)
                .setTitle(R.string.wifi_display_disconnect_title)
                .setMessage(Html.fromHtml(getResources().getString(
                        R.string.wifi_display_disconnect_text, display.getFriendlyDisplayName())))
                .setPositiveButton(android.R.string.ok, ok)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
    }

    private void showOptionsDialog(final WifiDisplay display) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.wifi_display_options, null);
        final EditText nameEditText = (EditText)view.findViewById(R.id.name);
        nameEditText.setText(display.getFriendlyDisplayName());

        DialogInterface.OnClickListener done = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = nameEditText.getText().toString().trim();
                if (name.isEmpty() || name.equals(display.getDeviceName())) {
                    name = null;
                }
                mDisplayManager.renameWifiDisplay(display.getDeviceAddress(), name);
            }
        };
        DialogInterface.OnClickListener forget = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mDisplayManager.forgetWifiDisplay(display.getDeviceAddress());
            }
        };

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setCancelable(true)
                .setTitle(R.string.wifi_display_options_title)
                .setView(view)
                .setPositiveButton(R.string.wifi_display_options_done, done)
                .setNegativeButton(R.string.wifi_display_options_forget, forget)
                .create();
        dialog.show();
    }

    private static boolean contains(WifiDisplay[] displays, String address) {
        for (WifiDisplay d : displays) {
            if (d.getDeviceAddress().equals(address)) {
                return true;
            }
        }
        return false;
    }

    private final CompoundButton.OnCheckedChangeListener mSwitchOnCheckedChangedListener =
            new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mWifiDisplayOnSetting = isChecked;
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.WIFI_DISPLAY_ON, isChecked ? 1 : 0);
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(DisplayManager.ACTION_WIFI_DISPLAY_STATUS_CHANGED)) {
                WifiDisplayStatus status = (WifiDisplayStatus)intent.getParcelableExtra(
                        DisplayManager.EXTRA_WIFI_DISPLAY_STATUS);
                mWifiDisplayStatus = status;
                applyState();
            }
        }
    };

    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            update();
        }
    };

    private final class WifiDisplayPreference extends Preference
            implements View.OnClickListener {
        private final WifiDisplay mDisplay;

        public WifiDisplayPreference(Context context, WifiDisplay display) {
            super(context);

            mDisplay = display;
            setTitle(display.getFriendlyDisplayName());
        }

        public WifiDisplay getDisplay() {
            return mDisplay;
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);

            ImageView deviceDetails = (ImageView) view.findViewById(R.id.deviceDetails);
            if (deviceDetails != null) {
                deviceDetails.setOnClickListener(this);

                if (!isEnabled()) {
                    TypedValue value = new TypedValue();
                    getContext().getTheme().resolveAttribute(android.R.attr.disabledAlpha,
                            value, true);
                    deviceDetails.setImageAlpha((int)(value.getFloat() * 255));
                }
            }
        }

        @Override
        public void onClick(View v) {
            showOptionsDialog(mDisplay);
        }
    }
}
