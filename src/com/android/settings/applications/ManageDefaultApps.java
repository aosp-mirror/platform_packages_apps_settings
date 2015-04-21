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

import android.annotation.Nullable;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.provider.SearchIndexableResource;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

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
    private static final String KEY_DEFAULT_DIALER = "default_dialer";
    private static final String KEY_DEFAULT_EMERGENCY_APP = "default_emergency_app";
    private static final String KEY_SMS_APPLICATION = "default_sms_app";

    private DefaultBrowserPreference mDefaultBrowserPreference;
    private PackageManager mPm;
    private int myUserId;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.default_apps);

        mPm = getPackageManager();
        myUserId = UserHandle.myUserId();

        mDefaultBrowserPreference = (DefaultBrowserPreference) findPreference(KEY_DEFAULT_BROWSER);
        mDefaultBrowserPreference.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        final CharSequence packageName = mDefaultBrowserPreference.getValue();
                        if (TextUtils.isEmpty(packageName)) {
                            return false;
                        }
                        return mPm.setDefaultBrowserPackageName(packageName.toString(), myUserId);
                    }
        });
        final boolean isRestrictedUser = UserManager.get(getActivity())
                .getUserInfo(myUserId).isRestricted();

        // Restricted users cannot currently read/write SMS.
        // Remove SMS Application if the device does not support SMS
        if (isRestrictedUser || !DefaultSmsPreference.isAvailable(getActivity())) {
            removePreference(KEY_SMS_APPLICATION);
        }

        if (!DefaultDialerPreference.isAvailable(getActivity())) {
            removePreference(KEY_DEFAULT_DIALER);
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
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String packageName = getPackageManager().getDefaultBrowserPackageName(
                UserHandle.myUserId());
        if (!TextUtils.isEmpty(packageName)) {
            // Check if the package is still there
            Intent intent = new Intent();
            intent.setPackage(packageName);
            ResolveInfo info = mPm.resolveActivityAsUser(intent, 0, myUserId);
            if (info != null) {
                mDefaultBrowserPreference.setValue(packageName);
            } else {
                // Otherwise select the first one
                mDefaultBrowserPreference.setValueIndex(0);
            }
        } else {
            Log.d(TAG, "Cannot set empty default Browser value!");
        }
    }

    @Override
    protected int getMetricsCategory() {
        return InstrumentedFragment.VIEW_CATEGORY_DEFAULT_APPS;
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
