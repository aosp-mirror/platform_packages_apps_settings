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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings.Secure;
import android.text.TextUtils;
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

    private final SettingObserver mObserver = new SettingObserver();

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
        getContentResolver().registerContentObserver(
                Secure.getUriFor(Secure.ENABLED_NOTIFICATION_POLICY_ACCESS_PACKAGES), false,
                mObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(mObserver);
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
            final CharSequence label = app.loadLabel(mPkgMan);
            final SwitchPreference pref = new SwitchPreference(mContext);
            pref.setPersistent(false);
            pref.setIcon(app.loadIcon(mPkgMan));
            pref.setTitle(label);
            pref.setChecked(hasAccess(pkg));
            pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final boolean access = (Boolean) newValue;
                    if (!access) {
                        // disabling access
                        setAccess(mContext, pkg, access);
                        return true;
                    }
                    // enabling access: show a scary dialog first
                    new ScaryWarningDialogFragment()
                            .setPkgInfo(pkg, label)
                            .show(getFragmentManager(), "dialog");
                    return false;
                }
            });
            screen.addPreference(pref);
        }
        mEmpty.setVisibility(apps.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean hasAccess(String pkg) {
        return mNoMan.isNotificationPolicyAccessGrantedForPackage(pkg);
    }

    private static void setAccess(final Context context, final String pkg, final boolean access) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final NotificationManager mgr = context.getSystemService(NotificationManager.class);
                mgr.setNotificationPolicyAccessGranted(pkg, access);
            }
        });
    }

    private final class SettingObserver extends ContentObserver {
        public SettingObserver() {
            super(new Handler(Looper.getMainLooper()));
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            reloadList();
        }
    }

    public static class ScaryWarningDialogFragment extends DialogFragment {
        static final String KEY_PKG = "p";
        static final String KEY_LABEL = "l";

        public ScaryWarningDialogFragment setPkgInfo(String pkg, CharSequence label) {
            Bundle args = new Bundle();
            args.putString(KEY_PKG, pkg);
            args.putString(KEY_LABEL, TextUtils.isEmpty(label) ? pkg : label.toString());
            setArguments(args);
            return this;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Bundle args = getArguments();
            final String pkg = args.getString(KEY_PKG);
            final String label = args.getString(KEY_LABEL);

            final String title = getResources().getString(R.string.zen_access_warning_dialog_title,
                    label);
            final String summary = getResources()
                    .getString(R.string.zen_access_warning_dialog_summary);
            return new AlertDialog.Builder(getContext())
                    .setMessage(summary)
                    .setTitle(title)
                    .setCancelable(true)
                    .setPositiveButton(R.string.allow,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    setAccess(getContext(), pkg, true);
                                }
                            })
                    .setNegativeButton(R.string.deny,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // pass
                                }
                            })
                    .create();
        }
    }
}
