/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.applications.appops;

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
import android.util.Log;
import android.util.SparseArray;

import com.android.settings.R;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class AppOpsState {
    static final String TAG = "AppOpsState";
    static final boolean DEBUG = false;

    final Context mContext;
    final AppOpsManager mAppOps;
    final PackageManager mPm;
    final CharSequence[] mOpSummaries;
    final CharSequence[] mOpLabels;

    public AppOpsState(Context context) {
        mContext = context;
        mAppOps = (AppOpsManager)context.getSystemService(Context.APP_OPS_SERVICE);
        mPm = context.getPackageManager();
        mOpSummaries = context.getResources().getTextArray(R.array.app_ops_summaries);
        mOpLabels = context.getResources().getTextArray(R.array.app_ops_labels);
    }

    public static class OpsTemplate implements Parcelable {
        public final int[] ops;
        public final boolean[] showPerms;

        public OpsTemplate(int[] _ops, boolean[] _showPerms) {
            ops = _ops;
            showPerms = _showPerms;
        }

        OpsTemplate(Parcel src) {
            ops = src.createIntArray();
            showPerms = src.createBooleanArray();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeIntArray(ops);
            dest.writeBooleanArray(showPerms);
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
                    AppOpsManager.OP_GPS,
                    AppOpsManager.OP_WIFI_SCAN,
                    AppOpsManager.OP_NEIGHBORING_CELLS,
                    AppOpsManager.OP_MONITOR_LOCATION,
                    AppOpsManager.OP_MONITOR_HIGH_POWER_LOCATION },
            new boolean[] { true,
                    true,
                    false,
                    false,
                    false,
                    false,
                    false }
            );

    public static final OpsTemplate PERSONAL_TEMPLATE = new OpsTemplate(
            new int[] { AppOpsManager.OP_READ_CONTACTS,
                    AppOpsManager.OP_WRITE_CONTACTS,
                    AppOpsManager.OP_READ_CALL_LOG,
                    AppOpsManager.OP_WRITE_CALL_LOG,
                    AppOpsManager.OP_READ_CALENDAR,
                    AppOpsManager.OP_WRITE_CALENDAR,
                    AppOpsManager.OP_READ_CLIPBOARD,
                    AppOpsManager.OP_WRITE_CLIPBOARD },
            new boolean[] { true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    false,
                    false }
            );

    public static final OpsTemplate MESSAGING_TEMPLATE = new OpsTemplate(
            new int[] { AppOpsManager.OP_READ_SMS,
                    AppOpsManager.OP_RECEIVE_SMS,
                    AppOpsManager.OP_RECEIVE_EMERGECY_SMS,
                    AppOpsManager.OP_RECEIVE_MMS,
                    AppOpsManager.OP_RECEIVE_WAP_PUSH,
                    AppOpsManager.OP_WRITE_SMS,
                    AppOpsManager.OP_SEND_SMS,
                    AppOpsManager.OP_READ_ICC_SMS,
                    AppOpsManager.OP_WRITE_ICC_SMS },
            new boolean[] { true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true }
            );

    public static final OpsTemplate MEDIA_TEMPLATE = new OpsTemplate(
            new int[] { AppOpsManager.OP_VIBRATE,
                    AppOpsManager.OP_CAMERA,
                    AppOpsManager.OP_RECORD_AUDIO,
                    AppOpsManager.OP_PLAY_AUDIO,
                    AppOpsManager.OP_TAKE_MEDIA_BUTTONS,
                    AppOpsManager.OP_TAKE_AUDIO_FOCUS,
                    AppOpsManager.OP_AUDIO_MASTER_VOLUME,
                    AppOpsManager.OP_AUDIO_VOICE_VOLUME,
                    AppOpsManager.OP_AUDIO_RING_VOLUME,
                    AppOpsManager.OP_AUDIO_MEDIA_VOLUME,
                    AppOpsManager.OP_AUDIO_ALARM_VOLUME,
                    AppOpsManager.OP_AUDIO_NOTIFICATION_VOLUME,
                    AppOpsManager.OP_AUDIO_BLUETOOTH_VOLUME,
                    AppOpsManager.OP_AUDIO_ACCESSIBILITY_VOLUME,
                    AppOpsManager.OP_MUTE_MICROPHONE},
            new boolean[] { false,
                    true,
                    true,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false }
            );

    public static final OpsTemplate DEVICE_TEMPLATE = new OpsTemplate(
            new int[] { AppOpsManager.OP_POST_NOTIFICATION,
                    AppOpsManager.OP_ACCESS_NOTIFICATIONS,
                    AppOpsManager.OP_CALL_PHONE,
                    AppOpsManager.OP_WRITE_SETTINGS,
                    AppOpsManager.OP_SYSTEM_ALERT_WINDOW,
                    AppOpsManager.OP_WAKE_LOCK,
                    AppOpsManager.OP_PROJECT_MEDIA,
                    AppOpsManager.OP_ACTIVATE_VPN,
                    AppOpsManager.OP_ASSIST_STRUCTURE,
                    AppOpsManager.OP_ASSIST_SCREENSHOT},
            new boolean[] { false,
                    true,
                    true,
                    true,
                    true,
                    true,
                    false,
                    false,
                    false,
                    false }
            );

    public static final OpsTemplate RUN_IN_BACKGROUND_TEMPLATE = new OpsTemplate(
            new int[] { AppOpsManager.OP_RUN_IN_BACKGROUND },
            new boolean[] { false }
            );

    public static final OpsTemplate[] ALL_TEMPLATES = new OpsTemplate[] {
            LOCATION_TEMPLATE, PERSONAL_TEMPLATE, MESSAGING_TEMPLATE,
            MEDIA_TEMPLATE, DEVICE_TEMPLATE, RUN_IN_BACKGROUND_TEMPLATE
    };

    /**
     * This class holds the per-item data in our Loader.
     */
    public static class AppEntry {
        private final AppOpsState mState;
        private final ApplicationInfo mInfo;
        private final File mApkFile;
        private final SparseArray<AppOpsManager.OpEntry> mOps
                = new SparseArray<AppOpsManager.OpEntry>();
        private final SparseArray<AppOpEntry> mOpSwitches
                = new SparseArray<AppOpEntry>();
        private String mLabel;
        private Drawable mIcon;
        private boolean mMounted;

        public AppEntry(AppOpsState state, ApplicationInfo info) {
            mState = state;
            mInfo = info;
            mApkFile = new File(info.sourceDir);
        }

        public void addOp(AppOpEntry entry, AppOpsManager.OpEntry op) {
            mOps.put(op.getOp(), op);
            mOpSwitches.put(AppOpsManager.opToSwitch(op.getOp()), entry);
        }

        public boolean hasOp(int op) {
            return mOps.indexOfKey(op) >= 0;
        }

        public AppOpEntry getOpSwitch(int op) {
            return mOpSwitches.get(AppOpsManager.opToSwitch(op));
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

            return mState.mContext.getDrawable(
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
        private final ArrayList<AppOpsManager.OpEntry> mSwitchOps
                = new ArrayList<AppOpsManager.OpEntry>();
        private final AppEntry mApp;
        private final int mSwitchOrder;
        private int mOverriddenPrimaryMode = -1;

        public AppOpEntry(AppOpsManager.PackageOps pkg, AppOpsManager.OpEntry op, AppEntry app,
                int switchOrder) {
            mPkgOps = pkg;
            mApp = app;
            mSwitchOrder = switchOrder;
            mApp.addOp(this, op);
            mOps.add(op);
            mSwitchOps.add(op);
        }

        private static void addOp(ArrayList<AppOpsManager.OpEntry> list, AppOpsManager.OpEntry op) {
            for (int i=0; i<list.size(); i++) {
                AppOpsManager.OpEntry pos = list.get(i);
                if (pos.isRunning() != op.isRunning()) {
                    if (op.isRunning()) {
                        list.add(i, op);
                        return;
                    }
                    continue;
                }
                if (pos.getTime() < op.getTime()) {
                    list.add(i, op);
                    return;
                }
            }
            list.add(op);
        }

        public void addOp(AppOpsManager.OpEntry op) {
            mApp.addOp(this, op);
            addOp(mOps, op);
            if (mApp.getOpSwitch(AppOpsManager.opToSwitch(op.getOp())) == null) {
                addOp(mSwitchOps, op);
            }
        }

        public AppEntry getAppEntry() {
            return mApp;
        }

        public int getSwitchOrder() {
            return mSwitchOrder;
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

        public int getPrimaryOpMode() {
            return mOverriddenPrimaryMode >= 0 ? mOverriddenPrimaryMode : mOps.get(0).getMode();
        }

        public void overridePrimaryOpMode(int mode) {
            mOverriddenPrimaryMode = mode;
        }

        private CharSequence getCombinedText(ArrayList<AppOpsManager.OpEntry> ops,
                CharSequence[] items) {
            if (ops.size() == 1) {
                return items[ops.get(0).getOp()];
            } else {
                StringBuilder builder = new StringBuilder();
                for (int i=0; i<ops.size(); i++) {
                    if (i > 0) {
                        builder.append(", ");
                    }
                    builder.append(items[ops.get(i).getOp()]);
                }
                return builder.toString();
            }
        }

        public CharSequence getSummaryText(AppOpsState state) {
            return getCombinedText(mOps, state.mOpSummaries);
        }

        public CharSequence getSwitchText(AppOpsState state) {
            if (mSwitchOps.size() > 0) {
                return getCombinedText(mSwitchOps, state.mOpLabels);
            } else {
                return getCombinedText(mOps, state.mOpLabels);
            }
        }

        public CharSequence getTimeText(Resources res, boolean showEmptyText) {
            if (isRunning()) {
                return res.getText(R.string.app_ops_running);
            }
            if (getTime() > 0) {
                return DateUtils.getRelativeTimeSpanString(getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE);
            }
            return showEmptyText ? res.getText(R.string.app_ops_never_used) : "";
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
     * Perform app op state comparison of application entry objects.
     */
    public static final Comparator<AppOpEntry> RECENCY_COMPARATOR = new Comparator<AppOpEntry>() {
        private final Collator sCollator = Collator.getInstance();
        @Override
        public int compare(AppOpEntry object1, AppOpEntry object2) {
            if (object1.getSwitchOrder() != object2.getSwitchOrder()) {
                return object1.getSwitchOrder() < object2.getSwitchOrder() ? -1 : 1;
            }
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

    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<AppOpEntry> LABEL_COMPARATOR = new Comparator<AppOpEntry>() {
        private final Collator sCollator = Collator.getInstance();
        @Override
        public int compare(AppOpEntry object1, AppOpEntry object2) {
            return sCollator.compare(object1.getAppEntry().getLabel(),
                    object2.getAppEntry().getLabel());
        }
    };

    private void addOp(List<AppOpEntry> entries, AppOpsManager.PackageOps pkgOps,
            AppEntry appEntry, AppOpsManager.OpEntry opEntry, boolean allowMerge, int switchOrder) {
        if (allowMerge && entries.size() > 0) {
            AppOpEntry last = entries.get(entries.size()-1);
            if (last.getAppEntry() == appEntry) {
                boolean lastExe = last.getTime() != 0;
                boolean entryExe = opEntry.getTime() != 0;
                if (lastExe == entryExe) {
                    if (DEBUG) Log.d(TAG, "Add op " + opEntry.getOp() + " to package "
                            + pkgOps.getPackageName() + ": append to " + last);
                    last.addOp(opEntry);
                    return;
                }
            }
        }
        AppOpEntry entry = appEntry.getOpSwitch(opEntry.getOp());
        if (entry != null) {
            entry.addOp(opEntry);
            return;
        }
        entry = new AppOpEntry(pkgOps, opEntry, appEntry, switchOrder);
        if (DEBUG) Log.d(TAG, "Add op " + opEntry.getOp() + " to package "
                + pkgOps.getPackageName() + ": making new " + entry);
        entries.add(entry);
    }

    public AppOpsManager getAppOpsManager() {
        return mAppOps;
    }

    public List<AppOpEntry> buildState(OpsTemplate tpl) {
        return buildState(tpl, 0, null, RECENCY_COMPARATOR);
    }

    private AppEntry getAppEntry(final Context context, final HashMap<String, AppEntry> appEntries,
            final String packageName, ApplicationInfo appInfo) {
        AppEntry appEntry = appEntries.get(packageName);
        if (appEntry == null) {
            if (appInfo == null) {
                try {
                    appInfo = mPm.getApplicationInfo(packageName,
                            PackageManager.MATCH_DISABLED_COMPONENTS
                            | PackageManager.MATCH_ANY_USER);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w(TAG, "Unable to find info for package " + packageName);
                    return null;
                }
            }
            appEntry = new AppEntry(this, appInfo);
            appEntry.loadLabel(context);
            appEntries.put(packageName, appEntry);
        }
        return appEntry;
    }

    public List<AppOpEntry> buildState(OpsTemplate tpl, int uid, String packageName) {
        return buildState(tpl, uid, packageName, RECENCY_COMPARATOR);
    }

    public List<AppOpEntry> buildState(OpsTemplate tpl, int uid, String packageName,
            Comparator<AppOpEntry> comparator) {
        final Context context = mContext;

        final HashMap<String, AppEntry> appEntries = new HashMap<String, AppEntry>();
        final List<AppOpEntry> entries = new ArrayList<AppOpEntry>();

        final ArrayList<String> perms = new ArrayList<String>();
        final ArrayList<Integer> permOps = new ArrayList<Integer>();
        final int[] opToOrder = new int[AppOpsManager._NUM_OP];
        for (int i=0; i<tpl.ops.length; i++) {
            if (tpl.showPerms[i]) {
                String perm = AppOpsManager.opToPermission(tpl.ops[i]);
                if (perm != null && !perms.contains(perm)) {
                    perms.add(perm);
                    permOps.add(tpl.ops[i]);
                    opToOrder[tpl.ops[i]] = i;
                }
            }
        }

        List<AppOpsManager.PackageOps> pkgs;
        if (packageName != null) {
            pkgs = mAppOps.getOpsForPackage(uid, packageName, tpl.ops);
        } else {
            pkgs = mAppOps.getPackagesForOps(tpl.ops);
        }

        if (pkgs != null) {
            for (int i=0; i<pkgs.size(); i++) {
                AppOpsManager.PackageOps pkgOps = pkgs.get(i);
                AppEntry appEntry = getAppEntry(context, appEntries, pkgOps.getPackageName(), null);
                if (appEntry == null) {
                    continue;
                }
                for (int j=0; j<pkgOps.getOps().size(); j++) {
                    AppOpsManager.OpEntry opEntry = pkgOps.getOps().get(j);
                    addOp(entries, pkgOps, appEntry, opEntry, packageName == null,
                            packageName == null ? 0 : opToOrder[opEntry.getOp()]);
                }
            }
        }

        List<PackageInfo> apps;
        if (packageName != null) {
            apps = new ArrayList<PackageInfo>();
            try {
                PackageInfo pi = mPm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
                apps.add(pi);
            } catch (NameNotFoundException e) {
            }
        } else {
            String[] permsArray = new String[perms.size()];
            perms.toArray(permsArray);
            apps = mPm.getPackagesHoldingPermissions(permsArray, 0);
        }
        for (int i=0; i<apps.size(); i++) {
            PackageInfo appInfo = apps.get(i);
            AppEntry appEntry = getAppEntry(context, appEntries, appInfo.packageName,
                    appInfo.applicationInfo);
            if (appEntry == null) {
                continue;
            }
            List<AppOpsManager.OpEntry> dummyOps = null;
            AppOpsManager.PackageOps pkgOps = null;
            if (appInfo.requestedPermissions != null) {
                for (int j=0; j<appInfo.requestedPermissions.length; j++) {
                    if (appInfo.requestedPermissionsFlags != null) {
                        if ((appInfo.requestedPermissionsFlags[j]
                                & PackageInfo.REQUESTED_PERMISSION_GRANTED) == 0) {
                            if (DEBUG) Log.d(TAG, "Pkg " + appInfo.packageName + " perm "
                                    + appInfo.requestedPermissions[j] + " not granted; skipping");
                            continue;
                        }
                    }
                    if (DEBUG) Log.d(TAG, "Pkg " + appInfo.packageName + ": requested perm "
                            + appInfo.requestedPermissions[j]);
                    for (int k=0; k<perms.size(); k++) {
                        if (!perms.get(k).equals(appInfo.requestedPermissions[j])) {
                            continue;
                        }
                        if (DEBUG) Log.d(TAG, "Pkg " + appInfo.packageName + " perm " + perms.get(k)
                                + " has op " + permOps.get(k) + ": " + appEntry.hasOp(permOps.get(k)));
                        if (appEntry.hasOp(permOps.get(k))) {
                            continue;
                        }
                        if (dummyOps == null) {
                            dummyOps = new ArrayList<AppOpsManager.OpEntry>();
                            pkgOps = new AppOpsManager.PackageOps(
                                    appInfo.packageName, appInfo.applicationInfo.uid, dummyOps);

                        }
                        AppOpsManager.OpEntry opEntry = new AppOpsManager.OpEntry(
                                permOps.get(k), AppOpsManager.MODE_ALLOWED, Collections.emptyMap());
                        dummyOps.add(opEntry);
                        addOp(entries, pkgOps, appEntry, opEntry, packageName == null,
                                packageName == null ? 0 : opToOrder[opEntry.getOp()]);
                    }
                }
            }
        }

        // Sort the list.
        Collections.sort(entries, comparator);

        // Done!
        return entries;
    }
}
