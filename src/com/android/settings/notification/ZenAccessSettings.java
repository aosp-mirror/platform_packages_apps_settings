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

import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.FilterTouchesSwitchPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ZenAccessSettings extends EmptyTextSettings {
    private final String TAG = "ZenAccessSettings";

    private final SettingObserver mObserver = new SettingObserver();
    private Context mContext;
    private PackageManager mPkgMan;
    private NotificationManager mNoMan;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_ZEN_MODE_ACCESS;
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
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setEmptyText(R.string.zen_access_empty_text);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!ActivityManager.isLowRamDeviceStatic()) {
            reloadList();
            getContentResolver().registerContentObserver(
                    Secure.getUriFor(Secure.ENABLED_NOTIFICATION_POLICY_ACCESS_PACKAGES), false,
                    mObserver);
            getContentResolver().registerContentObserver(
                    Secure.getUriFor(Secure.ENABLED_NOTIFICATION_LISTENERS), false,
                    mObserver);
        } else {
            setEmptyText(R.string.disabled_low_ram_device);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!ActivityManager.isLowRamDeviceStatic()) {
            getContentResolver().unregisterContentObserver(mObserver);
        }
    }

    private void reloadList() {
        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();
        final ArrayList<ApplicationInfo> apps = new ArrayList<>();
        final ArraySet<String> requesting = getPackagesRequestingNotificationPolicyAccess();
        if (!requesting.isEmpty()) {
            final List<ApplicationInfo> installed = mPkgMan.getInstalledApplications(0);
            if (installed != null) {
                for (ApplicationInfo app : installed) {
                    if (requesting.contains(app.packageName)) {
                        apps.add(app);
                    }
                }
            }
        }
        ArraySet<String> autoApproved = new ArraySet<>();
        autoApproved.addAll(mNoMan.getEnabledNotificationListenerPackages());
        requesting.addAll(autoApproved);
        Collections.sort(apps, new PackageItemInfo.DisplayNameComparator(mPkgMan));
        for (ApplicationInfo app : apps) {
            final String pkg = app.packageName;
            final CharSequence label = app.loadLabel(mPkgMan);
            final FilterTouchesSwitchPreference pref = new FilterTouchesSwitchPreference(
                    getPrefContext());
            pref.setPersistent(false);
            pref.setIcon(app.loadIcon(mPkgMan));
            pref.setTitle(label);
            pref.setChecked(hasAccess(pkg));
            if (autoApproved.contains(pkg)) {
                pref.setEnabled(false);
                pref.setSummary(getString(R.string.zen_access_disabled_package_warning));
            }
            pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final boolean access = (Boolean) newValue;
                    if (access) {
                        new ScaryWarningDialogFragment()
                                .setPkgInfo(pkg, label)
                                .show(getFragmentManager(), "dialog");
                    } else {
                        new FriendlyWarningDialogFragment()
                                .setPkgInfo(pkg, label)
                                .show(getFragmentManager(), "dialog");
                    }
                    return false;
                }
            });
            screen.addPreference(pref);
        }
    }

    private ArraySet<String> getPackagesRequestingNotificationPolicyAccess() {
        ArraySet<String> requestingPackages = new ArraySet<>();
        try {
            final String[] PERM = {
                    android.Manifest.permission.ACCESS_NOTIFICATION_POLICY
            };
            final ParceledListSlice list = AppGlobals.getPackageManager()
                    .getPackagesHoldingPermissions(PERM, 0 /*flags*/,
                            ActivityManager.getCurrentUser());
            final List<PackageInfo> pkgs = list.getList();
            if (pkgs != null) {
                for (PackageInfo info : pkgs) {
                    requestingPackages.add(info.packageName);
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot reach packagemanager", e);
        }
        return requestingPackages;
    }

    private boolean hasAccess(String pkg) {
        return mNoMan.isNotificationPolicyAccessGrantedForPackage(pkg);
    }

    private static void setAccess(final Context context, final String pkg, final boolean access) {
        logSpecialPermissionChange(access, pkg, context);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final NotificationManager mgr = context.getSystemService(NotificationManager.class);
                mgr.setNotificationPolicyAccessGranted(pkg, access);
            }
        });
    }

    @VisibleForTesting
    static void logSpecialPermissionChange(boolean enable, String packageName, Context context) {
        int logCategory = enable ? MetricsEvent.APP_SPECIAL_PERMISSION_DND_ALLOW
                : MetricsEvent.APP_SPECIAL_PERMISSION_DND_DENY;
        FeatureFactory.getFactory(context).getMetricsFeatureProvider().action(context,
                logCategory, packageName);
    }


    private static void deleteRules(final Context context, final String pkg) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final NotificationManager mgr = context.getSystemService(NotificationManager.class);
                mgr.removeAutomaticZenRules(pkg);
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

    /**
     * Warning dialog when allowing zen access warning about the privileges being granted.
     */
    public static class ScaryWarningDialogFragment extends InstrumentedDialogFragment {
        static final String KEY_PKG = "p";
        static final String KEY_LABEL = "l";

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.DIALOG_ZEN_ACCESS_GRANT;
        }

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

    /**
     * Warning dialog when revoking zen access warning that zen rule instances will be deleted.
     */
    public static class FriendlyWarningDialogFragment extends InstrumentedDialogFragment {
        static final String KEY_PKG = "p";
        static final String KEY_LABEL = "l";


        @Override
        public int getMetricsCategory() {
            return MetricsEvent.DIALOG_ZEN_ACCESS_REVOKE;
        }

        public FriendlyWarningDialogFragment setPkgInfo(String pkg, CharSequence label) {
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

            final String title = getResources().getString(
                    R.string.zen_access_revoke_warning_dialog_title, label);
            final String summary = getResources()
                    .getString(R.string.zen_access_revoke_warning_dialog_summary);
            return new AlertDialog.Builder(getContext())
                    .setMessage(summary)
                    .setTitle(title)
                    .setCancelable(true)
                    .setPositiveButton(R.string.okay,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    deleteRules(getContext(), pkg);
                                    setAccess(getContext(), pkg, false);
                                }
                            })
                    .setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // pass
                                }
                            })
                    .create();
        }
    }
}
