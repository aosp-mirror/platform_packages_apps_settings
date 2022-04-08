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

package com.android.settings.fuelgauge;

import android.os.BatteryStats;
import android.os.BatteryStats.Counter;
import android.os.BatteryStats.Timer;
import android.os.BatteryStats.Uid;
import android.util.ArrayMap;
import android.util.SparseArray;
import android.util.SparseIntArray;

/**
 * Fake UID for testing power usage screen.
 */
public class FakeUid extends Uid {

    private final int mUid;

    public FakeUid(int uid) {
        mUid = uid;
    }

    @Override
    public int getUid() {
        return mUid;
    }

    @Override
    public ArrayMap<String, ? extends Wakelock> getWakelockStats() {
        return null;
    }

    @Override
    public Timer getAggregatedPartialWakelockTimer() {
        return null;
    }

    @Override
    public Timer getMulticastWakelockStats() {
        return null;
    }

    @Override
    public ArrayMap<String, ? extends Timer> getSyncStats() {
        return null;
    }

    @Override
    public ArrayMap<String, ? extends Timer> getJobStats() {
        return null;
    }

    @Override
    public ArrayMap<String, SparseIntArray> getJobCompletionStats() {
        return null;
    }

    @Override
    public SparseArray<? extends Sensor> getSensorStats() {
        return null;
    }

    @Override
    public SparseArray<? extends Pid> getPidStats() {
        return null;
    }

    @Override
    public ArrayMap<String, ? extends Proc> getProcessStats() {
        return null;
    }

    @Override
    public ArrayMap<String, ? extends Pkg> getPackageStats() {
        return null;
    }

    @Override
    public void noteWifiRunningLocked(long elapsedRealtime) {
    }

    @Override
    public void noteWifiStoppedLocked(long elapsedRealtime) {
    }

    @Override
    public void noteFullWifiLockAcquiredLocked(long elapsedRealtime) {
    }

    @Override
    public void noteFullWifiLockReleasedLocked(long elapsedRealtime) {
    }

    @Override
    public void noteWifiScanStartedLocked(long elapsedRealtime) {
    }

    @Override
    public void noteWifiScanStoppedLocked(long elapsedRealtime) {
    }

    @Override
    public void noteWifiBatchedScanStartedLocked(int csph, long elapsedRealtime) {
    }

    @Override
    public void noteWifiBatchedScanStoppedLocked(long elapsedRealtime) {
    }

    @Override
    public void noteWifiMulticastEnabledLocked(long elapsedRealtime) {
    }

    @Override
    public void noteWifiMulticastDisabledLocked(long elapsedRealtime) {
    }

    @Override
    public void noteActivityResumedLocked(long elapsedRealtime) {
    }

    @Override
    public void noteActivityPausedLocked(long elapsedRealtime) {
    }

    @Override
    public long getWifiRunningTime(long elapsedRealtimeUs, int which) {
        return 0;
    }

    @Override
    public long getFullWifiLockTime(long elapsedRealtimeUs, int which) {
        return 0;
    }

    @Override
    public long getWifiScanTime(long elapsedRealtimeUs, int which) {
        return 0;
    }

    @Override
    public int getWifiScanCount(int which) {
        return 0;
    }

    @Override
    public Timer getWifiScanTimer() {
        return null;
    }

    @Override
    public int getWifiScanBackgroundCount(int which)  {
        return 0;
    }

    @Override
    public long getWifiScanActualTime(long elapsedRealtimeUs)  {
        return 0;
    }

    @Override
    public long getWifiScanBackgroundTime(long elapsedRealtimeUs)  {
        return 0;
    }

    @Override
    public Timer getWifiScanBackgroundTimer() {
        return null;
    }

    @Override
    public long getWifiBatchedScanTime(int csphBin, long elapsedRealtimeUs, int which) {
        return 0;
    }

    @Override
    public int getWifiBatchedScanCount(int csphBin, int which) {
        return 0;
    }

    @Override
    public long getWifiMulticastTime(long elapsedRealtimeUs, int which) {
        return 0;
    }

    @Override
    public Timer getAudioTurnedOnTimer() {
        return null;
    }

    @Override
    public Timer getVideoTurnedOnTimer() {
        return null;
    }

    @Override
    public Timer getFlashlightTurnedOnTimer() {
        return null;
    }

    @Override
    public Timer getCameraTurnedOnTimer() {
        return null;
    }

    @Override
    public Timer getForegroundActivityTimer() {
        return null;
    }

    @Override
    public Timer getForegroundServiceTimer() {
        return null;
    }

    @Override
    public long getProcessStateTime(int state, long elapsedRealtimeUs, int which) {
        return 0;
    }

    @Override
    public Timer getProcessStateTimer(int state) {
        return null;
    }

    @Override
    public Timer getVibratorOnTimer() {
        return null;
    }

    @Override
    public void noteUserActivityLocked(int type) {
    }

    @Override
    public boolean hasUserActivity() {
        return false;
    }

    @Override
    public int getUserActivityCount(int type, int which) {
        return 0;
    }

    @Override
    public boolean hasNetworkActivity() {
        return false;
    }

    @Override
    public long getNetworkActivityBytes(int type, int which) {
        return 0;
    }

    @Override
    public long getNetworkActivityPackets(int type, int which) {
        return 0;
    }

    @Override
    public long getMobileRadioActiveTime(int which) {
        return 0;
    }

    @Override
    public int getMobileRadioActiveCount(int which) {
        return 0;
    }

    @Override
    public long getUserCpuTimeUs(int which) {
        return 0;
    }

    @Override
    public long getSystemCpuTimeUs(int which) {
        return 0;
    }

    @Override
    public long getTimeAtCpuSpeed(int cluster, int step, int which) {
        return 0;
    }

    @Override
    public BatteryStats.ControllerActivityCounter getWifiControllerActivity() {
        return null;
    }

    @Override
    public BatteryStats.ControllerActivityCounter getBluetoothControllerActivity() {
        return null;
    }

    @Override
    public BatteryStats.ControllerActivityCounter getModemControllerActivity() {
        return null;
    }

    @Override
    public Timer getBluetoothScanTimer() {
        return null;
    }

    @Override
    public Timer getBluetoothScanBackgroundTimer() {
        return null;
    }

    @Override
    public Timer getBluetoothUnoptimizedScanTimer() {
        return null;
    }

    @Override
    public Timer getBluetoothUnoptimizedScanBackgroundTimer() {
        return null;
    }

    @Override
    public Counter getBluetoothScanResultCounter() {
        return null;
    }

    @Override
    public Counter getBluetoothScanResultBgCounter() {
        return null;
    }

    @Override
    public long getWifiRadioApWakeupCount(int which) {
        return 0;
    }

    @Override
    public void getDeferredJobsCheckinLineLocked(StringBuilder sb, int which) {
    }

    @Override
    public void getDeferredJobsLineLocked(StringBuilder sb, int which) {
    }

    @Override
    public long getMobileRadioApWakeupCount(int which) {
        return 0;
    }

    @Override
    public long[] getCpuFreqTimes(int which) {
        return null;
    }

    @Override
    public long[] getScreenOffCpuFreqTimes(int which) {
        return null;
    }

    @Override
    public long getCpuActiveTime() {
        return 0;
    }

    @Override
    public long[] getCpuClusterTimes() {
        return null;
    }

    @Override
    public long[] getCpuFreqTimes(int procState, int which) {
        return null;
    }

    @Override
    public long[] getScreenOffCpuFreqTimes(int procState, int which) {
        return null;
    }
}
