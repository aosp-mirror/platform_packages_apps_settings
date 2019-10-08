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

package com.android.settings.wifi.slice;

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;
import static android.provider.SettingsSlicesContract.KEY_WIFI;

import static com.android.settings.slices.CustomSliceRegistry.WIFI_SLICE_URI;

import android.annotation.ColorInt;
import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settings.wifi.WifiDialogActivity;
import com.android.settings.wifi.WifiSettings;
import com.android.settings.wifi.WifiUtils;
import com.android.settings.wifi.details.WifiNetworkDetailsFragment;
import com.android.settingslib.wifi.AccessPoint;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link CustomSliceable} for Wi-Fi, used by generic clients.
 */
public class WifiSlice implements CustomSliceable {

    @VisibleForTesting
    static final int DEFAULT_EXPANDED_ROW_COUNT = 3;

    protected final Context mContext;
    protected final WifiManager mWifiManager;
    protected final ConnectivityManager mConnectivityManager;

    public WifiSlice(Context context) {
        mContext = context;
        mWifiManager = mContext.getSystemService(WifiManager.class);
        mConnectivityManager = mContext.getSystemService(ConnectivityManager.class);
    }

    @Override
    public Uri getUri() {
        return WIFI_SLICE_URI;
    }

    @Override
    public Slice getSlice() {
        // Reload theme for switching dark mode on/off
        mContext.getTheme().applyStyle(R.style.Theme_Settings_Home, true /* force */);

        final boolean isWifiEnabled = isWifiEnabled();
        ListBuilder listBuilder = getHeaderRow(isWifiEnabled);
        if (!isWifiEnabled) {
            WifiScanWorker.clearClickedWifi();
            return listBuilder.build();
        }

        final WifiScanWorker worker = SliceBackgroundWorker.getInstance(getUri());
        final List<AccessPoint> apList = worker != null ? worker.getResults() : null;
        final int apCount = apList == null ? 0 : apList.size();
        final boolean isFirstApActive = apCount > 0 && apList.get(0).isActive();
        handleNetworkCallback(worker, isFirstApActive);

        // Need a loading text when results are not ready or out of date.
        boolean needLoadingRow = true;
        // Skip checking the existence of the first access point if it's active
        int index = isFirstApActive ? 1 : 0;
        // This loop checks the existence of reachable APs to determine the validity of the current
        // AP list.
        for (; index < apCount; index++) {
            if (apList.get(index).isReachable()) {
                needLoadingRow = false;
                break;
            }
        }

        // Add AP rows
        final CharSequence placeholder = mContext.getText(R.string.summary_placeholder);
        for (int i = 0; i < DEFAULT_EXPANDED_ROW_COUNT; i++) {
            if (i < apCount) {
                listBuilder.addRow(getAccessPointRow(apList.get(i)));
            } else if (needLoadingRow) {
                listBuilder.addRow(getLoadingRow(placeholder));
                needLoadingRow = false;
            } else {
                listBuilder.addRow(new ListBuilder.RowBuilder()
                        .setTitle(placeholder)
                        .setSubtitle(placeholder));
            }
        }
        return listBuilder.build();
    }

    private void handleNetworkCallback(WifiScanWorker worker, boolean isFirstApActive) {
        if (worker == null) {
            return;
        }
        if (isFirstApActive) {
            worker.registerNetworkCallback(mWifiManager.getCurrentNetwork());
        } else {
            worker.unregisterNetworkCallback();
        }
    }

    private ListBuilder getHeaderRow(boolean isWifiEnabled) {
        final IconCompat icon = IconCompat.createWithResource(mContext,
                R.drawable.ic_settings_wireless);
        final String title = mContext.getString(R.string.wifi_settings);
        final PendingIntent toggleAction = getBroadcastIntent(mContext);
        final PendingIntent primaryAction = getPrimaryAction();
        final SliceAction primarySliceAction = SliceAction.createDeeplink(primaryAction, icon,
                ListBuilder.ICON_IMAGE, title);
        final SliceAction toggleSliceAction = SliceAction.createToggle(toggleAction,
                null /* actionTitle */, isWifiEnabled);

        return new ListBuilder(mContext, getUri(), ListBuilder.INFINITY)
                .setAccentColor(COLOR_NOT_TINTED)
                .setKeywords(getKeywords())
                .addRow(new ListBuilder.RowBuilder()
                        .setTitle(title)
                        .addEndItem(toggleSliceAction)
                        .setPrimaryAction(primarySliceAction));
    }

    private ListBuilder.RowBuilder getAccessPointRow(AccessPoint accessPoint) {
        final boolean isCaptivePortal = accessPoint.isActive() && isCaptivePortal();
        final CharSequence title = accessPoint.getTitle();
        final CharSequence summary = getAccessPointSummary(accessPoint, isCaptivePortal);
        final IconCompat levelIcon = getAccessPointLevelIcon(accessPoint);
        final ListBuilder.RowBuilder rowBuilder = new ListBuilder.RowBuilder()
                .setTitleItem(levelIcon, ListBuilder.ICON_IMAGE)
                .setTitle(title)
                .setSubtitle(summary)
                .setPrimaryAction(getAccessPointAction(accessPoint, isCaptivePortal, levelIcon,
                        title));

        if (isCaptivePortal) {
            rowBuilder.addEndItem(getCaptivePortalEndAction(accessPoint, title));
        } else {
            final IconCompat endIcon = getEndIcon(accessPoint);
            if (endIcon != null) {
                rowBuilder.addEndItem(endIcon, ListBuilder.ICON_IMAGE);
            }
        }
        return rowBuilder;
    }

    private CharSequence getAccessPointSummary(AccessPoint accessPoint, boolean isCaptivePortal) {
        if (isCaptivePortal) {
            return mContext.getText(R.string.wifi_tap_to_sign_in);
        }

        final CharSequence summary = accessPoint.getSettingsSummary();
        return TextUtils.isEmpty(summary) ? mContext.getText(R.string.disconnected) : summary;
    }

    private IconCompat getAccessPointLevelIcon(AccessPoint accessPoint) {
        final Drawable d = mContext.getDrawable(
                com.android.settingslib.Utils.getWifiIconResource(accessPoint.getLevel()));

        final @ColorInt int color;
        if (accessPoint.isActive()) {
            final NetworkInfo.State state = accessPoint.getNetworkInfo().getState();
            if (state == NetworkInfo.State.CONNECTED) {
                color = Utils.getColorAccentDefaultColor(mContext);
            } else { // connecting
                color = Utils.getDisabled(mContext, Utils.getColorAttrDefaultColor(mContext,
                        android.R.attr.colorControlNormal));
            }
        } else {
            color = Utils.getColorAttrDefaultColor(mContext, android.R.attr.colorControlNormal);
        }

        d.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        return Utils.createIconWithDrawable(d);
    }

    private IconCompat getEndIcon(AccessPoint accessPoint) {
        if (accessPoint.isActive()) {
            return null;
        } else if (accessPoint.getSecurity() != AccessPoint.SECURITY_NONE) {
            return IconCompat.createWithResource(mContext, R.drawable.ic_friction_lock_closed);
        } else if (accessPoint.isMetered()) {
            return IconCompat.createWithResource(mContext, R.drawable.ic_friction_money);
        }
        return null;
    }

    private SliceAction getCaptivePortalEndAction(AccessPoint accessPoint, CharSequence title) {
        return getAccessPointAction(accessPoint, false /* isCaptivePortal */,
                IconCompat.createWithResource(mContext, R.drawable.ic_settings_accent), title);
    }

    private SliceAction getAccessPointAction(AccessPoint accessPoint, boolean isCaptivePortal,
            IconCompat icon, CharSequence title) {
        final int requestCode = accessPoint.hashCode();
        if (isCaptivePortal) {
            final Intent intent = new Intent(mContext, ConnectToWifiHandler.class)
                    .putExtra(ConnectivityManager.EXTRA_NETWORK, mWifiManager.getCurrentNetwork());
            return getBroadcastAction(requestCode, intent, icon, title);
        }

        final Bundle extras = new Bundle();
        accessPoint.saveWifiState(extras);

        if (accessPoint.isActive()) {
            final Intent intent = new SubSettingLauncher(mContext)
                    .setTitleRes(R.string.pref_title_network_details)
                    .setDestination(WifiNetworkDetailsFragment.class.getName())
                    .setArguments(extras)
                    .setSourceMetricsCategory(SettingsEnums.WIFI)
                    .toIntent();
            return getActivityAction(requestCode, intent, icon, title);
        } else if (WifiUtils.getConnectingType(accessPoint) != WifiUtils.CONNECT_TYPE_OTHERS) {
            final Intent intent = new Intent(mContext, ConnectToWifiHandler.class)
                    .putExtra(WifiDialogActivity.KEY_ACCESS_POINT_STATE, extras);
            return getBroadcastAction(requestCode, intent, icon, title);
        } else {
            final Intent intent = new Intent(mContext, WifiDialogActivity.class)
                    .putExtra(WifiDialogActivity.KEY_ACCESS_POINT_STATE, extras);
            return getActivityAction(requestCode, intent, icon, title);
        }
    }

    private SliceAction getActivityAction(int requestCode, Intent intent, IconCompat icon,
            CharSequence title) {
        final PendingIntent pi = PendingIntent.getActivity(mContext, requestCode, intent,
                0 /* flags */);
        return SliceAction.createDeeplink(pi, icon, ListBuilder.ICON_IMAGE, title);
    }

    private SliceAction getBroadcastAction(int requestCode, Intent intent, IconCompat icon,
            CharSequence title) {
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        final PendingIntent pi = PendingIntent.getBroadcast(mContext, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return SliceAction.create(pi, icon, ListBuilder.ICON_IMAGE, title);
    }

    private ListBuilder.RowBuilder getLoadingRow(CharSequence placeholder) {
        final CharSequence title = mContext.getText(R.string.wifi_empty_list_wifi_on);

        // for aligning to the Wi-Fi AP's name
        final IconCompat emptyIcon = Utils.createIconWithDrawable(
                new ColorDrawable(Color.TRANSPARENT));

        return new ListBuilder.RowBuilder()
                .setTitleItem(emptyIcon, ListBuilder.ICON_IMAGE)
                .setTitle(placeholder)
                .setSubtitle(title);
    }

    private boolean isCaptivePortal() {
        final NetworkCapabilities nc = mConnectivityManager.getNetworkCapabilities(
                mWifiManager.getCurrentNetwork());
        return WifiUtils.canSignIntoNetwork(nc);
    }

    /**
     * Update the current wifi status to the boolean value keyed by
     * {@link android.app.slice.Slice#EXTRA_TOGGLE_STATE} on {@param intent}.
     */
    @Override
    public void onNotifyChange(Intent intent) {
        final boolean newState = intent.getBooleanExtra(EXTRA_TOGGLE_STATE,
                mWifiManager.isWifiEnabled());
        mWifiManager.setWifiEnabled(newState);
        // Do not notifyChange on Uri. The service takes longer to update the current value than it
        // does for the Slice to check the current value again. Let {@link WifiScanWorker}
        // handle it.
    }

    @Override
    public Intent getIntent() {
        final String screenTitle = mContext.getText(R.string.wifi_settings).toString();
        final Uri contentUri = new Uri.Builder().appendPath(KEY_WIFI).build();
        final Intent intent = SliceBuilderUtils.buildSearchResultPageIntent(mContext,
                WifiSettings.class.getName(), KEY_WIFI, screenTitle,
                SettingsEnums.DIALOG_WIFI_AP_EDIT)
                .setClassName(mContext.getPackageName(), SubSettings.class.getName())
                .setData(contentUri);

        return intent;
    }

    private boolean isWifiEnabled() {
        switch (mWifiManager.getWifiState()) {
            case WifiManager.WIFI_STATE_ENABLED:
            case WifiManager.WIFI_STATE_ENABLING:
                return true;
            default:
                return false;
        }
    }

    private PendingIntent getPrimaryAction() {
        final Intent intent = getIntent();
        return PendingIntent.getActivity(mContext, 0 /* requestCode */,
                intent, 0 /* flags */);
    }

    private Set<String> getKeywords() {
        final String keywords = mContext.getString(R.string.keywords_wifi);
        return Arrays.asList(TextUtils.split(keywords, ","))
                .stream()
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    @Override
    public Class getBackgroundWorkerClass() {
        return WifiScanWorker.class;
    }
}
