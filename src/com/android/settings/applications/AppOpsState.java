/**
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.settings.applications;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

import com.android.settings.R;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class AppOpsState {
    final Context mContext;
    final AppOpsManager mAppOps;
    final PackageManager mPm;

    List<AppOpEntry> mApps;

    public AppOpsState(Context context) {
        mContext = context;
        mAppOps = (AppOpsManager)context.getSystemService(Context.APP_OPS_SERVICE);
        mPm = context.getPackageManager();
    }

    public static class OpsTemplate implements Parcelable {
        public final int[] ops;
        public final String[] perms;
        public final int[] permOps;

        public OpsTemplate(int[] _ops, String[] _perms, int[] _permOps) {
            ops = _ops;
            perms = _perms;
            permOps = _permOps;
        }

        OpsTemplate(Parcel src) {
            ops = src.createIntArray();
            perms = src.createStringArray();
            permOps = src.createIntArray();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeIntArray(ops);
            dest.writeStringArray(perms);
            dest.writeIntArray(permOps);
        }

        public static final Creator<OpsTemplate> CREATOR = new Creator<OpsTemplate>() {
            @Override public OpsTemplate createFromParcel(Parcel source) {
                return new OpsTemplate(source);
            }

            @Override public OpsTemplate[] newArray(int size) {
                return new OpsTemplate[size];
            }
        };
    }

    public static final OpsTemplate LOCATION_TEMPLATE = new OpsTemplate(
            new int[] { AppOpsManager.OP_COARSE_LOCATION,
                    AppOpsManager.OP_FINE_LOCATION,
                    AppOpsManager.OP_GPS },
            new String[] { android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION },
            new int[] { AppOpsManager.OP_COARSE_LOCATION,
                    AppOpsManager.OP_FINE_LOCATION }
            );

    public static final OpsTemplate PERSONAL_TEMPLATE = new OpsTemplate(
            new int[] { AppOpsManager.OP_READ_CONTACTS,
                    AppOpsManager.OP_WRITE_CONTACTS,
                    AppOpsManager.OP_READ_CALL_LOG,
                    AppOpsManager.OP_WRITE_CALL_LOG },
            new String[] { android.Manifest.permission.READ_CONTACTS,
                            android.Manifest.permission.WRITE_CONTACTS,
                            android.Manifest.permission.READ_CALL_LOG,
                            android.Manifest.permission.WRITE_CALL_LOG },
            new int[] { AppOpsManager.OP_READ_CONTACTS,
                    AppOpsManager.OP_WRITE_CONTACTS,
                    AppOpsManager.OP_READ_CALL_LOG,
                    AppOpsManager.OP_WRITE_CALL_LOG }
            );

    public static final OpsTemplate DEVICE_TEMPLATE = new OpsTemplate(
            new int[] { AppOpsManager.OP_VIBRATE },
            new String[] { android.Manifest.permission.VIBRATE },
            new int[] { AppOpsManager.OP_VIBRATE }
            );

    /**
     * This class holds the per-item data in our Loader.
     */
    public static class AppEntry {
        private final AppOpsState mState;
        private final ApplicationInfo mInfo;
        private final File mApkFile;
        private String mLabel;
        private Drawable mIcon;
        private boolean mMounted;

        public AppEntry(AppOpsState state, ApplicationInfo info) {
            mState = state;
            mInfo = info;
            mApkFile = new File(info.sourceDir);
        }

        public ApplicationInfo getApplicationInfo() {
            return mInfo;
        }

        public String getLabel() {
            return mLabel;
        }

        public Drawable getIcon() {
            if (mIcon == null) {
                if (mApkFile.exists()) {
                    mIcon = mInfo.loadIcon(mState.mPm);
                    return mIcon;
                } else {
                    mMounted = false;
                }
            } else if (!mMounted) {
                // If the app wasn't mounted but is now mounted, reload
                // its icon.
                if (mApkFile.exists()) {
                    mMounted = true;
                    mIcon = mInfo.loadIcon(mState.mPm);
                    return mIcon;
                }
            } else {
                return mIcon;
            }

            return mState.mContext.getResources().getDrawable(
                    android.R.drawable.sym_def_app_icon);
        }

        @Override public String toString() {
            return mLabel;
        }

        void loadLabel(Context context) {
            if (mLabel == null || !mMounted) {
                if (!mApkFile.exists()) {
                    mMounted = false;
                    mLabel = mInfo.packageName;
                } else {
                    mMounted = true;
                    CharSequence label = mInfo.loadLabel(context.getPackageManager());
                    mLabel = label != null ? label.toString() : mInfo.packageName;
                }
            }
        }
    }

    /**
     * This class holds the per-item data in our Loader.
     */
    public static class AppOpEntry {
        private final AppOpsManager.PackageOps mPkgOps;
        private final ArrayList<AppOpsManager.OpEntry> mOps
                = new ArrayList<AppOpsManager.OpEntry>();
        private final AppEntry mApp;

        public AppOpEntry(AppOpsManager.PackageOps pkg, AppOpsManager.OpEntry op, AppEntry app) {
            mPkgOps = pkg;
            mOps.add(op);
            mApp = app;
        }

        public void addOp(AppOpsManager.OpEntry op) {
            for (int i=0; i<mOps.size(); i++) {
                AppOpsManager.OpEntry pos = mOps.get(i);
                if (pos.isRunning() != op.isRunning()) {
                    if (op.isRunning()) {
                        mOps.add(i, op);
                        return;
                    }
                }
                if (pos.getTime() > op.getTime()) {
                    mOps.add(i, op);
                    return;
                }
            }
            mOps.add(op);
        }

        public AppEntry getAppEntry() {
            return mApp;
        }

        public AppOpsManager.PackageOps getPackageOps() {
            return mPkgOps;
        }

        public int getNumOpEntry() {
            return mOps.size();
        }

        public AppOpsManager.OpEntry getOpEntry(int pos) {
            return mOps.get(pos);
        }

        public CharSequence getLabelText(CharSequence opNames[]) {
            if (getNumOpEntry() == 1) {
                return opNames[getOpEntry(0).getOp()];
            } else {
                StringBuilder builder = new StringBuilder();
                for (int i=0; i<getNumOpEntry(); i++) {
                    if (i > 0) {
                        builder.append(", ");
                    }
                    builder.append(opNames[getOpEntry(i).getOp()]);
                }
                return builder.toString();
            }
        }

        public CharSequence getTimeText(Resources res) {
            if (isRunning()) {
                return res.getText(R.string.app_ops_running);
            }
            if (getTime() > 0) {
                return DateUtils.getRelativeTimeSpanString(getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE);
            }
            return "";
        }

        public boolean isRunning() {
            return mOps.get(0).isRunning();
        }

        public long getTime() {
            return mOps.get(0).getTime();
        }

        @Override public String toString() {
            return mApp.getLabel();
        }
    }

    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<AppOpEntry> APP_OP_COMPARATOR = new Comparator<AppOpEntry>() {
        private final Collator sCollator = Collator.getInstance();
        @Override
        public int compare(AppOpEntry object1, AppOpEntry object2) {
            if (object1.isRunning() != object2.isRunning()) {
                // Currently running ops go first.
                return object1.isRunning() ? -1 : 1;
            }
            if (object1.getTime() != object2.getTime()) {
                // More recent times go first.
                return object1.getTime() > object2.getTime() ? -1 : 1;
            }
            return sCollator.compare(object1.getAppEntry().getLabel(),
                    object2.getAppEntry().getLabel());
        }
    };

    private void addOp(List<AppOpEntry> entries, AppOpsManager.PackageOps pkgOps,
            AppEntry appEntry, AppOpsManager.OpEntry opEntry) {
        if (entries.size() > 0) {
            AppOpEntry last = entries.get(entries.size()-1);
            if (last.getAppEntry() == appEntry) {
                boolean lastExe = last.getTime() != 0;
                boolean entryExe = opEntry.getTime() != 0;
                if (lastExe == entryExe) {
                    last.addOp(opEntry);
                    return;
                }
            }
        }
        AppOpEntry entry = new AppOpEntry(pkgOps, opEntry, appEntry);
        entries.add(entry);
    }

    public List<AppOpEntry> buildState(OpsTemplate tpl) {
        return buildState(tpl, 0, null);
    }

    public List<AppOpEntry> buildState(OpsTemplate tpl, int uid, String packageName) {
        final Context context = mContext;

        final HashMap<String, AppEntry> appEntries = new HashMap<String, AppEntry>();

        List<AppOpsManager.PackageOps> pkgs;
        if (packageName != null) {
            pkgs = mAppOps.getOpsForPackage(uid, packageName, tpl.ops);
        } else {
            pkgs = mAppOps.getPackagesForOps(tpl.ops);
        }
        List<AppOpEntry> entries = new ArrayList<AppOpEntry>(pkgs.size());
        for (int i=0; i<pkgs.size(); i++) {
            AppOpsManager.PackageOps pkgOps = pkgs.get(i);
            AppEntry appEntry = appEntries.get(pkgOps.getPackageName());
            if (appEntry == null) {
                ApplicationInfo appInfo = null;
                try {
                    appInfo = mPm.getApplicationInfo(pkgOps.getPackageName(),
                            PackageManager.GET_DISABLED_COMPONENTS
                            | PackageManager.GET_UNINSTALLED_PACKAGES);
                } catch (PackageManager.NameNotFoundException e) {
                }
                appEntry = new AppEntry(this, appInfo);
                appEntry.loadLabel(context);
                appEntries.put(pkgOps.getPackageName(), appEntry);
            }
            for (int j=0; j<pkgOps.getOps().size(); j++) {
                AppOpsManager.OpEntry opEntry = pkgOps.getOps().get(j);
                addOp(entries, pkgOps, appEntry, opEntry);
            }
        }

        if (tpl.perms != null) {
            List<PackageInfo> apps;
            if (packageName != null) {
                apps = new ArrayList<PackageInfo>();
                try {
                    PackageInfo pi = mPm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
                    apps.add(pi);
                } catch (NameNotFoundException e) {
                }
            } else {
                apps = mPm.getPackagesHoldingPermissions(tpl.perms, 0);
            }
            for (int i=0; i<apps.size(); i++) {
                PackageInfo appInfo = apps.get(i);
                AppEntry appEntry = appEntries.get(appInfo.packageName);
                if (appEntry == null) {
                    appEntry = new AppEntry(this, appInfo.applicationInfo);
                    appEntry.loadLabel(context);
                    appEntries.put(appInfo.packageName, appEntry);
                    List<AppOpsManager.OpEntry> dummyOps
                            = new ArrayList<AppOpsManager.OpEntry>();
                    AppOpsManager.PackageOps pkgOps = new AppOpsManager.PackageOps(
                            appInfo.packageName, appInfo.applicationInfo.uid, dummyOps);
                    for (int j=0; j<appInfo.requestedPermissions.length; j++) {
                        if (appInfo.requestedPermissionsFlags != null) {
                            if ((appInfo.requestedPermissionsFlags[j]
                                    & PackageInfo.REQUESTED_PERMISSION_GRANTED) == 0) {
                                break;
                            }
                        }
                        for (int k=0; k<tpl.perms.length; k++) {
                            if (tpl.perms[k].equals(appInfo.requestedPermissions[j])) {
                                AppOpsManager.OpEntry opEntry = new AppOpsManager.OpEntry(
                                        tpl.permOps[k], 0, 0);
                                dummyOps.add(opEntry);
                                addOp(entries, pkgOps, appEntry, opEntry);
                            }
                        }
                    }
                }
            }
        }

        // Sort the list.
        Collections.sort(entries, APP_OP_COMPARATOR);

        // Done!
        return entries;
    }
}
