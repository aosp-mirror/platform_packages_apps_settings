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

package com.android.settings.network;

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.telephony.euicc.EuiccManager;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.AddPreference;
import com.android.settingslib.Utils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.mobile.dataservice.MobileNetworkInfoEntity;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;
import com.android.settingslib.mobile.dataservice.UiccInfoEntity;

import java.util.List;
import java.util.stream.Collectors;

public class MobileNetworkSummaryController extends AbstractPreferenceController implements
        LifecycleObserver, PreferenceControllerMixin,
        MobileNetworkRepository.MobileNetworkCallback {
    private static final String TAG = "MobileNetSummaryCtlr";

    private static final String KEY = "mobile_network_list";

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private UserManager mUserManager;
    private AddPreference mPreference;

    private MobileNetworkRepository mMobileNetworkRepository;
    private List<SubscriptionInfoEntity> mSubInfoEntityList;
    private List<UiccInfoEntity> mUiccInfoEntityList;
    private List<MobileNetworkInfoEntity> mMobileNetworkInfoEntityList;
    private boolean mIsAirplaneModeOn;
    private LifecycleOwner mLifecycleOwner;

    /**
     * This controls the summary text and click behavior of the "Mobile network" item on the
     * Network & internet page. There are 3 separate cases depending on the number of mobile network
     * subscriptions:
     * <ul>
     * <li>No subscription: click action begins a UI flow to add a network subscription, and
     * the summary text indicates this</li>
     *
     * <li>One subscription: click action takes you to details for that one network, and
     * the summary text is the network name</li>
     *
     * <li>More than one subscription: click action takes you to a page listing the subscriptions,
     * and the summary text gives the count of SIMs</li>
     * </ul>
     */
    public MobileNetworkSummaryController(Context context, Lifecycle lifecycle,
            LifecycleOwner lifecycleOwner) {
        super(context);
        mMetricsFeatureProvider = FeatureFactory.getFactory(mContext).getMetricsFeatureProvider();
        mUserManager = context.getSystemService(UserManager.class);
        mLifecycleOwner = lifecycleOwner;
        mMobileNetworkRepository = MobileNetworkRepository.create(context, this);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        mMobileNetworkRepository.addRegister(mLifecycleOwner);
        update();
    }

    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        mMobileNetworkRepository.removeRegister();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public CharSequence getSummary() {

        if ((mSubInfoEntityList == null || mSubInfoEntityList.isEmpty()) || (
                mUiccInfoEntityList == null || mUiccInfoEntityList.isEmpty()) || (
                mMobileNetworkInfoEntityList == null || mMobileNetworkInfoEntityList.isEmpty())) {
            if (MobileNetworkUtils.showEuiccSettingsDetecting(mContext)) {
                return mContext.getResources().getString(
                        R.string.mobile_network_summary_add_a_network);
            }
            // set empty string to override previous text for carrier when SIM available
            return "";
        } else if (mSubInfoEntityList.size() == 1) {
            SubscriptionInfoEntity info = mSubInfoEntityList.get(0);
            CharSequence displayName = info.uniqueName;
            if (info.isEmbedded || mUiccInfoEntityList.get(0).isActive
                    || mMobileNetworkInfoEntityList.get(0).showToggleForPhysicalSim) {
                return displayName;
            }
            return mContext.getString(R.string.mobile_network_tap_to_activate, displayName);
        } else {
            return mSubInfoEntityList.stream()
                    .map(SubscriptionInfoEntity::getUniqueDisplayName)
                    .collect(Collectors.joining(", "));
        }
    }

    private void logPreferenceClick(Preference preference) {
        mMetricsFeatureProvider.logClickedPreference(preference,
                preference.getExtras().getInt(DashboardFragment.CATEGORY));
    }

    private void startAddSimFlow() {
        final Intent intent = new Intent(EuiccManager.ACTION_PROVISION_EMBEDDED_SUBSCRIPTION);
        intent.putExtra(EuiccManager.EXTRA_FORCE_PROVISION, true);
        mContext.startActivity(intent);
    }

    private void initPreference() {
        refreshSummary(mPreference);
        mPreference.setOnPreferenceClickListener(null);
        mPreference.setOnAddClickListener(null);
        mPreference.setFragment(null);
        mPreference.setEnabled(!mIsAirplaneModeOn);
    }

    private void update() {
        if (mPreference == null || mPreference.isDisabledByAdmin()) {
            return;
        }

        initPreference();
        if (((mSubInfoEntityList == null || mSubInfoEntityList.isEmpty())
                || (mUiccInfoEntityList == null || mUiccInfoEntityList.isEmpty())
                || (mMobileNetworkInfoEntityList == null
                || mMobileNetworkInfoEntityList.isEmpty()))) {
            if (MobileNetworkUtils.showEuiccSettingsDetecting(mContext)) {
                mPreference.setOnPreferenceClickListener((Preference pref) -> {
                    logPreferenceClick(pref);
                    startAddSimFlow();
                    return true;
                });
            } else {
                mPreference.setEnabled(false);
            }
            return;
        }

        // We have one or more existing subscriptions, so we want the plus button if eSIM is
        // supported.
        if (MobileNetworkUtils.showEuiccSettingsDetecting(mContext)) {
            mPreference.setAddWidgetEnabled(!mIsAirplaneModeOn);
            mPreference.setOnAddClickListener(p -> {
                logPreferenceClick(p);
                startAddSimFlow();
            });
        }

        if (mSubInfoEntityList.size() == 1) {
            mPreference.setOnPreferenceClickListener((Preference pref) -> {
                logPreferenceClick(pref);
                SubscriptionInfoEntity info = mSubInfoEntityList.get(0);
                if (info.isEmbedded || mUiccInfoEntityList.get(0).isActive
                        || mMobileNetworkInfoEntityList.get(0).showToggleForPhysicalSim) {
                    MobileNetworkUtils.launchMobileNetworkSettings(mContext, info);
                    return true;
                }

                SubscriptionUtil.startToggleSubscriptionDialogActivity(
                        mContext, Integer.parseInt(info.subId), true);
                return true;
            });
        } else {
            mPreference.setFragment(MobileNetworkListFragment.class.getCanonicalName());
        }
    }

    @Override
    public boolean isAvailable() {
        return SubscriptionUtil.isSimHardwareVisible(mContext) &&
                !Utils.isWifiOnly(mContext) && mUserManager.isAdminUser();
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
        mIsAirplaneModeOn = airplaneModeEnabled;
        update();
    }

    @Override
    public void onAvailableSubInfoChanged(List<SubscriptionInfoEntity> subInfoEntityList) {
        if ((mSubInfoEntityList != null &&
                (subInfoEntityList.isEmpty() || !subInfoEntityList.equals(mSubInfoEntityList)))
                || (!subInfoEntityList.isEmpty() && mSubInfoEntityList == null)) {
            Log.d(TAG, "subInfo list from framework is changed, update the subInfo entity list.");
            mSubInfoEntityList = subInfoEntityList;
            update();
        }
    }

    @Override
    public void onActiveSubInfoChanged(List<SubscriptionInfoEntity> activeSubInfoList) {
    }

    @Override
    public void onAllUiccInfoChanged(List<UiccInfoEntity> uiccInfoEntityList) {
        mUiccInfoEntityList = uiccInfoEntityList;
        update();
    }

    @Override
    public void onAllMobileNetworkInfoChanged(
            List<MobileNetworkInfoEntity> mobileNetworkInfoEntityList) {
        mMobileNetworkInfoEntityList = mobileNetworkInfoEntityList;
        update();
    }
}
