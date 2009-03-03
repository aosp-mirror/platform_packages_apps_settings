/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.settings.battery_history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.android.internal.app.IBatteryStats;
import com.android.settings.R;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.BatteryStats.Timer;
import android.os.BatteryStats.Uid;
import android.util.Log;
import android.util.LogPrinter;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class BatteryHistory extends Activity implements OnClickListener, OnItemSelectedListener {
    private static final String TAG = "BatteryHistory";

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 60 * 60;
    private static final int SECONDS_PER_DAY = 24 * 60 * 60;
    
    // Must be in sync with the values in res/values/array.xml (id battery_history_type_spinner)
    private static final int CPU_USAGE = 0;
    private static final int NETWORK_USAGE = 1;
    private static final int GPS_USAGE = 2;
    private static final int SENSOR_USAGE = 3;
    private static final int WAKE_LOCKS = 4;
    private static final int SCREEN_ON = 5;

    // Must be in sync with the values in res/values/array.xml (id battery_history_which_spinner)
    private static final int UNPLUGGED = 0;
    private static final int CURRENT = 1;
    private static final int TOTAL = 2;
    
    private BatteryStats mStats;
    private int mWhich = BatteryStats.STATS_UNPLUGGED;
    private int mType = CPU_USAGE;
    
    private GraphableButton[] mButtons;
    IBatteryStats mBatteryInfo;
    
    private List<CpuUsage> mCpuUsage = new ArrayList<CpuUsage>();
    private List<NetworkUsage> mNetworkUsage = new ArrayList<NetworkUsage>();
    private List<SensorUsage> mSensorUsage = new ArrayList<SensorUsage>();
    private List<SensorUsage> mGpsUsage = new ArrayList<SensorUsage>();
    private List<WakelockUsage> mWakelockUsage = new ArrayList<WakelockUsage>();
    
    private boolean mHaveCpuUsage, mHaveNetworkUsage, mHaveSensorUsage,
            mHaveWakelockUsage, mHaveScreenOnTime;
    
    private LinearLayout mGraphLayout;
    private LinearLayout mTextLayout;
    private TextView mMessageText;
    private TextView mDetailsText;
    private Button mDetailsBackButton;
    private Spinner mTypeSpinner;
    private Spinner mWhichSpinner;
    
    private boolean mDetailsShown = false;
    
    private static String getLabel(String packageName, PackageManager pm) {
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            CharSequence label = ai.loadLabel(pm);
            if (label != null) {
                return label.toString();
            }
        } catch (NameNotFoundException e) {
            return packageName;
        }
        
        return "";
    }
    
    void formatTime(double millis, StringBuilder sb) {
        int seconds = (int) Math.floor(millis / 1000);
        
        int days = 0, hours = 0, minutes = 0;
        if (seconds > SECONDS_PER_DAY) {
            days = seconds / SECONDS_PER_DAY;
            seconds -= days * SECONDS_PER_DAY;
        }
        if (seconds > SECONDS_PER_HOUR) {
            hours = seconds / SECONDS_PER_HOUR;
            seconds -= hours * SECONDS_PER_HOUR;
        }
        if (seconds > SECONDS_PER_MINUTE) {
            minutes = seconds / SECONDS_PER_MINUTE;
            seconds -= minutes * SECONDS_PER_MINUTE;
        }
        if (days > 0) {
            sb.append(getString(R.string.battery_history_days, days, hours, minutes, seconds));
        } else if (hours > 0) {
            sb.append(getString(R.string.battery_history_hours, hours, minutes, seconds));
        } else if (minutes > 0) { 
            sb.append(getString(R.string.battery_history_minutes, minutes, seconds));
        } else {
            sb.append(getString(R.string.battery_history_seconds, seconds));
        }
    }
    
    abstract class Graphable implements Comparable<Graphable> {        
        protected String mName;
        protected String mNamePackage;
        protected boolean mUniqueName;
        protected String[] mPackages;
        protected String[] mPackageNames;
        
        public abstract String getLabel();
        public abstract double getSortValue();
        public abstract double[] getValues();
        public abstract void getInfo(StringBuilder info);
        
        public int compareTo(Graphable o) {
            double t = getSortValue();
            double ot = o.getSortValue();
            if (t < ot) {
                // Largest first
                return 1;
            } else if (t > ot) {
                return -1;
            } else {
                return 0;
            }
        }
                
        // Side effects: sets mName and mUniqueName
        void getNameForUid(int uid) {
            PackageManager pm = getPackageManager();
            mPackages = pm.getPackagesForUid(uid);
            if (mPackages == null) {
                mName = Integer.toString(uid);
                mNamePackage = null;
                return;
            }
            
            mPackageNames = new String[mPackages.length];
            System.arraycopy(mPackages, 0, mPackageNames, 0, mPackages.length);
            
            // Convert package names to user-facing labels where possible
            for (int i = 0; i < mPackageNames.length; i++) {
                mPackageNames[i] = BatteryHistory.getLabel(mPackageNames[i], pm);
            }

            if (mPackageNames.length == 1) {
                mNamePackage = mPackages[0];
                mName = mPackageNames[0];
                mUniqueName = true;
            } else {
                mName = getString(R.string.battery_history_uid, uid); // Default name
                // Look for an official name for this UID.
                for (String name : mPackages) {
                    try {
                        PackageInfo pi = pm.getPackageInfo(name, 0);
                        if (pi.sharedUserLabel != 0) {
                            CharSequence nm = pm.getText(name,
                                    pi.sharedUserLabel, pi.applicationInfo);
                            if (nm != null) {
                                mName = nm.toString();
                                break;
                            }
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
            }
        }
    }

    class CpuUsage extends Graphable {
        String mProcess;
        double[] mUsage;
        long mStarts;
        
        public CpuUsage(int uid, String process, long userTime, long systemTime, long starts) {
            getNameForUid(uid);
            mProcess = process;
            PackageManager pm = BatteryHistory.this.getPackageManager();
            mName = BatteryHistory.getLabel(process, pm);
            mUsage = new double[2];
            
            mUsage[0] = userTime;
            mUsage[1] = userTime + systemTime;
            mStarts = starts;
        }
        
        public String getLabel() {
            return mName;
        }
        
        public double getSortValue() {
            return mUsage[1];
        }
        
        public double[] getValues() {
            return mUsage;
        }
        
        public void getInfo(StringBuilder info) {
            info.append(getString(R.string.battery_history_cpu_usage, mProcess));
            info.append("\n\n");
            info.append(getString(R.string.battery_history_user_time));
            formatTime(mUsage[0] * 10, info);
            info.append('\n');
            info.append(getString(R.string.battery_history_system_time));
            formatTime((mUsage[1] - mUsage[0]) * 10, info);
            info.append('\n');
            info.append(getString(R.string.battery_history_total_time));
            formatTime((mUsage[1]) * 10, info);
            info.append('\n');
            info.append(getString(R.string.battery_history_starts, mStarts));
        }
    }
    
    class NetworkUsage extends Graphable {
        double[] mUsage;
        
        public NetworkUsage(int uid, long received, long sent) {
            getNameForUid(uid);
            
            mUsage = new double[2];
            mUsage[0] = received;
            mUsage[1] = received + sent;
        }
        
        public String getLabel() {
            return mName;
        }
        
        public double getSortValue() {
            return mUsage[1];
        }
        
        public double[] getValues() {
            return mUsage;
        }
        
        public void getInfo(StringBuilder info) {
            info.append(getString(R.string.battery_history_network_usage, mName));
            info.append("\n\n");
            info.append(getString(R.string.battery_history_bytes_received, (long) mUsage[0]));
            info.append('\n');
            info.append(getString(R.string.battery_history_bytes_sent,
                    (long) mUsage[1] - (long) mUsage[0]));
            info.append('\n');
            info.append(getString(R.string.battery_history_bytes_total, (long) mUsage[1]));

            if (!mUniqueName) {
                info.append("\n\n");
                info.append(getString(R.string.battery_history_packages_sharing_this_uid));
                info.append('\n');

                PackageManager pm = BatteryHistory.this.getPackageManager();
                List<String> names = new ArrayList<String>();
                for (String name : mPackageNames) {
                    names.add(BatteryHistory.getLabel(name, pm));
                }
                Collections.sort(names);
                for (String name : names) {
                    info.append("    ");
                    info.append(name);
                    info.append('\n');
                }
            }
        }
    }
    
    class SensorUsage extends Graphable {
        double[] mUsage;
        int mCount;
        
        public SensorUsage(int uid, long time, int count) {
            getNameForUid(uid);
            
            mUsage = new double[1];
            mUsage[0] = time;
            
            mCount = count;
        }
        
        public String getLabel() {
            return mName;
        }
        
        public double getSortValue() {
            return mUsage[0];
        }
        
        public double[] getValues() {
            return mUsage;
        }
        
        public void getInfo(StringBuilder info) {
            info.append(getString(R.string.battery_history_sensor));
            info.append(mName);
            info.append("\n\n");
            info.append(getString(R.string.battery_history_total_time));
            formatTime(mUsage[0], info);
            info.append("\n\n");
        }
    }
    
    
    class WakelockUsage extends Graphable {
        double[] mUsage;
        int mCount;
        
        public WakelockUsage(int uid, long time, int count) {
            getNameForUid(uid);
            
            mUsage = new double[1];
            mUsage[0] = time;
            
            mCount = count;
        }
        
        public String getLabel() {
            return mName;
        }
        
        public double getSortValue() {
            return mUsage[0];
        }
        
        public double[] getValues() {
            return mUsage;
        }
        
        public void getInfo(StringBuilder info) {
            info.append(getString(R.string.battery_history_wakelock));
            info.append(mName);
            info.append("\n\n");
            info.append(getString(R.string.battery_history_total_time));
            formatTime(mUsage[0], info);
            info.append("\n\n");
        }
    }
    
    private List<? extends Graphable> getGraphRecords() {
        switch (mType) {
            case CPU_USAGE: return mCpuUsage;
            case NETWORK_USAGE : return mNetworkUsage;
            case SENSOR_USAGE: return mSensorUsage;
            case GPS_USAGE: return mGpsUsage;
            case WAKE_LOCKS: return mWakelockUsage;
            case SCREEN_ON: return null;
            default:
                return (List<? extends Graphable>) null; // TODO
        }
    }
    
    private void displayScreenUsage() {
        mMessageText.setVisibility(View.VISIBLE);
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.battery_history_screen_on));
        sb.append("\n\n");
        sb.append(getString(R.string.battery_history_screen_on_battery));
        sb.append(' ');
        formatTime((double) mStats.getBatteryScreenOnTime(), sb);
        sb.append('\n');
        sb.append(getString(R.string.battery_history_screen_on_plugged));
        sb.append(' ');
        formatTime((double) mStats.getPluggedScreenOnTime(), sb);
        sb.append('\n');
        mMessageText.setText(sb.toString());
    }
    
    private void displayGraph() {
        Log.i(TAG, "displayGraph");

        collectStatistics();
        
        // Hide the UI and selectively enable it below
        mMessageText.setVisibility(View.GONE);
        for (int i = 0; i < mButtons.length; i++) {
            mButtons[i].setVisibility(View.INVISIBLE);
        }
        
        if (mType == SCREEN_ON) {
            displayScreenUsage();
            return;
        }
        
        double maxValue = -Double.MAX_VALUE;
        
        List<? extends Graphable> records = getGraphRecords();
        for (Graphable g : records) {
            double[] values = g.getValues();
            maxValue = Math.max(maxValue, values[values.length - 1]);
        }
        
        int[] colors = new int[2];
        colors[0] = 0xff0000ff;
        colors[1] = 0xffff0000;
        
        for (int i = 0; i < mButtons.length; i++) {
            mButtons[i].setVisibility(View.INVISIBLE);
        }
        
        int numRecords = Math.min(records.size(), mButtons.length);
        if (numRecords == 0) {
             mMessageText.setVisibility(View.VISIBLE);
             mMessageText.setText(R.string.battery_history_no_data);
        } else {
            for (int i = 0; i < numRecords; i++) {
                Graphable r = records.get(i);           

                mButtons[i].setText(r.getLabel());
                mButtons[i].setValues(r.getValues(), maxValue);
                mButtons[i].setVisibility(View.VISIBLE);
            }
        }
    }
    
    private void hideDetails() {
        mTextLayout.setVisibility(View.GONE);
        mGraphLayout.setVisibility(View.VISIBLE);
        mDetailsShown = false;
    }
    
    private void showDetails(int id) {
        mGraphLayout.setVisibility(View.GONE);
        mTextLayout.setVisibility(View.VISIBLE);
            
        StringBuilder info = new StringBuilder();
        List<? extends Graphable> records = getGraphRecords();
        if (id < records.size()) {
            Graphable record = records.get(id);
            record.getInfo(info);
        } else {
            info.append(getString(R.string.battery_history_details_for, id));
        }
        mDetailsText.setText(info.toString());
        mDetailsShown = true;
    }

    private void processCpuUsage() {
        mCpuUsage.clear();
        
        SparseArray<? extends Uid> uidStats = mStats.getUidStats();
        final int NU = uidStats.size();
        for (int iu = 0; iu < NU; iu++) {
            Uid u = uidStats.valueAt(iu);

            Map<String, ? extends BatteryStats.Uid.Proc> processStats = u.getProcessStats();
            if (processStats.size() > 0) {
                for (Map.Entry<String, ? extends BatteryStats.Uid.Proc> ent
                        : processStats.entrySet()) {

                    Uid.Proc ps = ent.getValue();
                    long userTime = ps.getUserTime(mWhich);
                    long systemTime = ps.getSystemTime(mWhich);
                    long starts = ps.getStarts(mWhich);

                    if (userTime != 0 || systemTime != 0) {
                        mCpuUsage.add(new CpuUsage(u.getUid(), ent.getKey(),
                                userTime, systemTime, starts));
                    }
                }
            }
        }
        Collections.sort(mCpuUsage);
    }
    
    private void processNetworkUsage() {
        mNetworkUsage.clear();
        
        SparseArray<? extends Uid> uidStats = mStats.getUidStats();
        final int NU = uidStats.size();
        for (int iu = 0; iu < NU; iu++) {
            Uid u = uidStats.valueAt(iu);
            
            long received = u.getTcpBytesReceived(mWhich);
            long sent = u.getTcpBytesSent(mWhich);
            if (received + sent > 0) {
                mNetworkUsage.add(new NetworkUsage(u.getUid(), received, sent));
            }
        }
        Collections.sort(mNetworkUsage);
    }
    
    private void processSensorUsage() {
        mGpsUsage.clear();
        mSensorUsage.clear();
        
        long uSecTime = SystemClock.elapsedRealtime() * 1000;
        final long uSecNow = mStats.getBatteryUptime(uSecTime);
        
        SparseArray<? extends Uid> uidStats = mStats.getUidStats();
        final int NU = uidStats.size();
        for (int iu = 0; iu < NU; iu++) {
            Uid u = uidStats.valueAt(iu);
            int uid = u.getUid();
            
            Map<Integer, ? extends BatteryStats.Uid.Sensor> sensorStats = u.getSensorStats();
            long timeGps = 0;
            int countGps = 0;
            long timeOther = 0;
            int countOther = 0;
            if (sensorStats.size() > 0) {
                for (Map.Entry<Integer, ? extends BatteryStats.Uid.Sensor> ent
                        : sensorStats.entrySet()) {

                    Uid.Sensor se = ent.getValue();
                    int handle = se.getHandle();
                    Timer timer = se.getSensorTime();
                    if (timer != null) {
                        // Convert from microseconds to milliseconds with rounding
                        long totalTime = (timer.getTotalTime(uSecNow, mWhich) + 500) / 1000;
                        int count = timer.getCount(mWhich);
                        if (handle == BatteryStats.Uid.Sensor.GPS) {
                            timeGps += totalTime;
                            countGps += count;
                        } else {
                            timeOther += totalTime;
                            countOther += count;
                        }
                    }
                }
            }
            
            if (timeGps > 0) {
                mGpsUsage.add(new SensorUsage(uid, timeGps, countGps));
            }
            if (timeOther > 0) {
                mSensorUsage.add(new SensorUsage(uid, timeOther, countOther));
            }
        }
        
        Collections.sort(mGpsUsage);
        Collections.sort(mSensorUsage);
    }
    
    private void processWakelockUsage() {
        mWakelockUsage.clear();
        
        long uSecTime = SystemClock.elapsedRealtime() * 1000;
        final long uSecNow = mStats.getBatteryUptime(uSecTime);
        
        SparseArray<? extends Uid> uidStats = mStats.getUidStats();
        final int NU = uidStats.size();
        for (int iu = 0; iu < NU; iu++) {
            Uid u = uidStats.valueAt(iu);
            int uid = u.getUid();
            
            Map<String, ? extends BatteryStats.Uid.Wakelock> wakelockStats = u.getWakelockStats();
            long time = 0;
            int count = 0;
            if (wakelockStats.size() > 0) {
                for (Map.Entry<String, ? extends BatteryStats.Uid.Wakelock> ent
                        : wakelockStats.entrySet()) {

                    Uid.Wakelock wl = ent.getValue();
                    Timer timer = wl.getWakeTime(BatteryStats.WAKE_TYPE_PARTIAL);
                    if (timer != null) {
                        // Convert from microseconds to milliseconds with rounding
                        time += (timer.getTotalTime(uSecNow, mWhich) + 500) / 1000;
                        count += timer.getCount(mWhich);
                    }
                }
            }
            
            if (time > 0) {
                mWakelockUsage.add(new WakelockUsage(uid, time, count));
            }
        }
        
        Collections.sort(mWakelockUsage);
    }
    
    private void processScreenOn() {
        // Do nothing
    }
    
    private void collectStatistics() {
        if (mType == CPU_USAGE) {
            if (!mHaveCpuUsage) {
                mHaveCpuUsage = true;
                processCpuUsage();
            }
        }
        if (mType == NETWORK_USAGE) {
            if (!mHaveNetworkUsage) {
                mHaveNetworkUsage = true;
                processNetworkUsage();
            }
        }
        if (mType == GPS_USAGE || mType == SENSOR_USAGE) {
            if (!mHaveSensorUsage) {
                mHaveSensorUsage = true;
                processSensorUsage();
            }
        }
        if (mType == WAKE_LOCKS) {
            if (!mHaveWakelockUsage) {
                mHaveWakelockUsage = true;
                processWakelockUsage();
            }
        }
        if (mType == SCREEN_ON) {
            if (!mHaveScreenOnTime) {
                mHaveScreenOnTime = true;
                processScreenOn();
            }
        }
    }
    
    private void load() {
        try {
            byte[] data = mBatteryInfo.getStatistics();
            Parcel parcel = Parcel.obtain();
            //Log.i(TAG, "Got data: " + data.length + " bytes");
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            mStats = com.android.internal.os.BatteryStatsImpl.CREATOR
                    .createFromParcel(parcel);
            //Log.i(TAG, "RECEIVED BATTERY INFO:");
            //mStats.dumpLocked(new LogPrinter(Log.INFO, TAG));
            
            mHaveCpuUsage =  mHaveNetworkUsage =  mHaveSensorUsage
                    = mHaveWakelockUsage = mHaveScreenOnTime = false;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException:", e);
        }
    }
    
    public void onClick(View v) {
        if (v == mDetailsBackButton) {
            hideDetails();
            return;
        }
        
        int id = ((Integer) v.getTag()).intValue();
        showDetails(id);
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mDetailsShown) {
            hideDetails();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int oldWhich = mWhich;
        
        if (parent.equals(mTypeSpinner)) {
            mType = position;
            switch (position) {
                case CPU_USAGE:
                    mWhichSpinner.setEnabled(true);
                    break;
                case NETWORK_USAGE:
                    mWhichSpinner.setEnabled(true);
                    break;
                case GPS_USAGE:
                    mWhichSpinner.setEnabled(true);
                    break;
                case SENSOR_USAGE:
                    mWhichSpinner.setEnabled(true);
                    break;
                case WAKE_LOCKS:
                    mWhichSpinner.setEnabled(true);
                    break;
                case SCREEN_ON:
                    mWhichSpinner.setEnabled(false);
                    break;
            }
        } else if (parent.equals(mWhichSpinner)) {
            switch (position) {
                case UNPLUGGED:
                    mWhich = BatteryStats.STATS_UNPLUGGED;
                    break;
                case CURRENT:
                    mWhich = BatteryStats.STATS_CURRENT;
                    break;
                case TOTAL:
                    mWhich = BatteryStats.STATS_TOTAL;
                    break;
            }
        }
        
        if (oldWhich != mWhich) {
            mHaveCpuUsage =  mHaveNetworkUsage =  mHaveSensorUsage
                    = mHaveWakelockUsage = mHaveScreenOnTime = false;
        }
        
        displayGraph();
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        BatteryStats stats = mStats;
        mStats = null;
        return stats;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mStats != null) {
            outState.putParcelable("stats", mStats);
        }
        outState.putInt("type", mType);
        outState.putInt("which", mWhich);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.i(TAG, "onCreate");
        
        setContentView(R.layout.battery_history);
        
        mGraphLayout = (LinearLayout) findViewById(R.id.graphLayout);
        mTextLayout = (LinearLayout) findViewById(R.id.textLayout);
        mDetailsText = (TextView) findViewById(R.id.detailsText);
        mMessageText = (TextView) findViewById(R.id.messageText);
        
        mTypeSpinner = (Spinner) findViewById(R.id.typeSpinner);
        mTypeSpinner.setOnItemSelectedListener(this);
        
        mWhichSpinner = (Spinner) findViewById(R.id.whichSpinner);
        mWhichSpinner.setOnItemSelectedListener(this);
        
        mButtons = new GraphableButton[8];
        mButtons[0] = (GraphableButton) findViewById(R.id.button0);
        mButtons[1] = (GraphableButton) findViewById(R.id.button1);
        mButtons[2] = (GraphableButton) findViewById(R.id.button2);
        mButtons[3] = (GraphableButton) findViewById(R.id.button3);
        mButtons[4] = (GraphableButton) findViewById(R.id.button4);
        mButtons[5] = (GraphableButton) findViewById(R.id.button5);
        mButtons[6] = (GraphableButton) findViewById(R.id.button6);
        mButtons[7] = (GraphableButton) findViewById(R.id.button7);
        
        for (int i = 0; i < mButtons.length; i++) {
            mButtons[i].setTag(i);
            mButtons[i].setOnClickListener(this);
        }
        
        mBatteryInfo = IBatteryStats.Stub.asInterface(
                ServiceManager.getService("batteryinfo"));
        
        mStats = (BatteryStats)getLastNonConfigurationInstance();
        if (icicle != null) {
            if (mStats == null) {
                mStats = (BatteryStats)icicle.getParcelable("stats");
            }
            mType = icicle.getInt("type");
            mWhich = icicle.getInt("which");
        }
        if (mStats == null) {
            load();
        }
        displayGraph();
    }
}
