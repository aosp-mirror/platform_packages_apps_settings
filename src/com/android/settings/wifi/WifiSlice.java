/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;
import static android.provider.SettingsSlicesContract.KEY_WIFI;

import android.annotation.ColorInt;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiSsid;
import android.os.Bundle;
import android.provider.SettingsSlicesContract;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settings.wifi.details.WifiNetworkDetailsFragment;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.WifiTracker;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to build a Wifi Slice, and handle all associated actions.
 */
public class WifiSlice implements CustomSliceable {

    /**
     * Backing Uri for the Wifi Slice.
     */
    public static final Uri WIFI_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSlicesContract.AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(KEY_WIFI)
            .build();

    @VisibleForTesting
    static final int DEFAULT_EXPANDED_ROW_COUNT = 3;

    private final Context mContext;

    public WifiSlice(Context context) {
        mContext = context;
    }

    @Override
    public Uri getUri() {
        return WIFI_URI;
    }

    @Override
    public IntentFilter getIntentFilter() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        return filter;
    }

    /**
     * Return a Wifi Slice bound to {@link #WIFI_URI}.
     */
    @Override
    public Slice getSlice() {
        final boolean isWifiEnabled = isWifiEnabled();
        final IconCompat icon = IconCompat.createWithResource(mContext,
                R.drawable.ic_settings_wireless);
        final String title = mContext.getString(R.string.wifi_settings);
        final CharSequence summary = getSummary();
        @ColorInt final int color = Utils.getColorAccentDefaultColor(mContext);
        final PendingIntent toggleAction = getBroadcastIntent(mContext);
        final PendingIntent primaryAction = getPrimaryAction();
        final SliceAction primarySliceAction = new SliceAction(primaryAction, icon, title);
        final SliceAction toggleSliceAction = new SliceAction(toggleAction, null /* actionTitle */,
                isWifiEnabled);

        final ListBuilder listBuilder = new ListBuilder(mContext, WIFI_URI, ListBuilder.INFINITY)
                .setAccentColor(color)
                .addRow(new RowBuilder()
                        .setTitle(title)
                        .setSubtitle(summary)
                        .addEndItem(toggleSliceAction)
                        .setPrimaryAction(primarySliceAction));

        if (!isWifiEnabled) {
            return listBuilder.build();
        }

        List<AccessPoint> results = getBackgroundWorker().getResults();
        if (results == null) {
            results = new ArrayList<>();
        }
        final int apCount = results.size();
        // Add AP rows
        final CharSequence placeholder = mContext.getText(R.string.summary_placeholder);
        for (int i = 0; i < DEFAULT_EXPANDED_ROW_COUNT; i++) {
            if (i < apCount) {
                listBuilder.addRow(getAccessPointRow(results.get(i)));
            } else {
                listBuilder.addRow(new RowBuilder()
                        .setTitle(placeholder)
                        .setSubtitle(placeholder));
            }
        }
        return listBuilder.build();
    }

    private RowBuilder getAccessPointRow(AccessPoint accessPoint) {
        final String title = accessPoint.getConfigName();
        final IconCompat levelIcon = IconCompat.createWithResource(mContext,
                com.android.settingslib.Utils.getWifiIconResource(accessPoint.getLevel()));
        final CharSequence apSummary = accessPoint.getSettingsSummary();
        final RowBuilder rowBuilder = new RowBuilder()
                .setTitleItem(levelIcon, ListBuilder.ICON_IMAGE)
                .setTitle(title)
                .setSubtitle(!TextUtils.isEmpty(apSummary)
                        ? apSummary
                        : mContext.getText(R.string.summary_placeholder))
                .setPrimaryAction(new SliceAction(
                        getAccessPointAction(accessPoint), levelIcon, title));

        final IconCompat endIcon = getEndIcon(accessPoint);
        if (endIcon != null) {
            rowBuilder.addEndItem(endIcon, ListBuilder.ICON_IMAGE);
        }
        return rowBuilder;
    }

    private IconCompat getEndIcon(AccessPoint accessPoint) {
        if (accessPoint.isActive()) {
            return IconCompat.createWithResource(mContext, R.drawable.ic_settings);
        } else if (accessPoint.getSecurity() != AccessPoint.SECURITY_NONE) {
            return IconCompat.createWithResource(mContext, R.drawable.ic_friction_lock_closed);
        } else if (accessPoint.isMetered()) {
            return IconCompat.createWithResource(mContext, R.drawable.ic_friction_money);
        }
        return null;
    }

    private PendingIntent getAccessPointAction(AccessPoint accessPoint) {
        final Bundle extras = new Bundle();
        accessPoint.saveWifiState(extras);

        Intent intent;
        if (accessPoint.isActive()) {
            intent = new SubSettingLauncher(mContext)
                    .setTitleRes(R.string.pref_title_network_details)
                    .setDestination(WifiNetworkDetailsFragment.class.getName())
                    .setArguments(extras)
                    .setSourceMetricsCategory(MetricsEvent.WIFI)
                    .toIntent();
        } else {
            intent = new Intent(mContext, WifiDialogActivity.class);
            intent.putExtra(WifiDialogActivity.KEY_ACCESS_POINT_STATE, extras);
        }
        return PendingIntent.getActivity(mContext, accessPoint.hashCode() /* requestCode */,
                intent, 0 /* flags */);
    }

    /**
     * Update the current wifi status to the boolean value keyed by
     * {@link android.app.slice.Slice#EXTRA_TOGGLE_STATE} on {@param intent}.
     */
    @Override
    public void onNotifyChange(Intent intent) {
        final WifiManager wifiManager = mContext.getSystemService(WifiManager.class);
        final boolean newState = intent.getBooleanExtra(EXTRA_TOGGLE_STATE,
                wifiManager.isWifiEnabled());
        wifiManager.setWifiEnabled(newState);
        // Do not notifyChange on Uri. The service takes longer to update the current value than it
        // does for the Slice to check the current value again. Let {@link SliceBroadcastRelay}
        // handle it.
    }

    @Override
    public Intent getIntent() {
        final String screenTitle = mContext.getText(R.string.wifi_settings).toString();
        final Uri contentUri = new Uri.Builder().appendPath(KEY_WIFI).build();
        final Intent intent = SliceBuilderUtils.buildSearchResultPageIntent(mContext,
                WifiSettings.class.getName(), KEY_WIFI, screenTitle,
                MetricsEvent.DIALOG_WIFI_AP_EDIT)
                .setClassName(mContext.getPackageName(), SubSettings.class.getName())
                .setData(contentUri);

        return intent;
    }

    private boolean isWifiEnabled() {
        final WifiManager wifiManager = mContext.getSystemService(WifiManager.class);

        switch (wifiManager.getWifiState()) {
            case WifiManager.WIFI_STATE_ENABLED:
            case WifiManager.WIFI_STATE_ENABLING:
                return true;
            case WifiManager.WIFI_STATE_DISABLED:
            case WifiManager.WIFI_STATE_DISABLING:
            case WifiManager.WIFI_STATE_UNKNOWN:
            default:
                return false;
        }
    }

    private CharSequence getSummary() {
        final WifiManager wifiManager = mContext.getSystemService(WifiManager.class);

        switch (wifiManager.getWifiState()) {
            case WifiManager.WIFI_STATE_ENABLED:
                final String ssid = WifiInfo.removeDoubleQuotes(wifiManager.getConnectionInfo()
                        .getSSID());
                if (TextUtils.equals(ssid, WifiSsid.NONE)) {
                    return mContext.getText(R.string.disconnected);
                }
                return ssid;
            case WifiManager.WIFI_STATE_ENABLING:
                return mContext.getText(R.string.disconnected);
            case WifiManager.WIFI_STATE_DISABLED:
            case WifiManager.WIFI_STATE_DISABLING:
                return mContext.getText(R.string.switch_off_text);
            case WifiManager.WIFI_STATE_UNKNOWN:
            default:
                return "";
        }
    }

    private PendingIntent getPrimaryAction() {
        final Intent intent = getIntent();
        return PendingIntent.getActivity(mContext, 0 /* requestCode */,
                intent, 0 /* flags */);
    }

    @Override
    public SliceBackgroundWorker getBackgroundWorker() {
        return WifiScanWorker.getInstance(mContext, WIFI_URI);
    }

    private static class WifiScanWorker extends SliceBackgroundWorker<AccessPoint>
            implements WifiTracker.WifiListener {

        // TODO: enforce all the SliceBackgroundWorkers being singletons at syntax level
        private static WifiScanWorker mWifiScanWorker;

        private final Context mContext;

        private WifiTracker mWifiTracker;

        private WifiScanWorker(Context context, Uri uri) {
            super(context.getContentResolver(), uri);
            mContext = context;
        }

        public static WifiScanWorker getInstance(Context context, Uri uri) {
            if (mWifiScanWorker == null) {
                mWifiScanWorker = new WifiScanWorker(context, uri);
            }
            return mWifiScanWorker;
        }

        @Override
        protected void onSlicePinned() {
            if (mWifiTracker == null) {
                mWifiTracker = new WifiTracker(mContext, this, true, true);
            }
            mWifiTracker.onStart();
            onAccessPointsChanged();
        }

        @Override
        protected void onSliceUnpinned() {
            mWifiTracker.onStop();
        }

        @Override
        public void close() {
            mWifiTracker.onDestroy();
            mWifiScanWorker = null;
        }

        @Override
        public void onWifiStateChanged(int state) {
        }

        @Override
        public void onConnectedChanged() {
        }

        @Override
        public void onAccessPointsChanged() {
            // in case state has changed
            if (!mWifiTracker.getManager().isWifiEnabled()) {
                updateResults(null);
                return;
            }
            // AccessPoints are sorted by the WifiTracker
            final List<AccessPoint> accessPoints = mWifiTracker.getAccessPoints();
            final List<AccessPoint> resultList = new ArrayList<>();
            for (AccessPoint ap : accessPoints) {
                if (ap.isReachable()) {
                    resultList.add(ap);
                }
            }
            updateResults(resultList);
        }
    }
}
