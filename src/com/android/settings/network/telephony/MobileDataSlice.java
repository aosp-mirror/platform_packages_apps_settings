/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.network.telephony;

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import android.annotation.ColorInt;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.network.MobileDataContentObserver;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settingslib.WirelessUtils;

import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.util.List;

/**
 * Custom {@link Slice} for Mobile Data.
 * <p>
 *     We make a custom slice instead of using {@link MobileDataPreferenceController} because the
 *     pref controller is generalized across any carrier, and thus does not control a specific
 *     subscription. We attempt to reuse any telephony-specific code from the preference controller.
 *
 * </p>
 *
 */
public class MobileDataSlice implements CustomSliceable {
    private static final String TAG = "MobileDataSlice";

    private final Context mContext;
    private final SubscriptionManager mSubscriptionManager;
    private final TelephonyManager mTelephonyManager;

    public MobileDataSlice(Context context) {
        mContext = context;
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
    }

    @Override
    public Slice getSlice() {
        ListBuilder listBuilder = createListBuilder();
        if (!isConfigMobileNetworksAllowed()) {
            return listBuilder.build();
        }

        final IconCompat icon = IconCompat.createWithResource(mContext,
                R.drawable.ic_network_cell);
        final String title = mContext.getText(R.string.mobile_data_settings_title).toString();
        @ColorInt final int color = Utils.getColorAccentDefaultColor(mContext);

        // Return empty slice until we can show a disabled-action Slice, blaming Airplane mode.
        if (isAirplaneModeEnabled()) {
            return listBuilder.build();
        }

        // Return empty slice until we can show a disabled-action Slice.
        if (!isMobileDataAvailable()) {
            return listBuilder.build();
        }

        final CharSequence summary = getSummary();
        final PendingIntent toggleAction = getBroadcastIntent(mContext);
        final PendingIntent primaryAction = getPrimaryAction();
        final SliceAction primarySliceAction = SliceAction.createDeeplink(primaryAction, icon,
                ListBuilder.ICON_IMAGE, title);
        final SliceAction toggleSliceAction = SliceAction.createToggle(toggleAction,
                null /* actionTitle */, isMobileDataEnabled());
        final ListBuilder.RowBuilder rowBuilder = new ListBuilder.RowBuilder()
                .setTitle(title)
                .addEndItem(toggleSliceAction)
                .setPrimaryAction(primarySliceAction);
        if (!Utils.isSettingsIntelligence(mContext)) {
            rowBuilder.setSubtitle(summary);
        }

        return listBuilder
                .setAccentColor(color)
                .addRow(rowBuilder)
                .build();
    }

    @VisibleForTesting
    ListBuilder createListBuilder() {
        return new ListBuilder(mContext, getUri(), ListBuilder.INFINITY);
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.MOBILE_DATA_SLICE_URI;
    }

    @Override
    public void onNotifyChange(Intent intent) {
        final boolean newState = intent.getBooleanExtra(EXTRA_TOGGLE_STATE,
                isMobileDataEnabled());

        final int defaultSubId = getDefaultSubscriptionId(mSubscriptionManager);
        if (defaultSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return; // No subscription - do nothing.
        }
        Log.d(TAG, "setMobileDataEnabled: " + newState);
        MobileNetworkUtils.setMobileDataEnabled(mContext, defaultSubId, newState,
                false /* disableOtherSubscriptions */);
        // Do not notifyChange on Uri. The service takes longer to update the current value than it
        // does for the Slice to check the current value again. Let {@link WifiScanWorker}
        // handle it.
    }

    @Override
    public IntentFilter getIntentFilter() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        return filter;
    }

    @Override
    public Intent getIntent() {
        return new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS).setPackage(
                SETTINGS_PACKAGE_NAME);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_network;
    }

    @Override
    public Class<? extends SliceBackgroundWorker> getBackgroundWorkerClass() {
        return MobileDataWorker.class;
    }

    protected static int getDefaultSubscriptionId(SubscriptionManager subscriptionManager) {
        final SubscriptionInfo defaultSubscription = subscriptionManager.getActiveSubscriptionInfo(
                subscriptionManager.getDefaultDataSubscriptionId());
        if (defaultSubscription == null) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID; // No default subscription
        }

        return defaultSubscription.getSubscriptionId();
    }

    private CharSequence getSummary() {
        final SubscriptionInfo defaultSubscription = mSubscriptionManager.getActiveSubscriptionInfo(
                mSubscriptionManager.getDefaultDataSubscriptionId());
        if (defaultSubscription == null) {
            return null; // no summary text
        }

        return SubscriptionUtil.getUniqueSubscriptionDisplayName(defaultSubscription, mContext);
    }

    private PendingIntent getPrimaryAction() {
        final Intent intent = getIntent();
        return PendingIntent.getActivity(mContext, 0 /* requestCode */, intent,
                PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * @return {@code true} when mobile data is not supported by the current device.
     */
    private boolean isMobileDataAvailable() {
        final List<SubscriptionInfo> subInfoList =
                SubscriptionUtil.getSelectableSubscriptionInfoList(mContext);

        return !(subInfoList == null || subInfoList.isEmpty());
    }

    @VisibleForTesting
    boolean isAirplaneModeEnabled() {
        return WirelessUtils.isAirplaneModeOn(mContext);
    }

    @VisibleForTesting
    boolean isMobileDataEnabled() {
        if (mTelephonyManager == null) {
            return false;
        }

        return mTelephonyManager.isDataEnabled();
    }

    @VisibleForTesting
    boolean isConfigMobileNetworksAllowed() {
        if (mContext == null) return true;
        UserManager userManager = mContext.getSystemService(UserManager.class);
        if (userManager == null) return true;
        boolean isAllowed = userManager.isAdminUser() && !userManager.hasUserRestriction(
                UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS);
        if (!isAllowed) {
            Log.w(TAG, "The user is not allowed to configure Mobile Networks.");
        }
        return isAllowed;
    }

    /**
     * Listener for mobile data state changes.
     *
     * <p>
     *     Listen to individual subscription changes since there is no framework broadcast.
     *
     *     This worker registers a ContentObserver in the background and updates the MobileData
     *     Slice when the value changes.
     */
    public static class MobileDataWorker extends SliceBackgroundWorker<Void> {

        DataContentObserver mMobileDataObserver;

        public MobileDataWorker(Context context, Uri uri) {
            super(context, uri);
            final Handler handler = new Handler(Looper.getMainLooper());
            mMobileDataObserver = new DataContentObserver(handler, this);
        }

        @Override
        protected void onSlicePinned() {
            final SubscriptionManager subscriptionManager =
                    getContext().getSystemService(SubscriptionManager.class);
            mMobileDataObserver.register(getContext(),
                    getDefaultSubscriptionId(subscriptionManager));
        }

        @Override
        protected void onSliceUnpinned() {
            mMobileDataObserver.unRegister(getContext());
        }

        @Override
        public void close() throws IOException {
            mMobileDataObserver = null;
        }

        public void updateSlice() {
            notifySliceChange();
        }

        public class DataContentObserver extends ContentObserver {

            private final MobileDataWorker mSliceBackgroundWorker;

            public DataContentObserver(Handler handler, MobileDataWorker backgroundWorker) {
                super(handler);
                mSliceBackgroundWorker = backgroundWorker;
            }

            @Override
            public void onChange(boolean selfChange) {
                mSliceBackgroundWorker.updateSlice();
            }

            public void register(Context context, int subId) {
                final Uri uri = MobileDataContentObserver.getObservableUri(context, subId);
                context.getContentResolver().registerContentObserver(uri, false, this);
            }

            public void unRegister(Context context) {
                context.getContentResolver().unregisterContentObserver(this);
            }
        }
    }
}
