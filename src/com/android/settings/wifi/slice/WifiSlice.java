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
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.network.NetworkProviderSettings;
import com.android.settings.network.WifiSwitchPreferenceController;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settings.wifi.AppStateChangeWifiStateBridge;
import com.android.settings.wifi.WifiDialogActivity;
import com.android.settings.wifi.WifiUtils;
import com.android.settings.wifi.details.WifiNetworkDetailsFragment;
import com.android.settingslib.wifi.WifiEnterpriseRestrictionUtils;
import com.android.wifitrackerlib.WifiEntry;

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
    private static final String TAG = "WifiSlice";

    protected final Context mContext;
    protected final WifiManager mWifiManager;
    protected final WifiRestriction mWifiRestriction;

    public WifiSlice(Context context) {
        this(context, new WifiRestriction());
    }

    @VisibleForTesting
    WifiSlice(Context context, WifiRestriction wifiRestriction) {
        mContext = context;
        mWifiManager = mContext.getSystemService(WifiManager.class);
        mWifiRestriction = wifiRestriction;
    }

    @Override
    public Uri getUri() {
        return WIFI_SLICE_URI;
    }

    @Override
    public Slice getSlice() {
        // If external calling package doesn't have Wi-Fi permission.
        final boolean isPermissionGranted =
                isCallerExemptUid(mContext) || isPermissionGranted(mContext);
        final boolean isWifiEnabled = isWifiEnabled();
        ListBuilder listBuilder = getListBuilder(isWifiEnabled, null /* wifiSliceItem */,
                isPermissionGranted);
        // If the caller doesn't have the permission granted, just return a slice without a toggle.
        if (!isWifiEnabled || !isPermissionGranted) {
            return listBuilder.build();
        }

        final WifiScanWorker worker = SliceBackgroundWorker.getInstance(getUri());
        final List<WifiSliceItem> apList = worker != null ? worker.getResults() : null;
        final int apCount = apList == null ? 0 : apList.size();
        final boolean isFirstApActive = apCount > 0
                && apList.get(0).getConnectedState() != WifiEntry.CONNECTED_STATE_DISCONNECTED;

        if (isFirstApActive) {
            // refresh header subtext
            listBuilder = getListBuilder(
                    true /* isWifiEnabled */, apList.get(0), true /* isWiFiPermissionGranted */);
        }

        if (isApRowCollapsed()) {
            return listBuilder.build();
        }

        // Add AP rows
        final CharSequence placeholder = mContext.getText(R.string.summary_placeholder);
        for (int i = 0; i < DEFAULT_EXPANDED_ROW_COUNT; i++) {
            if (i < apCount) {
                listBuilder.addRow(getWifiSliceItemRow(apList.get(i)));
            } else if (i == apCount) {
                listBuilder.addRow(getLoadingRow(placeholder));
            } else {
                listBuilder.addRow(new ListBuilder.RowBuilder()
                        .setTitle(placeholder)
                        .setSubtitle(placeholder));
            }
        }
        return listBuilder.build();
    }

    private boolean isCallerExemptUid(Context context) {
        final String[] allowedUidNames = context.getResources().getStringArray(
                R.array.config_exempt_wifi_permission_uid_name);
        final String uidName =
                context.getPackageManager().getNameForUid(Binder.getCallingUid());
        Log.d(TAG, "calling uid name : " + uidName);

        for (String allowedUidName : allowedUidNames) {
            if (TextUtils.equals(uidName, allowedUidName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPermissionGranted(Context settingsContext) {
        final int callingUid = Binder.getCallingUid();
        final String callingPackage = settingsContext.getPackageManager()
                .getPackagesForUid(callingUid)[0];

        Context packageContext;
        try {
            packageContext = settingsContext.createPackageContext(callingPackage, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot create Context for package: " + callingPackage);
            return false;
        }

        // If app doesn't have related Wi-Fi permission, they shouldn't show Wi-Fi slice.
        final boolean hasPermission = packageContext.checkPermission(
                android.Manifest.permission.CHANGE_WIFI_STATE, Binder.getCallingPid(),
                callingUid) == PackageManager.PERMISSION_GRANTED;
        AppStateChangeWifiStateBridge.WifiSettingsState state =
                new AppStateChangeWifiStateBridge(settingsContext, null, null)
                        .getWifiSettingsInfo(callingPackage, callingUid);

        return hasPermission && state.isPermissible();
    }

    protected boolean isApRowCollapsed() {
        return false;
    }

    protected ListBuilder.RowBuilder getHeaderRow(boolean isWifiEnabled,
            WifiSliceItem wifiSliceItem) {
        final IconCompat icon = IconCompat.createWithResource(mContext,
                R.drawable.ic_settings_wireless);
        final String title = mContext.getString(R.string.wifi_settings);
        final PendingIntent primaryAction = getPrimaryAction();
        final SliceAction primarySliceAction = SliceAction.createDeeplink(primaryAction, icon,
                ListBuilder.ICON_IMAGE, title);

        final ListBuilder.RowBuilder builder = new ListBuilder.RowBuilder()
                .setTitle(title)
                .setPrimaryAction(primarySliceAction);

        if (!mWifiRestriction.isChangeWifiStateAllowed(mContext)) {
            builder.setSubtitle(mContext.getString(R.string.not_allowed_by_ent));
        }
        return builder;
    }

    private ListBuilder getListBuilder(boolean isWifiEnabled, WifiSliceItem wifiSliceItem,
            boolean isWiFiPermissionGranted) {
        final ListBuilder builder = new ListBuilder(mContext, getUri(), ListBuilder.INFINITY)
                .setAccentColor(COLOR_NOT_TINTED)
                .setKeywords(getKeywords())
                .addRow(getHeaderRow(isWifiEnabled, wifiSliceItem));
        if (!isWiFiPermissionGranted || !mWifiRestriction.isChangeWifiStateAllowed(mContext)) {
            return builder;
        }

        final PendingIntent toggleAction = getBroadcastIntent(mContext);
        final SliceAction toggleSliceAction = SliceAction.createToggle(toggleAction,
                null /* actionTitle */, isWifiEnabled);
        builder.addAction(toggleSliceAction);

        return builder;
    }

    protected ListBuilder.RowBuilder getWifiSliceItemRow(WifiSliceItem wifiSliceItem) {
        final CharSequence title = wifiSliceItem.getTitle();
        final IconCompat levelIcon = getWifiSliceItemLevelIcon(wifiSliceItem);
        final ListBuilder.RowBuilder rowBuilder = new ListBuilder.RowBuilder()
                .setTitleItem(levelIcon, ListBuilder.ICON_IMAGE)
                .setTitle(title)
                .setSubtitle(wifiSliceItem.getSummary())
                .setContentDescription(wifiSliceItem.getContentDescription())
                .setPrimaryAction(getWifiEntryAction(wifiSliceItem, levelIcon, title));

        final IconCompat endIcon = getEndIcon(wifiSliceItem);
        if (endIcon != null) {
            rowBuilder.addEndItem(endIcon, ListBuilder.ICON_IMAGE);
        }
        return rowBuilder;
    }

    protected IconCompat getWifiSliceItemLevelIcon(WifiSliceItem wifiSliceItem) {
        final @ColorInt int tint;
        if (wifiSliceItem.getConnectedState() == WifiEntry.CONNECTED_STATE_CONNECTED) {
            tint = Utils.getColorAccentDefaultColor(mContext);
        } else if (wifiSliceItem.getConnectedState() == WifiEntry.CONNECTED_STATE_DISCONNECTED) {
            tint = Utils.getColorAttrDefaultColor(mContext, android.R.attr.colorControlNormal);
        } else {
            tint = Utils.getDisabled(mContext, Utils.getColorAttrDefaultColor(mContext,
                    android.R.attr.colorControlNormal));
        }

        final Drawable drawable = mContext.getDrawable(
                WifiUtils.getInternetIconResource(wifiSliceItem.getLevel(),
                        wifiSliceItem.shouldShowXLevelIcon()));
        drawable.setTint(tint);
        return Utils.createIconWithDrawable(drawable);
    }

    protected IconCompat getEndIcon(WifiSliceItem wifiSliceItem) {
        if (wifiSliceItem.getConnectedState() != WifiEntry.CONNECTED_STATE_DISCONNECTED) {
            return IconCompat.createWithResource(mContext, R.drawable.ic_settings_24dp);
        }

        if (wifiSliceItem.getSecurity() != WifiEntry.SECURITY_NONE) {
            return IconCompat.createWithResource(mContext, R.drawable.ic_friction_lock_closed);
        }
        return null;
    }

    protected SliceAction getWifiEntryAction(WifiSliceItem wifiSliceItem, IconCompat icon,
            CharSequence title) {
        final int requestCode = wifiSliceItem.getKey().hashCode();

        if (wifiSliceItem.getConnectedState() != WifiEntry.CONNECTED_STATE_DISCONNECTED) {
            final Bundle bundle = new Bundle();
            bundle.putString(WifiNetworkDetailsFragment.KEY_CHOSEN_WIFIENTRY_KEY,
                    wifiSliceItem.getKey());
            final Intent intent = new SubSettingLauncher(mContext)
                    .setTitleRes(R.string.pref_title_network_details)
                    .setDestination(WifiNetworkDetailsFragment.class.getName())
                    .setArguments(bundle)
                    .setSourceMetricsCategory(SettingsEnums.WIFI)
                    .toIntent();
            return getActivityAction(requestCode, intent, icon, title);
        }

        if (wifiSliceItem.shouldEditBeforeConnect()) {
            final Intent intent = new Intent(mContext, WifiDialogActivity.class)
                    .putExtra(WifiDialogActivity.KEY_CHOSEN_WIFIENTRY_KEY, wifiSliceItem.getKey());
            return getActivityAction(requestCode, intent, icon, title);
        }

        final Intent intent = new Intent(mContext, ConnectToWifiHandler.class)
                .putExtra(ConnectToWifiHandler.KEY_CHOSEN_WIFIENTRY_KEY, wifiSliceItem.getKey())
                .putExtra(ConnectToWifiHandler.KEY_WIFI_SLICE_URI, getUri());
        return getBroadcastAction(requestCode, intent, icon, title);
    }

    private SliceAction getActivityAction(int requestCode, Intent intent, IconCompat icon,
            CharSequence title) {
        final PendingIntent pi = PendingIntent.getActivity(mContext, requestCode, intent,
                PendingIntent.FLAG_IMMUTABLE /* flags */);
        return SliceAction.createDeeplink(pi, icon, ListBuilder.ICON_IMAGE, title);
    }

    private SliceAction getBroadcastAction(int requestCode, Intent intent, IconCompat icon,
            CharSequence title) {
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        final PendingIntent pi = PendingIntent.getBroadcast(mContext, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
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
        final String className = NetworkProviderSettings.class.getName();
        final String key = WifiSwitchPreferenceController.KEY;

        final Intent intent = SliceBuilderUtils.buildSearchResultPageIntent(mContext, className,
                        key, screenTitle, SettingsEnums.DIALOG_WIFI_AP_EDIT, this)
                .setClassName(mContext.getPackageName(), SubSettings.class.getName())
                .setData(contentUri);

        return intent;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_network;
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
                intent, PendingIntent.FLAG_IMMUTABLE /* flags */);
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

    @VisibleForTesting
    static class WifiRestriction {
        public boolean isChangeWifiStateAllowed(@Nullable Context context) {
            if (context == null) return true;
            return WifiEnterpriseRestrictionUtils.isChangeWifiStateAllowed(context);
        }
    }
}
