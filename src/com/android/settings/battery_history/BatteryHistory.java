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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.android.internal.app.IBatteryStats;
import com.android.settings.R;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.BatteryStats.Timer;
import android.os.BatteryStats.Uid;
import android.util.Log;
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
    private static final int SENSOR_USAGE = 2;
    private static final int SCREEN_ON = 3;
    private static final int WAKE_LOCKS = 4;

    // App names to use as labels for the shared UIDs that contain them
    private final HashSet<String> mKnownApps = new HashSet<String>();
    
    private BatteryStats mStats;
    private int mWhich = BatteryStats.STATS_UNPLUGGED;
    private int mType = CPU_USAGE;
    
    private GraphableButton[] mButtons;
    IBatteryStats mBatteryInfo;
    
    private List<CpuUsage> mCpuUsage = new ArrayList<CpuUsage>();
    private List<NetworkUsage> mNetworkUsage = new ArrayList<NetworkUsage>();
    private List<SensorUsage> mSensorUsage = new ArrayList<SensorUsage>();
    private long mScreenOnTime;
    
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
        protected boolean mUniqueName;
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
            mPackageNames = pm.getPackagesForUid(uid);
            // Convert package names to user-facing labels where possible
            for (int i = 0; i < mPackageNames.length; i++) {
                mPackageNames[i] = BatteryHistory.getLabel(mPackageNames[i], pm);
            }

            if (mPackageNames.length == 1) {
                mName = mPackageNames[0];
                mUniqueName = true;
            } else {
                mName = getString(R.string.battery_history_uid, uid); // Default name
                // If one of the names for this UID is in mKnownApps, use it
                for (String name : mPackageNames) {
                    if (mKnownApps.contains(name)) {
                        mName = name;
                        break;
                    }
                }
            }
        }
    }

    class CpuUsage extends Graphable {
        double[] mUsage;
        long mStarts;
        
        public CpuUsage(String name, long userTime, long systemTime, long starts) {
            PackageManager pm = BatteryHistory.this.getPackageManager();
            mName = BatteryHistory.getLabel(name, pm);
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
            info.append(getString(R.string.battery_history_cpu_usage, mName));
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
        int mSensorNumber;
        double[] mUsage;
        HashMap<Integer,Integer> mCounts;
        
        public SensorUsage(int sensorNumber, String sensorName, long totalTime,
                HashMap<Integer,Integer> counts) {
            mName = sensorName;
            mSensorNumber = sensorNumber;
            
            mUsage = new double[1];
            mUsage[0] = totalTime;
            
            mCounts = counts;
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
            
            PackageManager pm = getPackageManager();
            String[] packageNames = null;
            for (Map.Entry<Integer,Integer> ent : mCounts.entrySet()) {
                int uid = ent.getKey().intValue();
                int count = ent.getValue().intValue();
                packageNames = pm.getPackagesForUid(uid).clone();
                
                if (packageNames.length == 1) {
                    info.append(getString(R.string.battery_history_sensor_usage,
                            count, packageNames[0]));
                } else {
                    info.append(getString(R.string.battery_history_sensor_usage_multi, count));
                    info.append("\n");
                    // Convert package names to user-facing labels where possible
                    for (int i = 0; i < packageNames.length; i++) {
                        info.append("    ");
                        info.append(BatteryHistory.getLabel(packageNames[i], pm));
                        info.append("\n");
                    }
                }
            }
        }
    }
    
    private List<? extends Graphable> getGraphRecords() {
        switch (mType) {
            case CPU_USAGE: return mCpuUsage;
            case NETWORK_USAGE : return mNetworkUsage;
            case SENSOR_USAGE: return mSensorUsage;
            case SCREEN_ON: return null;
            case WAKE_LOCKS:
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
                        mCpuUsage.add(new CpuUsage(ent.getKey(), userTime, systemTime, starts));
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
    
    class SensorRecord {
        String name;
        long totalTime;
        HashMap<Integer,Integer> counts = new HashMap<Integer,Integer>();
    }
    
    private void processSensorUsage() {
        mSensorUsage.clear();
        
        HashMap<Integer,SensorRecord> records = new HashMap<Integer,SensorRecord>();
        
        long uSecTime = SystemClock.elapsedRealtime() * 1000;
        final long uSecNow = mStats.getBatteryUptime(uSecTime);
        
        SparseArray<? extends Uid> uidStats = mStats.getUidStats();
        final int NU = uidStats.size();
        for (int iu = 0; iu < NU; iu++) {
            Uid u = uidStats.valueAt(iu);
            int uid = u.getUid();
            
            Map<Integer, ? extends BatteryStats.Uid.Sensor> sensorStats = u.getSensorStats();
            if (sensorStats.size() > 0) {
                for (Map.Entry<Integer, ? extends BatteryStats.Uid.Sensor> ent
                        : sensorStats.entrySet()) {

                    Uid.Sensor se = ent.getValue();
                    String name = se.getName();
                    int sensorNumber = ent.getKey();
                    Timer timer = se.getSensorTime();
                    if (timer != null) {
                        // Convert from microseconds to milliseconds with rounding
                        long totalTime = (timer.getTotalTime(uSecNow, mWhich) + 500) / 1000;
                        int count = timer.getCount(mWhich);
                        
                        SensorRecord record = records.get(sensorNumber);
                        if (record == null) {
                            record = new SensorRecord();
                        }
                        record.name = name;
                        record.totalTime += totalTime;
                        Integer c = record.counts.get(uid);
                        if (c == null) {
                            record.counts.put(uid, count);
                        } else {
                            record.counts.put(uid, c.intValue() + count);
                        }
                        records.put(sensorNumber, record);
                    }
                }
            }
        }
        
        for (Map.Entry<Integer,SensorRecord> record : records.entrySet()) {
            int sensorNumber = record.getKey().intValue();
            SensorRecord r = record.getValue();
            mSensorUsage.add(new SensorUsage(sensorNumber, r.name, r.totalTime, r.counts));
        }
        Collections.sort(mSensorUsage);
    }
    
    private void processScreenOn() {
        // Do nothing
    }
    
    private void collectStatistics() {
        processCpuUsage();
        processNetworkUsage();
        processSensorUsage();
        processScreenOn();
    }
    
    private void refresh() {
        try {
            mStats = mBatteryInfo.getStatistics();
            collectStatistics();
            displayGraph();
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
        if (parent.equals(mTypeSpinner)) {
            mType = position;
            switch (position) {
                case CPU_USAGE:
                    mWhichSpinner.setEnabled(true);
                    break;
                case NETWORK_USAGE:
                    mWhichSpinner.setEnabled(true);
                    break;
                case SENSOR_USAGE:
                    mWhichSpinner.setEnabled(true);
                    break;
                case SCREEN_ON:
                    mWhichSpinner.setEnabled(false);
                    break;
                case WAKE_LOCKS:
                    break;
            }
        } else if (parent.equals(mWhichSpinner)) {
            mWhich = position;
        }
        
        refresh();
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.i(TAG, "onCreate");
        
        String knownApps = getString(R.string.battery_history_known_apps);
        String[] knownAppNames = knownApps.split(";");
        for (String name : knownAppNames) {
            mKnownApps.add(name);
        }
        
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
        
        mBatteryInfo = IBatteryStats.Stub.asInterface(ServiceManager.getService("batteryinfo"));
        refresh();
    }
}
