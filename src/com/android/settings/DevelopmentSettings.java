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

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.ActivityThread;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.admin.DevicePolicyManager;
import android.app.backup.IBackupManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemProperties;
import android.os.Trace;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.MultiCheckPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.HardwareRenderer;
import android.view.IWindowManager;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.HashSet;

/*
 * Displays preferences for application developers.
 */
public class DevelopmentSettings extends PreferenceFragment
        implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener,
                OnPreferenceChangeListener, CompoundButton.OnCheckedChangeListener {

    private static final String ENABLE_ADB = "enable_adb";
    private static final String KEEP_SCREEN_ON = "keep_screen_on";
    private static final String ALLOW_MOCK_LOCATION = "allow_mock_location";
    private static final String HDCP_CHECKING_KEY = "hdcp_checking";
    private static final String HDCP_CHECKING_PROPERTY = "persist.sys.hdcp_checking";
    private static final String ENFORCE_READ_EXTERNAL = "enforce_read_external";
    private static final String LOCAL_BACKUP_PASSWORD = "local_backup_password";
    private static final String HARDWARE_UI_PROPERTY = "persist.sys.ui.hw";

    private static final String DEBUG_APP_KEY = "debug_app";
    private static final String WAIT_FOR_DEBUGGER_KEY = "wait_for_debugger";
    private static final String STRICT_MODE_KEY = "strict_mode";
    private static final String POINTER_LOCATION_KEY = "pointer_location";
    private static final String SHOW_TOUCHES_KEY = "show_touches";
    private static final String SHOW_SCREEN_UPDATES_KEY = "show_screen_updates";
    private static final String DISABLE_OVERLAYS_KEY = "disable_overlays";
    private static final String SHOW_CPU_USAGE_KEY = "show_cpu_usage";
    private static final String FORCE_HARDWARE_UI_KEY = "force_hw_ui";
    private static final String TRACK_FRAME_TIME_KEY = "track_frame_time";
    private static final String SHOW_HW_SCREEN_UPDATES_KEY = "show_hw_screen_udpates";
    private static final String DEBUG_LAYOUT_KEY = "debug_layout";
    private static final String WINDOW_ANIMATION_SCALE_KEY = "window_animation_scale";
    private static final String TRANSITION_ANIMATION_SCALE_KEY = "transition_animation_scale";
    private static final String ANIMATOR_DURATION_SCALE_KEY = "animator_duration_scale";

    private static final String ENABLE_TRACES_KEY = "enable_traces";

    private static final String IMMEDIATELY_DESTROY_ACTIVITIES_KEY
            = "immediately_destroy_activities";
    private static final String APP_PROCESS_LIMIT_KEY = "app_process_limit";

    private static final String SHOW_ALL_ANRS_KEY = "show_all_anrs";

    private static final String TAG_CONFIRM_ENFORCE = "confirm_enforce";

    private static final int RESULT_DEBUG_APP = 1000;

    private IWindowManager mWindowManager;
    private IBackupManager mBackupManager;
    private DevicePolicyManager mDpm;

    private Switch mEnabledSwitch;
    private boolean mLastEnabledState;
    private boolean mHaveDebugSettings;
    private boolean mDontPokeProperties;

    private CheckBoxPreference mEnableAdb;
    private CheckBoxPreference mKeepScreenOn;
    private CheckBoxPreference mEnforceReadExternal;
    private CheckBoxPreference mAllowMockLocation;
    private PreferenceScreen mPassword;

    private String mDebugApp;
    private Preference mDebugAppPref;
    private CheckBoxPreference mWaitForDebugger;

    private CheckBoxPreference mStrictMode;
    private CheckBoxPreference mPointerLocation;
    private CheckBoxPreference mShowTouches;
    private CheckBoxPreference mShowScreenUpdates;
    private CheckBoxPreference mDisableOverlays;
    private CheckBoxPreference mShowCpuUsage;
    private CheckBoxPreference mForceHardwareUi;
    private CheckBoxPreference mTrackFrameTime;
    private CheckBoxPreference mShowHwScreenUpdates;
    private CheckBoxPreference mDebugLayout;
    private ListPreference mWindowAnimationScale;
    private ListPreference mTransitionAnimationScale;
    private ListPreference mAnimatorDurationScale;
    private MultiCheckPreference mEnableTracesPref;

    private CheckBoxPreference mImmediatelyDestroyActivities;
    private ListPreference mAppProcessLimit;

    private CheckBoxPreference mShowAllANRs;

    private final ArrayList<Preference> mAllPrefs = new ArrayList<Preference>();
    private final ArrayList<CheckBoxPreference> mResetCbPrefs
            = new ArrayList<CheckBoxPreference>();

    private final HashSet<Preference> mDisabledPrefs = new HashSet<Preference>();

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
        mDpm = (DevicePolicyManager)getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);

        addPreferencesFromResource(R.xml.development_prefs);

        mEnableAdb = findAndInitCheckboxPref(ENABLE_ADB);
        mKeepScreenOn = findAndInitCheckboxPref(KEEP_SCREEN_ON);
        mEnforceReadExternal = findAndInitCheckboxPref(ENFORCE_READ_EXTERNAL);
        mAllowMockLocation = findAndInitCheckboxPref(ALLOW_MOCK_LOCATION);
        mPassword = (PreferenceScreen) findPreference(LOCAL_BACKUP_PASSWORD);
        mAllPrefs.add(mPassword);

        mDebugAppPref = findPreference(DEBUG_APP_KEY);
        mAllPrefs.add(mDebugAppPref);
        mWaitForDebugger = findAndInitCheckboxPref(WAIT_FOR_DEBUGGER_KEY);
        mStrictMode = findAndInitCheckboxPref(STRICT_MODE_KEY);
        mPointerLocation = findAndInitCheckboxPref(POINTER_LOCATION_KEY);
        mShowTouches = findAndInitCheckboxPref(SHOW_TOUCHES_KEY);
        mShowScreenUpdates = findAndInitCheckboxPref(SHOW_SCREEN_UPDATES_KEY);
        mDisableOverlays = findAndInitCheckboxPref(DISABLE_OVERLAYS_KEY);
        mShowCpuUsage = findAndInitCheckboxPref(SHOW_CPU_USAGE_KEY);
        mForceHardwareUi = findAndInitCheckboxPref(FORCE_HARDWARE_UI_KEY);
        mTrackFrameTime = findAndInitCheckboxPref(TRACK_FRAME_TIME_KEY);
        mShowHwScreenUpdates = findAndInitCheckboxPref(SHOW_HW_SCREEN_UPDATES_KEY);
        mDebugLayout = findAndInitCheckboxPref(DEBUG_LAYOUT_KEY);
        mWindowAnimationScale = (ListPreference) findPreference(WINDOW_ANIMATION_SCALE_KEY);
        mAllPrefs.add(mWindowAnimationScale);
        mWindowAnimationScale.setOnPreferenceChangeListener(this);
        mTransitionAnimationScale = (ListPreference) findPreference(TRANSITION_ANIMATION_SCALE_KEY);
        mAllPrefs.add(mTransitionAnimationScale);
        mTransitionAnimationScale.setOnPreferenceChangeListener(this);
        mAnimatorDurationScale = (ListPreference) findPreference(ANIMATOR_DURATION_SCALE_KEY);
        mAllPrefs.add(mAnimatorDurationScale);
        mAnimatorDurationScale.setOnPreferenceChangeListener(this);
        mEnableTracesPref = (MultiCheckPreference)findPreference(ENABLE_TRACES_KEY);
        String[] traceValues = new String[Trace.TRACE_TAGS.length];
        for (int i=Trace.TRACE_FLAGS_START_BIT; i<traceValues.length; i++) {
            traceValues[i] = Integer.toString(1<<i);
        }
        mEnableTracesPref.setEntries(Trace.TRACE_TAGS);
        mEnableTracesPref.setEntryValues(traceValues);
        mAllPrefs.add(mEnableTracesPref);
        mEnableTracesPref.setOnPreferenceChangeListener(this);

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

        Preference hdcpChecking = findPreference(HDCP_CHECKING_KEY);
        if (hdcpChecking != null) {
            mAllPrefs.add(hdcpChecking);
        }
        removeHdcpOptionsForProduction();
    }

    private CheckBoxPreference findAndInitCheckboxPref(String key) {
        CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
        if (pref == null) {
            throw new IllegalArgumentException("Cannot find preference with key = " + key);
        }
        mAllPrefs.add(pref);
        mResetCbPrefs.add(pref);
        return pref;
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
        for (int i = 0; i < mAllPrefs.size(); i++) {
            Preference pref = mAllPrefs.get(i);
            pref.setEnabled(enabled && !mDisabledPrefs.contains(pref));
        }
        updateAllOptions();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mDpm.getMaximumTimeToLock(null) > 0) {
            // A DeviceAdmin has specified a maximum time until the device
            // will lock...  in this case we can't allow the user to turn
            // on "stay awake when plugged in" because that would defeat the
            // restriction.
            mDisabledPrefs.add(mKeepScreenOn);
        } else {
            mDisabledPrefs.remove(mKeepScreenOn);
        }

        final ContentResolver cr = getActivity().getContentResolver();
        mLastEnabledState = Settings.Secure.getInt(cr,
                Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
        mEnabledSwitch.setChecked(mLastEnabledState);
        setPrefsEnabledState(mLastEnabledState);

        if (mHaveDebugSettings && !mLastEnabledState) {
            // Overall debugging is disabled, but there are some debug
            // settings that are enabled.  This is an invalid state.  Switch
            // to debug settings being enabled, so the user knows there is
            // stuff enabled and can turn it all off if they want.
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED, 1);
            mLastEnabledState = true;
            setPrefsEnabledState(mLastEnabledState);
        }
    }

    void updateCheckBox(CheckBoxPreference checkBox, boolean value) {
        checkBox.setChecked(value);
        mHaveDebugSettings |= value;
    }

    private void updateAllOptions() {
        final Context context = getActivity();
        final ContentResolver cr = context.getContentResolver();
        mHaveDebugSettings = false;
        updateCheckBox(mEnableAdb, Settings.Secure.getInt(cr,
                Settings.Secure.ADB_ENABLED, 0) != 0);
        updateCheckBox(mKeepScreenOn, Settings.System.getInt(cr,
                Settings.System.STAY_ON_WHILE_PLUGGED_IN, 0) != 0);
        updateCheckBox(mEnforceReadExternal, isPermissionEnforced(context, READ_EXTERNAL_STORAGE));
        updateCheckBox(mAllowMockLocation, Settings.Secure.getInt(cr,
                Settings.Secure.ALLOW_MOCK_LOCATION, 0) != 0);
        updateHdcpValues();
        updatePasswordSummary();
        updateDebuggerOptions();
        updateStrictModeVisualOptions();
        updatePointerLocationOptions();
        updateShowTouchesOptions();
        updateFlingerOptions();
        updateCpuUsageOptions();
        updateHardwareUiOptions();
        updateTrackFrameTimeOptions();
        updateShowHwScreenUpdatesOptions();
        updateDebugLayoutOptions();
        updateAnimationScaleOptions();
        updateEnableTracesOptions();
        updateImmediatelyDestroyActivitiesOptions();
        updateAppProcessLimitOptions();
        updateShowAllANRsOptions();
    }

    private void resetDangerousOptions() {
        mDontPokeProperties = true;
        for (int i=0; i<mResetCbPrefs.size(); i++) {
            CheckBoxPreference cb = mResetCbPrefs.get(i);
            if (cb.isChecked()) {
                cb.setChecked(false);
                onPreferenceTreeClick(null, cb);
            }
        }
        resetDebuggerOptions();
        writeAnimationScaleOption(0, mWindowAnimationScale, null);
        writeAnimationScaleOption(1, mTransitionAnimationScale, null);
        writeAnimationScaleOption(2, mAnimatorDurationScale, null);
        writeEnableTracesOptions(0);
        writeAppProcessLimitOptions(null);
        mHaveDebugSettings = false;
        updateAllOptions();
        mDontPokeProperties = false;
        pokeSystemProperties();
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

    private void writeDebuggerOptions() {
        try {
            ActivityManagerNative.getDefault().setDebugApp(
                mDebugApp, mWaitForDebugger.isChecked(), true);
        } catch (RemoteException ex) {
        }
    }

    private static void resetDebuggerOptions() {
        try {
            ActivityManagerNative.getDefault().setDebugApp(
                    null, false, true);
        } catch (RemoteException ex) {
        }
    }

    private void updateDebuggerOptions() {
        mDebugApp = Settings.System.getString(
                getActivity().getContentResolver(), Settings.System.DEBUG_APP);
        updateCheckBox(mWaitForDebugger, Settings.System.getInt(
                getActivity().getContentResolver(), Settings.System.WAIT_FOR_DEBUGGER, 0) != 0);
        if (mDebugApp != null && mDebugApp.length() > 0) {
            String label;
            try {
                ApplicationInfo ai = getActivity().getPackageManager().getApplicationInfo(mDebugApp,
                        PackageManager.GET_DISABLED_COMPONENTS);
                CharSequence lab = getActivity().getPackageManager().getApplicationLabel(ai);
                label = lab != null ? lab.toString() : mDebugApp;
            } catch (PackageManager.NameNotFoundException e) {
                label = mDebugApp;
            }
            mDebugAppPref.setSummary(getResources().getString(R.string.debug_app_set, label));
            mWaitForDebugger.setEnabled(true);
            mHaveDebugSettings = true;
        } else {
            mDebugAppPref.setSummary(getResources().getString(R.string.debug_app_not_set));
            mWaitForDebugger.setEnabled(false);
        }
    }

    // Returns the current state of the system property that controls
    // strictmode flashes.  One of:
    //    0: not explicitly set one way or another
    //    1: on
    //    2: off
    private static int currentStrictModeActiveIndex() {
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
        updateCheckBox(mStrictMode, currentStrictModeActiveIndex() == 1);
    }

    private void writePointerLocationOptions() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.POINTER_LOCATION, mPointerLocation.isChecked() ? 1 : 0);
    }

    private void updatePointerLocationOptions() {
        updateCheckBox(mPointerLocation, Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.POINTER_LOCATION, 0) != 0);
    }

    private void writeShowTouchesOptions() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.SHOW_TOUCHES, mShowTouches.isChecked() ? 1 : 0);
    }

    private void updateShowTouchesOptions() {
        updateCheckBox(mShowTouches, Settings.System.getInt(getActivity().getContentResolver(),
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
                updateCheckBox(mShowScreenUpdates, showUpdates != 0);
                @SuppressWarnings("unused")
                int showBackground = reply.readInt();
                int disableOverlays = reply.readInt();
                updateCheckBox(mDisableOverlays, disableOverlays != 0);
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
        updateCheckBox(mForceHardwareUi, SystemProperties.getBoolean(HARDWARE_UI_PROPERTY, false));
    }
    
    private void writeHardwareUiOptions() {
        SystemProperties.set(HARDWARE_UI_PROPERTY, mForceHardwareUi.isChecked() ? "true" : "false");
        pokeSystemProperties();
    }

    private void updateTrackFrameTimeOptions() {
        updateCheckBox(mTrackFrameTime,
                SystemProperties.getBoolean(HardwareRenderer.PROFILE_PROPERTY, false));
    }

    private void writeTrackFrameTimeOptions() {
        SystemProperties.set(HardwareRenderer.PROFILE_PROPERTY,
                mTrackFrameTime.isChecked() ? "true" : "false");
        pokeSystemProperties();
    }

    private void updateShowHwScreenUpdatesOptions() {
        updateCheckBox(mShowHwScreenUpdates,
                SystemProperties.getBoolean(HardwareRenderer.DEBUG_DIRTY_REGIONS_PROPERTY, false));
    }

    private void writeShowHwScreenUpdatesOptions() {
        SystemProperties.set(HardwareRenderer.DEBUG_DIRTY_REGIONS_PROPERTY,
                mShowHwScreenUpdates.isChecked() ? "true" : "false");
        pokeSystemProperties();
    }

    private void updateDebugLayoutOptions() {
        updateCheckBox(mDebugLayout,
                SystemProperties.getBoolean(View.DEBUG_LAYOUT_PROPERTY, false));
    }

    private void writeDebugLayoutOptions() {
        SystemProperties.set(View.DEBUG_LAYOUT_PROPERTY,
                mDebugLayout.isChecked() ? "true" : "false");
        pokeSystemProperties();
    }

    private void updateCpuUsageOptions() {
        updateCheckBox(mShowCpuUsage, Settings.System.getInt(getActivity().getContentResolver(),
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
        updateCheckBox(mImmediatelyDestroyActivities, Settings.System.getInt(
            getActivity().getContentResolver(), Settings.System.ALWAYS_FINISH_ACTIVITIES, 0) != 0);
    }

    private void updateAnimationScaleValue(int which, ListPreference pref) {
        try {
            float scale = mWindowManager.getAnimationScale(which);
            if (scale != 1) {
                mHaveDebugSettings = true;
            }
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
                    if (i != 0) {
                        mHaveDebugSettings = true;
                    }
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
        updateCheckBox(mShowAllANRs, Settings.Secure.getInt(
            getActivity().getContentResolver(), Settings.Secure.ANR_SHOW_BACKGROUND, 0) != 0);
    }

    private void updateEnableTracesOptions() {
        String strValue = SystemProperties.get(Trace.PROPERTY_TRACE_TAG_ENABLEFLAGS);
        long flags = SystemProperties.getLong(Trace.PROPERTY_TRACE_TAG_ENABLEFLAGS, 0);
        String[] values = mEnableTracesPref.getEntryValues();
        int numSet = 0;
        for (int i=Trace.TRACE_FLAGS_START_BIT; i<values.length; i++) {
            boolean set = (flags&(1<<i)) != 0;
            mEnableTracesPref.setValue(i-Trace.TRACE_FLAGS_START_BIT, set);
            if (set) {
                numSet++;
            }
        }
        if (numSet == 0) {
            mEnableTracesPref.setSummary(R.string.enable_traces_summary_none);
        } else if (numSet == values.length) {
            mHaveDebugSettings = true;
            mEnableTracesPref.setSummary(R.string.enable_traces_summary_all);
        } else {
            mHaveDebugSettings = true;
            mEnableTracesPref.setSummary(getString(R.string.enable_traces_summary_num, numSet));
        }
    }

    private void writeEnableTracesOptions() {
        long value = 0;
        String[] values = mEnableTracesPref.getEntryValues();
        for (int i=Trace.TRACE_FLAGS_START_BIT; i<values.length; i++) {
            if (mEnableTracesPref.getValue(i-Trace.TRACE_FLAGS_START_BIT)) {
                value |= 1<<i;
            }
        }
        writeEnableTracesOptions(value);
        // Make sure summary is updated.
        updateEnableTracesOptions();
    }

    private void writeEnableTracesOptions(long value) {
        SystemProperties.set(Trace.PROPERTY_TRACE_TAG_ENABLEFLAGS,
                "0x" + Long.toString(value, 16));
        pokeSystemProperties();
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
                            .setIconAttribute(android.R.attr.alertDialogIcon)
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_DEBUG_APP) {
            if (resultCode == Activity.RESULT_OK) {
                mDebugApp = data.getAction();
                writeDebuggerOptions();
                updateDebuggerOptions();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
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
                        .setIconAttribute(android.R.attr.alertDialogIcon)
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
        } else if (preference == mEnforceReadExternal) {
            if (mEnforceReadExternal.isChecked()) {
                ConfirmEnforceFragment.show(this);
            } else {
                setPermissionEnforced(getActivity(), READ_EXTERNAL_STORAGE, false);
            }
        } else if (preference == mAllowMockLocation) {
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.ALLOW_MOCK_LOCATION,
                    mAllowMockLocation.isChecked() ? 1 : 0);
        } else if (preference == mDebugAppPref) {
            startActivityForResult(new Intent(getActivity(), AppPicker.class), RESULT_DEBUG_APP);
        } else if (preference == mWaitForDebugger) {
            writeDebuggerOptions();
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
        } else if (preference == mTrackFrameTime) {
            writeTrackFrameTimeOptions();
        } else if (preference == mShowHwScreenUpdates) {
            writeShowHwScreenUpdatesOptions();
        } else if (preference == mDebugLayout) {
            writeDebugLayoutOptions();
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (HDCP_CHECKING_KEY.equals(preference.getKey())) {
            SystemProperties.set(HDCP_CHECKING_PROPERTY, newValue.toString());
            updateHdcpValues();
            pokeSystemProperties();
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
        } else if (preference == mEnableTracesPref) {
            writeEnableTracesOptions();
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

    void pokeSystemProperties() {
        if (!mDontPokeProperties) {
            (new SystemPropPoker()).execute();
        }
    }

    static class SystemPropPoker extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            String[] services;
            try {
                services = ServiceManager.listServices();
            } catch (RemoteException e) {
                return null;
            }
            for (String service : services) {
                IBinder obj = ServiceManager.checkService(service);
                if (obj != null) {
                    Parcel data = Parcel.obtain();
                    try {
                        obj.transact(IBinder.SYSPROPS_TRANSACTION, data, null, 0);
                    } catch (RemoteException e) {
                    }
                    data.recycle();
                }
            }
            return null;
        }
    }

    /**
     * Dialog to confirm enforcement of {@link #READ_EXTERNAL_STORAGE}.
     */
    public static class ConfirmEnforceFragment extends DialogFragment {
        public static void show(DevelopmentSettings parent) {
            final ConfirmEnforceFragment dialog = new ConfirmEnforceFragment();
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_ENFORCE);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.enforce_read_external_confirm_title);
            builder.setMessage(R.string.enforce_read_external_confirm_message);

            builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setPermissionEnforced(context, READ_EXTERNAL_STORAGE, true);
                    ((DevelopmentSettings) getTargetFragment()).updateAllOptions();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((DevelopmentSettings) getTargetFragment()).updateAllOptions();
                }
            });

            return builder.create();
        }
    }

    private static boolean isPermissionEnforced(Context context, String permission) {
        try {
            return ActivityThread.getPackageManager().isPermissionEnforced(READ_EXTERNAL_STORAGE);
        } catch (RemoteException e) {
            throw new RuntimeException("Problem talking with PackageManager", e);
        }
    }

    private static void setPermissionEnforced(
            Context context, String permission, boolean enforced) {
        try {
            // TODO: offload to background thread
            ActivityThread.getPackageManager()
                    .setPermissionEnforced(READ_EXTERNAL_STORAGE, enforced);
        } catch (RemoteException e) {
            throw new RuntimeException("Problem talking with PackageManager", e);
        }
    }
}
