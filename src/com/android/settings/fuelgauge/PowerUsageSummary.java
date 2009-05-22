/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.BatteryStats.Uid;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.PowerProfile;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Displays a list of apps and subsystems that consume power, ordered by how much power was
 * consumed since the last time it was unplugged.
 */
public class PowerUsageSummary extends PreferenceActivity {

    private static final boolean DEBUG = true;

    private static final String TAG = "PowerUsageSummary";
    private static final String PREF_APP_LIST = "app_list";

    IBatteryStats mBatteryInfo;
    BatteryStatsImpl mStats;
    private List<BatterySipper> mUsageList = new ArrayList<BatterySipper>();

    private PreferenceGroup mAppListGroup;

    private int mStatsType = BatteryStats.STATS_UNPLUGGED;

    private static final int MIN_POWER_THRESHOLD = 5;
    private static final int MAX_ITEMS_TO_LIST = 10;

    private double mMaxPower = 1;
    private double mTotalPower;

    private boolean mScaleByMax = true;

    private PowerProfile mPowerProfile;

    private static final long BATTERY_SIZE = 1200;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.power_usage_summary);
        mBatteryInfo = IBatteryStats.Stub.asInterface(
                ServiceManager.getService("batteryinfo"));
        mAppListGroup = (PreferenceGroup) findPreference(PREF_APP_LIST);
        mPowerProfile = new PowerProfile(this, "power_profile_default");
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateAppsList();
    }

    private void updateAppsList() {
        if (mStats == null) {
            load();
        }

        mAppListGroup.removeAll();
        mUsageList.clear();
        processCpuUsage();
        processMiscUsage();

        mAppListGroup.setOrderingAsAdded(false);

        Collections.sort(mUsageList);
        for (BatterySipper g : mUsageList) {
            if (g.getSortValue() < MIN_POWER_THRESHOLD) continue;
            double percent =  ((g.getSortValue() / mTotalPower) * 100 / BATTERY_SIZE);
            PowerGaugePreference pref = new PowerGaugePreference(this, g.getIcon());
            double scaleByMax = (g.getSortValue() * 100) / mTotalPower;
            pref.setSummary(g.getLabel() + "  ( " + (int) g.getSortValue() + "mA, "
                    + String.format("%3.2f", scaleByMax) + "% )");
            pref.setOrder(Integer.MAX_VALUE - (int) g.getSortValue()); // Invert the order
            pref.setGaugeValue(mScaleByMax ? scaleByMax : percent);
            mAppListGroup.addPreference(pref);
            if (mAppListGroup.getPreferenceCount() > MAX_ITEMS_TO_LIST) break;
        }
    }

    private void processCpuUsage() {
        final int which = mStatsType;
        final double powerCpuNormal = mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_NORMAL);
        long uSecTime = mStats.computeBatteryRealtime(SystemClock.elapsedRealtime(), which) * 1000;
        SparseArray<? extends Uid> uidStats = mStats.getUidStats();
        final int NU = uidStats.size();
        if (DEBUG) Log.i(TAG, "uidStats size = " + NU);
        for (int iu = 0; iu < NU; iu++) {
            Uid u = uidStats.valueAt(iu);
            double power = 0;
            //mUsageList.add(new AppUsage(u.getUid(), new double[] {power}));
            Map<String, ? extends BatteryStats.Uid.Proc> processStats = u.getProcessStats();
            if (processStats.size() > 0) {
                for (Map.Entry<String, ? extends BatteryStats.Uid.Proc> ent
                        : processStats.entrySet()) {

                    Uid.Proc ps = ent.getValue();
                    long userTime = ps.getUserTime(which);
                    long systemTime = ps.getSystemTime(which);
                    //long starts = ps.getStarts(which);
                    power += (userTime + systemTime) * 10 /* convert to milliseconds */
                            * powerCpuNormal;

                }
            }
            power /= 1000;

            Map<Integer, ? extends BatteryStats.Uid.Sensor> sensorStats = u.getSensorStats();
            for (Map.Entry<Integer, ? extends BatteryStats.Uid.Sensor> sensorEntry
                    : sensorStats.entrySet()) {
                Uid.Sensor sensor = sensorEntry.getValue();
                int sensorType = sensor.getHandle();
                BatteryStats.Timer timer = sensor.getSensorTime();
                long sensorTime = timer.getTotalTimeLocked(uSecTime, which);
                double multiplier = 0;
                switch (sensorType) {
                    case Uid.Sensor.GPS:
                        multiplier = mPowerProfile.getAveragePower(PowerProfile.POWER_GPS_ON);
                        break;
                }
                power += multiplier * sensorTime;
            }
            if (power != 0) {
                BatterySipper app = new BatterySipper(null, 0, u.getUid(), new double[] {power});
                mUsageList.add(app);
            }
            if (power > mMaxPower) mMaxPower = power;
            mTotalPower += power;
            if (DEBUG) Log.i(TAG, "Added power = " + power);
        }
    }

    private double getPhoneOnPower(long uSecNow) {
        return mPowerProfile.getAveragePower(PowerProfile.POWER_RADIO_ACTIVE)
                * mStats.getPhoneOnTime(uSecNow, mStatsType) / 1000 / 1000;
    }

    private double getScreenOnPower(long uSecNow) {
        double power = 0;
        power += mStats.getScreenOnTime(uSecNow, mStatsType)
                * mPowerProfile.getAveragePower(PowerProfile.POWER_SCREEN_ON) / 1000; // millis
        final double screenFullPower =
                mPowerProfile.getAveragePower(PowerProfile.POWER_SCREEN_FULL);
        for (int i = 0; i < BatteryStats.NUM_SCREEN_BRIGHTNESS_BINS; i++) {
            double screenBinPower = screenFullPower * i / BatteryStats.NUM_SCREEN_BRIGHTNESS_BINS;
            long brightnessTime = mStats.getScreenBrightnessTime(i, uSecNow, mStatsType) / 1000;
            power += screenBinPower * brightnessTime;
            if (DEBUG) {
                Log.i(TAG, "Screen bin power = " + (int) screenBinPower + ", time = "
                        + brightnessTime);
            }
        }
        return power / 1000;
    }

    private double getRadioPower(long uSecNow, int which) {
        double power = 0;
        final int BINS = BatteryStats.NUM_SIGNAL_STRENGTH_BINS;
        for (int i = 0; i < BINS; i++) {
            power += mStats.getPhoneSignalStrengthTime(i, uSecNow, which) / 1000 / 1000 *
                mPowerProfile.getAveragePower(PowerProfile.POWER_RADIO_ON)
                * ((BINS - i) / (double) BINS);
        }
        return power;
    }

    private void processMiscUsage() {
        final int which = mStatsType;
        long uSecTime = SystemClock.elapsedRealtime() * 1000;
        final long uSecNow = mStats.computeBatteryRealtime(uSecTime, which);
        final long timeSinceUnplugged = uSecNow;
        if (DEBUG) {
            Log.i(TAG, "Uptime since last unplugged = " + (timeSinceUnplugged / 1000));
        }

        double phoneOnPower = getPhoneOnPower(uSecNow);
        addEntry(getString(R.string.power_phone), android.R.drawable.ic_menu_call, phoneOnPower);

        double screenOnPower = getScreenOnPower(uSecNow);
        addEntry(getString(R.string.power_screen), android.R.drawable.ic_menu_view, screenOnPower);

        double wifiPower = (mStats.getWifiOnTime(uSecNow, which) * 0 /* TODO */
                * mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_ON)
            + mStats.getWifiRunningTime(uSecNow, which)
                * mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_ON)) / 1000 / 1000;
        addEntry(getString(R.string.power_wifi), R.drawable.ic_wifi_signal_4, wifiPower);

        double idlePower = ((timeSinceUnplugged -  mStats.getScreenOnTime(uSecNow, mStatsType))
                * mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_IDLE)) / 1000 / 1000;
        addEntry(getString(R.string.power_idle), android.R.drawable.ic_lock_power_off, idlePower);

        double radioPower = getRadioPower(uSecNow, which);
        addEntry(getString(R.string.power_cell),
                android.R.drawable.ic_menu_sort_by_size, radioPower);
    }

    private void addEntry(String label, int iconId, double power) {
        if (power > mMaxPower) mMaxPower = power;
        mTotalPower += power;
        BatterySipper bs = new BatterySipper(label, iconId, 0, new double[] {power});
        mUsageList.add(bs);
    }

    private void load() {
        try {
            byte[] data = mBatteryInfo.getStatistics();
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            mStats = com.android.internal.os.BatteryStatsImpl.CREATOR
                    .createFromParcel(parcel);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException:", e);
        }
    }

    class BatterySipper implements Comparable<BatterySipper> {
        String mLabel;
        Drawable mIcon;
        int mUid;
        double mValue;
        double[] mValues;

        BatterySipper(String label, int iconId, int uid, double[] values) {
            mValues = values;
            mLabel = label;
            if (iconId > 0) {
                mIcon = getResources().getDrawable(iconId);
            }
            if (mValues != null) mValue = mValues[0];
            //if (uid > 0 && (mLabel == null || mIcon == null) // TODO:
            if ((label == null || iconId == 0) && uid > 0) {
                getNameForUid(uid);
            }
        }

        double getSortValue() {
            return mValue;
        }

        double[] getValues() {
            return mValues;
        }

        Drawable getIcon() {
            return mIcon;
        }

        public int compareTo(BatterySipper other) {
            // Return the flipped value because we want the items in descending order
            return (int) (other.getSortValue() - getSortValue());
        }

        String getLabel() {
            return mLabel;
        }

        /**
         * Sets mLabel and mIcon
         * @param uid Uid of the application
         */
        void getNameForUid(int uid) {
            // TODO: Do this on a separate thread
            PackageManager pm = getPackageManager();
            String[] packages = pm.getPackagesForUid(uid);
            if (packages == null) {
                mLabel = Integer.toString(uid);
                return;
            }

            String[] packageNames = new String[packages.length];
            System.arraycopy(packages, 0, packageNames, 0, packages.length);

            // Convert package names to user-facing labels where possible
            for (int i = 0; i < packageNames.length; i++) {
                //packageNames[i] = PowerUsageSummary.getLabel(packageNames[i], pm);
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(packageNames[i], 0);
                    CharSequence label = ai.loadLabel(pm);
                    if (label != null) {
                        packageNames[i] = label.toString();
                    }
                    if (mIcon == null) {
                        mIcon = ai.loadIcon(pm);
                    }
                } catch (NameNotFoundException e) {
                }
            }

            if (packageNames.length == 1) {
                mLabel = packageNames[0];
            } else {
                // Look for an official name for this UID.
                for (String name : packages) {
                    try {
                        PackageInfo pi = pm.getPackageInfo(name, 0);
                        if (pi.sharedUserLabel != 0) {
                            CharSequence nm = pm.getText(name,
                                    pi.sharedUserLabel, pi.applicationInfo);
                            if (nm != null) {
                                mLabel = nm.toString();
                                break;
                            }
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
            }
        }
    }
}