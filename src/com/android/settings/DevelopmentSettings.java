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

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.backup.IBackupManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.VerifierDeviceIdentity;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.IWindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;

/*
 * Displays preferences for application developers.
 */
public class DevelopmentSettings extends PreferenceFragment
        implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener,
                OnPreferenceChangeListener, CompoundButton.OnCheckedChangeListener {

    private static final String ENABLE_ADB = "enable_adb";

    private static final String VERIFIER_DEVICE_IDENTIFIER = "verifier_device_identifier";
    private static final String KEEP_SCREEN_ON = "keep_screen_on";
    private static final String ALLOW_MOCK_LOCATION = "allow_mock_location";
    private static final String HDCP_CHECKING_KEY = "hdcp_checking";
    private static final String HDCP_CHECKING_PROPERTY = "persist.sys.hdcp_checking";
    private static final String LOCAL_BACKUP_PASSWORD = "local_backup_password";
    private static final String HARDWARE_UI_PROPERTY = "persist.sys.ui.hw";

    private static final String STRICT_MODE_KEY = "strict_mode";
    private static final String POINTER_LOCATION_KEY = "pointer_location";
    private static final String SHOW_TOUCHES_KEY = "show_touches";
    private static final String SHOW_SCREEN_UPDATES_KEY = "show_screen_updates";
    private static final String DISABLE_OVERLAYS_KEY = "disable_overlays";
    private static final String SHOW_CPU_USAGE_KEY = "show_cpu_usage";
    private static final String FORCE_HARDWARE_UI_KEY = "force_hw_ui";
    private static final String WINDOW_ANIMATION_SCALE_KEY = "window_animation_scale";
    private static final String TRANSITION_ANIMATION_SCALE_KEY = "transition_animation_scale";
    private static final String ANIMATOR_DURATION_SCALE_KEY = "animator_duration_scale";

    private static final String IMMEDIATELY_DESTROY_ACTIVITIES_KEY
            = "immediately_destroy_activities";
    private static final String APP_PROCESS_LIMIT_KEY = "app_process_limit";

    private static final String SHOW_ALL_ANRS_KEY = "show_all_anrs";

    private IWindowManager mWindowManager;
    private IBackupManager mBackupManager;

    private Switch mEnabledSwitch;
    private boolean mLastEnabledState;

    private CheckBoxPreference mEnableAdb;
    private CheckBoxPreference mKeepScreenOn;
    private CheckBoxPreference mAllowMockLocation;
    private PreferenceScreen mPassword;

    private CheckBoxPreference mStrictMode;
    private CheckBoxPreference mPointerLocation;
    private CheckBoxPreference mShowTouches;
    private CheckBoxPreference mShowScreenUpdates;
    private CheckBoxPreference mDisableOverlays;
    private CheckBoxPreference mShowCpuUsage;
    private CheckBoxPreference mForceHardwareUi;
    private ListPreference mWindowAnimationScale;
    private ListPreference mTransitionAnimationScale;
    private ListPreference mAnimatorDurationScale;

    private CheckBoxPreference mImmediatelyDestroyActivities;
    private ListPreference mAppProcessLimit;

    private CheckBoxPreference mShowAllANRs;

    private final ArrayList<Preference> mAllPrefs = new ArrayList<Preference>();
    private final ArrayList<CheckBoxPreference> mResetCbPrefs
            = new ArrayList<CheckBoxPreference>();

    // To track whether a confirmation dialog was clicked.
    private boolean mDialogClicked;
    private Dialog mEnableDialog;
    private Dialog mAdbDialog;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        mBackupManager = IBackupManager.Stub.asInterface(
                ServiceManager.getService(Context.BACKUP_SERVICE));

        addPreferencesFromResource(R.xml.development_prefs);

        mEnableAdb = (CheckBoxPreference) findPreference(ENABLE_ADB);
        mAllPrefs.add(mEnableAdb);
        mResetCbPrefs.add(mEnableAdb);
        mKeepScreenOn = (CheckBoxPreference) findPreference(KEEP_SCREEN_ON);
        mAllPrefs.add(mKeepScreenOn);
        mResetCbPrefs.add(mKeepScreenOn);
        mAllowMockLocation = (CheckBoxPreference) findPreference(ALLOW_MOCK_LOCATION);
        mAllPrefs.add(mAllowMockLocation);
        mResetCbPrefs.add(mAllowMockLocation);
        mPassword = (PreferenceScreen) findPreference(LOCAL_BACKUP_PASSWORD);
        mAllPrefs.add(mPassword);

        mStrictMode = (CheckBoxPreference) findPreference(STRICT_MODE_KEY);
        mAllPrefs.add(mStrictMode);
        mResetCbPrefs.add(mStrictMode);
        mResetCbPrefs.add(mAllowMockLocation);
        mPointerLocation = (CheckBoxPreference) findPreference(POINTER_LOCATION_KEY);
        mAllPrefs.add(mPointerLocation);
        mResetCbPrefs.add(mPointerLocation);
        mShowTouches = (CheckBoxPreference) findPreference(SHOW_TOUCHES_KEY);
        mAllPrefs.add(mShowTouches);
        mResetCbPrefs.add(mShowTouches);
        mShowScreenUpdates = (CheckBoxPreference) findPreference(SHOW_SCREEN_UPDATES_KEY);
        mAllPrefs.add(mShowScreenUpdates);
        mResetCbPrefs.add(mShowScreenUpdates);
        mDisableOverlays = (CheckBoxPreference) findPreference(DISABLE_OVERLAYS_KEY);
        mAllPrefs.add(mDisableOverlays);
        mResetCbPrefs.add(mDisableOverlays);
        mShowCpuUsage = (CheckBoxPreference) findPreference(SHOW_CPU_USAGE_KEY);
        mAllPrefs.add(mShowCpuUsage);
        mResetCbPrefs.add(mShowCpuUsage);
        mForceHardwareUi = (CheckBoxPreference) findPreference(FORCE_HARDWARE_UI_KEY);
        mAllPrefs.add(mForceHardwareUi);
        mResetCbPrefs.add(mForceHardwareUi);
        mWindowAnimationScale = (ListPreference) findPreference(WINDOW_ANIMATION_SCALE_KEY);
        mAllPrefs.add(mWindowAnimationScale);
        mWindowAnimationScale.setOnPreferenceChangeListener(this);
        mTransitionAnimationScale = (ListPreference) findPreference(TRANSITION_ANIMATION_SCALE_KEY);
        mAllPrefs.add(mTransitionAnimationScale);
        mTransitionAnimationScale.setOnPreferenceChangeListener(this);
        mAnimatorDurationScale = (ListPreference) findPreference(ANIMATOR_DURATION_SCALE_KEY);
        mAllPrefs.add(mAnimatorDurationScale);
        mAnimatorDurationScale.setOnPreferenceChangeListener(this);

        mImmediatelyDestroyActivities = (CheckBoxPreference) findPreference(
                IMMEDIATELY_DESTROY_ACTIVITIES_KEY);
        mAllPrefs.add(mImmediatelyDestroyActivities);
        mResetCbPrefs.add(mImmediatelyDestroyActivities);
        mAppProcessLimit = (ListPreference) findPreference(APP_PROCESS_LIMIT_KEY);
        mAllPrefs.add(mAppProcessLimit);
        mAppProcessLimit.setOnPreferenceChangeListener(this);

        mShowAllANRs = (CheckBoxPreference) findPreference(
                SHOW_ALL_ANRS_KEY);
        mAllPrefs.add(mShowAllANRs);
        mResetCbPrefs.add(mShowAllANRs);

        final Preference verifierDeviceIdentifier = findPreference(VERIFIER_DEVICE_IDENTIFIER);
        final PackageManager pm = getActivity().getPackageManager();
        final VerifierDeviceIdentity verifierIndentity = pm.getVerifierDeviceIdentity();
        if (verifierIndentity != null) {
            verifierDeviceIdentifier.setSummary(verifierIndentity.toString());
        }

        Preference hdcpChecking = findPreference(HDCP_CHECKING_KEY);
        if (hdcpChecking != null) {
            mAllPrefs.add(hdcpChecking);
        }
        removeHdcpOptionsForProduction();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Activity activity = getActivity();
        mEnabledSwitch = new Switch(activity);

        final int padding = activity.getResources().getDimensionPixelSize(
                R.dimen.action_bar_switch_padding);
        mEnabledSwitch.setPadding(0, 0, padding, 0);
        mEnabledSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        final Activity activity = getActivity();
        activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(mEnabledSwitch, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL | Gravity.RIGHT));
    }

    @Override
    public void onStop() {
        super.onStop();
        final Activity activity = getActivity();
        activity.getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(null);
    }

    private void removeHdcpOptionsForProduction() {
        if ("user".equals(Build.TYPE)) {
            Preference hdcpChecking = findPreference(HDCP_CHECKING_KEY);
            if (hdcpChecking != null) {
                // Remove the preference
                getPreferenceScreen().removePreference(hdcpChecking);
                mAllPrefs.remove(hdcpChecking);
            }
        }
    }

    private void setPrefsEnabledState(boolean enabled) {
        for (int i=0; i<mAllPrefs.size(); i++) {
            mAllPrefs.get(i).setEnabled(enabled);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        final ContentResolver cr = getActivity().getContentResolver();
        mLastEnabledState = Settings.Secure.getInt(cr,
                Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
        mEnabledSwitch.setChecked(mLastEnabledState);
        setPrefsEnabledState(mLastEnabledState);
        updateAllOptions();
    }

    private void updateAllOptions() {
        final ContentResolver cr = getActivity().getContentResolver();
        mEnableAdb.setChecked(Settings.Secure.getInt(cr,
                Settings.Secure.ADB_ENABLED, 0) != 0);
        mKeepScreenOn.setChecked(Settings.System.getInt(cr,
                Settings.System.STAY_ON_WHILE_PLUGGED_IN, 0) != 0);
        mAllowMockLocation.setChecked(Settings.Secure.getInt(cr,
                Settings.Secure.ALLOW_MOCK_LOCATION, 0) != 0);
        updateHdcpValues();
        updatePasswordSummary();
        updateStrictModeVisualOptions();
        updatePointerLocationOptions();
        updateShowTouchesOptions();
        updateFlingerOptions();
        updateCpuUsageOptions();
        updateHardwareUiOptions();
        updateAnimationScaleOptions();
        updateImmediatelyDestroyActivitiesOptions();
        updateAppProcessLimitOptions();
        updateShowAllANRsOptions();
    }

    private void resetDangerousOptions() {
        for (int i=0; i<mResetCbPrefs.size(); i++) {
            CheckBoxPreference cb = mResetCbPrefs.get(i);
            if (cb.isChecked()) {
                cb.setChecked(false);
                onPreferenceTreeClick(null, cb);
            }
        }
        writeAnimationScaleOption(0, mWindowAnimationScale, null);
        writeAnimationScaleOption(1, mTransitionAnimationScale, null);
        writeAnimationScaleOption(2, mAnimatorDurationScale, null);
        writeAppProcessLimitOptions(null);
        updateAllOptions();
    }

    private void updateHdcpValues() {
        int index = 1; // Defaults to drm-only. Needs to match with R.array.hdcp_checking_values
        ListPreference hdcpChecking = (ListPreference) findPreference(HDCP_CHECKING_KEY);
        if (hdcpChecking != null) {
            String currentValue = SystemProperties.get(HDCP_CHECKING_PROPERTY);
            String[] values = getResources().getStringArray(R.array.hdcp_checking_values);
            String[] summaries = getResources().getStringArray(R.array.hdcp_checking_summaries);
            for (int i = 0; i < values.length; i++) {
                if (currentValue.equals(values[i])) {
                    index = i;
                    break;
                }
            }
            hdcpChecking.setValue(values[index]);
            hdcpChecking.setSummary(summaries[index]);
            hdcpChecking.setOnPreferenceChangeListener(this);
        }
    }

    private void updatePasswordSummary() {
        try {
            if (mBackupManager.hasBackupPassword()) {
                mPassword.setSummary(R.string.local_backup_password_summary_change);
            } else {
                mPassword.setSummary(R.string.local_backup_password_summary_none);
            }
        } catch (RemoteException e) {
            // Not much we can do here
        }
    }

    // Returns the current state of the system property that controls
    // strictmode flashes.  One of:
    //    0: not explicitly set one way or another
    //    1: on
    //    2: off
    private int currentStrictModeActiveIndex() {
        if (TextUtils.isEmpty(SystemProperties.get(StrictMode.VISUAL_PROPERTY))) {
            return 0;
        }
        boolean enabled = SystemProperties.getBoolean(StrictMode.VISUAL_PROPERTY, false);
        return enabled ? 1 : 2;
    }

    private void writeStrictModeVisualOptions() {
        try {
            mWindowManager.setStrictModeVisualIndicatorPreference(mStrictMode.isChecked()
                    ? "1" : "");
        } catch (RemoteException e) {
        }
    }

    private void updateStrictModeVisualOptions() {
        mStrictMode.setChecked(currentStrictModeActiveIndex() == 1);
    }

    private void writePointerLocationOptions() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.POINTER_LOCATION, mPointerLocation.isChecked() ? 1 : 0);
    }

    private void updatePointerLocationOptions() {
        mPointerLocation.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.POINTER_LOCATION, 0) != 0);
    }

    private void writeShowTouchesOptions() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.SHOW_TOUCHES, mShowTouches.isChecked() ? 1 : 0);
    }

    private void updateShowTouchesOptions() {
        mShowTouches.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SHOW_TOUCHES, 0) != 0);
    }

    private void updateFlingerOptions() {
        // magic communication with surface flinger.
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                flinger.transact(1010, data, reply, 0);
                @SuppressWarnings("unused")
                int showCpu = reply.readInt();
                @SuppressWarnings("unused")
                int enableGL = reply.readInt();
                int showUpdates = reply.readInt();
                mShowScreenUpdates.setChecked(showUpdates != 0);
                @SuppressWarnings("unused")
                int showBackground = reply.readInt();
                int disableOverlays = reply.readInt();
                mDisableOverlays.setChecked(disableOverlays != 0);
                reply.recycle();
                data.recycle();
            }
        } catch (RemoteException ex) {
        }
    }

    private void writeShowUpdatesOption() {
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                final int showUpdates = mShowScreenUpdates.isChecked() ? 1 : 0; 
                data.writeInt(showUpdates);
                flinger.transact(1002, data, null, 0);
                data.recycle();

                updateFlingerOptions();
            }
        } catch (RemoteException ex) {
        }
    }

    private void writeDisableOverlaysOption() {
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                final int disableOverlays = mDisableOverlays.isChecked() ? 1 : 0; 
                data.writeInt(disableOverlays);
                flinger.transact(1008, data, null, 0);
                data.recycle();

                updateFlingerOptions();
            }
        } catch (RemoteException ex) {
        }
    }

    private void updateHardwareUiOptions() {
        mForceHardwareUi.setChecked(SystemProperties.getBoolean(HARDWARE_UI_PROPERTY, false));
    }
    
    private void writeHardwareUiOptions() {
        SystemProperties.set(HARDWARE_UI_PROPERTY, mForceHardwareUi.isChecked() ? "true" : "false");
    }

    private void updateCpuUsageOptions() {
        mShowCpuUsage.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SHOW_PROCESSES, 0) != 0);
    }
    
    private void writeCpuUsageOptions() {
        boolean value = mShowCpuUsage.isChecked();
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.SHOW_PROCESSES, value ? 1 : 0);
        Intent service = (new Intent())
                .setClassName("com.android.systemui", "com.android.systemui.LoadAverageService");
        if (value) {
            getActivity().startService(service);
        } else {
            getActivity().stopService(service);
        }
    }

    private void writeImmediatelyDestroyActivitiesOptions() {
        try {
            ActivityManagerNative.getDefault().setAlwaysFinish(
                    mImmediatelyDestroyActivities.isChecked());
        } catch (RemoteException ex) {
        }
    }

    private void updateImmediatelyDestroyActivitiesOptions() {
        mImmediatelyDestroyActivities.setChecked(Settings.System.getInt(
            getActivity().getContentResolver(), Settings.System.ALWAYS_FINISH_ACTIVITIES, 0) != 0);
    }

    private void updateAnimationScaleValue(int which, ListPreference pref) {
        try {
            float scale = mWindowManager.getAnimationScale(which);
            CharSequence[] values = pref.getEntryValues();
            for (int i=0; i<values.length; i++) {
                float val = Float.parseFloat(values[i].toString());
                if (scale <= val) {
                    pref.setValueIndex(i);
                    pref.setSummary(pref.getEntries()[i]);
                    return;
                }
            }
            pref.setValueIndex(values.length-1);
            pref.setSummary(pref.getEntries()[0]);
        } catch (RemoteException e) {
        }
    }

    private void updateAnimationScaleOptions() {
        updateAnimationScaleValue(0, mWindowAnimationScale);
        updateAnimationScaleValue(1, mTransitionAnimationScale);
        updateAnimationScaleValue(2, mAnimatorDurationScale);
    }

    private void writeAnimationScaleOption(int which, ListPreference pref, Object newValue) {
        try {
            float scale = newValue != null ? Float.parseFloat(newValue.toString()) : 1;
            mWindowManager.setAnimationScale(which, scale);
            updateAnimationScaleValue(which, pref);
        } catch (RemoteException e) {
        }
    }

    private void updateAppProcessLimitOptions() {
        try {
            int limit = ActivityManagerNative.getDefault().getProcessLimit();
            CharSequence[] values = mAppProcessLimit.getEntryValues();
            for (int i=0; i<values.length; i++) {
                int val = Integer.parseInt(values[i].toString());
                if (val >= limit) {
                    mAppProcessLimit.setValueIndex(i);
                    mAppProcessLimit.setSummary(mAppProcessLimit.getEntries()[i]);
                    return;
                }
            }
            mAppProcessLimit.setValueIndex(0);
            mAppProcessLimit.setSummary(mAppProcessLimit.getEntries()[0]);
        } catch (RemoteException e) {
        }
    }

    private void writeAppProcessLimitOptions(Object newValue) {
        try {
            int limit = newValue != null ? Integer.parseInt(newValue.toString()) : -1;
            ActivityManagerNative.getDefault().setProcessLimit(limit);
            updateAppProcessLimitOptions();
        } catch (RemoteException e) {
        }
    }

    private void writeShowAllANRsOptions() {
        Settings.Secure.putInt(getActivity().getContentResolver(),
                Settings.Secure.ANR_SHOW_BACKGROUND,
                mShowAllANRs.isChecked() ? 1 : 0);
    }

    private void updateShowAllANRsOptions() {
        mShowAllANRs.setChecked(Settings.Secure.getInt(
            getActivity().getContentResolver(), Settings.Secure.ANR_SHOW_BACKGROUND, 0) != 0);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mEnabledSwitch) {
            if (isChecked != mLastEnabledState) {
                if (isChecked) {
                    mDialogClicked = false;
                    if (mEnableDialog != null) dismissDialogs();
                    mEnableDialog = new AlertDialog.Builder(getActivity()).setMessage(
                            getActivity().getResources().getString(
                                    R.string.dev_settings_warning_message))
                            .setTitle(R.string.dev_settings_warning_title)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, this)
                            .setNegativeButton(android.R.string.no, this)
                            .show();
                    mEnableDialog.setOnDismissListener(this);
                } else {
                    resetDangerousOptions();
                    Settings.Secure.putInt(getActivity().getContentResolver(),
                            Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED, 0);
                    mLastEnabledState = isChecked;
                    setPrefsEnabledState(mLastEnabledState);
                }
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (Utils.isMonkeyRunning()) {
            return false;
        }

        if (preference == mEnableAdb) {
            if (mEnableAdb.isChecked()) {
                mDialogClicked = false;
                if (mAdbDialog != null) dismissDialogs();
                mAdbDialog = new AlertDialog.Builder(getActivity()).setMessage(
                        getActivity().getResources().getString(R.string.adb_warning_message))
                        .setTitle(R.string.adb_warning_title)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, this)
                        .setNegativeButton(android.R.string.no, this)
                        .show();
                mAdbDialog.setOnDismissListener(this);
            } else {
                Settings.Secure.putInt(getActivity().getContentResolver(),
                        Settings.Secure.ADB_ENABLED, 0);
            }
        } else if (preference == mKeepScreenOn) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STAY_ON_WHILE_PLUGGED_IN, 
                    mKeepScreenOn.isChecked() ? 
                    (BatteryManager.BATTERY_PLUGGED_AC | BatteryManager.BATTERY_PLUGGED_USB) : 0);
        } else if (preference == mAllowMockLocation) {
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.ALLOW_MOCK_LOCATION,
                    mAllowMockLocation.isChecked() ? 1 : 0);
        } else if (preference == mStrictMode) {
            writeStrictModeVisualOptions();
        } else if (preference == mPointerLocation) {
            writePointerLocationOptions();
        } else if (preference == mShowTouches) {
            writeShowTouchesOptions();
        } else if (preference == mShowScreenUpdates) {
            writeShowUpdatesOption();
        } else if (preference == mDisableOverlays) {
            writeDisableOverlaysOption();
        } else if (preference == mShowCpuUsage) {
            writeCpuUsageOptions();
        } else if (preference == mImmediatelyDestroyActivities) {
            writeImmediatelyDestroyActivitiesOptions();
        } else if (preference == mShowAllANRs) {
            writeShowAllANRsOptions();
        } else if (preference == mForceHardwareUi) {
            writeHardwareUiOptions();
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (HDCP_CHECKING_KEY.equals(preference.getKey())) {
            SystemProperties.set(HDCP_CHECKING_PROPERTY, newValue.toString());
            updateHdcpValues();
            return true;
        } else if (preference == mWindowAnimationScale) {
            writeAnimationScaleOption(0, mWindowAnimationScale, newValue);
            return true;
        } else if (preference == mTransitionAnimationScale) {
            writeAnimationScaleOption(1, mTransitionAnimationScale, newValue);
            return true;
        } else if (preference == mAnimatorDurationScale) {
            writeAnimationScaleOption(2, mAnimatorDurationScale, newValue);
            return true;
        } else if (preference == mAppProcessLimit) {
            writeAppProcessLimitOptions(newValue);
            return true;
        }
        return false;
    }

    private void dismissDialogs() {
        if (mAdbDialog != null) {
            mAdbDialog.dismiss();
            mAdbDialog = null;
        }
        if (mEnableDialog != null) {
            mEnableDialog.dismiss();
            mEnableDialog = null;
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (dialog == mAdbDialog) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                mDialogClicked = true;
                Settings.Secure.putInt(getActivity().getContentResolver(),
                        Settings.Secure.ADB_ENABLED, 1);
            } else {
                // Reset the toggle
                mEnableAdb.setChecked(false);
            }
        } else if (dialog == mEnableDialog) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                mDialogClicked = true;
                Settings.Secure.putInt(getActivity().getContentResolver(),
                        Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED, 1);
                mLastEnabledState = true;
                setPrefsEnabledState(mLastEnabledState);
            } else {
                // Reset the toggle
                mEnabledSwitch.setChecked(false);
            }
        }
    }

    public void onDismiss(DialogInterface dialog) {
        // Assuming that onClick gets called first
        if (dialog == mAdbDialog) {
            if (!mDialogClicked) {
                mEnableAdb.setChecked(false);
            }
            mAdbDialog = null;
        } else if (dialog == mEnableDialog) {
            if (!mDialogClicked) {
                mEnabledSwitch.setChecked(false);
            }
            mEnableDialog = null;
        }
    }

    @Override
    public void onDestroy() {
        dismissDialogs();
        super.onDestroy();
    }
}
