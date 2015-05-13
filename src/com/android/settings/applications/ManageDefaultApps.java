/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.applications;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.content.PackageMonitor;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ManageDefaultApps extends SettingsPreferenceFragment
        implements Preference.OnPreferenceClickListener, Indexable {

    private static final String TAG = ManageDefaultApps.class.getSimpleName();

    private static final String KEY_DEFAULT_BROWSER = "default_browser";
    private static final String KEY_DEFAULT_PHONE_APP = "default_phone_app";
    private static final String KEY_DEFAULT_EMERGENCY_APP = "default_emergency_app";
    private static final String KEY_SMS_APPLICATION = "default_sms_app";

    private DefaultBrowserPreference mDefaultBrowserPreference;
    private PackageManager mPm;
    private int myUserId;

    private static final long DELAY_UPDATE_BROWSER_MILLIS = 500;

    private final Handler mHandler = new Handler();

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateDefaultBrowserPreference();
        }
    };

    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            sendUpdate();
        }

        @Override
        public void onPackageAppeared(String packageName, int reason) {
            sendUpdate();
        }

        @Override
        public void onPackageDisappeared(String packageName, int reason) {
            sendUpdate();
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            sendUpdate();
        }

        private void sendUpdate() {
            mHandler.postDelayed(mUpdateRunnable, DELAY_UPDATE_BROWSER_MILLIS);
        }
    };

    private void updateDefaultBrowserPreference() {
        mDefaultBrowserPreference.refreshBrowserApps();

        final PackageManager pm = getPackageManager();

        String packageName = pm.getDefaultBrowserPackageName(UserHandle.myUserId());
        if (!TextUtils.isEmpty(packageName)) {
            // Check if the default Browser package is still there
            Intent intent = new Intent();
            intent.setPackage(packageName);
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setData(Uri.parse("http:"));

            ResolveInfo info = mPm.resolveActivityAsUser(intent, 0, myUserId);
            if (info != null) {
                mDefaultBrowserPreference.setValue(packageName);
                CharSequence label = info.loadLabel(pm);
                mDefaultBrowserPreference.setSummary(label);
            } else {
                mDefaultBrowserPreference.setSummary(R.string.default_browser_title_none);
            }
        } else {
            mDefaultBrowserPreference.setSummary(R.string.default_browser_title_none);
            Log.d(TAG, "Cannot set empty default Browser value!");
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.default_apps);

        mPm = getPackageManager();
        myUserId = UserHandle.myUserId();

        mDefaultBrowserPreference = (DefaultBrowserPreference) findPreference(KEY_DEFAULT_BROWSER);
        mDefaultBrowserPreference.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (newValue == null) {
                            return false;
                        }
                        final CharSequence packageName = (CharSequence) newValue;
                        if (TextUtils.isEmpty(packageName)) {
                            return false;
                        }
                        boolean result = mPm.setDefaultBrowserPackageName(
                                packageName.toString(), myUserId);
                        if (result) {
                            mDefaultBrowserPreference.setValue(packageName.toString());
                            final CharSequence appName = mDefaultBrowserPreference.getEntry();
                            mDefaultBrowserPreference.setSummary(appName);
                        }
                        return result;
                    }
        });
        final boolean isRestrictedUser = UserManager.get(getActivity())
                .getUserInfo(myUserId).isRestricted();

        // Restricted users cannot currently read/write SMS.
        // Remove SMS Application if the device does not support SMS
        if (isRestrictedUser || !DefaultSmsPreference.isAvailable(getActivity())) {
            removePreference(KEY_SMS_APPLICATION);
        }

        if (!DefaultPhonePreference.isAvailable(getActivity())) {
            removePreference(KEY_DEFAULT_PHONE_APP);
        }

        if (!DefaultEmergencyPreference.isAvailable(getActivity())) {
            removePreference(KEY_DEFAULT_EMERGENCY_APP);
        }

        if (DefaultEmergencyPreference.isCapable(getActivity())) {
            Index.getInstance(getActivity()).updateFromClassNameResource(
                    ManageDefaultApps.class.getName(), true, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDefaultBrowserPreference();
        mPackageMonitor.register(getActivity(), getActivity().getMainLooper(), false);
    }

    @Override
    public void onPause() {
        super.onPause();

        mPackageMonitor.unregister();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATIONS_DEFAULT_APPS;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                    Context context, boolean enabled) {
                SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.default_apps;
                return Arrays.asList(sir);
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final ArrayList<String> result = new ArrayList<String>();

                // Remove SMS Application if the device does not support SMS
                final boolean isRestrictedUser = UserManager.get(context)
                        .getUserInfo(UserHandle.myUserId()).isRestricted();
                if (!DefaultSmsPreference.isAvailable(context) || isRestrictedUser) {
                    result.add(KEY_SMS_APPLICATION);
                }

                if (!DefaultEmergencyPreference.isAvailable(context)) {
                    result.add(KEY_DEFAULT_EMERGENCY_APP);
                }

                return result;
            }
    };
}
