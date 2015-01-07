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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceInfoSettings extends SettingsPreferenceFragment implements Indexable {

    private static final String LOG_TAG = "DeviceInfoSettings";
    private static final String FILENAME_PROC_VERSION = "/proc/version";
    private static final String FILENAME_MSV = "/sys/board_properties/soc/msv";

    private static final String KEY_CONTAINER = "container";
    private static final String KEY_REGULATORY_INFO = "regulatory_info";
    private static final String KEY_TERMS = "terms";
    private static final String KEY_LICENSE = "license";
    private static final String KEY_COPYRIGHT = "copyright";
    private static final String KEY_WEBVIEW_LICENSE = "webview_license";
    private static final String KEY_SYSTEM_UPDATE_SETTINGS = "system_update_settings";
    private static final String PROPERTY_URL_SAFETYLEGAL = "ro.url.safetylegal";
    private static final String PROPERTY_SELINUX_STATUS = "ro.build.selinux";
    private static final String KEY_KERNEL_VERSION = "kernel_version";
    private static final String KEY_BUILD_NUMBER = "build_number";
    private static final String KEY_DEVICE_MODEL = "device_model";
    private static final String KEY_SELINUX_STATUS = "selinux_status";
    private static final String KEY_BASEBAND_VERSION = "baseband_version";
    private static final String KEY_FIRMWARE_VERSION = "firmware_version";
    private static final String KEY_UPDATE_SETTING = "additional_system_update_settings";
    private static final String KEY_EQUIPMENT_ID = "fcc_equipment_id";
    private static final String PROPERTY_EQUIPMENT_ID = "ro.ril.fccid";
    private static final String KEY_DEVICE_FEEDBACK = "device_feedback";
    private static final String KEY_SAFETY_LEGAL = "safetylegal";

    static final int TAPS_TO_BE_A_DEVELOPER = 7;

    long[] mHits = new long[3];
    int mDevHitCountdown;
    Toast mDevHitToast;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.device_info_settings);

        setStringSummary(KEY_FIRMWARE_VERSION, Build.VERSION.RELEASE);
        findPreference(KEY_FIRMWARE_VERSION).setEnabled(true);
        setValueSummary(KEY_BASEBAND_VERSION, "gsm.version.baseband");
        setStringSummary(KEY_DEVICE_MODEL, Build.MODEL + getMsvSuffix());
        setValueSummary(KEY_EQUIPMENT_ID, PROPERTY_EQUIPMENT_ID);
        setStringSummary(KEY_DEVICE_MODEL, Build.MODEL);
        setStringSummary(KEY_BUILD_NUMBER, Build.DISPLAY);
        findPreference(KEY_BUILD_NUMBER).setEnabled(true);
        findPreference(KEY_KERNEL_VERSION).setSummary(getFormattedKernelVersion());

        if (!SELinux.isSELinuxEnabled()) {
            String status = getResources().getString(R.string.selinux_status_disabled);
            setStringSummary(KEY_SELINUX_STATUS, status);
        } else if (!SELinux.isSELinuxEnforced()) {
            String status = getResources().getString(R.string.selinux_status_permissive);
            setStringSummary(KEY_SELINUX_STATUS, status);
        }

        // Remove selinux information if property is not present
        removePreferenceIfPropertyMissing(getPreferenceScreen(), KEY_SELINUX_STATUS,
                PROPERTY_SELINUX_STATUS);

        // Remove Safety information preference if PROPERTY_URL_SAFETYLEGAL is not set
        removePreferenceIfPropertyMissing(getPreferenceScreen(), KEY_SAFETY_LEGAL,
                PROPERTY_URL_SAFETYLEGAL);

        // Remove Equipment id preference if FCC ID is not set by RIL
        removePreferenceIfPropertyMissing(getPreferenceScreen(), KEY_EQUIPMENT_ID,
                PROPERTY_EQUIPMENT_ID);

        // Remove Baseband version if wifi-only device
        if (Utils.isWifiOnly(getActivity())) {
            getPreferenceScreen().removePreference(findPreference(KEY_BASEBAND_VERSION));
        }

        // Dont show feedback option if there is no reporter.
        if (TextUtils.isEmpty(getFeedbackReporterPackage(getActivity()))) {
            getPreferenceScreen().removePreference(findPreference(KEY_DEVICE_FEEDBACK));
        }

        /*
         * Settings is a generic app and should not contain any device-specific
         * info.
         */
        final Activity act = getActivity();
        // These are contained in the "container" preference group
        PreferenceGroup parentPreference = (PreferenceGroup) findPreference(KEY_CONTAINER);
        Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference, KEY_TERMS,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference, KEY_LICENSE,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference, KEY_COPYRIGHT,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference, KEY_WEBVIEW_LICENSE,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);

        // These are contained by the root preference screen
        parentPreference = getPreferenceScreen();
        if (UserHandle.myUserId() == UserHandle.USER_OWNER) {
            Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference,
                    KEY_SYSTEM_UPDATE_SETTINGS,
                    Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        } else {
            // Remove for secondary users
            removePreference(KEY_SYSTEM_UPDATE_SETTINGS);
        }

        // Read platform settings for additional system update setting
        removePreferenceIfBoolFalse(KEY_UPDATE_SETTING,
                R.bool.config_additional_system_update_setting_enable);

        // Remove regulatory information if none present.
        final Intent intent = new Intent(Settings.ACTION_SHOW_REGULATORY_INFO);
        if (getPackageManager().queryIntentActivities(intent, 0).isEmpty()) {
            Preference pref = findPreference(KEY_REGULATORY_INFO);
            if (pref != null) {
                getPreferenceScreen().removePreference(pref);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mDevHitCountdown = getActivity().getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE).getBoolean(DevelopmentSettings.PREF_SHOW,
                        android.os.Build.TYPE.equals("eng")) ? -1 : TAPS_TO_BE_A_DEVELOPER;
        mDevHitToast = null;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals(KEY_FIRMWARE_VERSION)) {
            System.arraycopy(mHits, 1, mHits, 0, mHits.length-1);
            mHits[mHits.length-1] = SystemClock.uptimeMillis();
            if (mHits[0] >= (SystemClock.uptimeMillis()-500)) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("android",
                        com.android.internal.app.PlatLogoActivity.class.getName());
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Unable to start activity " + intent.toString());
                }
            }
        } else if (preference.getKey().equals(KEY_BUILD_NUMBER)) {
            // Don't enable developer options for secondary users.
            if (UserHandle.myUserId() != UserHandle.USER_OWNER) return true;

            final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
            if (um.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES)) return true;

            if (mDevHitCountdown > 0) {
                mDevHitCountdown--;
                if (mDevHitCountdown == 0) {
                    getActivity().getSharedPreferences(DevelopmentSettings.PREF_FILE,
                            Context.MODE_PRIVATE).edit().putBoolean(
                                    DevelopmentSettings.PREF_SHOW, true).apply();
                    if (mDevHitToast != null) {
                        mDevHitToast.cancel();
                    }
                    mDevHitToast = Toast.makeText(getActivity(), R.string.show_dev_on,
                            Toast.LENGTH_LONG);
                    mDevHitToast.show();
                    // This is good time to index the Developer Options
                    Index.getInstance(
                            getActivity().getApplicationContext()).updateFromClassNameResource(
                                    DevelopmentSettings.class.getName(), true, true);

                } else if (mDevHitCountdown > 0
                        && mDevHitCountdown < (TAPS_TO_BE_A_DEVELOPER-2)) {
                    if (mDevHitToast != null) {
                        mDevHitToast.cancel();
                    }
                    mDevHitToast = Toast.makeText(getActivity(), getResources().getQuantityString(
                            R.plurals.show_dev_countdown, mDevHitCountdown, mDevHitCountdown),
                            Toast.LENGTH_SHORT);
                    mDevHitToast.show();
                }
            } else if (mDevHitCountdown < 0) {
                if (mDevHitToast != null) {
                    mDevHitToast.cancel();
                }
                mDevHitToast = Toast.makeText(getActivity(), R.string.show_dev_already,
                        Toast.LENGTH_LONG);
                mDevHitToast.show();
            }
        } else if (preference.getKey().equals(KEY_DEVICE_FEEDBACK)) {
            sendFeedback();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void removePreferenceIfPropertyMissing(PreferenceGroup preferenceGroup,
            String preference, String property ) {
        if (SystemProperties.get(property).equals("")) {
            // Property is missing so remove preference from group
            try {
                preferenceGroup.removePreference(findPreference(preference));
            } catch (RuntimeException e) {
                Log.d(LOG_TAG, "Property '" + property + "' missing and no '"
                        + preference + "' preference");
            }
        }
    }

    private void removePreferenceIfBoolFalse(String preference, int resId) {
        if (!getResources().getBoolean(resId)) {
            Preference pref = findPreference(preference);
            if (pref != null) {
                getPreferenceScreen().removePreference(pref);
            }
        }
    }

    private void setStringSummary(String preference, String value) {
        try {
            findPreference(preference).setSummary(value);
        } catch (RuntimeException e) {
            findPreference(preference).setSummary(
                getResources().getString(R.string.device_info_default));
        }
    }

    private void setValueSummary(String preference, String property) {
        try {
            findPreference(preference).setSummary(
                    SystemProperties.get(property,
                            getResources().getString(R.string.device_info_default)));
        } catch (RuntimeException e) {
            // No recovery
        }
    }

    private void sendFeedback() {
        String reporterPackage = getFeedbackReporterPackage(getActivity());
        if (TextUtils.isEmpty(reporterPackage)) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_BUG_REPORT);
        intent.setPackage(reporterPackage);
        startActivityForResult(intent, 0);
    }

    /**
     * Reads a line from the specified file.
     * @param filename the file to read from
     * @return the first line, if any.
     * @throws IOException if the file couldn't be read
     */
    private static String readLine(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    public static String getFormattedKernelVersion() {
        try {
            return formatKernelVersion(readLine(FILENAME_PROC_VERSION));

        } catch (IOException e) {
            Log.e(LOG_TAG,
                "IO Exception when getting kernel version for Device Info screen",
                e);

            return "Unavailable";
        }
    }

    public static String formatKernelVersion(String rawKernelVersion) {
        // Example (see tests for more):
        // Linux version 3.0.31-g6fb96c9 (android-build@xxx.xxx.xxx.xxx.com) \
        //     (gcc version 4.6.x-xxx 20120106 (prerelease) (GCC) ) #1 SMP PREEMPT \
        //     Thu Jun 28 11:02:39 PDT 2012

        final String PROC_VERSION_REGEX =
            "Linux version (\\S+) " + /* group 1: "3.0.31-g6fb96c9" */
            "\\((\\S+?)\\) " +        /* group 2: "x@y.com" (kernel builder) */
            "(?:\\(gcc.+? \\)) " +    /* ignore: GCC version information */
            "(#\\d+) " +              /* group 3: "#1" */
            "(?:.*?)?" +              /* ignore: optional SMP, PREEMPT, and any CONFIG_FLAGS */
            "((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)"; /* group 4: "Thu Jun 28 11:02:39 PDT 2012" */

        Matcher m = Pattern.compile(PROC_VERSION_REGEX).matcher(rawKernelVersion);
        if (!m.matches()) {
            Log.e(LOG_TAG, "Regex did not match on /proc/version: " + rawKernelVersion);
            return "Unavailable";
        } else if (m.groupCount() < 4) {
            Log.e(LOG_TAG, "Regex match on /proc/version only returned " + m.groupCount()
                    + " groups");
            return "Unavailable";
        }
        return m.group(1) + "\n" +                 // 3.0.31-g6fb96c9
            m.group(2) + " " + m.group(3) + "\n" + // x@y.com #1
            m.group(4);                            // Thu Jun 28 11:02:39 PDT 2012
    }

    /**
     * Returns " (ENGINEERING)" if the msv file has a zero value, else returns "".
     * @return a string to append to the model number description.
     */
    private String getMsvSuffix() {
        // Production devices should have a non-zero value. If we can't read it, assume it's a
        // production device so that we don't accidentally show that it's an ENGINEERING device.
        try {
            String msv = readLine(FILENAME_MSV);
            // Parse as a hex number. If it evaluates to a zero, then it's an engineering build.
            if (Long.parseLong(msv, 16) == 0) {
                return " (ENGINEERING)";
            }
        } catch (IOException ioe) {
            // Fail quietly, as the file may not exist on some devices.
        } catch (NumberFormatException nfe) {
            // Fail quietly, returning empty string should be sufficient
        }
        return "";
    }

    private static String getFeedbackReporterPackage(Context context) {
        final String feedbackReporter =
                context.getResources().getString(R.string.oem_preferred_feedback_reporter);
        if (TextUtils.isEmpty(feedbackReporter)) {
            // Reporter not configured. Return.
            return feedbackReporter;
        }
        // Additional checks to ensure the reporter is on system image, and reporter is
        // configured to listen to the intent. Otherwise, dont show the "send feedback" option.
        final Intent intent = new Intent(Intent.ACTION_BUG_REPORT);

        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolvedPackages =
                pm.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
        for (ResolveInfo info : resolvedPackages) {
            if (info.activityInfo != null) {
                if (!TextUtils.isEmpty(info.activityInfo.packageName)) {
                    try {
                        ApplicationInfo ai = pm.getApplicationInfo(info.activityInfo.packageName, 0);
                        if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                            // Package is on the system image
                            if (TextUtils.equals(
                                        info.activityInfo.packageName, feedbackReporter)) {
                                return feedbackReporter;
                            }
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                         // No need to do anything here.
                    }
                }
            }
        }
        return null;
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
                sir.xmlResId = R.xml.device_info_settings;
                return Arrays.asList(sir);
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = new ArrayList<String>();
                if (isPropertyMissing(PROPERTY_SELINUX_STATUS)) {
                    keys.add(KEY_SELINUX_STATUS);
                }
                if (isPropertyMissing(PROPERTY_URL_SAFETYLEGAL)) {
                    keys.add(KEY_SAFETY_LEGAL);
                }
                if (isPropertyMissing(PROPERTY_EQUIPMENT_ID)) {
                    keys.add(KEY_EQUIPMENT_ID);
                }
                // Remove Baseband version if wifi-only device
                if (Utils.isWifiOnly(context)) {
                    keys.add((KEY_BASEBAND_VERSION));
                }
                // Dont show feedback option if there is no reporter.
                if (TextUtils.isEmpty(getFeedbackReporterPackage(context))) {
                    keys.add(KEY_DEVICE_FEEDBACK);
                }
                if (!checkIntentAction(context, "android.settings.TERMS")) {
                    keys.add(KEY_TERMS);
                }
                if (!checkIntentAction(context, "android.settings.LICENSE")) {
                    keys.add(KEY_LICENSE);
                }
                if (!checkIntentAction(context, "android.settings.COPYRIGHT")) {
                    keys.add(KEY_COPYRIGHT);
                }
                if (!checkIntentAction(context, "android.settings.WEBVIEW_LICENSE")) {
                    keys.add(KEY_WEBVIEW_LICENSE);
                }
                if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
                    keys.add(KEY_SYSTEM_UPDATE_SETTINGS);
                }
                if (!context.getResources().getBoolean(
                        R.bool.config_additional_system_update_setting_enable)) {
                    keys.add(KEY_UPDATE_SETTING);
                }
                return keys;
            }

            private boolean isPropertyMissing(String property) {
                return SystemProperties.get(property).equals("");
            }

            private boolean checkIntentAction(Context context, String action) {
                final Intent intent = new Intent(action);

                // Find the activity that is in the system image
                final PackageManager pm = context.getPackageManager();
                final List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
                final int listSize = list.size();

                for (int i = 0; i < listSize; i++) {
                    ResolveInfo resolveInfo = list.get(i);
                    if ((resolveInfo.activityInfo.applicationInfo.flags &
                            ApplicationInfo.FLAG_SYSTEM) != 0) {
                        return true;
                    }
                }

                return false;
            }
        };

}

