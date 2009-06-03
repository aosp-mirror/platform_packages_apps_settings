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

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.BatteryStats.Timer;
import android.os.BatteryStats.Uid;
import android.os.BatteryStats.Uid.Sensor;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;

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

    private static final int MENU_STATS_TYPE = Menu.FIRST;
    private static final int MENU_STATS_REFRESH = Menu.FIRST + 1;

    enum DrainType {
        IDLE,
        CELL,
        PHONE,
        WIFI,
        BLUETOOTH,
        SCREEN,
        APP
    }

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

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.power_usage_summary);
        mBatteryInfo = IBatteryStats.Stub.asInterface(
                ServiceManager.getService("batteryinfo"));
        mAppListGroup = getPreferenceScreen();
        mPowerProfile = new PowerProfile(this, "power_profile_default");
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateAppsList();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        PowerGaugePreference pgp = (PowerGaugePreference) preference;
        BatterySipper sipper = pgp.getInfo();
        Intent intent = new Intent(this, PowerUsageDetail.class);
        intent.putExtra(PowerUsageDetail.EXTRA_TITLE, sipper.mLabel);
        intent.putExtra(PowerUsageDetail.EXTRA_PERCENT, sipper.getSortValue() * 100 / mTotalPower);

        switch (sipper.mDrainType) {
            case APP:
            {
                Uid uid = sipper.mUid;
                int[] types = new int[] {
                    R.string.usage_type_cpu,
                    R.string.usage_type_cpu_foreground,
                    R.string.usage_type_gps,
                    R.string.usage_type_data_send,
                    R.string.usage_type_data_recv,
                    R.string.usage_type_audio,
                    R.string.usage_type_video,
                };
                double[] values = new double[] {
                    sipper.mCpuTime,
                    sipper.mCpuFgTime,
                    sipper.mGpsTime,
                    uid != null? uid.getTcpBytesSent(mStatsType) : 0,
                    uid != null? uid.getTcpBytesReceived(mStatsType) : 0,
                    0,
                    0
                };
                intent.putExtra(PowerUsageDetail.EXTRA_DETAIL_TYPES, types);
                intent.putExtra(PowerUsageDetail.EXTRA_DETAIL_VALUES, values);

            }
            break;
        }
        startActivity(intent);

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /*
        menu.add(0, MENU_STATS_TYPE, 0, R.string.menu_stats_total)
                .setIcon(com.android.internal.R.drawable.ic_menu_info_details)
                .setAlphabeticShortcut('t');
        */
        menu.add(0, MENU_STATS_REFRESH, 0, R.string.menu_stats_refresh)
                .setIcon(com.android.internal.R.drawable.ic_menu_refresh)
                .setAlphabeticShortcut('r');
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        /*
        menu.findItem(MENU_STATS_TYPE).setTitle(mStatsType == BatteryStats.STATS_TOTAL
                ? R.string.menu_stats_unplugged
                : R.string.menu_stats_total);
        */
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_STATS_TYPE:
                if (mStatsType == BatteryStats.STATS_TOTAL) {
                    mStatsType = BatteryStats.STATS_UNPLUGGED;
                } else {
                    mStatsType = BatteryStats.STATS_TOTAL;
                }
                updateAppsList();
                return true;
            case MENU_STATS_REFRESH:
                mStats = null;
                updateAppsList();
                return true;
            default:
                return false;
        }
    }

    private void updateAppsList() {
        if (mStats == null) {
            load();
        }
        mMaxPower = 0;
        mTotalPower = 0;

        mAppListGroup.removeAll();
        mUsageList.clear();
        processAppUsage();
        processMiscUsage();

        mAppListGroup.setOrderingAsAdded(false);

        Collections.sort(mUsageList);
        for (BatterySipper g : mUsageList) {
            if (g.getSortValue() < MIN_POWER_THRESHOLD) continue;
            double percent =  ((g.getSortValue() / mTotalPower) * 100);
            PowerGaugePreference pref = new PowerGaugePreference(this, g.getIcon(), g);
            double scaleByMax = (g.getSortValue() * 100) / mMaxPower;
            pref.setSummary(g.getLabel() + "  ( " + String.format("%3.2f", percent) + "% )");
            pref.setOrder(Integer.MAX_VALUE - (int) g.getSortValue()); // Invert the order
            pref.setGaugeValue(mScaleByMax ? scaleByMax : percent);
            mAppListGroup.addPreference(pref);
            if (mAppListGroup.getPreferenceCount() > MAX_ITEMS_TO_LIST) break;
        }
    }

    private void processAppUsage() {
        SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
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
            long cpuTime = 0;
            long cpuFgTime = 0;
            long gpsTime = 0;
            if (processStats.size() > 0) {
                for (Map.Entry<String, ? extends BatteryStats.Uid.Proc> ent
                        : processStats.entrySet()) {

                    Uid.Proc ps = ent.getValue();
                    long userTime = ps.getUserTime(which);
                    long systemTime = ps.getSystemTime(which);
                    long foregroundTime = ps.getForegroundTime(which);
                    cpuFgTime += foregroundTime * 10; // convert to millis
                    if (DEBUG) Log.i(TAG, "CPU Fg time for " + u.getUid() + " = " + foregroundTime);
                    cpuTime = (userTime + systemTime) * 10; // convert to millis
                    power += cpuTime * powerCpuNormal;

                }
            }
            if (cpuFgTime > cpuTime) {
                if (DEBUG && cpuFgTime > cpuTime + 10000) {
                    Log.i(TAG, "WARNING! Cputime is more than 10 seconds behind Foreground time");
                }
                cpuTime = cpuFgTime; // Statistics may not have been gathered yet.
            }
            power /= 1000;

            Map<Integer, ? extends BatteryStats.Uid.Sensor> sensorStats = u.getSensorStats();
            for (Map.Entry<Integer, ? extends BatteryStats.Uid.Sensor> sensorEntry
                    : sensorStats.entrySet()) {
                Uid.Sensor sensor = sensorEntry.getValue();
                int sensorType = sensor.getHandle();
                BatteryStats.Timer timer = sensor.getSensorTime();
                long sensorTime = timer.getTotalTimeLocked(uSecTime, which) / 1000;
                double multiplier = 0;
                switch (sensorType) {
                    case Uid.Sensor.GPS:
                        multiplier = mPowerProfile.getAveragePower(PowerProfile.POWER_GPS_ON);
                        gpsTime = sensorTime;
                        break;
                    default:
                        android.hardware.Sensor sensorData =
                                sensorManager.getDefaultSensor(sensorType);
                        if (sensorData != null) {
                            multiplier = sensorData.getPower();
                            if (DEBUG) {
                                Log.i(TAG, "Got sensor " + sensorData.getName() + " with power = "
                                        + multiplier);
                            }
                        }
                }
                power += (multiplier * sensorTime) / 1000;
            }
            if (power != 0) {
                BatterySipper app = new BatterySipper(null, DrainType.APP, 0, u,
                        new double[] {power});
                app.mCpuTime = cpuTime;
                app.mGpsTime = gpsTime;
                app.mCpuFgTime = cpuFgTime;
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
            double screenBinPower = screenFullPower * (i + 0.5f)
                    / BatteryStats.NUM_SCREEN_BRIGHTNESS_BINS;
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
        addEntry(getString(R.string.power_phone), DrainType.PHONE,
                android.R.drawable.ic_menu_call, phoneOnPower);

        double screenOnPower = getScreenOnPower(uSecNow);
        addEntry(getString(R.string.power_screen), DrainType.SCREEN,
                android.R.drawable.ic_menu_view, screenOnPower);

        double wifiPower = (mStats.getWifiOnTime(uSecNow, which) * 0 /* TODO */
                * mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_ON)
            + mStats.getWifiRunningTime(uSecNow, which)
                * mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_ON)) / 1000 / 1000;
        addEntry(getString(R.string.power_wifi), DrainType.WIFI,
                R.drawable.ic_wifi_signal_4, wifiPower);

        double idlePower = ((timeSinceUnplugged -  mStats.getScreenOnTime(uSecNow, mStatsType))
                * mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_IDLE)) / 1000 / 1000;
        addEntry(getString(R.string.power_idle), DrainType.IDLE,
                android.R.drawable.ic_lock_power_off, idlePower);

        double radioPower = getRadioPower(uSecNow, which);
        addEntry(getString(R.string.power_cell), DrainType.CELL,
                android.R.drawable.ic_menu_sort_by_size, radioPower);
    }

    private void addEntry(String label, DrainType drainType, int iconId, double power) {
        if (power > mMaxPower) mMaxPower = power;
        mTotalPower += power;
        BatterySipper bs = new BatterySipper(label, drainType, iconId, null, new double[] {power});
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
        Uid mUid;
        double mValue;
        double[] mValues;
        DrainType mDrainType;
        long mCpuTime;
        long mGpsTime;
        long mCpuFgTime;

        BatterySipper(String label, DrainType drainType, int iconId, Uid uid, double[] values) {
            mValues = values;
            mLabel = label;
            mDrainType = drainType;
            if (iconId > 0) {
                mIcon = getResources().getDrawable(iconId);
            }
            if (mValues != null) mValue = mValues[0];
            //if (uid > 0 && (mLabel == null || mIcon == null) // TODO:
            if ((label == null || iconId == 0) && uid!= null) {
                getNameForUid(uid.getUid());
            }
            mUid = uid;
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