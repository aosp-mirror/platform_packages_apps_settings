/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.profiles;

import java.util.ArrayList;
import java.util.UUID;

import android.app.AirplaneModeSettings;
import android.app.AlertDialog;
import android.app.ConnectionSettings;
import android.app.Profile;
import android.app.ProfileGroup;
import android.app.ProfileManager;
import android.app.RingModeSettings;
import android.app.StreamSettings;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class ProfileConfig extends SettingsPreferenceFragment
    implements Preference.OnPreferenceChangeListener {

    static final String TAG = "ProfileConfig";

    public static final String PROFILE_SERVICE = "profile";

    private ProfileManager mProfileManager;

    private static final int MENU_NFC_WRITE = Menu.FIRST;

    private static final int MENU_DELETE = Menu.FIRST + 1;

    private static final int MENU_TRIGGERS = Menu.FIRST + 2;

    private Profile mProfile;

    private NamePreference mNamePreference;

    private ListPreference mScreenLockModePreference;

    // constant value that can be used to check return code from sub activity.
    private static final int PROFILE_GROUP_DETAILS = 1;

    private StreamItem[] mStreams;

    private ArrayList<ConnectionItem> mConnections;

    private RingModeItem mRingMode;

    private AirplaneModeItem mAirplaneMode;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mStreams = new StreamItem[] {
                new StreamItem(AudioManager.STREAM_ALARM, getString(R.string.alarm_volume_title)),
                new StreamItem(AudioManager.STREAM_MUSIC, getString(R.string.media_volume_title)),
                new StreamItem(AudioManager.STREAM_RING, getString(R.string.incoming_call_volume_title)),
                new StreamItem(AudioManager.STREAM_NOTIFICATION, getString(R.string.notification_volume_title))
        };

        mConnections = new ArrayList<ConnectionItem>();
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            mConnections.add(new ConnectionItem(ConnectionSettings.PROFILE_CONNECTION_BLUETOOTH, getString(R.string.toggleBluetooth)));
        }
        mConnections.add(new ConnectionItem(ConnectionSettings.PROFILE_CONNECTION_GPS, getString(R.string.toggleGPS)));
        mConnections.add(new ConnectionItem(ConnectionSettings.PROFILE_CONNECTION_WIFI, getString(R.string.toggleWifi)));
        mConnections.add(new ConnectionItem(ConnectionSettings.PROFILE_CONNECTION_SYNC, getString(R.string.toggleSync)));

        PackageManager pm = getActivity().getPackageManager();
        boolean isMobileData = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        if (isMobileData) {
            mConnections.add(new ConnectionItem(ConnectionSettings.PROFILE_CONNECTION_MOBILEDATA, getString(R.string.toggleData)));
            mConnections.add(new ConnectionItem(ConnectionSettings.PROFILE_CONNECTION_WIFIAP, getString(R.string.toggleWifiAp)));
            final TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            if (tm.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
                mConnections.add(new ConnectionItem(ConnectionSettings.PROFILE_CONNECTION_2G3G, getString(R.string.toggle2g3g), R.array.profile_networkmode_entries));
            }
        }        
        if (NfcAdapter.getDefaultAdapter(getActivity()) != null) {
            mConnections.add(new ConnectionItem(ConnectionSettings.PROFILE_CONNECTION_NFC, getString(R.string.toggleNfc)));
        }

        addPreferencesFromResource(R.xml.profile_config);

        mProfileManager = (ProfileManager) getActivity().getSystemService(PROFILE_SERVICE);

        final Bundle args = getArguments();
        mProfile = (args != null) ? (Profile) args.getParcelable("Profile") : null;

        if (mProfile == null) {
            mProfile = new Profile(getString(R.string.new_profile_name));
            mProfileManager.addProfile(mProfile);
        }

        setHasOptionsMenu(true);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (NfcAdapter.getDefaultAdapter(getActivity()) != null) {
            MenuItem nfc = menu.add(0, MENU_NFC_WRITE, 0, R.string.profile_write_nfc_tag)
                .setIcon(R.drawable.ic_menu_nfc_writer_dark);
            nfc.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
        MenuItem triggers = menu.add(0, MENU_TRIGGERS, 0, R.string.profile_triggers)
                .setIcon(R.drawable.ic_location);
        triggers.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        MenuItem delete = menu.add(0, MENU_DELETE, 1, R.string.profile_menu_delete)
                .setIcon(R.drawable.ic_menu_trash_holo_dark);
        delete.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_DELETE:
                deleteProfile();
                return true;
            case MENU_NFC_WRITE:
                startNFCProfileWriter();
                return true;
            case MENU_TRIGGERS:
                startTriggerFragment();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mProfile = mProfileManager.getProfile(mProfile.getUuid());
        fillList();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Save profile here
        if (mProfile != null) {
            mProfileManager.updateProfile(mProfile);
        }
    }

    private void startNFCProfileWriter() {
        PreferenceActivity pa = (PreferenceActivity) getActivity();
        Intent i = new Intent(this.getActivity(), NFCProfileWriter.class);
        i.putExtra(NFCProfileWriter.EXTRA_PROFILE_UUID, mProfile.getUuid().toString());
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pa.startActivity(i);
    }

    private void startTriggerFragment() {
        final PreferenceActivity pa = (PreferenceActivity) getActivity();
        final Bundle args = new Bundle();
        args.putParcelable("profile", mProfile);

        pa.startPreferencePanel(TriggersFragment.class.getName(), args, 0, "", null, 0);
    }

    private void fillList() {
        PreferenceScreen prefSet = getPreferenceScreen();

        // Add the General section
        PreferenceGroup generalPrefs = (PreferenceGroup) prefSet.findPreference("profile_general_section");
        if (generalPrefs != null) {
            generalPrefs.removeAll();

            // Name preference
            mNamePreference = new NamePreference(getActivity(), mProfile.getName());
            mNamePreference.setOnPreferenceChangeListener(this);
            generalPrefs.addPreference(mNamePreference);
        }

        // Populate system settings
        PreferenceGroup systemPrefs = (PreferenceGroup) prefSet.findPreference("profile_system_settings");
        if (systemPrefs != null) {
            systemPrefs.removeAll();
            // Ring mode preference
            if (mRingMode == null) {
                mRingMode = new RingModeItem();
            }
            RingModeSettings rms = mProfile.getRingMode();
            if (rms == null) {
                rms = new RingModeSettings();
                mProfile.setRingMode(rms);
            }
            mRingMode.mSettings = rms;
            ProfileRingModePreference rmp = new ProfileRingModePreference(getActivity());
            rmp.setRingModeItem(mRingMode);
            rmp.setTitle(R.string.ring_mode_title);
            rmp.setPersistent(false);
            rmp.setSummary(getActivity());
            rmp.setOnPreferenceChangeListener(this);
            mRingMode.mCheckbox = rmp;
            systemPrefs.addPreference(rmp);

            // Airplane mode preference
            if (mAirplaneMode == null) {
                mAirplaneMode = new AirplaneModeItem();
            }
            AirplaneModeSettings ams = mProfile.getAirplaneMode();
            if (ams == null) {
                ams = new AirplaneModeSettings();
                mProfile.setAirplaneMode(ams);
            }
            mAirplaneMode.mSettings = ams;
            ProfileAirplaneModePreference amp = new ProfileAirplaneModePreference(getActivity());
            amp.setAirplaneModeItem(mAirplaneMode);
            amp.setTitle(R.string.profile_airplanemode_title);
            amp.setPersistent(false);
            amp.setSummary(getActivity());
            amp.setOnPreferenceChangeListener(this);
            mAirplaneMode.mCheckbox = amp;
            systemPrefs.addPreference(amp);

            // Lockscreen mode preference
            mScreenLockModePreference = new ListPreference(getActivity());
            mScreenLockModePreference.setTitle(R.string.profile_lockmode_title);
            mScreenLockModePreference.setEntries(R.array.profile_lockmode_entries);
            mScreenLockModePreference.setEntryValues(R.array.profile_lockmode_values);
            mScreenLockModePreference.setPersistent(false);
            mScreenLockModePreference.setSummary(getResources().getStringArray(
                    R.array.profile_lockmode_summaries)[mProfile.getScreenLockMode()]);
            mScreenLockModePreference.setValue(String.valueOf(mProfile.getScreenLockMode()));
            mScreenLockModePreference.setOnPreferenceChangeListener(this);

            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (dpm.requireSecureKeyguard()) {
                mScreenLockModePreference.setEnabled(false);
                mScreenLockModePreference.setSummary(R.string.unlock_set_unlock_disabled_summary);
            }

            systemPrefs.addPreference(mScreenLockModePreference);
        }

        // Populate the audio streams list
        final AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        PreferenceGroup streamList = (PreferenceGroup) prefSet.findPreference("profile_volumeoverrides");
        if (streamList != null) {
            streamList.removeAll();
            for (StreamItem stream : mStreams) {
                StreamSettings settings = mProfile.getSettingsForStream(stream.mStreamId);
                if (settings == null) {
                    settings = new StreamSettings(stream.mStreamId);
                    mProfile.setStreamSettings(settings);
                }
                stream.mSettings = settings;
                StreamVolumePreference pref = new StreamVolumePreference(getActivity());
                pref.setKey("stream_" + stream.mStreamId);
                pref.setTitle(stream.mLabel);
                pref.setSummary(getString(R.string.volume_override_summary) + " " + settings.getValue() 
                        + "/" + am.getStreamMaxVolume(stream.mStreamId)); 
                pref.setPersistent(false);
                pref.setStreamItem(stream);
                stream.mCheckbox = pref;
                streamList.addPreference(pref);
            }
        }

        // Populate Connections list
        PreferenceGroup connectionList = (PreferenceGroup) prefSet.findPreference("profile_connectionoverrides");
        if (connectionList != null) {
            connectionList.removeAll();
            for (ConnectionItem connection : mConnections) {
                String[] connectionstrings = getResources().getStringArray(connection.mChoices);
                ConnectionSettings settings = mProfile.getSettingsForConnection(connection.mConnectionId);
                if (settings == null) {
                    settings = new ConnectionSettings(connection.mConnectionId);
                    mProfile.setConnectionSettings(settings);
                }
                connection.mSettings = settings;
                ProfileConnectionPreference pref = new ProfileConnectionPreference(getActivity());
                pref.setKey("connection_" + connection.mConnectionId);
                pref.setTitle(connection.mLabel);
                pref.setSummary(connectionstrings[settings.getValue()]);
                pref.setPersistent(false);
                pref.setConnectionItem(connection);
                connection.mCheckbox = pref;
                connectionList.addPreference(pref);
            }
        }

        // Populate Application groups
        PreferenceGroup groupList = (PreferenceGroup) prefSet.findPreference("profile_appgroups");
        if (groupList != null) {
            groupList.removeAll();
            for (ProfileGroup profileGroup : mProfile.getProfileGroups()) {
                PreferenceScreen pref = new PreferenceScreen(getActivity(), null);
                UUID uuid = profileGroup.getUuid();
                pref.setKey(uuid.toString());
                pref.setTitle(mProfileManager.getNotificationGroup(uuid).getName());
                //pref.setSummary(R.string.profile_summary);  // summary is repetitive, consider removing
                pref.setPersistent(false);
                pref.setSelectable(true);
                groupList.addPreference(pref);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof StreamVolumePreference) {
            for (StreamItem stream : mStreams) {
                if (preference == stream.mCheckbox) {
                    stream.mSettings.setOverride((Boolean) newValue);
                }
            }
        } else if (preference instanceof ProfileConnectionPreference) {
            for (ConnectionItem connection : mConnections) {
                if (preference == connection.mCheckbox) {
                    connection.mSettings.setOverride((Boolean) newValue);
                }
            }
        } else if (preference == mRingMode.mCheckbox) {
            mRingMode.mSettings.setOverride((Boolean) newValue);
        } else if (preference == mAirplaneMode.mCheckbox) {
            mAirplaneMode.mSettings.setOverride((Boolean) newValue);
        } else if (preference == mNamePreference) {
            String name = mNamePreference.getName().toString();
            if (!name.equals(mProfile.getName())) {
                if (!mProfileManager.profileExists(name)) {
                    mProfile.setName(name);
                } else {
                    mNamePreference.setName(mProfile.getName());
                    Toast.makeText(getActivity(), R.string.duplicate_profile_name, Toast.LENGTH_LONG).show();
                }
            }
        } else if (preference == mScreenLockModePreference) {
            mProfile.setScreenLockMode(Integer.valueOf((String) newValue));
            mScreenLockModePreference.setSummary(getResources().getStringArray(
                    R.array.profile_lockmode_summaries)[mProfile.getScreenLockMode()]);
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Log.d(TAG, "onPreferenceTreeClick(): entered" + preferenceScreen.getKey() + preference.getKey());
        if (preference instanceof PreferenceScreen) {
            startProfileGroupActivity(preference.getKey(), preference.getTitle().toString());
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void startProfileGroupActivity(String key, String title) {
        Bundle args = new Bundle();
        args.putString("ProfileGroup", key.toString());
        args.putParcelable("Profile", mProfile);

        String header = mProfile.getName().toString() + ": " + title.toString();
        PreferenceActivity pa = (PreferenceActivity) getActivity();
        pa.startPreferencePanel(ProfileGroupConfig.class.getName(), args,
                0, header, this, PROFILE_GROUP_DETAILS);
    }

    private void deleteProfile() {
        if (mProfile.getUuid().equals(mProfileManager.getActiveProfile().getUuid())) {
            Toast toast = Toast.makeText(getActivity(), getString(R.string.profile_cannot_delete),
                    Toast.LENGTH_SHORT);
            toast.show();
        } else {
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(R.string.profile_menu_delete);
            alert.setIconAttribute(android.R.attr.alertDialogIcon);
            alert.setMessage(R.string.profile_delete_confirm);
            alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    doDelete();
                }
            });
            alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            alert.create().show();
        }
    }

    private void doDelete() {
        mProfileManager.removeProfile(mProfile);
        mProfile = null;
        finish();
    }

    static class StreamItem {
        int mStreamId;
        String mLabel;
        StreamSettings mSettings;
        StreamVolumePreference mCheckbox;

        public StreamItem(int streamId, String label) {
            mStreamId = streamId;
            mLabel = label;
        }
    }

    static class ConnectionItem {
        int mConnectionId;
        String mLabel;
        ConnectionSettings mSettings;
        ProfileConnectionPreference mCheckbox;
        int mChoices;

        public ConnectionItem(int connectionId, String label) {
            mConnectionId = connectionId;
            mChoices = R.array.profile_connection_entries;
            mLabel = label;
        }

        public ConnectionItem(int connectionId, String label, int choices) {
            mConnectionId = connectionId;
            mLabel = label;
            mChoices = choices;
        }
    }

    static class RingModeItem {
        RingModeSettings mSettings;
        ProfileRingModePreference mCheckbox;

        public RingModeItem() {
            // nothing to do
        }
    }

    static class AirplaneModeItem {
        AirplaneModeSettings mSettings;
        ProfileAirplaneModePreference mCheckbox;

        public AirplaneModeItem() {
            // nothing to do
        }
    }
}
