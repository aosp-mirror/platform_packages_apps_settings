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
package com.android.settings.dashboard.conditional;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.TetherSettings;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.TetherUtil;

public class HotspotCondition extends Condition {

    private final WifiManager mWifiManager;

    public HotspotCondition(ConditionManager manager) {
        super(manager);
        mWifiManager = mManager.getContext().getSystemService(WifiManager.class);
    }

    @Override
    public void refreshState() {
        boolean wifiTetherEnabled = mWifiManager.isWifiApEnabled();
        setActive(wifiTetherEnabled);
    }

    @Override
    protected Class<?> getReceiverClass() {
        return Receiver.class;
    }

    @Override
    public Icon getIcon() {
        return Icon.createWithResource(mManager.getContext(), R.drawable.ic_hotspot);
    }

    private String getSsid() {
        WifiConfiguration wifiConfig = mWifiManager.getWifiApConfiguration();
        if (wifiConfig == null) {
            return mManager.getContext().getString(
                    com.android.internal.R.string.wifi_tether_configure_ssid_default);
        } else {
            return wifiConfig.SSID;
        }
    }

    @Override
    public CharSequence getTitle() {
        return mManager.getContext().getString(R.string.condition_hotspot_title);
    }

    @Override
    public CharSequence getSummary() {
        return mManager.getContext().getString(R.string.condition_hotspot_summary, getSsid());
    }

    @Override
    public CharSequence[] getActions() {
        final Context context = mManager.getContext();
        if (RestrictedLockUtils.hasBaseUserRestriction(context,
                UserManager.DISALLOW_CONFIG_TETHERING, UserHandle.myUserId())) {
            return new CharSequence[0];
        }
        return new CharSequence[] { context.getString(R.string.condition_turn_off) };
    }

    @Override
    public void onPrimaryClick() {
        Utils.startWithFragment(mManager.getContext(), TetherSettings.class.getName(), null, null,
                0, R.string.tether_settings_title_all, null);
    }

    @Override
    public void onActionClick(int index) {
        if (index == 0) {
            final Context context = mManager.getContext();
            final EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(context,
                    UserManager.DISALLOW_CONFIG_TETHERING, UserHandle.myUserId());
            if (admin != null) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(context, admin);
            } else {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
                        Context.CONNECTIVITY_SERVICE);
                cm.stopTethering(ConnectivityManager.TETHERING_WIFI);
                setActive(false);
            }
        } else {
            throw new IllegalArgumentException("Unexpected index " + index);
        }
    }

    @Override
    public int getMetricsConstant() {
        return MetricsEvent.SETTINGS_CONDITION_HOTSPOT;
    }

    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                ConditionManager.get(context).getCondition(HotspotCondition.class)
                        .refreshState();
            }
        }
    }
}
