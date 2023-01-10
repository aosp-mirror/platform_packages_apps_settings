/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage;

import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.BatteryConsumer;
import android.os.Handler;
import android.os.Process;
import android.os.RemoteException;
import android.os.UidBatteryConsumer;
import android.os.UserBatteryConsumer;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.DebugUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settingslib.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

/**
 * Wraps the power usage data of a BatterySipper with information about package name
 * and icon image.
 */
public class BatteryEntry {

    /** The app name and icon in app list. */
    public static final class NameAndIcon {
        public final String mName;
        public final String mPackageName;
        public final Drawable mIcon;
        public final int mIconId;

        public NameAndIcon(String name, Drawable icon, int iconId) {
            this(name, /*packageName=*/ null, icon, iconId);
        }

        public NameAndIcon(
                String name, String packageName, Drawable icon, int iconId) {
            this.mName = name;
            this.mIcon = icon;
            this.mIconId = iconId;
            this.mPackageName = packageName;
        }
    }

    public static final int MSG_UPDATE_NAME_ICON = 1;
    public static final int MSG_REPORT_FULLY_DRAWN = 2;

    private static final String TAG = "BatteryEntry";
    private static final String PACKAGE_SYSTEM = "android";

    static final HashMap<String, UidToDetail> sUidCache = new HashMap<>();

    static final ArrayList<BatteryEntry> sRequestQueue = new ArrayList<BatteryEntry>();
    static Handler sHandler;

    static Locale sCurrentLocale = null;

    private static class NameAndIconLoader extends Thread {
        private boolean mAbort = false;

        NameAndIconLoader() {
            super("BatteryUsage Icon Loader");
        }

        public void abort() {
            mAbort = true;
        }

        @Override
        public void run() {
            while (true) {
                BatteryEntry be;
                synchronized (sRequestQueue) {
                    if (sRequestQueue.isEmpty() || mAbort) {
                        if (sHandler != null) {
                            sHandler.sendEmptyMessage(MSG_REPORT_FULLY_DRAWN);
                        }
                        return;
                    }
                    be = sRequestQueue.remove(0);
                }
                final NameAndIcon nameAndIcon =
                        BatteryEntry.loadNameAndIcon(
                                be.mContext, be.getUid(), sHandler, be,
                                be.mDefaultPackageName, be.mName, be.mIcon);
                if (nameAndIcon != null) {
                    be.mIcon = nameAndIcon.mIcon;
                    be.mName = nameAndIcon.mName;
                    be.mDefaultPackageName = nameAndIcon.mPackageName;
                }
            }
        }
    }

    private static NameAndIconLoader sRequestThread;

    /** Starts the request queue. */
    public static void startRequestQueue() {
        if (sHandler != null) {
            synchronized (sRequestQueue) {
                if (!sRequestQueue.isEmpty()) {
                    if (sRequestThread != null) {
                        sRequestThread.abort();
                    }
                    sRequestThread = new NameAndIconLoader();
                    sRequestThread.setPriority(Thread.MIN_PRIORITY);
                    sRequestThread.start();
                    sRequestQueue.notify();
                }
            }
        }
    }

    /** Stops the request queue. */
    public static void stopRequestQueue() {
        synchronized (sRequestQueue) {
            if (sRequestThread != null) {
                sRequestThread.abort();
                sRequestThread = null;
                sRequestQueue.clear();
                sHandler = null;
            }
        }
    }

    /** Clears the UID cache. */
    public static void clearUidCache() {
        sUidCache.clear();
    }

    public static final Comparator<BatteryEntry> COMPARATOR =
            (a, b) -> Double.compare(b.getConsumedPower(), a.getConsumedPower());

    private final Context mContext;
    private final BatteryConsumer mBatteryConsumer;
    private final int mUid;
    private final boolean mIsHidden;
    @ConvertUtils.ConsumerType
    private final int mConsumerType;
    @BatteryConsumer.PowerComponent
    private final int mPowerComponentId;
    private long mUsageDurationMs;
    private long mTimeInForegroundMs;
    private long mTimeInBackgroundMs;

    public String mName;
    public Drawable mIcon;
    public int mIconId;
    public double mPercent;
    private String mDefaultPackageName;
    private double mConsumedPower;

    static class UidToDetail {
        String mName;
        String mPackageName;
        Drawable mIcon;
    }

    public BatteryEntry(Context context, Handler handler, UserManager um,
            BatteryConsumer batteryConsumer, boolean isHidden, int uid, String[] packages,
            String packageName) {
        this(context, handler, um, batteryConsumer, isHidden, uid, packages, packageName, true);
    }

    public BatteryEntry(Context context, Handler handler, UserManager um,
            BatteryConsumer batteryConsumer, boolean isHidden, int uid, String[] packages,
            String packageName, boolean loadDataInBackground) {
        sHandler = handler;
        mContext = context;
        mBatteryConsumer = batteryConsumer;
        mIsHidden = isHidden;
        mDefaultPackageName = packageName;
        mPowerComponentId = -1;

        if (batteryConsumer instanceof UidBatteryConsumer) {
            mUid = uid;
            mConsumerType = ConvertUtils.CONSUMER_TYPE_UID_BATTERY;
            mConsumedPower = batteryConsumer.getConsumedPower();

            UidBatteryConsumer uidBatteryConsumer = (UidBatteryConsumer) batteryConsumer;
            if (mDefaultPackageName == null) {
                // Apps should only have one package
                if (packages != null && packages.length == 1) {
                    mDefaultPackageName = packages[0];
                } else {
                    mDefaultPackageName = isSystemUid(uid)
                            ? PACKAGE_SYSTEM : uidBatteryConsumer.getPackageWithHighestDrain();
                }
            }
            if (mDefaultPackageName != null) {
                PackageManager pm = context.getPackageManager();
                try {
                    ApplicationInfo appInfo =
                            pm.getApplicationInfo(mDefaultPackageName, 0 /* no flags */);
                    mName = pm.getApplicationLabel(appInfo).toString();
                } catch (NameNotFoundException e) {
                    Log.d(TAG, "PackageManager failed to retrieve ApplicationInfo for: "
                            + mDefaultPackageName);
                    mName = mDefaultPackageName;
                }
            }
            getQuickNameIconForUid(uid, packages, loadDataInBackground);
            mTimeInForegroundMs =
                    uidBatteryConsumer.getTimeInStateMs(UidBatteryConsumer.STATE_FOREGROUND);
            mTimeInBackgroundMs =
                    uidBatteryConsumer.getTimeInStateMs(UidBatteryConsumer.STATE_BACKGROUND);
        } else if (batteryConsumer instanceof UserBatteryConsumer) {
            mUid = Process.INVALID_UID;
            mConsumerType = ConvertUtils.CONSUMER_TYPE_USER_BATTERY;
            mConsumedPower = batteryConsumer.getConsumedPower();
            final NameAndIcon nameAndIcon = getNameAndIconFromUserId(
                    context, ((UserBatteryConsumer) batteryConsumer).getUserId());
            mIcon = nameAndIcon.mIcon;
            mName = nameAndIcon.mName;
        } else {
            throw new IllegalArgumentException("Unsupported: " + batteryConsumer);
        }
    }

    /** Battery entry for a power component of AggregateBatteryConsumer */
    public BatteryEntry(Context context, int powerComponentId, double devicePowerMah,
            double appsPowerMah, long usageDurationMs) {
        mContext = context;
        mBatteryConsumer = null;
        mUid = Process.INVALID_UID;
        mIsHidden = false;
        mPowerComponentId = powerComponentId;
        mConsumedPower =
                powerComponentId == BatteryConsumer.POWER_COMPONENT_SCREEN
                        ? devicePowerMah
                        : devicePowerMah - appsPowerMah;
        mUsageDurationMs = usageDurationMs;
        mConsumerType = ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY;

        final NameAndIcon nameAndIcon =
                getNameAndIconFromPowerComponent(context, powerComponentId);
        mIconId = nameAndIcon.mIconId;
        mName = nameAndIcon.mName;
        if (mIconId != 0) {
            mIcon = context.getDrawable(mIconId);
        }
    }

    /** Battery entry for a custom power component of AggregateBatteryConsumer */
    public BatteryEntry(Context context, int powerComponentId, String powerComponentName,
            double devicePowerMah, double appsPowerMah) {
        mContext = context;
        mBatteryConsumer = null;
        mUid = Process.INVALID_UID;
        mIsHidden = false;
        mPowerComponentId = powerComponentId;

        mIconId = R.drawable.ic_power_system;
        mIcon = context.getDrawable(mIconId);
        mName = powerComponentName;
        mConsumedPower =
                powerComponentId == BatteryConsumer.POWER_COMPONENT_SCREEN
                        ? devicePowerMah
                        : devicePowerMah - appsPowerMah;
        mConsumerType = ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public String getLabel() {
        return mName;
    }

    @ConvertUtils.ConsumerType
    public int getConsumerType() {
        return mConsumerType;
    }

    @BatteryConsumer.PowerComponent
    public int getPowerComponentId() {
        return mPowerComponentId;
    }

    void getQuickNameIconForUid(
            final int uid, final String[] packages, final boolean loadDataInBackground) {
        // Locale sync to system config in Settings
        final Locale locale = Locale.getDefault();
        if (sCurrentLocale != locale) {
            clearUidCache();
            sCurrentLocale = locale;
        }

        final String uidString = Integer.toString(uid);
        if (sUidCache.containsKey(uidString)) {
            UidToDetail utd = sUidCache.get(uidString);
            mDefaultPackageName = utd.mPackageName;
            mName = utd.mName;
            mIcon = utd.mIcon;
            return;
        }

        if (packages == null || packages.length == 0) {
            final NameAndIcon nameAndIcon = getNameAndIconFromUid(mContext, mName, uid);
            mIcon = nameAndIcon.mIcon;
            mName = nameAndIcon.mName;
        } else {
            mIcon = mContext.getPackageManager().getDefaultActivityIcon();
        }

        // Avoids post the loading icon and label in the background request.
        if (sHandler != null && loadDataInBackground) {
            synchronized (sRequestQueue) {
                sRequestQueue.add(this);
            }
        }
    }

    /** Loads the app label and icon image and stores into the cache. */
    public static NameAndIcon loadNameAndIcon(
            Context context,
            int uid,
            Handler handler,
            BatteryEntry batteryEntry,
            String defaultPackageName,
            String name,
            Drawable icon) {
        // Bail out if the current sipper is not an App sipper.
        if (uid == 0 || uid == Process.INVALID_UID) {
            return null;
        }

        final PackageManager pm = context.getPackageManager();
        final String[] packages = isSystemUid(uid)
                ? new String[]{PACKAGE_SYSTEM} : pm.getPackagesForUid(uid);
        if (packages != null) {
            final String[] packageLabels = new String[packages.length];
            System.arraycopy(packages, 0, packageLabels, 0, packages.length);

            // Convert package names to user-facing labels where possible
            final IPackageManager ipm = AppGlobals.getPackageManager();
            final int userId = UserHandle.getUserId(uid);
            for (int i = 0; i < packageLabels.length; i++) {
                try {
                    final ApplicationInfo ai = ipm.getApplicationInfo(packageLabels[i],
                            0 /* no flags */, userId);
                    if (ai == null) {
                        Log.d(TAG, "Retrieving null app info for package "
                                + packageLabels[i] + ", user " + userId);
                        continue;
                    }
                    final CharSequence label = ai.loadLabel(pm);
                    if (label != null) {
                        packageLabels[i] = label.toString();
                    }
                    if (ai.icon != 0) {
                        defaultPackageName = packages[i];
                        icon = ai.loadIcon(pm);
                        break;
                    }
                } catch (RemoteException e) {
                    Log.d(TAG, "Error while retrieving app info for package "
                            + packageLabels[i] + ", user " + userId, e);
                }
            }

            if (packageLabels.length == 1) {
                name = packageLabels[0];
            } else {
                // Look for an official name for this UID.
                for (String pkgName : packages) {
                    try {
                        final PackageInfo pi = ipm.getPackageInfo(pkgName, 0, userId);
                        if (pi == null) {
                            Log.d(TAG, "Retrieving null package info for package "
                                    + pkgName + ", user " + userId);
                            continue;
                        }
                        if (pi.sharedUserLabel != 0) {
                            final CharSequence nm = pm.getText(pkgName,
                                    pi.sharedUserLabel, pi.applicationInfo);
                            if (nm != null) {
                                name = nm.toString();
                                if (pi.applicationInfo.icon != 0) {
                                    defaultPackageName = pkgName;
                                    icon = pi.applicationInfo.loadIcon(pm);
                                }
                                break;
                            }
                        }
                    } catch (RemoteException e) {
                        Log.d(TAG, "Error while retrieving package info for package "
                                + pkgName + ", user " + userId, e);
                    }
                }
            }
        }

        final String uidString = Integer.toString(uid);
        if (icon == null) {
            icon = pm.getDefaultActivityIcon();
        }

        UidToDetail utd = new UidToDetail();
        utd.mName = name;
        utd.mIcon = icon;
        utd.mPackageName = defaultPackageName;

        sUidCache.put(uidString, utd);
        if (handler != null) {
            handler.sendMessage(handler.obtainMessage(MSG_UPDATE_NAME_ICON, batteryEntry));
        }
        return new NameAndIcon(name, defaultPackageName, icon, /*iconId=*/ 0);
    }

    /** Returns a string that uniquely identifies this battery consumer. */
    public String getKey() {
        if (mBatteryConsumer instanceof UidBatteryConsumer) {
            return Integer.toString(mUid);
        } else if (mBatteryConsumer instanceof UserBatteryConsumer) {
            return "U|" + ((UserBatteryConsumer) mBatteryConsumer).getUserId();
        } else {
            return "S|" + mPowerComponentId;
        }
    }

    /** Returns true if the entry is hidden from the battery usage summary list. */
    public boolean isHidden() {
        return mIsHidden;
    }

    /** Returns true if this entry describes an app (UID). */
    public boolean isAppEntry() {
        return mBatteryConsumer instanceof UidBatteryConsumer;
    }

    /** Returns true if this entry describes a User. */
    public boolean isUserEntry() {
        if (mBatteryConsumer instanceof UserBatteryConsumer) {
            return true;
        }
        return false;
    }

    /**
     * Returns the package name that should be used to represent the UID described
     * by this entry.
     */
    public String getDefaultPackageName() {
        return mDefaultPackageName;
    }

    /**
     * Returns the UID of the app described by this entry.
     */
    public int getUid() {
        return mUid;
    }

    /** Returns foreground foreground time/ms that is attributed to this entry. */
    public long getTimeInForegroundMs() {
        if (mBatteryConsumer instanceof UidBatteryConsumer) {
            return mTimeInForegroundMs;
        } else {
            return mUsageDurationMs;
        }
    }

    /** Returns background activity time/ms that is attributed to this entry. */
    public long getTimeInBackgroundMs() {
        if (mBatteryConsumer instanceof UidBatteryConsumer) {
            return mTimeInBackgroundMs;
        } else {
            return 0;
        }
    }

    /**
     * Returns total amount of power (in milli-amp-hours) that is attributed to this entry.
     */
    public double getConsumedPower() {
        return mConsumedPower;
    }

    /**
     * Adds the consumed power of the supplied BatteryConsumer to this entry. Also
     * uses its package with highest drain, if necessary.
     */
    public void add(BatteryConsumer batteryConsumer) {
        mConsumedPower += batteryConsumer.getConsumedPower();
        if (batteryConsumer instanceof UidBatteryConsumer) {
            UidBatteryConsumer uidBatteryConsumer = (UidBatteryConsumer) batteryConsumer;
            mTimeInForegroundMs += uidBatteryConsumer.getTimeInStateMs(
                    UidBatteryConsumer.STATE_FOREGROUND);
            mTimeInBackgroundMs += uidBatteryConsumer.getTimeInStateMs(
                    UidBatteryConsumer.STATE_BACKGROUND);
            if (mDefaultPackageName == null) {
                mDefaultPackageName = uidBatteryConsumer.getPackageWithHighestDrain();
            }
        }
    }

    /** Gets name and icon resource from UserBatteryConsumer userId. */
    public static NameAndIcon getNameAndIconFromUserId(
            Context context, final int userId) {
        UserManager um = context.getSystemService(UserManager.class);
        UserInfo info = um.getUserInfo(userId);

        Drawable icon = null;
        String name = null;
        if (info != null) {
            icon = Utils.getUserIcon(context, um, info);
            name = Utils.getUserLabel(context, info);
        } else {
            name = context.getResources().getString(
                    R.string.running_process_item_removed_user_label);
        }
        return new NameAndIcon(name, icon, 0 /* iconId */);
    }

    /** Gets name and icon resource from UidBatteryConsumer uid. */
    public static NameAndIcon getNameAndIconFromUid(
            Context context, String name, final int uid) {
        Drawable icon = context.getDrawable(R.drawable.ic_power_system);
        if (uid == 0) {
            name = context.getResources().getString(R.string.process_kernel_label);
        } else if (uid == BatteryUtils.UID_REMOVED_APPS) {
            name = context.getResources().getString(R.string.process_removed_apps);
        } else if (uid == BatteryUtils.UID_TETHERING) {
            name = context.getResources().getString(R.string.process_network_tethering);
        } else if ("mediaserver".equals(name)) {
            name = context.getResources().getString(R.string.process_mediaserver_label);
        } else if ("dex2oat".equals(name) || "dex2oat32".equals(name)
                || "dex2oat64".equals(name)) {
            name = context.getResources().getString(R.string.process_dex2oat_label);
        }
        return new NameAndIcon(name, icon, 0 /* iconId */);
    }

    /** Gets name and icon resource from BatteryConsumer power component ID. */
    public static NameAndIcon getNameAndIconFromPowerComponent(
            Context context, @BatteryConsumer.PowerComponent int powerComponentId) {
        String name;
        int iconId;
        switch (powerComponentId) {
            case BatteryConsumer.POWER_COMPONENT_AMBIENT_DISPLAY:
                name = context.getResources().getString(R.string.ambient_display_screen_title);
                iconId = R.drawable.ic_settings_aod;
                break;
            case BatteryConsumer.POWER_COMPONENT_BLUETOOTH:
                name = context.getResources().getString(R.string.power_bluetooth);
                iconId = R.drawable.ic_settings_bluetooth;
                break;
            case BatteryConsumer.POWER_COMPONENT_CAMERA:
                name = context.getResources().getString(R.string.power_camera);
                iconId = R.drawable.ic_settings_camera;
                break;
            case BatteryConsumer.POWER_COMPONENT_MOBILE_RADIO:
                name = context.getResources().getString(R.string.power_cell);
                iconId = R.drawable.ic_cellular_1_bar;
                break;
            case BatteryConsumer.POWER_COMPONENT_FLASHLIGHT:
                name = context.getResources().getString(R.string.power_flashlight);
                iconId = R.drawable.ic_settings_display;
                break;
            case BatteryConsumer.POWER_COMPONENT_PHONE:
                name = context.getResources().getString(R.string.power_phone);
                iconId = R.drawable.ic_settings_voice_calls;
                break;
            case BatteryConsumer.POWER_COMPONENT_SCREEN:
                name = context.getResources().getString(R.string.power_screen);
                iconId = R.drawable.ic_settings_display;
                break;
            case BatteryConsumer.POWER_COMPONENT_WIFI:
                name = context.getResources().getString(R.string.power_wifi);
                iconId = R.drawable.ic_settings_wireless_no_theme;
                break;
            case BatteryConsumer.POWER_COMPONENT_IDLE:
            case BatteryConsumer.POWER_COMPONENT_MEMORY:
                name = context.getResources().getString(R.string.power_idle);
                iconId = R.drawable.ic_settings_phone_idle;
                break;
            default:
                Log.w(TAG, "unknown attribute:" + DebugUtils.constantToString(
                        BatteryConsumer.class, "POWER_COMPONENT_", powerComponentId));
                name = null;
                iconId = R.drawable.ic_power_system;
                break;
        }
        return new NameAndIcon(name, null /* icon */, iconId);
    }

    /** Whether the uid is system uid. */
    public static boolean isSystemUid(int uid) {
        return uid == Process.SYSTEM_UID;
    }
}
