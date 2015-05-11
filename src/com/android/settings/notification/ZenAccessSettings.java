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

package com.android.settings.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ZenAccessSettings extends SettingsPreferenceFragment {
    private Context mContext;
    private PackageManager mPkgMan;
    private NotificationManager mNoMan;
    private TextView mEmpty;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_ZEN_MODE_ACCESS;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = getActivity();
        mPkgMan = mContext.getPackageManager();
        mNoMan = mContext.getSystemService(NotificationManager.class);
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(mContext));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View v =  inflater.inflate(R.layout.managed_service_settings, container, false);
        mEmpty = (TextView) v.findViewById(android.R.id.empty);
        mEmpty.setText(R.string.zen_access_empty_text);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadList();
    }

    private void reloadList() {
        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();
        final ArrayList<ApplicationInfo> apps = new ArrayList<>();
        final ArraySet<String> requesting = mNoMan.getPackagesRequestingNotificationPolicyAccess();
        if (requesting != null && !requesting.isEmpty()) {
            final List<ApplicationInfo> installed = mPkgMan.getInstalledApplications(0);
            if (installed != null) {
                for (ApplicationInfo app : installed) {
                    if (requesting.contains(app.packageName)) {
                        apps.add(app);
                    }
                }
            }
        }
        Collections.sort(apps, new PackageItemInfo.DisplayNameComparator(mPkgMan));
        for (ApplicationInfo app : apps) {
            final String pkg = app.packageName;
            final SwitchPreference pref = new SwitchPreference(mContext);
            pref.setPersistent(false);
            pref.setIcon(app.loadIcon(mPkgMan));
            pref.setTitle(app.packageName);
            pref.setChecked(hasAccess(pkg));
            pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final boolean access = (Boolean) newValue;
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            setAccess(pkg, access);
                        }
                    });
                    return true;
                }
            });
            screen.addPreference(pref);
        }
        mEmpty.setVisibility(apps.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean hasAccess(String pkg) {
        return mNoMan.isNotificationPolicyAccessGrantedForPackage(pkg);
    }

    private void setAccess(String pkg, boolean access) {
        mNoMan.setNotificationPolicyAccessGranted(pkg, access);
    }

}
