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

package com.android.settings.wifi;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.NetworkScorerAppData;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.network.NetworkScoreManagerWrapper;
import com.android.settings.utils.NotificationChannelHelper;
import com.android.settings.utils.NotificationChannelHelper.NotificationChannelWrapper;

/**
 * {@link PreferenceController} that shows whether we should notify user when open network is
 * available. The preference links to {@link NotificationChannel} settings.
 */
public class NotifyOpenNetworksPreferenceController extends PreferenceController {

    private static final String TAG = "OpenNetworks";
    private static final String KEY_NOTIFY_OPEN_NETWORKS = "notify_open_networks";

    private NetworkScoreManagerWrapper mNetworkScoreManager;
    private NotificationChannelHelper mNotificationChannelHelper;
    private PackageManager mPackageManager;

    public NotifyOpenNetworksPreferenceController(
            Context context,
            NetworkScoreManagerWrapper networkScoreManager,
            NotificationChannelHelper notificationChannelHelper,
            PackageManager packageManager) {
        super(context);
        mNetworkScoreManager = networkScoreManager;
        mNotificationChannelHelper = notificationChannelHelper;
        mPackageManager = packageManager;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_NOTIFY_OPEN_NETWORKS;
    }

    @Override
    public boolean isAvailable() {
        return getNotificationChannel() != null;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), KEY_NOTIFY_OPEN_NETWORKS)) {
            return false;
        }
        NetworkScorerAppData scorer = mNetworkScoreManager.getActiveScorer();
        if (scorer == null) {
            return false;
        }

        Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_CHANNEL_ID,
                scorer.getNetworkAvailableNotificationChannelId());
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, scorer.getRecommendationServicePackageName());
        mContext.startActivity(intent);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        NotificationChannelWrapper channel = getNotificationChannel();
        if (channel == null) {
            preference.setSummary(null);
        } else {
            preference.setSummary(channel.getImportance() != NotificationManager.IMPORTANCE_NONE ?
                    R.string.notification_toggle_on : R.string.notification_toggle_off);
        }
    }

    @Nullable
    private NotificationChannelWrapper getNotificationChannel() {
        NetworkScorerAppData scorer = mNetworkScoreManager.getActiveScorer();
        if (scorer == null) {
            return null;
        }
        String packageName = scorer.getRecommendationServicePackageName();
        String channelId = scorer.getNetworkAvailableNotificationChannelId();
        if (packageName == null || channelId == null) {
            return null;
        }
        try {
            return mNotificationChannelHelper.getNotificationChannelForPackage(
                    packageName,
                    mPackageManager.getPackageUid(packageName, 0 /* flags */),
                    channelId,
                    false /* includeDeleted */ );
        } catch (RemoteException | PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Failed to get notification channel.", e);
            return null;
        }
    }
}
