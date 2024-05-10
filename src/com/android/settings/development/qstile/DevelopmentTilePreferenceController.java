/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development.qstile;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import com.android.internal.statusbar.IStatusBarService;
import com.android.settings.core.BasePreferenceController;

import java.util.ArrayList;
import java.util.List;

public class DevelopmentTilePreferenceController extends BasePreferenceController {

    private static final String TAG = "DevTilePrefController";
    private final OnChangeHandler mOnChangeHandler;
    private final PackageManager mPackageManager;

    public DevelopmentTilePreferenceController(Context context, String key) {
        super(context, key);
        mOnChangeHandler = new OnChangeHandler(context);
        mPackageManager = context.getPackageManager();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Context context = screen.getContext();
        List<ServiceInfo> serviceInfos = getTileServiceList(context);

        for (ServiceInfo sInfo : serviceInfos) {
            // Check if the tile requires a flag. If it does, hide tile if flag is off.
            if (sInfo.metaData != null) {
                String flag = sInfo.metaData.getString(
                        DevelopmentTiles.META_DATA_REQUIRES_SYSTEM_PROPERTY);
                if (flag != null) {
                    boolean enabled = SystemProperties.getBoolean(flag, false);
                    if (!enabled) {
                        // Flagged tile, flag is not enabled
                        continue;
                    }
                }
            }

            final int enabledSetting = mPackageManager.getComponentEnabledSetting(
                    new ComponentName(sInfo.packageName, sInfo.name));
            boolean checked = enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    || ((enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
                    && sInfo.enabled);

            TwoStatePreference preference = new SwitchPreferenceCompat(context);
            preference.setTitle(sInfo.loadLabel(mPackageManager));
            preference.setIcon(sInfo.icon);
            preference.setKey(sInfo.name);
            preference.setChecked(checked);
            preference.setOnPreferenceChangeListener(mOnChangeHandler);
            screen.addPreference(preference);
        }
    }

    /**
     * Get Quick Settings services from PackageManager
     */
    public static List<ServiceInfo> getTileServiceList(Context context) {
        Intent intent = new Intent(TileService.ACTION_QS_TILE)
                .setPackage(context.getPackageName());
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfos = packageManager.queryIntentServices(intent,
                PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.GET_META_DATA);

        List<ServiceInfo> servicesInfos = new ArrayList<>();
        for (ResolveInfo info : resolveInfos) {
            ServiceInfo sInfo = info.serviceInfo;
            servicesInfos.add(sInfo);
        }
        return servicesInfos;
    }

    @VisibleForTesting
    static class OnChangeHandler implements Preference.OnPreferenceChangeListener {

        private final Context mContext;
        private final PackageManager mPackageManager;
        private IStatusBarService mStatusBarService;

        public OnChangeHandler(Context context) {
            mContext = context;
            mPackageManager = context.getPackageManager();
            mStatusBarService = IStatusBarService.Stub.asInterface(
                    ServiceManager.checkService(Context.STATUS_BAR_SERVICE));
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            ComponentName componentName = new ComponentName(
                    mContext.getPackageName(), preference.getKey());
            mPackageManager.setComponentEnabledSetting(componentName, enabled
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            try {
                if (mStatusBarService != null) {
                    if (enabled) {
                        mStatusBarService.addTile(componentName);
                    } else {
                        mStatusBarService.remTile(componentName);
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to modify QS tile for component " +
                        componentName.toString(), e);
            }
            return true;
        }
    }
}
