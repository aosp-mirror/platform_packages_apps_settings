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

import android.app.Dialog;
import android.app.role.RoleManager;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.hardware.input.InputSettings;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.dashboard.profileselector.ProfileSelectDialog;
import com.android.settings.dashboard.profileselector.UserAdapter;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.ArrayList;
import java.util.List;

/**
 * This class adds stylus preferences.
 */
public class StylusDevicesController extends AbstractPreferenceController implements
        Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnResume {

    @VisibleForTesting
    static final String KEY_STYLUS = "device_stylus";
    @VisibleForTesting
    static final String KEY_HANDWRITING = "handwriting_switch";
    @VisibleForTesting
    static final String KEY_IGNORE_BUTTON = "ignore_button";
    @VisibleForTesting
    static final String KEY_DEFAULT_NOTES = "default_notes";
    @VisibleForTesting
    static final String KEY_SHOW_STYLUS_POINTER_ICON = "show_stylus_pointer_icon";

    private static final String TAG = "StylusDevicesController";

    @Nullable
    private final InputDevice mInputDevice;

    @Nullable
    private final CachedBluetoothDevice mCachedBluetoothDevice;

    @VisibleForTesting
    PreferenceCategory mPreferencesContainer;

    @VisibleForTesting
    Dialog mDialog;

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
    private Preference createOrUpdateDefaultNotesPreference(@Nullable Preference preference) {
        RoleManager rm = mContext.getSystemService(RoleManager.class);
        if (rm == null || !rm.isRoleAvailable(RoleManager.ROLE_NOTES)) {
            return null;
        }

        // Check if the connected stylus supports the tail button. A connected device is when input
        // device is available (mInputDevice != null). For a cached device (mInputDevice == null)
        // there isn't way to check if the device supports the button so assume it does.
        if (mInputDevice != null) {
            boolean doesStylusSupportTailButton =
                    mInputDevice.hasKeys(KeyEvent.KEYCODE_STYLUS_BUTTON_TAIL)[0];
            if (!doesStylusSupportTailButton) {
                return null;
            }
        }

        Preference pref = preference == null ? new Preference(mContext) : preference;
        pref.setKey(KEY_DEFAULT_NOTES);
        pref.setTitle(mContext.getString(R.string.stylus_default_notes_app));
        pref.setIcon(R.drawable.ic_article);
        pref.setOnPreferenceClickListener(this);
        pref.setEnabled(true);

        UserHandle user = getDefaultNoteTaskProfile();
        List<String> roleHolders = rm.getRoleHoldersAsUser(RoleManager.ROLE_NOTES, user);
        if (roleHolders.isEmpty()) {
            pref.setSummary(R.string.default_app_none);
            return pref;
        }

        String packageName = roleHolders.get(0);
        PackageManager pm = mContext.getPackageManager();
        String appName = packageName;
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName,
                    PackageManager.ApplicationInfoFlags.of(0));
            appName = ai == null ? "" : pm.getApplicationLabel(ai).toString();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Notes role package not found.");
        }

        if (mContext.getSystemService(UserManager.class).isManagedProfile(user.getIdentifier())) {
            pref.setSummary(
                    mContext.getString(R.string.stylus_default_notes_summary_work, appName));
        } else {
            pref.setSummary(appName);
        }
        return pref;
    }

    private PrimarySwitchPreference createOrUpdateHandwritingPreference(
            PrimarySwitchPreference preference) {
        PrimarySwitchPreference pref = preference == null ? new PrimarySwitchPreference(mContext)
                : preference;
        pref.setKey(KEY_HANDWRITING);
        pref.setTitle(mContext.getString(R.string.stylus_textfield_handwriting));
        pref.setIcon(R.drawable.ic_text_fields_alt);
        // Using a two-target preference, clicking will send an intent and change will toggle.
        pref.setOnPreferenceChangeListener(this);
        pref.setOnPreferenceClickListener(this);
        pref.setChecked(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.STYLUS_HANDWRITING_ENABLED,
                Secure.STYLUS_HANDWRITING_DEFAULT_VALUE) == 1);
        pref.setVisible(currentInputMethodSupportsHandwriting());
        return pref;
    }

    private TwoStatePreference createButtonPressPreference() {
        TwoStatePreference pref = new SwitchPreferenceCompat(mContext);
        pref.setKey(KEY_IGNORE_BUTTON);
        pref.setTitle(mContext.getString(R.string.stylus_ignore_button));
        pref.setIcon(R.drawable.ic_block);
        pref.setOnPreferenceClickListener(this);
        pref.setChecked(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.STYLUS_BUTTONS_ENABLED, 1) == 0);
        return pref;
    }

    @Nullable
    private SwitchPreferenceCompat createShowStylusPointerIconPreference(
            SwitchPreferenceCompat preference) {
        if (!mContext.getResources()
                .getBoolean(com.android.internal.R.bool.config_enableStylusPointerIcon)) {
            // If the config is not enabled, no need to show the preference to user
            return null;
        }
        SwitchPreferenceCompat pref = preference == null ? new SwitchPreferenceCompat(mContext)
                : preference;
        pref.setKey(KEY_SHOW_STYLUS_POINTER_ICON);
        pref.setTitle(mContext.getString(R.string.show_stylus_pointer_icon));
        pref.setIcon(R.drawable.ic_stylus);
        pref.setOnPreferenceClickListener(this);
        pref.setChecked(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.STYLUS_POINTER_ICON_ENABLED,
                InputSettings.DEFAULT_STYLUS_POINTER_ICON_ENABLED) == 1);
        return pref;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        switch (key) {
            case KEY_DEFAULT_NOTES:
                PackageManager pm = mContext.getPackageManager();
                String packageName = pm.getPermissionControllerPackageName();
                Intent intent = new Intent(Intent.ACTION_MANAGE_DEFAULT_APP).setPackage(
                        packageName).putExtra(Intent.EXTRA_ROLE_NAME, RoleManager.ROLE_NOTES);

                List<UserHandle> users = getUserAndManagedProfiles();
                if (users.size() <= 1) {
                    mContext.startActivity(intent);
                } else {
                    createAndShowProfileSelectDialog(intent, users);
                }
                break;
            case KEY_HANDWRITING:
                InputMethodManager imm = mContext.getSystemService(InputMethodManager.class);
                InputMethodInfo inputMethod = imm.getCurrentInputMethodInfo();
                if (inputMethod == null) break;
                Intent handwritingIntent =
                        inputMethod.createStylusHandwritingSettingsActivityIntent();
                if (handwritingIntent != null) {
                    mContext.startActivity(handwritingIntent);
                }
                break;
            case KEY_IGNORE_BUTTON:
                Settings.Secure.putInt(mContext.getContentResolver(),
                        Secure.STYLUS_BUTTONS_ENABLED,
                        ((TwoStatePreference) preference).isChecked() ? 0 : 1);
                break;
            case KEY_SHOW_STYLUS_POINTER_ICON:
                Settings.Secure.putInt(mContext.getContentResolver(),
                        Secure.STYLUS_POINTER_ICON_ENABLED,
                        ((SwitchPreferenceCompat) preference).isChecked() ? 1 : 0);
                break;
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        switch (key) {
            case KEY_HANDWRITING:
                Settings.Secure.putInt(mContext.getContentResolver(),
                        Settings.Secure.STYLUS_HANDWRITING_ENABLED,
                        (boolean) newValue ? 1 : 0);
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

        Preference currNotesPref = mPreferencesContainer.findPreference(KEY_DEFAULT_NOTES);
        Preference notesPref = createOrUpdateDefaultNotesPreference(currNotesPref);
        if (currNotesPref == null && notesPref != null) {
            mPreferencesContainer.addPreference(notesPref);
        }

        PrimarySwitchPreference currHandwritingPref = mPreferencesContainer.findPreference(
                KEY_HANDWRITING);
        Preference handwritingPref = createOrUpdateHandwritingPreference(currHandwritingPref);
        if (currHandwritingPref == null) {
            mPreferencesContainer.addPreference(handwritingPref);
        }

        Preference buttonPref = mPreferencesContainer.findPreference(KEY_IGNORE_BUTTON);
        if (buttonPref == null) {
            mPreferencesContainer.addPreference(createButtonPressPreference());
        }
        SwitchPreferenceCompat currShowStylusPointerIconPref = mPreferencesContainer
                .findPreference(KEY_SHOW_STYLUS_POINTER_ICON);
        Preference showStylusPointerIconPref =
                createShowStylusPointerIconPreference(currShowStylusPointerIconPref);
        if (currShowStylusPointerIconPref == null && showStylusPointerIconPref != null) {
            mPreferencesContainer.addPreference(showStylusPointerIconPref);
        }
    }

    private boolean currentInputMethodSupportsHandwriting() {
        InputMethodManager imm = mContext.getSystemService(InputMethodManager.class);
        InputMethodInfo inputMethod = imm.getCurrentInputMethodInfo();
        return inputMethod != null && inputMethod.supportsStylusHandwriting();
    }

    private List<UserHandle> getUserAndManagedProfiles() {
        UserManager um = mContext.getSystemService(UserManager.class);
        final List<UserHandle> userManagedProfiles = new ArrayList<>();
        // Add the current user, then add all the associated managed profiles.
        final UserHandle currentUser = Process.myUserHandle();
        userManagedProfiles.add(currentUser);

        final List<UserInfo> userInfos = um.getUsers();
        for (UserInfo info : userInfos) {
            int userId = info.id;
            if (um.isManagedProfile(userId)
                    && um.getProfileParent(userId).id == currentUser.getIdentifier()) {
                userManagedProfiles.add(UserHandle.of(userId));
            }
        }
        return userManagedProfiles;
    }

    private UserHandle getDefaultNoteTaskProfile() {
        final int userId = Secure.getInt(
                mContext.getContentResolver(),
                Secure.DEFAULT_NOTE_TASK_PROFILE,
                UserHandle.myUserId());
        return UserHandle.of(userId);
    }

    @VisibleForTesting
    UserAdapter.OnClickListener createProfileDialogClickCallback(
            Intent intent, List<UserHandle> users) {
        // TODO(b/281659827): improve UX flow for when activity is cancelled
        return (int position) -> {
            intent.putExtra(Intent.EXTRA_USER, users.get(position));

            Secure.putInt(mContext.getContentResolver(),
                    Secure.DEFAULT_NOTE_TASK_PROFILE,
                    users.get(position).getIdentifier());
            mContext.startActivity(intent);

            mDialog.dismiss();
        };
    }

    private void createAndShowProfileSelectDialog(Intent intent, List<UserHandle> users) {
        mDialog = ProfileSelectDialog.createDialog(
                mContext,
                users,
                createProfileDialogClickCallback(intent, users));
        mDialog.show();
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
