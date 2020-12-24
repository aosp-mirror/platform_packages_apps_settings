/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.settings.network;

import static com.android.settings.network.telephony.MobileNetworkUtils.NO_CELL_DATA_TYPE_ICON;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.builders.GridRowBuilder;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.wifi.slice.WifiSliceItem;
import com.android.settingslib.WirelessUtils;
import com.android.settingslib.net.SignalStrengthUtil;
import com.android.settingslib.utils.ThreadUtils;
import com.android.wifitrackerlib.WifiEntry;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * The helper is for slice of carrier and non-Carrier, used by ProviderModelSlice.
 */
public class ProviderModelSliceHelper {
    private static final String TAG = "ProviderModelSlice";
    private final SubscriptionManager mSubscriptionManager;
    private final TelephonyManager mTelephonyManager;
    protected final Context mContext;
    private CustomSliceable mSliceable;

    public ProviderModelSliceHelper(Context context, CustomSliceable sliceable) {
        mContext = context;
        mSliceable = sliceable;
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
    }

    private static void log(String s) {
        Log.d(TAG, s);
    }

    protected ListBuilder.HeaderBuilder createHeader() {
        return new ListBuilder.HeaderBuilder()
                .setTitle(mContext.getText(R.string.summary_placeholder))
                .setPrimaryAction(getPrimarySliceAction());
    }

    protected ListBuilder createListBuilder(Uri uri) {
        final ListBuilder builder = new ListBuilder(mContext, uri, ListBuilder.INFINITY)
                .setAccentColor(-1)
                .setKeywords(getKeywords());
        return builder;
    }

    protected GridRowBuilder createMessageGridRow(int messageResId) {
        final CharSequence title = mContext.getText(messageResId);
        return new GridRowBuilder()
                // Add cells to the grid row.
                .addCell(new GridRowBuilder.CellBuilder().addTitleText(title))
                .setPrimaryAction(getPrimarySliceAction());
    }

    @Nullable
    protected WifiSliceItem getConnectedWifiItem(List<WifiSliceItem> wifiList) {
        if (wifiList == null) {
            return null;
        }
        Optional<WifiSliceItem> item = wifiList.stream()
                .filter(x -> x.getConnectedState() == WifiEntry.CONNECTED_STATE_CONNECTED)
                .findFirst();
        return item.isPresent() ? item.get() : null;
    }

    protected boolean hasCarrier() {
        if (isAirplaneModeEnabled()
                || mSubscriptionManager == null || mTelephonyManager == null
                || mSubscriptionManager.getDefaultDataSubscriptionId()
                == mSubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return false;
        }
        return true;
    }

    protected ListBuilder.RowBuilder createCarrierRow() {
        final String title = getMobileTitle();
        final String summary = getMobileSummary();
        Drawable drawable = mContext.getDrawable(
                R.drawable.ic_signal_strength_zero_bar_no_internet);
        try {
            drawable = getMobileDrawable(drawable);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        final IconCompat levelIcon = Utils.createIconWithDrawable(drawable);
        final PendingIntent toggleAction = mSliceable.getBroadcastIntent(mContext);
        final SliceAction toggleSliceAction = SliceAction.createToggle(toggleAction,
                "mobile_toggle" /* actionTitle */, isMobileDataEnabled());
        final ListBuilder.RowBuilder rowBuilder = new ListBuilder.RowBuilder()
                .setTitle(title)
                .setTitleItem(levelIcon, ListBuilder.ICON_IMAGE)
                .addEndItem(toggleSliceAction)
                .setPrimaryAction(toggleSliceAction)
                .setSubtitle(summary);
        return rowBuilder;
    }

    protected SliceAction getPrimarySliceAction() {
        return SliceAction.createDeeplink(
                getPrimaryAction(),
                Utils.createIconWithDrawable(new ColorDrawable(Color.TRANSPARENT)),
                ListBuilder.ICON_IMAGE, mContext.getText(R.string.summary_placeholder));
    }

    private PendingIntent getPrimaryAction() {
        final Intent intent = new Intent("android.settings.NETWORK_PROVIDER_SETTINGS")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(mContext, 0 /* requestCode */,
                intent, PendingIntent.FLAG_IMMUTABLE /* flags */);
    }

    private boolean shouldInflateSignalStrength(int subId) {
        return SignalStrengthUtil.shouldInflateSignalStrength(mContext, subId);
    }

    protected boolean isAirplaneModeEnabled() {
        return WirelessUtils.isAirplaneModeOn(mContext);
    }

    protected boolean isMobileDataEnabled() {
        if (mTelephonyManager == null) {
            return false;
        }
        return mTelephonyManager.isDataEnabled();
    }

    protected boolean isDataSimActive() {
        return MobileNetworkUtils.activeNetworkIsCellular(mContext);
    }

    protected boolean isNoCarrierData() {
        if (mTelephonyManager == null) {
            return false;
        }
        boolean mobileDataOnAndNoData = isMobileDataEnabled()
                && mTelephonyManager.getDataState() != mTelephonyManager.DATA_CONNECTED;
        ServiceState serviceState = mTelephonyManager.getServiceState();
        boolean mobileDataOffAndOutOfService = !isMobileDataEnabled() && serviceState != null
                && serviceState.getState() == serviceState.STATE_OUT_OF_SERVICE;
        log("mobileDataOnAndNoData: " + mobileDataOnAndNoData
                + ",mobileDataOffAndOutOfService: " + mobileDataOffAndOutOfService);
        return mobileDataOnAndNoData || mobileDataOffAndOutOfService;
    }

    private boolean isAirplaneSafeNetworksModeEnabled() {
        // TODO: isAirplaneSafeNetworksModeEnabled is not READY
        return false;
    }

    @VisibleForTesting
    Drawable getMobileDrawable(Drawable drawable) throws Throwable {
        // set color and drawable
        if (mTelephonyManager == null) {
            log("mTelephonyManager == null");
            return drawable;
        }
        if (!isNoCarrierData()) {
            Semaphore lock = new Semaphore(0);
            AtomicReference<Drawable> shared = new AtomicReference<>();
            ThreadUtils.postOnMainThread(() -> {
                shared.set(getDrawableWithSignalStrength());
                lock.release();
            });
            lock.acquire();
            drawable = shared.get();
        }

        if (isDataSimActive()) {
            drawable.setTint(Utils.getColorAccentDefaultColor(mContext));
        }
        return drawable;
    }

    /**
     * To get the signal bar icon with level.
     *
     * @return The Drawable which is a signal bar icon with level.
     */
    public Drawable getDrawableWithSignalStrength() {
        final SignalStrength strength = mTelephonyManager.getSignalStrength();
        int level = (strength == null) ? 0 : strength.getLevel();
        int numLevels = SignalStrength.NUM_SIGNAL_STRENGTH_BINS;
        if (mSubscriptionManager != null && shouldInflateSignalStrength(
                mSubscriptionManager.getDefaultDataSubscriptionId())) {
            level += 1;
            numLevels += 1;
        }
        return MobileNetworkUtils.getSignalStrengthIcon(mContext, level, numLevels,
                NO_CELL_DATA_TYPE_ICON, false);
    }

    private String getMobileSummary() {
        String summary = "";
        //TODO: get radio technology.
        String networkType = "";
        if (isDataSimActive()) {
            summary = mContext.getString(R.string.mobile_data_connection_active, networkType);
        } else if (!isMobileDataEnabled()) {
            summary = mContext.getString(R.string.mobile_data_off_summary);
        }
        return summary;
    }

    private String getMobileTitle() {
        String title = mContext.getText(R.string.mobile_data_settings_title).toString();
        if (mSubscriptionManager == null) {
            return title;
        }
        final SubscriptionInfo defaultSubscription = mSubscriptionManager.getActiveSubscriptionInfo(
                mSubscriptionManager.getDefaultDataSubscriptionId());
        if (defaultSubscription != null) {
            title = defaultSubscription.getDisplayName().toString();
        }
        return title;
    }

    protected SubscriptionManager getSubscriptionManager() {
        return mSubscriptionManager;
    }

    private Set<String> getKeywords() {
        final String keywords = mContext.getString(R.string.keywords_internet);
        return Arrays.stream(TextUtils.split(keywords, ","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }
}
