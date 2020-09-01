/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkTemplate;
import android.net.Uri;
import android.os.IBinder;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.annotation.VisibleForTesting;

import com.android.settings.applications.ProcStatsData;
import com.android.settings.fuelgauge.batterytip.AnomalyConfigJobService;
import com.android.settingslib.net.DataUsageController;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class SettingsDumpService extends Service {
    @VisibleForTesting
    static final String KEY_SERVICE = "service";
    @VisibleForTesting
    static final String KEY_STORAGE = "storage";
    @VisibleForTesting
    static final String KEY_DATAUSAGE = "datausage";
    @VisibleForTesting
    static final String KEY_MEMORY = "memory";
    @VisibleForTesting
    static final String KEY_DEFAULT_BROWSER_APP = "default_browser_app";
    @VisibleForTesting
    static final String KEY_ANOMALY_DETECTION = "anomaly_detection";
    @VisibleForTesting
    static final Intent BROWSER_INTENT =
            new Intent("android.intent.action.VIEW", Uri.parse("http://"));

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        JSONObject dump = new JSONObject();

        try {
            dump.put(KEY_SERVICE, "Settings State");
            dump.put(KEY_STORAGE, dumpStorage());
            dump.put(KEY_DATAUSAGE, dumpDataUsage());
            dump.put(KEY_MEMORY, dumpMemory());
            dump.put(KEY_DEFAULT_BROWSER_APP, dumpDefaultBrowser());
            dump.put(KEY_ANOMALY_DETECTION, dumpAnomalyDetection());
        } catch (Exception e) {
            e.printStackTrace();
        }

        writer.println(dump);
    }

    private JSONObject dumpMemory() throws JSONException {
        JSONObject obj = new JSONObject();
        ProcStatsData statsManager = new ProcStatsData(this, false);
        statsManager.refreshStats(true);
        ProcStatsData.MemInfo memInfo = statsManager.getMemInfo();

        obj.put("used", String.valueOf(memInfo.realUsedRam));
        obj.put("free", String.valueOf(memInfo.realFreeRam));
        obj.put("total", String.valueOf(memInfo.realTotalRam));
        obj.put("state", statsManager.getMemState());

        return obj;
    }

    private JSONObject dumpDataUsage() throws JSONException {
        JSONObject obj = new JSONObject();
        DataUsageController controller = new DataUsageController(this);
        ConnectivityManager connectivityManager = getSystemService(ConnectivityManager.class);
        SubscriptionManager manager = this.getSystemService(SubscriptionManager.class);
        TelephonyManager telephonyManager = this.getSystemService(TelephonyManager.class);
        if (connectivityManager.isNetworkSupported(ConnectivityManager.TYPE_MOBILE)) {
            JSONArray array = new JSONArray();
            for (SubscriptionInfo info : manager.getAvailableSubscriptionInfoList()) {
                telephonyManager = telephonyManager
                        .createForSubscriptionId(info.getSubscriptionId());
                NetworkTemplate mobileAll = NetworkTemplate.buildTemplateMobileAll(
                        telephonyManager.getSubscriberId());
                final JSONObject usage = dumpDataUsage(mobileAll, controller);
                usage.put("subId", info.getSubscriptionId());
                array.put(usage);
            }
            obj.put("cell", array);
        }
        if (connectivityManager.isNetworkSupported(ConnectivityManager.TYPE_WIFI)) {
            obj.put("wifi", dumpDataUsage(NetworkTemplate.buildTemplateWifiWildcard(), controller));
        }
        if (connectivityManager.isNetworkSupported(ConnectivityManager.TYPE_ETHERNET)) {
            obj.put("ethernet", dumpDataUsage(NetworkTemplate.buildTemplateEthernet(), controller));
        }
        return obj;
    }

    private JSONObject dumpDataUsage(NetworkTemplate template, DataUsageController controller)
            throws JSONException {
        JSONObject obj = new JSONObject();
        DataUsageController.DataUsageInfo usage = controller.getDataUsageInfo(template);
        obj.put("carrier", usage.carrier);
        obj.put("start", usage.startDate);
        obj.put("usage", usage.usageLevel);
        obj.put("warning", usage.warningLevel);
        obj.put("limit", usage.limitLevel);
        return obj;
    }

    private JSONObject dumpStorage() throws JSONException {
        JSONObject obj = new JSONObject();
        StorageManager manager = getSystemService(StorageManager.class);
        for (VolumeInfo volume : manager.getVolumes()) {
            JSONObject volObj = new JSONObject();
            if (volume.isMountedReadable()) {
                File path = volume.getPath();
                volObj.put("used", String.valueOf(path.getTotalSpace() - path.getFreeSpace()));
                volObj.put("total", String.valueOf(path.getTotalSpace()));
            }
            volObj.put("path", volume.getInternalPath());
            volObj.put("state", volume.getState());
            volObj.put("stateDesc", volume.getStateDescription());
            volObj.put("description", volume.getDescription());
            obj.put(volume.getId(), volObj);
        }
        return obj;
    }

    @VisibleForTesting
    String dumpDefaultBrowser() {
        final ResolveInfo resolveInfo = getPackageManager().resolveActivity(
                BROWSER_INTENT, PackageManager.MATCH_DEFAULT_ONLY);

        if (resolveInfo == null || resolveInfo.activityInfo.packageName.equals("android")) {
            return null;
        } else {
            return resolveInfo.activityInfo.packageName;
        }
    }

    @VisibleForTesting
    JSONObject dumpAnomalyDetection() throws JSONException {
        final JSONObject obj = new JSONObject();
        final SharedPreferences sharedPreferences = getSharedPreferences(
                AnomalyConfigJobService.PREF_DB,
                Context.MODE_PRIVATE);
        final int currentVersion = sharedPreferences.getInt(
                AnomalyConfigJobService.KEY_ANOMALY_CONFIG_VERSION,
                0 /* defValue */);
        obj.put("anomaly_config_version", String.valueOf(currentVersion));

        return obj;
    }
}
