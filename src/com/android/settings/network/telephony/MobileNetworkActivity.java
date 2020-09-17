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

package com.android.settings.network.telephony;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.ims.ImsRcsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.internal.util.CollectionUtils;
import com.android.settings.R;
import com.android.settings.core.SettingsBaseActivity;
import com.android.settings.network.ProxySubscriptionManager;
import com.android.settings.network.SubscriptionUtil;

import java.util.List;

/**
 * Activity for displaying MobileNetworkSettings
 */
public class MobileNetworkActivity extends SettingsBaseActivity
        implements ProxySubscriptionManager.OnActiveSubscriptionChangedListener {

    private static final String TAG = "MobileNetworkActivity";
    @VisibleForTesting
    static final String MOBILE_SETTINGS_TAG = "mobile_settings:";
    @VisibleForTesting
    static final int SUB_ID_NULL = Integer.MIN_VALUE;

    @VisibleForTesting
    ProxySubscriptionManager mProxySubscriptionMgr;

    private int mCurSubscriptionId;

    // This flag forces subscription information fragment to be re-created.
    // Otherwise, fragment will be kept when subscription id has not been changed.
    //
    // Set initial value to true allows subscription information fragment to be re-created when
    // Activity re-create occur.
    private boolean mFragmentForceReload = true;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        validate(intent);
        setIntent(intent);

        int updateSubscriptionIndex = SUB_ID_NULL;
        if (intent != null) {
            updateSubscriptionIndex = intent.getIntExtra(Settings.EXTRA_SUB_ID, SUB_ID_NULL);
        }
        int oldSubId = mCurSubscriptionId;
        mCurSubscriptionId = updateSubscriptionIndex;
        mFragmentForceReload = (mCurSubscriptionId == oldSubId);
        final SubscriptionInfo info = getSubscription();
        updateSubscriptions(info);

        // If the subscription has changed or the new intent doesnt contain the opt in action,
        // remove the old discovery dialog. If the activity is being recreated, we will see
        // onCreate -> onNewIntent, so the dialog will first be recreated for the old subscription
        // and then removed.
        if (updateSubscriptionIndex != oldSubId || !doesIntentContainOptInAction(intent)) {
            removeContactDiscoveryDialog(oldSubId);
        }
        // evaluate showing the new discovery dialog if this intent contains an action to show the
        // opt-in.
        if (doesIntentContainOptInAction(intent)) {
            maybeShowContactDiscoveryDialog(info);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final UserManager userManager = this.getSystemService(UserManager.class);
        if (!userManager.isAdminUser()) {
            this.finish();
            return;
        }

        final Toolbar toolbar = findViewById(R.id.action_bar);
        toolbar.setVisibility(View.VISIBLE);
        setActionBar(toolbar);

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        getProxySubscriptionManager().setLifecycle(getLifecycle());

        final Intent startIntent = getIntent();
        validate(startIntent);
        mCurSubscriptionId = savedInstanceState != null
                ? savedInstanceState.getInt(Settings.EXTRA_SUB_ID, SUB_ID_NULL)
                : ((startIntent != null)
                ? startIntent.getIntExtra(Settings.EXTRA_SUB_ID, SUB_ID_NULL)
                : SUB_ID_NULL);

        final SubscriptionInfo subscription = getSubscription();
        maybeShowContactDiscoveryDialog(subscription);

        // Since onChanged() will take place immediately when addActiveSubscriptionsListener(),
        // perform registration after mCurSubscriptionId been configured.
        registerActiveSubscriptionsListener();

        updateSubscriptions(subscription);
    }

    @VisibleForTesting
    ProxySubscriptionManager getProxySubscriptionManager() {
        if (mProxySubscriptionMgr == null) {
            mProxySubscriptionMgr = ProxySubscriptionManager.getInstance(this);
        }
        return mProxySubscriptionMgr;
    }

    @VisibleForTesting
    void registerActiveSubscriptionsListener() {
        getProxySubscriptionManager().addActiveSubscriptionsListener(this);
    }

    /**
     * Implementation of ProxySubscriptionManager.OnActiveSubscriptionChangedListener
     */
    public void onChanged() {
        SubscriptionInfo info = getSubscription();
        int oldSubIndex = mCurSubscriptionId;
        updateSubscriptions(info);

        // Remove the dialog if the subscription associated with this activity changes.
        if (info == null) {
            // Close the activity when subscription removed
            if ((oldSubIndex != SUB_ID_NULL)
                    && (!isFinishing()) && (!isDestroyed())) {
                finish();
            }
            return;
        }
        int subIndex = info.getSubscriptionId();
        if (subIndex != oldSubIndex) {
            removeContactDiscoveryDialog(oldSubIndex);
        }
    }

    @Override
    protected void onStart() {
        getProxySubscriptionManager().setLifecycle(getLifecycle());
        super.onStart();
        // updateSubscriptions doesn't need to be called, onChanged will always be called after we
        // register a listener.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mProxySubscriptionMgr == null) {
            return;
        }
        mProxySubscriptionMgr.removeActiveSubscriptionsListener(this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        saveInstanceState(outState);
    }

    @VisibleForTesting
    void saveInstanceState(@NonNull Bundle outState) {
        outState.putInt(Settings.EXTRA_SUB_ID, mCurSubscriptionId);
    }

    private void updateTitleAndNavigation(SubscriptionInfo subscription) {
        // Set the title to the name of the subscription. If we don't have subscription info, the
        // title will just default to the label for this activity that's already specified in
        // AndroidManifest.xml.
        if (subscription != null) {
            setTitle(subscription.getDisplayName());
        }
    }

    @VisibleForTesting
    void updateSubscriptions(SubscriptionInfo subscription) {
        if (subscription == null) {
            return;
        }
        final int subscriptionIndex = subscription.getSubscriptionId();

        updateTitleAndNavigation(subscription);
        switchFragment(subscription);

        mCurSubscriptionId = subscriptionIndex;
        mFragmentForceReload = false;
    }

    /**
     * Get the current subscription to display. First check whether intent has {@link
     * Settings#EXTRA_SUB_ID} and if so find the subscription with that id. If not, just return the
     * first one in the mSubscriptionInfos list since it is already sorted by sim slot.
     */
    @VisibleForTesting
    SubscriptionInfo getSubscription() {
        if (mCurSubscriptionId != SUB_ID_NULL) {
            return getSubscriptionForSubId(mCurSubscriptionId);
        }
        final List<SubscriptionInfo> subInfos = getProxySubscriptionManager()
                .getActiveSubscriptionsInfo();
        if (CollectionUtils.isEmpty(subInfos)) {
            return null;
        }
        return subInfos.get(0);
    }

    @VisibleForTesting
    SubscriptionInfo getSubscriptionForSubId(int subId) {
        return SubscriptionUtil.getAvailableSubscription(this,
                getProxySubscriptionManager(), subId);
    }

    @VisibleForTesting
    void switchFragment(SubscriptionInfo subInfo) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        final int subId = subInfo.getSubscriptionId();
        final Bundle bundle = new Bundle();
        bundle.putInt(Settings.EXTRA_SUB_ID, subId);

        final String fragmentTag = buildFragmentTag(subId);
        if (fragmentManager.findFragmentByTag(fragmentTag) != null) {
            if (!mFragmentForceReload) {
                Log.d(TAG, "Keep current fragment: " + fragmentTag);
                return;
            }
            Log.d(TAG, "Construct fragment: " + fragmentTag);
        }

        final Fragment fragment = new MobileNetworkSettings();
        fragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.content_frame, fragment, fragmentTag);
        fragmentTransaction.commit();
    }

    private void removeContactDiscoveryDialog(int subId) {
        ContactDiscoveryDialogFragment fragment = getContactDiscoveryFragment(subId);
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private ContactDiscoveryDialogFragment getContactDiscoveryFragment(int subId) {
        // In the case that we are rebuilding this activity after it has been destroyed and
        // recreated, look up the dialog in the fragment manager.
        return (ContactDiscoveryDialogFragment) getSupportFragmentManager()
                .findFragmentByTag(ContactDiscoveryDialogFragment.getFragmentTag(subId));
    }

    private void maybeShowContactDiscoveryDialog(SubscriptionInfo info) {
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        CharSequence carrierName = "";
        if (info != null) {
            subId = info.getSubscriptionId();
            carrierName = info.getDisplayName();
        }
        // If this activity was launched using ACTION_SHOW_CAPABILITY_DISCOVERY_OPT_IN, show the
        // associated dialog only if the opt-in has not been granted yet.
        boolean showOptInDialog = doesIntentContainOptInAction(getIntent())
                // has the carrier config enabled capability discovery?
                && MobileNetworkUtils.isContactDiscoveryVisible(this, subId)
                // has the user already enabled this configuration?
                && !MobileNetworkUtils.isContactDiscoveryEnabled(this, subId);
        ContactDiscoveryDialogFragment fragment = getContactDiscoveryFragment(subId);
        if (showOptInDialog) {
            if (fragment == null) {
                fragment = ContactDiscoveryDialogFragment.newInstance(subId, carrierName);
            }
            // Only try to show the dialog if it has not already been added, otherwise we may
            // accidentally add it multiple times, causing multiple dialogs.
            if (!fragment.isAdded()) {
                fragment.show(getSupportFragmentManager(),
                        ContactDiscoveryDialogFragment.getFragmentTag(subId));
            }
        }
    }

    private boolean doesIntentContainOptInAction(Intent intent) {
        String intentAction = (intent != null ? intent.getAction() : null);
        return TextUtils.equals(intentAction,
                ImsRcsManager.ACTION_SHOW_CAPABILITY_DISCOVERY_OPT_IN);
    }

    private void validate(Intent intent) {
        // Do not allow ACTION_SHOW_CAPABILITY_DISCOVERY_OPT_IN without a subscription id specified,
        // since we do not want the user to accidentally turn on capability polling for the wrong
        // subscription.
        if (doesIntentContainOptInAction(intent)) {
            if (SUB_ID_NULL == intent.getIntExtra(Settings.EXTRA_SUB_ID, SUB_ID_NULL)) {
                throw new IllegalArgumentException("Intent with action "
                        + "SHOW_CAPABILITY_DISCOVERY_OPT_IN must also include the extra "
                        + "Settings#EXTRA_SUB_ID");
            }
        }
    }

    @VisibleForTesting
    String buildFragmentTag(int subscriptionId) {
        return MOBILE_SETTINGS_TAG + subscriptionId;
    }
}
