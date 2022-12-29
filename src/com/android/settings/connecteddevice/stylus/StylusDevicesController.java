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

package com.android.settings.connecteddevice.stylus;

import android.app.role.RoleManager;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;
import android.view.InputDevice;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.List;

/**
 * This class adds stylus preferences.
 */
public class StylusDevicesController extends AbstractPreferenceController implements
        Preference.OnPreferenceClickListener, LifecycleObserver, OnResume {

    @VisibleForTesting
    static final String KEY_STYLUS = "device_stylus";
    @VisibleForTesting
    static final String KEY_HANDWRITING = "handwriting_switch";
    @VisibleForTesting
    static final String KEY_IGNORE_BUTTON = "ignore_button";
    @VisibleForTesting
    static final String KEY_DEFAULT_NOTES = "default_notes";

    private static final String TAG = "StylusDevicesController";

    @Nullable
    private final InputDevice mInputDevice;

    @Nullable
    private final CachedBluetoothDevice mCachedBluetoothDevice;

    @VisibleForTesting
    PreferenceCategory mPreferencesContainer;

    public StylusDevicesController(Context context, InputDevice inputDevice,
            CachedBluetoothDevice cachedBluetoothDevice, Lifecycle lifecycle) {
        super(context);
        mInputDevice = inputDevice;
        mCachedBluetoothDevice = cachedBluetoothDevice;
        lifecycle.addObserver(this);
    }

    @Override
    public boolean isAvailable() {
        return isDeviceStylus(mInputDevice, mCachedBluetoothDevice);
    }

    @Nullable
    private Preference createDefaultNotesPreference() {
        RoleManager rm = mContext.getSystemService(RoleManager.class);
        if (rm == null) {
            return null;
        }

        // TODO(b/254834764): replace with notes role once merged
        List<String> roleHolders = rm.getRoleHoldersAsUser(RoleManager.ROLE_ASSISTANT,
                mContext.getUser());
        if (roleHolders.isEmpty()) {
            return null;
        }

        String packageName = roleHolders.get(0);
        PackageManager pm = mContext.getPackageManager();
        String appName = packageName;
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName,
                    PackageManager.ApplicationInfoFlags.of(0));
            appName = ai == null ? packageName : pm.getApplicationLabel(ai).toString();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Notes role package not found.");
        }

        Preference pref = new Preference(mContext);
        pref.setKey(KEY_DEFAULT_NOTES);
        pref.setTitle(mContext.getString(R.string.stylus_default_notes_app));
        pref.setIcon(R.drawable.ic_article);
        pref.setEnabled(true);
        pref.setSummary(appName);
        return pref;
    }

    private SwitchPreference createHandwritingPreference() {
        SwitchPreference pref = new SwitchPreference(mContext);
        pref.setKey(KEY_HANDWRITING);
        pref.setTitle(mContext.getString(R.string.stylus_textfield_handwriting));
        pref.setIcon(R.drawable.ic_text_fields_alt);
        pref.setOnPreferenceClickListener(this);
        pref.setChecked(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.STYLUS_HANDWRITING_ENABLED, 0) == 1);
        return pref;
    }

    private SwitchPreference createButtonPressPreference() {
        SwitchPreference pref = new SwitchPreference(mContext);
        pref.setKey(KEY_IGNORE_BUTTON);
        pref.setTitle(mContext.getString(R.string.stylus_ignore_button));
        pref.setIcon(R.drawable.ic_block);
        pref.setOnPreferenceClickListener(this);
        pref.setChecked(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.STYLUS_BUTTONS_DISABLED, 0) == 1);
        return pref;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        switch (key) {
            case KEY_DEFAULT_NOTES:
                PackageManager pm = mContext.getPackageManager();
                String packageName = pm.getPermissionControllerPackageName();
                // TODO(b/254834764): replace with notes role once merged
                Intent intent = new Intent(Intent.ACTION_MANAGE_DEFAULT_APP).setPackage(
                        packageName).putExtra(Intent.EXTRA_ROLE_NAME, RoleManager.ROLE_ASSISTANT);
                mContext.startActivity(intent);
                break;
            case KEY_HANDWRITING:
                Settings.Global.putInt(mContext.getContentResolver(),
                        Settings.Global.STYLUS_HANDWRITING_ENABLED,
                        ((SwitchPreference) preference).isChecked() ? 1 : 0);
                break;
            case KEY_IGNORE_BUTTON:
                Settings.Secure.putInt(mContext.getContentResolver(),
                        Secure.STYLUS_BUTTONS_DISABLED,
                        ((SwitchPreference) preference).isChecked() ? 1 : 0);
                break;
        }
        return true;
    }

    @Override
    public final void displayPreference(PreferenceScreen screen) {
        mPreferencesContainer = (PreferenceCategory) screen.findPreference(getPreferenceKey());
        super.displayPreference(screen);

        refresh();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_STYLUS;
    }

    @Override
    public void onResume() {
        refresh();
    }

    private void refresh() {
        if (!isAvailable()) return;

        Preference notesPref = mPreferencesContainer.findPreference(KEY_DEFAULT_NOTES);
        if (notesPref == null) {
            notesPref = createDefaultNotesPreference();
            if (notesPref != null) {
                mPreferencesContainer.addPreference(notesPref);
            }
        }

        Preference handwritingPref = mPreferencesContainer.findPreference(KEY_HANDWRITING);
        // TODO(b/255732419): add proper InputMethodInfo conditional to show or hide
        // InputMethodManager imm = mContext.getSystemService(InputMethodManager.class);
        if (handwritingPref == null) {
            mPreferencesContainer.addPreference(createHandwritingPreference());
        }

        Preference buttonPref = mPreferencesContainer.findPreference(KEY_IGNORE_BUTTON);
        if (buttonPref == null) {
            mPreferencesContainer.addPreference(createButtonPressPreference());
        }
    }

    /**
     * Identifies whether a device is a stylus using the associated {@link InputDevice} or
     * {@link CachedBluetoothDevice}.
     *
     * InputDevices are only available when the device is USI or Bluetooth-connected, whereas
     * CachedBluetoothDevices are available for Bluetooth devices when connected or paired,
     * so to handle all cases, both are needed.
     *
     * @param inputDevice           The associated input device of the stylus
     * @param cachedBluetoothDevice The associated bluetooth device of the stylus
     */
    public static boolean isDeviceStylus(@Nullable InputDevice inputDevice,
            @Nullable CachedBluetoothDevice cachedBluetoothDevice) {
        if (inputDevice != null && inputDevice.supportsSource(InputDevice.SOURCE_STYLUS)) {
            return true;
        }

        if (cachedBluetoothDevice != null) {
            BluetoothDevice bluetoothDevice = cachedBluetoothDevice.getDevice();
            String deviceType = BluetoothUtils.getStringMetaData(bluetoothDevice,
                    BluetoothDevice.METADATA_DEVICE_TYPE);
            return TextUtils.equals(deviceType, BluetoothDevice.DEVICE_TYPE_STYLUS);
        }

        return false;
    }

}
