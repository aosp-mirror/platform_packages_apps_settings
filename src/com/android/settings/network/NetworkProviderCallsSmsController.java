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

import static androidx.lifecycle.Lifecycle.Event;

import android.content.Context;
import android.os.UserManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.Utils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.List;

public class NetworkProviderCallsSmsController extends AbstractPreferenceController implements
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient, LifecycleObserver {

    private static final String TAG = "NetworkProviderCallsSmsController";
    private static final String KEY = "calls_and_sms";
    private static final String RTL_MARK = "\u200F";

    private UserManager mUserManager;
    private SubscriptionManager mSubscriptionManager;
    private SubscriptionsChangeListener mSubscriptionsChangeListener;
    private TelephonyManager mTelephonyManager;
    private RestrictedPreference mPreference;
    private boolean mIsRtlMode;

    /**
     * The summary text and click behavior of the "Calls & SMS" item on the
     * Network & internet page.
     */
    public NetworkProviderCallsSmsController(Context context, Lifecycle lifecycle) {
        super(context);

        mUserManager = context.getSystemService(UserManager.class);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mIsRtlMode = context.getResources().getConfiguration().getLayoutDirection()
                == View.LAYOUT_DIRECTION_RTL;
        if (lifecycle != null) {
            mSubscriptionsChangeListener = new SubscriptionsChangeListener(context, this);
            lifecycle.addObserver(this);
        }
    }

    @OnLifecycleEvent(Event.ON_RESUME)
    public void onResume() {
        mSubscriptionsChangeListener.start();
        update();
    }

    @OnLifecycleEvent(Event.ON_PAUSE)
    public void onPause() {
        mSubscriptionsChangeListener.stop();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public CharSequence getSummary() {
        final List<SubscriptionInfo> subs = SubscriptionUtil.getActiveSubscriptions(
                mSubscriptionManager);

        if (subs.isEmpty()) {
            return setSummaryResId(R.string.calls_sms_no_sim);
        } else {
            final StringBuilder summary = new StringBuilder();
            for (SubscriptionInfo subInfo : subs) {
                int subsSize = subs.size();
                int subId = subInfo.getSubscriptionId();
                final CharSequence displayName = SubscriptionUtil.getUniqueSubscriptionDisplayName(
                        subInfo, mContext);

                // Set displayName as summary if there is only one valid SIM.
                if (subsSize == 1
                        && SubscriptionManager.isValidSubscriptionId(subId)
                        && isInService(subId)) {
                    return displayName;
                }

                CharSequence status = getPreferredStatus(subsSize, subId);
                if (status.toString().isEmpty()) {
                    // If there are 2 or more SIMs and one of these has no preferred status,
                    // set only its displayName as summary.
                    summary.append(displayName);
                } else {
                    summary.append(displayName)
                            .append(" (")
                            .append(status)
                            .append(")");
                }
                // Do not add ", " for the last subscription.
                if (subInfo != subs.get(subs.size() - 1)) {
                    summary.append(", ");
                }

                if (mIsRtlMode) {
                    summary.insert(0, RTL_MARK).insert(summary.length(), RTL_MARK);
                }
            }
            return summary;
        }
    }

    @VisibleForTesting
    protected CharSequence getPreferredStatus(int subsSize, int subId) {
        String status = "";
        boolean isDataPreferred = subId == getDefaultVoiceSubscriptionId();
        boolean isSmsPreferred = subId == getDefaultSmsSubscriptionId();

        if (!SubscriptionManager.isValidSubscriptionId(subId) || !isInService(subId)) {
            status = setSummaryResId(subsSize > 1 ? R.string.calls_sms_unavailable :
                    R.string.calls_sms_temp_unavailable);
        } else {
            if (isDataPreferred && isSmsPreferred) {
                status = setSummaryResId(R.string.calls_sms_preferred);
            } else if (isDataPreferred) {
                status = setSummaryResId(R.string.calls_sms_calls_preferred);
            } else if (isSmsPreferred) {
                status = setSummaryResId(R.string.calls_sms_sms_preferred);
            }
        }
        return status;
    }

    private String setSummaryResId(int resId) {
        return mContext.getResources().getString(resId);
    }

    @VisibleForTesting
    protected int getDefaultVoiceSubscriptionId() {
        return SubscriptionManager.getDefaultVoiceSubscriptionId();
    }

    @VisibleForTesting
    protected int getDefaultSmsSubscriptionId() {
        return SubscriptionManager.getDefaultSmsSubscriptionId();
    }

    private void update() {
        if (mPreference == null || mPreference.isDisabledByAdmin()) {
            return;
        }
        refreshSummary(mPreference);
        mPreference.setOnPreferenceClickListener(null);
        mPreference.setFragment(null);

        final List<SubscriptionInfo> subs = SubscriptionUtil.getActiveSubscriptions(
                mSubscriptionManager);
        if (subs.isEmpty()) {
            mPreference.setEnabled(false);
        } else {
            mPreference.setEnabled(true);
            mPreference.setFragment(NetworkProviderCallsSmsFragment.class.getCanonicalName());
        }
    }

    @Override
    public boolean isAvailable() {
        return SubscriptionUtil.isSimHardwareVisible(mContext) &&
                mUserManager.isAdminUser();
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
        update();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference == null) {
            return;
        }
        refreshSummary(mPreference);
        update();
    }

    @Override
    public void onSubscriptionsChanged() {
        refreshSummary(mPreference);
        update();
    }

    @VisibleForTesting
    protected boolean isInService(int subId) {
        ServiceState serviceState =
                mTelephonyManager.createForSubscriptionId(subId).getServiceState();
        return Utils.isInService(serviceState);
    }
}
