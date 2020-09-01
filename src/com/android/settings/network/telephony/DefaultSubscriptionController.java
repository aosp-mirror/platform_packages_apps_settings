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
 * limitations under the License.
 */

package com.android.settings.network.telephony;

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import android.content.ComponentName;
import android.content.Context;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.SubscriptionsChangeListener;

import java.util.ArrayList;
import java.util.List;

/**
 * This implements common controller functionality for a Preference letting the user see/change
 * what mobile network subscription is used by default for some service controlled by the
 * SubscriptionManager. This can be used for services such as Calls or SMS.
 */
public abstract class DefaultSubscriptionController extends TelephonyBasePreferenceController
        implements LifecycleObserver, Preference.OnPreferenceChangeListener,
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {
    private static final String TAG = "DefaultSubController";

    protected SubscriptionsChangeListener mChangeListener;
    protected ListPreference mPreference;
    protected SubscriptionManager mManager;
    protected TelecomManager mTelecomManager;

    private static final String EMERGENCY_ACCOUNT_HANDLE_ID = "E";
    private static final ComponentName PSTN_CONNECTION_SERVICE_COMPONENT =
            new ComponentName("com.android.phone",
                    "com.android.services.telephony.TelephonyConnectionService");

    public DefaultSubscriptionController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mManager = context.getSystemService(SubscriptionManager.class);
        mChangeListener = new SubscriptionsChangeListener(context, this);
    }

    public void init(Lifecycle lifecycle) {
        lifecycle.addObserver(this);
    }

    /** @return SubscriptionInfo for the default subscription for the service, or null if there
     * isn't one. */
    protected abstract SubscriptionInfo getDefaultSubscriptionInfo();

    /** @return the id of the default subscription for the service, or
     * SubscriptionManager.INVALID_SUBSCRIPTION_ID if there isn't one. */
    protected abstract int getDefaultSubscriptionId();

    /** Called to change the default subscription for the service. */
    protected abstract void setDefaultSubscription(int subscriptionId);

    @Override
    public int getAvailabilityStatus(int subId) {
        final List<SubscriptionInfo> subs = SubscriptionUtil.getActiveSubscriptions(mManager);
        if (subs.size() > 1) {
            return AVAILABLE;
        } else {
            return CONDITIONALLY_UNAVAILABLE;
        }
    }

    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        mChangeListener.start();
        updateEntries();
    }

    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        mChangeListener.stop();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        updateEntries();
    }

    @Override
    public CharSequence getSummary() {
        final PhoneAccountHandle handle = getDefaultCallingAccountHandle();
        if ((handle != null) && (!isCallingAccountBindToSubscription(handle))) {
            // display VoIP account in summary when configured through settings within dialer
            return getLabelFromCallingAccount(handle);
        }
        final SubscriptionInfo info = getDefaultSubscriptionInfo();
        if (info != null) {
            // display subscription based account
            return info.getDisplayName();
        } else {
            return mContext.getString(R.string.calls_and_sms_ask_every_time);
        }
    }

    private void updateEntries() {
        if (mPreference == null) {
            return;
        }
        if (!isAvailable()) {
            mPreference.setVisible(false);
            return;
        }
        mPreference.setVisible(true);

        // TODO(b/135142209) - for now we need to manually ensure we're registered as a change
        // listener, because this might not have happened during displayPreference if
        // getAvailabilityStatus returned CONDITIONALLY_UNAVAILABLE at the time.
        mPreference.setOnPreferenceChangeListener(this);

        final List<SubscriptionInfo> subs = SubscriptionUtil.getActiveSubscriptions(mManager);

        // We'll have one entry for each available subscription, plus one for a "ask me every
        // time" entry at the end.
        final ArrayList<CharSequence> displayNames = new ArrayList<>();
        final ArrayList<CharSequence> subscriptionIds = new ArrayList<>();

        final int serviceDefaultSubId = getDefaultSubscriptionId();
        boolean subIsAvailable = false;

        for (SubscriptionInfo sub : subs) {
            if (sub.isOpportunistic()) {
                continue;
            }
            displayNames.add(sub.getDisplayName());
            final int subId = sub.getSubscriptionId();
            subscriptionIds.add(Integer.toString(subId));
            if (subId == serviceDefaultSubId) {
                subIsAvailable = true;
            }
        }
        // Add the extra "Ask every time" value at the end.
        displayNames.add(mContext.getString(R.string.calls_and_sms_ask_every_time));
        subscriptionIds.add(Integer.toString(SubscriptionManager.INVALID_SUBSCRIPTION_ID));

        mPreference.setEntries(displayNames.toArray(new CharSequence[0]));
        mPreference.setEntryValues(subscriptionIds.toArray(new CharSequence[0]));

        if (subIsAvailable) {
            mPreference.setValue(Integer.toString(serviceDefaultSubId));
        } else {
            mPreference.setValue(Integer.toString(SubscriptionManager.INVALID_SUBSCRIPTION_ID));
        }
    }

    /**
     * Get default calling account
     *
     * @return current calling account {@link PhoneAccountHandle}
     */
    public PhoneAccountHandle getDefaultCallingAccountHandle() {
        final PhoneAccountHandle currentSelectPhoneAccount =
                getTelecomManager().getUserSelectedOutgoingPhoneAccount();
        if (currentSelectPhoneAccount == null) {
            return null;
        }
        final List<PhoneAccountHandle> accountHandles =
                getTelecomManager().getCallCapablePhoneAccounts(false);
        final PhoneAccountHandle emergencyAccountHandle = new PhoneAccountHandle(
                PSTN_CONNECTION_SERVICE_COMPONENT, EMERGENCY_ACCOUNT_HANDLE_ID);
        if (currentSelectPhoneAccount.equals(emergencyAccountHandle)) {
            return null;
        }
        for (PhoneAccountHandle handle : accountHandles) {
            if (currentSelectPhoneAccount.equals(handle)) {
                return currentSelectPhoneAccount;
            }
        }
        return null;
    }

    @VisibleForTesting
    TelecomManager getTelecomManager() {
        if (mTelecomManager == null) {
            mTelecomManager = mContext.getSystemService(TelecomManager.class);
        }
        return mTelecomManager;
    }

    @VisibleForTesting
    PhoneAccount getPhoneAccount(PhoneAccountHandle handle) {
        return getTelecomManager().getPhoneAccount(handle);
    }

    /**
     * Check if calling account bind to subscription
     *
     * @param handle {@link PhoneAccountHandle} for specific calling account
     */
    public boolean isCallingAccountBindToSubscription(PhoneAccountHandle handle) {
        final PhoneAccount account = getPhoneAccount(handle);
        if (account == null) {
            return false;
        }
        return account.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION);
    }

    /**
     * Get label from calling account
     *
     * @param handle to get label from {@link PhoneAccountHandle}
     * @return label of calling account
     */
    public CharSequence getLabelFromCallingAccount(PhoneAccountHandle handle) {
        CharSequence label = null;
        final PhoneAccount account = getPhoneAccount(handle);
        if (account != null) {
            label = account.getLabel();
        }
        if (label != null) {
            label = mContext.getPackageManager().getUserBadgedLabel(label, handle.getUserHandle());
        }
        return (label != null) ? label : "";
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final int subscriptionId = Integer.parseInt((String) newValue);
        setDefaultSubscription(subscriptionId);
        refreshSummary(mPreference);
        return true;
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
    }

    @Override
    public void onSubscriptionsChanged() {
        if (mPreference != null) {
            updateEntries();
            refreshSummary(mPreference);
        }
    }
}
