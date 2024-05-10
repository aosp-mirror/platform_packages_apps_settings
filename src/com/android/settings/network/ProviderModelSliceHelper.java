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

import static com.android.settings.network.MobileIconGroupExtKt.maybeToHtml;
import static com.android.settings.network.telephony.MobileNetworkUtils.NO_CELL_DATA_TYPE_ICON;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.telephony.AccessNetworkConstants;
import android.telephony.NetworkRegistrationInfo;
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
 * TODO: Remove the class in U because Settings does not use slice anymore.
 */
public class ProviderModelSliceHelper {
    private static final String TAG = "ProviderModelSlice";
    private final SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;
    protected final Context mContext;
    private CustomSliceable mSliceable;

    public ProviderModelSliceHelper(Context context, CustomSliceable sliceable) {
        mContext = context;
        mSliceable = sliceable;
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
    }

    /**
     * @return whether there is the carrier item in the slice.
     */
    public boolean hasCarrier() {
        if (isAirplaneModeEnabled()
                || mSubscriptionManager == null || mTelephonyManager == null
                || mSubscriptionManager.getActiveSubscriptionIdList().length <= 0) {
            return false;
        }
        return true;
    }

    /**
     * @return whether the MobileData's is enabled.
     */
    public boolean isMobileDataEnabled() {
        return mTelephonyManager.isDataEnabled();
    }

    /**
     * To check the carrier data status.
     *
     * @return whether the carrier data is active.
     */
    public boolean isDataSimActive() {
        return MobileNetworkUtils.activeNetworkIsCellular(mContext);
    }

    /**
     * @return whether the ServiceState's data state is in-service.
     */
    public boolean isDataStateInService() {
        final ServiceState serviceState = mTelephonyManager.getServiceState();
        NetworkRegistrationInfo regInfo =
                (serviceState == null) ? null : serviceState.getNetworkRegistrationInfo(
                        NetworkRegistrationInfo.DOMAIN_PS,
                        AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
        return (regInfo == null) ? false : regInfo.isRegistered();
    }

    /**
     * @return whether the ServiceState's voice state is in-service.
     */
    public boolean isVoiceStateInService() {
        final ServiceState serviceState = mTelephonyManager.getServiceState();
        return serviceState != null
                && serviceState.getState() == serviceState.STATE_IN_SERVICE;
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
                NO_CELL_DATA_TYPE_ICON, false, false);
    }

    /**
     * To update the telephony with subid.
     */
    public void updateTelephony() {
        if (mSubscriptionManager == null || mSubscriptionManager.getDefaultDataSubscriptionId()
                == mSubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return;
        }
        mTelephonyManager = mTelephonyManager.createForSubscriptionId(
                mSubscriptionManager.getDefaultDataSubscriptionId());
    }

    protected ListBuilder createListBuilder(Uri uri) {
        final ListBuilder builder = new ListBuilder(mContext, uri, ListBuilder.INFINITY)
                .setAccentColor(-1)
                .setKeywords(getKeywords());
        return builder;
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

    protected ListBuilder.RowBuilder createCarrierRow(String networkTypeDescription) {
        final String title = getMobileTitle();
        final CharSequence summary = getMobileSummary(networkTypeDescription);
        Drawable drawable = mContext.getDrawable(
                R.drawable.ic_signal_strength_zero_bar_no_internet);
        try {
            drawable = getMobileDrawable(drawable);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        final IconCompat levelIcon = Utils.createIconWithDrawable(drawable);
        final PendingIntent rowIntent = mSliceable.getBroadcastIntent(mContext);
        final SliceAction primaryAction = SliceAction.create(rowIntent,
                levelIcon, ListBuilder.ICON_IMAGE, title);
        final SliceAction toggleAction = SliceAction.createToggle(rowIntent,
                "mobile_toggle" /* actionTitle */, isMobileDataEnabled());
        final ListBuilder.RowBuilder rowBuilder = new ListBuilder.RowBuilder()
                .setTitle(title)
                .setTitleItem(levelIcon, ListBuilder.ICON_IMAGE)
                .addEndItem(toggleAction)
                .setPrimaryAction(primaryAction)
                .setSubtitle(summary);
        return rowBuilder;
    }

    protected SliceAction getPrimarySliceAction(String intentAction) {
        return SliceAction.createDeeplink(
                getPrimaryAction(intentAction),
                Utils.createIconWithDrawable(new ColorDrawable(Color.TRANSPARENT)),
                ListBuilder.ICON_IMAGE, mContext.getText(R.string.summary_placeholder));
    }

    protected boolean isAirplaneModeEnabled() {
        return WirelessUtils.isAirplaneModeOn(mContext);
    }

    protected SubscriptionManager getSubscriptionManager() {
        return mSubscriptionManager;
    }

    private static void log(String s) {
        Log.d(TAG, s);
    }

    private PendingIntent getPrimaryAction(String intentAction) {
        final Intent intent = new Intent(intentAction)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(mContext, 0 /* requestCode */,
                intent, PendingIntent.FLAG_IMMUTABLE /* flags */);
    }

    private boolean shouldInflateSignalStrength(int subId) {
        return SignalStrengthUtil.shouldInflateSignalStrength(mContext, subId);
    }

    @VisibleForTesting
    Drawable getMobileDrawable(Drawable drawable) throws Throwable {
        // set color and drawable
        if (mTelephonyManager == null) {
            log("mTelephonyManager == null");
            return drawable;
        }
        if (isDataStateInService() || isVoiceStateInService()) {
            Semaphore lock = new Semaphore(0);
            AtomicReference<Drawable> shared = new AtomicReference<>();
            ThreadUtils.postOnMainThread(() -> {
                shared.set(getDrawableWithSignalStrength());
                lock.release();
            });
            lock.acquire();
            drawable = shared.get();
        }

        drawable.setTint(
                Utils.getColorAttrDefaultColor(mContext, android.R.attr.colorControlNormal));
        if (isDataSimActive()) {
            drawable.setTint(Utils.getColorAccentDefaultColor(mContext));
        }
        return drawable;
    }

    private CharSequence getMobileSummary(String networkTypeDescription) {
        if (!isMobileDataEnabled()) {
            return mContext.getString(R.string.mobile_data_off_summary);
        }
        if (!isDataStateInService()) {
            return mContext.getString(R.string.mobile_data_no_connection);
        }
        String summary = networkTypeDescription;
        if (isDataSimActive()) {
            summary = mContext.getString(R.string.preference_summary_default_combination,
                    mContext.getString(R.string.mobile_data_connection_active),
                    networkTypeDescription);
        }
        return maybeToHtml(summary);
    }

    protected String getMobileTitle() {
        String title = mContext.getText(R.string.mobile_data_settings_title).toString();
        if (mSubscriptionManager == null) {
            return title;
        }
        final SubscriptionInfo defaultSubscription = mSubscriptionManager.getActiveSubscriptionInfo(
                mSubscriptionManager.getDefaultDataSubscriptionId());
        if (defaultSubscription != null) {
            title = SubscriptionUtil.getUniqueSubscriptionDisplayName(
                    defaultSubscription, mContext).toString();
        }
        return title;
    }

    private Set<String> getKeywords() {
        final String keywords = mContext.getString(R.string.keywords_internet);
        return Arrays.stream(TextUtils.split(keywords, ","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }
}
